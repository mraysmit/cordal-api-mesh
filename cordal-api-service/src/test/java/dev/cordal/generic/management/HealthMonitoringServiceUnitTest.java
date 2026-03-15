package dev.cordal.generic.management;

import dev.cordal.generic.config.DatabaseConfig;
import dev.cordal.generic.config.EndpointConfigurationManager;
import dev.cordal.generic.config.ApiEndpointConfig;
import dev.cordal.generic.config.QueryConfig;
import dev.cordal.generic.database.DatabaseConnectionManager;
import dev.cordal.generic.dto.ConfigurationHealthResponse;
import dev.cordal.generic.dto.DatabaseHealthResponse;
import dev.cordal.generic.dto.DeploymentInfoResponse;
import dev.cordal.generic.dto.HealthStatusResponse;
import dev.cordal.generic.dto.MemoryUsageResponse;
import dev.cordal.generic.dto.ReadinessCheckResponse;
import dev.cordal.generic.dto.ServiceHealthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("deprecation")
public class HealthMonitoringServiceUnitTest {

    private DatabaseConnectionManager databaseConnectionManager;
    private EndpointConfigurationManager configurationManager;
    private HealthMonitoringService service;

    @BeforeEach
    void setUp() {
        databaseConnectionManager = mock(DatabaseConnectionManager.class);
        configurationManager = mock(EndpointConfigurationManager.class);
        service = new HealthMonitoringService(databaseConnectionManager, configurationManager);
    }

    @Test
    void serviceHealthShouldExposeRuntimeDataAndBackwardCompatibleMap() {
        ServiceHealthResponse health = service.getServiceHealth();
        Map<String, Object> map = service.getServiceHealthMap();

        assertThat(health.isUp()).isTrue();
        assertThat(health.getStatus()).isEqualTo("UP");
        assertThat(health.getUptime()).matches("\\d+d \\d+h \\d+m \\d+s");
        assertThat(health.getMemoryUsage()).isNotNull();
        assertThat(health.getThreadCount()).isPositive();
        assertThat(map)
            .containsEntry("status", "UP")
            .containsEntry("uptime", health.getUptime())
            .containsEntry("threadCount", health.getThreadCount());
        assertThat(map.get("memoryUsage")).isInstanceOf(Map.class);
    }

    @Test
    void checkDatabaseHealthShouldCacheHealthyResults() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);

        when(databaseConnectionManager.getDataSource("orders-db")).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(3)).thenReturn(true);

        HealthMonitoringService.DatabaseHealthStatus first = service.checkDatabaseHealth("orders-db");
        HealthMonitoringService.DatabaseHealthStatus second = service.checkDatabaseHealth("orders-db");

        assertThat(first.getStatus()).isEqualTo("UP");
        assertThat(first.getMessage()).isEqualTo("Connection successful");
        assertThat(second).isSameAs(first);
        verify(databaseConnectionManager, times(1)).getDataSource("orders-db");
        verify(dataSource, times(1)).getConnection();
        verify(connection, times(1)).close();
    }

    @Test
    void checkDatabaseHealthShouldHandleMissingAndFailingDataSources() throws Exception {
        DataSource invalidDataSource = mock(DataSource.class);

        when(databaseConnectionManager.getDataSource("missing-db")).thenReturn(null);
        when(databaseConnectionManager.getDataSource("invalid-db")).thenReturn(invalidDataSource);
        when(invalidDataSource.getConnection()).thenThrow(new SQLException("connection refused"));
        when(databaseConnectionManager.getDataSource("error-db")).thenThrow(new IllegalArgumentException("Database not configured: error-db"));

        HealthMonitoringService.DatabaseHealthStatus missing = service.checkDatabaseHealth("missing-db");
        HealthMonitoringService.DatabaseHealthStatus invalid = service.checkDatabaseHealth("invalid-db");
        HealthMonitoringService.DatabaseHealthStatus error = service.checkDatabaseHealth("error-db");

        assertThat(missing.getStatus()).isEqualTo("DOWN");
        assertThat(missing.getMessage()).isEqualTo("DataSource not found");
        assertThat(invalid.getStatus()).isEqualTo("DOWN");
        assertThat(invalid.getMessage()).isEqualTo("Connection validation failed");
        assertThat(error.getStatus()).isEqualTo("DOWN");
        assertThat(error.getMessage()).contains("Health check error: Database not configured: error-db");
    }

    @Test
    void databasesHealthShouldProduceTypedAndMapRepresentations() {
        DatabaseConfig databaseConfig = new DatabaseConfig();

        when(configurationManager.getAllDatabaseConfigurations()).thenReturn(Map.of("orders-db", databaseConfig));
        HealthMonitoringService spyService = spy(service);
        HealthMonitoringService.DatabaseHealthStatus status = new HealthMonitoringService.DatabaseHealthStatus(
            "orders-db", "UP", "Connection successful", Instant.now(), 12
        );
        doReturn(status).when(spyService).checkDatabaseHealth("orders-db");

        Map<String, DatabaseHealthResponse> typed = spyService.getDatabasesHealth();
        Map<String, Object> mapped = spyService.getDatabasesHealthMap();

        assertThat(typed).containsKey("orders-db");
        assertThat(typed.get("orders-db").isUp()).isTrue();
        assertThat(mapped).containsKey("orders-db");
        assertThat(mapped.get("orders-db")).isInstanceOf(Map.class);
    }

    @Test
    void configurationHealthShouldReportUpAndDownStates() {
        when(configurationManager.getAllDatabaseConfigurations()).thenReturn(Map.of("orders-db", new DatabaseConfig()));
        when(configurationManager.getAllQueryConfigurations()).thenReturn(Map.of("orders-query", new QueryConfig()));
        when(configurationManager.getAllEndpointConfigurations()).thenReturn(Map.of("orders", new ApiEndpointConfig()));

        ConfigurationHealthResponse healthy = service.getConfigurationHealth();
        Map<String, Object> healthyMap = service.getConfigurationHealthMap();

        assertThat(healthy.isUp()).isTrue();
        assertThat(healthy.getTotalConfigurations()).isEqualTo(3);
        assertThat(healthyMap).containsEntry("status", "UP").containsEntry("lastValidation", "SUCCESS");

        when(configurationManager.getAllDatabaseConfigurations()).thenThrow(new RuntimeException("config failure"));

        ConfigurationHealthResponse down = service.getConfigurationHealth();

        assertThat(down.isDown()).isTrue();
        assertThat(down.getError()).isEqualTo("config failure");
    }

    @Test
    void overallHealthShouldReportUpDegradedAndDown() {
        HealthMonitoringService spyService = spy(service);

        doReturn(Map.of("orders-db", DatabaseHealthResponse.up("orders-db", "ok", 10)))
            .when(spyService).getDatabasesHealth();
        doReturn(ConfigurationHealthResponse.up(1, 1, 1)).when(spyService).getConfigurationHealth();
        assertThat(spyService.getOverallHealth()).isEqualTo("UP");

        doReturn(Map.of("orders-db", DatabaseHealthResponse.down("orders-db", "down", 10)))
            .when(spyService).getDatabasesHealth();
        assertThat(spyService.getOverallHealth()).isEqualTo("DEGRADED");

        doReturn(ConfigurationHealthResponse.down("broken config")).when(spyService).getConfigurationHealth();
        assertThat(spyService.getOverallHealth()).isEqualTo("DOWN");

        doThrow(new RuntimeException("boom-health")).when(spyService).getDatabasesHealth();
        assertThat(spyService.getOverallHealth()).isEqualTo("DOWN");
    }

    @Test
    void healthStatusShouldAggregateTypedAndMapViews() {
        HealthMonitoringService spyService = spy(service);
        ServiceHealthResponse serviceHealth = ServiceHealthResponse.up("0d 0h 1m 0s", new MemoryUsageResponse(100, 50, 20, 30, 40), 12);
        Map<String, DatabaseHealthResponse> databases = Map.of("orders-db", DatabaseHealthResponse.up("orders-db", "ok", 10));
        ConfigurationHealthResponse configuration = ConfigurationHealthResponse.up(1, 1, 1);

        doReturn(serviceHealth).when(spyService).getServiceHealth();
        doReturn(databases).when(spyService).getDatabasesHealth();
        doReturn(configuration).when(spyService).getConfigurationHealth();
        doReturn("UP").when(spyService).getOverallHealth();

        HealthStatusResponse health = spyService.getHealthStatus();
        Map<String, Object> healthMap = spyService.getHealthStatusMap();

        assertThat(health.isHealthy()).isTrue();
        assertThat(health.getService()).isEqualTo(serviceHealth);
        assertThat(health.getDatabases()).isEqualTo(databases);
        assertThat(health.getConfiguration()).isEqualTo(configuration);
        assertThat(healthMap).containsEntry("overall", "UP");
        assertThat(healthMap.get("service")).isInstanceOf(Map.class);
        assertThat(healthMap.get("databases")).isInstanceOf(Map.class);
    }

    @Test
    void readinessAndLivenessChecksShouldReflectDependencyStates() {
        HealthMonitoringService spyService = spy(service);

        when(configurationManager.getAllDatabaseConfigurations()).thenReturn(Map.of("orders-db", new DatabaseConfig()));
        doReturn(Map.of("orders-db", DatabaseHealthResponse.up("orders-db", "ok", 10))).when(spyService).getDatabasesHealth();
        doReturn(new MemoryUsageResponse(100, 50, 20, 30, 40)).when(spyService).getMemoryUsage();

        ReadinessCheckResponse ready = spyService.getReadinessCheck();
        ReadinessCheckResponse alive = spyService.getLivenessCheck();

        assertThat(ready.isReady()).isTrue();
        assertThat(ready.getChecks()).containsEntry("configuration", "OK").containsEntry("databases", "OK").containsEntry("memory", "OK");
        assertThat(alive.isReady()).isTrue();
        assertThat(alive.getChecks()).containsEntry("application", "UP").containsEntry("memory", "OK");

        when(configurationManager.getAllDatabaseConfigurations()).thenReturn(Map.of());
        doReturn(Map.of("orders-db", DatabaseHealthResponse.down("orders-db", "down", 10))).when(spyService).getDatabasesHealth();
        doReturn(new MemoryUsageResponse(100, 50, 49, 1, 99)).when(spyService).getMemoryUsage();

        ReadinessCheckResponse notReady = spyService.getReadinessCheck();
        ReadinessCheckResponse notAlive = spyService.getLivenessCheck();

        assertThat(notReady.isNotReady()).isTrue();
        assertThat(notReady.getChecks())
            .containsEntry("configuration", "NO_DATABASES")
            .containsEntry("databases", "SOME_DOWN")
            .containsEntry("memory", "HIGH_USAGE");
        assertThat(notAlive.isNotReady()).isTrue();
        assertThat(notAlive.getChecks()).containsEntry("memory", "CRITICAL");
    }

    @Test
    void deploymentAndJarInfoShouldProvideDiagnosticMetadata() {
        DeploymentInfoResponse deploymentInfo = service.getDeploymentInfo();
        Map<String, Object> deploymentMap = service.getDeploymentInfoMap();
        Map<String, Object> jarInfo = service.getJarInfo();

        assertThat(deploymentInfo.getApplicationName()).isEqualTo("Generic API Service");
        assertThat(deploymentInfo.getApplicationVersion()).isNotBlank();
        assertThat(deploymentInfo.getJarPath()).isNotBlank();
        assertThat(deploymentMap)
            .containsEntry("applicationName", "Generic API Service")
            .containsKey("jarPath")
            .containsKey("javaVersion");
        assertThat(jarInfo)
            .containsKeys("jarPath", "jarType", "classPath", "libraryPath", "modulePath")
            .doesNotContainKey("error");
    }
}
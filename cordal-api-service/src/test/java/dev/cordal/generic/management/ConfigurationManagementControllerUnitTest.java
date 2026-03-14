package dev.cordal.generic.management;

import dev.cordal.dto.ConfigurationSourceInfoResponse;
import dev.cordal.dto.ConfigurationStatisticsResponse;
import dev.cordal.generic.config.ApiEndpointConfig;
import dev.cordal.generic.config.DatabaseConfig;
import dev.cordal.generic.config.QueryConfig;
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConfigurationManagementControllerUnitTest {

    private ConfigurationManagementService service;
    private ConfigurationManagementController controller;
    private Context ctx;

    @BeforeEach
    void setUp() {
        service = mock(ConfigurationManagementService.class);
        controller = new ConfigurationManagementController(service);
        ctx = mock(Context.class);
        when(ctx.status(any(Integer.class))).thenReturn(ctx);
    }

    @Test
    void databaseEndpointsShouldHandleSuccessNotFoundAndErrors() {
        DatabaseConfig config = new DatabaseConfig();
        config.setUrl("jdbc:h2:mem:test");
        Map<String, Object> listResult = Map.of("count", 1);
        Map<String, Object> saveResult = Map.of("saved", true);
        Map<String, Object> deleteResult = Map.of("deleted", true);

        when(ctx.pathParam("name")).thenReturn("orders-db");
        when(ctx.bodyAsClass(DatabaseConfig.class)).thenReturn(config);
        when(service.getAllDatabaseConfigurationsMap()).thenReturn(listResult);
        when(service.getDatabaseConfiguration("orders-db")).thenReturn(Optional.of(config)).thenReturn(Optional.empty());
        when(service.saveDatabaseConfigurationMap("orders-db", config)).thenReturn(saveResult);
        when(service.deleteDatabaseConfigurationMap("orders-db")).thenReturn(deleteResult);

        controller.getAllDatabaseConfigurations(ctx);
        controller.getDatabaseConfiguration(ctx);
        controller.getDatabaseConfiguration(ctx);
        controller.saveDatabaseConfiguration(ctx);
        controller.deleteDatabaseConfiguration(ctx);

        verify(ctx).json(listResult);
        verify(ctx).json(saveResult);
        verify(ctx).json(deleteResult);
        verify(ctx).status(404);
        verify(ctx).json(argThat(payload -> payload instanceof Map<?, ?> map
            && Boolean.FALSE.equals(map.get("found"))
            && "orders-db".equals(map.get("name"))));
    }

    @Test
    void databaseEndpointsShouldMapIllegalStateAndGenericFailures() {
        DatabaseConfig config = new DatabaseConfig();

        when(ctx.pathParam("name")).thenReturn("orders-db");
        when(ctx.bodyAsClass(DatabaseConfig.class)).thenReturn(config);
        when(service.getAllDatabaseConfigurationsMap()).thenThrow(new RuntimeException("boom-list"));
        when(service.getDatabaseConfiguration("orders-db")).thenThrow(new RuntimeException("boom-get"));
        when(service.saveDatabaseConfigurationMap("orders-db", config)).thenThrow(new IllegalStateException("yaml mode"));
        when(service.deleteDatabaseConfigurationMap("orders-db")).thenThrow(new RuntimeException("boom-delete"));

        controller.getAllDatabaseConfigurations(ctx);
        controller.getDatabaseConfiguration(ctx);
        controller.saveDatabaseConfiguration(ctx);
        controller.deleteDatabaseConfiguration(ctx);

        verify(ctx).status(500);
        verify(ctx).status(400);
    }

    @Test
    void queryEndpointsShouldHandleSuccessNotFoundAndErrors() {
        QueryConfig config = new QueryConfig();
        Map<String, Object> listResult = Map.of("count", 1);
        Map<String, Object> byDatabaseResult = Map.of("database", "orders-db");
        Map<String, Object> saveResult = Map.of("saved", true);
        Map<String, Object> deleteResult = Map.of("deleted", true);

        when(ctx.pathParam("name")).thenReturn("orders-query");
        when(ctx.pathParam("databaseName")).thenReturn("orders-db");
        when(ctx.bodyAsClass(QueryConfig.class)).thenReturn(config);
        when(service.getAllQueryConfigurationsMap()).thenReturn(listResult);
        when(service.getQueryConfiguration("orders-query")).thenReturn(Optional.of(config)).thenReturn(Optional.empty());
        when(service.getQueryConfigurationsByDatabaseMap("orders-db")).thenReturn(byDatabaseResult);
        when(service.saveQueryConfigurationMap("orders-query", config)).thenReturn(saveResult);
        when(service.deleteQueryConfigurationMap("orders-query")).thenReturn(deleteResult);

        controller.getAllQueryConfigurations(ctx);
        controller.getQueryConfiguration(ctx);
        controller.getQueryConfiguration(ctx);
        controller.getQueryConfigurationsByDatabase(ctx);
        controller.saveQueryConfiguration(ctx);
        controller.deleteQueryConfiguration(ctx);

        verify(ctx).json(listResult);
        verify(ctx).json(byDatabaseResult);
        verify(ctx).json(saveResult);
        verify(ctx).json(deleteResult);
        verify(ctx).status(404);
    }

    @Test
    void queryEndpointsShouldMapFailures() {
        QueryConfig config = new QueryConfig();

        when(ctx.pathParam("name")).thenReturn("orders-query");
        when(ctx.pathParam("databaseName")).thenReturn("orders-db");
        when(ctx.bodyAsClass(QueryConfig.class)).thenReturn(config);
        when(service.getAllQueryConfigurationsMap()).thenThrow(new RuntimeException("boom-list"));
        when(service.getQueryConfiguration("orders-query")).thenThrow(new RuntimeException("boom-get"));
        when(service.getQueryConfigurationsByDatabaseMap("orders-db")).thenThrow(new RuntimeException("boom-filter"));
        when(service.saveQueryConfigurationMap("orders-query", config)).thenThrow(new IllegalStateException("yaml mode"));
        when(service.deleteQueryConfigurationMap("orders-query")).thenThrow(new RuntimeException("boom-delete"));

        controller.getAllQueryConfigurations(ctx);
        controller.getQueryConfiguration(ctx);
        controller.getQueryConfigurationsByDatabase(ctx);
        controller.saveQueryConfiguration(ctx);
        controller.deleteQueryConfiguration(ctx);

        verify(ctx).status(500);
        verify(ctx).status(400);
    }

    @Test
    void endpointEndpointsShouldHandleSuccessNotFoundAndErrors() {
        ApiEndpointConfig config = new ApiEndpointConfig();
        Map<String, Object> listResult = Map.of("count", 1);
        Map<String, Object> byQueryResult = Map.of("query", "orders-query");
        Map<String, Object> saveResult = Map.of("saved", true);
        Map<String, Object> deleteResult = Map.of("deleted", true);

        when(ctx.pathParam("name")).thenReturn("orders");
        when(ctx.pathParam("queryName")).thenReturn("orders-query");
        when(ctx.bodyAsClass(ApiEndpointConfig.class)).thenReturn(config);
        when(service.getAllEndpointConfigurationsMap()).thenReturn(listResult);
        when(service.getEndpointConfiguration("orders")).thenReturn(Optional.of(config)).thenReturn(Optional.empty());
        when(service.getEndpointConfigurationsByQueryMap("orders-query")).thenReturn(byQueryResult);
        when(service.saveEndpointConfigurationMap("orders", config)).thenReturn(saveResult);
        when(service.deleteEndpointConfigurationMap("orders")).thenReturn(deleteResult);

        controller.getAllEndpointConfigurations(ctx);
        controller.getEndpointConfiguration(ctx);
        controller.getEndpointConfiguration(ctx);
        controller.getEndpointConfigurationsByQuery(ctx);
        controller.saveEndpointConfiguration(ctx);
        controller.deleteEndpointConfiguration(ctx);

        verify(ctx).json(listResult);
        verify(ctx).json(byQueryResult);
        verify(ctx).json(saveResult);
        verify(ctx).json(deleteResult);
        verify(ctx).status(404);
    }

    @Test
    void endpointEndpointsShouldMapFailures() {
        ApiEndpointConfig config = new ApiEndpointConfig();

        when(ctx.pathParam("name")).thenReturn("orders");
        when(ctx.pathParam("queryName")).thenReturn("orders-query");
        when(ctx.bodyAsClass(ApiEndpointConfig.class)).thenReturn(config);
        when(service.getAllEndpointConfigurationsMap()).thenThrow(new RuntimeException("boom-list"));
        when(service.getEndpointConfiguration("orders")).thenThrow(new RuntimeException("boom-get"));
        when(service.getEndpointConfigurationsByQueryMap("orders-query")).thenThrow(new RuntimeException("boom-filter"));
        when(service.saveEndpointConfigurationMap("orders", config)).thenThrow(new IllegalStateException("yaml mode"));
        when(service.deleteEndpointConfigurationMap("orders")).thenThrow(new RuntimeException("boom-delete"));

        controller.getAllEndpointConfigurations(ctx);
        controller.getEndpointConfiguration(ctx);
        controller.getEndpointConfigurationsByQuery(ctx);
        controller.saveEndpointConfiguration(ctx);
        controller.deleteEndpointConfiguration(ctx);

        verify(ctx).status(500);
        verify(ctx).status(400);
    }

    @Test
    void managementEndpointsShouldReturnAvailabilityAndMetadata() {
        ConfigurationStatisticsResponse statistics = mock(ConfigurationStatisticsResponse.class);
        ConfigurationSourceInfoResponse sourceInfo = new ConfigurationSourceInfoResponse("database", true, List.of("yaml", "database"), Instant.now());

        when(service.getConfigurationStatistics()).thenReturn(statistics);
        when(service.getConfigurationSourceInfo()).thenReturn(sourceInfo);
        when(service.isConfigurationManagementAvailable()).thenReturn(true);

        controller.getConfigurationStatistics(ctx);
        controller.getConfigurationSourceInfo(ctx);
        controller.getConfigurationManagementAvailability(ctx);

        verify(ctx).json(statistics);
        verify(ctx).json(sourceInfo);
        verify(ctx).json(argThat(payload -> payload instanceof Map<?, ?> map
            && Boolean.TRUE.equals(map.get("available"))
            && "database".equals(map.get("source"))));
    }

    @Test
    void managementEndpointsShouldMapAvailabilityFailures() {
        when(service.getConfigurationStatistics()).thenThrow(new RuntimeException("boom-stats"));
        when(service.getConfigurationSourceInfo()).thenThrow(new RuntimeException("boom-source"));

        controller.getConfigurationStatistics(ctx);
        controller.getConfigurationSourceInfo(ctx);
        controller.getConfigurationManagementAvailability(ctx);

        verify(ctx).status(500);
    }
}
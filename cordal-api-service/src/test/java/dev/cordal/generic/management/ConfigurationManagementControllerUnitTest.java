package dev.cordal.generic.management;

import dev.cordal.dto.ConfigurationSourceInfoResponse;
import dev.cordal.dto.ConfigurationStatisticsResponse;
import dev.cordal.generic.config.ApiEndpointConfig;
import dev.cordal.generic.config.DatabaseConfig;
import dev.cordal.generic.config.QueryConfig;
import dev.cordal.generic.dto.ConfigurationCollectionResponse;
import dev.cordal.generic.dto.ConfigurationOperationResponse;
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
        ConfigurationCollectionResponse<DatabaseConfig> listResult = ConfigurationCollectionResponse.of("database", Map.of("orders-db", config));
        ConfigurationOperationResponse saveResult = ConfigurationOperationResponse.created("orders-db");
        ConfigurationOperationResponse deleteResult = ConfigurationOperationResponse.deleted("orders-db", true);

        when(ctx.pathParam("name")).thenReturn("orders-db");
        when(ctx.bodyAsClass(DatabaseConfig.class)).thenReturn(config);
        when(service.getAllDatabaseConfigurations()).thenReturn(listResult);
        when(service.getDatabaseConfiguration("orders-db")).thenReturn(Optional.of(config)).thenReturn(Optional.empty());
        when(service.saveDatabaseConfiguration("orders-db", config)).thenReturn(saveResult);
        when(service.deleteDatabaseConfiguration("orders-db")).thenReturn(deleteResult);

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
        when(service.getAllDatabaseConfigurations()).thenThrow(new RuntimeException("boom-list"));
        when(service.getDatabaseConfiguration("orders-db")).thenThrow(new RuntimeException("boom-get"));
        when(service.saveDatabaseConfiguration("orders-db", config)).thenThrow(new IllegalStateException("yaml mode"));
        when(service.deleteDatabaseConfiguration("orders-db")).thenThrow(new RuntimeException("boom-delete"));

        controller.getAllDatabaseConfigurations(ctx);
        controller.getDatabaseConfiguration(ctx);
        controller.saveDatabaseConfiguration(ctx);
        controller.deleteDatabaseConfiguration(ctx);

        verify(ctx, times(3)).status(500);
        verify(ctx, times(1)).status(400);
    }

    @Test
    void queryEndpointsShouldHandleSuccessNotFoundAndErrors() {
        QueryConfig config = new QueryConfig();
        ConfigurationCollectionResponse<QueryConfig> listResult = ConfigurationCollectionResponse.of("database", Map.of("orders-query", config));
        ConfigurationCollectionResponse<QueryConfig> byDatabaseResult = ConfigurationCollectionResponse.forDatabase("database", Map.of("orders-query", config), "orders-db");
        ConfigurationOperationResponse saveResult = ConfigurationOperationResponse.updated("orders-query");
        ConfigurationOperationResponse deleteResult = ConfigurationOperationResponse.deleted("orders-query", true);

        when(ctx.pathParam("name")).thenReturn("orders-query");
        when(ctx.pathParam("databaseName")).thenReturn("orders-db");
        when(ctx.bodyAsClass(QueryConfig.class)).thenReturn(config);
        when(service.getAllQueryConfigurations()).thenReturn(listResult);
        when(service.getQueryConfiguration("orders-query")).thenReturn(Optional.of(config)).thenReturn(Optional.empty());
        when(service.getQueryConfigurationsByDatabase("orders-db")).thenReturn(byDatabaseResult);
        when(service.saveQueryConfiguration("orders-query", config)).thenReturn(saveResult);
        when(service.deleteQueryConfiguration("orders-query")).thenReturn(deleteResult);

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
        when(service.getAllQueryConfigurations()).thenThrow(new RuntimeException("boom-list"));
        when(service.getQueryConfiguration("orders-query")).thenThrow(new RuntimeException("boom-get"));
        when(service.getQueryConfigurationsByDatabase("orders-db")).thenThrow(new RuntimeException("boom-filter"));
        when(service.saveQueryConfiguration("orders-query", config)).thenThrow(new IllegalStateException("yaml mode"));
        when(service.deleteQueryConfiguration("orders-query")).thenThrow(new RuntimeException("boom-delete"));

        controller.getAllQueryConfigurations(ctx);
        controller.getQueryConfiguration(ctx);
        controller.getQueryConfigurationsByDatabase(ctx);
        controller.saveQueryConfiguration(ctx);
        controller.deleteQueryConfiguration(ctx);

        verify(ctx, times(4)).status(500);
        verify(ctx, times(1)).status(400);
    }

    @Test
    void endpointEndpointsShouldHandleSuccessNotFoundAndErrors() {
        ApiEndpointConfig config = new ApiEndpointConfig();
        ConfigurationCollectionResponse<ApiEndpointConfig> listResult = ConfigurationCollectionResponse.of("database", Map.of("orders", config));
        ConfigurationCollectionResponse<ApiEndpointConfig> byQueryResult = ConfigurationCollectionResponse.forQuery("database", Map.of("orders", config), "orders-query");
        ConfigurationOperationResponse saveResult = ConfigurationOperationResponse.created("orders");
        ConfigurationOperationResponse deleteResult = ConfigurationOperationResponse.deleted("orders", true);

        when(ctx.pathParam("name")).thenReturn("orders");
        when(ctx.pathParam("queryName")).thenReturn("orders-query");
        when(ctx.bodyAsClass(ApiEndpointConfig.class)).thenReturn(config);
        when(service.getAllEndpointConfigurations()).thenReturn(listResult);
        when(service.getEndpointConfiguration("orders")).thenReturn(Optional.of(config)).thenReturn(Optional.empty());
        when(service.getEndpointConfigurationsByQuery("orders-query")).thenReturn(byQueryResult);
        when(service.saveEndpointConfiguration("orders", config)).thenReturn(saveResult);
        when(service.deleteEndpointConfiguration("orders")).thenReturn(deleteResult);

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
        when(service.getAllEndpointConfigurations()).thenThrow(new RuntimeException("boom-list"));
        when(service.getEndpointConfiguration("orders")).thenThrow(new RuntimeException("boom-get"));
        when(service.getEndpointConfigurationsByQuery("orders-query")).thenThrow(new RuntimeException("boom-filter"));
        when(service.saveEndpointConfiguration("orders", config)).thenThrow(new IllegalStateException("yaml mode"));
        when(service.deleteEndpointConfiguration("orders")).thenThrow(new RuntimeException("boom-delete"));

        controller.getAllEndpointConfigurations(ctx);
        controller.getEndpointConfiguration(ctx);
        controller.getEndpointConfigurationsByQuery(ctx);
        controller.saveEndpointConfiguration(ctx);
        controller.deleteEndpointConfiguration(ctx);

        verify(ctx, times(4)).status(500);
        verify(ctx, times(1)).status(400);
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

        verify(ctx, times(3)).status(500);
    }
}
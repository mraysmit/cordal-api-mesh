package dev.cordal.generic;

import dev.cordal.common.exception.ApiException;
import dev.cordal.generic.config.ApiEndpointConfig;
import dev.cordal.generic.config.DatabaseConfig;
import dev.cordal.generic.config.QueryConfig;
import dev.cordal.generic.dto.ConfigurationParametersResponse;
import dev.cordal.generic.dto.ConfigurationSchemaResponse;
import dev.cordal.generic.dto.ConfigurationSummaryResponse;
import dev.cordal.generic.dto.ConfigurationValidationResponse;
import dev.cordal.generic.dto.DatabaseConnectionsResponse;
import dev.cordal.generic.dto.RequestParameters;
import dev.cordal.generic.management.UsageStatisticsService;
import dev.cordal.generic.model.GenericResponse;
import io.javalin.http.Context;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GenericApiControllerUnitTest {

    @Test
    void handleEndpointRequestShouldMergeParametersAndRecordSuccess() {
        GenericApiService genericApiService = mock(GenericApiService.class);
        UsageStatisticsService statisticsService = mock(UsageStatisticsService.class);
        GenericApiController controller = new GenericApiController(genericApiService, statisticsService);
        Context ctx = mock(Context.class);
        GenericResponse response = GenericResponse.single(Map.of("result", "ok"));

        when(ctx.queryParamMap()).thenReturn(Map.of("region", List.of("west")));
        when(ctx.pathParamMap()).thenReturn(Map.of("orderId", "42"));
        when(ctx.formParamMap()).thenReturn(Map.of("status", List.of("open")));
        when(ctx.queryParam("async")).thenReturn(null);
        when(genericApiService.executeEndpoint(eq("orders"), any(RequestParameters.class))).thenReturn(response);

        controller.handleEndpointRequest(ctx, "orders");

        verify(ctx).json(response);
        verify(genericApiService).executeEndpoint(eq("orders"), argThat((RequestParameters parameters) ->
            "42".equals(parameters.asMap().get("orderId"))
                && "west".equals(parameters.asMap().get("region"))
                && "open".equals(parameters.asMap().get("status"))
        ));
        verify(statisticsService).recordEndpointUsage(eq("orders"), anyLong(), eq(true));
    }

    @Test
    void handleEndpointRequestShouldReturnAsyncReceipt() {
        GenericApiService genericApiService = mock(GenericApiService.class);
        UsageStatisticsService statisticsService = mock(UsageStatisticsService.class);
        GenericApiController controller = new GenericApiController(genericApiService, statisticsService);
        Context ctx = mock(Context.class);

        when(ctx.queryParamMap()).thenReturn(Map.of("async", List.of("true"), "region", List.of("west")));
        when(ctx.pathParamMap()).thenReturn(Map.of("orderId", "42"));
        when(ctx.formParamMap()).thenReturn(Map.of());
        when(ctx.queryParam("async")).thenReturn("true");
        when(genericApiService.executeEndpointAsync(eq("orders"), any(RequestParameters.class)))
            .thenReturn(CompletableFuture.completedFuture(GenericResponse.single(Map.of("result", "ok"))));

        controller.handleEndpointRequest(ctx, "orders");

        verify(ctx).json(argThat(response -> {
            if (!(response instanceof Map<?, ?> payload)) {
                return false;
            }
            return "Request submitted for async processing".equals(payload.get("message"))
                && "orders".equals(payload.get("endpoint"))
                && payload.containsKey("requestId");
        }));
        verify(genericApiService).executeEndpointAsync(eq("orders"), argThat((RequestParameters parameters) ->
            "42".equals(parameters.asMap().get("orderId"))
                && "west".equals(parameters.asMap().get("region"))
                && "true".equals(parameters.asMap().get("async"))
        ));
        verify(genericApiService, never()).executeEndpoint(any(), any(RequestParameters.class));
        verify(statisticsService).recordEndpointUsage(eq("orders"), anyLong(), eq(true));
    }

    @Test
    void getEndpointConfigurationShouldReturnNotFoundWhenMissing() {
        GenericApiService genericApiService = mock(GenericApiService.class);
        UsageStatisticsService statisticsService = mock(UsageStatisticsService.class);
        GenericApiController controller = new GenericApiController(genericApiService, statisticsService);
        Context ctx = mock(Context.class);

        when(ctx.pathParam("endpointName")).thenReturn("missing");
        when(genericApiService.getEndpointConfiguration("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getEndpointConfiguration(ctx))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Endpoint not found: missing");
    }

    @Test
    void getHealthStatusShouldReturnUpWhenAllEndpointsAreAvailable() {
        GenericApiService genericApiService = mock(GenericApiService.class);
        UsageStatisticsService statisticsService = mock(UsageStatisticsService.class);
        GenericApiController controller = new GenericApiController(genericApiService, statisticsService);
        Context ctx = mock(Context.class);

        when(genericApiService.getAvailableEndpoints()).thenReturn(Map.of("orders", new ApiEndpointConfig()));
        when(genericApiService.getAllEndpoints()).thenReturn(Map.of("orders", new ApiEndpointConfig()));
        when(genericApiService.getUnavailableEndpoints()).thenReturn(Map.of());

        controller.getHealthStatus(ctx);

        verify(ctx).status(200);
        verify(ctx).json(argThat(response -> {
            if (!(response instanceof Map<?, ?> payload)) {
                return false;
            }
            return "UP".equals(payload.get("status"))
                && "Generic API Service".equals(payload.get("service"))
                && Integer.valueOf(1).equals(payload.get("availableEndpoints"));
        }));
    }

    @Test
    void getHealthStatusShouldReturnDegradedWhenSomeEndpointsAreUnavailable() {
        GenericApiService genericApiService = mock(GenericApiService.class);
        UsageStatisticsService statisticsService = mock(UsageStatisticsService.class);
        GenericApiController controller = new GenericApiController(genericApiService, statisticsService);
        Context ctx = mock(Context.class);

        when(genericApiService.getAvailableEndpoints()).thenReturn(Map.of("orders", new ApiEndpointConfig()));
        when(genericApiService.getAllEndpoints()).thenReturn(Map.of("orders", new ApiEndpointConfig(), "reports", new ApiEndpointConfig()));
        when(genericApiService.getUnavailableEndpoints()).thenReturn(Map.of("reports", "database unavailable"));

        controller.getHealthStatus(ctx);

        verify(ctx).status(200);
        verify(ctx).json(argThat(response -> {
            if (!(response instanceof Map<?, ?> payload)) {
                return false;
            }
            return "DEGRADED".equals(payload.get("status"))
                && payload.containsKey("unavailableEndpoints")
                && payload.get("message").toString().contains("unavailable due to database connectivity issues");
        }));
    }

    @Test
    void getHealthStatusShouldReturnServiceUnavailableWhenNoEndpointsAreAvailable() {
        GenericApiService genericApiService = mock(GenericApiService.class);
        UsageStatisticsService statisticsService = mock(UsageStatisticsService.class);
        GenericApiController controller = new GenericApiController(genericApiService, statisticsService);
        Context ctx = mock(Context.class);

        when(genericApiService.getAvailableEndpoints()).thenReturn(Map.of());
        when(genericApiService.getAllEndpoints()).thenReturn(Map.of("orders", new ApiEndpointConfig()));
        when(genericApiService.getUnavailableEndpoints()).thenReturn(Map.of("orders", "database unavailable"));

        controller.getHealthStatus(ctx);

        verify(ctx).status(503);
        verify(ctx).json(argThat(response -> {
            if (!(response instanceof Map<?, ?> payload)) {
                return false;
            }
            return "DOWN".equals(payload.get("status"))
                && Integer.valueOf(0).equals(payload.get("availableEndpoints"));
        }));
    }

    @Test
    void getHealthStatusShouldReturnDownWhenHealthCheckThrows() {
        GenericApiService genericApiService = mock(GenericApiService.class);
        UsageStatisticsService statisticsService = mock(UsageStatisticsService.class);
        GenericApiController controller = new GenericApiController(genericApiService, statisticsService);
        Context ctx = mock(Context.class);

        when(ctx.status(503)).thenReturn(ctx);
        when(genericApiService.getAvailableEndpoints()).thenThrow(new RuntimeException("boom"));

        controller.getHealthStatus(ctx);

        verify(ctx).status(503);
        verify(ctx).json(argThat(response -> {
            if (!(response instanceof Map<?, ?> payload)) {
                return false;
            }
            return "DOWN".equals(payload.get("status"))
                && "boom".equals(payload.get("error"));
        }));
    }

    @Test
    void configurationSummaryAndValidationEndpointsShouldReturnServicePayloads() {
        GenericApiService genericApiService = mock(GenericApiService.class);
        UsageStatisticsService statisticsService = mock(UsageStatisticsService.class);
        GenericApiController controller = new GenericApiController(genericApiService, statisticsService);
        Context ctx = mock(Context.class);

        QueryConfig queryConfig = new QueryConfig();
        queryConfig.setName("orders-query");
        DatabaseConfig databaseConfig = new DatabaseConfig();
        databaseConfig.setUrl("jdbc:h2:mem:test");
        Map<String, Object> relationships = Map.of("orders", List.of("orders-query"));
        Map<String, Object> endpointConnectivity = Map.of("reachable", 3);
        ConfigurationValidationResponse databaseValidation =
            ConfigurationValidationResponse.forConfigType("database", 1, List.of(), List.of("database warning"));

        when(ctx.pathParam("queryName")).thenReturn("orders-query");
        when(ctx.pathParam("databaseName")).thenReturn("orders-db");
        when(genericApiService.getAvailableEndpoints()).thenReturn(Map.of("orders", new ApiEndpointConfig()));
        when(genericApiService.getAllQueryConfigurations()).thenReturn(Map.of("orders-query", queryConfig));
        when(genericApiService.getQueryConfiguration("orders-query")).thenReturn(Optional.of(queryConfig));
        when(genericApiService.getAllDatabaseConfigurations()).thenReturn(Map.of("orders-db", databaseConfig));
        when(genericApiService.getDatabaseConfiguration("orders-db")).thenReturn(Optional.of(databaseConfig));
        when(genericApiService.getConfigurationRelationships()).thenReturn(relationships);
        ConfigurationValidationResponse allValidation = mock(ConfigurationValidationResponse.class);
        ConfigurationValidationResponse endpointValidation = mock(ConfigurationValidationResponse.class);
        ConfigurationValidationResponse queryValidation = mock(ConfigurationValidationResponse.class);
        when(genericApiService.validateConfigurations()).thenReturn(allValidation);
        when(genericApiService.validateEndpointConfigurations()).thenReturn(endpointValidation);
        when(genericApiService.validateQueryConfigurations()).thenReturn(queryValidation);
        when(genericApiService.validateDatabaseConfigurations()).thenReturn(databaseValidation);
        when(genericApiService.validateEndpointConnectivity()).thenReturn(endpointConnectivity);
        when(genericApiService.validateConfigurationRelationships()).thenReturn(Map.of("relationships", "ok"));

        controller.getAvailableEndpoints(ctx);
        controller.getQueryConfigurations(ctx);
        controller.getQueryConfiguration(ctx);
        controller.getCompleteConfiguration(ctx);
        controller.getDatabaseConfigurations(ctx);
        controller.getDatabaseConfiguration(ctx);
        controller.getConfigurationRelationships(ctx);
        controller.validateConfigurations(ctx);
        controller.validateEndpointConfigurations(ctx);
        controller.validateQueryConfigurations(ctx);
        controller.validateDatabaseConfigurations(ctx);
        controller.validateEndpointConnectivity(ctx);
        controller.validateConfigurationRelationships(ctx);

        verify(ctx).json(argThat(response -> response instanceof Map<?, ?> payload
            && Integer.valueOf(1).equals(payload.get("totalEndpoints"))
            && payload.containsKey("endpoints")));
        verify(ctx).json(argThat(response -> response instanceof Map<?, ?> payload
            && Integer.valueOf(1).equals(payload.get("totalQueries"))
            && payload.containsKey("queries")));
        verify(ctx).json(queryConfig);
        verify(ctx).json(argThat(response -> response instanceof Map<?, ?> payload
            && payload.containsKey("summary")
            && payload.containsKey("databases")));
        verify(ctx).json(argThat(response -> response instanceof Map<?, ?> payload
            && Integer.valueOf(1).equals(payload.get("totalDatabases"))
            && payload.containsKey("databases")));
        verify(ctx).json(databaseConfig);
        verify(ctx).json(relationships);
        verify(ctx).json(allValidation);
        verify(ctx).json(endpointValidation);
        verify(ctx).json(queryValidation);
        verify(ctx).json(databaseValidation);
        verify(ctx).json(endpointConnectivity);
        verify(ctx).json(Map.of("relationships", "ok"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void granularConfigurationEndpointsShouldReturnSchemaParameterAndConnectionPayloads() {
        GenericApiService genericApiService = mock(GenericApiService.class);
        UsageStatisticsService statisticsService = mock(UsageStatisticsService.class);
        GenericApiController controller = new GenericApiController(genericApiService, statisticsService);
        Context ctx = mock(Context.class);

        ConfigurationSchemaResponse endpointSchema = mock(ConfigurationSchemaResponse.class);
        ConfigurationParametersResponse<ApiEndpointConfig.EndpointParameter> endpointParameters = mock(ConfigurationParametersResponse.class);
        DatabaseConnectionsResponse endpointConnections = mock(DatabaseConnectionsResponse.class);
        ConfigurationSummaryResponse endpointSummary = mock(ConfigurationSummaryResponse.class);
        ConfigurationSchemaResponse querySchema = mock(ConfigurationSchemaResponse.class);
        ConfigurationParametersResponse<QueryConfig.QueryParameter> queryParameters = mock(ConfigurationParametersResponse.class);
        DatabaseConnectionsResponse queryConnections = mock(DatabaseConnectionsResponse.class);
        ConfigurationSummaryResponse querySummary = mock(ConfigurationSummaryResponse.class);
        ConfigurationSchemaResponse databaseSchema = mock(ConfigurationSchemaResponse.class);
        Map<String, Object> databaseParameters = Map.of("orders-db", Map.of("maximumPoolSize", 10));
        Map<String, Object> databaseConnections = Map.of("orders-db", "jdbc:h2:mem:test");
        Map<String, Object> databaseSummary = Map.of("type", "database", "count", 1);

        when(genericApiService.getEndpointConfigurationSchema()).thenReturn(endpointSchema);
        when(genericApiService.getEndpointParameters()).thenReturn(endpointParameters);
        when(genericApiService.getEndpointDatabaseConnections()).thenReturn(endpointConnections);
        when(genericApiService.getEndpointConfigurationSummary()).thenReturn(endpointSummary);
        when(genericApiService.getQueryConfigurationSchema()).thenReturn(querySchema);
        when(genericApiService.getQueryParameters()).thenReturn(queryParameters);
        when(genericApiService.getQueryDatabaseConnections()).thenReturn(queryConnections);
        when(genericApiService.getQueryConfigurationSummary()).thenReturn(querySummary);
        when(genericApiService.getDatabaseConfigurationSchema()).thenReturn(databaseSchema);
        when(genericApiService.getDatabaseParameters()).thenReturn(databaseParameters);
        when(genericApiService.getDatabaseConnections()).thenReturn(databaseConnections);
        when(genericApiService.getDatabaseConfigurationSummary()).thenReturn(databaseSummary);

        controller.getEndpointConfigurationSchema(ctx);
        controller.getEndpointParameters(ctx);
        controller.getEndpointDatabaseConnections(ctx);
        controller.getEndpointConfigurationSummary(ctx);
        controller.getQueryConfigurationSchema(ctx);
        controller.getQueryParameters(ctx);
        controller.getQueryDatabaseConnections(ctx);
        controller.getQueryConfigurationSummary(ctx);
        controller.getDatabaseConfigurationSchema(ctx);
        controller.getDatabaseParameters(ctx);
        controller.getDatabaseConnections(ctx);
        controller.getDatabaseConfigurationSummary(ctx);

        verify(ctx).json(endpointSchema);
        verify(ctx).json(endpointParameters);
        verify(ctx).json(endpointConnections);
        verify(ctx).json(endpointSummary);
        verify(ctx).json(querySchema);
        verify(ctx).json(queryParameters);
        verify(ctx).json(queryConnections);
        verify(ctx).json(querySummary);
        verify(ctx).json(databaseSchema);
        verify(ctx).json(databaseParameters);
        verify(ctx).json(databaseConnections);
        verify(ctx).json(databaseSummary);
    }
}
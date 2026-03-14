package dev.cordal.generic;

import dev.cordal.common.exception.ApiException;
import dev.cordal.generic.config.ApiEndpointConfig;
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
        when(genericApiService.executeEndpoint(eq("orders"), any(Map.class))).thenReturn(response);

        controller.handleEndpointRequest(ctx, "orders");

        verify(ctx).json(response);
        verify(genericApiService).executeEndpoint(eq("orders"), argThat((Map<String, Object> parameters) ->
            "42".equals(parameters.get("orderId"))
                && "west".equals(parameters.get("region"))
                && "open".equals(parameters.get("status"))
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
        when(genericApiService.executeEndpointAsync(eq("orders"), any(Map.class)))
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
        verify(genericApiService).executeEndpointAsync(eq("orders"), argThat((Map<String, Object> parameters) ->
            "42".equals(parameters.get("orderId"))
                && "west".equals(parameters.get("region"))
                && "true".equals(parameters.get("async"))
        ));
        verify(genericApiService, never()).executeEndpoint(any(), any(Map.class));
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
}
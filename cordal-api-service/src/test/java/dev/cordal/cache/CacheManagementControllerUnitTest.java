package dev.cordal.cache;

import dev.cordal.cache.dto.CacheInvalidationRequest;
import dev.cordal.common.cache.CacheManager;
import dev.cordal.common.cache.CacheStatistics;
import dev.cordal.common.metrics.CacheMetricsCollector;
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CacheManagementControllerUnitTest {

    private CacheManager cacheManager;
    private CacheMetricsCollector metricsCollector;
    private CacheManagementController controller;
    private Context ctx;

    @BeforeEach
    void setUp() {
        cacheManager = mock(CacheManager.class);
        metricsCollector = mock(CacheMetricsCollector.class);
        controller = new CacheManagementController(cacheManager, metricsCollector);
        ctx = mock(Context.class);
        when(ctx.status(any(Integer.class))).thenReturn(ctx);
    }

    @Test
    void statisticsEndpointsShouldReturnPayloadsAndHandleNotFound() {
        CacheStatistics stats = CacheStatistics.builder().hitCount(5).missCount(5).size(2).build();
        CacheMetricsCollector.QueryCacheMetrics queryMetrics = new CacheMetricsCollector.QueryCacheMetrics("orders-query");

        when(metricsCollector.getOverallStatistics()).thenReturn(Map.of("totalRequests", 10L, "hitRate", 0.5d));
        when(cacheManager.getAllStatistics()).thenReturn(Map.of("orders", stats));
        when(cacheManager.getCacheNames()).thenReturn(Set.of("orders", "reports"));
        when(ctx.pathParam("cacheName")).thenReturn("orders").thenReturn("missing");
        when(ctx.pathParam("queryName")).thenReturn("orders-query").thenReturn("missing-query");
        when(cacheManager.getStatistics("orders")).thenReturn(stats);
        when(cacheManager.getStatistics("missing")).thenReturn(null);
        when(metricsCollector.getAllQueryStatistics()).thenReturn(Map.of("orders-query", queryMetrics));
        when(metricsCollector.getQueryStatistics("orders-query")).thenReturn(queryMetrics);
        when(metricsCollector.getQueryStatistics("missing-query")).thenReturn(null);

        controller.getCacheStatistics(ctx);
        controller.getCacheStatisticsByName(ctx);
        controller.getCacheStatisticsByName(ctx);
        controller.getQueryMetrics(ctx);
        controller.getQueryMetricsByName(ctx);
        controller.getQueryMetricsByName(ctx);
        controller.getCacheNames(ctx);

        verify(ctx, times(2)).status(404);
    }

    @Test
    void cacheMutationEndpointsShouldHandleSuccessAndNotFound() {
        CacheInvalidationRequest specificRequest = new CacheInvalidationRequest();
        specificRequest.setPatterns(List.of("orders:*"));
        specificRequest.setCacheName("orders");

        CacheInvalidationRequest allCachesRequest = new CacheInvalidationRequest();
        allCachesRequest.setPatterns(List.of("reports:*"));

        when(ctx.pathParam("cacheName")).thenReturn("missing").thenReturn("orders");
        when(cacheManager.cacheExists("missing")).thenReturn(false);
        when(cacheManager.cacheExists("orders")).thenReturn(true);
        when(cacheManager.getCacheNames()).thenReturn(Set.of("orders", "reports"));
        when(ctx.bodyAsClass(CacheInvalidationRequest.class)).thenReturn(specificRequest).thenReturn(allCachesRequest);
        when(cacheManager.invalidate("orders", "orders:*")).thenReturn(2);
        when(cacheManager.invalidate("orders", "reports:*")).thenReturn(1);
        when(cacheManager.invalidate("reports", "reports:*")).thenReturn(3);

        controller.clearCache(ctx);
        controller.clearCache(ctx);
        controller.clearAllCaches(ctx);
        controller.invalidateCacheByPattern(ctx);
        controller.invalidateCacheByPattern(ctx);
        controller.resetCacheMetrics(ctx);

        verify(ctx).status(404);
        verify(cacheManager).clear("orders");
        verify(cacheManager).clearAll();
        verify(metricsCollector).resetMetrics();
    }

    @Test
    void invalidateCacheShouldRejectMissingPatterns() {
        CacheInvalidationRequest request = new CacheInvalidationRequest();
        when(ctx.bodyAsClass(CacheInvalidationRequest.class)).thenReturn(request);

        controller.invalidateCacheByPattern(ctx);

        verify(ctx).status(400);
    }

    @Test
    void healthEndpointShouldReportHealthyAndLowHitRateStates() {
        CacheStatistics healthyStats = CacheStatistics.builder().hitCount(8).missCount(2).build();
        CacheStatistics unhealthyStats = CacheStatistics.builder().hitCount(5).missCount(150).build();

        when(metricsCollector.getOverallStatistics()).thenReturn(Map.of("totalRequests", 10L, "hitRate", 0.8d))
            .thenReturn(Map.of("totalRequests", 155L, "hitRate", 0.03d));
        when(cacheManager.getAllStatistics()).thenReturn(Map.of("orders", healthyStats))
            .thenReturn(Map.of("orders", unhealthyStats));

        controller.getCacheHealth(ctx);
        controller.getCacheHealth(ctx);

        verify(ctx).json(argThat(payload -> payload instanceof Map<?, ?> map
            && "healthy".equals(map.get("status"))));
        verify(ctx).json(argThat(payload -> payload instanceof Map<?, ?> map
            && "low_hit_rate".equals(map.get("status"))));
    }

    @Test
    void endpointsShouldReturnInternalServerErrorOnFailures() {
        when(metricsCollector.getOverallStatistics()).thenThrow(new RuntimeException("boom-stats"));
        when(ctx.pathParam("cacheName")).thenReturn("orders");
        when(cacheManager.getStatistics("orders")).thenThrow(new RuntimeException("boom-cache"));
        when(metricsCollector.getAllQueryStatistics()).thenThrow(new RuntimeException("boom-query-list"));
        when(ctx.pathParam("queryName")).thenReturn("orders-query");
        when(metricsCollector.getQueryStatistics("orders-query")).thenThrow(new RuntimeException("boom-query"));
        when(cacheManager.cacheExists("orders")).thenThrow(new RuntimeException("boom-clear"));
        when(ctx.bodyAsClass(CacheInvalidationRequest.class)).thenThrow(new RuntimeException("boom-invalidate"));
        when(cacheManager.getCacheNames()).thenThrow(new RuntimeException("boom-names"));
        doThrow(new RuntimeException("boom-clear-all")).when(cacheManager).clearAll();
        doThrow(new RuntimeException("boom-reset")).when(metricsCollector).resetMetrics();

        controller.getCacheStatistics(ctx);
        controller.getCacheStatisticsByName(ctx);
        controller.getQueryMetrics(ctx);
        controller.getQueryMetricsByName(ctx);
        controller.clearCache(ctx);
        controller.clearAllCaches(ctx);
        controller.invalidateCacheByPattern(ctx);
        controller.getCacheNames(ctx);
        controller.resetCacheMetrics(ctx);
        controller.getCacheHealth(ctx);

        verify(ctx, times(10)).status(500);
    }
}
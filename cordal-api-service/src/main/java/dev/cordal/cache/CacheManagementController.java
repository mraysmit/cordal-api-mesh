package dev.cordal.cache;

import dev.cordal.common.cache.CacheManager;
import dev.cordal.common.cache.CacheStatistics;
import dev.cordal.common.metrics.CacheMetricsCollector;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;

/**
 * REST controller for cache management operations
 */
@Singleton
public class CacheManagementController {
    private static final Logger logger = LoggerFactory.getLogger(CacheManagementController.class);
    
    private final CacheManager cacheManager;
    private final CacheMetricsCollector cacheMetricsCollector;

    @Inject
    public CacheManagementController(CacheManager cacheManager, CacheMetricsCollector cacheMetricsCollector) {
        this.cacheManager = cacheManager;
        this.cacheMetricsCollector = cacheMetricsCollector;
    }

    /**
     * Get overall cache statistics
     * GET /api/cache/statistics
     */
    public void getCacheStatistics(Context ctx) {
        try {
            Map<String, Object> overallStats = cacheMetricsCollector.getOverallStatistics();
            Map<String, CacheStatistics> cacheStats = cacheManager.getAllStatistics();
            
            Map<String, Object> response = Map.of(
                "overall", overallStats,
                "caches", cacheStats,
                "cacheNames", cacheManager.getCacheNames()
            );
            
            ctx.json(response);
            logger.debug("Retrieved cache statistics");
            
        } catch (Exception e) {
            logger.error("Error retrieving cache statistics", e);
            ctx.status(500).json(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Get statistics for a specific cache
     * GET /api/cache/statistics/{cacheName}
     */
    public void getCacheStatisticsByName(Context ctx) {
        try {
            String cacheName = ctx.pathParam("cacheName");
            CacheStatistics stats = cacheManager.getStatistics(cacheName);
            
            if (stats != null) {
                ctx.json(stats);
                logger.debug("Retrieved statistics for cache: {}", cacheName);
            } else {
                ctx.status(404).json(Map.of("error", "Cache not found: " + cacheName));
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving cache statistics", e);
            ctx.status(500).json(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Get query-specific cache metrics
     * GET /api/cache/query-metrics
     */
    public void getQueryMetrics(Context ctx) {
        try {
            Map<String, CacheMetricsCollector.QueryCacheMetrics> queryMetrics = 
                cacheMetricsCollector.getAllQueryStatistics();
            
            ctx.json(queryMetrics);
            logger.debug("Retrieved query cache metrics for {} queries", queryMetrics.size());
            
        } catch (Exception e) {
            logger.error("Error retrieving query cache metrics", e);
            ctx.status(500).json(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Get metrics for a specific query
     * GET /api/cache/query-metrics/{queryName}
     */
    public void getQueryMetricsByName(Context ctx) {
        try {
            String queryName = ctx.pathParam("queryName");
            CacheMetricsCollector.QueryCacheMetrics metrics = 
                cacheMetricsCollector.getQueryStatistics(queryName);
            
            if (metrics != null) {
                ctx.json(metrics);
                logger.debug("Retrieved metrics for query: {}", queryName);
            } else {
                ctx.status(404).json(Map.of("error", "Query metrics not found: " + queryName));
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving query metrics", e);
            ctx.status(500).json(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Clear a specific cache
     * DELETE /api/cache/{cacheName}
     */
    public void clearCache(Context ctx) {
        try {
            String cacheName = ctx.pathParam("cacheName");
            
            if (!cacheManager.cacheExists(cacheName)) {
                ctx.status(404).json(Map.of("error", "Cache not found: " + cacheName));
                return;
            }
            
            cacheManager.clear(cacheName);
            ctx.json(Map.of("message", "Cache cleared successfully", "cacheName", cacheName));
            logger.info("Cleared cache: {}", cacheName);
            
        } catch (Exception e) {
            logger.error("Error clearing cache", e);
            ctx.status(500).json(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Clear all caches
     * DELETE /api/cache
     */
    public void clearAllCaches(Context ctx) {
        try {
            cacheManager.clearAll();
            ctx.json(Map.of("message", "All caches cleared successfully"));
            logger.info("Cleared all caches");
            
        } catch (Exception e) {
            logger.error("Error clearing all caches", e);
            ctx.status(500).json(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Invalidate cache entries by pattern
     * POST /api/cache/invalidate
     * Body: { "patterns": ["pattern1", "pattern2"], "cacheName": "optional" }
     */
    public void invalidateCacheByPattern(Context ctx) {
        try {
            Map<String, Object> request = ctx.bodyAsClass(Map.class);
            @SuppressWarnings("unchecked")
            List<String> patterns = (List<String>) request.get("patterns");
            String cacheName = (String) request.get("cacheName");
            
            if (patterns == null || patterns.isEmpty()) {
                ctx.status(400).json(Map.of("error", "Patterns are required"));
                return;
            }
            
            int totalInvalidated = 0;
            
            if (cacheName != null) {
                // Invalidate from specific cache
                if (!cacheManager.cacheExists(cacheName)) {
                    ctx.status(404).json(Map.of("error", "Cache not found: " + cacheName));
                    return;
                }
                totalInvalidated = cacheManager.invalidate(cacheName, patterns.toArray(new String[0]));
            } else {
                // Invalidate from all caches
                for (String cache : cacheManager.getCacheNames()) {
                    totalInvalidated += cacheManager.invalidate(cache, patterns.toArray(new String[0]));
                }
            }
            
            ctx.json(Map.of(
                "message", "Cache invalidation completed",
                "patterns", patterns,
                "cacheName", cacheName != null ? cacheName : "all",
                "entriesInvalidated", totalInvalidated
            ));
            
            logger.info("Invalidated {} cache entries with patterns: {} from cache: {}", 
                       totalInvalidated, patterns, cacheName != null ? cacheName : "all");
            
        } catch (Exception e) {
            logger.error("Error invalidating cache", e);
            ctx.status(500).json(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Get list of all cache names
     * GET /api/cache/names
     */
    public void getCacheNames(Context ctx) {
        try {
            java.util.Set<String> cacheNames = cacheManager.getCacheNames();
            ctx.json(Map.of("cacheNames", cacheNames));
            logger.debug("Retrieved cache names: {}", cacheNames);
            
        } catch (Exception e) {
            logger.error("Error retrieving cache names", e);
            ctx.status(500).json(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Reset cache metrics
     * POST /api/cache/metrics/reset
     */
    public void resetCacheMetrics(Context ctx) {
        try {
            cacheMetricsCollector.resetMetrics();
            ctx.json(Map.of("message", "Cache metrics reset successfully"));
            logger.info("Cache metrics reset");
            
        } catch (Exception e) {
            logger.error("Error resetting cache metrics", e);
            ctx.status(500).json(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Get cache health status
     * GET /api/cache/health
     */
    public void getCacheHealth(Context ctx) {
        try {
            Map<String, Object> overallStats = cacheMetricsCollector.getOverallStatistics();
            Map<String, CacheStatistics> cacheStats = cacheManager.getAllStatistics();
            
            boolean healthy = true;
            String status = "healthy";
            
            // Simple health checks
            for (CacheStatistics stats : cacheStats.values()) {
                if (stats.getHitRate() < 0.1 && stats.getTotalRequests() > 100) {
                    healthy = false;
                    status = "low_hit_rate";
                    break;
                }
            }
            
            Map<String, Object> health = Map.of(
                "status", status,
                "healthy", healthy,
                "totalCaches", cacheStats.size(),
                "totalRequests", overallStats.get("totalRequests"),
                "overallHitRate", overallStats.get("hitRate"),
                "timestamp", java.time.Instant.now()
            );
            
            ctx.json(health);
            logger.debug("Retrieved cache health status: {}", status);
            
        } catch (Exception e) {
            logger.error("Error retrieving cache health", e);
            ctx.status(500).json(Map.of("error", "Internal server error"));
        }
    }
}

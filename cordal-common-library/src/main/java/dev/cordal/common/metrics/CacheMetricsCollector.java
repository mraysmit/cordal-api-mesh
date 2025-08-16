package dev.cordal.common.metrics;

import dev.cordal.common.cache.CacheManager;
import dev.cordal.common.cache.CacheStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Collects and tracks cache-related metrics
 */
@Singleton
public class CacheMetricsCollector {
    private static final Logger logger = LoggerFactory.getLogger(CacheMetricsCollector.class);
    
    private final CacheManager cacheManager;
    
    // Metrics tracking
    private final AtomicLong totalCacheRequests = new AtomicLong(0);
    private final AtomicLong totalCacheHits = new AtomicLong(0);
    private final AtomicLong totalCacheMisses = new AtomicLong(0);
    private final AtomicLong totalCacheTimeMs = new AtomicLong(0);
    private final AtomicLong totalDatabaseTimeMs = new AtomicLong(0);
    
    // Per-query metrics
    private final Map<String, QueryCacheMetrics> queryMetrics = new ConcurrentHashMap<>();
    
    @Inject
    public CacheMetricsCollector(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        logger.info("CacheMetricsCollector initialized");
    }

    /**
     * Record a cache hit
     * 
     * @param queryName the name of the query
     * @param cacheName the name of the cache
     * @param cacheKey the cache key
     * @param responseTimeMs the response time from cache
     */
    public void recordCacheHit(String queryName, String cacheName, String cacheKey, long responseTimeMs) {
        totalCacheRequests.incrementAndGet();
        totalCacheHits.incrementAndGet();
        totalCacheTimeMs.addAndGet(responseTimeMs);
        
        getOrCreateQueryMetrics(queryName).recordHit(responseTimeMs);
        
        logger.debug("Cache hit recorded: query={}, cache={}, responseTime={}ms", 
                    queryName, cacheName, responseTimeMs);
    }

    /**
     * Record a cache miss
     * 
     * @param queryName the name of the query
     * @param cacheName the name of the cache
     * @param cacheKey the cache key
     * @param databaseResponseTimeMs the response time from database
     */
    public void recordCacheMiss(String queryName, String cacheName, String cacheKey, long databaseResponseTimeMs) {
        totalCacheRequests.incrementAndGet();
        totalCacheMisses.incrementAndGet();
        totalDatabaseTimeMs.addAndGet(databaseResponseTimeMs);
        
        getOrCreateQueryMetrics(queryName).recordMiss(databaseResponseTimeMs);
        
        logger.debug("Cache miss recorded: query={}, cache={}, databaseResponseTime={}ms", 
                    queryName, cacheName, databaseResponseTimeMs);
    }

    /**
     * Create cache performance metrics for a specific query execution
     * 
     * @param queryName the name of the query
     * @param cacheHit whether it was a cache hit
     * @param cacheName the name of the cache
     * @param cacheKey the cache key
     * @param responseTimeMs the response time
     * @return cache performance metrics
     */
    public CachePerformanceMetrics createCacheMetrics(String queryName, boolean cacheHit, 
                                                     String cacheName, String cacheKey, 
                                                     long responseTimeMs) {
        CacheStatistics cacheStats = cacheManager.getStatistics(cacheName);
        
        CachePerformanceMetrics metrics = new CachePerformanceMetrics(queryName, cacheHit, cacheName);
        metrics.setCacheKey(cacheKey);
        metrics.setAverageResponseTimeMs((double) responseTimeMs);
        metrics.setTotalTimeMs(responseTimeMs);
        metrics.setTotalRequests(1);
        
        if (cacheHit) {
            metrics.setCacheResponseTimeMs(responseTimeMs);
        } else {
            metrics.setDatabaseResponseTimeMs(responseTimeMs);
        }
        
        if (cacheStats != null) {
            metrics.setCacheHitCount(cacheStats.getHitCount());
            metrics.setCacheMissCount(cacheStats.getMissCount());
            metrics.setCacheEvictionCount(cacheStats.getEvictionCount());
            metrics.setCacheSize(cacheStats.getSize());
            metrics.setCacheHitRate(cacheStats.getHitRate());
            metrics.setCacheMissRate(cacheStats.getMissRate());
        }
        
        return metrics;
    }

    /**
     * Get overall cache statistics
     * 
     * @return map of cache statistics
     */
    public Map<String, Object> getOverallStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        long totalRequests = totalCacheRequests.get();
        long totalHits = totalCacheHits.get();
        long totalMisses = totalCacheMisses.get();
        
        stats.put("totalRequests", totalRequests);
        stats.put("totalHits", totalHits);
        stats.put("totalMisses", totalMisses);
        stats.put("hitRate", totalRequests > 0 ? (double) totalHits / totalRequests : 0.0);
        stats.put("missRate", totalRequests > 0 ? (double) totalMisses / totalRequests : 0.0);
        stats.put("averageCacheResponseTimeMs", totalHits > 0 ? (double) totalCacheTimeMs.get() / totalHits : 0.0);
        stats.put("averageDatabaseResponseTimeMs", totalMisses > 0 ? (double) totalDatabaseTimeMs.get() / totalMisses : 0.0);
        
        // Add per-cache statistics
        Map<String, CacheStatistics> cacheStats = cacheManager.getAllStatistics();
        stats.put("cacheStatistics", cacheStats);
        
        return stats;
    }

    /**
     * Get statistics for a specific query
     * 
     * @param queryName the name of the query
     * @return query cache metrics, or null if not found
     */
    public QueryCacheMetrics getQueryStatistics(String queryName) {
        return queryMetrics.get(queryName);
    }

    /**
     * Get statistics for all queries
     * 
     * @return map of query name to metrics
     */
    public Map<String, QueryCacheMetrics> getAllQueryStatistics() {
        return Map.copyOf(queryMetrics);
    }

    /**
     * Reset all metrics
     */
    public void resetMetrics() {
        totalCacheRequests.set(0);
        totalCacheHits.set(0);
        totalCacheMisses.set(0);
        totalCacheTimeMs.set(0);
        totalDatabaseTimeMs.set(0);
        queryMetrics.clear();
        
        logger.info("Cache metrics reset");
    }

    /**
     * Get or create query metrics for a specific query
     */
    private QueryCacheMetrics getOrCreateQueryMetrics(String queryName) {
        return queryMetrics.computeIfAbsent(queryName, k -> new QueryCacheMetrics(queryName));
    }

    /**
     * Metrics for a specific query
     */
    public static class QueryCacheMetrics {
        private final String queryName;
        private final AtomicLong hits = new AtomicLong(0);
        private final AtomicLong misses = new AtomicLong(0);
        private final AtomicLong totalCacheTimeMs = new AtomicLong(0);
        private final AtomicLong totalDatabaseTimeMs = new AtomicLong(0);
        private final LocalDateTime firstAccess;
        private volatile LocalDateTime lastAccess;

        public QueryCacheMetrics(String queryName) {
            this.queryName = queryName;
            this.firstAccess = LocalDateTime.now();
            this.lastAccess = this.firstAccess;
        }

        public void recordHit(long responseTimeMs) {
            hits.incrementAndGet();
            totalCacheTimeMs.addAndGet(responseTimeMs);
            lastAccess = LocalDateTime.now();
        }

        public void recordMiss(long responseTimeMs) {
            misses.incrementAndGet();
            totalDatabaseTimeMs.addAndGet(responseTimeMs);
            lastAccess = LocalDateTime.now();
        }

        // Getters
        public String getQueryName() { return queryName; }
        public long getHits() { return hits.get(); }
        public long getMisses() { return misses.get(); }
        public long getTotalRequests() { return hits.get() + misses.get(); }
        public double getHitRate() { 
            long total = getTotalRequests();
            return total > 0 ? (double) hits.get() / total : 0.0; 
        }
        public double getAverageCacheResponseTimeMs() {
            long hitCount = hits.get();
            return hitCount > 0 ? (double) totalCacheTimeMs.get() / hitCount : 0.0;
        }
        public double getAverageDatabaseResponseTimeMs() {
            long missCount = misses.get();
            return missCount > 0 ? (double) totalDatabaseTimeMs.get() / missCount : 0.0;
        }
        public LocalDateTime getFirstAccess() { return firstAccess; }
        public LocalDateTime getLastAccess() { return lastAccess; }

        @Override
        public String toString() {
            return "QueryCacheMetrics{" +
                   "queryName='" + queryName + '\'' +
                   ", hits=" + hits.get() +
                   ", misses=" + misses.get() +
                   ", hitRate=" + String.format("%.2f%%", getHitRate() * 100) +
                   ", avgCacheTime=" + String.format("%.2fms", getAverageCacheResponseTimeMs()) +
                   ", avgDbTime=" + String.format("%.2fms", getAverageDatabaseResponseTimeMs()) +
                   '}';
        }
    }
}

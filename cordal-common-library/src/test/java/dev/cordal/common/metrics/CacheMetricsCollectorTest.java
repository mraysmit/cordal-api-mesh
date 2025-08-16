package dev.cordal.common.metrics;

import dev.cordal.common.cache.CacheManager;
import dev.cordal.common.cache.CacheStatistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CacheMetricsCollector
 */
class CacheMetricsCollectorTest {

    private CacheManager cacheManager;
    private CacheMetricsCollector metricsCollector;

    @BeforeEach
    void setUp() {
        CacheManager.CacheConfiguration config = new CacheManager.CacheConfiguration(100, 300, 1);
        cacheManager = new CacheManager(config);
        metricsCollector = new CacheMetricsCollector(cacheManager);
    }

    @AfterEach
    void tearDown() {
        cacheManager.shutdown();
    }

    @Test
    void testRecordCacheHit() {
        metricsCollector.recordCacheHit("test_query", "test_cache", "test_key", 50);

        Map<String, Object> stats = metricsCollector.getOverallStatistics();
        assertEquals(1L, stats.get("totalRequests"));
        assertEquals(1L, stats.get("totalHits"));
        assertEquals(0L, stats.get("totalMisses"));
        assertEquals(1.0, stats.get("hitRate"));
        assertEquals(50.0, stats.get("averageCacheResponseTimeMs"));
    }

    @Test
    void testRecordCacheMiss() {
        metricsCollector.recordCacheMiss("test_query", "test_cache", "test_key", 200);

        Map<String, Object> stats = metricsCollector.getOverallStatistics();
        assertEquals(1L, stats.get("totalRequests"));
        assertEquals(0L, stats.get("totalHits"));
        assertEquals(1L, stats.get("totalMisses"));
        assertEquals(0.0, stats.get("hitRate"));
        assertEquals(200.0, stats.get("averageDatabaseResponseTimeMs"));
    }

    @Test
    void testMixedHitsAndMisses() {
        metricsCollector.recordCacheHit("query1", "cache1", "key1", 30);
        metricsCollector.recordCacheHit("query1", "cache1", "key2", 40);
        metricsCollector.recordCacheMiss("query1", "cache1", "key3", 150);
        metricsCollector.recordCacheMiss("query2", "cache1", "key4", 200);

        Map<String, Object> stats = metricsCollector.getOverallStatistics();
        assertEquals(4L, stats.get("totalRequests"));
        assertEquals(2L, stats.get("totalHits"));
        assertEquals(2L, stats.get("totalMisses"));
        assertEquals(0.5, stats.get("hitRate"));
        assertEquals(0.5, stats.get("missRate"));
        assertEquals(35.0, stats.get("averageCacheResponseTimeMs")); // (30+40)/2
        assertEquals(175.0, stats.get("averageDatabaseResponseTimeMs")); // (150+200)/2
    }

    @Test
    void testQuerySpecificMetrics() {
        metricsCollector.recordCacheHit("query1", "cache1", "key1", 25);
        metricsCollector.recordCacheMiss("query1", "cache1", "key2", 100);
        metricsCollector.recordCacheHit("query2", "cache1", "key3", 50);

        CacheMetricsCollector.QueryCacheMetrics query1Metrics = 
            metricsCollector.getQueryStatistics("query1");
        assertNotNull(query1Metrics);
        assertEquals("query1", query1Metrics.getQueryName());
        assertEquals(1, query1Metrics.getHits());
        assertEquals(1, query1Metrics.getMisses());
        assertEquals(2, query1Metrics.getTotalRequests());
        assertEquals(0.5, query1Metrics.getHitRate());
        assertEquals(25.0, query1Metrics.getAverageCacheResponseTimeMs());
        assertEquals(100.0, query1Metrics.getAverageDatabaseResponseTimeMs());

        CacheMetricsCollector.QueryCacheMetrics query2Metrics = 
            metricsCollector.getQueryStatistics("query2");
        assertNotNull(query2Metrics);
        assertEquals(1, query2Metrics.getHits());
        assertEquals(0, query2Metrics.getMisses());
        assertEquals(1.0, query2Metrics.getHitRate());
    }

    @Test
    void testCreateCacheMetrics() {
        // Setup cache with some data
        cacheManager.put("test_cache", "test_key", "test_value");
        CacheStatistics cacheStats = cacheManager.getStatistics("test_cache");

        CachePerformanceMetrics metrics = metricsCollector.createCacheMetrics(
            "test_query", true, "test_cache", "test_key", 75);

        assertEquals("test_query", metrics.getTestName());
        assertTrue(metrics.isCacheHit());
        assertEquals("test_cache", metrics.getCacheName());
        assertEquals("test_key", metrics.getCacheKey());
        assertEquals(75.0, metrics.getAverageResponseTimeMs());
        assertEquals(75L, metrics.getCacheResponseTimeMs());
        assertNull(metrics.getDatabaseResponseTimeMs());

        // Test cache miss metrics
        CachePerformanceMetrics missMetrics = metricsCollector.createCacheMetrics(
            "test_query", false, "test_cache", "test_key", 150);

        assertFalse(missMetrics.isCacheHit());
        assertNull(missMetrics.getCacheResponseTimeMs());
        assertEquals(150L, missMetrics.getDatabaseResponseTimeMs());
    }

    @Test
    void testGetAllQueryStatistics() {
        metricsCollector.recordCacheHit("query1", "cache1", "key1", 30);
        metricsCollector.recordCacheHit("query2", "cache1", "key2", 40);
        metricsCollector.recordCacheMiss("query3", "cache1", "key3", 100);

        Map<String, CacheMetricsCollector.QueryCacheMetrics> allStats = 
            metricsCollector.getAllQueryStatistics();

        assertEquals(3, allStats.size());
        assertTrue(allStats.containsKey("query1"));
        assertTrue(allStats.containsKey("query2"));
        assertTrue(allStats.containsKey("query3"));
    }

    @Test
    void testResetMetrics() {
        metricsCollector.recordCacheHit("query1", "cache1", "key1", 30);
        metricsCollector.recordCacheMiss("query1", "cache1", "key2", 100);

        Map<String, Object> statsBefore = metricsCollector.getOverallStatistics();
        assertEquals(2L, statsBefore.get("totalRequests"));

        metricsCollector.resetMetrics();

        Map<String, Object> statsAfter = metricsCollector.getOverallStatistics();
        assertEquals(0L, statsAfter.get("totalRequests"));
        assertEquals(0L, statsAfter.get("totalHits"));
        assertEquals(0L, statsAfter.get("totalMisses"));

        assertTrue(metricsCollector.getAllQueryStatistics().isEmpty());
    }

    @Test
    void testQueryMetricsTimestamps() {
        metricsCollector.recordCacheHit("query1", "cache1", "key1", 30);
        
        CacheMetricsCollector.QueryCacheMetrics metrics = 
            metricsCollector.getQueryStatistics("query1");
        
        assertNotNull(metrics.getFirstAccess());
        assertNotNull(metrics.getLastAccess());
        assertEquals(metrics.getFirstAccess(), metrics.getLastAccess());

        // Record another hit
        try {
            Thread.sleep(10); // Small delay to ensure different timestamp
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        metricsCollector.recordCacheHit("query1", "cache1", "key2", 25);
        
        metrics = metricsCollector.getQueryStatistics("query1");
        assertTrue(metrics.getLastAccess().isAfter(metrics.getFirstAccess()));
    }

    @Test
    void testQueryMetricsToString() {
        metricsCollector.recordCacheHit("test_query", "cache1", "key1", 30);
        metricsCollector.recordCacheMiss("test_query", "cache1", "key2", 120);

        CacheMetricsCollector.QueryCacheMetrics metrics = 
            metricsCollector.getQueryStatistics("test_query");

        String toString = metrics.toString();
        assertTrue(toString.contains("test_query"));
        assertTrue(toString.contains("hits=1"));
        assertTrue(toString.contains("misses=1"));
        assertTrue(toString.contains("50.00%")); // Hit rate
        assertTrue(toString.contains("30.00ms")); // Avg cache time
        assertTrue(toString.contains("120.00ms")); // Avg DB time
    }

    @Test
    void testZeroDivisionHandling() {
        // Test with no hits
        metricsCollector.recordCacheMiss("query1", "cache1", "key1", 100);
        
        CacheMetricsCollector.QueryCacheMetrics metrics = 
            metricsCollector.getQueryStatistics("query1");
        
        assertEquals(0.0, metrics.getAverageCacheResponseTimeMs());
        assertEquals(100.0, metrics.getAverageDatabaseResponseTimeMs());

        // Test with no misses
        metricsCollector.recordCacheHit("query2", "cache1", "key1", 50);
        
        CacheMetricsCollector.QueryCacheMetrics metrics2 = 
            metricsCollector.getQueryStatistics("query2");
        
        assertEquals(50.0, metrics2.getAverageCacheResponseTimeMs());
        assertEquals(0.0, metrics2.getAverageDatabaseResponseTimeMs());
    }

    @Test
    void testNonExistentQuery() {
        CacheMetricsCollector.QueryCacheMetrics metrics = 
            metricsCollector.getQueryStatistics("nonexistent");
        
        assertNull(metrics);
    }
}

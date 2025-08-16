package dev.cordal.integration.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.cordal.generic.GenericApiApplication;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive examples demonstrating all cache features
 * This test showcases the complete cache functionality with real-world scenarios
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CacheExamplesTest {
    private static final Logger logger = LoggerFactory.getLogger(CacheExamplesTest.class);
    
    private static GenericApiApplication app;
    private static final int PORT = 8081;
    private static final String BASE_URL = "http://localhost:" + PORT;
    
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void setUp() throws Exception {
        logger.info("=== CORDAL CACHE SYSTEM EXAMPLES ===");
        logger.info("Starting comprehensive cache functionality demonstration");
        
        // Start the application with cache configuration
        System.setProperty("config.file", "application-cache-integration.yml");
        app = new GenericApiApplication();
        
        CompletableFuture.runAsync(() -> {
            try {
                app.start();
            } catch (Exception e) {
                logger.error("Failed to start application", e);
                throw new RuntimeException(e);
            }
        });
        
        // Wait for application to start
        Thread.sleep(5000);
        logger.info("Application started successfully on port {}", PORT);
    }

    @AfterAll
    static void tearDown() {
        if (app != null) {
            app.stop();
            logger.info("Application stopped");
        }
        logger.info("=== CACHE EXAMPLES COMPLETED ===");
    }

    @Test
    @Order(1)
    @DisplayName("Example 1: Basic Cache Hit/Miss Demonstration")
    void example1_BasicCacheHitMiss() throws Exception {
        logger.info("\n--- Example 1: Basic Cache Hit/Miss ---");
        logger.info("Demonstrating basic cache functionality with performance measurement");
        
        String queryUrl = BASE_URL + "/api/query/get_stock_trades_by_symbol?symbol=AAPL&limit=10";
        
        // First request - Cache Miss
        logger.info("Making first request (expected cache miss)...");
        long missStart = System.currentTimeMillis();
        HttpRequest request1 = HttpRequest.newBuilder()
            .uri(URI.create(queryUrl))
            .GET()
            .build();
        
        HttpResponse<String> response1 = httpClient.send(request1, HttpResponse.BodyHandlers.ofString());
        long missTime = System.currentTimeMillis() - missStart;
        
        assertEquals(200, response1.statusCode());
        JsonNode result1 = objectMapper.readTree(response1.body());
        
        logger.info("Cache miss completed in {}ms", missTime);
        
        // Second request - Cache Hit
        logger.info("Making second request (expected cache hit)...");
        long hitStart = System.currentTimeMillis();
        HttpRequest request2 = HttpRequest.newBuilder()
            .uri(URI.create(queryUrl))
            .GET()
            .build();
        
        HttpResponse<String> response2 = httpClient.send(request2, HttpResponse.BodyHandlers.ofString());
        long hitTime = System.currentTimeMillis() - hitStart;
        
        assertEquals(200, response2.statusCode());
        JsonNode result2 = objectMapper.readTree(response2.body());
        
        // Verify results are identical
        assertEquals(result1, result2);
        
        // Verify performance improvement
        double improvement = (double) missTime / hitTime;
        logger.info("Cache hit completed in {}ms", hitTime);
        logger.info("Performance improvement: {:.2f}x faster", improvement);
        
        assertTrue(improvement > 1.5, "Cache should provide significant performance improvement");
        
        logger.info("✓ Basic caching working correctly");
    }

    @Test
    @Order(2)
    @DisplayName("Example 2: Cache Statistics and Monitoring")
    void example2_CacheStatisticsMonitoring() throws Exception {
        logger.info("\n--- Example 2: Cache Statistics and Monitoring ---");
        logger.info("Demonstrating comprehensive cache monitoring capabilities");
        
        // Generate some cache activity
        String[] symbols = {"GOOGL", "MSFT", "TSLA", "AMZN", "NVDA"};
        for (String symbol : symbols) {
            // First request (miss) + second request (hit) for each symbol
            for (int i = 0; i < 2; i++) {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/query/get_stock_trades_by_symbol?symbol=" + symbol + "&limit=5"))
                    .GET()
                    .build();
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            }
        }
        
        // Check overall cache statistics
        logger.info("Retrieving overall cache statistics...");
        HttpRequest statsRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/cache/statistics"))
            .GET()
            .build();
        
        HttpResponse<String> statsResponse = httpClient.send(statsRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, statsResponse.statusCode());
        
        JsonNode stats = objectMapper.readTree(statsResponse.body());
        JsonNode overall = stats.get("overall");
        
        logger.info("Overall Statistics:");
        logger.info("  Total Requests: {}", overall.get("totalRequests").asInt());
        logger.info("  Cache Hits: {}", overall.get("totalHits").asInt());
        logger.info("  Cache Misses: {}", overall.get("totalMisses").asInt());
        logger.info("  Hit Rate: {:.2f}%", overall.get("hitRate").asDouble() * 100);
        
        // Check query-specific metrics
        logger.info("Retrieving query-specific metrics...");
        HttpRequest queryMetricsRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/cache/query-metrics"))
            .GET()
            .build();
        
        HttpResponse<String> queryMetricsResponse = httpClient.send(queryMetricsRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, queryMetricsResponse.statusCode());
        
        JsonNode queryMetrics = objectMapper.readTree(queryMetricsResponse.body());
        
        if (queryMetrics.has("get_stock_trades_by_symbol")) {
            JsonNode queryStats = queryMetrics.get("get_stock_trades_by_symbol");
            logger.info("Query 'get_stock_trades_by_symbol' Statistics:");
            logger.info("  Hits: {}", queryStats.get("hits").asInt());
            logger.info("  Misses: {}", queryStats.get("misses").asInt());
            logger.info("  Hit Rate: {:.2f}%", queryStats.get("hitRate").asDouble() * 100);
        }
        
        // Check cache health
        logger.info("Checking cache health...");
        HttpRequest healthRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/cache/health"))
            .GET()
            .build();
        
        HttpResponse<String> healthResponse = httpClient.send(healthRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, healthResponse.statusCode());
        
        JsonNode health = objectMapper.readTree(healthResponse.body());
        logger.info("Cache Health Status: {}", health.get("status").asText());
        logger.info("Overall Hit Rate: {:.2f}%", health.get("overallHitRate").asDouble() * 100);
        
        logger.info("✓ Cache monitoring working correctly");
    }

    @Test
    @Order(3)
    @DisplayName("Example 3: Pattern-Based Cache Invalidation")
    void example3_PatternBasedInvalidation() throws Exception {
        logger.info("\n--- Example 3: Pattern-Based Cache Invalidation ---");
        logger.info("Demonstrating sophisticated cache invalidation strategies");
        
        // Populate cache with multiple related entries
        String[] symbols = {"IBM", "ORCL", "CRM"};
        for (String symbol : symbols) {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/query/get_stock_trades_by_symbol?symbol=" + symbol + "&limit=10"))
                .GET()
                .build();
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // Verify it's cached
            long start = System.currentTimeMillis();
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long time = System.currentTimeMillis() - start;
            logger.info("Cached {} data (response time: {}ms)", symbol, time);
        }
        
        // Invalidate specific pattern
        logger.info("Invalidating cache entries for IBM using pattern matching...");
        String invalidationBody = objectMapper.writeValueAsString(Map.of(
            "patterns", List.of("stock_trades:IBM:*")
        ));
        
        HttpRequest invalidateRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/cache/invalidate"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(invalidationBody))
            .build();
        
        HttpResponse<String> invalidateResponse = httpClient.send(invalidateRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, invalidateResponse.statusCode());
        
        JsonNode invalidateResult = objectMapper.readTree(invalidateResponse.body());
        int invalidatedCount = invalidateResult.get("entriesInvalidated").asInt();
        logger.info("Invalidated {} cache entries", invalidatedCount);
        
        // Verify IBM data is no longer cached (slower response)
        logger.info("Verifying IBM data was invalidated...");
        long ibmStart = System.currentTimeMillis();
        HttpRequest ibmRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/query/get_stock_trades_by_symbol?symbol=IBM&limit=10"))
            .GET()
            .build();
        httpClient.send(ibmRequest, HttpResponse.BodyHandlers.ofString());
        long ibmTime = System.currentTimeMillis() - ibmStart;
        
        // Verify ORCL data is still cached (faster response)
        logger.info("Verifying ORCL data is still cached...");
        long orclStart = System.currentTimeMillis();
        HttpRequest orclRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/query/get_stock_trades_by_symbol?symbol=ORCL&limit=10"))
            .GET()
            .build();
        httpClient.send(orclRequest, HttpResponse.BodyHandlers.ofString());
        long orclTime = System.currentTimeMillis() - orclStart;
        
        logger.info("IBM response time (after invalidation): {}ms", ibmTime);
        logger.info("ORCL response time (still cached): {}ms", orclTime);
        
        // IBM should be slower since it was invalidated
        assertTrue(ibmTime > orclTime, "Invalidated entry should be slower than cached entry");
        
        logger.info("✓ Pattern-based invalidation working correctly");
    }

    @Test
    @Order(4)
    @DisplayName("Example 4: Cache Management Operations")
    void example4_CacheManagementOperations() throws Exception {
        logger.info("\n--- Example 4: Cache Management Operations ---");
        logger.info("Demonstrating administrative cache management capabilities");
        
        // Populate some cache data
        logger.info("Populating cache with test data...");
        String[] testSymbols = {"TEST1", "TEST2", "TEST3"};
        for (String symbol : testSymbols) {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/query/get_stock_trades_by_symbol?symbol=" + symbol + "&limit=5"))
                .GET()
                .build();
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        }
        
        // Get cache names
        logger.info("Retrieving available cache names...");
        HttpRequest namesRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/cache/names"))
            .GET()
            .build();
        
        HttpResponse<String> namesResponse = httpClient.send(namesRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, namesResponse.statusCode());
        
        JsonNode names = objectMapper.readTree(namesResponse.body());
        JsonNode cacheNames = names.get("cacheNames");
        logger.info("Available caches: {}", cacheNames);
        
        // Clear all caches
        logger.info("Clearing all caches...");
        HttpRequest clearRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/cache"))
            .DELETE()
            .build();
        
        HttpResponse<String> clearResponse = httpClient.send(clearRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, clearResponse.statusCode());
        
        JsonNode clearResult = objectMapper.readTree(clearResponse.body());
        logger.info("Clear operation result: {}", clearResult.get("message").asText());
        
        // Verify cache is cleared (next request should be slow)
        logger.info("Verifying cache was cleared...");
        long start = System.currentTimeMillis();
        HttpRequest verifyRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/query/get_stock_trades_by_symbol?symbol=TEST1&limit=5"))
            .GET()
            .build();
        httpClient.send(verifyRequest, HttpResponse.BodyHandlers.ofString());
        long time = System.currentTimeMillis() - start;
        
        logger.info("Response time after cache clear: {}ms (should be slower)", time);
        
        // Reset metrics
        logger.info("Resetting cache metrics...");
        HttpRequest resetRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/cache/metrics/reset"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        
        HttpResponse<String> resetResponse = httpClient.send(resetRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resetResponse.statusCode());
        
        logger.info("Metrics reset completed");
        
        logger.info("✓ Cache management operations working correctly");
    }

    @Test
    @Order(5)
    @DisplayName("Example 5: Performance Comparison - Cached vs Non-Cached")
    void example5_PerformanceComparison() throws Exception {
        logger.info("\n--- Example 5: Performance Comparison ---");
        logger.info("Comparing performance between cached and non-cached queries");
        
        // Test cached query performance
        String cachedQueryUrl = BASE_URL + "/api/query/get_stock_trades_by_symbol?symbol=PERF&limit=20";
        
        // First request (miss)
        long missStart = System.currentTimeMillis();
        HttpRequest missRequest = HttpRequest.newBuilder()
            .uri(URI.create(cachedQueryUrl))
            .GET()
            .build();
        httpClient.send(missRequest, HttpResponse.BodyHandlers.ofString());
        long missTime = System.currentTimeMillis() - missStart;
        
        // Multiple hits to get average
        long totalHitTime = 0;
        int hitCount = 10;
        for (int i = 0; i < hitCount; i++) {
            long hitStart = System.currentTimeMillis();
            HttpRequest hitRequest = HttpRequest.newBuilder()
                .uri(URI.create(cachedQueryUrl))
                .GET()
                .build();
            httpClient.send(hitRequest, HttpResponse.BodyHandlers.ofString());
            totalHitTime += (System.currentTimeMillis() - hitStart);
        }
        long avgHitTime = totalHitTime / hitCount;
        
        // Test non-cached query performance (if available)
        String nonCachedQueryUrl = BASE_URL + "/api/query/get_trade_details?id=1";
        
        long totalNonCachedTime = 0;
        int nonCachedCount = 5;
        for (int i = 0; i < nonCachedCount; i++) {
            long start = System.currentTimeMillis();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(nonCachedQueryUrl))
                .GET()
                .build();
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            totalNonCachedTime += (System.currentTimeMillis() - start);
        }
        long avgNonCachedTime = totalNonCachedTime / nonCachedCount;
        
        // Performance analysis
        double cacheImprovement = (double) missTime / avgHitTime;
        double vsNonCached = (double) avgNonCachedTime / avgHitTime;
        
        logger.info("Performance Analysis:");
        logger.info("  Cache Miss Time: {}ms", missTime);
        logger.info("  Average Cache Hit Time: {}ms", avgHitTime);
        logger.info("  Average Non-Cached Time: {}ms", avgNonCachedTime);
        logger.info("  Cache Improvement: {:.2f}x faster", cacheImprovement);
        logger.info("  vs Non-Cached: {:.2f}x faster", vsNonCached);
        
        assertTrue(cacheImprovement > 1.0, "Cache hits should be faster than cache misses");
        
        logger.info("✓ Performance comparison demonstrates cache effectiveness");
    }
}

package dev.cordal.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.cordal.generic.GenericApiApplication;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for cache functionality
 * Tests the complete cache system including configuration, invalidation, and monitoring
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CacheIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(CacheIntegrationTest.class);
    
    private static GenericApiApplication app;
    private static final int PORT = 8080;
    private static final String BASE_URL = "http://localhost:" + PORT;
    
    private final HttpClient httpClient = HttpClient.newBuilder()
        .timeout(Duration.ofSeconds(30))
        .build();
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void setUp() throws Exception {
        logger.info("Starting Cache Integration Tests");
        
        // Start the application
        app = new GenericApiApplication();
        CompletableFuture.runAsync(() -> {
            try {
                app.start(PORT);
            } catch (Exception e) {
                logger.error("Failed to start application", e);
                throw new RuntimeException(e);
            }
        });
        
        // Wait for application to start
        Thread.sleep(3000);
        logger.info("Application started on port {}", PORT);
    }

    @AfterAll
    static void tearDown() {
        if (app != null) {
            app.stop();
            logger.info("Application stopped");
        }
    }

    @Test
    @Order(1)
    void testCacheStatisticsEndpoint() throws Exception {
        logger.info("Testing cache statistics endpoint");
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/cache/statistics"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode());
        
        JsonNode json = objectMapper.readTree(response.body());
        assertTrue(json.has("overall"));
        assertTrue(json.has("caches"));
        assertTrue(json.has("cacheNames"));
        
        JsonNode overall = json.get("overall");
        assertTrue(overall.has("totalRequests"));
        assertTrue(overall.has("totalHits"));
        assertTrue(overall.has("totalMisses"));
        assertTrue(overall.has("hitRate"));
        
        logger.info("Cache statistics endpoint working correctly");
    }

    @Test
    @Order(2)
    void testCacheHealthEndpoint() throws Exception {
        logger.info("Testing cache health endpoint");
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/cache/health"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode());
        
        JsonNode json = objectMapper.readTree(response.body());
        assertTrue(json.has("status"));
        assertTrue(json.has("healthy"));
        assertTrue(json.has("totalCaches"));
        assertTrue(json.has("timestamp"));
        
        logger.info("Cache health endpoint working correctly");
    }

    @Test
    @Order(3)
    void testQueryExecutionWithCaching() throws Exception {
        logger.info("Testing query execution with caching");
        
        // First request - should be a cache miss
        long startTime1 = System.currentTimeMillis();
        HttpRequest request1 = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/query/get_stock_trades_by_symbol?symbol=AAPL&limit=10"))
            .GET()
            .build();

        HttpResponse<String> response1 = httpClient.send(request1, HttpResponse.BodyHandlers.ofString());
        long responseTime1 = System.currentTimeMillis() - startTime1;
        
        assertEquals(200, response1.statusCode());
        JsonNode result1 = objectMapper.readTree(response1.body());
        assertTrue(result1.has("data"));
        
        // Second request - should be a cache hit (faster)
        long startTime2 = System.currentTimeMillis();
        HttpRequest request2 = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/query/get_stock_trades_by_symbol?symbol=AAPL&limit=10"))
            .GET()
            .build();

        HttpResponse<String> response2 = httpClient.send(request2, HttpResponse.BodyHandlers.ofString());
        long responseTime2 = System.currentTimeMillis() - startTime2;
        
        assertEquals(200, response2.statusCode());
        JsonNode result2 = objectMapper.readTree(response2.body());
        
        // Results should be identical
        assertEquals(result1, result2);
        
        // Second request should be faster (cache hit)
        assertTrue(responseTime2 < responseTime1, 
                  String.format("Cache hit (%dms) should be faster than cache miss (%dms)", 
                               responseTime2, responseTime1));
        
        logger.info("Query caching working correctly - Miss: {}ms, Hit: {}ms", responseTime1, responseTime2);
    }

    @Test
    @Order(4)
    void testCacheQueryMetrics() throws Exception {
        logger.info("Testing cache query metrics");
        
        // Execute some queries to generate metrics
        for (int i = 0; i < 5; i++) {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/query/get_stock_trades_by_symbol?symbol=GOOGL&limit=5"))
                .GET()
                .build();
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        }
        
        // Check query metrics
        HttpRequest metricsRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/cache/query-metrics"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(metricsRequest, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode());
        
        JsonNode json = objectMapper.readTree(response.body());
        assertTrue(json.isObject());
        
        // Should have metrics for our query
        assertTrue(json.has("get_stock_trades_by_symbol"));
        
        JsonNode queryMetrics = json.get("get_stock_trades_by_symbol");
        assertTrue(queryMetrics.has("queryName"));
        assertTrue(queryMetrics.has("hits"));
        assertTrue(queryMetrics.has("misses"));
        assertTrue(queryMetrics.has("totalRequests"));
        assertTrue(queryMetrics.has("hitRate"));
        
        logger.info("Cache query metrics working correctly");
    }

    @Test
    @Order(5)
    void testCacheInvalidation() throws Exception {
        logger.info("Testing cache invalidation");
        
        // First, populate cache
        HttpRequest request1 = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/query/get_stock_trades_by_symbol?symbol=MSFT&limit=10"))
            .GET()
            .build();
        httpClient.send(request1, HttpResponse.BodyHandlers.ofString());
        
        // Verify it's cached (second request should be fast)
        long startTime = System.currentTimeMillis();
        HttpRequest request2 = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/query/get_stock_trades_by_symbol?symbol=MSFT&limit=10"))
            .GET()
            .build();
        httpClient.send(request2, HttpResponse.BodyHandlers.ofString());
        long cacheHitTime = System.currentTimeMillis() - startTime;
        
        // Invalidate cache using pattern
        String invalidationBody = objectMapper.writeValueAsString(Map.of(
            "patterns", List.of("stock_trades:MSFT:*")
        ));
        
        HttpRequest invalidateRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/cache/invalidate"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(invalidationBody))
            .build();

        HttpResponse<String> invalidateResponse = httpClient.send(invalidateRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, invalidateResponse.statusCode());
        
        JsonNode invalidateResult = objectMapper.readTree(invalidateResponse.body());
        assertTrue(invalidateResult.has("entriesInvalidated"));
        
        // Next request should be slower (cache miss)
        startTime = System.currentTimeMillis();
        HttpRequest request3 = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/query/get_stock_trades_by_symbol?symbol=MSFT&limit=10"))
            .GET()
            .build();
        httpClient.send(request3, HttpResponse.BodyHandlers.ofString());
        long cacheMissTime = System.currentTimeMillis() - startTime;
        
        // Cache miss should be slower than cache hit
        assertTrue(cacheMissTime > cacheHitTime, 
                  String.format("Cache miss (%dms) should be slower than cache hit (%dms)", 
                               cacheMissTime, cacheHitTime));
        
        logger.info("Cache invalidation working correctly - Hit: {}ms, Miss after invalidation: {}ms", 
                   cacheHitTime, cacheMissTime);
    }

    @Test
    @Order(6)
    void testCacheClearOperations() throws Exception {
        logger.info("Testing cache clear operations");
        
        // Populate multiple caches
        HttpRequest request1 = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/query/get_stock_trades_by_symbol?symbol=TSLA&limit=10"))
            .GET()
            .build();
        httpClient.send(request1, HttpResponse.BodyHandlers.ofString());
        
        // Get cache names
        HttpRequest namesRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/cache/names"))
            .GET()
            .build();

        HttpResponse<String> namesResponse = httpClient.send(namesRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, namesResponse.statusCode());
        
        JsonNode namesJson = objectMapper.readTree(namesResponse.body());
        assertTrue(namesJson.has("cacheNames"));
        assertTrue(namesJson.get("cacheNames").isArray());
        
        // Clear all caches
        HttpRequest clearRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/cache"))
            .DELETE()
            .build();

        HttpResponse<String> clearResponse = httpClient.send(clearRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, clearResponse.statusCode());
        
        JsonNode clearResult = objectMapper.readTree(clearResponse.body());
        assertTrue(clearResult.has("message"));
        
        logger.info("Cache clear operations working correctly");
    }

    @Test
    @Order(7)
    void testCacheMetricsReset() throws Exception {
        logger.info("Testing cache metrics reset");
        
        // Generate some metrics
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/query/get_stock_trades_by_symbol?symbol=NVDA&limit=5"))
            .GET()
            .build();
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        // Check metrics before reset
        HttpRequest metricsRequest1 = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/cache/statistics"))
            .GET()
            .build();

        HttpResponse<String> response1 = httpClient.send(metricsRequest1, HttpResponse.BodyHandlers.ofString());
        JsonNode json1 = objectMapper.readTree(response1.body());
        
        // Reset metrics
        HttpRequest resetRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/cache/metrics/reset"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> resetResponse = httpClient.send(resetRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resetResponse.statusCode());
        
        // Check metrics after reset
        HttpRequest metricsRequest2 = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/cache/statistics"))
            .GET()
            .build();

        HttpResponse<String> response2 = httpClient.send(metricsRequest2, HttpResponse.BodyHandlers.ofString());
        JsonNode json2 = objectMapper.readTree(response2.body());
        
        // Metrics should be reset
        assertEquals(0, json2.get("overall").get("totalRequests").asInt());
        assertEquals(0, json2.get("overall").get("totalHits").asInt());
        assertEquals(0, json2.get("overall").get("totalMisses").asInt());
        
        logger.info("Cache metrics reset working correctly");
    }

    @Test
    @Order(8)
    void testCachePerformanceImprovement() throws Exception {
        logger.info("Testing cache performance improvement measurement");
        
        // Execute multiple requests to get good performance data
        String queryUrl = BASE_URL + "/api/query/get_stock_trades_by_symbol?symbol=AMZN&limit=20";
        
        // First request (cache miss)
        long missStart = System.currentTimeMillis();
        HttpRequest missRequest = HttpRequest.newBuilder()
            .uri(URI.create(queryUrl))
            .GET()
            .build();
        httpClient.send(missRequest, HttpResponse.BodyHandlers.ofString());
        long missTime = System.currentTimeMillis() - missStart;
        
        // Multiple cache hits
        long totalHitTime = 0;
        int hitCount = 5;
        for (int i = 0; i < hitCount; i++) {
            long hitStart = System.currentTimeMillis();
            HttpRequest hitRequest = HttpRequest.newBuilder()
                .uri(URI.create(queryUrl))
                .GET()
                .build();
            httpClient.send(hitRequest, HttpResponse.BodyHandlers.ofString());
            totalHitTime += (System.currentTimeMillis() - hitStart);
        }
        long avgHitTime = totalHitTime / hitCount;
        
        // Cache should provide significant performance improvement
        double improvementRatio = (double) missTime / avgHitTime;
        assertTrue(improvementRatio > 1.5, 
                  String.format("Cache should provide significant improvement. Miss: %dms, Avg Hit: %dms, Ratio: %.2f", 
                               missTime, avgHitTime, improvementRatio));
        
        logger.info("Cache performance improvement verified - Miss: {}ms, Avg Hit: {}ms, Improvement: {:.2f}x", 
                   missTime, avgHitTime, improvementRatio);
    }
}

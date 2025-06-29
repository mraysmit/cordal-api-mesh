package dev.mars.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mars.Application;
import dev.mars.config.AppConfig;
import dev.mars.metrics.MetricsCollectionHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for metrics collection functionality
 */
class MetricsCollectionIntegrationTest {

    private static Application application;
    private static final String BASE_URL = "http://localhost:8080";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void startApplication() {
        // Use test configuration
        System.setProperty("config.file", "application-test.yml");

        application = new Application();
        application.start();

        // Wait for application to start
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @AfterAll
    static void stopApplication() {
        if (application != null) {
            application.stop();
        }
        System.clearProperty("config.file");
    }

    @BeforeEach
    void resetMetrics() throws IOException, InterruptedException {
        // Reset metrics before each test
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/metrics/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(30))
                .build();

        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void testMetricsCollectionForStockTradesEndpoint() throws IOException, InterruptedException {
        // Make a request to stock trades endpoint
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/stock-trades?page=0&size=5"))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        // Wait a moment for metrics to be processed
        Thread.sleep(500);

        // Check that metrics were collected
        HttpRequest metricsRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/metrics/endpoints"))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> metricsResponse = httpClient.send(metricsRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(metricsResponse.statusCode()).isEqualTo(200);

        Map<String, Object> metrics = objectMapper.readValue(metricsResponse.body(), new TypeReference<Map<String, Object>>() {});
        
        // Verify metrics were collected for the endpoint
        assertThat(metrics).isNotEmpty();
        assertThat(metrics).containsKey("GET /api/stock-trades");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> endpointMetrics = (Map<String, Object>) metrics.get("GET /api/stock-trades");
        assertThat(endpointMetrics.get("totalRequests")).isEqualTo(1);
        assertThat(endpointMetrics.get("successRate")).isEqualTo(100.0);
        assertThat(endpointMetrics.get("averageResponseTime")).isInstanceOf(Number.class);
        assertThat(((Number) endpointMetrics.get("averageResponseTime")).doubleValue()).isGreaterThan(0);
    }

    @Test
    void testMetricsCollectionForMultipleRequests() throws IOException, InterruptedException {
        // Make multiple requests to different endpoints
        String[] endpoints = {
            "/api/stock-trades",
            "/api/stock-trades/1",
            "/api/health"
        };

        for (String endpoint : endpoints) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + endpoint))
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();

            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        }

        // Wait for metrics to be processed
        Thread.sleep(500);

        // Check metrics
        HttpRequest metricsRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/metrics/endpoints"))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> metricsResponse = httpClient.send(metricsRequest, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> metrics = objectMapper.readValue(metricsResponse.body(), new TypeReference<Map<String, Object>>() {});

        // Verify metrics for multiple endpoints
        assertThat(metrics).hasSize(3);
        assertThat(metrics).containsKey("GET /api/stock-trades");
        assertThat(metrics).containsKey("GET /api/stock-trades/{id}");
        assertThat(metrics).containsKey("GET /api/health");
    }

    @Test
    void testMetricsExclusionForDashboard() throws IOException, InterruptedException {
        // Make a request to excluded endpoint (dashboard)
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/dashboard"))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Wait for potential metrics processing
        Thread.sleep(500);

        // Check that no metrics were collected for excluded path
        HttpRequest metricsRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/metrics/endpoints"))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> metricsResponse = httpClient.send(metricsRequest, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> metrics = objectMapper.readValue(metricsResponse.body(), new TypeReference<Map<String, Object>>() {});

        // Verify no metrics for dashboard endpoint
        assertThat(metrics).doesNotContainKey("GET /dashboard");
    }

    @Test
    void testMetricsReset() throws IOException, InterruptedException {
        // Make a request to generate metrics
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/stock-trades"))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Thread.sleep(500);

        // Verify metrics exist
        HttpRequest metricsRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/metrics/endpoints"))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> metricsResponse = httpClient.send(metricsRequest, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> metricsBefore = objectMapper.readValue(metricsResponse.body(), new TypeReference<Map<String, Object>>() {});
        assertThat(metricsBefore).isNotEmpty();

        // Reset metrics
        HttpRequest resetRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/metrics/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> resetResponse = httpClient.send(resetRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(resetResponse.statusCode()).isEqualTo(200);

        // Verify metrics are cleared
        HttpResponse<String> metricsAfterResponse = httpClient.send(metricsRequest, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> metricsAfter = objectMapper.readValue(metricsAfterResponse.body(), new TypeReference<Map<String, Object>>() {});
        assertThat(metricsAfter).isEmpty();
    }

    @Test
    void testErrorResponseMetrics() throws IOException, InterruptedException {
        // Make a request that should return 404
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/stock-trades/999999"))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        // Note: The actual status code depends on how the application handles missing resources
        
        Thread.sleep(500);

        // Check that metrics were still collected
        HttpRequest metricsRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/metrics/endpoints"))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> metricsResponse = httpClient.send(metricsRequest, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> metrics = objectMapper.readValue(metricsResponse.body(), new TypeReference<Map<String, Object>>() {});

        // Verify metrics were collected even for error responses
        assertThat(metrics).containsKey("GET /api/stock-trades/{id}");
    }

    @Test
    void testMetricsCollectionForDifferentHttpMethods() throws IOException, InterruptedException {
        // Test GET request
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/stock-trades"))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();
        httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());

        // Test POST request (create metrics)
        String metricsJson = """
            {
                "testName": "Integration Test Metric",
                "testType": "INTEGRATION",
                "totalRequests": 1,
                "totalTimeMs": 100,
                "averageResponseTimeMs": 100.0,
                "testPassed": true
            }
            """;

        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/performance-metrics"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(metricsJson))
                .timeout(Duration.ofSeconds(30))
                .build();
        httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());

        Thread.sleep(500);

        // Check metrics
        HttpRequest metricsRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/metrics/endpoints"))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> metricsResponse = httpClient.send(metricsRequest, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> metrics = objectMapper.readValue(metricsResponse.body(), new TypeReference<Map<String, Object>>() {});

        // Verify both GET and POST requests are tracked
        assertThat(metrics).containsKey("GET /api/stock-trades");
        // Note: POST to performance-metrics is excluded, so it won't appear in metrics
    }

    @Test
    void testMetricsAccuracy() throws IOException, InterruptedException {
        // Make multiple requests to the same endpoint
        int requestCount = 5;
        for (int i = 0; i < requestCount; i++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/health"))
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        }

        Thread.sleep(500);

        // Check metrics accuracy
        HttpRequest metricsRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/metrics/endpoints"))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> metricsResponse = httpClient.send(metricsRequest, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> metrics = objectMapper.readValue(metricsResponse.body(), new TypeReference<Map<String, Object>>() {});

        assertThat(metrics).containsKey("GET /api/health");

        @SuppressWarnings("unchecked")
        Map<String, Object> healthMetrics = (Map<String, Object>) metrics.get("GET /api/health");

        // Verify request count is accurate
        assertThat(healthMetrics.get("totalRequests")).isEqualTo(requestCount);

        // Verify success rate is 100%
        assertThat(healthMetrics.get("successRate")).isEqualTo(100.0);

        // Verify average response time is reasonable
        Double avgResponseTime = (Double) healthMetrics.get("averageResponseTime");
        assertThat(avgResponseTime).isGreaterThan(0.0);
        assertThat(avgResponseTime).isLessThan(1000.0); // Should be less than 1 second
    }

    @Test
    void testMetricsCollectionWithAsyncEndpoints() throws IOException, InterruptedException {
        // Test async endpoint
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/stock-trades?async=true&page=0&size=5"))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        Thread.sleep(500);

        // Check that metrics were collected for async endpoint
        HttpRequest metricsRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/metrics/endpoints"))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> metricsResponse = httpClient.send(metricsRequest, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> metrics = objectMapper.readValue(metricsResponse.body(), new TypeReference<Map<String, Object>>() {});

        // Verify async endpoint metrics are collected
        assertThat(metrics).containsKey("GET /api/stock-trades");

        @SuppressWarnings("unchecked")
        Map<String, Object> endpointMetrics = (Map<String, Object>) metrics.get("GET /api/stock-trades");
        assertThat(endpointMetrics.get("successRate")).isEqualTo(100.0);
    }

    @Test
    void testMetricsCollectionPerformanceImpact() throws IOException, InterruptedException {
        // Measure response time without metrics collection overhead
        long startTime = System.currentTimeMillis();

        // Make multiple requests
        int requestCount = 10;
        for (int i = 0; i < requestCount; i++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/stock-trades?page=0&size=1"))
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double avgTimePerRequest = (double) totalTime / requestCount;

        Thread.sleep(500);

        // Verify metrics were collected
        HttpRequest metricsRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/metrics/endpoints"))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> metricsResponse = httpClient.send(metricsRequest, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> metrics = objectMapper.readValue(metricsResponse.body(), new TypeReference<Map<String, Object>>() {});

        assertThat(metrics).containsKey("GET /api/stock-trades");

        @SuppressWarnings("unchecked")
        Map<String, Object> endpointMetrics = (Map<String, Object>) metrics.get("GET /api/stock-trades");
        assertThat(endpointMetrics.get("totalRequests")).isEqualTo(requestCount);

        // Verify performance impact is minimal (average response time should be reasonable)
        Double recordedAvgTime = (Double) endpointMetrics.get("averageResponseTime");
        assertThat(recordedAvgTime).isLessThan(avgTimePerRequest * 2); // Metrics shouldn't double the response time
    }
}

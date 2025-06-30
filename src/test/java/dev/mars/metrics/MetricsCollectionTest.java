package dev.mars.metrics;

import dev.mars.Application;
import dev.mars.model.PerformanceMetrics;
import dev.mars.service.PerformanceMetricsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests to verify that metrics are being captured correctly
 */
public class MetricsCollectionTest {

    private Application application;
    private PerformanceMetricsService metricsService;
    private String BASE_URL;
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @BeforeEach
    void setUp() {
        // Use test configuration with sync metrics saving
        System.setProperty("config.file", "application-test.yml");

        application = new Application();
        application.start();

        // Get the actual port the application started on
        int port = application.getApp().port();
        BASE_URL = "http://localhost:" + port;

        // Get the metrics service from the injector
        metricsService = application.getInjector().getInstance(PerformanceMetricsService.class);

        // Wait for application to start
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Clear any existing metrics to ensure clean state
        try {
            var metricsDatabaseManager = application.getInjector().getInstance(dev.mars.database.MetricsDatabaseManager.class);
            metricsDatabaseManager.cleanDatabase();
            Thread.sleep(500); // Wait for deletion to complete
        } catch (Exception e) {
            // Ignore cleanup errors during setup
        }
    }

    @AfterEach
    void tearDown() {
        if (application != null) {
            try {
                application.stop();
                // Wait for proper shutdown
                Thread.sleep(500);
            } catch (Exception e) {
                // Ignore cleanup errors in tests
            }
        }
        System.clearProperty("config.file");
    }

    @Test
    void testMetricsAreCollectedForStockTradesEndpoint() throws IOException, InterruptedException {
        // Get initial count of metrics in database
        var initialMetrics = metricsService.getAllMetrics(0, 100);
        int initialCount = initialMetrics.getData().size();

        // Make API request to stock trades endpoint
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/generic/stock-trades"))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        // Wait for metrics to be processed and saved
        Thread.sleep(1000);

        // Verify metrics were saved to database
        var finalMetrics = metricsService.getAllMetrics(0, 100);
        int finalCount = finalMetrics.getData().size();

        // Should have more metrics than before
        assertThat(finalCount).isGreaterThan(initialCount);

        // Verify the metrics contain API request data for stock trades
        List<PerformanceMetrics> newMetrics = finalMetrics.getData();
        boolean foundStockTradesMetric = newMetrics.stream()
                .anyMatch(metric ->
                    metric.getTestType().equals("API_REQUEST") &&
                    metric.getTestName().contains("GET /api/generic/stock-trades"));

        assertThat(foundStockTradesMetric).isTrue();
    }

    @Test
    void testMetricsContainCorrectResponseTimeData() throws IOException, InterruptedException {
        // Make a specific API request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/health"))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        // Wait for metrics to be saved
        Thread.sleep(1000);

        // Find the health check metric in the database
        var allMetrics = metricsService.getAllMetrics(0, 100);
        
        PerformanceMetrics healthMetric = allMetrics.getData().stream()
                .filter(metric -> metric.getTestName().contains("GET /api/health"))
                .filter(metric -> metric.getTestType().equals("API_REQUEST"))
                .findFirst()
                .orElse(null);

        assertThat(healthMetric).isNotNull();
        assertThat(healthMetric.getTotalRequests()).isEqualTo(1);
        assertThat(healthMetric.getTestPassed()).isTrue();
        assertThat(healthMetric.getAverageResponseTimeMs()).isGreaterThan(0);
        assertThat(healthMetric.getTimestamp()).isNotNull();
    }

    @Test
    void testMetricsCollectionForMultipleEndpoints() throws IOException, InterruptedException {
        // Make requests to different endpoints
        String[] endpoints = {
            "/api/generic/stock-trades",
            "/api/generic/stock-trades/1",
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
        Thread.sleep(1000);

        // Verify metrics were collected for all endpoints
        var allMetrics = metricsService.getAllMetrics(0, 100);
        List<PerformanceMetrics> apiMetrics = allMetrics.getData().stream()
                .filter(metric -> metric.getTestType().equals("API_REQUEST"))
                .toList();

        // Should have metrics for all endpoints
        boolean hasStockTrades = apiMetrics.stream()
                .anyMatch(metric -> metric.getTestName().contains("GET /api/generic/stock-trades"));
        boolean hasStockTradeById = apiMetrics.stream()
                .anyMatch(metric -> metric.getTestName().contains("GET /api/generic/stock-trades/{id}"));
        boolean hasHealth = apiMetrics.stream()
                .anyMatch(metric -> metric.getTestName().contains("GET /api/health"));

        assertThat(hasStockTrades).isTrue();
        assertThat(hasStockTradeById).isTrue();
        assertThat(hasHealth).isTrue();
    }

    @Test
    void testMetricsExcludeConfiguredPaths() throws IOException, InterruptedException {
        // Get initial metrics count
        var initialMetrics = metricsService.getAllMetrics(0, 100);
        int initialCount = initialMetrics.getData().size();

        // Note: Dashboard is disabled in test environment to prevent hanging
        // Test with a different excluded path that's still available
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/performance-metrics"))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        // Wait for potential metrics processing
        Thread.sleep(1000);

        // Verify no new metrics were added for excluded path
        var finalMetrics = metricsService.getAllMetrics(0, 100);

        // Should not have any new metrics for performance-metrics endpoint (it's excluded)
        boolean hasPerformanceMetricsMetric = finalMetrics.getData().stream()
                .anyMatch(metric ->
                    metric.getTestType().equals("API_REQUEST") &&
                    metric.getTestName().contains("GET /api/performance-metrics"));

        assertThat(hasPerformanceMetricsMetric).isFalse();
    }

    @Test
    void testMetricsIncludeMemoryData() throws IOException, InterruptedException {
        // Make an API request that should include memory metrics
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/generic/stock-trades?page=0&size=5"))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        // Wait for metrics to be saved
        Thread.sleep(1000);

        // Find the metric in the database
        var allMetrics = metricsService.getAllMetrics(0, 100);
        
        PerformanceMetrics stockTradeMetric = allMetrics.getData().stream()
                .filter(metric -> metric.getTestName().contains("GET /api/generic/stock-trades"))
                .filter(metric -> metric.getTestType().equals("API_REQUEST"))
                .findFirst()
                .orElse(null);

        assertThat(stockTradeMetric).isNotNull();
        
        // Verify memory metrics are included (based on test configuration)
        assertThat(stockTradeMetric.getMemoryUsageBytes()).isNotNull();
        assertThat(stockTradeMetric.getMemoryUsageBytes()).isGreaterThan(0);
    }

    @Test
    void testMetricsForAsyncEndpoints() throws IOException, InterruptedException {
        // Test async endpoint
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/generic/stock-trades?async=true&page=0&size=5"))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        // Wait for metrics to be saved
        Thread.sleep(1000);

        // Verify async endpoint metrics are collected
        var allMetrics = metricsService.getAllMetrics(0, 100);
        
        boolean hasAsyncMetric = allMetrics.getData().stream()
                .anyMatch(metric ->
                    metric.getTestType().equals("API_REQUEST") &&
                    metric.getTestName().contains("GET /api/generic/stock-trades") &&
                    metric.getTestPassed());

        assertThat(hasAsyncMetric).isTrue();
    }

    @Test
    void testMetricsAdditionalDataContainsEndpointInfo() throws IOException, InterruptedException {
        // Make an API request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/generic/stock-trades/123"))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Wait for metrics to be saved
        Thread.sleep(1000);

        // Find the metric in the database
        var allMetrics = metricsService.getAllMetrics(0, 100);
        
        PerformanceMetrics metric = allMetrics.getData().stream()
                .filter(m -> m.getTestName().contains("GET /api/generic/stock-trades/{id}"))
                .filter(m -> m.getTestType().equals("API_REQUEST"))
                .findFirst()
                .orElse(null);

        assertThat(metric).isNotNull();
        
        // Verify additional metrics JSON contains endpoint information
        String additionalMetrics = metric.getAdditionalMetrics();
        assertThat(additionalMetrics).isNotNull();
        assertThat(additionalMetrics).contains("endpoint");
        assertThat(additionalMetrics).contains("method");
        assertThat(additionalMetrics).contains("path");
        assertThat(additionalMetrics).contains("statusCode");
    }

    @Test
    void testMetricsTimestampAccuracy() throws IOException, InterruptedException {
        long beforeRequest = System.currentTimeMillis();
        
        // Make an API request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/health"))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        long afterRequest = System.currentTimeMillis();

        // Wait for metrics to be saved
        Thread.sleep(1000);

        // Find the metric in the database
        var allMetrics = metricsService.getAllMetrics(0, 100);
        
        PerformanceMetrics metric = allMetrics.getData().stream()
                .filter(m -> m.getTestName().contains("GET /api/health"))
                .filter(m -> m.getTestType().equals("API_REQUEST"))
                .findFirst()
                .orElse(null);

        assertThat(metric).isNotNull();
        
        // Verify timestamp is within reasonable range
        long metricTimestamp = java.sql.Timestamp.valueOf(metric.getTimestamp()).getTime();
        assertThat(metricTimestamp).isBetween(beforeRequest, afterRequest + 5000); // Allow 5 second buffer
    }
}

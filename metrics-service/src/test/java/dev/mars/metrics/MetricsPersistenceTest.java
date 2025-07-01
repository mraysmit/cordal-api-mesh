package dev.mars.metrics;

import dev.mars.metrics.MetricsApplication;
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
 * Test to verify that metrics are properly persisted to the database
 */
class MetricsPersistenceTest {

    private MetricsApplication application;
    private PerformanceMetricsService metricsService;
    private String BASE_URL;
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @BeforeEach
    void setUp() {
        // Use test configuration with sync metrics saving
        System.setProperty("config.file", "application-test.yml");

        application = new MetricsApplication();
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
    void testMetricsArePersistentToDatabase() throws IOException, InterruptedException {
        // Get initial count of metrics in database
        var initialMetrics = metricsService.getAllMetrics(0, 100);
        int initialCount = initialMetrics.getData().size();

        // Make API requests to generate metrics
        String[] endpoints = {
            "/api/performance-metrics",
            "/api/performance-metrics/summary",
            "/api/health"
        };

        for (String endpoint : endpoints) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + endpoint))
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isIn(200, 404); // 404 is acceptable for non-existent IDs
        }

        // Wait for metrics to be processed and saved
        Thread.sleep(1000);

        // Verify metrics were saved to database
        var finalMetrics = metricsService.getAllMetrics(0, 100);
        int finalCount = finalMetrics.getData().size();

        // Should have more metrics than before
        assertThat(finalCount).isGreaterThan(initialCount);

        // Verify the metrics contain API request data
        List<PerformanceMetrics> newMetrics = finalMetrics.getData();
        boolean foundApiRequestMetric = newMetrics.stream()
                .anyMatch(metric -> metric.getTestType().equals("API_REQUEST"));

        assertThat(foundApiRequestMetric).isTrue();
    }

    @Test
    void testMetricsContainCorrectData() throws IOException, InterruptedException {
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
    void testMetricsWithMemoryData() throws IOException, InterruptedException {
        // Make an API request that should include memory metrics
        // Use health endpoint since it exists and is not excluded
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/health"))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        // Wait for metrics to be saved
        Thread.sleep(1000);

        // Find the metric in the database
        var allMetrics = metricsService.getAllMetrics(0, 100);

        PerformanceMetrics healthMetric = allMetrics.getData().stream()
                .filter(metric -> metric.getTestName().contains("GET /api/health"))
                .filter(metric -> metric.getTestType().equals("API_REQUEST"))
                .findFirst()
                .orElse(null);

        assertThat(healthMetric).isNotNull();

        // Verify memory metrics are included (based on test configuration)
        assertThat(healthMetric.getMemoryUsageBytes()).isNotNull();
        assertThat(healthMetric.getMemoryUsageBytes()).isGreaterThan(0);
    }

    @Test
    void testMetricsAdditionalData() throws IOException, InterruptedException {
        // Make an API request to an endpoint that exists
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/health"))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        // Status code doesn't matter for this test

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
        
        // Verify additional metrics JSON contains endpoint information
        String additionalMetrics = metric.getAdditionalMetrics();
        assertThat(additionalMetrics).isNotNull();
        assertThat(additionalMetrics).contains("endpoint");
        assertThat(additionalMetrics).contains("method");
        assertThat(additionalMetrics).contains("path");
        assertThat(additionalMetrics).contains("statusCode");
    }

    @Test
    void testMetricsForErrorResponses() throws IOException, InterruptedException {
        // Make a request that will likely result in an error (non-existent ID)
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/generic/stock-trades/999999"))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        // The response might be 404 or 200 depending on how the app handles missing resources

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
        
        // Verify the metric was recorded regardless of the response status
        assertThat(metric.getTotalRequests()).isEqualTo(1);
        assertThat(metric.getAverageResponseTimeMs()).isGreaterThan(0);
        
        // The testPassed field should reflect the HTTP status code
        if (response.statusCode() >= 400) {
            assertThat(metric.getTestPassed()).isFalse();
        } else {
            assertThat(metric.getTestPassed()).isTrue();
        }
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

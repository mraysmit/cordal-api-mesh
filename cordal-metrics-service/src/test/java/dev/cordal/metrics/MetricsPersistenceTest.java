package dev.cordal.metrics;

import dev.cordal.metrics.MetricsApplication;
import dev.cordal.common.model.PerformanceMetrics;
import dev.cordal.service.PerformanceMetricsService;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify that metrics are properly persisted to the database
 */
class MetricsPersistenceTest {

    private MetricsApplication application;
    private PerformanceMetricsService metricsService;

    @BeforeEach
    void setUp() {
        // Use test configuration with sync metrics saving
        System.setProperty("metrics.config.file", "application-test.yml");

        // Initialize application for testing (no server startup)
        application = new MetricsApplication();
        application.initializeForTesting();

        // Get the metrics service from the injector
        metricsService = application.getInjector().getInstance(PerformanceMetricsService.class);

        // Clear any existing metrics to ensure clean state for each test
        try {
            var metricsDatabaseManager = application.getInjector().getInstance(dev.cordal.database.MetricsDatabaseManager.class);
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
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
        System.clearProperty("metrics.config.file");
    }



    @Test
    void testMetricsArePersistentToDatabase() {
        Javalin app = application.getApp();

        JavalinTest.test(app, (server, client) -> {
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
                var response = client.get(endpoint);
                assertThat(response.code()).isIn(200, 404); // 404 is acceptable for non-existent IDs
            }

            // Wait for metrics to be processed and saved (since async is disabled, this should be quick)
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

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
        });
    }

    @Test
    void testMetricsContainCorrectData() {
        Javalin app = application.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Make a specific API request
            var response = client.get("/api/health");
            assertThat(response.code()).isEqualTo(200);

            // Wait for metrics to be saved (since async is disabled, this should be quick)
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

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
        });
    }

    @Test
    void testMetricsWithMemoryData() {
        Javalin app = application.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Make an API request that should include memory metrics
            var response = client.get("/api/health");
            assertThat(response.code()).isEqualTo(200);

            // Wait for metrics to be saved (since async is disabled, this should be quick)
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

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
        });
    }

    @Test
    void testMetricsAdditionalData() {
        Javalin app = application.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Make an API request to an endpoint that exists
            client.get("/api/health");

            // Wait for metrics to be saved (since async is disabled, this should be quick)
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

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
        });
    }

    @Test
    void testMetricsForErrorResponses() {
        Javalin app = application.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Make a request that will likely result in an error (non-existent endpoint)
            var response = client.get("/api/nonexistent/endpoint");
            // The response might be 404 or other error code

            // Wait for metrics to be saved (since async is disabled, this should be quick)
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Find the metric in the database
            var allMetrics = metricsService.getAllMetrics(0, 100);

            PerformanceMetrics metric = allMetrics.getData().stream()
                    .filter(m -> m.getTestName().contains("GET /api/nonexistent/endpoint"))
                    .filter(m -> m.getTestType().equals("API_REQUEST"))
                    .findFirst()
                    .orElse(null);

            assertThat(metric).isNotNull();

            // Verify the metric was recorded regardless of the response status
            assertThat(metric.getTotalRequests()).isEqualTo(1);
            assertThat(metric.getAverageResponseTimeMs()).isGreaterThan(0);

            // The testPassed field should reflect the HTTP status code
            if (response.code() >= 400) {
                assertThat(metric.getTestPassed()).isFalse();
            } else {
                assertThat(metric.getTestPassed()).isTrue();
            }
        });
    }

    @Test
    void testMetricsTimestampAccuracy() {
        Javalin app = application.getApp();

        JavalinTest.test(app, (server, client) -> {
            long beforeRequest = System.currentTimeMillis();

            // Make an API request
            client.get("/api/health");

            long afterRequest = System.currentTimeMillis();

            // Wait for metrics to be saved (since async is disabled, this should be quick)
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

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
        });
    }
}

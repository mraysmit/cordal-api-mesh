package dev.cordal.metrics;

import dev.cordal.config.MetricsConfig;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Metrics Application
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MetricsApplicationTest {

    private MetricsApplication application;
    private int testPort = 18081; // Use different port for testing

    @BeforeEach
    void setUp() {
        // Set test configuration
        System.setProperty("metrics.config.file", "application-test.yml");

        application = new MetricsApplication();
        // Don't start the server here - let individual tests handle it
    }

    @AfterEach
    void tearDown() {
        if (application != null) {
            try {
                application.stop();
            } catch (Exception e) {
                // Ignore cleanup errors in tests
            }
        }
        System.clearProperty("metrics.config.file");
    }

    @Test
    void shouldStartApplicationSuccessfully() {
        // Initialize application without starting the server to avoid port conflicts
        application.initializeForTesting();

        assertThat(application).isNotNull();
        assertThat(application.getApp()).isNotNull();
        assertThat(application.getInjector()).isNotNull();
    }

    @Test
    void shouldHaveHealthEndpoint() {
        // Create a fresh MetricsApplication instance for this test to avoid reuse issues
        MetricsApplication testApp = new MetricsApplication();
        testApp.initializeForTesting();

        Javalin app = testApp.getApp();

        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/api/health");
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            assertThat(responseBody).contains("status");
            assertThat(responseBody).contains("UP");
            assertThat(responseBody).contains("metrics-service");
        });
    }

    @Test
    void shouldHavePerformanceMetricsEndpoints() {
        MetricsApplication testApp = new MetricsApplication();
        testApp.initializeForTesting();

        Javalin app = testApp.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Test get all metrics
            var response = client.get("/api/performance-metrics");
            assertThat(response.code()).isEqualTo(200);

            // Test get summary
            response = client.get("/api/performance-metrics/summary");
            assertThat(response.code()).isEqualTo(200);

            // Test get trends
            response = client.get("/api/performance-metrics/trends");
            assertThat(response.code()).isEqualTo(200);

            // Test get test types
            response = client.get("/api/performance-metrics/test-types");
            assertThat(response.code()).isEqualTo(200);
        });
    }

    @Test
    void shouldHaveRealTimeMetricsEndpoints() {
        MetricsApplication testApp = new MetricsApplication();
        testApp.initializeForTesting();

        Javalin app = testApp.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Test endpoint metrics summary
            var response = client.get("/api/metrics/endpoints");
            assertThat(response.code()).isEqualTo(200);

            // Test reset metrics
            response = client.post("/api/metrics/reset", "");
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            assertThat(responseBody).contains("reset successfully");
        });
    }

    @Test
    void shouldHaveCustomDashboard() {
        MetricsApplication testApp = new MetricsApplication();
        testApp.initializeForTesting();

        Javalin app = testApp.getApp();

        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/dashboard");
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            assertThat(responseBody).contains("Metrics Dashboard");
            assertThat(responseBody).contains("Metrics Service");
        });
    }

    @Test
    void shouldCreateAndRetrievePerformanceMetrics() {
        MetricsApplication testApp = new MetricsApplication();
        testApp.initializeForTesting();

        Javalin app = testApp.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Create a performance metric
            String metricJson = """
                {
                    "testName": "Integration Test",
                    "testType": "API",
                    "totalTimeMs": 150,
                    "testPassed": true,
                    "memoryUsageBytes": 1024000,
                    "averageResponseTimeMs": 50.0,
                    "totalRequests": 100,
                    "concurrentThreads": 1
                }
                """;

            var createResponse = client.post("/api/performance-metrics", metricJson);
            assertThat(createResponse.code()).isEqualTo(201);

            // Retrieve all metrics to verify creation
            var getResponse = client.get("/api/performance-metrics");
            assertThat(getResponse.code()).isEqualTo(200);

            String responseBody = getResponse.body().string();
            assertThat(responseBody).contains("Integration Test");
        });
    }

    @Test
    void shouldFilterMetricsByTestType() {
        MetricsApplication testApp = new MetricsApplication();
        testApp.initializeForTesting();

        Javalin app = testApp.getApp();

        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/api/performance-metrics/test-type/API");
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            assertThat(responseBody).contains("data");
        });
    }

    @Test
    void shouldFilterMetricsByDateRange() {
        MetricsApplication testApp = new MetricsApplication();
        testApp.initializeForTesting();

        Javalin app = testApp.getApp();

        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/api/performance-metrics/date-range?startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59");
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            assertThat(responseBody).contains("data");
        });
    }

    @Test
    void shouldCollectMetricsAutomatically() {
        MetricsApplication testApp = new MetricsApplication();
        testApp.initializeForTesting();

        Javalin app = testApp.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Make a request to trigger metrics collection
            client.get("/api/health");

            // Check that endpoint metrics were collected
            var response = client.get("/api/metrics/endpoints");
            assertThat(response.code()).isEqualTo(200);

            // The response should contain metrics data
            String responseBody = response.body().string();
            assertThat(responseBody).contains("GET /api/health");
        });
    }

    @Test
    void shouldHandleInvalidMetricCreation() {
        MetricsApplication testApp = new MetricsApplication();
        testApp.initializeForTesting();

        Javalin app = testApp.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Try to create an invalid metric
            String invalidJson = """
                {
                    "testName": "",
                    "duration": -1
                }
                """;

            var response = client.post("/api/performance-metrics", invalidJson);
            // Should handle validation errors gracefully
            assertThat(response.code()).isIn(400, 422, 500);
        });
    }
}

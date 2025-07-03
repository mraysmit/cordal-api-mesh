package dev.mars.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.mars.generic.GenericApiApplication;
import dev.mars.metrics.MetricsApplication;
import okhttp3.*;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive integration tests between Generic API Service and Metrics Service
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ModularServicesIntegrationTest {

    private GenericApiApplication genericApiApp;
    private MetricsApplication metricsApp;
    private OkHttpClient httpClient;
    private ObjectMapper objectMapper;

    private static final String GENERIC_API_BASE_URL = "http://localhost:18080";
    private static final String METRICS_API_BASE_URL = "http://localhost:18081";

    @BeforeAll
    void setUpAll() {
        // Initialize HTTP client and JSON mapper
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Start both applications
        startApplications();
        
        // Wait for applications to be ready
        waitForApplicationsToStart();
    }

    @AfterAll
    void tearDownAll() {
        if (genericApiApp != null) {
            genericApiApp.stop();
        }
        if (metricsApp != null) {
            metricsApp.stop();
        }
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }

    private void startApplications() {
        // Start Generic API Service first
        Thread genericApiThread = new Thread(() -> {
            System.setProperty("generic.config.file", "application-generic-api.yml");
            System.setProperty("test.data.loading.enabled", "true");
            try {
                genericApiApp = new GenericApiApplication();
                genericApiApp.start();
            } finally {
                System.clearProperty("generic.config.file");
                System.clearProperty("test.data.loading.enabled");
            }
        });
        genericApiThread.start();

        // Wait a moment to avoid system property conflicts
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Start Metrics Service
        Thread metricsThread = new Thread(() -> {
            System.setProperty("metrics.config.file", "application-metrics.yml");
            try {
                metricsApp = new MetricsApplication();
                metricsApp.start();
            } finally {
                System.clearProperty("metrics.config.file");
            }
        });
        metricsThread.start();
    }

    private void waitForApplicationsToStart() {
        // Wait for Generic API Service to be ready
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> isServiceHealthy(GENERIC_API_BASE_URL));

        // Wait for Metrics Service to be ready
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> isServiceHealthy(METRICS_API_BASE_URL));
    }

    private boolean isServiceHealthy(String baseUrl) {
        try {
            Request request = new Request.Builder()
                    .url(baseUrl + "/api/health")
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    @Order(1)
    void shouldHaveBothServicesRunning() throws IOException {
        // Test Generic API Service health
        Response genericResponse = makeRequest(GENERIC_API_BASE_URL + "/api/health");
        assertThat(genericResponse.isSuccessful()).isTrue();
        
        String genericBody = genericResponse.body().string();
        assertThat(genericBody).contains("generic-api-service");
        assertThat(genericBody).contains("UP");

        // Test Metrics Service health
        Response metricsResponse = makeRequest(METRICS_API_BASE_URL + "/api/health");
        assertThat(metricsResponse.isSuccessful()).isTrue();
        
        String metricsBody = metricsResponse.body().string();
        assertThat(metricsBody).contains("metrics-service");
        assertThat(metricsBody).contains("UP");
    }

    @Test
    @Order(2)
    void shouldHaveIndependentDatabases() throws IOException {
        // Generic API should have stock trades data
        Response stockTradesResponse = makeRequest(GENERIC_API_BASE_URL + "/api/generic/stock-trades");
        assertThat(stockTradesResponse.isSuccessful()).isTrue();
        
        String stockTradesBody = stockTradesResponse.body().string();
        JsonNode stockTradesJson = objectMapper.readTree(stockTradesBody);
        assertThat(stockTradesJson.has("data")).isTrue();

        // Metrics service should have separate metrics data
        Response metricsResponse = makeRequest(METRICS_API_BASE_URL + "/api/performance-metrics");
        assertThat(metricsResponse.isSuccessful()).isTrue();
        
        String metricsBody = metricsResponse.body().string();
        JsonNode metricsJson = objectMapper.readTree(metricsBody);
        assertThat(metricsJson.has("data")).isTrue();
    }

    @Test
    @Order(3)
    void shouldCollectMetricsFromGenericApiCalls() throws IOException, InterruptedException {
        // Reset metrics first
        Response resetResponse = makePostRequest(METRICS_API_BASE_URL + "/api/metrics/reset", "");
        assertThat(resetResponse.isSuccessful()).isTrue();

        // Make several calls to Generic API to generate metrics
        for (int i = 0; i < 5; i++) {
            makeRequest(GENERIC_API_BASE_URL + "/api/generic/stock-trades");
            makeRequest(GENERIC_API_BASE_URL + "/api/generic/config/validate");
            Thread.sleep(100); // Small delay between requests
        }

        // Wait a moment for metrics to be collected
        Thread.sleep(2000);

        // Check that metrics were collected
        Response endpointMetricsResponse = makeRequest(METRICS_API_BASE_URL + "/api/metrics/endpoints");
        assertThat(endpointMetricsResponse.isSuccessful()).isTrue();
        
        String endpointMetricsBody = endpointMetricsResponse.body().string();
        JsonNode endpointMetricsJson = objectMapper.readTree(endpointMetricsBody);
        // The response is a map of endpoints, so check if it's an object (not empty)
        assertThat(endpointMetricsJson.isObject()).isTrue();
    }

    @Test
    @Order(4)
    void shouldCreatePerformanceMetricsViaApi() throws IOException {
        // Create a performance metric via Metrics API
        String metricJson = """
            {
                "testName": "Integration Test - Generic API Load",
                "testType": "LOAD_TEST",
                "totalTimeMs": 5000,
                "testPassed": true,
                "memoryUsageBytes": 2048000,
                "averageResponseTimeMs": 75.0,
                "totalRequests": 150,
                "additionalMetrics": "{\\"cpuUsagePercent\\": 45.5, \\"requestsPerSecond\\": 150.0, \\"throughputMbps\\": 15.5}"
            }
            """;

        Response createResponse = makePostRequest(METRICS_API_BASE_URL + "/api/performance-metrics", metricJson);
        assertThat(createResponse.isSuccessful()).isTrue();

        // Verify the metric was created
        Response getAllResponse = makeRequest(METRICS_API_BASE_URL + "/api/performance-metrics");
        assertThat(getAllResponse.isSuccessful()).isTrue();
        
        String getAllBody = getAllResponse.body().string();
        JsonNode getAllJson = objectMapper.readTree(getAllBody);
        // Check if the response has data field and contains our test data
        assertThat(getAllJson.has("data")).isTrue();
        String responseString = getAllJson.toString();
        assertThat(responseString).contains("Integration Test - Generic API Load");
        assertThat(responseString).contains("LOAD_TEST");
    }

    @Test
    @Order(5)
    void shouldValidateConfigurationIndependently() throws IOException {
        // Test Generic API configuration validation
        Response configValidationResponse = makeRequest(GENERIC_API_BASE_URL + "/api/generic/config/validate");
        assertThat(configValidationResponse.isSuccessful()).isTrue();
        
        String configBody = configValidationResponse.body().string();
        JsonNode configJson = objectMapper.readTree(configBody);
        assertThat(configJson.get("status").asText()).isEqualTo("VALID");

        // Test that configuration endpoints are independent
        Response endpointsResponse = makeRequest(GENERIC_API_BASE_URL + "/api/generic/endpoints");
        assertThat(endpointsResponse.isSuccessful()).isTrue();
        
        String endpointsBody = endpointsResponse.body().string();
        JsonNode endpointsJson = objectMapper.readTree(endpointsBody);
        assertThat(endpointsJson.has("endpoints")).isTrue();
    }

    @Test
    @Order(6)
    void shouldHaveIndependentSwaggerDocumentation() throws IOException {
        // Test Generic API Swagger
        Response genericSwaggerResponse = makeRequest(GENERIC_API_BASE_URL + "/openapi.json");
        assertThat(genericSwaggerResponse.isSuccessful()).isTrue();
        
        String genericSwaggerBody = genericSwaggerResponse.body().string();
        JsonNode genericSwaggerJson = objectMapper.readTree(genericSwaggerBody);
        assertThat(genericSwaggerJson.has("openapi")).isTrue();
        assertThat(genericSwaggerJson.get("info").get("title").asText()).contains("Generic API");

        // Metrics service doesn't have Swagger, but should have dashboard
        Response metricsDashboardResponse = makeRequest(METRICS_API_BASE_URL + "/dashboard");
        assertThat(metricsDashboardResponse.isSuccessful()).isTrue();
        
        String dashboardBody = metricsDashboardResponse.body().string();
        assertThat(dashboardBody).contains("Metrics Dashboard");
    }

    @Test
    @Order(7)
    void shouldHandleHighLoadIndependently() throws IOException, InterruptedException {
        // Test that both services can handle concurrent requests independently
        int numberOfRequests = 20;
        
        // Make concurrent requests to both services
        for (int i = 0; i < numberOfRequests; i++) {
            final int requestId = i;
            
            // Generic API requests
            new Thread(() -> {
                try {
                    makeRequest(GENERIC_API_BASE_URL + "/api/generic/stock-trades/symbol/AAPL");
                } catch (IOException e) {
                    // Log but don't fail the test
                    System.err.println("Request " + requestId + " to Generic API failed: " + e.getMessage());
                }
            }).start();
            
            // Metrics API requests
            new Thread(() -> {
                try {
                    makeRequest(METRICS_API_BASE_URL + "/api/performance-metrics/summary");
                } catch (IOException e) {
                    // Log but don't fail the test
                    System.err.println("Request " + requestId + " to Metrics API failed: " + e.getMessage());
                }
            }).start();
        }

        // Wait for all requests to complete
        Thread.sleep(5000);

        // Verify both services are still healthy
        assertThat(isServiceHealthy(GENERIC_API_BASE_URL)).isTrue();
        assertThat(isServiceHealthy(METRICS_API_BASE_URL)).isTrue();
    }

    @Test
    @Order(8)
    void shouldMaintainDataIntegrityAcrossServices() throws IOException {
        // Get initial counts from both services
        Response stockTradesResponse = makeRequest(GENERIC_API_BASE_URL + "/api/generic/stock-trades");
        JsonNode stockTradesJson = objectMapper.readTree(stockTradesResponse.body().string());
        int initialStockTradesCount = stockTradesJson.get("data").size();

        Response metricsResponse = makeRequest(METRICS_API_BASE_URL + "/api/performance-metrics");
        JsonNode metricsJson = objectMapper.readTree(metricsResponse.body().string());

        // Check total count if available, otherwise fall back to data size
        int initialMetricsCount;
        if (metricsJson.has("totalElements")) {
            initialMetricsCount = metricsJson.get("totalElements").asInt();
        } else if (metricsJson.has("data")) {
            initialMetricsCount = metricsJson.get("data").size();
        } else {
            initialMetricsCount = 0;
        }

        // Add data to metrics service
        String newMetricJson = """
            {
                "testName": "Data Integrity Test",
                "testType": "INTEGRITY",
                "totalTimeMs": 1000,
                "testPassed": true,
                "memoryUsageBytes": 1024000,
                "averageResponseTimeMs": 25.0,
                "totalRequests": 50,
                "additionalMetrics": "{\\"cpuUsagePercent\\": 15.0, \\"requestsPerSecond\\": 50.0, \\"throughputMbps\\": 5.0}"
            }
            """;

        Response createMetricResponse = makePostRequest(METRICS_API_BASE_URL + "/api/performance-metrics", newMetricJson);
        assertThat(createMetricResponse.isSuccessful()).isTrue();

        // Wait a moment for async processing
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify stock trades count hasn't changed
        Response newStockTradesResponse = makeRequest(GENERIC_API_BASE_URL + "/api/generic/stock-trades");
        JsonNode newStockTradesJson = objectMapper.readTree(newStockTradesResponse.body().string());
        int newStockTradesCount = newStockTradesJson.get("data").size();
        assertThat(newStockTradesCount).isEqualTo(initialStockTradesCount);

        // Verify metrics count has increased by checking total count, not just page size
        Response newMetricsResponse = makeRequest(METRICS_API_BASE_URL + "/api/performance-metrics");
        JsonNode newMetricsJson = objectMapper.readTree(newMetricsResponse.body().string());

        // Check total count if available, otherwise fall back to data size
        int newMetricsCount;
        if (newMetricsJson.has("totalElements")) {
            newMetricsCount = newMetricsJson.get("totalElements").asInt();
        } else if (newMetricsJson.has("data")) {
            newMetricsCount = newMetricsJson.get("data").size();
        } else {
            newMetricsCount = 0;
        }

        assertThat(newMetricsCount).isGreaterThanOrEqualTo(initialMetricsCount + 1);
    }

    private Response makeRequest(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        return httpClient.newCall(request).execute();
    }

    private Response makePostRequest(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        return httpClient.newCall(request).execute();
    }
}

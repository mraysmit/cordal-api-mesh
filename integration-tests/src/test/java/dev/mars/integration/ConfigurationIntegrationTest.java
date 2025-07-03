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
 * Integration tests for configuration management across services
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfigurationIntegrationTest {

    private GenericApiApplication genericApiApp;
    private MetricsApplication metricsApp;
    private OkHttpClient httpClient;
    private ObjectMapper objectMapper;

    private static final String GENERIC_API_BASE_URL = "http://localhost:18080";
    private static final String METRICS_API_BASE_URL = "http://localhost:18081";

    @BeforeAll
    void setUpAll() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        startApplications();
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
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> isServiceHealthy(GENERIC_API_BASE_URL) && isServiceHealthy(METRICS_API_BASE_URL));
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
    void shouldHaveIndependentServerConfigurations() throws IOException {
        // Generic API should be on port 8080
        Response genericHealthResponse = makeRequest(GENERIC_API_BASE_URL + "/api/health");
        assertThat(genericHealthResponse.isSuccessful()).isTrue();
        
        String genericHealthBody = genericHealthResponse.body().string();
        JsonNode genericHealthJson = objectMapper.readTree(genericHealthBody);
        assertThat(genericHealthJson.get("service").asText()).isEqualTo("generic-api-service");

        // Metrics API should be on port 8081
        Response metricsHealthResponse = makeRequest(METRICS_API_BASE_URL + "/api/health");
        assertThat(metricsHealthResponse.isSuccessful()).isTrue();
        
        String metricsHealthBody = metricsHealthResponse.body().string();
        JsonNode metricsHealthJson = objectMapper.readTree(metricsHealthBody);
        assertThat(metricsHealthJson.get("service").asText()).isEqualTo("metrics-service");
    }

    @Test
    void shouldHaveValidGenericApiConfiguration() throws IOException {
        // Test configuration validation
        Response configValidationResponse = makeRequest(GENERIC_API_BASE_URL + "/api/generic/config/validate");
        assertThat(configValidationResponse.isSuccessful()).isTrue();
        
        String configBody = configValidationResponse.body().string();
        JsonNode configJson = objectMapper.readTree(configBody);
        assertThat(configJson.get("status").asText()).isEqualTo("VALID");
        assertThat(configJson.has("errors")).isTrue();

        // Test endpoint configurations
        Response endpointsResponse = makeRequest(GENERIC_API_BASE_URL + "/api/generic/endpoints");
        assertThat(endpointsResponse.isSuccessful()).isTrue();
        
        String endpointsBody = endpointsResponse.body().string();
        JsonNode endpointsJson = objectMapper.readTree(endpointsBody);
        assertThat(endpointsJson.has("endpoints")).isTrue();
        assertThat(endpointsJson.get("endpoints").isObject()).isTrue();

        // Test database configurations
        Response databasesResponse = makeRequest(GENERIC_API_BASE_URL + "/api/generic/config/databases");
        assertThat(databasesResponse.isSuccessful()).isTrue();
        
        String databasesBody = databasesResponse.body().string();
        JsonNode databasesJson = objectMapper.readTree(databasesBody);
        assertThat(databasesJson.has("databases")).isTrue();
    }

    @Test
    void shouldHaveValidConfigurationRelationships() throws IOException {
        // Test that endpoint configurations reference valid queries
        Response endpointValidationResponse = makeRequest(GENERIC_API_BASE_URL + "/api/generic/config/validate/endpoints");
        assertThat(endpointValidationResponse.isSuccessful()).isTrue();
        
        String endpointValidationBody = endpointValidationResponse.body().string();
        JsonNode endpointValidationJson = objectMapper.readTree(endpointValidationBody);
        assertThat(endpointValidationJson.get("status").asText()).isEqualTo("VALID");

        // Test that query configurations reference valid databases
        Response queryValidationResponse = makeRequest(GENERIC_API_BASE_URL + "/api/generic/config/validate/queries");
        assertThat(queryValidationResponse.isSuccessful()).isTrue();

        String queryValidationBody = queryValidationResponse.body().string();
        JsonNode queryValidationJson = objectMapper.readTree(queryValidationBody);
        assertThat(queryValidationJson.get("status").asText()).isEqualTo("VALID");

        // Test database configurations
        Response databaseValidationResponse = makeRequest(GENERIC_API_BASE_URL + "/api/generic/config/validate/databases");
        assertThat(databaseValidationResponse.isSuccessful()).isTrue();

        String databaseValidationBody = databaseValidationResponse.body().string();
        JsonNode databaseValidationJson = objectMapper.readTree(databaseValidationBody);
        assertThat(databaseValidationJson.get("status").asText()).isEqualTo("VALID");

        // Test overall relationship validation
        Response relationshipValidationResponse = makeRequest(GENERIC_API_BASE_URL + "/api/generic/config/validate/relationships");
        assertThat(relationshipValidationResponse.isSuccessful()).isTrue();

        String relationshipValidationBody = relationshipValidationResponse.body().string();
        JsonNode relationshipValidationJson = objectMapper.readTree(relationshipValidationBody);
        assertThat(relationshipValidationJson.get("status").asText()).isEqualTo("VALID");
    }

    @Test
    void shouldHaveWorkingYamlConfiguredEndpoints() throws IOException {
        // Test stock trades endpoints (configured via YAML)
        Response stockTradesResponse = makeRequest(GENERIC_API_BASE_URL + "/api/generic/stock-trades");
        assertThat(stockTradesResponse.isSuccessful()).isTrue();
        
        String stockTradesBody = stockTradesResponse.body().string();
        JsonNode stockTradesJson = objectMapper.readTree(stockTradesBody);
        assertThat(stockTradesJson.has("data")).isTrue();
        assertThat(stockTradesJson.get("data").isArray()).isTrue();

        // Test stock trades by symbol
        Response stockTradesBySymbolResponse = makeRequest(GENERIC_API_BASE_URL + "/api/generic/stock-trades/symbol/AAPL");
        assertThat(stockTradesBySymbolResponse.isSuccessful()).isTrue();
        
        String stockTradesBySymbolBody = stockTradesBySymbolResponse.body().string();
        JsonNode stockTradesBySymbolJson = objectMapper.readTree(stockTradesBySymbolBody);
        assertThat(stockTradesBySymbolJson.has("data")).isTrue();
        assertThat(stockTradesBySymbolJson.get("data").isArray()).isTrue();

        // Test stock trades by date range
        Response stockTradesByDateResponse = makeRequest(GENERIC_API_BASE_URL + "/api/generic/stock-trades/date-range?start_date=2024-01-01&end_date=2024-12-31");
        assertThat(stockTradesByDateResponse.isSuccessful()).isTrue();
        
        String stockTradesByDateBody = stockTradesByDateResponse.body().string();
        JsonNode stockTradesByDateJson = objectMapper.readTree(stockTradesByDateBody);
        assertThat(stockTradesByDateJson.has("data")).isTrue();
    }

    @Test
    void shouldHaveIndependentDatabaseConfigurations() throws IOException {
        // Generic API should use its own database
        Response genericConfigResponse = makeRequest(GENERIC_API_BASE_URL + "/api/generic/config");
        assertThat(genericConfigResponse.isSuccessful()).isTrue();
        
        String genericConfigBody = genericConfigResponse.body().string();
        JsonNode genericConfigJson = objectMapper.readTree(genericConfigBody);
        
        // Should have database configuration
        assertThat(genericConfigJson.has("databases")).isTrue();
        JsonNode databasesConfig = genericConfigJson.get("databases");
        assertThat(databasesConfig.toString()).contains("stocktrades");

        // Verify Generic API has stock trades data
        Response stockTradesResponse = makeRequest(GENERIC_API_BASE_URL + "/api/generic/stock-trades");
        assertThat(stockTradesResponse.isSuccessful()).isTrue();
        
        String stockTradesBody = stockTradesResponse.body().string();
        JsonNode stockTradesJson = objectMapper.readTree(stockTradesBody);
        assertThat(stockTradesJson.get("data").size()).isGreaterThan(0);

        // Verify Metrics API has separate metrics data
        Response metricsResponse = makeRequest(METRICS_API_BASE_URL + "/api/performance-metrics");
        assertThat(metricsResponse.isSuccessful()).isTrue();
        
        String metricsBody = metricsResponse.body().string();
        JsonNode metricsJson = objectMapper.readTree(metricsBody);
        assertThat(metricsJson.has("data")).isTrue();
    }

    @Test
    void shouldHaveWorkingSwaggerConfiguration() throws IOException {
        // Test OpenAPI JSON endpoint
        Response openApiResponse = makeRequest(GENERIC_API_BASE_URL + "/openapi.json");
        assertThat(openApiResponse.isSuccessful()).isTrue();
        
        String openApiBody = openApiResponse.body().string();
        JsonNode openApiJson = objectMapper.readTree(openApiBody);
        assertThat(openApiJson.has("openapi")).isTrue();
        assertThat(openApiJson.has("info")).isTrue();
        assertThat(openApiJson.has("paths")).isTrue();

        // Verify it contains our configured endpoints
        JsonNode paths = openApiJson.get("paths");
        assertThat(paths.has("/api/generic/stock-trades")).isTrue();
        assertThat(paths.has("/api/generic/config/validate")).isTrue();

        // Test Swagger UI endpoint
        Response swaggerUiResponse = makeRequest(GENERIC_API_BASE_URL + "/swagger");
        assertThat(swaggerUiResponse.isSuccessful()).isTrue();
        
        String swaggerUiBody = swaggerUiResponse.body().string();
        assertThat(swaggerUiBody).contains("swagger");
        assertThat(swaggerUiBody).contains("Javalin API Mesh");

        // Test API docs endpoint
        Response apiDocsResponse = makeRequest(GENERIC_API_BASE_URL + "/api-docs");
        assertThat(apiDocsResponse.isSuccessful()).isTrue();
    }

    @Test
    void shouldHaveWorkingMetricsDashboardConfiguration() throws IOException {
        // Test custom dashboard
        Response dashboardResponse = makeRequest(METRICS_API_BASE_URL + "/dashboard");
        assertThat(dashboardResponse.isSuccessful()).isTrue();
        
        String dashboardBody = dashboardResponse.body().string();
        assertThat(dashboardBody).contains("Metrics Dashboard");
        assertThat(dashboardBody).contains("Metrics Service");
        assertThat(dashboardBody).contains("performance-metrics");

        // Test dashboard with trailing slash
        Response dashboardSlashResponse = makeRequest(METRICS_API_BASE_URL + "/dashboard/");
        assertThat(dashboardSlashResponse.isSuccessful()).isTrue();
    }

    @Test
    void shouldHandleConfigurationErrors() throws IOException {
        // Test non-existent endpoint configuration
        Response nonExistentEndpointResponse = makeRequest(GENERIC_API_BASE_URL + "/api/generic/endpoints/non-existent");
        // Should handle gracefully (either 404 or empty result)
        assertThat(nonExistentEndpointResponse.code()).isIn(200, 404);

        // Test non-existent query configuration
        Response nonExistentQueryResponse = makeRequest(GENERIC_API_BASE_URL + "/api/generic/config/queries/non-existent");
        assertThat(nonExistentQueryResponse.code()).isIn(200, 404);

        // Test non-existent database configuration
        Response nonExistentDatabaseResponse = makeRequest(GENERIC_API_BASE_URL + "/api/generic/config/databases/non-existent");
        assertThat(nonExistentDatabaseResponse.code()).isIn(200, 404);
    }

    private Response makeRequest(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        return httpClient.newCall(request).execute();
    }
}

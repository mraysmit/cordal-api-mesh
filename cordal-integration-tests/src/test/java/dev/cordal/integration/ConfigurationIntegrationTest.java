package dev.cordal.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.cordal.generic.GenericApiApplication;
import dev.cordal.metrics.MetricsApplication;
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

    private static final String GENERIC_API_BASE_URL = "http://localhost:19080";
    private static final String METRICS_API_BASE_URL = "http://localhost:18081";

    @BeforeAll
    void setUpAll() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Create test database and populate data BEFORE starting applications
        createTestDatabase();

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
        System.out.println("DEBUG: Starting applications...");

        // Start Generic API Service first
        Thread genericApiThread = new Thread(() -> {
            System.setProperty("generic.config.file", "application-generic-api.yml");
            System.setProperty("test.data.loading.enabled", "true");
            // Use the integration test configuration directory where stock trades examples are located
            System.setProperty("config.directories", "src/test/resources/config");
            // Use integration test configuration files that include stock trades examples
            System.setProperty("database.patterns", "integration-test-databases.yml");
            System.setProperty("query.patterns", "integration-test-queries.yml");
            System.setProperty("endpoint.patterns", "integration-test-api-endpoints.yml");
            System.out.println("DEBUG: Starting Generic API Service with config: " + System.getProperty("generic.config.file"));
            System.out.println("DEBUG: Test data loading enabled: " + System.getProperty("test.data.loading.enabled"));
            System.out.println("DEBUG: Config directories: " + System.getProperty("config.directories"));
            try {
                genericApiApp = new GenericApiApplication();
                System.out.println("DEBUG: Generic API Application created, starting...");
                genericApiApp.start();
                System.out.println("DEBUG: Generic API Service started successfully on port 19080");
            } catch (Exception e) {
                System.err.println("ERROR: Failed to start Generic API Service: " + e.getMessage());
                e.printStackTrace();
            } finally {
                System.clearProperty("generic.config.file");
                System.clearProperty("test.data.loading.enabled");
                System.clearProperty("config.directories");
            }
        });
        genericApiThread.setName("GenericAPI-Startup");
        genericApiThread.start();

        // Wait a moment to avoid system property conflicts
        try {
            Thread.sleep(2000); // Increased wait time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Start Metrics Service
        Thread metricsThread = new Thread(() -> {
            System.setProperty("metrics.config.file", "application-metrics.yml");
            System.out.println("DEBUG: Starting Metrics Service with config: " + System.getProperty("metrics.config.file"));
            try {
                metricsApp = new MetricsApplication();
                System.out.println("DEBUG: Metrics Application created, starting...");
                metricsApp.start();
                System.out.println("DEBUG: Metrics Service started successfully on port 18081");
            } catch (Exception e) {
                System.err.println("ERROR: Failed to start Metrics Service: " + e.getMessage());
                e.printStackTrace();
            } finally {
                System.clearProperty("metrics.config.file");
            }
        });
        metricsThread.setName("Metrics-Startup");
        metricsThread.start();

        System.out.println("DEBUG: Both application startup threads launched");
    }

    private void createTestDatabase() {
        System.out.println("DEBUG: Creating test database and populating data...");
        try {
            // Create H2 database connection directly
            String jdbcUrl = "jdbc:h2:mem:stocktrades;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
            try (java.sql.Connection connection = java.sql.DriverManager.getConnection(jdbcUrl, "sa", "")) {

                // Create the stock_trades table
                String createTableSql = """
                    CREATE TABLE IF NOT EXISTS stock_trades (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        symbol VARCHAR(10) NOT NULL,
                        trade_type VARCHAR(10) NOT NULL,
                        quantity INTEGER NOT NULL,
                        price DECIMAL(10,2) NOT NULL,
                        total_value DECIMAL(15,2) NOT NULL,
                        trade_date_time TIMESTAMP NOT NULL,
                        trader_id VARCHAR(50) NOT NULL,
                        exchange VARCHAR(20) NOT NULL
                    )
                    """;

                try (java.sql.Statement stmt = connection.createStatement()) {
                    stmt.execute(createTableSql);
                    System.out.println("DEBUG: Created stock_trades table");
                }

                // Insert sample data
                String insertSql = """
                    INSERT INTO stock_trades (symbol, trade_type, quantity, price, total_value, trade_date_time, trader_id, exchange) VALUES
                    ('AAPL', 'BUY', 100, 150.00, 15000.00, '2025-01-15 10:30:00', 'trader1', 'NASDAQ'),
                    ('GOOGL', 'SELL', 50, 2800.00, 140000.00, '2025-01-15 11:00:00', 'trader2', 'NASDAQ'),
                    ('MSFT', 'BUY', 200, 300.00, 60000.00, '2025-01-15 14:30:00', 'trader1', 'NASDAQ'),
                    ('TSLA', 'BUY', 75, 800.00, 60000.00, '2025-01-15 15:00:00', 'trader3', 'NASDAQ'),
                    ('AMZN', 'SELL', 25, 3200.00, 80000.00, '2025-01-15 16:30:00', 'trader2', 'NASDAQ')
                    """;

                try (java.sql.Statement stmt = connection.createStatement()) {
                    int rowsInserted = stmt.executeUpdate(insertSql);
                    System.out.println("DEBUG: Inserted " + rowsInserted + " rows of test data");
                }

                // Verify data exists
                try (java.sql.Statement stmt = connection.createStatement();
                     java.sql.ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM stock_trades")) {
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        System.out.println("DEBUG: Verified " + count + " records in stock_trades table");
                    }
                }

                System.out.println("DEBUG: Test database created and populated successfully!");
            }
        } catch (Exception e) {
            System.err.println("ERROR: Failed to create test database: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Test database creation failed", e);
        }
    }



    private void waitForApplicationsToStart() {
        System.out.println("DEBUG: Waiting for applications to start...");
        System.out.println("DEBUG: Generic API URL: " + GENERIC_API_BASE_URL);
        System.out.println("DEBUG: Metrics API URL: " + METRICS_API_BASE_URL);

        Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> {
                    boolean genericHealthy = isServiceHealthy(GENERIC_API_BASE_URL);
                    boolean metricsHealthy = isServiceHealthy(METRICS_API_BASE_URL);
                    System.out.println("DEBUG: Health check - Generic API: " + genericHealthy + ", Metrics: " + metricsHealthy);
                    return genericHealthy && metricsHealthy;
                });

        System.out.println("DEBUG: Both applications are healthy and ready!");
    }

    private boolean isServiceHealthy(String baseUrl) {
        try {
            String healthUrl = baseUrl + "/api/health";
            Request request = new Request.Builder()
                    .url(healthUrl)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                boolean healthy = response.isSuccessful();
                if (!healthy) {
                    System.out.println("DEBUG: Health check failed for " + baseUrl + " - Status: " + response.code());
                }
                return healthy;
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Health check exception for " + baseUrl + ": " + e.getMessage());
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
        System.out.println("DEBUG: Stock trades API response code: " + stockTradesResponse.code());
        if (!stockTradesResponse.isSuccessful()) {
            System.out.println("DEBUG: Stock trades API response body: " + stockTradesResponse.body().string());
        }
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

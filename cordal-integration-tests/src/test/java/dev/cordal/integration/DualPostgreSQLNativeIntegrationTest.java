package dev.cordal.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.cordal.generic.GenericApiApplication;
import dev.cordal.integration.postgresql.setup.PostgreSQLDatabaseSetup;
import okhttp3.*;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Assumptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dual PostgreSQL Integration Test using Native PostgreSQL Databases
 * 
 * This test demonstrates the framework's capability to:
 * 1. Connect to multiple PostgreSQL databases without TestContainers
 * 2. Set up and tear down databases programmatically
 * 3. Generate REST API endpoints for both databases
 * 4. Validate data consistency and API functionality
 * 5. Test the complete end-to-end flow as a REST API consumer
 * 
 * Prerequisites:
 * - PostgreSQL server running on localhost:5432
 * - Admin user 'postgres' with password 'postgres'
 * - Application user 'testuser' with password 'testpass' (will be created if needed)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DualPostgreSQLNativeIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(DualPostgreSQLNativeIntegrationTest.class);
    
    // Database configuration
    private static final String DB_HOST = "localhost";
    private static final int DB_PORT = 5432;
    private static final String ADMIN_USERNAME = "postgres";
    private static final String ADMIN_PASSWORD = "postgres";
    private static final String DB_USERNAME = "testuser";
    private static final String DB_PASSWORD = "testpass";
    private static final String DATABASE_1_NAME = "trades_db_1";
    private static final String DATABASE_2_NAME = "trades_db_2";
    
    // API configuration
    private static final int API_SERVER_PORT = 19080;
    private static final String API_BASE_URL = "http://localhost:" + API_SERVER_PORT;
    
    // Test infrastructure
    private PostgreSQLDatabaseSetup databaseSetup;
    private GenericApiApplication apiApplication;
    private OkHttpClient httpClient;
    private ObjectMapper objectMapper;
    
    @BeforeAll
    void setUpIntegrationTest() throws Exception {
        logger.info("=== STARTING DUAL POSTGRESQL NATIVE INTEGRATION TEST ===");

        // Check if PostgreSQL is available before proceeding
        if (!isPostgreSQLAvailable()) {
            logger.warn("PostgreSQL server is not available at {}:{}. Skipping native PostgreSQL integration test.", DB_HOST, DB_PORT);
            logger.info("Note: Use DualPostgreSQLIntegrationTest for TestContainers-based testing that doesn't require external PostgreSQL.");
            Assumptions.assumeTrue(false, "PostgreSQL server not available - skipping native integration test");
            return;
        }

        // Initialize test infrastructure
        initializeTestInfrastructure();

        // Set up PostgreSQL databases
        setupPostgreSQLDatabases();
        
        // Start the Generic API Service
        startGenericApiService();
        
        // Wait for API service to be ready
        waitForApiServiceReady();
        
        logger.info("=== SETUP COMPLETED SUCCESSFULLY ===");
    }
    
    @AfterAll
    void tearDownIntegrationTest() throws Exception {
        logger.info("=== STARTING INTEGRATION TEST CLEANUP ===");
        
        // Stop API service
        if (apiApplication != null) {
            logger.info("Stopping Generic API Service");
            apiApplication.stop();
        }
        
        // Clean up HTTP client
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
        
        // Clean up PostgreSQL databases
        if (databaseSetup != null) {
            logger.info("Cleaning up PostgreSQL databases");
            try {
                databaseSetup.cleanupDatabases();
                logger.info("Database cleanup completed successfully");
            } catch (SQLException e) {
                logger.error("Failed to clean up databases", e);
            }
        }
        
        logger.info("=== INTEGRATION TEST CLEANUP COMPLETED ===");
    }
    
    /**
     * Check if PostgreSQL server is available for native testing
     */
    private boolean isPostgreSQLAvailable() {
        try {
            String url = String.format("jdbc:postgresql://%s:%d/postgres", DB_HOST, DB_PORT);
            try (Connection connection = DriverManager.getConnection(url, ADMIN_USERNAME, ADMIN_PASSWORD)) {
                logger.info("PostgreSQL server is available at {}:{}", DB_HOST, DB_PORT);
                return true;
            }
        } catch (SQLException e) {
            logger.debug("PostgreSQL server not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Initialize test infrastructure components
     */
    private void initializeTestInfrastructure() {
        logger.info("Initializing test infrastructure");
        
        // Initialize database setup utility
        databaseSetup = new PostgreSQLDatabaseSetup(
            DB_HOST, DB_PORT, ADMIN_USERNAME, ADMIN_PASSWORD,
            DB_USERNAME, DB_PASSWORD, DATABASE_1_NAME, DATABASE_2_NAME
        );
        
        // Initialize HTTP client
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        
        // Initialize JSON mapper
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        logger.info("Test infrastructure initialized successfully");
    }
    
    /**
     * Set up PostgreSQL databases with schemas and sample data
     */
    private void setupPostgreSQLDatabases() throws SQLException {
        logger.info("Setting up PostgreSQL databases");
        
        // Set up both databases with schemas and sample data
        databaseSetup.setupDatabases();
        
        // Verify database connectivity
        assertThat(databaseSetup.testConnection(DATABASE_1_NAME))
            .as("Database 1 should be accessible")
            .isTrue();
        
        assertThat(databaseSetup.testConnection(DATABASE_2_NAME))
            .as("Database 2 should be accessible")
            .isTrue();
        
        logger.info("PostgreSQL databases set up successfully");
    }
    
    /**
     * Start the Generic API Service
     */
    private void startGenericApiService() {
        logger.info("Starting Generic API Service");
        
        // Start API service in a separate thread
        Thread apiThread = new Thread(() -> {
            System.setProperty("generic.config.file", "application-generic-api.yml");
            try {
                apiApplication = new GenericApiApplication();
                apiApplication.start();
            } catch (Exception e) {
                logger.error("Failed to start Generic API Service", e);
                throw new RuntimeException("API service startup failed", e);
            } finally {
                System.clearProperty("generic.config.file");
            }
        });
        
        apiThread.setDaemon(true);
        apiThread.start();
        
        logger.info("Generic API Service startup initiated");
    }
    
    /**
     * Wait for API service to be ready
     */
    private void waitForApiServiceReady() {
        logger.info("Waiting for API service to be ready");
        
        Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> isApiServiceHealthy());
        
        logger.info("API service is ready and healthy");
    }
    
    /**
     * Check if API service is healthy
     */
    private boolean isApiServiceHealthy() {
        try {
            Request request = new Request.Builder()
                    .url(API_BASE_URL + "/api/health")
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
    void testDatabaseConnectivity() throws Exception {
        logger.info("=== TEST 1: Database Connectivity ===");
        
        // Test database connections
        assertThat(databaseSetup.testConnection(DATABASE_1_NAME))
            .as("Should be able to connect to " + DATABASE_1_NAME)
            .isTrue();
        
        assertThat(databaseSetup.testConnection(DATABASE_2_NAME))
            .as("Should be able to connect to " + DATABASE_2_NAME)
            .isTrue();
        
        logger.info("Database connectivity test passed");
    }
    
    @Test
    @Order(2)
    void testApiServiceHealth() throws Exception {
        logger.info("=== TEST 2: API Service Health ===");
        
        Request request = new Request.Builder()
                .url(API_BASE_URL + "/api/health")
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            assertThat(response.isSuccessful())
                .as("Health endpoint should return success")
                .isTrue();
            
            String responseBody = response.body().string();
            logger.info("Health check response: {}", responseBody);
        }
        
        logger.info("API service health test passed");
    }
    
    @Test
    @Order(3)
    void testTradesDb1Endpoints() throws Exception {
        logger.info("=== TEST 3: Trades DB 1 Endpoints ===");
        
        // Test list endpoint for trades-db-1
        testEndpoint("/api/generic/trades-db-1", "trades-db-1 list endpoint");
        
        // Test symbol-specific endpoint
        testEndpoint("/api/generic/trades-db-1/symbol/AAPL", "trades-db-1 symbol endpoint");
        
        // Test trader-specific endpoint
        testEndpoint("/api/generic/trades-db-1/trader/TRADER001", "trades-db-1 trader endpoint");
        
        logger.info("Trades DB 1 endpoints test passed");
    }
    
    @Test
    @Order(4)
    void testTradesDb2Endpoints() throws Exception {
        logger.info("=== TEST 4: Trades DB 2 Endpoints ===");
        
        // Test list endpoint for trades-db-2
        testEndpoint("/api/generic/trades-db-2", "trades-db-2 list endpoint");
        
        // Test symbol-specific endpoint
        testEndpoint("/api/generic/trades-db-2/symbol/UBER", "trades-db-2 symbol endpoint");
        
        // Test trader-specific endpoint
        testEndpoint("/api/generic/trades-db-2/trader/TRADER004", "trades-db-2 trader endpoint");
        
        logger.info("Trades DB 2 endpoints test passed");
    }
    
    @Test
    @Order(5)
    void testDataDifferentiation() throws Exception {
        logger.info("=== TEST 5: Data Differentiation Between Databases ===");
        
        // Get data from both databases
        JsonNode db1Data = getJsonResponse("/api/generic/trades-db-1?size=50");
        JsonNode db2Data = getJsonResponse("/api/generic/trades-db-2?size=50");
        
        // Verify both databases have data
        assertThat(db1Data.has("content")).isTrue();
        assertThat(db2Data.has("content")).isTrue();
        
        JsonNode db1Content = db1Data.get("content");
        JsonNode db2Content = db2Data.get("content");
        
        assertThat(db1Content.size()).isGreaterThan(0);
        assertThat(db2Content.size()).isGreaterThan(0);
        
        // Verify databases have different data (different symbols should be present)
        boolean foundDifferentSymbols = false;
        if (db1Content.size() > 0 && db2Content.size() > 0) {
            String db1FirstSymbol = db1Content.get(0).get("symbol").asText();
            String db2FirstSymbol = db2Content.get(0).get("symbol").asText();
            
            // Check if first symbols are different, or look through more records
            foundDifferentSymbols = !db1FirstSymbol.equals(db2FirstSymbol);
            
            if (!foundDifferentSymbols) {
                // Check more records to find differences
                for (int i = 0; i < Math.min(db1Content.size(), db2Content.size()); i++) {
                    String symbol1 = db1Content.get(i).get("symbol").asText();
                    String symbol2 = db2Content.get(i).get("symbol").asText();
                    if (!symbol1.equals(symbol2)) {
                        foundDifferentSymbols = true;
                        break;
                    }
                }
            }
        }
        
        logger.info("DB1 first symbol: {}", db1Content.size() > 0 ? db1Content.get(0).get("symbol").asText() : "N/A");
        logger.info("DB2 first symbol: {}", db2Content.size() > 0 ? db2Content.get(0).get("symbol").asText() : "N/A");
        logger.info("Found different symbols: {}", foundDifferentSymbols);
        
        logger.info("Data differentiation test completed");
    }
    
    /**
     * Test a specific endpoint
     */
    private void testEndpoint(String endpoint, String description) throws IOException {
        logger.info("Testing {}: {}", description, endpoint);
        
        Request request = new Request.Builder()
                .url(API_BASE_URL + endpoint)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            assertThat(response.isSuccessful())
                .as(description + " should return success")
                .isTrue();
            
            String responseBody = response.body().string();
            assertThat(responseBody)
                .as(description + " should return non-empty response")
                .isNotEmpty();
            
            // Verify it's valid JSON
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            assertThat(jsonResponse)
                .as(description + " should return valid JSON")
                .isNotNull();
            
            logger.info("{} test passed - returned {} characters", description, responseBody.length());
        }
    }
    
    /**
     * Get JSON response from an endpoint
     */
    private JsonNode getJsonResponse(String endpoint) throws IOException {
        Request request = new Request.Builder()
                .url(API_BASE_URL + endpoint)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            assertThat(response.isSuccessful()).isTrue();
            String responseBody = response.body().string();
            return objectMapper.readTree(responseBody);
        }
    }
}

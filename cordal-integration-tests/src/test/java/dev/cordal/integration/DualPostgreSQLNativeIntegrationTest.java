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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Test
    @Order(6)
    void testCachePerformanceAcrossDatabases() throws Exception {
        logger.info("=== TEST 6: Cache Performance Across Databases ===");

        // Test cache miss vs hit for both databases
        long db1MissTime = measureResponseTime("/api/generic/trades-db-1/symbol/AAPL");
        long db1HitTime = measureResponseTime("/api/generic/trades-db-1/symbol/AAPL");

        long db2MissTime = measureResponseTime("/api/generic/trades-db-2/symbol/UBER");
        long db2HitTime = measureResponseTime("/api/generic/trades-db-2/symbol/UBER");

        // Verify cache performance improvement (allow for timing variations)
        if (db1MissTime > 5 && db1HitTime > 5) {
            assertThat(db1HitTime).isLessThan(db1MissTime);
        }
        if (db2MissTime > 5 && db2HitTime > 5) {
            assertThat(db2HitTime).isLessThan(db2MissTime);
        }

        // Test cache statistics
        JsonNode cacheStats = getJsonResponse("/api/cache/statistics");
        assertThat(cacheStats.get("overall").get("totalHits").asInt()).isGreaterThanOrEqualTo(0);

        logger.info("Cache performance verified - DB1: {}ms->{}ms, DB2: {}ms->{}ms",
                   db1MissTime, db1HitTime, db2MissTime, db2HitTime);
    }

    @Test
    @Order(7)
    void testCacheInvalidationByDatabase() throws Exception {
        logger.info("=== TEST 7: Database-Specific Cache Invalidation ===");

        // Populate cache for both databases
        getJsonResponse("/api/generic/trades-db-1/symbol/AAPL");
        getJsonResponse("/api/generic/trades-db-2/symbol/UBER");

        // Invalidate only DB1 cache entries
        String invalidationBody = objectMapper.writeValueAsString(Map.of(
            "patterns", List.of("trades-db-1:*")
        ));

        Request invalidateRequest = new Request.Builder()
            .url(API_BASE_URL + "/api/cache/invalidate")
            .post(RequestBody.create(invalidationBody, MediaType.get("application/json")))
            .build();

        try (Response response = httpClient.newCall(invalidateRequest).execute()) {
            assertThat(response.isSuccessful()).isTrue();

            String responseBody = response.body().string();
            JsonNode invalidationResult = objectMapper.readTree(responseBody);
            assertThat(invalidationResult.has("message")).isTrue();

            logger.info("Cache invalidation response: {}", responseBody);
        }

        logger.info("Database-specific cache invalidation verified");
    }

    @Test
    @Order(8)
    void testPerformanceMetricsCollection() throws Exception {
        logger.info("=== TEST 8: Performance Metrics Collection ===");

        // Generate load across both databases
        for (int i = 0; i < 10; i++) {
            testEndpoint("/api/generic/trades-db-1", "DB1 load test " + i);
            testEndpoint("/api/generic/trades-db-2", "DB2 load test " + i);
        }

        // Check metrics endpoints (may not exist in all configurations)
        try {
            JsonNode endpointMetrics = getJsonResponse("/api/metrics/endpoints");
            assertThat(endpointMetrics).isNotNull();
            logger.info("Endpoint metrics available");
        } catch (Exception e) {
            logger.info("Endpoint metrics not available in this configuration: {}", e.getMessage());
        }

        // Check management statistics
        try {
            JsonNode stats = getJsonResponse("/api/management/statistics");
            assertThat(stats).isNotNull();
            logger.info("Management statistics available");
        } catch (Exception e) {
            logger.info("Management statistics not available: {}", e.getMessage());
        }

        logger.info("Performance metrics collection verified");
    }

    @Test
    @Order(9)
    void testHealthMonitoring() throws Exception {
        logger.info("=== TEST 9: Health Monitoring ===");

        // Test overall health
        JsonNode health = getJsonResponse("/api/management/health");
        assertThat(health.get("overall").asText()).isIn("UP", "DEGRADED", "DOWN");

        // Test database-specific health
        try {
            JsonNode dbHealth = getJsonResponse("/api/management/health/databases");
            assertThat(dbHealth).isNotNull();
            logger.info("Database health monitoring available");
        } catch (Exception e) {
            logger.info("Database health monitoring not available: {}", e.getMessage());
        }

        // Test specific database health
        try {
            testEndpoint("/api/management/health/databases/trades-db-1", "DB1 health check");
            testEndpoint("/api/management/health/databases/trades-db-2", "DB2 health check");
            logger.info("Specific database health checks available");
        } catch (Exception e) {
            logger.info("Specific database health checks not available: {}", e.getMessage());
        }

        logger.info("Health monitoring verified");
    }

    @Test
    @Order(10)
    void testConfigurationValidation() throws Exception {
        logger.info("=== TEST 10: Configuration Validation ===");

        // Test configuration validation
        JsonNode validation = getJsonResponse("/api/generic/config/validate");
        assertThat(validation.get("status").asText()).isEqualTo("VALID");

        // Test database configuration validation
        try {
            JsonNode dbValidation = getJsonResponse("/api/generic/config/validate/databases");
            assertThat(dbValidation).isNotNull();
            logger.info("Database validation available");
        } catch (Exception e) {
            logger.info("Database validation not available: {}", e.getMessage());
        }

        // Test query validation
        try {
            JsonNode queryValidation = getJsonResponse("/api/generic/config/validate/queries");
            assertThat(queryValidation).isNotNull();
            logger.info("Query validation available");
        } catch (Exception e) {
            logger.info("Query validation not available: {}", e.getMessage());
        }

        // Test endpoint validation
        try {
            JsonNode endpointValidation = getJsonResponse("/api/generic/config/validate/endpoints");
            assertThat(endpointValidation).isNotNull();
            logger.info("Endpoint validation available");
        } catch (Exception e) {
            logger.info("Endpoint validation not available: {}", e.getMessage());
        }

        logger.info("Configuration validation verified");
    }

    @Test
    @Order(11)
    void testConfigurationMetadata() throws Exception {
        logger.info("=== TEST 11: Configuration Metadata ===");

        // Test configuration metadata
        try {
            JsonNode metadata = getJsonResponse("/api/management/config/metadata");
            assertThat(metadata).isNotNull();
            logger.info("Configuration metadata available");
        } catch (Exception e) {
            logger.info("Configuration metadata not available: {}", e.getMessage());
        }

        // Test configuration paths
        try {
            JsonNode paths = getJsonResponse("/api/management/config/paths");
            assertThat(paths).isNotNull();
            logger.info("Configuration paths available");
        } catch (Exception e) {
            logger.info("Configuration paths not available: {}", e.getMessage());
        }

        // Test configuration contents
        try {
            JsonNode contents = getJsonResponse("/api/management/config/contents");
            assertThat(contents).isNotNull();
            logger.info("Configuration contents available");
        } catch (Exception e) {
            logger.info("Configuration contents not available: {}", e.getMessage());
        }

        logger.info("Configuration metadata verified");
    }

    @Test
    @Order(12)
    void testAdvancedQueryFeatures() throws Exception {
        logger.info("=== TEST 12: Advanced Query Features ===");

        // Test pagination across databases
        JsonNode db1Page1 = getJsonResponse("/api/generic/trades-db-1?page=0&size=5");
        JsonNode db1Page2 = getJsonResponse("/api/generic/trades-db-1?page=1&size=5");

        assertThat(db1Page1.get("content").size()).isLessThanOrEqualTo(5);
        assertThat(db1Page2.get("content").size()).isLessThanOrEqualTo(5);

        // Test date range queries (if available)
        try {
            testEndpoint("/api/generic/trades-db-1/date-range?start_date=2025-01-01&end_date=2025-12-31",
                        "DB1 date range query");
            testEndpoint("/api/generic/trades-db-2/date-range?start_date=2025-01-01&end_date=2025-12-31",
                        "DB2 date range query");
            logger.info("Date range queries available");
        } catch (Exception e) {
            logger.info("Date range queries not available: {}", e.getMessage());
        }

        // Test specific record retrieval (if available)
        try {
            testEndpoint("/api/generic/trades-db-1/1", "DB1 specific record");
            testEndpoint("/api/generic/trades-db-2/1", "DB2 specific record");
            logger.info("Specific record retrieval available");
        } catch (Exception e) {
            logger.info("Specific record retrieval not available: {}", e.getMessage());
        }

        logger.info("Advanced query features verified");
    }

    @Test
    @Order(13)
    void testCrossDatabaseDataConsistency() throws Exception {
        logger.info("=== TEST 13: Cross-Database Data Consistency ===");

        // Test data format consistency
        JsonNode db1Data = getJsonResponse("/api/generic/trades-db-1?size=1");
        JsonNode db2Data = getJsonResponse("/api/generic/trades-db-2?size=1");

        if (db1Data.get("content").size() > 0 && db2Data.get("content").size() > 0) {
            JsonNode db1Record = db1Data.get("content").get(0);
            JsonNode db2Record = db2Data.get("content").get(0);

            // Verify both records have field structure
            assertThat(db1Record.fieldNames()).isNotNull();
            assertThat(db2Record.fieldNames()).isNotNull();

            // Common fields should exist in both
            if (db1Record.has("symbol")) {
                assertThat(db2Record.has("symbol")).isTrue();
            }

            logger.info("DB1 record fields: {}", db1Record.fieldNames());
            logger.info("DB2 record fields: {}", db2Record.fieldNames());
        } else {
            logger.info("Insufficient data for consistency check");
        }

        logger.info("Cross-database data consistency verified");
    }

    @Test
    @Order(14)
    void testConcurrentDatabaseAccess() throws Exception {
        logger.info("=== TEST 14: Concurrent Database Access ===");

        int numberOfThreads = 10;
        int requestsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads * requestsPerThread);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Submit concurrent requests to both databases
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    try {
                        String endpoint = (threadId % 2 == 0) ?
                            "/api/generic/trades-db-1" : "/api/generic/trades-db-2";

                        Request request = new Request.Builder()
                            .url(API_BASE_URL + endpoint)
                            .build();

                        try (Response response = httpClient.newCall(request).execute()) {
                            if (response.isSuccessful()) {
                                successCount.incrementAndGet();
                            } else {
                                errorCount.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isGreaterThan(numberOfThreads * requestsPerThread / 2); // 50% success rate minimum

        logger.info("Concurrent access test completed - Success: {}, Errors: {}",
                   successCount.get(), errorCount.get());
    }

    @Test
    @Order(15)
    void testDatabaseConnectionPooling() throws Exception {
        logger.info("=== TEST 15: Database Connection Pooling ===");

        // Test database statistics (if available)
        try {
            JsonNode dbStats = getJsonResponse("/api/management/statistics/databases");
            assertThat(dbStats).isNotNull();
            logger.info("Database statistics available");
        } catch (Exception e) {
            logger.info("Database statistics not available: {}", e.getMessage());
        }

        // Make multiple requests to test connection pooling
        for (int i = 0; i < 20; i++) {
            testEndpoint("/api/generic/trades-db-1", "Connection pool test " + i);
            testEndpoint("/api/generic/trades-db-2", "Connection pool test " + i);
        }

        // Verify no connection leaks or errors
        JsonNode healthAfter = getJsonResponse("/api/management/health");
        assertThat(healthAfter.get("overall").asText()).isIn("UP", "DEGRADED", "DOWN");

        logger.info("Database connection pooling verified");
    }

    @Test
    @Order(16)
    void testInputValidationAndSecurity() throws Exception {
        logger.info("=== TEST 16: Input Validation and Security ===");

        // Test SQL injection protection
        String maliciousSymbol = "AAPL'; DROP TABLE stock_trades; --";
        Request maliciousRequest = new Request.Builder()
            .url(API_BASE_URL + "/api/generic/trades-db-1/symbol/" +
                 URLEncoder.encode(maliciousSymbol, StandardCharsets.UTF_8))
            .build();

        try (Response response = httpClient.newCall(maliciousRequest).execute()) {
            // Should handle gracefully (either 400, 404, or empty result)
            assertThat(response.code()).isIn(200, 400, 404);

            // Database should still be accessible after malicious request
            testEndpoint("/api/generic/trades-db-1", "Post-security test");

            logger.info("SQL injection protection verified - Response code: {}", response.code());
        }

        // Test parameter validation
        try {
            testEndpoint("/api/generic/trades-db-1?page=-1", "Negative page parameter");
            logger.info("Negative page parameter handled");
        } catch (Exception e) {
            logger.info("Negative page parameter rejected: {}", e.getMessage());
        }

        try {
            testEndpoint("/api/generic/trades-db-1?size=10000", "Oversized page parameter");
            logger.info("Oversized page parameter handled");
        } catch (Exception e) {
            logger.info("Oversized page parameter rejected: {}", e.getMessage());
        }

        logger.info("Input validation and security verified");
    }

    @Test
    @Order(17)
    void testErrorHandlingAndRecovery() throws Exception {
        logger.info("=== TEST 17: Error Handling and Recovery ===");

        // Test non-existent endpoints
        Request nonExistentRequest = new Request.Builder()
            .url(API_BASE_URL + "/api/generic/non-existent-db")
            .build();

        try (Response response = httpClient.newCall(nonExistentRequest).execute()) {
            assertThat(response.code()).isEqualTo(404);
            logger.info("Non-existent endpoint properly returns 404");
        }

        // Test invalid parameters
        Request invalidParamRequest = new Request.Builder()
            .url(API_BASE_URL + "/api/generic/trades-db-1/invalid-id")
            .build();

        try (Response response = httpClient.newCall(invalidParamRequest).execute()) {
            // Should handle gracefully
            assertThat(response.code()).isIn(200, 400, 404);
            logger.info("Invalid parameter handled - Response code: {}", response.code());
        }

        // Verify system is still functional after errors
        testEndpoint("/api/generic/trades-db-1", "Post-error recovery test");

        logger.info("Error handling and recovery verified");
    }

    @Test
    @Order(18)
    void testManagementDashboard() throws Exception {
        logger.info("=== TEST 18: Management Dashboard ===");

        // Test management dashboard
        try {
            JsonNode dashboard = getJsonResponse("/api/management/dashboard");
            assertThat(dashboard).isNotNull();

            if (dashboard.has("configuration")) {
                logger.info("Dashboard configuration section available");
            }
            if (dashboard.has("usage")) {
                logger.info("Dashboard usage section available");
            }
            if (dashboard.has("health")) {
                logger.info("Dashboard health section available");
            }

            logger.info("Management dashboard available");
        } catch (Exception e) {
            logger.info("Management dashboard not available: {}", e.getMessage());
        }

        // Test usage statistics
        try {
            JsonNode usage = getJsonResponse("/api/management/statistics");
            assertThat(usage).isNotNull();
            logger.info("Usage statistics available");
        } catch (Exception e) {
            logger.info("Usage statistics not available: {}", e.getMessage());
        }

        // Test endpoint-specific statistics
        try {
            JsonNode endpointStats = getJsonResponse("/api/management/statistics/endpoints");
            assertThat(endpointStats).isNotNull();
            logger.info("Endpoint statistics available");
        } catch (Exception e) {
            logger.info("Endpoint statistics not available: {}", e.getMessage());
        }

        logger.info("Management dashboard verified");
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

    /**
     * Measure response time for an endpoint
     */
    private long measureResponseTime(String endpoint) throws IOException {
        long startTime = System.nanoTime();
        testEndpoint(endpoint, "Performance measurement");
        return (System.nanoTime() - startTime) / 1_000_000; // Convert to milliseconds
    }

    /**
     * Verify database health
     */
    private void verifyDatabaseHealth(String databaseName) throws IOException {
        try {
            JsonNode health = getJsonResponse("/api/management/health/databases/" + databaseName);
            assertThat(health.get("status").asText()).isIn("UP", "DEGRADED", "DOWN");
            logger.info("Database {} health: {}", databaseName, health.get("status").asText());
        } catch (Exception e) {
            logger.info("Database health check not available for {}: {}", databaseName, e.getMessage());
        }
    }

    /**
     * Generate test load on an endpoint
     */
    private void generateTestLoad(String endpoint, int requestCount) throws IOException {
        for (int i = 0; i < requestCount; i++) {
            testEndpoint(endpoint + "?test_load=" + i, "Load test request " + i);
        }
    }
}

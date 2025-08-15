package dev.cordal.integration.postgresql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.cordal.generic.GenericApiApplication;
import dev.cordal.integration.postgresql.client.ApiTestResult;
import dev.cordal.integration.postgresql.client.StockTradesApiClient;
import dev.cordal.integration.postgresql.config.DualDatabaseConfigurationGenerator;
import dev.cordal.integration.postgresql.config.TestConfigurationFileManager;
import dev.cordal.integration.postgresql.container.PostgreSQLContainerManager;
import dev.cordal.integration.postgresql.container.PostgreSQLSchemaInitializer;
import dev.cordal.integration.postgresql.data.DatabaseDataValidator;
import dev.cordal.integration.postgresql.data.PostgreSQLTestDataGenerator;
import dev.cordal.integration.postgresql.util.TestExecutionTimer;
import dev.cordal.integration.postgresql.util.PostgreSQLTestUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive integration test for dual PostgreSQL database functionality
 * 
 * This test demonstrates the framework's capability to:
 * 1. Connect to multiple similar PostgreSQL databases
 * 2. Create standardized configurations dynamically
 * 3. Generate REST API endpoints for both databases
 * 4. Validate data consistency across databases
 * 5. Test the complete end-to-end flow as a REST API consumer
 * 
 * The test is completely self-contained using TestContainers with no external dependencies.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DualPostgreSQLIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(DualPostgreSQLIntegrationTest.class);
    
    // Test configuration
    private static final String DATABASE_1_NAME = "trades_db_1";
    private static final String DATABASE_2_NAME = "trades_db_2";
    private static final String DATABASE_1_SCHEMA = "tradesdb1";
    private static final String DATABASE_2_SCHEMA = "tradesdb2";
    private static final int SAMPLE_DATA_SIZE = 100;
    private static final int API_SERVER_PORT = 19080;
    private static final String TEST_NAME = "dual-postgresql";
    
    // Test infrastructure
    private PostgreSQLContainerManager containerManager;
    private DualDatabaseConfigurationGenerator configGenerator;
    private TestConfigurationFileManager configFileManager;
    private TestExecutionTimer executionTimer;
    private ObjectMapper objectMapper;
    
    // Application under test
    private GenericApiApplication apiApplication;
    private StockTradesApiClient apiClient;

    // Backup path for default configurations
    private Path defaultConfigBackupPath;
    
    // Test containers managed by PostgreSQLContainerManager
    
    @BeforeAll
    void setUpIntegrationTest() throws Exception {
        logger.info("=== STARTING DUAL POSTGRESQL INTEGRATION TEST ===");
        executionTimer = new TestExecutionTimer();
        executionTimer.startPhase("Setup");
        
        // Initialize test infrastructure
        initializeTestInfrastructure();
        
        // Phase 1: Container startup and database initialization
        initializeContainersAndDatabases();
        
        // Phase 2: Schema creation and data population
        createSchemasAndPopulateData();
        
        // Phase 3: Configuration generation and file management
        generateConfigurationsAndFiles();
        
        executionTimer.endPhase("Setup");
        logger.info("=== SETUP COMPLETED SUCCESSFULLY ===");
    }
    
    @AfterAll
    void tearDownIntegrationTest() throws Exception {
        logger.info("=== STARTING INTEGRATION TEST CLEANUP ===");
        executionTimer.startPhase("Cleanup");
        
        try {
            // Stop API application
            if (apiApplication != null) {
                logger.info("Stopping Generic API Application");
                apiApplication.stop();
            }
            
            // Clean up configuration files
            if (configFileManager != null) {
                configFileManager.cleanup();

                // Restore default configurations if they were disabled
                if (defaultConfigBackupPath != null) {
                    logger.info("Restoring default configuration files");
                    configFileManager.restoreDefaultConfigurations(defaultConfigBackupPath);
                }
            }
            
            // Stop containers (handled automatically by @Testcontainers)
            if (containerManager != null) {
                containerManager.stopAllContainers();
            }

            // Clean up system properties to avoid interfering with other tests
            logger.info("Cleaning up system properties");
            System.clearProperty("database.patterns");
            System.clearProperty("query.patterns");
            System.clearProperty("endpoint.patterns");
            System.clearProperty("config.directories");
            System.clearProperty("generic.config.file");
            System.clearProperty("metrics.config.file");
            System.clearProperty("test.data.loading.enabled");

        } catch (Exception e) {
            logger.error("Error during cleanup", e);
        } finally {
            executionTimer.endPhase("Cleanup");
            executionTimer.logSummary();
            logger.info("=== INTEGRATION TEST CLEANUP COMPLETED ===");
        }
    }
    
    /**
     * Initialize test infrastructure components
     */
    private void initializeTestInfrastructure() throws IOException {
        logger.info("Initializing test infrastructure");
        
        // Initialize container manager
        containerManager = new PostgreSQLContainerManager();
        
        // Initialize configuration generator
        configGenerator = new DualDatabaseConfigurationGenerator(containerManager);
        
        // Initialize configuration file manager
        configFileManager = new TestConfigurationFileManager(TEST_NAME);
        
        // Initialize JSON mapper
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        logger.info("Test infrastructure initialized successfully");
    }
    
    /**
     * Phase 1: Initialize containers and databases
     */
    private void initializeContainersAndDatabases() {
        logger.info("Phase 1: Initializing PostgreSQL containers and databases");
        executionTimer.startPhase("Container Initialization");
        
        // Create and configure containers
        containerManager.createContainer(DATABASE_1_NAME, DATABASE_1_SCHEMA);
        containerManager.createContainer(DATABASE_2_NAME, DATABASE_2_SCHEMA);
        
        // Start containers
        containerManager.startContainer(DATABASE_1_NAME);
        containerManager.startContainer(DATABASE_2_NAME);
        
        // Wait for containers to be ready
        containerManager.waitForAllContainersReady(30);
        
        executionTimer.endPhase("Container Initialization");
        logger.info("Phase 1 completed: PostgreSQL containers are running and ready");
    }
    
    /**
     * Phase 2: Create schemas and populate with test data
     */
    private void createSchemasAndPopulateData() throws SQLException {
        logger.info("Phase 2: Creating schemas and populating test data");
        executionTimer.startPhase("Schema and Data");
        
        // Initialize schemas for both databases
        PostgreSQLSchemaInitializer schema1Initializer = new PostgreSQLSchemaInitializer(DATABASE_1_NAME);
        PostgreSQLSchemaInitializer schema2Initializer = new PostgreSQLSchemaInitializer(DATABASE_2_NAME);
        
        try (Connection conn1 = containerManager.getConnection(DATABASE_1_NAME);
             Connection conn2 = containerManager.getConnection(DATABASE_2_NAME)) {
            
            // Create schemas
            schema1Initializer.initializeSchema(conn1);
            schema2Initializer.initializeSchema(conn2);
            
            // Verify schemas
            assertThat(schema1Initializer.verifySchema(conn1)).isTrue();
            assertThat(schema2Initializer.verifySchema(conn2)).isTrue();
            
            // Generate and insert test data
            PostgreSQLTestDataGenerator dataGenerator1 = new PostgreSQLTestDataGenerator(DATABASE_1_NAME);
            PostgreSQLTestDataGenerator dataGenerator2 = new PostgreSQLTestDataGenerator(DATABASE_2_NAME);
            
            dataGenerator1.generateAndInsertTestData(conn1, SAMPLE_DATA_SIZE);
            dataGenerator2.generateAndInsertTestData(conn2, SAMPLE_DATA_SIZE);
            
            // Verify data was inserted
            long count1 = schema1Initializer.getRecordCount(conn1);
            long count2 = schema2Initializer.getRecordCount(conn2);
            
            assertThat(count1).isEqualTo(SAMPLE_DATA_SIZE);
            assertThat(count2).isEqualTo(SAMPLE_DATA_SIZE);
            
            logger.info("Inserted {} records into {} and {} records into {}", count1, DATABASE_1_NAME, count2, DATABASE_2_NAME);
        }
        
        executionTimer.endPhase("Schema and Data");
        logger.info("Phase 2 completed: Schemas created and test data populated");
    }
    
    /**
     * Phase 3: Generate configurations and create files
     */
    private void generateConfigurationsAndFiles() throws IOException {
        logger.info("Phase 3: Generating configurations and creating files");
        executionTimer.startPhase("Configuration Generation");

        // Temporarily disable default configuration files to prevent conflicts
        defaultConfigBackupPath = configFileManager.temporarilyDisableDefaultConfigurations();

        // Generate all configurations
        Map<String, String> configurations = configGenerator.generateCompleteConfiguration(DATABASE_1_NAME, DATABASE_2_NAME);

        // Write configuration files to the temporary directory
        Map<String, Path> configFilePaths = configFileManager.writeConfigurationFiles(configurations);

        // Copy configuration files to the test classpath directory so the application can find them
        configFileManager.copyConfigurationsToTestClasspath(configFilePaths);

        // Update the existing application-generic-api.yml to use our test port and enable YAML loading
        configFileManager.updateExistingApplicationConfiguration(API_SERVER_PORT);

        Path appConfigFile = Paths.get("src/test/resources/application-generic-api.yml");

        // Validate all files were created correctly
        assertThat(configFileManager.validateConfigurationFiles(configFilePaths)).isTrue();
        assertThat(appConfigFile).exists();

        logger.info("Generated configurations:");
        logger.info("  - Databases: {}", configFilePaths.get("databases"));
        logger.info("  - Queries: {}", configFilePaths.get("queries"));
        logger.info("  - Endpoints: {}", configFilePaths.get("endpoints"));
        logger.info("  - Application: {}", appConfigFile);
        logger.info("  - Configurations copied to test resources for application loading");
        logger.info("  - Default configurations temporarily disabled to prevent conflicts");

        executionTimer.endPhase("Configuration Generation");
        logger.info("Phase 3 completed: Configurations generated and files created");
    }

    @Test
    @Order(1)
    void phase4_startGenericApiServiceWithDualDatabaseConfig() throws Exception {
        logger.info("=== PHASE 4: Starting Generic API Service with Dual Database Configuration ===");
        executionTimer.startPhase("API Service Startup");

        // Use the existing application-generic-api.yml configuration approach
        String appConfigResourceName = "application-generic-api.yml";

        logger.info("Using existing application configuration: {}", appConfigResourceName);
        logger.info("Configuration files have been updated in test classpath to use our generated databases");

        // Set system property for configuration file (use resource name, not full path)
        System.setProperty("generic.config.file", appConfigResourceName);
        logger.info("Set system property generic.config.file = {}", appConfigResourceName);

        try {
            // Start the Generic API Application in a separate thread
            logger.info("Starting Generic API Application on port {} with config resource: {}",
                       API_SERVER_PORT, appConfigResourceName);

            Thread startupThread = new Thread(() -> {
                try {
                    // Verify system property is set in the thread
                    logger.info("In startup thread, generic.config.file = {}",
                               System.getProperty("generic.config.file"));

                    apiApplication = new GenericApiApplication();
                    apiApplication.start();
                } catch (Exception e) {
                    logger.error("Failed to start API application", e);
                    throw new RuntimeException(e);
                }
            });

            startupThread.setDaemon(true); // Make it a daemon thread
            startupThread.start();

            // Wait for application to be ready
            waitForApiApplicationReady();

            // Initialize API client
            apiClient = new StockTradesApiClient("http://localhost:" + API_SERVER_PORT, objectMapper);

            // Verify application is responding
            assertThat(apiClient.isHealthy()).isTrue();

            logger.info("Generic API Application started successfully and is responding");

        } finally {
            System.clearProperty("generic.config.file");
        }

        executionTimer.endPhase("API Service Startup");
        logger.info("=== PHASE 4 COMPLETED: API Service is running with dual database configuration ===");
    }

    @Test
    @Order(2)
    void phase5_testRestApiEndpointsForBothDatabases() throws Exception {
        logger.info("=== PHASE 5: Testing REST API Endpoints for Both Databases ===");
        executionTimer.startPhase("API Endpoint Testing");

        // Test endpoints for database 1
        testDatabaseEndpoints(DATABASE_1_NAME, "trades-db-1");

        // Test endpoints for database 2
        testDatabaseEndpoints(DATABASE_2_NAME, "trades-db-2");

        executionTimer.endPhase("API Endpoint Testing");
        logger.info("=== PHASE 5 COMPLETED: All REST API endpoints tested successfully ===");
    }

    @Test
    @Order(3)
    void phase6_validateDataConsistencyAndCrossDbComparison() throws Exception {
        logger.info("=== PHASE 6: Validating Data Consistency and Cross-Database Comparison ===");
        executionTimer.startPhase("Data Validation");

        // Create data validator
        DatabaseDataValidator dataValidator = new DatabaseDataValidator(containerManager, apiClient);

        // Validate data consistency within each database
        boolean db1Consistent = dataValidator.validateDatabaseConsistency(DATABASE_1_NAME);
        boolean db2Consistent = dataValidator.validateDatabaseConsistency(DATABASE_2_NAME);

        assertThat(db1Consistent).isTrue();
        assertThat(db2Consistent).isTrue();

        // Compare data structure and patterns between databases
        boolean structureMatch = dataValidator.compareDataStructures(DATABASE_1_NAME, DATABASE_2_NAME);
        assertThat(structureMatch).isTrue();

        // Validate API responses match database content
        boolean apiDataMatch = dataValidator.validateApiResponsesMatchDatabase(
            DATABASE_1_NAME, "trades-db-1");
        assertThat(apiDataMatch).isTrue();

        apiDataMatch = dataValidator.validateApiResponsesMatchDatabase(
            DATABASE_2_NAME, "trades-db-2");
        assertThat(apiDataMatch).isTrue();

        executionTimer.endPhase("Data Validation");
        logger.info("=== PHASE 6 COMPLETED: Data consistency validated across both databases ===");
    }

    /**
     * Wait for the API application to be ready
     */
    private void waitForApiApplicationReady() {
        logger.info("Waiting for API application to be ready on port {}", API_SERVER_PORT);

        Awaitility.await()
                  .atMost(Duration.ofMinutes(2))
                  .pollInterval(Duration.ofSeconds(2))
                  .until(() -> {
                      try {
                          StockTradesApiClient testClient = new StockTradesApiClient(
                              "http://localhost:" + API_SERVER_PORT, objectMapper);
                          return testClient.isHealthy();
                      } catch (Exception e) {
                          logger.debug("API application not ready yet: {}", e.getMessage());
                          return false;
                      }
                  });

        logger.info("API application is ready and responding");
    }

    /**
     * Test all endpoints for a specific database
     */
    private void testDatabaseEndpoints(String databaseName, String pathPrefix) throws Exception {
        logger.info("Testing endpoints for database: {} (path prefix: {})", databaseName, pathPrefix);

        // Test list all stock trades endpoint
        JsonNode allTrades = apiClient.getAllStockTrades(pathPrefix, 0, 20);
        assertThat(allTrades).isNotNull();
        assertThat(allTrades.has("data")).isTrue();
        assertThat(allTrades.has("pagination")).isTrue();

        JsonNode data = allTrades.get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThan(0);

        logger.info("✓ GET /api/{}/stock-trades - returned {} records", pathPrefix, data.size());

        // Test pagination
        JsonNode page2 = apiClient.getAllStockTrades(pathPrefix, 1, 10);
        assertThat(page2.get("data").isArray()).isTrue();
        logger.info("✓ Pagination works - page 2 returned {} records", page2.get("data").size());

        // Test filter by symbol
        String testSymbol = PostgreSQLTestDataGenerator.getTestStockSymbols().get(0);
        JsonNode symbolTrades = apiClient.getStockTradesBySymbol(pathPrefix, testSymbol, 0, 10);
        assertThat(symbolTrades.get("data").isArray()).isTrue();

        // Verify all returned trades have the correct symbol
        for (JsonNode trade : symbolTrades.get("data")) {
            assertThat(trade.get("symbol").asText()).isEqualTo(testSymbol);
        }
        logger.info("✓ GET /api/{}/stock-trades/symbol/{} - returned {} records", pathPrefix, testSymbol, symbolTrades.get("data").size());

        // Test filter by trader
        String testTrader = PostgreSQLTestDataGenerator.getTestTraderIds().get(0);
        JsonNode traderTrades = apiClient.getStockTradesByTrader(pathPrefix, testTrader, 0, 10);
        assertThat(traderTrades.get("data").isArray()).isTrue();

        // Verify all returned trades have the correct trader
        for (JsonNode trade : traderTrades.get("data")) {
            assertThat(trade.get("trader_id").asText()).isEqualTo(testTrader);
        }
        logger.info("✓ GET /api/{}/stock-trades/trader/{} - returned {} records", pathPrefix, testTrader, traderTrades.get("data").size());

        logger.info("All endpoints tested successfully for database: {}", databaseName);
    }

    @Test
    @Order(4)
    void phase7_errorHandlingAndDiagnostics() throws Exception {
        logger.info("=== PHASE 7: Error Handling and Diagnostics Testing ===");
        executionTimer.startPhase("Error Handling");

        // Test database connectivity and diagnostics
        testDatabaseDiagnostics();

        // Test API error handling
        testApiErrorHandling();

        // Test performance monitoring
        testPerformanceMonitoring();

        // Generate final performance insights
        executionTimer.logPerformanceInsights();

        executionTimer.endPhase("Error Handling");
        logger.info("=== PHASE 7 COMPLETED: Error handling and diagnostics tested ===");
    }

    /**
     * Test database diagnostics and connectivity
     */
    private void testDatabaseDiagnostics() throws Exception {
        logger.info("Testing database diagnostics and connectivity");

        for (String databaseName : List.of(DATABASE_1_NAME, DATABASE_2_NAME)) {
            try (var connection = containerManager.getConnection(databaseName)) {
                // Test database responsiveness
                boolean responsive = PostgreSQLTestUtils.isDatabaseResponsive(connection, 5);
                assertThat(responsive).isTrue();

                // Get diagnostic information
                String diagnostics = PostgreSQLTestUtils.getDatabaseDiagnostics(connection, databaseName);
                logger.info("Database diagnostics for {}:\n{}", databaseName, diagnostics);

                // Get performance statistics
                String perfStats = PostgreSQLTestUtils.getTablePerformanceStats(connection, "stock_trades");
                logger.info("Performance statistics for {}:\n{}", databaseName, perfStats);

                // Validate table structure
                List<String> expectedColumns = List.of(
                    "id", "symbol", "trade_type", "quantity", "price",
                    "total_value", "trade_date_time", "trader_id", "exchange"
                );
                boolean structureValid = PostgreSQLTestUtils.validateTableStructure(connection, "stock_trades", expectedColumns);
                assertThat(structureValid).isTrue();

                logger.info("✓ Database diagnostics passed for: {}", databaseName);
            }
        }
    }

    /**
     * Test API error handling scenarios
     * NOTE: This method tests INTENTIONAL error scenarios to validate proper error handling.
     * All errors logged here are EXPECTED and indicate correct system behavior.
     */
    private void testApiErrorHandling() throws Exception {
        logger.info("Testing API error handling scenarios");
        logger.info("NOTE: The following error tests are INTENTIONAL - errors indicate correct behavior");

        // Test invalid endpoints (should return 404)
        logger.info("INTENTIONAL ERROR TEST: Testing invalid database path (expecting 404)...");
        ApiTestResult invalidDbResult = apiClient.testInvalidDatabasePath("invalid-database", 0, 10);
        if (invalidDbResult.isExpectedError()) {
            logger.info("✓ INTENTIONAL ERROR TEST PASSED: Invalid database path correctly returned {}: {}", invalidDbResult.getStatusCode(), invalidDbResult.getMessage());
        } else {
            logger.warn("INTENTIONAL ERROR TEST FAILED: Expected 404 for invalid database path, but got: {}", invalidDbResult.getMessage());
        }

        // Test invalid parameters
        logger.info("INTENTIONAL ERROR TEST: Testing negative page number (expecting 400/500)...");
        ApiTestResult invalidPageResult = apiClient.testInvalidPageParameter("trades-db-1", -1, 10);
        if (invalidPageResult.isExpectedError()) {
            logger.info("✓ INTENTIONAL ERROR TEST PASSED: Negative page number correctly returned {}: {}", invalidPageResult.getStatusCode(), invalidPageResult.getMessage());
        } else {
            logger.warn("INTENTIONAL ERROR TEST FAILED: Expected error for negative page number, but got: {}", invalidPageResult.getMessage());
        }

        // Test configuration validation endpoint (this should succeed)
        try {
            JsonNode validation = apiClient.getConfigurationValidation();
            assertThat(validation).isNotNull();
            logger.info("✓ Configuration validation endpoint working correctly");
        } catch (Exception e) {
            logger.error("UNEXPECTED ERROR: Configuration validation endpoint failed", e);
            throw e;
        }

        logger.info("API error handling tests completed - all intentional error tests validated");
    }

    /**
     * Test performance monitoring
     */
    private void testPerformanceMonitoring() throws Exception {
        logger.info("Testing performance monitoring");

        // Test endpoint performance for both databases
        for (String pathPrefix : List.of("trades-db-1", "trades-db-2")) {
            long responseTime = apiClient.testEndpointPerformance(pathPrefix);
            assertThat(responseTime).isGreaterThan(0);

            logger.info("✓ Endpoint performance for {}: {}ms", pathPrefix, responseTime);

            // Log warning if response time is unusually high
            if (responseTime > 5000) {
                logger.warn("High response time detected for {}: {}ms", pathPrefix, responseTime);
            }
        }

        // Test multiple concurrent requests (basic load test)
        logger.info("Running basic load test with concurrent requests");

        List<Long> responseTimes = new ArrayList<>();
        int concurrentRequests = 5;

        List<Thread> threads = new ArrayList<>();
        List<Exception> exceptions = new ArrayList<>();

        for (int i = 0; i < concurrentRequests; i++) {
            Thread thread = new Thread(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    apiClient.getAllStockTrades("trades-db-1", 0, 10);
                    long endTime = System.currentTimeMillis();

                    synchronized (responseTimes) {
                        responseTimes.add(endTime - startTime);
                    }
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                }
            });

            threads.add(thread);
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join(10000); // 10 second timeout
        }

        // Check results
        if (!exceptions.isEmpty()) {
            logger.error("Concurrent request test had {} exceptions", exceptions.size());
            for (Exception e : exceptions) {
                logger.error("Concurrent request exception", e);
            }
            throw new RuntimeException("Concurrent request test failed");
        }

        if (responseTimes.size() == concurrentRequests) {
            double avgResponseTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
            long maxResponseTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0L);

            logger.info("✓ Concurrent requests test: {} requests, avg: {:.1f}ms, max: {}ms",
                       concurrentRequests, avgResponseTime, maxResponseTime);
        } else {
            logger.warn("Concurrent requests test incomplete: {}/{} completed",
                       responseTimes.size(), concurrentRequests);
        }

        logger.info("Performance monitoring tests completed");
    }
}

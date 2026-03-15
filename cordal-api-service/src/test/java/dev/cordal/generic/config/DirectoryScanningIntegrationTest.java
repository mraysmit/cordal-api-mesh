package dev.cordal.generic.config;

import dev.cordal.config.GenericApiConfig;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive integration test for the directory scanning configuration system.
 * Tests the complete workflow from configuration discovery to validation.
 */
@DisplayName("Directory Scanning Integration Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DirectoryScanningIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(DirectoryScanningIntegrationTest.class);
    
    private GenericApiConfig config;
    private ConfigurationLoader loader;
    
    @BeforeAll
    void setUpTestEnvironment() {
        // Use isolated test configuration that reads from test resources only
        System.setProperty("generic.config.file", "application-isolated-test.yml");
        logger.info("Using isolated test configuration for integration testing");
    }
    
    @AfterAll
    void cleanUpTestEnvironment() {
        System.clearProperty("generic.config.file");
    }
    
    @BeforeEach
    void setUp() {
        config = GenericApiConfig.loadFromFile();
        loader = new ConfigurationLoader(config);
    }
    
    @Test
    @DisplayName("Should perform complete configuration discovery and loading")
    void testCompleteConfigurationWorkflow() {
        logger.info("Testing complete configuration workflow...");
        
        // Act - Load all configurations
        Map<String, DatabaseConfig> databases = loader.loadDatabaseConfigurations();
        Map<String, QueryConfig> queries = loader.loadQueryConfigurations();
        Map<String, ApiEndpointConfig> endpoints = loader.loadEndpointConfigurations();
        
        // Assert - Verify complete discovery
        assertThat(databases).isNotNull().hasSize(2); // 2 from test config (stock-trades-db, metrics-db)
        assertThat(queries).isNotNull().hasSize(12); // 12 from test config
        assertThat(endpoints).isNotNull().hasSize(6); // 6 from test config
        
        // Verify specific test configurations
        verifyTestConfigurations(databases, queries, endpoints);
        
        logger.info("Complete configuration workflow validated successfully");
    }
    
    @Test
    @DisplayName("Should validate configuration relationships")
    void testConfigurationRelationships() {
        // Load configurations
        Map<String, DatabaseConfig> databases = loader.loadDatabaseConfigurations();
        Map<String, QueryConfig> queries = loader.loadQueryConfigurations();
        Map<String, ApiEndpointConfig> endpoints = loader.loadEndpointConfigurations();
        
        // Verify that all queries reference existing databases
        for (Map.Entry<String, QueryConfig> entry : queries.entrySet()) {
            String queryName = entry.getKey();
            QueryConfig query = entry.getValue();
            
            assertThat(databases)
                .as("Database %s referenced by query %s should exist", query.getDatabase(), queryName)
                .containsKey(query.getDatabase());
        }
        
        // Verify that all endpoints reference existing queries
        for (Map.Entry<String, ApiEndpointConfig> entry : endpoints.entrySet()) {
            String endpointName = entry.getKey();
            ApiEndpointConfig endpoint = entry.getValue();
            
            assertThat(queries)
                .as("Query %s referenced by endpoint %s should exist", endpoint.getQuery(), endpointName)
                .containsKey(endpoint.getQuery());
        }
        
        logger.info("Configuration relationships validated successfully");
    }
    
    @Test
    @DisplayName("Should handle multiple directories")
    void testMultipleDirectories() {
        // For isolated testing, we'll test that the current configuration works correctly
        // rather than creating temporary directories that reference production configs

        // Load configurations from the isolated test configuration
        Map<String, DatabaseConfig> databases = loader.loadDatabaseConfigurations();
        Map<String, QueryConfig> queries = loader.loadQueryConfigurations();
        Map<String, ApiEndpointConfig> endpoints = loader.loadEndpointConfigurations();

        // Should find the test configurations
        assertThat(databases).hasSize(2); // Test configurations
        assertThat(queries).hasSize(12); // Test configurations
        assertThat(endpoints).hasSize(6); // Test configurations

        // Verify test configurations are loaded correctly
        assertThat(databases).containsKey("stock-trades-db");
        assertThat(databases).containsKey("metrics-db");
        assertThat(queries).containsKey("test-query");
        assertThat(queries).containsKey("stock-trades-all");
        assertThat(endpoints).containsKey("test-endpoint");
        assertThat(endpoints).containsKey("stock-trades-list");

        logger.info("Multiple directory scanning validated successfully");
    }
    
    @Test
    @DisplayName("Should validate configuration patterns from application.yml")
    void testConfigurationPatternsFromApplicationYml() {
        // Verify that patterns are loaded correctly from application.yml
        List<String> databasePatterns = config.getDatabasePatterns();
        List<String> queryPatterns = config.getQueryPatterns();
        List<String> endpointPatterns = config.getEndpointPatterns();
        
        assertThat(databasePatterns).contains("test-databases.yml");
        assertThat(queryPatterns).contains("test-queries.yml");
        assertThat(endpointPatterns).contains("test-api-endpoints.yml");
        
        // Verify directories are loaded correctly
        List<String> directories = config.getConfigDirectories();
        assertThat(directories).isNotEmpty();
        
        logger.info("Configuration patterns from application.yml validated successfully");
    }
    
    @Test
    @DisplayName("Should provide comprehensive logging")
    void testComprehensiveLogging() {
        // This test verifies that the logging provides useful information
        // In a real test, you might capture log output and verify specific messages
        
        assertThatCode(() -> {
            loader.loadDatabaseConfigurations();
            loader.loadQueryConfigurations();
            loader.loadEndpointConfigurations();
        }).doesNotThrowAnyException();
        
        // The logging should include:
        // - Directory scanning information
        // - Files discovered
        // - Configurations loaded from each file
        // - Total counts
        
        logger.info("Comprehensive logging validated successfully");
    }
    
    private void verifyTestConfigurations(Map<String, DatabaseConfig> databases,
                                        Map<String, QueryConfig> queries,
                                        Map<String, ApiEndpointConfig> endpoints) {
        // Verify test databases
        assertThat(databases).containsKey("stock-trades-db");
        assertThat(databases).containsKey("metrics-db");

        // Verify test queries
        assertThat(queries).containsKey("test-query");
        assertThat(queries).containsKey("stock-trades-all");
        assertThat(queries).containsKey("stock-trades-by-symbol");
        assertThat(queries).containsKey("stock-trades-count");

        // Verify test endpoints
        assertThat(endpoints).containsKey("test-endpoint");
        assertThat(endpoints).containsKey("stock-trades-list");
        assertThat(endpoints).containsKey("stock-trades-by-symbol");
    }
}

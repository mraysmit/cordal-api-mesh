package dev.cordal.generic.config;

import dev.cordal.config.GenericApiConfig;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for directory scanning configuration functionality.
 * Tests the new naming pattern-based configuration discovery system.
 *
 * IMPORTANT: This test class includes tests that INTENTIONALLY generate ERROR log messages.
 * Tests like "testHandleEmptyDirectories" are designed to validate error handling.
 * All ERROR messages from these specific tests are EXPECTED and part of the validation process.
 */
@DisplayName("Directory Scanning Configuration Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DirectoryScanningConfigurationTest {
    private static final Logger logger = LoggerFactory.getLogger(DirectoryScanningConfigurationTest.class);

    private GenericApiConfig config;
    private ConfigurationLoader loader;
    
    @BeforeAll
    void setUpTestEnvironment() throws IOException {
        // Use isolated test configuration that reads from test resources only
        System.setProperty("generic.config.file", "application-isolated-test.yml");
        logger.info("Using isolated test configuration that does not read from production generic-config");
    }
    
    @AfterAll
    void cleanUpTestEnvironment() throws IOException {
        System.clearProperty("generic.config.file");
    }
    
    @BeforeEach
    void setUp() {
        // Create configuration that uses isolated test configuration
        config = GenericApiConfig.loadFromFile();
        loader = new ConfigurationLoader(config);
    }
    
    @Test
    @DisplayName("Should discover multiple database configuration files")
    void testDiscoverMultipleDatabaseFiles() {
        // Act
        Map<String, DatabaseConfig> databases = loader.loadDatabaseConfigurations();

        // Assert
        assertThat(databases).isNotNull();
        assertThat(databases).hasSize(2); // 2 from test config (stock-trades-db, metrics-db)

        // Verify test databases
        assertThat(databases).containsKey("stock-trades-db");
        assertThat(databases).containsKey("metrics-db");

        logger.info("Successfully discovered {} database configurations", databases.size());
    }
    
    @Test
    @DisplayName("Should discover multiple query configuration files")
    void testDiscoverMultipleQueryFiles() {
        // Act
        Map<String, QueryConfig> queries = loader.loadQueryConfigurations();

        // Assert
        assertThat(queries).isNotNull();
        assertThat(queries).hasSize(12); // 12 from test config

        // Verify some test queries
        assertThat(queries).containsKey("test-query");
        assertThat(queries).containsKey("test-count-query");
        assertThat(queries).containsKey("stock-trades-all");
        assertThat(queries).containsKey("stock-trades-by-symbol");
        assertThat(queries).containsKey("stock-trades-count");

        logger.info("Successfully discovered {} query configurations", queries.size());
    }
    
    @Test
    @DisplayName("Should discover multiple endpoint configuration files")
    void testDiscoverMultipleEndpointFiles() {
        // Act
        Map<String, ApiEndpointConfig> endpoints = loader.loadEndpointConfigurations();

        // Assert
        assertThat(endpoints).isNotNull();
        assertThat(endpoints).hasSize(6); // 6 from test config

        // Verify some test endpoints
        assertThat(endpoints).containsKey("test-endpoint");
        assertThat(endpoints).containsKey("stock-trades-list");
        assertThat(endpoints).containsKey("stock-trades-by-symbol");
        assertThat(endpoints).containsKey("stock-trades-by-id");
        assertThat(endpoints).containsKey("stock-trades-by-trader");

        logger.info("Successfully discovered {} endpoint configurations", endpoints.size());
    }
    
    @Test
    @DisplayName("Should handle empty directories gracefully")
    void testHandleEmptyDirectories() throws IOException {
        logger.info("=== STARTING INTENTIONAL ERROR TEST ===");
        logger.info("TEST PURPOSE: Validating missing configuration file detection");
        logger.info("EXPECTED BEHAVIOR: ERROR logs and ConfigurationException will be generated");
        logger.info("NOTE: Any ERROR messages in this test are INTENTIONAL and EXPECTED");

        // Create empty directory
        Path emptyDir = Files.createTempDirectory("empty-config");

        try {
            // Create a simple config that points to the empty directory
            // We'll create a custom config by modifying the config object directly
            GenericApiConfig emptyConfig = new GenericApiConfig();

            // Access the config field through reflection to set the directories
            java.lang.reflect.Field configField = GenericApiConfig.class.getDeclaredField("config");
            configField.setAccessible(true);
            GenericApiConfig.ConfigPaths configPaths = (GenericApiConfig.ConfigPaths) configField.get(emptyConfig);
            configPaths.setDirectories(List.of(emptyDir.toString()));

            ConfigurationLoader emptyLoader = new ConfigurationLoader(emptyConfig);

            // Should throw ConfigurationException when no files are found
            assertThatThrownBy(() -> emptyLoader.loadDatabaseConfigurations())
                .isInstanceOf(dev.cordal.common.exception.ConfigurationException.class)
                .hasMessageContaining("No database configuration files found");

        } catch (Exception e) {
            throw new RuntimeException("Failed to set up test configuration", e);
        } finally {
            Files.deleteIfExists(emptyDir);
            logger.info("=== INTENTIONAL ERROR TEST COMPLETED SUCCESSFULLY ===");
            logger.info("TEST RESULT: Missing configuration file detection working correctly");
            logger.info("NOTE: All ERROR messages above were INTENTIONAL and part of the test validation");
        }
    }
    
    @Test
    @DisplayName("Should validate naming patterns correctly")
    void testNamingPatternValidation() {
        // Test that files matching patterns are discovered
        List<String> databasePatterns = config.getDatabasePatterns();
        List<String> queryPatterns = config.getQueryPatterns();
        List<String> endpointPatterns = config.getEndpointPatterns();

        // For isolated test configuration, we use specific test file names
        assertThat(databasePatterns).contains("test-databases.yml");
        assertThat(queryPatterns).contains("test-queries.yml");
        assertThat(endpointPatterns).contains("test-api-endpoints.yml");

        logger.info("Naming patterns validated successfully");
    }
    
}

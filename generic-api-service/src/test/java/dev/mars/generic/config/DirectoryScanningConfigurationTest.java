package dev.mars.generic.config;

import dev.mars.config.GenericApiConfig;
import dev.mars.generic.config.ApiEndpointConfig;
import dev.mars.generic.config.DatabaseConfig;
import dev.mars.generic.config.QueryConfig;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for directory scanning configuration functionality.
 * Tests the new naming pattern-based configuration discovery system.
 */
@DisplayName("Directory Scanning Configuration Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DirectoryScanningConfigurationTest {
    private static final Logger logger = LoggerFactory.getLogger(DirectoryScanningConfigurationTest.class);
    
    private Path testConfigDir;
    private GenericApiConfig config;
    private ConfigurationLoader loader;
    
    @BeforeAll
    void setUpTestEnvironment() throws IOException {
        // Create temporary test directory structure
        testConfigDir = Files.createTempDirectory("test-config-scanning");
        logger.info("Created test directory: {}", testConfigDir);
        
        // Create test configuration files
        createTestConfigurationFiles();
        
        // Set up test application.yml that points to our test directory
        System.setProperty("generic.config.file", "application-directory-scanning-test.yml");
    }
    
    @AfterAll
    void cleanUpTestEnvironment() throws IOException {
        // Clean up temporary directory
        if (testConfigDir != null && Files.exists(testConfigDir)) {
            Files.walk(testConfigDir)
                .map(Path::toFile)
                .forEach(File::delete);
            Files.deleteIfExists(testConfigDir);
        }
        System.clearProperty("generic.config.file");
    }
    
    @BeforeEach
    void setUp() {
        // Create configuration that uses directory scanning
        config = createTestConfig();
        loader = new ConfigurationLoader(config);
    }
    
    @Test
    @DisplayName("Should discover multiple database configuration files")
    void testDiscoverMultipleDatabaseFiles() {
        // Act
        Map<String, DatabaseConfig> databases = loader.loadDatabaseConfigurations();
        
        // Assert
        assertThat(databases).isNotNull();
        assertThat(databases).hasSize(3); // 3 from production config (analytics, datawarehouse, stocktrades)
        
        // Verify production databases
        assertThat(databases).containsKey("analytics");
        assertThat(databases).containsKey("datawarehouse");
        assertThat(databases).containsKey("stocktrades");
        
        logger.info("Successfully discovered {} database configurations", databases.size());
    }
    
    @Test
    @DisplayName("Should discover multiple query configuration files")
    void testDiscoverMultipleQueryFiles() {
        // Act
        Map<String, QueryConfig> queries = loader.loadQueryConfigurations();
        
        // Assert
        assertThat(queries).isNotNull();
        assertThat(queries).hasSize(12); // 12 from production config (3 analytics + 9 stocktrades)
        
        // Verify some production queries
        assertThat(queries).containsKey("daily-trading-volume");
        assertThat(queries).containsKey("top-performers");
        assertThat(queries).containsKey("market-summary");
        assertThat(queries).containsKey("stock-trades-all");
        assertThat(queries).containsKey("stock-trades-by-symbol");
        
        logger.info("Successfully discovered {} query configurations", queries.size());
    }
    
    @Test
    @DisplayName("Should discover multiple endpoint configuration files")
    void testDiscoverMultipleEndpointFiles() {
        // Act
        Map<String, ApiEndpointConfig> endpoints = loader.loadEndpointConfigurations();
        
        // Assert
        assertThat(endpoints).isNotNull();
        assertThat(endpoints).hasSize(8); // 8 from production config (3 analytics + 5 stocktrades)
        
        // Verify some production endpoints
        assertThat(endpoints).containsKey("analytics-daily-volume");
        assertThat(endpoints).containsKey("analytics-top-performers");
        assertThat(endpoints).containsKey("analytics-market-summary");
        assertThat(endpoints).containsKey("stock-trades-list");
        assertThat(endpoints).containsKey("stock-trades-by-symbol");
        
        logger.info("Successfully discovered {} endpoint configurations", endpoints.size());
    }
    
    @Test
    @DisplayName("Should handle empty directories gracefully")
    void testHandleEmptyDirectories() throws IOException {
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
                .isInstanceOf(dev.mars.common.exception.ConfigurationException.class)
                .hasMessageContaining("No database configuration files found");

        } catch (Exception e) {
            throw new RuntimeException("Failed to set up test configuration", e);
        } finally {
            Files.deleteIfExists(emptyDir);
        }
    }
    
    @Test
    @DisplayName("Should validate naming patterns correctly")
    void testNamingPatternValidation() {
        // Test that files matching patterns are discovered
        List<String> databasePatterns = config.getDatabasePatterns();
        List<String> queryPatterns = config.getQueryPatterns();
        List<String> endpointPatterns = config.getEndpointPatterns();
        
        assertThat(databasePatterns).contains("*-database.yml", "*-databases.yml");
        assertThat(queryPatterns).contains("*-query.yml", "*-queries.yml");
        assertThat(endpointPatterns).contains("*-endpoint.yml", "*-endpoints.yml", "*-api.yml");
        
        logger.info("Naming patterns validated successfully");
    }
    
    private void createTestConfigurationFiles() throws IOException {
        // Create test database files
        createTestDatabaseFile1();
        createTestDatabaseFile2();
        
        // Create test query files
        createTestQueryFile1();
        createTestQueryFile2();
        
        // Create test endpoint files
        createTestEndpointFile1();
        createTestEndpointFile2();
    }
    
    private void createTestDatabaseFile1() throws IOException {
        String content = """
            databases:
              test-db-1:
                name: "test-db-1"
                description: "Test database 1"
                url: "jdbc:h2:mem:testdb1"
                username: "sa"
                password: ""
                driver: "org.h2.Driver"
              test-db-2:
                name: "test-db-2"
                description: "Test database 2"
                url: "jdbc:h2:mem:testdb2"
                username: "sa"
                password: ""
                driver: "org.h2.Driver"
            """;
        Files.writeString(testConfigDir.resolve("test1-databases.yml"), content);
    }
    
    private void createTestDatabaseFile2() throws IOException {
        String content = """
            databases:
              analytics-db:
                name: "analytics-db"
                description: "Analytics database"
                url: "jdbc:h2:mem:analytics"
                username: "sa"
                password: ""
                driver: "org.h2.Driver"
              reporting-db:
                name: "reporting-db"
                description: "Reporting database"
                url: "jdbc:h2:mem:reporting"
                username: "sa"
                password: ""
                driver: "org.h2.Driver"
            """;
        Files.writeString(testConfigDir.resolve("analytics-database.yml"), content);
    }
    
    private void createTestQueryFile1() throws IOException {
        String content = """
            queries:
              test-query-1:
                name: "Test Query 1"
                description: "First test query"
                database: "test-db-1"
                sql: "SELECT * FROM test_table_1"
              test-query-2:
                name: "Test Query 2"
                description: "Second test query"
                database: "test-db-2"
                sql: "SELECT * FROM test_table_2"
            """;
        Files.writeString(testConfigDir.resolve("test1-queries.yml"), content);
    }
    
    private void createTestQueryFile2() throws IOException {
        String content = """
            queries:
              analytics-query:
                name: "Analytics Query"
                description: "Analytics query"
                database: "analytics-db"
                sql: "SELECT * FROM analytics_table"
              reporting-query:
                name: "Reporting Query"
                description: "Reporting query"
                database: "reporting-db"
                sql: "SELECT * FROM reporting_table"
            """;
        Files.writeString(testConfigDir.resolve("analytics-query.yml"), content);
    }
    
    private void createTestEndpointFile1() throws IOException {
        String content = """
            endpoints:
              test-endpoint-1:
                description: "Test endpoint 1"
                method: "GET"
                path: "/api/test/endpoint1"
                query: "test-query-1"
              test-endpoint-2:
                description: "Test endpoint 2"
                method: "GET"
                path: "/api/test/endpoint2"
                query: "test-query-2"
            """;
        Files.writeString(testConfigDir.resolve("test1-endpoints.yml"), content);
    }
    
    private void createTestEndpointFile2() throws IOException {
        String content = """
            endpoints:
              analytics-endpoint:
                description: "Analytics endpoint"
                method: "GET"
                path: "/api/analytics/data"
                query: "analytics-query"
              reporting-endpoint:
                description: "Reporting endpoint"
                method: "GET"
                path: "/api/reporting/data"
                query: "reporting-query"
            """;
        Files.writeString(testConfigDir.resolve("analytics-api.yml"), content);
    }
    
    private GenericApiConfig createTestConfig() {
        return createConfigWithDirectory(testConfigDir.toString());
    }

    private GenericApiConfig createConfigWithDirectory(String directory) {
        // Create a test config that uses directory scanning
        // We'll need to create a test application.yml that points to our test directory
        System.setProperty("test.config.directory", directory);

        GenericApiConfig testConfig = GenericApiConfig.loadFromFile();
        return testConfig;
    }
}

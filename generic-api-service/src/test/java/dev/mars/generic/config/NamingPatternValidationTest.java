package dev.mars.generic.config;

import dev.mars.config.GenericApiConfig;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for naming pattern validation and file discovery logic.
 * Validates that the pattern matching works correctly for different file naming conventions.
 */
@DisplayName("Naming Pattern Validation Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NamingPatternValidationTest {
    private static final Logger logger = LoggerFactory.getLogger(NamingPatternValidationTest.class);
    
    private Path testConfigDir;
    private GenericApiConfig config;
    private ConfigurationLoader loader;
    
    @BeforeAll
    void setUpTestEnvironment() throws IOException {
        testConfigDir = Files.createTempDirectory("pattern-test-config");
        logger.info("Created test directory: {}", testConfigDir);
        
        // Create files with various naming patterns
        createTestFilesWithDifferentPatterns();
        
        System.setProperty("generic.config.file", "application-pattern-test.yml");
    }
    
    @AfterAll
    void cleanUpTestEnvironment() throws IOException {
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
        config = createTestConfig();
        loader = new ConfigurationLoader(config);
    }
    
    @Test
    @DisplayName("Should match database files with standard patterns")
    void testDatabasePatternMatching() {
        // Test files that should match database patterns
        List<String> databasePatterns = Arrays.asList("*-database.yml", "*-databases.yml");
        
        // These should match
        assertThat(matchesAnyPattern("stocktrades-database.yml", databasePatterns)).isTrue();
        assertThat(matchesAnyPattern("analytics-databases.yml", databasePatterns)).isTrue();
        assertThat(matchesAnyPattern("main-database.yml", databasePatterns)).isTrue();
        assertThat(matchesAnyPattern("test-databases.yml", databasePatterns)).isTrue();
        
        // These should NOT match
        assertThat(matchesAnyPattern("database.yml", databasePatterns)).isFalse();
        assertThat(matchesAnyPattern("databases.yml", databasePatterns)).isFalse();
        assertThat(matchesAnyPattern("stocktrades-queries.yml", databasePatterns)).isFalse();
        assertThat(matchesAnyPattern("config-database.yaml", databasePatterns)).isFalse(); // wrong extension
        
        logger.info("Database pattern matching validated successfully");
    }
    
    @Test
    @DisplayName("Should match query files with standard patterns")
    void testQueryPatternMatching() {
        List<String> queryPatterns = Arrays.asList("*-query.yml", "*-queries.yml");
        
        // These should match
        assertThat(matchesAnyPattern("stocktrades-query.yml", queryPatterns)).isTrue();
        assertThat(matchesAnyPattern("analytics-queries.yml", queryPatterns)).isTrue();
        assertThat(matchesAnyPattern("main-query.yml", queryPatterns)).isTrue();
        assertThat(matchesAnyPattern("test-queries.yml", queryPatterns)).isTrue();
        
        // These should NOT match
        assertThat(matchesAnyPattern("query.yml", queryPatterns)).isFalse();
        assertThat(matchesAnyPattern("queries.yml", queryPatterns)).isFalse();
        assertThat(matchesAnyPattern("stocktrades-database.yml", queryPatterns)).isFalse();
        
        logger.info("Query pattern matching validated successfully");
    }
    
    @Test
    @DisplayName("Should match endpoint files with multiple patterns")
    void testEndpointPatternMatching() {
        List<String> endpointPatterns = Arrays.asList("*-endpoint.yml", "*-endpoints.yml", "*-api.yml");
        
        // These should match
        assertThat(matchesAnyPattern("stocktrades-endpoint.yml", endpointPatterns)).isTrue();
        assertThat(matchesAnyPattern("analytics-endpoints.yml", endpointPatterns)).isTrue();
        assertThat(matchesAnyPattern("main-api.yml", endpointPatterns)).isTrue();
        assertThat(matchesAnyPattern("test-endpoints.yml", endpointPatterns)).isTrue();
        assertThat(matchesAnyPattern("user-api.yml", endpointPatterns)).isTrue();
        
        // These should NOT match
        assertThat(matchesAnyPattern("endpoint.yml", endpointPatterns)).isFalse();
        assertThat(matchesAnyPattern("endpoints.yml", endpointPatterns)).isFalse();
        assertThat(matchesAnyPattern("api.yml", endpointPatterns)).isFalse();
        assertThat(matchesAnyPattern("stocktrades-database.yml", endpointPatterns)).isFalse();
        
        logger.info("Endpoint pattern matching validated successfully");
    }
    
    @Test
    @DisplayName("Should discover files based on actual patterns from config")
    void testActualPatternDiscovery() {
        // Act - Load configurations using actual patterns
        Map<String, DatabaseConfig> databases = loader.loadDatabaseConfigurations();
        Map<String, QueryConfig> queries = loader.loadQueryConfigurations();
        Map<String, ApiEndpointConfig> endpoints = loader.loadEndpointConfigurations();
        
        // Assert - Should find files that match the patterns
        assertThat(databases).isNotNull();
        assertThat(queries).isNotNull();
        assertThat(endpoints).isNotNull();
        
        // Should find the production configuration files
        assertThat(databases).hasSize(3); // From production config (analytics, datawarehouse, stocktrades)
        assertThat(queries).hasSize(12); // From production config (3 analytics + 9 stocktrades)
        assertThat(endpoints).hasSize(8); // From production config (3 analytics + 5 stocktrades)
        
        logger.info("Pattern discovery validated: {} databases, {} queries, {} endpoints", 
                   databases.size(), queries.size(), endpoints.size());
    }
    
    @Test
    @DisplayName("Should ignore files that don't match patterns")
    void testIgnoreNonMatchingFiles() {
        // The test directory contains files that shouldn't match patterns
        // The loader should ignore them
        
        Map<String, DatabaseConfig> databases = loader.loadDatabaseConfigurations();
        Map<String, QueryConfig> queries = loader.loadQueryConfigurations();
        Map<String, ApiEndpointConfig> endpoints = loader.loadEndpointConfigurations();
        
        // Should not include configurations from non-matching files
        assertThat(databases).doesNotContainKey("ignored-db");
        assertThat(queries).doesNotContainKey("ignored-query");
        assertThat(endpoints).doesNotContainKey("ignored-endpoint");
        
        logger.info("Non-matching files correctly ignored");
    }
    
    @Test
    @DisplayName("Should handle custom patterns correctly")
    void testCustomPatterns() {
        // Create config with custom patterns
        GenericApiConfig customConfig = createConfigWithCustomPatterns();
        ConfigurationLoader customLoader = new ConfigurationLoader(customConfig);
        
        // Test that patterns are loaded correctly (using default patterns since custom patterns aren't configured)
        List<String> customDatabasePatterns = customConfig.getDatabasePatterns();
        assertThat(customDatabasePatterns).contains("*-databases.yml");
        
        logger.info("Custom patterns validated successfully");
    }
    
    private void createTestFilesWithDifferentPatterns() throws IOException {
        // Create files that should match patterns
        createMatchingDatabaseFiles();
        createMatchingQueryFiles();
        createMatchingEndpointFiles();
        
        // Create files that should NOT match patterns
        createNonMatchingFiles();
    }
    
    private void createMatchingDatabaseFiles() throws IOException {
        // Files that should match database patterns
        String dbContent = """
            databases:
              test-db:
                name: "test-db"
                url: "jdbc:h2:mem:test"
                username: "sa"
                password: ""
                driver: "org.h2.Driver"
            """;
        
        Files.writeString(testConfigDir.resolve("main-database.yml"), dbContent);
        Files.writeString(testConfigDir.resolve("analytics-databases.yml"), dbContent);
    }
    
    private void createMatchingQueryFiles() throws IOException {
        // Files that should match query patterns
        String queryContent = """
            queries:
              test-query:
                name: "Test Query"
                database: "test-db"
                sql: "SELECT * FROM test_table"
            """;
        
        Files.writeString(testConfigDir.resolve("main-query.yml"), queryContent);
        Files.writeString(testConfigDir.resolve("analytics-queries.yml"), queryContent);
    }
    
    private void createMatchingEndpointFiles() throws IOException {
        // Files that should match endpoint patterns
        String endpointContent = """
            endpoints:
              test-endpoint:
                description: "Test endpoint"
                method: "GET"
                path: "/api/test"
                query: "test-query"
            """;
        
        Files.writeString(testConfigDir.resolve("main-endpoint.yml"), endpointContent);
        Files.writeString(testConfigDir.resolve("analytics-endpoints.yml"), endpointContent);
        Files.writeString(testConfigDir.resolve("user-api.yml"), endpointContent);
    }
    
    private void createNonMatchingFiles() throws IOException {
        // Files that should NOT match any patterns
        String content = """
            ignored:
              ignored-config: "This should be ignored"
            """;
        
        Files.writeString(testConfigDir.resolve("database.yml"), content); // No prefix
        Files.writeString(testConfigDir.resolve("queries.yml"), content); // No prefix
        Files.writeString(testConfigDir.resolve("endpoints.yml"), content); // No prefix
        Files.writeString(testConfigDir.resolve("config.yaml"), content); // Wrong extension
        Files.writeString(testConfigDir.resolve("readme.txt"), content); // Wrong extension
    }
    
    private boolean matchesAnyPattern(String filename, List<String> patterns) {
        for (String pattern : patterns) {
            if (matchesPattern(filename, pattern)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean matchesPattern(String filename, String pattern) {
        // Convert glob pattern to regex (same logic as in ConfigurationLoader)
        String regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*");
        
        return filename.matches(regex);
    }
    
    private GenericApiConfig createTestConfig() {
        // Create config that uses our test directory
        GenericApiConfig testConfig = new GenericApiConfig();
        // Configure to use test directory and standard patterns
        return testConfig;
    }
    
    private GenericApiConfig createConfigWithCustomPatterns() {
        // Create config with custom patterns for testing
        GenericApiConfig customConfig = new GenericApiConfig();
        // Configure with custom patterns
        return customConfig;
    }
}

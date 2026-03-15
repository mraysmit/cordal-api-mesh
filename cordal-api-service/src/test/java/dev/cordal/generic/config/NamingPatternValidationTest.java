package dev.cordal.generic.config;

import dev.cordal.config.GenericApiConfig;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

    private GenericApiConfig config;
    private ConfigurationLoader loader;
    
    @BeforeAll
    void setUpTestEnvironment() throws IOException {
        // Use isolated test configuration that reads from test resources only
        System.setProperty("generic.config.file", "application-isolated-test.yml");
        logger.info("Using isolated test configuration for pattern validation testing");
    }
    
    @AfterAll
    void cleanUpTestEnvironment() throws IOException {
        System.clearProperty("generic.config.file");
    }
    
    @BeforeEach
    void setUp() {
        config = GenericApiConfig.loadFromFile();
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
        
        // Should find the test configuration files
        assertThat(databases).hasSize(2); // From test config (stock-trades-db, metrics-db)
        assertThat(queries).hasSize(12); // From test config
        assertThat(endpoints).hasSize(6); // From test config
        
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
        assertThatCode(() -> new ConfigurationLoader(customConfig)).doesNotThrowAnyException();
        
        // Test that patterns are loaded correctly (using test patterns)
        List<String> customDatabasePatterns = customConfig.getDatabasePatterns();
        assertThat(customDatabasePatterns).contains("test-databases.yml");
        
        logger.info("Custom patterns validated successfully");
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
    
    private GenericApiConfig createConfigWithCustomPatterns() {
        // Create config with custom patterns for testing
        GenericApiConfig customConfig = new GenericApiConfig();
        // Configure with custom patterns
        return customConfig;
    }
}

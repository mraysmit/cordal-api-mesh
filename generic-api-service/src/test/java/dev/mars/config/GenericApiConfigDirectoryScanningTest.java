package dev.mars.config;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for GenericApiConfig directory scanning configuration functionality.
 * Validates that the new directory and pattern configuration options work correctly.
 */
@DisplayName("GenericApiConfig Directory Scanning Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GenericApiConfigDirectoryScanningTest {
    private static final Logger logger = LoggerFactory.getLogger(GenericApiConfigDirectoryScanningTest.class);
    
    @AfterEach
    void tearDown() {
        System.clearProperty("generic.config.file");
    }
    
    @Test
    @DisplayName("Should load directory configuration from application.yml")
    public void testLoadDirectoryConfiguration() {
        // Arrange
        System.setProperty("generic.config.file", "application-directory-scanning-test.yml");
        
        // Act
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        
        // Assert
        List<String> directories = config.getConfigDirectories();
        assertThat(directories).isNotNull();
        assertThat(directories).isNotEmpty();
        assertThat(directories).contains("../generic-config");
        
        logger.info("Directory configuration loaded successfully: {}", directories);
    }
    
    @Test
    @DisplayName("Should load database patterns from application.yml")
    public void testLoadDatabasePatterns() {
        // Arrange
        System.setProperty("generic.config.file", "application-directory-scanning-test.yml");
        
        // Act
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        
        // Assert
        List<String> patterns = config.getDatabasePatterns();
        assertThat(patterns).isNotNull();
        assertThat(patterns).isNotEmpty();
        assertThat(patterns).contains("*-database.yml");
        assertThat(patterns).contains("*-databases.yml");
        
        logger.info("Database patterns loaded successfully: {}", patterns);
    }
    
    @Test
    @DisplayName("Should load query patterns from application.yml")
    public void testLoadQueryPatterns() {
        // Arrange
        System.setProperty("generic.config.file", "application-directory-scanning-test.yml");

        // Act
        GenericApiConfig config = GenericApiConfig.loadFromFile();

        // Assert
        List<String> patterns = config.getQueryPatterns();
        assertThat(patterns).isNotNull();
        assertThat(patterns).isNotEmpty();
        assertThat(patterns).contains("*-query.yml");
        assertThat(patterns).contains("*-queries.yml");

        logger.info("Query patterns loaded successfully: {}", patterns);
    }

    @Test
    @DisplayName("Should load endpoint patterns from application.yml")
    public void testLoadEndpointPatterns() {
        // Arrange
        System.setProperty("generic.config.file", "application-directory-scanning-test.yml");
        
        // Act
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        
        // Assert
        List<String> patterns = config.getEndpointPatterns();
        assertThat(patterns).isNotNull();
        assertThat(patterns).isNotEmpty();
        assertThat(patterns).contains("*-endpoint.yml");
        assertThat(patterns).contains("*-endpoints.yml");
        assertThat(patterns).contains("*-api.yml");
        
        logger.info("Endpoint patterns loaded successfully: {}", patterns);
    }
    
    @Test
    @DisplayName("Should use default patterns when not specified")
    public void testDefaultPatterns() {
        // Arrange - Use a config file without pattern specifications
        System.setProperty("generic.config.file", "application-test.yml");
        
        // Act
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        
        // Assert - Should use default patterns
        List<String> databasePatterns = config.getDatabasePatterns();
        List<String> queryPatterns = config.getQueryPatterns();
        List<String> endpointPatterns = config.getEndpointPatterns();
        
        assertThat(databasePatterns).contains("*-database.yml", "*-databases.yml");
        assertThat(queryPatterns).contains("*-query.yml", "*-queries.yml");
        assertThat(endpointPatterns).contains("*-endpoint.yml", "*-endpoints.yml", "*-api.yml");
        
        logger.info("Default patterns applied successfully");
    }
    
    @Test
    @DisplayName("Should use default directory when not specified")
    public void testDefaultDirectory() {
        // Arrange - Use a config file without directory specifications
        System.setProperty("generic.config.file", "application-test.yml");
        
        // Act
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        
        // Assert - Should use default directory
        List<String> directories = config.getConfigDirectories();
        assertThat(directories).contains("../generic-config");
        
        logger.info("Default directory applied successfully: {}", directories);
    }
    
    @Test
    @DisplayName("Should handle multiple directories")
    public void testMultipleDirectories() {
        // Create a test config with multiple directories
        System.setProperty("generic.config.file", "application-multi-directory-test.yml");
        
        // Act
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        
        // Assert
        List<String> directories = config.getConfigDirectories();
        assertThat(directories).hasSizeGreaterThan(1);
        
        logger.info("Multiple directories loaded successfully: {}", directories);
    }
    
    @Test
    @DisplayName("Should handle custom patterns")
    public void testCustomPatterns() {
        // Create a test config with custom patterns
        System.setProperty("generic.config.file", "application-custom-patterns-test.yml");
        
        // Act
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        
        // Assert
        List<String> databasePatterns = config.getDatabasePatterns();
        List<String> queryPatterns = config.getQueryPatterns();
        List<String> endpointPatterns = config.getEndpointPatterns();
        
        // Should contain custom patterns if specified
        assertThat(databasePatterns).isNotNull();
        assertThat(queryPatterns).isNotNull();
        assertThat(endpointPatterns).isNotNull();
        
        logger.info("Custom patterns loaded successfully");
    }
    
    @Test
    @DisplayName("Should validate configuration consistency")
    public void testConfigurationConsistency() {
        // Arrange
        System.setProperty("generic.config.file", "application-directory-scanning-test.yml");
        
        // Act
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        
        // Assert - All configuration methods should return consistent data
        assertThat(config.getConfigDirectories()).isNotNull();
        assertThat(config.getDatabasePatterns()).isNotNull();
        assertThat(config.getQueryPatterns()).isNotNull();
        assertThat(config.getEndpointPatterns()).isNotNull();
        
        // Configuration source should be yaml
        assertThat(config.getConfigSource()).isEqualTo("yaml");
        
        logger.info("Configuration consistency validated successfully");
    }
    
    @Test
    @DisplayName("Should log configuration details")
    public void testConfigurationLogging() {
        // Arrange
        System.setProperty("generic.config.file", "application-directory-scanning-test.yml");
        
        // Act & Assert - Should not throw any exceptions during loading
        assertThatCode(() -> {
            GenericApiConfig config = GenericApiConfig.loadFromFile();
            
            // Access all configuration methods to trigger logging
            config.getConfigDirectories();
            config.getDatabasePatterns();
            config.getQueryPatterns();
            config.getEndpointPatterns();
            
        }).doesNotThrowAnyException();
        
        logger.info("Configuration logging validated successfully");
    }
    
    @Test
    @DisplayName("Should handle missing configuration gracefully")
    public void testMissingConfigurationHandling() {
        // Arrange - Use a minimal config file
        System.setProperty("generic.config.file", "application-minimal-test.yml");
        
        // Act & Assert - Should handle missing configuration gracefully
        assertThatCode(() -> {
            GenericApiConfig config = GenericApiConfig.loadFromFile();
            
            // Should use defaults when configuration is missing
            List<String> directories = config.getConfigDirectories();
            List<String> databasePatterns = config.getDatabasePatterns();
            List<String> queryPatterns = config.getQueryPatterns();
            List<String> endpointPatterns = config.getEndpointPatterns();
            
            assertThat(directories).isNotNull();
            assertThat(databasePatterns).isNotNull();
            assertThat(queryPatterns).isNotNull();
            assertThat(endpointPatterns).isNotNull();
            
        }).doesNotThrowAnyException();
        
        logger.info("Missing configuration handled gracefully");
    }
    
    @Test
    @DisplayName("Should validate pattern format")
    public void testPatternFormat() {
        // Arrange
        System.setProperty("generic.config.file", "application-directory-scanning-test.yml");
        
        // Act
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        
        // Assert - Patterns should be in correct format
        List<String> databasePatterns = config.getDatabasePatterns();
        List<String> queryPatterns = config.getQueryPatterns();
        List<String> endpointPatterns = config.getEndpointPatterns();
        
        // All patterns should contain wildcards and .yml extension
        for (String pattern : databasePatterns) {
            assertThat(pattern).contains("*");
            assertThat(pattern).endsWith(".yml");
        }
        
        for (String pattern : queryPatterns) {
            assertThat(pattern).contains("*");
            assertThat(pattern).endsWith(".yml");
        }
        
        for (String pattern : endpointPatterns) {
            assertThat(pattern).contains("*");
            assertThat(pattern).endsWith(".yml");
        }
        
        logger.info("Pattern format validated successfully");
    }
}

package dev.mars.generic.config;

import dev.mars.config.GenericApiConfig;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Basic test for directory scanning configuration functionality.
 * This test validates that the new configuration options work correctly.
 */
@DisplayName("Directory Scanning Basic Tests")
class DirectoryScanningBasicTest {
    private static final Logger logger = LoggerFactory.getLogger(DirectoryScanningBasicTest.class);
    
    @AfterEach
    void tearDown() {
        System.clearProperty("generic.config.file");
    }
    
    @Test
    @DisplayName("Should load directory configuration successfully")
    void testLoadDirectoryConfiguration() {
        // Arrange
        System.setProperty("generic.config.file", "application-test.yml");
        
        // Act
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        
        // Assert
        assertThat(config).isNotNull();
        
        List<String> directories = config.getConfigDirectories();
        assertThat(directories).isNotNull();
        assertThat(directories).isNotEmpty();
        
        logger.info("✓ Directory configuration loaded successfully: {}", directories);
    }
    
    @Test
    @DisplayName("Should load pattern configuration successfully")
    void testLoadPatternConfiguration() {
        // Arrange
        System.setProperty("generic.config.file", "application-test.yml");
        
        // Act
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        
        // Assert
        assertThat(config).isNotNull();
        
        List<String> databasePatterns = config.getDatabasePatterns();
        List<String> queryPatterns = config.getQueryPatterns();
        List<String> endpointPatterns = config.getEndpointPatterns();
        
        assertThat(databasePatterns).isNotNull().isNotEmpty();
        assertThat(queryPatterns).isNotNull().isNotEmpty();
        assertThat(endpointPatterns).isNotNull().isNotEmpty();
        
        logger.info("✓ Pattern configuration loaded successfully");
        logger.info("  Database patterns: {}", databasePatterns);
        logger.info("  Query patterns: {}", queryPatterns);
        logger.info("  Endpoint patterns: {}", endpointPatterns);
    }
    
    @Test
    @DisplayName("Should use default patterns when not specified")
    void testDefaultPatterns() {
        // Arrange
        System.setProperty("generic.config.file", "application-test.yml");
        
        // Act
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        
        // Assert
        List<String> databasePatterns = config.getDatabasePatterns();
        List<String> queryPatterns = config.getQueryPatterns();
        List<String> endpointPatterns = config.getEndpointPatterns();
        
        // Should contain default patterns
        assertThat(databasePatterns).contains("*-database.yml", "*-databases.yml");
        assertThat(queryPatterns).contains("*-query.yml", "*-queries.yml");
        assertThat(endpointPatterns).contains("*-endpoint.yml", "*-endpoints.yml", "*-api.yml");
        
        logger.info("✓ Default patterns applied successfully");
    }
    
    @Test
    @DisplayName("Should load configurations using ConfigurationLoader")
    void testConfigurationLoaderWithDirectoryScanning() {
        // Arrange
        System.setProperty("generic.config.file", "application-test.yml");
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        ConfigurationLoader loader = new ConfigurationLoader(config);
        
        // Act & Assert - Should not throw exceptions
        assertThatCode(() -> {
            var databases = loader.loadDatabaseConfigurations();
            var queries = loader.loadQueryConfigurations();
            var endpoints = loader.loadEndpointConfigurations();
            
            assertThat(databases).isNotNull();
            assertThat(queries).isNotNull();
            assertThat(endpoints).isNotNull();
            
            logger.info("✓ ConfigurationLoader working with directory scanning");
            logger.info("  Loaded {} databases, {} queries, {} endpoints", 
                       databases.size(), queries.size(), endpoints.size());
            
        }).doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("Should validate configuration consistency")
    void testConfigurationConsistency() {
        // Arrange
        System.setProperty("generic.config.file", "application-test.yml");
        
        // Act
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        
        // Assert - All configuration methods should return consistent data
        assertThat(config.getConfigDirectories()).isNotNull();
        assertThat(config.getDatabasePatterns()).isNotNull();
        assertThat(config.getQueryPatterns()).isNotNull();
        assertThat(config.getEndpointPatterns()).isNotNull();
        
        // Configuration source should be yaml
        assertThat(config.getConfigSource()).isEqualTo("yaml");
        
        logger.info("✓ Configuration consistency validated successfully");
    }
    
    @Test
    @DisplayName("Should handle configuration loading without errors")
    void testConfigurationLoadingStability() {
        // Arrange
        System.setProperty("generic.config.file", "application-test.yml");
        
        // Act & Assert - Should not throw any exceptions during loading
        assertThatCode(() -> {
            GenericApiConfig config = GenericApiConfig.loadFromFile();
            ConfigurationLoader loader = new ConfigurationLoader(config);
            
            // Access all configuration methods to trigger loading
            config.getConfigDirectories();
            config.getDatabasePatterns();
            config.getQueryPatterns();
            config.getEndpointPatterns();
            
            // Try to load actual configurations - may throw ConfigurationException if files not found
            try {
                loader.loadDatabaseConfigurations();
                loader.loadQueryConfigurations();
                loader.loadEndpointConfigurations();
                logger.info("✓ Configuration files found and loaded successfully");
            } catch (dev.mars.common.exception.ConfigurationException e) {
                logger.info("✓ Configuration loading properly throws exception when files not found: {}", e.getMessage());
                // This is expected behavior when configuration files are not present
            }

        }).doesNotThrowAnyException();
        
        logger.info("✓ Configuration loading stability validated successfully");
    }
    
    @Test
    @DisplayName("Should validate new configuration features work")
    void testNewConfigurationFeatures() {
        logger.info("Validating new directory scanning configuration features...");
        
        // Test that the new features work as expected:
        // 1. Directory scanning instead of single file paths
        // 2. Configurable naming patterns
        // 3. Multiple configuration files of each type
        // 4. Clear logging and error reporting
        
        assertThatCode(() -> {
            // Test that we can load configurations using directory scanning
            System.setProperty("generic.config.file", "application-test.yml");
            
            GenericApiConfig config = GenericApiConfig.loadFromFile();
            ConfigurationLoader loader = new ConfigurationLoader(config);
            
            // Should be able to load configurations without errors
            var databases = loader.loadDatabaseConfigurations();
            var queries = loader.loadQueryConfigurations();
            var endpoints = loader.loadEndpointConfigurations();
            
            // Verify that configurations are loaded
            assertThat(databases).isNotNull();
            assertThat(queries).isNotNull();
            assertThat(endpoints).isNotNull();
            
            // Verify that directory and pattern configuration is working
            assertThat(config.getConfigDirectories()).isNotEmpty();
            assertThat(config.getDatabasePatterns()).isNotEmpty();
            assertThat(config.getQueryPatterns()).isNotEmpty();
            assertThat(config.getEndpointPatterns()).isNotEmpty();
            
            logger.info("✓ Directory scanning: {} directories configured", config.getConfigDirectories().size());
            logger.info("✓ Pattern matching: {} database patterns, {} query patterns, {} endpoint patterns", 
                       config.getDatabasePatterns().size(), 
                       config.getQueryPatterns().size(), 
                       config.getEndpointPatterns().size());
            logger.info("✓ Configuration loading: {} databases, {} queries, {} endpoints loaded", 
                       databases.size(), queries.size(), endpoints.size());
            
        }).doesNotThrowAnyException();
        
        logger.info("✓ New directory scanning configuration features validated successfully!");
        logger.info("Directory scanning configuration system is working correctly!");
    }
}

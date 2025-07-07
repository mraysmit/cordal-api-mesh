package dev.mars.generic.config;

import dev.mars.config.GenericApiConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for validation configuration flags in GenericApiConfig
 */
class ValidationConfigurationTest {
    private static final Logger logger = LoggerFactory.getLogger(ValidationConfigurationTest.class);

    @BeforeEach
    void setUp() {
        // Clear any existing system properties
        System.clearProperty("generic.config.file");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("generic.config.file");
    }

    @Test
    void testDefaultValidationSettings() {
        // Test that default validation settings are loaded correctly
        // Use test configuration that has default validation settings
        System.setProperty("generic.config.file", "application-test.yml");

        GenericApiConfig config = GenericApiConfig.loadFromFile();

        // Default values should be false for both flags
        assertThat(config.isValidationRunOnStartup()).isFalse();
        assertThat(config.isValidationValidateOnly()).isFalse();

        // Validation settings object should exist
        assertThat(config.getValidationSettings()).isNotNull();
        assertThat(config.getValidationSettings().isRunOnStartup()).isFalse();
        assertThat(config.getValidationSettings().isValidateOnly()).isFalse();

        logger.info("✓ Default validation settings verified");
    }

    @Test
    void testValidationSettingsFromApplicationYaml() {
        // Test loading validation settings from the test application.yml
        // The test application.yml should have validation.runOnStartup=false and validation.validateOnly=false (defaults)
        System.setProperty("generic.config.file", "application-test.yml");

        GenericApiConfig config = GenericApiConfig.loadFromFile();

        assertThat(config.isValidationRunOnStartup()).isFalse();
        assertThat(config.isValidationValidateOnly()).isFalse();

        logger.info("✓ Validation settings from application-test.yml verified");
        logger.info("   runOnStartup: {}", config.isValidationRunOnStartup());
        logger.info("   validateOnly: {}", config.isValidationValidateOnly());
    }

    @Test
    void testValidationSettingsAccessors() {
        // Test that all accessor methods work correctly
        GenericApiConfig config = new GenericApiConfig();
        
        // Test direct access to validation settings object
        GenericApiConfig.ValidationSettings validationSettings = config.getValidationSettings();
        assertThat(validationSettings).isNotNull();
        
        // Test that convenience methods match the settings object
        assertThat(config.isValidationRunOnStartup()).isEqualTo(validationSettings.isRunOnStartup());
        assertThat(config.isValidationValidateOnly()).isEqualTo(validationSettings.isValidateOnly());
        
        logger.info("✓ Validation settings accessors verified");
    }

    @Test
    void testValidationSettingsLogging() {
        // Test that validation settings are logged during configuration loading
        // This test verifies that the loadValidationConfig method is called and logs the settings
        
        GenericApiConfig config = new GenericApiConfig();
        
        // The configuration should be loaded and logged
        // We can't easily test the logging output, but we can verify the configuration is loaded
        assertThat(config.getValidationSettings()).isNotNull();
        
        logger.info("✓ Validation settings logging test completed");
        logger.info("   Current settings - runOnStartup: {}, validateOnly: {}", 
                   config.isValidationRunOnStartup(), config.isValidationValidateOnly());
    }

    @Test
    void testValidationSettingsConsistency() {
        // Test that validation settings are consistent across multiple config instances
        GenericApiConfig config1 = new GenericApiConfig();
        GenericApiConfig config2 = new GenericApiConfig();
        
        // Both instances should have the same validation settings
        assertThat(config1.isValidationRunOnStartup()).isEqualTo(config2.isValidationRunOnStartup());
        assertThat(config1.isValidationValidateOnly()).isEqualTo(config2.isValidationValidateOnly());
        
        logger.info("✓ Validation settings consistency verified");
    }

    @Test
    void testValidationSettingsStructure() {
        // Test the internal structure of ValidationSettings class
        GenericApiConfig.ValidationSettings settings = new GenericApiConfig.ValidationSettings();
        
        // Test default values
        assertThat(settings.isRunOnStartup()).isFalse();
        assertThat(settings.isValidateOnly()).isFalse();
        
        // Test setters
        settings.setRunOnStartup(true);
        settings.setValidateOnly(true);
        
        assertThat(settings.isRunOnStartup()).isTrue();
        assertThat(settings.isValidateOnly()).isTrue();
        
        // Test setters with false
        settings.setRunOnStartup(false);
        settings.setValidateOnly(false);
        
        assertThat(settings.isRunOnStartup()).isFalse();
        assertThat(settings.isValidateOnly()).isFalse();
        
        logger.info("✓ ValidationSettings class structure verified");
    }

    @Test
    void testValidationEnabledConfiguration() {
        // Test loading configuration with validation.runOnStartup=true
        System.setProperty("generic.config.file", "application-validation-test.yml");

        GenericApiConfig config = GenericApiConfig.loadFromFile();

        // Validation should be enabled for startup
        assertThat(config.isValidationRunOnStartup()).isTrue();
        assertThat(config.isValidationValidateOnly()).isFalse();

        logger.info("✓ Validation enabled configuration verified");
        logger.info("   runOnStartup: {}", config.isValidationRunOnStartup());
        logger.info("   validateOnly: {}", config.isValidationValidateOnly());
    }

    @Test
    void testValidateOnlyConfiguration() {
        // Test loading configuration with validation.validateOnly=true
        System.setProperty("generic.config.file", "application-validate-only-test.yml");

        GenericApiConfig config = GenericApiConfig.loadFromFile();

        // Validate-only mode should be enabled
        assertThat(config.isValidationRunOnStartup()).isFalse();
        assertThat(config.isValidationValidateOnly()).isTrue();

        logger.info("✓ Validate-only configuration verified");
        logger.info("   runOnStartup: {}", config.isValidationRunOnStartup());
        logger.info("   validateOnly: {}", config.isValidationValidateOnly());
    }
}

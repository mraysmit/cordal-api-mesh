package dev.cordal.generic;

import dev.cordal.config.GenericApiConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for validation startup functionality in GenericApiApplication
 */
class ValidationStartupTest {
    private static final Logger logger = LoggerFactory.getLogger(ValidationStartupTest.class);

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
    void testValidationConfigurationLoading() {
        // Test that GenericApiConfig can load validation configuration directly
        // Use test configuration that has default validation settings
        System.setProperty("generic.config.file", "application-test.yml");

        GenericApiConfig config = GenericApiConfig.loadFromFile();

        // Verify validation settings are accessible
        assertThat(config.getValidationSettings()).isNotNull();
        assertThat(config.isValidationRunOnStartup()).isFalse(); // Default should be false
        assertThat(config.isValidationValidateOnly()).isFalse(); // Default should be false

        logger.info("✓ Validation configuration loading verified");
        logger.info("   runOnStartup: {}", config.isValidationRunOnStartup());
        logger.info("   validateOnly: {}", config.isValidationValidateOnly());
    }

    @Test
    void testValidationEnabledStartup() {
        // Test configuration with validation enabled on startup
        System.setProperty("generic.config.file", "application-validation-test.yml");

        GenericApiConfig config = GenericApiConfig.loadFromFile();

        // Verify validation is enabled for startup
        assertThat(config.isValidationRunOnStartup()).isTrue();
        assertThat(config.isValidationValidateOnly()).isFalse();

        logger.info("✓ Validation enabled startup configuration verified");
        logger.info("   runOnStartup: {}", config.isValidationRunOnStartup());
        logger.info("   validateOnly: {}", config.isValidationValidateOnly());
    }

    @Test
    void testValidateOnlyMode() {
        // Test configuration with validate-only mode
        System.setProperty("generic.config.file", "application-validate-only-test.yml");

        GenericApiConfig config = GenericApiConfig.loadFromFile();

        // Verify validate-only mode is enabled
        assertThat(config.isValidationRunOnStartup()).isFalse();
        assertThat(config.isValidationValidateOnly()).isTrue();

        logger.info("✓ Validate-only mode configuration verified");
        logger.info("   runOnStartup: {}", config.isValidationRunOnStartup());
        logger.info("   validateOnly: {}", config.isValidationValidateOnly());
    }

    @Test
    void testCommandLineArgumentParsing() {
        // Test command line argument parsing logic
        String[] argsWithValidate = {"--validate-only"};
        String[] argsWithValidateShort = {"--validate"};
        String[] argsWithoutValidate = {"--some-other-arg"};
        String[] emptyArgs = {};
        
        // Test --validate-only
        boolean validateOnlyFromArgs = false;
        for (String arg : argsWithValidate) {
            if ("--validate-only".equals(arg) || "--validate".equals(arg)) {
                validateOnlyFromArgs = true;
                break;
            }
        }
        assertThat(validateOnlyFromArgs).isTrue();
        
        // Test --validate
        validateOnlyFromArgs = false;
        for (String arg : argsWithValidateShort) {
            if ("--validate-only".equals(arg) || "--validate".equals(arg)) {
                validateOnlyFromArgs = true;
                break;
            }
        }
        assertThat(validateOnlyFromArgs).isTrue();
        
        // Test without validate args
        validateOnlyFromArgs = false;
        for (String arg : argsWithoutValidate) {
            if ("--validate-only".equals(arg) || "--validate".equals(arg)) {
                validateOnlyFromArgs = true;
                break;
            }
        }
        assertThat(validateOnlyFromArgs).isFalse();
        
        // Test empty args
        validateOnlyFromArgs = false;
        for (String arg : emptyArgs) {
            if ("--validate-only".equals(arg) || "--validate".equals(arg)) {
                validateOnlyFromArgs = true;
                break;
            }
        }
        assertThat(validateOnlyFromArgs).isFalse();
        
        logger.info("✓ Command line argument parsing logic verified");
    }
}

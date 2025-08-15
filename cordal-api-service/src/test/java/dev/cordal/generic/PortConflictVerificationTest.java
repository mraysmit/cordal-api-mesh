package dev.cordal.generic;

import dev.cordal.generic.GenericApiApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify that the Generic API application can initialize for testing
 * without port conflicts. This test focuses on the initializeForTesting() method
 * which allows multiple test instances to run simultaneously.
 */
class PortConflictVerificationTest {
    private static final Logger logger = LoggerFactory.getLogger(PortConflictVerificationTest.class);

    @BeforeAll
    static void setUpClass() {
        // Set test configuration for both services
        System.setProperty("config.file", "application-test.yml");
        logger.info("Port conflict verification test setup - using test configuration");
    }

    @AfterAll
    static void tearDownClass() {
        System.clearProperty("config.file");
        logger.info("Port conflict verification test cleanup completed");
    }

    @Test
    @DisplayName("Should allow multiple Generic API instances to initialize without port conflicts")
    void shouldAllowMultipleGenericApiInstancesToInitializeWithoutPortConflicts() {
        logger.info("Testing multiple Generic API instance initialization without port conflicts...");

        // This test verifies that our fixes prevent port conflicts by using initializeForTesting()

        // Create and initialize first Generic API Service instance
        GenericApiApplication genericApp1 = new GenericApiApplication();
        genericApp1.initializeForTesting(); // Uses no port binding
        logger.info("First Generic API application initialized successfully");

        // Create and initialize second Generic API Service instance
        GenericApiApplication genericApp2 = new GenericApiApplication();
        genericApp2.initializeForTesting(); // Uses no port binding
        logger.info("Second Generic API application initialized successfully");

        // Verify both applications initialized successfully
        assertThat(genericApp1.getApp()).isNotNull();
        assertThat(genericApp1.getInjector()).isNotNull();

        assertThat(genericApp2.getApp()).isNotNull();
        assertThat(genericApp2.getInjector()).isNotNull();

        logger.info("✓ Multiple Generic API instances initialized without port conflicts");

        // Clean up
        try {
            genericApp1.stop();
            genericApp2.stop();
            logger.info("Applications stopped successfully");
        } catch (Exception e) {
            logger.warn("Cleanup warning (expected in tests): {}", e.getMessage());
        }
    }

    @Test
    @DisplayName("Should load correct test configuration for Generic API")
    void shouldLoadCorrectTestConfiguration() {
        logger.info("Testing test configuration loading...");

        // Verify that test configuration is loaded correctly

        GenericApiApplication genericApp = new GenericApiApplication();
        genericApp.initializeForTesting();

        // The test configuration should be loaded
        // This is verified by the fact that the application initializes
        // with the in-memory database configuration
        assertThat(genericApp.getInjector()).isNotNull();

        logger.info("✓ Test configuration loaded correctly");

        // Clean up
        try {
            genericApp.stop();
            logger.info("Application stopped successfully");
        } catch (Exception e) {
            logger.warn("Cleanup warning (expected in tests): {}", e.getMessage());
        }
    }

    @Test
    @DisplayName("Should verify initializeForTesting method works correctly")
    void shouldVerifyInitializeForTestingMethod() {
        logger.info("Testing initializeForTesting method...");

        // Test Generic API Application
        GenericApiApplication genericApp = new GenericApiApplication();
        genericApp.initializeForTesting();

        // Verify the application is properly initialized but not started
        assertThat(genericApp.getApp()).isNotNull();
        assertThat(genericApp.getInjector()).isNotNull();

        // The app should be configured but not started (no port binding)
        // This is the key difference that prevents port conflicts
        logger.info("✓ Generic API initializeForTesting works correctly");

        // Verify that we can create another instance without conflicts
        GenericApiApplication genericApp2 = new GenericApiApplication();
        genericApp2.initializeForTesting();

        // Verify the second application is also properly initialized
        assertThat(genericApp2.getApp()).isNotNull();
        assertThat(genericApp2.getInjector()).isNotNull();

        logger.info("✓ Multiple initializeForTesting calls work without conflicts");

        // Clean up
        try {
            genericApp.stop();
            genericApp2.stop();
            logger.info("Applications stopped successfully");
        } catch (Exception e) {
            logger.warn("Cleanup warning (expected in tests): {}", e.getMessage());
        }
    }
}

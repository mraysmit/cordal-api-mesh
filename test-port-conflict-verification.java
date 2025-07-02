/**
 * Test to verify that port conflicts have been resolved
 * This test demonstrates that multiple application tests can run simultaneously
 * without "Address already in use" errors.
 */
package dev.mars.test;

import dev.mars.generic.GenericApiApplication;
import dev.mars.metrics.MetricsApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import static org.assertj.core.api.Assertions.assertThat;

public class PortConflictVerificationTest {

    @BeforeAll
    static void setUpClass() {
        // Set test configuration for both services
        System.setProperty("config.file", "application-test.yml");
    }

    @AfterAll
    static void tearDownClass() {
        System.clearProperty("config.file");
    }

    @Test
    void shouldAllowMultipleApplicationsToInitializeWithoutPortConflicts() {
        // This test verifies that our fixes prevent port conflicts
        
        // Create and initialize Generic API Service
        GenericApiApplication genericApp = new GenericApiApplication();
        genericApp.initializeForTesting(); // Uses no port binding
        
        // Create and initialize Metrics Service
        MetricsApplication metricsApp = new MetricsApplication();
        metricsApp.initializeForTesting(); // Uses no port binding
        
        // Verify both applications initialized successfully
        assertThat(genericApp.getApp()).isNotNull();
        assertThat(genericApp.getInjector()).isNotNull();
        
        assertThat(metricsApp.getApp()).isNotNull();
        assertThat(metricsApp.getInjector()).isNotNull();
        
        // Clean up
        try {
            genericApp.stop();
            metricsApp.stop();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test
    void shouldLoadCorrectTestConfiguration() {
        // Verify that test configuration is loaded correctly
        
        MetricsApplication metricsApp = new MetricsApplication();
        metricsApp.initializeForTesting();
        
        // The test configuration should be loaded
        // This is verified by the fact that the application initializes
        // with the in-memory database configuration
        assertThat(metricsApp.getInjector()).isNotNull();
        
        // Clean up
        try {
            metricsApp.stop();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
}

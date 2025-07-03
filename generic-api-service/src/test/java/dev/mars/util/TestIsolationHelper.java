package dev.mars.util;

import dev.mars.generic.GenericApiApplication;
import dev.mars.test.TestDatabaseManager;

/**
 * Utility class to help with test isolation and cleanup
 */
public class TestIsolationHelper {

    /**
     * Ensures clean state for database-related tests
     * Note: DatabaseManager is no longer available in production,
     * so this method now only provides a delay for cleanup
     */
    public static void cleanDatabaseState(GenericApiApplication application) {
        if (application == null) {
            return;
        }

        try {
            // DatabaseManager is no longer available in production
            // Tests should handle their own database cleanup using TestDatabaseManager

            // Small delay to ensure any background cleanup completes
            Thread.sleep(200);
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
    
    /**
     * Comprehensive cleanup for integration tests
     */
    public static void cleanAllState(GenericApiApplication application) {
        cleanDatabaseState(application);
    }

    /**
     * Safely stops an application with proper cleanup
     */
    public static void safeStopApplication(GenericApiApplication application) {
        if (application == null) {
            return;
        }

        try {
            application.stop();
            // Wait for proper shutdown
            Thread.sleep(500);
        } catch (Exception e) {
            // Ignore cleanup errors in tests
        }
    }
    
    /**
     * Sets up test configuration and clears any existing system properties
     */
    public static void setupTestConfiguration() {
        // Clear any existing config properties
        System.clearProperty("generic.config.file");
        
        // Set test configuration
        System.setProperty("generic.config.file", "application-test.yml");
    }
    
    /**
     * Cleans up test configuration
     */
    public static void cleanupTestConfiguration() {
        System.clearProperty("generic.config.file");
    }
    
    /**
     * Waits for application to be ready
     */
    public static void waitForApplicationReady(GenericApiApplication application, long timeoutMs) {
        if (application == null) {
            return;
        }

        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                if (application.getApp() != null && application.getInjector() != null) {
                    // Application appears ready
                    Thread.sleep(500); // Additional buffer
                    return;
                }
                Thread.sleep(100);
            } catch (Exception e) {
                // Continue waiting
            }
        }
    }
}

package dev.mars.util;

import dev.mars.generic.GenericApiApplication;
import dev.mars.database.DatabaseManager;

/**
 * Utility class to help with test isolation and cleanup
 */
public class TestIsolationHelper {

    /**
     * Ensures clean state for database-related tests
     */
    public static void cleanDatabaseState(GenericApiApplication application) {
        if (application == null) {
            return;
        }

        try {
            // Clean main database
            var databaseManager = application.getInjector().getInstance(DatabaseManager.class);
            databaseManager.cleanDatabase();

            // Small delay to ensure cleanup completes
            Thread.sleep(200);
        } catch (Exception e) {
            // Ignore cleanup errors - test may not have metrics database manager
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
        System.clearProperty("config.file");
        
        // Set test configuration
        System.setProperty("config.file", "application-test.yml");
    }
    
    /**
     * Cleans up test configuration
     */
    public static void cleanupTestConfiguration() {
        System.clearProperty("config.file");
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

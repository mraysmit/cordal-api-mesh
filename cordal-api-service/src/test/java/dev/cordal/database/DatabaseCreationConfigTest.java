package dev.cordal.database;

import dev.cordal.config.GenericApiConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for database creation configuration functionality
 */
public class DatabaseCreationConfigTest {

    @Test
    public void testDatabaseCreateIfMissingDefaultValue() {
        // Test default configuration
        GenericApiConfig config = new GenericApiConfig();
        assertTrue(config.isDatabaseCreateIfMissing(), "Database creation should be enabled by default");
    }

    @Test
    public void testDatabaseCreateIfMissingCanBeDisabled() {
        // Set system property to use test config with createIfMissing=false
        System.setProperty("generic.config.file", "application-test-no-create.yml");
        
        try {
            GenericApiConfig config = new GenericApiConfig();
            assertFalse(config.isDatabaseCreateIfMissing(), "Database creation should be disabled when configured");
        } finally {
            // Clean up system property
            System.clearProperty("generic.config.file");
        }
    }

    @Test
    public void testDatabaseManagerRespectsCreateIfMissingConfig() {
        // Test with creation enabled
        System.setProperty("generic.config.file", "application-test.yml");
        
        try {
            GenericApiConfig config = new GenericApiConfig();
            assertTrue(config.isDatabaseCreateIfMissing(), "Database creation should be enabled in test config");
            
            // Create DatabaseManager - should not throw exception
            DatabaseManager dbManager = new DatabaseManager(config);
            assertNotNull(dbManager, "DatabaseManager should be created successfully");
            
            // Initialize schema - should work
            assertDoesNotThrow(() -> dbManager.initializeSchema(), 
                             "Schema initialization should succeed when creation is enabled");
            
        } finally {
            System.clearProperty("generic.config.file");
        }
    }

    @Test
    public void testDatabaseManagerWithCreateIfMissingDisabled() {
        // Test with creation disabled
        System.setProperty("generic.config.file", "application-test-no-create.yml");
        
        try {
            GenericApiConfig config = new GenericApiConfig();
            assertFalse(config.isDatabaseCreateIfMissing(), "Database creation should be disabled");
            
            // Create DatabaseManager - should not throw exception
            DatabaseManager dbManager = new DatabaseManager(config);
            assertNotNull(dbManager, "DatabaseManager should be created successfully");
            
            // Initialize schema - should still work for in-memory database
            assertDoesNotThrow(() -> dbManager.initializeSchema(), 
                             "Schema initialization should succeed for in-memory database");
            
        } finally {
            System.clearProperty("generic.config.file");
        }
    }
}

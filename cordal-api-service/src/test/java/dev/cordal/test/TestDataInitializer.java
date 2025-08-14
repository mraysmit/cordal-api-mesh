package dev.cordal.test;

import dev.cordal.generic.database.DatabaseConnectionManager;
import dev.cordal.test.TestDatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test data initializer for setting up test data in test databases
 * This class coordinates test database schema management for core system testing
 * Note: Example data (like stock trades) should be managed by integration tests
 */
public class TestDataInitializer {
    private static final Logger logger = LoggerFactory.getLogger(TestDataInitializer.class);
    
    private final DatabaseConnectionManager databaseConnectionManager;
    private final TestDatabaseManager testDatabaseManager;
    
    public TestDataInitializer(DatabaseConnectionManager databaseConnectionManager, 
                              TestDatabaseManager testDatabaseManager) {
        this.databaseConnectionManager = databaseConnectionManager;
        this.testDatabaseManager = testDatabaseManager;
    }
    
    /**
     * Initialize all test data needed for integration tests
     * This includes both test database schema and external database test data
     */
    public void initializeAllTestData() {
        logger.info("Initializing all test data");
        
        try {
            // Initialize test database schema (stock trades tables in test databases)
            logger.info("Initializing test database schema");
            testDatabaseManager.initializeSchema();
            logger.info("Test database schema initialized successfully");
            
            // Initialize external database test data if needed
            // Note: For now, most tests use the test database (stock-trades-db)
            // which is handled by TestDatabaseManager
            
            logger.info("All test data initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize test data", e);
            throw new RuntimeException("Failed to initialize test data", e);
        }
    }

    /**
     * Note: External database initialization (like stock trades) should be handled
     * by integration tests, not by the core test framework
     */
    
    /**
     * Clean all test data (for cleanup between tests)
     */
    public void cleanAllTestData() {
        logger.info("Cleaning all test data");
        
        try {
            // Clean test database
            testDatabaseManager.cleanDatabase();
            
            // Note: External database cleanup (like stock trades) should be handled
            // by integration tests, not by the core test framework
            
            logger.info("All test data cleaned successfully");
            
        } catch (Exception e) {
            logger.error("Failed to clean test data", e);
            throw new RuntimeException("Failed to clean test data", e);
        }
    }
}

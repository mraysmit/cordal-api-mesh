package dev.mars.test;

import dev.mars.generic.database.DatabaseConnectionManager;
import dev.mars.test.TestDatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test data initializer for setting up test data in test databases
 * This class coordinates between TestDatabaseManager (for test database schema)
 * and StockTradesInitializer (for external database test data)
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
     * Initialize stock trades data in external databases (if configured)
     * This is separate from the test database and would be used for
     * tests that specifically test external database connectivity
     */
    public void initializeExternalStockTradesData() {
        logger.info("Initializing external stock trades data for testing");
        
        try {
            StockTradesInitializer stockTradesInitializer = new StockTradesInitializer(databaseConnectionManager);
            stockTradesInitializer.initialize();
            logger.info("External stock trades data initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize external stock trades data", e);
            // Don't throw exception - external databases might not be configured in tests
            logger.warn("External stock trades data initialization failed, continuing with test database only");
        }
    }
    
    /**
     * Clean all test data (for cleanup between tests)
     */
    public void cleanAllTestData() {
        logger.info("Cleaning all test data");
        
        try {
            // Clean test database
            testDatabaseManager.cleanDatabase();
            
            // Clean external databases if they were initialized
            try {
                StockTradesInitializer stockTradesInitializer = new StockTradesInitializer(databaseConnectionManager);
                stockTradesInitializer.cleanStockTradesDatabase();
            } catch (Exception e) {
                logger.debug("Could not clean external stock trades database (may not be configured): {}", e.getMessage());
            }
            
            logger.info("All test data cleaned successfully");
            
        } catch (Exception e) {
            logger.error("Failed to clean test data", e);
            throw new RuntimeException("Failed to clean test data", e);
        }
    }
}

package dev.mars.test;

import dev.mars.common.util.StockTradesTestDataInitializer;
import dev.mars.generic.database.DatabaseConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;

/**
 * Test utility class for managing stock trades test data
 * This class provides methods to initialize and manage stock trades data for testing purposes
 */
public class StockTradesTestDataManager {
    private static final Logger logger = LoggerFactory.getLogger(StockTradesTestDataManager.class);
    
    private final DatabaseConnectionManager databaseConnectionManager;
    
    public StockTradesTestDataManager(DatabaseConnectionManager databaseConnectionManager) {
        this.databaseConnectionManager = databaseConnectionManager;
    }
    
    /**
     * Initialize stock trades data for testing using the common library initializer
     * This method creates the stock_trades table and populates it with sample data
     * 
     * @throws Exception if initialization fails
     */
    public void initializeStockTradesData() throws Exception {
        logger.info("Initializing stock trades data for testing");
        
        try (Connection connection = databaseConnectionManager.getConnection("stocktrades")) {
            StockTradesTestDataInitializer.initializeStockTradesForTesting(connection);
            logger.info("Stock trades data initialized successfully for testing");
        } catch (Exception e) {
            logger.error("Failed to initialize stock trades data for testing", e);
            throw e;
        }
    }
    
    /**
     * Initialize stock trades data with error handling that doesn't throw exceptions
     * This is useful for tests that should continue even if stock trades data initialization fails
     * 
     * @return true if initialization was successful, false otherwise
     */
    public boolean initializeStockTradesDataSafely() {
        try {
            initializeStockTradesData();
            return true;
        } catch (Exception e) {
            logger.warn("Stock trades data initialization failed, continuing without stock trades data", e);
            return false;
        }
    }
    
    /**
     * Check if stock trades data is available
     * 
     * @return true if stock trades data is available, false otherwise
     */
    public boolean isStockTradesDataAvailable() {
        try (Connection connection = databaseConnectionManager.getConnection("stocktrades")) {
            // Try to execute a simple query to check if data exists
            try (var statement = connection.createStatement();
                 var resultSet = statement.executeQuery("SELECT COUNT(*) FROM stock_trades")) {
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    logger.debug("Found {} stock trades records", count);
                    return count > 0;
                }
            }
        } catch (Exception e) {
            logger.debug("Stock trades data not available: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * Clean stock trades data (for test cleanup)
     * This method removes all data from the stock_trades table
     */
    public void cleanStockTradesData() {
        try (Connection connection = databaseConnectionManager.getConnection("stocktrades")) {
            try (var statement = connection.createStatement()) {
                statement.execute("DELETE FROM stock_trades");
                logger.info("Stock trades data cleaned successfully");
            }
        } catch (Exception e) {
            logger.warn("Failed to clean stock trades data", e);
        }
    }
    
    /**
     * Get the count of stock trades records
     * 
     * @return the number of stock trades records, or -1 if unable to determine
     */
    public int getStockTradesCount() {
        try (Connection connection = databaseConnectionManager.getConnection("stocktrades")) {
            try (var statement = connection.createStatement();
                 var resultSet = statement.executeQuery("SELECT COUNT(*) FROM stock_trades")) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (Exception e) {
            logger.debug("Unable to get stock trades count: {}", e.getMessage());
        }
        return -1;
    }
    
    /**
     * Factory method to create a StockTradesTestDataManager from a GenericApiApplication
     * This is a convenience method for tests that have access to the application instance
     * 
     * @param application the GenericApiApplication instance
     * @return a new StockTradesTestDataManager instance
     */
    public static StockTradesTestDataManager fromApplication(dev.mars.generic.GenericApiApplication application) {
        DatabaseConnectionManager dbConnectionManager = 
            application.getInjector().getInstance(DatabaseConnectionManager.class);
        return new StockTradesTestDataManager(dbConnectionManager);
    }
}

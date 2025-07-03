package dev.mars.common.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Schema manager for stock trades database
 * Handles creation of stock trades tables only
 * Performance metrics tables are managed by the metrics-service
 * This should be used with the stocktrades database, not the api-service-config database
 */
public class StockTradesSchemaManager {
    private static final Logger logger = LoggerFactory.getLogger(StockTradesSchemaManager.class);
    
    private final BaseDatabaseManager databaseManager;
    
    public StockTradesSchemaManager(BaseDatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }
    
    /**
     * Initialize the stock trades database schema
     * Creates tables for stock trades only
     * Performance metrics tables are managed by metrics-service
     */
    public void initializeSchema() {
        logger.info("Initializing stock trades database schema");
        
        String createStockTradesTableSql = """
            CREATE TABLE IF NOT EXISTS stock_trades (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                symbol VARCHAR(10) NOT NULL,
                trade_type VARCHAR(4) NOT NULL CHECK (trade_type IN ('BUY', 'SELL')),
                quantity INTEGER NOT NULL CHECK (quantity > 0),
                price DECIMAL(10,2) NOT NULL CHECK (price > 0),
                total_value DECIMAL(15,2) NOT NULL,
                trade_date_time TIMESTAMP NOT NULL,
                trader_id VARCHAR(50) NOT NULL,
                exchange VARCHAR(20) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """;



        // Indexes for stock trades table
        String createStockTradesIndexes1 = "CREATE INDEX IF NOT EXISTS idx_stock_trades_symbol ON stock_trades(symbol)";
        String createStockTradesIndexes2 = "CREATE INDEX IF NOT EXISTS idx_stock_trades_trader ON stock_trades(trader_id)";
        String createStockTradesIndexes3 = "CREATE INDEX IF NOT EXISTS idx_stock_trades_date ON stock_trades(trade_date_time)";

        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement()) {

            // Create tables
            statement.execute(createStockTradesTableSql);
            logger.info("Stock trades table created/verified");

            // Create indexes
            statement.execute(createStockTradesIndexes1);
            statement.execute(createStockTradesIndexes2);
            statement.execute(createStockTradesIndexes3);
            logger.info("Stock trades database indexes created/verified");

        } catch (SQLException e) {
            logger.error("Failed to initialize stock trades database schema", e);
            throw new RuntimeException("Failed to initialize stock trades database schema", e);
        }

        logger.info("Stock trades database schema initialized successfully");
    }
    
    /**
     * Check if the database is accessible
     */
    public boolean isHealthy() {
        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("SELECT 1");
            return true;

        } catch (SQLException e) {
            logger.error("Stock trades database health check failed", e);
            return false;
        }
    }
    
    /**
     * Clean all data from the stock trades database (for testing purposes)
     */
    public void cleanDatabase() {
        logger.info("Cleaning stock trades database for testing");

        String deleteStockTradesSql = "DELETE FROM stock_trades";
        String resetStockTradesSequenceSql = "ALTER TABLE stock_trades ALTER COLUMN id RESTART WITH 1";

        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(deleteStockTradesSql);
            statement.execute(resetStockTradesSequenceSql);

            logger.info("Stock trades database cleaned successfully");

        } catch (SQLException e) {
            logger.error("Failed to clean stock trades database", e);
            throw new RuntimeException("Failed to clean stock trades database", e);
        }
    }
}

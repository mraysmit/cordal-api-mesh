package dev.cordal.test;

import dev.cordal.common.database.BaseDatabaseManager;
import dev.cordal.test.DatabaseConfigAdapter;
import dev.cordal.config.GenericApiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * Test database manager for handling database initialization and schema creation
 * Used only for testing purposes - extends BaseDatabaseManager from common-library
 */
public class TestDatabaseManager extends BaseDatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(TestDatabaseManager.class);

    public TestDatabaseManager(GenericApiConfig genericApiConfig) {
        super(DatabaseConfigAdapter.createCommonDatabaseConfig(genericApiConfig));
    }
    
    @Override
    public void initializeSchema() {
        logger.info("Initializing test database schema");

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



        String createIndexSql1 = """
            CREATE INDEX IF NOT EXISTS idx_stock_trades_symbol ON stock_trades(symbol)
            """;

        String createIndexSql2 = """
            CREATE INDEX IF NOT EXISTS idx_stock_trades_trader_id ON stock_trades(trader_id)
            """;

        String createIndexSql3 = """
            CREATE INDEX IF NOT EXISTS idx_stock_trades_trade_date_time ON stock_trades(trade_date_time)
            """;



        try {
            // Create tables using the base class method
            executeSql(createStockTradesTableSql);
            logger.info("Stock trades table created/verified");



            // Create stock trades indexes
            executeSql(createIndexSql1);
            executeSql(createIndexSql2);
            executeSql(createIndexSql3);
            logger.info("Stock trades indexes created/verified");



        } catch (SQLException e) {
            logger.error("Failed to initialize test database schema", e);
            throw new RuntimeException("Failed to initialize test database schema", e);
        }
    }
    
    /**
     * Clean all data from the database (for testing purposes)
     */
    public void cleanDatabase() {
        logger.info("Cleaning test database for testing");

        String deleteStockTradesSql = "DELETE FROM stock_trades";
        String resetStockTradesSequenceSql = "ALTER TABLE stock_trades ALTER COLUMN id RESTART WITH 1";

        try {
            executeSql(deleteStockTradesSql);
            executeSql(resetStockTradesSequenceSql);

            logger.info("Test database cleaned successfully");

        } catch (SQLException e) {
            logger.error("Failed to clean test database", e);
            throw new RuntimeException("Failed to clean test database", e);
        }
    }
}

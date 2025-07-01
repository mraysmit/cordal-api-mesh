package dev.mars.database;

import dev.mars.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database manager for handling database initialization and schema creation
 */
public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    
    private final DataSource dataSource;
    
    public DatabaseManager(DatabaseConfig databaseConfig) {
        this.dataSource = databaseConfig.getDataSource();
    }
    
    /**
     * Initialize the database schema
     */
    public void initializeSchema() {
        logger.info("Initializing database schema");

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

        String createPerformanceMetricsTableSql = """
            CREATE TABLE IF NOT EXISTS performance_metrics (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                test_name VARCHAR(255) NOT NULL,
                test_type VARCHAR(100) NOT NULL,
                timestamp TIMESTAMP NOT NULL,
                total_requests INTEGER,
                total_time_ms BIGINT,
                average_response_time_ms DOUBLE,
                concurrent_threads INTEGER,
                requests_per_thread INTEGER,
                page_size INTEGER,
                memory_usage_bytes BIGINT,
                memory_increase_bytes BIGINT,
                test_passed BOOLEAN,
                additional_metrics TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

        // Stock trades indexes
        String createIndexSql1 = "CREATE INDEX IF NOT EXISTS idx_stock_trades_symbol ON stock_trades(symbol)";
        String createIndexSql2 = "CREATE INDEX IF NOT EXISTS idx_stock_trades_trader_id ON stock_trades(trader_id)";
        String createIndexSql3 = "CREATE INDEX IF NOT EXISTS idx_stock_trades_trade_date_time ON stock_trades(trade_date_time)";

        // Performance metrics indexes
        String createIndexSql4 = "CREATE INDEX IF NOT EXISTS idx_performance_metrics_test_type ON performance_metrics(test_type)";
        String createIndexSql5 = "CREATE INDEX IF NOT EXISTS idx_performance_metrics_timestamp ON performance_metrics(timestamp)";
        String createIndexSql6 = "CREATE INDEX IF NOT EXISTS idx_performance_metrics_test_name ON performance_metrics(test_name)";

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // Create stock trades table
            statement.execute(createStockTradesTableSql);
            logger.info("Stock trades table created/verified");

            // Create performance metrics table
            statement.execute(createPerformanceMetricsTableSql);
            logger.info("Performance metrics table created/verified");

            // Create stock trades indexes
            statement.execute(createIndexSql1);
            statement.execute(createIndexSql2);
            statement.execute(createIndexSql3);
            logger.info("Stock trades indexes created/verified");

            // Create performance metrics indexes
            statement.execute(createIndexSql4);
            statement.execute(createIndexSql5);
            statement.execute(createIndexSql6);
            logger.info("Performance metrics indexes created/verified");

        } catch (SQLException e) {
            logger.error("Failed to initialize database schema", e);
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }
    
    /**
     * Get a connection from the data source
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    /**
     * Check if the database is accessible
     */
    public boolean isHealthy() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("SELECT 1");
            return true;

        } catch (SQLException e) {
            logger.error("Database health check failed", e);
            return false;
        }
    }

    /**
     * Clean all data from the database (for testing purposes)
     */
    public void cleanDatabase() {
        logger.info("Cleaning database for testing");

        String deleteStockTradesSql = "DELETE FROM stock_trades";
        String deletePerformanceMetricsSql = "DELETE FROM performance_metrics";
        String resetStockTradesSequenceSql = "ALTER TABLE stock_trades ALTER COLUMN id RESTART WITH 1";
        String resetPerformanceMetricsSequenceSql = "ALTER TABLE performance_metrics ALTER COLUMN id RESTART WITH 1";

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(deleteStockTradesSql);
            statement.execute(resetStockTradesSequenceSql);

            statement.execute(deletePerformanceMetricsSql);
            statement.execute(resetPerformanceMetricsSequenceSql);

            logger.info("Database cleaned successfully");

        } catch (SQLException e) {
            logger.error("Failed to clean database", e);
            throw new RuntimeException("Failed to clean database", e);
        }
    }
}

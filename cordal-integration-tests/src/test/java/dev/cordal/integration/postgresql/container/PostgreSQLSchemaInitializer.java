package dev.cordal.integration.postgresql.container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Initializes PostgreSQL database schema for stock trades integration testing
 * Creates tables, indexes, and constraints needed for the dual-database test
 */
public class PostgreSQLSchemaInitializer {
    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLSchemaInitializer.class);
    
    private final String databaseName;
    
    public PostgreSQLSchemaInitializer(String databaseName) {
        this.databaseName = databaseName;
    }
    
    /**
     * Initialize the complete database schema
     * 
     * @param connection Database connection
     * @throws SQLException if schema creation fails
     */
    public void initializeSchema(Connection connection) throws SQLException {
        logger.info("Initializing PostgreSQL schema for database: {}", databaseName);
        
        try (Statement statement = connection.createStatement()) {
            // Create the stock_trades table
            createStockTradesTable(statement);
            
            // Create indexes for performance
            createIndexes(statement);
            
            // Create any additional constraints
            createConstraints(statement);
            
            logger.info("PostgreSQL schema initialized successfully for database: {}", databaseName);
        } catch (SQLException e) {
            logger.error("Failed to initialize PostgreSQL schema for database: {}", databaseName, e);
            throw e;
        }
    }
    
    /**
     * Create the stock_trades table with all necessary columns and constraints
     */
    private void createStockTradesTable(Statement statement) throws SQLException {
        logger.debug("Creating stock_trades table for database: {}", databaseName);
        
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS stock_trades (
                id BIGSERIAL PRIMARY KEY,
                symbol VARCHAR(10) NOT NULL,
                trade_type VARCHAR(4) NOT NULL CHECK (trade_type IN ('BUY', 'SELL')),
                quantity INTEGER NOT NULL CHECK (quantity > 0),
                price DECIMAL(10,2) NOT NULL CHECK (price > 0),
                total_value DECIMAL(15,2) NOT NULL CHECK (total_value > 0),
                trade_date_time TIMESTAMP NOT NULL,
                trader_id VARCHAR(50) NOT NULL,
                exchange VARCHAR(20) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        
        statement.execute(createTableSql);
        logger.debug("stock_trades table created successfully for database: {}", databaseName);
    }
    
    /**
     * Create indexes for optimal query performance
     */
    private void createIndexes(Statement statement) throws SQLException {
        logger.debug("Creating indexes for database: {}", databaseName);
        
        // Index on symbol for symbol-based queries
        String symbolIndexSql = """
            CREATE INDEX IF NOT EXISTS idx_stock_trades_symbol 
            ON stock_trades (symbol)
            """;
        statement.execute(symbolIndexSql);
        
        // Index on trade_date_time for date range queries and ordering
        String dateIndexSql = """
            CREATE INDEX IF NOT EXISTS idx_stock_trades_trade_date_time 
            ON stock_trades (trade_date_time DESC)
            """;
        statement.execute(dateIndexSql);
        
        // Index on trader_id for trader-based queries
        String traderIndexSql = """
            CREATE INDEX IF NOT EXISTS idx_stock_trades_trader_id 
            ON stock_trades (trader_id)
            """;
        statement.execute(traderIndexSql);
        
        // Composite index for symbol and date queries
        String compositeIndexSql = """
            CREATE INDEX IF NOT EXISTS idx_stock_trades_symbol_date 
            ON stock_trades (symbol, trade_date_time DESC)
            """;
        statement.execute(compositeIndexSql);
        
        // Index on exchange for exchange-based filtering
        String exchangeIndexSql = """
            CREATE INDEX IF NOT EXISTS idx_stock_trades_exchange 
            ON stock_trades (exchange)
            """;
        statement.execute(exchangeIndexSql);
        
        logger.debug("Indexes created successfully for database: {}", databaseName);
    }
    
    /**
     * Create additional constraints and triggers
     */
    private void createConstraints(Statement statement) throws SQLException {
        logger.debug("Creating additional constraints for database: {}", databaseName);
        
        // Create a trigger to automatically update the updated_at timestamp
        String createTriggerFunctionSql = """
            CREATE OR REPLACE FUNCTION update_updated_at_column()
            RETURNS TRIGGER AS $$
            BEGIN
                NEW.updated_at = CURRENT_TIMESTAMP;
                RETURN NEW;
            END;
            $$ language 'plpgsql'
            """;
        statement.execute(createTriggerFunctionSql);
        
        String createTriggerSql = """
            DROP TRIGGER IF EXISTS update_stock_trades_updated_at ON stock_trades;
            CREATE TRIGGER update_stock_trades_updated_at
                BEFORE UPDATE ON stock_trades
                FOR EACH ROW
                EXECUTE FUNCTION update_updated_at_column()
            """;
        statement.execute(createTriggerSql);
        
        // Add constraint to ensure total_value matches quantity * price
        // PostgreSQL doesn't support IF NOT EXISTS with ADD CONSTRAINT, so we'll handle it differently
        try {
            String totalValueConstraintSql = """
                ALTER TABLE stock_trades
                ADD CONSTRAINT chk_total_value_calculation
                CHECK (ABS(total_value - (quantity * price)) < 0.01)
                """;
            statement.execute(totalValueConstraintSql);
        } catch (SQLException e) {
            // Constraint might already exist, which is fine
            if (!e.getMessage().contains("already exists")) {
                throw e;
            }
            logger.debug("Constraint chk_total_value_calculation already exists, skipping");
        }
        
        logger.debug("Additional constraints created successfully for database: {}", databaseName);
    }
    
    /**
     * Verify that the schema was created correctly
     * 
     * @param connection Database connection
     * @return true if schema is valid, false otherwise
     */
    public boolean verifySchema(Connection connection) {
        logger.debug("Verifying schema for database: {}", databaseName);
        
        try (Statement statement = connection.createStatement()) {
            // Check if stock_trades table exists and has expected structure
            String checkTableSql = """
                SELECT COUNT(*) as table_count
                FROM information_schema.tables 
                WHERE table_name = 'stock_trades' 
                AND table_schema = 'public'
                """;
            
            var resultSet = statement.executeQuery(checkTableSql);
            if (resultSet.next() && resultSet.getInt("table_count") == 1) {
                logger.debug("stock_trades table exists for database: {}", databaseName);
                
                // Check if required columns exist
                String checkColumnsSql = """
                    SELECT COUNT(*) as column_count
                    FROM information_schema.columns 
                    WHERE table_name = 'stock_trades' 
                    AND table_schema = 'public'
                    AND column_name IN ('id', 'symbol', 'trade_type', 'quantity', 'price', 
                                       'total_value', 'trade_date_time', 'trader_id', 'exchange')
                    """;
                
                var columnResultSet = statement.executeQuery(checkColumnsSql);
                if (columnResultSet.next() && columnResultSet.getInt("column_count") == 9) {
                    logger.debug("All required columns exist for database: {}", databaseName);
                    return true;
                } else {
                    logger.warn("Missing required columns in stock_trades table for database: {}", databaseName);
                    return false;
                }
            } else {
                logger.warn("stock_trades table does not exist for database: {}", databaseName);
                return false;
            }
        } catch (SQLException e) {
            logger.error("Failed to verify schema for database: {}", databaseName, e);
            return false;
        }
    }
    
    /**
     * Clean up the database by dropping all tables (for testing cleanup)
     * 
     * @param connection Database connection
     * @throws SQLException if cleanup fails
     */
    public void cleanupSchema(Connection connection) throws SQLException {
        logger.info("Cleaning up schema for database: {}", databaseName);
        
        try (Statement statement = connection.createStatement()) {
            // Drop the trigger first
            statement.execute("DROP TRIGGER IF EXISTS update_stock_trades_updated_at ON stock_trades");
            statement.execute("DROP FUNCTION IF EXISTS update_updated_at_column()");
            
            // Drop the table
            statement.execute("DROP TABLE IF EXISTS stock_trades CASCADE");
            
            logger.info("Schema cleaned up successfully for database: {}", databaseName);
        } catch (SQLException e) {
            logger.error("Failed to cleanup schema for database: {}", databaseName, e);
            throw e;
        }
    }
    
    /**
     * Get the count of records in the stock_trades table
     * 
     * @param connection Database connection
     * @return Number of records in the table
     * @throws SQLException if query fails
     */
    public long getRecordCount(Connection connection) throws SQLException {
        String countSql = "SELECT COUNT(*) as record_count FROM stock_trades";
        
        try (Statement statement = connection.createStatement();
             var resultSet = statement.executeQuery(countSql)) {
            
            if (resultSet.next()) {
                return resultSet.getLong("record_count");
            }
            return 0;
        }
    }
}

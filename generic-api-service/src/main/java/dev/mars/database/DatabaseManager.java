package dev.mars.database;

import dev.mars.common.database.BaseDatabaseManager;
import dev.mars.config.DatabaseConfigAdapter;
import dev.mars.config.GenericApiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database manager for handling database initialization and schema creation
 * Now extends BaseDatabaseManager from common-library
 */
public class DatabaseManager extends BaseDatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private final GenericApiConfig genericApiConfig;

    public DatabaseManager(GenericApiConfig genericApiConfig) {
        super(DatabaseConfigAdapter.createCommonDatabaseConfig(genericApiConfig));
        this.genericApiConfig = genericApiConfig;

        // Ensure database can be created if configured to do so
        if (genericApiConfig.isDatabaseCreateIfMissing()) {
            ensureDatabaseCanBeCreated();
        }
    }

    /**
     * Ensure the database directory exists and database can be created
     */
    private void ensureDatabaseCanBeCreated() {
        String databaseUrl = genericApiConfig.getDatabaseUrl();
        logger.info("Checking if database can be created for URL: {}", databaseUrl);

        // Extract file path from H2 database URL
        if (databaseUrl.startsWith("jdbc:h2:")) {
            String filePath = databaseUrl.substring("jdbc:h2:".length());

            // Remove H2 connection parameters
            int paramIndex = filePath.indexOf(';');
            if (paramIndex > 0) {
                filePath = filePath.substring(0, paramIndex);
            }

            // Skip directory creation for in-memory databases
            if (filePath.startsWith("mem:")) {
                logger.info("In-memory database detected, skipping directory creation");
                return;
            }

            // Handle relative paths
            if (!filePath.startsWith("/") && !filePath.matches("^[A-Za-z]:.*")) {
                // It's a relative path, resolve it from current working directory
                filePath = System.getProperty("user.dir") + File.separator + filePath;
            }

            try {
                Path dbPath = Paths.get(filePath).getParent();
                if (dbPath != null && !Files.exists(dbPath)) {
                    Files.createDirectories(dbPath);
                    logger.info("Created database directory: {}", dbPath.toAbsolutePath());
                } else if (dbPath != null) {
                    logger.info("Database directory already exists: {}", dbPath.toAbsolutePath());
                } else {
                    logger.info("Database will be created in current directory");
                }
            } catch (Exception e) {
                logger.warn("Could not create database directory for path: {}", filePath, e);
                // Don't throw exception here - let H2 handle it during connection
            }
        } else {
            logger.info("Non-file database URL detected, skipping directory creation");
        }
    }

    /**
     * Initialize the database schema for API service configuration
     */
    public void initializeSchema() {
        logger.info("Initializing API service configuration database schema");
        logger.info("Database creation enabled: {}", genericApiConfig.isDatabaseCreateIfMissing());

        String createConfigDatabasesTableSql = """
            CREATE TABLE IF NOT EXISTS config_databases (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL UNIQUE,
                description TEXT,
                url VARCHAR(500) NOT NULL,
                username VARCHAR(255),
                password VARCHAR(255),
                driver VARCHAR(255) NOT NULL,
                maximum_pool_size INTEGER DEFAULT 10,
                minimum_idle INTEGER DEFAULT 2,
                connection_timeout BIGINT DEFAULT 30000,
                idle_timeout BIGINT DEFAULT 600000,
                max_lifetime BIGINT DEFAULT 1800000,
                leak_detection_threshold BIGINT DEFAULT 60000,
                connection_test_query VARCHAR(255) DEFAULT 'SELECT 1',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """;

        String createConfigQueriesTableSql = """
            CREATE TABLE IF NOT EXISTS config_queries (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL UNIQUE,
                description TEXT,
                database_name VARCHAR(255) NOT NULL,
                sql_query TEXT NOT NULL,
                query_type VARCHAR(50) DEFAULT 'SELECT',
                timeout_seconds INTEGER DEFAULT 30,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """;

        String createConfigEndpointsTableSql = """
            CREATE TABLE IF NOT EXISTS config_endpoints (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL UNIQUE,
                description TEXT,
                path VARCHAR(500) NOT NULL,
                method VARCHAR(10) NOT NULL,
                query_name VARCHAR(255) NOT NULL,
                response_format VARCHAR(50) DEFAULT 'json',
                cache_enabled BOOLEAN DEFAULT false,
                cache_ttl_seconds INTEGER DEFAULT 300,
                rate_limit_enabled BOOLEAN DEFAULT false,
                rate_limit_requests INTEGER DEFAULT 100,
                rate_limit_window_seconds INTEGER DEFAULT 60,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """;

        // Configuration tables indexes
        String createIndexSql1 = "CREATE INDEX IF NOT EXISTS idx_config_databases_name ON config_databases(name)";
        String createIndexSql2 = "CREATE INDEX IF NOT EXISTS idx_config_queries_name ON config_queries(name)";
        String createIndexSql3 = "CREATE INDEX IF NOT EXISTS idx_config_queries_database ON config_queries(database_name)";
        String createIndexSql4 = "CREATE INDEX IF NOT EXISTS idx_config_endpoints_name ON config_endpoints(name)";
        String createIndexSql5 = "CREATE INDEX IF NOT EXISTS idx_config_endpoints_path ON config_endpoints(path)";
        String createIndexSql6 = "CREATE INDEX IF NOT EXISTS idx_config_endpoints_query ON config_endpoints(query_name)";

        // Stock trades table for demo data
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

        // Performance metrics table for demo data
        String createPerformanceMetricsTableSql = """
            CREATE TABLE IF NOT EXISTS performance_metrics (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                test_name VARCHAR(255) NOT NULL,
                test_type VARCHAR(100) NOT NULL,
                timestamp TIMESTAMP NOT NULL,
                total_requests INTEGER NOT NULL,
                total_time_ms BIGINT NOT NULL,
                average_response_time_ms DOUBLE NOT NULL,
                concurrent_threads INTEGER NOT NULL,
                requests_per_thread INTEGER NOT NULL,
                page_size INTEGER NOT NULL,
                memory_usage_bytes BIGINT,
                memory_increase_bytes BIGINT,
                test_passed BOOLEAN NOT NULL,
                additional_metrics TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

        // Additional indexes for demo tables
        String createStockTradesIndexes1 = "CREATE INDEX IF NOT EXISTS idx_stock_trades_symbol ON stock_trades(symbol)";
        String createStockTradesIndexes2 = "CREATE INDEX IF NOT EXISTS idx_stock_trades_trader ON stock_trades(trader_id)";
        String createStockTradesIndexes3 = "CREATE INDEX IF NOT EXISTS idx_stock_trades_date ON stock_trades(trade_date_time)";
        String createPerformanceMetricsIndexes1 = "CREATE INDEX IF NOT EXISTS idx_performance_metrics_timestamp ON performance_metrics(timestamp)";
        String createPerformanceMetricsIndexes2 = "CREATE INDEX IF NOT EXISTS idx_performance_metrics_test_name ON performance_metrics(test_name)";

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // Create configuration tables
            statement.execute(createConfigDatabasesTableSql);
            logger.info("Configuration databases table created/verified");

            statement.execute(createConfigQueriesTableSql);
            logger.info("Configuration queries table created/verified");

            statement.execute(createConfigEndpointsTableSql);
            logger.info("Configuration endpoints table created/verified");

            // Create demo data tables
            statement.execute(createStockTradesTableSql);
            logger.info("Stock trades table created/verified");

            statement.execute(createPerformanceMetricsTableSql);
            logger.info("Performance metrics table created/verified");

            // Create indexes
            statement.execute(createIndexSql1);
            statement.execute(createIndexSql2);
            statement.execute(createIndexSql3);
            statement.execute(createIndexSql4);
            statement.execute(createIndexSql5);
            statement.execute(createIndexSql6);
            logger.info("Configuration table indexes created/verified");

            // Create demo table indexes
            statement.execute(createStockTradesIndexes1);
            statement.execute(createStockTradesIndexes2);
            statement.execute(createStockTradesIndexes3);
            statement.execute(createPerformanceMetricsIndexes1);
            statement.execute(createPerformanceMetricsIndexes2);
            logger.info("Demo table indexes created/verified");

        } catch (SQLException e) {
            logger.error("Failed to initialize database schema", e);
            if (genericApiConfig.isDatabaseCreateIfMissing()) {
                logger.error("Database creation was enabled but failed. Check database URL: {}",
                           genericApiConfig.getDatabaseUrl());
                logger.error("Ensure the database directory exists and is writable");
            }
            throw new RuntimeException("Failed to initialize database schema", e);
        }

        // Insert sample data if tables are empty
        insertSampleDataIfNeeded();

        logger.info("API service configuration database schema initialized successfully");
    }

    /**
     * Insert sample data if the tables are empty
     */
    private void insertSampleDataIfNeeded() {
        try (Connection connection = dataSource.getConnection()) {
            // Check if stock_trades table has data
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM stock_trades")) {

                if (rs.next() && rs.getInt(1) == 0) {
                    logger.info("Inserting sample stock trades data");
                    insertSampleStockTrades(connection);
                }
            }

            // Check if performance_metrics table has data
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM performance_metrics")) {

                if (rs.next() && rs.getInt(1) == 0) {
                    logger.info("Inserting sample performance metrics data");
                    insertSamplePerformanceMetrics(connection);
                }
            }

        } catch (SQLException e) {
            logger.warn("Failed to insert sample data", e);
        }
    }

    /**
     * Insert sample stock trades data
     */
    private void insertSampleStockTrades(Connection connection) throws SQLException {
        String insertSql = """
            INSERT INTO stock_trades (symbol, trade_type, quantity, price, total_value, trade_date_time, trader_id, exchange)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
            // Sample data
            Object[][] sampleData = {
                {"AAPL", "BUY", 100, 150.25, 15025.00, "2024-01-15 09:30:00", "TRADER001", "NASDAQ"},
                {"GOOGL", "SELL", 50, 2800.50, 140025.00, "2024-01-15 10:15:00", "TRADER002", "NASDAQ"},
                {"MSFT", "BUY", 200, 375.75, 75150.00, "2024-01-15 11:00:00", "TRADER001", "NASDAQ"},
                {"TSLA", "BUY", 75, 220.30, 16522.50, "2024-01-15 14:30:00", "TRADER003", "NASDAQ"},
                {"AMZN", "SELL", 25, 3200.00, 80000.00, "2024-01-15 15:45:00", "TRADER002", "NASDAQ"},
                {"NVDA", "BUY", 150, 450.80, 67620.00, "2024-01-16 09:45:00", "TRADER004", "NASDAQ"},
                {"META", "SELL", 80, 325.60, 26048.00, "2024-01-16 11:30:00", "TRADER001", "NASDAQ"},
                {"NFLX", "BUY", 60, 480.25, 28815.00, "2024-01-16 13:15:00", "TRADER003", "NASDAQ"},
                {"AMD", "BUY", 300, 125.40, 37620.00, "2024-01-16 14:00:00", "TRADER005", "NASDAQ"},
                {"ORCL", "SELL", 120, 95.75, 11490.00, "2024-01-16 16:00:00", "TRADER002", "NASDAQ"}
            };

            for (Object[] row : sampleData) {
                stmt.setString(1, (String) row[0]);
                stmt.setString(2, (String) row[1]);
                stmt.setInt(3, (Integer) row[2]);
                stmt.setBigDecimal(4, new java.math.BigDecimal(row[3].toString()));
                stmt.setBigDecimal(5, new java.math.BigDecimal(row[4].toString()));
                stmt.setTimestamp(6, java.sql.Timestamp.valueOf((String) row[5]));
                stmt.setString(7, (String) row[6]);
                stmt.setString(8, (String) row[7]);
                stmt.addBatch();
            }

            int[] results = stmt.executeBatch();
            logger.info("Inserted {} sample stock trades", results.length);
        }
    }

    /**
     * Insert sample performance metrics data
     */
    private void insertSamplePerformanceMetrics(Connection connection) throws SQLException {
        String insertSql = """
            INSERT INTO performance_metrics (test_name, test_type, timestamp, total_requests, total_time_ms,
                                           average_response_time_ms, concurrent_threads, requests_per_thread,
                                           page_size, memory_usage_bytes, memory_increase_bytes, test_passed, additional_metrics)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
            // Sample data
            Object[][] sampleData = {
                {"API Load Test", "LOAD", "2024-01-15 10:00:00", 1000, 5000L, 5.0, 10, 100, 20, 512000000L, 50000000L, true, "{'success_rate': 99.5}"},
                {"Stress Test", "STRESS", "2024-01-15 11:00:00", 5000, 25000L, 5.0, 50, 100, 20, 1024000000L, 200000000L, true, "{'max_concurrent': 50}"},
                {"Endurance Test", "ENDURANCE", "2024-01-15 12:00:00", 10000, 60000L, 6.0, 20, 500, 50, 768000000L, 100000000L, true, "{'duration_hours': 1}"},
                {"Spike Test", "SPIKE", "2024-01-15 13:00:00", 2000, 8000L, 4.0, 100, 20, 10, 1536000000L, 300000000L, false, "{'peak_rps': 500}"},
                {"Volume Test", "VOLUME", "2024-01-15 14:00:00", 50000, 120000L, 2.4, 25, 2000, 100, 2048000000L, 500000000L, true, "{'data_volume_gb': 10}"}
            };

            for (Object[] row : sampleData) {
                stmt.setString(1, (String) row[0]);
                stmt.setString(2, (String) row[1]);
                stmt.setTimestamp(3, java.sql.Timestamp.valueOf((String) row[2]));
                stmt.setInt(4, (Integer) row[3]);
                stmt.setLong(5, (Long) row[4]);
                stmt.setDouble(6, (Double) row[5]);
                stmt.setInt(7, (Integer) row[6]);
                stmt.setInt(8, (Integer) row[7]);
                stmt.setInt(9, (Integer) row[8]);
                stmt.setLong(10, (Long) row[9]);
                stmt.setLong(11, (Long) row[10]);
                stmt.setBoolean(12, (Boolean) row[11]);
                stmt.setString(13, (String) row[12]);
                stmt.addBatch();
            }

            int[] results = stmt.executeBatch();
            logger.info("Inserted {} sample performance metrics", results.length);
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

        String deleteConfigDatabasesSql = "DELETE FROM config_databases";
        String deleteConfigQueriesSql = "DELETE FROM config_queries";
        String deleteConfigEndpointsSql = "DELETE FROM config_endpoints";
        String resetConfigDatabasesSequenceSql = "ALTER TABLE config_databases ALTER COLUMN id RESTART WITH 1";
        String resetConfigQueriesSequenceSql = "ALTER TABLE config_queries ALTER COLUMN id RESTART WITH 1";
        String resetConfigEndpointsSequenceSql = "ALTER TABLE config_endpoints ALTER COLUMN id RESTART WITH 1";

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(deleteConfigDatabasesSql);
            statement.execute(resetConfigDatabasesSequenceSql);

            statement.execute(deleteConfigQueriesSql);
            statement.execute(resetConfigQueriesSequenceSql);

            statement.execute(deleteConfigEndpointsSql);
            statement.execute(resetConfigEndpointsSequenceSql);

            logger.info("Database cleaned successfully");

        } catch (SQLException e) {
            logger.error("Failed to clean database", e);
            throw new RuntimeException("Failed to clean database", e);
        }
    }
}

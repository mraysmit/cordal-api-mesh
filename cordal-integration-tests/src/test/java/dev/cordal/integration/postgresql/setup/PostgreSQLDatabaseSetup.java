package dev.cordal.integration.postgresql.setup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

/**
 * PostgreSQL Database Setup Utility for Dual Database Integration Testing
 * 
 * This utility provides methods to:
 * 1. Create PostgreSQL databases
 * 2. Set up schemas and tables
 * 3. Insert sample data
 * 4. Clean up databases
 * 
 * Designed to work with native PostgreSQL installations without TestContainers.
 */
public class PostgreSQLDatabaseSetup {
    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLDatabaseSetup.class);
    
    // Database configuration
    private final String host;
    private final int port;
    private final String adminUsername;
    private final String adminPassword;
    private final String databaseUsername;
    private final String databasePassword;
    
    // Database names
    private final String database1Name;
    private final String database2Name;
    
    // Sample data configuration
    private static final int SAMPLE_DATA_SIZE = 100;
    private static final String[] STOCK_SYMBOLS = {
        "AAPL", "GOOGL", "MSFT", "AMZN", "TSLA", "META", "NVDA", "NFLX",
        "ORCL", "CRM", "ADBE", "INTC", "AMD", "PYPL", "UBER", "LYFT",
        "SPOT", "TWTR", "SNAP", "ZM", "DOCU", "SHOP", "SQ", "ROKU"
    };
    
    private static final String[] TRADER_IDS = {
        "TRADER001", "TRADER002", "TRADER003", "ALGO_TRADER_A", "ALGO_TRADER_B",
        "INSTITUTIONAL_01", "INSTITUTIONAL_02", "RETAIL_TRADER_X", "RETAIL_TRADER_Y",
        "HEDGE_FUND_ALPHA", "HEDGE_FUND_BETA", "PENSION_FUND_01", "MUTUAL_FUND_A",
        "QUANT_TRADER_1", "QUANT_TRADER_2", "DAY_TRADER_01", "SWING_TRADER_01", "SCALPER_01"
    };
    
    private static final String[] EXCHANGES = {"NYSE", "NASDAQ", "AMEX", "CBOE", "BATS"};
    private static final String[] TRADE_TYPES = {"BUY", "SELL"};
    
    public PostgreSQLDatabaseSetup(String host, int port, String adminUsername, String adminPassword,
                                   String databaseUsername, String databasePassword,
                                   String database1Name, String database2Name) {
        this.host = host;
        this.port = port;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.databaseUsername = databaseUsername;
        this.databasePassword = databasePassword;
        this.database1Name = database1Name;
        this.database2Name = database2Name;
    }
    
    /**
     * Set up both databases with schemas and sample data
     */
    public void setupDatabases() throws SQLException {
        logger.info("Starting PostgreSQL dual database setup");
        
        // Create databases
        createDatabase(database1Name);
        createDatabase(database2Name);
        
        // Set up schemas and data
        setupDatabaseSchema(database1Name);
        setupDatabaseSchema(database2Name);
        
        // Insert sample data
        insertSampleData(database1Name);
        insertSampleData(database2Name);
        
        logger.info("PostgreSQL dual database setup completed successfully");
    }
    
    /**
     * Clean up both databases
     */
    public void cleanupDatabases() throws SQLException {
        logger.info("Starting PostgreSQL dual database cleanup");
        
        dropDatabase(database1Name);
        dropDatabase(database2Name);
        
        logger.info("PostgreSQL dual database cleanup completed");
    }
    
    /**
     * Create a PostgreSQL database
     */
    private void createDatabase(String databaseName) throws SQLException {
        logger.info("Creating database: {}", databaseName);
        
        String adminJdbcUrl = String.format("jdbc:postgresql://%s:%d/postgres", host, port);
        
        try (Connection adminConnection = DriverManager.getConnection(adminJdbcUrl, adminUsername, adminPassword)) {
            // Check if database exists
            if (databaseExists(adminConnection, databaseName)) {
                logger.info("Database {} already exists, dropping it first", databaseName);
                dropDatabase(databaseName);
            }
            
            // Create database
            String createDbSql = String.format("CREATE DATABASE \"%s\" OWNER \"%s\"", databaseName, databaseUsername);
            try (Statement stmt = adminConnection.createStatement()) {
                stmt.execute(createDbSql);
                logger.info("Database {} created successfully", databaseName);
            }
        }
    }
    
    /**
     * Drop a PostgreSQL database
     */
    private void dropDatabase(String databaseName) throws SQLException {
        logger.info("Dropping database: {}", databaseName);
        
        String adminJdbcUrl = String.format("jdbc:postgresql://%s:%d/postgres", host, port);
        
        try (Connection adminConnection = DriverManager.getConnection(adminJdbcUrl, adminUsername, adminPassword)) {
            if (!databaseExists(adminConnection, databaseName)) {
                logger.info("Database {} does not exist, skipping drop", databaseName);
                return;
            }
            
            // Terminate active connections to the database
            String terminateConnectionsSql = String.format(
                "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '%s' AND pid <> pg_backend_pid()",
                databaseName
            );
            
            try (Statement stmt = adminConnection.createStatement()) {
                stmt.execute(terminateConnectionsSql);
            }
            
            // Drop database
            String dropDbSql = String.format("DROP DATABASE IF EXISTS \"%s\"", databaseName);
            try (Statement stmt = adminConnection.createStatement()) {
                stmt.execute(dropDbSql);
                logger.info("Database {} dropped successfully", databaseName);
            }
        }
    }
    
    /**
     * Check if a database exists
     */
    private boolean databaseExists(Connection adminConnection, String databaseName) throws SQLException {
        String checkDbSql = "SELECT 1 FROM pg_database WHERE datname = ?";
        try (PreparedStatement stmt = adminConnection.prepareStatement(checkDbSql)) {
            stmt.setString(1, databaseName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
    
    /**
     * Set up database schema (tables, indexes, etc.)
     */
    private void setupDatabaseSchema(String databaseName) throws SQLException {
        logger.info("Setting up schema for database: {}", databaseName);
        
        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, databaseName);
        
        try (Connection connection = DriverManager.getConnection(jdbcUrl, databaseUsername, databasePassword)) {
            // Create stock_trades table
            String createTableSql = """
                CREATE TABLE IF NOT EXISTS stock_trades (
                    id BIGSERIAL PRIMARY KEY,
                    symbol VARCHAR(10) NOT NULL,
                    trade_type VARCHAR(10) NOT NULL,
                    quantity INTEGER NOT NULL,
                    price DECIMAL(10,2) NOT NULL,
                    total_value DECIMAL(15,2) NOT NULL,
                    trade_date_time TIMESTAMP NOT NULL,
                    trader_id VARCHAR(50) NOT NULL,
                    exchange VARCHAR(20) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
            
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(createTableSql);
                logger.info("Created stock_trades table in database: {}", databaseName);
            }
            
            // Create indexes for better performance
            createIndexes(connection, databaseName);
        }
    }
    
    /**
     * Create indexes on the stock_trades table
     */
    private void createIndexes(Connection connection, String databaseName) throws SQLException {
        logger.info("Creating indexes for database: {}", databaseName);
        
        String[] indexSqls = {
            "CREATE INDEX IF NOT EXISTS idx_stock_trades_symbol ON stock_trades(symbol)",
            "CREATE INDEX IF NOT EXISTS idx_stock_trades_trader_id ON stock_trades(trader_id)",
            "CREATE INDEX IF NOT EXISTS idx_stock_trades_trade_date ON stock_trades(trade_date_time)",
            "CREATE INDEX IF NOT EXISTS idx_stock_trades_exchange ON stock_trades(exchange)",
            "CREATE INDEX IF NOT EXISTS idx_stock_trades_trade_type ON stock_trades(trade_type)"
        };
        
        try (Statement stmt = connection.createStatement()) {
            for (String indexSql : indexSqls) {
                stmt.execute(indexSql);
            }
            logger.info("Created indexes for database: {}", databaseName);
        }
    }
    
    /**
     * Insert sample data into the database
     */
    private void insertSampleData(String databaseName) throws SQLException {
        logger.info("Inserting sample data into database: {}", databaseName);
        
        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, databaseName);
        
        try (Connection connection = DriverManager.getConnection(jdbcUrl, databaseUsername, databasePassword)) {
            // Clear existing data
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DELETE FROM stock_trades");
            }
            
            // Generate and insert sample data
            List<StockTrade> trades = generateSampleTrades();
            insertTrades(connection, trades);
            
            logger.info("Inserted {} sample trades into database: {}", trades.size(), databaseName);
        }
    }
    
    /**
     * Generate sample stock trade data
     */
    private List<StockTrade> generateSampleTrades() {
        List<StockTrade> trades = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < SAMPLE_DATA_SIZE; i++) {
            String symbol = STOCK_SYMBOLS[random.nextInt(STOCK_SYMBOLS.length)];
            String tradeType = TRADE_TYPES[random.nextInt(TRADE_TYPES.length)];
            int quantity = 10 + random.nextInt(1000); // 10 to 1010
            double price = getRealisticPrice(symbol, random);
            double totalValue = quantity * price;
            LocalDateTime tradeDateTime = LocalDateTime.now().minusDays(random.nextInt(30));
            String traderId = TRADER_IDS[random.nextInt(TRADER_IDS.length)];
            String exchange = EXCHANGES[random.nextInt(EXCHANGES.length)];
            
            trades.add(new StockTrade(symbol, tradeType, quantity, price, totalValue, 
                                    tradeDateTime, traderId, exchange));
        }
        
        return trades;
    }
    
    /**
     * Get realistic price for a stock symbol
     */
    private double getRealisticPrice(String symbol, Random random) {
        return switch (symbol) {
            case "AAPL" -> 150.0 + random.nextDouble() * 50.0;
            case "GOOGL" -> 2500.0 + random.nextDouble() * 500.0;
            case "MSFT" -> 300.0 + random.nextDouble() * 100.0;
            case "AMZN" -> 3000.0 + random.nextDouble() * 500.0;
            case "TSLA" -> 700.0 + random.nextDouble() * 300.0;
            case "META" -> 200.0 + random.nextDouble() * 100.0;
            case "NVDA" -> 400.0 + random.nextDouble() * 200.0;
            default -> 50.0 + random.nextDouble() * 200.0;
        };
    }
    
    /**
     * Insert trades into the database
     */
    private void insertTrades(Connection connection, List<StockTrade> trades) throws SQLException {
        String insertSql = """
            INSERT INTO stock_trades (symbol, trade_type, quantity, price, total_value, 
                                    trade_date_time, trader_id, exchange) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
            for (StockTrade trade : trades) {
                stmt.setString(1, trade.symbol());
                stmt.setString(2, trade.tradeType());
                stmt.setInt(3, trade.quantity());
                stmt.setBigDecimal(4, java.math.BigDecimal.valueOf(trade.price()));
                stmt.setBigDecimal(5, java.math.BigDecimal.valueOf(trade.totalValue()));
                stmt.setTimestamp(6, Timestamp.valueOf(trade.tradeDateTime()));
                stmt.setString(7, trade.traderId());
                stmt.setString(8, trade.exchange());
                stmt.addBatch();
            }
            
            stmt.executeBatch();
        }
    }
    
    /**
     * Get database connection info for a specific database
     */
    public DatabaseConnectionInfo getConnectionInfo(String databaseName) {
        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, databaseName);
        return new DatabaseConnectionInfo(jdbcUrl, databaseUsername, databasePassword, databaseName, host, port);
    }
    
    /**
     * Test database connectivity
     */
    public boolean testConnection(String databaseName) {
        try {
            String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, databaseName);
            try (Connection connection = DriverManager.getConnection(jdbcUrl, databaseUsername, databasePassword)) {
                try (Statement stmt = connection.createStatement()) {
                    try (ResultSet rs = stmt.executeQuery("SELECT 1")) {
                        return rs.next();
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to test connection to database: {}", databaseName, e);
            return false;
        }
    }
    
    /**
     * Record representing a stock trade
     */
    public record StockTrade(
        String symbol,
        String tradeType,
        int quantity,
        double price,
        double totalValue,
        LocalDateTime tradeDateTime,
        String traderId,
        String exchange
    ) {}
    
    /**
     * Record representing database connection information
     */
    public record DatabaseConnectionInfo(
        String jdbcUrl,
        String username,
        String password,
        String databaseName,
        String host,
        int port
    ) {}
}

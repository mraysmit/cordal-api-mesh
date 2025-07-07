package dev.mars.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class to create the stock_trades table in the H2 stocktrades database
 * and populate it with sample data for testing purposes.
 */
public class StockTradesTableCreator {
    
    private static final String DB_URL = "jdbc:h2:../data/stocktrades;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";
    
    public static void main(String[] args) {
        StockTradesTableCreator creator = new StockTradesTableCreator();
        try {
            creator.createTableAndData();
            System.out.println("✅ SUCCESS: stock_trades table created and populated successfully!");
        } catch (Exception e) {
            System.err.println("❌ ERROR: Failed to create table: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void createTableAndData() throws SQLException {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            System.out.println("🔗 Connected to H2 database: " + DB_URL);
            
            // Create the table
            createStockTradesTable(connection);
            
            // Insert sample data
            insertSampleData(connection);
            
            // Verify the data
            verifyData(connection);
        }
    }
    
    private void createStockTradesTable(Connection connection) throws SQLException {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS stock_trades (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                symbol VARCHAR(10) NOT NULL,
                trade_type VARCHAR(10) NOT NULL,
                quantity INTEGER NOT NULL,
                price DECIMAL(10,2) NOT NULL,
                total_value DECIMAL(15,2) NOT NULL,
                trade_date_time TIMESTAMP NOT NULL,
                trader_id VARCHAR(50) NOT NULL,
                exchange VARCHAR(20) NOT NULL
            )
            """;
        
        try (Statement statement = connection.createStatement()) {
            statement.execute(createTableSQL);
            System.out.println("📋 Created stock_trades table");
        }
    }
    
    private void insertSampleData(Connection connection) throws SQLException {
        String insertSQL = """
            INSERT INTO stock_trades (symbol, trade_type, quantity, price, total_value, trade_date_time, trader_id, exchange)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement statement = connection.prepareStatement(insertSQL)) {
            // Sample data
            Object[][] sampleTrades = {
                {"AAPL", "BUY", 100, 150.25, 15025.00, "2024-01-15 09:30:00", "TRADER001", "NASDAQ"},
                {"GOOGL", "SELL", 50, 2800.50, 140025.00, "2024-01-15 10:15:00", "TRADER002", "NASDAQ"},
                {"MSFT", "BUY", 75, 380.75, 28556.25, "2024-01-15 11:00:00", "TRADER001", "NASDAQ"},
                {"TSLA", "BUY", 25, 220.00, 5500.00, "2024-01-15 14:30:00", "TRADER003", "NASDAQ"},
                {"AMZN", "SELL", 30, 3200.00, 96000.00, "2024-01-15 15:45:00", "TRADER002", "NASDAQ"},
                {"NVDA", "BUY", 40, 450.25, 18010.00, "2024-01-16 09:45:00", "TRADER004", "NASDAQ"},
                {"META", "SELL", 60, 320.75, 19245.00, "2024-01-16 11:30:00", "TRADER001", "NASDAQ"},
                {"NFLX", "BUY", 20, 480.50, 9610.00, "2024-01-16 13:15:00", "TRADER003", "NASDAQ"},
                {"AMD", "BUY", 80, 125.00, 10000.00, "2024-01-16 14:00:00", "TRADER005", "NASDAQ"},
                {"INTC", "SELL", 150, 45.25, 6787.50, "2024-01-16 16:00:00", "TRADER004", "NASDAQ"}
            };
            
            for (Object[] trade : sampleTrades) {
                statement.setString(1, (String) trade[0]);  // symbol
                statement.setString(2, (String) trade[1]);  // trade_type
                statement.setInt(3, (Integer) trade[2]);    // quantity
                statement.setDouble(4, (Double) trade[3]);  // price
                statement.setDouble(5, (Double) trade[4]);  // total_value
                statement.setTimestamp(6, Timestamp.valueOf((String) trade[5])); // trade_date_time
                statement.setString(7, (String) trade[6]);  // trader_id
                statement.setString(8, (String) trade[7]);  // exchange
                
                statement.addBatch();
            }
            
            int[] results = statement.executeBatch();
            System.out.println("📊 Inserted " + results.length + " sample stock trades");
        }
    }
    
    private void verifyData(Connection connection) throws SQLException {
        String countSQL = "SELECT COUNT(*) as total FROM stock_trades";
        String sampleSQL = "SELECT symbol, trade_type, quantity, price FROM stock_trades LIMIT 3";
        
        try (Statement statement = connection.createStatement()) {
            // Count total records
            var countResult = statement.executeQuery(countSQL);
            if (countResult.next()) {
                int total = countResult.getInt("total");
                System.out.println("✅ Verification: " + total + " records in stock_trades table");
            }
            
            // Show sample records
            System.out.println("📋 Sample records:");
            var sampleResult = statement.executeQuery(sampleSQL);
            while (sampleResult.next()) {
                String symbol = sampleResult.getString("symbol");
                String tradeType = sampleResult.getString("trade_type");
                int quantity = sampleResult.getInt("quantity");
                double price = sampleResult.getDouble("price");
                System.out.printf("   - %s %s %d @ $%.2f%n", symbol, tradeType, quantity, price);
            }
        }
    }
}

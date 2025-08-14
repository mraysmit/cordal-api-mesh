package dev.cordal.integration.examples;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * EXAMPLE IMPLEMENTATION: Utility class to check stock_trades table
 * 
 * This class provides example database checking functionality for stock trading data
 * used in integration testing. This is NOT part of the core system and should
 * only be used for integration testing and examples.
 * 
 * Checks if the stock_trades table exists and has data in the H2 stocktrades database.
 */
public class StockTradesChecker {
    
    private static final String DB_URL = "jdbc:h2:../data/stocktrades;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";
    
    public static void main(String[] args) {
        StockTradesChecker checker = new StockTradesChecker();
        try {
            checker.checkDatabase();
        } catch (Exception e) {
            System.err.println("❌ ERROR: Failed to check example database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void checkDatabase() throws SQLException {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            System.out.println("🔗 Connected to H2 example database: " + DB_URL);
            
            // Check if stock_trades table exists
            if (tableExists(connection, "STOCK_TRADES")) {
                System.out.println("✅ Example table 'STOCK_TRADES' exists");
                
                // Count records
                int recordCount = countRecords(connection);
                System.out.println("📊 Example record count: " + recordCount);
                
                if (recordCount > 0) {
                    // Show sample records
                    showSampleRecords(connection);
                } else {
                    System.out.println("⚠️  Example table exists but is empty");
                }
            } else {
                System.out.println("❌ Example table 'STOCK_TRADES' does not exist");
                
                // List all tables
                listAllTables(connection);
            }
        }
    }
    
    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?";
        try (var pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
    
    private int countRecords(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM stock_trades";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
    
    private void showSampleRecords(Connection connection) throws SQLException {
        String sql = "SELECT id, symbol, trade_type, quantity, price, trade_date_time FROM STOCK_TRADES LIMIT 5";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            
            System.out.println("📋 Sample example records:");
            while (rs.next()) {
                long id = rs.getLong("id");
                String symbol = rs.getString("symbol");
                String tradeType = rs.getString("trade_type");
                int quantity = rs.getInt("quantity");
                double price = rs.getDouble("price");
                String dateTime = rs.getString("trade_date_time");
                
                System.out.printf("   - ID: %d, %s %s %d @ $%.2f on %s%n", 
                    id, symbol, tradeType, quantity, price, dateTime);
            }
        }
    }
    
    private void listAllTables(Connection connection) throws SQLException {
        String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            
            System.out.println("📋 Available tables in example database:");
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                System.out.println("   - " + tableName);
            }
        }
    }
}

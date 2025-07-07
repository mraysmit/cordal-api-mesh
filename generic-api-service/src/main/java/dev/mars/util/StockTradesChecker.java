package dev.mars.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility class to check if the stock_trades table exists and has data
 * in the H2 stocktrades database.
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
            System.err.println("❌ ERROR: Failed to check database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void checkDatabase() throws SQLException {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            System.out.println("🔗 Connected to H2 database: " + DB_URL);
            
            // Check if stock_trades table exists
            if (tableExists(connection, "STOCK_TRADES")) {
                System.out.println("✅ Table 'STOCK_TRADES' exists");
                
                // Count records
                int recordCount = countRecords(connection);
                System.out.println("📊 Record count: " + recordCount);
                
                if (recordCount > 0) {
                    // Show sample records
                    showSampleRecords(connection);
                } else {
                    System.out.println("⚠️  Table exists but is empty");
                }
            } else {
                System.out.println("❌ Table 'STOCK_TRADES' does not exist");
                
                // List all tables
                listAllTables(connection);
            }
        }
    }
    
    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?";
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
    
    private int countRecords(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM STOCK_TRADES";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
    
    private void showSampleRecords(Connection connection) throws SQLException {
        String sql = "SELECT id, symbol, trade_type, quantity, price, trade_date_time FROM STOCK_TRADES LIMIT 5";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            
            System.out.println("📋 Sample records:");
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
            
            System.out.println("📋 Available tables in PUBLIC schema:");
            boolean hasAnyTables = false;
            while (rs.next()) {
                hasAnyTables = true;
                String tableName = rs.getString("TABLE_NAME");
                System.out.println("   - " + tableName);
            }
            
            if (!hasAnyTables) {
                System.out.println("   (No user tables found)");
            }
        }
    }
}

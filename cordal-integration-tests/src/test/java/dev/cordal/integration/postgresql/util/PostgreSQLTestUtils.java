package dev.cordal.integration.postgresql.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Utility methods for PostgreSQL integration testing
 * Provides common functionality for database testing, error handling, and diagnostics
 */
public class PostgreSQLTestUtils {
    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLTestUtils.class);
    
    /**
     * Wait for a database connection to become available with retry logic
     * 
     * @param connectionSupplier Supplier that provides database connections
     * @param maxWaitSeconds Maximum time to wait in seconds
     * @param retryIntervalSeconds Interval between retries in seconds
     * @return true if connection became available, false if timeout
     */
    public static boolean waitForDatabaseConnection(
            DatabaseConnectionSupplier connectionSupplier, 
            int maxWaitSeconds, 
            int retryIntervalSeconds) {
        
        logger.info("Waiting for database connection (max: {}s, retry interval: {}s)", 
                   maxWaitSeconds, retryIntervalSeconds);
        
        Instant startTime = Instant.now();
        Duration maxWait = Duration.ofSeconds(maxWaitSeconds);
        Duration retryInterval = Duration.ofSeconds(retryIntervalSeconds);
        
        while (Duration.between(startTime, Instant.now()).compareTo(maxWait) < 0) {
            try (Connection connection = connectionSupplier.getConnection()) {
                // Test the connection
                connection.createStatement().execute("SELECT 1");
                logger.info("Database connection is available");
                return true;
                
            } catch (Exception e) {
                logger.debug("Database connection not yet available: {}", e.getMessage());
                
                try {
                    Thread.sleep(retryInterval.toMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.warn("Interrupted while waiting for database connection");
                    return false;
                }
            }
        }
        
        logger.error("Database connection did not become available within {}s", maxWaitSeconds);
        return false;
    }
    
    /**
     * Execute a SQL query with retry logic for transient failures
     * 
     * @param connection Database connection
     * @param sql SQL query to execute
     * @param maxRetries Maximum number of retries
     * @param retryDelayMs Delay between retries in milliseconds
     * @return true if query executed successfully, false otherwise
     */
    public static boolean executeWithRetry(Connection connection, String sql, int maxRetries, long retryDelayMs) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                connection.createStatement().execute(sql);
                logger.debug("SQL executed successfully on attempt {}: {}", attempt, sql);
                return true;
                
            } catch (SQLException e) {
                logger.warn("SQL execution failed on attempt {} of {}: {} - {}", 
                           attempt, maxRetries, sql, e.getMessage());
                
                if (attempt == maxRetries) {
                    logger.error("SQL execution failed after {} attempts: {}", maxRetries, sql, e);
                    return false;
                }
                
                if (retryDelayMs > 0) {
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.warn("Interrupted during retry delay");
                        return false;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Get database diagnostic information for troubleshooting
     * 
     * @param connection Database connection
     * @param databaseName Name of the database for logging
     * @return Diagnostic information as a formatted string
     */
    public static String getDatabaseDiagnostics(Connection connection, String databaseName) {
        StringBuilder diagnostics = new StringBuilder();
        diagnostics.append("=== Database Diagnostics for ").append(databaseName).append(" ===\n");
        
        try {
            // Database metadata
            var metaData = connection.getMetaData();
            diagnostics.append("Database Product: ").append(metaData.getDatabaseProductName())
                      .append(" ").append(metaData.getDatabaseProductVersion()).append("\n");
            diagnostics.append("Driver: ").append(metaData.getDriverName())
                      .append(" ").append(metaData.getDriverVersion()).append("\n");
            diagnostics.append("URL: ").append(metaData.getURL()).append("\n");
            diagnostics.append("Username: ").append(metaData.getUserName()).append("\n");
            
            // Connection status
            diagnostics.append("Connection Valid: ").append(connection.isValid(5)).append("\n");
            diagnostics.append("Auto Commit: ").append(connection.getAutoCommit()).append("\n");
            diagnostics.append("Read Only: ").append(connection.isReadOnly()).append("\n");
            
            // Table information
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'");
                 ResultSet resultSet = statement.executeQuery()) {
                
                List<String> tables = new ArrayList<>();
                while (resultSet.next()) {
                    tables.add(resultSet.getString("table_name"));
                }
                diagnostics.append("Tables: ").append(tables).append("\n");
            }
            
            // Stock trades table info if it exists
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*) as count FROM stock_trades");
                 ResultSet resultSet = statement.executeQuery()) {
                
                if (resultSet.next()) {
                    diagnostics.append("Stock Trades Count: ").append(resultSet.getLong("count")).append("\n");
                }
            } catch (SQLException e) {
                diagnostics.append("Stock Trades Table: Not accessible (").append(e.getMessage()).append(")\n");
            }
            
        } catch (SQLException e) {
            diagnostics.append("Error getting diagnostics: ").append(e.getMessage()).append("\n");
        }
        
        diagnostics.append("=== End Diagnostics ===");
        return diagnostics.toString();
    }
    
    /**
     * Validate that a table has the expected structure
     * 
     * @param connection Database connection
     * @param tableName Name of the table to validate
     * @param expectedColumns List of expected column names
     * @return true if table structure is valid, false otherwise
     */
    public static boolean validateTableStructure(Connection connection, String tableName, List<String> expectedColumns) {
        logger.debug("Validating table structure for: {}", tableName);
        
        try {
            String columnQuery = """
                SELECT column_name 
                FROM information_schema.columns 
                WHERE table_name = ? AND table_schema = 'public'
                ORDER BY ordinal_position
                """;
            
            List<String> actualColumns = new ArrayList<>();
            
            try (PreparedStatement statement = connection.prepareStatement(columnQuery)) {
                statement.setString(1, tableName);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        actualColumns.add(resultSet.getString("column_name"));
                    }
                }
            }
            
            boolean structureValid = actualColumns.containsAll(expectedColumns);
            
            if (!structureValid) {
                logger.error("Table structure validation failed for {}", tableName);
                logger.error("Expected columns: {}", expectedColumns);
                logger.error("Actual columns: {}", actualColumns);
            } else {
                logger.debug("Table structure validation passed for {}", tableName);
            }
            
            return structureValid;
            
        } catch (SQLException e) {
            logger.error("Failed to validate table structure for {}", tableName, e);
            return false;
        }
    }
    
    /**
     * Clean up database resources safely
     * 
     * @param connection Database connection to close
     * @param databaseName Name of the database for logging
     */
    public static void cleanupDatabaseResources(Connection connection, String databaseName) {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    logger.debug("Closed database connection for: {}", databaseName);
                }
            } catch (SQLException e) {
                logger.warn("Error closing database connection for {}: {}", databaseName, e.getMessage());
            }
        }
    }
    
    /**
     * Measure the execution time of a database operation
     * 
     * @param operation Database operation to measure
     * @param operationName Name of the operation for logging
     * @return Execution time in milliseconds, or -1 if operation failed
     */
    public static long measureDatabaseOperation(DatabaseOperation operation, String operationName) {
        logger.debug("Measuring execution time for: {}", operationName);
        
        long startTime = System.currentTimeMillis();
        
        try {
            operation.execute();
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            logger.debug("Database operation '{}' completed in {}ms", operationName, executionTime);
            return executionTime;
            
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            logger.error("Database operation '{}' failed after {}ms: {}", 
                        operationName, executionTime, e.getMessage());
            return -1;
        }
    }
    
    /**
     * Check if a database is responsive within a timeout
     * 
     * @param connection Database connection
     * @param timeoutSeconds Timeout in seconds
     * @return true if database is responsive, false otherwise
     */
    public static boolean isDatabaseResponsive(Connection connection, int timeoutSeconds) {
        try {
            return connection.isValid(timeoutSeconds);
        } catch (SQLException e) {
            logger.debug("Database responsiveness check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get performance statistics for a table
     * 
     * @param connection Database connection
     * @param tableName Name of the table
     * @return Performance statistics as a formatted string
     */
    public static String getTablePerformanceStats(Connection connection, String tableName) {
        StringBuilder stats = new StringBuilder();
        stats.append("=== Performance Stats for ").append(tableName).append(" ===\n");
        
        try {
            // Record count
            String countSql = "SELECT COUNT(*) as count FROM " + tableName;
            try (PreparedStatement statement = connection.prepareStatement(countSql);
                 ResultSet resultSet = statement.executeQuery()) {
                
                if (resultSet.next()) {
                    stats.append("Record Count: ").append(resultSet.getLong("count")).append("\n");
                }
            }
            
            // Table size (PostgreSQL specific)
            String sizeSql = "SELECT pg_size_pretty(pg_total_relation_size(?)) as size";
            try (PreparedStatement statement = connection.prepareStatement(sizeSql)) {
                statement.setString(1, tableName);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        stats.append("Table Size: ").append(resultSet.getString("size")).append("\n");
                    }
                }
            } catch (SQLException e) {
                stats.append("Table Size: Not available (").append(e.getMessage()).append(")\n");
            }
            
        } catch (SQLException e) {
            stats.append("Error getting performance stats: ").append(e.getMessage()).append("\n");
        }
        
        stats.append("=== End Performance Stats ===");
        return stats.toString();
    }
    
    /**
     * Functional interface for database connection suppliers
     */
    @FunctionalInterface
    public interface DatabaseConnectionSupplier {
        Connection getConnection() throws SQLException;
    }
    
    /**
     * Functional interface for database operations
     */
    @FunctionalInterface
    public interface DatabaseOperation {
        void execute() throws Exception;
    }
}

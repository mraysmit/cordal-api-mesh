package dev.cordal.integration.postgresql.data;

import com.fasterxml.jackson.databind.JsonNode;
import dev.cordal.integration.postgresql.client.StockTradesApiClient;
import dev.cordal.integration.postgresql.container.PostgreSQLContainerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Validates data consistency and integrity across dual PostgreSQL databases
 * Compares database content with API responses and validates cross-database consistency
 */
public class DatabaseDataValidator {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseDataValidator.class);
    
    private final PostgreSQLContainerManager containerManager;
    private final StockTradesApiClient apiClient;
    
    public DatabaseDataValidator(PostgreSQLContainerManager containerManager, StockTradesApiClient apiClient) {
        this.containerManager = containerManager;
        this.apiClient = apiClient;
    }
    
    /**
     * Validate data consistency within a single database
     * 
     * @param databaseName Name of the database to validate
     * @return true if data is consistent, false otherwise
     */
    public boolean validateDatabaseConsistency(String databaseName) {
        logger.info("Validating data consistency for database: {}", databaseName);
        
        try (Connection connection = containerManager.getConnection(databaseName)) {
            // Check for data integrity constraints
            boolean constraintsValid = validateDataConstraints(connection, databaseName);
            
            // Check for data completeness
            boolean dataComplete = validateDataCompleteness(connection, databaseName);
            
            // Check for data quality
            boolean dataQuality = validateDataQuality(connection, databaseName);
            
            boolean consistent = constraintsValid && dataComplete && dataQuality;
            
            logger.info("Database consistency validation for {}: {} (constraints: {}, completeness: {}, quality: {})",
                       databaseName, consistent ? "PASSED" : "FAILED", 
                       constraintsValid, dataComplete, dataQuality);
            
            return consistent;
            
        } catch (SQLException e) {
            logger.error("Failed to validate database consistency for: {}", databaseName, e);
            return false;
        }
    }
    
    /**
     * Compare data structures and patterns between two databases
     * 
     * @param database1Name Name of the first database
     * @param database2Name Name of the second database
     * @return true if structures match, false otherwise
     */
    public boolean compareDataStructures(String database1Name, String database2Name) {
        logger.info("Comparing data structures between {} and {}", database1Name, database2Name);
        
        try (Connection conn1 = containerManager.getConnection(database1Name);
             Connection conn2 = containerManager.getConnection(database2Name)) {
            
            // Compare record counts
            boolean countsMatch = compareRecordCounts(conn1, conn2, database1Name, database2Name);
            
            // Compare data distributions
            boolean distributionsMatch = compareDataDistributions(conn1, conn2, database1Name, database2Name);
            
            // Compare schema structures
            boolean schemasMatch = compareSchemaStructures(conn1, conn2, database1Name, database2Name);
            
            boolean structuresMatch = countsMatch && distributionsMatch && schemasMatch;
            
            logger.info("Data structure comparison: {} (counts: {}, distributions: {}, schemas: {})",
                       structuresMatch ? "MATCH" : "MISMATCH", 
                       countsMatch, distributionsMatch, schemasMatch);
            
            return structuresMatch;
            
        } catch (SQLException e) {
            logger.error("Failed to compare data structures between {} and {}", database1Name, database2Name, e);
            return false;
        }
    }
    
    /**
     * Validate that API responses match database content
     * 
     * @param databaseName Name of the database
     * @param pathPrefix API path prefix for the database
     * @return true if API responses match database content, false otherwise
     */
    public boolean validateApiResponsesMatchDatabase(String databaseName, String pathPrefix) {
        logger.info("Validating API responses match database content for: {} (path: {})", databaseName, pathPrefix);
        
        try {
            // Validate total count matches
            boolean countMatches = validateApiCountMatchesDatabase(databaseName, pathPrefix);
            
            // Validate sample data matches
            boolean dataMatches = validateApiDataMatchesDatabase(databaseName, pathPrefix);
            
            // Validate filtering works correctly
            boolean filteringWorks = validateApiFilteringMatchesDatabase(databaseName, pathPrefix);
            
            boolean apiMatches = countMatches && dataMatches && filteringWorks;
            
            logger.info("API-Database validation for {}: {} (count: {}, data: {}, filtering: {})",
                       databaseName, apiMatches ? "PASSED" : "FAILED",
                       countMatches, dataMatches, filteringWorks);
            
            return apiMatches;
            
        } catch (Exception e) {
            logger.error("Failed to validate API responses for database: {}", databaseName, e);
            return false;
        }
    }
    
    /**
     * Validate data constraints (foreign keys, check constraints, etc.)
     */
    private boolean validateDataConstraints(Connection connection, String databaseName) throws SQLException {
        logger.debug("Validating data constraints for: {}", databaseName);
        
        // Check that all total_value calculations are correct
        String constraintCheckSql = """
            SELECT COUNT(*) as violation_count
            FROM stock_trades 
            WHERE ABS(total_value - (quantity * price)) >= 0.01
            """;
        
        try (PreparedStatement statement = connection.prepareStatement(constraintCheckSql);
             ResultSet resultSet = statement.executeQuery()) {
            
            if (resultSet.next()) {
                int violations = resultSet.getInt("violation_count");
                if (violations > 0) {
                    logger.error("Found {} total_value calculation violations in {}", violations, databaseName);
                    return false;
                }
            }
        }
        
        // Check for valid trade types
        String tradeTypeCheckSql = """
            SELECT COUNT(*) as invalid_count
            FROM stock_trades 
            WHERE trade_type NOT IN ('BUY', 'SELL')
            """;
        
        try (PreparedStatement statement = connection.prepareStatement(tradeTypeCheckSql);
             ResultSet resultSet = statement.executeQuery()) {
            
            if (resultSet.next()) {
                int invalid = resultSet.getInt("invalid_count");
                if (invalid > 0) {
                    logger.error("Found {} invalid trade types in {}", invalid, databaseName);
                    return false;
                }
            }
        }
        
        logger.debug("Data constraints validation passed for: {}", databaseName);
        return true;
    }
    
    /**
     * Validate data completeness (no null values where not expected, etc.)
     */
    private boolean validateDataCompleteness(Connection connection, String databaseName) throws SQLException {
        logger.debug("Validating data completeness for: {}", databaseName);
        
        // Check for null values in required fields
        String nullCheckSql = """
            SELECT COUNT(*) as null_count
            FROM stock_trades 
            WHERE symbol IS NULL 
               OR trade_type IS NULL 
               OR quantity IS NULL 
               OR price IS NULL 
               OR total_value IS NULL 
               OR trade_date_time IS NULL 
               OR trader_id IS NULL 
               OR exchange IS NULL
            """;
        
        try (PreparedStatement statement = connection.prepareStatement(nullCheckSql);
             ResultSet resultSet = statement.executeQuery()) {
            
            if (resultSet.next()) {
                int nullCount = resultSet.getInt("null_count");
                if (nullCount > 0) {
                    logger.error("Found {} records with null required fields in {}", nullCount, databaseName);
                    return false;
                }
            }
        }
        
        logger.debug("Data completeness validation passed for: {}", databaseName);
        return true;
    }
    
    /**
     * Validate data quality (reasonable values, formats, etc.)
     */
    private boolean validateDataQuality(Connection connection, String databaseName) throws SQLException {
        logger.debug("Validating data quality for: {}", databaseName);
        
        // Check for reasonable price ranges
        String priceCheckSql = """
            SELECT COUNT(*) as invalid_count
            FROM stock_trades 
            WHERE price <= 0 OR price > 10000
            """;
        
        try (PreparedStatement statement = connection.prepareStatement(priceCheckSql);
             ResultSet resultSet = statement.executeQuery()) {
            
            if (resultSet.next()) {
                int invalid = resultSet.getInt("invalid_count");
                if (invalid > 0) {
                    logger.warn("Found {} records with unusual prices in {}", invalid, databaseName);
                    // Don't fail for this - just warn
                }
            }
        }
        
        // Check for reasonable quantities
        String quantityCheckSql = """
            SELECT COUNT(*) as invalid_count
            FROM stock_trades 
            WHERE quantity <= 0 OR quantity > 1000000
            """;
        
        try (PreparedStatement statement = connection.prepareStatement(quantityCheckSql);
             ResultSet resultSet = statement.executeQuery()) {
            
            if (resultSet.next()) {
                int invalid = resultSet.getInt("invalid_count");
                if (invalid > 0) {
                    logger.warn("Found {} records with unusual quantities in {}", invalid, databaseName);
                    // Don't fail for this - just warn
                }
            }
        }
        
        logger.debug("Data quality validation passed for: {}", databaseName);
        return true;
    }
    
    /**
     * Compare record counts between two databases
     */
    private boolean compareRecordCounts(Connection conn1, Connection conn2, String db1Name, String db2Name) throws SQLException {
        String countSql = "SELECT COUNT(*) as record_count FROM stock_trades";
        
        long count1, count2;
        
        try (PreparedStatement statement = conn1.prepareStatement(countSql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            count1 = resultSet.getLong("record_count");
        }
        
        try (PreparedStatement statement = conn2.prepareStatement(countSql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            count2 = resultSet.getLong("record_count");
        }
        
        boolean countsMatch = count1 == count2;
        logger.info("Record count comparison: {} = {}, {} = {} (match: {})",
                   db1Name, count1, db2Name, count2, countsMatch);

        return countsMatch;
    }

    /**
     * Compare data distributions between two databases
     */
    private boolean compareDataDistributions(Connection conn1, Connection conn2, String db1Name, String db2Name) throws SQLException {
        logger.debug("Comparing data distributions between {} and {}", db1Name, db2Name);

        // Compare symbol distributions
        boolean symbolDistributionsMatch = compareSymbolDistributions(conn1, conn2, db1Name, db2Name);

        // Compare trader distributions
        boolean traderDistributionsMatch = compareTraderDistributions(conn1, conn2, db1Name, db2Name);

        boolean distributionsMatch = symbolDistributionsMatch && traderDistributionsMatch;
        logger.debug("Data distributions comparison: {} (symbols: {}, traders: {})",
                    distributionsMatch, symbolDistributionsMatch, traderDistributionsMatch);

        return distributionsMatch;
    }

    /**
     * Compare schema structures between two databases
     */
    private boolean compareSchemaStructures(Connection conn1, Connection conn2, String db1Name, String db2Name) throws SQLException {
        logger.debug("Comparing schema structures between {} and {}", db1Name, db2Name);

        // For this test, we know both databases have the same schema since we created them
        // In a real scenario, you would compare column names, types, constraints, etc.

        String columnCheckSql = """
            SELECT column_name, data_type, is_nullable
            FROM information_schema.columns
            WHERE table_name = 'stock_trades'
            AND table_schema = 'public'
            ORDER BY ordinal_position
            """;

        List<String> schema1 = new ArrayList<>();
        List<String> schema2 = new ArrayList<>();

        try (PreparedStatement statement = conn1.prepareStatement(columnCheckSql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                schema1.add(String.format("%s:%s:%s",
                    resultSet.getString("column_name"),
                    resultSet.getString("data_type"),
                    resultSet.getString("is_nullable")));
            }
        }

        try (PreparedStatement statement = conn2.prepareStatement(columnCheckSql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                schema2.add(String.format("%s:%s:%s",
                    resultSet.getString("column_name"),
                    resultSet.getString("data_type"),
                    resultSet.getString("is_nullable")));
            }
        }

        boolean schemasMatch = schema1.equals(schema2);
        logger.debug("Schema structures comparison: {} ({} columns each)", schemasMatch, schema1.size());

        return schemasMatch;
    }

    /**
     * Compare symbol distributions between databases
     */
    private boolean compareSymbolDistributions(Connection conn1, Connection conn2, String db1Name, String db2Name) throws SQLException {
        String symbolDistSql = """
            SELECT symbol, COUNT(*) as count
            FROM stock_trades
            GROUP BY symbol
            ORDER BY symbol
            """;

        Map<String, Long> dist1 = new HashMap<>();
        Map<String, Long> dist2 = new HashMap<>();

        try (PreparedStatement statement = conn1.prepareStatement(symbolDistSql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                dist1.put(resultSet.getString("symbol"), resultSet.getLong("count"));
            }
        }

        try (PreparedStatement statement = conn2.prepareStatement(symbolDistSql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                dist2.put(resultSet.getString("symbol"), resultSet.getLong("count"));
            }
        }

        // For test data generated with the same seed, distributions should be identical
        boolean distributionsMatch = dist1.equals(dist2);
        logger.debug("Symbol distributions: {} has {} symbols, {} has {} symbols (match: {})",
                    db1Name, dist1.size(), db2Name, dist2.size(), distributionsMatch);

        return distributionsMatch;
    }

    /**
     * Compare trader distributions between databases
     */
    private boolean compareTraderDistributions(Connection conn1, Connection conn2, String db1Name, String db2Name) throws SQLException {
        String traderDistSql = """
            SELECT trader_id, COUNT(*) as count
            FROM stock_trades
            GROUP BY trader_id
            ORDER BY trader_id
            """;

        Map<String, Long> dist1 = new HashMap<>();
        Map<String, Long> dist2 = new HashMap<>();

        try (PreparedStatement statement = conn1.prepareStatement(traderDistSql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                dist1.put(resultSet.getString("trader_id"), resultSet.getLong("count"));
            }
        }

        try (PreparedStatement statement = conn2.prepareStatement(traderDistSql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                dist2.put(resultSet.getString("trader_id"), resultSet.getLong("count"));
            }
        }

        boolean distributionsMatch = dist1.equals(dist2);
        logger.debug("Trader distributions: {} has {} traders, {} has {} traders (match: {})",
                    db1Name, dist1.size(), db2Name, dist2.size(), distributionsMatch);

        return distributionsMatch;
    }

    /**
     * Validate that API count matches database count
     */
    private boolean validateApiCountMatchesDatabase(String databaseName, String pathPrefix) throws Exception {
        // Get count from API
        JsonNode apiResponse = apiClient.getAllStockTrades(pathPrefix, 0, 1);
        if (!apiResponse.has("pagination") || !apiResponse.get("pagination").has("totalElements")) {
            logger.error("API response missing pagination info for {}", databaseName);
            return false;
        }

        long apiCount = apiResponse.get("pagination").get("totalElements").asLong();

        // Get count from database
        try (Connection connection = containerManager.getConnection(databaseName);
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) as count FROM stock_trades");
             ResultSet resultSet = statement.executeQuery()) {

            resultSet.next();
            long dbCount = resultSet.getLong("count");

            boolean countsMatch = apiCount == dbCount;
            logger.debug("Count validation for {}: API = {}, DB = {} (match: {})",
                        databaseName, apiCount, dbCount, countsMatch);

            return countsMatch;
        }
    }

    /**
     * Validate that API data matches database data
     */
    private boolean validateApiDataMatchesDatabase(String databaseName, String pathPrefix) throws Exception {
        // Get sample data from API
        JsonNode apiResponse = apiClient.getAllStockTrades(pathPrefix, 0, 10);
        if (!apiResponse.has("data")) {
            logger.error("API response missing data for {}", databaseName);
            return false;
        }

        JsonNode apiData = apiResponse.get("data");
        if (apiData.size() == 0) {
            logger.warn("No data returned from API for {}", databaseName);
            return true; // Empty is valid
        }

        // Get corresponding data from database
        String dbSql = """
            SELECT id, symbol, trade_type, quantity, price, total_value,
                   trade_date_time, trader_id, exchange
            FROM stock_trades
            ORDER BY trade_date_time DESC
            LIMIT 10
            """;

        try (Connection connection = containerManager.getConnection(databaseName);
             PreparedStatement statement = connection.prepareStatement(dbSql);
             ResultSet resultSet = statement.executeQuery()) {

            int index = 0;
            while (resultSet.next() && index < apiData.size()) {
                JsonNode apiRecord = apiData.get(index);

                // Compare key fields
                if (!apiRecord.get("symbol").asText().equals(resultSet.getString("symbol")) ||
                    !apiRecord.get("trade_type").asText().equals(resultSet.getString("trade_type")) ||
                    apiRecord.get("quantity").asInt() != resultSet.getInt("quantity")) {

                    logger.error("Data mismatch at index {} for {}", index, databaseName);
                    return false;
                }

                index++;
            }

            logger.debug("API data matches database data for {} ({} records checked)", databaseName, index);
            return true;
        }
    }

    /**
     * Validate that API filtering matches database filtering
     */
    private boolean validateApiFilteringMatchesDatabase(String databaseName, String pathPrefix) throws Exception {
        // Test symbol filtering
        String testSymbol = PostgreSQLTestDataGenerator.getTestStockSymbols().get(0);

        JsonNode apiResponse = apiClient.getStockTradesBySymbol(pathPrefix, testSymbol, 0, 100);
        if (!apiResponse.has("data")) {
            logger.error("API symbol filtering response missing data for {}", databaseName);
            return false;
        }

        // Verify all returned records have the correct symbol
        for (JsonNode record : apiResponse.get("data")) {
            if (!record.get("symbol").asText().equals(testSymbol)) {
                logger.error("Symbol filtering failed for {}: expected {}, got {}",
                           databaseName, testSymbol, record.get("symbol").asText());
                return false;
            }
        }

        logger.debug("API filtering validation passed for {} (symbol: {})", databaseName, testSymbol);
        return true;
    }
}

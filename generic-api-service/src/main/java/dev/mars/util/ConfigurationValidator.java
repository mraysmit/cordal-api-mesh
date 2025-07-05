package dev.mars.util;

import dev.mars.generic.config.EndpointConfigurationManager;
import dev.mars.generic.database.DatabaseConnectionManager;
import dev.mars.generic.config.DatabaseConfig;
import dev.mars.generic.config.QueryConfig;
import dev.mars.generic.config.ApiEndpointConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * Configuration validation service for validating configuration chains and database schemas
 */
public class ConfigurationValidator {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationValidator.class);

    private final EndpointConfigurationManager configurationManager;
    private final DatabaseConnectionManager databaseConnectionManager;

    public ConfigurationValidator(EndpointConfigurationManager configurationManager,
                                DatabaseConnectionManager databaseConnectionManager) {
        this.configurationManager = configurationManager;
        this.databaseConnectionManager = databaseConnectionManager;
    }

    /**
     * Validate the configuration chain: endpoints -> queries -> databases
     */
    public ValidationResult validateConfigurationChain() {
        ValidationResult result = new ValidationResult();

        // Load all configurations
        Map<String, DatabaseConfig> databases = configurationManager.getAllDatabaseConfigurations();
        Map<String, QueryConfig> queries = configurationManager.getAllQueryConfigurations();
        Map<String, ApiEndpointConfig> endpoints = configurationManager.getAllEndpointConfigurations();

        logger.info("[CHECK] Loaded {} databases, {} queries, {} endpoints",
                databases.size(), queries.size(), endpoints.size());

        // Validate endpoint -> query dependencies
        logger.info("[CHECK] Validating endpoint -> query dependencies...");
        for (Map.Entry<String, ApiEndpointConfig> entry : endpoints.entrySet()) {
            String endpointName = entry.getKey();
            ApiEndpointConfig endpoint = entry.getValue();

            String queryName = endpoint.getQuery();
            if (queryName == null || queryName.trim().isEmpty()) {
                result.addError("Endpoint '" + endpointName + "' has no query defined");
                continue;
            }

            if (!queries.containsKey(queryName)) {
                result.addError("Endpoint '" + endpointName + "' references non-existent query: " + queryName);
            } else {
                result.addSuccess("Endpoint '" + endpointName + "' -> query '" + queryName + "' [OK]");
            }

            // Check count query for paginated endpoints
            if (endpoint.getPagination() != null && endpoint.getPagination().isEnabled()) {
                String countQuery = endpoint.getCountQuery();
                if (countQuery != null && !queries.containsKey(countQuery)) {
                    result.addError("Endpoint '" + endpointName + "' references non-existent count query: " + countQuery);
                }
            }
        }

        // Validate query -> database dependencies
        logger.info("[CHECK] Validating query -> database dependencies...");
        for (Map.Entry<String, QueryConfig> entry : queries.entrySet()) {
            String queryName = entry.getKey();
            QueryConfig query = entry.getValue();

            String databaseName = query.getDatabase();
            if (databaseName == null || databaseName.trim().isEmpty()) {
                result.addError("Query '" + queryName + "' has no database defined");
                continue;
            }

            if (!databases.containsKey(databaseName)) {
                result.addError("Query '" + queryName + "' references non-existent database: " + databaseName);
            } else {
                result.addSuccess("Query '" + queryName + "' -> database '" + databaseName + "' [OK]");
            }
        }

        return result;
    }

    /**
     * Validate database schema: tables, fields, and query compatibility
     */
    public ValidationResult validateDatabaseSchema() {
        ValidationResult result = new ValidationResult();

        Map<String, QueryConfig> queries = configurationManager.getAllQueryConfigurations();
        Map<String, DatabaseConfig> databases = configurationManager.getAllDatabaseConfigurations();

        // Group queries by database for efficient validation
        Map<String, List<QueryConfig>> queriesByDatabase = new HashMap<>();
        for (QueryConfig query : queries.values()) {
            String databaseName = query.getDatabase();
            queriesByDatabase.computeIfAbsent(databaseName, k -> new ArrayList<>()).add(query);
        }

        // Validate each database and its queries
        for (Map.Entry<String, List<QueryConfig>> entry : queriesByDatabase.entrySet()) {
            String databaseName = entry.getKey();
            List<QueryConfig> databaseQueries = entry.getValue();

            logger.info("[CHECK] Validating database '{}' with {} queries...", databaseName, databaseQueries.size());

            try {
                DataSource dataSource = databaseConnectionManager.getDataSource(databaseName);
                if (dataSource == null) {
                    result.addError("Database '" + databaseName + "' data source not available");
                    continue;
                }

                try (Connection connection = dataSource.getConnection()) {
                    DatabaseMetaData metaData = connection.getMetaData();

                    // Validate each query for this database
                    for (QueryConfig query : databaseQueries) {
                        validateQuerySchema(query, metaData, result);
                    }

                    result.addSuccess("Database '" + databaseName + "' schema validation completed");

                } catch (SQLException e) {
                    result.addError("Failed to connect to database '" + databaseName + "': " + e.getMessage());
                }

            } catch (Exception e) {
                result.addError("Error validating database '" + databaseName + "': " + e.getMessage());
            }
        }

        return result;
    }

    /**
     * Validate a specific query against database schema
     */
    private void validateQuerySchema(QueryConfig query, DatabaseMetaData metaData, ValidationResult result) {
        try {
            String queryName = query.getName();
            String sql = query.getSql();

            // Extract table names from SQL
            Set<String> referencedTables = SqlAnalyzer.extractTableNamesFromSql(sql);

            // Validate each referenced table exists
            for (String tableName : referencedTables) {
                if (SqlAnalyzer.tableExists(metaData, tableName)) {
                    result.addSuccess("Query '" + queryName + "' -> table '" + tableName + "' [EXISTS]");

                    // Extract and validate column names
                    Set<String> referencedColumns = SqlAnalyzer.extractColumnNamesFromSql(sql, tableName);
                    SqlAnalyzer.validateTableColumns(metaData, tableName, referencedColumns, queryName, result);

                } else {
                    result.addError("Query '" + queryName + "' references non-existent table: " + tableName);
                }
            }

            // Validate query parameters
            SqlAnalyzer.validateQueryParameters(query, result);

        } catch (Exception e) {
            result.addError("Error validating query '" + query.getName() + "': " + e.getMessage());
        }
    }

    /**
     * Display validation results in a formatted way
     */
    public void displayValidationResults(String validationType, ValidationResult result) {
        logger.info("[RESULTS] {} Validation Results:", validationType);
        logger.info("+------------------------------------------------------------------------------+");
        logger.info("| Type: {} | Success: {} | Errors: {} |",
                String.format("%-20s", validationType),
                String.format("%7d", result.getSuccessCount()),
                String.format("%6d", result.getErrorCount()));
        logger.info("+------------------------------------------------------------------------------+");

        // Display errors first
        if (!result.getErrors().isEmpty()) {
            logger.info("| ERRORS:");
            for (String error : result.getErrors()) {
                logger.info("| [ERROR] {}", error);
            }
        }

        // Display successes (limit to first 10 to avoid spam)
        if (!result.getSuccesses().isEmpty()) {
            logger.info("| SUCCESSES:");
            List<String> successes = result.getSuccesses();
            int displayCount = Math.min(successes.size(), 10);
            for (int i = 0; i < displayCount; i++) {
                logger.info("| [OK] {}", successes.get(i));
            }
            if (successes.size() > 10) {
                logger.info("| ... and {} more successful validations", successes.size() - 10);
            }
        }

        logger.info("+------------------------------------------------------------------------------+");

        if (result.isSuccess()) {
            logger.info("[SUCCESS] {} validation passed", validationType);
        } else {
            logger.info("[FAILED] {} validation failed with {} errors", validationType, result.getErrorCount());
        }
    }
}

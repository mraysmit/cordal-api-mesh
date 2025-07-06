package dev.mars.util;

import dev.mars.generic.config.EndpointConfigurationManager;
import dev.mars.generic.database.DatabaseConnectionManager;
import dev.mars.generic.config.DatabaseConfig;
import dev.mars.generic.config.QueryConfig;
import dev.mars.generic.config.ApiEndpointConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;

/**
 * Configuration validation service for validating configuration chains and database schemas
 */
public class ConfigurationValidator {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationValidator.class);

    private final EndpointConfigurationManager configurationManager;
    private final DatabaseConnectionManager databaseConnectionManager;
    private final HttpClient httpClient;

    public ConfigurationValidator(EndpointConfigurationManager configurationManager,
                                DatabaseConnectionManager databaseConnectionManager) {
        this.configurationManager = configurationManager;
        this.databaseConnectionManager = databaseConnectionManager;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
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

            // Get database configuration to extract schema information
            DatabaseConfig databaseConfig = databases.get(databaseName);
            String schemaInfo = extractSchemaFromDatabase(databaseConfig);

            logger.info("[CHECK] Validating database '{}'{} with {} queries...",
                    databaseName, schemaInfo, databaseQueries.size());

            // Check if database is available first
            if (!databaseConnectionManager.isDatabaseAvailable(databaseName)) {
                String failureReason = databaseConnectionManager.getDatabaseFailureReason(databaseName);
                logger.warn("Skipping schema validation for unavailable database '{}': {}", databaseName, failureReason);
                result.addWarning("Database '" + databaseName + "' is unavailable - " + failureReason + " (endpoints using this database will return service unavailable errors)");
                continue;
            }

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

            // Get database config for schema context
            DatabaseConfig dbConfig = configurationManager.getAllDatabaseConfigurations().get(query.getDatabase());
            String schemaName = extractSchemaFromUrl(dbConfig != null ? dbConfig.getUrl() : null);
            String schemaContext = extractSchemaFromDatabase(dbConfig);

            // Extract table names from SQL
            Set<String> referencedTables = SqlAnalyzer.extractTableNamesFromSql(sql);

            // Validate each referenced table exists
            for (String tableName : referencedTables) {
                if (SqlAnalyzer.tableExists(metaData, tableName, schemaName)) {
                    String tableDisplayName = schemaName != null ? schemaName + "." + tableName : tableName;
                    result.addSuccess("Query '" + queryName + "' -> table '" + tableDisplayName + "' [EXISTS]");

                    // Extract and validate column names
                    Set<String> referencedColumns = SqlAnalyzer.extractColumnNamesFromSql(sql, tableName);
                    SqlAnalyzer.validateTableColumns(metaData, tableName, referencedColumns, queryName, result, schemaName);

                } else {
                    String tableDisplayName = schemaName != null ? schemaName + "." + tableName : tableName;
                    result.addError("Database '" + query.getDatabase() + "'" + schemaContext + " - Query '" + queryName + "' references non-existent table: " + tableDisplayName);
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
        logger.info("| Type: {} | Success: {} | Warnings: {} | Errors: {} |",
                String.format("%-15s", validationType),
                String.format("%6d", result.getSuccessCount()),
                String.format("%7d", result.getWarningCount()),
                String.format("%5d", result.getErrorCount()));
        logger.info("+------------------------------------------------------------------------------+");

        // Display errors first (show first 10 prominently)
        if (!result.getErrors().isEmpty()) {
            logger.info("| ERRORS:");
            List<String> errors = result.getErrors();
            int displayCount = Math.min(errors.size(), 10);

            for (int i = 0; i < displayCount; i++) {
                logger.info("| [ERROR] {}", errors.get(i));
            }

            if (errors.size() > 10) {
                logger.info("| ... and {} more errors (use management APIs for complete list)", errors.size() - 10);
            }
        }

        // Display warnings (show first 5)
        if (!result.getWarnings().isEmpty()) {
            logger.info("| WARNINGS:");
            List<String> warnings = result.getWarnings();
            int displayCount = Math.min(warnings.size(), 5);

            for (int i = 0; i < displayCount; i++) {
                logger.info("| [WARN] {}", warnings.get(i));
            }

            if (warnings.size() > 5) {
                logger.info("| ... and {} more warnings", warnings.size() - 5);
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
            if (result.getWarningCount() > 0) {
                logger.info("[SUCCESS] {} validation passed with {} warnings", validationType, result.getWarningCount());
            } else {
                logger.info("[SUCCESS] {} validation passed", validationType);
            }
        } else {
            logger.info("[FAILED] {} validation failed with {} errors", validationType, result.getErrorCount());
        }
    }

    /**
     * Validate all endpoints by making actual HTTP requests
     */
    public ValidationResult validateEndpointConnectivity(String baseUrl) {
        ValidationResult result = new ValidationResult();

        Map<String, ApiEndpointConfig> endpoints = configurationManager.getAllEndpointConfigurations();

        logger.info("[CHECK] Testing {} endpoints for connectivity", endpoints.size());

        for (Map.Entry<String, ApiEndpointConfig> entry : endpoints.entrySet()) {
            String endpointName = entry.getKey();
            ApiEndpointConfig endpoint = entry.getValue();

            validateSingleEndpoint(baseUrl, endpointName, endpoint, result);
        }

        return result;
    }

    /**
     * Validate a single endpoint by making an HTTP request
     */
    private void validateSingleEndpoint(String baseUrl, String endpointName, ApiEndpointConfig endpoint, ValidationResult result) {
        try {
            String fullUrl = baseUrl + endpoint.getPath();

            // For endpoints with path parameters, use sample values
            fullUrl = replaceSamplePathParameters(fullUrl);

            // Add sample query parameters for testing
            fullUrl = addSampleQueryParameters(fullUrl, endpoint);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .timeout(Duration.ofSeconds(10))
                    .method(endpoint.getMethod(), HttpRequest.BodyPublishers.noBody())
                    .build();

            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long responseTime = System.currentTimeMillis() - startTime;

            int statusCode = response.statusCode();

            if (statusCode >= 200 && statusCode < 300) {
                result.addSuccess("Endpoint '" + endpointName + "' -> " + endpoint.getMethod() + " " + endpoint.getPath() +
                                " [" + statusCode + "] (" + responseTime + "ms)");
            } else if (statusCode == 400) {
                // 400 might be expected for endpoints requiring specific parameters
                result.addSuccess("Endpoint '" + endpointName + "' -> " + endpoint.getMethod() + " " + endpoint.getPath() +
                                " [" + statusCode + " - Bad Request, likely needs specific parameters] (" + responseTime + "ms)");
            } else if (statusCode == 404) {
                result.addError("Endpoint '" + endpointName + "' -> " + endpoint.getMethod() + " " + endpoint.getPath() +
                              " [" + statusCode + " - Not Found] - Endpoint not registered");
            } else if (statusCode >= 500) {
                result.addError("Endpoint '" + endpointName + "' -> " + endpoint.getMethod() + " " + endpoint.getPath() +
                              " [" + statusCode + " - Server Error] (" + responseTime + "ms)");
            } else {
                result.addSuccess("Endpoint '" + endpointName + "' -> " + endpoint.getMethod() + " " + endpoint.getPath() +
                                " [" + statusCode + "] (" + responseTime + "ms)");
            }

        } catch (IOException | InterruptedException e) {
            result.addError("Endpoint '" + endpointName + "' -> " + endpoint.getMethod() + " " + endpoint.getPath() +
                          " [CONNECTION ERROR]: " + e.getMessage());
        } catch (Exception e) {
            result.addError("Endpoint '" + endpointName + "' -> " + endpoint.getMethod() + " " + endpoint.getPath() +
                          " [VALIDATION ERROR]: " + e.getMessage());
        }
    }

    /**
     * Replace path parameters with sample values for testing
     */
    private String replaceSamplePathParameters(String url) {
        // Replace common path parameters with sample values
        url = url.replaceAll("\\{id\\}", "1");
        url = url.replaceAll("\\{symbol\\}", "AAPL");
        url = url.replaceAll("\\{trader_id\\}", "TRADER_001");
        url = url.replaceAll("\\{exchange\\}", "NASDAQ");
        url = url.replaceAll("\\{trade_type\\}", "BUY");
        url = url.replaceAll("\\{databaseName\\}", "postgres-trades");
        url = url.replaceAll("\\{name\\}", "test");
        url = url.replaceAll("\\{queryName\\}", "test-query");

        return url;
    }

    /**
     * Add sample query parameters for testing endpoints
     */
    private String addSampleQueryParameters(String url, ApiEndpointConfig endpoint) {
        // For paginated endpoints, add basic pagination parameters
        if (endpoint.getPagination() != null && endpoint.getPagination().isEnabled()) {
            String separator = url.contains("?") ? "&" : "?";
            url += separator + "page=0&size=2";
        }

        // For date range endpoints, add sample date parameters
        if (url.contains("date-range")) {
            String separator = url.contains("?") ? "&" : "?";
            url += separator + "start_date=2024-01-01&end_date=2024-12-31";
        }

        // For analytics endpoints that might need date parameters
        if (url.contains("/analytics/daily-volume")) {
            String separator = url.contains("?") ? "&" : "?";
            url += separator + "start_date=2024-01-01&end_date=2024-12-31";
        }

        return url;
    }

    /**
     * Extract schema information from database configuration for better logging
     */
    private String extractSchemaFromDatabase(DatabaseConfig databaseConfig) {
        if (databaseConfig == null) {
            return "";
        }

        String url = databaseConfig.getUrl();
        if (url == null) {
            return "";
        }

        // Extract schema from JDBC URL parameters
        String schema = extractSchemaFromUrl(url);
        if (schema != null && !schema.isEmpty()) {
            return " (schema: " + schema + ")";
        }

        // Try to determine database type for default schema info
        if (url.contains("postgresql")) {
            return " (PostgreSQL)";
        } else if (url.contains("h2")) {
            return " (H2)";
        } else if (url.contains("mysql")) {
            return " (MySQL)";
        }

        return "";
    }

    /**
     * Extract schema name from JDBC URL
     */
    private String extractSchemaFromUrl(String url) {
        if (url == null) {
            return null;
        }

        // Look for currentSchema parameter in PostgreSQL URLs
        if (url.contains("currentSchema=")) {
            int start = url.indexOf("currentSchema=") + 14;
            int end = url.indexOf("&", start);
            if (end == -1) {
                end = url.length();
            }
            return url.substring(start, end);
        }

        // Look for schema parameter in other database URLs
        if (url.contains("schema=")) {
            int start = url.indexOf("schema=") + 7;
            int end = url.indexOf("&", start);
            if (end == -1) {
                end = url.length();
            }
            return url.substring(start, end);
        }

        return null;
    }
}

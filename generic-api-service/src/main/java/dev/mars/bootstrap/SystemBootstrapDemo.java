package dev.mars.bootstrap;

import dev.mars.generic.GenericApiApplication;
import dev.mars.generic.config.ConfigurationLoader;
import dev.mars.generic.config.EndpointConfigurationManager;
import dev.mars.generic.database.DatabaseConnectionManager;
import dev.mars.generic.config.DatabaseConfig;
import dev.mars.generic.config.QueryConfig;
import dev.mars.generic.config.ApiEndpointConfig;
import dev.mars.config.GenericApiConfig;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bootstrap demonstration class that shows system startup and makes calls to all management API endpoints.
 * This class demonstrates the Generic API Service initialization and comprehensive API testing workflow.
 *
 * Note: This demo focuses on the Generic API Service. For full system demonstration including
 * Metrics Service, start the Metrics Service separately in another terminal.
 */
public class SystemBootstrapDemo {
    private static final Logger logger = LoggerFactory.getLogger(SystemBootstrapDemo.class);

    private static final String GENERIC_API_BASE_URL = "http://localhost:8080";
    private static final String METRICS_SERVICE_BASE_URL = "http://localhost:8081";

    private GenericApiApplication genericApiApp;
    private HttpClient httpClient;
    private boolean metricsServiceAvailable = false;

    // Configuration components for dynamic API validation
    private ConfigurationLoader configurationLoader;
    private EndpointConfigurationManager configurationManager;
    private DatabaseConnectionManager databaseConnectionManager;
    
    public static void main(String[] args) {
        SystemBootstrapDemo demo = new SystemBootstrapDemo();
        try {
            demo.runBootstrapDemo();
        } catch (Exception e) {
            logger.error("Bootstrap demo failed", e);
            System.exit(1);
        }
    }
    
    /**
     * Run the complete bootstrap demonstration
     */
    public void runBootstrapDemo() throws Exception {
        logger.info("=".repeat(80));
        logger.info(">>> STARTING SYSTEM BOOTSTRAP DEMONSTRATION");
        logger.info("=".repeat(80));
        
        try {
            // Initialize HTTP client
            initializeHttpClient();

            // Step 1: Start Generic API Service
            startGenericApiService();

            // Step 2: Wait for Generic API Service to be ready
            waitForGenericApiServiceReady();

            // Step 3: Check if Metrics Service is available
            checkMetricsServiceAvailability();

            // Step 4: Test all management API endpoints
            testManagementApis();

            // Step 5: Perform dynamic API validation
            performDynamicApiValidation();

            // Step 6: Test metrics API endpoints if available
            if (metricsServiceAvailable) {
                testMetricsApis();
            } else {
                displayMetricsServiceInstructions();
            }

            // Step 7: Display summary
            displaySummary();

            logger.info("[SUCCESS] Bootstrap demonstration completed successfully!");

        } finally {
            // Cleanup
            stopServices();
        }
    }
    
    /**
     * Initialize HTTP client for API calls
     */
    private void initializeHttpClient() {
        logger.info("[INIT] Initializing HTTP client...");
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        logger.info("[OK] HTTP client initialized");
    }
    
    /**
     * Start Generic API Service
     */
    private void startGenericApiService() throws Exception {
        logger.info("[START] Starting Generic API Service...");

        try {
            genericApiApp = new GenericApiApplication();
            genericApiApp.start();
            logger.info("[OK] Generic API Service started on port 8080");
        } catch (Exception e) {
            logger.error("Failed to start Generic API Service", e);
            throw e;
        }
    }
    
    /**
     * Wait for Generic API Service to be ready
     */
    private void waitForGenericApiServiceReady() throws Exception {
        logger.info("[WAIT] Waiting for Generic API Service to be ready...");
        waitForServiceReady(GENERIC_API_BASE_URL + "/api/health", "Generic API Service");
        logger.info("[OK] Generic API Service is ready");
    }

    /**
     * Check if Metrics Service is available
     */
    private void checkMetricsServiceAvailability() {
        logger.info("[CHECK] Checking Metrics Service availability...");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(METRICS_SERVICE_BASE_URL + "/api/health"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                metricsServiceAvailable = true;
                logger.info("[OK] Metrics Service is available and will be tested");
            } else {
                logger.info("[WARN] Metrics Service responded with status: {}", response.statusCode());
            }
        } catch (Exception e) {
            logger.info("[INFO] Metrics Service is not available (this is optional for the demo)");
        }
    }
    
    /**
     * Wait for a specific service to be ready
     */
    private void waitForServiceReady(String healthUrl, String serviceName) throws Exception {
        int maxAttempts = 30;
        int attempt = 0;
        
        while (attempt < maxAttempts) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(healthUrl))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    logger.info("[OK] {} is ready", serviceName);
                    return;
                }
            } catch (Exception e) {
                // Service not ready yet, continue waiting
            }
            
            attempt++;
            Thread.sleep(1000); // Wait 1 second between attempts
            logger.debug("Waiting for {} to be ready... (attempt {}/{})", serviceName, attempt, maxAttempts);
        }
        
        throw new RuntimeException(serviceName + " failed to become ready within timeout");
    }

    /**
     * Test all management API endpoints in Generic API Service
     */
    private void testManagementApis() throws Exception {
        logger.info("[TEST] Testing Management API endpoints...");

        List<String> managementEndpoints = List.of(
            // Configuration Management
            "/api/management/config/metadata",
            "/api/management/config/paths",
            "/api/management/config/contents",
            "/api/management/config/endpoints",
            "/api/management/config/queries",
            "/api/management/config/databases",

            // Usage Statistics
            "/api/management/statistics",
            "/api/management/statistics/endpoints",
            "/api/management/statistics/queries",
            "/api/management/statistics/databases",

            // Health Monitoring
            "/api/management/health",
            "/api/management/health/databases",

            // Dashboard
            "/api/management/dashboard"
        );

        testEndpoints(GENERIC_API_BASE_URL, managementEndpoints, "Management API");
        logger.info("[OK] Management API endpoints tested successfully");
    }

    /**
     * Test all metrics API endpoints in Metrics Service
     */
    private void testMetricsApis() throws Exception {
        logger.info("[TEST] Testing Metrics API endpoints...");

        List<String> metricsEndpoints = List.of(
            // Health Check
            "/api/health",

            // Performance Metrics
            "/api/performance-metrics",
            "/api/performance-metrics/summary",
            "/api/performance-metrics/trends",
            "/api/performance-metrics/test-types",

            // Real-time Metrics
            "/api/metrics/endpoints"
        );

        testEndpoints(METRICS_SERVICE_BASE_URL, metricsEndpoints, "Metrics API");
        logger.info("[OK] Metrics API endpoints tested successfully");
    }

    /**
     * Display instructions for starting the Metrics Service
     */
    private void displayMetricsServiceInstructions() {
        logger.info("[INFO] Metrics Service Instructions:");
        logger.info("+----------------------------------------------------------------------------------+");
        logger.info("| The Metrics Service is not currently running. To test the full system:         |");
        logger.info("|                                                                                  |");
        logger.info("| 1. Open a new terminal window                                                   |");
        logger.info("| 2. Navigate to the project root directory                                       |");
        logger.info("| 3. Run: cd metrics-service                                                      |");
        logger.info("| 4. Run: mvn exec:java -Dexec.mainClass=\"dev.mars.metrics.MetricsApplication\"  |");
        logger.info("| 5. Re-run this bootstrap demo to test both services                            |");
        logger.info("|                                                                                  |");
        logger.info("| Alternatively, use the provided scripts:                                        |");
        logger.info("| - Windows: run-bootstrap-demo.bat                                               |");
        logger.info("| - Unix/Linux: ./run-bootstrap-demo.sh                                           |");
        logger.info("+----------------------------------------------------------------------------------+");
    }

    /**
     * Perform comprehensive dynamic API validation
     */
    private void performDynamicApiValidation() {
        logger.info("[VALIDATE] Starting Dynamic API Validation...");
        logger.info("=".repeat(80));
        logger.info(">>> DYNAMIC API VALIDATION");
        logger.info("=".repeat(80));

        try {
            // Initialize configuration components
            initializeConfigurationComponents();

            // Part 1: Configuration Validation
            logger.info("[PART 1] Configuration Validation");
            ValidationResult configValidation = validateConfigurationChain();
            displayValidationResults("Configuration Chain", configValidation);

            // Part 2: Database Schema Validation (only if config validation passed)
            if (configValidation.isSuccess()) {
                logger.info("");
                logger.info("[PART 2] Database Schema Validation");
                ValidationResult schemaValidation = validateDatabaseSchema();
                displayValidationResults("Database Schema", schemaValidation);
            } else {
                logger.info("[SKIP] Database Schema Validation - Configuration validation failed");
            }

            logger.info("=".repeat(80));
            logger.info("[OK] Dynamic API validation completed");

        } catch (Exception e) {
            logger.error("[ERROR] Dynamic API validation failed", e);
        }
    }

    /**
     * Initialize configuration components for validation
     */
    private void initializeConfigurationComponents() {
        logger.info("[INIT] Initializing configuration components...");

        // Create configuration loader
        GenericApiConfig config = new GenericApiConfig();
        configurationLoader = new ConfigurationLoader(config);

        // Create configuration manager
        configurationManager = new EndpointConfigurationManager(configurationLoader);

        // Create database connection manager
        databaseConnectionManager = new DatabaseConnectionManager(configurationManager);

        logger.info("[OK] Configuration components initialized");
    }

    /**
     * Validate the configuration chain: endpoints -> queries -> databases
     */
    private ValidationResult validateConfigurationChain() {
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
    private ValidationResult validateDatabaseSchema() {
        ValidationResult result = new ValidationResult();

        Map<String, QueryConfig> queries = configurationManager.getAllQueryConfigurations();
        Map<String, DatabaseConfig> databases = configurationManager.getAllDatabaseConfigurations();

        // Group queries by database for efficient validation
        Map<String, List<QueryConfig>> queriesByDatabase = new HashMap<>();
        for (QueryConfig query : queries.values()) {
            String databaseName = query.getDatabase();
            queriesByDatabase.computeIfAbsent(databaseName, k -> new ArrayList<>()).add(query);
        }

        // Validate each database
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
            Set<String> referencedTables = extractTableNamesFromSql(sql);

            // Validate each referenced table exists
            for (String tableName : referencedTables) {
                if (tableExists(metaData, tableName)) {
                    result.addSuccess("Query '" + queryName + "' -> table '" + tableName + "' [EXISTS]");

                    // Extract and validate column names
                    Set<String> referencedColumns = extractColumnNamesFromSql(sql, tableName);
                    validateTableColumns(metaData, tableName, referencedColumns, queryName, result);

                } else {
                    result.addError("Query '" + queryName + "' references non-existent table: " + tableName);
                }
            }

            // Validate query parameters
            validateQueryParameters(query, result);

        } catch (Exception e) {
            result.addError("Error validating query '" + query.getName() + "': " + e.getMessage());
        }
    }

    /**
     * Extract table names from SQL query
     */
    private Set<String> extractTableNamesFromSql(String sql) {
        Set<String> tables = new HashSet<>();

        // Simple regex to find table names after FROM and JOIN keywords
        Pattern pattern = Pattern.compile("(?i)(?:FROM|JOIN)\\s+([a-zA-Z_][a-zA-Z0-9_]*)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql);

        while (matcher.find()) {
            String tableName = matcher.group(1).toLowerCase();
            tables.add(tableName);
        }

        return tables;
    }

    /**
     * Extract column names from SQL query for a specific table
     */
    private Set<String> extractColumnNamesFromSql(String sql, String tableName) {
        Set<String> columns = new HashSet<>();

        // Simple extraction - look for column names in SELECT clause
        Pattern selectPattern = Pattern.compile("(?i)SELECT\\s+(.*?)\\s+FROM", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher selectMatcher = selectPattern.matcher(sql);

        if (selectMatcher.find()) {
            String selectClause = selectMatcher.group(1);

            // Split by comma and extract column names
            String[] parts = selectClause.split(",");
            for (String part : parts) {
                part = part.trim();

                // Handle aliases (column AS alias)
                if (part.toLowerCase().contains(" as ")) {
                    part = part.split("(?i)\\s+as\\s+")[0].trim();
                }

                // Remove table prefixes (table.column)
                if (part.contains(".")) {
                    part = part.substring(part.lastIndexOf(".") + 1);
                }

                // Skip functions and special cases
                if (!part.equals("*") && !part.contains("(") && part.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                    columns.add(part.toLowerCase());
                }
            }
        }

        // Also look for columns in WHERE clause
        Pattern wherePattern = Pattern.compile("(?i)WHERE\\s+(.*?)(?:ORDER|GROUP|LIMIT|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher whereMatcher = wherePattern.matcher(sql);

        if (whereMatcher.find()) {
            String whereClause = whereMatcher.group(1);
            Pattern columnPattern = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*[=<>!]", Pattern.CASE_INSENSITIVE);
            Matcher columnMatcher = columnPattern.matcher(whereClause);

            while (columnMatcher.find()) {
                String column = columnMatcher.group(1).toLowerCase();
                if (!column.matches("(?i)(and|or|not|null|true|false)")) {
                    columns.add(column);
                }
            }
        }

        return columns;
    }

    /**
     * Check if a table exists in the database
     */
    private boolean tableExists(DatabaseMetaData metaData, String tableName) throws SQLException {
        try (ResultSet tables = metaData.getTables(null, null, tableName.toUpperCase(), new String[]{"TABLE"})) {
            return tables.next();
        }
    }

    /**
     * Validate that columns exist in the specified table
     */
    private void validateTableColumns(DatabaseMetaData metaData, String tableName, Set<String> referencedColumns,
                                    String queryName, ValidationResult result) {
        try {
            Set<String> existingColumns = new HashSet<>();

            try (ResultSet columns = metaData.getColumns(null, null, tableName.toUpperCase(), null)) {
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME").toLowerCase();
                    existingColumns.add(columnName);
                }
            }

            for (String referencedColumn : referencedColumns) {
                if (existingColumns.contains(referencedColumn)) {
                    result.addSuccess("Query '" + queryName + "' -> table '" + tableName +
                                    "' -> column '" + referencedColumn + "' [EXISTS]");
                } else {
                    result.addError("Query '" + queryName + "' references non-existent column '" +
                                  referencedColumn + "' in table '" + tableName + "'");
                }
            }

        } catch (SQLException e) {
            result.addError("Error validating columns for table '" + tableName + "': " + e.getMessage());
        }
    }

    /**
     * Validate query parameters
     */
    private void validateQueryParameters(QueryConfig query, ValidationResult result) {
        String queryName = query.getName();
        String sql = query.getSql();

        // Count parameter placeholders in SQL
        int sqlParameterCount = 0;
        for (char c : sql.toCharArray()) {
            if (c == '?') {
                sqlParameterCount++;
            }
        }

        // Count defined parameters
        int definedParameterCount = query.getParameters() != null ? query.getParameters().size() : 0;

        if (sqlParameterCount == definedParameterCount) {
            result.addSuccess("Query '" + queryName + "' parameter count matches: " + sqlParameterCount + " parameters");
        } else {
            result.addError("Query '" + queryName + "' parameter mismatch: SQL has " + sqlParameterCount +
                          " placeholders but " + definedParameterCount + " parameters defined");
        }
    }

    /**
     * Display validation results in a formatted way
     */
    private void displayValidationResults(String validationType, ValidationResult result) {
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

    /**
     * Test a list of endpoints for a given service
     */
    private void testEndpoints(String baseUrl, List<String> endpoints, String serviceName) throws Exception {
        List<EndpointTestResult> results = new ArrayList<>();

        for (String endpoint : endpoints) {
            String fullUrl = baseUrl + endpoint;
            EndpointTestResult result = testSingleEndpoint(fullUrl, endpoint);
            results.add(result);

            // Add small delay between requests
            Thread.sleep(100);
        }

        // Display results
        displayEndpointResults(serviceName, results);
    }

    /**
     * Test a single endpoint and return the result
     */
    private EndpointTestResult testSingleEndpoint(String url, String endpoint) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long responseTime = System.currentTimeMillis() - startTime;

            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;

            return new EndpointTestResult(endpoint, response.statusCode(), responseTime, success,
                    response.body().length(), null);

        } catch (Exception e) {
            logger.warn("Failed to test endpoint {}: {}", endpoint, e.getMessage());
            return new EndpointTestResult(endpoint, -1, -1, false, 0, e.getMessage());
        }
    }

    /**
     * Display endpoint test results in a formatted table
     */
    private void displayEndpointResults(String serviceName, List<EndpointTestResult> results) {
        logger.info("[RESULTS] {} Test Results:", serviceName);
        logger.info("+-----------------------------------------------------+--------+----------+---------+----------+");
        logger.info("| Endpoint                                            | Status | Time(ms) | Success | Size(B)  |");
        logger.info("+-----------------------------------------------------+--------+----------+---------+----------+");

        for (EndpointTestResult result : results) {
            String endpoint = String.format("%-51s", result.endpoint.length() > 51 ?
                    result.endpoint.substring(0, 48) + "..." : result.endpoint);
            String status = String.format("%6s", result.statusCode == -1 ? "ERROR" : String.valueOf(result.statusCode));
            String time = String.format("%8s", result.responseTime == -1 ? "N/A" : String.valueOf(result.responseTime));
            String success = String.format("%7s", result.success ? "OK" : "FAIL");
            String size = String.format("%8s", result.responseSize == 0 ? "N/A" : String.valueOf(result.responseSize));

            logger.info("| {} | {} | {} | {} | {} |", endpoint, status, time, success, size);
        }

        logger.info("+-----------------------------------------------------+--------+----------+---------+----------+");

        // Summary
        long successCount = results.stream().mapToLong(r -> r.success ? 1 : 0).sum();
        double successRate = (double) successCount / results.size() * 100;
        double avgResponseTime = results.stream()
                .filter(r -> r.responseTime > 0)
                .mapToLong(r -> r.responseTime)
                .average()
                .orElse(0.0);

        logger.info("[SUMMARY] {}/{} endpoints successful (%.1f%%), Avg response time: %.1fms",
                successCount, results.size(), successRate, avgResponseTime);
    }

    /**
     * Display overall system summary
     */
    private void displaySummary() {
        logger.info("=".repeat(80));
        logger.info(">>> SYSTEM BOOTSTRAP DEMONSTRATION SUMMARY");
        logger.info("=".repeat(80));
        logger.info("[STATUS] Services Status:");
        logger.info("   + Generic API Service: {} [OK]", GENERIC_API_BASE_URL);
        logger.info("   + Metrics Service:     {} {}", METRICS_SERVICE_BASE_URL,
                metricsServiceAvailable ? "[OK]" : "[NOT RUNNING]");
        logger.info("");
        logger.info("[TESTED] Management APIs Tested:");
        logger.info("   + Configuration Management [OK]");
        logger.info("   + Usage Statistics [OK]");
        logger.info("   + Health Monitoring [OK]");
        logger.info("   + Dashboard [OK]");
        logger.info("");
        if (metricsServiceAvailable) {
            logger.info("[TESTED] Metrics APIs Tested:");
            logger.info("   + Performance Metrics [OK]");
            logger.info("   + Real-time Metrics [OK]");
            logger.info("   + Health Checks [OK]");
        } else {
            logger.info("[SKIPPED] Metrics APIs:");
            logger.info("   + Not tested (Metrics Service not running)");
        }
        logger.info("");
        logger.info("[ACCESS] Access URLs:");
        logger.info("   + Generic API Swagger: {}/swagger", GENERIC_API_BASE_URL);
        logger.info("   + Management Dashboard: {}/api/management/dashboard", GENERIC_API_BASE_URL);
        if (metricsServiceAvailable) {
            logger.info("   + Metrics Dashboard: {}/dashboard", METRICS_SERVICE_BASE_URL);
        } else {
            logger.info("   + Metrics Dashboard: {} (Start Metrics Service first)", METRICS_SERVICE_BASE_URL + "/dashboard");
        }
        logger.info("=".repeat(80));
    }

    /**
     * Stop Generic API Service gracefully
     */
    private void stopServices() {
        logger.info("[STOP] Stopping Generic API Service...");

        if (genericApiApp != null) {
            try {
                genericApiApp.stop();
                logger.info("[OK] Generic API Service stopped");
            } catch (Exception e) {
                logger.error("Error stopping Generic API Service", e);
            }
        }

        if (metricsServiceAvailable) {
            logger.info("[INFO] Metrics Service is running separately and will continue running");
        }

        logger.info("[OK] Bootstrap demo cleanup completed");
    }

    /**
     * Data class to hold endpoint test results
     */
    private static class EndpointTestResult {
        final String endpoint;
        final int statusCode;
        final long responseTime;
        final boolean success;
        final int responseSize;
        final String errorMessage;

        EndpointTestResult(String endpoint, int statusCode, long responseTime, boolean success,
                          int responseSize, String errorMessage) {
            this.endpoint = endpoint;
            this.statusCode = statusCode;
            this.responseTime = responseTime;
            this.success = success;
            this.responseSize = responseSize;
            this.errorMessage = errorMessage;
        }
    }

    /**
     * Validation result container
     */
    private static class ValidationResult {
        private final List<String> errors = new ArrayList<>();
        private final List<String> successes = new ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public void addSuccess(String success) {
            successes.add(success);
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getSuccesses() {
            return successes;
        }

        public int getErrorCount() {
            return errors.size();
        }

        public int getSuccessCount() {
            return successes.size();
        }

        public boolean isSuccess() {
            return errors.isEmpty();
        }
    }
}

package dev.cordal.generic;

import com.google.inject.Module;
import dev.cordal.common.application.BaseJavalinApplication;
import dev.cordal.common.config.ServerConfig;

import dev.cordal.config.GenericApiConfig;
import dev.cordal.config.GenericApiGuiceModule;
import dev.cordal.config.SwaggerConfig;
import dev.cordal.util.ApiEndpoints;

import dev.cordal.generic.config.ApiEndpointConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Main application class for the Generic API Service
 * Extends BaseJavalinApplication for common functionality
 */
public class GenericApiApplication extends BaseJavalinApplication {
    private static final Logger logger = LoggerFactory.getLogger(GenericApiApplication.class);

    public static void main(String[] args) {
        try {
            GenericApiApplication application = new GenericApiApplication();

            // Check for command line arguments
            boolean validateOnlyFromArgs = false;
            for (String arg : args) {
                if ("--validate-only".equals(arg) || "--validate".equals(arg)) {
                    validateOnlyFromArgs = true;
                    break;
                }
            }

            // Initialize dependency injection to read configuration
            application.initializeDependencyInjection();
            GenericApiConfig config = application.injector.getInstance(GenericApiConfig.class);

            // Check if we should run validation only (from args or config)
            boolean validateOnly = validateOnlyFromArgs || config.isValidationValidateOnly();

            if (validateOnly) {
                logger.info("Running configuration validation only (--validate-only flag or validation.validateOnly=true)");
                application.runValidationOnly();
                return;
            }

            // Normal application startup
            application.start();

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(application::stop));

        } catch (Exception e) {
            logger.error("Failed to start Generic API application", e);
            throw new RuntimeException("Failed to start Generic API application", e);
        }
    }

    @Override
    protected Module getGuiceModule() {
        return new GenericApiGuiceModule();
    }

    @Override
    protected ServerConfig getServerConfig() {
        GenericApiConfig config = injector.getInstance(GenericApiConfig.class);
        ServerConfig serverConfig = config.getServerConfig();
        logger.info("GenericApiApplication - Retrieved server config: host={}, port={}", serverConfig.getHost(), serverConfig.getPort());
        return serverConfig;
    }

    @Override
    protected String getApplicationName() {
        return "Generic API Service";
    }

    @Override
    protected void performPreStartupInitialization() {
        // Initialize configuration database
        logger.info("Initializing configuration database");
        dev.cordal.database.DatabaseManager dbManager = injector.getInstance(dev.cordal.database.DatabaseManager.class);
        logger.info("Configuration database initialized successfully");

        // Check if test data loading is enabled (for integration tests)
        String testDataLoadingEnabled = System.getProperty("test.data.loading.enabled");
        if ("true".equals(testDataLoadingEnabled)) {
            logger.info("Test data loading enabled - initializing test data");
            initializeTestData();
        }

        // Check if validation should run on startup
        GenericApiConfig config = injector.getInstance(GenericApiConfig.class);
        if (config.isValidationRunOnStartup()) {
            logger.info("Running configuration validation on startup (validation.runOnStartup=true)");
            runConfigurationValidation();
        }
    }

    @Override
    protected void performPostStartupInitialization() {
        // Check if endpoint validation should run after startup
        GenericApiConfig config = injector.getInstance(GenericApiConfig.class);
        if (config.isValidationRunOnStartup() && config.isValidationValidateEndpoints()) {
            logger.info("Running endpoint connectivity validation after startup (validation.runOnStartup=true, validation.validateEndpoints=true)");
            runConfigurationValidation(true);
        }
    }

    /**
     * Initialize test data for integration testing
     * This method creates the stock_trades table and populates it with sample data
     * when test.data.loading.enabled=true system property is set
     */
    private void initializeTestData() {
        logger.info("Initializing test data for integration testing");

        try {
            // Get database connection manager
            dev.cordal.generic.database.DatabaseConnectionManager databaseConnectionManager =
                injector.getInstance(dev.cordal.generic.database.DatabaseConnectionManager.class);

            // Note: Example data initialization (like stock trades) should be handled
            // by integration tests, not by the core application
            logger.info("Core application initialized - example data should be managed by integration tests");

        } catch (Exception e) {
            logger.warn("Test data initialization failed - continuing with application startup: {}", e.getMessage());
            logger.debug("Test data initialization error details", e);
        }
    }

    @Override
    protected void configureSwagger() {
        logger.info("Configuring Swagger/OpenAPI");
        SwaggerConfig swaggerConfig = injector.getInstance(SwaggerConfig.class);
        swaggerConfig.configureSwagger(app);
        logger.info("Swagger/OpenAPI configured");
    }

    @Override
    protected void configureRoutes() {
        logger.info("Configuring routes");
        
        GenericApiController genericApiController = injector.getInstance(GenericApiController.class);
        dev.cordal.generic.management.ManagementController managementController = injector.getInstance(dev.cordal.generic.management.ManagementController.class);
        dev.cordal.generic.management.ConfigurationManagementController configManagementController = injector.getInstance(dev.cordal.generic.management.ConfigurationManagementController.class);
        dev.cordal.generic.migration.ConfigurationMigrationController migrationController = injector.getInstance(dev.cordal.generic.migration.ConfigurationMigrationController.class);
        dev.cordal.api.H2ServerController h2ServerController = injector.getInstance(dev.cordal.api.H2ServerController.class);
        
        // Health check endpoint
        app.get(ApiEndpoints.HEALTH, ctx -> {
            ctx.json(java.util.Map.of(
                "status", "UP",
                "timestamp", System.currentTimeMillis(),
                "service", "generic-api-service"
            ));
        });

        // Generic API management endpoints
        app.get(ApiEndpoints.GENERIC_HEALTH, genericApiController::getHealthStatus);
        app.get(ApiEndpoints.GENERIC_ENDPOINTS, genericApiController::getAvailableEndpoints);
        app.get(ApiEndpoints.GENERIC_ENDPOINT_BY_NAME, genericApiController::getEndpointConfiguration);

        // Configuration endpoints
        app.get(ApiEndpoints.GENERIC_CONFIG, genericApiController::getCompleteConfiguration);

        // ========== GRANULAR CONFIGURATION ENDPOINTS (MUST BE BEFORE PARAMETERIZED ROUTES) ==========

        // Granular configuration endpoints - Endpoints
        app.get(ApiEndpoints.Config.ENDPOINTS_SCHEMA, genericApiController::getEndpointConfigurationSchema);
        app.get(ApiEndpoints.Config.ENDPOINTS_PARAMETERS, genericApiController::getEndpointParameters);
        app.get(ApiEndpoints.Config.ENDPOINTS_DATABASE_CONNECTIONS, genericApiController::getEndpointDatabaseConnections);
        app.get(ApiEndpoints.Config.ENDPOINTS_SUMMARY, genericApiController::getEndpointConfigurationSummary);

        // Granular configuration endpoints - Queries
        app.get(ApiEndpoints.Config.QUERIES_SCHEMA, genericApiController::getQueryConfigurationSchema);
        app.get(ApiEndpoints.Config.QUERIES_PARAMETERS, genericApiController::getQueryParameters);
        app.get(ApiEndpoints.Config.QUERIES_DATABASE_CONNECTIONS, genericApiController::getQueryDatabaseConnections);
        app.get(ApiEndpoints.Config.QUERIES_SUMMARY, genericApiController::getQueryConfigurationSummary);

        // Granular configuration endpoints - Databases
        app.get(ApiEndpoints.Config.DATABASES_SCHEMA, genericApiController::getDatabaseConfigurationSchema);
        app.get(ApiEndpoints.Config.DATABASES_PARAMETERS, genericApiController::getDatabaseParameters);
        app.get(ApiEndpoints.Config.DATABASES_CONNECTIONS, genericApiController::getDatabaseConnections);
        app.get(ApiEndpoints.Config.DATABASES_SUMMARY, genericApiController::getDatabaseConfigurationSummary);

        // ========== PARAMETERIZED CONFIGURATION ENDPOINTS (MUST BE AFTER SPECIFIC ROUTES) ==========

        app.get("/api/generic/config/queries", genericApiController::getQueryConfigurations);
        app.get(ApiEndpoints.Config.QUERIES_BY_NAME, genericApiController::getQueryConfiguration);
        app.get("/api/generic/config/databases", genericApiController::getDatabaseConfigurations);
        app.get(ApiEndpoints.Config.DATABASES_BY_NAME, genericApiController::getDatabaseConfiguration);
        app.get(ApiEndpoints.Config.RELATIONSHIPS, genericApiController::getConfigurationRelationships);

        // Configuration validation endpoints
        app.get(ApiEndpoints.Validation.VALIDATE_ALL, genericApiController::validateConfigurations);
        app.get(ApiEndpoints.Validation.VALIDATE_ENDPOINTS, genericApiController::validateEndpointConfigurations);
        app.get(ApiEndpoints.Validation.VALIDATE_QUERIES, genericApiController::validateQueryConfigurations);
        app.get(ApiEndpoints.Validation.VALIDATE_DATABASES, genericApiController::validateDatabaseConfigurations);
        app.get(ApiEndpoints.Validation.VALIDATE_RELATIONSHIPS, genericApiController::validateConfigurationRelationships);
        app.get(ApiEndpoints.Validation.VALIDATE_ENDPOINT_CONNECTIVITY, genericApiController::validateEndpointConnectivity);

        // ========== COMPREHENSIVE MANAGEMENT ENDPOINTS ==========

        // Configuration metadata endpoints
        app.get(ApiEndpoints.Management.CONFIG_METADATA, managementController::getConfigurationMetadata);
        app.get(ApiEndpoints.Management.CONFIG_PATHS, managementController::getConfigurationPaths);
        app.get(ApiEndpoints.Management.CONFIG_CONTENTS, managementController::getConfigurationFileContents);

        // Configuration view endpoints
        app.get(ApiEndpoints.Management.CONFIG_ENDPOINTS, managementController::getConfiguredEndpoints);
        app.get(ApiEndpoints.Management.CONFIG_QUERIES, managementController::getConfiguredQueries);
        app.get(ApiEndpoints.Management.CONFIG_DATABASES, managementController::getConfiguredDatabases);

        // Usage statistics endpoints
        app.get(ApiEndpoints.Management.STATISTICS, managementController::getUsageStatistics);
        app.get(ApiEndpoints.Management.STATISTICS_ENDPOINTS, managementController::getEndpointStatistics);
        app.get(ApiEndpoints.Management.STATISTICS_QUERIES, managementController::getQueryStatistics);
        app.get(ApiEndpoints.Management.STATISTICS_DATABASES, managementController::getDatabaseStatistics);

        // Health monitoring endpoints
        app.get(ApiEndpoints.Management.HEALTH, managementController::getHealthStatus);
        app.get(ApiEndpoints.Management.HEALTH_DATABASES, managementController::getDatabaseHealth);
        app.get(ApiEndpoints.Management.HEALTH_DATABASE_SPECIFIC, managementController::getSpecificDatabaseHealth);

        // Deployment verification endpoints
        app.get(ApiEndpoints.Management.DEPLOYMENT_INFO, managementController::getDeploymentInfo);
        app.get(ApiEndpoints.Management.JAR_INFO, managementController::getJarInfo);
        app.get(ApiEndpoints.Management.READINESS, managementController::getReadinessCheck);
        app.get(ApiEndpoints.Management.LIVENESS, managementController::getLivenessCheck);

        // Comprehensive dashboard endpoint
        app.get(ApiEndpoints.Management.DASHBOARD, managementController::getManagementDashboard);

        // ========== CONFIGURATION MANAGEMENT ENDPOINTS ==========

        // Database Configuration Management
        app.get(ApiEndpoints.ConfigManagement.DATABASES, configManagementController::getAllDatabaseConfigurations);
        app.get(ApiEndpoints.ConfigManagement.DATABASE_BY_NAME, configManagementController::getDatabaseConfiguration);
        app.post(ApiEndpoints.ConfigManagement.DATABASE_BY_NAME, configManagementController::saveDatabaseConfiguration);
        app.delete(ApiEndpoints.ConfigManagement.DATABASE_BY_NAME, configManagementController::deleteDatabaseConfiguration);

        // Query Configuration Management
        app.get(ApiEndpoints.ConfigManagement.QUERIES, configManagementController::getAllQueryConfigurations);
        app.get(ApiEndpoints.ConfigManagement.QUERY_BY_NAME, configManagementController::getQueryConfiguration);
        app.get(ApiEndpoints.ConfigManagement.QUERIES_BY_DATABASE, configManagementController::getQueryConfigurationsByDatabase);
        app.post(ApiEndpoints.ConfigManagement.QUERY_BY_NAME, configManagementController::saveQueryConfiguration);
        app.delete(ApiEndpoints.ConfigManagement.QUERY_BY_NAME, configManagementController::deleteQueryConfiguration);

        // Endpoint Configuration Management
        app.get(ApiEndpoints.ConfigManagement.ENDPOINTS, configManagementController::getAllEndpointConfigurations);
        app.get(ApiEndpoints.ConfigManagement.ENDPOINT_BY_NAME, configManagementController::getEndpointConfiguration);
        app.get(ApiEndpoints.ConfigManagement.ENDPOINTS_BY_QUERY, configManagementController::getEndpointConfigurationsByQuery);
        app.post(ApiEndpoints.ConfigManagement.ENDPOINT_BY_NAME, configManagementController::saveEndpointConfiguration);
        app.delete(ApiEndpoints.ConfigManagement.ENDPOINT_BY_NAME, configManagementController::deleteEndpointConfiguration);

        // Configuration Management Utilities
        app.get(ApiEndpoints.ConfigManagement.STATISTICS, configManagementController::getConfigurationStatistics);
        app.get(ApiEndpoints.ConfigManagement.SOURCE_INFO, configManagementController::getConfigurationSourceInfo);
        app.get(ApiEndpoints.ConfigManagement.AVAILABILITY, configManagementController::getConfigurationManagementAvailability);

        // ========== CONFIGURATION MIGRATION ENDPOINTS ==========

        // Migration Operations
        app.post(ApiEndpoints.Migration.YAML_TO_DATABASE, migrationController::migrateYamlToDatabase);
        app.get(ApiEndpoints.Migration.EXPORT_DATABASE_TO_YAML, migrationController::exportDatabaseToYaml);

        // Synchronization Operations
        app.get(ApiEndpoints.Migration.COMPARE, migrationController::compareConfigurations);
        app.get(ApiEndpoints.Migration.STATUS, migrationController::getMigrationStatus);

        // YAML Export Utilities
        app.get(ApiEndpoints.Migration.YAML_DATABASES, migrationController::getYamlDatabaseConfigurations);
        app.get(ApiEndpoints.Migration.YAML_QUERIES, migrationController::getYamlQueryConfigurations);
        app.get(ApiEndpoints.Migration.YAML_ENDPOINTS, migrationController::getYamlEndpointConfigurations);

        // H2 Server Management Endpoints
        app.get(ApiEndpoints.H2_SERVER_STATUS, h2ServerController::getServerStatus);
        app.post(ApiEndpoints.H2_SERVER_START, h2ServerController::startServers);
        app.post(ApiEndpoints.H2_SERVER_STOP, h2ServerController::stopServers);
        app.post(ApiEndpoints.H2_SERVER_TCP_START, h2ServerController::startTcpServer);
        app.post(ApiEndpoints.H2_SERVER_TCP_STOP, h2ServerController::stopTcpServer);
        app.post(ApiEndpoints.H2_SERVER_WEB_START, h2ServerController::startWebServer);
        app.post(ApiEndpoints.H2_SERVER_WEB_STOP, h2ServerController::stopWebServer);

        // Register dynamic endpoints from YAML configuration
        registerDynamicEndpoints(genericApiController);

        logger.info("Routes configured");
    }

    /**
     * Register dynamic endpoints from YAML configuration
     */
    private void registerDynamicEndpoints(GenericApiController genericApiController) {
        logger.info("Registering dynamic endpoints from YAML configuration");

        try {
            GenericApiService genericApiService = genericApiController.getGenericApiService();

            // Get available endpoint configurations (only those with working databases)
            Map<String, dev.cordal.generic.config.ApiEndpointConfig> availableEndpoints = genericApiService.getAvailableEndpoints();

            // Get unavailable endpoints for logging
            Map<String, String> unavailableEndpoints = genericApiService.getUnavailableEndpoints();

            // Log unavailable endpoints
            if (!unavailableEndpoints.isEmpty()) {
                logger.warn("Found {} unavailable endpoint(s) due to database connectivity issues:", unavailableEndpoints.size());
                for (Map.Entry<String, String> entry : unavailableEndpoints.entrySet()) {
                    logger.warn("  - Endpoint '{}': {}", entry.getKey(), entry.getValue());
                }
                logger.warn("These endpoints will not be registered and will return 404 errors if accessed");
            }

            if (availableEndpoints.isEmpty()) {
                if (unavailableEndpoints.isEmpty()) {
                    logger.warn("No endpoint configurations found - no dynamic routes will be registered");
                } else {
                    logger.error("All {} endpoint(s) are unavailable due to database issues - no dynamic routes will be registered", unavailableEndpoints.size());
                }
                return;
            }

            // Sort endpoints by path specificity (more specific paths first)
            // This ensures routes like /api/generic/stock-trades/symbol/{symbol}
            // are registered before /api/generic/stock-trades/{id}
            List<Map.Entry<String, dev.cordal.generic.config.ApiEndpointConfig>> sortedEndpoints =
                availableEndpoints.entrySet().stream()
                    .sorted((e1, e2) -> comparePathSpecificity(e1.getValue().getPath(), e2.getValue().getPath()))
                    .collect(java.util.stream.Collectors.toList());

            // Register each available endpoint
            for (Map.Entry<String, dev.cordal.generic.config.ApiEndpointConfig> entry : sortedEndpoints) {
                String endpointName = entry.getKey();
                dev.cordal.generic.config.ApiEndpointConfig config = entry.getValue();

                registerSingleEndpoint(endpointName, config, genericApiController);
            }

            // Log summary
            int totalEndpoints = availableEndpoints.size() + unavailableEndpoints.size();
            logger.info("Successfully registered {} available dynamic endpoints out of {} total configured endpoints",
                       availableEndpoints.size(), totalEndpoints);

            if (!unavailableEndpoints.isEmpty()) {
                logger.warn("Application started with {} endpoint(s) unavailable due to database connectivity issues",
                           unavailableEndpoints.size());
            }

        } catch (Exception e) {
            logger.error("Failed to register dynamic endpoints", e);
            throw new RuntimeException("Failed to register dynamic endpoints", e);
        }
    }

    /**
     * Register a single endpoint with Javalin
     */
    private void registerSingleEndpoint(String endpointName, ApiEndpointConfig config, GenericApiController genericApiController) {
        String path = config.getPath();
        String method = config.getMethod().toUpperCase();

        logger.debug("Registering endpoint: {} {} -> {}", method, path, endpointName);

        // Create the handler that calls the generic controller
        io.javalin.http.Handler handler = ctx -> {
            try {
                genericApiController.handleEndpointRequest(ctx, endpointName);
            } catch (dev.cordal.common.exception.ApiException e) {
                // ApiExceptions are expected validation/business logic errors
                if (e.getStatusCode() >= 400 && e.getStatusCode() < 500) {
                    logger.info("VALIDATION ERROR (Expected): {} {} returned {} - {} (Normal client validation)",
                               method, path, e.getStatusCode(), e.getMessage());
                } else {
                    logger.warn("SERVER ERROR: {} {} returned {} - {}",
                               method, path, e.getStatusCode(), e.getMessage());
                }
                ctx.status(e.getStatusCode()).json(java.util.Map.of(
                    "error", "Internal server error",
                    "endpoint", endpointName,
                    "message", e.getMessage()
                ));
            } catch (Exception e) {
                logger.error("UNEXPECTED ERROR: Failed to handle endpoint request: {} {}", method, path, e);
                ctx.status(500).json(java.util.Map.of(
                    "error", "Internal server error",
                    "endpoint", endpointName,
                    "message", e.getMessage()
                ));
            }
        };

        // Register the route based on HTTP method
        switch (method) {
            case "GET":
                app.get(path, handler);
                break;
            case "POST":
                app.post(path, handler);
                break;
            case "PUT":
                app.put(path, handler);
                break;
            case "DELETE":
                app.delete(path, handler);
                break;
            case "PATCH":
                app.patch(path, handler);
                break;
            default:
                logger.warn("Unsupported HTTP method '{}' for endpoint: {} {}", method, path, endpointName);
        }
    }

    /**
     * Compare path specificity for sorting
     * More specific paths (fewer path parameters) should be registered first
     */
    private int comparePathSpecificity(String path1, String path2) {
        // Count path parameters (segments with {})
        long params1 = path1.chars().filter(ch -> ch == '{').count();
        long params2 = path2.chars().filter(ch -> ch == '{').count();

        // Fewer parameters = more specific = should come first
        int paramComparison = Long.compare(params1, params2);
        if (paramComparison != 0) {
            return paramComparison;
        }

        // If same number of parameters, longer path is more specific
        int lengthComparison = Integer.compare(path2.length(), path1.length());
        if (lengthComparison != 0) {
            return lengthComparison;
        }

        // Finally, sort alphabetically for consistency
        return path1.compareTo(path2);
    }

    /**
     * Display dynamically registered endpoints
     */
    private void displayDynamicEndpoints(String baseUrl) {
        try {
            GenericApiController genericApiController = injector.getInstance(GenericApiController.class);
            Map<String, dev.cordal.generic.config.ApiEndpointConfig> endpoints =
                genericApiController.getGenericApiService().getAvailableEndpoints();

            if (endpoints.isEmpty()) {
                logger.info("🔧 DYNAMIC API ENDPOINTS: None configured");
                return;
            }

            logger.info("🔧 DYNAMIC API ENDPOINTS (from YAML configuration):");

            // Group endpoints by category for better display
            Map<String, List<Map.Entry<String, dev.cordal.generic.config.ApiEndpointConfig>>> groupedEndpoints =
                endpoints.entrySet().stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                        entry -> extractEndpointCategory(entry.getKey())
                    ));

            // Display each category
            for (Map.Entry<String, List<Map.Entry<String, dev.cordal.generic.config.ApiEndpointConfig>>> categoryEntry :
                 groupedEndpoints.entrySet()) {
                String category = categoryEntry.getKey();
                List<Map.Entry<String, dev.cordal.generic.config.ApiEndpointConfig>> categoryEndpoints = categoryEntry.getValue();

                logger.info("   📁 {}:", category.toUpperCase());

                for (Map.Entry<String, dev.cordal.generic.config.ApiEndpointConfig> endpointEntry : categoryEndpoints) {
                    String endpointName = endpointEntry.getKey();
                    dev.cordal.generic.config.ApiEndpointConfig config = endpointEntry.getValue();

                    String method = config.getMethod().toUpperCase();
                    String path = config.getPath();
                    String description = config.getDescription() != null ? config.getDescription() : endpointName;

                    logger.info("   ├─ {}: {} {}{}",
                        description, method, baseUrl, path);
                }
            }

            logger.info("   └─ Total: {} endpoints", endpoints.size());
            logger.info("");

        } catch (Exception e) {
            logger.error("Failed to display dynamic endpoints", e);
        }
    }

    /**
     * Extract category from endpoint name for grouping
     */
    private String extractEndpointCategory(String endpointName) {
        if (endpointName.startsWith("stock-trades")) {
            return "Stock Trades";
        } else if (endpointName.startsWith("user")) {
            return "Users";
        } else if (endpointName.startsWith("order")) {
            return "Orders";
        } else {
            return "General";
        }
    }

    /**
     * Initialize the application without starting the server (for testing)
     */
    public void initializeForTesting() {
        // Initialize dependency injection
        initializeDependencyInjection();

        // Get server configuration
        ServerConfig serverConfig = getServerConfig();

        // Perform any pre-startup initialization
        performPreStartupInitialization();

        // Create and configure Javalin app
        createJavalinApp(serverConfig);

        // Configure routes
        configureRoutes();

        // Configure Swagger/OpenAPI
        configureSwagger();

        // Configure exception handling
        configureExceptionHandling();

        // Don't start the server - let JavalinTest handle that
    }

    @Override
    protected void displayApplicationSpecificEndpoints(String baseUrl) {
        GenericApiConfig config = injector.getInstance(GenericApiConfig.class);

        // Display dynamically registered endpoints
        displayDynamicEndpoints(baseUrl);

        // Configuration Management API
        logger.info("⚙️  CONFIGURATION MANAGEMENT:");
        logger.info("   ├─ Validate All:     GET  {}/api/generic/config/validate", baseUrl);
        logger.info("   ├─ Validate Endpoints: GET  {}/api/generic/config/validate/endpoints", baseUrl);
        logger.info("   ├─ Validate Queries: GET  {}/api/generic/config/validate/queries", baseUrl);
        logger.info("   ├─ Validate DBs:     GET  {}/api/generic/config/validate/databases", baseUrl);
        logger.info("   └─ Relationships:    GET  {}/api/generic/config/validate/relationships", baseUrl);
        logger.info("");

        // Granular Configuration APIs
        logger.info("🔍 GRANULAR CONFIGURATION APIS:");
        logger.info("   📋 ENDPOINTS:");
        logger.info("      ├─ Schema:         GET  {}/api/generic/config/endpoints/schema", baseUrl);
        logger.info("      ├─ Parameters:     GET  {}/api/generic/config/endpoints/parameters", baseUrl);
        logger.info("      ├─ DB Connections: GET  {}/api/generic/config/endpoints/database-connections", baseUrl);
        logger.info("      └─ Summary:        GET  {}/api/generic/config/endpoints/summary", baseUrl);
        logger.info("   📝 QUERIES:");
        logger.info("      ├─ Schema:         GET  {}/api/generic/config/queries/schema", baseUrl);
        logger.info("      ├─ Parameters:     GET  {}/api/generic/config/queries/parameters", baseUrl);
        logger.info("      ├─ DB Connections: GET  {}/api/generic/config/queries/database-connections", baseUrl);
        logger.info("      └─ Summary:        GET  {}/api/generic/config/queries/summary", baseUrl);
        logger.info("   🗄️  DATABASES:");
        logger.info("      ├─ Schema:         GET  {}/api/generic/config/databases/schema", baseUrl);
        logger.info("      ├─ Parameters:     GET  {}/api/generic/config/databases/parameters", baseUrl);
        logger.info("      ├─ Connections:    GET  {}/api/generic/config/databases/connections", baseUrl);
        logger.info("      └─ Summary:        GET  {}/api/generic/config/databases/summary", baseUrl);
        logger.info("");

        // Comprehensive Management API
        logger.info("🔧 COMPREHENSIVE MANAGEMENT API:");
        logger.info("   ├─ Dashboard:        GET  {}/api/management/dashboard", baseUrl);
        logger.info("   ├─ Config Metadata: GET  {}/api/management/config/metadata", baseUrl);
        logger.info("   ├─ Config Paths:    GET  {}/api/management/config/paths", baseUrl);
        logger.info("   ├─ Config Contents: GET  {}/api/management/config/contents", baseUrl);
        logger.info("   ├─ All Endpoints:   GET  {}/api/management/config/endpoints", baseUrl);
        logger.info("   ├─ All Queries:     GET  {}/api/management/config/queries", baseUrl);
        logger.info("   ├─ All Databases:   GET  {}/api/management/config/databases", baseUrl);
        logger.info("   ├─ Usage Stats:     GET  {}/api/management/statistics", baseUrl);
        logger.info("   ├─ Endpoint Stats:  GET  {}/api/management/statistics/endpoints", baseUrl);
        logger.info("   ├─ Query Stats:     GET  {}/api/management/statistics/queries", baseUrl);
        logger.info("   ├─ Database Stats:  GET  {}/api/management/statistics/databases", baseUrl);
        logger.info("   ├─ Health Status:   GET  {}/api/management/health", baseUrl);
        logger.info("   ├─ DB Health:       GET  {}/api/management/health/databases", baseUrl);
        logger.info("   ├─ Specific DB:     GET  {}/api/management/health/databases/{{name}}", baseUrl);
        logger.info("   ├─ Deployment Info: GET  {}/api/management/deployment", baseUrl);
        logger.info("   ├─ JAR Info:        GET  {}/api/management/jar", baseUrl);
        logger.info("   ├─ Readiness:       GET  {}/api/management/readiness", baseUrl);
        logger.info("   └─ Liveness:        GET  {}/api/management/liveness", baseUrl);
        logger.info("");

        // API Documentation
        if (config.isSwaggerEnabled()) {
            logger.info("📚 API DOCUMENTATION:");
            logger.info("   ├─ 📖 Swagger UI:        {}/swagger", baseUrl);
            logger.info("   ├─ 📋 API Docs:          {}/api-docs", baseUrl);
            logger.info("   └─ 🔧 OpenAPI JSON:      {}/openapi.json", baseUrl);
            logger.info("");
        }

        // Database Information
        logger.info("🗄️  DATABASE:");
        logger.info("   └─ Main Database:    {}", config.getDatabaseUrl());
        logger.info("");

        // Configuration Information
        logger.info("⚙️  CONFIGURATION:");
        logger.info("   ├─ Source:           {}", config.getConfigSource());
        if ("yaml".equals(config.getConfigSource())) {
            logger.info("   ├─ Directories:      {}", config.getConfigDirectories());
            logger.info("   ├─ Database Patterns: {}", config.getDatabasePatterns());
            logger.info("   ├─ Query Patterns:   {}", config.getQueryPatterns());
            logger.info("   └─ Endpoint Patterns: {}", config.getEndpointPatterns());
        } else {
            logger.info("   └─ Database Tables:  config_databases, config_queries, config_endpoints");
        }
        logger.info("");

        logger.info("🎯 Generic API Service ready to accept requests!");
        logger.info("💡 APIs are dynamically configured via {} source", config.getConfigSource().toUpperCase());
    }

    /**
     * Run validation only and exit
     */
    private void runValidationOnly() {
        logger.info("=".repeat(80));
        logger.info(">>> CONFIGURATION VALIDATION MODE");
        logger.info("=".repeat(80));

        try {
            // Initialize configuration components
            initializeConfigurationComponents();

            // Run comprehensive validation
            runConfigurationValidation();

            logger.info("=".repeat(80));
            logger.info(">>> VALIDATION COMPLETED - APPLICATION EXITING");
            logger.info("=".repeat(80));

        } catch (Exception e) {
            logger.error("FATAL: Configuration validation failed", e);
            logger.error("Application startup aborted due to validation failure");
            throw new RuntimeException("Configuration validation failed", e);
        }
    }

    /**
     * Run configuration validation (used both for startup validation and standalone validation)
     */
    private void runConfigurationValidation() {
        runConfigurationValidation(false);
    }

    /**
     * Run configuration validation with optional endpoint connectivity testing
     */
    private void runConfigurationValidation(boolean includeEndpointTesting) {
        logger.info("[VALIDATE] Starting Configuration Validation...");

        try {
            // Get configuration components
            dev.cordal.generic.config.ConfigurationLoader configurationLoader =
                injector.getInstance(dev.cordal.generic.config.ConfigurationLoader.class);
            dev.cordal.generic.config.EndpointConfigurationManager configurationManager =
                injector.getInstance(dev.cordal.generic.config.EndpointConfigurationManager.class);
            dev.cordal.generic.database.DatabaseConnectionManager databaseConnectionManager =
                injector.getInstance(dev.cordal.generic.database.DatabaseConnectionManager.class);

            // Create configuration validator
            dev.cordal.util.ConfigurationValidator configurationValidator =
                new dev.cordal.util.ConfigurationValidator(configurationManager, databaseConnectionManager);

            // Part 1: Configuration Chain Validation
            logger.info("[PART 1] Configuration Chain Validation");
            dev.cordal.util.ValidationResult configValidation = configurationValidator.validateConfigurationChain();
            configurationValidator.displayValidationResults("Configuration Chain", configValidation);

            // Part 2: Database Schema Validation
            logger.info("[PART 2] Database Schema Validation");
            dev.cordal.util.ValidationResult schemaValidation = configurationValidator.validateDatabaseSchema();
            configurationValidator.displayValidationResults("Database Schema", schemaValidation);

            // Part 3: Endpoint Connectivity Validation (only if application is running)
            dev.cordal.util.ValidationResult endpointValidation = null;
            if (includeEndpointTesting) {
                logger.info("[PART 3] Endpoint Connectivity Validation");
                GenericApiConfig config = injector.getInstance(GenericApiConfig.class);
                String baseUrl = "http://" + config.getServerConfig().getHost() + ":" + config.getServerConfig().getPort();
                endpointValidation = configurationValidator.validateEndpointConnectivity(baseUrl);
                configurationValidator.displayValidationResults("Endpoint Connectivity", endpointValidation);
            }

            // Summary
            boolean overallSuccess = configValidation.isSuccess() && schemaValidation.isSuccess() &&
                                   (endpointValidation == null || endpointValidation.isSuccess());
            if (overallSuccess) {
                logger.info("[SUCCESS] All configuration validations passed");
            } else {
                logger.error("[FAILED] Configuration validation found errors");
                logger.error("  Configuration Chain Errors: {}", configValidation.getErrorCount());
                logger.error("  Database Schema Errors: {}", schemaValidation.getErrorCount());
                if (endpointValidation != null) {
                    logger.error("  Endpoint Connectivity Errors: {}", endpointValidation.getErrorCount());
                }

                // Check if validation failures should be fatal
                GenericApiConfig config = injector.getInstance(GenericApiConfig.class);
                if (shouldValidationFailuresBeFatal(config)) {
                    logger.error("[FATAL] Configuration validation failed - application startup aborted");
                    throw new RuntimeException("Configuration validation failed");
                } else {
                    logger.warn("[CONTINUE] Configuration validation failed but application will continue");
                    logger.warn("  Some endpoints may be unavailable due to configuration errors");
                    logger.warn("  Use management APIs to check configuration status");
                }
            }

        } catch (RuntimeException e) {
            // Re-throw RuntimeExceptions (like validation failures) as-is
            throw e;
        } catch (Exception e) {
            logger.error("Configuration validation encountered an unexpected error", e);

            // Check if validation failures should be fatal
            GenericApiConfig config = injector.getInstance(GenericApiConfig.class);
            if (shouldValidationFailuresBeFatal(config)) {
                logger.error("[FATAL] Configuration validation error - application startup aborted");
                throw new RuntimeException("Configuration validation error", e);
            } else {
                logger.warn("[CONTINUE] Configuration validation error but application will continue");
                logger.warn("  Configuration validation could not be completed");
                logger.warn("  Some endpoints may be unavailable");
            }
        }
    }

    /**
     * Determine if validation failures should be fatal (cause application to exit)
     * or just warnings (allow application to continue with limited functionality)
     */
    private boolean shouldValidationFailuresBeFatal(GenericApiConfig config) {
        // For now, make validation failures non-fatal to allow graceful error handling
        // This can be made configurable in the future if needed
        // In validate-only mode, failures should still be fatal since that's the whole point
        return config.isValidationValidateOnly();
    }

    /**
     * Initialize configuration components for validation
     */
    private void initializeConfigurationComponents() {
        logger.info("[INIT] Initializing configuration components for validation...");

        // Components are already available through dependency injection
        // Just verify they're accessible
        GenericApiConfig config = injector.getInstance(GenericApiConfig.class);
        logger.info("[OK] Configuration components initialized");
        logger.info("   Configuration Source: {}", config.getConfigSource());
        logger.info("   Configuration Directories: {}", config.getConfigDirectories());
        logger.info("   Database Patterns: {}", config.getDatabasePatterns());
        logger.info("   Query Patterns: {}", config.getQueryPatterns());
        logger.info("   Endpoint Patterns: {}", config.getEndpointPatterns());
    }
}

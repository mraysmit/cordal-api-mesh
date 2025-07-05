package dev.mars.generic;

import com.google.inject.Module;
import dev.mars.common.application.BaseJavalinApplication;
import dev.mars.common.config.ServerConfig;

import dev.mars.config.GenericApiConfig;
import dev.mars.config.GenericApiGuiceModule;
import dev.mars.config.SwaggerConfig;
import dev.mars.util.ApiEndpoints;

import dev.mars.generic.config.ApiEndpointConfig;
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
            System.exit(1);
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
        dev.mars.database.DatabaseManager dbManager = injector.getInstance(dev.mars.database.DatabaseManager.class);
        logger.info("Configuration database initialized successfully");

        // Check if validation should run on startup
        GenericApiConfig config = injector.getInstance(GenericApiConfig.class);
        if (config.isValidationRunOnStartup()) {
            logger.info("Running configuration validation on startup (validation.runOnStartup=true)");
            runConfigurationValidation();
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
        dev.mars.generic.management.ManagementController managementController = injector.getInstance(dev.mars.generic.management.ManagementController.class);
        
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

        // Comprehensive dashboard endpoint
        app.get(ApiEndpoints.Management.DASHBOARD, managementController::getManagementDashboard);

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
            // Get endpoint configurations
            Map<String, dev.mars.generic.config.ApiEndpointConfig> endpoints =
                genericApiController.getGenericApiService().getAvailableEndpoints();

            if (endpoints.isEmpty()) {
                logger.warn("No endpoint configurations found - no dynamic routes will be registered");
                return;
            }

            // Sort endpoints by path specificity (more specific paths first)
            // This ensures routes like /api/generic/stock-trades/symbol/{symbol}
            // are registered before /api/generic/stock-trades/{id}
            List<Map.Entry<String, dev.mars.generic.config.ApiEndpointConfig>> sortedEndpoints =
                endpoints.entrySet().stream()
                    .sorted((e1, e2) -> comparePathSpecificity(e1.getValue().getPath(), e2.getValue().getPath()))
                    .collect(java.util.stream.Collectors.toList());

            // Register each endpoint
            for (Map.Entry<String, dev.mars.generic.config.ApiEndpointConfig> entry : sortedEndpoints) {
                String endpointName = entry.getKey();
                dev.mars.generic.config.ApiEndpointConfig config = entry.getValue();

                registerSingleEndpoint(endpointName, config, genericApiController);
            }

            logger.info("Successfully registered {} dynamic endpoints", endpoints.size());

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
            } catch (Exception e) {
                logger.error("Failed to handle endpoint request: {} {}", method, path, e);
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
            Map<String, dev.mars.generic.config.ApiEndpointConfig> endpoints =
                genericApiController.getGenericApiService().getAvailableEndpoints();

            if (endpoints.isEmpty()) {
                logger.info("🔧 DYNAMIC API ENDPOINTS: None configured");
                return;
            }

            logger.info("🔧 DYNAMIC API ENDPOINTS (from YAML configuration):");

            // Group endpoints by category for better display
            Map<String, List<Map.Entry<String, dev.mars.generic.config.ApiEndpointConfig>>> groupedEndpoints =
                endpoints.entrySet().stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                        entry -> extractEndpointCategory(entry.getKey())
                    ));

            // Display each category
            for (Map.Entry<String, List<Map.Entry<String, dev.mars.generic.config.ApiEndpointConfig>>> categoryEntry :
                 groupedEndpoints.entrySet()) {
                String category = categoryEntry.getKey();
                List<Map.Entry<String, dev.mars.generic.config.ApiEndpointConfig>> categoryEndpoints = categoryEntry.getValue();

                logger.info("   📁 {}:", category.toUpperCase());

                for (Map.Entry<String, dev.mars.generic.config.ApiEndpointConfig> endpointEntry : categoryEndpoints) {
                    String endpointName = endpointEntry.getKey();
                    dev.mars.generic.config.ApiEndpointConfig config = endpointEntry.getValue();

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
        logger.info("   └─ Specific DB:     GET  {}/api/management/health/databases/{{name}}", baseUrl);
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
            logger.info("   ├─ Databases:        {}", config.getDatabasesConfigPath());
            logger.info("   ├─ Queries:          {}", config.getQueriesConfigPath());
            logger.info("   └─ Endpoints:        {}", config.getEndpointsConfigPath());
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
            System.exit(1);
        }
    }

    /**
     * Run configuration validation (used both for startup validation and standalone validation)
     */
    private void runConfigurationValidation() {
        logger.info("[VALIDATE] Starting Configuration Validation...");

        try {
            // Get configuration components
            dev.mars.generic.config.ConfigurationLoader configurationLoader =
                injector.getInstance(dev.mars.generic.config.ConfigurationLoader.class);
            dev.mars.generic.config.EndpointConfigurationManager configurationManager =
                injector.getInstance(dev.mars.generic.config.EndpointConfigurationManager.class);
            dev.mars.generic.database.DatabaseConnectionManager databaseConnectionManager =
                injector.getInstance(dev.mars.generic.database.DatabaseConnectionManager.class);

            // Create configuration validator
            dev.mars.util.ConfigurationValidator configurationValidator =
                new dev.mars.util.ConfigurationValidator(configurationManager, databaseConnectionManager);

            // Part 1: Configuration Chain Validation
            logger.info("[PART 1] Configuration Chain Validation");
            dev.mars.util.ValidationResult configValidation = configurationValidator.validateConfigurationChain();
            configurationValidator.displayValidationResults("Configuration Chain", configValidation);

            // Part 2: Database Schema Validation
            logger.info("[PART 2] Database Schema Validation");
            dev.mars.util.ValidationResult schemaValidation = configurationValidator.validateDatabaseSchema();
            configurationValidator.displayValidationResults("Database Schema", schemaValidation);

            // Summary
            boolean overallSuccess = configValidation.isSuccess() && schemaValidation.isSuccess();
            if (overallSuccess) {
                logger.info("[SUCCESS] All configuration validations passed");
            } else {
                logger.error("[FAILED] Configuration validation failed");
                logger.error("  Configuration Chain Errors: {}", configValidation.getErrorCount());
                logger.error("  Database Schema Errors: {}", schemaValidation.getErrorCount());
                throw new RuntimeException("Configuration validation failed");
            }

        } catch (Exception e) {
            logger.error("Configuration validation failed", e);
            throw e;
        }
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
        logger.info("   Databases Path: {}", config.getDatabasesConfigPath());
        logger.info("   Queries Path: {}", config.getQueriesConfigPath());
        logger.info("   Endpoints Path: {}", config.getEndpointsConfigPath());
    }
}

package dev.mars.generic;

import com.google.inject.Module;
import dev.mars.common.application.BaseJavalinApplication;
import dev.mars.common.config.ServerConfig;
import dev.mars.common.util.StockTradesTestDataInitializer;

import dev.mars.config.GenericApiConfig;
import dev.mars.config.GenericApiGuiceModule;
import dev.mars.config.SwaggerConfig;

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
        logger.info("GenericApiApplication - Retrieved server config: host={}, port={}",
                   serverConfig.getHost(), serverConfig.getPort());
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

        // Check if test data loading is enabled via system property
        String testDataProperty = System.getProperty("test.data.loading.enabled", "false");
        boolean testDataLoadingEnabled = Boolean.parseBoolean(testDataProperty);
        logger.info("Test data loading property: '{}', enabled: {}", testDataProperty, testDataLoadingEnabled);

        if (testDataLoadingEnabled) {
            logger.info("Test data loading enabled - initializing stock trades data");
            try {
                // Initialize stock trades data for testing using common library
                initializeStockTradesForTesting();
                logger.info("Stock trades data initialized successfully for testing");
            } catch (Exception e) {
                logger.error("Failed to initialize stock trades data for testing", e);
                throw new RuntimeException("Failed to initialize stock trades data for testing", e);
            }
        } else {
            logger.info("Production startup - no data loading performed");
        }
    }

    @Override
    protected void configureSwagger() {
        logger.info("Configuring Swagger/OpenAPI");
        SwaggerConfig swaggerConfig = injector.getInstance(SwaggerConfig.class);
        swaggerConfig.configureSwagger(app);
        logger.info("Swagger/OpenAPI configured");
    }

    /**
     * Initialize stock trades data for testing using common library
     */
    private void initializeStockTradesForTesting() throws Exception {
        logger.info("Initializing stock trades table and sample data for testing");

        // Get database connection manager
        dev.mars.generic.database.DatabaseConnectionManager dbConnectionManager =
            injector.getInstance(dev.mars.generic.database.DatabaseConnectionManager.class);

        // Get connection to stocktrades database and use common library initializer
        try (java.sql.Connection connection = dbConnectionManager.getConnection("stocktrades")) {
            StockTradesTestDataInitializer.initializeStockTradesForTesting(connection);
        }
    }
    @Override
    protected void configureRoutes() {
        logger.info("Configuring routes");
        
        GenericApiController genericApiController = injector.getInstance(GenericApiController.class);
        dev.mars.generic.management.ManagementController managementController = injector.getInstance(dev.mars.generic.management.ManagementController.class);
        
        // Health check endpoint
        app.get("/api/health", ctx -> {
            ctx.json(java.util.Map.of(
                "status", "UP",
                "timestamp", System.currentTimeMillis(),
                "service", "generic-api-service"
            ));
        });

        // Generic API management endpoints
        app.get("/api/generic/health", genericApiController::getHealthStatus);
        app.get("/api/generic/endpoints", genericApiController::getAvailableEndpoints);
        app.get("/api/generic/endpoints/{endpointName}", genericApiController::getEndpointConfiguration);

        // Configuration endpoints
        app.get("/api/generic/config", genericApiController::getCompleteConfiguration);

        // ========== GRANULAR CONFIGURATION ENDPOINTS (MUST BE BEFORE PARAMETERIZED ROUTES) ==========

        // Granular configuration endpoints - Endpoints
        app.get("/api/generic/config/endpoints/schema", genericApiController::getEndpointConfigurationSchema);
        app.get("/api/generic/config/endpoints/parameters", genericApiController::getEndpointParameters);
        app.get("/api/generic/config/endpoints/database-connections", genericApiController::getEndpointDatabaseConnections);
        app.get("/api/generic/config/endpoints/summary", genericApiController::getEndpointConfigurationSummary);

        // Granular configuration endpoints - Queries
        app.get("/api/generic/config/queries/schema", genericApiController::getQueryConfigurationSchema);
        app.get("/api/generic/config/queries/parameters", genericApiController::getQueryParameters);
        app.get("/api/generic/config/queries/database-connections", genericApiController::getQueryDatabaseConnections);
        app.get("/api/generic/config/queries/summary", genericApiController::getQueryConfigurationSummary);

        // Granular configuration endpoints - Databases
        app.get("/api/generic/config/databases/schema", genericApiController::getDatabaseConfigurationSchema);
        app.get("/api/generic/config/databases/parameters", genericApiController::getDatabaseParameters);
        app.get("/api/generic/config/databases/connections", genericApiController::getDatabaseConnections);
        app.get("/api/generic/config/databases/summary", genericApiController::getDatabaseConfigurationSummary);

        // ========== PARAMETERIZED CONFIGURATION ENDPOINTS (MUST BE AFTER SPECIFIC ROUTES) ==========

        app.get("/api/generic/config/queries", genericApiController::getQueryConfigurations);
        app.get("/api/generic/config/queries/{queryName}", genericApiController::getQueryConfiguration);
        app.get("/api/generic/config/databases", genericApiController::getDatabaseConfigurations);
        app.get("/api/generic/config/databases/{databaseName}", genericApiController::getDatabaseConfiguration);
        app.get("/api/generic/config/relationships", genericApiController::getConfigurationRelationships);

        // Configuration validation endpoints
        app.get("/api/generic/config/validate", genericApiController::validateConfigurations);
        app.get("/api/generic/config/validate/endpoints", genericApiController::validateEndpointConfigurations);
        app.get("/api/generic/config/validate/queries", genericApiController::validateQueryConfigurations);
        app.get("/api/generic/config/validate/databases", genericApiController::validateDatabaseConfigurations);
        app.get("/api/generic/config/validate/relationships", genericApiController::validateConfigurationRelationships);

        // ========== COMPREHENSIVE MANAGEMENT ENDPOINTS ==========

        // Configuration metadata endpoints
        app.get("/api/management/config/metadata", managementController::getConfigurationMetadata);
        app.get("/api/management/config/paths", managementController::getConfigurationPaths);
        app.get("/api/management/config/contents", managementController::getConfigurationFileContents);

        // Configuration view endpoints
        app.get("/api/management/config/endpoints", managementController::getConfiguredEndpoints);
        app.get("/api/management/config/queries", managementController::getConfiguredQueries);
        app.get("/api/management/config/databases", managementController::getConfiguredDatabases);

        // Usage statistics endpoints
        app.get("/api/management/statistics", managementController::getUsageStatistics);
        app.get("/api/management/statistics/endpoints", managementController::getEndpointStatistics);
        app.get("/api/management/statistics/queries", managementController::getQueryStatistics);
        app.get("/api/management/statistics/databases", managementController::getDatabaseStatistics);

        // Health monitoring endpoints
        app.get("/api/management/health", managementController::getHealthStatus);
        app.get("/api/management/health/databases", managementController::getDatabaseHealth);
        app.get("/api/management/health/databases/{databaseName}", managementController::getSpecificDatabaseHealth);

        // Comprehensive dashboard endpoint
        app.get("/api/management/dashboard", managementController::getManagementDashboard);

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
}

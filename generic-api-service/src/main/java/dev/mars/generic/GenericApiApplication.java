package dev.mars.generic;

import com.google.inject.Module;
import dev.mars.common.application.BaseJavalinApplication;
import dev.mars.common.config.ServerConfig;
import dev.mars.config.GenericApiConfig;
import dev.mars.config.GenericApiGuiceModule;
import dev.mars.config.SwaggerConfig;
import dev.mars.database.DataLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        // Initialize data loader (this will create schema and load sample data)
        injector.getInstance(DataLoader.class);
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

        // Stock trades generic endpoints (configured via YAML)
        // IMPORTANT: Specific routes must be registered before generic routes with path parameters
        app.get("/api/generic/stock-trades", ctx ->
            genericApiController.handleEndpointRequest(ctx, "stock-trades-list"));

        app.get("/api/generic/stock-trades/symbol/{symbol}", ctx ->
            genericApiController.handleEndpointRequest(ctx, "stock-trades-by-symbol"));

        app.get("/api/generic/stock-trades/trader/{trader_id}", ctx ->
            genericApiController.handleEndpointRequest(ctx, "stock-trades-by-trader"));

        app.get("/api/generic/stock-trades/date-range", ctx ->
            genericApiController.handleEndpointRequest(ctx, "stock-trades-by-date-range"));

        // Generic {id} route must be last to avoid catching specific routes
        app.get("/api/generic/stock-trades/{id}", ctx ->
            genericApiController.handleEndpointRequest(ctx, "stock-trades-by-id"));
        
        logger.info("Routes configured");
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


        // Generic API Endpoints
        logger.info("🔧 GENERIC API SYSTEM:");
        logger.info("   ├─ Stock Trades:     GET  {}/api/generic/stock-trades", baseUrl);
        logger.info("   ├─ Trade by ID:      GET  {}/api/generic/stock-trades/{{id}}", baseUrl);
        logger.info("   ├─ By Symbol:        GET  {}/api/generic/stock-trades/symbol/{{symbol}}", baseUrl);
        logger.info("   ├─ By Trader:        GET  {}/api/generic/stock-trades/trader/{{trader_id}}", baseUrl);
        logger.info("   └─ Date Range:       GET  {}/api/generic/stock-trades/date-range", baseUrl);
        logger.info("");

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

        logger.info("🎯 Generic API Service ready to accept requests!");
        logger.info("💡 APIs are dynamically configured via YAML files");
    }
}

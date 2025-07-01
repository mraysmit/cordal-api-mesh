package dev.mars.generic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.mars.config.GenericApiConfig;
import dev.mars.config.GenericApiGuiceModule;
import dev.mars.config.SwaggerConfig;
import dev.mars.database.DataLoader;
import dev.mars.exception.GlobalExceptionHandler;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application class for the Generic API Service
 */
public class GenericApiApplication {
    private static final Logger logger = LoggerFactory.getLogger(GenericApiApplication.class);
    
    private Javalin app;
    private Injector injector;
    
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
    
    public void start() {
        logger.info("Starting Generic API Service");
        
        try {
            // Initialize dependency injection
            initializeDependencyInjection();
            
            // Get configuration
            GenericApiConfig config = injector.getInstance(GenericApiConfig.class);
            
            // Initialize data loader (this will create schema and load sample data)
            injector.getInstance(DataLoader.class);
            
            // Create and configure Javalin app
            createJavalinApp(config);
            
            // Configure routes
            configureRoutes();

            // Configure Swagger/OpenAPI
            configureSwagger();

            // Configure exception handling
            configureExceptionHandling();
            
            // Start the server
            startServer(config);
            
            logger.info("Generic API Service started successfully");

            // Display all available endpoints
            displayAvailableEndpoints(config);
            
        } catch (Exception e) {
            logger.error("Failed to start Generic API Service", e);
            throw new RuntimeException("Generic API Service startup failed", e);
        }
    }
    
    public void stop() {
        logger.info("Stopping Generic API Service");
        
        if (app != null) {
            app.stop();
            app = null;
            logger.info("Javalin server stopped");
        }
        
        logger.info("Generic API Service stopped");
    }
    
    private void initializeDependencyInjection() {
        logger.info("Initializing dependency injection");
        injector = Guice.createInjector(new GenericApiGuiceModule());
        logger.info("Dependency injection initialized");
    }
    
    private void createJavalinApp(GenericApiConfig config) {
        logger.info("Creating Javalin application");
        
        // Configure Jackson for JSON serialization
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        app = Javalin.create(javalinConfig -> {
            // Configure JSON mapper
            javalinConfig.jsonMapper(new JavalinJackson(objectMapper, true));

            // Enable CORS for development
            javalinConfig.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.anyHost();
                    it.allowCredentials = false;
                });
            });

            // Enable request logging
            javalinConfig.bundledPlugins.enableDevLogging();

            // Set server configuration
            javalinConfig.jetty.defaultHost = config.getServerHost();
            javalinConfig.jetty.defaultPort = config.getServerPort();
        });
        
        logger.info("Javalin application created");
    }
    
    private void configureRoutes() {
        logger.info("Configuring routes");
        
        GenericApiController genericApiController = injector.getInstance(GenericApiController.class);
        
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

    private void configureSwagger() {
        logger.info("Configuring Swagger/OpenAPI");
        SwaggerConfig swaggerConfig = injector.getInstance(SwaggerConfig.class);
        swaggerConfig.configureSwagger(app);
        logger.info("Swagger/OpenAPI configured");
    }
    
    private void configureExceptionHandling() {
        logger.info("Configuring exception handling");
        GlobalExceptionHandler.configure(app);
        logger.info("Exception handling configured");
    }
    
    private void startServer(GenericApiConfig config) {
        logger.info("Starting server on {}:{}", config.getServerHost(), config.getServerPort());
        app.start();
        logger.info("Server started successfully");
    }
    
    // Getter for testing purposes
    public Javalin getApp() {
        return app;
    }

    public Injector getInjector() {
        return injector;
    }

    /**
     * Initialize the application without starting the server (for testing)
     */
    public void initializeForTesting() {
        // Initialize dependency injection
        initializeDependencyInjection();

        // Get configuration
        GenericApiConfig config = injector.getInstance(GenericApiConfig.class);

        // Initialize data loader (this will create schema and load sample data)
        injector.getInstance(DataLoader.class);

        // Create and configure Javalin app
        createJavalinApp(config);

        // Configure routes
        configureRoutes();

        // Configure Swagger/OpenAPI
        configureSwagger();

        // Configure exception handling
        configureExceptionHandling();

        // Don't start the server - let JavalinTest handle that
    }

    public int getPort() {
        return app != null ? app.port() : -1;
    }

    /**
     * Display all available endpoints on startup
     */
    private void displayAvailableEndpoints(GenericApiConfig config) {
        String host = config.getServerHost();
        int port = config.getServerPort();
        String baseUrl = String.format("http://%s:%d", host, port);

        logger.info("=".repeat(80));
        logger.info("🚀 GENERIC API SERVICE STARTED SUCCESSFULLY");
        logger.info("=".repeat(80));
        logger.info("📍 Server URL: {}", baseUrl);
        logger.info("");

        // Health and System Endpoints
        logger.info("🏥 HEALTH & SYSTEM:");
        logger.info("   ├─ Health Check:     GET  {}/api/health", baseUrl);
        logger.info("   └─ Generic Health:   GET  {}/api/generic/health", baseUrl);
        logger.info("");

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

        logger.info("=".repeat(80));
        logger.info("🎯 Generic API Service ready to accept requests!");
        logger.info("💡 APIs are dynamically configured via YAML files");
        logger.info("=".repeat(80));
    }
}

package dev.mars;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.mars.config.AppConfig;
import dev.mars.config.DatabaseConfig;
import dev.mars.config.GuiceModule;
import dev.mars.config.SwaggerConfig;
import dev.mars.database.DataLoader;
import dev.mars.exception.GlobalExceptionHandler;
import dev.mars.routes.ApiRoutes;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application class for the Javalin API Mesh - Generic API System
 */
public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    
    private Javalin app;
    private Injector injector;
    
    public static void main(String[] args) {
        try {
            Application application = new Application();
            application.start();
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(application::stop));
            
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            System.exit(1);
        }
    }
    
    public void start() {
        logger.info("Starting Javalin API Mesh - Generic API System");
        
        try {
            // Initialize dependency injection
            initializeDependencyInjection();
            
            // Get configuration
            AppConfig appConfig = injector.getInstance(AppConfig.class);
            
            // Initialize data loader (this will create schema and load sample data)
            injector.getInstance(DataLoader.class);
            
            // Create and configure Javalin app
            createJavalinApp(appConfig);
            
            // Configure routes
            configureRoutes();

            // Configure Swagger/OpenAPI
            configureSwagger();

            // Configure exception handling
            configureExceptionHandling();
            
            // Start the server
            startServer(appConfig);
            
            logger.info("Application started successfully");

            // Display all available endpoints
            displayAvailableEndpoints(appConfig);
            
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            throw new RuntimeException("Application startup failed", e);
        }
    }
    
    public void stop() {
        logger.info("Stopping application");
        
        if (app != null) {
            app.stop();
            app = null; // Clear the reference to allow new instance creation
            logger.info("Javalin server stopped");
        }
        
        // Close database connections
        if (injector != null) {
            try {
                DatabaseConfig databaseConfig = injector.getInstance(DatabaseConfig.class);
                databaseConfig.close();
                logger.info("Database connections closed");
            } catch (Exception e) {
                logger.error("Error closing database connections", e);
            }
        }
        
        logger.info("Application stopped");
    }
    
    private void initializeDependencyInjection() {
        logger.info("Initializing dependency injection");
        injector = Guice.createInjector(new GuiceModule());
        logger.info("Dependency injection initialized");
    }
    
    private void createJavalinApp(AppConfig appConfig) {
        logger.info("Creating Javalin application");
        
        // Configure Jackson for JSON serialization
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        app = Javalin.create(config -> {
            // Configure JSON mapper
            config.jsonMapper(new JavalinJackson(objectMapper, true));

            // Enable CORS for development
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.anyHost();
                    it.allowCredentials = false;
                });
            });

            // Enable request logging
            config.bundledPlugins.enableDevLogging();

            // Set server configuration
            config.jetty.defaultHost = appConfig.getServerHost();
            config.jetty.defaultPort = appConfig.getServerPort();
        });
        
        logger.info("Javalin application created");
    }
    
    private void configureRoutes() {
        logger.info("Configuring routes");
        ApiRoutes apiRoutes = injector.getInstance(ApiRoutes.class);
        apiRoutes.configure(app);
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
    
    private void startServer(AppConfig appConfig) {
        logger.info("Starting server on {}:{}", appConfig.getServerHost(), appConfig.getServerPort());
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

    public int getPort() {
        return app != null ? app.port() : -1;
    }

    /**
     * Display all available endpoints on startup
     */
    private void displayAvailableEndpoints(AppConfig appConfig) {
        String host = appConfig.getServerHost();
        int port = appConfig.getServerPort();
        String baseUrl = String.format("http://%s:%d", host, port);

        logger.info("=".repeat(80));
        logger.info("🚀 JAVALIN API MESH - GENERIC API SYSTEM STARTED SUCCESSFULLY");
        logger.info("=".repeat(80));
        logger.info("📍 Server URL: {}", baseUrl);
        logger.info("");

        // Health and System Endpoints
        logger.info("🏥 HEALTH & SYSTEM:");
        logger.info("   ├─ Health Check:     GET  {}/api/health", baseUrl);
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



        // Performance Metrics API Endpoints
        logger.info("📊 PERFORMANCE METRICS API:");
        logger.info("   ├─ Get All Metrics:  GET  {}/api/performance-metrics", baseUrl);
        logger.info("   ├─ Get by ID:        GET  {}/api/performance-metrics/{{id}}", baseUrl);
        logger.info("   ├─ Create Metric:    POST {}/api/performance-metrics", baseUrl);
        logger.info("   ├─ Get Summary:      GET  {}/api/performance-metrics/summary", baseUrl);
        logger.info("   ├─ Get Trends:       GET  {}/api/performance-metrics/trends", baseUrl);
        logger.info("   ├─ Get Test Types:   GET  {}/api/performance-metrics/test-types", baseUrl);
        logger.info("   ├─ Get by Type:      GET  {}/api/performance-metrics/test-type/{{type}}", baseUrl);
        logger.info("   └─ Get Date Range:   GET  {}/api/performance-metrics/date-range", baseUrl);
        logger.info("");

        // API Documentation
        logger.info("📚 API DOCUMENTATION:");
        logger.info("   ├─ 📖 Swagger UI:        {}/swagger", baseUrl);
        logger.info("   ├─ 📋 API Docs:          {}/api-docs", baseUrl);
        logger.info("   └─ 🔧 OpenAPI JSON:      {}/openapi.json", baseUrl);
        logger.info("");

        // Dashboard Endpoints
        logger.info("📱 DASHBOARDS:");

        // Custom Dashboard
        if (appConfig.getMetricsDashboard().getCustom().isEnabled()) {
            String dashboardPath = appConfig.getMetricsDashboard().getCustom().getPath();
            logger.info("   ├─ 🎨 Custom Dashboard:  {}{}", baseUrl, dashboardPath);
            logger.info("   │  └─ Interactive performance monitoring with charts");
        } else {
            logger.info("   ├─ 🎨 Custom Dashboard:  ❌ DISABLED");
        }

        // Grafana Integration
        if (appConfig.getMetricsDashboard().getGrafana().isEnabled()) {
            logger.info("   └─ 📊 Grafana Integration: ✅ ENABLED");
            logger.info("      ├─ Grafana URL:      {}", appConfig.getMetricsDashboard().getGrafana().getUrl());

            if (appConfig.getMetricsDashboard().getGrafana().getPrometheus().isEnabled()) {
                String prometheusPath = appConfig.getMetricsDashboard().getGrafana().getPrometheus().getPath();
                logger.info("      └─ Prometheus:       GET  {}{}", baseUrl, prometheusPath);
            } else {
                logger.info("      └─ Prometheus:       ❌ DISABLED");
            }
        } else {
            logger.info("   └─ 📊 Grafana Integration: ❌ DISABLED");
        }

        logger.info("");

        // Database Information
        logger.info("🗄️  DATABASES:");
        logger.info("   ├─ Main Database:    {}", appConfig.getDatabaseUrl());
        logger.info("   └─ Metrics Database: {}", appConfig.getMetricsDatabase().getUrl());
        logger.info("");

        // Configuration Information
        logger.info("⚙️  CONFIGURATION:");
        logger.info("   ├─ API Configuration: 📄 YAML-based (api-endpoints.yml, queries.yml, databases.yml)");
        logger.info("   ├─ Custom Dashboard:  {}", appConfig.getMetricsDashboard().getCustom().isEnabled() ? "✅ ENABLED" : "❌ DISABLED");
        logger.info("   ├─ Grafana Mode:      {}", appConfig.getMetricsDashboard().getGrafana().isEnabled() ? "✅ ENABLED" : "❌ DISABLED");
        logger.info("   └─ Prometheus:        {}",
            appConfig.getMetricsDashboard().getGrafana().isEnabled() &&
            appConfig.getMetricsDashboard().getGrafana().getPrometheus().isEnabled() ? "✅ ENABLED" : "❌ DISABLED");

        logger.info("=".repeat(80));
        logger.info("🎯 Generic API System ready to accept requests!");
        logger.info("💡 APIs are dynamically configured via YAML files");
        logger.info("=".repeat(80));
    }
}

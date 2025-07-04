package dev.mars.common.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import dev.mars.common.config.ServerConfig;
import dev.mars.common.exception.BaseGlobalExceptionHandler;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for Javalin applications
 * Provides common application bootstrap patterns and configuration
 */
public abstract class BaseJavalinApplication {
    private static final Logger logger = LoggerFactory.getLogger(BaseJavalinApplication.class);
    
    protected Javalin app;
    protected Injector injector;

    /**
     * Abstract method to get the Guice module for dependency injection
     */
    protected abstract Module getGuiceModule();

    /**
     * Abstract method to get server configuration
     */
    protected abstract ServerConfig getServerConfig();

    /**
     * Abstract method to configure application-specific routes
     */
    protected abstract void configureRoutes();

    /**
     * Abstract method to get application name for logging
     */
    protected abstract String getApplicationName();

    /**
     * Start the application
     */
    public void start() {
        logger.info("Starting {}", getApplicationName());
        
        try {
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

            // Configure Swagger/OpenAPI if needed
            configureSwagger();

            // Configure exception handling
            configureExceptionHandling();
            
            // Start the server
            startServer(serverConfig);
            
            logger.info("{} started successfully", getApplicationName());

            // Display available endpoints
            displayAvailableEndpoints(serverConfig);
            
        } catch (Exception e) {
            logger.error("Failed to start {}", getApplicationName(), e);
            throw new RuntimeException(getApplicationName() + " startup failed", e);
        }
    }

    /**
     * Stop the application
     */
    public void stop() {
        logger.info("Stopping {}", getApplicationName());
        
        if (app != null) {
            app.stop();
            app = null;
            logger.info("Javalin server stopped");
        }
        
        // Perform any cleanup
        performCleanup();
        
        logger.info("{} stopped", getApplicationName());
    }

    /**
     * Initialize dependency injection
     */
    protected void initializeDependencyInjection() {
        logger.info("Initializing dependency injection");
        injector = Guice.createInjector(getGuiceModule());
        logger.info("Dependency injection initialized");
    }

    /**
     * Create and configure Javalin application
     */
    protected void createJavalinApp(ServerConfig serverConfig) {
        logger.info("Creating Javalin application");

        // Configure Jackson for JSON serialization
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        // Configure to write dates as ISO-8601 strings instead of timestamps
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        app = Javalin.create(config -> {
            // Configure JSON mapper
            config.jsonMapper(new JavalinJackson(objectMapper, true));

            // Enable CORS if configured
            if (serverConfig.isEnableCors()) {
                config.bundledPlugins.enableCors(cors -> {
                    cors.addRule(it -> {
                        it.anyHost();
                        it.allowCredentials = false;
                    });
                });
            }

            // Enable request logging if configured
            if (serverConfig.isEnableDevLogging()) {
                config.bundledPlugins.enableDevLogging();
            }

            // Set server configuration
            config.jetty.defaultHost = serverConfig.getHost();
            config.jetty.defaultPort = serverConfig.getPort();
        });
        
        logger.info("Javalin application created");
    }

    /**
     * Configure Swagger/OpenAPI documentation
     * Override in subclasses if needed
     */
    protected void configureSwagger() {
        // Default implementation - can be overridden
        logger.debug("Configuring Swagger documentation");
        
        // Serve Swagger UI
        app.get("/swagger", ctx -> {
            ctx.redirect("/webjars/swagger-ui/index.html");
        });
        
        // Serve static Swagger UI files
        app.get("/webjars/*", ctx -> {
            ctx.result(getClass().getClassLoader().getResourceAsStream("META-INF/resources" + ctx.path()));
        });
    }

    /**
     * Configure exception handling
     */
    protected void configureExceptionHandling() {
        logger.info("Configuring exception handling");
        BaseGlobalExceptionHandler.configure(app);
    }

    /**
     * Start the Javalin server
     */
    protected void startServer(ServerConfig serverConfig) {
        logger.info("Starting server on {}:{}", serverConfig.getHost(), serverConfig.getPort());
        app.start(serverConfig.getHost(), serverConfig.getPort());
    }

    /**
     * Display available endpoints
     */
    protected void displayAvailableEndpoints(ServerConfig serverConfig) {
        String baseUrl = String.format("http://%s:%d", serverConfig.getHost(), serverConfig.getPort());
        
        logger.info("=== {} Endpoints ===", getApplicationName());
        logger.info("Base URL: {}", baseUrl);
        logger.info("Swagger UI: {}/swagger", baseUrl);
        
        // Subclasses can override to display specific endpoints
        displayApplicationSpecificEndpoints(baseUrl);
    }

    /**
     * Display application-specific endpoints
     * Override in subclasses to show specific endpoints
     */
    protected void displayApplicationSpecificEndpoints(String baseUrl) {
        // Default implementation - can be overridden
    }

    /**
     * Perform any pre-startup initialization
     * Override in subclasses if needed
     */
    protected void performPreStartupInitialization() {
        // Default implementation - can be overridden
    }

    /**
     * Perform any cleanup on shutdown
     * Override in subclasses if needed
     */
    protected void performCleanup() {
        // Default implementation - can be overridden
    }

    /**
     * Get the Javalin app instance
     */
    public Javalin getApp() {
        return app;
    }

    /**
     * Get the Guice injector
     */
    public Injector getInjector() {
        return injector;
    }

    /**
     * Get the server port
     */
    public int getPort() {
        if (app != null) {
            try {
                // Try to get the actual port from the running server
                return app.port();
            } catch (Exception e) {
                // If app is not started yet, return the configured port
                ServerConfig serverConfig = getServerConfig();
                return serverConfig != null ? serverConfig.getPort() : 8081;
            }
        }
        // If no app, return default port
        return 8081;
    }
}

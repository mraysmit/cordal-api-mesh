package dev.mars.generic;

import com.google.inject.Module;
import dev.mars.common.application.BaseJavalinApplication;
import dev.mars.common.config.ServerConfig;
import dev.mars.config.GenericApiConfig;
import dev.mars.config.GenericApiGuiceModule;
import dev.mars.config.SwaggerConfig;

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
                // Initialize stock trades data for testing using SQL directly
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
     * Initialize stock trades data for testing
     */
    private void initializeStockTradesForTesting() throws Exception {
        logger.info("Initializing stock trades table and sample data for testing");

        // Get database connection manager
        dev.mars.generic.database.DatabaseConnectionManager dbConnectionManager =
            injector.getInstance(dev.mars.generic.database.DatabaseConnectionManager.class);

        // Get connection to stocktrades database
        try (java.sql.Connection connection = dbConnectionManager.getConnection("stocktrades");
             java.sql.Statement statement = connection.createStatement()) {

            // Create stock_trades table if it doesn't exist
            String createTableSql = """
                CREATE TABLE IF NOT EXISTS stock_trades (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    symbol VARCHAR(10) NOT NULL,
                    trade_type VARCHAR(4) NOT NULL CHECK (trade_type IN ('BUY', 'SELL')),
                    quantity INTEGER NOT NULL CHECK (quantity > 0),
                    price DECIMAL(10,2) NOT NULL CHECK (price > 0),
                    total_value DECIMAL(15,2) NOT NULL,
                    trade_date_time TIMESTAMP NOT NULL,
                    trader_id VARCHAR(50) NOT NULL,
                    exchange VARCHAR(20) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """;

            statement.execute(createTableSql);
            logger.info("Stock trades table created/verified");

            // Check if data already exists
            try (java.sql.ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM stock_trades")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    logger.info("Stock trades data already exists, skipping sample data loading");
                    return;
                }
            }

            // Insert sample data
            String insertSql = """
                INSERT INTO stock_trades (symbol, trade_type, quantity, price, total_value,
                                        trade_date_time, trader_id, exchange)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

            try (java.sql.PreparedStatement preparedStatement = connection.prepareStatement(insertSql)) {
                // Sample data
                Object[][] sampleData = {
                    {"AAPL", "BUY", 100, 150.50, 15050.00, "2024-01-15 10:30:00", "trader001", "NASDAQ"},
                    {"GOOGL", "SELL", 50, 2800.75, 140037.50, "2024-01-15 11:15:00", "trader002", "NASDAQ"},
                    {"MSFT", "BUY", 200, 380.25, 76050.00, "2024-01-15 14:20:00", "trader001", "NASDAQ"},
                    {"TSLA", "BUY", 75, 220.80, 16560.00, "2024-01-16 09:45:00", "trader003", "NASDAQ"},
                    {"AMZN", "SELL", 25, 3200.00, 80000.00, "2024-01-16 13:30:00", "trader002", "NASDAQ"}
                };

                for (Object[] row : sampleData) {
                    preparedStatement.setString(1, (String) row[0]);
                    preparedStatement.setString(2, (String) row[1]);
                    preparedStatement.setInt(3, (Integer) row[2]);
                    preparedStatement.setBigDecimal(4, new java.math.BigDecimal(row[3].toString()));
                    preparedStatement.setBigDecimal(5, new java.math.BigDecimal(row[4].toString()));
                    preparedStatement.setTimestamp(6, java.sql.Timestamp.valueOf((String) row[5]));
                    preparedStatement.setString(7, (String) row[6]);
                    preparedStatement.setString(8, (String) row[7]);
                    preparedStatement.executeUpdate();
                }

                logger.info("Loaded {} sample stock trades", sampleData.length);
            }
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

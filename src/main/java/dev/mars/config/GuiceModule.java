package dev.mars.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import dev.mars.controller.PerformanceMetricsController;
import dev.mars.controller.StockTradeController;
import dev.mars.database.DataLoader;
import dev.mars.database.DatabaseManager;
import dev.mars.database.MetricsDatabaseManager;
import dev.mars.generic.GenericApiController;
import dev.mars.generic.GenericApiService;
import dev.mars.generic.GenericRepository;
import dev.mars.generic.config.ConfigurationLoader;
import dev.mars.generic.config.EndpointConfigurationManager;
import dev.mars.metrics.MetricsCollectionHandler;
import dev.mars.repository.PerformanceMetricsRepository;
import dev.mars.repository.StockTradeRepository;
import dev.mars.routes.ApiRoutes;
import dev.mars.service.PerformanceMetricsService;
import dev.mars.service.StockTradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guice dependency injection module
 */
public class GuiceModule extends AbstractModule {
    private static final Logger logger = LoggerFactory.getLogger(GuiceModule.class);
    
    @Override
    protected void configure() {
        logger.info("Configuring Guice dependency injection");

        // Note: Using @Provides methods instead of bind() to ensure proper injection

        logger.info("Guice module configured successfully");
    }
    
    @Provides
    @Singleton
    public AppConfig provideAppConfig() {
        logger.info("Creating AppConfig instance");
        return new AppConfig();
    }
    
    @Provides
    @Singleton
    public DatabaseConfig provideDatabaseConfig(AppConfig appConfig) {
        logger.info("Creating DatabaseConfig instance");
        return new DatabaseConfig(appConfig);
    }
    
    @Provides
    @Singleton
    public DatabaseManager provideDatabaseManager(DatabaseConfig databaseConfig) {
        logger.info("Creating DatabaseManager instance");
        DatabaseManager databaseManager = new DatabaseManager(databaseConfig);
        
        // Initialize database schema
        databaseManager.initializeSchema();
        
        return databaseManager;
    }
    
    @Provides
    @Singleton
    public DataLoader provideDataLoader(DatabaseManager databaseManager, AppConfig appConfig) {
        logger.info("Creating DataLoader instance");
        DataLoader dataLoader = new DataLoader(databaseManager, appConfig);
        
        // Load sample data if configured
        dataLoader.loadSampleDataIfNeeded();
        
        return dataLoader;
    }

    @Provides
    @Singleton
    public MetricsDatabaseConfig provideMetricsDatabaseConfig(AppConfig appConfig) {
        logger.info("Creating MetricsDatabaseConfig instance");
        return new MetricsDatabaseConfig(appConfig);
    }

    @Provides
    @Singleton
    public MetricsDatabaseManager provideMetricsDatabaseManager(MetricsDatabaseConfig metricsDatabaseConfig) {
        logger.info("Creating MetricsDatabaseManager instance");
        return new MetricsDatabaseManager(metricsDatabaseConfig);
    }

    @Provides
    @Singleton
    public StockTradeRepository provideStockTradeRepository(DatabaseManager databaseManager) {
        logger.info("Creating StockTradeRepository instance");
        return new StockTradeRepository(databaseManager);
    }

    @Provides
    @Singleton
    public StockTradeService provideStockTradeService(StockTradeRepository repository) {
        logger.info("Creating StockTradeService instance");
        return new StockTradeService(repository);
    }

    @Provides
    @Singleton
    public StockTradeController provideStockTradeController(StockTradeService service) {
        logger.info("Creating StockTradeController instance");
        return new StockTradeController(service);
    }

    @Provides
    @Singleton
    public PerformanceMetricsRepository providePerformanceMetricsRepository(MetricsDatabaseManager metricsDatabaseManager) {
        logger.info("Creating PerformanceMetricsRepository instance");
        return new PerformanceMetricsRepository(metricsDatabaseManager);
    }

    @Provides
    @Singleton
    public PerformanceMetricsService providePerformanceMetricsService(PerformanceMetricsRepository repository) {
        logger.info("Creating PerformanceMetricsService instance");
        return new PerformanceMetricsService(repository);
    }

    @Provides
    @Singleton
    public PerformanceMetricsController providePerformanceMetricsController(PerformanceMetricsService service) {
        logger.info("Creating PerformanceMetricsController instance");
        return new PerformanceMetricsController(service);
    }

    @Provides
    @Singleton
    public MetricsCollectionHandler provideMetricsCollectionHandler(PerformanceMetricsService service, AppConfig appConfig) {
        logger.info("Creating MetricsCollectionHandler instance");
        return new MetricsCollectionHandler(service, appConfig);
    }

    @Provides
    @Singleton
    public SwaggerConfig provideSwaggerConfig(AppConfig appConfig) {
        logger.info("Creating SwaggerConfig instance");
        return new SwaggerConfig(appConfig);
    }

    @Provides
    @Singleton
    public ConfigurationLoader provideConfigurationLoader() {
        logger.info("Creating ConfigurationLoader instance");
        return new ConfigurationLoader();
    }

    @Provides
    @Singleton
    public EndpointConfigurationManager provideEndpointConfigurationManager(ConfigurationLoader configurationLoader) {
        logger.info("Creating EndpointConfigurationManager instance");
        EndpointConfigurationManager manager = new EndpointConfigurationManager(configurationLoader);

        // Validate configurations on startup
        manager.validateConfigurations();

        return manager;
    }

    @Provides
    @Singleton
    public GenericRepository provideGenericRepository(DatabaseManager databaseManager) {
        logger.info("Creating GenericRepository instance");
        return new GenericRepository(databaseManager);
    }

    @Provides
    @Singleton
    public GenericApiService provideGenericApiService(GenericRepository genericRepository,
                                                     EndpointConfigurationManager configurationManager) {
        logger.info("Creating GenericApiService instance");
        return new GenericApiService(genericRepository, configurationManager);
    }

    @Provides
    @Singleton
    public GenericApiController provideGenericApiController(GenericApiService genericApiService) {
        logger.info("Creating GenericApiController instance");
        return new GenericApiController(genericApiService);
    }

    @Provides
    @Singleton
    public ApiRoutes provideApiRoutes(StockTradeController stockTradeController,
                                     PerformanceMetricsController performanceMetricsController,
                                     MetricsCollectionHandler metricsCollectionHandler,
                                     GenericApiController genericApiController,
                                     AppConfig appConfig) {
        logger.info("Creating ApiRoutes instance");
        return new ApiRoutes(stockTradeController, performanceMetricsController, metricsCollectionHandler, genericApiController, appConfig);
    }
}

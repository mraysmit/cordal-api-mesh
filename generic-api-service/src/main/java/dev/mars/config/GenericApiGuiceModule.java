package dev.mars.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import dev.mars.database.DataLoader;
import dev.mars.database.DatabaseManager;
import dev.mars.generic.GenericApiController;
import dev.mars.generic.GenericApiService;
import dev.mars.generic.GenericRepository;
import dev.mars.generic.config.ConfigurationLoader;
import dev.mars.generic.config.EndpointConfigurationManager;
import dev.mars.generic.database.DatabaseConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guice dependency injection module for Generic API Service
 */
public class GenericApiGuiceModule extends AbstractModule {
    private static final Logger logger = LoggerFactory.getLogger(GenericApiGuiceModule.class);
    
    @Override
    protected void configure() {
        logger.info("Configuring Generic API Guice dependency injection");
        logger.info("Generic API Guice module configured successfully");
    }
    
    @Provides
    @Singleton
    public GenericApiConfig provideGenericApiConfig() {
        logger.info("Creating GenericApiConfig instance");
        return GenericApiConfig.loadFromFile();
    }
    
    @Provides
    @Singleton
    public DatabaseConfig provideDatabaseConfig(GenericApiConfig genericApiConfig) {
        logger.info("Creating DatabaseConfig instance");
        return new DatabaseConfig(genericApiConfig);
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
    public DataLoader provideDataLoader(DatabaseManager databaseManager, GenericApiConfig genericApiConfig) {
        logger.info("Creating DataLoader instance");
        DataLoader dataLoader = new DataLoader(databaseManager, genericApiConfig);
        
        // Load sample data if configured
        dataLoader.loadSampleDataIfNeeded();
        
        return dataLoader;
    }

    @Provides
    @Singleton
    public SwaggerConfig provideSwaggerConfig(GenericApiConfig genericApiConfig) {
        logger.info("Creating SwaggerConfig instance");
        return new SwaggerConfig(genericApiConfig);
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
    public DatabaseConnectionManager provideDatabaseConnectionManager(EndpointConfigurationManager configurationManager) {
        logger.info("Creating DatabaseConnectionManager instance");
        return new DatabaseConnectionManager(configurationManager);
    }

    @Provides
    @Singleton
    public GenericRepository provideGenericRepository(DatabaseConnectionManager databaseConnectionManager) {
        logger.info("Creating GenericRepository instance");
        return new GenericRepository(databaseConnectionManager);
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
}

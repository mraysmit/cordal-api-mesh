package dev.cordal.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import dev.cordal.cache.CacheInvalidationService;
import dev.cordal.cache.CacheManagementController;
import dev.cordal.common.cache.CacheEventPublisher;
import dev.cordal.common.cache.CacheInvalidationEngine;
import dev.cordal.common.cache.CacheManager;
import dev.cordal.common.cache.CacheProviderFactory;
import dev.cordal.common.cache.FailFastCacheProvider;
import dev.cordal.common.cache.InMemoryCacheProvider;
import dev.cordal.common.cache.NoOpCacheProvider;
import dev.cordal.common.metrics.CacheMetricsCollector;
import dev.cordal.generic.GenericApiController;
import dev.cordal.generic.GenericApiService;
import dev.cordal.generic.GenericRepository;
import dev.cordal.generic.config.ConfigurationLoader;
import dev.cordal.generic.config.ConfigurationLoaderFactory;
import dev.cordal.generic.config.EndpointConfigurationManager;
import dev.cordal.generic.database.DatabaseConnectionManager;
import dev.cordal.database.loader.DatabaseConfigurationLoader;
import dev.cordal.database.repository.DatabaseConfigurationRepository;
import dev.cordal.database.repository.QueryConfigurationRepository;
import dev.cordal.database.repository.EndpointConfigurationRepository;
import dev.cordal.generic.management.ConfigurationMetadataService;
import dev.cordal.generic.management.ConfigurationManagementService;
import dev.cordal.generic.management.ConfigurationManagementController;
import dev.cordal.generic.management.UsageStatisticsService;
import dev.cordal.generic.management.HealthMonitoringService;
import dev.cordal.generic.management.ManagementController;
import dev.cordal.generic.migration.ConfigurationMigrationService;
import dev.cordal.generic.migration.ConfigurationMigrationController;
import dev.cordal.database.DatabaseManager;
import dev.cordal.database.ConfigurationDataLoader;
import dev.cordal.api.H2ServerController;
import dev.cordal.hotreload.FileWatcherService;
import dev.cordal.hotreload.ConfigurationStateManager;
import dev.cordal.hotreload.ConfigurationReloadManager;
import dev.cordal.hotreload.ValidationPipeline;
import dev.cordal.hotreload.DynamicEndpointRegistry;
import dev.cordal.hotreload.AtomicUpdateManager;
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

        // Default cache provider factory — bind a different CacheProviderFactory to swap implementations.
        bind(CacheProviderFactory.class).toInstance(
            (name, maxSize, defaultTtl) -> new InMemoryCacheProvider(maxSize, defaultTtl)
        );

        logger.info("Generic API Guice module configured successfully");
    }
    
    @Provides
    @Singleton
    GenericApiConfig provideGenericApiConfig() {
        logger.info("Creating GenericApiConfig instance");
        return GenericApiConfig.loadFromFile();
    }
    



    @Provides
    @Singleton
    SwaggerConfig provideSwaggerConfig(GenericApiConfig genericApiConfig) {
        logger.info("Creating SwaggerConfig instance");
        return new SwaggerConfig(genericApiConfig);
    }

    @Provides
    @Singleton
    ConfigurationLoader provideConfigurationLoader(GenericApiConfig genericApiConfig) {
        logger.info("Creating ConfigurationLoader instance");
        return new ConfigurationLoader(genericApiConfig);
    }

    @Provides
    @Singleton
    DatabaseConfigurationRepository provideDatabaseConfigurationRepository(DatabaseManager databaseManager) {
        logger.info("Creating DatabaseConfigurationRepository instance");
        return new DatabaseConfigurationRepository(databaseManager);
    }

    @Provides
    @Singleton
    QueryConfigurationRepository provideQueryConfigurationRepository(DatabaseManager databaseManager) {
        logger.info("Creating QueryConfigurationRepository instance");
        return new QueryConfigurationRepository(databaseManager);
    }

    @Provides
    @Singleton
    EndpointConfigurationRepository provideEndpointConfigurationRepository(DatabaseManager databaseManager) {
        logger.info("Creating EndpointConfigurationRepository instance");
        return new EndpointConfigurationRepository(databaseManager);
    }

    @Provides
    @Singleton
    DatabaseConfigurationLoader provideDatabaseConfigurationLoader(DatabaseConfigurationRepository databaseRepository,
                                                                   QueryConfigurationRepository queryRepository,
                                                                   EndpointConfigurationRepository endpointRepository) {
        logger.info("Creating DatabaseConfigurationLoader instance");
        return new DatabaseConfigurationLoader(databaseRepository, queryRepository, endpointRepository);
    }

    @Provides
    @Singleton
    ConfigurationLoaderFactory provideConfigurationLoaderFactory(GenericApiConfig genericApiConfig,
                                                                 ConfigurationLoader yamlConfigurationLoader,
                                                                 DatabaseConfigurationLoader databaseConfigurationLoader) {
        logger.info("Creating ConfigurationLoaderFactory instance");
        return new ConfigurationLoaderFactory(genericApiConfig, yamlConfigurationLoader, databaseConfigurationLoader);
    }

    @Provides
    @Singleton
    EndpointConfigurationManager provideEndpointConfigurationManager(ConfigurationLoaderFactory configurationLoaderFactory,
                                                                    GenericApiConfig genericApiConfig) {
        logger.info("Creating EndpointConfigurationManager instance");
        EndpointConfigurationManager manager = new EndpointConfigurationManager(configurationLoaderFactory);

        // Only validate configurations on startup if configured to do so
        if (genericApiConfig.isValidationRunOnStartup()) {
            logger.info("Running configuration validation on startup (validation.runOnStartup=true)");
            manager.validateConfigurations();
        } else {
            logger.info("Skipping configuration validation on startup (validation.runOnStartup=false)");
        }

        return manager;
    }

    @Provides
    @Singleton
    DatabaseConnectionManager provideDatabaseConnectionManager(EndpointConfigurationManager configurationManager) {
        logger.info("Creating DatabaseConnectionManager instance");
        return new DatabaseConnectionManager(configurationManager);
    }

    @Provides
    @Singleton
    CacheManager provideCacheManager(GenericApiConfig genericApiConfig, CacheProviderFactory cacheProviderFactory) {
        logger.info("Creating CacheManager instance using {}", cacheProviderFactory.getClass().getSimpleName());
        GenericApiConfig.CacheSettings cacheSettings = genericApiConfig.getCacheSettings();

        if (!cacheSettings.isEnabled()) {
            logger.info("Cache is disabled, creating CacheManager with minimal configuration");
        }

        CacheManager.CacheConfiguration config = new CacheManager.CacheConfiguration(
            cacheSettings.getMaxSize(),
            cacheSettings.getDefaultTtlSeconds(),
            cacheSettings.getCleanupIntervalSeconds()
        );

        String provider = cacheSettings.getProvider();
        CacheProviderFactory selectedProviderFactory;
        if ("noop".equals(provider)) {
            logger.info("Cache provider selected: noop");
            selectedProviderFactory = (name, maxSize, defaultTtl) -> new NoOpCacheProvider();
        } else if ("failfast".equals(provider)) {
            logger.info("Cache provider selected: failfast");
            selectedProviderFactory = (name, maxSize, defaultTtl) -> new FailFastCacheProvider();
        } else if ("inmemory".equals(provider)) {
            logger.info("Cache provider selected: inmemory");
            selectedProviderFactory = cacheProviderFactory;
        } else {
            logger.warn("Unknown cache provider '{}'. Falling back to inmemory", provider);
            selectedProviderFactory = cacheProviderFactory;
        }

        return new CacheManager(config, selectedProviderFactory);
    }

    @Provides
    @Singleton
    CacheMetricsCollector provideCacheMetricsCollector(CacheManager cacheManager) {
        logger.info("Creating CacheMetricsCollector instance");
        return new CacheMetricsCollector(cacheManager);
    }

    @Provides
    @Singleton
    dev.cordal.generic.cache.QueryResultCache provideQueryResultCache(CacheManager cacheManager) {
        logger.info("Creating QueryResultCache instance");
        return new dev.cordal.generic.cache.QueryResultCache(cacheManager);
    }

    @Provides
    @Singleton
    GenericRepository provideGenericRepository(DatabaseConnectionManager databaseConnectionManager,
                                               CacheManager cacheManager,
                                               CacheMetricsCollector cacheMetricsCollector,
                                               dev.cordal.generic.cache.QueryResultCache queryResultCache) {
        logger.info("Creating GenericRepository instance");
        return new GenericRepository(databaseConnectionManager, cacheManager, cacheMetricsCollector, queryResultCache);
    }

    @Provides
    @Singleton
    CacheEventPublisher provideCacheEventPublisher() {
        logger.info("Creating CacheEventPublisher instance");
        return new CacheEventPublisher();
    }

    @Provides
    @Singleton
    CacheInvalidationEngine provideCacheInvalidationEngine(CacheManager cacheManager,
                                                           CacheEventPublisher eventPublisher) {
        logger.info("Creating CacheInvalidationEngine instance");
        return new CacheInvalidationEngine(cacheManager, eventPublisher);
    }

    @Provides
    @Singleton
    CacheInvalidationService provideCacheInvalidationService(CacheEventPublisher eventPublisher,
                                                             CacheInvalidationEngine invalidationEngine) {
        logger.info("Creating CacheInvalidationService instance");
        return new CacheInvalidationService(eventPublisher, invalidationEngine);
    }

    @Provides
    @Singleton
    CacheManagementController provideCacheManagementController(CacheManager cacheManager,
                                                               CacheMetricsCollector cacheMetricsCollector) {
        logger.info("Creating CacheManagementController instance");
        return new CacheManagementController(cacheManager, cacheMetricsCollector);
    }

    @Provides
    @Singleton
    GenericApiService provideGenericApiService(GenericRepository genericRepository,
                                               EndpointConfigurationManager configurationManager,
                                               DatabaseConnectionManager databaseConnectionManager) {
        logger.info("Creating GenericApiService instance");
        return new GenericApiService(genericRepository, configurationManager, databaseConnectionManager);
    }

    @Provides
    @Singleton
    GenericApiController provideGenericApiController(GenericApiService genericApiService,
                                                     UsageStatisticsService statisticsService) {
        logger.info("Creating GenericApiController instance");
        return new GenericApiController(genericApiService, statisticsService);
    }

    @Provides
    @Singleton
    ConfigurationMetadataService provideConfigurationMetadataService(GenericApiConfig genericApiConfig) {
        logger.info("Creating ConfigurationMetadataService instance");
        return new ConfigurationMetadataService(genericApiConfig);
    }

    @Provides
    @Singleton
    UsageStatisticsService provideUsageStatisticsService() {
        logger.info("Creating UsageStatisticsService instance");
        return new UsageStatisticsService();
    }

    @Provides
    @Singleton
    HealthMonitoringService provideHealthMonitoringService(DatabaseConnectionManager databaseConnectionManager,
                                                           EndpointConfigurationManager configurationManager) {
        logger.info("Creating HealthMonitoringService instance");
        return new HealthMonitoringService(databaseConnectionManager, configurationManager);
    }

    @Provides
    @Singleton
    ConfigurationManagementService provideConfigurationManagementService(DatabaseConfigurationRepository databaseRepository,
                                                                        QueryConfigurationRepository queryRepository,
                                                                        EndpointConfigurationRepository endpointRepository,
                                                                        ConfigurationLoaderFactory configurationLoaderFactory,
                                                                        EndpointConfigurationManager configurationManager) {
        logger.info("Creating ConfigurationManagementService instance");
        return new ConfigurationManagementService(databaseRepository, queryRepository, endpointRepository,
                                                 configurationLoaderFactory, configurationManager);
    }

    @Provides
    @Singleton
    ConfigurationManagementController provideConfigurationManagementController(ConfigurationManagementService configurationManagementService) {
        logger.info("Creating ConfigurationManagementController instance");
        return new ConfigurationManagementController(configurationManagementService);
    }

    @Provides
    @Singleton
    ConfigurationMigrationService provideConfigurationMigrationService(DatabaseConfigurationRepository databaseRepository,
                                                                      QueryConfigurationRepository queryRepository,
                                                                      EndpointConfigurationRepository endpointRepository,
                                                                      ConfigurationLoader yamlLoader,
                                                                      DatabaseConfigurationLoader databaseLoader,
                                                                      ConfigurationLoaderFactory configurationLoaderFactory) {
        logger.info("Creating ConfigurationMigrationService instance");
        return new ConfigurationMigrationService(databaseRepository, queryRepository, endpointRepository,
                                                yamlLoader, databaseLoader, configurationLoaderFactory);
    }

    @Provides
    @Singleton
    ConfigurationMigrationController provideConfigurationMigrationController(ConfigurationMigrationService configurationMigrationService) {
        logger.info("Creating ConfigurationMigrationController instance");
        return new ConfigurationMigrationController(configurationMigrationService);
    }

    @Provides
    @Singleton
    ManagementController provideManagementController(ConfigurationMetadataService metadataService,
                                                     UsageStatisticsService statisticsService,
                                                     HealthMonitoringService healthService,
                                                     EndpointConfigurationManager configurationManager) {
        logger.info("Creating ManagementController instance");
        return new ManagementController(metadataService, statisticsService, healthService, configurationManager);
    }

    @Provides
    @Singleton
    DatabaseManager provideDatabaseManager(GenericApiConfig genericApiConfig) {
        logger.info("Creating DatabaseManager instance");
        DatabaseManager databaseManager = new DatabaseManager(genericApiConfig);

        // Initialize schema on startup
        logger.info("Initializing database schema");
        databaseManager.initializeSchema();

        return databaseManager;
    }

    @Provides
    @Singleton
    ConfigurationDataLoader provideConfigurationDataLoader(DatabaseManager databaseManager,
                                                           GenericApiConfig genericApiConfig,
                                                           dev.cordal.generic.config.ConfigurationLoader configurationLoader) {
        logger.info("Creating ConfigurationDataLoader instance");
        ConfigurationDataLoader dataLoader = new ConfigurationDataLoader(databaseManager, genericApiConfig, configurationLoader);

        // Load configuration data from YAML if needed
        logger.info("Loading configuration data from YAML if needed");
        dataLoader.loadConfigurationDataIfNeeded();

        return dataLoader;
    }

    @Provides
    @Singleton
    H2ServerConfig provideH2ServerConfig(GenericApiConfig genericApiConfig) {
        logger.info("Creating H2ServerConfig instance");
        return new H2ServerConfig(genericApiConfig);
    }

    @Provides
    @Singleton
    H2ServerController provideH2ServerController(H2ServerConfig h2ServerConfig) {
        logger.info("Creating H2ServerController instance");
        return new H2ServerController(h2ServerConfig);
    }

    @Provides
    @Singleton
    FileWatcherService provideFileWatcherService() {
        logger.info("Creating FileWatcherService instance");
        return new FileWatcherService();
    }

    @Provides
    @Singleton
    ConfigurationStateManager provideConfigurationStateManager() {
        logger.info("Creating ConfigurationStateManager instance");
        return new ConfigurationStateManager();
    }

    @Provides
    @Singleton
    ValidationPipeline provideValidationPipeline(
                                                 ConfigurationStateManager stateManager) {
        logger.info("Creating ValidationPipeline instance");
        return new ValidationPipeline(stateManager);
    }

    @Provides
    @Singleton
    DynamicEndpointRegistry provideDynamicEndpointRegistry() {
        logger.info("Creating DynamicEndpointRegistry instance");
        return new DynamicEndpointRegistry();
    }

    @Provides
    @Singleton
    AtomicUpdateManager provideAtomicUpdateManager(
                                                   DynamicEndpointRegistry endpointRegistry) {
        logger.info("Creating AtomicUpdateManager instance");
        return new AtomicUpdateManager(endpointRegistry);
    }

    @Provides
    @Singleton
    ConfigurationReloadManager provideConfigurationReloadManager(
            FileWatcherService fileWatcher,
            ConfigurationStateManager stateManager,
            ValidationPipeline validationPipeline,
            AtomicUpdateManager atomicUpdateManager,
            GenericApiConfig config) {
        logger.info("Creating ConfigurationReloadManager instance");
        return new ConfigurationReloadManager(fileWatcher, stateManager, validationPipeline,
                                            atomicUpdateManager, config);
    }
}

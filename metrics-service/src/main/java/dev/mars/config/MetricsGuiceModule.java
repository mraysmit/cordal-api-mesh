package dev.mars.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import dev.mars.common.config.DatabaseConfig;
import dev.mars.common.database.MetricsDatabaseManager;
import dev.mars.controller.PerformanceMetricsController;
import dev.mars.metrics.MetricsCollectionHandler;
import dev.mars.repository.PerformanceMetricsRepository;
import dev.mars.service.PerformanceMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guice dependency injection module for Metrics Service
 */
public class MetricsGuiceModule extends AbstractModule {
    private static final Logger logger = LoggerFactory.getLogger(MetricsGuiceModule.class);
    
    @Override
    protected void configure() {
        logger.info("Configuring Metrics Guice dependency injection");
        logger.info("Metrics Guice module configured successfully");
    }
    
    @Provides
    @Singleton
    public MetricsConfig provideMetricsConfig() {
        logger.info("Creating MetricsConfig instance");
        return MetricsConfig.loadFromFile();
    }
    
    @Provides
    @Singleton
    public DatabaseConfig provideMetricsDatabaseConfig(MetricsConfig metricsConfig) {
        logger.info("Creating DatabaseConfig instance for metrics database");
        DatabaseConfig config = new DatabaseConfig();
        config.setName("metrics-db");
        config.setDescription("Metrics database for performance monitoring");
        config.setUrl(metricsConfig.getMetricsDatabase().getUrl());
        config.setUsername(metricsConfig.getMetricsDatabase().getUsername());
        config.setPassword(metricsConfig.getMetricsDatabase().getPassword());
        config.setDriver(metricsConfig.getMetricsDatabase().getDriver());
        return config;
    }

    @Provides
    @Singleton
    public MetricsDatabaseManager provideMetricsDatabaseManager(DatabaseConfig databaseConfig) {
        logger.info("Creating MetricsDatabaseManager instance");
        return new MetricsDatabaseManager(databaseConfig);
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
    public MetricsCollectionHandler provideMetricsCollectionHandler(PerformanceMetricsService service, MetricsConfig metricsConfig) {
        logger.info("Creating MetricsCollectionHandler instance");
        return new MetricsCollectionHandler(service, metricsConfig);
    }
}

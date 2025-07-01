package dev.mars.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import dev.mars.controller.PerformanceMetricsController;
import dev.mars.database.MetricsDatabaseManager;
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
    public MetricsDatabaseConfig provideMetricsDatabaseConfig(MetricsConfig metricsConfig) {
        logger.info("Creating MetricsDatabaseConfig instance");
        return new MetricsDatabaseConfig(metricsConfig);
    }

    @Provides
    @Singleton
    public MetricsDatabaseManager provideMetricsDatabaseManager(MetricsDatabaseConfig metricsDatabaseConfig) {
        logger.info("Creating MetricsDatabaseManager instance");
        return new MetricsDatabaseManager(metricsDatabaseConfig);
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

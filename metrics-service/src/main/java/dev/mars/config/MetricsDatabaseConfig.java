package dev.mars.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;

/**
 * Configuration for metrics database connection
 */
@Singleton
public class MetricsDatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(MetricsDatabaseConfig.class);
    
    private final HikariDataSource dataSource;
    
    @Inject
    public MetricsDatabaseConfig(MetricsConfig metricsConfig) {
        logger.info("Initializing metrics database configuration");

        HikariConfig config = new HikariConfig();

        // Database connection settings
        config.setJdbcUrl(metricsConfig.getMetricsDatabase().getUrl());
        config.setUsername(metricsConfig.getMetricsDatabase().getUsername());
        config.setPassword(metricsConfig.getMetricsDatabase().getPassword());
        config.setDriverClassName(metricsConfig.getMetricsDatabase().getDriver());

        // Connection pool settings - using defaults since MetricsConfig doesn't have pool settings
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        // Pool name for identification
        config.setPoolName("MetricsPool");
        
        // Additional settings for better performance
        config.setLeakDetectionThreshold(60000); // 60 seconds
        config.setConnectionTestQuery("SELECT 1");
        
        this.dataSource = new HikariDataSource(config);
        
        logger.info("Metrics database connection pool initialized successfully");
        logger.info("Metrics database URL: {}", metricsConfig.getMetricsDatabase().getUrl());
        logger.info("Maximum pool size: 10"); // Using default value
    }
    
    /**
     * Get the metrics database data source
     */
    public DataSource getDataSource() {
        return dataSource;
    }
    
    /**
     * Close the connection pool
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Closing metrics database connection pool");
            dataSource.close();
        }
    }
}

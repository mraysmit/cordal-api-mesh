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
    public MetricsDatabaseConfig(AppConfig appConfig) {
        logger.info("Initializing metrics database configuration");
        
        HikariConfig config = new HikariConfig();
        
        // Database connection settings
        config.setJdbcUrl(appConfig.getMetricsDatabase().getUrl());
        config.setUsername(appConfig.getMetricsDatabase().getUsername());
        config.setPassword(appConfig.getMetricsDatabase().getPassword());
        config.setDriverClassName(appConfig.getMetricsDatabase().getDriver());
        
        // Connection pool settings
        config.setMaximumPoolSize(appConfig.getMetricsDatabase().getPool().getMaximumPoolSize());
        config.setMinimumIdle(appConfig.getMetricsDatabase().getPool().getMinimumIdle());
        config.setConnectionTimeout(appConfig.getMetricsDatabase().getPool().getConnectionTimeout());
        config.setIdleTimeout(appConfig.getMetricsDatabase().getPool().getIdleTimeout());
        config.setMaxLifetime(appConfig.getMetricsDatabase().getPool().getMaxLifetime());
        
        // Pool name for identification
        config.setPoolName("MetricsPool");
        
        // Additional settings for better performance
        config.setLeakDetectionThreshold(60000); // 60 seconds
        config.setConnectionTestQuery("SELECT 1");
        
        this.dataSource = new HikariDataSource(config);
        
        logger.info("Metrics database connection pool initialized successfully");
        logger.info("Metrics database URL: {}", appConfig.getMetricsDatabase().getUrl());
        logger.info("Maximum pool size: {}", appConfig.getMetricsDatabase().getPool().getMaximumPoolSize());
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

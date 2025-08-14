package dev.cordal.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * Database configuration class for setting up connection pool
 */
public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    
    private final GenericApiConfig genericApiConfig;
    private HikariDataSource dataSource;

    public DatabaseConfig(GenericApiConfig genericApiConfig) {
        this.genericApiConfig = genericApiConfig;
        initializeDataSource();
    }
    
    private void initializeDataSource() {
        try {
            HikariConfig config = new HikariConfig();
            
            // Basic database configuration
            config.setJdbcUrl(genericApiConfig.getDatabaseUrl());
            config.setUsername(genericApiConfig.getDatabaseUsername());
            config.setPassword(genericApiConfig.getDatabasePassword());
            config.setDriverClassName(genericApiConfig.getDatabaseDriver());

            // Connection pool configuration - using defaults since GenericApiConfig doesn't have these
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            
            // Additional HikariCP settings
            config.setPoolName("ApiServiceConfigPool");
            config.setLeakDetectionThreshold(60000);
            config.setConnectionTestQuery("SELECT 1");
            
            // H2 specific settings
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            
            this.dataSource = new HikariDataSource(config);
            
            logger.info("Database connection pool initialized successfully");
            logger.info("Database URL: {}", genericApiConfig.getDatabaseUrl());
            logger.info("Maximum pool size: 10"); // Using default value
            
        } catch (Exception e) {
            logger.error("Failed to initialize database connection pool", e);
            throw new RuntimeException("Failed to initialize database connection pool", e);
        }
    }
    
    public DataSource getDataSource() {
        return dataSource;
    }
    
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Closing database connection pool");
            dataSource.close();
        }
    }
}

package dev.mars.config;

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
    
    private final AppConfig appConfig;
    private HikariDataSource dataSource;
    
    public DatabaseConfig(AppConfig appConfig) {
        this.appConfig = appConfig;
        initializeDataSource();
    }
    
    private void initializeDataSource() {
        try {
            HikariConfig config = new HikariConfig();
            
            // Basic database configuration
            config.setJdbcUrl(appConfig.getDatabaseUrl());
            config.setUsername(appConfig.getDatabaseUsername());
            config.setPassword(appConfig.getDatabasePassword());
            config.setDriverClassName(appConfig.getDatabaseDriver());
            
            // Connection pool configuration
            config.setMaximumPoolSize(appConfig.getMaximumPoolSize());
            config.setMinimumIdle(appConfig.getMinimumIdle());
            config.setConnectionTimeout(appConfig.getConnectionTimeout());
            config.setIdleTimeout(appConfig.getIdleTimeout());
            config.setMaxLifetime(appConfig.getMaxLifetime());
            
            // Additional HikariCP settings
            config.setPoolName("StockTradePool");
            config.setLeakDetectionThreshold(60000);
            config.setConnectionTestQuery("SELECT 1");
            
            // H2 specific settings
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            
            this.dataSource = new HikariDataSource(config);
            
            logger.info("Database connection pool initialized successfully");
            logger.info("Database URL: {}", appConfig.getDatabaseUrl());
            logger.info("Maximum pool size: {}", appConfig.getMaximumPoolSize());
            
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

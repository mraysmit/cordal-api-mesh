package dev.mars.common.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.mars.common.config.DatabaseConfig;
import dev.mars.common.config.PoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * Factory class for creating database connections with HikariCP
 * Common database connection factory used across all modules
 */
public class DatabaseConnectionFactory {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionFactory.class);

    /**
     * Create a HikariDataSource from DatabaseConfig
     */
    public static HikariDataSource createDataSource(DatabaseConfig config) {
        logger.info("Creating data source for database: {}", config.getName());
        
        HikariConfig hikariConfig = new HikariConfig();
        
        // Basic connection settings
        hikariConfig.setJdbcUrl(config.getUrl());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setDriverClassName(config.getDriver());
        
        // Pool configuration
        PoolConfig poolConfig = config.getPool();
        if (poolConfig != null) {
            hikariConfig.setMaximumPoolSize(poolConfig.getMaximumPoolSize());
            hikariConfig.setMinimumIdle(poolConfig.getMinimumIdle());
            hikariConfig.setConnectionTimeout(poolConfig.getConnectionTimeout());
            hikariConfig.setIdleTimeout(poolConfig.getIdleTimeout());
            hikariConfig.setMaxLifetime(poolConfig.getMaxLifetime());
            hikariConfig.setLeakDetectionThreshold(poolConfig.getLeakDetectionThreshold());
            hikariConfig.setConnectionTestQuery(poolConfig.getConnectionTestQuery());
        } else {
            // Default pool settings
            hikariConfig.setMaximumPoolSize(10);
            hikariConfig.setMinimumIdle(2);
            hikariConfig.setConnectionTimeout(30000);
            hikariConfig.setIdleTimeout(600000);
            hikariConfig.setMaxLifetime(1800000);
            hikariConfig.setLeakDetectionThreshold(60000);
            hikariConfig.setConnectionTestQuery("SELECT 1");
        }
        
        // Additional HikariCP settings
        hikariConfig.setPoolName(config.getName() + "-pool");
        hikariConfig.setAutoCommit(true);
        hikariConfig.setConnectionInitSql("SELECT 1");
        
        logger.info("Data source created successfully for database: {}", config.getName());
        return new HikariDataSource(hikariConfig);
    }

    /**
     * Create a simple HikariDataSource with basic configuration
     */
    public static HikariDataSource createDataSource(String url, String username, String password, String driver) {
        DatabaseConfig config = new DatabaseConfig();
        config.setName("default");
        config.setUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriver(driver);
        
        return createDataSource(config);
    }
}

package dev.mars.config;

import dev.mars.common.config.DatabaseConfig;
import dev.mars.common.config.PoolConfig;

/**
 * Adapter class to convert GenericApiConfig to common-library DatabaseConfig
 */
public class DatabaseConfigAdapter {
    
    public static DatabaseConfig createCommonDatabaseConfig(GenericApiConfig genericApiConfig) {
        DatabaseConfig config = new DatabaseConfig();
        
        config.setName("api-service-config-database");
        config.setUrl(genericApiConfig.getDatabaseUrl());
        config.setUsername(genericApiConfig.getDatabaseUsername());
        config.setPassword(genericApiConfig.getDatabasePassword());
        config.setDriver(genericApiConfig.getDatabaseDriver());
        
        // Create pool configuration with default values
        PoolConfig poolConfig = new PoolConfig();
        poolConfig.setMaximumPoolSize(10);
        poolConfig.setMinimumIdle(2);
        poolConfig.setConnectionTimeout(30000);
        poolConfig.setIdleTimeout(600000);
        poolConfig.setMaxLifetime(1800000);
        poolConfig.setLeakDetectionThreshold(60000);
        poolConfig.setConnectionTestQuery("SELECT 1");
        
        config.setPool(poolConfig);
        
        return config;
    }
}

package dev.cordal.test;

import dev.cordal.common.config.DatabaseConfig;
import dev.cordal.common.config.PoolConfig;
import dev.cordal.config.GenericApiConfig;

/**
 * Test adapter class to convert GenericApiConfig to common-library DatabaseConfig
 * Used only for testing purposes
 */
public class DatabaseConfigAdapter {
    
    public static DatabaseConfig createCommonDatabaseConfig(GenericApiConfig genericApiConfig) {
        DatabaseConfig config = new DatabaseConfig();
        
        config.setName("test-database");
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

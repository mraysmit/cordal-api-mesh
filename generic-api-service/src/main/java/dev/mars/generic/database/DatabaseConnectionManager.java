package dev.mars.generic.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.mars.generic.config.DatabaseConfig;
import dev.mars.generic.config.EndpointConfigurationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages multiple database connections based on configuration
 */
@Singleton
public class DatabaseConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionManager.class);
    
    private final Map<String, HikariDataSource> dataSources;
    private final EndpointConfigurationManager configurationManager;
    
    @Inject
    public DatabaseConnectionManager(EndpointConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
        this.dataSources = new ConcurrentHashMap<>();
        
        logger.info("Initializing database connection manager");
        initializeDataSources();
        logger.info("Database connection manager initialized with {} databases", dataSources.size());
    }
    
    /**
     * Initialize all configured data sources
     */
    private void initializeDataSources() {
        Map<String, DatabaseConfig> databaseConfigs = configurationManager.getAllDatabaseConfigurations();
        
        for (Map.Entry<String, DatabaseConfig> entry : databaseConfigs.entrySet()) {
            String databaseName = entry.getKey();
            DatabaseConfig config = entry.getValue();
            
            try {
                HikariDataSource dataSource = createDataSource(databaseName, config);
                dataSources.put(databaseName, dataSource);
                logger.info("Initialized data source for database: {}", databaseName);
            } catch (Exception e) {
                logger.error("Failed to initialize data source for database: {}", databaseName, e);
                throw new RuntimeException("Failed to initialize data source for database: " + databaseName, e);
            }
        }
    }
    
    /**
     * Create a HikariCP data source from database configuration
     */
    private HikariDataSource createDataSource(String databaseName, DatabaseConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        
        // Basic database configuration
        hikariConfig.setJdbcUrl(config.getUrl());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setDriverClassName(config.getDriver());
        
        // Connection pool configuration
        if (config.getPool() != null) {
            DatabaseConfig.PoolConfig poolConfig = config.getPool();
            hikariConfig.setMaximumPoolSize(poolConfig.getMaximumPoolSize());
            hikariConfig.setMinimumIdle(poolConfig.getMinimumIdle());
            hikariConfig.setConnectionTimeout(poolConfig.getConnectionTimeout());
            hikariConfig.setIdleTimeout(poolConfig.getIdleTimeout());
            hikariConfig.setMaxLifetime(poolConfig.getMaxLifetime());
            hikariConfig.setLeakDetectionThreshold(poolConfig.getLeakDetectionThreshold());
            hikariConfig.setConnectionTestQuery(poolConfig.getConnectionTestQuery());
        }
        
        // Pool name for identification
        hikariConfig.setPoolName(databaseName + "Pool");
        
        // Additional HikariCP settings for better performance
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        return new HikariDataSource(hikariConfig);
    }
    
    /**
     * Get a connection for the specified database
     */
    public Connection getConnection(String databaseName) throws SQLException {
        HikariDataSource dataSource = dataSources.get(databaseName);
        if (dataSource == null) {
            throw new IllegalArgumentException("Database not configured: " + databaseName);
        }
        
        return dataSource.getConnection();
    }
    
    /**
     * Get the data source for the specified database
     */
    public DataSource getDataSource(String databaseName) {
        HikariDataSource dataSource = dataSources.get(databaseName);
        if (dataSource == null) {
            throw new IllegalArgumentException("Database not configured: " + databaseName);
        }
        
        return dataSource;
    }
    
    /**
     * Check if a database is healthy
     */
    public boolean isDatabaseHealthy(String databaseName) {
        try {
            HikariDataSource dataSource = dataSources.get(databaseName);
            if (dataSource == null) {
                return false;
            }
            
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                
                statement.execute("SELECT 1");
                return true;
                
            }
        } catch (SQLException e) {
            logger.error("Database health check failed for: {}", databaseName, e);
            return false;
        }
    }
    
    /**
     * Check if all databases are healthy
     */
    public boolean areAllDatabasesHealthy() {
        for (String databaseName : dataSources.keySet()) {
            if (!isDatabaseHealthy(databaseName)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Get all configured database names
     */
    public java.util.Set<String> getDatabaseNames() {
        return dataSources.keySet();
    }
    
    /**
     * Close all data sources
     */
    public void shutdown() {
        logger.info("Shutting down database connection manager");
        
        for (Map.Entry<String, HikariDataSource> entry : dataSources.entrySet()) {
            String databaseName = entry.getKey();
            HikariDataSource dataSource = entry.getValue();
            
            try {
                dataSource.close();
                logger.info("Closed data source for database: {}", databaseName);
            } catch (Exception e) {
                logger.error("Error closing data source for database: {}", databaseName, e);
            }
        }
        
        dataSources.clear();
        logger.info("Database connection manager shutdown completed");
    }
}

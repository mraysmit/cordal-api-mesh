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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages multiple database connections based on configuration
 */
@Singleton
public class DatabaseConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionManager.class);
    
    private final Map<String, HikariDataSource> dataSources;
    private final Map<String, String> failedDatabases; // database name -> error message
    private final EndpointConfigurationManager configurationManager;

    @Inject
    public DatabaseConnectionManager(EndpointConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
        this.dataSources = new ConcurrentHashMap<>();
        this.failedDatabases = new ConcurrentHashMap<>();

        logger.info("Initializing database connection manager");
        initializeDataSources();
        logger.info("Database connection manager initialized with {} successful databases and {} failed databases",
                   dataSources.size(), failedDatabases.size());
    }
    
    /**
     * Initialize all configured data sources - continue even if some fail
     */
    private void initializeDataSources() {
        Map<String, DatabaseConfig> databaseConfigs = configurationManager.getAllDatabaseConfigurations();

        for (Map.Entry<String, DatabaseConfig> entry : databaseConfigs.entrySet()) {
            String databaseName = entry.getKey();
            DatabaseConfig config = entry.getValue();

            try {
                HikariDataSource dataSource = createDataSource(databaseName, config);

                // Test the connection to ensure it's actually working
                try (Connection testConnection = dataSource.getConnection()) {
                    testConnection.createStatement().execute("SELECT 1");
                    dataSources.put(databaseName, dataSource);
                    logger.info("Successfully initialized and tested data source for database: {}", databaseName);
                } catch (SQLException testException) {
                    // Close the data source if connection test fails
                    try {
                        dataSource.close();
                    } catch (Exception closeException) {
                        logger.warn("Failed to close failed data source for database: {}", databaseName, closeException);
                    }
                    throw testException;
                }

            } catch (Exception e) {
                String errorMessage = "Failed to initialize data source: " + e.getMessage();
                failedDatabases.put(databaseName, errorMessage);
                logger.error("Failed to initialize data source for database: {} - {}", databaseName, errorMessage, e);
                logger.warn("Database '{}' will be marked as unavailable. Endpoints using this database will return errors.", databaseName);
            }
        }

        // Log summary
        if (!failedDatabases.isEmpty()) {
            logger.warn("Application started with {} database(s) unavailable: {}",
                       failedDatabases.size(), failedDatabases.keySet());
            logger.info("Endpoints depending on unavailable databases will return service unavailable errors");
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
        // Try to set the driver class - this might fail if driver is not available
        try {
            hikariConfig.setDriverClassName(config.getDriver());
        } catch (RuntimeException e) {
            // Check if it's a driver loading issue
            if (e.getMessage() != null && e.getMessage().contains("Failed to load driver class")) {
                logger.error("Database driver not available for database '{}': {}", databaseName, config.getDriver());
                logger.error("Driver loading error: {}", e.getMessage());
                logger.warn("Make sure the JDBC driver dependency is included in the classpath");
                throw new RuntimeException("Database driver not available: " + config.getDriver() + " - " + e.getMessage(), e);
            } else {
                throw e; // Re-throw if it's a different error
            }
        }
        
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
        // Check if database failed during initialization
        if (failedDatabases.containsKey(databaseName)) {
            throw new SQLException("Database '" + databaseName + "' is unavailable: " + failedDatabases.get(databaseName));
        }

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
     * Check if a database is available (not failed during initialization)
     */
    public boolean isDatabaseAvailable(String databaseName) {
        return dataSources.containsKey(databaseName) && !failedDatabases.containsKey(databaseName);
    }

    /**
     * Get the error message for a failed database
     */
    public String getDatabaseFailureReason(String databaseName) {
        return failedDatabases.get(databaseName);
    }

    /**
     * Get all failed database names
     */
    public Set<String> getFailedDatabaseNames() {
        return new HashSet<>(failedDatabases.keySet());
    }

    /**
     * Get all available database names
     */
    public Set<String> getAvailableDatabaseNames() {
        return new HashSet<>(dataSources.keySet());
    }

    /**
     * Check if a database is healthy (available and can execute queries)
     */
    public boolean isDatabaseHealthy(String databaseName) {
        // First check if database is available
        if (!isDatabaseAvailable(databaseName)) {
            return false;
        }

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

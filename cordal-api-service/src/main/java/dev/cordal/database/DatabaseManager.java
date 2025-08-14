package dev.cordal.database;

import dev.cordal.common.database.BaseDatabaseManager;
import dev.cordal.config.DatabaseConfigAdapter;
import dev.cordal.config.GenericApiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database manager for handling database initialization and schema creation
 * Now extends BaseDatabaseManager from common-library
 */
public class DatabaseManager extends BaseDatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private final GenericApiConfig genericApiConfig;

    public DatabaseManager(GenericApiConfig genericApiConfig) {
        super(DatabaseConfigAdapter.createCommonDatabaseConfig(genericApiConfig));
        this.genericApiConfig = genericApiConfig;

        // Ensure database can be created if configured to do so
        if (genericApiConfig.isDatabaseCreateIfMissing()) {
            ensureDatabaseCanBeCreated();
        }
    }

    /**
     * TODO: depends on H2 database needs to extend to at least PG
     * Ensure the database directory exists and database can be created
     */
    private void ensureDatabaseCanBeCreated() {
        String databaseUrl = genericApiConfig.getDatabaseUrl();
        logger.info("Checking if database can be created for URL: {}", databaseUrl);

        // Extract file path from H2 database URL
        if (databaseUrl.startsWith("jdbc:h2:")) {
            String filePath = databaseUrl.substring("jdbc:h2:".length());

            // Remove H2 connection parameters
            int paramIndex = filePath.indexOf(';');
            if (paramIndex > 0) {
                filePath = filePath.substring(0, paramIndex);
            }

            // Skip directory creation for in-memory databases
            if (filePath.startsWith("mem:")) {
                logger.info("In-memory database detected, skipping directory creation");
                return;
            }

            // Handle relative paths
            if (!filePath.startsWith("/") && !filePath.matches("^[A-Za-z]:.*")) {
                // It's a relative path, resolve it from current working directory
                filePath = System.getProperty("user.dir") + File.separator + filePath;
            }

            try {
                Path dbPath = Paths.get(filePath).getParent();
                if (dbPath != null && !Files.exists(dbPath)) {
                    Files.createDirectories(dbPath);
                    logger.info("Created database directory: {}", dbPath.toAbsolutePath());
                } else if (dbPath != null) {
                    logger.info("Database directory already exists: {}", dbPath.toAbsolutePath());
                } else {
                    logger.info("Database will be created in current directory");
                }
            } catch (Exception e) {
                logger.warn("Could not create database directory for path: {}", filePath, e);
                // Don't throw exception here - let H2 handle it during connection
            }
        } else {
            logger.info("Non-file database URL detected, skipping directory creation");
        }
    }

    /**
     * Initialize the database schema for API service configuration
     */
    public void initializeSchema() {
        logger.info("Initializing API service configuration database schema");
        logger.info("Database creation enabled: {}", genericApiConfig.isDatabaseCreateIfMissing());

        String createConfigDatabasesTableSql = """
            CREATE TABLE IF NOT EXISTS config_databases (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL UNIQUE,
                description TEXT,
                url VARCHAR(500) NOT NULL,
                username VARCHAR(255),
                password VARCHAR(255),
                driver VARCHAR(255) NOT NULL,
                maximum_pool_size INTEGER DEFAULT 10,
                minimum_idle INTEGER DEFAULT 2,
                connection_timeout BIGINT DEFAULT 30000,
                idle_timeout BIGINT DEFAULT 600000,
                max_lifetime BIGINT DEFAULT 1800000,
                leak_detection_threshold BIGINT DEFAULT 60000,
                connection_test_query VARCHAR(255) DEFAULT 'SELECT 1',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """;

        String createConfigQueriesTableSql = """
            CREATE TABLE IF NOT EXISTS config_queries (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL UNIQUE,
                description TEXT,
                database_name VARCHAR(255) NOT NULL,
                sql_query TEXT NOT NULL,
                query_type VARCHAR(50) DEFAULT 'SELECT',
                timeout_seconds INTEGER DEFAULT 30,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """;

        String createConfigEndpointsTableSql = """
            CREATE TABLE IF NOT EXISTS config_endpoints (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL UNIQUE,
                description TEXT,
                path VARCHAR(500) NOT NULL,
                method VARCHAR(10) NOT NULL,
                query_name VARCHAR(255) NOT NULL,
                response_format VARCHAR(50) DEFAULT 'json',
                cache_enabled BOOLEAN DEFAULT false,
                cache_ttl_seconds INTEGER DEFAULT 300,
                rate_limit_enabled BOOLEAN DEFAULT false,
                rate_limit_requests INTEGER DEFAULT 100,
                rate_limit_window_seconds INTEGER DEFAULT 60,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """;

        // Configuration tables indexes
        String createIndexSql1 = "CREATE INDEX IF NOT EXISTS idx_config_databases_name ON config_databases(name)";
        String createIndexSql2 = "CREATE INDEX IF NOT EXISTS idx_config_queries_name ON config_queries(name)";
        String createIndexSql3 = "CREATE INDEX IF NOT EXISTS idx_config_queries_database ON config_queries(database_name)";
        String createIndexSql4 = "CREATE INDEX IF NOT EXISTS idx_config_endpoints_name ON config_endpoints(name)";
        String createIndexSql5 = "CREATE INDEX IF NOT EXISTS idx_config_endpoints_path ON config_endpoints(path)";
        String createIndexSql6 = "CREATE INDEX IF NOT EXISTS idx_config_endpoints_query ON config_endpoints(query_name)";





        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // Create configuration tables
            statement.execute(createConfigDatabasesTableSql);
            logger.info("Configuration databases table created/verified");

            statement.execute(createConfigQueriesTableSql);
            logger.info("Configuration queries table created/verified");

            statement.execute(createConfigEndpointsTableSql);
            logger.info("Configuration endpoints table created/verified");

            // Create configuration table indexes
            statement.execute(createIndexSql1);
            statement.execute(createIndexSql2);
            statement.execute(createIndexSql3);
            statement.execute(createIndexSql4);
            statement.execute(createIndexSql5);
            statement.execute(createIndexSql6);
            logger.info("Configuration table indexes created/verified");

        } catch (SQLException e) {
            logger.error("Failed to initialize database schema", e);
            if (genericApiConfig.isDatabaseCreateIfMissing()) {
                logger.error("Database creation was enabled but failed. Check database URL: {}",
                           genericApiConfig.getDatabaseUrl());
                logger.error("Ensure the database directory exists and is writable");
            }
            throw new RuntimeException("Failed to initialize database schema", e);
        }

        logger.info("API service configuration database schema initialized successfully");
    }







    /**
     * Get a connection from the data source
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    /**
     * Check if the database is accessible
     */
    public boolean isHealthy() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("SELECT 1");
            return true;

        } catch (SQLException e) {
            logger.error("Database health check failed", e);
            return false;
        }
    }

    /**
     * Clean all data from the database (for testing purposes)
     */
    public void cleanDatabase() {
        logger.info("Cleaning database for testing");

        String deleteConfigDatabasesSql = "DELETE FROM config_databases";
        String deleteConfigQueriesSql = "DELETE FROM config_queries";
        String deleteConfigEndpointsSql = "DELETE FROM config_endpoints";
        String resetConfigDatabasesSequenceSql = "ALTER TABLE config_databases ALTER COLUMN id RESTART WITH 1";
        String resetConfigQueriesSequenceSql = "ALTER TABLE config_queries ALTER COLUMN id RESTART WITH 1";
        String resetConfigEndpointsSequenceSql = "ALTER TABLE config_endpoints ALTER COLUMN id RESTART WITH 1";

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(deleteConfigDatabasesSql);
            statement.execute(resetConfigDatabasesSequenceSql);

            statement.execute(deleteConfigQueriesSql);
            statement.execute(resetConfigQueriesSequenceSql);

            statement.execute(deleteConfigEndpointsSql);
            statement.execute(resetConfigEndpointsSequenceSql);

            logger.info("Database cleaned successfully");

        } catch (SQLException e) {
            logger.error("Failed to clean database", e);
            throw new RuntimeException("Failed to clean database", e);
        }
    }
}

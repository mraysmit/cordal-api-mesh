package dev.mars.database;

import dev.mars.config.GenericApiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data loader for populating the database with sample configuration data
 * This is used when config.source is set to "database"
 */
public class ConfigurationDataLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationDataLoader.class);
    
    private final DatabaseManager databaseManager;
    private final GenericApiConfig genericApiConfig;
    
    public ConfigurationDataLoader(DatabaseManager databaseManager, GenericApiConfig genericApiConfig) {
        this.databaseManager = databaseManager;
        this.genericApiConfig = genericApiConfig;
    }
    
    /**
     * Load sample configuration data if needed and if config source is database
     */
    public void loadSampleConfigurationDataIfNeeded() {
        if (!"database".equals(genericApiConfig.getConfigSource())) {
            logger.info("Configuration source is not database, skipping configuration data loading");
            return;
        }
        
        logger.info("Loading sample configuration data for API service");
        
        try {
            if (isConfigurationDataAlreadyLoaded()) {
                logger.info("Configuration data already exists, skipping data loading");
                return;
            }
            
            loadSampleConfigurationData();
            
        } catch (Exception e) {
            logger.error("Failed to load sample configuration data", e);
            throw new RuntimeException("Failed to load sample configuration data", e);
        }
    }
    
    private boolean isConfigurationDataAlreadyLoaded() throws SQLException {
        String countSql = "SELECT COUNT(*) FROM config_databases";
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(countSql);
             ResultSet resultSet = statement.executeQuery()) {
            
            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                return count > 0;
            }
            
            return false;
        }
    }
    
    private void loadSampleConfigurationData() throws SQLException {
        logger.info("Loading sample configuration data");
        
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            
            // Load sample database configurations
            loadSampleDatabaseConfigurations(connection);
            
            // Load sample query configurations
            loadSampleQueryConfigurations(connection);
            
            // Load sample endpoint configurations
            loadSampleEndpointConfigurations(connection);
            
            connection.commit();
            logger.info("Successfully loaded sample configuration data");
            
        } catch (SQLException e) {
            logger.error("Failed to load sample configuration data", e);
            throw e;
        }
    }
    
    private void loadSampleDatabaseConfigurations(Connection connection) throws SQLException {
        String insertSql = """
            INSERT INTO config_databases (name, description, url, username, password, driver,
                                        maximum_pool_size, minimum_idle, connection_timeout,
                                        idle_timeout, max_lifetime, leak_detection_threshold,
                                        connection_test_query) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            // Sample database configuration
            statement.setString(1, "api-service-config-db");
            statement.setString(2, "Main database for API service configuration data");
            statement.setString(3, "jdbc:h2:./data/api-service-config;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1");
            statement.setString(4, "sa");
            statement.setString(5, "");
            statement.setString(6, "org.h2.Driver");
            statement.setInt(7, 10);
            statement.setInt(8, 2);
            statement.setLong(9, 30000);
            statement.setLong(10, 600000);
            statement.setLong(11, 1800000);
            statement.setLong(12, 60000);
            statement.setString(13, "SELECT 1");
            statement.executeUpdate();
            
            // Metrics database configuration
            statement.setString(1, "metrics-db");
            statement.setString(2, "Database for performance metrics and monitoring data");
            statement.setString(3, "jdbc:h2:./data/metrics;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1");
            statement.setString(4, "sa");
            statement.setString(5, "");
            statement.setString(6, "org.h2.Driver");
            statement.setInt(7, 5);
            statement.setInt(8, 1);
            statement.setLong(9, 30000);
            statement.setLong(10, 600000);
            statement.setLong(11, 1800000);
            statement.setLong(12, 60000);
            statement.setString(13, "SELECT 1");
            statement.executeUpdate();
            
            logger.info("Loaded sample database configurations");
        }
    }
    
    private void loadSampleQueryConfigurations(Connection connection) throws SQLException {
        String insertSql = """
            INSERT INTO config_queries (name, description, database_name, sql_query, query_type, timeout_seconds) 
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            // Sample query configurations
            statement.setString(1, "get-all-databases");
            statement.setString(2, "Get all database configurations");
            statement.setString(3, "api-service-config-db");
            statement.setString(4, "SELECT * FROM config_databases ORDER BY name");
            statement.setString(5, "SELECT");
            statement.setInt(6, 30);
            statement.executeUpdate();
            
            statement.setString(1, "get-all-queries");
            statement.setString(2, "Get all query configurations");
            statement.setString(3, "api-service-config-db");
            statement.setString(4, "SELECT * FROM config_queries ORDER BY name");
            statement.setString(5, "SELECT");
            statement.setInt(6, 30);
            statement.executeUpdate();
            
            statement.setString(1, "get-all-endpoints");
            statement.setString(2, "Get all endpoint configurations");
            statement.setString(3, "api-service-config-db");
            statement.setString(4, "SELECT * FROM config_endpoints ORDER BY name");
            statement.setString(5, "SELECT");
            statement.setInt(6, 30);
            statement.executeUpdate();
            
            logger.info("Loaded sample query configurations");
        }
    }
    
    private void loadSampleEndpointConfigurations(Connection connection) throws SQLException {
        String insertSql = """
            INSERT INTO config_endpoints (name, description, path, method, query_name, response_format,
                                        cache_enabled, cache_ttl_seconds, rate_limit_enabled,
                                        rate_limit_requests, rate_limit_window_seconds) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            // Sample endpoint configurations
            statement.setString(1, "list-databases");
            statement.setString(2, "List all database configurations");
            statement.setString(3, "/api/config/databases");
            statement.setString(4, "GET");
            statement.setString(5, "get-all-databases");
            statement.setString(6, "json");
            statement.setBoolean(7, true);
            statement.setInt(8, 300);
            statement.setBoolean(9, true);
            statement.setInt(10, 100);
            statement.setInt(11, 60);
            statement.executeUpdate();
            
            statement.setString(1, "list-queries");
            statement.setString(2, "List all query configurations");
            statement.setString(3, "/api/config/queries");
            statement.setString(4, "GET");
            statement.setString(5, "get-all-queries");
            statement.setString(6, "json");
            statement.setBoolean(7, true);
            statement.setInt(8, 300);
            statement.setBoolean(9, true);
            statement.setInt(10, 100);
            statement.setInt(11, 60);
            statement.executeUpdate();
            
            statement.setString(1, "list-endpoints");
            statement.setString(2, "List all endpoint configurations");
            statement.setString(3, "/api/config/endpoints");
            statement.setString(4, "GET");
            statement.setString(5, "get-all-endpoints");
            statement.setString(6, "json");
            statement.setBoolean(7, true);
            statement.setInt(8, 300);
            statement.setBoolean(9, true);
            statement.setInt(10, 100);
            statement.setInt(11, 60);
            statement.executeUpdate();
            
            logger.info("Loaded sample endpoint configurations");
        }
    }
}

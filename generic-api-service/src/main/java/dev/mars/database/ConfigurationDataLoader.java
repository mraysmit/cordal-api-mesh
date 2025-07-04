package dev.mars.database;

import dev.mars.config.GenericApiConfig;
import dev.mars.generic.config.ConfigurationLoader;
import dev.mars.generic.config.ApiEndpointConfig;
import dev.mars.generic.config.DatabaseConfig;
import dev.mars.generic.config.QueryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * Data loader for populating the database with configuration data from YAML files
 * This is used when config.source is set to "database" and config.loadFromYaml is true
 */
public class ConfigurationDataLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationDataLoader.class);

    private final DatabaseManager databaseManager;
    private final GenericApiConfig genericApiConfig;
    private final ConfigurationLoader configurationLoader;

    public ConfigurationDataLoader(DatabaseManager databaseManager, GenericApiConfig genericApiConfig, ConfigurationLoader configurationLoader) {
        this.databaseManager = databaseManager;
        this.genericApiConfig = genericApiConfig;
        this.configurationLoader = configurationLoader;
    }
    
    /**
     * Load configuration data from YAML files if needed and if configured to do so
     */
    public void loadConfigurationDataIfNeeded() {
        if (!"database".equals(genericApiConfig.getConfigSource())) {
            logger.info("Configuration source is not database, skipping configuration data loading");
            return;
        }

        if (!genericApiConfig.isLoadConfigFromYaml()) {
            logger.info("Loading configuration from YAML is disabled, skipping configuration data loading");
            return;
        }

        logger.info("Loading configuration data from YAML files for API service");

        try {
            if (isConfigurationDataAlreadyLoaded()) {
                logger.info("Configuration data already exists, skipping data loading");
                return;
            }

            loadConfigurationDataFromYaml();

        } catch (Exception e) {
            logger.error("Failed to load configuration data from YAML files", e);
            throw new RuntimeException("Failed to load configuration data from YAML files", e);
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
    
    private void loadConfigurationDataFromYaml() throws SQLException {
        logger.info("Loading configuration data from YAML files");

        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);

            // Load database configurations from YAML
            loadDatabaseConfigurationsFromYaml(connection);

            // Load query configurations from YAML
            loadQueryConfigurationsFromYaml(connection);

            // Load endpoint configurations from YAML
            loadEndpointConfigurationsFromYaml(connection);

            connection.commit();
            logger.info("Successfully loaded configuration data from YAML files");

        } catch (SQLException e) {
            logger.error("Failed to load configuration data from YAML files", e);
            throw e;
        }
    }
    
    private void loadDatabaseConfigurationsFromYaml(Connection connection) throws SQLException {
        logger.info("Loading database configurations from YAML");

        Map<String, DatabaseConfig> databases = configurationLoader.loadDatabaseConfigurations();

        String insertSql = """
            INSERT INTO config_databases (name, description, url, username, password, driver,
                                        maximum_pool_size, minimum_idle, connection_timeout,
                                        idle_timeout, max_lifetime, leak_detection_threshold,
                                        connection_test_query)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            for (Map.Entry<String, DatabaseConfig> entry : databases.entrySet()) {
                String key = entry.getKey();
                DatabaseConfig config = entry.getValue();

                statement.setString(1, key);
                statement.setString(2, config.getDescription());
                statement.setString(3, config.getUrl());
                statement.setString(4, config.getUsername());
                statement.setString(5, config.getPassword());
                statement.setString(6, config.getDriver());

                // Handle pool configuration - use defaults if pool is null
                DatabaseConfig.PoolConfig pool = config.getPool();
                if (pool != null) {
                    statement.setInt(7, pool.getMaximumPoolSize());
                    statement.setInt(8, pool.getMinimumIdle());
                    statement.setLong(9, pool.getConnectionTimeout());
                    statement.setLong(10, pool.getIdleTimeout());
                    statement.setLong(11, pool.getMaxLifetime());
                    statement.setLong(12, pool.getLeakDetectionThreshold());
                    statement.setString(13, pool.getConnectionTestQuery());
                } else {
                    // Use default values if pool config is not provided
                    statement.setInt(7, 10);
                    statement.setInt(8, 2);
                    statement.setLong(9, 30000);
                    statement.setLong(10, 600000);
                    statement.setLong(11, 1800000);
                    statement.setLong(12, 60000);
                    statement.setString(13, "SELECT 1");
                }
                statement.executeUpdate();

                logger.debug("Loaded database configuration: {}", key);
            }

            logger.info("Loaded {} database configurations from YAML", databases.size());
        }
    }
    
    private void loadQueryConfigurationsFromYaml(Connection connection) throws SQLException {
        logger.info("Loading query configurations from YAML");

        Map<String, QueryConfig> queries = configurationLoader.loadQueryConfigurations();

        String insertSql = """
            INSERT INTO config_queries (name, description, database_name, sql_query, query_type, timeout_seconds)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            for (Map.Entry<String, QueryConfig> entry : queries.entrySet()) {
                String key = entry.getKey();
                QueryConfig config = entry.getValue();

                statement.setString(1, key);
                statement.setString(2, config.getDescription());
                statement.setString(3, config.getDatabase());
                statement.setString(4, config.getSql());
                statement.setString(5, "SELECT"); // Default query type since QueryConfig doesn't have getType()
                statement.setInt(6, 30); // Default timeout since QueryConfig doesn't have getTimeoutSeconds()
                statement.executeUpdate();

                logger.debug("Loaded query configuration: {}", key);
            }

            logger.info("Loaded {} query configurations from YAML", queries.size());
        }
    }
    
    private void loadEndpointConfigurationsFromYaml(Connection connection) throws SQLException {
        logger.info("Loading endpoint configurations from YAML");

        Map<String, ApiEndpointConfig> endpoints = configurationLoader.loadEndpointConfigurations();

        String insertSql = """
            INSERT INTO config_endpoints (name, description, path, method, query_name, response_format,
                                        cache_enabled, cache_ttl_seconds, rate_limit_enabled,
                                        rate_limit_requests, rate_limit_window_seconds)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            for (Map.Entry<String, ApiEndpointConfig> entry : endpoints.entrySet()) {
                String key = entry.getKey();
                ApiEndpointConfig config = entry.getValue();

                statement.setString(1, key);
                statement.setString(2, config.getDescription());
                statement.setString(3, config.getPath());
                statement.setString(4, config.getMethod());
                statement.setString(5, config.getQuery());
                statement.setString(6, "json"); // Default response format since ApiEndpointConfig doesn't have getResponseFormat()
                statement.setBoolean(7, false); // Default cache disabled since ApiEndpointConfig doesn't have isCacheEnabled()
                statement.setInt(8, 300); // Default cache TTL since ApiEndpointConfig doesn't have getCacheTtlSeconds()
                statement.setBoolean(9, false); // Default rate limit disabled since ApiEndpointConfig doesn't have isRateLimitEnabled()
                statement.setInt(10, 100); // Default rate limit requests since ApiEndpointConfig doesn't have getRateLimitRequests()
                statement.setInt(11, 60); // Default rate limit window since ApiEndpointConfig doesn't have getRateLimitWindowSeconds()
                statement.executeUpdate();

                logger.debug("Loaded endpoint configuration: {}", key);
            }

            logger.info("Loaded {} endpoint configurations from YAML", endpoints.size());
        }
    }
}

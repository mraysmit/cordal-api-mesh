package dev.mars.database.repository;

import dev.mars.database.DatabaseManager;
import dev.mars.generic.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.*;
import java.util.*;

/**
 * Repository for managing database configurations stored in H2 database
 */
@Singleton
public class DatabaseConfigurationRepository {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfigurationRepository.class);

    private final DatabaseManager databaseManager;

    @Inject
    public DatabaseConfigurationRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        logger.info("Database configuration repository initialized");
    }

    /**
     * Load all database configurations from the database
     */
    public Map<String, DatabaseConfig> loadAll() {
        logger.info("Loading all database configurations from database");
        Map<String, DatabaseConfig> configurations = new HashMap<>();

        String selectSql = """
            SELECT name, description, url, username, password, driver,
                   maximum_pool_size, minimum_idle, connection_timeout,
                   idle_timeout, max_lifetime, leak_detection_threshold,
                   connection_test_query
            FROM config_databases
            ORDER BY name
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectSql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                DatabaseConfig config = mapResultSetToConfig(resultSet);
                configurations.put(config.getName(), config);
                logger.debug("Loaded database configuration: {}", config.getName());
            }

            logger.info("Loaded {} database configurations from database", configurations.size());
            return configurations;

        } catch (SQLException e) {
            logger.error("Failed to load database configurations from database", e);
            throw new RuntimeException("Failed to load database configurations", e);
        }
    }

    /**
     * Load a specific database configuration by name
     */
    public Optional<DatabaseConfig> loadByName(String name) {
        logger.debug("Loading database configuration by name: {}", name);

        String selectSql = """
            SELECT name, description, url, username, password, driver,
                   maximum_pool_size, minimum_idle, connection_timeout,
                   idle_timeout, max_lifetime, leak_detection_threshold,
                   connection_test_query
            FROM config_databases
            WHERE name = ?
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectSql)) {

            statement.setString(1, name);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    DatabaseConfig config = mapResultSetToConfig(resultSet);
                    logger.debug("Found database configuration: {}", name);
                    return Optional.of(config);
                } else {
                    logger.debug("Database configuration not found: {}", name);
                    return Optional.empty();
                }
            }

        } catch (SQLException e) {
            logger.error("Failed to load database configuration by name: {}", name, e);
            throw new RuntimeException("Failed to load database configuration: " + name, e);
        }
    }

    /**
     * Save a database configuration to the database
     */
    public void save(String key, DatabaseConfig config) {
        logger.debug("Saving database configuration: {}", key);

        String mergeSql = """
            MERGE INTO config_databases (name, description, url, username, password, driver,
                                        maximum_pool_size, minimum_idle, connection_timeout,
                                        idle_timeout, max_lifetime, leak_detection_threshold,
                                        connection_test_query)
            KEY(name)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(mergeSql)) {

            setStatementParameters(statement, key, config);
            int rowsAffected = statement.executeUpdate();

            if (rowsAffected > 0) {
                logger.debug("Successfully saved database configuration: {}", key);
            } else {
                logger.warn("No rows affected when saving database configuration: {}", key);
            }

        } catch (SQLException e) {
            logger.error("Failed to save database configuration: {}", key, e);
            throw new RuntimeException("Failed to save database configuration: " + key, e);
        }
    }

    /**
     * Delete a database configuration by name
     */
    public boolean delete(String name) {
        logger.debug("Deleting database configuration: {}", name);

        String deleteSql = "DELETE FROM config_databases WHERE name = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(deleteSql)) {

            statement.setString(1, name);
            int rowsAffected = statement.executeUpdate();

            if (rowsAffected > 0) {
                logger.debug("Successfully deleted database configuration: {}", name);
                return true;
            } else {
                logger.debug("Database configuration not found for deletion: {}", name);
                return false;
            }

        } catch (SQLException e) {
            logger.error("Failed to delete database configuration: {}", name, e);
            throw new RuntimeException("Failed to delete database configuration: " + name, e);
        }
    }

    /**
     * Check if a database configuration exists
     */
    public boolean exists(String name) {
        logger.debug("Checking if database configuration exists: {}", name);

        String selectSql = "SELECT COUNT(*) FROM config_databases WHERE name = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectSql)) {

            statement.setString(1, name);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    boolean exists = resultSet.getInt(1) > 0;
                    logger.debug("Database configuration exists: {} = {}", name, exists);
                    return exists;
                }
            }

        } catch (SQLException e) {
            logger.error("Failed to check if database configuration exists: {}", name, e);
            throw new RuntimeException("Failed to check database configuration existence: " + name, e);
        }

        return false;
    }

    /**
     * Get count of database configurations
     */
    public int getCount() {
        logger.debug("Getting count of database configurations");

        String selectSql = "SELECT COUNT(*) FROM config_databases";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectSql);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                logger.debug("Database configuration count: {}", count);
                return count;
            }

        } catch (SQLException e) {
            logger.error("Failed to get database configuration count", e);
            throw new RuntimeException("Failed to get database configuration count", e);
        }

        return 0;
    }

    /**
     * Map ResultSet to DatabaseConfig object
     */
    private DatabaseConfig mapResultSetToConfig(ResultSet resultSet) throws SQLException {
        DatabaseConfig config = new DatabaseConfig();

        config.setName(resultSet.getString("name"));
        config.setDescription(resultSet.getString("description"));
        config.setUrl(resultSet.getString("url"));
        config.setUsername(resultSet.getString("username"));
        config.setPassword(resultSet.getString("password"));
        config.setDriver(resultSet.getString("driver"));

        // Create pool configuration
        DatabaseConfig.PoolConfig poolConfig = new DatabaseConfig.PoolConfig();
        poolConfig.setMaximumPoolSize(resultSet.getInt("maximum_pool_size"));
        poolConfig.setMinimumIdle(resultSet.getInt("minimum_idle"));
        poolConfig.setConnectionTimeout(resultSet.getLong("connection_timeout"));
        poolConfig.setIdleTimeout(resultSet.getLong("idle_timeout"));
        poolConfig.setMaxLifetime(resultSet.getLong("max_lifetime"));
        poolConfig.setLeakDetectionThreshold(resultSet.getLong("leak_detection_threshold"));
        poolConfig.setConnectionTestQuery(resultSet.getString("connection_test_query"));

        config.setPool(poolConfig);

        return config;
    }

    /**
     * Set prepared statement parameters from DatabaseConfig
     */
    private void setStatementParameters(PreparedStatement statement, String key, DatabaseConfig config) throws SQLException {
        statement.setString(1, key);
        statement.setString(2, config.getDescription());
        statement.setString(3, config.getUrl());
        statement.setString(4, config.getUsername());
        statement.setString(5, config.getPassword());
        statement.setString(6, config.getDriver());

        // Pool configuration parameters
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
            // Use default values if pool config is null
            statement.setInt(7, 10);
            statement.setInt(8, 2);
            statement.setLong(9, 30000);
            statement.setLong(10, 600000);
            statement.setLong(11, 1800000);
            statement.setLong(12, 60000);
            statement.setString(13, "SELECT 1");
        }
    }
}

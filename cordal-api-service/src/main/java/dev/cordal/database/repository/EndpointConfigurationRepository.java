package dev.cordal.database.repository;

import dev.cordal.database.DatabaseManager;
import dev.cordal.generic.config.ApiEndpointConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.*;
import java.util.*;

/**
 * Repository for managing endpoint configurations stored in H2 database
 * Note: Currently only stores basic endpoint information. Complex nested structures
 * (pagination, parameters, response) are not stored in the database table.
 */
@Singleton
public class EndpointConfigurationRepository {
    private static final Logger logger = LoggerFactory.getLogger(EndpointConfigurationRepository.class);

    private final DatabaseManager databaseManager;

    @Inject
    public EndpointConfigurationRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        logger.info("Endpoint configuration repository initialized");
    }

    /**
     * Load all endpoint configurations from the database
     */
    public Map<String, ApiEndpointConfig> loadAll() {
        logger.info("Loading all endpoint configurations from database");
        Map<String, ApiEndpointConfig> configurations = new HashMap<>();

        String selectSql = """
            SELECT name, description, path, method, query_name, response_format,
                   cache_enabled, cache_ttl_seconds, rate_limit_enabled,
                   rate_limit_requests, rate_limit_window_seconds
            FROM config_endpoints
            ORDER BY name
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectSql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                ApiEndpointConfig config = mapResultSetToConfig(resultSet);
                configurations.put(resultSet.getString("name"), config);
                logger.debug("Loaded endpoint configuration: {}", resultSet.getString("name"));
            }

            logger.info("Loaded {} endpoint configurations from database", configurations.size());
            return configurations;

        } catch (SQLException e) {
            logger.error("Failed to load endpoint configurations from database", e);
            throw new RuntimeException("Failed to load endpoint configurations", e);
        }
    }

    /**
     * Load a specific endpoint configuration by name
     */
    public Optional<ApiEndpointConfig> loadByName(String name) {
        logger.debug("Loading endpoint configuration by name: {}", name);

        String selectSql = """
            SELECT name, description, path, method, query_name, response_format,
                   cache_enabled, cache_ttl_seconds, rate_limit_enabled,
                   rate_limit_requests, rate_limit_window_seconds
            FROM config_endpoints
            WHERE name = ?
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectSql)) {

            statement.setString(1, name);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    ApiEndpointConfig config = mapResultSetToConfig(resultSet);
                    logger.debug("Found endpoint configuration: {}", name);
                    return Optional.of(config);
                } else {
                    logger.debug("Endpoint configuration not found: {}", name);
                    return Optional.empty();
                }
            }

        } catch (SQLException e) {
            logger.error("Failed to load endpoint configuration by name: {}", name, e);
            throw new RuntimeException("Failed to load endpoint configuration: " + name, e);
        }
    }

    /**
     * Load endpoint configurations by query name
     */
    public List<ApiEndpointConfig> loadByQuery(String queryName) {
        logger.debug("Loading endpoint configurations by query: {}", queryName);
        List<ApiEndpointConfig> configurations = new ArrayList<>();

        String selectSql = """
            SELECT name, description, path, method, query_name, response_format,
                   cache_enabled, cache_ttl_seconds, rate_limit_enabled,
                   rate_limit_requests, rate_limit_window_seconds
            FROM config_endpoints
            WHERE query_name = ?
            ORDER BY name
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectSql)) {

            statement.setString(1, queryName);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ApiEndpointConfig config = mapResultSetToConfig(resultSet);
                    configurations.add(config);
                    logger.debug("Loaded endpoint configuration: {} for query: {}", resultSet.getString("name"), queryName);
                }
            }

            logger.debug("Loaded {} endpoint configurations for query: {}", configurations.size(), queryName);
            return configurations;

        } catch (SQLException e) {
            logger.error("Failed to load endpoint configurations by query: {}", queryName, e);
            throw new RuntimeException("Failed to load endpoint configurations for query: " + queryName, e);
        }
    }

    /**
     * Load endpoint configurations by path
     */
    public List<ApiEndpointConfig> loadByPath(String path) {
        logger.debug("Loading endpoint configurations by path: {}", path);
        List<ApiEndpointConfig> configurations = new ArrayList<>();

        String selectSql = """
            SELECT name, description, path, method, query_name, response_format,
                   cache_enabled, cache_ttl_seconds, rate_limit_enabled,
                   rate_limit_requests, rate_limit_window_seconds
            FROM config_endpoints
            WHERE path = ?
            ORDER BY name
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectSql)) {

            statement.setString(1, path);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ApiEndpointConfig config = mapResultSetToConfig(resultSet);
                    configurations.add(config);
                    logger.debug("Loaded endpoint configuration: {} for path: {}", resultSet.getString("name"), path);
                }
            }

            logger.debug("Loaded {} endpoint configurations for path: {}", configurations.size(), path);
            return configurations;

        } catch (SQLException e) {
            logger.error("Failed to load endpoint configurations by path: {}", path, e);
            throw new RuntimeException("Failed to load endpoint configurations for path: " + path, e);
        }
    }

    /**
     * Save an endpoint configuration to the database
     */
    public void save(String key, ApiEndpointConfig config) {
        logger.debug("Saving endpoint configuration: {}", key);

        String mergeSql = """
            MERGE INTO config_endpoints (name, description, path, method, query_name, response_format,
                                        cache_enabled, cache_ttl_seconds, rate_limit_enabled,
                                        rate_limit_requests, rate_limit_window_seconds)
            KEY(name)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(mergeSql)) {

            setStatementParameters(statement, key, config);
            int rowsAffected = statement.executeUpdate();

            if (rowsAffected > 0) {
                logger.debug("Successfully saved endpoint configuration: {}", key);
            } else {
                logger.warn("No rows affected when saving endpoint configuration: {}", key);
            }

        } catch (SQLException e) {
            logger.error("Failed to save endpoint configuration: {}", key, e);
            throw new RuntimeException("Failed to save endpoint configuration: " + key, e);
        }
    }

    /**
     * Delete an endpoint configuration by name
     */
    public boolean delete(String name) {
        logger.debug("Deleting endpoint configuration: {}", name);

        String deleteSql = "DELETE FROM config_endpoints WHERE name = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(deleteSql)) {

            statement.setString(1, name);
            int rowsAffected = statement.executeUpdate();

            if (rowsAffected > 0) {
                logger.debug("Successfully deleted endpoint configuration: {}", name);
                return true;
            } else {
                logger.debug("Endpoint configuration not found for deletion: {}", name);
                return false;
            }

        } catch (SQLException e) {
            logger.error("Failed to delete endpoint configuration: {}", name, e);
            throw new RuntimeException("Failed to delete endpoint configuration: " + name, e);
        }
    }

    /**
     * Check if an endpoint configuration exists
     */
    public boolean exists(String name) {
        logger.debug("Checking if endpoint configuration exists: {}", name);

        String selectSql = "SELECT COUNT(*) FROM config_endpoints WHERE name = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectSql)) {

            statement.setString(1, name);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    boolean exists = resultSet.getInt(1) > 0;
                    logger.debug("Endpoint configuration exists: {} = {}", name, exists);
                    return exists;
                }
            }

        } catch (SQLException e) {
            logger.error("Failed to check if endpoint configuration exists: {}", name, e);
            throw new RuntimeException("Failed to check endpoint configuration existence: " + name, e);
        }

        return false;
    }

    /**
     * Get count of endpoint configurations
     */
    public int getCount() {
        logger.debug("Getting count of endpoint configurations");

        String selectSql = "SELECT COUNT(*) FROM config_endpoints";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectSql);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                logger.debug("Endpoint configuration count: {}", count);
                return count;
            }

        } catch (SQLException e) {
            logger.error("Failed to get endpoint configuration count", e);
            throw new RuntimeException("Failed to get endpoint configuration count", e);
        }

        return 0;
    }

    /**
     * Get count of endpoint configurations by query
     */
    public int getCountByQuery(String queryName) {
        logger.debug("Getting count of endpoint configurations for query: {}", queryName);

        String selectSql = "SELECT COUNT(*) FROM config_endpoints WHERE query_name = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectSql)) {

            statement.setString(1, queryName);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    logger.debug("Endpoint configuration count for query {}: {}", queryName, count);
                    return count;
                }
            }

        } catch (SQLException e) {
            logger.error("Failed to get endpoint configuration count for query: {}", queryName, e);
            throw new RuntimeException("Failed to get endpoint configuration count for query: " + queryName, e);
        }

        return 0;
    }

    /**
     * Map ResultSet to ApiEndpointConfig object
     */
    private ApiEndpointConfig mapResultSetToConfig(ResultSet resultSet) throws SQLException {
        ApiEndpointConfig config = new ApiEndpointConfig();

        config.setPath(resultSet.getString("path"));
        config.setMethod(resultSet.getString("method"));
        config.setDescription(resultSet.getString("description"));
        config.setQuery(resultSet.getString("query_name"));

        // Note: The database table stores additional fields (response_format, cache_*, rate_limit_*)
        // but the ApiEndpointConfig model doesn't have corresponding fields for these.
        // Complex nested structures (pagination, parameters, response) are not stored in the database.
        // This is consistent with the current ConfigurationDataLoader implementation.

        // Initialize empty collections for complex structures
        config.setParameters(new ArrayList<>());

        return config;
    }

    /**
     * Set prepared statement parameters from ApiEndpointConfig
     */
    private void setStatementParameters(PreparedStatement statement, String key, ApiEndpointConfig config) throws SQLException {
        statement.setString(1, key);
        statement.setString(2, config.getDescription());
        statement.setString(3, config.getPath());
        statement.setString(4, config.getMethod());
        statement.setString(5, config.getQuery());

        // Use default values for fields not present in ApiEndpointConfig model
        // This is consistent with the current ConfigurationDataLoader implementation
        statement.setString(6, "json"); // Default response format
        statement.setBoolean(7, false); // Default cache disabled
        statement.setInt(8, 300); // Default cache TTL
        statement.setBoolean(9, false); // Default rate limit disabled
        statement.setInt(10, 100); // Default rate limit requests
        statement.setInt(11, 60); // Default rate limit window
    }
}

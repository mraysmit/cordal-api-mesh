package dev.mars.database.repository;

import dev.mars.database.DatabaseManager;
import dev.mars.generic.config.QueryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.*;
import java.util.*;

/**
 * Repository for managing query configurations stored in H2 database
 */
@Singleton
public class QueryConfigurationRepository {
    private static final Logger logger = LoggerFactory.getLogger(QueryConfigurationRepository.class);

    private final DatabaseManager databaseManager;

    @Inject
    public QueryConfigurationRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        logger.info("Query configuration repository initialized");
    }

    /**
     * Load all query configurations from the database
     */
    public Map<String, QueryConfig> loadAll() {
        logger.info("Loading all query configurations from database");
        Map<String, QueryConfig> configurations = new HashMap<>();

        String selectSql = """
            SELECT name, description, database_name, sql_query, query_type, timeout_seconds
            FROM config_queries
            ORDER BY name
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectSql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                QueryConfig config = mapResultSetToConfig(resultSet);
                configurations.put(config.getName(), config);
                logger.debug("Loaded query configuration: {}", config.getName());
            }

            logger.info("Loaded {} query configurations from database", configurations.size());
            return configurations;

        } catch (SQLException e) {
            logger.error("Failed to load query configurations from database", e);
            throw new RuntimeException("Failed to load query configurations", e);
        }
    }

    /**
     * Load a specific query configuration by name
     */
    public Optional<QueryConfig> loadByName(String name) {
        logger.debug("Loading query configuration by name: {}", name);

        String selectSql = """
            SELECT name, description, database_name, sql_query, query_type, timeout_seconds
            FROM config_queries
            WHERE name = ?
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectSql)) {

            statement.setString(1, name);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    QueryConfig config = mapResultSetToConfig(resultSet);
                    logger.debug("Found query configuration: {}", name);
                    return Optional.of(config);
                } else {
                    logger.debug("Query configuration not found: {}", name);
                    return Optional.empty();
                }
            }

        } catch (SQLException e) {
            logger.error("Failed to load query configuration by name: {}", name, e);
            throw new RuntimeException("Failed to load query configuration: " + name, e);
        }
    }

    /**
     * Load query configurations by database name
     */
    public List<QueryConfig> loadByDatabase(String databaseName) {
        logger.debug("Loading query configurations by database: {}", databaseName);
        List<QueryConfig> configurations = new ArrayList<>();

        String selectSql = """
            SELECT name, description, database_name, sql_query, query_type, timeout_seconds
            FROM config_queries
            WHERE database_name = ?
            ORDER BY name
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectSql)) {

            statement.setString(1, databaseName);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    QueryConfig config = mapResultSetToConfig(resultSet);
                    configurations.add(config);
                    logger.debug("Loaded query configuration: {} for database: {}", config.getName(), databaseName);
                }
            }

            logger.debug("Loaded {} query configurations for database: {}", configurations.size(), databaseName);
            return configurations;

        } catch (SQLException e) {
            logger.error("Failed to load query configurations by database: {}", databaseName, e);
            throw new RuntimeException("Failed to load query configurations for database: " + databaseName, e);
        }
    }

    /**
     * Save a query configuration to the database
     */
    public void save(String key, QueryConfig config) {
        logger.debug("Saving query configuration: {}", key);

        String mergeSql = """
            MERGE INTO config_queries (name, description, database_name, sql_query, query_type, timeout_seconds)
            KEY(name)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(mergeSql)) {

            setStatementParameters(statement, key, config);
            int rowsAffected = statement.executeUpdate();

            if (rowsAffected > 0) {
                logger.debug("Successfully saved query configuration: {}", key);
            } else {
                logger.warn("No rows affected when saving query configuration: {}", key);
            }

        } catch (SQLException e) {
            logger.error("Failed to save query configuration: {}", key, e);
            throw new RuntimeException("Failed to save query configuration: " + key, e);
        }
    }

    /**
     * Delete a query configuration by name
     */
    public boolean delete(String name) {
        logger.debug("Deleting query configuration: {}", name);

        String deleteSql = "DELETE FROM config_queries WHERE name = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(deleteSql)) {

            statement.setString(1, name);
            int rowsAffected = statement.executeUpdate();

            if (rowsAffected > 0) {
                logger.debug("Successfully deleted query configuration: {}", name);
                return true;
            } else {
                logger.debug("Query configuration not found for deletion: {}", name);
                return false;
            }

        } catch (SQLException e) {
            logger.error("Failed to delete query configuration: {}", name, e);
            throw new RuntimeException("Failed to delete query configuration: " + name, e);
        }
    }

    /**
     * Check if a query configuration exists
     */
    public boolean exists(String name) {
        logger.debug("Checking if query configuration exists: {}", name);

        String selectSql = "SELECT COUNT(*) FROM config_queries WHERE name = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectSql)) {

            statement.setString(1, name);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    boolean exists = resultSet.getInt(1) > 0;
                    logger.debug("Query configuration exists: {} = {}", name, exists);
                    return exists;
                }
            }

        } catch (SQLException e) {
            logger.error("Failed to check if query configuration exists: {}", name, e);
            throw new RuntimeException("Failed to check query configuration existence: " + name, e);
        }

        return false;
    }

    /**
     * Get count of query configurations
     */
    public int getCount() {
        logger.debug("Getting count of query configurations");

        String selectSql = "SELECT COUNT(*) FROM config_queries";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectSql);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                logger.debug("Query configuration count: {}", count);
                return count;
            }

        } catch (SQLException e) {
            logger.error("Failed to get query configuration count", e);
            throw new RuntimeException("Failed to get query configuration count", e);
        }

        return 0;
    }

    /**
     * Get count of query configurations by database
     */
    public int getCountByDatabase(String databaseName) {
        logger.debug("Getting count of query configurations for database: {}", databaseName);

        String selectSql = "SELECT COUNT(*) FROM config_queries WHERE database_name = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectSql)) {

            statement.setString(1, databaseName);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    logger.debug("Query configuration count for database {}: {}", databaseName, count);
                    return count;
                }
            }

        } catch (SQLException e) {
            logger.error("Failed to get query configuration count for database: {}", databaseName, e);
            throw new RuntimeException("Failed to get query configuration count for database: " + databaseName, e);
        }

        return 0;
    }

    /**
     * Map ResultSet to QueryConfig object
     */
    private QueryConfig mapResultSetToConfig(ResultSet resultSet) throws SQLException {
        QueryConfig config = new QueryConfig();

        config.setName(resultSet.getString("name"));
        config.setDescription(resultSet.getString("description"));
        config.setDatabase(resultSet.getString("database_name"));
        config.setSql(resultSet.getString("sql_query"));

        // Note: QueryConfig doesn't have queryType and timeoutSeconds fields in the current model
        // These are stored in the database but not mapped to the config object
        // This is consistent with the current ConfigurationDataLoader implementation

        // Parameters are not stored in the database table currently
        // They are defined in YAML and loaded separately
        config.setParameters(new ArrayList<>());

        return config;
    }

    /**
     * Set prepared statement parameters from QueryConfig
     */
    private void setStatementParameters(PreparedStatement statement, String key, QueryConfig config) throws SQLException {
        statement.setString(1, key);
        statement.setString(2, config.getDescription());
        statement.setString(3, config.getDatabase());
        statement.setString(4, config.getSql());
        statement.setString(5, "SELECT"); // Default query type since QueryConfig doesn't have this field
        statement.setInt(6, 30); // Default timeout since QueryConfig doesn't have this field
    }
}

package dev.mars.config;

import dev.mars.database.ConfigurationDataLoader;
import dev.mars.database.DatabaseManager;
import dev.mars.generic.config.DatabaseConfig;
import dev.mars.generic.config.QueryConfig;
import dev.mars.generic.config.ApiEndpointConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests for loading configurations from database source
 */
public class DatabaseConfigurationLoaderTest {

    private DatabaseManager databaseManager;
    private ConfigurationDataLoader configurationDataLoader;
    private TestGenericApiConfig genericApiConfig;

    @BeforeEach
    void setUp() {
        // Use test configuration
        System.setProperty("generic.config.file", "application-test.yml");

        // Create test config with database source
        genericApiConfig = new TestGenericApiConfig("database");
        databaseManager = new DatabaseManager(genericApiConfig);
        
        // Initialize schema
        databaseManager.initializeSchema();
        databaseManager.cleanDatabase();
        
        configurationDataLoader = new ConfigurationDataLoader(databaseManager, genericApiConfig);
        
        // Load sample data for testing
        configurationDataLoader.loadSampleConfigurationDataIfNeeded();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("generic.config.file");
    }

    @Test
    void testLoadDatabaseConfigurationsFromDatabase() {
        // Act - simulate loading database configurations from database
        Map<String, DatabaseConfig> databaseConfigs = loadDatabaseConfigurationsFromDatabase();

        // Assert
        assertThat(databaseConfigs).isNotEmpty();
        assertThat(databaseConfigs).hasSize(2); // api-service-config-db and metrics-db
        
        // Verify api-service-config-db
        DatabaseConfig apiServiceConfig = databaseConfigs.get("api-service-config-db");
        assertThat(apiServiceConfig).isNotNull();
        assertThat(apiServiceConfig.getName()).isEqualTo("api-service-config-db");
        assertThat(apiServiceConfig.getDescription()).contains("API service configuration");
        assertThat(apiServiceConfig.getUrl()).contains("api-service-config");
        assertThat(apiServiceConfig.getDriver()).isEqualTo("org.h2.Driver");
        
        // Verify metrics-db
        DatabaseConfig metricsConfig = databaseConfigs.get("metrics-db");
        assertThat(metricsConfig).isNotNull();
        assertThat(metricsConfig.getName()).isEqualTo("metrics-db");
        assertThat(metricsConfig.getDescription()).contains("metrics");
        assertThat(metricsConfig.getUrl()).contains("metrics");
    }

    @Test
    void testLoadQueryConfigurationsFromDatabase() {
        // Act - simulate loading query configurations from database
        Map<String, QueryConfig> queryConfigs = loadQueryConfigurationsFromDatabase();

        // Assert
        assertThat(queryConfigs).isNotEmpty();
        assertThat(queryConfigs).hasSize(3); // get-all-databases, get-all-queries, get-all-endpoints
        
        // Verify get-all-databases query
        QueryConfig getAllDatabases = queryConfigs.get("get-all-databases");
        assertThat(getAllDatabases).isNotNull();
        assertThat(getAllDatabases.getName()).isEqualTo("get-all-databases");
        assertThat(getAllDatabases.getDatabase()).isEqualTo("api-service-config-db");
        assertThat(getAllDatabases.getSql()).contains("SELECT * FROM config_databases");
        
        // Verify get-all-queries query
        QueryConfig getAllQueries = queryConfigs.get("get-all-queries");
        assertThat(getAllQueries).isNotNull();
        assertThat(getAllQueries.getName()).isEqualTo("get-all-queries");
        assertThat(getAllQueries.getDatabase()).isEqualTo("api-service-config-db");
        assertThat(getAllQueries.getSql()).contains("SELECT * FROM config_queries");
    }

    @Test
    void testLoadEndpointConfigurationsFromDatabase() {
        // Act - simulate loading endpoint configurations from database
        Map<String, ApiEndpointConfig> endpointConfigs = loadEndpointConfigurationsFromDatabase();

        // Assert
        assertThat(endpointConfigs).isNotEmpty();
        assertThat(endpointConfigs).hasSize(3); // list-databases, list-queries, list-endpoints
        
        // Verify list-databases endpoint
        ApiEndpointConfig listDatabases = endpointConfigs.get("list-databases");
        assertThat(listDatabases).isNotNull();
        assertThat(listDatabases.getPath()).isEqualTo("/api/config/databases");
        assertThat(listDatabases.getMethod()).isEqualTo("GET");
        assertThat(listDatabases.getQuery()).isEqualTo("get-all-databases");

        // Verify list-queries endpoint
        ApiEndpointConfig listQueries = endpointConfigs.get("list-queries");
        assertThat(listQueries).isNotNull();
        assertThat(listQueries.getPath()).isEqualTo("/api/config/queries");
        assertThat(listQueries.getQuery()).isEqualTo("get-all-queries");
    }

    @Test
    void testDatabaseConfigurationIntegrity() {
        // Load all configurations
        Map<String, DatabaseConfig> databaseConfigs = loadDatabaseConfigurationsFromDatabase();
        Map<String, QueryConfig> queryConfigs = loadQueryConfigurationsFromDatabase();
        Map<String, ApiEndpointConfig> endpointConfigs = loadEndpointConfigurationsFromDatabase();

        // Verify referential integrity
        for (QueryConfig queryConfig : queryConfigs.values()) {
            String databaseName = queryConfig.getDatabase();
            assertThat(databaseConfigs).containsKey(databaseName);
        }

        for (ApiEndpointConfig endpointConfig : endpointConfigs.values()) {
            String queryName = endpointConfig.getQuery();
            assertThat(queryConfigs).containsKey(queryName);
        }
    }

    @Test
    void testConfigurationSourceSelection() {
        // Test that config source is correctly identified
        assertThat(genericApiConfig.getConfigSource()).isEqualTo("database");
        
        // Test that data loader respects the config source
        TestGenericApiConfig yamlConfig = new TestGenericApiConfig("yaml");
        ConfigurationDataLoader yamlLoader = new ConfigurationDataLoader(databaseManager, yamlConfig);
        
        // Clean database
        databaseManager.cleanDatabase();
        
        // Try to load with yaml config - should not load anything
        yamlLoader.loadSampleConfigurationDataIfNeeded();
        
        // Verify no data was loaded
        Map<String, DatabaseConfig> configs = loadDatabaseConfigurationsFromDatabase();
        assertThat(configs).isEmpty();
    }

    // Helper methods to simulate configuration loading from database
    private Map<String, DatabaseConfig> loadDatabaseConfigurationsFromDatabase() {
        Map<String, DatabaseConfig> configs = new HashMap<>();
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT name, description, url, username, password, driver FROM config_databases");
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                DatabaseConfig config = new DatabaseConfig();
                config.setName(resultSet.getString("name"));
                config.setDescription(resultSet.getString("description"));
                config.setUrl(resultSet.getString("url"));
                config.setUsername(resultSet.getString("username"));
                config.setPassword(resultSet.getString("password"));
                config.setDriver(resultSet.getString("driver"));
                
                configs.put(config.getName(), config);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load database configurations", e);
        }
        
        return configs;
    }

    private Map<String, QueryConfig> loadQueryConfigurationsFromDatabase() {
        Map<String, QueryConfig> configs = new HashMap<>();
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT name, description, database_name, sql_query FROM config_queries");
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                QueryConfig config = new QueryConfig();
                config.setName(resultSet.getString("name"));
                config.setDescription(resultSet.getString("description"));
                config.setDatabase(resultSet.getString("database_name"));
                config.setSql(resultSet.getString("sql_query"));
                
                configs.put(config.getName(), config);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load query configurations", e);
        }
        
        return configs;
    }

    private Map<String, ApiEndpointConfig> loadEndpointConfigurationsFromDatabase() {
        Map<String, ApiEndpointConfig> configs = new HashMap<>();
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT name, description, path, method, query_name FROM config_endpoints");
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                ApiEndpointConfig config = new ApiEndpointConfig();
                String name = resultSet.getString("name");
                config.setDescription(resultSet.getString("description"));
                config.setPath(resultSet.getString("path"));
                config.setMethod(resultSet.getString("method"));
                config.setQuery(resultSet.getString("query_name"));

                configs.put(name, config);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load endpoint configurations", e);
        }
        
        return configs;
    }

    /**
     * Test implementation of GenericApiConfig that allows setting config source
     */
    private static class TestGenericApiConfig extends GenericApiConfig {
        private final String configSource;

        public TestGenericApiConfig(String configSource) {
            super();
            this.configSource = configSource;
        }

        @Override
        public String getConfigSource() {
            return configSource;
        }
    }
}

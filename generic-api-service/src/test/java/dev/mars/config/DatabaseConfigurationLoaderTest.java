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
        
        // Create ConfigurationLoader for the data loader
        dev.mars.generic.config.ConfigurationLoader configurationLoader = new dev.mars.generic.config.ConfigurationLoader(genericApiConfig);

        configurationDataLoader = new ConfigurationDataLoader(databaseManager, genericApiConfig, configurationLoader);

        // Load configuration data for testing
        configurationDataLoader.loadConfigurationDataIfNeeded();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("generic.config.file");
    }

    @Test
    void testLoadDatabaseConfigurationsFromDatabase() {
        // Act - simulate loading database configurations from database
        Map<String, DatabaseConfig> databaseConfigs = loadDatabaseConfigurationsFromDatabase();

        // Assert - now we have YAML data loaded
        assertThat(databaseConfigs).isNotEmpty();
        assertThat(databaseConfigs).hasSize(2); // stock-trades-db and metrics-db from YAML

        // Verify stock-trades-db
        DatabaseConfig stockTradesConfig = databaseConfigs.get("stock-trades-db");
        assertThat(stockTradesConfig).isNotNull();
        assertThat(stockTradesConfig.getName()).isEqualTo("stock-trades-db");
        assertThat(stockTradesConfig.getUrl()).contains("testdb");
        assertThat(stockTradesConfig.getDriver()).isEqualTo("org.h2.Driver");

        // Verify metrics-db is present (from YAML test data)
        DatabaseConfig metricsConfig = databaseConfigs.get("metrics-db");
        assertThat(metricsConfig).isNotNull();
        assertThat(metricsConfig.getName()).isEqualTo("metrics-db");
        assertThat(metricsConfig.getUrl()).contains("testmetricsdb");
    }

    @Test
    void testLoadQueryConfigurationsFromDatabase() {
        // Act - simulate loading query configurations from database
        Map<String, QueryConfig> queryConfigs = loadQueryConfigurationsFromDatabase();

        // Assert - now we have YAML data loaded
        assertThat(queryConfigs).isNotEmpty();
        assertThat(queryConfigs).hasSize(12); // All queries from test-queries.yml

        // Verify test-query
        QueryConfig testQuery = queryConfigs.get("test-query");
        assertThat(testQuery).isNotNull();
        assertThat(testQuery.getName()).isEqualTo("test-query");
        assertThat(testQuery.getDatabase()).isEqualTo("stock-trades-db");
        assertThat(testQuery.getSql()).contains("stock_trades");

        // Verify stock-trades-all query
        QueryConfig stockTradesAll = queryConfigs.get("stock-trades-all");
        assertThat(stockTradesAll).isNotNull();
        assertThat(stockTradesAll.getName()).isEqualTo("stock-trades-all");
        assertThat(stockTradesAll.getDatabase()).isEqualTo("stock-trades-db");
        assertThat(stockTradesAll.getSql()).contains("stock_trades");
    }

    @Test
    void testLoadEndpointConfigurationsFromDatabase() {
        // Act - simulate loading endpoint configurations from database
        Map<String, ApiEndpointConfig> endpointConfigs = loadEndpointConfigurationsFromDatabase();

        // Assert - now we have YAML data loaded
        assertThat(endpointConfigs).isNotEmpty();
        assertThat(endpointConfigs).hasSize(6); // All endpoints from test-api-endpoints.yml

        // Verify test-endpoint
        ApiEndpointConfig testEndpoint = endpointConfigs.get("test-endpoint");
        assertThat(testEndpoint).isNotNull();
        assertThat(testEndpoint.getPath()).isEqualTo("/api/test/endpoint");
        assertThat(testEndpoint.getMethod()).isEqualTo("GET");
        assertThat(testEndpoint.getQuery()).isEqualTo("test-query");

        // Verify stock-trades-list endpoint
        ApiEndpointConfig stockTradesList = endpointConfigs.get("stock-trades-list");
        assertThat(stockTradesList).isNotNull();
        assertThat(stockTradesList.getPath()).isEqualTo("/api/generic/stock-trades");
        assertThat(stockTradesList.getQuery()).isEqualTo("stock-trades-all");
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
        dev.mars.generic.config.ConfigurationLoader yamlConfigurationLoader = new dev.mars.generic.config.ConfigurationLoader(yamlConfig);
        ConfigurationDataLoader yamlLoader = new ConfigurationDataLoader(databaseManager, yamlConfig, yamlConfigurationLoader);

        // Clean database
        databaseManager.cleanDatabase();

        // Try to load with yaml config - should not load anything
        yamlLoader.loadConfigurationDataIfNeeded();
        
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

        @Override
        public boolean isLoadConfigFromYaml() {
            // Enable loading from YAML when config source is database for testing
            return "database".equals(configSource);
        }
    }
}

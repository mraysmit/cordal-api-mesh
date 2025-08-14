package dev.cordal.config;

import dev.cordal.database.ConfigurationDataLoader;
import dev.cordal.database.DatabaseManager;
import dev.cordal.generic.config.DatabaseConfig;
import dev.cordal.generic.config.QueryConfig;
import dev.cordal.generic.config.ApiEndpointConfig;
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

        // Create test config with database source that doesn't try to load from YAML
        genericApiConfig = new TestGenericApiConfig("database");
        databaseManager = new DatabaseManager(genericApiConfig);

        // Initialize schema
        databaseManager.initializeSchema();
        databaseManager.cleanDatabase();

        // Create ConfigurationLoader for the data loader
        dev.cordal.generic.config.ConfigurationLoader configurationLoader = new dev.cordal.generic.config.ConfigurationLoader(genericApiConfig);

        configurationDataLoader = new ConfigurationDataLoader(databaseManager, genericApiConfig, configurationLoader);

        // Populate database with test data for testing
        populateTestData();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("generic.config.file");
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    /**
     * Populate database with test configuration data
     */
    private void populateTestData() {
        try (var connection = databaseManager.getConnection();
             var statement = connection.createStatement()) {

            // Insert test database configuration
            String insertDatabase = """
                INSERT INTO config_databases (name, driver, url, username, password, maximum_pool_size, minimum_idle, connection_timeout, idle_timeout, max_lifetime, leak_detection_threshold, connection_test_query, created_at, updated_at)
                VALUES ('test-database', 'org.h2.Driver', 'jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE', 'sa', '', 10, 2, 30000, 600000, 1800000, 60000, 'SELECT 1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """;
            statement.execute(insertDatabase);

            // Insert test query configuration
            String insertQuery = """
                INSERT INTO config_queries (name, database_name, sql_query, description, created_at, updated_at)
                VALUES ('test-query', 'test-database', 'SELECT * FROM test_table', 'Test query description', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """;
            statement.execute(insertQuery);

            // Insert test endpoint configuration
            String insertEndpoint = """
                INSERT INTO config_endpoints (name, path, method, query_name, description, created_at, updated_at)
                VALUES ('test-endpoint', '/test-endpoint', 'GET', 'test-query', 'Test endpoint description', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """;
            statement.execute(insertEndpoint);

        } catch (Exception e) {
            throw new RuntimeException("Failed to populate test data", e);
        }
    }

    @Test
    void testLoadDatabaseConfigurationsFromDatabase() {
        // Act - simulate loading database configurations from database
        Map<String, DatabaseConfig> databaseConfigs = loadDatabaseConfigurationsFromDatabase();

        // Assert - we have test data loaded
        assertThat(databaseConfigs).isNotEmpty();
        assertThat(databaseConfigs).hasSize(1); // test-database from test data

        // Verify test-database
        DatabaseConfig testConfig = databaseConfigs.get("test-database");
        assertThat(testConfig).isNotNull();
        assertThat(testConfig.getName()).isEqualTo("test-database");
        assertThat(testConfig.getUrl()).contains("testdb");
        assertThat(testConfig.getDriver()).isEqualTo("org.h2.Driver");
    }

    @Test
    void testLoadQueryConfigurationsFromDatabase() {
        // Act - simulate loading query configurations from database
        Map<String, QueryConfig> queryConfigs = loadQueryConfigurationsFromDatabase();

        // Assert - we have test data loaded
        assertThat(queryConfigs).isNotEmpty();
        assertThat(queryConfigs).hasSize(1); // test-query from test data

        // Verify test-query
        QueryConfig testQuery = queryConfigs.get("test-query");
        assertThat(testQuery).isNotNull();
        assertThat(testQuery.getName()).isEqualTo("test-query");
        assertThat(testQuery.getDatabase()).isEqualTo("test-database");
        assertThat(testQuery.getSql()).contains("SELECT");
    }

    @Test
    void testLoadEndpointConfigurationsFromDatabase() {
        // Act - simulate loading endpoint configurations from database
        Map<String, ApiEndpointConfig> endpointConfigs = loadEndpointConfigurationsFromDatabase();

        // Assert - we have test data loaded
        assertThat(endpointConfigs).isNotEmpty();
        assertThat(endpointConfigs).hasSize(1); // test-endpoint from test data

        // Verify test-endpoint
        ApiEndpointConfig testEndpoint = endpointConfigs.get("test-endpoint");
        assertThat(testEndpoint).isNotNull();
        assertThat(testEndpoint.getPath()).isEqualTo("/test-endpoint");
        assertThat(testEndpoint.getMethod()).isEqualTo("GET");
        assertThat(testEndpoint.getQuery()).isEqualTo("test-query");
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
        dev.cordal.generic.config.ConfigurationLoader yamlConfigurationLoader = new dev.cordal.generic.config.ConfigurationLoader(yamlConfig);
        ConfigurationDataLoader yamlLoader = new ConfigurationDataLoader(databaseManager, yamlConfig, yamlConfigurationLoader);

        // Clean database
        databaseManager.cleanDatabase();

        // Try to load with yaml config - should not load anything (config source is yaml, not database)
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
            // Don't load from YAML by default in tests to avoid directory scanning issues
            return false;
        }
    }
}

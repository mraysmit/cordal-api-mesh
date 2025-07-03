package dev.mars.database;

import dev.mars.config.GenericApiConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration tests for database configuration source functionality
 */
public class DatabaseConfigurationSourceTest {

    private DatabaseManager databaseManager;
    private ConfigurationDataLoader configurationDataLoader;
    private GenericApiConfig genericApiConfig;

    @BeforeEach
    void setUp() {
        // Use test configuration with database source
        System.setProperty("generic.config.file", "application-database-test.yml");

        // Create components manually to avoid Guice module complexity in tests
        genericApiConfig = new GenericApiConfig();
        databaseManager = new DatabaseManager(genericApiConfig);
        
        // Initialize schema explicitly since we're not using the Guice module
        databaseManager.initializeSchema();
        
        // Clean database before each test
        databaseManager.cleanDatabase();
        
        configurationDataLoader = new ConfigurationDataLoader(databaseManager, genericApiConfig);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("generic.config.file");
    }

    @Test
    void testLoadConfigurationDataWhenSourceIsDatabase() {
        // Create a test configuration that sets config.source to database
        TestGenericApiConfig testConfig = new TestGenericApiConfig("database");
        ConfigurationDataLoader testLoader = new ConfigurationDataLoader(databaseManager, testConfig);
        
        // Act
        testLoader.loadSampleConfigurationDataIfNeeded();

        // Assert - verify database configurations were loaded
        assertThatCode(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM config_databases");
                 ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    assertThat(count).isGreaterThan(0);
                }
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void testDatabaseConfigurationsContent() {
        // Create a test configuration that sets config.source to database
        TestGenericApiConfig testConfig = new TestGenericApiConfig("database");
        ConfigurationDataLoader testLoader = new ConfigurationDataLoader(databaseManager, testConfig);
        
        // Load sample data
        testLoader.loadSampleConfigurationDataIfNeeded();

        // Verify specific database configurations
        assertThatCode(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                     "SELECT name, description, url, driver FROM config_databases ORDER BY name");
                 ResultSet resultSet = statement.executeQuery()) {

                boolean foundApiServiceConfig = false;

                while (resultSet.next()) {
                    String name = resultSet.getString("name");
                    String description = resultSet.getString("description");
                    String url = resultSet.getString("url");
                    String driver = resultSet.getString("driver");

                    if ("api-service-config-db".equals(name)) {
                        foundApiServiceConfig = true;
                        assertThat(description).contains("API service configuration");
                        assertThat(url).contains("api-service-config");
                        assertThat(driver).isEqualTo("org.h2.Driver");
                    }
                }

                assertThat(foundApiServiceConfig).isTrue();
                // metrics-db should NOT be present (it's managed by metrics-service)
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void testQueryConfigurationsContent() {
        // Create a test configuration that sets config.source to database
        TestGenericApiConfig testConfig = new TestGenericApiConfig("database");
        ConfigurationDataLoader testLoader = new ConfigurationDataLoader(databaseManager, testConfig);
        
        // Load sample data
        testLoader.loadSampleConfigurationDataIfNeeded();

        // Verify query configurations
        assertThatCode(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                     "SELECT name, database_name, sql_query, query_type FROM config_queries ORDER BY name");
                 ResultSet resultSet = statement.executeQuery()) {

                boolean foundGetAllDatabases = false;
                boolean foundGetAllQueries = false;
                boolean foundGetAllEndpoints = false;

                while (resultSet.next()) {
                    String name = resultSet.getString("name");
                    String databaseName = resultSet.getString("database_name");
                    String sqlQuery = resultSet.getString("sql_query");
                    String queryType = resultSet.getString("query_type");

                    if ("get-all-databases".equals(name)) {
                        foundGetAllDatabases = true;
                        assertThat(databaseName).isEqualTo("api-service-config-db");
                        assertThat(sqlQuery).contains("SELECT * FROM config_databases");
                        assertThat(queryType).isEqualTo("SELECT");
                    } else if ("get-all-queries".equals(name)) {
                        foundGetAllQueries = true;
                        assertThat(databaseName).isEqualTo("api-service-config-db");
                        assertThat(sqlQuery).contains("SELECT * FROM config_queries");
                    } else if ("get-all-endpoints".equals(name)) {
                        foundGetAllEndpoints = true;
                        assertThat(databaseName).isEqualTo("api-service-config-db");
                        assertThat(sqlQuery).contains("SELECT * FROM config_endpoints");
                    }
                }

                assertThat(foundGetAllDatabases).isTrue();
                assertThat(foundGetAllQueries).isTrue();
                assertThat(foundGetAllEndpoints).isTrue();
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void testEndpointConfigurationsContent() {
        // Create a test configuration that sets config.source to database
        TestGenericApiConfig testConfig = new TestGenericApiConfig("database");
        ConfigurationDataLoader testLoader = new ConfigurationDataLoader(databaseManager, testConfig);
        
        // Load sample data
        testLoader.loadSampleConfigurationDataIfNeeded();

        // Verify endpoint configurations
        assertThatCode(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                     "SELECT name, path, method, query_name, response_format FROM config_endpoints ORDER BY name");
                 ResultSet resultSet = statement.executeQuery()) {

                boolean foundListDatabases = false;
                boolean foundListQueries = false;
                boolean foundListEndpoints = false;

                while (resultSet.next()) {
                    String name = resultSet.getString("name");
                    String path = resultSet.getString("path");
                    String method = resultSet.getString("method");
                    String queryName = resultSet.getString("query_name");
                    String responseFormat = resultSet.getString("response_format");

                    if ("list-databases".equals(name)) {
                        foundListDatabases = true;
                        assertThat(path).isEqualTo("/api/config/databases");
                        assertThat(method).isEqualTo("GET");
                        assertThat(queryName).isEqualTo("get-all-databases");
                        assertThat(responseFormat).isEqualTo("json");
                    } else if ("list-queries".equals(name)) {
                        foundListQueries = true;
                        assertThat(path).isEqualTo("/api/config/queries");
                        assertThat(queryName).isEqualTo("get-all-queries");
                    } else if ("list-endpoints".equals(name)) {
                        foundListEndpoints = true;
                        assertThat(path).isEqualTo("/api/config/endpoints");
                        assertThat(queryName).isEqualTo("get-all-endpoints");
                    }
                }

                assertThat(foundListDatabases).isTrue();
                assertThat(foundListQueries).isTrue();
                assertThat(foundListEndpoints).isTrue();
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void testNoDataLoadedWhenSourceIsYaml() {
        // Create a fresh database manager and config with YAML source
        TestGenericApiConfig yamlConfig = new TestGenericApiConfig("yaml");
        DatabaseManager freshDatabaseManager = new DatabaseManager(yamlConfig);
        freshDatabaseManager.initializeSchema();
        freshDatabaseManager.cleanDatabase();

        ConfigurationDataLoader yamlDataLoader = new ConfigurationDataLoader(freshDatabaseManager, yamlConfig);

        // Use YAML config which should not load data to database
        yamlDataLoader.loadSampleConfigurationDataIfNeeded();

        // Assert - verify no data was loaded
        assertThatCode(() -> {
            try (Connection connection = freshDatabaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM config_databases");
                 ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    assertThat(count).isEqualTo(0);
                }
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void testDataNotLoadedTwice() {
        // Create a test configuration that sets config.source to database
        TestGenericApiConfig testConfig = new TestGenericApiConfig("database");
        ConfigurationDataLoader testLoader = new ConfigurationDataLoader(databaseManager, testConfig);
        
        // Load data first time
        testLoader.loadSampleConfigurationDataIfNeeded();
        
        // Get count after first load
        int firstCount = getConfigDatabasesCount();
        
        // Load data second time
        testLoader.loadSampleConfigurationDataIfNeeded();
        
        // Get count after second load
        int secondCount = getConfigDatabasesCount();
        
        // Assert counts are the same (no duplicate data)
        assertThat(secondCount).isEqualTo(firstCount);
    }

    private int getConfigDatabasesCount() {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM config_databases");
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 0;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get count", e);
        }
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

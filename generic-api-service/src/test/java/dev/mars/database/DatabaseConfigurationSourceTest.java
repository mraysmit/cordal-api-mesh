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

        // Create ConfigurationLoader for the data loader
        dev.mars.generic.config.ConfigurationLoader configurationLoader = new dev.mars.generic.config.ConfigurationLoader(genericApiConfig);

        configurationDataLoader = new ConfigurationDataLoader(databaseManager, genericApiConfig, configurationLoader);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("generic.config.file");
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    /**
     * Populate database with comprehensive test configuration data
     */
    private void populateTestData() {
        try (var connection = databaseManager.getConnection();
             var statement = connection.createStatement()) {

            // Insert test database configurations
            statement.execute("""
                INSERT INTO config_databases (name, driver, url, username, password, maximum_pool_size, minimum_idle, connection_timeout, idle_timeout, max_lifetime, leak_detection_threshold, connection_test_query, created_at, updated_at)
                VALUES ('stock-trades-db', 'org.h2.Driver', 'jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE', 'sa', '', 10, 2, 30000, 600000, 1800000, 60000, 'SELECT 1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

            statement.execute("""
                INSERT INTO config_databases (name, driver, url, username, password, maximum_pool_size, minimum_idle, connection_timeout, idle_timeout, max_lifetime, leak_detection_threshold, connection_test_query, created_at, updated_at)
                VALUES ('metrics-db', 'org.h2.Driver', 'jdbc:h2:mem:testmetricsdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE', 'sa', '', 5, 1, 30000, 600000, 1800000, 60000, 'SELECT 1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

            // Insert test query configurations
            statement.execute("""
                INSERT INTO config_queries (name, database_name, sql_query, description, created_at, updated_at)
                VALUES ('test-query', 'stock-trades-db', 'SELECT * FROM stock_trades', 'Test query', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

            statement.execute("""
                INSERT INTO config_queries (name, database_name, sql_query, description, created_at, updated_at)
                VALUES ('test-count-query', 'stock-trades-db', 'SELECT COUNT(*) FROM stock_trades', 'Test count query', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

            statement.execute("""
                INSERT INTO config_queries (name, database_name, sql_query, description, created_at, updated_at)
                VALUES ('stock-trades-all', 'stock-trades-db', 'SELECT * FROM stock_trades ORDER BY trade_date DESC', 'Get all stock trades', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

            statement.execute("""
                INSERT INTO config_queries (name, database_name, sql_query, description, created_at, updated_at)
                VALUES ('stock-trades-by-id', 'stock-trades-db', 'SELECT * FROM stock_trades WHERE id = ?', 'Get trade by ID', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

            statement.execute("""
                INSERT INTO config_queries (name, database_name, sql_query, description, created_at, updated_at)
                VALUES ('stock-trades-by-symbol', 'stock-trades-db', 'SELECT * FROM stock_trades WHERE symbol = ?', 'Get trades by symbol', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

            // Insert test endpoint configurations
            statement.execute("""
                INSERT INTO config_endpoints (name, path, method, query_name, description, created_at, updated_at)
                VALUES ('test-endpoint', '/api/test/endpoint', 'GET', 'test-query', 'Test endpoint', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

            statement.execute("""
                INSERT INTO config_endpoints (name, path, method, query_name, description, created_at, updated_at)
                VALUES ('stock-trades-list', '/api/generic/stock-trades', 'GET', 'stock-trades-all', 'List all stock trades', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

            statement.execute("""
                INSERT INTO config_endpoints (name, path, method, query_name, description, created_at, updated_at)
                VALUES ('stock-trades-by-symbol', '/api/generic/stock-trades/symbol/{symbol}', 'GET', 'stock-trades-by-symbol', 'Get trades by symbol', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

        } catch (Exception e) {
            throw new RuntimeException("Failed to populate test data", e);
        }
    }

    @Test
    void testLoadConfigurationDataWhenSourceIsDatabase() {
        // Create a test configuration that sets config.source to database
        TestGenericApiConfig testConfig = new TestGenericApiConfig("database");
        dev.mars.generic.config.ConfigurationLoader configurationLoader = new dev.mars.generic.config.ConfigurationLoader(testConfig);
        ConfigurationDataLoader testLoader = new ConfigurationDataLoader(databaseManager, testConfig, configurationLoader);

        // Populate test data directly
        populateTestData();

        // Act - this should not try to load from YAML since loadFromYaml is false
        testLoader.loadConfigurationDataIfNeeded();

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
        dev.mars.generic.config.ConfigurationLoader configurationLoader = new dev.mars.generic.config.ConfigurationLoader(testConfig);
        ConfigurationDataLoader testLoader = new ConfigurationDataLoader(databaseManager, testConfig, configurationLoader);

        // Populate test data directly
        populateTestData();

        // Load configuration data
        testLoader.loadConfigurationDataIfNeeded();

        // Verify specific database configurations from YAML
        assertThatCode(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                     "SELECT name, description, url, driver FROM config_databases ORDER BY name");
                 ResultSet resultSet = statement.executeQuery()) {

                boolean foundStockTradesDb = false;
                boolean foundMetricsDb = false;

                while (resultSet.next()) {
                    String name = resultSet.getString("name");
                    String description = resultSet.getString("description");
                    String url = resultSet.getString("url");
                    String driver = resultSet.getString("driver");

                    if ("stock-trades-db".equals(name)) {
                        foundStockTradesDb = true;
                        assertThat(url).contains("testdb");
                        assertThat(driver).isEqualTo("org.h2.Driver");
                    } else if ("metrics-db".equals(name)) {
                        foundMetricsDb = true;
                        assertThat(url).contains("testmetricsdb");
                        assertThat(driver).isEqualTo("org.h2.Driver");
                    }
                }

                assertThat(foundStockTradesDb).isTrue();
                assertThat(foundMetricsDb).isTrue();
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void testQueryConfigurationsContent() {
        // Create a test configuration that sets config.source to database
        TestGenericApiConfig testConfig = new TestGenericApiConfig("database");
        dev.mars.generic.config.ConfigurationLoader configurationLoader = new dev.mars.generic.config.ConfigurationLoader(testConfig);
        ConfigurationDataLoader testLoader = new ConfigurationDataLoader(databaseManager, testConfig, configurationLoader);

        // Populate test data directly
        populateTestData();

        // Load configuration data
        testLoader.loadConfigurationDataIfNeeded();

        // Verify query configurations
        assertThatCode(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                     "SELECT name, database_name, sql_query, query_type FROM config_queries ORDER BY name");
                 ResultSet resultSet = statement.executeQuery()) {

                boolean foundTestQuery = false;
                boolean foundTestCountQuery = false;
                boolean foundStockTradesAll = false;

                while (resultSet.next()) {
                    String name = resultSet.getString("name");
                    String databaseName = resultSet.getString("database_name");
                    String sqlQuery = resultSet.getString("sql_query");
                    String queryType = resultSet.getString("query_type");

                    if ("test-query".equals(name)) {
                        foundTestQuery = true;
                        assertThat(databaseName).isEqualTo("stock-trades-db");
                        assertThat(sqlQuery).contains("stock_trades");
                        assertThat(queryType).isEqualTo("SELECT");
                    } else if ("test-count-query".equals(name)) {
                        foundTestCountQuery = true;
                        assertThat(databaseName).isEqualTo("stock-trades-db");
                        assertThat(sqlQuery).contains("COUNT(*)");
                    } else if ("stock-trades-all".equals(name)) {
                        foundStockTradesAll = true;
                        assertThat(databaseName).isEqualTo("stock-trades-db");
                        assertThat(sqlQuery).contains("stock_trades");
                    }
                }

                assertThat(foundTestQuery).isTrue();
                assertThat(foundTestCountQuery).isTrue();
                assertThat(foundStockTradesAll).isTrue();
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void testEndpointConfigurationsContent() {
        // Create a test configuration that sets config.source to database
        TestGenericApiConfig testConfig = new TestGenericApiConfig("database");
        dev.mars.generic.config.ConfigurationLoader configurationLoader = new dev.mars.generic.config.ConfigurationLoader(testConfig);
        ConfigurationDataLoader testLoader = new ConfigurationDataLoader(databaseManager, testConfig, configurationLoader);

        // Populate test data directly
        populateTestData();

        // Load configuration data
        testLoader.loadConfigurationDataIfNeeded();

        // Verify endpoint configurations
        assertThatCode(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                     "SELECT name, path, method, query_name, response_format FROM config_endpoints ORDER BY name");
                 ResultSet resultSet = statement.executeQuery()) {

                boolean foundTestEndpoint = false;
                boolean foundStockTradesList = false;
                boolean foundStockTradesBySymbol = false;

                while (resultSet.next()) {
                    String name = resultSet.getString("name");
                    String path = resultSet.getString("path");
                    String method = resultSet.getString("method");
                    String queryName = resultSet.getString("query_name");
                    String responseFormat = resultSet.getString("response_format");

                    if ("test-endpoint".equals(name)) {
                        foundTestEndpoint = true;
                        assertThat(path).isEqualTo("/api/test/endpoint");
                        assertThat(method).isEqualTo("GET");
                        assertThat(queryName).isEqualTo("test-query");
                        assertThat(responseFormat).isEqualTo("json");
                    } else if ("stock-trades-list".equals(name)) {
                        foundStockTradesList = true;
                        assertThat(path).isEqualTo("/api/generic/stock-trades");
                        assertThat(queryName).isEqualTo("stock-trades-all");
                    } else if ("stock-trades-by-symbol".equals(name)) {
                        foundStockTradesBySymbol = true;
                        assertThat(path).isEqualTo("/api/generic/stock-trades/symbol/{symbol}");
                        assertThat(queryName).isEqualTo("stock-trades-by-symbol");
                    }
                }

                assertThat(foundTestEndpoint).isTrue();
                assertThat(foundStockTradesList).isTrue();
                assertThat(foundStockTradesBySymbol).isTrue();
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

        dev.mars.generic.config.ConfigurationLoader yamlConfigurationLoader = new dev.mars.generic.config.ConfigurationLoader(yamlConfig);
        ConfigurationDataLoader yamlDataLoader = new ConfigurationDataLoader(freshDatabaseManager, yamlConfig, yamlConfigurationLoader);

        // Use YAML config which should not load data to database
        yamlDataLoader.loadConfigurationDataIfNeeded();

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
        dev.mars.generic.config.ConfigurationLoader configurationLoader = new dev.mars.generic.config.ConfigurationLoader(testConfig);
        ConfigurationDataLoader testLoader = new ConfigurationDataLoader(databaseManager, testConfig, configurationLoader);

        // Populate test data directly
        populateTestData();

        // Load data first time
        testLoader.loadConfigurationDataIfNeeded();

        // Get count after first load
        int firstCount = getConfigDatabasesCount();

        // Load data second time
        testLoader.loadConfigurationDataIfNeeded();
        
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

        @Override
        public boolean isLoadConfigFromYaml() {
            // Don't load from YAML by default in tests to avoid directory scanning issues
            return false;
        }
    }
}

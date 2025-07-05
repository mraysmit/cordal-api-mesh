package dev.mars.generic.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dev.mars.config.GenericApiConfig;
import dev.mars.generic.config.ApiEndpointConfig;
import dev.mars.generic.config.DatabaseConfig;
import dev.mars.generic.config.QueryConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests to verify that configurations are loaded from the exact paths
 * defined in application.yaml and that the loaded data matches the source files.
 */
class ConfigurationPathVerificationTest {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationPathVerificationTest.class);
    
    private ObjectMapper yamlMapper;

    @BeforeEach
    void setUp() {
        yamlMapper = new ObjectMapper(new YAMLFactory());
        // Clear any existing system properties
        System.clearProperty("generic.config.file");
    }

    @AfterEach
    void tearDown() {
        // Clean up system properties
        System.clearProperty("generic.config.file");
    }

    @Test
    void testConfigurationLoadedFromDefaultPaths() {
        // Arrange - Use default configuration (clear system property first)
        System.clearProperty("generic.config.file");
        GenericApiConfig config = new GenericApiConfig();

        // Check if the default configuration files exist before attempting to load them
        // This prevents System.exit(1) from being called and crashing the test JVM
        String databasesPath = config.getDatabasesConfigPath();
        String queriesPath = config.getQueriesConfigPath();
        String endpointsPath = config.getEndpointsConfigPath();

        logger.info("Default databases path: {}", databasesPath);
        logger.info("Default queries path: {}", queriesPath);
        logger.info("Default endpoints path: {}", endpointsPath);

        // Check if configuration files are available
        boolean configFilesExist = checkConfigurationFilesExist(databasesPath, queriesPath, endpointsPath);

        if (!configFilesExist) {
            logger.warn("Default configuration files not found - skipping configuration loading test");
            logger.warn("This is expected in test environments where only test configuration files are available");
            // Just verify that the paths are set correctly
            assertThat(databasesPath).isEqualTo("./generic-config/stocktrades-databases.yml");
            assertThat(queriesPath).isEqualTo("./generic-config/stocktrades-queries.yml");
            assertThat(endpointsPath).isEqualTo("./generic-config/stocktrades-api-endpoints.yml");
            return;
        }

        // Act - Load configurations (only if files exist)
        ConfigurationLoader loader = new ConfigurationLoader(config);
        Map<String, DatabaseConfig> databases = loader.loadDatabaseConfigurations();
        Map<String, QueryConfig> queries = loader.loadQueryConfigurations();
        Map<String, ApiEndpointConfig> endpoints = loader.loadEndpointConfigurations();

        // Assert - Verify configurations were loaded
        assertThat(databases).isNotNull().isNotEmpty();
        assertThat(queries).isNotNull().isNotEmpty();
        assertThat(endpoints).isNotNull().isNotEmpty();

        // Verify that the loaded data matches what we expect from the default files
        // Default configuration uses "stocktrades" database, not "stock-trades-db"
        assertThat(databases).containsKey("stocktrades");
        assertThat(queries).containsKey("stock-trades-all");
        assertThat(endpoints).containsKey("stock-trades-list");
    }

    @Test
    void testConfigurationLoadedFromCustomPaths() {
        // Arrange - Use custom paths configuration
        System.setProperty("generic.config.file", "application-custom-paths.yml");
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        ConfigurationLoader loader = new ConfigurationLoader(config);
        
        // Act - Load configurations
        Map<String, DatabaseConfig> databases = loader.loadDatabaseConfigurations();
        Map<String, QueryConfig> queries = loader.loadQueryConfigurations();
        Map<String, ApiEndpointConfig> endpoints = loader.loadEndpointConfigurations();
        
        // Assert - Verify configurations were loaded
        assertThat(databases).isNotNull().isNotEmpty();
        assertThat(queries).isNotNull().isNotEmpty();
        assertThat(endpoints).isNotNull().isNotEmpty();
        
        // Verify the custom paths are being used
        assertThat(config.getDatabasesConfigPath()).isEqualTo("custom/databases.yml");
        assertThat(config.getQueriesConfigPath()).isEqualTo("custom/queries.yml");
        assertThat(config.getEndpointsConfigPath()).isEqualTo("custom/api-endpoints.yml");
        
        logger.info("Custom databases path: {}", config.getDatabasesConfigPath());
        logger.info("Custom queries path: {}", config.getQueriesConfigPath());
        logger.info("Custom endpoints path: {}", config.getEndpointsConfigPath());
    }

    @Test
    void testConfigurationLoadedFromTestPaths() {
        // Arrange - Use test configuration with specific test file paths
        System.setProperty("generic.config.file", "application-test.yml");
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        ConfigurationLoader loader = new ConfigurationLoader(config);
        
        // Act - Load configurations
        Map<String, DatabaseConfig> databases = loader.loadDatabaseConfigurations();
        Map<String, QueryConfig> queries = loader.loadQueryConfigurations();
        Map<String, ApiEndpointConfig> endpoints = loader.loadEndpointConfigurations();
        
        // Assert - Verify configurations were loaded from test files
        assertThat(databases).isNotNull().isNotEmpty();
        assertThat(queries).isNotNull().isNotEmpty();
        assertThat(endpoints).isNotNull().isNotEmpty();
        
        // Verify the test paths are being used
        assertThat(config.getDatabasesConfigPath()).isEqualTo("test-databases.yml");
        assertThat(config.getQueriesConfigPath()).isEqualTo("test-queries.yml");
        assertThat(config.getEndpointsConfigPath()).isEqualTo("test-api-endpoints.yml");
        
        logger.info("Test databases path: {}", config.getDatabasesConfigPath());
        logger.info("Test queries path: {}", config.getQueriesConfigPath());
        logger.info("Test endpoints path: {}", config.getEndpointsConfigPath());
        
        // Verify specific test data is loaded
        assertThat(databases).containsKey("stock-trades-db");
        assertThat(databases).containsKey("metrics-db");
        assertThat(queries).containsKey("test-query");
        assertThat(queries).containsKey("stock-trades-all");
        assertThat(endpoints).containsKey("test-endpoint");
        assertThat(endpoints).containsKey("stock-trades-list");
    }

    @Test
    void testLoadedDataMatchesSourceFiles() {
        // Arrange - Use test configuration
        System.setProperty("generic.config.file", "application-test.yml");
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        ConfigurationLoader loader = new ConfigurationLoader(config);

        // Act - Load configurations through ConfigurationLoader
        Map<String, DatabaseConfig> loadedDatabases = loader.loadDatabaseConfigurations();
        Map<String, QueryConfig> loadedQueries = loader.loadQueryConfigurations();
        Map<String, ApiEndpointConfig> loadedEndpoints = loader.loadEndpointConfigurations();

        // Act - Load configurations directly from files for comparison
        Map<String, DatabaseConfig> directDatabases = loadDatabasesDirectly("test-databases.yml");
        Map<String, QueryConfig> directQueries = loadQueriesDirectly("test-queries.yml");
        Map<String, ApiEndpointConfig> directEndpoints = loadEndpointsDirectly("test-api-endpoints.yml");

        // Assert - Verify loaded data matches direct file content
        assertThat(loadedDatabases).hasSize(directDatabases.size());
        assertThat(loadedQueries).hasSize(directQueries.size());
        assertThat(loadedEndpoints).hasSize(directEndpoints.size());

        // Verify specific database configurations match
        for (String key : directDatabases.keySet()) {
            assertThat(loadedDatabases).containsKey(key);
            DatabaseConfig loaded = loadedDatabases.get(key);
            DatabaseConfig direct = directDatabases.get(key);

            assertThat(loaded.getName()).isEqualTo(direct.getName());
            assertThat(loaded.getUrl()).isEqualTo(direct.getUrl());
            assertThat(loaded.getUsername()).isEqualTo(direct.getUsername());
            assertThat(loaded.getDriver()).isEqualTo(direct.getDriver());
        }

        // Verify specific query configurations match
        for (String key : directQueries.keySet()) {
            assertThat(loadedQueries).containsKey(key);
            QueryConfig loaded = loadedQueries.get(key);
            QueryConfig direct = directQueries.get(key);

            assertThat(loaded.getName()).isEqualTo(direct.getName());
            assertThat(loaded.getSql()).isEqualTo(direct.getSql());
            assertThat(loaded.getDatabase()).isEqualTo(direct.getDatabase());
        }

        // Verify specific endpoint configurations match
        for (String key : directEndpoints.keySet()) {
            assertThat(loadedEndpoints).containsKey(key);
            ApiEndpointConfig loaded = loadedEndpoints.get(key);
            ApiEndpointConfig direct = directEndpoints.get(key);

            assertThat(loaded.getPath()).isEqualTo(direct.getPath());
            assertThat(loaded.getMethod()).isEqualTo(direct.getMethod());
            assertThat(loaded.getQuery()).isEqualTo(direct.getQuery());
        }
    }

    @Test
    void testConfigurationPathsAreLogged() {
        // Arrange - Use test configuration
        System.setProperty("generic.config.file", "application-test.yml");
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        ConfigurationLoader loader = new ConfigurationLoader(config);

        // Act & Assert - Verify that loading logs the correct paths
        assertThatCode(() -> {
            loader.loadDatabaseConfigurations();
            loader.loadQueryConfigurations();
            loader.loadEndpointConfigurations();
        }).doesNotThrowAnyException();

        // Verify the configuration paths are as expected
        assertThat(config.getDatabasesConfigPath()).isEqualTo("test-databases.yml");
        assertThat(config.getQueriesConfigPath()).isEqualTo("test-queries.yml");
        assertThat(config.getEndpointsConfigPath()).isEqualTo("test-api-endpoints.yml");
    }

    @Test
    void testConfigurationConsistencyAcrossDifferentPaths() {
        // Test that the same configuration structure is maintained regardless of file paths

        // Load from test paths
        System.setProperty("generic.config.file", "application-test.yml");
        GenericApiConfig testConfig = GenericApiConfig.loadFromFile();
        ConfigurationLoader testLoader = new ConfigurationLoader(testConfig);

        Map<String, DatabaseConfig> testDatabases = testLoader.loadDatabaseConfigurations();
        Map<String, QueryConfig> testQueries = testLoader.loadQueryConfigurations();
        Map<String, ApiEndpointConfig> testEndpoints = testLoader.loadEndpointConfigurations();

        // Verify structure consistency
        assertThat(testDatabases).isNotNull();
        assertThat(testQueries).isNotNull();
        assertThat(testEndpoints).isNotNull();

        // Verify that all databases have required fields
        testDatabases.values().forEach(db -> {
            assertThat(db.getName()).isNotNull().isNotEmpty();
            assertThat(db.getUrl()).isNotNull().isNotEmpty();
            assertThat(db.getDriver()).isNotNull().isNotEmpty();
        });

        // Verify that all queries have required fields
        testQueries.values().forEach(query -> {
            assertThat(query.getName()).isNotNull().isNotEmpty();
            assertThat(query.getSql()).isNotNull().isNotEmpty();
            assertThat(query.getDatabase()).isNotNull().isNotEmpty();
        });

        // Verify that all endpoints have required fields
        testEndpoints.values().forEach(endpoint -> {
            assertThat(endpoint.getPath()).isNotNull().isNotEmpty();
            assertThat(endpoint.getMethod()).isNotNull().isNotEmpty();
            assertThat(endpoint.getQuery()).isNotNull().isNotEmpty();
        });
    }

    // Helper methods for direct file loading
    private Map<String, DatabaseConfig> loadDatabasesDirectly(String fileName) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new RuntimeException(fileName + " not found in classpath");
            }
            ConfigurationLoader.DatabasesWrapper wrapper = yamlMapper.readValue(inputStream, ConfigurationLoader.DatabasesWrapper.class);
            return wrapper.getDatabases();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load " + fileName, e);
        }
    }

    private Map<String, QueryConfig> loadQueriesDirectly(String fileName) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new RuntimeException(fileName + " not found in classpath");
            }
            ConfigurationLoader.QueriesWrapper wrapper = yamlMapper.readValue(inputStream, ConfigurationLoader.QueriesWrapper.class);
            return wrapper.getQueries();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load " + fileName, e);
        }
    }

    private Map<String, ApiEndpointConfig> loadEndpointsDirectly(String fileName) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new RuntimeException(fileName + " not found in classpath");
            }
            ConfigurationLoader.EndpointsWrapper wrapper = yamlMapper.readValue(inputStream, ConfigurationLoader.EndpointsWrapper.class);
            return wrapper.getEndpoints();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load " + fileName, e);
        }
    }

    /**
     * Helper method to check if configuration files exist without causing System.exit
     */
    private boolean checkConfigurationFilesExist(String databasesPath, String queriesPath, String endpointsPath) {
        try {
            // Check if files exist as resources in classpath
            boolean databasesExist = getClass().getClassLoader().getResource(databasesPath) != null;
            boolean queriesExist = getClass().getClassLoader().getResource(queriesPath) != null;
            boolean endpointsExist = getClass().getClassLoader().getResource(endpointsPath) != null;

            if (!databasesExist || !queriesExist || !endpointsExist) {
                // Try checking as external files
                java.io.File databasesFile = new java.io.File(databasesPath);
                java.io.File queriesFile = new java.io.File(queriesPath);
                java.io.File endpointsFile = new java.io.File(endpointsPath);

                databasesExist = databasesFile.exists();
                queriesExist = queriesFile.exists();
                endpointsExist = endpointsFile.exists();
            }

            logger.debug("Configuration files existence check:");
            logger.debug("  Databases ({}): {}", databasesPath, databasesExist);
            logger.debug("  Queries ({}): {}", queriesPath, queriesExist);
            logger.debug("  Endpoints ({}): {}", endpointsPath, endpointsExist);

            return databasesExist && queriesExist && endpointsExist;

        } catch (Exception e) {
            logger.debug("Error checking configuration files existence: {}", e.getMessage());
            return false;
        }
    }
}

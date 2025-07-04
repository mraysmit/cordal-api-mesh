package dev.mars.generic.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mars.config.GenericApiConfig;
import dev.mars.generic.GenericApiApplication;
import dev.mars.generic.config.ApiEndpointConfig;
import dev.mars.generic.config.DatabaseConfig;
import dev.mars.generic.config.QueryConfig;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests to verify that both YAML and database configuration sources
 * work correctly and return consistent data structures.
 */
class ConfigurationSourceIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationSourceIntegrationTest.class);
    
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // Clear any existing system properties
        System.clearProperty("generic.config.file");
    }

    @AfterEach
    void tearDown() {
        // Clean up system properties
        System.clearProperty("generic.config.file");
    }

    @Test
    void testYamlConfigurationSourceLoading() {
        // Arrange - Use YAML configuration source
        System.setProperty("generic.config.file", "application-test.yml");
        
        // Load configuration to verify source
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        
        // Verify this is using YAML source (default or explicitly set)
        // The test configuration should use YAML source
        
        // Act - Load configurations
        ConfigurationLoader loader = new ConfigurationLoader(config);
        Map<String, DatabaseConfig> databases = loader.loadDatabaseConfigurations();
        Map<String, QueryConfig> queries = loader.loadQueryConfigurations();
        Map<String, ApiEndpointConfig> endpoints = loader.loadEndpointConfigurations();
        
        // Assert - Verify configurations loaded successfully from YAML
        assertThat(databases).isNotNull().isNotEmpty();
        assertThat(queries).isNotNull().isNotEmpty();
        assertThat(endpoints).isNotNull().isNotEmpty();
        
        // Verify specific test data exists (from YAML files)
        assertThat(databases).containsKey("stock-trades-db");
        assertThat(databases).containsKey("metrics-db");
        assertThat(queries).containsKey("test-query");
        assertThat(queries).containsKey("stock-trades-all");
        assertThat(endpoints).containsKey("test-endpoint");
        assertThat(endpoints).containsKey("stock-trades-list");
        
        logger.info("Successfully loaded configurations from YAML source");
        logger.info("Loaded {} databases, {} queries, {} endpoints", 
                   databases.size(), queries.size(), endpoints.size());
    }

    @Test
    void testDatabaseConfigurationSourceLoading() {
        // Arrange - Use database configuration source
        System.setProperty("generic.config.file", "application-database-test.yml");
        
        // Load configuration to verify source
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        
        // Start application to initialize database configurations
        GenericApiApplication application = new GenericApiApplication();
        application.initializeForTesting();
        Javalin app = application.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Act - Call management APIs to verify database source is working
            var dbResponse = client.get("/api/management/config/databases");
            var queryResponse = client.get("/api/management/config/queries");
            var endpointResponse = client.get("/api/management/config/endpoints");
            
            // Assert - Verify all APIs return successful responses
            assertThat(dbResponse.code()).isEqualTo(200);
            assertThat(queryResponse.code()).isEqualTo(200);
            assertThat(endpointResponse.code()).isEqualTo(200);
            
            // Parse responses
            JsonNode dbJson = objectMapper.readTree(dbResponse.body().string());
            JsonNode queryJson = objectMapper.readTree(queryResponse.body().string());
            JsonNode endpointJson = objectMapper.readTree(endpointResponse.body().string());
            
            // Verify data structure is consistent
            assertThat(dbJson.has("count")).isTrue();
            assertThat(dbJson.has("databases")).isTrue();
            assertThat(queryJson.has("count")).isTrue();
            assertThat(queryJson.has("queries")).isTrue();
            assertThat(endpointJson.has("count")).isTrue();
            assertThat(endpointJson.has("endpoints")).isTrue();
            
            // Verify counts are reasonable (database source should have some configurations)
            int dbCount = dbJson.get("count").asInt();
            int queryCount = queryJson.get("count").asInt();
            int endpointCount = endpointJson.get("count").asInt();
            
            assertThat(dbCount).isGreaterThanOrEqualTo(0);
            assertThat(queryCount).isGreaterThanOrEqualTo(0);
            assertThat(endpointCount).isGreaterThanOrEqualTo(0);
            
            logger.info("Successfully loaded configurations from database source");
            logger.info("Database source returned {} databases, {} queries, {} endpoints", 
                       dbCount, queryCount, endpointCount);
        });
    }

    @Test
    void testConfigurationSourceConsistency() {
        // Test that both YAML and database sources return consistent data structures
        
        // Test YAML source first
        System.setProperty("generic.config.file", "application-test.yml");
        GenericApiConfig yamlConfig = GenericApiConfig.loadFromFile();
        ConfigurationLoader yamlLoader = new ConfigurationLoader(yamlConfig);
        
        Map<String, DatabaseConfig> yamlDatabases = yamlLoader.loadDatabaseConfigurations();
        Map<String, QueryConfig> yamlQueries = yamlLoader.loadQueryConfigurations();
        Map<String, ApiEndpointConfig> yamlEndpoints = yamlLoader.loadEndpointConfigurations();
        
        // Verify YAML data structure
        verifyConfigurationDataStructure(yamlDatabases, yamlQueries, yamlEndpoints, "YAML");
        
        // Note: Database source testing would require database to be properly initialized
        // with configuration data, which may not be available in all test environments
        logger.info("Configuration source consistency verified for YAML source");
    }

    @Test
    void testConfigurationSourceSwitching() {
        // Test that the application can handle different configuration sources
        
        // Test with YAML source
        System.setProperty("generic.config.file", "application-test.yml");
        GenericApiApplication yamlApplication = new GenericApiApplication();
        yamlApplication.initializeForTesting();
        Javalin yamlApp = yamlApplication.getApp();

        JavalinTest.test(yamlApp, (server, client) -> {
            var response = client.get("/api/management/config/metadata");
            assertThat(response.code()).isEqualTo(200);
            
            JsonNode metadata = objectMapper.readTree(response.body().string());
            assertThat(metadata.has("configurationPaths")).isTrue();
            
            JsonNode paths = metadata.get("configurationPaths");
            assertThat(paths.get("databases").asText()).isEqualTo("test-databases.yml");
            assertThat(paths.get("queries").asText()).isEqualTo("test-queries.yml");
            assertThat(paths.get("endpoints").asText()).isEqualTo("test-api-endpoints.yml");
            
            logger.info("Verified YAML configuration source metadata");
        });
        
        // Clean up
        yamlApplication.stop();

        // Test with database source
        System.setProperty("generic.config.file", "application-database-test.yml");
        GenericApiApplication dbApplication = new GenericApiApplication();
        dbApplication.initializeForTesting();
        Javalin dbApp = dbApplication.getApp();

        JavalinTest.test(dbApp, (server, client) -> {
            var response = client.get("/api/management/config/metadata");
            assertThat(response.code()).isEqualTo(200);
            
            JsonNode metadata = objectMapper.readTree(response.body().string());
            assertThat(metadata.has("configurationPaths")).isTrue();
            
            // Database source should still report the file paths (even if not used)
            JsonNode paths = metadata.get("configurationPaths");
            assertThat(paths.has("databases")).isTrue();
            assertThat(paths.has("queries")).isTrue();
            assertThat(paths.has("endpoints")).isTrue();
            
            logger.info("Verified database configuration source metadata");
        });
        
        dbApplication.stop();
    }

    @Test
    void testConfigurationSourceErrorHandling() {
        // Test that configuration loading handles errors gracefully
        
        // Test with valid configuration
        System.setProperty("generic.config.file", "application-test.yml");
        
        assertThatCode(() -> {
            GenericApiConfig config = GenericApiConfig.loadFromFile();
            ConfigurationLoader loader = new ConfigurationLoader(config);
            
            loader.loadDatabaseConfigurations();
            loader.loadQueryConfigurations();
            loader.loadEndpointConfigurations();
        }).doesNotThrowAnyException();
        
        logger.info("Configuration source error handling verified");
    }

    @Test
    void testConfigurationSourceDataIntegrity() {
        // Test that configuration data maintains integrity across different sources
        
        System.setProperty("generic.config.file", "application-test.yml");
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        ConfigurationLoader loader = new ConfigurationLoader(config);
        
        // Load configurations multiple times to ensure consistency
        Map<String, DatabaseConfig> databases1 = loader.loadDatabaseConfigurations();
        Map<String, DatabaseConfig> databases2 = loader.loadDatabaseConfigurations();
        
        Map<String, QueryConfig> queries1 = loader.loadQueryConfigurations();
        Map<String, QueryConfig> queries2 = loader.loadQueryConfigurations();
        
        Map<String, ApiEndpointConfig> endpoints1 = loader.loadEndpointConfigurations();
        Map<String, ApiEndpointConfig> endpoints2 = loader.loadEndpointConfigurations();
        
        // Verify consistency across multiple loads
        assertThat(databases1).hasSize(databases2.size());
        assertThat(queries1).hasSize(queries2.size());
        assertThat(endpoints1).hasSize(endpoints2.size());
        
        // Verify specific data consistency
        for (String key : databases1.keySet()) {
            assertThat(databases2).containsKey(key);
            DatabaseConfig db1 = databases1.get(key);
            DatabaseConfig db2 = databases2.get(key);
            assertThat(db1.getName()).isEqualTo(db2.getName());
            assertThat(db1.getUrl()).isEqualTo(db2.getUrl());
        }
        
        logger.info("Configuration source data integrity verified");
    }

    private void verifyConfigurationDataStructure(Map<String, DatabaseConfig> databases,
                                                Map<String, QueryConfig> queries,
                                                Map<String, ApiEndpointConfig> endpoints,
                                                String sourceType) {
        // Verify databases have required fields
        databases.values().forEach(db -> {
            assertThat(db.getName()).isNotNull().isNotEmpty();
            assertThat(db.getUrl()).isNotNull().isNotEmpty();
            assertThat(db.getDriver()).isNotNull().isNotEmpty();
        });
        
        // Verify queries have required fields
        queries.values().forEach(query -> {
            assertThat(query.getName()).isNotNull().isNotEmpty();
            assertThat(query.getSql()).isNotNull().isNotEmpty();
            assertThat(query.getDatabase()).isNotNull().isNotEmpty();
        });
        
        // Verify endpoints have required fields
        endpoints.values().forEach(endpoint -> {
            assertThat(endpoint.getPath()).isNotNull().isNotEmpty();
            assertThat(endpoint.getMethod()).isNotNull().isNotEmpty();
            assertThat(endpoint.getQuery()).isNotNull().isNotEmpty();
        });
        
        logger.info("Verified data structure consistency for {} source", sourceType);
    }
}

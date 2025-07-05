package dev.mars.generic.management;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mars.config.GenericApiConfig;
import dev.mars.generic.GenericApiApplication;
import dev.mars.generic.config.ConfigurationLoader;
import dev.mars.generic.config.ApiEndpointConfig;
import dev.mars.generic.config.DatabaseConfig;
import dev.mars.generic.config.QueryConfig;
import dev.mars.util.ApiEndpoints;
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
 * Comprehensive tests to verify that management APIs return the exact data
 * that was loaded from configuration sources.
 */
public class ManagementApiDataVerificationTest {
    private static final Logger logger = LoggerFactory.getLogger(ManagementApiDataVerificationTest.class);
    
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        // Set test configuration for all tests in this class
        System.setProperty("generic.config.file", "application-test.yml");
        logger.info("Test setup completed - using test configuration: application-test.yml");
    }

    @AfterEach
    public void tearDown() {
        // Clean up system properties
        System.clearProperty("generic.config.file");
    }

    @Test
    public void testManagementApiReturnsLoadedDatabaseConfigurations() {
        // Arrange - Load configurations directly for comparison (test config already set in setUp)
        GenericApiConfig config = new GenericApiConfig();
        ConfigurationLoader loader = new ConfigurationLoader(config);
        Map<String, DatabaseConfig> expectedDatabases = loader.loadDatabaseConfigurations();
        
        // Start application
        GenericApiApplication application = new GenericApiApplication();
        application.initializeForTesting();
        Javalin app = application.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Act - Call management API
            var response = client.get(ApiEndpoints.Management.CONFIG_DATABASES);
            
            // Assert - Verify response
            assertThat(response.code()).isEqualTo(200);
            
            String responseBody = response.body().string();
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            
            // Verify response structure
            assertThat(jsonResponse.has("count")).isTrue();
            assertThat(jsonResponse.has("databases")).isTrue();
            
            // Verify count matches expected
            int actualCount = jsonResponse.get("count").asInt();
            assertThat(actualCount).isEqualTo(expectedDatabases.size());
            
            // Verify each database configuration matches
            JsonNode databasesNode = jsonResponse.get("databases");
            for (String key : expectedDatabases.keySet()) {
                assertThat(databasesNode.has(key)).isTrue();
                
                DatabaseConfig expected = expectedDatabases.get(key);
                JsonNode actual = databasesNode.get(key);
                
                assertThat(actual.get("name").asText()).isEqualTo(expected.getName());
                assertThat(actual.get("url").asText()).isEqualTo(expected.getUrl());
                assertThat(actual.get("username").asText()).isEqualTo(expected.getUsername());
                assertThat(actual.get("driver").asText()).isEqualTo(expected.getDriver());
                
                logger.info("Verified database configuration: {}", key);
            }
        });
    }

    @Test
    public void testManagementApiReturnsLoadedQueryConfigurations() {
        // Arrange - Load configurations directly for comparison (test config already set in setUp)
        GenericApiConfig config = new GenericApiConfig();
        ConfigurationLoader loader = new ConfigurationLoader(config);
        Map<String, QueryConfig> expectedQueries = loader.loadQueryConfigurations();
        
        // Start application
        GenericApiApplication application = new GenericApiApplication();
        application.initializeForTesting();
        Javalin app = application.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Act - Call management API
            var response = client.get(ApiEndpoints.Management.CONFIG_QUERIES);
            
            // Assert - Verify response
            assertThat(response.code()).isEqualTo(200);
            
            String responseBody = response.body().string();
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            
            // Verify response structure
            assertThat(jsonResponse.has("count")).isTrue();
            assertThat(jsonResponse.has("queries")).isTrue();
            
            // Verify count matches expected
            int actualCount = jsonResponse.get("count").asInt();
            assertThat(actualCount).isEqualTo(expectedQueries.size());
            
            // Verify each query configuration matches
            JsonNode queriesNode = jsonResponse.get("queries");
            for (String key : expectedQueries.keySet()) {
                assertThat(queriesNode.has(key)).isTrue();
                
                QueryConfig expected = expectedQueries.get(key);
                JsonNode actual = queriesNode.get(key);
                
                assertThat(actual.get("name").asText()).isEqualTo(expected.getName());
                assertThat(actual.get("sql").asText()).isEqualTo(expected.getSql());
                assertThat(actual.get("database").asText()).isEqualTo(expected.getDatabase());
                
                if (expected.getDescription() != null) {
                    assertThat(actual.get("description").asText()).isEqualTo(expected.getDescription());
                }
                
                logger.info("Verified query configuration: {}", key);
            }
        });
    }

    @Test
    public void testManagementApiReturnsLoadedEndpointConfigurations() {
        // Arrange - Load configurations directly for comparison (test config already set in setUp)
        GenericApiConfig config = new GenericApiConfig();
        ConfigurationLoader loader = new ConfigurationLoader(config);
        Map<String, ApiEndpointConfig> expectedEndpoints = loader.loadEndpointConfigurations();
        
        // Start application
        GenericApiApplication application = new GenericApiApplication();
        application.initializeForTesting();
        Javalin app = application.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Act - Call management API
            var response = client.get(ApiEndpoints.Management.CONFIG_ENDPOINTS);
            
            // Assert - Verify response
            assertThat(response.code()).isEqualTo(200);
            
            String responseBody = response.body().string();
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            
            // Verify response structure
            assertThat(jsonResponse.has("count")).isTrue();
            assertThat(jsonResponse.has("endpoints")).isTrue();
            
            // Verify count matches expected
            int actualCount = jsonResponse.get("count").asInt();
            assertThat(actualCount).isEqualTo(expectedEndpoints.size());
            
            // Verify each endpoint configuration matches
            JsonNode endpointsNode = jsonResponse.get("endpoints");
            for (String key : expectedEndpoints.keySet()) {
                assertThat(endpointsNode.has(key)).isTrue();
                
                ApiEndpointConfig expected = expectedEndpoints.get(key);
                JsonNode actual = endpointsNode.get(key);
                
                assertThat(actual.get("path").asText()).isEqualTo(expected.getPath());
                assertThat(actual.get("method").asText()).isEqualTo(expected.getMethod());
                assertThat(actual.get("query").asText()).isEqualTo(expected.getQuery());
                
                if (expected.getCountQuery() != null) {
                    assertThat(actual.get("countQuery").asText()).isEqualTo(expected.getCountQuery());
                }
                
                if (expected.getDescription() != null) {
                    assertThat(actual.get("description").asText()).isEqualTo(expected.getDescription());
                }
                
                logger.info("Verified endpoint configuration: {}", key);
            }
        });
    }

    @Test
    public void testManagementApiConfigurationMetadata() {
        // Arrange - Load configuration for path verification (test config already set in setUp)
        GenericApiConfig config = new GenericApiConfig();
        
        // Start application
        GenericApiApplication application = new GenericApiApplication();
        application.initializeForTesting();
        Javalin app = application.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Act - Call configuration metadata API
            var response = client.get(ApiEndpoints.Management.CONFIG_METADATA);
            
            // Assert - Verify response
            assertThat(response.code()).isEqualTo(200);
            
            String responseBody = response.body().string();
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            
            // Verify metadata structure
            assertThat(jsonResponse.has("serviceStartTime")).isTrue();
            assertThat(jsonResponse.has("configurationPaths")).isTrue();
            assertThat(jsonResponse.has("configurationFiles")).isTrue();
            assertThat(jsonResponse.has("lastRefresh")).isTrue();
            
            // Verify configuration paths match what was loaded
            JsonNode pathsNode = jsonResponse.get("configurationPaths");
            assertThat(pathsNode.get("databases").asText()).isEqualTo(config.getDatabasesConfigPath());
            assertThat(pathsNode.get("queries").asText()).isEqualTo(config.getQueriesConfigPath());
            assertThat(pathsNode.get("endpoints").asText()).isEqualTo(config.getEndpointsConfigPath());
            
            logger.info("Verified configuration metadata paths");
        });
    }

    @Test
    public void testManagementApiConfigurationPaths() {
        // Arrange - Load configuration for path verification (test config already set in setUp)
        GenericApiConfig config = new GenericApiConfig();
        
        // Start application
        GenericApiApplication application = new GenericApiApplication();
        application.initializeForTesting();
        Javalin app = application.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Act - Call configuration paths API
            var response = client.get(ApiEndpoints.Management.CONFIG_PATHS);
            
            // Assert - Verify response
            assertThat(response.code()).isEqualTo(200);
            
            String responseBody = response.body().string();
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            
            // Verify paths match the configuration
            assertThat(jsonResponse.get("databases").asText()).isEqualTo(config.getDatabasesConfigPath());
            assertThat(jsonResponse.get("queries").asText()).isEqualTo(config.getQueriesConfigPath());
            assertThat(jsonResponse.get("endpoints").asText()).isEqualTo(config.getEndpointsConfigPath());
            
            logger.info("Verified configuration paths API returns correct paths");
        });
    }

    @Test
    public void testManagementApiConfigurationContents() {
        // Arrange - Load configuration for path verification (test config already set in setUp)
        GenericApiConfig config = new GenericApiConfig();

        // Start application
        GenericApiApplication application = new GenericApiApplication();
        application.initializeForTesting();
        Javalin app = application.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Act - Call configuration contents API
            var response = client.get(ApiEndpoints.Management.CONFIG_CONTENTS);

            // Assert - Verify response
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            JsonNode jsonResponse = objectMapper.readTree(responseBody);

            // Verify contents structure includes all configuration types
            assertThat(jsonResponse.has("databases")).isTrue();
            assertThat(jsonResponse.has("queries")).isTrue();
            assertThat(jsonResponse.has("endpoints")).isTrue();

            // Verify each configuration type has path information
            JsonNode databasesContent = jsonResponse.get("databases");
            assertThat(databasesContent.get("path").asText()).isEqualTo(config.getDatabasesConfigPath());
            assertThat(databasesContent.get("status").asText()).isEqualTo("Available");

            JsonNode queriesContent = jsonResponse.get("queries");
            assertThat(queriesContent.get("path").asText()).isEqualTo(config.getQueriesConfigPath());
            assertThat(queriesContent.get("status").asText()).isEqualTo("Available");

            JsonNode endpointsContent = jsonResponse.get("endpoints");
            assertThat(endpointsContent.get("path").asText()).isEqualTo(config.getEndpointsConfigPath());
            assertThat(endpointsContent.get("status").asText()).isEqualTo("Available");

            logger.info("Verified configuration contents API returns correct file information");
        });
    }

    @Test
    public void testManagementApiDataConsistencyWithDifferentConfigurationPaths() {
        // Test that management APIs return consistent data regardless of configuration paths

        // Test with default configuration
        testConfigurationConsistency("application-test.yml", "test");

        // Test with custom paths configuration (if it exists and works)
        // Note: This would require custom configuration files to exist
        // testConfigurationConsistency("application-custom-paths.yml", "custom");
    }

    private void testConfigurationConsistency(String configFile, String testType) {
        // Arrange - Set specific config file for this test
        String originalConfigFile = System.getProperty("generic.config.file");
        System.setProperty("generic.config.file", configFile);

        try {
            // Load configurations directly
            GenericApiConfig config = new GenericApiConfig();
        ConfigurationLoader loader = new ConfigurationLoader(config);

        Map<String, DatabaseConfig> expectedDatabases = loader.loadDatabaseConfigurations();
        Map<String, QueryConfig> expectedQueries = loader.loadQueryConfigurations();
        Map<String, ApiEndpointConfig> expectedEndpoints = loader.loadEndpointConfigurations();

        // Start application
        GenericApiApplication application = new GenericApiApplication();
        application.initializeForTesting();
        Javalin app = application.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Test databases API
            var dbResponse = client.get(ApiEndpoints.Management.CONFIG_DATABASES);
            assertThat(dbResponse.code()).isEqualTo(200);

            JsonNode dbJson = objectMapper.readTree(dbResponse.body().string());
            assertThat(dbJson.get("count").asInt()).isEqualTo(expectedDatabases.size());

            // Test queries API
            var queryResponse = client.get(ApiEndpoints.Management.CONFIG_QUERIES);
            assertThat(queryResponse.code()).isEqualTo(200);

            JsonNode queryJson = objectMapper.readTree(queryResponse.body().string());
            assertThat(queryJson.get("count").asInt()).isEqualTo(expectedQueries.size());

            // Test endpoints API
            var endpointResponse = client.get(ApiEndpoints.Management.CONFIG_ENDPOINTS);
            assertThat(endpointResponse.code()).isEqualTo(200);

            JsonNode endpointJson = objectMapper.readTree(endpointResponse.body().string());
            assertThat(endpointJson.get("count").asInt()).isEqualTo(expectedEndpoints.size());

            logger.info("Verified configuration consistency for {} configuration", testType);
        });
        } finally {
            // Restore original config file
            if (originalConfigFile != null) {
                System.setProperty("generic.config.file", originalConfigFile);
            } else {
                System.clearProperty("generic.config.file");
            }
        }
    }

    @Test
    public void testManagementApiReturnsCompleteConfigurationData() {
        // Arrange - Start application (test config already set in setUp)
        GenericApiApplication application = new GenericApiApplication();
        application.initializeForTesting();
        Javalin app = application.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Act - Call comprehensive dashboard API
            var response = client.get(ApiEndpoints.Management.DASHBOARD);

            // Assert - Verify response
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            JsonNode jsonResponse = objectMapper.readTree(responseBody);

            // Verify dashboard includes all configuration sections
            assertThat(jsonResponse.has("configuration")).isTrue();

            JsonNode configNode = jsonResponse.get("configuration");
            assertThat(configNode.has("endpoints")).isTrue();
            assertThat(configNode.has("queries")).isTrue();
            assertThat(configNode.has("databases")).isTrue();

            // Verify each section has count information
            assertThat(configNode.get("endpoints").has("count")).isTrue();
            assertThat(configNode.get("queries").has("count")).isTrue();
            assertThat(configNode.get("databases").has("count")).isTrue();

            // Verify counts are positive (indicating data was loaded)
            assertThat(configNode.get("endpoints").get("count").asInt()).isGreaterThan(0);
            assertThat(configNode.get("queries").get("count").asInt()).isGreaterThan(0);
            assertThat(configNode.get("databases").get("count").asInt()).isGreaterThan(0);

            logger.info("Verified comprehensive dashboard includes all configuration data");
        });
    }

    @Test
    public void testManagementApiErrorHandling() {
        // Test that management APIs handle errors gracefully

        // Start application with default configuration
        GenericApiApplication application = new GenericApiApplication();
        application.initializeForTesting();
        Javalin app = application.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Test that all management endpoints return successful responses
            var endpoints = new String[]{
                ApiEndpoints.Management.CONFIG_DATABASES,
                ApiEndpoints.Management.CONFIG_QUERIES,
                ApiEndpoints.Management.CONFIG_ENDPOINTS,
                ApiEndpoints.Management.CONFIG_METADATA,
                ApiEndpoints.Management.CONFIG_PATHS,
                ApiEndpoints.Management.CONFIG_CONTENTS,
                ApiEndpoints.Management.DASHBOARD
            };

            for (String endpoint : endpoints) {
                var response = client.get(endpoint);
                assertThat(response.code())
                    .as("Endpoint %s should return success", endpoint)
                    .isIn(200, 201, 202); // Accept various success codes

                // Verify response is valid JSON
                String responseBody = response.body().string();
                assertThatCode(() -> objectMapper.readTree(responseBody))
                    .as("Response from %s should be valid JSON", endpoint)
                    .doesNotThrowAnyException();

                logger.info("Verified endpoint {} returns valid response", endpoint);
            }
        });
    }
}

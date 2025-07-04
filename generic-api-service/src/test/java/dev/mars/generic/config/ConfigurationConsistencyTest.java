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

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests to verify consistency between ConfigurationLoader data
 * and Management API responses, including metadata accuracy and cross-validation.
 */
class ConfigurationConsistencyTest {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationConsistencyTest.class);
    
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
    void testConfigurationLoaderAndManagementApiConsistency() {
        // Arrange - Use test configuration
        System.setProperty("generic.config.file", "application-test.yml");
        
        // Load configurations directly through ConfigurationLoader
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        ConfigurationLoader loader = new ConfigurationLoader(config);
        
        Map<String, DatabaseConfig> directDatabases = loader.loadDatabaseConfigurations();
        Map<String, QueryConfig> directQueries = loader.loadQueryConfigurations();
        Map<String, ApiEndpointConfig> directEndpoints = loader.loadEndpointConfigurations();
        
        // Start application and test Management APIs
        GenericApiApplication application = new GenericApiApplication();
        application.initializeForTesting();
        Javalin app = application.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Get data from Management APIs
            var dbResponse = client.get("/api/management/config/databases");
            var queryResponse = client.get("/api/management/config/queries");
            var endpointResponse = client.get("/api/management/config/endpoints");
            
            assertThat(dbResponse.code()).isEqualTo(200);
            assertThat(queryResponse.code()).isEqualTo(200);
            assertThat(endpointResponse.code()).isEqualTo(200);
            
            JsonNode dbJson = objectMapper.readTree(dbResponse.body().string());
            JsonNode queryJson = objectMapper.readTree(queryResponse.body().string());
            JsonNode endpointJson = objectMapper.readTree(endpointResponse.body().string());
            
            // Verify counts match
            assertThat(dbJson.get("count").asInt()).isEqualTo(directDatabases.size());
            assertThat(queryJson.get("count").asInt()).isEqualTo(directQueries.size());
            assertThat(endpointJson.get("count").asInt()).isEqualTo(directEndpoints.size());
            
            // Verify all keys exist in both sources
            JsonNode apiDatabases = dbJson.get("databases");
            JsonNode apiQueries = queryJson.get("queries");
            JsonNode apiEndpoints = endpointJson.get("endpoints");
            
            for (String key : directDatabases.keySet()) {
                assertThat(apiDatabases.has(key))
                    .as("Management API should contain database key: %s", key)
                    .isTrue();
            }
            
            for (String key : directQueries.keySet()) {
                assertThat(apiQueries.has(key))
                    .as("Management API should contain query key: %s", key)
                    .isTrue();
            }
            
            for (String key : directEndpoints.keySet()) {
                assertThat(apiEndpoints.has(key))
                    .as("Management API should contain endpoint key: %s", key)
                    .isTrue();
            }
            
            logger.info("Verified consistency between ConfigurationLoader and Management APIs");
        });
    }

    @Test
    void testConfigurationMetadataAccuracy() {
        // Arrange - Use test configuration
        System.setProperty("generic.config.file", "application-test.yml");
        
        // Record start time for comparison
        Instant testStartTime = Instant.now();
        
        // Load configuration for path verification
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        
        // Start application
        GenericApiApplication application = new GenericApiApplication();
        application.initializeForTesting();
        Javalin app = application.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Get metadata from Management API
            var response = client.get("/api/management/config/metadata");
            assertThat(response.code()).isEqualTo(200);
            
            JsonNode metadata = objectMapper.readTree(response.body().string());
            
            // Verify metadata structure
            assertThat(metadata.has("serviceStartTime")).isTrue();
            assertThat(metadata.has("configurationPaths")).isTrue();
            assertThat(metadata.has("configurationFiles")).isTrue();
            assertThat(metadata.has("lastRefresh")).isTrue();
            
            // Verify service start time is reasonable
            String startTimeStr = metadata.get("serviceStartTime").asText();
            Instant serviceStartTime = Instant.parse(startTimeStr);
            assertThat(serviceStartTime).isAfter(testStartTime.minusSeconds(60)); // Allow some margin
            assertThat(serviceStartTime).isBefore(Instant.now().plusSeconds(10));
            
            // Verify configuration paths match loaded configuration
            JsonNode paths = metadata.get("configurationPaths");
            assertThat(paths.get("databases").asText()).isEqualTo(config.getDatabasesConfigPath());
            assertThat(paths.get("queries").asText()).isEqualTo(config.getQueriesConfigPath());
            assertThat(paths.get("endpoints").asText()).isEqualTo(config.getEndpointsConfigPath());
            
            // Verify configuration files metadata
            JsonNode files = metadata.get("configurationFiles");
            assertThat(files.has("databases")).isTrue();
            assertThat(files.has("queries")).isTrue();
            assertThat(files.has("endpoints")).isTrue();
            assertThat(files.has("application")).isTrue();
            
            // Verify each file metadata has required fields
            for (String fileType : new String[]{"databases", "queries", "endpoints", "application"}) {
                JsonNode fileMetadata = files.get(fileType);
                assertThat(fileMetadata.has("configType")).isTrue();
                assertThat(fileMetadata.has("filePath")).isTrue();
                assertThat(fileMetadata.has("lastModified")).isTrue();
                assertThat(fileMetadata.has("status")).isTrue();
                
                assertThat(fileMetadata.get("configType").asText()).isEqualTo(fileType);
                assertThat(fileMetadata.get("status").asText()).isEqualTo("LOADED");
            }
            
            logger.info("Verified configuration metadata accuracy");
        });
    }

    @Test
    void testConfigurationPathsConsistency() {
        // Test different configuration files and verify paths are consistent
        
        String[] configFiles = {
            "application-test.yml",
            "application-custom-paths.yml"
        };
        
        for (String configFile : configFiles) {
            System.setProperty("generic.config.file", configFile);
            
            // Load configuration
            GenericApiConfig config = GenericApiConfig.loadFromFile();
            
            // Start application
            GenericApiApplication application = new GenericApiApplication();
            application.initializeForTesting();
            Javalin app = application.getApp();

            JavalinTest.test(app, (server, client) -> {
                // Get paths from Management API
                var response = client.get("/api/management/config/paths");
                assertThat(response.code()).isEqualTo(200);
                
                JsonNode paths = objectMapper.readTree(response.body().string());
                
                // Verify paths match configuration
                assertThat(paths.get("databases").asText()).isEqualTo(config.getDatabasesConfigPath());
                assertThat(paths.get("queries").asText()).isEqualTo(config.getQueriesConfigPath());
                assertThat(paths.get("endpoints").asText()).isEqualTo(config.getEndpointsConfigPath());
                
                logger.info("Verified path consistency for configuration: {}", configFile);
            });
            
            application.stop();
        }
    }

    @Test
    void testConfigurationDataIntegrityValidation() {
        // Arrange - Use test configuration
        System.setProperty("generic.config.file", "application-test.yml");
        
        // Load configurations directly
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        ConfigurationLoader loader = new ConfigurationLoader(config);
        
        Map<String, DatabaseConfig> databases = loader.loadDatabaseConfigurations();
        Map<String, QueryConfig> queries = loader.loadQueryConfigurations();
        Map<String, ApiEndpointConfig> endpoints = loader.loadEndpointConfigurations();
        
        // Verify referential integrity
        for (QueryConfig query : queries.values()) {
            if (query.getDatabase() != null) {
                assertThat(databases).containsKey(query.getDatabase())
                    .as("Query '%s' references database '%s' which should exist", 
                        query.getName(), query.getDatabase());
            }
        }
        
        for (ApiEndpointConfig endpoint : endpoints.values()) {
            if (endpoint.getQuery() != null) {
                assertThat(queries).containsKey(endpoint.getQuery())
                    .as("Endpoint '%s' references query '%s' which should exist", 
                        endpoint.getPath(), endpoint.getQuery());
            }
            
            if (endpoint.getCountQuery() != null) {
                assertThat(queries).containsKey(endpoint.getCountQuery())
                    .as("Endpoint '%s' references count query '%s' which should exist", 
                        endpoint.getPath(), endpoint.getCountQuery());
            }
        }
        
        logger.info("Verified configuration data integrity and referential consistency");
    }

    @Test
    void testConfigurationTimestampConsistency() {
        // Arrange - Use test configuration
        System.setProperty("generic.config.file", "application-test.yml");
        
        // Record test start time
        Instant testStart = Instant.now();
        
        // Start application
        GenericApiApplication application = new GenericApiApplication();
        application.initializeForTesting();
        Javalin app = application.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Get metadata multiple times to check timestamp consistency
            var response1 = client.get("/api/management/config/metadata");
            var response2 = client.get("/api/management/config/metadata");
            
            assertThat(response1.code()).isEqualTo(200);
            assertThat(response2.code()).isEqualTo(200);
            
            JsonNode metadata1 = objectMapper.readTree(response1.body().string());
            JsonNode metadata2 = objectMapper.readTree(response2.body().string());
            
            // Service start time should be consistent
            String startTime1 = metadata1.get("serviceStartTime").asText();
            String startTime2 = metadata2.get("serviceStartTime").asText();
            assertThat(startTime1).isEqualTo(startTime2);
            
            // Last refresh should be recent and reasonable
            String lastRefresh1 = metadata1.get("lastRefresh").asText();
            String lastRefresh2 = metadata2.get("lastRefresh").asText();
            
            Instant refresh1 = Instant.parse(lastRefresh1);
            Instant refresh2 = Instant.parse(lastRefresh2);
            
            assertThat(refresh1).isAfter(testStart.minusSeconds(10));
            assertThat(refresh2).isAfter(testStart.minusSeconds(10));
            assertThat(refresh2).isAfterOrEqualTo(refresh1); // Second call should be same or later
            
            logger.info("Verified configuration timestamp consistency");
        });
    }

    @Test
    void testConfigurationContentsConsistency() {
        // Arrange - Use test configuration
        System.setProperty("generic.config.file", "application-test.yml");
        
        // Load configuration for comparison
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        
        // Start application
        GenericApiApplication application = new GenericApiApplication();
        application.initializeForTesting();
        Javalin app = application.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Get configuration contents
            var response = client.get("/api/management/config/contents");
            assertThat(response.code()).isEqualTo(200);
            
            JsonNode contents = objectMapper.readTree(response.body().string());
            
            // Verify contents structure matches expected configuration
            assertThat(contents.has("databases")).isTrue();
            assertThat(contents.has("queries")).isTrue();
            assertThat(contents.has("endpoints")).isTrue();
            
            // Verify each configuration type has correct path information
            JsonNode dbContent = contents.get("databases");
            assertThat(dbContent.get("path").asText()).isEqualTo(config.getDatabasesConfigPath());
            assertThat(dbContent.get("status").asText()).isEqualTo("Available");
            
            JsonNode queryContent = contents.get("queries");
            assertThat(queryContent.get("path").asText()).isEqualTo(config.getQueriesConfigPath());
            assertThat(queryContent.get("status").asText()).isEqualTo("Available");
            
            JsonNode endpointContent = contents.get("endpoints");
            assertThat(endpointContent.get("path").asText()).isEqualTo(config.getEndpointsConfigPath());
            assertThat(endpointContent.get("status").asText()).isEqualTo("Available");
            
            logger.info("Verified configuration contents consistency");
        });
    }

    @Test
    void testCrossValidationBetweenConfigurationAndManagementApis() {
        // Comprehensive cross-validation test
        
        // Arrange - Use test configuration
        System.setProperty("generic.config.file", "application-test.yml");
        
        // Load all configurations directly
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        ConfigurationLoader loader = new ConfigurationLoader(config);
        
        Map<String, DatabaseConfig> directDatabases = loader.loadDatabaseConfigurations();
        Map<String, QueryConfig> directQueries = loader.loadQueryConfigurations();
        Map<String, ApiEndpointConfig> directEndpoints = loader.loadEndpointConfigurations();
        
        // Start application
        GenericApiApplication application = new GenericApiApplication();
        application.initializeForTesting();
        Javalin app = application.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Get comprehensive dashboard data
            var dashboardResponse = client.get("/api/management/dashboard");
            assertThat(dashboardResponse.code()).isEqualTo(200);
            
            JsonNode dashboard = objectMapper.readTree(dashboardResponse.body().string());
            JsonNode configSection = dashboard.get("configuration");
            
            // Cross-validate counts
            assertThat(configSection.get("databases").get("count").asInt())
                .isEqualTo(directDatabases.size());
            assertThat(configSection.get("queries").get("count").asInt())
                .isEqualTo(directQueries.size());
            assertThat(configSection.get("endpoints").get("count").asInt())
                .isEqualTo(directEndpoints.size());
            
            // Get individual API responses for detailed validation
            var dbResponse = client.get("/api/management/config/databases");
            var queryResponse = client.get("/api/management/config/queries");
            var endpointResponse = client.get("/api/management/config/endpoints");
            
            JsonNode dbData = objectMapper.readTree(dbResponse.body().string());
            JsonNode queryData = objectMapper.readTree(queryResponse.body().string());
            JsonNode endpointData = objectMapper.readTree(endpointResponse.body().string());
            
            // Validate that dashboard counts match individual API counts
            assertThat(configSection.get("databases").get("count").asInt())
                .isEqualTo(dbData.get("count").asInt());
            assertThat(configSection.get("queries").get("count").asInt())
                .isEqualTo(queryData.get("count").asInt());
            assertThat(configSection.get("endpoints").get("count").asInt())
                .isEqualTo(endpointData.get("count").asInt());
            
            logger.info("Successfully cross-validated all configuration data sources");
        });
    }
}

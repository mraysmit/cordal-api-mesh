package dev.cordal.generic.management;

import dev.cordal.config.GenericApiConfig;
import dev.cordal.database.DatabaseManager;
import dev.cordal.database.repository.DatabaseConfigurationRepository;
import dev.cordal.database.repository.QueryConfigurationRepository;
import dev.cordal.database.repository.EndpointConfigurationRepository;
import dev.cordal.database.loader.DatabaseConfigurationLoader;
import dev.cordal.generic.config.ConfigurationLoader;
import dev.cordal.generic.config.ConfigurationLoaderFactory;
import dev.cordal.generic.config.EndpointConfigurationManager;
import dev.cordal.generic.config.DatabaseConfig;
import dev.cordal.generic.config.QueryConfig;
import dev.cordal.generic.config.ApiEndpointConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Test configuration management APIs
 */
class ConfigurationManagementApiTest {

    private ConfigurationManagementService configManagementService;
    private DatabaseManager databaseManager;
    private GenericApiConfig config;

    @BeforeEach
    void setUp() {
        // Create isolated test configuration that doesn't read from production generic-config
        System.setProperty("generic.config.file", "application-isolated-test.yml");
        config = GenericApiConfig.loadFromFile();
        
        // Create database manager and initialize schema
        databaseManager = new DatabaseManager(config);
        databaseManager.initializeSchema();
        
        // Create repositories
        DatabaseConfigurationRepository databaseRepository = new DatabaseConfigurationRepository(databaseManager);
        QueryConfigurationRepository queryRepository = new QueryConfigurationRepository(databaseManager);
        EndpointConfigurationRepository endpointRepository = new EndpointConfigurationRepository(databaseManager);
        
        // Create loaders
        ConfigurationLoader yamlLoader = new ConfigurationLoader(config);
        DatabaseConfigurationLoader databaseLoader = new DatabaseConfigurationLoader(databaseRepository, queryRepository, endpointRepository);
        
        // Create factory and manager
        ConfigurationLoaderFactory factory = new ConfigurationLoaderFactory(config, yamlLoader, databaseLoader);
        EndpointConfigurationManager configManager = new EndpointConfigurationManager(factory);
        
        // Create configuration management service
        configManagementService = new ConfigurationManagementService(
            databaseRepository, queryRepository, endpointRepository, factory, configManager
        );
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("generic.config.file");
    }

    @Test
    void testConfigurationSourceInfo() {
        // Test getting configuration source information
        Map<String, Object> sourceInfo = configManagementService.getConfigurationSourceInfo();
        
        assertThat(sourceInfo).isNotNull();
        assertThat(sourceInfo).containsKey("currentSource");
        assertThat(sourceInfo).containsKey("managementAvailable");
        assertThat(sourceInfo).containsKey("supportedSources");
        assertThat(sourceInfo).containsKey("timestamp");
        
        // Should be YAML source by default in test
        assertThat(sourceInfo.get("currentSource")).isEqualTo("yaml");
        assertThat(sourceInfo.get("managementAvailable")).isEqualTo(false);
    }

    @Test
    void testConfigurationStatistics() {
        // Test getting configuration statistics
        Map<String, Object> stats = configManagementService.getConfigurationStatistics();
        
        assertThat(stats).isNotNull();
        assertThat(stats).containsKey("source");
        assertThat(stats).containsKey("timestamp");
        assertThat(stats).containsKey("statistics");
        assertThat(stats).containsKey("summary");
        
        // Check statistics structure
        Map<String, Object> statistics = (Map<String, Object>) stats.get("statistics");
        assertThat(statistics).containsKey("databases");
        assertThat(statistics).containsKey("queries");
        assertThat(statistics).containsKey("endpoints");
        
        Map<String, Object> summary = (Map<String, Object>) stats.get("summary");
        assertThat(summary).containsKey("totalConfigurations");
        assertThat(summary).containsKey("databasesCount");
        assertThat(summary).containsKey("queriesCount");
        assertThat(summary).containsKey("endpointsCount");
    }

    @Test
    void testGetAllDatabaseConfigurations() {
        // Test getting all database configurations
        Map<String, Object> result = configManagementService.getAllDatabaseConfigurations();
        
        assertThat(result).isNotNull();
        assertThat(result).containsKey("count");
        assertThat(result).containsKey("source");
        assertThat(result).containsKey("timestamp");
        assertThat(result).containsKey("databases");
        
        // Should have configurations from YAML source
        assertThat(result.get("count")).isEqualTo(2); // 2 databases in test config (stock-trades-db, metrics-db)
        assertThat(result.get("source")).isEqualTo("yaml");
    }

    @Test
    void testGetAllQueryConfigurations() {
        // Test getting all query configurations
        Map<String, Object> result = configManagementService.getAllQueryConfigurations();
        
        assertThat(result).isNotNull();
        assertThat(result).containsKey("count");
        assertThat(result).containsKey("source");
        assertThat(result).containsKey("timestamp");
        assertThat(result).containsKey("queries");
        
        // Should have configurations from YAML source
        assertThat(result.get("count")).isEqualTo(12); // 12 queries in test config
        assertThat(result.get("source")).isEqualTo("yaml");
    }

    @Test
    void testGetAllEndpointConfigurations() {
        // Test getting all endpoint configurations
        Map<String, Object> result = configManagementService.getAllEndpointConfigurations();
        
        assertThat(result).isNotNull();
        assertThat(result).containsKey("count");
        assertThat(result).containsKey("source");
        assertThat(result).containsKey("timestamp");
        assertThat(result).containsKey("endpoints");
        
        // Should have configurations from YAML source
        assertThat(result.get("count")).isEqualTo(6); // 6 endpoints in test config
        assertThat(result.get("source")).isEqualTo("yaml");
    }

    @Test
    void testGetNonExistentConfiguration() {
        // Test getting non-existent configurations
        Optional<DatabaseConfig> dbConfig = configManagementService.getDatabaseConfiguration("non-existent");
        assertThat(dbConfig).isEmpty();
        
        Optional<QueryConfig> queryConfig = configManagementService.getQueryConfiguration("non-existent");
        assertThat(queryConfig).isEmpty();
        
        Optional<ApiEndpointConfig> endpointConfig = configManagementService.getEndpointConfiguration("non-existent");
        assertThat(endpointConfig).isEmpty();
    }

    @Test
    void testConfigurationManagementNotAvailableWithYamlSource() {
        // Test that configuration management operations fail with YAML source
        DatabaseConfig testDbConfig = new DatabaseConfig();
        testDbConfig.setName("test-db");
        testDbConfig.setUrl("jdbc:h2:mem:test");
        testDbConfig.setDriver("org.h2.Driver");
        
        // Should throw IllegalStateException because we're using YAML source
        assertThatThrownBy(() -> configManagementService.saveDatabaseConfiguration("test-db", testDbConfig))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Configuration management is only available when using database source");
        
        assertThatThrownBy(() -> configManagementService.deleteDatabaseConfiguration("test-db"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Configuration management is only available when using database source");
    }

    @Test
    void testIsConfigurationManagementAvailable() {
        // Should return false when using YAML source
        boolean available = configManagementService.isConfigurationManagementAvailable();
        assertThat(available).isFalse();
    }
}

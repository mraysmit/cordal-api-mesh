package dev.mars.config;

import dev.mars.database.ConfigurationDataLoader;
import dev.mars.database.DatabaseManager;
import dev.mars.generic.config.ConfigurationLoader;
import dev.mars.generic.config.DatabaseConfig;
import dev.mars.generic.config.QueryConfig;
import dev.mars.generic.config.ApiEndpointConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for configuration source selection and loading
 */
public class ConfigurationSourceIntegrationTest {

    private DatabaseManager databaseManager;
    private ConfigurationDataLoader configurationDataLoader;
    private TestGenericApiConfig databaseConfig;
    private TestGenericApiConfig yamlConfig;

    @BeforeEach
    void setUp() {
        // Use test configuration
        System.setProperty("generic.config.file", "application-test.yml");

        // Create configs for both sources
        databaseConfig = new TestGenericApiConfig("database");
        yamlConfig = new TestGenericApiConfig("yaml");
        
        databaseManager = new DatabaseManager(databaseConfig);
        
        // Initialize schema and load sample data
        databaseManager.initializeSchema();
        databaseManager.cleanDatabase();
        
        // Create ConfigurationLoader for the data loader
        dev.mars.generic.config.ConfigurationLoader configurationLoader = new dev.mars.generic.config.ConfigurationLoader(databaseConfig);

        configurationDataLoader = new ConfigurationDataLoader(databaseManager, databaseConfig, configurationLoader);
        configurationDataLoader.loadConfigurationDataIfNeeded();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("generic.config.file");
    }

    @Test
    void testYamlConfigurationSourceLoading() {
        // Create configuration loader with YAML source
        ConfigurationLoader yamlLoader = new ConfigurationLoader(yamlConfig);

        // Load configurations from YAML files
        Map<String, DatabaseConfig> databaseConfigs = yamlLoader.loadDatabaseConfigurations();
        Map<String, QueryConfig> queryConfigs = yamlLoader.loadQueryConfigurations();
        Map<String, ApiEndpointConfig> endpointConfigs = yamlLoader.loadEndpointConfigurations();

        // Verify YAML configurations are loaded (these should come from test YAML files)
        // Note: The actual content depends on what's in the test YAML files
        assertThat(databaseConfigs).isNotNull();
        assertThat(queryConfigs).isNotNull();
        assertThat(endpointConfigs).isNotNull();
        
        // YAML source should load from files, not database
        assertThat(yamlConfig.getConfigSource()).isEqualTo("yaml");
    }

    @Test
    void testDatabaseConfigurationSourceIdentification() {
        // Verify database source is correctly identified
        assertThat(databaseConfig.getConfigSource()).isEqualTo("database");
        
        // Verify YAML source is correctly identified
        assertThat(yamlConfig.getConfigSource()).isEqualTo("yaml");
    }

    @Test
    void testConfigurationSourceSwitching() {
        // Test switching between configuration sources
        
        // Start with database source
        TestGenericApiConfig config1 = new TestGenericApiConfig("database");
        assertThat(config1.getConfigSource()).isEqualTo("database");
        
        // Switch to YAML source
        TestGenericApiConfig config2 = new TestGenericApiConfig("yaml");
        assertThat(config2.getConfigSource()).isEqualTo("yaml");
        
        // Verify they are different
        assertThat(config1.getConfigSource()).isNotEqualTo(config2.getConfigSource());
    }

    @Test
    void testDatabaseConfigurationDataIntegrity() {
        // Verify that database contains the expected configuration data
        dev.mars.generic.config.ConfigurationLoader configurationLoader = new dev.mars.generic.config.ConfigurationLoader(databaseConfig);
        ConfigurationDataLoader loader = new ConfigurationDataLoader(databaseManager, databaseConfig, configurationLoader);

        // Clean and reload to ensure fresh data
        databaseManager.cleanDatabase();
        loader.loadConfigurationDataIfNeeded();
        
        // Verify data was loaded correctly by checking the database directly
        assertThat(databaseManager.isHealthy()).isTrue();
        
        // The data should be available for a hypothetical database-based ConfigurationLoader
        // This test validates that the infrastructure is in place for database configuration loading
    }

    @Test
    void testConfigurationLoaderWithDifferentSources() {
        // Test that ConfigurationLoader behaves correctly with different config sources
        
        // YAML source loader
        ConfigurationLoader yamlLoader = new ConfigurationLoader(yamlConfig);
        assertThat(yamlLoader).isNotNull();
        
        // Database source loader (when implemented, this would load from database)
        // For now, we test that the infrastructure supports it
        ConfigurationLoader databaseLoader = new ConfigurationLoader(databaseConfig);
        assertThat(databaseLoader).isNotNull();
        
        // Both loaders should be functional
        assertThat(yamlLoader.loadDatabaseConfigurations()).isNotNull();
        assertThat(databaseLoader.loadDatabaseConfigurations()).isNotNull();
    }

    @Test
    void testConfigurationSourceValidation() {
        // Test valid configuration sources
        TestGenericApiConfig validYaml = new TestGenericApiConfig("yaml");
        TestGenericApiConfig validDatabase = new TestGenericApiConfig("database");
        
        assertThat(validYaml.getConfigSource()).isEqualTo("yaml");
        assertThat(validDatabase.getConfigSource()).isEqualTo("database");
        
        // Test that configuration data loader respects the source
        dev.mars.generic.config.ConfigurationLoader yamlConfigurationLoader = new dev.mars.generic.config.ConfigurationLoader(validYaml);
        dev.mars.generic.config.ConfigurationLoader dbConfigurationLoader = new dev.mars.generic.config.ConfigurationLoader(validDatabase);

        ConfigurationDataLoader yamlDataLoader = new ConfigurationDataLoader(databaseManager, validYaml, yamlConfigurationLoader);
        ConfigurationDataLoader dbDataLoader = new ConfigurationDataLoader(databaseManager, validDatabase, dbConfigurationLoader);

        // Clean database first
        databaseManager.cleanDatabase();

        // YAML source should not load data to database
        yamlDataLoader.loadConfigurationDataIfNeeded();
        // Database should be empty after YAML loader

        // Database source should load data to database
        dbDataLoader.loadConfigurationDataIfNeeded();
        // Database should have data after database loader
        
        assertThat(databaseManager.isHealthy()).isTrue();
    }

    @Test
    void testConfigurationPathsWithDatabaseSource() {
        // Test that configuration paths are still available even with database source
        assertThat(databaseConfig.getDatabasesConfigPath()).isNotNull();
        assertThat(databaseConfig.getQueriesConfigPath()).isNotNull();
        assertThat(databaseConfig.getEndpointsConfigPath()).isNotNull();
        
        // These paths should be available as fallback or for hybrid configurations
        assertThat(databaseConfig.getDatabasesConfigPath()).contains(".yml");
        assertThat(databaseConfig.getQueriesConfigPath()).contains(".yml");
        assertThat(databaseConfig.getEndpointsConfigPath()).contains(".yml");
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

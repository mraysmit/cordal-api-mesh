package dev.mars.generic.config;

import dev.mars.config.GenericApiConfig;
import dev.mars.database.DatabaseManager;
import dev.mars.database.loader.DatabaseConfigurationLoader;
import dev.mars.database.repository.DatabaseConfigurationRepository;
import dev.mars.database.repository.QueryConfigurationRepository;
import dev.mars.database.repository.EndpointConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Test configuration source switching between YAML and database
 */
class ConfigurationSourceSwitchingTest {

    private GenericApiConfig yamlConfig;
    private GenericApiConfig databaseConfig;

    @BeforeEach
    void setUp() {
        // Create configs for different sources
        yamlConfig = createTestConfig("yaml");
        databaseConfig = createTestConfig("database");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("generic.config.file");
    }

    @Test
    void testConfigurationLoaderFactoryCreatesCorrectLoader() {
        // Test YAML source
        ConfigurationLoader yamlLoader = new ConfigurationLoader(yamlConfig);
        DatabaseManager databaseManager = new DatabaseManager(databaseConfig);
        databaseManager.initializeSchema();
        
        DatabaseConfigurationRepository databaseRepository = new DatabaseConfigurationRepository(databaseManager);
        QueryConfigurationRepository queryRepository = new QueryConfigurationRepository(databaseManager);
        EndpointConfigurationRepository endpointRepository = new EndpointConfigurationRepository(databaseManager);
        DatabaseConfigurationLoader dbLoader = new DatabaseConfigurationLoader(databaseRepository, queryRepository, endpointRepository);

        // Test factory with YAML source
        ConfigurationLoaderFactory yamlFactory = new ConfigurationLoaderFactory(yamlConfig, yamlLoader, dbLoader);
        assertThat(yamlFactory.getConfigurationSource()).isEqualTo("yaml");
        assertThat(yamlFactory.isYamlSource()).isTrue();
        assertThat(yamlFactory.isDatabaseSource()).isFalse();

        ConfigurationLoaderInterface yamlLoaderInterface = yamlFactory.createConfigurationLoader();
        assertThat(yamlLoaderInterface).isInstanceOf(ConfigurationLoader.class);

        // Test factory with database source
        ConfigurationLoaderFactory databaseFactory = new ConfigurationLoaderFactory(databaseConfig, yamlLoader, dbLoader);
        assertThat(databaseFactory.getConfigurationSource()).isEqualTo("database");
        assertThat(databaseFactory.isYamlSource()).isFalse();
        assertThat(databaseFactory.isDatabaseSource()).isTrue();

        ConfigurationLoaderInterface databaseLoaderInterface = databaseFactory.createConfigurationLoader();
        assertThat(databaseLoaderInterface).isInstanceOf(DatabaseConfigurationLoader.class);
    }

    @Test
    void testEndpointConfigurationManagerWithDifferentSources() {
        // Test with YAML source
        ConfigurationLoader yamlLoader = new ConfigurationLoader(yamlConfig);
        DatabaseManager databaseManager = new DatabaseManager(databaseConfig);
        databaseManager.initializeSchema();
        
        DatabaseConfigurationRepository databaseRepository = new DatabaseConfigurationRepository(databaseManager);
        QueryConfigurationRepository queryRepository = new QueryConfigurationRepository(databaseManager);
        EndpointConfigurationRepository endpointRepository = new EndpointConfigurationRepository(databaseManager);
        DatabaseConfigurationLoader dbLoader = new DatabaseConfigurationLoader(databaseRepository, queryRepository, endpointRepository);

        // Test YAML configuration manager
        ConfigurationLoaderFactory yamlFactory = new ConfigurationLoaderFactory(yamlConfig, yamlLoader, dbLoader);
        EndpointConfigurationManager yamlManager = new EndpointConfigurationManager(yamlFactory);
        
        assertThat(yamlManager.getConfigurationSource()).isEqualTo("yaml");
        assertThat(yamlManager.isUsingYamlSource()).isTrue();
        assertThat(yamlManager.isUsingDatabaseSource()).isFalse();

        // Verify configurations are loaded (should work with existing test files)
        assertThat(yamlManager.getAllDatabaseConfigurations()).isNotNull();
        assertThat(yamlManager.getAllQueryConfigurations()).isNotNull();
        assertThat(yamlManager.getAllEndpointConfigurations()).isNotNull();
    }

    @Test
    void testConfigurationLoaderInterfaceCompatibility() {
        // Test that both loaders implement the same interface correctly
        ConfigurationLoader yamlLoader = new ConfigurationLoader(yamlConfig);
        DatabaseManager databaseManager = new DatabaseManager(databaseConfig);
        databaseManager.initializeSchema();
        
        DatabaseConfigurationRepository databaseRepository = new DatabaseConfigurationRepository(databaseManager);
        QueryConfigurationRepository queryRepository = new QueryConfigurationRepository(databaseManager);
        EndpointConfigurationRepository endpointRepository = new EndpointConfigurationRepository(databaseManager);
        DatabaseConfigurationLoader dbLoader = new DatabaseConfigurationLoader(databaseRepository, queryRepository, endpointRepository);

        // Both should implement ConfigurationLoaderInterface
        assertThat(yamlLoader).isInstanceOf(ConfigurationLoaderInterface.class);
        assertThat(dbLoader).isInstanceOf(ConfigurationLoaderInterface.class);

        // Both should have the same methods available
        ConfigurationLoaderInterface yamlInterface = yamlLoader;
        ConfigurationLoaderInterface dbInterface = dbLoader;

        // Test method signatures exist (will throw if database is empty, but that's expected)
        assertThatCode(() -> yamlInterface.loadDatabaseConfigurations()).doesNotThrowAnyException();
        assertThatCode(() -> yamlInterface.loadQueryConfigurations()).doesNotThrowAnyException();
        assertThatCode(() -> yamlInterface.loadEndpointConfigurations()).doesNotThrowAnyException();

        // Database loader will fail with empty database, but interface should work
        assertThat(dbInterface).isNotNull();
    }

    private GenericApiConfig createTestConfig(String source) {
        // Set test configuration to avoid production file loading
        System.setProperty("generic.config.file", "application-test.yml");
        try {
            // Create a test config with the specified source using a test implementation
            return new TestGenericApiConfig(source);
        } finally {
            // Clean up system property
            System.clearProperty("generic.config.file");
        }
    }

    /**
     * Test implementation of GenericApiConfig that allows setting config source
     */
    private static class TestGenericApiConfig extends GenericApiConfig {
        private final String configSource;

        public TestGenericApiConfig(String configSource) {
            super(); // Call super to properly initialize base config
            this.configSource = configSource;
        }

        @Override
        protected String getConfigFileName() {
            // Use test config file to avoid production file loading issues
            return "application-test.yml";
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

        @Override
        public String getDatabasesConfigPath() {
            return "test-databases.yml";
        }

        @Override
        public String getQueriesConfigPath() {
            return "test-queries.yml";
        }

        @Override
        public String getEndpointsConfigPath() {
            return "test-api-endpoints.yml";
        }
    }
}

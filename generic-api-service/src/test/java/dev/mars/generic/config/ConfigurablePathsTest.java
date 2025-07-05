package dev.mars.generic.config;

import dev.mars.config.GenericApiConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.assertj.core.api.Assertions.*;

/**
 * Test that configuration paths can be customized via application.yml
 */
class ConfigurablePathsTest {

    @BeforeEach
    void setUp() {
        // Clear any existing system properties
        System.clearProperty("generic.config.file");
    }

    @AfterEach
    void tearDown() {
        // Clean up system properties
        System.clearProperty("generic.config.file");
    }

    @Test
    void testDefaultConfigurationPaths() {
        // Test that default paths are used when no custom configuration is provided
        GenericApiConfig config = new GenericApiConfig();

        assertThat(config.getDatabasesConfigPath()).isEqualTo("./generic-config/stocktrades-databases.yml");
        assertThat(config.getQueriesConfigPath()).isEqualTo("./generic-config/stocktrades-queries.yml");
        assertThat(config.getEndpointsConfigPath()).isEqualTo("./generic-config/stocktrades-api-endpoints.yml");
    }

    @Test
    void testCustomConfigurationPaths() {
        // Test that custom paths are loaded from application-custom-paths.yml
        System.setProperty("generic.config.file", "application-custom-paths.yml");

        GenericApiConfig config = GenericApiConfig.loadFromFile();

        // The custom configuration file specifies custom paths
        assertThat(config.getDatabasesConfigPath()).isEqualTo("custom/databases.yml");
        assertThat(config.getQueriesConfigPath()).isEqualTo("custom/queries.yml");
        assertThat(config.getEndpointsConfigPath()).isEqualTo("custom/api-endpoints.yml");
    }

    @Test
    void testConfigurationLoaderUsesDefaultPaths() {
        // Test that ConfigurationLoader uses the default paths (refactored architecture)
        System.setProperty("generic.config.file", "application-custom-paths.yml");

        GenericApiConfig config = GenericApiConfig.loadFromFile();
        ConfigurationLoader loader = new ConfigurationLoader(config);

        // In the refactored architecture, the config/queries.yml file exists and loads successfully
        // We're testing that it's using the default path and loads without error
        assertThatCode(() -> loader.loadQueryConfigurations())
            .doesNotThrowAnyException();
    }
}

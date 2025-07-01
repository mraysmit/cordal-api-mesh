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
        System.clearProperty("config.file");
    }

    @AfterEach
    void tearDown() {
        // Clean up system properties
        System.clearProperty("config.file");
    }

    @Test
    void testDefaultConfigurationPaths() {
        // Test that default paths are used when no custom configuration is provided
        GenericApiConfig config = new GenericApiConfig();
        
        assertThat(config.getDatabasesConfigPath()).isEqualTo("config/databases.yml");
        assertThat(config.getQueriesConfigPath()).isEqualTo("config/queries.yml");
        assertThat(config.getEndpointsConfigPath()).isEqualTo("config/api-endpoints.yml");
    }

    @Test
    void testCustomConfigurationPaths() {
        // Test that custom paths are loaded from application.yml
        System.setProperty("config.file", "application-custom-paths.yml");
        
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        
        assertThat(config.getDatabasesConfigPath()).isEqualTo("custom/databases.yml");
        assertThat(config.getQueriesConfigPath()).isEqualTo("custom/queries.yml");
        assertThat(config.getEndpointsConfigPath()).isEqualTo("custom/api-endpoints.yml");
    }

    @Test
    void testConfigurationLoaderUsesCustomPaths() {
        // Test that ConfigurationLoader uses the paths from GenericApiConfig
        System.setProperty("config.file", "application-custom-paths.yml");
        
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        ConfigurationLoader loader = new ConfigurationLoader(config);
        
        // This should attempt to load from custom/queries.yml and fail since it doesn't exist
        // We're testing that it's using the custom path, not the default path
        assertThatThrownBy(() -> loader.loadQueryConfigurations())
            .isInstanceOf(RuntimeException.class)
            .hasRootCauseMessage("custom/queries.yml not found in classpath");
    }
}

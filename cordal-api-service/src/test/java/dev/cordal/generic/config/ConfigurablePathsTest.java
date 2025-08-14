package dev.cordal.generic.config;

import dev.cordal.config.GenericApiConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.assertj.core.api.Assertions.*;

/**
 * Test that configuration directories and patterns can be customized via application.yml
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
    void testDefaultConfigurationDirectoriesAndPatterns() {
        // Test that default directories and patterns are used when no custom configuration is provided
        GenericApiConfig config = new GenericApiConfig();

        assertThat(config.getConfigDirectories()).contains("../generic-config");
        assertThat(config.getDatabasePatterns()).contains("*-database.yml", "*-databases.yml");
        assertThat(config.getQueryPatterns()).contains("*-query.yml", "*-queries.yml");
        assertThat(config.getEndpointPatterns()).contains("*-endpoint.yml", "*-endpoints.yml", "*-api.yml");
    }

    @Test
    void testCustomConfigurationDirectoriesAndPatterns() {
        // Test that custom directories and patterns are loaded from application-custom-patterns-test.yml
        System.setProperty("generic.config.file", "application-custom-patterns-test.yml");

        GenericApiConfig config = GenericApiConfig.loadFromFile();

        // The custom configuration file specifies custom patterns
        assertThat(config.getConfigDirectories()).contains("../generic-config");
        assertThat(config.getDatabasePatterns()).contains("*-db-config.yml", "*-database-config.yml", "*-databases.yml");
        assertThat(config.getQueryPatterns()).contains("*-sql.yml", "*-query-config.yml", "*-queries.yml");
        assertThat(config.getEndpointPatterns()).contains("*-rest-api.yml", "*-endpoint-config.yml", "*-api.yml");
    }

    @Test
    void testConfigurationLoaderUsesDirectoryScanning() {
        // Test that ConfigurationLoader uses directory scanning (refactored architecture)
        System.setProperty("generic.config.file", "application-test.yml");

        GenericApiConfig config = GenericApiConfig.loadFromFile();
        ConfigurationLoader loader = new ConfigurationLoader(config);

        // In the refactored architecture, configuration files are discovered by directory scanning
        // We're testing that it uses directory scanning and loads without error
        assertThatCode(() -> {
            loader.loadDatabaseConfigurations();
            loader.loadQueryConfigurations();
            loader.loadEndpointConfigurations();
        }).doesNotThrowAnyException();
    }
}

package dev.mars.generic.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import dev.mars.config.GenericApiConfig;
import dev.mars.generic.config.ConfigurationLoader;
import dev.mars.generic.TestConfigurationLoader;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for EndpointConfigurationManager using real components
 */
class EndpointConfigurationManagerTest {

    private EndpointConfigurationManager manager;

    @BeforeEach
    void setUp() {
        // Use test configuration
        System.setProperty("generic.config.file", "application-test.yml");

        // Create manager with test configuration loader
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        ConfigurationLoader configurationLoader = new TestConfigurationLoader(config);
        manager = new EndpointConfigurationManager(configurationLoader);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("generic.config.file");
    }

    @Test
    void testManagerExists() {
        // Test that the manager can be instantiated
        assertThat(manager).isNotNull();
    }

    @Test
    void testGetQueryConfig_Found() {
        // Test getting a known query configuration
        var result = manager.getQueryConfig("stock-trades-all");
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("stock-trades-all");
    }

    @Test
    void testGetQueryConfig_NotFound() {
        // Test getting a non-existent query configuration
        var result = manager.getQueryConfig("nonexistent-query");
        assertThat(result).isEmpty();
    }

    @Test
    void testGetEndpointConfig_Found() {
        // Test getting a known endpoint configuration
        var result = manager.getEndpointConfig("stock-trades-list");
        assertThat(result).isPresent();
        assertThat(result.get().getPath()).isEqualTo("/api/generic/stock-trades");
        assertThat(result.get().getQuery()).isEqualTo("stock-trades-all");
    }

    @Test
    void testGetEndpointConfig_NotFound() {
        // Test getting a non-existent endpoint configuration
        var result = manager.getEndpointConfig("nonexistent-endpoint");
        assertThat(result).isEmpty();
    }

    @Test
    void testGetAllQueryConfigurations() {
        // Test getting all query configurations
        var result = manager.getAllQueryConfigurations();
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result).containsKey("stock-trades-all");
        assertThat(result).containsKey("stock-trades-count");
    }

    @Test
    void testGetAllEndpointConfigurations() {
        // Test getting all endpoint configurations
        var result = manager.getAllEndpointConfigurations();
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result).containsKey("stock-trades-list");
    }

    @Test
    void testHasQuery_Exists() {
        // Test checking for existing queries
        assertThat(manager.hasQuery("stock-trades-all")).isTrue();
        assertThat(manager.hasQuery("stock-trades-count")).isTrue();
    }

    @Test
    void testHasQuery_NotExists() {
        // Test checking for non-existent queries
        assertThat(manager.hasQuery("nonexistent-query")).isFalse();
    }

    @Test
    void testHasEndpoint_Exists() {
        // Test checking for existing endpoints
        assertThat(manager.hasEndpoint("stock-trades-list")).isTrue();
    }

    @Test
    void testHasEndpoint_NotExists() {
        // Test checking for non-existent endpoints
        assertThat(manager.hasEndpoint("nonexistent-endpoint")).isFalse();
    }

    @Test
    void testValidateConfigurations_Success() {
        // Test that configuration validation works
        assertThatCode(() -> manager.validateConfigurations())
            .doesNotThrowAnyException();
    }

}

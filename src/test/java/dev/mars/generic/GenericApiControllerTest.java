package dev.mars.generic;

import dev.mars.database.DatabaseManager;
import dev.mars.generic.config.ConfigurationLoader;
import dev.mars.generic.config.EndpointConfigurationManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for GenericApiController using real components
 */
class GenericApiControllerTest {

    private GenericApiController controller;
    private GenericApiService genericApiService;
    private GenericRepository genericRepository;
    private EndpointConfigurationManager configurationManager;
    private DatabaseManager databaseManager;

    @BeforeEach
    void setUp() throws SQLException {
        // Use test configuration
        System.setProperty("config.file", "application-test.yml");

        // Create components manually to avoid Guice module complexity in tests
        var appConfig = new dev.mars.config.AppConfig();
        var databaseConfig = new dev.mars.config.DatabaseConfig(appConfig);
        databaseManager = new DatabaseManager(databaseConfig);
        databaseManager.initializeSchema();
        databaseManager.cleanDatabase();

        // Create configuration components
        ConfigurationLoader configurationLoader = new ConfigurationLoader();
        configurationManager = new EndpointConfigurationManager(configurationLoader);

        // Create repository and service
        genericRepository = new GenericRepository(databaseManager);
        genericApiService = new GenericApiService(genericRepository, configurationManager);

        // Create controller
        controller = new GenericApiController(genericApiService);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("config.file");
    }

    @Test
    void testControllerExists() {
        // Test that the controller can be instantiated
        assertThat(controller).isNotNull();
        assertThat(genericApiService).isNotNull();
    }

    @Test
    void testGetAvailableEndpoints() {
        // Test that we can get available endpoints
        var endpoints = genericApiService.getAvailableEndpoints();
        assertThat(endpoints).isNotNull();
        // Should have at least the stock-trades-list endpoint from configuration
        assertThat(endpoints).containsKey("stock-trades-list");
    }

    @Test
    void testGetEndpointConfiguration() {
        // Test getting a specific endpoint configuration
        var endpointConfig = genericApiService.getEndpointConfiguration("stock-trades-list");
        assertThat(endpointConfig).isPresent();
        assertThat(endpointConfig.get().getPath()).isEqualTo("/api/generic/stock-trades");
        assertThat(endpointConfig.get().getMethod()).isEqualTo("GET");
    }

    @Test
    void testGetEndpointConfiguration_NotFound() {
        // Test getting a non-existent endpoint configuration
        var endpointConfig = genericApiService.getEndpointConfiguration("nonexistent-endpoint");
        assertThat(endpointConfig).isEmpty();
    }

    @Test
    void testConfigurationValidation() {
        // Test that configuration validation works
        assertThatCode(() -> configurationManager.validateConfigurations())
            .doesNotThrowAnyException();
    }

    @Test
    void testGetAllQueryConfigurations() {
        // Test getting all query configurations
        var queries = genericApiService.getAllQueryConfigurations();
        assertThat(queries).isNotNull();
        assertThat(queries).isNotEmpty();
        assertThat(queries).containsKey("stock-trades-all");
        assertThat(queries).containsKey("stock-trades-count");
    }

    @Test
    void testGetSpecificQueryConfiguration() {
        // Test getting a specific query configuration
        var queryConfig = genericApiService.getQueryConfiguration("stock-trades-all");
        assertThat(queryConfig).isPresent();
        assertThat(queryConfig.get().getName()).isEqualTo("stock-trades-all");
        assertThat(queryConfig.get().getDescription()).contains("Get All Stock Trades");
    }

    @Test
    void testGetNonexistentQueryConfiguration() {
        // Test getting a non-existent query configuration
        var queryConfig = genericApiService.getQueryConfiguration("nonexistent-query");
        assertThat(queryConfig).isEmpty();
    }
}

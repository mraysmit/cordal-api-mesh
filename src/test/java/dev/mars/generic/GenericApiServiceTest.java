package dev.mars.generic;

import dev.mars.database.DatabaseManager;
import dev.mars.exception.ApiException;
import dev.mars.generic.config.ApiEndpointConfig;
import dev.mars.generic.config.ConfigurationLoader;
import dev.mars.generic.config.EndpointConfigurationManager;
import dev.mars.generic.config.QueryConfig;
import dev.mars.generic.model.GenericResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for GenericApiService using real components
 */
class GenericApiServiceTest {

    private GenericApiService service;
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
        service = new GenericApiService(genericRepository, configurationManager);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("config.file");
    }

    @Test
    void testServiceExists() {
        // Test that the service can be instantiated
        assertThat(service).isNotNull();
        assertThat(configurationManager).isNotNull();
        assertThat(genericRepository).isNotNull();
    }

    @Test
    void testGetAvailableEndpoints() {
        // Test that we can get available endpoints
        var endpoints = service.getAvailableEndpoints();
        assertThat(endpoints).isNotNull();
        // Should have at least the stock-trades-list endpoint from configuration
        assertThat(endpoints).containsKey("stock-trades-list");
    }

    @Test
    void testGetEndpointConfiguration() {
        // Test getting a specific endpoint configuration
        var endpointConfig = service.getEndpointConfiguration("stock-trades-list");
        assertThat(endpointConfig).isPresent();
        assertThat(endpointConfig.get().getPath()).isEqualTo("/api/generic/stock-trades");
        assertThat(endpointConfig.get().getMethod()).isEqualTo("GET");
    }

    @Test
    void testGetEndpointConfiguration_NotFound() {
        // Test getting a non-existent endpoint configuration
        var endpointConfig = service.getEndpointConfiguration("nonexistent-endpoint");
        assertThat(endpointConfig).isEmpty();
    }

    @Test
    void testExecuteEndpoint_NotFound() {
        // Test executing a non-existent endpoint
        assertThatThrownBy(() -> service.executeEndpoint("nonexistent-endpoint", Map.of()))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Endpoint not found");
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
        var queries = service.getAllQueryConfigurations();
        assertThat(queries).isNotNull();
        assertThat(queries).isNotEmpty();
        assertThat(queries).containsKey("stock-trades-all");
        assertThat(queries).containsKey("stock-trades-count");
        assertThat(queries).containsKey("stock-trades-by-id");
    }

    @Test
    void testGetSpecificQueryConfiguration() {
        // Test getting a specific query configuration
        var queryConfig = service.getQueryConfiguration("stock-trades-all");
        assertThat(queryConfig).isPresent();
        assertThat(queryConfig.get().getName()).isEqualTo("stock-trades-all");
        assertThat(queryConfig.get().getDescription()).contains("Get All Stock Trades");
        assertThat(queryConfig.get().getSql()).contains("SELECT");
    }

    @Test
    void testGetNonexistentQueryConfiguration() {
        // Test getting a non-existent query configuration
        var queryConfig = service.getQueryConfiguration("nonexistent-query");
        assertThat(queryConfig).isEmpty();
    }

}

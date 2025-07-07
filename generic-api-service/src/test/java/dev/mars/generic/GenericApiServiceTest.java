package dev.mars.generic;

import dev.mars.test.TestDatabaseManager;
import dev.mars.common.exception.ApiException;
import dev.mars.generic.config.ApiEndpointConfig;
import dev.mars.generic.config.ConfigurationLoader;
import dev.mars.generic.config.EndpointConfigurationManager;
import dev.mars.generic.config.QueryConfig;
import dev.mars.generic.database.DatabaseConnectionManager;
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
    private TestDatabaseManager databaseManager;

    @BeforeEach
    void setUp() throws SQLException {
        // Use test configuration
        System.setProperty("generic.config.file", "application-test.yml");

        // Create components manually to avoid Guice module complexity in tests
        var genericApiConfig = new dev.mars.config.GenericApiConfig();
        databaseManager = new TestDatabaseManager(genericApiConfig);
        databaseManager.initializeSchema();
        databaseManager.cleanDatabase();

        // Create configuration components
        ConfigurationLoader configurationLoader = new TestConfigurationLoader(genericApiConfig);

        // Create database loader components for testing (using real DatabaseManager for repositories)
        dev.mars.database.DatabaseManager realDatabaseManager = new dev.mars.database.DatabaseManager(genericApiConfig);
        realDatabaseManager.initializeSchema();
        dev.mars.database.repository.DatabaseConfigurationRepository databaseRepository = new dev.mars.database.repository.DatabaseConfigurationRepository(realDatabaseManager);
        dev.mars.database.repository.QueryConfigurationRepository queryRepository = new dev.mars.database.repository.QueryConfigurationRepository(realDatabaseManager);
        dev.mars.database.repository.EndpointConfigurationRepository endpointRepository = new dev.mars.database.repository.EndpointConfigurationRepository(realDatabaseManager);
        dev.mars.database.loader.DatabaseConfigurationLoader databaseLoader = new dev.mars.database.loader.DatabaseConfigurationLoader(databaseRepository, queryRepository, endpointRepository);

        dev.mars.generic.config.ConfigurationLoaderFactory factory = new dev.mars.generic.config.ConfigurationLoaderFactory(genericApiConfig, configurationLoader, databaseLoader);
        configurationManager = new EndpointConfigurationManager(factory);

        // Create database connection manager and repository
        DatabaseConnectionManager databaseConnectionManager = new DatabaseConnectionManager(configurationManager);
        genericRepository = new GenericRepository(databaseConnectionManager);
        service = new GenericApiService(genericRepository, configurationManager, databaseConnectionManager);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("generic.config.file");
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
        // Should have at least the test-endpoint from test configuration
        assertThat(endpoints).containsKey("test-endpoint");
    }

    @Test
    void testGetEndpointConfiguration() {
        // Test getting a specific endpoint configuration
        var endpointConfig = service.getEndpointConfiguration("test-endpoint");
        assertThat(endpointConfig).isPresent();
        assertThat(endpointConfig.get().getPath()).isEqualTo("/api/test/endpoint");
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
        assertThat(queries).containsKey("test-query");
        assertThat(queries).containsKey("test-count-query");
        assertThat(queries).containsKey("test-paginated-query");
    }

    @Test
    void testGetSpecificQueryConfiguration() {
        // Test getting a specific query configuration
        var queryConfig = service.getQueryConfiguration("test-query");
        assertThat(queryConfig).isPresent();
        assertThat(queryConfig.get().getName()).isEqualTo("test-query");
        assertThat(queryConfig.get().getDescription()).contains("Test query for unit testing");
        assertThat(queryConfig.get().getSql()).contains("SELECT");
    }

    @Test
    void testGetNonexistentQueryConfiguration() {
        // Test getting a non-existent query configuration
        var queryConfig = service.getQueryConfiguration("nonexistent-query");
        assertThat(queryConfig).isEmpty();
    }

}

package dev.mars.generic;

import dev.mars.test.TestDatabaseManager;
import dev.mars.generic.config.ConfigurationLoader;
import dev.mars.generic.config.EndpointConfigurationManager;
import dev.mars.generic.database.DatabaseConnectionManager;
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

        // Create database connection manager, repository and service
        DatabaseConnectionManager databaseConnectionManager = new DatabaseConnectionManager(configurationManager);
        genericRepository = new GenericRepository(databaseConnectionManager);
        genericApiService = new GenericApiService(genericRepository, configurationManager, databaseConnectionManager);

        // Create controller
        dev.mars.generic.management.UsageStatisticsService statisticsService = new dev.mars.generic.management.UsageStatisticsService();
        controller = new GenericApiController(genericApiService, statisticsService);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("generic.config.file");
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
        // Should have at least the test-endpoint from test configuration
        assertThat(endpoints).containsKey("test-endpoint");
    }

    @Test
    void testGetEndpointConfiguration() {
        // Test getting a specific endpoint configuration
        var endpointConfig = genericApiService.getEndpointConfiguration("test-endpoint");
        assertThat(endpointConfig).isPresent();
        assertThat(endpointConfig.get().getPath()).isEqualTo("/api/test/endpoint");
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
        assertThat(queries).containsKey("test-query");
        assertThat(queries).containsKey("test-count-query");
    }

    @Test
    void testGetSpecificQueryConfiguration() {
        // Test getting a specific query configuration
        var queryConfig = genericApiService.getQueryConfiguration("test-query");
        assertThat(queryConfig).isPresent();
        assertThat(queryConfig.get().getName()).isEqualTo("test-query");
        assertThat(queryConfig.get().getDescription()).contains("Test query for unit testing");
    }

    @Test
    void testGetNonexistentQueryConfiguration() {
        // Test getting a non-existent query configuration
        var queryConfig = genericApiService.getQueryConfiguration("nonexistent-query");
        assertThat(queryConfig).isEmpty();
    }
}

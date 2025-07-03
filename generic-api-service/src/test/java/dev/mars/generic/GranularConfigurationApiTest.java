package dev.mars.generic;

import dev.mars.test.TestDatabaseManager;
import dev.mars.generic.config.ConfigurationLoader;
import dev.mars.generic.config.EndpointConfigurationManager;
import dev.mars.generic.database.DatabaseConnectionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for granular configuration APIs
 */
public class GranularConfigurationApiTest {

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
        configurationManager = new EndpointConfigurationManager(configurationLoader);

        // Create database connection manager, repository and service
        DatabaseConnectionManager databaseConnectionManager = new DatabaseConnectionManager(configurationManager);
        genericRepository = new GenericRepository(databaseConnectionManager);
        genericApiService = new GenericApiService(genericRepository, configurationManager);

        // Create controller
        dev.mars.generic.management.UsageStatisticsService statisticsService = new dev.mars.generic.management.UsageStatisticsService();
        controller = new GenericApiController(genericApiService, statisticsService);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (databaseManager != null) {
            databaseManager.cleanDatabase();
        }
    }

    @Test
    void testEndpointConfigurationSchema() {
        var schema = genericApiService.getEndpointConfigurationSchema();

        assertThat(schema).isNotNull();
        assertThat(schema.get("configType")).isEqualTo("endpoints");
        assertThat(schema.get("fields")).isNotNull();

        @SuppressWarnings("unchecked")
        var fields = (java.util.List<java.util.Map<String, Object>>) schema.get("fields");
        assertThat(fields).isNotEmpty();

        // Check that essential fields are present
        var fieldNames = fields.stream()
            .map(field -> (String) field.get("name"))
            .toList();
        assertThat(fieldNames).contains("path", "method", "query");
    }

    @Test
    void testEndpointParameters() {
        var parameters = genericApiService.getEndpointParameters();

        assertThat(parameters).isNotNull();
        assertThat(parameters.get("configType")).isEqualTo("endpoints");
        assertThat(parameters.get("parameters")).isNotNull();
        assertThat(parameters.get("totalEndpoints")).isNotNull();
        assertThat(parameters.get("endpointsWithParameters")).isNotNull();
    }

    @Test
    void testEndpointDatabaseConnections() {
        var connections = genericApiService.getEndpointDatabaseConnections();

        assertThat(connections).isNotNull();
        assertThat(connections.get("configType")).isEqualTo("endpoints");
        assertThat(connections.get("endpointDatabases")).isNotNull();
        assertThat(connections.get("referencedDatabases")).isNotNull();
        assertThat(connections.get("totalEndpoints")).isNotNull();
    }

    @Test
    void testEndpointConfigurationSummary() {
        var summary = genericApiService.getEndpointConfigurationSummary();

        assertThat(summary).isNotNull();
        assertThat(summary.get("configType")).isEqualTo("endpoints");
        assertThat(summary.get("totalCount")).isNotNull();
        assertThat(summary.get("byMethod")).isNotNull();
        assertThat(summary.get("referencedQueries")).isNotNull();
        assertThat(summary.get("referencedDatabases")).isNotNull();
    }

    @Test
    void testQueryConfigurationSchema() {
        var schema = genericApiService.getQueryConfigurationSchema();

        assertThat(schema).isNotNull();
        assertThat(schema.get("configType")).isEqualTo("queries");
        assertThat(schema.get("fields")).isNotNull();

        @SuppressWarnings("unchecked")
        var fields = (java.util.List<java.util.Map<String, Object>>) schema.get("fields");
        assertThat(fields).isNotEmpty();

        // Check that essential fields are present
        var fieldNames = fields.stream()
            .map(field -> (String) field.get("name"))
            .toList();
        assertThat(fieldNames).contains("name", "sql", "database");
    }

    @Test
    void testQueryParameters() {
        var parameters = genericApiService.getQueryParameters();

        assertThat(parameters).isNotNull();
        assertThat(parameters.get("configType")).isEqualTo("queries");
        assertThat(parameters.get("parameters")).isNotNull();
        assertThat(parameters.get("totalQueries")).isNotNull();
        assertThat(parameters.get("queriesWithParameters")).isNotNull();
    }

    @Test
    void testQueryDatabaseConnections() {
        var connections = genericApiService.getQueryDatabaseConnections();

        assertThat(connections).isNotNull();
        assertThat(connections.get("configType")).isEqualTo("queries");
        assertThat(connections.get("queryDatabases")).isNotNull();
        assertThat(connections.get("referencedDatabases")).isNotNull();
        assertThat(connections.get("totalQueries")).isNotNull();
    }

    @Test
    void testQueryConfigurationSummary() {
        var summary = genericApiService.getQueryConfigurationSummary();

        assertThat(summary).isNotNull();
        assertThat(summary.get("configType")).isEqualTo("queries");
        assertThat(summary.get("totalCount")).isNotNull();
        assertThat(summary.get("referencedDatabases")).isNotNull();
        assertThat(summary.get("parameterCounts")).isNotNull();
    }

    @Test
    void testDatabaseConfigurationSchema() {
        var schema = genericApiService.getDatabaseConfigurationSchema();

        assertThat(schema).isNotNull();
        assertThat(schema.get("configType")).isEqualTo("databases");
        assertThat(schema.get("fields")).isNotNull();

        @SuppressWarnings("unchecked")
        var fields = (java.util.List<java.util.Map<String, Object>>) schema.get("fields");
        assertThat(fields).isNotEmpty();

        // Check that essential fields are present
        var fieldNames = fields.stream()
            .map(field -> (String) field.get("name"))
            .toList();
        assertThat(fieldNames).contains("name", "url", "driver");
    }

    @Test
    void testDatabaseParameters() {
        var parameters = genericApiService.getDatabaseParameters();

        assertThat(parameters).isNotNull();
        assertThat(parameters.get("configType")).isEqualTo("databases");
        assertThat(parameters.get("poolConfigurations")).isNotNull();
        assertThat(parameters.get("totalDatabases")).isNotNull();
        assertThat(parameters.get("databasesWithPoolConfig")).isNotNull();
    }

    @Test
    void testDatabaseConnections() {
        var connections = genericApiService.getDatabaseConnections();

        assertThat(connections).isNotNull();
        assertThat(connections.get("configType")).isEqualTo("databases");
        assertThat(connections.get("connections")).isNotNull();
        assertThat(connections.get("totalDatabases")).isNotNull();
    }

    @Test
    void testDatabaseConfigurationSummary() {
        var summary = genericApiService.getDatabaseConfigurationSummary();

        assertThat(summary).isNotNull();
        assertThat(summary.get("configType")).isEqualTo("databases");
        assertThat(summary.get("totalCount")).isNotNull();
        assertThat(summary.get("uniqueDrivers")).isNotNull();
        assertThat(summary.get("driverTypes")).isNotNull();
    }
}

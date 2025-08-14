package dev.cordal.generic;

import dev.cordal.test.TestDatabaseManager;
import dev.cordal.generic.config.ConfigurationLoader;
import dev.cordal.generic.config.EndpointConfigurationManager;
import dev.cordal.generic.database.DatabaseConnectionManager;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test class for granular configuration APIs
 * Tests actual HTTP endpoints to catch routing issues like 404 errors
 */
public class GranularConfigurationApiTest {

    private GenericApiApplication application;
    private TestDatabaseManager databaseManager;

    @BeforeEach
    void setUp() throws SQLException {
        // Use test configuration
        System.setProperty("generic.config.file", "application-test.yml");

        // Create the full application to test actual HTTP routing
        application = new GenericApiApplication();
        application.initializeForTesting(); // Initialize without starting server

        // Initialize test database
        var genericApiConfig = new dev.cordal.config.GenericApiConfig();
        databaseManager = new TestDatabaseManager(genericApiConfig);
        databaseManager.initializeSchema();
        databaseManager.cleanDatabase();
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (databaseManager != null) {
            databaseManager.cleanDatabase();
        }
    }

    @Test
    void testEndpointConfigurationSchema() {
        JavalinTest.test(application.getApp(), (server, client) -> {
            var response = client.get("/api/generic/config/endpoints/schema");

            // Verify HTTP status is 200, not 404
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body()).isNotNull();

            // Read response body once and verify it contains expected JSON structure
            String responseBody = response.body().string();
            assertThat(responseBody).contains("\"configType\":\"endpoints\"");
            assertThat(responseBody).contains("\"fields\":");
        });
    }

    @Test
    void testEndpointParameters() {
        JavalinTest.test(application.getApp(), (server, client) -> {
            var response = client.get("/api/generic/config/endpoints/parameters");

            // Verify HTTP status is 200, not 404
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body()).isNotNull();

            // Read response body once and verify it contains expected JSON structure
            String responseBody = response.body().string();
            assertThat(responseBody).contains("\"configType\":\"endpoints\"");
            assertThat(responseBody).contains("\"parameters\":");
        });
    }

    @Test
    void testEndpointDatabaseConnections() {
        JavalinTest.test(application.getApp(), (server, client) -> {
            var response = client.get("/api/generic/config/endpoints/database-connections");

            // Verify HTTP status is 200, not 404
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body()).isNotNull();

            // Read response body once and verify it contains expected JSON structure
            String responseBody = response.body().string();
            assertThat(responseBody).contains("\"configType\":\"endpoints\"");
            assertThat(responseBody).contains("\"endpointDatabases\":");
        });
    }

    @Test
    void testEndpointConfigurationSummary() {
        JavalinTest.test(application.getApp(), (server, client) -> {
            var response = client.get("/api/generic/config/endpoints/summary");

            // Verify HTTP status is 200, not 404
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body()).isNotNull();

            // Read response body once and verify it contains expected JSON structure
            String responseBody = response.body().string();
            assertThat(responseBody).contains("\"configType\":\"endpoints\"");
            assertThat(responseBody).contains("\"totalCount\":");
        });
    }

    @Test
    void testQueryConfigurationSchema() {
        JavalinTest.test(application.getApp(), (server, client) -> {
            var response = client.get("/api/generic/config/queries/schema");

            // Verify HTTP status is 200, not 404 - THIS IS THE CRITICAL TEST!
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body()).isNotNull();

            // Read response body once and verify it contains expected JSON structure
            String responseBody = response.body().string();
            assertThat(responseBody).contains("\"configType\":\"queries\"");
            assertThat(responseBody).contains("\"fields\":");
        });
    }

    @Test
    void testQueryParameters() {
        JavalinTest.test(application.getApp(), (server, client) -> {
            var response = client.get("/api/generic/config/queries/parameters");

            // Verify HTTP status is 200, not 404 - THIS IS THE CRITICAL TEST!
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body()).isNotNull();

            // Read response body once and verify it contains expected JSON structure
            String responseBody = response.body().string();
            assertThat(responseBody).contains("\"configType\":\"queries\"");
            assertThat(responseBody).contains("\"parameters\":");
        });
    }

    @Test
    void testQueryDatabaseConnections() {
        JavalinTest.test(application.getApp(), (server, client) -> {
            var response = client.get("/api/generic/config/queries/database-connections");

            // Verify HTTP status is 200, not 404 - THIS IS THE CRITICAL TEST!
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body()).isNotNull();

            // Read response body once and verify it contains expected JSON structure
            String responseBody = response.body().string();
            assertThat(responseBody).contains("\"configType\":\"queries\"");
            assertThat(responseBody).contains("\"queryDatabases\":");
        });
    }

    @Test
    void testQueryConfigurationSummary() {
        JavalinTest.test(application.getApp(), (server, client) -> {
            var response = client.get("/api/generic/config/queries/summary");

            // Verify HTTP status is 200, not 404 - THIS IS THE CRITICAL TEST!
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body()).isNotNull();

            // Read response body once and verify it contains expected JSON structure
            String responseBody = response.body().string();
            assertThat(responseBody).contains("\"configType\":\"queries\"");
            assertThat(responseBody).contains("\"totalCount\":");
        });
    }

    @Test
    void testDatabaseConfigurationSchema() {
        JavalinTest.test(application.getApp(), (server, client) -> {
            var response = client.get("/api/generic/config/databases/schema");

            // Verify HTTP status is 200, not 404 - THIS IS THE CRITICAL TEST!
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body()).isNotNull();

            // Read response body once and verify it contains expected JSON structure
            String responseBody = response.body().string();
            assertThat(responseBody).contains("\"configType\":\"databases\"");
            assertThat(responseBody).contains("\"fields\":");
        });
    }

    @Test
    void testDatabaseParameters() {
        JavalinTest.test(application.getApp(), (server, client) -> {
            var response = client.get("/api/generic/config/databases/parameters");

            // Verify HTTP status is 200, not 404 - THIS IS THE CRITICAL TEST!
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body()).isNotNull();

            // Read response body once and verify it contains expected JSON structure
            String responseBody = response.body().string();
            assertThat(responseBody).contains("\"configType\":\"databases\"");
            assertThat(responseBody).contains("\"poolConfigurations\":");
        });
    }

    @Test
    void testDatabaseConnections() {
        JavalinTest.test(application.getApp(), (server, client) -> {
            var response = client.get("/api/generic/config/databases/connections");

            // Verify HTTP status is 200, not 404 - THIS IS THE CRITICAL TEST!
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body()).isNotNull();

            // Read response body once and verify it contains expected JSON structure
            String responseBody = response.body().string();
            assertThat(responseBody).contains("\"configType\":\"databases\"");
            assertThat(responseBody).contains("\"connections\":");
        });
    }

    @Test
    void testDatabaseConfigurationSummary() {
        JavalinTest.test(application.getApp(), (server, client) -> {
            var response = client.get("/api/generic/config/databases/summary");

            // Verify HTTP status is 200, not 404 - THIS IS THE CRITICAL TEST!
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body()).isNotNull();

            // Read response body once and verify it contains expected JSON structure
            String responseBody = response.body().string();
            assertThat(responseBody).contains("\"configType\":\"databases\"");
            assertThat(responseBody).contains("\"totalCount\":");
        });
    }

    /**
     * Test to verify that the tests can actually catch 404 errors
     * This test should fail if there's a routing issue
     */
    @Test
    void testNonExistentEndpointReturns404() {
        JavalinTest.test(application.getApp(), (server, client) -> {
            var response = client.get("/api/generic/config/nonexistent/endpoint");

            // This should return 404 - proving our tests can catch routing issues
            assertThat(response.code()).isEqualTo(404);
        });
    }
}

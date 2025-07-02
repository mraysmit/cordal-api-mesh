package dev.mars.generic.management;

import dev.mars.generic.GenericApiApplication;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Test the comprehensive management APIs
 */
class ManagementApiTest {

    @Test
    void shouldHaveManagementDashboard() {
        GenericApiApplication testApp = new GenericApiApplication();
        testApp.initializeForTesting();

        Javalin app = testApp.getApp();

        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/api/management/dashboard");
            assertThat(response.code()).isEqualTo(200);
            
            String responseBody = response.body().string();
            assertThat(responseBody).contains("configuration");
            assertThat(responseBody).contains("usage");
            assertThat(responseBody).contains("health");
            assertThat(responseBody).contains("system");
        });
    }

    @Test
    void shouldHaveConfigurationMetadataEndpoints() {
        GenericApiApplication testApp = new GenericApiApplication();
        testApp.initializeForTesting();

        Javalin app = testApp.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Test configuration metadata
            var response = client.get("/api/management/config/metadata");
            assertThat(response.code()).isEqualTo(200);

            // Test configuration paths
            response = client.get("/api/management/config/paths");
            assertThat(response.code()).isEqualTo(200);
            
            String responseBody = response.body().string();
            assertThat(responseBody).contains("databases");
            assertThat(responseBody).contains("queries");
            assertThat(responseBody).contains("endpoints");

            // Test configuration file contents
            response = client.get("/api/management/config/contents");
            assertThat(response.code()).isEqualTo(200);
        });
    }

    @Test
    void shouldHaveConfigurationViewEndpoints() {
        GenericApiApplication testApp = new GenericApiApplication();
        testApp.initializeForTesting();

        Javalin app = testApp.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Test configured endpoints
            var response = client.get("/api/management/config/endpoints");
            assertThat(response.code()).isEqualTo(200);
            
            String responseBody = response.body().string();
            assertThat(responseBody).contains("count");
            assertThat(responseBody).contains("endpoints");

            // Test configured queries
            response = client.get("/api/management/config/queries");
            assertThat(response.code()).isEqualTo(200);
            
            responseBody = response.body().string();
            assertThat(responseBody).contains("count");
            assertThat(responseBody).contains("queries");

            // Test configured databases
            response = client.get("/api/management/config/databases");
            assertThat(response.code()).isEqualTo(200);
            
            responseBody = response.body().string();
            assertThat(responseBody).contains("count");
            assertThat(responseBody).contains("databases");
        });
    }

    @Test
    void shouldHaveUsageStatisticsEndpoints() {
        GenericApiApplication testApp = new GenericApiApplication();
        testApp.initializeForTesting();

        Javalin app = testApp.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Test comprehensive usage statistics
            var response = client.get("/api/management/statistics");
            assertThat(response.code()).isEqualTo(200);
            
            String responseBody = response.body().string();
            assertThat(responseBody).contains("serviceStartTime");
            assertThat(responseBody).contains("uptime");
            assertThat(responseBody).contains("summary");

            // Test endpoint statistics
            response = client.get("/api/management/statistics/endpoints");
            assertThat(response.code()).isEqualTo(200);

            // Test query statistics
            response = client.get("/api/management/statistics/queries");
            assertThat(response.code()).isEqualTo(200);

            // Test database statistics
            response = client.get("/api/management/statistics/databases");
            assertThat(response.code()).isEqualTo(200);
        });
    }

    @Test
    void shouldHaveHealthMonitoringEndpoints() {
        GenericApiApplication testApp = new GenericApiApplication();
        testApp.initializeForTesting();

        Javalin app = testApp.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Test comprehensive health status
            var response = client.get("/api/management/health");
            assertThat(response.code()).isIn(200, 503); // Could be UP or DOWN
            
            String responseBody = response.body().string();
            assertThat(responseBody).contains("service");
            assertThat(responseBody).contains("databases");
            assertThat(responseBody).contains("configuration");
            assertThat(responseBody).contains("overall");

            // Test database health
            response = client.get("/api/management/health/databases");
            assertThat(response.code()).isEqualTo(200);

            // Test specific database health
            response = client.get("/api/management/health/databases/stock-trades-db");
            assertThat(response.code()).isIn(200, 503); // Could be UP or DOWN
        });
    }
}

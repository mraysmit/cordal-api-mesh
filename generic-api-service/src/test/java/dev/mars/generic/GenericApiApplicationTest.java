package dev.mars.generic;

import dev.mars.config.GenericApiConfig;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Generic API Application
 * Fixed to avoid Javalin instance reuse issues
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GenericApiApplicationTest {

    private GenericApiApplication application;
    private static final int TEST_PORT = 18080; // Use different port for testing

    @BeforeAll
    void setUpAll() {
        // Set test configuration
        System.setProperty("config.file", "application-test.yml");
    }

    @AfterAll
    void tearDownAll() {
        System.clearProperty("config.file");
    }

    @BeforeEach
    void setUp() {
        try {
            application = new GenericApiApplication();
            // Don't start the server here - let individual tests handle it
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Generic API Application", e);
        }
    }

    @AfterEach
    void tearDown() {
        if (application != null) {
            try {
                application.stop();
            } catch (Exception e) {
                // Ignore cleanup errors in tests
            }
        }
    }

    @Test
    void shouldStartApplicationSuccessfully() {
        // Start application for this test
        application.start();

        assertThat(application).isNotNull();
        assertThat(application.getApp()).isNotNull();
        assertThat(application.getInjector()).isNotNull();
    }

    @Test
    void shouldHaveHealthEndpoint() {
        // Create a fresh Javalin instance for this test to avoid reuse issues
        GenericApiApplication testApp = new GenericApiApplication();
        testApp.initializeForTesting(); // Initialize without starting

        Javalin app = testApp.getApp();

        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/api/health");
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            assertThat(responseBody).contains("status");
            assertThat(responseBody).contains("UP");
        });
    }

    @Test
    void shouldHaveGenericHealthEndpoint() {
        GenericApiApplication testApp = new GenericApiApplication();
        testApp.initializeForTesting();

        Javalin app = testApp.getApp();

        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/api/generic/health");
            assertThat(response.code()).isEqualTo(200);
        });
    }

    @Test
    void shouldHaveStockTradesEndpoint() {
        GenericApiApplication testApp = new GenericApiApplication();
        testApp.initializeForTesting();

        Javalin app = testApp.getApp();

        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/api/generic/stock-trades");
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            assertThat(responseBody).contains("data");
        });
    }

    @Test
    void shouldHaveConfigurationEndpoints() {
        GenericApiApplication testApp = new GenericApiApplication();
        testApp.initializeForTesting();

        Javalin app = testApp.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Test configuration validation endpoint
            var response = client.get("/api/generic/config/validate");
            assertThat(response.code()).isEqualTo(200);

            // Test endpoints listing
            response = client.get("/api/generic/endpoints");
            assertThat(response.code()).isEqualTo(200);

            // Test database configurations
            response = client.get("/api/generic/config/databases");
            assertThat(response.code()).isEqualTo(200);
        });
    }

    @Test
    void shouldHaveSwaggerEndpoints() {
        GenericApiApplication testApp = new GenericApiApplication();
        testApp.initializeForTesting();

        Javalin app = testApp.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Test OpenAPI JSON endpoint
            var response = client.get("/openapi.json");
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            assertThat(responseBody).contains("openapi");

            // Test Swagger UI endpoint
            response = client.get("/swagger");
            assertThat(response.code()).isEqualTo(200);
        });
    }

    @Test
    void shouldHandleStockTradesBySymbol() {
        GenericApiApplication testApp = new GenericApiApplication();
        testApp.initializeForTesting();

        Javalin app = testApp.getApp();

        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/api/generic/stock-trades/symbol/AAPL");
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            assertThat(responseBody).contains("data");
        });
    }

    @Test
    void shouldHandleStockTradesByDateRange() {
        GenericApiApplication testApp = new GenericApiApplication();
        testApp.initializeForTesting();

        Javalin app = testApp.getApp();

        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/api/generic/stock-trades/date-range?start_date=2024-01-01&end_date=2024-12-31");
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            assertThat(responseBody).contains("data");
        });
    }

    @Test
    void shouldValidateConfigurationRelationships() {
        GenericApiApplication testApp = new GenericApiApplication();
        testApp.initializeForTesting();

        Javalin app = testApp.getApp();

        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/api/generic/config/validate/relationships");
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            assertThat(responseBody).contains("VALID");
        });
    }
}

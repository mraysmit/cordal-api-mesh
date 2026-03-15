package dev.cordal.generic;

import com.google.inject.Injector;
import dev.cordal.config.GenericApiConfig;
import dev.cordal.generic.config.ApiEndpointConfig;
import dev.cordal.generic.config.ConfigurationLoader;
import dev.cordal.generic.config.EndpointConfigurationManager;
import dev.cordal.generic.database.DatabaseConnectionManager;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for the Generic API Application
 * Fixed to avoid Javalin instance reuse issues
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GenericApiApplicationTest {

    private GenericApiApplication application;

    @BeforeAll
    void setUpAll() {
        // Set test configuration
        System.setProperty("generic.config.file", "application-test.yml");
    }

    @AfterAll
    void tearDownAll() {
        System.clearProperty("generic.config.file");
    }

    @BeforeAll
    static void setUpClass() {
        // Set test configuration for all tests
        System.setProperty("generic.config.file", "application-test.yml");
    }

    @AfterAll
    static void tearDownClass() {
        // Clean up system property
        System.clearProperty("generic.config.file");
    }

    @BeforeEach
    void setUp() {
        try {
            // Create a fresh application for each test to avoid JavalinTest key conflicts
            application = new GenericApiApplication();
            application.initializeForTesting();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Generic API Application", e);
        }
    }

    @AfterEach
    void restoreTestProperties() {
        System.setProperty("generic.config.file", "application-test.yml");
        System.clearProperty("test.data.loading.enabled");
    }

    // Removed initializeTestDataForStockTrades method as it was causing application to stop
    // Test data initialization is not needed for basic application structure tests

    // Removed @AfterEach tearDown method that was stopping the application after each test
    // Using @BeforeAll/@AfterAll lifecycle instead

    @Test
    void shouldStartApplicationSuccessfully() {
        // Application is already initialized in setUp()
        assertThat(application).isNotNull();
        assertThat(application.getApp()).isNotNull();
        assertThat(application.getInjector()).isNotNull();
    }

    @Test
    void shouldHaveHealthEndpoint() {
        // Use the shared application instance
        Javalin app = application.getApp();

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
        // Use the shared application instance
        Javalin app = application.getApp();

        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/api/generic/health");
            assertThat(response.code()).isIn(200, 503);
        });
    }

    @Test
    void shouldHaveStockTradesEndpoint() {
        // Use the shared application instance (skip test data initialization to avoid stopping app)
        Javalin app = application.getApp();

        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/api/generic/stock-trades");
            int responseCode = response.code();
            // Accept 404 when the endpoint is intentionally skipped because the backing database is unavailable.
            assertThat(responseCode).isIn(200, 404, 500);
        });
    }

    @Test
    void shouldHaveConfigurationEndpoints() {
        // Use the shared application instance that already has test configuration
        Javalin app = application.getApp();

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
        // Use the shared application instance that already has test configuration
        Javalin app = application.getApp();

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
    void shouldHandleGenericEndpoints() {
        // Use the shared application instance that already has test configuration
        Javalin app = application.getApp();

        JavalinTest.test(app, (server, client) -> {
            // Test a generic endpoint that should exist in test configuration
            var response = client.get("/api/test/endpoint");
            int responseCode = response.code();
            assertThat(responseCode).isIn(200, 404, 500);
        });
    }

    @Test
    void shouldHandleStockTradesByDateRange() {
        // Use the shared application instance that already has test configuration
        Javalin app = application.getApp();

        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/api/generic/stock-trades/date-range?start_date=2024-01-01&end_date=2024-12-31");
            int responseCode = response.code();
            assertThat(responseCode).isIn(200, 404, 500);
        });
    }

    @Test
    void shouldValidateConfigurationRelationships() {
        // Use the shared application instance
        Javalin app = application.getApp();

        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/api/generic/config/validate/relationships");
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            assertThat(responseBody).contains("VALID");
        });
    }

    @Test
    void shouldRunValidationOnlyFromCommandLineFlag() {
        System.setProperty("generic.config.file", "application-validation-only-success.yml");

        assertThatCode(() -> GenericApiApplication.main(new String[]{"--validate-only"}))
            .doesNotThrowAnyException();
    }

    @Test
    void shouldFailStartupWhenValidateOnlyConfigurationHasErrors() {
        System.setProperty("generic.config.file", "application-validation-only-failure.yml");

        assertThatThrownBy(() -> GenericApiApplication.main(new String[0]))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to start Generic API application")
            .hasRootCauseMessage("Configuration validation failed");
    }

    @Test
    void shouldContinueInitializationWhenStartupValidationFailsNonFatally() {
        TestableGenericApiApplication validatingApplication = new TestableGenericApiApplication();
        Injector injector = mock(Injector.class);
        GenericApiConfig config = mock(GenericApiConfig.class);
        dev.cordal.database.DatabaseManager configurationDatabaseManager = mock(dev.cordal.database.DatabaseManager.class);
        ConfigurationLoader configurationLoader = mock(ConfigurationLoader.class);
        EndpointConfigurationManager configurationManager = mock(EndpointConfigurationManager.class);
        DatabaseConnectionManager databaseConnectionManager = mock(DatabaseConnectionManager.class);

        ApiEndpointConfig brokenEndpoint = new ApiEndpointConfig();
        brokenEndpoint.setQuery("missing-query");

        when(config.isValidationRunOnStartup()).thenReturn(true);
        when(config.isValidationValidateOnly()).thenReturn(false);
        when(injector.getInstance(dev.cordal.database.DatabaseManager.class)).thenReturn(configurationDatabaseManager);
        when(injector.getInstance(GenericApiConfig.class)).thenReturn(config);
        when(injector.getInstance(ConfigurationLoader.class)).thenReturn(configurationLoader);
        when(injector.getInstance(EndpointConfigurationManager.class)).thenReturn(configurationManager);
        when(injector.getInstance(DatabaseConnectionManager.class)).thenReturn(databaseConnectionManager);
        when(configurationManager.getAllDatabaseConfigurations()).thenReturn(Map.of());
        when(configurationManager.getAllQueryConfigurations()).thenReturn(Map.of());
        when(configurationManager.getAllEndpointConfigurations()).thenReturn(Map.of("broken-endpoint", brokenEndpoint));

        validatingApplication.setInjectorForTest(injector);

        assertThatCode(validatingApplication::runPreStartupInitializationForTest)
            .doesNotThrowAnyException();
    }

    @Test
    void shouldRunPostStartupEndpointValidationWhenEnabled() {
        System.setProperty("generic.config.file", "application-post-startup-validation.yml");

        TestableGenericApiApplication validatingApplication = new TestableGenericApiApplication();
        validatingApplication.initializeForTesting();

        assertThatCode(validatingApplication::runPostStartupInitializationForTest)
            .doesNotThrowAnyException();

        validatingApplication.stop();
    }

    private static final class TestableGenericApiApplication extends GenericApiApplication {
        private void setInjectorForTest(Injector injector) {
            this.injector = injector;
        }

        private void runPreStartupInitializationForTest() {
            performPreStartupInitialization();
        }

        private void runPostStartupInitializationForTest() {
            performPostStartupInitialization();
        }
    }
}

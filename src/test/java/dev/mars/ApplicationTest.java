package dev.mars;

import com.google.inject.Injector;
import dev.mars.config.AppConfig;
import dev.mars.database.DatabaseManager;
import dev.mars.generic.GenericApiService;
import io.javalin.Javalin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the Application class
 */
public class ApplicationTest {

    private Application application;

    @BeforeEach
    void setUp() {
        // Use test configuration
        System.setProperty("config.file", "application-test.yml");
        application = new Application();
    }

    @AfterEach
    void tearDown() {
        if (application != null) {
            try {
                application.stop();
                // Wait a moment for proper shutdown
                Thread.sleep(500);
            } catch (Exception e) {
                // Ignore cleanup errors in tests
            }
        }
        System.clearProperty("config.file");
    }

    @Test
    void testApplicationStartsSuccessfully() {
        assertThatCode(() -> {
            application.start();
            assertThat(application.getApp()).isNotNull();
            assertThat(application.getInjector()).isNotNull();
        }).doesNotThrowAnyException();
    }

    @Test
    void testApplicationComponents() {
        application.start();

        Injector injector = application.getInjector();
        assertThat(injector).isNotNull();

        // Test that all major components can be instantiated
        assertThat(injector.getInstance(AppConfig.class)).isNotNull();
        assertThat(injector.getInstance(DatabaseManager.class)).isNotNull();
        assertThat(injector.getInstance(GenericApiService.class)).isNotNull();
    }

    @Test
    void testApplicationStop() {
        application.start();
        assertThat(application.getApp()).isNotNull();

        assertThatCode(() -> application.stop()).doesNotThrowAnyException();
    }

    @Test
    void testJavalinAppConfiguration() {
        application.start();

        Javalin app = application.getApp();
        assertThat(app).isNotNull();

        // Test that the app is properly configured
        // Note: More detailed testing would require accessing internal Javalin state
    }

    @Test
    void testDependencyInjection() {
        application.start();

        Injector injector = application.getInjector();

        // Test that singleton instances are properly managed
        GenericApiService service1 = injector.getInstance(GenericApiService.class);
        GenericApiService service2 = injector.getInstance(GenericApiService.class);

        assertThat(service1).isSameAs(service2); // Should be the same singleton instance
    }

    @Test
    void testApplicationWithoutConfiguration() {
        System.clearProperty("config.file");

        // Create a new application instance for this test to avoid conflicts
        Application testApp = new Application();

        // Should still start with default configuration
        assertThatCode(() -> {
            testApp.start();
            assertThat(testApp.getApp()).isNotNull();
        }).doesNotThrowAnyException();

        // Clean up the test application
        try {
            testApp.stop();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
}

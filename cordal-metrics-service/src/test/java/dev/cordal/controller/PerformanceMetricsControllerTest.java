package dev.cordal.controller;

import dev.cordal.metrics.MetricsApplication;
import dev.cordal.service.PerformanceMetricsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for PerformanceMetricsController using real services
 */
class PerformanceMetricsControllerTest {

    private MetricsApplication application;
    private PerformanceMetricsService service;
    private PerformanceMetricsController controller;

    @BeforeEach
    void setUp() {
        // Use test configuration - MetricsConfig looks for "metrics.config.file" property
        System.setProperty("metrics.config.file", "application-test.yml");

        // Initialize application for testing (no server startup)
        application = new MetricsApplication();
        application.initializeForTesting();

        // Get real service from dependency injection
        service = application.getInjector().getInstance(PerformanceMetricsService.class);
        controller = application.getInjector().getInstance(PerformanceMetricsController.class);
    }

    @AfterEach
    void tearDown() {
        if (application != null) {
            application.stop();
        }
        System.clearProperty("metrics.config.file");
    }

    @Test
    void shouldCreateController() {
        // Test that controller can be created with real dependencies
        assertThat(controller).isNotNull();
        assertThat(service).isNotNull();
    }

    @Test
    void shouldHaveValidService() {
        // Test that the controller has a valid service
        assertThat(service).isNotNull();

        // Test that we can call the service method (basic smoke test)
        var result = service.getAllMetrics(1, 10);
        assertThat(result).isNotNull();
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
    }
}

package dev.mars.controller;

import dev.mars.dto.PagedResponse;
import dev.mars.model.PerformanceMetrics;
import dev.mars.service.PerformanceMetricsService;
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Basic unit tests for PerformanceMetricsController
 */
@ExtendWith(MockitoExtension.class)
class PerformanceMetricsControllerTest {

    @Mock
    private PerformanceMetricsService service;

    @Mock
    private Context context;

    private PerformanceMetricsController controller;

    @BeforeEach
    void setUp() {
        controller = new PerformanceMetricsController(service);
    }

    @Test
    void shouldCreateController() {
        // Test that controller can be created
        assertThat(controller).isNotNull();
    }

    @Test
    void shouldHandleGetAllRequest() {
        // Given
        when(context.queryParam("page")).thenReturn("1");
        when(context.queryParam("size")).thenReturn("10");

        // Mock the service to return a valid PagedResponse
        PagedResponse<PerformanceMetrics> mockResponse = new PagedResponse<>(
            Collections.emptyList(), 1, 10, 0L);
        when(service.getAllMetrics(1, 10)).thenReturn(mockResponse);

        // When
        controller.getAllPerformanceMetrics(context);

        // Then - verify the method was called (basic smoke test)
        verify(context, atLeastOnce()).queryParam(anyString());
        verify(context).json(mockResponse);
        verify(service).getAllMetrics(1, 10);
    }
}

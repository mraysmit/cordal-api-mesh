package dev.cordal.controller;

import dev.cordal.common.dto.PagedResponse;
import dev.cordal.common.model.PerformanceMetrics;
import dev.cordal.service.PerformanceMetricsService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for performance metrics API endpoints
 */
@Singleton
public class PerformanceMetricsController {
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMetricsController.class);
    
    private final PerformanceMetricsService performanceMetricsService;
    
    @Inject
    public PerformanceMetricsController(PerformanceMetricsService performanceMetricsService) {
        this.performanceMetricsService = performanceMetricsService;
    }
    
    /**
     * Get all performance metrics with pagination
     * GET /api/performance-metrics?page=0&size=20
     */
    public void getAllPerformanceMetrics(Context ctx) {
        try {
            int page = parseIntParameter(ctx, "page", 0);
            int size = Math.min(parseIntParameter(ctx, "size", 20), 100); // Max 100 per page

            PagedResponse<PerformanceMetrics> response = performanceMetricsService.getAllMetrics(page, size);

            ctx.json(response);
            logger.debug("Retrieved {} performance metrics for page {}", response.getData().size(), page);

        } catch (NumberFormatException e) {
            logger.warn("Invalid pagination parameters: {}", e.getMessage());
            ctx.status(400).json(Map.of("error", "Invalid pagination parameters"));
        } catch (Exception e) {
            logger.error("Error retrieving performance metrics", e);
            ctx.status(500).json(Map.of("error", "Internal server error"));
        }
    }
    
    /**
     * Get performance metrics by ID
     * GET /api/performance-metrics/{id}
     */
    public void getPerformanceMetricsById(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            
            Optional<PerformanceMetrics> metrics = performanceMetricsService.getMetricsById(id);
            
            if (metrics.isPresent()) {
                ctx.json(metrics.get());
                logger.debug("Retrieved performance metrics with ID: {}", id);
            } else {
                ctx.status(404).json(Map.of("error", "Performance metrics not found"));
            }
            
        } catch (NumberFormatException e) {
            logger.warn("Invalid ID parameter: {}", e.getMessage());
            ctx.status(400).json(Map.of("error", "Invalid ID parameter"));
        } catch (Exception e) {
            logger.error("Error retrieving performance metrics by ID", e);
            ctx.status(500).json(Map.of("error", "Internal server error"));
        }
    }
    
    /**
     * Get performance metrics by test type
     * GET /api/performance-metrics/test-type/{testType}?page=0&size=20
     */
    public void getPerformanceMetricsByTestType(Context ctx) {
        try {
            String testType = ctx.pathParam("testType");
            int page = parseIntParameter(ctx, "page", 0);
            int size = Math.min(parseIntParameter(ctx, "size", 20), 100);

            PagedResponse<PerformanceMetrics> response = performanceMetricsService.getMetricsByTestType(testType, page, size);

            ctx.json(response);
            logger.debug("Retrieved {} performance metrics for test type '{}' on page {}",
                        response.getData().size(), testType, page);

        } catch (NumberFormatException e) {
            logger.warn("Invalid pagination parameters: {}", e.getMessage());
            ctx.status(400).json(Map.of("error", "Invalid pagination parameters"));
        } catch (Exception e) {
            logger.error("Error retrieving performance metrics by test type", e);
            ctx.status(500).json(Map.of("error", "Internal server error"));
        }
    }
    
    /**
     * Get performance metrics within date range
     * GET /api/performance-metrics/date-range?startDate=2024-01-01T00:00:00&endDate=2024-01-31T23:59:59&page=0&size=20
     */
    public void getPerformanceMetricsByDateRange(Context ctx) {
        try {
            String startDateStr = ctx.queryParam("startDate");
            String endDateStr = ctx.queryParam("endDate");
            
            if (startDateStr == null || endDateStr == null) {
                ctx.status(400).json(Map.of("error", "startDate and endDate parameters are required"));
                return;
            }
            
            LocalDateTime startDate = LocalDateTime.parse(startDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDateTime endDate = LocalDateTime.parse(endDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            int page = parseIntParameter(ctx, "page", 0);
            int size = Math.min(parseIntParameter(ctx, "size", 20), 100);

            PagedResponse<PerformanceMetrics> response = performanceMetricsService.getMetricsByDateRange(
                startDate, endDate, page, size);

            ctx.json(response);
            logger.debug("Retrieved {} performance metrics for date range {} to {} on page {}",
                        response.getData().size(), startDate, endDate, page);
            
        } catch (DateTimeParseException e) {
            logger.warn("Invalid date format: {}", e.getMessage());
            ctx.status(400).json(Map.of("error", "Invalid date format. Use ISO format: yyyy-MM-ddTHH:mm:ss"));
        } catch (NumberFormatException e) {
            logger.warn("Invalid pagination parameters: {}", e.getMessage());
            ctx.status(400).json(Map.of("error", "Invalid pagination parameters"));
        } catch (Exception e) {
            logger.error("Error retrieving performance metrics by date range", e);
            ctx.status(500).json(Map.of("error", "Internal server error"));
        }
    }
    
    /**
     * Get available test types
     * GET /api/performance-metrics/test-types
     */
    public void getAvailableTestTypes(Context ctx) {
        try {
            ctx.json(Map.of("testTypes", performanceMetricsService.getAvailableTestTypes()));
            logger.debug("Retrieved available test types");
            
        } catch (Exception e) {
            logger.error("Error retrieving test types", e);
            ctx.status(500).json(Map.of("error", "Internal server error"));
        }
    }
    
    /**
     * Get performance summary statistics
     * GET /api/performance-metrics/summary
     */
    public void getPerformanceSummary(Context ctx) {
        try {
            Map<String, Object> summary = performanceMetricsService.getPerformanceSummary();
            ctx.json(summary);
            logger.debug("Retrieved performance summary");
            
        } catch (Exception e) {
            logger.error("Error retrieving performance summary", e);
            ctx.status(500).json(Map.of("error", "Internal server error"));
        }
    }
    
    /**
     * Get performance trends for dashboard charts
     * GET /api/performance-metrics/trends?testType=CONCURRENT&days=7
     */
    public void getPerformanceTrends(Context ctx) {
        try {
            String testType = ctx.queryParam("testType");
            int days = parseIntParameter(ctx, "days", 7);

            // Limit days to reasonable range
            days = Math.min(Math.max(days, 1), 365);

            Map<String, Object> trends = performanceMetricsService.getPerformanceTrends(testType, days);
            ctx.json(trends);
            logger.debug("Retrieved performance trends for test type '{}' over {} days", testType, days);

        } catch (NumberFormatException e) {
            logger.warn("Invalid days parameter: {}", e.getMessage());
            ctx.status(400).json(Map.of("error", "Invalid days parameter"));
        } catch (Exception e) {
            logger.error("Error retrieving performance trends", e);
            ctx.status(500).json(Map.of("error", "Internal server error"));
        }
    }
    
    /**
     * Create new performance metrics (for testing purposes)
     * POST /api/performance-metrics
     */
    public void createPerformanceMetrics(Context ctx) {
        try {
            PerformanceMetrics metrics = ctx.bodyAsClass(PerformanceMetrics.class);
            PerformanceMetrics savedMetrics = performanceMetricsService.saveMetrics(metrics);
            
            ctx.status(201).json(savedMetrics);
            logger.info("Created new performance metrics: {}", savedMetrics.getTestName());
            
        } catch (Exception e) {
            logger.error("Error creating performance metrics", e);
            ctx.status(500).json(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Parse integer parameter with default value
     */
    private int parseIntParameter(Context ctx, String paramName, int defaultValue) {
        String paramValue = ctx.queryParam(paramName);
        if (paramValue == null || paramValue.trim().isEmpty()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(paramValue);
        } catch (NumberFormatException e) {
            logger.warn("Invalid {} parameter: {}", paramName, paramValue);
            return defaultValue;
        }
    }
}

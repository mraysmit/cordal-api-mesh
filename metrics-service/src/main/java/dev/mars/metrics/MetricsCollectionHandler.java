package dev.mars.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mars.config.MetricsConfig;
import dev.mars.model.PerformanceMetrics;
import dev.mars.service.PerformanceMetricsService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic metrics collection handler that can be attached to any API endpoint
 * to automatically collect performance metrics.
 */
@Singleton
public class MetricsCollectionHandler {
    private static final Logger logger = LoggerFactory.getLogger(MetricsCollectionHandler.class);
    
    private final PerformanceMetricsService metricsService;
    private final MetricsConfig metricsConfig;
    private final ObjectMapper objectMapper;
    
    // Thread-local storage for request timing data
    private final ThreadLocal<RequestMetrics> requestMetrics = new ThreadLocal<>();
    
    // In-memory counters for aggregated metrics
    private final Map<String, EndpointMetrics> endpointMetrics = new ConcurrentHashMap<>();
    
    @Inject
    public MetricsCollectionHandler(PerformanceMetricsService metricsService,
                                  MetricsConfig metricsConfig) {
        this.metricsService = metricsService;
        this.metricsConfig = metricsConfig;
        this.objectMapper = new ObjectMapper();
        logger.info("MetricsCollectionHandler initialized");
    }
    
    /**
     * Before handler - captures request start time and initial metrics
     */
    public void beforeRequest(Context ctx) {
        if (!shouldCollectMetrics(ctx)) {
            return;
        }
        
        try {
            RequestMetrics metrics = new RequestMetrics();
            metrics.startTime = System.nanoTime(); // Use nanoTime for higher precision
            metrics.path = ctx.path();
            metrics.method = ctx.method().toString();
            metrics.endpoint = generateEndpointKey(ctx);
            
            // Capture initial memory if enabled (simplified for now)
            Runtime runtime = Runtime.getRuntime();
            metrics.initialMemory = runtime.totalMemory() - runtime.freeMemory();
            
            requestMetrics.set(metrics);
            
            logger.debug("Started metrics collection for: {} {}", metrics.method, metrics.path);
            
        } catch (Exception e) {
            logger.warn("Failed to start metrics collection for request", e);
        }
    }
    
    /**
     * After handler - captures response metrics and saves to database
     */
    public void afterRequest(Context ctx) {
        RequestMetrics metrics = requestMetrics.get();
        if (metrics == null) {
            return;
        }
        
        try {
            // Calculate response time with nanosecond precision
            long endTime = System.nanoTime();
            long responseTimeNanos = endTime - metrics.startTime;
            double responseTimeMs = responseTimeNanos / 1_000_000.0; // Convert to milliseconds with decimal precision
            
            // Capture final memory
            long memoryIncrease = 0;
            if (metrics.initialMemory > 0) {
                Runtime runtime = Runtime.getRuntime();
                long finalMemory = runtime.totalMemory() - runtime.freeMemory();
                memoryIncrease = finalMemory - metrics.initialMemory;
            }
            
            // Update endpoint metrics
            updateEndpointMetrics(metrics.endpoint, responseTimeMs, ctx.status().getCode());

            // Check sampling rate
            if (shouldSampleRequest()) {
                // Create and save performance metrics
                PerformanceMetrics performanceMetrics = createPerformanceMetrics(
                    metrics, responseTimeMs, memoryIncrease, ctx.status().getCode());
                
                if (metricsConfig.getMetricsCollection().isAsyncSave()) {
                    saveMetricsAsync(performanceMetrics);
                } else {
                    metricsService.saveMetrics(performanceMetrics);
                }
            }
            
            logger.debug("Completed metrics collection for: {} {} - {:.3f}ms",
                        metrics.method, metrics.path, responseTimeMs);
            
        } catch (Exception e) {
            logger.warn("Failed to complete metrics collection for request", e);
        } finally {
            requestMetrics.remove();
        }
    }
    
    /**
     * Get current endpoint metrics summary
     */
    public Map<String, Object> getEndpointMetricsSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        for (Map.Entry<String, EndpointMetrics> entry : endpointMetrics.entrySet()) {
            EndpointMetrics metrics = entry.getValue();
            Map<String, Object> endpointSummary = new HashMap<>();
            
            endpointSummary.put("totalRequests", metrics.totalRequests);
            endpointSummary.put("averageResponseTime",
                metrics.totalRequests > 0 ? metrics.totalResponseTime / metrics.totalRequests : 0.0);
            endpointSummary.put("successRate",
                metrics.totalRequests > 0 ? (double) metrics.successfulRequests / metrics.totalRequests * 100 : 0.0);
            endpointSummary.put("lastRequestTime", metrics.lastRequestTime);
            
            summary.put(entry.getKey(), endpointSummary);
        }
        
        return summary;
    }
    
    /**
     * Reset endpoint metrics (useful for testing)
     */
    public void resetMetrics() {
        endpointMetrics.clear();
        logger.info("Endpoint metrics reset");
    }
    
    // Private helper methods
    
    private boolean shouldCollectMetrics(Context ctx) {
        if (!metricsConfig.getMetricsCollection().isEnabled()) {
            return false;
        }

        // Check if the path should be excluded from metrics collection
        String requestPath = ctx.path();
        List<String> excludePaths = metricsConfig.getMetricsCollection().getExcludePaths();

        if (excludePaths != null) {
            for (String excludePath : excludePaths) {
                if (requestPath.startsWith(excludePath)) {
                    logger.debug("Excluding path from metrics collection: {}", requestPath);
                    return false;
                }
            }
        }

        return true;
    }
    
    private boolean shouldSampleRequest() {
        double samplingRate = metricsConfig.getMetricsCollection().getSamplingRate();
        return Math.random() < samplingRate;
    }
    
    private String generateEndpointKey(Context ctx) {
        return ctx.method() + " " + normalizePathForMetrics(ctx.path());
    }
    
    private String normalizePathForMetrics(String path) {
        // Replace path parameters with placeholders for better aggregation
        return path.replaceAll("/\\d+", "/{id}")
                  .replaceAll("/[A-Z]{2,}", "/{symbol}"); // For stock symbols
    }
    
    private void updateEndpointMetrics(String endpoint, double responseTime, int statusCode) {
        endpointMetrics.compute(endpoint, (key, existing) -> {
            if (existing == null) {
                existing = new EndpointMetrics();
            }

            existing.totalRequests++;
            existing.totalResponseTime += responseTime;
            existing.lastRequestTime = LocalDateTime.now();

            if (statusCode >= 200 && statusCode < 400) {
                existing.successfulRequests++;
            }

            return existing;
        });
    }
    
    private PerformanceMetrics createPerformanceMetrics(RequestMetrics requestMetrics,
                                                       double responseTime,
                                                       long memoryIncrease,
                                                       int statusCode) {
        String testName = "API Request - " + requestMetrics.endpoint;
        String testType = "API_REQUEST";

        PerformanceMetrics metrics = new PerformanceMetrics(testName, testType);
        metrics.setTotalRequests(1);
        metrics.setTotalTimeMs(Math.round(responseTime)); // Round to nearest millisecond for storage
        metrics.setAverageResponseTimeMs(responseTime); // Keep full precision for average
        metrics.setTestPassed(statusCode >= 200 && statusCode < 400);
        
        // Always include memory metrics for now
        Runtime runtime = Runtime.getRuntime();
        metrics.setMemoryUsageBytes(runtime.totalMemory() - runtime.freeMemory());
        metrics.setMemoryIncreaseBytes(memoryIncrease);
        
        // Add additional metrics as JSON
        try {
            Map<String, Object> additionalMetrics = new HashMap<>();
            additionalMetrics.put("endpoint", requestMetrics.endpoint);
            additionalMetrics.put("method", requestMetrics.method);
            additionalMetrics.put("path", requestMetrics.path);
            additionalMetrics.put("statusCode", statusCode);
            
            metrics.setAdditionalMetrics(objectMapper.writeValueAsString(additionalMetrics));
        } catch (Exception e) {
            logger.warn("Failed to serialize additional metrics", e);
        }
        
        return metrics;
    }
    
    private void saveMetricsAsync(PerformanceMetrics metrics) {
        CompletableFuture.runAsync(() -> {
            try {
                metricsService.saveMetrics(metrics);
            } catch (Exception e) {
                logger.error("Failed to save metrics asynchronously", e);
            }
        });
    }
    
    // Inner classes for data structures
    
    private static class RequestMetrics {
        long startTime;
        String path;
        String method;
        String endpoint;
        long initialMemory;
    }
    
    private static class EndpointMetrics {
        long totalRequests = 0;
        double totalResponseTime = 0.0;
        long successfulRequests = 0;
        LocalDateTime lastRequestTime;
    }
}

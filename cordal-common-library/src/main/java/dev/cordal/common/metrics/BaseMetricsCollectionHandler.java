package dev.cordal.common.metrics;

import dev.cordal.common.model.PerformanceMetrics;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Abstract base class for metrics collection handlers
 * Provides common metrics collection patterns and functionality
 */
public abstract class BaseMetricsCollectionHandler {
    private static final Logger logger = LoggerFactory.getLogger(BaseMetricsCollectionHandler.class);
    
    protected final ThreadLocal<RequestMetrics> requestMetrics = new ThreadLocal<>();
    protected final Map<String, EndpointMetrics> endpointMetrics = new ConcurrentHashMap<>();

    /**
     * Abstract method to get metrics collection configuration
     */
    protected abstract MetricsCollectionConfig getMetricsConfig();

    /**
     * Abstract method to save metrics (implementation specific)
     */
    protected abstract void saveMetrics(PerformanceMetrics metrics);

    /**
     * Before handler - captures request start time and initial metrics
     */
    public void beforeRequest(Context ctx) {
        if (!shouldCollectMetrics(ctx)) {
            return;
        }
        
        try {
            RequestMetrics metrics = new RequestMetrics();
            metrics.startTime = System.nanoTime();
            metrics.path = ctx.path();
            metrics.method = ctx.method().toString();
            metrics.endpoint = generateEndpointKey(ctx);
            
            // Capture initial memory
            Runtime runtime = Runtime.getRuntime();
            metrics.initialMemory = runtime.totalMemory() - runtime.freeMemory();
            
            requestMetrics.set(metrics);
            
            logger.debug("Started metrics collection for: {} {}", metrics.method, metrics.path);
            
        } catch (Exception e) {
            logger.warn("Failed to start metrics collection for request", e);
        }
    }

    /**
     * After handler - processes and saves metrics
     */
    public void afterRequest(Context ctx) {
        RequestMetrics metrics = requestMetrics.get();
        if (metrics == null) {
            return;
        }
        
        try {
            long endTime = System.nanoTime();
            double responseTimeMs = (endTime - metrics.startTime) / 1_000_000.0;
            
            // Calculate memory increase
            Runtime runtime = Runtime.getRuntime();
            long currentMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryIncrease = Math.max(0, currentMemory - metrics.initialMemory);
            
            // Update endpoint metrics
            updateEndpointMetrics(metrics.endpoint, responseTimeMs, ctx.status().getCode());

            // Check sampling rate
            if (shouldSampleRequest()) {
                // Create and save performance metrics
                PerformanceMetrics performanceMetrics = createPerformanceMetrics(
                    metrics, responseTimeMs, memoryIncrease, ctx.status().getCode());
                
                if (getMetricsConfig().isAsyncSave()) {
                    saveMetricsAsync(performanceMetrics);
                } else {
                    saveMetrics(performanceMetrics);
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
     * Check if metrics should be collected for this request
     */
    protected boolean shouldCollectMetrics(Context ctx) {
        MetricsCollectionConfig config = getMetricsConfig();
        
        if (!config.isEnabled()) {
            return false;
        }
        
        String path = ctx.path();
        List<String> excludePaths = config.getExcludePaths();
        
        return excludePaths == null || excludePaths.stream().noneMatch(path::startsWith);
    }

    /**
     * Check if this request should be sampled based on sampling rate
     */
    protected boolean shouldSampleRequest() {
        double samplingRate = getMetricsConfig().getSamplingRate();
        return ThreadLocalRandom.current().nextDouble() < samplingRate;
    }

    /**
     * Generate a unique endpoint key for metrics aggregation
     */
    protected String generateEndpointKey(Context ctx) {
        return ctx.method().toString() + " " + ctx.path();
    }

    /**
     * Update endpoint-specific metrics
     */
    protected void updateEndpointMetrics(String endpoint, double responseTime, int statusCode) {
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

    /**
     * Create performance metrics from request data
     */
    protected PerformanceMetrics createPerformanceMetrics(RequestMetrics requestMetrics,
                                                         double responseTime,
                                                         long memoryIncrease,
                                                         int statusCode) {
        String testName = "API Request - " + requestMetrics.endpoint;
        String testType = "API_REQUEST";

        PerformanceMetrics metrics = new PerformanceMetrics(testName, testType);
        metrics.setTotalRequests(1);
        metrics.setTotalTimeMs(Math.round(responseTime));
        metrics.setAverageResponseTimeMs(responseTime);
        metrics.setTestPassed(statusCode >= 200 && statusCode < 400);
        metrics.setMemoryIncreaseBytes(memoryIncrease);

        return metrics;
    }

    /**
     * Save metrics asynchronously
     */
    protected void saveMetricsAsync(PerformanceMetrics metrics) {
        CompletableFuture.runAsync(() -> {
            try {
                saveMetrics(metrics);
            } catch (Exception e) {
                logger.error("Failed to save metrics asynchronously", e);
            }
        });
    }

    /**
     * Get endpoint metrics summary
     */
    public Map<String, EndpointMetrics> getEndpointMetrics() {
        return Map.copyOf(endpointMetrics);
    }

    /**
     * Reset endpoint metrics
     */
    public void resetMetrics() {
        endpointMetrics.clear();
        logger.info("Endpoint metrics reset");
    }

    // Inner classes for data structures
    
    protected static class RequestMetrics {
        long startTime;
        String path;
        String method;
        String endpoint;
        long initialMemory;
    }
    
    public static class EndpointMetrics {
        public long totalRequests = 0;
        public double totalResponseTime = 0.0;
        public long successfulRequests = 0;
        public LocalDateTime lastRequestTime;
        
        public double getAverageResponseTime() {
            return totalRequests > 0 ? totalResponseTime / totalRequests : 0.0;
        }
        
        public double getSuccessRate() {
            return totalRequests > 0 ? (double) successfulRequests / totalRequests : 0.0;
        }
    }
}

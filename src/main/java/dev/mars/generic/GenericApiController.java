package dev.mars.generic;

import dev.mars.exception.ApiException;
import dev.mars.generic.config.ApiEndpointConfig;
import dev.mars.generic.model.GenericResponse;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Generic API controller that handles all configured endpoints
 */
@Singleton
public class GenericApiController {
    private static final Logger logger = LoggerFactory.getLogger(GenericApiController.class);
    
    private final GenericApiService genericApiService;
    
    @Inject
    public GenericApiController(GenericApiService genericApiService) {
        this.genericApiService = genericApiService;
    }
    
    /**
     * Handle generic endpoint requests
     */
    public void handleEndpointRequest(Context ctx, String endpointName) {
        logger.debug("Handling request for endpoint: {}", endpointName);
        
        try {
            // Extract request parameters
            Map<String, Object> requestParameters = extractRequestParameters(ctx);
            
            // Check if async processing is requested
            boolean async = parseBooleanParameter(ctx, "async", false);
            
            logger.debug("Request parameters for endpoint {}: {}, async: {}", 
                        endpointName, requestParameters, async);
            
            if (async) {
                handleAsyncRequest(ctx, endpointName, requestParameters);
            } else {
                GenericResponse response = genericApiService.executeEndpoint(endpointName, requestParameters);
                ctx.json(response);
            }
            
        } catch (Exception e) {
            logger.error("Error handling endpoint request: {}", endpointName, e);
            throw e;
        }
    }
    
    /**
     * Handle async endpoint requests
     */
    private void handleAsyncRequest(Context ctx, String endpointName, Map<String, Object> requestParameters) {
        logger.debug("Handling async request for endpoint: {}", endpointName);

        // Generate a request ID for tracking
        String requestId = java.util.UUID.randomUUID().toString();

        // Submit the async request (fire and forget)
        genericApiService.executeEndpointAsync(endpointName, requestParameters)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.error("Async request {} failed for endpoint {}", requestId, endpointName, throwable);
                } else {
                    logger.debug("Async request {} completed successfully for endpoint {}", requestId, endpointName);
                }
            });

        // Return immediate response
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Request submitted for async processing");
        response.put("requestId", requestId);
        response.put("endpoint", endpointName);
        response.put("timestamp", System.currentTimeMillis());

        ctx.json(response);
    }
    
    /**
     * Get available endpoints
     */
    public void getAvailableEndpoints(Context ctx) {
        logger.debug("Getting available endpoints");
        
        try {
            Map<String, ApiEndpointConfig> endpoints = genericApiService.getAvailableEndpoints();
            
            // Create summary response
            Map<String, Object> response = new HashMap<>();
            response.put("totalEndpoints", endpoints.size());
            response.put("endpoints", endpoints);
            
            ctx.json(response);
            
        } catch (Exception e) {
            logger.error("Error getting available endpoints", e);
            throw e;
        }
    }
    
    /**
     * Get specific endpoint configuration
     */
    public void getEndpointConfiguration(Context ctx) {
        String endpointName = ctx.pathParam("endpointName");
        logger.debug("Getting configuration for endpoint: {}", endpointName);
        
        try {
            ApiEndpointConfig config = genericApiService.getEndpointConfiguration(endpointName)
                    .orElseThrow(() -> ApiException.notFound("Endpoint not found: " + endpointName));
            
            ctx.json(config);
            
        } catch (Exception e) {
            logger.error("Error getting endpoint configuration: {}", endpointName, e);
            throw e;
        }
    }
    
    /**
     * Get all query configurations
     */
    public void getQueryConfigurations(Context ctx) {
        logger.debug("Getting all query configurations");

        try {
            var queryConfigurations = genericApiService.getAllQueryConfigurations();

            // Create summary response
            Map<String, Object> response = new HashMap<>();
            response.put("totalQueries", queryConfigurations.size());
            response.put("queries", queryConfigurations);

            ctx.json(response);

        } catch (Exception e) {
            logger.error("Error getting query configurations", e);
            throw e;
        }
    }

    /**
     * Get specific query configuration
     */
    public void getQueryConfiguration(Context ctx) {
        String queryName = ctx.pathParam("queryName");
        logger.debug("Getting configuration for query: {}", queryName);

        try {
            var queryConfig = genericApiService.getQueryConfiguration(queryName)
                    .orElseThrow(() -> ApiException.notFound("Query not found: " + queryName));

            ctx.json(queryConfig);

        } catch (Exception e) {
            logger.error("Error getting query configuration: {}", queryName, e);
            throw e;
        }
    }

    /**
     * Get complete configuration (endpoints + queries)
     */
    public void getCompleteConfiguration(Context ctx) {
        logger.debug("Getting complete configuration");

        try {
            var endpoints = genericApiService.getAvailableEndpoints();
            var queries = genericApiService.getAllQueryConfigurations();

            // Create comprehensive response
            Map<String, Object> response = new HashMap<>();
            response.put("summary", Map.of(
                "totalEndpoints", endpoints.size(),
                "totalQueries", queries.size(),
                "timestamp", System.currentTimeMillis()
            ));
            response.put("endpoints", endpoints);
            response.put("queries", queries);

            ctx.json(response);

        } catch (Exception e) {
            logger.error("Error getting complete configuration", e);
            throw e;
        }
    }

    /**
     * Extract request parameters from context
     */
    private Map<String, Object> extractRequestParameters(Context ctx) {
        Map<String, Object> parameters = new HashMap<>();
        
        // Add query parameters
        ctx.queryParamMap().forEach((key, values) -> {
            if (values != null && !values.isEmpty()) {
                // Use first value if multiple values provided
                parameters.put(key, values.get(0));
            }
        });
        
        // Add path parameters
        ctx.pathParamMap().forEach(parameters::put);
        
        // Add form parameters if present
        ctx.formParamMap().forEach((key, values) -> {
            if (values != null && !values.isEmpty()) {
                parameters.put(key, values.get(0));
            }
        });
        
        return parameters;
    }
    
    /**
     * Parse boolean parameter with default value
     */
    private boolean parseBooleanParameter(Context ctx, String paramName, boolean defaultValue) {
        String value = ctx.queryParam(paramName);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        
        try {
            return Boolean.parseBoolean(value);
        } catch (Exception e) {
            logger.warn("Invalid boolean value for parameter {}: {}, using default: {}", 
                       paramName, value, defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Parse integer parameter with default value
     */
    private int parseIntParameter(Context ctx, String paramName, int defaultValue) {
        String value = ctx.queryParam(paramName);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer value for parameter {}: {}, using default: {}", 
                       paramName, value, defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Health check for generic API
     */
    public void getHealthStatus(Context ctx) {
        logger.debug("Generic API health check");
        
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("service", "Generic API Service");
            health.put("timestamp", System.currentTimeMillis());
            
            // Add endpoint count
            int endpointCount = genericApiService.getAvailableEndpoints().size();
            health.put("availableEndpoints", endpointCount);
            
            ctx.json(health);
            
        } catch (Exception e) {
            logger.error("Health check failed", e);
            
            Map<String, Object> health = new HashMap<>();
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("timestamp", System.currentTimeMillis());
            
            ctx.status(503).json(health);
        }
    }
}

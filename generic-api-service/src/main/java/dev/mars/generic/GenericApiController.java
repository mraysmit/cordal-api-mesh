package dev.mars.generic;

import dev.mars.common.exception.ApiException;
import dev.mars.generic.config.ApiEndpointConfig;
import dev.mars.generic.config.DatabaseConfig;
import dev.mars.generic.model.GenericResponse;
import dev.mars.generic.management.UsageStatisticsService;
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
    private final UsageStatisticsService statisticsService;

    @Inject
    public GenericApiController(GenericApiService genericApiService, UsageStatisticsService statisticsService) {
        this.genericApiService = genericApiService;
        this.statisticsService = statisticsService;
    }

    /**
     * Get the generic API service (for internal use)
     */
    public GenericApiService getGenericApiService() {
        return genericApiService;
    }
    
    /**
     * Handle generic endpoint requests
     */
    public void handleEndpointRequest(Context ctx, String endpointName) {
        logger.debug("Handling request for endpoint: {}", endpointName);

        long startTime = System.currentTimeMillis();
        boolean success = false;

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

            success = true;

        } catch (Exception e) {
            logger.error("Error handling endpoint request: {}", endpointName, e);
            throw e;
        } finally {
            // Record usage statistics
            long executionTime = System.currentTimeMillis() - startTime;
            statisticsService.recordEndpointUsage(endpointName, executionTime, success);
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
     * Get complete configuration (endpoints + queries + databases)
     */
    public void getCompleteConfiguration(Context ctx) {
        logger.debug("Getting complete configuration");

        try {
            var endpoints = genericApiService.getAvailableEndpoints();
            var queries = genericApiService.getAllQueryConfigurations();
            var databases = genericApiService.getAllDatabaseConfigurations();

            // Create comprehensive response
            Map<String, Object> response = new HashMap<>();
            response.put("summary", Map.of(
                "totalEndpoints", endpoints.size(),
                "totalQueries", queries.size(),
                "totalDatabases", databases.size(),
                "timestamp", System.currentTimeMillis()
            ));
            response.put("endpoints", endpoints);
            response.put("queries", queries);
            response.put("databases", databases);

            ctx.json(response);

        } catch (Exception e) {
            logger.error("Error getting complete configuration", e);
            throw e;
        }
    }

    /**
     * Get all database configurations
     */
    public void getDatabaseConfigurations(Context ctx) {
        logger.debug("Getting all database configurations");

        try {
            var databaseConfigurations = genericApiService.getAllDatabaseConfigurations();

            // Create summary response
            Map<String, Object> response = new HashMap<>();
            response.put("totalDatabases", databaseConfigurations.size());
            response.put("databases", databaseConfigurations);

            ctx.json(response);

        } catch (Exception e) {
            logger.error("Error getting database configurations", e);
            throw e;
        }
    }

    /**
     * Get specific database configuration
     */
    public void getDatabaseConfiguration(Context ctx) {
        String databaseName = ctx.pathParam("databaseName");
        logger.debug("Getting configuration for database: {}", databaseName);

        try {
            DatabaseConfig config = genericApiService.getDatabaseConfiguration(databaseName)
                    .orElseThrow(() -> ApiException.notFound("Database not found: " + databaseName));

            ctx.json(config);

        } catch (Exception e) {
            logger.error("Error getting database configuration: {}", databaseName, e);
            throw e;
        }
    }

    /**
     * Get configuration relationships
     */
    public void getConfigurationRelationships(Context ctx) {
        logger.debug("Getting configuration relationships");

        try {
            var relationships = genericApiService.getConfigurationRelationships();
            ctx.json(relationships);

        } catch (Exception e) {
            logger.error("Error getting configuration relationships", e);
            throw e;
        }
    }

    /**
     * Validate all configurations
     */
    public void validateConfigurations(Context ctx) {
        logger.debug("Validating all configurations");

        try {
            var validationResults = genericApiService.validateConfigurations();
            ctx.json(validationResults);

        } catch (Exception e) {
            logger.error("Error validating configurations", e);
            throw e;
        }
    }

    /**
     * Validate endpoint configurations
     */
    public void validateEndpointConfigurations(Context ctx) {
        logger.debug("Validating endpoint configurations");

        try {
            var validationResults = genericApiService.validateEndpointConfigurations();
            ctx.json(validationResults);

        } catch (Exception e) {
            logger.error("Error validating endpoint configurations", e);
            throw e;
        }
    }

    /**
     * Validate query configurations
     */
    public void validateQueryConfigurations(Context ctx) {
        logger.debug("Validating query configurations");

        try {
            var validationResults = genericApiService.validateQueryConfigurations();
            ctx.json(validationResults);

        } catch (Exception e) {
            logger.error("Error validating query configurations", e);
            throw e;
        }
    }

    /**
     * Validate database configurations
     */
    public void validateDatabaseConfigurations(Context ctx) {
        logger.debug("Validating database configurations");

        try {
            var validationResults = genericApiService.validateDatabaseConfigurations();
            ctx.json(validationResults);

        } catch (Exception e) {
            logger.error("Error validating database configurations", e);
            throw e;
        }
    }

    /**
     * Validate endpoint connectivity
     */
    public void validateEndpointConnectivity(Context ctx) {
        logger.debug("Validating endpoint connectivity");

        try {
            var validationResults = genericApiService.validateEndpointConnectivity();
            ctx.json(validationResults);

        } catch (Exception e) {
            logger.error("Error validating endpoint connectivity", e);
            throw e;
        }
    }

    // ========== GRANULAR CONFIGURATION ENDPOINTS ==========

    /**
     * Get endpoint configuration schema (field names and data types)
     */
    public void getEndpointConfigurationSchema(Context ctx) {
        logger.debug("Getting endpoint configuration schema");

        try {
            var schema = genericApiService.getEndpointConfigurationSchema();
            ctx.json(schema);

        } catch (Exception e) {
            logger.error("Error getting endpoint configuration schema", e);
            throw e;
        }
    }

    /**
     * Get only parameters from all endpoint configurations
     */
    public void getEndpointParameters(Context ctx) {
        logger.debug("Getting endpoint parameters");

        try {
            var parameters = genericApiService.getEndpointParameters();
            ctx.json(parameters);

        } catch (Exception e) {
            logger.error("Error getting endpoint parameters", e);
            throw e;
        }
    }

    /**
     * Get database connections referenced by endpoints
     */
    public void getEndpointDatabaseConnections(Context ctx) {
        logger.debug("Getting endpoint database connections");

        try {
            var connections = genericApiService.getEndpointDatabaseConnections();
            ctx.json(connections);

        } catch (Exception e) {
            logger.error("Error getting endpoint database connections", e);
            throw e;
        }
    }

    /**
     * Get endpoint configuration summary
     */
    public void getEndpointConfigurationSummary(Context ctx) {
        logger.debug("Getting endpoint configuration summary");

        try {
            var summary = genericApiService.getEndpointConfigurationSummary();
            ctx.json(summary);

        } catch (Exception e) {
            logger.error("Error getting endpoint configuration summary", e);
            throw e;
        }
    }

    /**
     * Get query configuration schema (field names and data types)
     */
    public void getQueryConfigurationSchema(Context ctx) {
        logger.debug("Getting query configuration schema");

        try {
            var schema = genericApiService.getQueryConfigurationSchema();
            ctx.json(schema);

        } catch (Exception e) {
            logger.error("Error getting query configuration schema", e);
            throw e;
        }
    }

    /**
     * Get only parameters from all query configurations
     */
    public void getQueryParameters(Context ctx) {
        logger.debug("Getting query parameters");

        try {
            var parameters = genericApiService.getQueryParameters();
            ctx.json(parameters);

        } catch (Exception e) {
            logger.error("Error getting query parameters", e);
            throw e;
        }
    }

    /**
     * Get database connections referenced by queries
     */
    public void getQueryDatabaseConnections(Context ctx) {
        logger.debug("Getting query database connections");

        try {
            var connections = genericApiService.getQueryDatabaseConnections();
            ctx.json(connections);

        } catch (Exception e) {
            logger.error("Error getting query database connections", e);
            throw e;
        }
    }

    /**
     * Get query configuration summary
     */
    public void getQueryConfigurationSummary(Context ctx) {
        logger.debug("Getting query configuration summary");

        try {
            var summary = genericApiService.getQueryConfigurationSummary();
            ctx.json(summary);

        } catch (Exception e) {
            logger.error("Error getting query configuration summary", e);
            throw e;
        }
    }

    /**
     * Get database configuration schema (field names and data types)
     */
    public void getDatabaseConfigurationSchema(Context ctx) {
        logger.debug("Getting database configuration schema");

        try {
            var schema = genericApiService.getDatabaseConfigurationSchema();
            ctx.json(schema);

        } catch (Exception e) {
            logger.error("Error getting database configuration schema", e);
            throw e;
        }
    }

    /**
     * Get only connection parameters (pool settings) from database configurations
     */
    public void getDatabaseParameters(Context ctx) {
        logger.debug("Getting database parameters");

        try {
            var parameters = genericApiService.getDatabaseParameters();
            ctx.json(parameters);

        } catch (Exception e) {
            logger.error("Error getting database parameters", e);
            throw e;
        }
    }

    /**
     * Get only connection details from database configurations
     */
    public void getDatabaseConnections(Context ctx) {
        logger.debug("Getting database connections");

        try {
            var connections = genericApiService.getDatabaseConnections();
            ctx.json(connections);

        } catch (Exception e) {
            logger.error("Error getting database connections", e);
            throw e;
        }
    }

    /**
     * Get database configuration summary
     */
    public void getDatabaseConfigurationSummary(Context ctx) {
        logger.debug("Getting database configuration summary");

        try {
            var summary = genericApiService.getDatabaseConfigurationSummary();
            ctx.json(summary);

        } catch (Exception e) {
            logger.error("Error getting database configuration summary", e);
            throw e;
        }
    }

    /**
     * Validate configuration relationships
     */
    public void validateConfigurationRelationships(Context ctx) {
        logger.debug("Validating configuration relationships");

        try {
            var validationResults = genericApiService.validateConfigurationRelationships();
            ctx.json(validationResults);

        } catch (Exception e) {
            logger.error("Error validating configuration relationships", e);
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

            // Get endpoint availability information
            int availableEndpointCount = genericApiService.getAvailableEndpoints().size();
            int totalEndpointCount = genericApiService.getAllEndpoints().size();
            Map<String, String> unavailableEndpoints = genericApiService.getUnavailableEndpoints();

            // Determine overall status
            String status;
            if (availableEndpointCount == 0) {
                status = "DOWN";
            } else if (unavailableEndpoints.isEmpty()) {
                status = "UP";
            } else {
                status = "DEGRADED";
            }

            health.put("status", status);
            health.put("service", "Generic API Service");
            health.put("timestamp", System.currentTimeMillis());
            health.put("availableEndpoints", availableEndpointCount);
            health.put("totalEndpoints", totalEndpointCount);

            if (!unavailableEndpoints.isEmpty()) {
                health.put("unavailableEndpoints", unavailableEndpoints);
                health.put("message", unavailableEndpoints.size() + " endpoint(s) unavailable due to database connectivity issues");
            }

            // Set appropriate HTTP status
            if ("DOWN".equals(status)) {
                ctx.status(503); // Service Unavailable
            } else if ("DEGRADED".equals(status)) {
                ctx.status(200); // OK but with warnings
            } else {
                ctx.status(200); // OK
            }

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

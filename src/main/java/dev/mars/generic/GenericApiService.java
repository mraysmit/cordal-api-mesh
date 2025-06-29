package dev.mars.generic;

import dev.mars.dto.PagedResponse;
import dev.mars.exception.ApiException;
import dev.mars.generic.config.ApiEndpointConfig;
import dev.mars.generic.config.EndpointConfigurationManager;
import dev.mars.generic.config.QueryConfig;
import dev.mars.generic.model.GenericResponse;
import dev.mars.generic.model.QueryParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Generic API service that handles requests based on configuration
 */
@Singleton
public class GenericApiService {
    private static final Logger logger = LoggerFactory.getLogger(GenericApiService.class);
    
    private final GenericRepository genericRepository;
    private final EndpointConfigurationManager configurationManager;
    private final Executor asyncExecutor;
    
    @Inject
    public GenericApiService(GenericRepository genericRepository, 
                           EndpointConfigurationManager configurationManager) {
        this.genericRepository = genericRepository;
        this.configurationManager = configurationManager;
        this.asyncExecutor = ForkJoinPool.commonPool();
    }
    
    /**
     * Execute endpoint request synchronously
     */
    public GenericResponse executeEndpoint(String endpointName, Map<String, Object> requestParameters) {
        logger.debug("Executing endpoint: {} with parameters: {}", endpointName, requestParameters);
        
        // Get endpoint configuration
        ApiEndpointConfig endpointConfig = configurationManager.getEndpointConfig(endpointName)
                .orElseThrow(() -> ApiException.notFound("Endpoint not found: " + endpointName));
        
        // Get query configuration
        QueryConfig queryConfig = configurationManager.getQueryConfig(endpointConfig.getQuery())
                .orElseThrow(() -> ApiException.internalError("Query not found: " + endpointConfig.getQuery()));
        
        // Process parameters
        List<QueryParameter> queryParameters = processParameters(endpointConfig, queryConfig, requestParameters);
        
        // Execute based on response type
        if (endpointConfig.getPagination() != null && endpointConfig.getPagination().isEnabled()) {
            return executePaginatedEndpoint(endpointConfig, queryConfig, queryParameters, requestParameters);
        } else {
            return executeSingleEndpoint(queryConfig, queryParameters);
        }
    }
    
    /**
     * Execute endpoint request asynchronously
     */
    public CompletableFuture<GenericResponse> executeEndpointAsync(String endpointName, 
                                                                  Map<String, Object> requestParameters) {
        logger.debug("Executing endpoint asynchronously: {} with parameters: {}", endpointName, requestParameters);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeEndpoint(endpointName, requestParameters);
            } catch (Exception e) {
                logger.error("Async execution failed for endpoint: {}", endpointName, e);
                throw e;
            }
        }, asyncExecutor);
    }
    
    /**
     * Execute paginated endpoint
     */
    private GenericResponse executePaginatedEndpoint(ApiEndpointConfig endpointConfig, 
                                                   QueryConfig queryConfig,
                                                   List<QueryParameter> queryParameters,
                                                   Map<String, Object> requestParameters) {
        
        // Extract pagination parameters
        int page = getIntParameter(requestParameters, "page", 0);
        int size = getIntParameter(requestParameters, "size", endpointConfig.getPagination().getDefaultSize());
        
        // Validate pagination parameters
        validatePaginationParameters(page, size, endpointConfig.getPagination().getMaxSize());
        
        // Execute main query
        List<Map<String, Object>> results = genericRepository.executeQuery(queryConfig, queryParameters);
        
        // Execute count query if available
        long totalElements = 0;
        if (endpointConfig.getCountQuery() != null) {
            QueryConfig countQueryConfig = configurationManager.getQueryConfig(endpointConfig.getCountQuery())
                    .orElseThrow(() -> ApiException.internalError("Count query not found: " + endpointConfig.getCountQuery()));
            
            // Remove pagination parameters for count query
            List<QueryParameter> countParameters = removeParametersByName(queryParameters, Arrays.asList("limit", "offset"));
            totalElements = genericRepository.executeCountQuery(countQueryConfig, countParameters);
        }
        
        logger.debug("Paginated query returned {} results out of {} total", results.size(), totalElements);
        
        return GenericResponse.paged(results, page, size, totalElements);
    }
    
    /**
     * Execute single result endpoint
     */
    private GenericResponse executeSingleEndpoint(QueryConfig queryConfig, List<QueryParameter> queryParameters) {
        List<Map<String, Object>> results = genericRepository.executeQuery(queryConfig, queryParameters);
        
        if (results.isEmpty()) {
            throw ApiException.notFound("No data found");
        }
        
        if (results.size() == 1) {
            return GenericResponse.single(results.get(0));
        } else {
            return GenericResponse.list(results);
        }
    }
    
    /**
     * Process and validate request parameters
     */
    private List<QueryParameter> processParameters(ApiEndpointConfig endpointConfig, 
                                                  QueryConfig queryConfig,
                                                  Map<String, Object> requestParameters) {
        
        List<QueryParameter> queryParameters = new ArrayList<>();
        Map<String, Object> processedParams = new HashMap<>(requestParameters);
        
        // Add pagination parameters if enabled
        if (endpointConfig.getPagination() != null && endpointConfig.getPagination().isEnabled()) {
            int page = getIntParameter(processedParams, "page", 0);
            int size = getIntParameter(processedParams, "size", endpointConfig.getPagination().getDefaultSize());
            int offset = page * size;
            
            processedParams.put("limit", size);
            processedParams.put("offset", offset);
        }
        
        // Map parameters to query parameters
        int position = 1;
        for (QueryConfig.QueryParameter queryParam : queryConfig.getParameters()) {
            Object value = processedParams.get(queryParam.getName());
            
            if (value == null && queryParam.isRequired()) {
                throw ApiException.badRequest("Required parameter missing: " + queryParam.getName());
            }
            
            if (value != null) {
                QueryParameter param = QueryParameter.of(
                    queryParam.getName(), 
                    value, 
                    queryParam.getType(), 
                    position++
                );
                queryParameters.add(param);
            }
        }
        
        return queryParameters;
    }
    
    /**
     * Remove parameters by name from the list
     */
    private List<QueryParameter> removeParametersByName(List<QueryParameter> parameters, List<String> namesToRemove) {
        return parameters.stream()
                .filter(param -> !namesToRemove.contains(param.getName()))
                .collect(ArrayList::new, (list, param) -> {
                    // Adjust position for removed parameters
                    param.setPosition(list.size() + 1);
                    list.add(param);
                }, ArrayList::addAll);
    }
    
    /**
     * Get integer parameter with default value
     */
    private int getIntParameter(Map<String, Object> parameters, String name, int defaultValue) {
        Object value = parameters.get(name);
        if (value == null) {
            return defaultValue;
        }
        
        if (value instanceof Integer) {
            return (Integer) value;
        }
        
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                throw ApiException.badRequest("Invalid integer value for parameter: " + name);
            }
        }
        
        throw ApiException.badRequest("Invalid parameter type for: " + name);
    }
    
    /**
     * Validate pagination parameters
     */
    private void validatePaginationParameters(int page, int size, int maxSize) {
        if (page < 0) {
            throw ApiException.badRequest("Page number cannot be negative");
        }
        
        if (size <= 0) {
            throw ApiException.badRequest("Page size must be positive");
        }
        
        if (size > maxSize) {
            throw ApiException.badRequest("Page size cannot exceed " + maxSize);
        }
    }
    
    /**
     * Get all available endpoints
     */
    public Map<String, ApiEndpointConfig> getAvailableEndpoints() {
        return configurationManager.getAllEndpointConfigurations();
    }
    
    /**
     * Get endpoint configuration
     */
    public Optional<ApiEndpointConfig> getEndpointConfiguration(String endpointName) {
        return configurationManager.getEndpointConfig(endpointName);
    }

    /**
     * Get all query configurations
     */
    public Map<String, QueryConfig> getAllQueryConfigurations() {
        return configurationManager.getAllQueryConfigurations();
    }

    /**
     * Get specific query configuration
     */
    public Optional<QueryConfig> getQueryConfiguration(String queryName) {
        return configurationManager.getQueryConfig(queryName);
    }
}

package dev.mars.generic;

import dev.mars.dto.PagedResponse;
import dev.mars.common.exception.ApiException;
import dev.mars.generic.config.ApiEndpointConfig;
import dev.mars.generic.config.DatabaseConfig;
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

    /**
     * Get all database configurations
     */
    public Map<String, DatabaseConfig> getAllDatabaseConfigurations() {
        return configurationManager.getAllDatabaseConfigurations();
    }

    /**
     * Get specific database configuration
     */
    public Optional<DatabaseConfig> getDatabaseConfiguration(String databaseName) {
        return configurationManager.getDatabaseConfig(databaseName);
    }

    // ========== GRANULAR CONFIGURATION APIS ==========

    /**
     * Get endpoint configuration schema (field names and data types)
     */
    public Map<String, Object> getEndpointConfigurationSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("configType", "endpoints");

        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(createFieldInfo("path", "String", true, "API endpoint path"));
        fields.add(createFieldInfo("method", "String", true, "HTTP method (GET, POST, etc.)"));
        fields.add(createFieldInfo("description", "String", false, "Endpoint description"));
        fields.add(createFieldInfo("query", "String", true, "Reference to query configuration"));
        fields.add(createFieldInfo("countQuery", "String", false, "Reference to count query for pagination"));
        fields.add(createFieldInfo("pagination", "PaginationConfig", false, "Pagination configuration"));
        fields.add(createFieldInfo("parameters", "List<EndpointParameter>", false, "Endpoint parameters"));
        fields.add(createFieldInfo("response", "ResponseConfig", false, "Response configuration"));

        schema.put("fields", fields);
        schema.put("timestamp", System.currentTimeMillis());

        return schema;
    }

    /**
     * Get only parameters from all endpoint configurations
     */
    public Map<String, Object> getEndpointParameters() {
        Map<String, Object> response = new HashMap<>();
        response.put("configType", "endpoints");

        Map<String, List<ApiEndpointConfig.EndpointParameter>> parameters = new HashMap<>();

        for (Map.Entry<String, ApiEndpointConfig> entry : configurationManager.getAllEndpointConfigurations().entrySet()) {
            String endpointName = entry.getKey();
            ApiEndpointConfig config = entry.getValue();

            if (config.getParameters() != null && !config.getParameters().isEmpty()) {
                parameters.put(endpointName, config.getParameters());
            }
        }

        response.put("parameters", parameters);
        response.put("totalEndpoints", configurationManager.getAllEndpointConfigurations().size());
        response.put("endpointsWithParameters", parameters.size());
        response.put("timestamp", System.currentTimeMillis());

        return response;
    }

    /**
     * Get database connections referenced by endpoints (via queries)
     */
    public Map<String, Object> getEndpointDatabaseConnections() {
        Map<String, Object> response = new HashMap<>();
        response.put("configType", "endpoints");

        Map<String, String> endpointToDatabases = new HashMap<>();
        Set<String> referencedDatabases = new HashSet<>();

        for (Map.Entry<String, ApiEndpointConfig> entry : configurationManager.getAllEndpointConfigurations().entrySet()) {
            String endpointName = entry.getKey();
            ApiEndpointConfig config = entry.getValue();

            // Get the database through the query reference
            if (config.getQuery() != null) {
                Optional<QueryConfig> queryConfig = configurationManager.getQueryConfig(config.getQuery());
                if (queryConfig.isPresent() && queryConfig.get().getDatabase() != null) {
                    String database = queryConfig.get().getDatabase();
                    endpointToDatabases.put(endpointName, database);
                    referencedDatabases.add(database);
                }
            }
        }

        response.put("endpointDatabases", endpointToDatabases);
        response.put("referencedDatabases", new ArrayList<>(referencedDatabases));
        response.put("totalEndpoints", configurationManager.getAllEndpointConfigurations().size());
        response.put("endpointsWithDatabases", endpointToDatabases.size());
        response.put("timestamp", System.currentTimeMillis());

        return response;
    }

    /**
     * Get endpoint configuration summary
     */
    public Map<String, Object> getEndpointConfigurationSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("configType", "endpoints");

        Map<String, ApiEndpointConfig> endpoints = configurationManager.getAllEndpointConfigurations();

        // Count by method
        Map<String, Integer> byMethod = new HashMap<>();
        int withPagination = 0;
        int withParameters = 0;
        Set<String> referencedQueries = new HashSet<>();
        Set<String> referencedDatabases = new HashSet<>();

        for (ApiEndpointConfig config : endpoints.values()) {
            // Count by method
            String method = config.getMethod();
            byMethod.put(method, byMethod.getOrDefault(method, 0) + 1);

            // Count pagination
            if (config.getPagination() != null && config.getPagination().isEnabled()) {
                withPagination++;
            }

            // Count parameters
            if (config.getParameters() != null && !config.getParameters().isEmpty()) {
                withParameters++;
            }

            // Collect referenced queries
            if (config.getQuery() != null) {
                referencedQueries.add(config.getQuery());

                // Get database through query
                Optional<QueryConfig> queryConfig = configurationManager.getQueryConfig(config.getQuery());
                if (queryConfig.isPresent() && queryConfig.get().getDatabase() != null) {
                    referencedDatabases.add(queryConfig.get().getDatabase());
                }
            }

            if (config.getCountQuery() != null) {
                referencedQueries.add(config.getCountQuery());
            }
        }

        summary.put("totalCount", endpoints.size());
        summary.put("byMethod", byMethod);
        summary.put("withPagination", withPagination);
        summary.put("withParameters", withParameters);
        summary.put("referencedQueries", new ArrayList<>(referencedQueries));
        summary.put("referencedDatabases", new ArrayList<>(referencedDatabases));
        summary.put("timestamp", System.currentTimeMillis());

        return summary;
    }

    /**
     * Get query configuration schema (field names and data types)
     */
    public Map<String, Object> getQueryConfigurationSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("configType", "queries");

        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(createFieldInfo("name", "String", true, "Query name"));
        fields.add(createFieldInfo("description", "String", false, "Query description"));
        fields.add(createFieldInfo("sql", "String", true, "SQL query statement"));
        fields.add(createFieldInfo("database", "String", true, "Reference to database configuration"));
        fields.add(createFieldInfo("parameters", "List<QueryParameter>", false, "Query parameters"));

        schema.put("fields", fields);
        schema.put("timestamp", System.currentTimeMillis());

        return schema;
    }

    /**
     * Get only parameters from all query configurations
     */
    public Map<String, Object> getQueryParameters() {
        Map<String, Object> response = new HashMap<>();
        response.put("configType", "queries");

        Map<String, List<QueryConfig.QueryParameter>> parameters = new HashMap<>();

        for (Map.Entry<String, QueryConfig> entry : configurationManager.getAllQueryConfigurations().entrySet()) {
            String queryName = entry.getKey();
            QueryConfig config = entry.getValue();

            if (config.getParameters() != null && !config.getParameters().isEmpty()) {
                parameters.put(queryName, config.getParameters());
            }
        }

        response.put("parameters", parameters);
        response.put("totalQueries", configurationManager.getAllQueryConfigurations().size());
        response.put("queriesWithParameters", parameters.size());
        response.put("timestamp", System.currentTimeMillis());

        return response;
    }

    /**
     * Get database connections referenced by queries
     */
    public Map<String, Object> getQueryDatabaseConnections() {
        Map<String, Object> response = new HashMap<>();
        response.put("configType", "queries");

        Map<String, String> queryToDatabases = new HashMap<>();
        Set<String> referencedDatabases = new HashSet<>();

        for (Map.Entry<String, QueryConfig> entry : configurationManager.getAllQueryConfigurations().entrySet()) {
            String queryName = entry.getKey();
            QueryConfig config = entry.getValue();

            if (config.getDatabase() != null) {
                queryToDatabases.put(queryName, config.getDatabase());
                referencedDatabases.add(config.getDatabase());
            }
        }

        response.put("queryDatabases", queryToDatabases);
        response.put("referencedDatabases", new ArrayList<>(referencedDatabases));
        response.put("totalQueries", configurationManager.getAllQueryConfigurations().size());
        response.put("queriesWithDatabases", queryToDatabases.size());
        response.put("timestamp", System.currentTimeMillis());

        return response;
    }

    /**
     * Get query configuration summary
     */
    public Map<String, Object> getQueryConfigurationSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("configType", "queries");

        Map<String, QueryConfig> queries = configurationManager.getAllQueryConfigurations();

        int withParameters = 0;
        Set<String> referencedDatabases = new HashSet<>();
        Map<String, Integer> parameterCounts = new HashMap<>();

        for (QueryConfig config : queries.values()) {
            // Count parameters
            if (config.getParameters() != null && !config.getParameters().isEmpty()) {
                withParameters++;
                int paramCount = config.getParameters().size();
                parameterCounts.put(config.getName(), paramCount);
            }

            // Collect referenced databases
            if (config.getDatabase() != null) {
                referencedDatabases.add(config.getDatabase());
            }
        }

        summary.put("totalCount", queries.size());
        summary.put("withParameters", withParameters);
        summary.put("referencedDatabases", new ArrayList<>(referencedDatabases));
        summary.put("parameterCounts", parameterCounts);
        summary.put("timestamp", System.currentTimeMillis());

        return summary;
    }

    /**
     * Get database configuration schema (field names and data types)
     */
    public Map<String, Object> getDatabaseConfigurationSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("configType", "databases");

        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(createFieldInfo("name", "String", true, "Database name"));
        fields.add(createFieldInfo("description", "String", false, "Database description"));
        fields.add(createFieldInfo("url", "String", true, "Database connection URL"));
        fields.add(createFieldInfo("username", "String", true, "Database username"));
        fields.add(createFieldInfo("password", "String", true, "Database password"));
        fields.add(createFieldInfo("driver", "String", true, "Database driver class"));
        fields.add(createFieldInfo("pool", "PoolConfig", false, "Connection pool configuration"));

        schema.put("fields", fields);
        schema.put("timestamp", System.currentTimeMillis());

        return schema;
    }

    /**
     * Get only connection parameters (pool settings) from database configurations
     */
    public Map<String, Object> getDatabaseParameters() {
        Map<String, Object> response = new HashMap<>();
        response.put("configType", "databases");

        Map<String, DatabaseConfig.PoolConfig> parameters = new HashMap<>();

        for (Map.Entry<String, DatabaseConfig> entry : configurationManager.getAllDatabaseConfigurations().entrySet()) {
            String databaseName = entry.getKey();
            DatabaseConfig config = entry.getValue();

            if (config.getPool() != null) {
                parameters.put(databaseName, config.getPool());
            }
        }

        response.put("poolConfigurations", parameters);
        response.put("totalDatabases", configurationManager.getAllDatabaseConfigurations().size());
        response.put("databasesWithPoolConfig", parameters.size());
        response.put("timestamp", System.currentTimeMillis());

        return response;
    }

    /**
     * Get only connection details from database configurations
     */
    public Map<String, Object> getDatabaseConnections() {
        Map<String, Object> response = new HashMap<>();
        response.put("configType", "databases");

        Map<String, Map<String, Object>> connections = new HashMap<>();

        for (Map.Entry<String, DatabaseConfig> entry : configurationManager.getAllDatabaseConfigurations().entrySet()) {
            String databaseName = entry.getKey();
            DatabaseConfig config = entry.getValue();

            Map<String, Object> connectionInfo = new HashMap<>();
            connectionInfo.put("url", config.getUrl());
            connectionInfo.put("username", config.getUsername());
            connectionInfo.put("driver", config.getDriver());
            connectionInfo.put("description", config.getDescription());

            connections.put(databaseName, connectionInfo);
        }

        response.put("connections", connections);
        response.put("totalDatabases", configurationManager.getAllDatabaseConfigurations().size());
        response.put("timestamp", System.currentTimeMillis());

        return response;
    }

    /**
     * Get database configuration summary
     */
    public Map<String, Object> getDatabaseConfigurationSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("configType", "databases");

        Map<String, DatabaseConfig> databases = configurationManager.getAllDatabaseConfigurations();

        int withPoolConfig = 0;
        Map<String, String> driverTypes = new HashMap<>();
        Set<String> uniqueDrivers = new HashSet<>();

        for (Map.Entry<String, DatabaseConfig> entry : databases.entrySet()) {
            String databaseName = entry.getKey();
            DatabaseConfig config = entry.getValue();

            // Count pool configurations
            if (config.getPool() != null) {
                withPoolConfig++;
            }

            // Collect driver information
            if (config.getDriver() != null) {
                driverTypes.put(databaseName, config.getDriver());
                uniqueDrivers.add(config.getDriver());
            }
        }

        summary.put("totalCount", databases.size());
        summary.put("withPoolConfig", withPoolConfig);
        summary.put("driverTypes", driverTypes);
        summary.put("uniqueDrivers", new ArrayList<>(uniqueDrivers));
        summary.put("timestamp", System.currentTimeMillis());

        return summary;
    }

    /**
     * Helper method to create field information for schema APIs
     */
    private Map<String, Object> createFieldInfo(String name, String type, boolean required, String description) {
        Map<String, Object> fieldInfo = new HashMap<>();
        fieldInfo.put("name", name);
        fieldInfo.put("type", type);
        fieldInfo.put("required", required);
        fieldInfo.put("description", description);
        return fieldInfo;
    }

    /**
     * Get configuration relationships (endpoints -> queries -> databases)
     */
    public Map<String, Object> getConfigurationRelationships() {
        Map<String, Object> relationships = new HashMap<>();
        Map<String, Object> endpointRelationships = new HashMap<>();
        Map<String, Object> queryRelationships = new HashMap<>();

        // Build endpoint -> query -> database relationships
        for (Map.Entry<String, ApiEndpointConfig> entry : configurationManager.getAllEndpointConfigurations().entrySet()) {
            String endpointName = entry.getKey();
            ApiEndpointConfig endpoint = entry.getValue();

            Map<String, Object> endpointInfo = new HashMap<>();
            endpointInfo.put("path", endpoint.getPath());
            endpointInfo.put("method", endpoint.getMethod());
            endpointInfo.put("description", endpoint.getDescription());
            endpointInfo.put("query", endpoint.getQuery());
            endpointInfo.put("countQuery", endpoint.getCountQuery());

            // Get database for main query
            if (endpoint.getQuery() != null) {
                Optional<QueryConfig> queryConfig = configurationManager.getQueryConfig(endpoint.getQuery());
                if (queryConfig.isPresent()) {
                    endpointInfo.put("database", queryConfig.get().getDatabase());
                }
            }

            endpointRelationships.put(endpointName, endpointInfo);
        }

        // Build query -> database relationships
        for (Map.Entry<String, QueryConfig> entry : configurationManager.getAllQueryConfigurations().entrySet()) {
            String queryName = entry.getKey();
            QueryConfig query = entry.getValue();

            Map<String, Object> queryInfo = new HashMap<>();
            queryInfo.put("name", query.getName());
            queryInfo.put("description", query.getDescription());
            queryInfo.put("database", query.getDatabase());
            queryInfo.put("parameterCount", query.getParameters() != null ? query.getParameters().size() : 0);

            queryRelationships.put(queryName, queryInfo);
        }

        relationships.put("endpoints", endpointRelationships);
        relationships.put("queries", queryRelationships);
        relationships.put("databases", configurationManager.getAllDatabaseConfigurations());
        relationships.put("summary", Map.of(
            "totalEndpoints", endpointRelationships.size(),
            "totalQueries", queryRelationships.size(),
            "totalDatabases", configurationManager.getAllDatabaseConfigurations().size(),
            "timestamp", System.currentTimeMillis()
        ));

        return relationships;
    }

    /**
     * Validate all configurations and return validation results
     */
    public Map<String, Object> validateConfigurations() {
        Map<String, Object> validationResults = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try {
            // This will throw an exception if validation fails
            configurationManager.validateConfigurations();
            validationResults.put("status", "VALID");
            validationResults.put("message", "All configurations are valid");
        } catch (Exception e) {
            validationResults.put("status", "INVALID");
            validationResults.put("message", e.getMessage());
            errors.add(e.getMessage());
        }

        // Additional detailed validation
        validateEndpointConfigurations(errors, warnings);
        validateQueryConfigurations(errors, warnings);
        validateDatabaseConfigurations(errors, warnings);
        validateRelationships(errors, warnings);

        validationResults.put("errors", errors);
        validationResults.put("warnings", warnings);
        validationResults.put("errorCount", errors.size());
        validationResults.put("warningCount", warnings.size());
        validationResults.put("timestamp", System.currentTimeMillis());

        return validationResults;
    }

    /**
     * Validate endpoint configurations specifically
     */
    public Map<String, Object> validateEndpointConfigurations() {
        Map<String, Object> validationResults = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        validateEndpointConfigurations(errors, warnings);

        validationResults.put("status", errors.isEmpty() ? "VALID" : "INVALID");
        validationResults.put("errors", errors);
        validationResults.put("warnings", warnings);
        validationResults.put("errorCount", errors.size());
        validationResults.put("warningCount", warnings.size());
        validationResults.put("totalEndpoints", configurationManager.getAllEndpointConfigurations().size());
        validationResults.put("timestamp", System.currentTimeMillis());

        return validationResults;
    }

    /**
     * Validate query configurations specifically
     */
    public Map<String, Object> validateQueryConfigurations() {
        Map<String, Object> validationResults = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        validateQueryConfigurations(errors, warnings);

        validationResults.put("status", errors.isEmpty() ? "VALID" : "INVALID");
        validationResults.put("errors", errors);
        validationResults.put("warnings", warnings);
        validationResults.put("errorCount", errors.size());
        validationResults.put("warningCount", warnings.size());
        validationResults.put("totalQueries", configurationManager.getAllQueryConfigurations().size());
        validationResults.put("timestamp", System.currentTimeMillis());

        return validationResults;
    }

    /**
     * Validate database configurations specifically
     */
    public Map<String, Object> validateDatabaseConfigurations() {
        Map<String, Object> validationResults = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        validateDatabaseConfigurations(errors, warnings);

        validationResults.put("status", errors.isEmpty() ? "VALID" : "INVALID");
        validationResults.put("errors", errors);
        validationResults.put("warnings", warnings);
        validationResults.put("errorCount", errors.size());
        validationResults.put("warningCount", warnings.size());
        validationResults.put("totalDatabases", configurationManager.getAllDatabaseConfigurations().size());
        validationResults.put("timestamp", System.currentTimeMillis());

        return validationResults;
    }

    /**
     * Validate configuration relationships specifically
     */
    public Map<String, Object> validateConfigurationRelationships() {
        Map<String, Object> validationResults = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        validateRelationships(errors, warnings);

        validationResults.put("status", errors.isEmpty() ? "VALID" : "INVALID");
        validationResults.put("errors", errors);
        validationResults.put("warnings", warnings);
        validationResults.put("errorCount", errors.size());
        validationResults.put("warningCount", warnings.size());
        validationResults.put("timestamp", System.currentTimeMillis());

        return validationResults;
    }

    // Private validation helper methods
    private void validateEndpointConfigurations(List<String> errors, List<String> warnings) {
        for (Map.Entry<String, ApiEndpointConfig> entry : configurationManager.getAllEndpointConfigurations().entrySet()) {
            String endpointName = entry.getKey();
            ApiEndpointConfig endpoint = entry.getValue();

            if (endpoint.getPath() == null || endpoint.getPath().trim().isEmpty()) {
                errors.add("Endpoint '" + endpointName + "' has no path defined");
            }

            if (endpoint.getMethod() == null || endpoint.getMethod().trim().isEmpty()) {
                errors.add("Endpoint '" + endpointName + "' has no HTTP method defined");
            }

            if (endpoint.getQuery() == null || endpoint.getQuery().trim().isEmpty()) {
                errors.add("Endpoint '" + endpointName + "' has no query defined");
            }

            if (endpoint.getDescription() == null || endpoint.getDescription().trim().isEmpty()) {
                warnings.add("Endpoint '" + endpointName + "' has no description");
            }
        }
    }

    private void validateQueryConfigurations(List<String> errors, List<String> warnings) {
        for (Map.Entry<String, QueryConfig> entry : configurationManager.getAllQueryConfigurations().entrySet()) {
            String queryName = entry.getKey();
            QueryConfig query = entry.getValue();

            if (query.getSql() == null || query.getSql().trim().isEmpty()) {
                errors.add("Query '" + queryName + "' has no SQL defined");
            }

            if (query.getDatabase() == null || query.getDatabase().trim().isEmpty()) {
                errors.add("Query '" + queryName + "' has no database defined");
            }

            if (query.getDescription() == null || query.getDescription().trim().isEmpty()) {
                warnings.add("Query '" + queryName + "' has no description");
            }
        }
    }

    private void validateDatabaseConfigurations(List<String> errors, List<String> warnings) {
        for (Map.Entry<String, DatabaseConfig> entry : configurationManager.getAllDatabaseConfigurations().entrySet()) {
            String databaseName = entry.getKey();
            DatabaseConfig database = entry.getValue();

            if (database.getUrl() == null || database.getUrl().trim().isEmpty()) {
                errors.add("Database '" + databaseName + "' has no URL defined");
            }

            if (database.getDriver() == null || database.getDriver().trim().isEmpty()) {
                errors.add("Database '" + databaseName + "' has no driver defined");
            }

            if (database.getUsername() == null) {
                warnings.add("Database '" + databaseName + "' has no username defined");
            }

            if (database.getDescription() == null || database.getDescription().trim().isEmpty()) {
                warnings.add("Database '" + databaseName + "' has no description");
            }
        }
    }

    private void validateRelationships(List<String> errors, List<String> warnings) {
        // Validate endpoint -> query relationships
        for (Map.Entry<String, ApiEndpointConfig> entry : configurationManager.getAllEndpointConfigurations().entrySet()) {
            String endpointName = entry.getKey();
            ApiEndpointConfig endpoint = entry.getValue();

            if (endpoint.getQuery() != null && !configurationManager.hasQuery(endpoint.getQuery())) {
                errors.add("Endpoint '" + endpointName + "' references non-existent query: " + endpoint.getQuery());
            }

            if (endpoint.getCountQuery() != null && !configurationManager.hasQuery(endpoint.getCountQuery())) {
                errors.add("Endpoint '" + endpointName + "' references non-existent count query: " + endpoint.getCountQuery());
            }
        }

        // Validate query -> database relationships
        for (Map.Entry<String, QueryConfig> entry : configurationManager.getAllQueryConfigurations().entrySet()) {
            String queryName = entry.getKey();
            QueryConfig query = entry.getValue();

            if (query.getDatabase() != null && !configurationManager.hasDatabase(query.getDatabase())) {
                errors.add("Query '" + queryName + "' references non-existent database: " + query.getDatabase());
            }
        }
    }
}

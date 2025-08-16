package dev.cordal.generic;

import dev.cordal.common.exception.ApiException;
import dev.cordal.generic.config.ApiEndpointConfig;
import dev.cordal.generic.config.DatabaseConfig;
import dev.cordal.generic.config.EndpointConfigurationManager;
import dev.cordal.generic.config.QueryConfig;
import dev.cordal.generic.database.DatabaseConnectionManager;
import dev.cordal.generic.dto.RequestParameters;
import dev.cordal.generic.dto.ResponseMetadata;
import dev.cordal.generic.dto.*;
import dev.cordal.generic.model.GenericResponse;
import dev.cordal.generic.model.QueryParameter;
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
    private final DatabaseConnectionManager databaseConnectionManager;
    private final Executor asyncExecutor;

    @Inject
    public GenericApiService(GenericRepository genericRepository,
                           EndpointConfigurationManager configurationManager,
                           DatabaseConnectionManager databaseConnectionManager) {
        this.genericRepository = genericRepository;
        this.configurationManager = configurationManager;
        this.databaseConnectionManager = databaseConnectionManager;
        this.asyncExecutor = ForkJoinPool.commonPool();
    }
    
    /**
     * Execute endpoint request synchronously with type-safe parameters
     */
    public GenericResponse executeEndpoint(String endpointName, RequestParameters requestParameters) {
        return executeEndpoint(endpointName, requestParameters.asMap());
    }

    /**
     * Execute endpoint request synchronously (DEPRECATED - use RequestParameters version)
     * @deprecated Use executeEndpoint(String, RequestParameters) for type safety
     */
    @Deprecated
    public GenericResponse executeEndpoint(String endpointName, Map<String, Object> requestParameters) {
        logger.debug("Executing endpoint: {} with parameters: {}", endpointName, requestParameters);

        // Get endpoint configuration
        ApiEndpointConfig endpointConfig = configurationManager.getEndpointConfig(endpointName)
                .orElseThrow(() -> ApiException.notFound("Endpoint not found: " + endpointName));

        // Get query configuration
        QueryConfig queryConfig = configurationManager.getQueryConfig(endpointConfig.getQuery())
                .orElseThrow(() -> ApiException.internalError("Query not found: " + endpointConfig.getQuery()));

        // Check if the database for this endpoint is available
        String databaseName = queryConfig.getDatabase();
        if (!databaseConnectionManager.isDatabaseAvailable(databaseName)) {
            String failureReason = databaseConnectionManager.getDatabaseFailureReason(databaseName);
            logger.warn("Endpoint '{}' is unavailable because database '{}' is not available: {}",
                       endpointName, databaseName, failureReason);
            throw ApiException.serviceUnavailable(
                "Endpoint '" + endpointName + "' is temporarily unavailable due to database connectivity issues. " +
                "Database '" + databaseName + "' is not accessible: " + failureReason
            );
        }

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
     * Execute endpoint request asynchronously with type-safe parameters
     */
    public CompletableFuture<GenericResponse> executeEndpointAsync(String endpointName,
                                                                  RequestParameters requestParameters) {
        return executeEndpointAsync(endpointName, requestParameters.asMap());
    }

    /**
     * Execute endpoint request asynchronously (DEPRECATED - use RequestParameters version)
     * @deprecated Use executeEndpointAsync(String, RequestParameters) for type safety
     */
    @Deprecated
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
            throw ApiException.badRequest("VALIDATION ERROR: Page number cannot be negative (received: " + page + ")");
        }

        if (size <= 0) {
            throw ApiException.badRequest("VALIDATION ERROR: Page size must be positive (received: " + size + ")");
        }

        if (size > maxSize) {
            throw ApiException.badRequest("VALIDATION ERROR: Page size cannot exceed " + maxSize + " (received: " + size + ")");
        }
    }
    
    /**
     * Get all available endpoints (only those with available databases)
     */
    public Map<String, ApiEndpointConfig> getAvailableEndpoints() {
        Map<String, ApiEndpointConfig> allEndpoints = configurationManager.getAllEndpointConfigurations();
        Map<String, ApiEndpointConfig> availableEndpoints = new HashMap<>();

        for (Map.Entry<String, ApiEndpointConfig> entry : allEndpoints.entrySet()) {
            String endpointName = entry.getKey();
            ApiEndpointConfig endpointConfig = entry.getValue();

            // Check if the endpoint's database is available
            if (isEndpointAvailable(endpointName)) {
                availableEndpoints.put(endpointName, endpointConfig);
            }
        }

        return availableEndpoints;
    }

    /**
     * Get all endpoints including unavailable ones
     */
    public Map<String, ApiEndpointConfig> getAllEndpoints() {
        return configurationManager.getAllEndpointConfigurations();
    }

    /**
     * Get unavailable endpoints (those with unavailable databases)
     */
    public Map<String, String> getUnavailableEndpoints() {
        Map<String, ApiEndpointConfig> allEndpoints = configurationManager.getAllEndpointConfigurations();
        Map<String, String> unavailableEndpoints = new HashMap<>();

        for (Map.Entry<String, ApiEndpointConfig> entry : allEndpoints.entrySet()) {
            String endpointName = entry.getKey();
            ApiEndpointConfig endpointConfig = entry.getValue();

            // Get the database name for this endpoint
            Optional<QueryConfig> queryConfig = configurationManager.getQueryConfig(endpointConfig.getQuery());
            if (queryConfig.isPresent()) {
                String databaseName = queryConfig.get().getDatabase();
                if (!databaseConnectionManager.isDatabaseAvailable(databaseName)) {
                    String reason = databaseConnectionManager.getDatabaseFailureReason(databaseName);
                    unavailableEndpoints.put(endpointName, "Database '" + databaseName + "' unavailable: " + reason);
                }
            }
        }

        return unavailableEndpoints;
    }

    /**
     * Check if a specific endpoint is available
     */
    public boolean isEndpointAvailable(String endpointName) {
        Optional<ApiEndpointConfig> endpointConfig = configurationManager.getEndpointConfig(endpointName);
        if (!endpointConfig.isPresent()) {
            return false;
        }

        Optional<QueryConfig> queryConfig = configurationManager.getQueryConfig(endpointConfig.get().getQuery());
        if (!queryConfig.isPresent()) {
            return false;
        }

        String databaseName = queryConfig.get().getDatabase();
        return databaseConnectionManager.isDatabaseAvailable(databaseName);
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
     * Get endpoint configuration schema (field names and data types) with type-safe response
     */
    public ConfigurationSchemaResponse getEndpointConfigurationSchema() {
        List<ConfigurationSchemaResponse.SchemaField> fields = List.of(
            new ConfigurationSchemaResponse.SchemaField("path", "String", true, "API endpoint path"),
            new ConfigurationSchemaResponse.SchemaField("method", "String", true, "HTTP method (GET, POST, etc.)"),
            new ConfigurationSchemaResponse.SchemaField("description", "String", false, "Endpoint description"),
            new ConfigurationSchemaResponse.SchemaField("query", "String", true, "Reference to query configuration"),
            new ConfigurationSchemaResponse.SchemaField("countQuery", "String", false, "Reference to count query for pagination"),
            new ConfigurationSchemaResponse.SchemaField("pagination", "PaginationConfig", false, "Pagination configuration"),
            new ConfigurationSchemaResponse.SchemaField("parameters", "List<EndpointParameter>", false, "Endpoint parameters"),
            new ConfigurationSchemaResponse.SchemaField("response", "ResponseConfig", false, "Response configuration")
        );

        return ConfigurationSchemaResponse.of("endpoints", fields);
    }

    /**
     * Get endpoint configuration schema (DEPRECATED - use type-safe version)
     * @deprecated Use getEndpointConfigurationSchema() for type safety
     */
    @Deprecated
    public Map<String, Object> getEndpointConfigurationSchemaMap() {
        return getEndpointConfigurationSchema().toMap();
    }

    /**
     * Get only parameters from all endpoint configurations with type-safe response
     */
    public ConfigurationParametersResponse<ApiEndpointConfig.EndpointParameter> getEndpointParameters() {
        Map<String, List<ApiEndpointConfig.EndpointParameter>> parameters = new HashMap<>();

        for (Map.Entry<String, ApiEndpointConfig> entry : configurationManager.getAllEndpointConfigurations().entrySet()) {
            String endpointName = entry.getKey();
            ApiEndpointConfig config = entry.getValue();

            if (config.getParameters() != null && !config.getParameters().isEmpty()) {
                parameters.put(endpointName, config.getParameters());
            }
        }

        return ConfigurationParametersResponse.of("endpoints", parameters,
                                                 configurationManager.getAllEndpointConfigurations().size());
    }

    /**
     * Get only parameters from all endpoint configurations (DEPRECATED - use type-safe version)
     * @deprecated Use getEndpointParameters() for type safety
     */
    @Deprecated
    public Map<String, Object> getEndpointParametersMap() {
        return getEndpointParameters().toMap();
    }

    /**
     * Get database connections referenced by endpoints (via queries) with type-safe response
     */
    public DatabaseConnectionsResponse getEndpointDatabaseConnections() {
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

        return DatabaseConnectionsResponse.of("endpoints", endpointToDatabases, referencedDatabases,
                                             configurationManager.getAllEndpointConfigurations().size());
    }

    /**
     * Get database connections referenced by endpoints (DEPRECATED - use type-safe version)
     * @deprecated Use getEndpointDatabaseConnections() for type safety
     */
    @Deprecated
    public Map<String, Object> getEndpointDatabaseConnectionsMap() {
        return getEndpointDatabaseConnections().toMap();
    }

    /**
     * Get endpoint configuration summary with type-safe response
     */
    public ConfigurationSummaryResponse getEndpointConfigurationSummary() {
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

        return ConfigurationSummaryResponse.forEndpoints(endpoints.size(), withParameters, withPagination,
                                                        byMethod, referencedQueries, referencedDatabases);
    }

    /**
     * Get endpoint configuration summary (DEPRECATED - use type-safe version)
     * @deprecated Use getEndpointConfigurationSummary() for type safety
     */
    @Deprecated
    public Map<String, Object> getEndpointConfigurationSummaryMap() {
        return getEndpointConfigurationSummary().toMap();
    }

    /**
     * Get query configuration schema (field names and data types) with type-safe response
     */
    public ConfigurationSchemaResponse getQueryConfigurationSchema() {
        List<ConfigurationSchemaResponse.SchemaField> fields = List.of(
            new ConfigurationSchemaResponse.SchemaField("name", "String", true, "Query name"),
            new ConfigurationSchemaResponse.SchemaField("description", "String", false, "Query description"),
            new ConfigurationSchemaResponse.SchemaField("sql", "String", true, "SQL query statement"),
            new ConfigurationSchemaResponse.SchemaField("database", "String", true, "Reference to database configuration"),
            new ConfigurationSchemaResponse.SchemaField("parameters", "List<QueryParameter>", false, "Query parameters")
        );

        return ConfigurationSchemaResponse.of("queries", fields);
    }

    /**
     * Get query configuration schema (DEPRECATED - use type-safe version)
     * @deprecated Use getQueryConfigurationSchema() for type safety
     */
    @Deprecated
    public Map<String, Object> getQueryConfigurationSchemaMap() {
        return getQueryConfigurationSchema().toMap();
    }

    /**
     * Get only parameters from all query configurations with type-safe response
     */
    public ConfigurationParametersResponse<QueryConfig.QueryParameter> getQueryParameters() {
        Map<String, List<QueryConfig.QueryParameter>> parameters = new HashMap<>();

        for (Map.Entry<String, QueryConfig> entry : configurationManager.getAllQueryConfigurations().entrySet()) {
            String queryName = entry.getKey();
            QueryConfig config = entry.getValue();

            if (config.getParameters() != null && !config.getParameters().isEmpty()) {
                parameters.put(queryName, config.getParameters());
            }
        }

        return ConfigurationParametersResponse.of("queries", parameters,
                                                 configurationManager.getAllQueryConfigurations().size());
    }

    /**
     * Get only parameters from all query configurations (DEPRECATED - use type-safe version)
     * @deprecated Use getQueryParameters() for type safety
     */
    @Deprecated
    public Map<String, Object> getQueryParametersMap() {
        return getQueryParameters().toMap();
    }

    /**
     * Get database connections referenced by queries with type-safe response
     */
    public DatabaseConnectionsResponse getQueryDatabaseConnections() {
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

        return DatabaseConnectionsResponse.of("queries", queryToDatabases, referencedDatabases,
                                             configurationManager.getAllQueryConfigurations().size());
    }

    /**
     * Get database connections referenced by queries (DEPRECATED - use type-safe version)
     * @deprecated Use getQueryDatabaseConnections() for type safety
     */
    @Deprecated
    public Map<String, Object> getQueryDatabaseConnectionsMap() {
        return getQueryDatabaseConnections().toMap();
    }

    /**
     * Get query configuration summary with type-safe response
     */
    public ConfigurationSummaryResponse getQueryConfigurationSummary() {
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

        return ConfigurationSummaryResponse.forQueries(queries.size(), withParameters,
                                                      referencedDatabases, parameterCounts);
    }

    /**
     * Get query configuration summary (DEPRECATED - use type-safe version)
     * @deprecated Use getQueryConfigurationSummary() for type safety
     */
    @Deprecated
    public Map<String, Object> getQueryConfigurationSummaryMap() {
        return getQueryConfigurationSummary().toMap();
    }

    /**
     * Get database configuration schema (field names and data types) with type-safe response
     */
    public ConfigurationSchemaResponse getDatabaseConfigurationSchema() {
        List<ConfigurationSchemaResponse.SchemaField> fields = List.of(
            new ConfigurationSchemaResponse.SchemaField("name", "String", true, "Database name"),
            new ConfigurationSchemaResponse.SchemaField("description", "String", false, "Database description"),
            new ConfigurationSchemaResponse.SchemaField("url", "String", true, "Database connection URL"),
            new ConfigurationSchemaResponse.SchemaField("username", "String", true, "Database username"),
            new ConfigurationSchemaResponse.SchemaField("password", "String", true, "Database password"),
            new ConfigurationSchemaResponse.SchemaField("driver", "String", true, "Database driver class"),
            new ConfigurationSchemaResponse.SchemaField("pool", "PoolConfig", false, "Connection pool configuration")
        );

        return ConfigurationSchemaResponse.of("databases", fields);
    }

    /**
     * Get database configuration schema (DEPRECATED - use type-safe version)
     * @deprecated Use getDatabaseConfigurationSchema() for type safety
     */
    @Deprecated
    public Map<String, Object> getDatabaseConfigurationSchemaMap() {
        return getDatabaseConfigurationSchema().toMap();
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
     * Validate all configurations and return validation results with type-safe response
     */
    public ConfigurationValidationResponse validateConfigurations() {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try {
            // This will throw an exception if validation fails
            configurationManager.validateConfigurations();
        } catch (Exception e) {
            errors.add(e.getMessage());
        }

        // Additional detailed validation
        validateEndpointConfigurations(errors, warnings);
        validateQueryConfigurations(errors, warnings);
        validateDatabaseConfigurations(errors, warnings);
        validateRelationships(errors, warnings);

        if (errors.isEmpty()) {
            return warnings.isEmpty() ? ConfigurationValidationResponse.valid("All configurations are valid")
                                     : ConfigurationValidationResponse.validWithWarnings("All configurations are valid with warnings", warnings);
        } else {
            return ConfigurationValidationResponse.invalid("Configuration validation failed", errors);
        }
    }

    /**
     * Validate all configurations and return validation results (DEPRECATED - use type-safe version)
     * @deprecated Use validateConfigurations() for type safety
     */
    @Deprecated
    public Map<String, Object> validateConfigurationsMap() {
        return validateConfigurations().toMap();
    }

    /**
     * Validate endpoint configurations specifically with type-safe response
     */
    public ConfigurationValidationResponse validateEndpointConfigurations() {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        validateEndpointConfigurations(errors, warnings);

        return ConfigurationValidationResponse.forConfigType("endpoints",
                                                            configurationManager.getAllEndpointConfigurations().size(),
                                                            errors, warnings);
    }

    /**
     * Validate endpoint configurations specifically (DEPRECATED - use type-safe version)
     * @deprecated Use validateEndpointConfigurations() for type safety
     */
    @Deprecated
    public Map<String, Object> validateEndpointConfigurationsMap() {
        return validateEndpointConfigurations().toMap();
    }

    /**
     * Validate query configurations specifically with type-safe response
     */
    public ConfigurationValidationResponse validateQueryConfigurations() {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        validateQueryConfigurations(errors, warnings);

        return ConfigurationValidationResponse.forConfigType("queries",
                                                            configurationManager.getAllQueryConfigurations().size(),
                                                            errors, warnings);
    }

    /**
     * Validate query configurations specifically (DEPRECATED - use type-safe version)
     * @deprecated Use validateQueryConfigurations() for type safety
     */
    @Deprecated
    public Map<String, Object> validateQueryConfigurationsMap() {
        return validateQueryConfigurations().toMap();
    }

    /**
     * Validate database configurations specifically with type-safe response
     */
    public ConfigurationValidationResponse validateDatabaseConfigurations() {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        validateDatabaseConfigurations(errors, warnings);

        return ConfigurationValidationResponse.forConfigType("databases",
                                                            configurationManager.getAllDatabaseConfigurations().size(),
                                                            errors, warnings);
    }

    /**
     * Validate database configurations specifically (DEPRECATED - use type-safe version)
     * @deprecated Use validateDatabaseConfigurations() for type safety
     */
    @Deprecated
    public Map<String, Object> validateDatabaseConfigurationsMap() {
        return validateDatabaseConfigurations().toMap();
    }

    /**
     * Validate endpoint connectivity by making HTTP requests
     */
    public Map<String, Object> validateEndpointConnectivity() {
        Map<String, Object> validationResults = new HashMap<>();

        try {
            // Create configuration validator using existing dependencies
            dev.cordal.util.ConfigurationValidator configurationValidator =
                new dev.cordal.util.ConfigurationValidator(configurationManager, databaseConnectionManager);

            // Use default base URL (we'll need to get this from configuration or use localhost:8080)
            String baseUrl = "http://localhost:8080";

            // Validate endpoint connectivity
            dev.cordal.util.ValidationResult result = configurationValidator.validateEndpointConnectivity(baseUrl);

            validationResults.put("status", result.isSuccess() ? "VALID" : "INVALID");
            validationResults.put("errors", result.getErrors());
            validationResults.put("successes", result.getSuccesses());
            validationResults.put("errorCount", result.getErrorCount());
            validationResults.put("successCount", result.getSuccessCount());
            validationResults.put("totalEndpoints", configurationManager.getAllEndpointConfigurations().size());
            validationResults.put("baseUrl", baseUrl);
            validationResults.put("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            validationResults.put("status", "ERROR");
            validationResults.put("error", "Failed to validate endpoint connectivity: " + e.getMessage());
            validationResults.put("timestamp", System.currentTimeMillis());
        }

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

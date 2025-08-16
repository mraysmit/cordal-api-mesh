package dev.cordal.generic;

import dev.cordal.common.cache.CacheKeyBuilder;
import dev.cordal.common.cache.CacheManager;
import dev.cordal.common.exception.ApiException;
import dev.cordal.common.metrics.CacheMetricsCollector;
import dev.cordal.generic.cache.QueryResultCache;
import dev.cordal.generic.config.QueryConfig;
import dev.cordal.generic.database.DatabaseConnectionManager;
import dev.cordal.generic.dto.QueryResult;
import dev.cordal.generic.model.QueryParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generic repository for executing configured SQL queries
 */
@Singleton
public class GenericRepository {
    private static final Logger logger = LoggerFactory.getLogger(GenericRepository.class);
    private static final String QUERY_RESULTS_CACHE = "query_results";
    private static final String COUNT_RESULTS_CACHE = "count_results";

    private final DatabaseConnectionManager databaseConnectionManager;
    private final CacheManager cacheManager;
    private final CacheMetricsCollector cacheMetricsCollector;
    private final QueryResultCache queryResultCache;

    @Inject
    public GenericRepository(DatabaseConnectionManager databaseConnectionManager,
                           CacheManager cacheManager,
                           CacheMetricsCollector cacheMetricsCollector,
                           QueryResultCache queryResultCache) {
        this.databaseConnectionManager = databaseConnectionManager;
        this.cacheManager = cacheManager;
        this.cacheMetricsCollector = cacheMetricsCollector;
        this.queryResultCache = queryResultCache;
        logger.info("Generic repository initialized with caching and metrics support");
    }
    
    /**
     * Execute a query and return results as type-safe QueryResult objects
     */
    public List<QueryResult> executeQuerySafe(QueryConfig queryConfig, List<QueryParameter> parameters) {
        List<Map<String, Object>> rawResults = executeQuery(queryConfig, parameters);
        return rawResults.stream()
            .map(QueryResult::new)
            .collect(Collectors.toList());
    }

    /**
     * Execute a query and return results as list of maps (DEPRECATED - use executeQuerySafe)
     * @deprecated Use executeQuerySafe() for type safety
     */
    @Deprecated
    public List<Map<String, Object>> executeQuery(QueryConfig queryConfig, List<QueryParameter> parameters) {
        logger.debug("Executing query: {} with {} parameters on database: {}",
                    queryConfig.getName(), parameters.size(), queryConfig.getDatabase());

        // Check cache if enabled
        if (queryConfig.isCacheEnabled()) {
            String cacheKey = buildCacheKey(queryConfig, parameters);
            long cacheStartTime = System.currentTimeMillis();
            Optional<List<Map<String, Object>>> cachedResult = queryResultCache.get(QUERY_RESULTS_CACHE, cacheKey);

            if (cachedResult.isPresent()) {
                long cacheResponseTime = System.currentTimeMillis() - cacheStartTime;
                cacheMetricsCollector.recordCacheHit(queryConfig.getName(), QUERY_RESULTS_CACHE, cacheKey, cacheResponseTime);
                logger.debug("Cache hit for query: {} with key: {} in {}ms", queryConfig.getName(), cacheKey, cacheResponseTime);
                return cachedResult.get(); // Type-safe, no cast needed!
            }

            logger.debug("Cache miss for query: {} with key: {}", queryConfig.getName(), cacheKey);
        }

        // Execute query against database
        long dbStartTime = System.currentTimeMillis();
        List<Map<String, Object>> results = executeQueryDirect(queryConfig, parameters);
        long dbResponseTime = System.currentTimeMillis() - dbStartTime;

        // Store in cache if enabled and record cache miss
        if (queryConfig.isCacheEnabled()) {
            String cacheKey = buildCacheKey(queryConfig, parameters);
            long ttlMs = Duration.ofSeconds(queryConfig.getCache().getTtl()).toMillis();
            queryResultCache.put(QUERY_RESULTS_CACHE, cacheKey, results, ttlMs);
            cacheMetricsCollector.recordCacheMiss(queryConfig.getName(), QUERY_RESULTS_CACHE, cacheKey, dbResponseTime);
            logger.debug("Cached query result: {} with key: {} and TTL: {}s, DB response time: {}ms",
                        queryConfig.getName(), cacheKey, queryConfig.getCache().getTtl(), dbResponseTime);
        }

        return results;
    }

    /**
     * Execute a query directly against the database (bypassing cache)
     */
    private List<Map<String, Object>> executeQueryDirect(QueryConfig queryConfig, List<QueryParameter> parameters) {
        String sql = queryConfig.getSql();
        String databaseName = queryConfig.getDatabase();
        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection connection = databaseConnectionManager.getConnection(databaseName);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // Set parameters
            setParameters(statement, parameters);

            // Execute query
            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (resultSet.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();

                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnLabel(i);
                        Object value = resultSet.getObject(i);
                        row.put(columnName, value);
                    }

                    results.add(row);
                }
            }

            logger.debug("Query executed successfully, returned {} rows", results.size());
            return results;

        } catch (SQLException e) {
            logger.error("Failed to execute query: {}", queryConfig.getName(), e);
            throw ApiException.internalError("Failed to execute query: " + queryConfig.getName(), e);
        }
    }
    
    /**
     * Execute a count query and return the count value
     */
    public long executeCountQuery(QueryConfig queryConfig, List<QueryParameter> parameters) {
        logger.debug("Executing count query: {} with {} parameters on database: {}",
                    queryConfig.getName(), parameters.size(), queryConfig.getDatabase());

        // Check cache if enabled
        if (queryConfig.isCacheEnabled()) {
            String cacheKey = buildCacheKey(queryConfig, parameters);
            long cacheStartTime = System.currentTimeMillis();
            Optional<Long> cachedResult = cacheManager.get(COUNT_RESULTS_CACHE, cacheKey, Long.class);

            if (cachedResult.isPresent()) {
                long cacheResponseTime = System.currentTimeMillis() - cacheStartTime;
                cacheMetricsCollector.recordCacheHit(queryConfig.getName(), COUNT_RESULTS_CACHE, cacheKey, cacheResponseTime);
                logger.debug("Cache hit for count query: {} with key: {} in {}ms", queryConfig.getName(), cacheKey, cacheResponseTime);
                return cachedResult.get();
            }

            logger.debug("Cache miss for count query: {} with key: {}", queryConfig.getName(), cacheKey);
        }

        // Execute count query against database
        long dbStartTime = System.currentTimeMillis();
        long count = executeCountQueryDirect(queryConfig, parameters);
        long dbResponseTime = System.currentTimeMillis() - dbStartTime;

        // Store in cache if enabled and record cache miss
        if (queryConfig.isCacheEnabled()) {
            String cacheKey = buildCacheKey(queryConfig, parameters);
            Duration ttl = Duration.ofSeconds(queryConfig.getCache().getTtl());
            cacheManager.put(COUNT_RESULTS_CACHE, cacheKey, count, ttl);
            cacheMetricsCollector.recordCacheMiss(queryConfig.getName(), COUNT_RESULTS_CACHE, cacheKey, dbResponseTime);
            logger.debug("Cached count query result: {} with key: {} and TTL: {}s, DB response time: {}ms",
                        queryConfig.getName(), cacheKey, queryConfig.getCache().getTtl(), dbResponseTime);
        }

        return count;
    }

    /**
     * Execute a count query directly against the database (bypassing cache)
     */
    private long executeCountQueryDirect(QueryConfig queryConfig, List<QueryParameter> parameters) {
        String sql = queryConfig.getSql();
        String databaseName = queryConfig.getDatabase();

        try (Connection connection = databaseConnectionManager.getConnection(databaseName);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // Set parameters
            setParameters(statement, parameters);

            // Execute query
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    long count = resultSet.getLong(1);
                    logger.debug("Count query executed successfully, returned count: {}", count);
                    return count;
                } else {
                    logger.warn("Count query returned no results");
                    return 0;
                }
            }

        } catch (SQLException e) {
            logger.error("Failed to execute count query: {}", queryConfig.getName(), e);
            throw ApiException.internalError("Failed to execute count query: " + queryConfig.getName(), e);
        }
    }
    
    /**
     * Execute a query that returns a single result
     */
    /**
     * Execute a query and return single result as type-safe QueryResult
     */
    public Optional<QueryResult> executeSingleQuerySafe(QueryConfig queryConfig, List<QueryParameter> parameters) {
        return executeSingleQuery(queryConfig, parameters)
            .map(QueryResult::new);
    }

    /**
     * Execute a query and return single result as map (DEPRECATED - use executeSingleQuerySafe)
     * @deprecated Use executeSingleQuerySafe() for type safety
     */
    @Deprecated
    public Optional<Map<String, Object>> executeSingleQuery(QueryConfig queryConfig, List<QueryParameter> parameters) {
        logger.debug("Executing single query: {} with {} parameters", queryConfig.getName(), parameters.size());

        List<Map<String, Object>> results = executeQuery(queryConfig, parameters);

        if (results.isEmpty()) {
            logger.debug("Single query returned no results");
            return Optional.empty();
        }

        if (results.size() > 1) {
            logger.warn("Single query returned {} results, using first one", results.size());
        }

        return Optional.of(results.get(0));
    }

    /**
     * Build a cache key for the given query and parameters
     */
    private String buildCacheKey(QueryConfig queryConfig, List<QueryParameter> parameters) {
        // Convert QueryParameter list to Map for CacheKeyBuilder
        Map<String, Object> parameterMap = parameters.stream()
            .collect(Collectors.toMap(
                QueryParameter::getName,
                QueryParameter::getValue,
                (existing, replacement) -> replacement // Handle duplicate keys
            ));

        String keyPattern = queryConfig.getCache().getKeyPattern();
        return CacheKeyBuilder.buildKey(queryConfig.getName(), keyPattern, parameterMap);
    }
    
    /**
     * Set parameters on prepared statement
     */
    private void setParameters(PreparedStatement statement, List<QueryParameter> parameters) throws SQLException {
        // Sort parameters by position to ensure correct order
        parameters.sort(Comparator.comparingInt(QueryParameter::getPosition));
        
        for (QueryParameter param : parameters) {
            Object typedValue = param.getTypedValue();
            int position = param.getPosition();
            
            logger.debug("Setting parameter {} at position {}: {} ({})", 
                        param.getName(), position, typedValue, param.getType());
            
            if (typedValue == null) {
                statement.setNull(position, getSqlType(param.getType()));
            } else {
                setTypedParameter(statement, position, typedValue, param.getType());
            }
        }
    }
    
    /**
     * Set typed parameter on prepared statement
     */
    private void setTypedParameter(PreparedStatement statement, int position, Object value, String type) 
            throws SQLException {
        
        switch (type.toUpperCase()) {
            case "STRING":
                statement.setString(position, (String) value);
                break;
            case "INTEGER":
                statement.setInt(position, (Integer) value);
                break;
            case "LONG":
                statement.setLong(position, (Long) value);
                break;
            case "DECIMAL":
                statement.setBigDecimal(position, (java.math.BigDecimal) value);
                break;
            case "BOOLEAN":
                statement.setBoolean(position, (Boolean) value);
                break;
            case "TIMESTAMP":
                statement.setTimestamp(position, (Timestamp) value);
                break;
            default:
                // Default to string
                statement.setString(position, value.toString());
                break;
        }
    }
    
    /**
     * Get SQL type constant for null values
     */
    private int getSqlType(String type) {
        switch (type.toUpperCase()) {
            case "STRING":
                return Types.VARCHAR;
            case "INTEGER":
                return Types.INTEGER;
            case "LONG":
                return Types.BIGINT;
            case "DECIMAL":
                return Types.DECIMAL;
            case "BOOLEAN":
                return Types.BOOLEAN;
            case "TIMESTAMP":
                return Types.TIMESTAMP;
            default:
                return Types.VARCHAR;
        }
    }
}

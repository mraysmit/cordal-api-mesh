package dev.mars.generic;

import dev.mars.common.exception.ApiException;
import dev.mars.generic.config.QueryConfig;
import dev.mars.generic.database.DatabaseConnectionManager;
import dev.mars.generic.model.QueryParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.*;
import java.util.*;

/**
 * Generic repository for executing configured SQL queries
 */
@Singleton
public class GenericRepository {
    private static final Logger logger = LoggerFactory.getLogger(GenericRepository.class);

    private final DatabaseConnectionManager databaseConnectionManager;
    
    @Inject
    public GenericRepository(DatabaseConnectionManager databaseConnectionManager) {
        this.databaseConnectionManager = databaseConnectionManager;
        logger.info("Generic repository initialized");
    }
    
    /**
     * Execute a query and return results as list of maps
     */
    public List<Map<String, Object>> executeQuery(QueryConfig queryConfig, List<QueryParameter> parameters) {
        logger.debug("Executing query: {} with {} parameters on database: {}",
                    queryConfig.getName(), parameters.size(), queryConfig.getDatabase());

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

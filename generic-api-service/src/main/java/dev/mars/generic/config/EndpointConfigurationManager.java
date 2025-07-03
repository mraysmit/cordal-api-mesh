package dev.mars.generic.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Optional;

/**
 * Manages API endpoint and query configurations
 */
@Singleton
public class EndpointConfigurationManager {
    private static final Logger logger = LoggerFactory.getLogger(EndpointConfigurationManager.class);

    private final Map<String, QueryConfig> queryConfigurations;
    private final Map<String, ApiEndpointConfig> endpointConfigurations;
    private final Map<String, DatabaseConfig> databaseConfigurations;
    
    @Inject
    public EndpointConfigurationManager(ConfigurationLoader configurationLoader) {
        logger.info("Initializing endpoint configuration manager");

        this.databaseConfigurations = configurationLoader.loadDatabaseConfigurations();
        this.queryConfigurations = configurationLoader.loadQueryConfigurations();
        this.endpointConfigurations = configurationLoader.loadEndpointConfigurations();

        logger.info("Configuration manager initialized with {} databases, {} queries and {} endpoints",
                   databaseConfigurations.size(), queryConfigurations.size(), endpointConfigurations.size());

        // Note: We don't add default database configurations anymore
        // The configuration database is separate and managed by DatabaseManager
        // API endpoint databases should be explicitly configured in YAML files

        // Validate configurations
        validateConfigurations();
    }
    
    /**
     * Get query configuration by name
     */
    public Optional<QueryConfig> getQueryConfig(String queryName) {
        QueryConfig config = queryConfigurations.get(queryName);
        if (config == null) {
            logger.warn("Query configuration not found: {}", queryName);
            return Optional.empty();
        }
        return Optional.of(config);
    }
    
    /**
     * Get endpoint configuration by name
     */
    public Optional<ApiEndpointConfig> getEndpointConfig(String endpointName) {
        ApiEndpointConfig config = endpointConfigurations.get(endpointName);
        if (config == null) {
            logger.warn("Endpoint configuration not found: {}", endpointName);
            return Optional.empty();
        }
        return Optional.of(config);
    }
    
    /**
     * Get all query configurations
     */
    public Map<String, QueryConfig> getAllQueryConfigurations() {
        return queryConfigurations;
    }
    
    /**
     * Get all endpoint configurations
     */
    public Map<String, ApiEndpointConfig> getAllEndpointConfigurations() {
        return endpointConfigurations;
    }

    /**
     * Get database configuration by name
     */
    public Optional<DatabaseConfig> getDatabaseConfig(String databaseName) {
        DatabaseConfig config = databaseConfigurations.get(databaseName);
        if (config == null) {
            logger.warn("Database configuration not found: {}", databaseName);
            return Optional.empty();
        }
        return Optional.of(config);
    }

    /**
     * Get all database configurations
     */
    public Map<String, DatabaseConfig> getAllDatabaseConfigurations() {
        return databaseConfigurations;
    }
    
    /**
     * Check if a query exists
     */
    public boolean hasQuery(String queryName) {
        return queryConfigurations.containsKey(queryName);
    }

    /**
     * Check if a database exists
     */
    public boolean hasDatabase(String databaseName) {
        return databaseConfigurations.containsKey(databaseName);
    }
    
    /**
     * Check if an endpoint exists
     */
    public boolean hasEndpoint(String endpointName) {
        return endpointConfigurations.containsKey(endpointName);
    }
    
    /**
     * Validate all configurations
     */
    public void validateConfigurations() {
        logger.info("Validating configurations...");

        int validationErrors = 0;

        // Validate query -> database references
        for (Map.Entry<String, QueryConfig> entry : queryConfigurations.entrySet()) {
            String queryName = entry.getKey();
            QueryConfig queryConfig = entry.getValue();

            // Check if database exists
            if (queryConfig.getDatabase() != null && !hasDatabase(queryConfig.getDatabase())) {
                logger.error("Query '{}' references non-existent database: {}",
                           queryName, queryConfig.getDatabase());
                validationErrors++;
            }

            // Validate that database is specified
            if (queryConfig.getDatabase() == null || queryConfig.getDatabase().trim().isEmpty()) {
                logger.error("Query '{}' does not specify a database", queryName);
                validationErrors++;
            }
        }

        // Validate endpoint -> query references
        for (Map.Entry<String, ApiEndpointConfig> entry : endpointConfigurations.entrySet()) {
            String endpointName = entry.getKey();
            ApiEndpointConfig endpointConfig = entry.getValue();

            // Check if main query exists
            if (endpointConfig.getQuery() != null && !hasQuery(endpointConfig.getQuery())) {
                logger.error("Endpoint '{}' references non-existent query: {}",
                           endpointName, endpointConfig.getQuery());
                validationErrors++;
            }

            // Check if count query exists (if specified)
            if (endpointConfig.getCountQuery() != null && !hasQuery(endpointConfig.getCountQuery())) {
                logger.error("Endpoint '{}' references non-existent count query: {}",
                           endpointName, endpointConfig.getCountQuery());
                validationErrors++;
            }

            // Validate pagination configuration
            if (endpointConfig.getPagination() != null && endpointConfig.getPagination().isEnabled()) {
                if (endpointConfig.getCountQuery() == null) {
                    logger.warn("Endpoint '{}' has pagination enabled but no count query specified",
                              endpointName);
                }
            }
        }

        // Validate database configurations
        for (Map.Entry<String, DatabaseConfig> entry : databaseConfigurations.entrySet()) {
            String databaseName = entry.getKey();
            DatabaseConfig databaseConfig = entry.getValue();

            // Validate required fields
            if (databaseConfig.getUrl() == null || databaseConfig.getUrl().trim().isEmpty()) {
                logger.error("Database '{}' does not specify a URL", databaseName);
                validationErrors++;
            }

            if (databaseConfig.getDriver() == null || databaseConfig.getDriver().trim().isEmpty()) {
                logger.error("Database '{}' does not specify a driver", databaseName);
                validationErrors++;
            }
        }

        if (validationErrors > 0) {
            throw new RuntimeException("Configuration validation failed with " + validationErrors + " errors");
        }

        logger.info("Configuration validation completed successfully");
    }


}

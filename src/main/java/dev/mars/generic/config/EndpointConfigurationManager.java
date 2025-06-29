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
    
    @Inject
    public EndpointConfigurationManager(ConfigurationLoader configurationLoader) {
        logger.info("Initializing endpoint configuration manager");
        
        this.queryConfigurations = configurationLoader.loadQueryConfigurations();
        this.endpointConfigurations = configurationLoader.loadEndpointConfigurations();
        
        logger.info("Configuration manager initialized with {} queries and {} endpoints", 
                   queryConfigurations.size(), endpointConfigurations.size());
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
     * Check if a query exists
     */
    public boolean hasQuery(String queryName) {
        return queryConfigurations.containsKey(queryName);
    }
    
    /**
     * Check if an endpoint exists
     */
    public boolean hasEndpoint(String endpointName) {
        return endpointConfigurations.containsKey(endpointName);
    }
    
    /**
     * Validate that all endpoint queries exist
     */
    public void validateConfigurations() {
        logger.info("Validating endpoint configurations");
        
        int validationErrors = 0;
        
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
        
        if (validationErrors > 0) {
            throw new RuntimeException("Configuration validation failed with " + validationErrors + " errors");
        }
        
        logger.info("Configuration validation completed successfully");
    }
}

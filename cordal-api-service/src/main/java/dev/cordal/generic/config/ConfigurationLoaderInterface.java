package dev.cordal.generic.config;

import java.util.Map;

/**
 * Interface for configuration loaders that can load configurations from different sources
 * (YAML files, database, etc.)
 */
public interface ConfigurationLoaderInterface {
    
    /**
     * Load database configurations from the configured source
     * @return Map of database configurations keyed by name
     */
    Map<String, DatabaseConfig> loadDatabaseConfigurations();
    
    /**
     * Load query configurations from the configured source
     * @return Map of query configurations keyed by name
     */
    Map<String, QueryConfig> loadQueryConfigurations();
    
    /**
     * Load endpoint configurations from the configured source
     * @return Map of endpoint configurations keyed by name
     */
    Map<String, ApiEndpointConfig> loadEndpointConfigurations();
}

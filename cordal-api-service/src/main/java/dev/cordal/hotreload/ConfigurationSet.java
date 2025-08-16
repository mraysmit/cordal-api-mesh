package dev.cordal.hotreload;

import dev.cordal.generic.config.ApiEndpointConfig;
import dev.cordal.generic.config.QueryConfig;
import dev.cordal.generic.config.DatabaseConfig;

import java.util.Map;

/**
 * Represents a complete set of configuration (databases, queries, endpoints)
 */
public class ConfigurationSet {
    private final Map<String, DatabaseConfig> databases;
    private final Map<String, QueryConfig> queries;
    private final Map<String, ApiEndpointConfig> endpoints;
    
    public ConfigurationSet() {
        this.databases = Map.of();
        this.queries = Map.of();
        this.endpoints = Map.of();
    }
    
    public ConfigurationSet(Map<String, DatabaseConfig> databases,
                           Map<String, QueryConfig> queries,
                           Map<String, ApiEndpointConfig> endpoints) {
        this.databases = Map.copyOf(databases);
        this.queries = Map.copyOf(queries);
        this.endpoints = Map.copyOf(endpoints);
    }
    
    public Map<String, DatabaseConfig> getDatabases() { return databases; }
    public Map<String, QueryConfig> getQueries() { return queries; }
    public Map<String, ApiEndpointConfig> getEndpoints() { return endpoints; }
    
    public int getTotalConfigurations() {
        return databases.size() + queries.size() + endpoints.size();
    }
    
    @Override
    public String toString() {
        return String.format("ConfigurationSet{databases=%d, queries=%d, endpoints=%d}",
                           databases.size(), queries.size(), endpoints.size());
    }
}

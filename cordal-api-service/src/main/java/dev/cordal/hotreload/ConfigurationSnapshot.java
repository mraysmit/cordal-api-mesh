package dev.cordal.hotreload;

import dev.cordal.generic.config.ApiEndpointConfig;
import dev.cordal.generic.config.QueryConfig;
import dev.cordal.generic.config.DatabaseConfig;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable snapshot of configuration state at a point in time
 */
public class ConfigurationSnapshot {
    private final String version;
    private final Instant timestamp;
    private final Map<String, DatabaseConfig> databases;
    private final Map<String, QueryConfig> queries;
    private final Map<String, ApiEndpointConfig> endpoints;
    
    public ConfigurationSnapshot(String version, Instant timestamp,
                               Map<String, DatabaseConfig> databases,
                               Map<String, QueryConfig> queries,
                               Map<String, ApiEndpointConfig> endpoints) {
        this.version = version;
        this.timestamp = timestamp;
        this.databases = Map.copyOf(databases);
        this.queries = Map.copyOf(queries);
        this.endpoints = Map.copyOf(endpoints);
    }
    
    public String getVersion() {
        return version;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public Map<String, DatabaseConfig> getDatabases() {
        return databases;
    }
    
    public Map<String, QueryConfig> getQueries() {
        return queries;
    }
    
    public Map<String, ApiEndpointConfig> getEndpoints() {
        return endpoints;
    }
    
    public int getTotalConfigurations() {
        return databases.size() + queries.size() + endpoints.size();
    }
    
    @Override
    public String toString() {
        return String.format("ConfigurationSnapshot{version='%s', timestamp='%s', databases=%d, queries=%d, endpoints=%d}",
                           version, timestamp, databases.size(), queries.size(), endpoints.size());
    }
}

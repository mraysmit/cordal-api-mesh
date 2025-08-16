package dev.cordal.generic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

/**
 * Type-safe response for configuration collection operations
 * Replaces Map<String, Object> for getAllDatabaseConfigurations, getAllQueryConfigurations, etc.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfigurationCollectionResponse<T> {
    
    @JsonProperty("count")
    private final int count;
    
    @JsonProperty("source")
    private final String source;
    
    @JsonProperty("timestamp")
    private final Instant timestamp;
    
    @JsonProperty("configurations")
    private final Map<String, T> configurations;
    
    @JsonProperty("database")
    private final String database; // For query-by-database responses
    
    @JsonProperty("query")
    private final String query; // For endpoint-by-query responses
    
    /**
     * Constructor for general configuration lists (databases, queries, endpoints)
     */
    public ConfigurationCollectionResponse(int count, String source, Instant timestamp, Map<String, T> configurations) {
        this.count = count;
        this.source = source;
        this.timestamp = timestamp;
        this.configurations = configurations;
        this.database = null;
        this.query = null;
    }
    
    /**
     * Constructor for query configurations filtered by database
     */
    public ConfigurationCollectionResponse(int count, String source, Instant timestamp,
                                   Map<String, T> configurations, String database) {
        this.count = count;
        this.source = source;
        this.timestamp = timestamp;
        this.configurations = configurations;
        this.database = database;
        this.query = null;
    }
    
    /**
     * Constructor for endpoint configurations filtered by query
     */
    public ConfigurationCollectionResponse(int count, String source, Instant timestamp,
                                   Map<String, T> configurations, String query, boolean isQuery) {
        this.count = count;
        this.source = source;
        this.timestamp = timestamp;
        this.configurations = configurations;
        this.database = null;
        this.query = isQuery ? query : null;
    }
    
    // Static factory methods for better readability
    public static <T> ConfigurationCollectionResponse<T> of(String source, Map<String, T> configurations) {
        return new ConfigurationCollectionResponse<>(
            configurations.size(),
            source,
            Instant.now(),
            configurations
        );
    }

    public static <T> ConfigurationCollectionResponse<T> forDatabase(String source, Map<String, T> configurations, String database) {
        return new ConfigurationCollectionResponse<>(
            configurations.size(),
            source,
            Instant.now(),
            configurations,
            database
        );
    }

    public static <T> ConfigurationCollectionResponse<T> forQuery(String source, Map<String, T> configurations, String query) {
        return new ConfigurationCollectionResponse<>(
            configurations.size(),
            source,
            Instant.now(),
            configurations,
            query,
            true
        );
    }
    
    // Getters
    public int getCount() {
        return count;
    }
    
    public String getSource() {
        return source;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public Map<String, T> getConfigurations() {
        return configurations;
    }
    
    public String getDatabase() {
        return database;
    }
    
    public String getQuery() {
        return query;
    }
    
    /**
     * Convert to Map<String, Object> for backward compatibility
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("count", count);
        map.put("source", source);
        map.put("timestamp", timestamp);
        
        if (database != null) {
            map.put("database", database);
            map.put("queries", configurations);
        } else if (query != null) {
            map.put("query", query);
            map.put("endpoints", configurations);
        } else {
            // Determine the type based on generic parameter (this is a bit hacky but works)
            String configType = determineConfigurationType();
            map.put(configType, configurations);
        }
        
        return map;
    }
    
    private String determineConfigurationType() {
        if (configurations.isEmpty()) {
            return "configurations";
        }
        
        // Try to determine type from first configuration
        Object firstConfig = configurations.values().iterator().next();
        String className = firstConfig.getClass().getSimpleName();
        
        if (className.contains("Database")) {
            return "databases";
        } else if (className.contains("Query")) {
            return "queries";
        } else if (className.contains("Endpoint") || className.contains("Api")) {
            return "endpoints";
        } else {
            return "configurations";
        }
    }
    
    @Override
    public String toString() {
        return "ConfigurationCollectionResponse{" +
                "count=" + count +
                ", source='" + source + '\'' +
                ", timestamp=" + timestamp +
                ", database='" + database + '\'' +
                ", query='" + query + '\'' +
                ", configurations=" + configurations.size() + " items" +
                '}';
    }
}

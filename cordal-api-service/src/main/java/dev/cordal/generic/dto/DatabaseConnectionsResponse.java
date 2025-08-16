package dev.cordal.generic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Type-safe response for database connections information
 * Replaces Map<String, Object> for database connection methods (getEndpointDatabaseConnections, etc.)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatabaseConnectionsResponse {
    
    @JsonProperty("configType")
    private final String configType;
    
    @JsonProperty("connections")
    private final Map<String, String> connections; // configuration name -> database name
    
    @JsonProperty("referencedDatabases")
    private final Set<String> referencedDatabases;
    
    @JsonProperty("totalConfigurations")
    private final int totalConfigurations;
    
    @JsonProperty("configurationsWithDatabases")
    private final int configurationsWithDatabases;
    
    @JsonProperty("timestamp")
    private final Instant timestamp;
    
    /**
     * Constructor
     */
    public DatabaseConnectionsResponse(String configType, Map<String, String> connections,
                                     Set<String> referencedDatabases, int totalConfigurations,
                                     int configurationsWithDatabases, Instant timestamp) {
        this.configType = configType;
        this.connections = connections;
        this.referencedDatabases = referencedDatabases;
        this.totalConfigurations = totalConfigurations;
        this.configurationsWithDatabases = configurationsWithDatabases;
        this.timestamp = timestamp;
    }
    
    /**
     * Static factory method
     */
    public static DatabaseConnectionsResponse of(String configType, Map<String, String> connections,
                                               Set<String> referencedDatabases, int totalConfigurations) {
        return new DatabaseConnectionsResponse(
            configType,
            connections,
            referencedDatabases,
            totalConfigurations,
            connections.size(),
            Instant.now()
        );
    }
    
    // Getters
    public String getConfigType() {
        return configType;
    }
    
    public Map<String, String> getConnections() {
        return connections;
    }
    
    public Set<String> getReferencedDatabases() {
        return referencedDatabases;
    }
    
    public int getTotalConfigurations() {
        return totalConfigurations;
    }
    
    public int getConfigurationsWithDatabases() {
        return configurationsWithDatabases;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    /**
     * Get database for specific configuration
     */
    public String getDatabaseFor(String configurationName) {
        return connections.get(configurationName);
    }
    
    /**
     * Check if configuration has database connection
     */
    public boolean hasDatabase(String configurationName) {
        return connections.containsKey(configurationName);
    }
    
    /**
     * Get all configurations using specific database
     */
    public List<String> getConfigurationsUsingDatabase(String databaseName) {
        return connections.entrySet().stream()
            .filter(entry -> databaseName.equals(entry.getValue()))
            .map(Map.Entry::getKey)
            .toList();
    }
    
    /**
     * Get number of configurations using specific database
     */
    public int getConfigurationCountForDatabase(String databaseName) {
        return getConfigurationsUsingDatabase(databaseName).size();
    }
    
    /**
     * Get most used database
     */
    public String getMostUsedDatabase() {
        if (referencedDatabases.isEmpty()) return null;
        
        return referencedDatabases.stream()
            .max((db1, db2) -> Integer.compare(
                getConfigurationCountForDatabase(db1),
                getConfigurationCountForDatabase(db2)
            ))
            .orElse(null);
    }
    
    /**
     * Get database usage statistics
     */
    public Map<String, Integer> getDatabaseUsageStats() {
        Map<String, Integer> stats = new HashMap<>();
        for (String database : referencedDatabases) {
            stats.put(database, getConfigurationCountForDatabase(database));
        }
        return stats;
    }
    
    /**
     * Get percentage of configurations with database connections
     */
    public double getDatabaseCoveragePercentage() {
        if (totalConfigurations == 0) return 0.0;
        return (double) configurationsWithDatabases / totalConfigurations * 100.0;
    }
    
    /**
     * Get configurations without database connections count
     */
    public int getConfigurationsWithoutDatabases() {
        return totalConfigurations - configurationsWithDatabases;
    }
    
    /**
     * Check if any configurations have database connections
     */
    public boolean hasAnyDatabaseConnections() {
        return configurationsWithDatabases > 0;
    }
    
    /**
     * Get total number of unique databases referenced
     */
    public int getUniqueDatabaseCount() {
        return referencedDatabases.size();
    }
    
    /**
     * Check if database is referenced by any configuration
     */
    public boolean isDatabaseReferenced(String databaseName) {
        return referencedDatabases.contains(databaseName);
    }
    
    /**
     * Convert to Map<String, Object> for backward compatibility
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("configType", configType);
        
        // Use different key names based on config type for backward compatibility
        if ("endpoints".equals(configType)) {
            map.put("endpointDatabases", connections);
        } else if ("queries".equals(configType)) {
            map.put("queryDatabases", connections);
        } else {
            map.put("connections", connections);
        }
        
        map.put("referencedDatabases", List.copyOf(referencedDatabases));
        map.put("totalConfigurations", totalConfigurations);
        map.put("configurationsWithDatabases", configurationsWithDatabases);
        map.put("timestamp", timestamp.toEpochMilli());
        
        return map;
    }
    
    @Override
    public String toString() {
        return "DatabaseConnectionsResponse{" +
                "configType='" + configType + '\'' +
                ", totalConfigurations=" + totalConfigurations +
                ", configurationsWithDatabases=" + configurationsWithDatabases +
                ", uniqueDatabases=" + getUniqueDatabaseCount() +
                ", timestamp=" + timestamp +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        DatabaseConnectionsResponse that = (DatabaseConnectionsResponse) o;
        
        if (totalConfigurations != that.totalConfigurations) return false;
        if (configurationsWithDatabases != that.configurationsWithDatabases) return false;
        if (!configType.equals(that.configType)) return false;
        if (!connections.equals(that.connections)) return false;
        if (!referencedDatabases.equals(that.referencedDatabases)) return false;
        return timestamp.equals(that.timestamp);
    }
    
    @Override
    public int hashCode() {
        int result = configType.hashCode();
        result = 31 * result + connections.hashCode();
        result = 31 * result + referencedDatabases.hashCode();
        result = 31 * result + totalConfigurations;
        result = 31 * result + configurationsWithDatabases;
        result = 31 * result + timestamp.hashCode();
        return result;
    }
}

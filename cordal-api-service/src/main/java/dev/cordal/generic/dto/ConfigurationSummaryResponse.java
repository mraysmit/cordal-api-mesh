package dev.cordal.generic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Type-safe response for configuration summary information
 * Replaces Map<String, Object> for summary methods (getEndpointConfigurationSummary, etc.)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfigurationSummaryResponse {
    
    @JsonProperty("configType")
    private final String configType;
    
    @JsonProperty("totalCount")
    private final int totalCount;
    
    @JsonProperty("withParameters")
    private final int withParameters;
    
    @JsonProperty("withPagination")
    private final Integer withPagination; // Only for endpoints
    
    @JsonProperty("byMethod")
    private final Map<String, Integer> byMethod; // Only for endpoints
    
    @JsonProperty("referencedQueries")
    private final Set<String> referencedQueries; // Only for endpoints
    
    @JsonProperty("referencedDatabases")
    private final Set<String> referencedDatabases;
    
    @JsonProperty("parameterCounts")
    private final Map<String, Integer> parameterCounts; // Only for queries
    
    @JsonProperty("timestamp")
    private final Instant timestamp;
    
    /**
     * Constructor for endpoint summaries
     */
    public ConfigurationSummaryResponse(String configType, int totalCount, int withParameters,
                                      int withPagination, Map<String, Integer> byMethod,
                                      Set<String> referencedQueries, Set<String> referencedDatabases,
                                      Instant timestamp) {
        this.configType = configType;
        this.totalCount = totalCount;
        this.withParameters = withParameters;
        this.withPagination = withPagination;
        this.byMethod = byMethod;
        this.referencedQueries = referencedQueries;
        this.referencedDatabases = referencedDatabases;
        this.parameterCounts = null;
        this.timestamp = timestamp;
    }
    
    /**
     * Constructor for query summaries
     */
    public ConfigurationSummaryResponse(String configType, int totalCount, int withParameters,
                                      Set<String> referencedDatabases, Map<String, Integer> parameterCounts,
                                      Instant timestamp) {
        this.configType = configType;
        this.totalCount = totalCount;
        this.withParameters = withParameters;
        this.withPagination = null;
        this.byMethod = null;
        this.referencedQueries = null;
        this.referencedDatabases = referencedDatabases;
        this.parameterCounts = parameterCounts;
        this.timestamp = timestamp;
    }
    
    /**
     * Static factory method for endpoint summaries
     */
    public static ConfigurationSummaryResponse forEndpoints(int totalCount, int withParameters,
                                                           int withPagination, Map<String, Integer> byMethod,
                                                           Set<String> referencedQueries, Set<String> referencedDatabases) {
        return new ConfigurationSummaryResponse("endpoints", totalCount, withParameters, withPagination,
                                               byMethod, referencedQueries, referencedDatabases, Instant.now());
    }
    
    /**
     * Static factory method for query summaries
     */
    public static ConfigurationSummaryResponse forQueries(int totalCount, int withParameters,
                                                         Set<String> referencedDatabases, Map<String, Integer> parameterCounts) {
        return new ConfigurationSummaryResponse("queries", totalCount, withParameters,
                                               referencedDatabases, parameterCounts, Instant.now());
    }
    
    // Getters
    public String getConfigType() {
        return configType;
    }
    
    public int getTotalCount() {
        return totalCount;
    }
    
    public int getWithParameters() {
        return withParameters;
    }
    
    public Integer getWithPagination() {
        return withPagination;
    }
    
    public Map<String, Integer> getByMethod() {
        return byMethod;
    }
    
    public Set<String> getReferencedQueries() {
        return referencedQueries;
    }
    
    public Set<String> getReferencedDatabases() {
        return referencedDatabases;
    }
    
    public Map<String, Integer> getParameterCounts() {
        return parameterCounts;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    /**
     * Check if this is an endpoint summary
     */
    public boolean isEndpointSummary() {
        return "endpoints".equals(configType);
    }
    
    /**
     * Check if this is a query summary
     */
    public boolean isQuerySummary() {
        return "queries".equals(configType);
    }
    
    /**
     * Get percentage of configurations with parameters
     */
    public double getParameterCoveragePercentage() {
        if (totalCount == 0) return 0.0;
        return (double) withParameters / totalCount * 100.0;
    }
    
    /**
     * Get percentage of endpoints with pagination (only for endpoints)
     */
    public double getPaginationCoveragePercentage() {
        if (!isEndpointSummary() || totalCount == 0 || withPagination == null) return 0.0;
        return (double) withPagination / totalCount * 100.0;
    }
    
    /**
     * Get configurations without parameters count
     */
    public int getWithoutParameters() {
        return totalCount - withParameters;
    }
    
    /**
     * Get most common HTTP method (only for endpoints)
     */
    public String getMostCommonMethod() {
        if (byMethod == null || byMethod.isEmpty()) return null;
        return byMethod.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    /**
     * Get total referenced databases count
     */
    public int getReferencedDatabaseCount() {
        return referencedDatabases != null ? referencedDatabases.size() : 0;
    }
    
    /**
     * Get total referenced queries count (only for endpoints)
     */
    public int getReferencedQueryCount() {
        return referencedQueries != null ? referencedQueries.size() : 0;
    }
    
    /**
     * Convert to Map<String, Object> for backward compatibility
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("configType", configType);
        map.put("totalCount", totalCount);
        map.put("withParameters", withParameters);
        
        if (withPagination != null) {
            map.put("withPagination", withPagination);
        }
        if (byMethod != null) {
            map.put("byMethod", byMethod);
        }
        if (referencedQueries != null) {
            map.put("referencedQueries", List.copyOf(referencedQueries));
        }
        if (referencedDatabases != null) {
            map.put("referencedDatabases", List.copyOf(referencedDatabases));
        }
        if (parameterCounts != null) {
            map.put("parameterCounts", parameterCounts);
        }
        
        map.put("timestamp", timestamp.toEpochMilli());
        return map;
    }
    
    @Override
    public String toString() {
        return "ConfigurationSummaryResponse{" +
                "configType='" + configType + '\'' +
                ", totalCount=" + totalCount +
                ", withParameters=" + withParameters +
                ", referencedDatabases=" + getReferencedDatabaseCount() +
                ", timestamp=" + timestamp +
                '}';
    }
}

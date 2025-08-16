package dev.cordal.cache.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Type-safe DTO for cache invalidation requests
 * Replaces unsafe Map<String, Object> parsing in CacheManagementController
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CacheInvalidationRequest {
    
    @JsonProperty("patterns")
    private List<String> patterns;
    
    @JsonProperty("cacheName")
    private String cacheName;
    
    /**
     * Default constructor for JSON deserialization
     */
    public CacheInvalidationRequest() {
    }
    
    /**
     * Constructor
     */
    public CacheInvalidationRequest(List<String> patterns, String cacheName) {
        this.patterns = patterns;
        this.cacheName = cacheName;
    }
    
    // Getters and setters
    public List<String> getPatterns() {
        return patterns;
    }
    
    public void setPatterns(List<String> patterns) {
        this.patterns = patterns;
    }
    
    public String getCacheName() {
        return cacheName;
    }
    
    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }
    
    /**
     * Check if patterns are provided
     */
    public boolean hasPatterns() {
        return patterns != null && !patterns.isEmpty();
    }
    
    /**
     * Check if cache name is specified
     */
    public boolean hasCacheName() {
        return cacheName != null && !cacheName.trim().isEmpty();
    }
    
    /**
     * Get number of patterns
     */
    public int getPatternCount() {
        return patterns != null ? patterns.size() : 0;
    }
    
    @Override
    public String toString() {
        return "CacheInvalidationRequest{" +
                "patterns=" + (patterns != null ? patterns.size() + " patterns" : "null") +
                ", cacheName='" + cacheName + '\'' +
                '}';
    }
}

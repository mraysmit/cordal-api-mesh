package dev.cordal.generic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Type-safe metadata for API responses
 * Replaces the unsafe Map<String, Object> metadata pattern
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseMetadata {
    
    @JsonProperty("executionTimeMs")
    private Long executionTimeMs;
    
    @JsonProperty("queryName")
    private String queryName;
    
    @JsonProperty("databaseName")
    private String databaseName;
    
    @JsonProperty("cacheHit")
    private Boolean cacheHit;
    
    @JsonProperty("cacheKey")
    private String cacheKey;
    
    @JsonProperty("requestId")
    private String requestId;
    
    @JsonProperty("endpointName")
    private String endpointName;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("version")
    private String version;
    
    @JsonProperty("source")
    private String source;
    
    // Additional custom metadata
    @JsonProperty("custom")
    private Map<String, Object> customMetadata;
    
    /**
     * Default constructor
     */
    public ResponseMetadata() {
        this.timestamp = LocalDateTime.now();
        this.customMetadata = new HashMap<>();
    }
    
    /**
     * Constructor with basic metadata
     */
    public ResponseMetadata(String endpointName, String queryName, String databaseName) {
        this();
        this.endpointName = endpointName;
        this.queryName = queryName;
        this.databaseName = databaseName;
    }
    
    // Fluent builder methods
    public ResponseMetadata executionTime(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
        return this;
    }
    
    public ResponseMetadata queryName(String queryName) {
        this.queryName = queryName;
        return this;
    }
    
    public ResponseMetadata databaseName(String databaseName) {
        this.databaseName = databaseName;
        return this;
    }
    
    public ResponseMetadata cacheHit(Boolean cacheHit) {
        this.cacheHit = cacheHit;
        return this;
    }
    
    public ResponseMetadata cacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
        return this;
    }
    
    public ResponseMetadata requestId(String requestId) {
        this.requestId = requestId;
        return this;
    }
    
    public ResponseMetadata endpointName(String endpointName) {
        this.endpointName = endpointName;
        return this;
    }
    
    public ResponseMetadata version(String version) {
        this.version = version;
        return this;
    }
    
    public ResponseMetadata source(String source) {
        this.source = source;
        return this;
    }
    
    /**
     * Add custom metadata
     */
    public ResponseMetadata addCustom(String key, Object value) {
        if (customMetadata == null) {
            customMetadata = new HashMap<>();
        }
        customMetadata.put(key, value);
        return this;
    }
    
    /**
     * Add multiple custom metadata entries
     */
    public ResponseMetadata addCustom(Map<String, Object> metadata) {
        if (customMetadata == null) {
            customMetadata = new HashMap<>();
        }
        customMetadata.putAll(metadata);
        return this;
    }
    
    // Getters and Setters
    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    public String getQueryName() {
        return queryName;
    }
    
    public void setQueryName(String queryName) {
        this.queryName = queryName;
    }
    
    public String getDatabaseName() {
        return databaseName;
    }
    
    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }
    
    public Boolean getCacheHit() {
        return cacheHit;
    }
    
    public void setCacheHit(Boolean cacheHit) {
        this.cacheHit = cacheHit;
    }
    
    public String getCacheKey() {
        return cacheKey;
    }
    
    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public String getEndpointName() {
        return endpointName;
    }
    
    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public Map<String, Object> getCustomMetadata() {
        return customMetadata;
    }
    
    public void setCustomMetadata(Map<String, Object> customMetadata) {
        this.customMetadata = customMetadata;
    }
    
    /**
     * Convert to Map<String, Object> for backward compatibility
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        
        if (executionTimeMs != null) map.put("executionTimeMs", executionTimeMs);
        if (queryName != null) map.put("queryName", queryName);
        if (databaseName != null) map.put("databaseName", databaseName);
        if (cacheHit != null) map.put("cacheHit", cacheHit);
        if (cacheKey != null) map.put("cacheKey", cacheKey);
        if (requestId != null) map.put("requestId", requestId);
        if (endpointName != null) map.put("endpointName", endpointName);
        if (timestamp != null) map.put("timestamp", timestamp);
        if (version != null) map.put("version", version);
        if (source != null) map.put("source", source);
        if (customMetadata != null && !customMetadata.isEmpty()) {
            map.put("custom", customMetadata);
        }
        
        return map;
    }
    
    /**
     * Create from Map<String, Object> for backward compatibility
     * For simple test cases, treat the entire map as custom metadata
     */
    public static ResponseMetadata fromMap(Map<String, Object> map) {
        ResponseMetadata metadata = new ResponseMetadata();

        // Check if this looks like a structured metadata map or just arbitrary data
        Set<String> structuredFields = Set.of("executionTimeMs", "queryName", "databaseName",
            "cacheHit", "cacheKey", "requestId", "endpointName", "timestamp", "version", "custom");

        boolean hasStructuredFields = map.keySet().stream()
            .anyMatch(structuredFields::contains);

        if (hasStructuredFields) {
            // Parse as structured metadata
            if (map.get("executionTimeMs") instanceof Number) {
                metadata.setExecutionTimeMs(((Number) map.get("executionTimeMs")).longValue());
            }
            if (map.get("queryName") != null) {
                metadata.setQueryName(map.get("queryName").toString());
            }
            if (map.get("databaseName") != null) {
                metadata.setDatabaseName(map.get("databaseName").toString());
            }
            if (map.get("cacheHit") instanceof Boolean) {
                metadata.setCacheHit((Boolean) map.get("cacheHit"));
            }
            if (map.get("cacheKey") != null) {
                metadata.setCacheKey(map.get("cacheKey").toString());
            }
            if (map.get("requestId") != null) {
                metadata.setRequestId(map.get("requestId").toString());
            }
            if (map.get("endpointName") != null) {
                metadata.setEndpointName(map.get("endpointName").toString());
            }
            if (map.get("timestamp") instanceof LocalDateTime) {
                metadata.setTimestamp((LocalDateTime) map.get("timestamp"));
            }
            if (map.get("version") != null) {
                metadata.setVersion(map.get("version").toString());
            }
            if (map.get("source") != null) {
                metadata.setSource(map.get("source").toString());
            }
            if (map.get("custom") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> custom = (Map<String, Object>) map.get("custom");
                metadata.setCustomMetadata(new HashMap<>(custom));
            }

            // Add any unknown fields to custom metadata
            Map<String, Object> customData = new HashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!structuredFields.contains(entry.getKey())) {
                    customData.put(entry.getKey(), entry.getValue());
                }
            }

            if (!customData.isEmpty()) {
                if (metadata.getCustomMetadata() == null) {
                    metadata.setCustomMetadata(customData);
                } else {
                    metadata.getCustomMetadata().putAll(customData);
                }
            }
        } else {
            // Treat entire map as custom metadata (for backward compatibility with tests)
            metadata.setCustomMetadata(new HashMap<>(map));
        }

        return metadata;
    }
    
    @Override
    public String toString() {
        return "ResponseMetadata{" +
                "executionTimeMs=" + executionTimeMs +
                ", queryName='" + queryName + '\'' +
                ", databaseName='" + databaseName + '\'' +
                ", cacheHit=" + cacheHit +
                ", endpointName='" + endpointName + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

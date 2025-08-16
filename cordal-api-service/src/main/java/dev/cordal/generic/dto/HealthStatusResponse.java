package dev.cordal.generic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Type-safe response for comprehensive health status
 * Replaces Map<String, Object> for getHealthStatus()
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HealthStatusResponse {
    
    @JsonProperty("timestamp")
    private final Instant timestamp;
    
    @JsonProperty("service")
    private final ServiceHealthResponse service;
    
    @JsonProperty("databases")
    private final Map<String, DatabaseHealthResponse> databases;
    
    @JsonProperty("configuration")
    private final ConfigurationHealthResponse configuration;
    
    @JsonProperty("overall")
    private final String overall;
    
    /**
     * Constructor
     */
    public HealthStatusResponse(Instant timestamp, ServiceHealthResponse service,
                               Map<String, DatabaseHealthResponse> databases,
                               ConfigurationHealthResponse configuration, String overall) {
        this.timestamp = timestamp;
        this.service = service;
        this.databases = databases;
        this.configuration = configuration;
        this.overall = overall;
    }
    
    /**
     * Static factory method
     */
    public static HealthStatusResponse of(ServiceHealthResponse service,
                                        Map<String, DatabaseHealthResponse> databases,
                                        ConfigurationHealthResponse configuration,
                                        String overall) {
        return new HealthStatusResponse(Instant.now(), service, databases, configuration, overall);
    }
    
    // Getters
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public ServiceHealthResponse getService() {
        return service;
    }
    
    public Map<String, DatabaseHealthResponse> getDatabases() {
        return databases;
    }
    
    public ConfigurationHealthResponse getConfiguration() {
        return configuration;
    }
    
    public String getOverall() {
        return overall;
    }
    
    /**
     * Check if overall health is UP
     */
    public boolean isHealthy() {
        return "UP".equals(overall);
    }
    
    /**
     * Check if overall health is DOWN
     */
    public boolean isDown() {
        return "DOWN".equals(overall);
    }
    
    /**
     * Check if overall health is DEGRADED
     */
    public boolean isDegraded() {
        return "DEGRADED".equals(overall);
    }
    
    /**
     * Convert to Map<String, Object> for backward compatibility
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("timestamp", timestamp);
        map.put("service", service != null ? service.toMap() : null);
        
        if (databases != null) {
            Map<String, Object> dbMap = new HashMap<>();
            databases.forEach((name, health) -> dbMap.put(name, health.toMap()));
            map.put("databases", dbMap);
        }
        
        map.put("configuration", configuration != null ? configuration.toMap() : null);
        map.put("overall", overall);
        
        return map;
    }
    
    @Override
    public String toString() {
        return "HealthStatusResponse{" +
                "timestamp=" + timestamp +
                ", overall='" + overall + '\'' +
                ", service=" + service +
                ", databases=" + (databases != null ? databases.size() + " databases" : "null") +
                ", configuration=" + configuration +
                '}';
    }
}

package dev.cordal.generic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

/**
 * Type-safe response for configuration CRUD operations
 * Replaces Map<String, Object> for save/delete operations
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfigurationOperationResponse {
    
    @JsonProperty("success")
    private final boolean success;
    
    @JsonProperty("action")
    private final String action; // "created", "updated", "deleted"
    
    @JsonProperty("name")
    private final String name;
    
    @JsonProperty("timestamp")
    private final Instant timestamp;
    
    @JsonProperty("found")
    private final Boolean found; // For delete operations
    
    @JsonProperty("message")
    private final String message; // Optional message
    
    /**
     * Constructor for save operations (create/update)
     */
    public ConfigurationOperationResponse(boolean success, String action, String name, Instant timestamp) {
        this.success = success;
        this.action = action;
        this.name = name;
        this.timestamp = timestamp;
        this.found = null;
        this.message = null;
    }
    
    /**
     * Constructor for delete operations
     */
    public ConfigurationOperationResponse(boolean success, String action, String name, 
                                        Instant timestamp, boolean found) {
        this.success = success;
        this.action = action;
        this.name = name;
        this.timestamp = timestamp;
        this.found = found;
        this.message = null;
    }
    
    /**
     * Constructor with custom message
     */
    public ConfigurationOperationResponse(boolean success, String action, String name, 
                                        Instant timestamp, String message) {
        this.success = success;
        this.action = action;
        this.name = name;
        this.timestamp = timestamp;
        this.found = null;
        this.message = message;
    }
    
    /**
     * Constructor for delete operations with message
     */
    public ConfigurationOperationResponse(boolean success, String action, String name, 
                                        Instant timestamp, boolean found, String message) {
        this.success = success;
        this.action = action;
        this.name = name;
        this.timestamp = timestamp;
        this.found = found;
        this.message = message;
    }
    
    // Static factory methods for common operations
    public static ConfigurationOperationResponse created(String name) {
        return new ConfigurationOperationResponse(true, "created", name, Instant.now());
    }
    
    public static ConfigurationOperationResponse updated(String name) {
        return new ConfigurationOperationResponse(true, "updated", name, Instant.now());
    }
    
    public static ConfigurationOperationResponse deleted(String name, boolean found) {
        return new ConfigurationOperationResponse(true, "deleted", name, Instant.now(), found);
    }
    
    public static ConfigurationOperationResponse failed(String action, String name, String message) {
        return new ConfigurationOperationResponse(false, action, name, Instant.now(), message);
    }
    
    public static ConfigurationOperationResponse success(String action, String name) {
        return new ConfigurationOperationResponse(true, action, name, Instant.now());
    }
    
    // Getters
    public boolean isSuccess() {
        return success;
    }
    
    public String getAction() {
        return action;
    }
    
    public String getName() {
        return name;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public Boolean getFound() {
        return found;
    }
    
    public String getMessage() {
        return message;
    }
    
    /**
     * Check if this was a create operation
     */
    public boolean isCreated() {
        return "created".equals(action);
    }
    
    /**
     * Check if this was an update operation
     */
    public boolean isUpdated() {
        return "updated".equals(action);
    }
    
    /**
     * Check if this was a delete operation
     */
    public boolean isDeleted() {
        return "deleted".equals(action);
    }
    
    /**
     * Convert to Map<String, Object> for backward compatibility
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("success", success);
        map.put("action", action);
        map.put("name", name);
        map.put("timestamp", timestamp);
        
        if (found != null) {
            map.put("found", found);
        }
        
        if (message != null) {
            map.put("message", message);
        }
        
        return map;
    }
    
    /**
     * Create from Map<String, Object> for backward compatibility
     */
    public static ConfigurationOperationResponse fromMap(Map<String, Object> map) {
        boolean success = (Boolean) map.getOrDefault("success", false);
        String action = (String) map.getOrDefault("action", "unknown");
        String name = (String) map.getOrDefault("name", "");
        Instant timestamp = (Instant) map.getOrDefault("timestamp", Instant.now());
        Boolean found = (Boolean) map.get("found");
        String message = (String) map.get("message");
        
        if (found != null && message != null) {
            return new ConfigurationOperationResponse(success, action, name, timestamp, found, message);
        } else if (found != null) {
            return new ConfigurationOperationResponse(success, action, name, timestamp, found);
        } else if (message != null) {
            return new ConfigurationOperationResponse(success, action, name, timestamp, message);
        } else {
            return new ConfigurationOperationResponse(success, action, name, timestamp);
        }
    }
    
    @Override
    public String toString() {
        return "ConfigurationOperationResponse{" +
                "success=" + success +
                ", action='" + action + '\'' +
                ", name='" + name + '\'' +
                ", timestamp=" + timestamp +
                ", found=" + found +
                ", message='" + message + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        ConfigurationOperationResponse that = (ConfigurationOperationResponse) o;
        
        if (success != that.success) return false;
        if (!action.equals(that.action)) return false;
        if (!name.equals(that.name)) return false;
        if (!timestamp.equals(that.timestamp)) return false;
        if (found != null ? !found.equals(that.found) : that.found != null) return false;
        return message != null ? message.equals(that.message) : that.message == null;
    }
    
    @Override
    public int hashCode() {
        int result = (success ? 1 : 0);
        result = 31 * result + action.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + timestamp.hashCode();
        result = 31 * result + (found != null ? found.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        return result;
    }
}

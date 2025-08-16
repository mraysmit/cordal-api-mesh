package dev.cordal.generic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Type-safe response for configuration health status
 * Replaces Map<String, Object> for getConfigurationHealth()
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfigurationHealthResponse {
    
    @JsonProperty("status")
    private final String status;
    
    @JsonProperty("databasesLoaded")
    private final Integer databasesLoaded;
    
    @JsonProperty("queriesLoaded")
    private final Integer queriesLoaded;
    
    @JsonProperty("endpointsLoaded")
    private final Integer endpointsLoaded;
    
    @JsonProperty("lastValidation")
    private final String lastValidation;
    
    @JsonProperty("error")
    private final String error;
    
    /**
     * Constructor for successful configuration health
     */
    public ConfigurationHealthResponse(String status, int databasesLoaded, int queriesLoaded, 
                                     int endpointsLoaded, String lastValidation) {
        this.status = status;
        this.databasesLoaded = databasesLoaded;
        this.queriesLoaded = queriesLoaded;
        this.endpointsLoaded = endpointsLoaded;
        this.lastValidation = lastValidation;
        this.error = null;
    }
    
    /**
     * Constructor for failed configuration health
     */
    public ConfigurationHealthResponse(String status, String error) {
        this.status = status;
        this.error = error;
        this.databasesLoaded = null;
        this.queriesLoaded = null;
        this.endpointsLoaded = null;
        this.lastValidation = null;
    }
    
    /**
     * Static factory method for UP status
     */
    public static ConfigurationHealthResponse up(int databasesLoaded, int queriesLoaded, 
                                               int endpointsLoaded, String lastValidation) {
        return new ConfigurationHealthResponse("UP", databasesLoaded, queriesLoaded, 
                                             endpointsLoaded, lastValidation);
    }
    
    /**
     * Static factory method for UP status with SUCCESS validation
     */
    public static ConfigurationHealthResponse up(int databasesLoaded, int queriesLoaded, int endpointsLoaded) {
        return up(databasesLoaded, queriesLoaded, endpointsLoaded, "SUCCESS");
    }
    
    /**
     * Static factory method for DOWN status
     */
    public static ConfigurationHealthResponse down(String error) {
        return new ConfigurationHealthResponse("DOWN", error);
    }
    
    // Getters
    public String getStatus() {
        return status;
    }
    
    public Integer getDatabasesLoaded() {
        return databasesLoaded;
    }
    
    public Integer getQueriesLoaded() {
        return queriesLoaded;
    }
    
    public Integer getEndpointsLoaded() {
        return endpointsLoaded;
    }
    
    public String getLastValidation() {
        return lastValidation;
    }
    
    public String getError() {
        return error;
    }
    
    /**
     * Check if configuration is up
     */
    public boolean isUp() {
        return "UP".equals(status);
    }
    
    /**
     * Check if configuration is down
     */
    public boolean isDown() {
        return "DOWN".equals(status);
    }
    
    /**
     * Check if there are any configurations loaded
     */
    public boolean hasConfigurations() {
        return (databasesLoaded != null && databasesLoaded > 0) ||
               (queriesLoaded != null && queriesLoaded > 0) ||
               (endpointsLoaded != null && endpointsLoaded > 0);
    }
    
    /**
     * Get total configurations loaded
     */
    public int getTotalConfigurations() {
        int total = 0;
        if (databasesLoaded != null) total += databasesLoaded;
        if (queriesLoaded != null) total += queriesLoaded;
        if (endpointsLoaded != null) total += endpointsLoaded;
        return total;
    }
    
    /**
     * Check if validation was successful
     */
    public boolean isValidationSuccessful() {
        return "SUCCESS".equals(lastValidation);
    }
    
    /**
     * Convert to Map<String, Object> for backward compatibility
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", status);
        
        if (databasesLoaded != null) {
            map.put("databasesLoaded", databasesLoaded);
        }
        if (queriesLoaded != null) {
            map.put("queriesLoaded", queriesLoaded);
        }
        if (endpointsLoaded != null) {
            map.put("endpointsLoaded", endpointsLoaded);
        }
        if (lastValidation != null) {
            map.put("lastValidation", lastValidation);
        }
        if (error != null) {
            map.put("error", error);
        }
        
        return map;
    }
    
    @Override
    public String toString() {
        if (error != null) {
            return "ConfigurationHealthResponse{" +
                    "status='" + status + '\'' +
                    ", error='" + error + '\'' +
                    '}';
        } else {
            return "ConfigurationHealthResponse{" +
                    "status='" + status + '\'' +
                    ", databasesLoaded=" + databasesLoaded +
                    ", queriesLoaded=" + queriesLoaded +
                    ", endpointsLoaded=" + endpointsLoaded +
                    ", lastValidation='" + lastValidation + '\'' +
                    '}';
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        ConfigurationHealthResponse that = (ConfigurationHealthResponse) o;
        
        if (!status.equals(that.status)) return false;
        if (databasesLoaded != null ? !databasesLoaded.equals(that.databasesLoaded) : that.databasesLoaded != null)
            return false;
        if (queriesLoaded != null ? !queriesLoaded.equals(that.queriesLoaded) : that.queriesLoaded != null)
            return false;
        if (endpointsLoaded != null ? !endpointsLoaded.equals(that.endpointsLoaded) : that.endpointsLoaded != null)
            return false;
        if (lastValidation != null ? !lastValidation.equals(that.lastValidation) : that.lastValidation != null)
            return false;
        return error != null ? error.equals(that.error) : that.error == null;
    }
    
    @Override
    public int hashCode() {
        int result = status.hashCode();
        result = 31 * result + (databasesLoaded != null ? databasesLoaded.hashCode() : 0);
        result = 31 * result + (queriesLoaded != null ? queriesLoaded.hashCode() : 0);
        result = 31 * result + (endpointsLoaded != null ? endpointsLoaded.hashCode() : 0);
        result = 31 * result + (lastValidation != null ? lastValidation.hashCode() : 0);
        result = 31 * result + (error != null ? error.hashCode() : 0);
        return result;
    }
}

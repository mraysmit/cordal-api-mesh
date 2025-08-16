package dev.cordal.generic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Type-safe response for readiness checks
 * Replaces Map<String, Object> for getReadinessCheck() and getLivenessCheck()
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReadinessCheckResponse {
    
    @JsonProperty("status")
    private final String status;
    
    @JsonProperty("timestamp")
    private final Instant timestamp;
    
    @JsonProperty("checks")
    private final Map<String, String> checks;
    
    @JsonProperty("message")
    private final String message;
    
    /**
     * Constructor
     */
    public ReadinessCheckResponse(String status, Instant timestamp, Map<String, String> checks, String message) {
        this.status = status;
        this.timestamp = timestamp;
        this.checks = checks;
        this.message = message;
    }
    
    /**
     * Static factory method for UP status
     */
    public static ReadinessCheckResponse up(Map<String, String> checks) {
        return new ReadinessCheckResponse("UP", Instant.now(), checks, "All checks passed");
    }
    
    /**
     * Static factory method for UP status with custom message
     */
    public static ReadinessCheckResponse up(Map<String, String> checks, String message) {
        return new ReadinessCheckResponse("UP", Instant.now(), checks, message);
    }
    
    /**
     * Static factory method for DOWN status
     */
    public static ReadinessCheckResponse down(Map<String, String> checks, String message) {
        return new ReadinessCheckResponse("DOWN", Instant.now(), checks, message);
    }
    
    /**
     * Static factory method for simple UP status
     */
    public static ReadinessCheckResponse up() {
        return up(Map.of("service", "UP"), "Service is ready");
    }
    
    /**
     * Static factory method for simple DOWN status
     */
    public static ReadinessCheckResponse down(String message) {
        return down(Map.of("service", "DOWN"), message);
    }
    
    // Getters
    public String getStatus() {
        return status;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public Map<String, String> getChecks() {
        return checks;
    }
    
    public String getMessage() {
        return message;
    }
    
    /**
     * Check if the service is ready
     */
    public boolean isReady() {
        return "UP".equals(status);
    }
    
    /**
     * Check if the service is not ready
     */
    public boolean isNotReady() {
        return "DOWN".equals(status);
    }
    
    /**
     * Check if all individual checks passed
     */
    public boolean allChecksPassed() {
        if (checks == null || checks.isEmpty()) {
            return isReady();
        }
        return checks.values().stream().allMatch("UP"::equals);
    }
    
    /**
     * Get failed checks
     */
    public Map<String, String> getFailedChecks() {
        if (checks == null) {
            return Map.of();
        }
        Map<String, String> failed = new HashMap<>();
        checks.entrySet().stream()
            .filter(entry -> !"UP".equals(entry.getValue()))
            .forEach(entry -> failed.put(entry.getKey(), entry.getValue()));
        return failed;
    }
    
    /**
     * Get number of failed checks
     */
    public int getFailedCheckCount() {
        return getFailedChecks().size();
    }
    
    /**
     * Convert to Map<String, Object> for backward compatibility
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", status);
        map.put("timestamp", timestamp);
        if (checks != null) {
            map.put("checks", checks);
        }
        if (message != null) {
            map.put("message", message);
        }
        return map;
    }
    
    @Override
    public String toString() {
        return "ReadinessCheckResponse{" +
                "status='" + status + '\'' +
                ", timestamp=" + timestamp +
                ", checks=" + (checks != null ? checks.size() + " checks" : "no checks") +
                ", message='" + message + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        ReadinessCheckResponse that = (ReadinessCheckResponse) o;
        
        if (!status.equals(that.status)) return false;
        if (!timestamp.equals(that.timestamp)) return false;
        if (checks != null ? !checks.equals(that.checks) : that.checks != null) return false;
        return message != null ? message.equals(that.message) : that.message == null;
    }
    
    @Override
    public int hashCode() {
        int result = status.hashCode();
        result = 31 * result + timestamp.hashCode();
        result = 31 * result + (checks != null ? checks.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        return result;
    }
}

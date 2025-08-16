package dev.cordal.generic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.cordal.generic.management.HealthMonitoringService.DatabaseHealthStatus;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Type-safe response for database health status
 * Wraps the existing DatabaseHealthStatus for consistency with other DTOs
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatabaseHealthResponse {
    
    @JsonProperty("databaseName")
    private final String databaseName;
    
    @JsonProperty("status")
    private final String status;
    
    @JsonProperty("message")
    private final String message;
    
    @JsonProperty("checkTime")
    private final Instant checkTime;
    
    @JsonProperty("responseTimeMs")
    private final long responseTimeMs;
    
    /**
     * Constructor
     */
    public DatabaseHealthResponse(String databaseName, String status, String message, 
                                 Instant checkTime, long responseTimeMs) {
        this.databaseName = databaseName;
        this.status = status;
        this.message = message;
        this.checkTime = checkTime;
        this.responseTimeMs = responseTimeMs;
    }
    
    /**
     * Create from existing DatabaseHealthStatus
     */
    public static DatabaseHealthResponse from(DatabaseHealthStatus status) {
        return new DatabaseHealthResponse(
            status.getDatabaseName(),
            status.getStatus(),
            status.getMessage(),
            status.getCheckTime(),
            status.getResponseTimeMs()
        );
    }
    
    /**
     * Static factory method for UP status
     */
    public static DatabaseHealthResponse up(String databaseName, String message, long responseTimeMs) {
        return new DatabaseHealthResponse(databaseName, "UP", message, Instant.now(), responseTimeMs);
    }
    
    /**
     * Static factory method for DOWN status
     */
    public static DatabaseHealthResponse down(String databaseName, String message, long responseTimeMs) {
        return new DatabaseHealthResponse(databaseName, "DOWN", message, Instant.now(), responseTimeMs);
    }
    
    // Getters
    public String getDatabaseName() {
        return databaseName;
    }
    
    public String getStatus() {
        return status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public Instant getCheckTime() {
        return checkTime;
    }
    
    public long getResponseTimeMs() {
        return responseTimeMs;
    }
    
    /**
     * Check if database is up
     */
    public boolean isUp() {
        return "UP".equals(status);
    }
    
    /**
     * Check if database is down
     */
    public boolean isDown() {
        return "DOWN".equals(status);
    }
    
    /**
     * Check if response time is slow (>1000ms)
     */
    public boolean isSlow() {
        return responseTimeMs > 1000;
    }
    
    /**
     * Check if response time is very slow (>5000ms)
     */
    public boolean isVerySlow() {
        return responseTimeMs > 5000;
    }
    
    /**
     * Get performance status based on response time
     */
    public String getPerformanceStatus() {
        if (responseTimeMs > 5000) return "VERY_SLOW";
        if (responseTimeMs > 1000) return "SLOW";
        if (responseTimeMs > 500) return "MODERATE";
        return "FAST";
    }
    
    /**
     * Check if the health check is still valid (within TTL)
     */
    public boolean isValid(long ttlMs) {
        return java.time.Duration.between(checkTime, Instant.now()).toMillis() < ttlMs;
    }
    
    /**
     * Convert to Map<String, Object> for backward compatibility
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("databaseName", databaseName);
        map.put("status", status);
        map.put("message", message);
        map.put("checkTime", checkTime);
        map.put("responseTimeMs", responseTimeMs);
        return map;
    }
    
    /**
     * Convert to DatabaseHealthStatus for backward compatibility
     */
    public DatabaseHealthStatus toDatabaseHealthStatus() {
        return new DatabaseHealthStatus(databaseName, status, message, checkTime, responseTimeMs);
    }
    
    @Override
    public String toString() {
        return "DatabaseHealthResponse{" +
                "databaseName='" + databaseName + '\'' +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", checkTime=" + checkTime +
                ", responseTimeMs=" + responseTimeMs +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        DatabaseHealthResponse that = (DatabaseHealthResponse) o;
        
        if (responseTimeMs != that.responseTimeMs) return false;
        if (!databaseName.equals(that.databaseName)) return false;
        if (!status.equals(that.status)) return false;
        if (!message.equals(that.message)) return false;
        return checkTime.equals(that.checkTime);
    }
    
    @Override
    public int hashCode() {
        int result = databaseName.hashCode();
        result = 31 * result + status.hashCode();
        result = 31 * result + message.hashCode();
        result = 31 * result + checkTime.hashCode();
        result = 31 * result + (int) (responseTimeMs ^ (responseTimeMs >>> 32));
        return result;
    }
}

package dev.cordal.generic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Type-safe response for service health status
 * Replaces Map<String, Object> for getServiceHealth()
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceHealthResponse {
    
    @JsonProperty("status")
    private final String status;
    
    @JsonProperty("uptime")
    private final String uptime;
    
    @JsonProperty("memoryUsage")
    private final MemoryUsageResponse memoryUsage;
    
    @JsonProperty("threadCount")
    private final int threadCount;
    
    /**
     * Constructor
     */
    public ServiceHealthResponse(String status, String uptime, MemoryUsageResponse memoryUsage, int threadCount) {
        this.status = status;
        this.uptime = uptime;
        this.memoryUsage = memoryUsage;
        this.threadCount = threadCount;
    }
    
    /**
     * Static factory method
     */
    public static ServiceHealthResponse up(String uptime, MemoryUsageResponse memoryUsage, int threadCount) {
        return new ServiceHealthResponse("UP", uptime, memoryUsage, threadCount);
    }
    
    /**
     * Static factory method for down status
     */
    public static ServiceHealthResponse down(String uptime, MemoryUsageResponse memoryUsage, int threadCount) {
        return new ServiceHealthResponse("DOWN", uptime, memoryUsage, threadCount);
    }
    
    // Getters
    public String getStatus() {
        return status;
    }
    
    public String getUptime() {
        return uptime;
    }
    
    public MemoryUsageResponse getMemoryUsage() {
        return memoryUsage;
    }
    
    public int getThreadCount() {
        return threadCount;
    }
    
    /**
     * Check if service is up
     */
    public boolean isUp() {
        return "UP".equals(status);
    }
    
    /**
     * Check if service is down
     */
    public boolean isDown() {
        return "DOWN".equals(status);
    }
    
    /**
     * Convert to Map<String, Object> for backward compatibility
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", status);
        map.put("uptime", uptime);
        map.put("memoryUsage", memoryUsage != null ? memoryUsage.toMap() : null);
        map.put("threadCount", threadCount);
        return map;
    }
    
    @Override
    public String toString() {
        return "ServiceHealthResponse{" +
                "status='" + status + '\'' +
                ", uptime='" + uptime + '\'' +
                ", memoryUsage=" + memoryUsage +
                ", threadCount=" + threadCount +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        ServiceHealthResponse that = (ServiceHealthResponse) o;
        
        if (threadCount != that.threadCount) return false;
        if (!status.equals(that.status)) return false;
        if (!uptime.equals(that.uptime)) return false;
        return memoryUsage != null ? memoryUsage.equals(that.memoryUsage) : that.memoryUsage == null;
    }
    
    @Override
    public int hashCode() {
        int result = status.hashCode();
        result = 31 * result + uptime.hashCode();
        result = 31 * result + (memoryUsage != null ? memoryUsage.hashCode() : 0);
        result = 31 * result + threadCount;
        return result;
    }
}

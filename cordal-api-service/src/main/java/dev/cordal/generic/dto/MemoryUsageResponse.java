package dev.cordal.generic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Type-safe response for memory usage information
 * Replaces Map<String, Object> for getMemoryUsage()
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemoryUsageResponse {
    
    @JsonProperty("maxMemoryMB")
    private final long maxMemoryMB;
    
    @JsonProperty("totalMemoryMB")
    private final long totalMemoryMB;
    
    @JsonProperty("usedMemoryMB")
    private final long usedMemoryMB;
    
    @JsonProperty("freeMemoryMB")
    private final long freeMemoryMB;
    
    @JsonProperty("usagePercentage")
    private final long usagePercentage;
    
    /**
     * Constructor
     */
    public MemoryUsageResponse(long maxMemoryMB, long totalMemoryMB, long usedMemoryMB, 
                              long freeMemoryMB, long usagePercentage) {
        this.maxMemoryMB = maxMemoryMB;
        this.totalMemoryMB = totalMemoryMB;
        this.usedMemoryMB = usedMemoryMB;
        this.freeMemoryMB = freeMemoryMB;
        this.usagePercentage = usagePercentage;
    }
    
    /**
     * Static factory method from Runtime
     */
    public static MemoryUsageResponse fromRuntime() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        long maxMemoryMB = maxMemory / (1024 * 1024);
        long totalMemoryMB = totalMemory / (1024 * 1024);
        long usedMemoryMB = usedMemory / (1024 * 1024);
        long freeMemoryMB = freeMemory / (1024 * 1024);
        long usagePercentage = Math.round((double) usedMemory / totalMemory * 100);
        
        return new MemoryUsageResponse(maxMemoryMB, totalMemoryMB, usedMemoryMB, freeMemoryMB, usagePercentage);
    }
    
    // Getters
    public long getMaxMemoryMB() {
        return maxMemoryMB;
    }
    
    public long getTotalMemoryMB() {
        return totalMemoryMB;
    }
    
    public long getUsedMemoryMB() {
        return usedMemoryMB;
    }
    
    public long getFreeMemoryMB() {
        return freeMemoryMB;
    }
    
    public long getUsagePercentage() {
        return usagePercentage;
    }
    
    /**
     * Check if memory usage is high (>80%)
     */
    public boolean isHighUsage() {
        return usagePercentage > 80;
    }
    
    /**
     * Check if memory usage is critical (>90%)
     */
    public boolean isCriticalUsage() {
        return usagePercentage > 90;
    }
    
    /**
     * Check if memory usage is very critical (>95%)
     */
    public boolean isVeryCriticalUsage() {
        return usagePercentage > 95;
    }
    
    /**
     * Get memory status based on usage
     */
    public String getMemoryStatus() {
        if (usagePercentage > 95) return "CRITICAL";
        if (usagePercentage > 90) return "HIGH";
        if (usagePercentage > 80) return "MODERATE";
        return "OK";
    }
    
    /**
     * Convert to Map<String, Object> for backward compatibility
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("maxMemoryMB", maxMemoryMB);
        map.put("totalMemoryMB", totalMemoryMB);
        map.put("usedMemoryMB", usedMemoryMB);
        map.put("freeMemoryMB", freeMemoryMB);
        map.put("usagePercentage", usagePercentage);
        return map;
    }
    
    @Override
    public String toString() {
        return "MemoryUsageResponse{" +
                "maxMemoryMB=" + maxMemoryMB +
                ", totalMemoryMB=" + totalMemoryMB +
                ", usedMemoryMB=" + usedMemoryMB +
                ", freeMemoryMB=" + freeMemoryMB +
                ", usagePercentage=" + usagePercentage + "%" +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        MemoryUsageResponse that = (MemoryUsageResponse) o;
        
        if (maxMemoryMB != that.maxMemoryMB) return false;
        if (totalMemoryMB != that.totalMemoryMB) return false;
        if (usedMemoryMB != that.usedMemoryMB) return false;
        if (freeMemoryMB != that.freeMemoryMB) return false;
        return usagePercentage == that.usagePercentage;
    }
    
    @Override
    public int hashCode() {
        int result = (int) (maxMemoryMB ^ (maxMemoryMB >>> 32));
        result = 31 * result + (int) (totalMemoryMB ^ (totalMemoryMB >>> 32));
        result = 31 * result + (int) (usedMemoryMB ^ (usedMemoryMB >>> 32));
        result = 31 * result + (int) (freeMemoryMB ^ (freeMemoryMB >>> 32));
        result = 31 * result + (int) (usagePercentage ^ (usagePercentage >>> 32));
        return result;
    }
}

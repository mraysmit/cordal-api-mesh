package dev.cordal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Type-safe DTO for performance trends data
 * Replaces the unsafe Map<String, Object> return type
 */
public class PerformanceTrendsDto {
    
    @JsonProperty("dates")
    private final List<String> dates;
    
    @JsonProperty("averageResponseTimes")
    private final Map<String, Double> averageResponseTimes;
    
    @JsonProperty("successRates")
    private final Map<String, Double> successRates;
    
    @JsonProperty("totalDataPoints")
    private final int totalDataPoints;
    
    /**
     * Constructor for performance trends
     */
    public PerformanceTrendsDto(
            List<String> dates,
            Map<String, Double> averageResponseTimes,
            Map<String, Double> successRates,
            int totalDataPoints) {
        this.dates = new java.util.ArrayList<>(dates); // Defensive copy
        this.averageResponseTimes = new java.util.HashMap<>(averageResponseTimes); // Defensive copy
        this.successRates = new java.util.HashMap<>(successRates); // Defensive copy
        this.totalDataPoints = totalDataPoints;
    }
    
    /**
     * Constructor for empty trends (when no data exists)
     */
    public PerformanceTrendsDto() {
        this.dates = List.of();
        this.averageResponseTimes = Map.of();
        this.successRates = Map.of();
        this.totalDataPoints = 0;
    }
    
    // Getters
    public List<String> getDates() {
        return dates;
    }
    
    public Map<String, Double> getAverageResponseTimes() {
        return averageResponseTimes;
    }
    
    public Map<String, Double> getSuccessRates() {
        return successRates;
    }
    
    public int getTotalDataPoints() {
        return totalDataPoints;
    }
    
    /**
     * Check if trends data is empty
     */
    public boolean isEmpty() {
        return dates.isEmpty() || totalDataPoints == 0;
    }
    
    /**
     * Get the number of data points (dates)
     */
    public int getDateCount() {
        return dates.size();
    }
    
    @Override
    public String toString() {
        return "PerformanceTrendsDto{" +
                "dates=" + dates.size() + " dates" +
                ", averageResponseTimes=" + averageResponseTimes.size() + " entries" +
                ", successRates=" + successRates.size() + " entries" +
                ", totalDataPoints=" + totalDataPoints +
                '}';
    }
}

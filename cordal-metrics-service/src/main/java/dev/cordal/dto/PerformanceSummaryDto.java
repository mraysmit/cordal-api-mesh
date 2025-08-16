package dev.cordal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Type-safe DTO for performance summary statistics
 * Replaces the unsafe Map<String, Object> return type
 */
public class PerformanceSummaryDto {
    
    @JsonProperty("totalTests")
    private final int totalTests;
    
    @JsonProperty("averageResponseTime")
    private final double averageResponseTime;
    
    @JsonProperty("successRate")
    private final double successRate;
    
    @JsonProperty("testTypes")
    private final List<String> testTypes;
    
    @JsonProperty("testTypeDistribution")
    private final Map<String, Long> testTypeDistribution;
    
    @JsonProperty("lastTestTime")
    private final LocalDateTime lastTestTime;
    
    /**
     * Constructor for performance summary
     */
    public PerformanceSummaryDto(
            int totalTests,
            double averageResponseTime,
            double successRate,
            List<String> testTypes,
            Map<String, Long> testTypeDistribution,
            LocalDateTime lastTestTime) {
        this.totalTests = totalTests;
        this.averageResponseTime = Math.round(averageResponseTime * 100.0) / 100.0;
        this.successRate = Math.round(successRate * 100.0) / 100.0;
        this.testTypes = new java.util.ArrayList<>(testTypes); // Defensive copy
        this.testTypeDistribution = new java.util.HashMap<>(testTypeDistribution); // Defensive copy
        this.lastTestTime = lastTestTime;
    }
    
    /**
     * Constructor for empty summary (when no metrics exist)
     */
    public PerformanceSummaryDto() {
        this.totalTests = 0;
        this.averageResponseTime = 0.0;
        this.successRate = 0.0;
        this.testTypes = List.of();
        this.testTypeDistribution = Map.of();
        this.lastTestTime = null;
    }
    
    // Getters
    public int getTotalTests() {
        return totalTests;
    }
    
    public double getAverageResponseTime() {
        return averageResponseTime;
    }
    
    public double getSuccessRate() {
        return successRate;
    }
    
    public List<String> getTestTypes() {
        return testTypes;
    }
    
    public Map<String, Long> getTestTypeDistribution() {
        return testTypeDistribution;
    }
    
    public LocalDateTime getLastTestTime() {
        return lastTestTime;
    }
    
    @Override
    public String toString() {
        return "PerformanceSummaryDto{" +
                "totalTests=" + totalTests +
                ", averageResponseTime=" + averageResponseTime +
                ", successRate=" + successRate +
                ", testTypes=" + testTypes.size() + " types" +
                ", testTypeDistribution=" + testTypeDistribution.size() + " entries" +
                ", lastTestTime=" + (lastTestTime != null ? lastTestTime.toString() : "none") +
                '}';
    }
}

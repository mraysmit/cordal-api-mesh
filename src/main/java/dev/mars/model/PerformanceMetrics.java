package dev.mars.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Model representing performance test metrics
 */
public class PerformanceMetrics {
    private Long id;
    private String testName;
    private String testType;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private Integer totalRequests;
    private Long totalTimeMs;
    private Double averageResponseTimeMs;
    private Integer concurrentThreads;
    private Integer requestsPerThread;
    private Integer pageSize;
    private Long memoryUsageBytes;
    private Long memoryIncreaseBytes;
    private Boolean testPassed;
    private String additionalMetrics; // JSON string for flexible metrics
    
    // Default constructor
    public PerformanceMetrics() {
        this.timestamp = LocalDateTime.now();
    }
    
    // Constructor for basic metrics
    public PerformanceMetrics(String testName, String testType) {
        this();
        this.testName = testName;
        this.testType = testType;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTestName() {
        return testName;
    }
    
    public void setTestName(String testName) {
        this.testName = testName;
    }
    
    public String getTestType() {
        return testType;
    }
    
    public void setTestType(String testType) {
        this.testType = testType;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Integer getTotalRequests() {
        return totalRequests;
    }
    
    public void setTotalRequests(Integer totalRequests) {
        this.totalRequests = totalRequests;
    }
    
    public Long getTotalTimeMs() {
        return totalTimeMs;
    }
    
    public void setTotalTimeMs(Long totalTimeMs) {
        this.totalTimeMs = totalTimeMs;
    }
    
    public Double getAverageResponseTimeMs() {
        return averageResponseTimeMs;
    }
    
    public void setAverageResponseTimeMs(Double averageResponseTimeMs) {
        this.averageResponseTimeMs = averageResponseTimeMs;
    }
    
    public Integer getConcurrentThreads() {
        return concurrentThreads;
    }
    
    public void setConcurrentThreads(Integer concurrentThreads) {
        this.concurrentThreads = concurrentThreads;
    }
    
    public Integer getRequestsPerThread() {
        return requestsPerThread;
    }
    
    public void setRequestsPerThread(Integer requestsPerThread) {
        this.requestsPerThread = requestsPerThread;
    }
    
    public Integer getPageSize() {
        return pageSize;
    }
    
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
    
    public Long getMemoryUsageBytes() {
        return memoryUsageBytes;
    }
    
    public void setMemoryUsageBytes(Long memoryUsageBytes) {
        this.memoryUsageBytes = memoryUsageBytes;
    }
    
    public Long getMemoryIncreaseBytes() {
        return memoryIncreaseBytes;
    }
    
    public void setMemoryIncreaseBytes(Long memoryIncreaseBytes) {
        this.memoryIncreaseBytes = memoryIncreaseBytes;
    }
    
    public Boolean getTestPassed() {
        return testPassed;
    }
    
    public void setTestPassed(Boolean testPassed) {
        this.testPassed = testPassed;
    }
    
    public String getAdditionalMetrics() {
        return additionalMetrics;
    }
    
    public void setAdditionalMetrics(String additionalMetrics) {
        this.additionalMetrics = additionalMetrics;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PerformanceMetrics that = (PerformanceMetrics) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(testName, that.testName) &&
               Objects.equals(timestamp, that.timestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, testName, timestamp);
    }
    
    @Override
    public String toString() {
        return "PerformanceMetrics{" +
               "id=" + id +
               ", testName='" + testName + '\'' +
               ", testType='" + testType + '\'' +
               ", timestamp=" + timestamp +
               ", totalRequests=" + totalRequests +
               ", totalTimeMs=" + totalTimeMs +
               ", averageResponseTimeMs=" + averageResponseTimeMs +
               ", testPassed=" + testPassed +
               '}';
    }
}

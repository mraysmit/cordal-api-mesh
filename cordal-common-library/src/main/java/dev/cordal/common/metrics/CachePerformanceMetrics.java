package dev.cordal.common.metrics;

import com.fasterxml.jackson.annotation.JsonFormat;
import dev.cordal.common.cache.CacheStatistics;
import dev.cordal.common.model.PerformanceMetrics;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Extended performance metrics that includes cache statistics
 */
public class CachePerformanceMetrics extends PerformanceMetrics {
    
    // Cache-specific metrics
    private Boolean cacheEnabled;
    private Boolean cacheHit;
    private String cacheStrategy;
    private Long cacheResponseTimeMs;
    private Long databaseResponseTimeMs;
    private String cacheName;
    private String cacheKey;
    private Integer cacheTtlSeconds;
    
    // Cache statistics snapshot
    private Long cacheHitCount;
    private Long cacheMissCount;
    private Long cacheEvictionCount;
    private Long cacheSize;
    private Double cacheHitRate;
    private Double cacheMissRate;

    // Default constructor
    public CachePerformanceMetrics() {
        super();
        this.setTestType("CACHE_QUERY");
    }

    // Constructor with basic cache info
    public CachePerformanceMetrics(String testName, boolean cacheHit, String cacheName) {
        this();
        this.setTestName(testName);
        this.cacheHit = cacheHit;
        this.cacheName = cacheName;
        this.cacheEnabled = true;
    }

    /**
     * Create cache performance metrics from regular performance metrics and cache info
     */
    public static CachePerformanceMetrics fromPerformanceMetrics(
            PerformanceMetrics baseMetrics, 
            boolean cacheHit, 
            String cacheName,
            CacheStatistics cacheStats) {
        
        CachePerformanceMetrics cacheMetrics = new CachePerformanceMetrics();
        
        // Copy base metrics
        cacheMetrics.setId(baseMetrics.getId());
        cacheMetrics.setTestName(baseMetrics.getTestName());
        cacheMetrics.setTestType("CACHE_QUERY");
        cacheMetrics.setTimestamp(baseMetrics.getTimestamp());
        cacheMetrics.setTotalRequests(baseMetrics.getTotalRequests());
        cacheMetrics.setTotalTimeMs(baseMetrics.getTotalTimeMs());
        cacheMetrics.setAverageResponseTimeMs(baseMetrics.getAverageResponseTimeMs());
        cacheMetrics.setConcurrentThreads(baseMetrics.getConcurrentThreads());
        cacheMetrics.setRequestsPerThread(baseMetrics.getRequestsPerThread());
        cacheMetrics.setPageSize(baseMetrics.getPageSize());
        cacheMetrics.setMemoryUsageBytes(baseMetrics.getMemoryUsageBytes());
        cacheMetrics.setMemoryIncreaseBytes(baseMetrics.getMemoryIncreaseBytes());
        cacheMetrics.setTestPassed(baseMetrics.getTestPassed());
        cacheMetrics.setAdditionalMetrics(baseMetrics.getAdditionalMetrics());
        
        // Set cache-specific metrics
        cacheMetrics.setCacheEnabled(true);
        cacheMetrics.setCacheHit(cacheHit);
        cacheMetrics.setCacheName(cacheName);
        
        if (cacheStats != null) {
            cacheMetrics.setCacheHitCount(cacheStats.getHitCount());
            cacheMetrics.setCacheMissCount(cacheStats.getMissCount());
            cacheMetrics.setCacheEvictionCount(cacheStats.getEvictionCount());
            cacheMetrics.setCacheSize(cacheStats.getSize());
            cacheMetrics.setCacheHitRate(cacheStats.getHitRate());
            cacheMetrics.setCacheMissRate(cacheStats.getMissRate());
        }
        
        return cacheMetrics;
    }

    // Cache-specific getters and setters
    public Boolean getCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(Boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public Boolean getCacheHit() {
        return cacheHit;
    }

    public void setCacheHit(Boolean cacheHit) {
        this.cacheHit = cacheHit;
    }

    public String getCacheStrategy() {
        return cacheStrategy;
    }

    public void setCacheStrategy(String cacheStrategy) {
        this.cacheStrategy = cacheStrategy;
    }

    public Long getCacheResponseTimeMs() {
        return cacheResponseTimeMs;
    }

    public void setCacheResponseTimeMs(Long cacheResponseTimeMs) {
        this.cacheResponseTimeMs = cacheResponseTimeMs;
    }

    public Long getDatabaseResponseTimeMs() {
        return databaseResponseTimeMs;
    }

    public void setDatabaseResponseTimeMs(Long databaseResponseTimeMs) {
        this.databaseResponseTimeMs = databaseResponseTimeMs;
    }

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    public Integer getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(Integer cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public Long getCacheHitCount() {
        return cacheHitCount;
    }

    public void setCacheHitCount(Long cacheHitCount) {
        this.cacheHitCount = cacheHitCount;
    }

    public Long getCacheMissCount() {
        return cacheMissCount;
    }

    public void setCacheMissCount(Long cacheMissCount) {
        this.cacheMissCount = cacheMissCount;
    }

    public Long getCacheEvictionCount() {
        return cacheEvictionCount;
    }

    public void setCacheEvictionCount(Long cacheEvictionCount) {
        this.cacheEvictionCount = cacheEvictionCount;
    }

    public Long getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(Long cacheSize) {
        this.cacheSize = cacheSize;
    }

    public Double getCacheHitRate() {
        return cacheHitRate;
    }

    public void setCacheHitRate(Double cacheHitRate) {
        this.cacheHitRate = cacheHitRate;
    }

    public Double getCacheMissRate() {
        return cacheMissRate;
    }

    public void setCacheMissRate(Double cacheMissRate) {
        this.cacheMissRate = cacheMissRate;
    }

    /**
     * Calculate cache performance improvement
     * @return percentage improvement in response time due to caching, or null if not applicable
     */
    public Double getCachePerformanceImprovement() {
        if (cacheResponseTimeMs != null && databaseResponseTimeMs != null && databaseResponseTimeMs > 0) {
            double improvement = ((double) (databaseResponseTimeMs - cacheResponseTimeMs) / databaseResponseTimeMs) * 100;
            return Math.max(0, improvement); // Don't return negative improvements
        }
        return null;
    }

    /**
     * Check if this represents a cache hit
     */
    public boolean isCacheHit() {
        return Boolean.TRUE.equals(cacheHit);
    }

    /**
     * Check if caching is enabled for this query
     */
    public boolean isCacheEnabled() {
        return Boolean.TRUE.equals(cacheEnabled);
    }

    @Override
    public String toString() {
        return "CachePerformanceMetrics{" +
               "testName='" + getTestName() + '\'' +
               ", cacheEnabled=" + cacheEnabled +
               ", cacheHit=" + cacheHit +
               ", cacheStrategy='" + cacheStrategy + '\'' +
               ", cacheResponseTimeMs=" + cacheResponseTimeMs +
               ", databaseResponseTimeMs=" + databaseResponseTimeMs +
               ", cacheName='" + cacheName + '\'' +
               ", cacheHitRate=" + cacheHitRate +
               ", timestamp=" + getTimestamp() +
               '}';
    }
}

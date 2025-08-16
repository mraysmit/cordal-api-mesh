package dev.cordal.service;

import dev.cordal.common.dto.PagedResponse;
import dev.cordal.common.model.PerformanceMetrics;
import dev.cordal.dto.PerformanceSummaryDto;
import dev.cordal.dto.PerformanceTrendsDto;
import dev.cordal.repository.PerformanceMetricsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing performance metrics
 */
@Singleton
public class PerformanceMetricsService {
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMetricsService.class);
    
    private final PerformanceMetricsRepository repository;
    
    @Inject
    public PerformanceMetricsService(PerformanceMetricsRepository repository) {
        this.repository = repository;
    }
    
    /**
     * Save performance metrics
     */
    public PerformanceMetrics saveMetrics(PerformanceMetrics metrics) {
        logger.info("Saving performance metrics for test: {}", metrics.getTestName());
        return repository.save(metrics);
    }
    
    /**
     * Get performance metrics by ID
     */
    public Optional<PerformanceMetrics> getMetricsById(Long id) {
        return repository.findById(id);
    }
    
    /**
     * Get all performance metrics with pagination
     */
    public PagedResponse<PerformanceMetrics> getAllMetrics(int page, int size) {
        List<PerformanceMetrics> metrics = repository.findAll(page, size);
        long totalElements = repository.count();
        
        return new PagedResponse<>(metrics, page, size, totalElements);
    }
    
    /**
     * Get performance metrics by test type
     */
    public PagedResponse<PerformanceMetrics> getMetricsByTestType(String testType, int page, int size) {
        List<PerformanceMetrics> metrics = repository.findByTestType(testType, page, size);
        long totalElements = repository.count(); // Note: This could be optimized to count by test type
        
        return new PagedResponse<>(metrics, page, size, totalElements);
    }
    
    /**
     * Get performance metrics within date range
     */
    public PagedResponse<PerformanceMetrics> getMetricsByDateRange(
            LocalDateTime startDate, LocalDateTime endDate, int page, int size) {
        List<PerformanceMetrics> metrics = repository.findByDateRange(startDate, endDate, page, size);
        long totalElements = repository.count(); // Note: This could be optimized to count by date range
        
        return new PagedResponse<>(metrics, page, size, totalElements);
    }
    
    /**
     * Get available test types
     */
    public List<String> getAvailableTestTypes() {
        return repository.getDistinctTestTypes();
    }
    
    /**
     * Get performance summary statistics
     * Returns type-safe DTO instead of unsafe Map<String, Object>
     */
    public PerformanceSummaryDto getPerformanceSummary() {
        List<PerformanceMetrics> recentMetrics = repository.findAll(0, 100); // Get last 100 records

        if (recentMetrics.isEmpty()) {
            logger.debug("No performance metrics found, returning empty summary");
            return new PerformanceSummaryDto(); // Empty summary
        }

        // Calculate summary statistics
        double avgResponseTime = recentMetrics.stream()
            .filter(m -> m.getAverageResponseTimeMs() != null)
            .mapToDouble(PerformanceMetrics::getAverageResponseTimeMs)
            .average()
            .orElse(0.0);

        long passedTests = recentMetrics.stream()
            .filter(m -> m.getTestPassed() != null && m.getTestPassed())
            .count();

        double successRate = (double) passedTests / recentMetrics.size() * 100;

        Map<String, Long> testTypeDistribution = recentMetrics.stream()
            .collect(Collectors.groupingBy(
                PerformanceMetrics::getTestType,
                Collectors.counting()
            ));

        List<String> testTypes = getAvailableTestTypes();
        LocalDateTime lastTestTime = recentMetrics.get(0).getTimestamp();

        logger.debug("Generated performance summary: {} tests, {:.2f}ms avg response time, {:.1f}% success rate",
                    recentMetrics.size(), avgResponseTime, successRate);

        return new PerformanceSummaryDto(
            recentMetrics.size(),
            avgResponseTime,
            successRate,
            testTypes,
            testTypeDistribution,
            lastTestTime
        );
    }
    
    /**
     * Get performance trends for dashboard charts
     * Returns type-safe DTO instead of unsafe Map<String, Object>
     */
    public PerformanceTrendsDto getPerformanceTrends(String testType, int days) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);

        logger.debug("Retrieving performance trends for testType='{}' over {} days", testType, days);

        List<PerformanceMetrics> metrics = repository.findByDateRange(startDate, endDate, 0, 1000);

        if (testType != null && !testType.isEmpty()) {
            metrics = metrics.stream()
                .filter(m -> testType.equals(m.getTestType()))
                .collect(Collectors.toList());
            logger.debug("Filtered to {} metrics for test type '{}'", metrics.size(), testType);
        }

        if (metrics.isEmpty()) {
            logger.debug("No metrics found for the specified criteria, returning empty trends");
            return new PerformanceTrendsDto(); // Empty trends
        }

        // Group by date for trend analysis
        Map<String, List<PerformanceMetrics>> dailyMetrics = metrics.stream()
            .collect(Collectors.groupingBy(
                m -> m.getTimestamp().toLocalDate().toString()
            ));

        // Calculate daily averages
        Map<String, Double> dailyAverageResponseTimes = dailyMetrics.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream()
                    .filter(m -> m.getAverageResponseTimeMs() != null)
                    .mapToDouble(PerformanceMetrics::getAverageResponseTimeMs)
                    .average()
                    .orElse(0.0)
            ));

        Map<String, Double> dailySuccessRates = dailyMetrics.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    List<PerformanceMetrics> dayMetrics = entry.getValue();
                    long passed = dayMetrics.stream()
                        .filter(m -> m.getTestPassed() != null && m.getTestPassed())
                        .count();
                    return (double) passed / dayMetrics.size() * 100;
                }
            ));

        List<String> sortedDates = dailyAverageResponseTimes.keySet().stream()
            .sorted()
            .collect(Collectors.toList());

        logger.debug("Generated performance trends: {} dates, {} total data points",
                    sortedDates.size(), metrics.size());

        return new PerformanceTrendsDto(
            sortedDates,
            dailyAverageResponseTimes,
            dailySuccessRates,
            metrics.size()
        );
    }
    
    /**
     * Create a performance metrics builder for easier test integration
     */
    public static class MetricsBuilder {
        private final PerformanceMetrics metrics;
        
        public MetricsBuilder(String testName, String testType) {
            this.metrics = new PerformanceMetrics(testName, testType);
        }
        
        public MetricsBuilder totalRequests(int totalRequests) {
            metrics.setTotalRequests(totalRequests);
            return this;
        }
        
        public MetricsBuilder totalTime(long totalTimeMs) {
            metrics.setTotalTimeMs(totalTimeMs);
            if (metrics.getTotalRequests() != null && metrics.getTotalRequests() > 0) {
                metrics.setAverageResponseTimeMs((double) totalTimeMs / metrics.getTotalRequests());
            }
            return this;
        }
        
        public MetricsBuilder averageResponseTime(double averageResponseTimeMs) {
            metrics.setAverageResponseTimeMs(averageResponseTimeMs);
            return this;
        }
        
        public MetricsBuilder concurrency(int threads, int requestsPerThread) {
            metrics.setConcurrentThreads(threads);
            metrics.setRequestsPerThread(requestsPerThread);
            return this;
        }
        
        public MetricsBuilder pageSize(int pageSize) {
            metrics.setPageSize(pageSize);
            return this;
        }
        
        public MetricsBuilder memory(long usageBytes, long increaseBytes) {
            metrics.setMemoryUsageBytes(usageBytes);
            metrics.setMemoryIncreaseBytes(increaseBytes);
            return this;
        }
        
        public MetricsBuilder testPassed(boolean passed) {
            metrics.setTestPassed(passed);
            return this;
        }
        
        public MetricsBuilder additionalMetrics(String jsonMetrics) {
            metrics.setAdditionalMetrics(jsonMetrics);
            return this;
        }
        
        public PerformanceMetrics build() {
            return metrics;
        }
    }
    
    /**
     * Create a new metrics builder
     */
    public static MetricsBuilder builder(String testName, String testType) {
        return new MetricsBuilder(testName, testType);
    }
}

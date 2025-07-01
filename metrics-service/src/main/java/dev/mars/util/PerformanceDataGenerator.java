package dev.mars.util;

import dev.mars.metrics.MetricsApplication;
import dev.mars.model.PerformanceMetrics;
import dev.mars.service.PerformanceMetricsService;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * Utility to generate sample performance data for dashboard demonstration
 */
public class PerformanceDataGenerator {
    
    public static void main(String[] args) {
        System.out.println("Generating sample performance data...");

        // Use the metrics database configuration
        System.setProperty("config.file", "application.yml");

        // Start the metrics application to get access to services
        MetricsApplication app = new MetricsApplication();
        app.start();
        
        try {
            PerformanceMetricsService metricsService = app.getInjector().getInstance(PerformanceMetricsService.class);
            Random random = new Random();
            
            // Generate data for the last 7 days
            LocalDateTime now = LocalDateTime.now();
            
            for (int day = 6; day >= 0; day--) {
                LocalDateTime testTime = now.minusDays(day);
                
                // Generate 3-5 tests per day
                int testsPerDay = 3 + random.nextInt(3);
                
                for (int test = 0; test < testsPerDay; test++) {
                    LocalDateTime testDateTime = testTime.plusHours(random.nextInt(24)).plusMinutes(random.nextInt(60));
                    
                    // Generate different types of tests
                    String[] testTypes = {"CONCURRENT", "SYNC", "ASYNC", "PAGINATION", "MEMORY"};
                    String testType = testTypes[random.nextInt(testTypes.length)];
                    
                    PerformanceMetrics metrics = createSampleMetrics(testType, testDateTime, random);
                    metricsService.saveMetrics(metrics);
                    
                    System.out.printf("Generated %s test for %s%n", testType, testDateTime);
                }
            }
            
            System.out.println("Sample performance data generated successfully!");
            
        } finally {
            app.stop();
        }
    }
    
    private static PerformanceMetrics createSampleMetrics(String testType, LocalDateTime timestamp, Random random) {
        PerformanceMetrics metrics = new PerformanceMetrics();
        metrics.setTimestamp(timestamp);
        metrics.setTestType(testType);
        
        switch (testType) {
            case "CONCURRENT":
                metrics.setTestName("Concurrent Requests Test");
                metrics.setTotalRequests(200);
                metrics.setTotalTimeMs(300L + random.nextInt(200)); // 300-500ms
                metrics.setAverageResponseTimeMs(metrics.getTotalTimeMs() / (double) metrics.getTotalRequests());
                metrics.setConcurrentThreads(10);
                metrics.setRequestsPerThread(20);
                metrics.setTestPassed(random.nextDouble() > 0.1); // 90% success rate
                break;
                
            case "SYNC":
                metrics.setTestName("Sync Performance Test");
                metrics.setTotalRequests(50);
                metrics.setTotalTimeMs(2000L + random.nextInt(1000)); // 2-3 seconds
                metrics.setAverageResponseTimeMs(metrics.getTotalTimeMs() / (double) metrics.getTotalRequests());
                metrics.setTestPassed(random.nextDouble() > 0.05); // 95% success rate
                break;
                
            case "ASYNC":
                metrics.setTestName("Async Performance Test");
                metrics.setTotalRequests(50);
                metrics.setTotalTimeMs(1500L + random.nextInt(800)); // 1.5-2.3 seconds
                metrics.setAverageResponseTimeMs(metrics.getTotalTimeMs() / (double) metrics.getTotalRequests());
                metrics.setTestPassed(random.nextDouble() > 0.05); // 95% success rate
                break;
                
            case "PAGINATION":
                metrics.setTestName("Pagination Performance Test");
                metrics.setTotalRequests(1);
                int[] pageSizes = {10, 50, 100, 500, 1000};
                int pageSize = pageSizes[random.nextInt(pageSizes.length)];
                metrics.setPageSize(pageSize);
                // Larger page sizes take longer
                long baseTime = 50 + (pageSize / 10);
                metrics.setTotalTimeMs(baseTime + random.nextInt(50));
                metrics.setAverageResponseTimeMs((double) metrics.getTotalTimeMs());
                metrics.setTestPassed(random.nextDouble() > 0.02); // 98% success rate
                break;
                
            case "MEMORY":
                metrics.setTestName("Memory Usage Test");
                metrics.setTotalRequests(100);
                metrics.setTotalTimeMs(100L + random.nextInt(50)); // 100-150ms
                metrics.setAverageResponseTimeMs(metrics.getTotalTimeMs() / (double) metrics.getTotalRequests());
                metrics.setMemoryUsageBytes(15000000L + random.nextInt(5000000)); // 15-20MB
                metrics.setMemoryIncreaseBytes(100000L + random.nextInt(200000)); // 100-300KB
                metrics.setTestPassed(random.nextDouble() > 0.05); // 95% success rate
                break;
        }
        
        return metrics;
    }
}

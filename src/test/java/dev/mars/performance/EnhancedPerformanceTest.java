package dev.mars.performance;

import dev.mars.Application;
import dev.mars.model.PerformanceMetrics;
import dev.mars.service.PerformanceMetricsService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

/**
 * Enhanced performance tests that save metrics to the database for dashboard visualization
 */
class EnhancedPerformanceTest {

    private Application application;
    private String baseUrl;
    private PerformanceMetricsService metricsService;

    @BeforeEach
    void setUp() {
        System.setProperty("config.file", "application-test.yml");
        application = new Application();
        application.start();
        baseUrl = "http://localhost:" + application.getPort();
        
        // Get the metrics service from the injector
        metricsService = application.getInjector().getInstance(PerformanceMetricsService.class);
    }

    @AfterEach
    void tearDown() {
        if (application != null) {
            application.stop();
            application = null;
        }
        System.clearProperty("config.file");
    }

    @Test
    void testConcurrentRequestsWithMetrics() throws Exception {
        int numberOfThreads = 10;
        int requestsPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        OkHttpClient httpClient = new OkHttpClient();

        long startTime = System.currentTimeMillis();
        boolean testPassed = true;

        try {
            CompletableFuture<Void>[] futures = IntStream.range(0, numberOfThreads)
                .mapToObj(threadIndex -> CompletableFuture.runAsync(() -> {
                    for (int i = 0; i < requestsPerThread; i++) {
                        try {
                            Request request = new Request.Builder()
                                    .url(baseUrl + "/api/generic/stock-trades?page=0&size=10")
                                    .build();
                            try (Response response = httpClient.newCall(request).execute()) {
                                assertThat(response.code()).isEqualTo(200);
                            }
                        } catch (Exception e) {
                            throw new RuntimeException("Request failed", e);
                        }
                    }
                }, executor))
                .toArray(CompletableFuture[]::new);

            CompletableFuture.allOf(futures).join();

            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            int totalRequests = numberOfThreads * requestsPerThread;
            double averageResponseTime = (double) totalTime / totalRequests;

            // Assert that all requests completed within reasonable time
            assertThat(totalTime).isLessThan(30000); // 30 seconds max

            System.out.printf("Completed %d concurrent requests in %d ms%n", totalRequests, totalTime);
            System.out.printf("Average response time: %.2f ms%n", averageResponseTime);

            // Save metrics to database
            PerformanceMetrics metrics = PerformanceMetricsService.builder("Concurrent Requests Test", "CONCURRENT")
                .totalRequests(totalRequests)
                .totalTime(totalTime)
                .averageResponseTime(averageResponseTime)
                .concurrency(numberOfThreads, requestsPerThread)
                .testPassed(testPassed)
                .build();

            metricsService.saveMetrics(metrics);

        } catch (Exception e) {
            testPassed = false;
            throw e;
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
    }

    @Test
    void testAsyncVsSyncPerformanceWithMetrics() throws Exception {
        int numberOfRequests = 50;
        OkHttpClient httpClient = new OkHttpClient();
        boolean testPassed = true;

        try {
            // Test synchronous requests
            long syncStartTime = System.currentTimeMillis();
            for (int i = 0; i < numberOfRequests; i++) {
                Request request = new Request.Builder()
                        .url(baseUrl + "/api/generic/stock-trades?page=0&size=10&async=false")
                        .build();
                try (Response response = httpClient.newCall(request).execute()) {
                    assertThat(response.code()).isEqualTo(200);
                }
            }
            long syncEndTime = System.currentTimeMillis();
            long syncTotalTime = syncEndTime - syncStartTime;
            double syncAvgResponseTime = (double) syncTotalTime / numberOfRequests;

            // Test asynchronous requests
            long asyncStartTime = System.currentTimeMillis();
            for (int i = 0; i < numberOfRequests; i++) {
                Request request = new Request.Builder()
                        .url(baseUrl + "/api/generic/stock-trades?page=0&size=10&async=true")
                        .build();
                try (Response response = httpClient.newCall(request).execute()) {
                    assertThat(response.code()).isEqualTo(200);
                }
            }
            long asyncEndTime = System.currentTimeMillis();
            long asyncTotalTime = asyncEndTime - asyncStartTime;
            double asyncAvgResponseTime = (double) asyncTotalTime / numberOfRequests;

            System.out.printf("Sync requests: %d ms (avg: %.2f ms)%n", syncTotalTime, syncAvgResponseTime);
            System.out.printf("Async requests: %d ms (avg: %.2f ms)%n", asyncTotalTime, asyncAvgResponseTime);

            // Both should complete within reasonable time
            assertThat(syncTotalTime).isLessThan(10000); // 10 seconds max
            assertThat(asyncTotalTime).isLessThan(10000); // 10 seconds max

            // Save sync metrics
            PerformanceMetrics syncMetrics = PerformanceMetricsService.builder("Sync Performance Test", "SYNC")
                .totalRequests(numberOfRequests)
                .totalTime(syncTotalTime)
                .averageResponseTime(syncAvgResponseTime)
                .testPassed(testPassed)
                .build();

            metricsService.saveMetrics(syncMetrics);

            // Save async metrics
            PerformanceMetrics asyncMetrics = PerformanceMetricsService.builder("Async Performance Test", "ASYNC")
                .totalRequests(numberOfRequests)
                .totalTime(asyncTotalTime)
                .averageResponseTime(asyncAvgResponseTime)
                .testPassed(testPassed)
                .build();

            metricsService.saveMetrics(asyncMetrics);

        } catch (Exception e) {
            testPassed = false;
            throw e;
        }
    }

    @Test
    void testPaginationPerformanceWithMetrics() throws Exception {
        // Use page sizes within the configured maxSize limit (100)
        int[] pageSizes = {10, 20, 50, 75, 100};
        OkHttpClient httpClient = new OkHttpClient();

        for (int pageSize : pageSizes) {
            boolean testPassed = true;
            
            try {
                long startTime = System.currentTimeMillis();

                Request request = new Request.Builder()
                        .url(baseUrl + "/api/generic/stock-trades?page=0&size=" + pageSize)
                        .build();
                try (Response response = httpClient.newCall(request).execute()) {
                    assertThat(response.code()).isEqualTo(200);
                }

                long endTime = System.currentTimeMillis();
                long responseTime = endTime - startTime;

                System.out.printf("Page size %d: %d ms%n", pageSize, responseTime);

                // Larger page sizes should still respond within reasonable time
                assertThat(responseTime).isLessThan(5000); // 5 seconds max

                // Save metrics
                PerformanceMetrics metrics = PerformanceMetricsService.builder("Pagination Performance Test", "PAGINATION")
                    .totalRequests(1)
                    .totalTime(responseTime)
                    .averageResponseTime((double) responseTime)
                    .pageSize(pageSize)
                    .testPassed(testPassed)
                    .build();

                metricsService.saveMetrics(metrics);

            } catch (Exception e) {
                testPassed = false;
                
                // Still save failed test metrics
                PerformanceMetrics metrics = PerformanceMetricsService.builder("Pagination Performance Test", "PAGINATION")
                    .pageSize(pageSize)
                    .testPassed(testPassed)
                    .build();

                metricsService.saveMetrics(metrics);
                throw e;
            }
        }
    }

    @Test
    void testMemoryUsageWithMetrics() throws Exception {
        Runtime runtime = Runtime.getRuntime();
        OkHttpClient httpClient = new OkHttpClient();
        boolean testPassed = true;

        try {
            // Force garbage collection and get baseline memory
            System.gc();
            long baselineMemory = runtime.totalMemory() - runtime.freeMemory();

            long startTime = System.currentTimeMillis();

            // Make multiple requests to potentially cause memory issues
            for (int i = 0; i < 100; i++) {
                Request request = new Request.Builder()
                        .url(baseUrl + "/api/generic/stock-trades?page=0&size=100")
                        .build();
                try (Response response = httpClient.newCall(request).execute()) {
                    assertThat(response.code()).isEqualTo(200);
                    // Consume the response to ensure it's processed
                    response.body().string();
                }
            }

            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;

            // Force garbage collection and check memory
            System.gc();
            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryIncrease = finalMemory - baselineMemory;

            System.out.printf("Baseline memory: %d bytes%n", baselineMemory);
            System.out.printf("Final memory: %d bytes%n", finalMemory);
            System.out.printf("Memory increase: %d bytes%n", memoryIncrease);

            // Memory increase should be reasonable
            assertThat(memoryIncrease).isLessThan(50 * 1024 * 1024); // 50MB max increase

            // Save metrics
            PerformanceMetrics metrics = PerformanceMetricsService.builder("Memory Usage Test", "MEMORY")
                .totalRequests(100)
                .totalTime(totalTime)
                .averageResponseTime((double) totalTime / 100)
                .memory(finalMemory, memoryIncrease)
                .testPassed(testPassed)
                .build();

            metricsService.saveMetrics(metrics);

        } catch (Exception e) {
            testPassed = false;
            throw e;
        }
    }
}

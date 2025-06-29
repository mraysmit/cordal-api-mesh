package dev.mars.performance;

import dev.mars.Application;
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
 * Performance tests for the Stock Trade API
 * These tests are resource intensive and measure API performance
 */
class StockTradePerformanceTest {

    private Application application;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        System.setProperty("config.file", "application-test.yml");
        application = new Application();
        application.start();
        baseUrl = "http://localhost:" + application.getPort();
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
    void testConcurrentRequests() throws Exception {
        int numberOfThreads = 10;
        int requestsPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        OkHttpClient httpClient = new OkHttpClient();

        long startTime = System.currentTimeMillis();

        CompletableFuture<Void>[] futures = IntStream.range(0, numberOfThreads)
            .mapToObj(threadIndex -> CompletableFuture.runAsync(() -> {
                for (int i = 0; i < requestsPerThread; i++) {
                    try {
                        Request request = new Request.Builder()
                                .url(baseUrl + "/api/stock-trades?page=0&size=10")
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

        System.out.printf("Completed %d concurrent requests in %d ms%n", totalRequests, totalTime);
        System.out.printf("Average response time: %.2f ms%n", (double) totalTime / totalRequests);

        // Assert that all requests completed within reasonable time (adjust as needed)
        assertThat(totalTime).isLessThan(30000); // 30 seconds max

        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    @Test
    void testAsyncVsSyncPerformance() throws Exception {
        int numberOfRequests = 50;
        OkHttpClient httpClient = new OkHttpClient();

        // Test synchronous requests
        long syncStartTime = System.currentTimeMillis();
        for (int i = 0; i < numberOfRequests; i++) {
            Request request = new Request.Builder()
                    .url(baseUrl + "/api/stock-trades?page=0&size=10&async=false")
                    .build();
            try (Response response = httpClient.newCall(request).execute()) {
                assertThat(response.code()).isEqualTo(200);
            }
        }
        long syncEndTime = System.currentTimeMillis();
        long syncTotalTime = syncEndTime - syncStartTime;

        // Test asynchronous requests
        long asyncStartTime = System.currentTimeMillis();
        for (int i = 0; i < numberOfRequests; i++) {
            Request request = new Request.Builder()
                    .url(baseUrl + "/api/stock-trades?page=0&size=10&async=true")
                    .build();
            try (Response response = httpClient.newCall(request).execute()) {
                assertThat(response.code()).isEqualTo(200);
            }
        }
        long asyncEndTime = System.currentTimeMillis();
        long asyncTotalTime = asyncEndTime - asyncStartTime;

        System.out.printf("Sync requests: %d ms (avg: %.2f ms)%n",
                        syncTotalTime, (double) syncTotalTime / numberOfRequests);
        System.out.printf("Async requests: %d ms (avg: %.2f ms)%n",
                        asyncTotalTime, (double) asyncTotalTime / numberOfRequests);

        // Both should complete within reasonable time
        assertThat(syncTotalTime).isLessThan(10000); // 10 seconds max
        assertThat(asyncTotalTime).isLessThan(10000); // 10 seconds max
    }

    @Test
    void testLargePaginationPerformance() throws Exception {
        // Test performance with different page sizes
        int[] pageSizes = {10, 50, 100, 500, 1000};
        OkHttpClient httpClient = new OkHttpClient();

        for (int pageSize : pageSizes) {
            long startTime = System.currentTimeMillis();

            Request request = new Request.Builder()
                    .url(baseUrl + "/api/stock-trades?page=0&size=" + pageSize)
                    .build();
            try (Response response = httpClient.newCall(request).execute()) {
                assertThat(response.code()).isEqualTo(200);
            }

            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;

            System.out.printf("Page size %d: %d ms%n", pageSize, responseTime);

            // Larger page sizes should still respond within reasonable time
            assertThat(responseTime).isLessThan(5000); // 5 seconds max
        }
    }

    @Test
    void testMemoryUsage() throws Exception {
        Runtime runtime = Runtime.getRuntime();
        OkHttpClient httpClient = new OkHttpClient();

        // Force garbage collection and get baseline memory
        System.gc();
        long baselineMemory = runtime.totalMemory() - runtime.freeMemory();

        // Make multiple requests to potentially cause memory issues
        for (int i = 0; i < 100; i++) {
            Request request = new Request.Builder()
                    .url(baseUrl + "/api/stock-trades?page=0&size=100")
                    .build();
            try (Response response = httpClient.newCall(request).execute()) {
                assertThat(response.code()).isEqualTo(200);

                // Consume the response to ensure it's processed
                response.body().string();
            }
        }

        // Force garbage collection and check memory
        System.gc();
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - baselineMemory;

        System.out.printf("Baseline memory: %d bytes%n", baselineMemory);
        System.out.printf("Final memory: %d bytes%n", finalMemory);
        System.out.printf("Memory increase: %d bytes%n", memoryIncrease);

        // Memory increase should be reasonable (adjust threshold as needed)
        assertThat(memoryIncrease).isLessThan(50 * 1024 * 1024); // 50MB max increase
    }

    @Test
    void testDatabaseConnectionPoolPerformance() throws Exception {
        int numberOfThreads = 20;
        int requestsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        OkHttpClient httpClient = new OkHttpClient();

        long startTime = System.currentTimeMillis();

        CompletableFuture<Void>[] futures = IntStream.range(0, numberOfThreads)
            .mapToObj(threadIndex -> CompletableFuture.runAsync(() -> {
                for (int i = 0; i < requestsPerThread; i++) {
                    try {
                        // Mix different types of requests to test connection pool
                        Request request1 = new Request.Builder()
                                .url(baseUrl + "/api/stock-trades?page=0&size=5")
                                .build();
                        try (Response response1 = httpClient.newCall(request1).execute()) {
                            assertThat(response1.code()).isEqualTo(200);
                        }

                        Request request2 = new Request.Builder()
                                .url(baseUrl + "/api/health")
                                .build();
                        try (Response response2 = httpClient.newCall(request2).execute()) {
                            assertThat(response2.code()).isEqualTo(200);
                        }

                        Request request3 = new Request.Builder()
                                .url(baseUrl + "/api/stock-trades/symbol/AAPL?page=0&size=5")
                                .build();
                        try (Response response3 = httpClient.newCall(request3).execute()) {
                            assertThat(response3.code()).isEqualTo(200);
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
        int totalRequests = numberOfThreads * requestsPerThread * 3; // 3 requests per iteration

        System.out.printf("Completed %d database requests in %d ms%n", totalRequests, totalTime);
        System.out.printf("Average response time: %.2f ms%n", (double) totalTime / totalRequests);

        // All requests should complete without connection pool exhaustion
        assertThat(totalTime).isLessThan(60000); // 60 seconds max

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

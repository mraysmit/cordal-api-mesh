package dev.mars.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.mars.generic.GenericApiApplication;
import dev.mars.metrics.MetricsApplication;
import okhttp3.*;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance integration tests between services
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PerformanceIntegrationTest {

    private GenericApiApplication genericApiApp;
    private MetricsApplication metricsApp;
    private OkHttpClient httpClient;
    private ObjectMapper objectMapper;
    private ExecutorService executorService;

    private static final String GENERIC_API_BASE_URL = "http://localhost:18080";
    private static final String METRICS_API_BASE_URL = "http://localhost:18081";

    @BeforeAll
    void setUpAll() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        executorService = Executors.newFixedThreadPool(50);

        // Start applications
        startApplications();
        waitForApplicationsToStart();
    }

    @AfterAll
    void tearDownAll() {
        if (executorService != null) {
            executorService.shutdown();
        }
        if (genericApiApp != null) {
            genericApiApp.stop();
        }
        if (metricsApp != null) {
            metricsApp.stop();
        }
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }

    private void startApplications() {
        // Start Generic API Service first
        genericApiApp = new GenericApiApplication();
        Thread genericApiThread = new Thread(() -> {
            System.setProperty("config.file", "application-generic-api.yml");
            try {
                genericApiApp.start();
            } finally {
                System.clearProperty("config.file");
            }
        });
        genericApiThread.start();

        // Wait a moment to avoid system property conflicts
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Start Metrics Service
        metricsApp = new MetricsApplication();
        Thread metricsThread = new Thread(() -> {
            System.setProperty("config.file", "application-metrics.yml");
            try {
                metricsApp.start();
            } finally {
                System.clearProperty("config.file");
            }
        });
        metricsThread.start();
    }

    private void waitForApplicationsToStart() {
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> isServiceHealthy(GENERIC_API_BASE_URL) && isServiceHealthy(METRICS_API_BASE_URL));
    }

    private boolean isServiceHealthy(String baseUrl) {
        try {
            Request request = new Request.Builder()
                    .url(baseUrl + "/api/health")
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void shouldHandleConcurrentRequestsToGenericApi() throws InterruptedException {
        int numberOfRequests = 100;
        int concurrentThreads = 10;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Long> responseTimes = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numberOfRequests; i++) {
            executorService.submit(() -> {
                try {
                    long requestStart = System.currentTimeMillis();
                    Response response = makeRequest(GENERIC_API_BASE_URL + "/api/generic/stock-trades");
                    long requestEnd = System.currentTimeMillis();
                    
                    synchronized (responseTimes) {
                        responseTimes.add(requestEnd - requestStart);
                    }
                    
                    if (response.isSuccessful()) {
                        successCount.incrementAndGet();
                    } else {
                        errorCount.incrementAndGet();
                    }
                    response.close();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(60, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isGreaterThan((int)(numberOfRequests * 0.95)); // 95% success rate
        assertThat(errorCount.get()).isLessThan((int)(numberOfRequests * 0.05)); // Less than 5% errors

        // Calculate performance metrics
        double totalTime = (endTime - startTime) / 1000.0;
        double requestsPerSecond = numberOfRequests / totalTime;
        double averageResponseTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);

        System.out.println("Generic API Performance Results:");
        System.out.println("Total requests: " + numberOfRequests);
        System.out.println("Successful requests: " + successCount.get());
        System.out.println("Failed requests: " + errorCount.get());
        System.out.println("Total time: " + totalTime + " seconds");
        System.out.println("Requests per second: " + requestsPerSecond);
        System.out.println("Average response time: " + averageResponseTime + " ms");

        // Performance assertions
        assertThat(requestsPerSecond).isGreaterThan(10.0); // At least 10 RPS
        assertThat(averageResponseTime).isLessThan(1000.0); // Less than 1 second average
    }

    @Test
    void shouldHandleConcurrentRequestsToMetricsApi() throws InterruptedException {
        int numberOfRequests = 100;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Long> responseTimes = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numberOfRequests; i++) {
            final int requestId = i;
            executorService.submit(() -> {
                try {
                    long requestStart = System.currentTimeMillis();
                    Response response;
                    
                    // Mix of read and write operations
                    if (requestId % 10 == 0) {
                        // Create a metric (10% of requests)
                        String metricJson = String.format("""
                            {
                                "testName": "Performance Test %d",
                                "testType": "PERFORMANCE",
                                "duration": %d,
                                "success": true,
                                "memoryUsageBytes": %d,
                                "cpuUsagePercent": %.1f,
                                "requestsPerSecond": %.1f,
                                "responseTimeMs": %d,
                                "throughputMbps": %.1f
                            }
                            """, requestId, 
                            100 + (requestId % 1000),
                            1024000 + (requestId * 1000),
                            10.0 + (requestId % 50),
                            50.0 + (requestId % 100),
                            10 + (requestId % 100),
                            5.0 + (requestId % 20));
                        
                        response = makePostRequest(METRICS_API_BASE_URL + "/api/performance-metrics", metricJson);
                    } else {
                        // Read operations (90% of requests)
                        String[] endpoints = {
                            "/api/performance-metrics",
                            "/api/performance-metrics/summary",
                            "/api/performance-metrics/trends",
                            "/api/performance-metrics/test-types",
                            "/api/metrics/endpoints"
                        };
                        String endpoint = endpoints[requestId % endpoints.length];
                        response = makeRequest(METRICS_API_BASE_URL + endpoint);
                    }
                    
                    long requestEnd = System.currentTimeMillis();
                    
                    synchronized (responseTimes) {
                        responseTimes.add(requestEnd - requestStart);
                    }
                    
                    if (response.isSuccessful()) {
                        successCount.incrementAndGet();
                    } else {
                        errorCount.incrementAndGet();
                    }
                    response.close();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(60, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isGreaterThan((int)(numberOfRequests * 0.85)); // 85% success rate for integration tests

        // Calculate performance metrics
        double totalTime = (endTime - startTime) / 1000.0;
        double requestsPerSecond = numberOfRequests / totalTime;
        double averageResponseTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);

        System.out.println("Metrics API Performance Results:");
        System.out.println("Total requests: " + numberOfRequests);
        System.out.println("Successful requests: " + successCount.get());
        System.out.println("Failed requests: " + errorCount.get());
        System.out.println("Total time: " + totalTime + " seconds");
        System.out.println("Requests per second: " + requestsPerSecond);
        System.out.println("Average response time: " + averageResponseTime + " ms");

        // Performance assertions
        assertThat(requestsPerSecond).isGreaterThan(10.0); // At least 10 RPS
        assertThat(averageResponseTime).isLessThan(1000.0); // Less than 1 second average
    }

    @Test
    void shouldMaintainPerformanceUnderMixedLoad() throws InterruptedException {
        int totalRequests = 200;
        CountDownLatch latch = new CountDownLatch(totalRequests);
        AtomicInteger genericApiSuccess = new AtomicInteger(0);
        AtomicInteger metricsApiSuccess = new AtomicInteger(0);
        AtomicInteger totalErrors = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // Generate mixed load across both services
        for (int i = 0; i < totalRequests; i++) {
            final int requestId = i;
            executorService.submit(() -> {
                try {
                    Response response;
                    
                    if (requestId % 2 == 0) {
                        // Generic API requests
                        String[] genericEndpoints = {
                            "/api/generic/stock-trades",
                            "/api/generic/stock-trades/symbol/AAPL",
                            "/api/generic/config/validate",
                            "/api/generic/endpoints"
                        };
                        String endpoint = genericEndpoints[requestId % genericEndpoints.length];
                        response = makeRequest(GENERIC_API_BASE_URL + endpoint);
                        
                        if (response.isSuccessful()) {
                            genericApiSuccess.incrementAndGet();
                        } else {
                            totalErrors.incrementAndGet();
                        }
                    } else {
                        // Metrics API requests
                        String[] metricsEndpoints = {
                            "/api/performance-metrics",
                            "/api/performance-metrics/summary",
                            "/api/metrics/endpoints"
                        };
                        String endpoint = metricsEndpoints[requestId % metricsEndpoints.length];
                        response = makeRequest(METRICS_API_BASE_URL + endpoint);
                        
                        if (response.isSuccessful()) {
                            metricsApiSuccess.incrementAndGet();
                        } else {
                            totalErrors.incrementAndGet();
                        }
                    }
                    
                    response.close();
                } catch (Exception e) {
                    totalErrors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(90, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        assertThat(completed).isTrue();
        
        int totalSuccess = genericApiSuccess.get() + metricsApiSuccess.get();
        double successRate = (double) totalSuccess / totalRequests;

        System.out.println("Mixed Load Performance Results:");
        System.out.println("Total requests: " + totalRequests);
        System.out.println("Generic API successful: " + genericApiSuccess.get());
        System.out.println("Metrics API successful: " + metricsApiSuccess.get());
        System.out.println("Total errors: " + totalErrors.get());
        System.out.println("Success rate: " + (successRate * 100) + "%");
        System.out.println("Total time: " + ((endTime - startTime) / 1000.0) + " seconds");

        // Both services should maintain good performance under mixed load
        assertThat(successRate).isGreaterThan(0.95); // 95% success rate
        assertThat(genericApiSuccess.get()).isGreaterThan((int)(totalRequests * 0.4)); // At least 40% of half the requests
        assertThat(metricsApiSuccess.get()).isGreaterThan((int)(totalRequests * 0.4)); // At least 40% of half the requests
        
        // Verify both services are still healthy after load test
        assertThat(isServiceHealthy(GENERIC_API_BASE_URL)).isTrue();
        assertThat(isServiceHealthy(METRICS_API_BASE_URL)).isTrue();
    }

    private Response makeRequest(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        return httpClient.newCall(request).execute();
    }

    private Response makePostRequest(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        return httpClient.newCall(request).execute();
    }
}

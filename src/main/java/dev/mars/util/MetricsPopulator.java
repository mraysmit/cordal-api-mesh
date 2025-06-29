package dev.mars.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * Utility to populate metrics database via REST API
 */
public class MetricsPopulator {
    
    private static final String BASE_URL = "http://localhost:8080";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    public static void main(String[] args) {
        System.out.println("Populating metrics database via REST API...");
        
        try {
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
                    
                    String metricsJson = createSampleMetricsJson(testType, testDateTime, random);
                    
                    boolean success = postMetrics(metricsJson);
                    if (success) {
                        System.out.printf("✅ Generated %s test for %s%n", testType, testDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                    } else {
                        System.out.printf("❌ Failed to generate %s test for %s%n", testType, testDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                    }
                    
                    // Small delay to avoid overwhelming the server
                    Thread.sleep(100);
                }
            }
            
            System.out.println("\n🎉 Metrics database populated successfully!");
            System.out.println("📊 View dashboard at: " + BASE_URL + "/dashboard");
            
        } catch (Exception e) {
            System.err.println("❌ Error populating metrics: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static boolean postMetrics(String json) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/performance-metrics"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(30))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to post metrics: " + e.getMessage());
            return false;
        }
    }
    
    private static String createSampleMetricsJson(String testType, LocalDateTime timestamp, Random random) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"testType\":\"").append(testType).append("\",");
        json.append("\"timestamp\":\"").append(timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))).append("\",");
        
        switch (testType) {
            case "CONCURRENT":
                json.append("\"testName\":\"Concurrent Requests Test\",");
                json.append("\"totalRequests\":200,");
                long totalTime = 300L + random.nextInt(200); // 300-500ms
                json.append("\"totalTimeMs\":").append(totalTime).append(",");
                json.append("\"averageResponseTimeMs\":").append(totalTime / 200.0).append(",");
                json.append("\"concurrentThreads\":10,");
                json.append("\"requestsPerThread\":20,");
                json.append("\"testPassed\":").append(random.nextDouble() > 0.1);
                break;
                
            case "SYNC":
                json.append("\"testName\":\"Sync Performance Test\",");
                json.append("\"totalRequests\":50,");
                totalTime = 2000L + random.nextInt(1000); // 2-3 seconds
                json.append("\"totalTimeMs\":").append(totalTime).append(",");
                json.append("\"averageResponseTimeMs\":").append(totalTime / 50.0).append(",");
                json.append("\"testPassed\":").append(random.nextDouble() > 0.05);
                break;
                
            case "ASYNC":
                json.append("\"testName\":\"Async Performance Test\",");
                json.append("\"totalRequests\":50,");
                totalTime = 1500L + random.nextInt(800); // 1.5-2.3 seconds
                json.append("\"totalTimeMs\":").append(totalTime).append(",");
                json.append("\"averageResponseTimeMs\":").append(totalTime / 50.0).append(",");
                json.append("\"testPassed\":").append(random.nextDouble() > 0.05);
                break;
                
            case "PAGINATION":
                json.append("\"testName\":\"Pagination Performance Test\",");
                json.append("\"totalRequests\":1,");
                int[] pageSizes = {10, 50, 100, 500, 1000};
                int pageSize = pageSizes[random.nextInt(pageSizes.length)];
                json.append("\"pageSize\":").append(pageSize).append(",");
                // Larger page sizes take longer
                long baseTime = 50 + (pageSize / 10);
                totalTime = baseTime + random.nextInt(50);
                json.append("\"totalTimeMs\":").append(totalTime).append(",");
                json.append("\"averageResponseTimeMs\":").append((double) totalTime).append(",");
                json.append("\"testPassed\":").append(random.nextDouble() > 0.02);
                break;
                
            case "MEMORY":
                json.append("\"testName\":\"Memory Usage Test\",");
                json.append("\"totalRequests\":100,");
                totalTime = 100L + random.nextInt(50); // 100-150ms
                json.append("\"totalTimeMs\":").append(totalTime).append(",");
                json.append("\"averageResponseTimeMs\":").append(totalTime / 100.0).append(",");
                json.append("\"memoryUsageBytes\":").append(15000000L + random.nextInt(5000000)).append(",");
                json.append("\"memoryIncreaseBytes\":").append(100000L + random.nextInt(200000)).append(",");
                json.append("\"testPassed\":").append(random.nextDouble() > 0.05);
                break;
        }
        
        json.append("}");
        return json.toString();
    }
}

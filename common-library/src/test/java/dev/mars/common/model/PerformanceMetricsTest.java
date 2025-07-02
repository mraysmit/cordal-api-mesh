package dev.mars.common.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for PerformanceMetrics
 */
class PerformanceMetricsTest {

    @Test
    void shouldCreatePerformanceMetricsWithDefaults() {
        PerformanceMetrics metrics = new PerformanceMetrics();

        assertThat(metrics.getId()).isNull();
        assertThat(metrics.getTimestamp()).isNotNull();
        assertThat(metrics.getTimestamp()).isBefore(LocalDateTime.now().plusSeconds(1));
        assertThat(metrics.getTimestamp()).isAfter(LocalDateTime.now().minusSeconds(1));
    }

    @Test
    void shouldCreatePerformanceMetricsWithNameAndType() {
        PerformanceMetrics metrics = new PerformanceMetrics("Load Test", "LOAD_TEST");

        assertThat(metrics.getTestName()).isEqualTo("Load Test");
        assertThat(metrics.getTestType()).isEqualTo("LOAD_TEST");
        assertThat(metrics.getTimestamp()).isNotNull();
    }

    @Test
    void shouldSetAndGetAllProperties() {
        PerformanceMetrics metrics = new PerformanceMetrics();
        LocalDateTime testTime = LocalDateTime.now();

        metrics.setId(123L);
        metrics.setTestName("API Test");
        metrics.setTestType("API_REQUEST");
        metrics.setTimestamp(testTime);
        metrics.setTotalRequests(100);
        metrics.setTotalTimeMs(5000L);
        metrics.setAverageResponseTimeMs(50.5);
        metrics.setConcurrentThreads(10);
        metrics.setRequestsPerThread(10);
        metrics.setPageSize(20);
        metrics.setMemoryUsageBytes(1024000L);
        metrics.setMemoryIncreaseBytes(512000L);
        metrics.setTestPassed(true);
        metrics.setAdditionalMetrics("{\"custom\": \"data\"}");

        assertThat(metrics.getId()).isEqualTo(123L);
        assertThat(metrics.getTestName()).isEqualTo("API Test");
        assertThat(metrics.getTestType()).isEqualTo("API_REQUEST");
        assertThat(metrics.getTimestamp()).isEqualTo(testTime);
        assertThat(metrics.getTotalRequests()).isEqualTo(100);
        assertThat(metrics.getTotalTimeMs()).isEqualTo(5000L);
        assertThat(metrics.getAverageResponseTimeMs()).isEqualTo(50.5);
        assertThat(metrics.getConcurrentThreads()).isEqualTo(10);
        assertThat(metrics.getRequestsPerThread()).isEqualTo(10);
        assertThat(metrics.getPageSize()).isEqualTo(20);
        assertThat(metrics.getMemoryUsageBytes()).isEqualTo(1024000L);
        assertThat(metrics.getMemoryIncreaseBytes()).isEqualTo(512000L);
        assertThat(metrics.getTestPassed()).isTrue();
        assertThat(metrics.getAdditionalMetrics()).isEqualTo("{\"custom\": \"data\"}");
    }

    @Test
    void shouldHandleNullValues() {
        PerformanceMetrics metrics = new PerformanceMetrics();

        metrics.setId(null);
        metrics.setTestName(null);
        metrics.setTestType(null);
        metrics.setTimestamp(null);
        metrics.setTotalRequests(null);
        metrics.setTotalTimeMs(null);
        metrics.setAverageResponseTimeMs(null);
        metrics.setConcurrentThreads(null);
        metrics.setRequestsPerThread(null);
        metrics.setPageSize(null);
        metrics.setMemoryUsageBytes(null);
        metrics.setMemoryIncreaseBytes(null);
        metrics.setTestPassed(null);
        metrics.setAdditionalMetrics(null);

        assertThat(metrics.getId()).isNull();
        assertThat(metrics.getTestName()).isNull();
        assertThat(metrics.getTestType()).isNull();
        assertThat(metrics.getTimestamp()).isNull();
        assertThat(metrics.getTotalRequests()).isNull();
        assertThat(metrics.getTotalTimeMs()).isNull();
        assertThat(metrics.getAverageResponseTimeMs()).isNull();
        assertThat(metrics.getConcurrentThreads()).isNull();
        assertThat(metrics.getRequestsPerThread()).isNull();
        assertThat(metrics.getPageSize()).isNull();
        assertThat(metrics.getMemoryUsageBytes()).isNull();
        assertThat(metrics.getMemoryIncreaseBytes()).isNull();
        assertThat(metrics.getTestPassed()).isNull();
        assertThat(metrics.getAdditionalMetrics()).isNull();
    }

    @Test
    void shouldHaveToStringMethod() {
        PerformanceMetrics metrics = new PerformanceMetrics("Test", "API_REQUEST");
        metrics.setId(1L);
        metrics.setTotalRequests(50);
        metrics.setTestPassed(true);

        String toString = metrics.toString();
        assertThat(toString).contains("id=1");
        assertThat(toString).contains("testName='Test'");
        assertThat(toString).contains("testType='API_REQUEST'");
        assertThat(toString).contains("totalRequests=50");
        assertThat(toString).contains("testPassed=true");
    }

    @Test
    void shouldCreateTypicalApiRequestMetrics() {
        PerformanceMetrics metrics = new PerformanceMetrics("GET /api/users", "API_REQUEST");
        metrics.setTotalRequests(1);
        metrics.setTotalTimeMs(45L);
        metrics.setAverageResponseTimeMs(45.0);
        metrics.setTestPassed(true);
        metrics.setMemoryIncreaseBytes(1024L);

        assertThat(metrics.getTestName()).isEqualTo("GET /api/users");
        assertThat(metrics.getTestType()).isEqualTo("API_REQUEST");
        assertThat(metrics.getTotalRequests()).isEqualTo(1);
        assertThat(metrics.getTotalTimeMs()).isEqualTo(45L);
        assertThat(metrics.getAverageResponseTimeMs()).isEqualTo(45.0);
        assertThat(metrics.getTestPassed()).isTrue();
        assertThat(metrics.getMemoryIncreaseBytes()).isEqualTo(1024L);
    }
}

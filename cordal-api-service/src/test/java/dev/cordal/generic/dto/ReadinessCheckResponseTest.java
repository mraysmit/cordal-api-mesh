package dev.cordal.generic.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ReadinessCheckResponse DTO
 * Ensures type safety and proper data handling for readiness and liveness checks
 */
class ReadinessCheckResponseTest {

    @Test
    void shouldCreateUpReadinessCheck() {
        Map<String, String> checks = Map.of(
            "database", "UP",
            "configuration", "UP",
            "memory", "OK"
        );
        
        ReadinessCheckResponse response = ReadinessCheckResponse.up(checks);
        
        assertThat(response.getStatus()).isEqualTo("UP");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getChecks()).containsAllEntriesOf(checks);
        assertThat(response.getMessage()).isEqualTo("All checks passed");
        assertThat(response.isReady()).isTrue();
        assertThat(response.isNotReady()).isFalse();
    }

    @Test
    void shouldCreateUpReadinessCheckWithCustomMessage() {
        Map<String, String> checks = Map.of("service", "UP");
        String customMessage = "Service is healthy and ready";
        
        ReadinessCheckResponse response = ReadinessCheckResponse.up(checks, customMessage);
        
        assertThat(response.getStatus()).isEqualTo("UP");
        assertThat(response.getChecks()).containsAllEntriesOf(checks);
        assertThat(response.getMessage()).isEqualTo(customMessage);
        assertThat(response.isReady()).isTrue();
    }

    @Test
    void shouldCreateDownReadinessCheck() {
        Map<String, String> checks = Map.of(
            "database", "DOWN",
            "configuration", "UP",
            "memory", "HIGH_USAGE"
        );
        String message = "Database connection failed";
        
        ReadinessCheckResponse response = ReadinessCheckResponse.down(checks, message);
        
        assertThat(response.getStatus()).isEqualTo("DOWN");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getChecks()).containsAllEntriesOf(checks);
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.isReady()).isFalse();
        assertThat(response.isNotReady()).isTrue();
    }

    @Test
    void shouldCreateSimpleUpReadinessCheck() {
        ReadinessCheckResponse response = ReadinessCheckResponse.up();
        
        assertThat(response.getStatus()).isEqualTo("UP");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getChecks()).containsEntry("service", "UP");
        assertThat(response.getMessage()).isEqualTo("Service is ready");
        assertThat(response.isReady()).isTrue();
    }

    @Test
    void shouldCreateSimpleDownReadinessCheck() {
        String errorMessage = "Service initialization failed";
        ReadinessCheckResponse response = ReadinessCheckResponse.down(errorMessage);
        
        assertThat(response.getStatus()).isEqualTo("DOWN");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getChecks()).containsEntry("service", "DOWN");
        assertThat(response.getMessage()).isEqualTo(errorMessage);
        assertThat(response.isNotReady()).isTrue();
    }

    @Test
    void shouldCreateWithConstructor() {
        Map<String, String> checks = Map.of("test", "UP");
        Instant timestamp = Instant.now();
        
        ReadinessCheckResponse response = new ReadinessCheckResponse("UP", timestamp, checks, "Test message");
        
        assertThat(response.getStatus()).isEqualTo("UP");
        assertThat(response.getTimestamp()).isEqualTo(timestamp);
        assertThat(response.getChecks()).containsAllEntriesOf(checks);
        assertThat(response.getMessage()).isEqualTo("Test message");
    }

    @Test
    void shouldDetectAllChecksPassed() {
        Map<String, String> allUp = Map.of(
            "database", "UP",
            "configuration", "UP",
            "memory", "UP"
        );
        
        Map<String, String> someDown = Map.of(
            "database", "UP",
            "configuration", "DOWN",
            "memory", "UP"
        );
        
        ReadinessCheckResponse allUpResponse = ReadinessCheckResponse.up(allUp);
        ReadinessCheckResponse someDownResponse = ReadinessCheckResponse.down(someDown, "Some checks failed");
        
        assertThat(allUpResponse.allChecksPassed()).isTrue();
        assertThat(someDownResponse.allChecksPassed()).isFalse();
    }

    @Test
    void shouldGetFailedChecks() {
        Map<String, String> checks = Map.of(
            "database", "UP",
            "configuration", "DOWN",
            "memory", "HIGH_USAGE",
            "threads", "UP"
        );
        
        ReadinessCheckResponse response = ReadinessCheckResponse.down(checks, "Some checks failed");
        Map<String, String> failedChecks = response.getFailedChecks();
        
        assertThat(failedChecks).hasSize(2);
        assertThat(failedChecks).containsEntry("configuration", "DOWN");
        assertThat(failedChecks).containsEntry("memory", "HIGH_USAGE");
        assertThat(failedChecks).doesNotContainKey("database");
        assertThat(failedChecks).doesNotContainKey("threads");
    }

    @Test
    void shouldGetFailedCheckCount() {
        Map<String, String> checks = Map.of(
            "database", "UP",
            "configuration", "DOWN",
            "memory", "HIGH_USAGE"
        );
        
        ReadinessCheckResponse response = ReadinessCheckResponse.down(checks, "Some checks failed");
        
        assertThat(response.getFailedCheckCount()).isEqualTo(2);
    }

    @Test
    void shouldHandleEmptyChecks() {
        ReadinessCheckResponse upResponse = ReadinessCheckResponse.up(Map.of());
        ReadinessCheckResponse downResponse = ReadinessCheckResponse.down(Map.of(), "No checks");
        
        assertThat(upResponse.allChecksPassed()).isTrue(); // Empty checks default to ready status
        assertThat(downResponse.allChecksPassed()).isFalse(); // Down status overrides empty checks
        assertThat(upResponse.getFailedChecks()).isEmpty();
        assertThat(downResponse.getFailedChecks()).isEmpty();
        assertThat(upResponse.getFailedCheckCount()).isEqualTo(0);
        assertThat(downResponse.getFailedCheckCount()).isEqualTo(0);
    }

    @Test
    void shouldHandleNullChecks() {
        ReadinessCheckResponse response = new ReadinessCheckResponse("UP", Instant.now(), null, "Test");
        
        assertThat(response.getChecks()).isNull();
        assertThat(response.allChecksPassed()).isTrue(); // Null checks default to ready status
        assertThat(response.getFailedChecks()).isEmpty();
        assertThat(response.getFailedCheckCount()).isEqualTo(0);
    }

    @Test
    void shouldConvertToMap() {
        Map<String, String> checks = Map.of("database", "UP", "memory", "OK");
        ReadinessCheckResponse response = ReadinessCheckResponse.up(checks, "All good");
        Map<String, Object> map = response.toMap();
        
        assertThat(map).containsEntry("status", "UP");
        assertThat(map).containsKey("timestamp");
        assertThat(map).containsEntry("checks", checks);
        assertThat(map).containsEntry("message", "All good");
    }

    @Test
    void shouldConvertToMapWithNullValues() {
        ReadinessCheckResponse response = new ReadinessCheckResponse("UP", Instant.now(), null, null);
        Map<String, Object> map = response.toMap();
        
        assertThat(map).containsEntry("status", "UP");
        assertThat(map).containsKey("timestamp");
        assertThat(map).doesNotContainKey("checks"); // Null values should not be included
        assertThat(map).doesNotContainKey("message");
    }

    @Test
    void shouldHaveProperEqualsAndHashCode() {
        Instant timestamp = Instant.now();
        Map<String, String> checks = Map.of("test", "UP");
        
        ReadinessCheckResponse response1 = new ReadinessCheckResponse("UP", timestamp, checks, "Test");
        ReadinessCheckResponse response2 = new ReadinessCheckResponse("UP", timestamp, checks, "Test");
        ReadinessCheckResponse response3 = new ReadinessCheckResponse("DOWN", timestamp, checks, "Test");
        
        assertThat(response1).isEqualTo(response2);
        assertThat(response1).isNotEqualTo(response3);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }

    @Test
    void shouldHaveInformativeToString() {
        Map<String, String> checks = Map.of("database", "UP", "memory", "OK");
        ReadinessCheckResponse response = ReadinessCheckResponse.up(checks, "All systems go");
        String toString = response.toString();
        
        assertThat(toString).contains("ReadinessCheckResponse");
        assertThat(toString).contains("status='UP'");
        assertThat(toString).contains("2 checks");
        assertThat(toString).contains("message='All systems go'");
    }

    @Test
    void shouldHandleNullChecksInToString() {
        ReadinessCheckResponse response = new ReadinessCheckResponse("UP", Instant.now(), null, "Test");
        String toString = response.toString();
        
        assertThat(toString).contains("no checks");
    }

    @Test
    void shouldDetectStatusCorrectly() {
        // Test case sensitivity and exact matching
        assertThat(new ReadinessCheckResponse("UP", Instant.now(), null, null).isReady()).isTrue();
        assertThat(new ReadinessCheckResponse("up", Instant.now(), null, null).isReady()).isFalse();
        assertThat(new ReadinessCheckResponse("DOWN", Instant.now(), null, null).isNotReady()).isTrue();
        assertThat(new ReadinessCheckResponse("down", Instant.now(), null, null).isNotReady()).isFalse();
        assertThat(new ReadinessCheckResponse("DEGRADED", Instant.now(), null, null).isReady()).isFalse();
        assertThat(new ReadinessCheckResponse("DEGRADED", Instant.now(), null, null).isNotReady()).isFalse();
    }
}

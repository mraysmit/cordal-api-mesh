package dev.cordal.generic.dto;

import dev.cordal.generic.management.HealthMonitoringService.DatabaseHealthStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseHealthResponseTest {

    @Test
    void shouldCreateResponsesFromFactoriesAndStatusObjects() {
        Instant checkTime = Instant.parse("2026-03-15T03:20:00Z");
        DatabaseHealthStatus status = new DatabaseHealthStatus("orders-db", "UP", "Connection successful", checkTime, 250);

        DatabaseHealthResponse fromStatus = DatabaseHealthResponse.from(status);
        DatabaseHealthResponse up = DatabaseHealthResponse.up("orders-db", "ok", 100);
        DatabaseHealthResponse down = DatabaseHealthResponse.down("orders-db", "failed", 1500);

        assertThat(fromStatus.getDatabaseName()).isEqualTo("orders-db");
        assertThat(fromStatus.getStatus()).isEqualTo("UP");
        assertThat(fromStatus.getMessage()).isEqualTo("Connection successful");
        assertThat(fromStatus.getCheckTime()).isEqualTo(checkTime);
        assertThat(fromStatus.getResponseTimeMs()).isEqualTo(250);
        assertThat(up.isUp()).isTrue();
        assertThat(down.isDown()).isTrue();
    }

    @Test
    void shouldReportPerformanceStatusAndValidity() {
        Instant recent = Instant.now();
        Instant stale = recent.minusMillis(10_000);

        DatabaseHealthResponse fast = new DatabaseHealthResponse("orders-db", "UP", "ok", recent, 100);
        DatabaseHealthResponse moderate = new DatabaseHealthResponse("orders-db", "UP", "ok", recent, 700);
        DatabaseHealthResponse slow = new DatabaseHealthResponse("orders-db", "UP", "ok", recent, 1500);
        DatabaseHealthResponse verySlow = new DatabaseHealthResponse("orders-db", "UP", "ok", stale, 6000);

        assertThat(fast.getPerformanceStatus()).isEqualTo("FAST");
        assertThat(moderate.getPerformanceStatus()).isEqualTo("MODERATE");
        assertThat(slow.isSlow()).isTrue();
        assertThat(slow.isVerySlow()).isFalse();
        assertThat(slow.getPerformanceStatus()).isEqualTo("SLOW");
        assertThat(verySlow.isVerySlow()).isTrue();
        assertThat(verySlow.getPerformanceStatus()).isEqualTo("VERY_SLOW");
        assertThat(fast.isValid(1_000)).isTrue();
        assertThat(verySlow.isValid(1_000)).isFalse();
    }

    @Test
    void shouldConvertBackToMapAndHealthStatusAndSupportEquality() {
        Instant timestamp = Instant.parse("2026-03-15T03:30:00Z");
        DatabaseHealthResponse first = new DatabaseHealthResponse("orders-db", "DOWN", "timeout", timestamp, 5001);
        DatabaseHealthResponse second = new DatabaseHealthResponse("orders-db", "DOWN", "timeout", timestamp, 5001);

        Map<String, Object> map = first.toMap();
        DatabaseHealthStatus status = first.toDatabaseHealthStatus();

        assertThat(map)
            .containsEntry("databaseName", "orders-db")
            .containsEntry("status", "DOWN")
            .containsEntry("message", "timeout")
            .containsEntry("checkTime", timestamp)
            .containsEntry("responseTimeMs", 5001L);
        assertThat(status.getDatabaseName()).isEqualTo("orders-db");
        assertThat(status.getStatus()).isEqualTo("DOWN");
        assertThat(status.getMessage()).isEqualTo("timeout");
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
        assertThat(first.toString()).contains("DatabaseHealthResponse").contains("responseTimeMs=5001");
    }
}
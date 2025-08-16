package dev.cordal.generic.dto;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ServiceHealthResponse DTO
 * Ensures type safety and proper data handling for service health status
 */
class ServiceHealthResponseTest {

    @Test
    void shouldCreateUpServiceHealth() {
        MemoryUsageResponse memory = new MemoryUsageResponse(1024, 512, 256, 256, 50);
        ServiceHealthResponse response = ServiceHealthResponse.up("2 hours", memory, 25);
        
        assertThat(response.getStatus()).isEqualTo("UP");
        assertThat(response.getUptime()).isEqualTo("2 hours");
        assertThat(response.getMemoryUsage()).isEqualTo(memory);
        assertThat(response.getThreadCount()).isEqualTo(25);
        assertThat(response.isUp()).isTrue();
        assertThat(response.isDown()).isFalse();
    }

    @Test
    void shouldCreateDownServiceHealth() {
        MemoryUsageResponse memory = new MemoryUsageResponse(1024, 512, 256, 256, 50);
        ServiceHealthResponse response = ServiceHealthResponse.down("2 hours", memory, 25);
        
        assertThat(response.getStatus()).isEqualTo("DOWN");
        assertThat(response.getUptime()).isEqualTo("2 hours");
        assertThat(response.getMemoryUsage()).isEqualTo(memory);
        assertThat(response.getThreadCount()).isEqualTo(25);
        assertThat(response.isUp()).isFalse();
        assertThat(response.isDown()).isTrue();
    }

    @Test
    void shouldCreateServiceHealthWithConstructor() {
        MemoryUsageResponse memory = new MemoryUsageResponse(1024, 512, 256, 256, 50);
        ServiceHealthResponse response = new ServiceHealthResponse("DEGRADED", "1 hour", memory, 50);
        
        assertThat(response.getStatus()).isEqualTo("DEGRADED");
        assertThat(response.getUptime()).isEqualTo("1 hour");
        assertThat(response.getMemoryUsage()).isEqualTo(memory);
        assertThat(response.getThreadCount()).isEqualTo(50);
        assertThat(response.isUp()).isFalse(); // Only "UP" is considered up
        assertThat(response.isDown()).isFalse(); // Only "DOWN" is considered down
    }

    @Test
    void shouldConvertToMap() {
        MemoryUsageResponse memory = new MemoryUsageResponse(1024, 512, 256, 256, 50);
        ServiceHealthResponse response = ServiceHealthResponse.up("2 hours", memory, 25);
        Map<String, Object> map = response.toMap();
        
        assertThat(map).containsEntry("status", "UP");
        assertThat(map).containsEntry("uptime", "2 hours");
        assertThat(map).containsEntry("threadCount", 25);
        assertThat(map).containsKey("memoryUsage");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> memoryMap = (Map<String, Object>) map.get("memoryUsage");
        assertThat(memoryMap).containsEntry("usagePercentage", 50L);
    }

    @Test
    void shouldConvertToMapWithNullMemory() {
        ServiceHealthResponse response = new ServiceHealthResponse("UP", "2 hours", null, 25);
        Map<String, Object> map = response.toMap();
        
        assertThat(map).containsEntry("status", "UP");
        assertThat(map).containsEntry("uptime", "2 hours");
        assertThat(map).containsEntry("threadCount", 25);
        assertThat(map).containsEntry("memoryUsage", null);
    }

    @Test
    void shouldHaveProperEqualsAndHashCode() {
        MemoryUsageResponse memory1 = new MemoryUsageResponse(1024, 512, 256, 256, 50);
        MemoryUsageResponse memory2 = new MemoryUsageResponse(1024, 512, 256, 256, 50);
        MemoryUsageResponse memory3 = new MemoryUsageResponse(2048, 512, 256, 256, 50);
        
        ServiceHealthResponse response1 = ServiceHealthResponse.up("2 hours", memory1, 25);
        ServiceHealthResponse response2 = ServiceHealthResponse.up("2 hours", memory2, 25);
        ServiceHealthResponse response3 = ServiceHealthResponse.up("2 hours", memory3, 25);
        ServiceHealthResponse response4 = ServiceHealthResponse.down("2 hours", memory1, 25);
        
        assertThat(response1).isEqualTo(response2);
        assertThat(response1).isNotEqualTo(response3);
        assertThat(response1).isNotEqualTo(response4);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }

    @Test
    void shouldHaveInformativeToString() {
        MemoryUsageResponse memory = new MemoryUsageResponse(1024, 512, 256, 256, 50);
        ServiceHealthResponse response = ServiceHealthResponse.up("2 hours", memory, 25);
        String toString = response.toString();
        
        assertThat(toString).contains("ServiceHealthResponse");
        assertThat(toString).contains("status='UP'");
        assertThat(toString).contains("uptime='2 hours'");
        assertThat(toString).contains("threadCount=25");
        assertThat(toString).contains("memoryUsage=");
    }

    @Test
    void shouldHandleNullMemoryInEquals() {
        ServiceHealthResponse response1 = new ServiceHealthResponse("UP", "2 hours", null, 25);
        ServiceHealthResponse response2 = new ServiceHealthResponse("UP", "2 hours", null, 25);
        ServiceHealthResponse response3 = new ServiceHealthResponse("UP", "2 hours", 
            new MemoryUsageResponse(1024, 512, 256, 256, 50), 25);
        
        assertThat(response1).isEqualTo(response2);
        assertThat(response1).isNotEqualTo(response3);
    }

    @Test
    void shouldHandleHighThreadCount() {
        MemoryUsageResponse memory = new MemoryUsageResponse(1024, 512, 256, 256, 50);
        ServiceHealthResponse response = ServiceHealthResponse.up("2 hours", memory, 1500);
        
        assertThat(response.getThreadCount()).isEqualTo(1500);
        assertThat(response.isUp()).isTrue();
    }

    @Test
    void shouldHandleZeroThreadCount() {
        MemoryUsageResponse memory = new MemoryUsageResponse(1024, 512, 256, 256, 50);
        ServiceHealthResponse response = ServiceHealthResponse.up("2 hours", memory, 0);
        
        assertThat(response.getThreadCount()).isEqualTo(0);
        assertThat(response.isUp()).isTrue();
    }

    @Test
    void shouldHandleLongUptime() {
        MemoryUsageResponse memory = new MemoryUsageResponse(1024, 512, 256, 256, 50);
        String longUptime = "15 days, 3 hours, 42 minutes";
        ServiceHealthResponse response = ServiceHealthResponse.up(longUptime, memory, 25);
        
        assertThat(response.getUptime()).isEqualTo(longUptime);
        assertThat(response.isUp()).isTrue();
    }

    @Test
    void shouldHandleEmptyUptime() {
        MemoryUsageResponse memory = new MemoryUsageResponse(1024, 512, 256, 256, 50);
        ServiceHealthResponse response = ServiceHealthResponse.up("", memory, 25);
        
        assertThat(response.getUptime()).isEqualTo("");
        assertThat(response.isUp()).isTrue();
    }

    @Test
    void shouldDetectStatusCorrectly() {
        MemoryUsageResponse memory = new MemoryUsageResponse(1024, 512, 256, 256, 50);
        
        // Test various status values
        assertThat(new ServiceHealthResponse("UP", "1h", memory, 10).isUp()).isTrue();
        assertThat(new ServiceHealthResponse("up", "1h", memory, 10).isUp()).isFalse(); // Case sensitive
        assertThat(new ServiceHealthResponse("DOWN", "1h", memory, 10).isDown()).isTrue();
        assertThat(new ServiceHealthResponse("down", "1h", memory, 10).isDown()).isFalse(); // Case sensitive
        assertThat(new ServiceHealthResponse("DEGRADED", "1h", memory, 10).isUp()).isFalse();
        assertThat(new ServiceHealthResponse("DEGRADED", "1h", memory, 10).isDown()).isFalse();
    }
}

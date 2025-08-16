package dev.cordal.generic.dto;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MemoryUsageResponse DTO
 * Ensures type safety and proper data handling for memory usage information
 */
class MemoryUsageResponseTest {

    @Test
    void shouldCreateMemoryUsageFromRuntime() {
        MemoryUsageResponse response = MemoryUsageResponse.fromRuntime();
        
        assertThat(response.getMaxMemoryMB()).isGreaterThan(0);
        assertThat(response.getTotalMemoryMB()).isGreaterThan(0);
        assertThat(response.getUsedMemoryMB()).isGreaterThanOrEqualTo(0);
        assertThat(response.getFreeMemoryMB()).isGreaterThanOrEqualTo(0);
        assertThat(response.getUsagePercentage()).isBetween(0L, 100L);
    }

    @Test
    void shouldCreateMemoryUsageWithSpecificValues() {
        MemoryUsageResponse response = new MemoryUsageResponse(1024, 512, 256, 256, 50);
        
        assertThat(response.getMaxMemoryMB()).isEqualTo(1024);
        assertThat(response.getTotalMemoryMB()).isEqualTo(512);
        assertThat(response.getUsedMemoryMB()).isEqualTo(256);
        assertThat(response.getFreeMemoryMB()).isEqualTo(256);
        assertThat(response.getUsagePercentage()).isEqualTo(50);
    }

    @Test
    void shouldDetectHighMemoryUsage() {
        MemoryUsageResponse highUsage = new MemoryUsageResponse(1024, 512, 450, 62, 85);
        MemoryUsageResponse normalUsage = new MemoryUsageResponse(1024, 512, 256, 256, 50);
        
        assertThat(highUsage.isHighUsage()).isTrue();
        assertThat(normalUsage.isHighUsage()).isFalse();
    }

    @Test
    void shouldDetectCriticalMemoryUsage() {
        MemoryUsageResponse criticalUsage = new MemoryUsageResponse(1024, 512, 480, 32, 95);
        MemoryUsageResponse normalUsage = new MemoryUsageResponse(1024, 512, 256, 256, 50);
        
        assertThat(criticalUsage.isCriticalUsage()).isTrue();
        assertThat(normalUsage.isCriticalUsage()).isFalse();
    }

    @Test
    void shouldDetectVeryCriticalMemoryUsage() {
        MemoryUsageResponse veryCriticalUsage = new MemoryUsageResponse(1024, 512, 500, 12, 98);
        MemoryUsageResponse normalUsage = new MemoryUsageResponse(1024, 512, 256, 256, 50);
        
        assertThat(veryCriticalUsage.isVeryCriticalUsage()).isTrue();
        assertThat(normalUsage.isVeryCriticalUsage()).isFalse();
    }

    @Test
    void shouldGetCorrectMemoryStatus() {
        MemoryUsageResponse okUsage = new MemoryUsageResponse(1024, 512, 256, 256, 50);
        MemoryUsageResponse moderateUsage = new MemoryUsageResponse(1024, 512, 430, 82, 85);
        MemoryUsageResponse highUsage = new MemoryUsageResponse(1024, 512, 470, 42, 92);
        MemoryUsageResponse criticalUsage = new MemoryUsageResponse(1024, 512, 500, 12, 98);
        
        assertThat(okUsage.getMemoryStatus()).isEqualTo("OK");
        assertThat(moderateUsage.getMemoryStatus()).isEqualTo("MODERATE");
        assertThat(highUsage.getMemoryStatus()).isEqualTo("HIGH");
        assertThat(criticalUsage.getMemoryStatus()).isEqualTo("CRITICAL");
    }

    @Test
    void shouldConvertToMap() {
        MemoryUsageResponse response = new MemoryUsageResponse(1024, 512, 256, 256, 50);
        Map<String, Object> map = response.toMap();
        
        assertThat(map).containsEntry("maxMemoryMB", 1024L);
        assertThat(map).containsEntry("totalMemoryMB", 512L);
        assertThat(map).containsEntry("usedMemoryMB", 256L);
        assertThat(map).containsEntry("freeMemoryMB", 256L);
        assertThat(map).containsEntry("usagePercentage", 50L);
    }

    @Test
    void shouldHaveProperEqualsAndHashCode() {
        MemoryUsageResponse response1 = new MemoryUsageResponse(1024, 512, 256, 256, 50);
        MemoryUsageResponse response2 = new MemoryUsageResponse(1024, 512, 256, 256, 50);
        MemoryUsageResponse response3 = new MemoryUsageResponse(2048, 512, 256, 256, 50);
        
        assertThat(response1).isEqualTo(response2);
        assertThat(response1).isNotEqualTo(response3);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }

    @Test
    void shouldHaveInformativeToString() {
        MemoryUsageResponse response = new MemoryUsageResponse(1024, 512, 256, 256, 50);
        String toString = response.toString();
        
        assertThat(toString).contains("MemoryUsageResponse");
        assertThat(toString).contains("maxMemoryMB=1024");
        assertThat(toString).contains("totalMemoryMB=512");
        assertThat(toString).contains("usedMemoryMB=256");
        assertThat(toString).contains("freeMemoryMB=256");
        assertThat(toString).contains("usagePercentage=50%");
    }

    @Test
    void shouldHandleEdgeCases() {
        // Test boundary conditions for usage detection
        MemoryUsageResponse exactly80 = new MemoryUsageResponse(1000, 500, 400, 100, 80);
        MemoryUsageResponse exactly81 = new MemoryUsageResponse(1000, 500, 405, 95, 81);
        MemoryUsageResponse exactly90 = new MemoryUsageResponse(1000, 500, 450, 50, 90);
        MemoryUsageResponse exactly91 = new MemoryUsageResponse(1000, 500, 455, 45, 91);
        MemoryUsageResponse exactly95 = new MemoryUsageResponse(1000, 500, 475, 25, 95);
        MemoryUsageResponse exactly96 = new MemoryUsageResponse(1000, 500, 480, 20, 96);
        
        // Test 80% boundary
        assertThat(exactly80.isHighUsage()).isFalse();
        assertThat(exactly81.isHighUsage()).isTrue();
        
        // Test 90% boundary
        assertThat(exactly90.isCriticalUsage()).isFalse();
        assertThat(exactly91.isCriticalUsage()).isTrue();
        
        // Test 95% boundary
        assertThat(exactly95.isVeryCriticalUsage()).isFalse();
        assertThat(exactly96.isVeryCriticalUsage()).isTrue();
    }

    @Test
    void shouldHandleZeroValues() {
        MemoryUsageResponse zeroUsage = new MemoryUsageResponse(1024, 512, 0, 512, 0);
        
        assertThat(zeroUsage.getUsedMemoryMB()).isEqualTo(0);
        assertThat(zeroUsage.getUsagePercentage()).isEqualTo(0);
        assertThat(zeroUsage.isHighUsage()).isFalse();
        assertThat(zeroUsage.isCriticalUsage()).isFalse();
        assertThat(zeroUsage.isVeryCriticalUsage()).isFalse();
        assertThat(zeroUsage.getMemoryStatus()).isEqualTo("OK");
    }

    @Test
    void shouldHandleMaxValues() {
        MemoryUsageResponse maxUsage = new MemoryUsageResponse(1024, 512, 512, 0, 100);
        
        assertThat(maxUsage.getUsedMemoryMB()).isEqualTo(512);
        assertThat(maxUsage.getFreeMemoryMB()).isEqualTo(0);
        assertThat(maxUsage.getUsagePercentage()).isEqualTo(100);
        assertThat(maxUsage.isHighUsage()).isTrue();
        assertThat(maxUsage.isCriticalUsage()).isTrue();
        assertThat(maxUsage.isVeryCriticalUsage()).isTrue();
        assertThat(maxUsage.getMemoryStatus()).isEqualTo("CRITICAL");
    }
}

package dev.cordal.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PerformanceSummaryDto
 * Ensures type safety and proper data handling
 */
class PerformanceSummaryDtoTest {

    @Test
    void shouldCreateEmptySummary() {
        PerformanceSummaryDto summary = new PerformanceSummaryDto();
        
        assertThat(summary.getTotalTests()).isEqualTo(0);
        assertThat(summary.getAverageResponseTime()).isEqualTo(0.0);
        assertThat(summary.getSuccessRate()).isEqualTo(0.0);
        assertThat(summary.getTestTypes()).isEmpty();
        assertThat(summary.getTestTypeDistribution()).isEmpty();
        assertThat(summary.getLastTestTime()).isNull();
    }

    @Test
    void shouldCreatePopulatedSummary() {
        LocalDateTime testTime = LocalDateTime.now();
        List<String> testTypes = List.of("API_REQUEST", "CONCURRENT");
        Map<String, Long> distribution = Map.of("API_REQUEST", 10L, "CONCURRENT", 5L);
        
        PerformanceSummaryDto summary = new PerformanceSummaryDto(
            15,
            45.67,
            85.33,
            testTypes,
            distribution,
            testTime
        );
        
        assertThat(summary.getTotalTests()).isEqualTo(15);
        assertThat(summary.getAverageResponseTime()).isEqualTo(45.67); // Rounded to 2 decimal places
        assertThat(summary.getSuccessRate()).isEqualTo(85.33); // Rounded to 2 decimal places
        assertThat(summary.getTestTypes()).containsExactly("API_REQUEST", "CONCURRENT");
        assertThat(summary.getTestTypeDistribution()).containsEntry("API_REQUEST", 10L);
        assertThat(summary.getTestTypeDistribution()).containsEntry("CONCURRENT", 5L);
        assertThat(summary.getLastTestTime()).isEqualTo(testTime);
    }

    @Test
    void shouldRoundDecimalValues() {
        PerformanceSummaryDto summary = new PerformanceSummaryDto(
            10,
            45.6789,  // Should be rounded to 45.68
            85.3456,  // Should be rounded to 85.35
            List.of(),
            Map.of(),
            LocalDateTime.now()
        );
        
        assertThat(summary.getAverageResponseTime()).isEqualTo(45.68);
        assertThat(summary.getSuccessRate()).isEqualTo(85.35);
    }

    @Test
    void shouldCreateDefensiveCopies() {
        List<String> originalTestTypes = List.of("API_REQUEST");
        Map<String, Long> originalDistribution = Map.of("API_REQUEST", 10L);
        
        PerformanceSummaryDto summary = new PerformanceSummaryDto(
            10,
            45.0,
            85.0,
            originalTestTypes,
            originalDistribution,
            LocalDateTime.now()
        );
        
        // Verify that the DTO contains copies, not references
        assertThat(summary.getTestTypes()).isNotSameAs(originalTestTypes);
        assertThat(summary.getTestTypeDistribution()).isNotSameAs(originalDistribution);
        
        // Verify content is the same
        assertThat(summary.getTestTypes()).containsExactlyElementsOf(originalTestTypes);
        assertThat(summary.getTestTypeDistribution()).containsAllEntriesOf(originalDistribution);
    }

    @Test
    void shouldHaveProperToString() {
        LocalDateTime testTime = LocalDateTime.now();
        PerformanceSummaryDto summary = new PerformanceSummaryDto(
            15,
            45.67,
            85.33,
            List.of("API_REQUEST", "CONCURRENT"),
            Map.of("API_REQUEST", 10L, "CONCURRENT", 5L),
            testTime
        );
        
        String toString = summary.toString();
        assertThat(toString).contains("totalTests=15");
        assertThat(toString).contains("averageResponseTime=45.67");
        assertThat(toString).contains("successRate=85.33");
        assertThat(toString).contains("2 types");
        assertThat(toString).contains("2 entries");
    }

    @Test
    void shouldHandleNullSafely() {
        // Test that the DTO handles null values gracefully
        PerformanceSummaryDto summary = new PerformanceSummaryDto(
            0,
            0.0,
            0.0,
            List.of(),
            Map.of(),
            null  // null lastTestTime should be handled
        );
        
        assertThat(summary.getLastTestTime()).isNull();
        assertThat(summary.toString()).doesNotContain("null");
        assertThat(summary.toString()).contains("lastTestTime=none");
    }
}

package dev.cordal.dto;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PerformanceTrendsDto
 * Ensures type safety and proper data handling
 */
class PerformanceTrendsDtoTest {

    @Test
    void shouldCreateEmptyTrends() {
        PerformanceTrendsDto trends = new PerformanceTrendsDto();
        
        assertThat(trends.getDates()).isEmpty();
        assertThat(trends.getAverageResponseTimes()).isEmpty();
        assertThat(trends.getSuccessRates()).isEmpty();
        assertThat(trends.getTotalDataPoints()).isEqualTo(0);
        assertThat(trends.isEmpty()).isTrue();
        assertThat(trends.getDateCount()).isEqualTo(0);
    }

    @Test
    void shouldCreatePopulatedTrends() {
        List<String> dates = List.of("2024-01-01", "2024-01-02", "2024-01-03");
        Map<String, Double> responseTimes = Map.of(
            "2024-01-01", 45.5,
            "2024-01-02", 50.2,
            "2024-01-03", 42.8
        );
        Map<String, Double> successRates = Map.of(
            "2024-01-01", 95.0,
            "2024-01-02", 92.5,
            "2024-01-03", 98.1
        );
        
        PerformanceTrendsDto trends = new PerformanceTrendsDto(
            dates,
            responseTimes,
            successRates,
            150
        );
        
        assertThat(trends.getDates()).containsExactly("2024-01-01", "2024-01-02", "2024-01-03");
        assertThat(trends.getAverageResponseTimes()).containsEntry("2024-01-01", 45.5);
        assertThat(trends.getAverageResponseTimes()).containsEntry("2024-01-02", 50.2);
        assertThat(trends.getAverageResponseTimes()).containsEntry("2024-01-03", 42.8);
        assertThat(trends.getSuccessRates()).containsEntry("2024-01-01", 95.0);
        assertThat(trends.getSuccessRates()).containsEntry("2024-01-02", 92.5);
        assertThat(trends.getSuccessRates()).containsEntry("2024-01-03", 98.1);
        assertThat(trends.getTotalDataPoints()).isEqualTo(150);
        assertThat(trends.isEmpty()).isFalse();
        assertThat(trends.getDateCount()).isEqualTo(3);
    }

    @Test
    void shouldCreateDefensiveCopies() {
        List<String> originalDates = List.of("2024-01-01", "2024-01-02");
        Map<String, Double> originalResponseTimes = Map.of("2024-01-01", 45.5);
        Map<String, Double> originalSuccessRates = Map.of("2024-01-01", 95.0);
        
        PerformanceTrendsDto trends = new PerformanceTrendsDto(
            originalDates,
            originalResponseTimes,
            originalSuccessRates,
            100
        );
        
        // Verify that the DTO contains copies, not references
        assertThat(trends.getDates()).isNotSameAs(originalDates);
        assertThat(trends.getAverageResponseTimes()).isNotSameAs(originalResponseTimes);
        assertThat(trends.getSuccessRates()).isNotSameAs(originalSuccessRates);
        
        // Verify content is the same
        assertThat(trends.getDates()).containsExactlyElementsOf(originalDates);
        assertThat(trends.getAverageResponseTimes()).containsAllEntriesOf(originalResponseTimes);
        assertThat(trends.getSuccessRates()).containsAllEntriesOf(originalSuccessRates);
    }

    @Test
    void shouldDetectEmptyCorrectly() {
        // Empty dates should be considered empty
        PerformanceTrendsDto emptyDates = new PerformanceTrendsDto(
            List.of(),
            Map.of("2024-01-01", 45.5),
            Map.of("2024-01-01", 95.0),
            100
        );
        assertThat(emptyDates.isEmpty()).isTrue();
        
        // Zero data points should be considered empty
        PerformanceTrendsDto zeroDataPoints = new PerformanceTrendsDto(
            List.of("2024-01-01"),
            Map.of("2024-01-01", 45.5),
            Map.of("2024-01-01", 95.0),
            0
        );
        assertThat(zeroDataPoints.isEmpty()).isTrue();
        
        // Valid data should not be empty
        PerformanceTrendsDto validData = new PerformanceTrendsDto(
            List.of("2024-01-01"),
            Map.of("2024-01-01", 45.5),
            Map.of("2024-01-01", 95.0),
            10
        );
        assertThat(validData.isEmpty()).isFalse();
    }

    @Test
    void shouldHaveProperToString() {
        PerformanceTrendsDto trends = new PerformanceTrendsDto(
            List.of("2024-01-01", "2024-01-02"),
            Map.of("2024-01-01", 45.5, "2024-01-02", 50.2),
            Map.of("2024-01-01", 95.0, "2024-01-02", 92.5),
            150
        );
        
        String toString = trends.toString();
        assertThat(toString).contains("2 dates");
        assertThat(toString).contains("2 entries");
        assertThat(toString).contains("totalDataPoints=150");
    }

    @Test
    void shouldHandleEmptyMapsAndLists() {
        PerformanceTrendsDto trends = new PerformanceTrendsDto(
            List.of(),
            Map.of(),
            Map.of(),
            0
        );
        
        assertThat(trends.getDates()).isEmpty();
        assertThat(trends.getAverageResponseTimes()).isEmpty();
        assertThat(trends.getSuccessRates()).isEmpty();
        assertThat(trends.getTotalDataPoints()).isEqualTo(0);
        assertThat(trends.getDateCount()).isEqualTo(0);
        assertThat(trends.isEmpty()).isTrue();
    }

    @Test
    void shouldProvideCorrectDateCount() {
        PerformanceTrendsDto trends = new PerformanceTrendsDto(
            List.of("2024-01-01", "2024-01-02", "2024-01-03", "2024-01-04", "2024-01-05"),
            Map.of(),
            Map.of(),
            100
        );
        
        assertThat(trends.getDateCount()).isEqualTo(5);
        assertThat(trends.getDates()).hasSize(5);
    }
}

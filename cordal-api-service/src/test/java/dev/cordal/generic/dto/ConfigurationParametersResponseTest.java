package dev.cordal.generic.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationParametersResponseTest {

    @Test
    void shouldProvideParameterSummaryHelpers() {
        Map<String, List<String>> parameters = Map.of(
            "orders", List.of("id", "symbol"),
            "reports", List.of("fromDate")
        );
        ConfigurationParametersResponse<String> response = ConfigurationParametersResponse.of("endpoints", parameters, 4);

        assertThat(response.getConfigType()).isEqualTo("endpoints");
        assertThat(response.getParametersFor("orders")).containsExactly("id", "symbol");
        assertThat(response.hasParameters("orders")).isTrue();
        assertThat(response.hasParameters("missing")).isFalse();
        assertThat(response.getConfigurationNamesWithParameters()).isEqualTo(Set.of("orders", "reports"));
        assertThat(response.getTotalParameterCount()).isEqualTo(3);
        assertThat(response.getParameterCoveragePercentage()).isEqualTo(50.0);
        assertThat(response.hasAnyParameters()).isTrue();
        assertThat(response.getConfigurationsWithoutParameters()).isEqualTo(2);
    }

    @Test
    void shouldConvertToMapAndSupportEquality() {
        Instant timestamp = Instant.parse("2026-03-15T03:40:00Z");
        Map<String, List<Integer>> parameters = Map.of("orders", List.of(1, 2, 3));
        ConfigurationParametersResponse<Integer> first = new ConfigurationParametersResponse<>("queries", parameters, 2, 1, timestamp);
        ConfigurationParametersResponse<Integer> second = new ConfigurationParametersResponse<>("queries", parameters, 2, 1, timestamp);

        Map<String, Object> map = first.toMap();

        assertThat(map)
            .containsEntry("configType", "queries")
            .containsEntry("parameters", parameters)
            .containsEntry("totalConfigurations", 2)
            .containsEntry("configurationsWithParameters", 1)
            .containsEntry("timestamp", timestamp.toEpochMilli());
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
        assertThat(first.toString())
            .contains("ConfigurationParametersResponse")
            .contains("configType='queries'")
            .contains("totalParameterCount=3");
    }

    @Test
    void shouldHandleZeroConfigurationCoverage() {
        ConfigurationParametersResponse<String> response = new ConfigurationParametersResponse<>(
            "queries", Map.of(), 0, 0, Instant.parse("2026-03-15T03:45:00Z")
        );

        assertThat(response.getParameterCoveragePercentage()).isEqualTo(0.0);
        assertThat(response.hasAnyParameters()).isFalse();
        assertThat(response.getConfigurationsWithoutParameters()).isEqualTo(0);
    }
}
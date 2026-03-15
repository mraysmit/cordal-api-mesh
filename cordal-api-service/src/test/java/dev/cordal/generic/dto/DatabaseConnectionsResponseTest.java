package dev.cordal.generic.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseConnectionsResponseTest {

    @Test
    void shouldReportConnectionStatisticsAndMappings() {
        Map<String, String> connections = Map.of(
            "orders", "primary",
            "reports", "reporting",
            "archive", "primary"
        );
        DatabaseConnectionsResponse response = DatabaseConnectionsResponse.of(
            "endpoints", connections, Set.of("primary", "reporting"), 5
        );

        assertThat(response.getConfigType()).isEqualTo("endpoints");
        assertThat(response.getConnections()).isEqualTo(connections);
        assertThat(response.getDatabaseFor("orders")).isEqualTo("primary");
        assertThat(response.hasDatabase("archive")).isTrue();
        assertThat(response.hasDatabase("missing")).isFalse();
        assertThat(response.getConfigurationsUsingDatabase("primary")).containsExactlyInAnyOrder("orders", "archive");
        assertThat(response.getConfigurationCountForDatabase("primary")).isEqualTo(2);
        assertThat(response.getMostUsedDatabase()).isEqualTo("primary");
        assertThat(response.getDatabaseUsageStats())
            .containsEntry("primary", 2)
            .containsEntry("reporting", 1);
        assertThat(response.getDatabaseCoveragePercentage()).isEqualTo(60.0);
        assertThat(response.getConfigurationsWithoutDatabases()).isEqualTo(2);
        assertThat(response.hasAnyDatabaseConnections()).isTrue();
        assertThat(response.getUniqueDatabaseCount()).isEqualTo(2);
        assertThat(response.isDatabaseReferenced("reporting")).isTrue();
    }

    @Test
    void shouldConvertToMapWithBackwardCompatibleKeys() {
        Instant timestamp = Instant.parse("2026-03-15T03:00:00Z");
        Map<String, String> connections = Map.of("orders", "primary");
        DatabaseConnectionsResponse endpoints = new DatabaseConnectionsResponse("endpoints", connections, Set.of("primary"), 1, 1, timestamp);
        DatabaseConnectionsResponse queries = new DatabaseConnectionsResponse("queries", connections, Set.of("primary"), 1, 1, timestamp);
        DatabaseConnectionsResponse generic = new DatabaseConnectionsResponse("other", connections, Set.of("primary"), 1, 1, timestamp);

        assertThat(endpoints.toMap())
            .containsEntry("endpointDatabases", connections)
            .containsEntry("timestamp", timestamp.toEpochMilli());
        assertThat(queries.toMap()).containsEntry("queryDatabases", connections);
        assertThat(generic.toMap()).containsEntry("connections", connections);
    }

    @Test
    void shouldHandleEmptyConnectionsAndSupportEquality() {
        Instant timestamp = Instant.parse("2026-03-15T03:10:00Z");
        DatabaseConnectionsResponse first = new DatabaseConnectionsResponse("queries", Map.of(), Set.of(), 0, 0, timestamp);
        DatabaseConnectionsResponse second = new DatabaseConnectionsResponse("queries", Map.of(), Set.of(), 0, 0, timestamp);

        assertThat(first.getMostUsedDatabase()).isNull();
        assertThat(first.getDatabaseCoveragePercentage()).isEqualTo(0.0);
        assertThat(first.hasAnyDatabaseConnections()).isFalse();
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
        assertThat(first.toString())
            .contains("DatabaseConnectionsResponse")
            .contains("configType='queries'")
            .contains("totalConfigurations=0");
    }
}
package dev.cordal.generic.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseMetadataTest {

    @Test
    void shouldBuildMetadataFluentlyAndConvertToMap() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 3, 15, 10, 30);

        ResponseMetadata metadata = new ResponseMetadata("orders-endpoint", "orders-query", "orders-db")
            .executionTime(125L)
            .cacheHit(true)
            .cacheKey("orders:1")
            .requestId("req-1")
            .version("1.0.0")
            .source("cache")
            .addCustom("region", "us-east")
            .addCustom(Map.of("tenant", "acme"));
        metadata.setTimestamp(timestamp);

        Map<String, Object> map = metadata.toMap();

        assertThat(metadata.getEndpointName()).isEqualTo("orders-endpoint");
        assertThat(metadata.getQueryName()).isEqualTo("orders-query");
        assertThat(metadata.getDatabaseName()).isEqualTo("orders-db");
        assertThat(metadata.getExecutionTimeMs()).isEqualTo(125L);
        assertThat(metadata.getCacheHit()).isTrue();
        assertThat(metadata.getCacheKey()).isEqualTo("orders:1");
        assertThat(metadata.getRequestId()).isEqualTo("req-1");
        assertThat(metadata.getVersion()).isEqualTo("1.0.0");
        assertThat(metadata.getSource()).isEqualTo("cache");
        assertThat(metadata.getCustomMetadata())
            .containsEntry("region", "us-east")
            .containsEntry("tenant", "acme");
        assertThat(map)
            .containsEntry("executionTimeMs", 125L)
            .containsEntry("queryName", "orders-query")
            .containsEntry("databaseName", "orders-db")
            .containsEntry("cacheHit", true)
            .containsEntry("cacheKey", "orders:1")
            .containsEntry("requestId", "req-1")
            .containsEntry("endpointName", "orders-endpoint")
            .containsEntry("timestamp", timestamp)
            .containsEntry("version", "1.0.0")
            .containsEntry("source", "cache");
        assertThat(map.get("custom")).isEqualTo(metadata.getCustomMetadata());
    }

    @Test
    void shouldOmitNullValuesFromMap() {
        ResponseMetadata metadata = new ResponseMetadata();
        metadata.setTimestamp(null);

        assertThat(metadata.toMap()).isEmpty();
    }

    @Test
    void shouldCreateStructuredMetadataFromMapAndPreserveUnknownFields() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 3, 15, 12, 0);
        Map<String, Object> source = Map.ofEntries(
            Map.entry("executionTimeMs", 45),
            Map.entry("queryName", "orders-query"),
            Map.entry("databaseName", "orders-db"),
            Map.entry("cacheHit", true),
            Map.entry("cacheKey", "orders:1"),
            Map.entry("requestId", "req-1"),
            Map.entry("endpointName", "orders-endpoint"),
            Map.entry("timestamp", timestamp),
            Map.entry("version", "2.0.0"),
            Map.entry("source", "database"),
            Map.entry("custom", Map.of("env", "test")),
            Map.entry("traceId", "trace-123")
        );

        ResponseMetadata metadata = ResponseMetadata.fromMap(source);

        assertThat(metadata.getExecutionTimeMs()).isEqualTo(45L);
        assertThat(metadata.getQueryName()).isEqualTo("orders-query");
        assertThat(metadata.getDatabaseName()).isEqualTo("orders-db");
        assertThat(metadata.getCacheHit()).isTrue();
        assertThat(metadata.getCacheKey()).isEqualTo("orders:1");
        assertThat(metadata.getRequestId()).isEqualTo("req-1");
        assertThat(metadata.getEndpointName()).isEqualTo("orders-endpoint");
        assertThat(metadata.getTimestamp()).isEqualTo(timestamp);
        assertThat(metadata.getVersion()).isEqualTo("2.0.0");
        assertThat(metadata.getSource()).isEqualTo("database");
        assertThat(metadata.getCustomMetadata())
            .containsEntry("env", "test")
            .containsEntry("traceId", "trace-123");
    }

    @Test
    void shouldTreatUnstructuredMapAsCustomMetadata() {
        ResponseMetadata metadata = ResponseMetadata.fromMap(Map.of("foo", "bar", "count", 2));

        assertThat(metadata.getCustomMetadata())
            .containsEntry("foo", "bar")
            .containsEntry("count", 2);
    }

    @Test
    void shouldHaveInformativeToString() {
        ResponseMetadata metadata = new ResponseMetadata("orders-endpoint", "orders-query", "orders-db").executionTime(10L);

        assertThat(metadata.toString())
            .contains("ResponseMetadata")
            .contains("executionTimeMs=10")
            .contains("queryName='orders-query'")
            .contains("databaseName='orders-db'")
            .contains("endpointName='orders-endpoint'");
    }
}
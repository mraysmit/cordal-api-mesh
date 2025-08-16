package dev.cordal.generic.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ConfigurationCollectionResponse DTO
 * Ensures type safety and proper data handling for configuration list operations
 */
class ConfigurationCollectionResponseTest {

    @Test
    void shouldCreateGeneralConfigurationResponse() {
        Map<String, String> configs = Map.of(
            "config1", "value1",
            "config2", "value2"
        );
        
        ConfigurationCollectionResponse<String> response = ConfigurationCollectionResponse.of("yaml", configs);
        
        assertThat(response.getCount()).isEqualTo(2);
        assertThat(response.getSource()).isEqualTo("yaml");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getConfigurations()).containsAllEntriesOf(configs);
        assertThat(response.getDatabase()).isNull();
        assertThat(response.getQuery()).isNull();
    }

    @Test
    void shouldCreateDatabaseFilteredResponse() {
        Map<String, String> configs = Map.of(
            "query1", "SELECT * FROM users",
            "query2", "SELECT * FROM orders"
        );
        
        ConfigurationCollectionResponse<String> response = ConfigurationCollectionResponse.forDatabase(
            "database", configs, "test-db");
        
        assertThat(response.getCount()).isEqualTo(2);
        assertThat(response.getSource()).isEqualTo("database");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getConfigurations()).containsAllEntriesOf(configs);
        assertThat(response.getDatabase()).isEqualTo("test-db");
        assertThat(response.getQuery()).isNull();
    }

    @Test
    void shouldCreateQueryFilteredResponse() {
        Map<String, String> configs = Map.of(
            "endpoint1", "/api/users",
            "endpoint2", "/api/orders"
        );
        
        ConfigurationCollectionResponse<String> response = ConfigurationCollectionResponse.forQuery(
            "database", configs, "test-query");
        
        assertThat(response.getCount()).isEqualTo(2);
        assertThat(response.getSource()).isEqualTo("database");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getConfigurations()).containsAllEntriesOf(configs);
        assertThat(response.getDatabase()).isNull();
        assertThat(response.getQuery()).isEqualTo("test-query");
    }

    @Test
    void shouldCreateEmptyResponse() {
        Map<String, String> emptyConfigs = Map.of();
        
        ConfigurationCollectionResponse<String> response = ConfigurationCollectionResponse.of("yaml", emptyConfigs);
        
        assertThat(response.getCount()).isEqualTo(0);
        assertThat(response.getSource()).isEqualTo("yaml");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getConfigurations()).isEmpty();
        assertThat(response.getDatabase()).isNull();
        assertThat(response.getQuery()).isNull();
    }

    @Test
    void shouldCreateWithAllParameters() {
        Map<String, String> configs = Map.of("test", "value");
        Instant timestamp = Instant.now();
        
        ConfigurationCollectionResponse<String> response = new ConfigurationCollectionResponse<>(
            1, "test-source", timestamp, configs);
        
        assertThat(response.getCount()).isEqualTo(1);
        assertThat(response.getSource()).isEqualTo("test-source");
        assertThat(response.getTimestamp()).isEqualTo(timestamp);
        assertThat(response.getConfigurations()).containsAllEntriesOf(configs);
        assertThat(response.getDatabase()).isNull();
        assertThat(response.getQuery()).isNull();
    }

    @Test
    void shouldConvertToMapForDatabases() {
        Map<String, String> configs = Map.of("db1", "config1");
        ConfigurationCollectionResponse<String> response = ConfigurationCollectionResponse.of("yaml", configs);
        
        Map<String, Object> map = response.toMap();
        
        assertThat(map).containsEntry("count", 1);
        assertThat(map).containsEntry("source", "yaml");
        assertThat(map).containsKey("timestamp");
        // For general configurations, it should try to determine the type
        assertThat(map).containsKey("configurations"); // Fallback when type can't be determined
    }

    @Test
    void shouldConvertToMapForDatabaseFiltered() {
        Map<String, String> configs = Map.of("query1", "SELECT * FROM test");
        ConfigurationCollectionResponse<String> response = ConfigurationCollectionResponse.forDatabase(
            "database", configs, "test-db");
        
        Map<String, Object> map = response.toMap();
        
        assertThat(map).containsEntry("count", 1);
        assertThat(map).containsEntry("source", "database");
        assertThat(map).containsEntry("database", "test-db");
        assertThat(map).containsEntry("queries", configs);
        assertThat(map).containsKey("timestamp");
    }

    @Test
    void shouldConvertToMapForQueryFiltered() {
        Map<String, String> configs = Map.of("endpoint1", "/api/test");
        ConfigurationCollectionResponse<String> response = ConfigurationCollectionResponse.forQuery(
            "database", configs, "test-query");
        
        Map<String, Object> map = response.toMap();
        
        assertThat(map).containsEntry("count", 1);
        assertThat(map).containsEntry("source", "database");
        assertThat(map).containsEntry("query", "test-query");
        assertThat(map).containsEntry("endpoints", configs);
        assertThat(map).containsKey("timestamp");
    }

    @Test
    void shouldDetermineConfigurationTypeFromClassName() {
        // Test with mock objects that have specific class names
        Map<String, MockDatabaseConfig> dbConfigs = Map.of("db1", new MockDatabaseConfig());
        ConfigurationCollectionResponse<MockDatabaseConfig> dbResponse = 
            ConfigurationCollectionResponse.of("yaml", dbConfigs);
        
        Map<String, Object> dbMap = dbResponse.toMap();
        assertThat(dbMap).containsKey("databases");
        
        Map<String, MockQueryConfig> queryConfigs = Map.of("query1", new MockQueryConfig());
        ConfigurationCollectionResponse<MockQueryConfig> queryResponse = 
            ConfigurationCollectionResponse.of("yaml", queryConfigs);
        
        Map<String, Object> queryMap = queryResponse.toMap();
        assertThat(queryMap).containsKey("queries");
        
        Map<String, MockEndpointConfig> endpointConfigs = Map.of("endpoint1", new MockEndpointConfig());
        ConfigurationCollectionResponse<MockEndpointConfig> endpointResponse = 
            ConfigurationCollectionResponse.of("yaml", endpointConfigs);
        
        Map<String, Object> endpointMap = endpointResponse.toMap();
        assertThat(endpointMap).containsKey("endpoints");
    }

    @Test
    void shouldHandleEmptyConfigurationsInTypeDetection() {
        Map<String, String> emptyConfigs = Map.of();
        ConfigurationCollectionResponse<String> response = ConfigurationCollectionResponse.of("yaml", emptyConfigs);
        
        Map<String, Object> map = response.toMap();
        assertThat(map).containsKey("configurations"); // Should use fallback for empty configs
    }

    @Test
    void shouldHaveInformativeToString() {
        Map<String, String> configs = Map.of("config1", "value1", "config2", "value2");
        ConfigurationCollectionResponse<String> response = ConfigurationCollectionResponse.forDatabase(
            "database", configs, "test-db");
        
        String toString = response.toString();
        assertThat(toString).contains("ConfigurationCollectionResponse");
        assertThat(toString).contains("count=2");
        assertThat(toString).contains("source='database'");
        assertThat(toString).contains("database='test-db'");
        assertThat(toString).contains("2 items");
    }

    @Test
    void shouldHandleNullValuesInToString() {
        Map<String, String> configs = Map.of("config1", "value1");
        ConfigurationCollectionResponse<String> response = ConfigurationCollectionResponse.of("yaml", configs);
        
        String toString = response.toString();
        assertThat(toString).contains("database='null'");
        assertThat(toString).contains("query='null'");
    }

    // Mock classes for testing type determination
    private static class MockDatabaseConfig {
        @Override
        public String toString() {
            return "MockDatabaseConfig{}";
        }
    }

    private static class MockQueryConfig {
        @Override
        public String toString() {
            return "MockQueryConfig{}";
        }
    }

    private static class MockEndpointConfig {
        @Override
        public String toString() {
            return "MockEndpointConfig{}";
        }
    }

    private static class MockApiConfig {
        @Override
        public String toString() {
            return "MockApiConfig{}";
        }
    }

    @Test
    void shouldDetectApiConfigAsEndpoints() {
        Map<String, MockApiConfig> apiConfigs = Map.of("api1", new MockApiConfig());
        ConfigurationCollectionResponse<MockApiConfig> response = 
            ConfigurationCollectionResponse.of("yaml", apiConfigs);
        
        Map<String, Object> map = response.toMap();
        assertThat(map).containsKey("endpoints"); // Api should be detected as endpoints
    }

    @Test
    void shouldFallbackToConfigurationsForUnknownTypes() {
        Map<String, String> unknownConfigs = Map.of("unknown1", "value1");
        ConfigurationCollectionResponse<String> response = 
            ConfigurationCollectionResponse.of("yaml", unknownConfigs);
        
        Map<String, Object> map = response.toMap();
        assertThat(map).containsKey("configurations"); // Should use fallback
    }
}

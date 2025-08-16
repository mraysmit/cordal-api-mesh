package dev.cordal.generic.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ConfigurationOperationResponse DTO
 * Ensures type safety and proper data handling for configuration CRUD operations
 */
class ConfigurationOperationResponseTest {

    @Test
    void shouldCreateSuccessfulCreateResponse() {
        ConfigurationOperationResponse response = ConfigurationOperationResponse.created("test-config");
        
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getAction()).isEqualTo("created");
        assertThat(response.getName()).isEqualTo("test-config");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getFound()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.isCreated()).isTrue();
        assertThat(response.isUpdated()).isFalse();
        assertThat(response.isDeleted()).isFalse();
    }

    @Test
    void shouldCreateSuccessfulUpdateResponse() {
        ConfigurationOperationResponse response = ConfigurationOperationResponse.updated("test-config");
        
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getAction()).isEqualTo("updated");
        assertThat(response.getName()).isEqualTo("test-config");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getFound()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.isCreated()).isFalse();
        assertThat(response.isUpdated()).isTrue();
        assertThat(response.isDeleted()).isFalse();
    }

    @Test
    void shouldCreateSuccessfulDeleteResponse() {
        ConfigurationOperationResponse response = ConfigurationOperationResponse.deleted("test-config", true);
        
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getAction()).isEqualTo("deleted");
        assertThat(response.getName()).isEqualTo("test-config");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getFound()).isTrue();
        assertThat(response.getMessage()).isNull();
        assertThat(response.isCreated()).isFalse();
        assertThat(response.isUpdated()).isFalse();
        assertThat(response.isDeleted()).isTrue();
    }

    @Test
    void shouldCreateDeleteResponseWhenNotFound() {
        ConfigurationOperationResponse response = ConfigurationOperationResponse.deleted("test-config", false);
        
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getAction()).isEqualTo("deleted");
        assertThat(response.getName()).isEqualTo("test-config");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getFound()).isFalse();
        assertThat(response.getMessage()).isNull();
        assertThat(response.isDeleted()).isTrue();
    }

    @Test
    void shouldCreateFailedResponse() {
        ConfigurationOperationResponse response = ConfigurationOperationResponse.failed(
            "create", "test-config", "Database connection failed");
        
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getAction()).isEqualTo("create");
        assertThat(response.getName()).isEqualTo("test-config");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getFound()).isNull();
        assertThat(response.getMessage()).isEqualTo("Database connection failed");
        assertThat(response.isCreated()).isFalse();
    }

    @Test
    void shouldCreateGenericSuccessResponse() {
        ConfigurationOperationResponse response = ConfigurationOperationResponse.success("validate", "test-config");
        
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getAction()).isEqualTo("validate");
        assertThat(response.getName()).isEqualTo("test-config");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getFound()).isNull();
        assertThat(response.getMessage()).isNull();
    }

    @Test
    void shouldCreateResponseWithAllFields() {
        Instant timestamp = Instant.now();
        ConfigurationOperationResponse response = new ConfigurationOperationResponse(
            true, "custom", "test-config", timestamp, true, "Custom message");
        
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getAction()).isEqualTo("custom");
        assertThat(response.getName()).isEqualTo("test-config");
        assertThat(response.getTimestamp()).isEqualTo(timestamp);
        assertThat(response.getFound()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Custom message");
    }

    @Test
    void shouldConvertToMap() {
        ConfigurationOperationResponse response = ConfigurationOperationResponse.created("test-config");
        Map<String, Object> map = response.toMap();
        
        assertThat(map).containsEntry("success", true);
        assertThat(map).containsEntry("action", "created");
        assertThat(map).containsEntry("name", "test-config");
        assertThat(map).containsKey("timestamp");
        assertThat(map).doesNotContainKey("found"); // Should not include null values
        assertThat(map).doesNotContainKey("message");
    }

    @Test
    void shouldConvertToMapWithAllFields() {
        ConfigurationOperationResponse response = ConfigurationOperationResponse.deleted("test-config", true);
        Map<String, Object> map = response.toMap();
        
        assertThat(map).containsEntry("success", true);
        assertThat(map).containsEntry("action", "deleted");
        assertThat(map).containsEntry("name", "test-config");
        assertThat(map).containsKey("timestamp");
        assertThat(map).containsEntry("found", true);
    }

    @Test
    void shouldCreateFromMap() {
        Map<String, Object> map = Map.of(
            "success", true,
            "action", "updated",
            "name", "test-config",
            "timestamp", Instant.now(),
            "found", false,
            "message", "Test message"
        );
        
        ConfigurationOperationResponse response = ConfigurationOperationResponse.fromMap(map);
        
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getAction()).isEqualTo("updated");
        assertThat(response.getName()).isEqualTo("test-config");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getFound()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Test message");
    }

    @Test
    void shouldCreateFromMapWithMissingFields() {
        Map<String, Object> map = Map.of(
            "success", false,
            "action", "create"
        );
        
        ConfigurationOperationResponse response = ConfigurationOperationResponse.fromMap(map);
        
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getAction()).isEqualTo("create");
        assertThat(response.getName()).isEqualTo(""); // Default value
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getFound()).isNull();
        assertThat(response.getMessage()).isNull();
    }

    @Test
    void shouldHaveProperEqualsAndHashCode() {
        Instant timestamp = Instant.now();
        ConfigurationOperationResponse response1 = new ConfigurationOperationResponse(
            true, "created", "test-config", timestamp);
        ConfigurationOperationResponse response2 = new ConfigurationOperationResponse(
            true, "created", "test-config", timestamp);
        ConfigurationOperationResponse response3 = new ConfigurationOperationResponse(
            false, "created", "test-config", timestamp);
        
        assertThat(response1).isEqualTo(response2);
        assertThat(response1).isNotEqualTo(response3);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }

    @Test
    void shouldHaveInformativeToString() {
        ConfigurationOperationResponse response = ConfigurationOperationResponse.created("test-config");
        String toString = response.toString();
        
        assertThat(toString).contains("ConfigurationOperationResponse");
        assertThat(toString).contains("success=true");
        assertThat(toString).contains("action='created'");
        assertThat(toString).contains("name='test-config'");
    }

    @Test
    void shouldHandleNullValuesInToString() {
        ConfigurationOperationResponse response = new ConfigurationOperationResponse(
            true, "test", "config", Instant.now());
        String toString = response.toString();
        
        assertThat(toString).contains("found=null");
        assertThat(toString).contains("message='null'");
    }

    @Test
    void shouldDetectActionTypes() {
        assertThat(ConfigurationOperationResponse.created("test").isCreated()).isTrue();
        assertThat(ConfigurationOperationResponse.updated("test").isUpdated()).isTrue();
        assertThat(ConfigurationOperationResponse.deleted("test", true).isDeleted()).isTrue();
        
        ConfigurationOperationResponse customAction = ConfigurationOperationResponse.success("validate", "test");
        assertThat(customAction.isCreated()).isFalse();
        assertThat(customAction.isUpdated()).isFalse();
        assertThat(customAction.isDeleted()).isFalse();
    }
}

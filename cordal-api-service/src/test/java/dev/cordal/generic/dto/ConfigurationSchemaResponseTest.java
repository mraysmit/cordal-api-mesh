package dev.cordal.generic.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ConfigurationSchemaResponse DTO
 * Ensures type safety and proper data handling for configuration schema information
 */
class ConfigurationSchemaResponseTest {

    @Test
    void shouldCreateSchemaResponse() {
        List<ConfigurationSchemaResponse.SchemaField> fields = List.of(
            new ConfigurationSchemaResponse.SchemaField("name", "String", true, "Configuration name"),
            new ConfigurationSchemaResponse.SchemaField("description", "String", false, "Configuration description")
        );
        
        ConfigurationSchemaResponse response = ConfigurationSchemaResponse.of("endpoints", fields);
        
        assertThat(response.getConfigType()).isEqualTo("endpoints");
        assertThat(response.getFields()).hasSize(2);
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void shouldCreateSchemaResponseWithTimestamp() {
        List<ConfigurationSchemaResponse.SchemaField> fields = List.of(
            new ConfigurationSchemaResponse.SchemaField("path", "String", true, "API path")
        );
        Instant timestamp = Instant.now();
        
        ConfigurationSchemaResponse response = new ConfigurationSchemaResponse("queries", fields, timestamp);
        
        assertThat(response.getConfigType()).isEqualTo("queries");
        assertThat(response.getFields()).hasSize(1);
        assertThat(response.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    void shouldGetFieldByName() {
        List<ConfigurationSchemaResponse.SchemaField> fields = List.of(
            new ConfigurationSchemaResponse.SchemaField("name", "String", true, "Configuration name"),
            new ConfigurationSchemaResponse.SchemaField("description", "String", false, "Configuration description")
        );
        
        ConfigurationSchemaResponse response = ConfigurationSchemaResponse.of("endpoints", fields);
        
        ConfigurationSchemaResponse.SchemaField nameField = response.getField("name");
        assertThat(nameField).isNotNull();
        assertThat(nameField.getName()).isEqualTo("name");
        assertThat(nameField.getType()).isEqualTo("String");
        assertThat(nameField.isRequired()).isTrue();
        
        ConfigurationSchemaResponse.SchemaField nonExistentField = response.getField("nonexistent");
        assertThat(nonExistentField).isNull();
    }

    @Test
    void shouldCheckIfFieldExists() {
        List<ConfigurationSchemaResponse.SchemaField> fields = List.of(
            new ConfigurationSchemaResponse.SchemaField("name", "String", true, "Configuration name")
        );
        
        ConfigurationSchemaResponse response = ConfigurationSchemaResponse.of("endpoints", fields);
        
        assertThat(response.hasField("name")).isTrue();
        assertThat(response.hasField("nonexistent")).isFalse();
    }

    @Test
    void shouldGetRequiredFields() {
        List<ConfigurationSchemaResponse.SchemaField> fields = List.of(
            new ConfigurationSchemaResponse.SchemaField("name", "String", true, "Configuration name"),
            new ConfigurationSchemaResponse.SchemaField("description", "String", false, "Configuration description"),
            new ConfigurationSchemaResponse.SchemaField("path", "String", true, "API path")
        );
        
        ConfigurationSchemaResponse response = ConfigurationSchemaResponse.of("endpoints", fields);
        
        List<ConfigurationSchemaResponse.SchemaField> requiredFields = response.getRequiredFields();
        assertThat(requiredFields).hasSize(2);
        assertThat(requiredFields).extracting(ConfigurationSchemaResponse.SchemaField::getName)
                                  .containsExactlyInAnyOrder("name", "path");
    }

    @Test
    void shouldGetOptionalFields() {
        List<ConfigurationSchemaResponse.SchemaField> fields = List.of(
            new ConfigurationSchemaResponse.SchemaField("name", "String", true, "Configuration name"),
            new ConfigurationSchemaResponse.SchemaField("description", "String", false, "Configuration description"),
            new ConfigurationSchemaResponse.SchemaField("path", "String", true, "API path")
        );
        
        ConfigurationSchemaResponse response = ConfigurationSchemaResponse.of("endpoints", fields);
        
        List<ConfigurationSchemaResponse.SchemaField> optionalFields = response.getOptionalFields();
        assertThat(optionalFields).hasSize(1);
        assertThat(optionalFields.get(0).getName()).isEqualTo("description");
    }

    @Test
    void shouldConvertToMap() {
        List<ConfigurationSchemaResponse.SchemaField> fields = List.of(
            new ConfigurationSchemaResponse.SchemaField("name", "String", true, "Configuration name")
        );
        
        ConfigurationSchemaResponse response = ConfigurationSchemaResponse.of("endpoints", fields);
        Map<String, Object> map = response.toMap();
        
        assertThat(map).containsEntry("configType", "endpoints");
        assertThat(map).containsKey("fields");
        assertThat(map).containsKey("timestamp");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fieldMaps = (List<Map<String, Object>>) map.get("fields");
        assertThat(fieldMaps).hasSize(1);
        assertThat(fieldMaps.get(0)).containsEntry("name", "name");
    }

    @Test
    void shouldHaveInformativeToString() {
        List<ConfigurationSchemaResponse.SchemaField> fields = List.of(
            new ConfigurationSchemaResponse.SchemaField("name", "String", true, "Configuration name"),
            new ConfigurationSchemaResponse.SchemaField("description", "String", false, "Configuration description")
        );
        
        ConfigurationSchemaResponse response = ConfigurationSchemaResponse.of("endpoints", fields);
        String toString = response.toString();
        
        assertThat(toString).contains("ConfigurationSchemaResponse");
        assertThat(toString).contains("configType='endpoints'");
        assertThat(toString).contains("2 fields");
    }

    // SchemaField tests
    @Test
    void shouldCreateSchemaField() {
        ConfigurationSchemaResponse.SchemaField field = 
            new ConfigurationSchemaResponse.SchemaField("name", "String", true, "Configuration name");
        
        assertThat(field.getName()).isEqualTo("name");
        assertThat(field.getType()).isEqualTo("String");
        assertThat(field.isRequired()).isTrue();
        assertThat(field.getDescription()).isEqualTo("Configuration name");
    }

    @Test
    void shouldDetectFieldTypes() {
        ConfigurationSchemaResponse.SchemaField stringField = 
            new ConfigurationSchemaResponse.SchemaField("name", "String", true, "Name");
        ConfigurationSchemaResponse.SchemaField intField = 
            new ConfigurationSchemaResponse.SchemaField("count", "Integer", false, "Count");
        ConfigurationSchemaResponse.SchemaField boolField = 
            new ConfigurationSchemaResponse.SchemaField("enabled", "Boolean", false, "Enabled");
        ConfigurationSchemaResponse.SchemaField listField = 
            new ConfigurationSchemaResponse.SchemaField("items", "List<String>", false, "Items");
        ConfigurationSchemaResponse.SchemaField configField = 
            new ConfigurationSchemaResponse.SchemaField("config", "DatabaseConfig", false, "Config");
        
        assertThat(stringField.isType("String")).isTrue();
        assertThat(stringField.isStringType()).isTrue();
        assertThat(stringField.isNumericType()).isFalse();
        assertThat(stringField.isBooleanType()).isFalse();
        assertThat(stringField.isComplexType()).isFalse();
        
        assertThat(intField.isNumericType()).isTrue();
        assertThat(intField.isStringType()).isFalse();
        
        assertThat(boolField.isBooleanType()).isTrue();
        assertThat(boolField.isStringType()).isFalse();
        
        assertThat(listField.isComplexType()).isTrue();
        assertThat(listField.isStringType()).isFalse();
        
        assertThat(configField.isComplexType()).isTrue();
        assertThat(configField.isStringType()).isFalse();
    }

    @Test
    void shouldConvertSchemaFieldToMap() {
        ConfigurationSchemaResponse.SchemaField field = 
            new ConfigurationSchemaResponse.SchemaField("name", "String", true, "Configuration name");
        
        Map<String, Object> map = field.toMap();
        
        assertThat(map).containsEntry("name", "name");
        assertThat(map).containsEntry("type", "String");
        assertThat(map).containsEntry("required", true);
        assertThat(map).containsEntry("description", "Configuration name");
    }

    @Test
    void shouldHaveProperSchemaFieldEqualsAndHashCode() {
        ConfigurationSchemaResponse.SchemaField field1 = 
            new ConfigurationSchemaResponse.SchemaField("name", "String", true, "Configuration name");
        ConfigurationSchemaResponse.SchemaField field2 = 
            new ConfigurationSchemaResponse.SchemaField("name", "String", true, "Configuration name");
        ConfigurationSchemaResponse.SchemaField field3 = 
            new ConfigurationSchemaResponse.SchemaField("name", "Integer", true, "Configuration name");
        
        assertThat(field1).isEqualTo(field2);
        assertThat(field1).isNotEqualTo(field3);
        assertThat(field1.hashCode()).isEqualTo(field2.hashCode());
    }

    @Test
    void shouldHaveInformativeSchemaFieldToString() {
        ConfigurationSchemaResponse.SchemaField field = 
            new ConfigurationSchemaResponse.SchemaField("name", "String", true, "Configuration name");
        
        String toString = field.toString();
        
        assertThat(toString).contains("SchemaField");
        assertThat(toString).contains("name='name'");
        assertThat(toString).contains("type='String'");
        assertThat(toString).contains("required=true");
        assertThat(toString).contains("description='Configuration name'");
    }

    @Test
    void shouldHandleNullDescription() {
        ConfigurationSchemaResponse.SchemaField field = 
            new ConfigurationSchemaResponse.SchemaField("name", "String", true, null);
        
        assertThat(field.getDescription()).isNull();
        
        Map<String, Object> map = field.toMap();
        assertThat(map).containsEntry("description", null);
    }

    @Test
    void shouldDetectNumericTypes() {
        ConfigurationSchemaResponse.SchemaField intField = 
            new ConfigurationSchemaResponse.SchemaField("count", "Integer", false, "Count");
        ConfigurationSchemaResponse.SchemaField longField = 
            new ConfigurationSchemaResponse.SchemaField("id", "Long", false, "ID");
        ConfigurationSchemaResponse.SchemaField doubleField = 
            new ConfigurationSchemaResponse.SchemaField("rate", "Double", false, "Rate");
        ConfigurationSchemaResponse.SchemaField stringField = 
            new ConfigurationSchemaResponse.SchemaField("name", "String", false, "Name");
        
        assertThat(intField.isNumericType()).isTrue();
        assertThat(longField.isNumericType()).isTrue();
        assertThat(doubleField.isNumericType()).isTrue();
        assertThat(stringField.isNumericType()).isFalse();
    }
}

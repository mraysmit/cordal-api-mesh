package dev.cordal.generic.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ConfigurationValidationResponse DTO
 * Ensures type safety and proper data handling for configuration validation results
 */
class ConfigurationValidationResponseTest {

    @Test
    void shouldCreateValidResponse() {
        ConfigurationValidationResponse response = ConfigurationValidationResponse.valid("All configurations are valid");
        
        assertThat(response.getStatus()).isEqualTo(ConfigurationValidationResponse.ValidationStatus.VALID);
        assertThat(response.getMessage()).isEqualTo("All configurations are valid");
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getErrorCount()).isEqualTo(0);
        assertThat(response.getWarningCount()).isEqualTo(0);
        assertThat(response.isValid()).isTrue();
        assertThat(response.isInvalid()).isFalse();
        assertThat(response.hasErrors()).isFalse();
        assertThat(response.hasWarnings()).isFalse();
        assertThat(response.isClean()).isTrue();
    }

    @Test
    void shouldCreateInvalidResponse() {
        List<String> errors = List.of("Missing required field 'name'", "Invalid URL format");
        ConfigurationValidationResponse response = ConfigurationValidationResponse.invalid("Validation failed", errors);
        
        assertThat(response.getStatus()).isEqualTo(ConfigurationValidationResponse.ValidationStatus.INVALID);
        assertThat(response.getMessage()).isEqualTo("Validation failed");
        assertThat(response.getErrors()).containsExactlyElementsOf(errors);
        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getErrorCount()).isEqualTo(2);
        assertThat(response.getWarningCount()).isEqualTo(0);
        assertThat(response.isValid()).isFalse();
        assertThat(response.isInvalid()).isTrue();
        assertThat(response.hasErrors()).isTrue();
        assertThat(response.hasWarnings()).isFalse();
        assertThat(response.isClean()).isFalse();
    }

    @Test
    void shouldCreateValidResponseWithWarnings() {
        List<String> warnings = List.of("Deprecated field 'oldField' used", "Performance warning: large query");
        ConfigurationValidationResponse response = ConfigurationValidationResponse.validWithWarnings("Valid with warnings", warnings);
        
        assertThat(response.getStatus()).isEqualTo(ConfigurationValidationResponse.ValidationStatus.VALID);
        assertThat(response.getMessage()).isEqualTo("Valid with warnings");
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getWarnings()).containsExactlyElementsOf(warnings);
        assertThat(response.getErrorCount()).isEqualTo(0);
        assertThat(response.getWarningCount()).isEqualTo(2);
        assertThat(response.isValid()).isTrue();
        assertThat(response.isInvalid()).isFalse();
        assertThat(response.hasErrors()).isFalse();
        assertThat(response.hasWarnings()).isTrue();
        assertThat(response.isClean()).isFalse();
    }

    @Test
    void shouldCreateForConfigType() {
        List<String> errors = List.of("Invalid endpoint path");
        List<String> warnings = List.of("Missing description");
        
        ConfigurationValidationResponse response = ConfigurationValidationResponse.forConfigType(
            "endpoints", 5, errors, warnings);
        
        assertThat(response.getConfigType()).isEqualTo("endpoints");
        assertThat(response.getTotalConfigurations()).isEqualTo(5);
        assertThat(response.getStatus()).isEqualTo(ConfigurationValidationResponse.ValidationStatus.INVALID);
        assertThat(response.getMessage()).isEqualTo("Found 1 errors in endpoints configurations");
        assertThat(response.getErrors()).containsExactlyElementsOf(errors);
        assertThat(response.getWarnings()).containsExactlyElementsOf(warnings);
    }

    @Test
    void shouldCreateForConfigTypeWithNoErrors() {
        List<String> warnings = List.of("Missing description");
        
        ConfigurationValidationResponse response = ConfigurationValidationResponse.forConfigType(
            "queries", 3, List.of(), warnings);
        
        assertThat(response.getConfigType()).isEqualTo("queries");
        assertThat(response.getTotalConfigurations()).isEqualTo(3);
        assertThat(response.getStatus()).isEqualTo(ConfigurationValidationResponse.ValidationStatus.VALID);
        assertThat(response.getMessage()).isEqualTo("All queries configurations are valid");
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getWarnings()).containsExactlyElementsOf(warnings);
    }

    @Test
    void shouldCreateWithConstructor() {
        List<String> errors = List.of("Error 1");
        List<String> warnings = List.of("Warning 1");
        Instant timestamp = Instant.now();
        
        ConfigurationValidationResponse response = new ConfigurationValidationResponse(
            ConfigurationValidationResponse.ValidationStatus.INVALID, "Test message", errors, warnings, timestamp);
        
        assertThat(response.getStatus()).isEqualTo(ConfigurationValidationResponse.ValidationStatus.INVALID);
        assertThat(response.getMessage()).isEqualTo("Test message");
        assertThat(response.getErrors()).containsExactlyElementsOf(errors);
        assertThat(response.getWarnings()).containsExactlyElementsOf(warnings);
        assertThat(response.getTimestamp()).isEqualTo(timestamp);
        assertThat(response.getConfigType()).isNull();
        assertThat(response.getTotalConfigurations()).isNull();
    }

    @Test
    void shouldCreateWithConfigTypeConstructor() {
        List<String> errors = List.of("Error 1");
        List<String> warnings = List.of("Warning 1");
        Instant timestamp = Instant.now();
        
        ConfigurationValidationResponse response = new ConfigurationValidationResponse(
            ConfigurationValidationResponse.ValidationStatus.VALID, "Test message", errors, warnings, 
            "databases", 10, timestamp);
        
        assertThat(response.getStatus()).isEqualTo(ConfigurationValidationResponse.ValidationStatus.VALID);
        assertThat(response.getMessage()).isEqualTo("Test message");
        assertThat(response.getErrors()).containsExactlyElementsOf(errors);
        assertThat(response.getWarnings()).containsExactlyElementsOf(warnings);
        assertThat(response.getTimestamp()).isEqualTo(timestamp);
        assertThat(response.getConfigType()).isEqualTo("databases");
        assertThat(response.getTotalConfigurations()).isEqualTo(10);
    }

    @Test
    void shouldGetValidationSummary() {
        ConfigurationValidationResponse cleanResponse = ConfigurationValidationResponse.valid("All good");
        assertThat(cleanResponse.getSummary()).isEqualTo("Validation passed with no issues");
        
        ConfigurationValidationResponse warningsResponse = ConfigurationValidationResponse.validWithWarnings(
            "Valid with warnings", List.of("Warning 1", "Warning 2"));
        assertThat(warningsResponse.getSummary()).isEqualTo("Validation passed with 2 warning(s)");
        
        ConfigurationValidationResponse errorsResponse = ConfigurationValidationResponse.invalid(
            "Failed", List.of("Error 1"));
        assertThat(errorsResponse.getSummary()).isEqualTo("Validation failed with 1 error(s)");
        
        ConfigurationValidationResponse errorsAndWarningsResponse = new ConfigurationValidationResponse(
            ConfigurationValidationResponse.ValidationStatus.INVALID, "Failed", 
            List.of("Error 1"), List.of("Warning 1"), Instant.now());
        assertThat(errorsAndWarningsResponse.getSummary()).isEqualTo("Validation failed with 1 error(s) and 1 warning(s)");
    }

    @Test
    void shouldConvertToMap() {
        List<String> errors = List.of("Error 1");
        List<String> warnings = List.of("Warning 1");
        
        ConfigurationValidationResponse response = ConfigurationValidationResponse.forConfigType(
            "endpoints", 5, errors, warnings);
        
        Map<String, Object> map = response.toMap();
        
        assertThat(map).containsEntry("status", "INVALID");
        assertThat(map).containsEntry("errors", errors);
        assertThat(map).containsEntry("warnings", warnings);
        assertThat(map).containsEntry("errorCount", 1);
        assertThat(map).containsEntry("warningCount", 1);
        assertThat(map).containsEntry("configType", "endpoints");
        assertThat(map).containsEntry("totalConfigurations", 5);
        assertThat(map).containsKey("timestamp");
    }

    @Test
    void shouldConvertToMapWithoutOptionalFields() {
        ConfigurationValidationResponse response = ConfigurationValidationResponse.valid("All good");
        Map<String, Object> map = response.toMap();
        
        assertThat(map).containsEntry("status", "VALID");
        assertThat(map).containsEntry("errors", List.of());
        assertThat(map).containsEntry("warnings", List.of());
        assertThat(map).containsEntry("errorCount", 0);
        assertThat(map).containsEntry("warningCount", 0);
        assertThat(map).doesNotContainKey("configType"); // Should not include null values
        assertThat(map).doesNotContainKey("totalConfigurations");
        assertThat(map).containsKey("timestamp");
    }

    @Test
    void shouldHaveInformativeToString() {
        ConfigurationValidationResponse response = ConfigurationValidationResponse.forConfigType(
            "endpoints", 5, List.of("Error 1"), List.of("Warning 1"));
        
        String toString = response.toString();
        
        assertThat(toString).contains("ConfigurationValidationResponse");
        assertThat(toString).contains("status=INVALID");
        assertThat(toString).contains("errorCount=1");
        assertThat(toString).contains("warningCount=1");
        assertThat(toString).contains("configType='endpoints'");
    }

    @Test
    void shouldHandleNullErrorsAndWarnings() {
        ConfigurationValidationResponse response = new ConfigurationValidationResponse(
            ConfigurationValidationResponse.ValidationStatus.VALID, "Test", null, null, Instant.now());
        
        assertThat(response.getErrors()).isNull();
        assertThat(response.getWarnings()).isNull();
        assertThat(response.getErrorCount()).isEqualTo(0);
        assertThat(response.getWarningCount()).isEqualTo(0);
        assertThat(response.hasErrors()).isFalse();
        assertThat(response.hasWarnings()).isFalse();
        assertThat(response.isClean()).isTrue();
    }

    @Test
    void shouldHandleEmptyErrorsAndWarnings() {
        ConfigurationValidationResponse response = new ConfigurationValidationResponse(
            ConfigurationValidationResponse.ValidationStatus.VALID, "Test", List.of(), List.of(), Instant.now());
        
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getErrorCount()).isEqualTo(0);
        assertThat(response.getWarningCount()).isEqualTo(0);
        assertThat(response.hasErrors()).isFalse();
        assertThat(response.hasWarnings()).isFalse();
        assertThat(response.isClean()).isTrue();
    }

    @Test
    void shouldValidateStatusEnum() {
        assertThat(ConfigurationValidationResponse.ValidationStatus.VALID.toString()).isEqualTo("VALID");
        assertThat(ConfigurationValidationResponse.ValidationStatus.INVALID.toString()).isEqualTo("INVALID");
    }
}

package dev.cordal.generic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Type-safe response for configuration validation results
 * Replaces Map<String, Object> for validation methods (validateConfigurations, etc.)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfigurationValidationResponse {
    
    @JsonProperty("status")
    private final ValidationStatus status;
    
    @JsonProperty("message")
    private final String message;
    
    @JsonProperty("errors")
    private final List<String> errors;
    
    @JsonProperty("warnings")
    private final List<String> warnings;
    
    @JsonProperty("errorCount")
    private final int errorCount;
    
    @JsonProperty("warningCount")
    private final int warningCount;
    
    @JsonProperty("totalConfigurations")
    private final Integer totalConfigurations; // Optional, for specific config type validations
    
    @JsonProperty("configType")
    private final String configType; // Optional, for specific config type validations
    
    @JsonProperty("timestamp")
    private final Instant timestamp;
    
    /**
     * Constructor for general validation
     */
    public ConfigurationValidationResponse(ValidationStatus status, String message, List<String> errors,
                                         List<String> warnings, Instant timestamp) {
        this.status = status;
        this.message = message;
        this.errors = errors;
        this.warnings = warnings;
        this.errorCount = errors != null ? errors.size() : 0;
        this.warningCount = warnings != null ? warnings.size() : 0;
        this.totalConfigurations = null;
        this.configType = null;
        this.timestamp = timestamp;
    }
    
    /**
     * Constructor for specific configuration type validation
     */
    public ConfigurationValidationResponse(ValidationStatus status, String message, List<String> errors,
                                         List<String> warnings, String configType, int totalConfigurations,
                                         Instant timestamp) {
        this.status = status;
        this.message = message;
        this.errors = errors;
        this.warnings = warnings;
        this.errorCount = errors != null ? errors.size() : 0;
        this.warningCount = warnings != null ? warnings.size() : 0;
        this.totalConfigurations = totalConfigurations;
        this.configType = configType;
        this.timestamp = timestamp;
    }
    
    /**
     * Static factory method for valid configuration
     */
    public static ConfigurationValidationResponse valid(String message) {
        return new ConfigurationValidationResponse(ValidationStatus.VALID, message, List.of(), List.of(), Instant.now());
    }
    
    /**
     * Static factory method for invalid configuration
     */
    public static ConfigurationValidationResponse invalid(String message, List<String> errors) {
        return new ConfigurationValidationResponse(ValidationStatus.INVALID, message, errors, List.of(), Instant.now());
    }
    
    /**
     * Static factory method for valid configuration with warnings
     */
    public static ConfigurationValidationResponse validWithWarnings(String message, List<String> warnings) {
        return new ConfigurationValidationResponse(ValidationStatus.VALID, message, List.of(), warnings, Instant.now());
    }
    
    /**
     * Static factory method for specific configuration type validation
     */
    public static ConfigurationValidationResponse forConfigType(String configType, int totalConfigurations,
                                                               List<String> errors, List<String> warnings) {
        ValidationStatus status = errors.isEmpty() ? ValidationStatus.VALID : ValidationStatus.INVALID;
        String message = errors.isEmpty() ? "All " + configType + " configurations are valid" 
                                         : "Found " + errors.size() + " errors in " + configType + " configurations";
        
        return new ConfigurationValidationResponse(status, message, errors, warnings, 
                                                 configType, totalConfigurations, Instant.now());
    }
    
    // Getters
    public ValidationStatus getStatus() {
        return status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
    
    public int getErrorCount() {
        return errorCount;
    }
    
    public int getWarningCount() {
        return warningCount;
    }
    
    public Integer getTotalConfigurations() {
        return totalConfigurations;
    }
    
    public String getConfigType() {
        return configType;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    /**
     * Check if validation passed
     */
    public boolean isValid() {
        return status == ValidationStatus.VALID;
    }
    
    /**
     * Check if validation failed
     */
    public boolean isInvalid() {
        return status == ValidationStatus.INVALID;
    }
    
    /**
     * Check if there are any errors
     */
    public boolean hasErrors() {
        return errorCount > 0;
    }
    
    /**
     * Check if there are any warnings
     */
    public boolean hasWarnings() {
        return warningCount > 0;
    }
    
    /**
     * Check if validation is clean (no errors or warnings)
     */
    public boolean isClean() {
        return !hasErrors() && !hasWarnings();
    }
    
    /**
     * Get validation summary
     */
    public String getSummary() {
        if (isClean()) {
            return "Validation passed with no issues";
        } else if (isValid() && hasWarnings()) {
            return "Validation passed with " + warningCount + " warning(s)";
        } else {
            return "Validation failed with " + errorCount + " error(s)" + 
                   (hasWarnings() ? " and " + warningCount + " warning(s)" : "");
        }
    }
    
    /**
     * Convert to Map<String, Object> for backward compatibility
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", status.toString());
        map.put("message", message);
        map.put("errors", errors);
        map.put("warnings", warnings);
        map.put("errorCount", errorCount);
        map.put("warningCount", warningCount);
        
        if (totalConfigurations != null) {
            map.put("totalConfigurations", totalConfigurations);
        }
        if (configType != null) {
            map.put("configType", configType);
        }
        
        map.put("timestamp", timestamp.toEpochMilli());
        return map;
    }
    
    @Override
    public String toString() {
        return "ConfigurationValidationResponse{" +
                "status=" + status +
                ", errorCount=" + errorCount +
                ", warningCount=" + warningCount +
                (configType != null ? ", configType='" + configType + '\'' : "") +
                ", timestamp=" + timestamp +
                '}';
    }
    
    /**
     * Validation status enum
     */
    public enum ValidationStatus {
        VALID,
        INVALID
    }
}

package dev.cordal.generic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Type-safe response for configuration schema information
 * Replaces Map<String, Object> for schema methods (getEndpointConfigurationSchema, etc.)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfigurationSchemaResponse {
    
    @JsonProperty("configType")
    private final String configType;
    
    @JsonProperty("fields")
    private final List<SchemaField> fields;
    
    @JsonProperty("timestamp")
    private final Instant timestamp;
    
    /**
     * Constructor
     */
    public ConfigurationSchemaResponse(String configType, List<SchemaField> fields, Instant timestamp) {
        this.configType = configType;
        this.fields = fields;
        this.timestamp = timestamp;
    }
    
    /**
     * Static factory method
     */
    public static ConfigurationSchemaResponse of(String configType, List<SchemaField> fields) {
        return new ConfigurationSchemaResponse(configType, fields, Instant.now());
    }
    
    // Getters
    public String getConfigType() {
        return configType;
    }
    
    public List<SchemaField> getFields() {
        return fields;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    /**
     * Get field by name
     */
    public SchemaField getField(String name) {
        return fields.stream()
            .filter(field -> name.equals(field.getName()))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Check if field exists
     */
    public boolean hasField(String name) {
        return getField(name) != null;
    }
    
    /**
     * Get required fields
     */
    public List<SchemaField> getRequiredFields() {
        return fields.stream()
            .filter(SchemaField::isRequired)
            .toList();
    }
    
    /**
     * Get optional fields
     */
    public List<SchemaField> getOptionalFields() {
        return fields.stream()
            .filter(field -> !field.isRequired())
            .toList();
    }
    
    /**
     * Convert to Map<String, Object> for backward compatibility
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("configType", configType);
        map.put("fields", fields.stream().map(SchemaField::toMap).toList());
        map.put("timestamp", timestamp.toEpochMilli());
        return map;
    }
    
    @Override
    public String toString() {
        return "ConfigurationSchemaResponse{" +
                "configType='" + configType + '\'' +
                ", fields=" + fields.size() + " fields" +
                ", timestamp=" + timestamp +
                '}';
    }
    
    /**
     * Schema field definition
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SchemaField {
        
        @JsonProperty("name")
        private final String name;
        
        @JsonProperty("type")
        private final String type;
        
        @JsonProperty("required")
        private final boolean required;
        
        @JsonProperty("description")
        private final String description;
        
        public SchemaField(String name, String type, boolean required, String description) {
            this.name = name;
            this.type = type;
            this.required = required;
            this.description = description;
        }
        
        // Getters
        public String getName() {
            return name;
        }
        
        public String getType() {
            return type;
        }
        
        public boolean isRequired() {
            return required;
        }
        
        public String getDescription() {
            return description;
        }
        
        /**
         * Check if field is of specific type
         */
        public boolean isType(String expectedType) {
            return expectedType.equals(type);
        }
        
        /**
         * Check if field is a string type
         */
        public boolean isStringType() {
            return "String".equals(type);
        }
        
        /**
         * Check if field is a numeric type
         */
        public boolean isNumericType() {
            return "Integer".equals(type) || "Long".equals(type) || "Double".equals(type);
        }
        
        /**
         * Check if field is a boolean type
         */
        public boolean isBooleanType() {
            return "Boolean".equals(type);
        }
        
        /**
         * Check if field is a complex type (List, Map, Object)
         */
        public boolean isComplexType() {
            return type.startsWith("List<") || type.startsWith("Map<") || 
                   type.contains("Config") || type.contains("Object");
        }
        
        /**
         * Convert to Map<String, Object> for backward compatibility
         */
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("name", name);
            map.put("type", type);
            map.put("required", required);
            map.put("description", description);
            return map;
        }
        
        @Override
        public String toString() {
            return "SchemaField{" +
                    "name='" + name + '\'' +
                    ", type='" + type + '\'' +
                    ", required=" + required +
                    ", description='" + description + '\'' +
                    '}';
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            
            SchemaField that = (SchemaField) o;
            
            if (required != that.required) return false;
            if (!name.equals(that.name)) return false;
            if (!type.equals(that.type)) return false;
            return description != null ? description.equals(that.description) : that.description == null;
        }
        
        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + type.hashCode();
            result = 31 * result + (required ? 1 : 0);
            result = 31 * result + (description != null ? description.hashCode() : 0);
            return result;
        }
    }
}

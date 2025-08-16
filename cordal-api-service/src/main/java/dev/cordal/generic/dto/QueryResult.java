package dev.cordal.generic.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Type-safe wrapper for database query results
 * Replaces the unsafe Map<String, Object> pattern while maintaining flexibility
 */
public class QueryResult {
    
    @JsonIgnore
    private final Map<String, Object> data;
    
    /**
     * Constructor from Map<String, Object> (for migration from existing code)
     */
    public QueryResult(Map<String, Object> data) {
        this.data = new LinkedHashMap<>(data); // Defensive copy
    }
    
    /**
     * Constructor for empty result
     */
    public QueryResult() {
        this.data = new LinkedHashMap<>();
    }
    
    /**
     * Get all data as map (for JSON serialization)
     * This allows Jackson to serialize all fields dynamically
     */
    @JsonAnyGetter
    public Map<String, Object> getData() {
        return data;
    }
    
    /**
     * Check if a field exists
     */
    public boolean hasField(String fieldName) {
        return data.containsKey(fieldName);
    }
    
    /**
     * Get field value as Object (unsafe, but sometimes needed)
     */
    public Object getField(String fieldName) {
        return data.get(fieldName);
    }
    
    /**
     * Get field value as String with null safety
     */
    public Optional<String> getString(String fieldName) {
        Object value = data.get(fieldName);
        return value != null ? Optional.of(value.toString()) : Optional.empty();
    }
    
    /**
     * Get field value as String with default
     */
    public String getString(String fieldName, String defaultValue) {
        return getString(fieldName).orElse(defaultValue);
    }
    
    /**
     * Get field value as Integer with null safety
     */
    public Optional<Integer> getInteger(String fieldName) {
        Object value = data.get(fieldName);
        if (value == null) return Optional.empty();
        
        if (value instanceof Integer) {
            return Optional.of((Integer) value);
        } else if (value instanceof Number) {
            return Optional.of(((Number) value).intValue());
        } else {
            try {
                return Optional.of(Integer.parseInt(value.toString()));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
    }
    
    /**
     * Get field value as Integer with default
     */
    public Integer getInteger(String fieldName, Integer defaultValue) {
        return getInteger(fieldName).orElse(defaultValue);
    }
    
    /**
     * Get field value as Long with null safety
     */
    public Optional<Long> getLong(String fieldName) {
        Object value = data.get(fieldName);
        if (value == null) return Optional.empty();
        
        if (value instanceof Long) {
            return Optional.of((Long) value);
        } else if (value instanceof Number) {
            return Optional.of(((Number) value).longValue());
        } else {
            try {
                return Optional.of(Long.parseLong(value.toString()));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
    }
    
    /**
     * Get field value as Long with default
     */
    public Long getLong(String fieldName, Long defaultValue) {
        return getLong(fieldName).orElse(defaultValue);
    }
    
    /**
     * Get field value as Double with null safety
     */
    public Optional<Double> getDouble(String fieldName) {
        Object value = data.get(fieldName);
        if (value == null) return Optional.empty();
        
        if (value instanceof Double) {
            return Optional.of((Double) value);
        } else if (value instanceof Number) {
            return Optional.of(((Number) value).doubleValue());
        } else {
            try {
                return Optional.of(Double.parseDouble(value.toString()));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
    }
    
    /**
     * Get field value as Double with default
     */
    public Double getDouble(String fieldName, Double defaultValue) {
        return getDouble(fieldName).orElse(defaultValue);
    }
    
    /**
     * Get field value as BigDecimal with null safety
     */
    public Optional<BigDecimal> getBigDecimal(String fieldName) {
        Object value = data.get(fieldName);
        if (value == null) return Optional.empty();
        
        if (value instanceof BigDecimal) {
            return Optional.of((BigDecimal) value);
        } else if (value instanceof Number) {
            return Optional.of(BigDecimal.valueOf(((Number) value).doubleValue()));
        } else {
            try {
                return Optional.of(new BigDecimal(value.toString()));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
    }
    
    /**
     * Get field value as Boolean with null safety
     */
    public Optional<Boolean> getBoolean(String fieldName) {
        Object value = data.get(fieldName);
        if (value == null) return Optional.empty();
        
        if (value instanceof Boolean) {
            return Optional.of((Boolean) value);
        } else {
            return Optional.of(Boolean.parseBoolean(value.toString()));
        }
    }
    
    /**
     * Get field value as Boolean with default
     */
    public Boolean getBoolean(String fieldName, Boolean defaultValue) {
        return getBoolean(fieldName).orElse(defaultValue);
    }
    
    /**
     * Get field value as LocalDateTime with null safety
     */
    public Optional<LocalDateTime> getLocalDateTime(String fieldName) {
        Object value = data.get(fieldName);
        if (value == null) return Optional.empty();
        
        if (value instanceof LocalDateTime) {
            return Optional.of((LocalDateTime) value);
        } else if (value instanceof java.sql.Timestamp) {
            return Optional.of(((java.sql.Timestamp) value).toLocalDateTime());
        } else {
            // Could add more date parsing logic here if needed
            return Optional.empty();
        }
    }
    
    /**
     * Get all field names
     */
    public java.util.Set<String> getFieldNames() {
        return data.keySet();
    }
    
    /**
     * Check if result is empty
     */
    public boolean isEmpty() {
        return data.isEmpty();
    }
    
    /**
     * Get number of fields
     */
    public int size() {
        return data.size();
    }
    
    @Override
    public String toString() {
        return "QueryResult{" + data + "}";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueryResult that = (QueryResult) o;
        return data.equals(that.data);
    }
    
    @Override
    public int hashCode() {
        return data.hashCode();
    }
}

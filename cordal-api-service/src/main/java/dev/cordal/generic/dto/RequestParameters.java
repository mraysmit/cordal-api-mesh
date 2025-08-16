package dev.cordal.generic.dto;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Type-safe wrapper for API request parameters
 * Replaces the unsafe Map<String, Object> pattern with proper validation and type conversion
 */
public class RequestParameters {
    
    private final Map<String, Object> parameters;
    
    /**
     * Constructor from Map<String, Object> (for migration from existing code)
     */
    public RequestParameters(Map<String, Object> parameters) {
        this.parameters = new HashMap<>(parameters); // Defensive copy
    }
    
    /**
     * Constructor for empty parameters
     */
    public RequestParameters() {
        this.parameters = new HashMap<>();
    }
    
    /**
     * Add a parameter
     */
    public RequestParameters add(String name, Object value) {
        parameters.put(name, value);
        return this;
    }
    
    /**
     * Check if parameter exists
     */
    public boolean hasParameter(String name) {
        return parameters.containsKey(name);
    }
    
    /**
     * Get parameter as Object (unsafe, but sometimes needed for backward compatibility)
     */
    public Object getParameter(String name) {
        return parameters.get(name);
    }
    
    /**
     * Get parameter as String with null safety
     */
    public Optional<String> getString(String name) {
        Object value = parameters.get(name);
        return value != null ? Optional.of(value.toString()) : Optional.empty();
    }
    
    /**
     * Get parameter as String with default value
     */
    public String getString(String name, String defaultValue) {
        return getString(name).orElse(defaultValue);
    }
    
    /**
     * Get required String parameter (throws exception if missing)
     */
    public String getRequiredString(String name) {
        return getString(name)
            .orElseThrow(() -> new IllegalArgumentException("Required parameter '" + name + "' is missing"));
    }
    
    /**
     * Get parameter as Integer with null safety and validation
     */
    public Optional<Integer> getInteger(String name) {
        Object value = parameters.get(name);
        if (value == null) return Optional.empty();
        
        if (value instanceof Integer) {
            return Optional.of((Integer) value);
        } else if (value instanceof Number) {
            return Optional.of(((Number) value).intValue());
        } else {
            try {
                return Optional.of(Integer.parseInt(value.toString()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Parameter '" + name + "' must be a valid integer, got: " + value);
            }
        }
    }
    
    /**
     * Get parameter as Integer with default value
     */
    public Integer getInteger(String name, Integer defaultValue) {
        return getInteger(name).orElse(defaultValue);
    }
    
    /**
     * Get required Integer parameter (throws exception if missing or invalid)
     */
    public Integer getRequiredInteger(String name) {
        return getInteger(name)
            .orElseThrow(() -> new IllegalArgumentException("Required integer parameter '" + name + "' is missing"));
    }
    
    /**
     * Get parameter as Long with null safety and validation
     */
    public Optional<Long> getLong(String name) {
        Object value = parameters.get(name);
        if (value == null) return Optional.empty();
        
        if (value instanceof Long) {
            return Optional.of((Long) value);
        } else if (value instanceof Number) {
            return Optional.of(((Number) value).longValue());
        } else {
            try {
                return Optional.of(Long.parseLong(value.toString()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Parameter '" + name + "' must be a valid long, got: " + value);
            }
        }
    }
    
    /**
     * Get parameter as Long with default value
     */
    public Long getLong(String name, Long defaultValue) {
        return getLong(name).orElse(defaultValue);
    }
    
    /**
     * Get parameter as Double with null safety and validation
     */
    public Optional<Double> getDouble(String name) {
        Object value = parameters.get(name);
        if (value == null) return Optional.empty();
        
        if (value instanceof Double) {
            return Optional.of((Double) value);
        } else if (value instanceof Number) {
            return Optional.of(((Number) value).doubleValue());
        } else {
            try {
                return Optional.of(Double.parseDouble(value.toString()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Parameter '" + name + "' must be a valid double, got: " + value);
            }
        }
    }
    
    /**
     * Get parameter as Double with default value
     */
    public Double getDouble(String name, Double defaultValue) {
        return getDouble(name).orElse(defaultValue);
    }
    
    /**
     * Get parameter as Boolean with null safety
     */
    public Optional<Boolean> getBoolean(String name) {
        Object value = parameters.get(name);
        if (value == null) return Optional.empty();
        
        if (value instanceof Boolean) {
            return Optional.of((Boolean) value);
        } else {
            String stringValue = value.toString().toLowerCase();
            return Optional.of("true".equals(stringValue) || "1".equals(stringValue) || "yes".equals(stringValue));
        }
    }
    
    /**
     * Get parameter as Boolean with default value
     */
    public Boolean getBoolean(String name, Boolean defaultValue) {
        return getBoolean(name).orElse(defaultValue);
    }
    
    /**
     * Get all parameter names
     */
    public Set<String> getParameterNames() {
        return parameters.keySet();
    }
    
    /**
     * Get all parameters as Map (for backward compatibility)
     */
    public Map<String, Object> asMap() {
        return new HashMap<>(parameters);
    }
    
    /**
     * Check if parameters are empty
     */
    public boolean isEmpty() {
        return parameters.isEmpty();
    }
    
    /**
     * Get number of parameters
     */
    public int size() {
        return parameters.size();
    }
    
    /**
     * Validate that all required parameters are present
     */
    public void validateRequired(String... requiredParams) {
        for (String param : requiredParams) {
            if (!hasParameter(param)) {
                throw new IllegalArgumentException("Required parameter '" + param + "' is missing");
            }
        }
    }
    
    /**
     * Validate integer parameter is within range
     */
    public void validateIntegerRange(String name, int min, int max) {
        getInteger(name).ifPresent(value -> {
            if (value < min || value > max) {
                throw new IllegalArgumentException(
                    "Parameter '" + name + "' must be between " + min + " and " + max + ", got: " + value);
            }
        });
    }
    
    @Override
    public String toString() {
        return "RequestParameters{" + parameters + "}";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestParameters that = (RequestParameters) o;
        return parameters.equals(that.parameters);
    }
    
    @Override
    public int hashCode() {
        return parameters.hashCode();
    }
}

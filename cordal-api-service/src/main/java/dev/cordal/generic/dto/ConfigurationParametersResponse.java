package dev.cordal.generic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Type-safe response for configuration parameters information
 * Replaces Map<String, Object> for parameter methods (getEndpointParameters, getQueryParameters, etc.)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfigurationParametersResponse<T> {
    
    @JsonProperty("configType")
    private final String configType;
    
    @JsonProperty("parameters")
    private final Map<String, List<T>> parameters;
    
    @JsonProperty("totalConfigurations")
    private final int totalConfigurations;
    
    @JsonProperty("configurationsWithParameters")
    private final int configurationsWithParameters;
    
    @JsonProperty("timestamp")
    private final Instant timestamp;
    
    /**
     * Constructor
     */
    public ConfigurationParametersResponse(String configType, Map<String, List<T>> parameters,
                                         int totalConfigurations, int configurationsWithParameters,
                                         Instant timestamp) {
        this.configType = configType;
        this.parameters = parameters;
        this.totalConfigurations = totalConfigurations;
        this.configurationsWithParameters = configurationsWithParameters;
        this.timestamp = timestamp;
    }
    
    /**
     * Static factory method
     */
    public static <T> ConfigurationParametersResponse<T> of(String configType, Map<String, List<T>> parameters,
                                                           int totalConfigurations) {
        return new ConfigurationParametersResponse<>(
            configType,
            parameters,
            totalConfigurations,
            parameters.size(),
            Instant.now()
        );
    }
    
    // Getters
    public String getConfigType() {
        return configType;
    }
    
    public Map<String, List<T>> getParameters() {
        return parameters;
    }
    
    public int getTotalConfigurations() {
        return totalConfigurations;
    }
    
    public int getConfigurationsWithParameters() {
        return configurationsWithParameters;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    /**
     * Get parameters for specific configuration
     */
    public List<T> getParametersFor(String configurationName) {
        return parameters.get(configurationName);
    }
    
    /**
     * Check if configuration has parameters
     */
    public boolean hasParameters(String configurationName) {
        List<T> params = parameters.get(configurationName);
        return params != null && !params.isEmpty();
    }
    
    /**
     * Get all configuration names that have parameters
     */
    public Set<String> getConfigurationNamesWithParameters() {
        return parameters.keySet();
    }
    
    /**
     * Get total parameter count across all configurations
     */
    public int getTotalParameterCount() {
        return parameters.values().stream()
            .mapToInt(List::size)
            .sum();
    }
    
    /**
     * Get percentage of configurations with parameters
     */
    public double getParameterCoveragePercentage() {
        if (totalConfigurations == 0) return 0.0;
        return (double) configurationsWithParameters / totalConfigurations * 100.0;
    }
    
    /**
     * Check if any configurations have parameters
     */
    public boolean hasAnyParameters() {
        return configurationsWithParameters > 0;
    }
    
    /**
     * Get configurations without parameters count
     */
    public int getConfigurationsWithoutParameters() {
        return totalConfigurations - configurationsWithParameters;
    }
    
    /**
     * Convert to Map<String, Object> for backward compatibility
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("configType", configType);
        map.put("parameters", parameters);
        map.put("totalConfigurations", totalConfigurations);
        map.put("configurationsWithParameters", configurationsWithParameters);
        map.put("timestamp", timestamp.toEpochMilli());
        return map;
    }
    
    @Override
    public String toString() {
        return "ConfigurationParametersResponse{" +
                "configType='" + configType + '\'' +
                ", totalConfigurations=" + totalConfigurations +
                ", configurationsWithParameters=" + configurationsWithParameters +
                ", totalParameterCount=" + getTotalParameterCount() +
                ", timestamp=" + timestamp +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        ConfigurationParametersResponse<?> that = (ConfigurationParametersResponse<?>) o;
        
        if (totalConfigurations != that.totalConfigurations) return false;
        if (configurationsWithParameters != that.configurationsWithParameters) return false;
        if (!configType.equals(that.configType)) return false;
        if (!parameters.equals(that.parameters)) return false;
        return timestamp.equals(that.timestamp);
    }
    
    @Override
    public int hashCode() {
        int result = configType.hashCode();
        result = 31 * result + parameters.hashCode();
        result = 31 * result + totalConfigurations;
        result = 31 * result + configurationsWithParameters;
        result = 31 * result + timestamp.hashCode();
        return result;
    }
}

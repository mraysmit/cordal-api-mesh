package dev.cordal.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

/**
 * Abstract base configuration class providing common configuration patterns
 * Common configuration framework used across all modules
 */
public abstract class BaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(BaseConfig.class);
    
    protected Map<String, Object> configData;
    protected final ObjectMapper yamlMapper;

    public BaseConfig() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        loadConfiguration();
    }

    /**
     * Abstract method to get the configuration file name
     */
    protected abstract String getConfigFileName();

    /**
     * Load configuration from YAML file
     */
    protected void loadConfiguration() {
        String configFileName = getConfigFileName();
        logger.info("Loading configuration from: {}", configFileName);

        if (configFileName == null) {
            logger.warn("Configuration file name is null, using empty configuration");
            configData = Map.of();
            return;
        }

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configFileName)) {
            if (inputStream == null) {
                logger.warn("Configuration file not found: {}, using defaults", configFileName);
                configData = Map.of();
                return;
            }
            
            Yaml yaml = new Yaml();
            configData = yaml.load(inputStream);
            
            if (configData == null) {
                configData = Map.of();
            }
            
            logger.info("Configuration loaded successfully from: {}", configFileName);
            
        } catch (Exception e) {
            logger.error("Failed to load configuration from: {}", configFileName, e);
            configData = Map.of();
        }
    }

    /**
     * Get a nested configuration value with type safety
     */
    @SuppressWarnings("unchecked")
    protected <T> T getNestedValue(String path, Class<T> type, T defaultValue) {
        try {
            String[] parts = path.split("\\.");
            Object current = configData;
            
            for (String part : parts) {
                if (current instanceof Map) {
                    current = ((Map<String, Object>) current).get(part);
                } else {
                    return defaultValue;
                }
                
                if (current == null) {
                    return defaultValue;
                }
            }
            
            if (type.isInstance(current)) {
                return type.cast(current);
            } else if (type == Integer.class && current instanceof Number) {
                return type.cast(((Number) current).intValue());
            } else if (type == Long.class && current instanceof Number) {
                return type.cast(((Number) current).longValue());
            } else if (type == Double.class && current instanceof Number) {
                return type.cast(((Number) current).doubleValue());
            } else if (type == Boolean.class && current instanceof Boolean) {
                return type.cast(current);
            } else if (type == String.class) {
                return type.cast(current.toString());
            }
            
            return defaultValue;
            
        } catch (Exception e) {
            logger.warn("Failed to get configuration value for path: {}, using default", path, e);
            return defaultValue;
        }
    }

    /**
     * Get a string configuration value
     */
    protected String getString(String path, String defaultValue) {
        return getNestedValue(path, String.class, defaultValue);
    }

    /**
     * Get an integer configuration value
     */
    protected Integer getInteger(String path, Integer defaultValue) {
        return getNestedValue(path, Integer.class, defaultValue);
    }

    /**
     * Get a boolean configuration value
     */
    protected Boolean getBoolean(String path, Boolean defaultValue) {
        return getNestedValue(path, Boolean.class, defaultValue);
    }

    /**
     * Get a long configuration value
     */
    protected Long getLong(String path, Long defaultValue) {
        return getNestedValue(path, Long.class, defaultValue);
    }

    /**
     * Get a double configuration value
     */
    protected Double getDouble(String path, Double defaultValue) {
        return getNestedValue(path, Double.class, defaultValue);
    }

    /**
     * Get the raw configuration data
     */
    public Map<String, Object> getConfigData() {
        return configData;
    }

    /**
     * Reload configuration
     */
    public void reload() {
        logger.info("Reloading configuration");
        loadConfiguration();
    }
}

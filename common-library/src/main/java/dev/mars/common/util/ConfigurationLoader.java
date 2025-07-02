package dev.mars.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;

/**
 * Utility class for loading YAML configuration files
 * Common configuration loading utility used across all modules
 */
public class ConfigurationLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationLoader.class);
    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    /**
     * Load configuration from a YAML file in the classpath
     */
    public static Map<String, Object> loadConfiguration(String fileName) {
        logger.info("Loading configuration from: {}", fileName);
        
        try (InputStream inputStream = ConfigurationLoader.class.getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                logger.warn("Configuration file not found: {}", fileName);
                return Map.of();
            }
            
            Map<String, Object> config = yamlMapper.readValue(inputStream, Map.class);
            
            if (config == null) {
                config = Map.of();
            }
            
            logger.info("Configuration loaded successfully from: {} with {} top-level keys", 
                       fileName, config.size());
            return config;
            
        } catch (Exception e) {
            logger.error("Failed to load configuration from: {}", fileName, e);
            return Map.of();
        }
    }

    /**
     * Load and parse configuration into a specific type
     */
    public static <T> T loadConfiguration(String fileName, Class<T> configClass) {
        logger.info("Loading configuration from: {} into {}", fileName, configClass.getSimpleName());
        
        try (InputStream inputStream = ConfigurationLoader.class.getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                logger.warn("Configuration file not found: {}", fileName);
                return null;
            }
            
            T config = yamlMapper.readValue(inputStream, configClass);
            logger.info("Configuration loaded successfully from: {} into {}", fileName, configClass.getSimpleName());
            return config;
            
        } catch (Exception e) {
            logger.error("Failed to load configuration from: {} into {}", fileName, configClass.getSimpleName(), e);
            return null;
        }
    }

    /**
     * Get a nested value from configuration map
     */
    @SuppressWarnings("unchecked")
    public static <T> T getNestedValue(Map<String, Object> config, String path, Class<T> type, T defaultValue) {
        try {
            String[] parts = path.split("\\.");
            Object current = config;
            
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
}

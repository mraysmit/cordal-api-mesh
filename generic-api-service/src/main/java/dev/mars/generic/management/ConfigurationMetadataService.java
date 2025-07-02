package dev.mars.generic.management;

import dev.mars.config.GenericApiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to track configuration metadata including load times, file paths, and content
 */
@Singleton
public class ConfigurationMetadataService {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationMetadataService.class);
    
    private final GenericApiConfig genericApiConfig;
    private final Map<String, ConfigurationFileMetadata> configurationMetadata;
    private final Instant serviceStartTime;
    
    @Inject
    public ConfigurationMetadataService(GenericApiConfig genericApiConfig) {
        this.genericApiConfig = genericApiConfig;
        this.configurationMetadata = new ConcurrentHashMap<>();
        this.serviceStartTime = Instant.now();
        
        logger.info("Configuration metadata service initialized at {}", serviceStartTime);
        initializeMetadata();
    }
    
    private void initializeMetadata() {
        // Record metadata for each configuration file
        recordConfigurationFile("databases", genericApiConfig.getDatabasesConfigPath());
        recordConfigurationFile("queries", genericApiConfig.getQueriesConfigPath());
        recordConfigurationFile("endpoints", genericApiConfig.getEndpointsConfigPath());
        recordConfigurationFile("application", "application.yml");
    }
    
    private void recordConfigurationFile(String configType, String filePath) {
        ConfigurationFileMetadata metadata = new ConfigurationFileMetadata(
            configType,
            filePath,
            Instant.now(),
            "LOADED"
        );
        configurationMetadata.put(configType, metadata);
        logger.debug("Recorded metadata for {} configuration: {}", configType, filePath);
    }
    
    /**
     * Get all configuration metadata
     */
    public Map<String, Object> getConfigurationMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        
        metadata.put("serviceStartTime", serviceStartTime);
        metadata.put("configurationPaths", getConfigurationPaths());
        metadata.put("configurationFiles", configurationMetadata);
        metadata.put("lastRefresh", Instant.now());
        
        return metadata;
    }
    
    /**
     * Get configuration file paths
     */
    public Map<String, String> getConfigurationPaths() {
        Map<String, String> paths = new HashMap<>();
        paths.put("databases", genericApiConfig.getDatabasesConfigPath());
        paths.put("queries", genericApiConfig.getQueriesConfigPath());
        paths.put("endpoints", genericApiConfig.getEndpointsConfigPath());
        paths.put("application", "application.yml");
        return paths;
    }
    
    /**
     * Get configuration file contents (simulated - in real implementation would read from classpath)
     */
    public Map<String, Object> getConfigurationFileContents() {
        Map<String, Object> contents = new HashMap<>();
        
        // Note: In a real implementation, you would read the actual file contents
        // For now, we'll return metadata about what would be loaded
        contents.put("databases", Map.of(
            "path", genericApiConfig.getDatabasesConfigPath(),
            "status", "Available",
            "note", "File content would be loaded from classpath"
        ));
        
        contents.put("queries", Map.of(
            "path", genericApiConfig.getQueriesConfigPath(),
            "status", "Available",
            "note", "File content would be loaded from classpath"
        ));
        
        contents.put("endpoints", Map.of(
            "path", genericApiConfig.getEndpointsConfigPath(),
            "status", "Available",
            "note", "File content would be loaded from classpath"
        ));
        
        return contents;
    }
    
    /**
     * Update configuration file status
     */
    public void updateConfigurationStatus(String configType, String status) {
        ConfigurationFileMetadata metadata = configurationMetadata.get(configType);
        if (metadata != null) {
            metadata.setStatus(status);
            metadata.setLastModified(Instant.now());
            logger.debug("Updated {} configuration status to: {}", configType, status);
        }
    }
    
    /**
     * Configuration file metadata holder
     */
    public static class ConfigurationFileMetadata {
        private final String configType;
        private final String filePath;
        private final Instant loadTime;
        private String status;
        private Instant lastModified;
        
        public ConfigurationFileMetadata(String configType, String filePath, Instant loadTime, String status) {
            this.configType = configType;
            this.filePath = filePath;
            this.loadTime = loadTime;
            this.status = status;
            this.lastModified = loadTime;
        }
        
        // Getters and setters
        public String getConfigType() { return configType; }
        public String getFilePath() { return filePath; }
        public Instant getLoadTime() { return loadTime; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Instant getLastModified() { return lastModified; }
        public void setLastModified(Instant lastModified) { this.lastModified = lastModified; }
    }
}

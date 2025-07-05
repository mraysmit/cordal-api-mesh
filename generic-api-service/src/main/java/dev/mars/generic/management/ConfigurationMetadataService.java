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
        // Record metadata for configuration directories and patterns
        recordConfigurationDirectories("databases", genericApiConfig.getConfigDirectories(), genericApiConfig.getDatabasePatterns());
        recordConfigurationDirectories("queries", genericApiConfig.getConfigDirectories(), genericApiConfig.getQueryPatterns());
        recordConfigurationDirectories("endpoints", genericApiConfig.getConfigDirectories(), genericApiConfig.getEndpointPatterns());
        recordConfigurationFile("application", "application.yml");
    }

    private void recordConfigurationDirectories(String configType, java.util.List<String> directories, java.util.List<String> patterns) {
        ConfigurationFileMetadata metadata = new ConfigurationFileMetadata(
            configType,
            "Directories: " + directories + ", Patterns: " + patterns,
            Instant.now(),
            "LOADED"
        );
        configurationMetadata.put(configType, metadata);
        logger.debug("Recorded metadata for {} configuration: directories={}, patterns={}", configType, directories, patterns);
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
     * Get configuration directories and patterns
     */
    public Map<String, Object> getConfigurationPaths() {
        Map<String, Object> paths = new HashMap<>();
        paths.put("directories", genericApiConfig.getConfigDirectories());
        paths.put("databasePatterns", genericApiConfig.getDatabasePatterns());
        paths.put("queryPatterns", genericApiConfig.getQueryPatterns());
        paths.put("endpointPatterns", genericApiConfig.getEndpointPatterns());
        paths.put("application", "application.yml");
        return paths;
    }
    
    /**
     * Get configuration file contents (simulated - in real implementation would scan directories)
     */
    public Map<String, Object> getConfigurationFileContents() {
        Map<String, Object> contents = new HashMap<>();

        // Note: In a real implementation, you would scan directories and read actual file contents
        // For now, we'll return metadata about what would be loaded
        contents.put("databases", Map.of(
            "directories", genericApiConfig.getConfigDirectories(),
            "patterns", genericApiConfig.getDatabasePatterns(),
            "status", "Available",
            "note", "Files would be discovered by scanning directories with patterns"
        ));

        contents.put("queries", Map.of(
            "directories", genericApiConfig.getConfigDirectories(),
            "patterns", genericApiConfig.getQueryPatterns(),
            "status", "Available",
            "note", "Files would be discovered by scanning directories with patterns"
        ));

        contents.put("endpoints", Map.of(
            "directories", genericApiConfig.getConfigDirectories(),
            "patterns", genericApiConfig.getEndpointPatterns(),
            "status", "Available",
            "note", "Files would be discovered by scanning directories with patterns"
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

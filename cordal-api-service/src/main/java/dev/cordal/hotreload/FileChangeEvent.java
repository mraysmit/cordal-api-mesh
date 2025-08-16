package dev.cordal.hotreload;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Event representing a configuration file change
 */
public class FileChangeEvent {
    private final Path filePath;
    private final WatchEvent.Kind<?> eventKind;
    private final long timestamp;
    private final String fileName;
    private final String fileExtension;
    private final ConfigurationFileType fileType;
    
    public FileChangeEvent(Path filePath, WatchEvent.Kind<?> eventKind, long timestamp) {
        this.filePath = filePath;
        this.eventKind = eventKind;
        this.timestamp = timestamp;
        this.fileName = filePath.getFileName().toString();
        this.fileExtension = getFileExtension(fileName);
        this.fileType = determineFileType(fileName);
    }
    
    public Path getFilePath() {
        return filePath;
    }
    
    public WatchEvent.Kind<?> getEventKind() {
        return eventKind;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public String getFileExtension() {
        return fileExtension;
    }
    
    public ConfigurationFileType getFileType() {
        return fileType;
    }
    
    public LocalDateTime getTimestampAsDateTime() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }
    
    public boolean isCreate() {
        return eventKind.name().equals("ENTRY_CREATE");
    }
    
    public boolean isModify() {
        return eventKind.name().equals("ENTRY_MODIFY");
    }
    
    public boolean isDelete() {
        return eventKind.name().equals("ENTRY_DELETE");
    }
    
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }
    
    private ConfigurationFileType determineFileType(String fileName) {
        String lowerFileName = fileName.toLowerCase();
        
        if (lowerFileName.contains("endpoint") || lowerFileName.contains("api")) {
            return ConfigurationFileType.ENDPOINT;
        } else if (lowerFileName.contains("quer")) {
            return ConfigurationFileType.QUERY;
        } else if (lowerFileName.contains("database")) {
            return ConfigurationFileType.DATABASE;
        } else {
            return ConfigurationFileType.UNKNOWN;
        }
    }
    
    @Override
    public String toString() {
        return String.format("FileChangeEvent{file='%s', event='%s', type='%s', timestamp='%s'}", 
                           fileName, eventKind.name(), fileType, getTimestampAsDateTime());
    }
    
    /**
     * Enumeration of configuration file types
     */
    public enum ConfigurationFileType {
        ENDPOINT,
        QUERY, 
        DATABASE,
        UNKNOWN
    }
}

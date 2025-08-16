package dev.cordal.hotreload;

import java.time.Instant;

/**
 * Comprehensive status information for the hot reload system
 */
public class ReloadStatusInfo {
    private final boolean enabled;
    private final ConfigurationReloadManager.ReloadStatus status;
    private final int totalReloadAttempts;
    private final String lastError;
    private final ConfigurationStateStatistics stateStatistics;
    private final FileWatcherStatus fileWatcherStatus;
    private final Instant timestamp;
    
    public ReloadStatusInfo(boolean enabled,
                           ConfigurationReloadManager.ReloadStatus status,
                           int totalReloadAttempts,
                           String lastError,
                           ConfigurationStateStatistics stateStatistics,
                           FileWatcherStatus fileWatcherStatus) {
        this.enabled = enabled;
        this.status = status;
        this.totalReloadAttempts = totalReloadAttempts;
        this.lastError = lastError;
        this.stateStatistics = stateStatistics;
        this.fileWatcherStatus = fileWatcherStatus;
        this.timestamp = Instant.now();
    }
    
    // Getters
    public boolean isEnabled() { return enabled; }
    public ConfigurationReloadManager.ReloadStatus getStatus() { return status; }
    public int getTotalReloadAttempts() { return totalReloadAttempts; }
    public String getLastError() { return lastError; }
    public ConfigurationStateStatistics getStateStatistics() { return stateStatistics; }
    public FileWatcherStatus getFileWatcherStatus() { return fileWatcherStatus; }
    public Instant getTimestamp() { return timestamp; }
    
    public boolean hasError() {
        return lastError != null;
    }
    
    public boolean isHealthy() {
        return enabled && status != ConfigurationReloadManager.ReloadStatus.ERROR && 
               status != ConfigurationReloadManager.ReloadStatus.DISABLED;
    }
    
    @Override
    public String toString() {
        return String.format("ReloadStatusInfo{enabled=%s, status=%s, attempts=%d, healthy=%s, timestamp=%s}",
                           enabled, status, totalReloadAttempts, isHealthy(), timestamp);
    }
}

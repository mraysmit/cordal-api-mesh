package dev.cordal.hotreload;

import java.time.Instant;

/**
 * Statistics about the configuration state manager
 */
public class ConfigurationStateStatistics {
    private final int totalSnapshots;
    private final String currentVersion;
    private final Instant currentTimestamp;
    private final int maxSnapshotHistory;
    
    public ConfigurationStateStatistics(int totalSnapshots, String currentVersion, 
                                      Instant currentTimestamp, int maxSnapshotHistory) {
        this.totalSnapshots = totalSnapshots;
        this.currentVersion = currentVersion;
        this.currentTimestamp = currentTimestamp;
        this.maxSnapshotHistory = maxSnapshotHistory;
    }
    
    public int getTotalSnapshots() {
        return totalSnapshots;
    }
    
    public String getCurrentVersion() {
        return currentVersion;
    }
    
    public Instant getCurrentTimestamp() {
        return currentTimestamp;
    }
    
    public int getMaxSnapshotHistory() {
        return maxSnapshotHistory;
    }
    
    @Override
    public String toString() {
        return String.format("ConfigurationStateStatistics{snapshots=%d/%d, current='%s', timestamp='%s'}",
                           totalSnapshots, maxSnapshotHistory, currentVersion, currentTimestamp);
    }
}

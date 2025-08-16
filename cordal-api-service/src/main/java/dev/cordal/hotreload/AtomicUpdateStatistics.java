package dev.cordal.hotreload;

/**
 * Statistics for the atomic update manager
 */
public class AtomicUpdateStatistics {
    private final boolean updateInProgress;
    private final String currentUpdateId;
    
    public AtomicUpdateStatistics(boolean updateInProgress, String currentUpdateId) {
        this.updateInProgress = updateInProgress;
        this.currentUpdateId = currentUpdateId;
    }
    
    public boolean isUpdateInProgress() { return updateInProgress; }
    public String getCurrentUpdateId() { return currentUpdateId; }
    
    @Override
    public String toString() {
        return String.format("AtomicUpdateStatistics{updateInProgress=%s, currentUpdateId='%s'}",
                           updateInProgress, currentUpdateId);
    }
}

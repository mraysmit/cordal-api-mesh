package dev.cordal.hotreload;

/**
 * Statistics for the dynamic endpoint registry
 */
public class EndpointRegistryStatistics {
    private final int totalEndpoints;
    private final int activeEndpoints;
    private final boolean updateInProgress;
    private final int totalRegistrations;
    
    public EndpointRegistryStatistics(int totalEndpoints, int activeEndpoints, 
                                    boolean updateInProgress, int totalRegistrations) {
        this.totalEndpoints = totalEndpoints;
        this.activeEndpoints = activeEndpoints;
        this.updateInProgress = updateInProgress;
        this.totalRegistrations = totalRegistrations;
    }
    
    public int getTotalEndpoints() { return totalEndpoints; }
    public int getActiveEndpoints() { return activeEndpoints; }
    public boolean isUpdateInProgress() { return updateInProgress; }
    public int getTotalRegistrations() { return totalRegistrations; }
    
    public int getInactiveEndpoints() {
        return totalEndpoints - activeEndpoints;
    }
    
    @Override
    public String toString() {
        return String.format("EndpointRegistryStatistics{total=%d, active=%d, inactive=%d, updating=%s, registrations=%d}",
                           totalEndpoints, activeEndpoints, getInactiveEndpoints(), updateInProgress, totalRegistrations);
    }
}

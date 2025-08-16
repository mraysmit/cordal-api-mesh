package dev.cordal.hotreload;

import dev.cordal.generic.config.ApiEndpointConfig;
import dev.cordal.generic.config.QueryConfig;
import dev.cordal.generic.config.DatabaseConfig;

import java.util.*;

/**
 * Represents the differences between two configuration states
 */
public class ConfigurationDelta {
    
    // Database changes
    public final Map<String, DatabaseConfig> addedDatabases = new HashMap<>();
    public final Map<String, DatabaseConfig> modifiedDatabases = new HashMap<>();
    public final Set<String> removedDatabases = new HashSet<>();
    
    // Query changes
    public final Map<String, QueryConfig> addedQueries = new HashMap<>();
    public final Map<String, QueryConfig> modifiedQueries = new HashMap<>();
    public final Set<String> removedQueries = new HashSet<>();
    
    // Endpoint changes
    public final Map<String, ApiEndpointConfig> addedEndpoints = new HashMap<>();
    public final Map<String, ApiEndpointConfig> modifiedEndpoints = new HashMap<>();
    public final Set<String> removedEndpoints = new HashSet<>();
    
    /**
     * Check if this delta contains any changes
     */
    public boolean hasChanges() {
        return hasDatabaseChanges() || hasQueryChanges() || hasEndpointChanges();
    }
    
    /**
     * Check if there are any database changes
     */
    public boolean hasDatabaseChanges() {
        return !addedDatabases.isEmpty() || !modifiedDatabases.isEmpty() || !removedDatabases.isEmpty();
    }
    
    /**
     * Check if there are any query changes
     */
    public boolean hasQueryChanges() {
        return !addedQueries.isEmpty() || !modifiedQueries.isEmpty() || !removedQueries.isEmpty();
    }
    
    /**
     * Check if there are any endpoint changes
     */
    public boolean hasEndpointChanges() {
        return !addedEndpoints.isEmpty() || !modifiedEndpoints.isEmpty() || !removedEndpoints.isEmpty();
    }
    
    /**
     * Get total number of changes
     */
    public int getTotalChanges() {
        return addedDatabases.size() + modifiedDatabases.size() + removedDatabases.size() +
               addedQueries.size() + modifiedQueries.size() + removedQueries.size() +
               addedEndpoints.size() + modifiedEndpoints.size() + removedEndpoints.size();
    }
    
    /**
     * Get a summary of changes
     */
    public ConfigurationDeltaSummary getSummary() {
        return new ConfigurationDeltaSummary(
            addedDatabases.size(), modifiedDatabases.size(), removedDatabases.size(),
            addedQueries.size(), modifiedQueries.size(), removedQueries.size(),
            addedEndpoints.size(), modifiedEndpoints.size(), removedEndpoints.size()
        );
    }
    
    /**
     * Get all affected database names
     */
    public Set<String> getAffectedDatabases() {
        Set<String> affected = new HashSet<>();
        affected.addAll(addedDatabases.keySet());
        affected.addAll(modifiedDatabases.keySet());
        affected.addAll(removedDatabases);
        return affected;
    }
    
    /**
     * Get all affected query names
     */
    public Set<String> getAffectedQueries() {
        Set<String> affected = new HashSet<>();
        affected.addAll(addedQueries.keySet());
        affected.addAll(modifiedQueries.keySet());
        affected.addAll(removedQueries);
        return affected;
    }
    
    /**
     * Get all affected endpoint names
     */
    public Set<String> getAffectedEndpoints() {
        Set<String> affected = new HashSet<>();
        affected.addAll(addedEndpoints.keySet());
        affected.addAll(modifiedEndpoints.keySet());
        affected.addAll(removedEndpoints);
        return affected;
    }
    
    @Override
    public String toString() {
        return String.format("ConfigurationDelta{databases=[+%d,~%d,-%d], queries=[+%d,~%d,-%d], endpoints=[+%d,~%d,-%d]}",
                           addedDatabases.size(), modifiedDatabases.size(), removedDatabases.size(),
                           addedQueries.size(), modifiedQueries.size(), removedQueries.size(),
                           addedEndpoints.size(), modifiedEndpoints.size(), removedEndpoints.size());
    }
    
    /**
     * Summary of configuration delta changes
     */
    public static class ConfigurationDeltaSummary {
        private final int addedDatabases, modifiedDatabases, removedDatabases;
        private final int addedQueries, modifiedQueries, removedQueries;
        private final int addedEndpoints, modifiedEndpoints, removedEndpoints;
        
        public ConfigurationDeltaSummary(int addedDatabases, int modifiedDatabases, int removedDatabases,
                                       int addedQueries, int modifiedQueries, int removedQueries,
                                       int addedEndpoints, int modifiedEndpoints, int removedEndpoints) {
            this.addedDatabases = addedDatabases;
            this.modifiedDatabases = modifiedDatabases;
            this.removedDatabases = removedDatabases;
            this.addedQueries = addedQueries;
            this.modifiedQueries = modifiedQueries;
            this.removedQueries = removedQueries;
            this.addedEndpoints = addedEndpoints;
            this.modifiedEndpoints = modifiedEndpoints;
            this.removedEndpoints = removedEndpoints;
        }
        
        // Getters
        public int getAddedDatabases() { return addedDatabases; }
        public int getModifiedDatabases() { return modifiedDatabases; }
        public int getRemovedDatabases() { return removedDatabases; }
        public int getAddedQueries() { return addedQueries; }
        public int getModifiedQueries() { return modifiedQueries; }
        public int getRemovedQueries() { return removedQueries; }
        public int getAddedEndpoints() { return addedEndpoints; }
        public int getModifiedEndpoints() { return modifiedEndpoints; }
        public int getRemovedEndpoints() { return removedEndpoints; }
        
        public int getTotalChanges() {
            return addedDatabases + modifiedDatabases + removedDatabases +
                   addedQueries + modifiedQueries + removedQueries +
                   addedEndpoints + modifiedEndpoints + removedEndpoints;
        }
        
        @Override
        public String toString() {
            return String.format("ConfigurationDeltaSummary{total=%d, databases=[+%d,~%d,-%d], queries=[+%d,~%d,-%d], endpoints=[+%d,~%d,-%d]}",
                               getTotalChanges(),
                               addedDatabases, modifiedDatabases, removedDatabases,
                               addedQueries, modifiedQueries, removedQueries,
                               addedEndpoints, modifiedEndpoints, removedEndpoints);
        }
    }
}

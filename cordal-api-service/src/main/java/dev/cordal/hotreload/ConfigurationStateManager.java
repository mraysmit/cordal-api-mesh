package dev.cordal.hotreload;

import dev.cordal.generic.config.ApiEndpointConfig;
import dev.cordal.generic.config.QueryConfig;
import dev.cordal.generic.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages configuration state, versioning, and snapshots for hot reload functionality
 */
@Singleton
public class ConfigurationStateManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationStateManager.class);
    
    private final AtomicLong versionCounter = new AtomicLong(1);
    private final Map<String, ConfigurationSnapshot> snapshots = new ConcurrentHashMap<>();
    private final int maxSnapshotHistory = 10; // Keep last 10 snapshots
    
    private volatile ConfigurationSnapshot currentSnapshot;
    
    @Inject
    public ConfigurationStateManager() {
        logger.info("ConfigurationStateManager initialized");
    }
    
    /**
     * Create a snapshot of the current configuration state
     */
    public String createSnapshot(Map<String, DatabaseConfig> databases,
                               Map<String, QueryConfig> queries,
                               Map<String, ApiEndpointConfig> endpoints) {
        String version = generateVersion();
        
        ConfigurationSnapshot snapshot = new ConfigurationSnapshot(
            version,
            Instant.now(),
            new HashMap<>(databases),
            new HashMap<>(queries),
            new HashMap<>(endpoints)
        );
        
        snapshots.put(version, snapshot);
        currentSnapshot = snapshot;
        
        // Clean up old snapshots
        cleanupOldSnapshots();
        
        logger.info("Created configuration snapshot: {} (databases={}, queries={}, endpoints={})",
                   version, databases.size(), queries.size(), endpoints.size());
        
        return version;
    }
    
    /**
     * Restore configuration from a snapshot
     */
    public Optional<ConfigurationSnapshot> restoreSnapshot(String version) {
        ConfigurationSnapshot snapshot = snapshots.get(version);
        if (snapshot == null) {
            logger.warn("Configuration snapshot not found: {}", version);
            return Optional.empty();
        }
        
        currentSnapshot = snapshot;
        logger.info("Restored configuration snapshot: {}", version);
        return Optional.of(snapshot);
    }
    
    /**
     * Get the current configuration snapshot
     */
    public Optional<ConfigurationSnapshot> getCurrentSnapshot() {
        return Optional.ofNullable(currentSnapshot);
    }
    
    /**
     * Get a specific configuration snapshot
     */
    public Optional<ConfigurationSnapshot> getSnapshot(String version) {
        return Optional.ofNullable(snapshots.get(version));
    }
    
    /**
     * Get all available snapshot versions
     */
    public List<String> getAvailableVersions() {
        return new ArrayList<>(snapshots.keySet());
    }
    
    /**
     * Calculate the difference between two configuration states
     */
    public ConfigurationDelta calculateDelta(ConfigurationSnapshot oldSnapshot, 
                                           Map<String, DatabaseConfig> newDatabases,
                                           Map<String, QueryConfig> newQueries,
                                           Map<String, ApiEndpointConfig> newEndpoints) {
        
        ConfigurationDelta delta = new ConfigurationDelta();
        
        if (oldSnapshot == null) {
            // Everything is new
            delta.addedDatabases.putAll(newDatabases);
            delta.addedQueries.putAll(newQueries);
            delta.addedEndpoints.putAll(newEndpoints);
        } else {
            // Calculate differences
            calculateDatabaseDelta(delta, oldSnapshot.getDatabases(), newDatabases);
            calculateQueryDelta(delta, oldSnapshot.getQueries(), newQueries);
            calculateEndpointDelta(delta, oldSnapshot.getEndpoints(), newEndpoints);
        }
        
        logger.debug("Calculated configuration delta: added={}/{}/{}, modified={}/{}/{}, removed={}/{}/{}",
                    delta.addedDatabases.size(), delta.addedQueries.size(), delta.addedEndpoints.size(),
                    delta.modifiedDatabases.size(), delta.modifiedQueries.size(), delta.modifiedEndpoints.size(),
                    delta.removedDatabases.size(), delta.removedQueries.size(), delta.removedEndpoints.size());
        
        return delta;
    }
    
    /**
     * Validate configuration dependencies
     */
    public ConfigurationValidationResult validateDependencies(ConfigurationDelta delta,
                                                             Map<String, DatabaseConfig> allDatabases,
                                                             Map<String, QueryConfig> allQueries,
                                                             Map<String, ApiEndpointConfig> allEndpoints) {
        
        ConfigurationValidationResult result = new ConfigurationValidationResult();
        
        // Validate query dependencies on databases
        for (String queryName : delta.addedQueries.keySet()) {
            QueryConfig query = delta.addedQueries.get(queryName);
            if (!allDatabases.containsKey(query.getDatabase())) {
                result.addError("Query '" + queryName + "' references non-existent database: " + query.getDatabase());
            }
        }
        
        for (String queryName : delta.modifiedQueries.keySet()) {
            QueryConfig query = delta.modifiedQueries.get(queryName);
            if (!allDatabases.containsKey(query.getDatabase())) {
                result.addError("Modified query '" + queryName + "' references non-existent database: " + query.getDatabase());
            }
        }
        
        // Validate endpoint dependencies on queries
        for (String endpointName : delta.addedEndpoints.keySet()) {
            ApiEndpointConfig endpoint = delta.addedEndpoints.get(endpointName);
            if (!allQueries.containsKey(endpoint.getQuery())) {
                result.addError("Endpoint '" + endpointName + "' references non-existent query: " + endpoint.getQuery());
            }
        }
        
        for (String endpointName : delta.modifiedEndpoints.keySet()) {
            ApiEndpointConfig endpoint = delta.modifiedEndpoints.get(endpointName);
            if (!allQueries.containsKey(endpoint.getQuery())) {
                result.addError("Modified endpoint '" + endpointName + "' references non-existent query: " + endpoint.getQuery());
            }
        }
        
        // Check for removal of dependencies that are still in use
        for (String databaseName : delta.removedDatabases) {
            for (QueryConfig query : allQueries.values()) {
                if (databaseName.equals(query.getDatabase())) {
                    result.addError("Cannot remove database '" + databaseName + "' - still referenced by query: " + query.getName());
                }
            }
        }
        
        for (String queryName : delta.removedQueries) {
            for (ApiEndpointConfig endpoint : allEndpoints.values()) {
                if (queryName.equals(endpoint.getQuery())) {
                    result.addError("Cannot remove query '" + queryName + "' - still referenced by endpoint: " + endpoint.getPath());
                }
            }
        }
        
        logger.debug("Configuration dependency validation completed with {} errors", result.getErrors().size());
        return result;
    }
    
    /**
     * Get configuration state statistics
     */
    public ConfigurationStateStatistics getStatistics() {
        return new ConfigurationStateStatistics(
            snapshots.size(),
            currentSnapshot != null ? currentSnapshot.getVersion() : null,
            currentSnapshot != null ? currentSnapshot.getTimestamp() : null,
            maxSnapshotHistory
        );
    }
    
    private String generateVersion() {
        return Instant.now().toString() + "-v" + versionCounter.getAndIncrement();
    }
    
    private void cleanupOldSnapshots() {
        if (snapshots.size() <= maxSnapshotHistory) {
            return;
        }
        
        // Sort by timestamp and remove oldest
        List<ConfigurationSnapshot> sortedSnapshots = new ArrayList<>(snapshots.values());
        sortedSnapshots.sort(Comparator.comparing(ConfigurationSnapshot::getTimestamp));
        
        int toRemove = snapshots.size() - maxSnapshotHistory;
        for (int i = 0; i < toRemove; i++) {
            ConfigurationSnapshot oldest = sortedSnapshots.get(i);
            snapshots.remove(oldest.getVersion());
            logger.debug("Removed old configuration snapshot: {}", oldest.getVersion());
        }
    }
    
    private void calculateDatabaseDelta(ConfigurationDelta delta, 
                                      Map<String, DatabaseConfig> oldDatabases,
                                      Map<String, DatabaseConfig> newDatabases) {
        
        // Find added and modified databases
        for (Map.Entry<String, DatabaseConfig> entry : newDatabases.entrySet()) {
            String name = entry.getKey();
            DatabaseConfig newConfig = entry.getValue();
            DatabaseConfig oldConfig = oldDatabases.get(name);
            
            if (oldConfig == null) {
                delta.addedDatabases.put(name, newConfig);
            } else if (!configsEqual(oldConfig, newConfig)) {
                delta.modifiedDatabases.put(name, newConfig);
            }
        }
        
        // Find removed databases
        for (String name : oldDatabases.keySet()) {
            if (!newDatabases.containsKey(name)) {
                delta.removedDatabases.add(name);
            }
        }
    }
    
    private void calculateQueryDelta(ConfigurationDelta delta,
                                   Map<String, QueryConfig> oldQueries,
                                   Map<String, QueryConfig> newQueries) {
        
        // Find added and modified queries
        for (Map.Entry<String, QueryConfig> entry : newQueries.entrySet()) {
            String name = entry.getKey();
            QueryConfig newConfig = entry.getValue();
            QueryConfig oldConfig = oldQueries.get(name);
            
            if (oldConfig == null) {
                delta.addedQueries.put(name, newConfig);
            } else if (!configsEqual(oldConfig, newConfig)) {
                delta.modifiedQueries.put(name, newConfig);
            }
        }
        
        // Find removed queries
        for (String name : oldQueries.keySet()) {
            if (!newQueries.containsKey(name)) {
                delta.removedQueries.add(name);
            }
        }
    }
    
    private void calculateEndpointDelta(ConfigurationDelta delta,
                                      Map<String, ApiEndpointConfig> oldEndpoints,
                                      Map<String, ApiEndpointConfig> newEndpoints) {
        
        // Find added and modified endpoints
        for (Map.Entry<String, ApiEndpointConfig> entry : newEndpoints.entrySet()) {
            String name = entry.getKey();
            ApiEndpointConfig newConfig = entry.getValue();
            ApiEndpointConfig oldConfig = oldEndpoints.get(name);
            
            if (oldConfig == null) {
                delta.addedEndpoints.put(name, newConfig);
            } else if (!configsEqual(oldConfig, newConfig)) {
                delta.modifiedEndpoints.put(name, newConfig);
            }
        }
        
        // Find removed endpoints
        for (String name : oldEndpoints.keySet()) {
            if (!newEndpoints.containsKey(name)) {
                delta.removedEndpoints.add(name);
            }
        }
    }
    
    private boolean configsEqual(Object config1, Object config2) {
        // Simple equality check - in a real implementation, you might want
        // more sophisticated comparison logic
        return Objects.equals(config1, config2);
    }
}

package dev.cordal.hotreload;

import dev.cordal.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages atomic configuration updates to ensure zero-downtime deployments
 * Coordinates updates across databases, queries, and endpoints
 */
@Singleton
public class AtomicUpdateManager {
    private static final Logger logger = LoggerFactory.getLogger(AtomicUpdateManager.class);
    
    private final DatabaseManager databaseManager;
    private final DynamicEndpointRegistry endpointRegistry;
    private final AtomicBoolean updateInProgress = new AtomicBoolean(false);
    private final AtomicReference<String> currentUpdateId = new AtomicReference<>();
    
    @Inject
    public AtomicUpdateManager(DatabaseManager databaseManager, DynamicEndpointRegistry endpointRegistry) {
        this.databaseManager = databaseManager;
        this.endpointRegistry = endpointRegistry;
        logger.info("AtomicUpdateManager initialized");
    }
    
    /**
     * Apply configuration changes atomically
     */
    public AtomicUpdateResult applyChanges(ConfigurationDelta delta, ConfigurationSet newConfiguration) {
        String updateId = generateUpdateId();
        logger.info("Starting atomic update: {} (changes: {})", updateId, delta.getTotalChanges());
        
        if (!updateInProgress.compareAndSet(false, true)) {
            return new AtomicUpdateResult.Builder(updateId).failure("Another update is already in progress").build();
        }
        
        currentUpdateId.set(updateId);
        AtomicUpdateResult.Builder resultBuilder = new AtomicUpdateResult.Builder(updateId);
        
        try {
            // Phase 1: Prepare for update
            logger.debug("Phase 1: Preparing for atomic update");
            PrepareResult prepareResult = prepareUpdate(delta, newConfiguration);
            if (!prepareResult.isSuccess()) {
                return resultBuilder.failure(prepareResult.getError()).build();
            }
            
            // Phase 2: Apply database changes
            logger.debug("Phase 2: Applying database changes");
            DatabaseUpdateResult dbResult = applyDatabaseChanges(delta);
            resultBuilder.databaseResult(dbResult);
            
            if (!dbResult.isSuccess()) {
                logger.error("Database update failed, rolling back");
                rollbackDatabaseChanges(delta);
                return resultBuilder.failure("Database update failed: " + dbResult.getError()).build();
            }
            
            // Phase 3: Apply endpoint changes
            logger.debug("Phase 3: Applying endpoint changes");
            EndpointUpdateResult endpointResult = applyEndpointChanges(delta);
            resultBuilder.endpointResult(endpointResult);
            
            if (!endpointResult.isSuccess()) {
                logger.error("Endpoint update failed, rolling back");
                rollbackDatabaseChanges(delta);
                rollbackEndpointChanges(delta);
                return resultBuilder.failure("Endpoint update failed: " + endpointResult.getError()).build();
            }
            
            // Phase 4: Validate final state
            logger.debug("Phase 4: Validating final state");
            ValidationResult validationResult = validateFinalState(newConfiguration);
            resultBuilder.validationResult(validationResult);
            
            if (!validationResult.isValid()) {
                logger.error("Final state validation failed, rolling back");
                rollbackDatabaseChanges(delta);
                rollbackEndpointChanges(delta);
                return resultBuilder.failure("Final validation failed").build();
            }
            
            logger.info("Atomic update completed successfully: {}", updateId);
            return resultBuilder.success("Atomic update completed").build();
            
        } catch (Exception e) {
            logger.error("Atomic update failed with exception: {}", updateId, e);
            
            // Attempt rollback
            try {
                rollbackDatabaseChanges(delta);
                rollbackEndpointChanges(delta);
            } catch (Exception rollbackException) {
                logger.error("Rollback failed for update: {}", updateId, rollbackException);
            }
            
            return resultBuilder.failure("Update failed: " + e.getMessage()).build();
            
        } finally {
            updateInProgress.set(false);
            currentUpdateId.set(null);
        }
    }
    
    /**
     * Check if an update is currently in progress
     */
    public boolean isUpdateInProgress() {
        return updateInProgress.get();
    }
    
    /**
     * Get the current update ID if an update is in progress
     */
    public String getCurrentUpdateId() {
        return currentUpdateId.get();
    }
    
    /**
     * Get atomic update manager statistics
     */
    public AtomicUpdateStatistics getStatistics() {
        return new AtomicUpdateStatistics(
            updateInProgress.get(),
            currentUpdateId.get()
        );
    }
    
    /**
     * Prepare for the atomic update
     */
    private PrepareResult prepareUpdate(ConfigurationDelta delta, ConfigurationSet newConfiguration) {
        try {
            // Begin atomic update in endpoint registry
            if (!endpointRegistry.beginAtomicUpdate()) {
                return PrepareResult.failure("Failed to begin atomic update in endpoint registry");
            }
            
            // Validate that we can perform all required operations
            if (delta.hasEndpointChanges()) {
                // Check if we can register/unregister endpoints
                for (String endpointName : delta.addedEndpoints.keySet()) {
                    if (endpointRegistry.getActiveEndpoints().containsKey(endpointName)) {
                        return PrepareResult.failure("Endpoint already exists: " + endpointName);
                    }
                }
            }
            
            logger.debug("Atomic update preparation completed successfully");
            return PrepareResult.success();
            
        } catch (Exception e) {
            logger.error("Failed to prepare atomic update", e);
            return PrepareResult.failure("Preparation failed: " + e.getMessage());
        }
    }
    
    /**
     * Apply database configuration changes
     */
    private DatabaseUpdateResult applyDatabaseChanges(ConfigurationDelta delta) {
        DatabaseUpdateResult.Builder builder = new DatabaseUpdateResult.Builder();
        
        try {
            // Add new databases
            for (var entry : delta.addedDatabases.entrySet()) {
                String dbName = entry.getKey();
                var dbConfig = entry.getValue();
                
                try {
                    // This would integrate with the actual DatabaseManager
                    // For now, we simulate the operation
                    logger.debug("Adding database: {}", dbName);
                    builder.addedDatabase(dbName);
                } catch (Exception e) {
                    logger.error("Failed to add database: {}", dbName, e);
                    builder.addError("Failed to add database '" + dbName + "': " + e.getMessage());
                }
            }
            
            // Update modified databases
            for (var entry : delta.modifiedDatabases.entrySet()) {
                String dbName = entry.getKey();
                var dbConfig = entry.getValue();
                
                try {
                    logger.debug("Updating database: {}", dbName);
                    builder.updatedDatabase(dbName);
                } catch (Exception e) {
                    logger.error("Failed to update database: {}", dbName, e);
                    builder.addError("Failed to update database '" + dbName + "': " + e.getMessage());
                }
            }
            
            // Remove databases (done last to avoid dependency issues)
            for (String dbName : delta.removedDatabases) {
                try {
                    logger.debug("Removing database: {}", dbName);
                    builder.removedDatabase(dbName);
                } catch (Exception e) {
                    logger.error("Failed to remove database: {}", dbName, e);
                    builder.addError("Failed to remove database '" + dbName + "': " + e.getMessage());
                }
            }
            
            return builder.build();
            
        } catch (Exception e) {
            logger.error("Database update failed", e);
            return builder.addError("Database update exception: " + e.getMessage()).build();
        }
    }
    
    /**
     * Apply endpoint configuration changes
     */
    private EndpointUpdateResult applyEndpointChanges(ConfigurationDelta delta) {
        EndpointUpdateResult.Builder builder = new EndpointUpdateResult.Builder();
        
        try {
            // Remove endpoints first to avoid conflicts
            for (String endpointName : delta.removedEndpoints) {
                EndpointRegistrationResult result = endpointRegistry.unregisterEndpoint(endpointName);
                if (result.isSuccess()) {
                    builder.removedEndpoint(endpointName);
                } else {
                    builder.addError("Failed to remove endpoint '" + endpointName + "': " + result.getError());
                }
            }
            
            // Update modified endpoints
            for (var entry : delta.modifiedEndpoints.entrySet()) {
                String endpointName = entry.getKey();
                var endpointConfig = entry.getValue();
                
                EndpointRegistrationResult result = endpointRegistry.updateEndpoint(endpointName, endpointConfig);
                if (result.isSuccess()) {
                    builder.updatedEndpoint(endpointName);
                } else {
                    builder.addError("Failed to update endpoint '" + endpointName + "': " + result.getError());
                }
            }
            
            // Add new endpoints
            for (var entry : delta.addedEndpoints.entrySet()) {
                String endpointName = entry.getKey();
                var endpointConfig = entry.getValue();
                
                EndpointRegistrationResult result = endpointRegistry.registerEndpoint(endpointName, endpointConfig);
                if (result.isSuccess()) {
                    builder.addedEndpoint(endpointName);
                } else {
                    builder.addError("Failed to add endpoint '" + endpointName + "': " + result.getError());
                }
            }
            
            // Commit the atomic update
            endpointRegistry.commitAtomicUpdate();
            
            return builder.build();
            
        } catch (Exception e) {
            logger.error("Endpoint update failed", e);
            endpointRegistry.rollbackAtomicUpdate();
            return builder.addError("Endpoint update exception: " + e.getMessage()).build();
        }
    }
    
    /**
     * Validate the final state after all updates
     */
    private ValidationResult validateFinalState(ConfigurationSet newConfiguration) {
        try {
            // Validate endpoint registry state
            EndpointValidationResult endpointValidation = endpointRegistry.validateAllEndpoints();
            
            ValidationResult.Builder builder = new ValidationResult.Builder();
            
            if (!endpointValidation.isAllValid()) {
                for (var entry : endpointValidation.getInvalidEndpoints().entrySet()) {
                    builder.addError("Invalid endpoint '" + entry.getKey() + "': " + entry.getValue());
                }
            }
            
            return builder.build();
            
        } catch (Exception e) {
            logger.error("Final state validation failed", e);
            return new ValidationResult.Builder()
                .addError("Final validation exception: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * Rollback database changes
     */
    private void rollbackDatabaseChanges(ConfigurationDelta delta) {
        logger.info("Rolling back database changes");
        
        // This would implement actual rollback logic
        // For now, we just log the rollback operations
        
        for (String dbName : delta.addedDatabases.keySet()) {
            logger.debug("Rollback: removing added database: {}", dbName);
        }
        
        for (String dbName : delta.modifiedDatabases.keySet()) {
            logger.debug("Rollback: reverting modified database: {}", dbName);
        }
        
        for (String dbName : delta.removedDatabases) {
            logger.debug("Rollback: restoring removed database: {}", dbName);
        }
    }
    
    /**
     * Rollback endpoint changes
     */
    private void rollbackEndpointChanges(ConfigurationDelta delta) {
        logger.info("Rolling back endpoint changes");
        
        endpointRegistry.rollbackAtomicUpdate();
        
        // Additional rollback logic would go here
        for (String endpointName : delta.addedEndpoints.keySet()) {
            logger.debug("Rollback: removing added endpoint: {}", endpointName);
            endpointRegistry.unregisterEndpoint(endpointName);
        }
    }
    
    /**
     * Generate unique update ID
     */
    private String generateUpdateId() {
        return "update-" + System.currentTimeMillis() + "-" + System.nanoTime() % 10000;
    }
    
    /**
     * Result of preparation phase
     */
    private static class PrepareResult {
        private final boolean success;
        private final String error;
        
        private PrepareResult(boolean success, String error) {
            this.success = success;
            this.error = error;
        }
        
        public static PrepareResult success() {
            return new PrepareResult(true, null);
        }
        
        public static PrepareResult failure(String error) {
            return new PrepareResult(false, error);
        }
        
        public boolean isSuccess() { return success; }
        public String getError() { return error; }
    }
}

package dev.cordal.hotreload;

import dev.cordal.config.GenericApiConfig;
import dev.cordal.generic.config.ApiEndpointConfig;
import dev.cordal.generic.config.QueryConfig;
import dev.cordal.generic.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Orchestrates the entire configuration reload process
 * Coordinates between file watching, validation, and dynamic updates
 */
@Singleton
public class ConfigurationReloadManager implements ConfigurationChangeListener {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationReloadManager.class);
    
    private final FileWatcherService fileWatcher;
    private final ConfigurationStateManager stateManager;
    private final ValidationPipeline validationPipeline;
    private final DynamicEndpointRegistry endpointRegistry;
    private final AtomicUpdateManager atomicUpdateManager;
    private final GenericApiConfig config;
    
    private final AtomicBoolean isEnabled = new AtomicBoolean(false);
    private final AtomicInteger reloadAttempts = new AtomicInteger(0);
    private final AtomicReference<ReloadStatus> currentStatus = new AtomicReference<>(ReloadStatus.IDLE);
    private final AtomicReference<String> lastError = new AtomicReference<>();
    
    @Inject
    public ConfigurationReloadManager(
            FileWatcherService fileWatcher,
            ConfigurationStateManager stateManager,
            ValidationPipeline validationPipeline,
            DynamicEndpointRegistry endpointRegistry,
            AtomicUpdateManager atomicUpdateManager,
            GenericApiConfig config) {
        
        this.fileWatcher = fileWatcher;
        this.stateManager = stateManager;
        this.validationPipeline = validationPipeline;
        this.endpointRegistry = endpointRegistry;
        this.atomicUpdateManager = atomicUpdateManager;
        this.config = config;
        
        logger.info("ConfigurationReloadManager initialized");
    }
    
    /**
     * Initialize and start the hot reload system
     */
    public void initialize() {
        if (!config.isHotReloadEnabled()) {
            logger.info("Hot reload is disabled in configuration");
            return;
        }
        
        logger.info("Initializing hot reload system");
        
        try {
            // Register as file change listener
            fileWatcher.registerChangeListener(this);
            fileWatcher.setDebounceDelay(config.getHotReloadDebounceMs());
            
            // Start file watching if enabled
            if (config.isHotReloadWatchDirectories()) {
                startFileWatching();
            }
            
            isEnabled.set(true);
            currentStatus.set(ReloadStatus.WATCHING);
            
            logger.info("Hot reload system initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize hot reload system", e);
            currentStatus.set(ReloadStatus.ERROR);
            lastError.set("Initialization failed: " + e.getMessage());
            throw new RuntimeException("Hot reload initialization failed", e);
        }
    }
    
    /**
     * Shutdown the hot reload system
     */
    public void shutdown() {
        logger.info("Shutting down hot reload system");
        
        isEnabled.set(false);
        currentStatus.set(ReloadStatus.SHUTTING_DOWN);
        
        try {
            fileWatcher.unregisterChangeListener(this);
            fileWatcher.stopWatching();
            
            currentStatus.set(ReloadStatus.IDLE);
            logger.info("Hot reload system shutdown complete");
            
        } catch (Exception e) {
            logger.error("Error during hot reload shutdown", e);
            currentStatus.set(ReloadStatus.ERROR);
            lastError.set("Shutdown error: " + e.getMessage());
        }
    }
    
    /**
     * Handle file change events from the file watcher
     */
    @Override
    public void onConfigurationFileChanged(FileChangeEvent event) {
        if (!isEnabled.get()) {
            logger.debug("Hot reload disabled, ignoring file change: {}", event.getFileName());
            return;
        }
        
        logger.info("Configuration file changed: {} ({})", event.getFileName(), event.getEventKind().name());
        
        // Process reload asynchronously to avoid blocking file watcher
        CompletableFuture.runAsync(() -> processConfigurationReload(event))
            .exceptionally(throwable -> {
                logger.error("Async configuration reload failed", throwable);
                handleReloadFailure("Async processing failed: " + throwable.getMessage());
                return null;
            });
    }
    
    /**
     * Manually trigger a configuration reload
     */
    public ReloadResult triggerReload(ReloadRequest request) {
        if (!isEnabled.get()) {
            return ReloadResult.failure("Hot reload is not enabled");
        }
        
        if (currentStatus.get() == ReloadStatus.RELOADING) {
            return ReloadResult.failure("Reload already in progress");
        }
        
        logger.info("Manual reload triggered: {}", request);
        
        try {
            return performReload(request);
        } catch (Exception e) {
            logger.error("Manual reload failed", e);
            return ReloadResult.failure("Manual reload failed: " + e.getMessage());
        }
    }
    
    /**
     * Get current reload status
     */
    public ReloadStatusInfo getStatus() {
        return new ReloadStatusInfo(
            isEnabled.get(),
            currentStatus.get(),
            reloadAttempts.get(),
            lastError.get(),
            stateManager.getStatistics(),
            fileWatcher.getStatus()
        );
    }
    
    /**
     * Process configuration reload from file change
     */
    private void processConfigurationReload(FileChangeEvent event) {
        if (currentStatus.get() == ReloadStatus.RELOADING) {
            logger.warn("Reload already in progress, skipping file change: {}", event.getFileName());
            return;
        }
        
        ReloadRequest request = ReloadRequest.fromFileChange(event);
        performReload(request);
    }
    
    /**
     * Perform the actual configuration reload
     */
    private ReloadResult performReload(ReloadRequest request) {
        String reloadId = generateReloadId();
        logger.info("Starting configuration reload: {} (request: {})", reloadId, request);
        
        currentStatus.set(ReloadStatus.RELOADING);
        int attempt = reloadAttempts.incrementAndGet();
        
        try {
            // Phase 1: Load and parse new configuration
            logger.debug("Phase 1: Loading new configuration");
            ConfigurationSet newConfig = loadNewConfiguration(request);
            
            // Phase 2: Create snapshot of current state
            logger.debug("Phase 2: Creating current state snapshot");
            String snapshotId = createCurrentSnapshot();
            
            // Phase 3: Calculate configuration delta
            logger.debug("Phase 3: Calculating configuration delta");
            ConfigurationDelta delta = calculateDelta(newConfig);
            
            if (!delta.hasChanges()) {
                logger.info("No configuration changes detected, reload complete");
                currentStatus.set(ReloadStatus.WATCHING);
                return ReloadResult.success("No changes detected", delta);
            }
            
            // Phase 4: Validate configuration changes
            if (config.isHotReloadValidateBeforeApply()) {
                logger.debug("Phase 4: Validating configuration changes");
                ValidationResult validation = validationPipeline.validate(delta, newConfig);
                
                if (!validation.isValid()) {
                    logger.warn("Configuration validation failed: {}", validation.getErrors());
                    handleReloadFailure("Validation failed: " + validation.getErrors());
                    return ReloadResult.failure("Validation failed", validation.getErrors());
                }
            }
            
            // Phase 5: Apply configuration changes atomically
            logger.debug("Phase 5: Applying configuration changes");
            AtomicUpdateResult updateResult = atomicUpdateManager.applyChanges(delta, newConfig);
            
            if (!updateResult.isSuccess()) {
                logger.error("Atomic update failed: {}", updateResult.getError());
                
                if (config.isHotReloadRollbackOnFailure()) {
                    logger.info("Rolling back to snapshot: {}", snapshotId);
                    rollbackToSnapshot(snapshotId);
                }
                
                handleReloadFailure("Update failed: " + updateResult.getError());
                return ReloadResult.failure("Update failed", updateResult.getError());
            }
            
            // Phase 6: Create new snapshot with updated configuration
            logger.debug("Phase 6: Creating new configuration snapshot");
            String newSnapshotId = stateManager.createSnapshot(
                newConfig.getDatabases(),
                newConfig.getQueries(),
                newConfig.getEndpoints()
            );
            
            currentStatus.set(ReloadStatus.WATCHING);
            lastError.set(null);
            
            logger.info("Configuration reload completed successfully: {} -> {}", reloadId, newSnapshotId);
            return ReloadResult.success("Reload completed successfully", delta, updateResult);
            
        } catch (Exception e) {
            logger.error("Configuration reload failed: {}", reloadId, e);
            handleReloadFailure("Reload exception: " + e.getMessage());
            
            if (config.isHotReloadRollbackOnFailure() && attempt <= config.getHotReloadMaxAttempts()) {
                logger.info("Attempting rollback for failed reload: {}", reloadId);
                // Rollback logic would go here
            }
            
            return ReloadResult.failure("Reload failed: " + e.getMessage());
        }
    }
    
    /**
     * Start file watching for configuration directories
     */
    private void startFileWatching() {
        // Implementation would start watching the configured directories
        // This is a placeholder for the actual implementation
        logger.info("Starting file watching for configuration directories");
    }
    
    /**
     * Load new configuration from files
     */
    private ConfigurationSet loadNewConfiguration(ReloadRequest request) {
        // Implementation would load and parse YAML configuration files
        // This is a placeholder for the actual implementation
        logger.debug("Loading new configuration for request: {}", request);
        return new ConfigurationSet(); // Placeholder
    }
    
    /**
     * Create snapshot of current configuration state
     */
    private String createCurrentSnapshot() {
        // Implementation would get current configuration and create snapshot
        // This is a placeholder for the actual implementation
        return stateManager.createSnapshot(
            Map.of(), // Current databases
            Map.of(), // Current queries  
            Map.of()  // Current endpoints
        );
    }
    
    /**
     * Calculate delta between current and new configuration
     */
    private ConfigurationDelta calculateDelta(ConfigurationSet newConfig) {
        // Implementation would calculate actual delta
        // This is a placeholder for the actual implementation
        return new ConfigurationDelta();
    }
    
    /**
     * Rollback to a previous configuration snapshot
     */
    private void rollbackToSnapshot(String snapshotId) {
        try {
            stateManager.restoreSnapshot(snapshotId);
            logger.info("Successfully rolled back to snapshot: {}", snapshotId);
        } catch (Exception e) {
            logger.error("Rollback failed for snapshot: {}", snapshotId, e);
        }
    }
    
    /**
     * Handle reload failure
     */
    private void handleReloadFailure(String error) {
        currentStatus.set(ReloadStatus.ERROR);
        lastError.set(error);
        
        // Check if we've exceeded max attempts
        if (reloadAttempts.get() >= config.getHotReloadMaxAttempts()) {
            logger.error("Max reload attempts exceeded ({}), disabling hot reload", config.getHotReloadMaxAttempts());
            isEnabled.set(false);
            currentStatus.set(ReloadStatus.DISABLED);
        }
    }
    
    /**
     * Generate unique reload ID for tracking
     */
    private String generateReloadId() {
        return "reload-" + Instant.now().toEpochMilli() + "-" + reloadAttempts.get();
    }
    
    /**
     * Enumeration of reload status states
     */
    public enum ReloadStatus {
        IDLE,
        WATCHING,
        RELOADING,
        ERROR,
        DISABLED,
        SHUTTING_DOWN
    }
}

package dev.cordal.hotreload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Service for monitoring YAML configuration files for changes
 * Provides debounced file change notifications to prevent reload storms
 */
@Singleton
public class FileWatcherService {
    private static final Logger logger = LoggerFactory.getLogger(FileWatcherService.class);
    
    private final AtomicBoolean isWatching = new AtomicBoolean(false);
    private final Set<ConfigurationChangeListener> listeners = ConcurrentHashMap.newKeySet();
    private final Map<WatchKey, Path> watchKeys = new ConcurrentHashMap<>();
    private final Map<Path, Long> lastModifiedTimes = new ConcurrentHashMap<>();
    
    private WatchService watchService;
    private ExecutorService watcherExecutor;
    private ScheduledExecutorService debounceExecutor;
    
    // Configuration
    private long debounceDelayMs = 300; // 300ms debounce delay
    private Set<String> watchedPatterns = new HashSet<>();
    
    @Inject
    public FileWatcherService() {
        logger.info("FileWatcherService initialized");
    }
    
    /**
     * Start watching the specified directories for file changes
     */
    public synchronized void startWatching(List<Path> directories, List<String> patterns) {
        if (isWatching.get()) {
            logger.warn("File watcher is already running");
            return;
        }
        
        try {
            this.watchedPatterns = new HashSet<>(patterns);
            this.watchService = FileSystems.getDefault().newWatchService();
            this.watcherExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "FileWatcher");
                t.setDaemon(true);
                return t;
            });
            this.debounceExecutor = Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r, "FileWatcher-Debounce");
                t.setDaemon(true);
                return t;
            });
            
            // Register directories for watching
            for (Path directory : directories) {
                registerDirectory(directory);
            }
            
            // Start the watcher thread
            watcherExecutor.submit(this::watchLoop);
            isWatching.set(true);
            
            logger.info("File watcher started monitoring {} directories with patterns: {}", 
                       directories.size(), patterns);
            
        } catch (IOException e) {
            logger.error("Failed to start file watcher", e);
            stopWatching();
            throw new RuntimeException("Failed to start file watcher", e);
        }
    }
    
    /**
     * Stop watching for file changes
     */
    public synchronized void stopWatching() {
        if (!isWatching.get()) {
            return;
        }
        
        logger.info("Stopping file watcher");
        isWatching.set(false);
        
        // Shutdown executors
        if (watcherExecutor != null) {
            watcherExecutor.shutdown();
            try {
                if (!watcherExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    watcherExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                watcherExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        if (debounceExecutor != null) {
            debounceExecutor.shutdown();
            try {
                if (!debounceExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    debounceExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                debounceExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Close watch service
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                logger.warn("Error closing watch service", e);
            }
        }
        
        // Clear state
        watchKeys.clear();
        lastModifiedTimes.clear();
        
        logger.info("File watcher stopped");
    }
    
    /**
     * Register a configuration change listener
     */
    public void registerChangeListener(ConfigurationChangeListener listener) {
        listeners.add(listener);
        logger.debug("Registered configuration change listener: {}", listener.getClass().getSimpleName());
    }
    
    /**
     * Unregister a configuration change listener
     */
    public void unregisterChangeListener(ConfigurationChangeListener listener) {
        listeners.remove(listener);
        logger.debug("Unregistered configuration change listener: {}", listener.getClass().getSimpleName());
    }
    
    /**
     * Set the debounce delay in milliseconds
     */
    public void setDebounceDelay(long delayMs) {
        this.debounceDelayMs = delayMs;
        logger.debug("Set debounce delay to {}ms", delayMs);
    }
    
    /**
     * Check if the file watcher is currently running
     */
    public boolean isWatching() {
        return isWatching.get();
    }
    
    /**
     * Get the current status of the file watcher
     */
    public FileWatcherStatus getStatus() {
        return new FileWatcherStatus(
            isWatching.get(),
            watchKeys.size(),
            listeners.size(),
            new ArrayList<>(watchedPatterns),
            debounceDelayMs
        );
    }
    
    /**
     * Register a directory for watching
     */
    private void registerDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            logger.warn("Directory does not exist, skipping: {}", directory);
            return;
        }
        
        if (!Files.isDirectory(directory)) {
            logger.warn("Path is not a directory, skipping: {}", directory);
            return;
        }
        
        WatchKey key = directory.register(watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE);
        
        watchKeys.put(key, directory);
        logger.debug("Registered directory for watching: {}", directory);
    }
    
    /**
     * Main watch loop that processes file system events
     */
    private void watchLoop() {
        logger.info("File watcher loop started");
        
        while (isWatching.get()) {
            try {
                WatchKey key = watchService.poll(1, TimeUnit.SECONDS);
                if (key == null) {
                    continue; // Timeout, check if still watching
                }
                
                Path directory = watchKeys.get(key);
                if (directory == null) {
                    logger.warn("Unknown watch key, skipping events");
                    key.reset();
                    continue;
                }
                
                for (WatchEvent<?> event : key.pollEvents()) {
                    processWatchEvent(directory, event);
                }
                
                // Reset the key to receive further events
                boolean valid = key.reset();
                if (!valid) {
                    logger.warn("Watch key no longer valid for directory: {}", directory);
                    watchKeys.remove(key);
                }
                
            } catch (InterruptedException e) {
                logger.info("File watcher interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error in file watcher loop", e);
                // Continue watching despite errors
            }
        }
        
        logger.info("File watcher loop ended");
    }
    
    /**
     * Process a single watch event
     */
    private void processWatchEvent(Path directory, WatchEvent<?> event) {
        WatchEvent.Kind<?> kind = event.kind();
        
        if (kind == StandardWatchEventKinds.OVERFLOW) {
            logger.warn("File system events overflow detected");
            return;
        }
        
        @SuppressWarnings("unchecked")
        WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
        Path fileName = pathEvent.context();
        Path fullPath = directory.resolve(fileName);
        
        // Check if file matches our patterns
        if (!matchesWatchedPatterns(fileName.toString())) {
            logger.trace("File {} does not match watched patterns, ignoring", fileName);
            return;
        }
        
        // Skip temporary files and hidden files
        String fileNameStr = fileName.toString();
        if (fileNameStr.startsWith(".") || fileNameStr.endsWith(".tmp") || 
            fileNameStr.endsWith(".swp") || fileNameStr.contains("~")) {
            logger.trace("Ignoring temporary/hidden file: {}", fileName);
            return;
        }
        
        logger.debug("File system event: {} {} in {}", kind.name(), fileName, directory);
        
        // Schedule debounced notification
        scheduleDebounceNotification(fullPath, kind);
    }
    
    /**
     * Check if a filename matches any of the watched patterns
     */
    private boolean matchesWatchedPatterns(String fileName) {
        return watchedPatterns.stream()
            .anyMatch(pattern -> matchesPattern(fileName, pattern));
    }
    
    /**
     * Simple pattern matching (supports * wildcard)
     */
    private boolean matchesPattern(String fileName, String pattern) {
        // Convert glob pattern to regex
        String regex = pattern.replace("*", ".*");
        return fileName.matches(regex);
    }
    
    /**
     * Schedule a debounced notification for file changes
     */
    private void scheduleDebounceNotification(Path filePath, WatchEvent.Kind<?> eventKind) {
        // Cancel any existing scheduled notification for this file
        long currentTime = System.currentTimeMillis();
        lastModifiedTimes.put(filePath, currentTime);
        
        // Schedule new notification after debounce delay
        debounceExecutor.schedule(() -> {
            Long lastModified = lastModifiedTimes.get(filePath);
            if (lastModified != null && lastModified.equals(currentTime)) {
                // No newer modification, send notification
                notifyListeners(filePath, eventKind);
                lastModifiedTimes.remove(filePath);
            }
        }, debounceDelayMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Notify all registered listeners about a file change
     */
    private void notifyListeners(Path filePath, WatchEvent.Kind<?> eventKind) {
        if (listeners.isEmpty()) {
            logger.debug("No listeners registered for file change: {}", filePath);
            return;
        }
        
        FileChangeEvent changeEvent = new FileChangeEvent(filePath, eventKind, System.currentTimeMillis());
        
        logger.info("Notifying {} listeners about file change: {} {}", 
                   listeners.size(), eventKind.name(), filePath);
        
        for (ConfigurationChangeListener listener : listeners) {
            try {
                listener.onConfigurationFileChanged(changeEvent);
            } catch (Exception e) {
                logger.error("Error notifying listener {} about file change", 
                           listener.getClass().getSimpleName(), e);
            }
        }
    }
}

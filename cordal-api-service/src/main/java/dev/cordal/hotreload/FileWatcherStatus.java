package dev.cordal.hotreload;

import java.util.List;

/**
 * Status information for the file watcher service
 */
public class FileWatcherStatus {
    private final boolean isWatching;
    private final int watchedDirectories;
    private final int registeredListeners;
    private final List<String> watchedPatterns;
    private final long debounceDelayMs;
    
    public FileWatcherStatus(boolean isWatching, int watchedDirectories, int registeredListeners, 
                           List<String> watchedPatterns, long debounceDelayMs) {
        this.isWatching = isWatching;
        this.watchedDirectories = watchedDirectories;
        this.registeredListeners = registeredListeners;
        this.watchedPatterns = watchedPatterns;
        this.debounceDelayMs = debounceDelayMs;
    }
    
    public boolean isWatching() {
        return isWatching;
    }
    
    public int getWatchedDirectories() {
        return watchedDirectories;
    }
    
    public int getRegisteredListeners() {
        return registeredListeners;
    }
    
    public List<String> getWatchedPatterns() {
        return watchedPatterns;
    }
    
    public long getDebounceDelayMs() {
        return debounceDelayMs;
    }
    
    @Override
    public String toString() {
        return String.format("FileWatcherStatus{watching=%s, directories=%d, listeners=%d, patterns=%s, debounce=%dms}", 
                           isWatching, watchedDirectories, registeredListeners, watchedPatterns, debounceDelayMs);
    }
}

package dev.cordal.common.cache;

/**
 * Interface for listening to cache events
 */
@FunctionalInterface
public interface CacheEventListener {
    
    /**
     * Handle a cache event
     * 
     * @param event the cache event to handle
     */
    void onEvent(CacheEvent event);
}

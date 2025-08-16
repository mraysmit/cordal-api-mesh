package dev.cordal.common.cache;

import java.time.Duration;
import java.util.Optional;

/**
 * Interface for cache providers that support TTL and eviction policies
 */
public interface CacheProvider {
    
    /**
     * Get a value from the cache
     * 
     * @param key the cache key
     * @param type the expected type of the cached value
     * @return Optional containing the cached value if present and not expired, empty otherwise
     */
    <T> Optional<T> get(String key, Class<T> type);
    
    /**
     * Put a value into the cache with TTL
     * 
     * @param key the cache key
     * @param value the value to cache
     * @param ttl time to live for the cached entry
     */
    void put(String key, Object value, Duration ttl);
    
    /**
     * Put a value into the cache with default TTL
     * 
     * @param key the cache key
     * @param value the value to cache
     */
    void put(String key, Object value);
    
    /**
     * Remove a specific key from the cache
     * 
     * @param key the cache key to remove
     * @return true if the key was present and removed, false otherwise
     */
    boolean remove(String key);
    
    /**
     * Remove all keys matching a pattern
     * 
     * @param pattern the pattern to match (supports wildcards)
     * @return number of keys removed
     */
    int removePattern(String pattern);
    
    /**
     * Clear all entries from the cache
     */
    void clear();
    
    /**
     * Get the current size of the cache
     * 
     * @return number of entries in the cache
     */
    int size();
    
    /**
     * Check if the cache contains a key
     * 
     * @param key the cache key to check
     * @return true if the key exists and is not expired, false otherwise
     */
    boolean containsKey(String key);
    
    /**
     * Get cache statistics
     * 
     * @return statistics about cache performance
     */
    CacheStatistics getStatistics();
    
    /**
     * Clean up expired entries
     * This method should be called periodically to remove expired entries
     */
    void cleanup();
}

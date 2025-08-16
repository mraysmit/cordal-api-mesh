package dev.cordal.common.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Central cache manager that coordinates multiple cache instances
 */
@Singleton
public class CacheManager {
    private static final Logger logger = LoggerFactory.getLogger(CacheManager.class);
    
    private final Map<String, CacheProvider> caches = new ConcurrentHashMap<>();
    private final CacheConfiguration globalConfig;
    private final ScheduledExecutorService cleanupExecutor;
    
    /**
     * Default constructor with default configuration
     */
    public CacheManager() {
        this(new CacheConfiguration());
    }
    
    /**
     * Constructor with custom configuration
     */
    public CacheManager(CacheConfiguration config) {
        this.globalConfig = config;
        this.cleanupExecutor = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "cache-cleanup");
            t.setDaemon(true);
            return t;
        });
        
        // Schedule periodic cleanup
        cleanupExecutor.scheduleAtFixedRate(
            this::cleanupAllCaches,
            config.getCleanupIntervalSeconds(),
            config.getCleanupIntervalSeconds(),
            TimeUnit.SECONDS
        );
        
        logger.info("CacheManager initialized with global config: {}", config);
    }
    
    /**
     * Get a value from the specified cache
     * 
     * @param cacheName the name of the cache
     * @param key the cache key
     * @param type the expected type of the cached value
     * @return Optional containing the cached value if present, empty otherwise
     */
    public <T> Optional<T> get(String cacheName, String key, Class<T> type) {
        CacheProvider cache = getOrCreateCache(cacheName);
        return cache.get(key, type);
    }
    
    /**
     * Put a value into the specified cache with TTL
     * 
     * @param cacheName the name of the cache
     * @param key the cache key
     * @param value the value to cache
     * @param ttl time to live for the cached entry
     */
    public void put(String cacheName, String key, Object value, Duration ttl) {
        CacheProvider cache = getOrCreateCache(cacheName);
        cache.put(key, value, ttl);
    }
    
    /**
     * Put a value into the specified cache with default TTL
     * 
     * @param cacheName the name of the cache
     * @param key the cache key
     * @param value the value to cache
     */
    public void put(String cacheName, String key, Object value) {
        CacheProvider cache = getOrCreateCache(cacheName);
        cache.put(key, value);
    }
    
    /**
     * Remove a specific key from the specified cache
     * 
     * @param cacheName the name of the cache
     * @param key the cache key to remove
     * @return true if the key was present and removed, false otherwise
     */
    public boolean remove(String cacheName, String key) {
        CacheProvider cache = caches.get(cacheName);
        return cache != null && cache.remove(key);
    }
    
    /**
     * Remove all keys matching patterns from the specified cache
     * 
     * @param cacheName the name of the cache
     * @param patterns the patterns to match (supports wildcards)
     * @return number of keys removed
     */
    public int invalidate(String cacheName, String... patterns) {
        CacheProvider cache = caches.get(cacheName);
        if (cache == null) {
            return 0;
        }
        
        int totalRemoved = 0;
        for (String pattern : patterns) {
            totalRemoved += cache.removePattern(pattern);
        }
        
        logger.debug("Invalidated {} entries from cache '{}' using patterns: {}", 
                    totalRemoved, cacheName, String.join(", ", patterns));
        return totalRemoved;
    }
    
    /**
     * Clear all entries from the specified cache
     * 
     * @param cacheName the name of the cache
     */
    public void clear(String cacheName) {
        CacheProvider cache = caches.get(cacheName);
        if (cache != null) {
            cache.clear();
            logger.info("Cleared cache: {}", cacheName);
        }
    }
    
    /**
     * Clear all entries from all caches
     */
    public void clearAll() {
        for (Map.Entry<String, CacheProvider> entry : caches.entrySet()) {
            entry.getValue().clear();
            logger.info("Cleared cache: {}", entry.getKey());
        }
    }
    
    /**
     * Get statistics for the specified cache
     * 
     * @param cacheName the name of the cache
     * @return cache statistics, or null if cache doesn't exist
     */
    public CacheStatistics getStatistics(String cacheName) {
        CacheProvider cache = caches.get(cacheName);
        return cache != null ? cache.getStatistics() : null;
    }
    
    /**
     * Get statistics for all caches
     * 
     * @return map of cache names to their statistics
     */
    public Map<String, CacheStatistics> getAllStatistics() {
        Map<String, CacheStatistics> stats = new ConcurrentHashMap<>();
        for (Map.Entry<String, CacheProvider> entry : caches.entrySet()) {
            stats.put(entry.getKey(), entry.getValue().getStatistics());
        }
        return stats;
    }
    
    /**
     * Check if a cache exists
     * 
     * @param cacheName the name of the cache
     * @return true if the cache exists, false otherwise
     */
    public boolean cacheExists(String cacheName) {
        return caches.containsKey(cacheName);
    }
    
    /**
     * Get the names of all existing caches
     * 
     * @return set of cache names
     */
    public java.util.Set<String> getCacheNames() {
        return caches.keySet();
    }
    
    /**
     * Get or create a cache with the given name
     * 
     * @param cacheName the name of the cache
     * @return the cache provider
     */
    private CacheProvider getOrCreateCache(String cacheName) {
        return caches.computeIfAbsent(cacheName, name -> {
            InMemoryCacheProvider cache = new InMemoryCacheProvider(
                globalConfig.getMaxSize(),
                Duration.ofSeconds(globalConfig.getDefaultTtlSeconds())
            );
            logger.info("Created new cache: {} with maxSize={}, defaultTtl={}s", 
                       name, globalConfig.getMaxSize(), globalConfig.getDefaultTtlSeconds());
            return cache;
        });
    }
    
    /**
     * Cleanup expired entries from all caches
     */
    private void cleanupAllCaches() {
        try {
            for (Map.Entry<String, CacheProvider> entry : caches.entrySet()) {
                entry.getValue().cleanup();
            }
        } catch (Exception e) {
            logger.warn("Error during cache cleanup", e);
        }
    }
    
    /**
     * Shutdown the cache manager and cleanup resources
     */
    public void shutdown() {
        logger.info("Shutting down CacheManager");
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        clearAll();
    }
    
    /**
     * Configuration for the cache manager
     */
    public static class CacheConfiguration {
        private int maxSize = 1000;
        private int defaultTtlSeconds = 300; // 5 minutes
        private int cleanupIntervalSeconds = 60; // 1 minute
        
        public CacheConfiguration() {}
        
        public CacheConfiguration(int maxSize, int defaultTtlSeconds, int cleanupIntervalSeconds) {
            this.maxSize = maxSize;
            this.defaultTtlSeconds = defaultTtlSeconds;
            this.cleanupIntervalSeconds = cleanupIntervalSeconds;
        }
        
        public int getMaxSize() {
            return maxSize;
        }
        
        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }
        
        public int getDefaultTtlSeconds() {
            return defaultTtlSeconds;
        }
        
        public void setDefaultTtlSeconds(int defaultTtlSeconds) {
            this.defaultTtlSeconds = defaultTtlSeconds;
        }
        
        public int getCleanupIntervalSeconds() {
            return cleanupIntervalSeconds;
        }
        
        public void setCleanupIntervalSeconds(int cleanupIntervalSeconds) {
            this.cleanupIntervalSeconds = cleanupIntervalSeconds;
        }
        
        @Override
        public String toString() {
            return "CacheConfiguration{" +
                   "maxSize=" + maxSize +
                   ", defaultTtlSeconds=" + defaultTtlSeconds +
                   ", cleanupIntervalSeconds=" + cleanupIntervalSeconds +
                   '}';
        }
    }
}

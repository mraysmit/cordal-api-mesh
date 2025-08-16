package dev.cordal.common.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * In-memory cache provider with LRU eviction and TTL support
 */
public class InMemoryCacheProvider implements CacheProvider {
    private static final Logger logger = LoggerFactory.getLogger(InMemoryCacheProvider.class);
    
    private final int maxSize;
    private final Duration defaultTtl;
    private final Map<String, CacheEntry> cache;
    private final LinkedHashMap<String, Long> accessOrder; // For LRU tracking
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    // Statistics
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong evictionCount = new AtomicLong(0);
    
    public InMemoryCacheProvider(int maxSize, Duration defaultTtl) {
        this.maxSize = maxSize;
        this.defaultTtl = defaultTtl;
        this.cache = new ConcurrentHashMap<>();
        this.accessOrder = new LinkedHashMap<String, Long>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
                return false; // We handle eviction manually
            }
        };
        
        logger.info("Initialized InMemoryCacheProvider with maxSize={}, defaultTtl={}", maxSize, defaultTtl);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        lock.readLock().lock();
        try {
            CacheEntry entry = cache.get(key);
            
            if (entry == null) {
                missCount.incrementAndGet();
                return Optional.empty();
            }
            
            if (entry.isExpired()) {
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    // Double-check after acquiring write lock
                    entry = cache.get(key);
                    if (entry != null && entry.isExpired()) {
                        cache.remove(key);
                        accessOrder.remove(key);
                        logger.debug("Removed expired cache entry: {}", key);
                    }
                    missCount.incrementAndGet();
                    return Optional.empty();
                } finally {
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }
            }
            
            // Update access order
            updateAccessOrder(key);
            hitCount.incrementAndGet();

            // Check type compatibility before casting
            Object value = entry.getValue();
            if (!type.isInstance(value)) {
                logger.warn("Cache entry type mismatch for key {}: expected {}, got {}",
                           key, type.getSimpleName(), value.getClass().getSimpleName());
                missCount.incrementAndGet();
                return Optional.empty();
            }

            return Optional.of((T) value);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public void put(String key, Object value, Duration ttl) {
        if (key == null || value == null) {
            logger.warn("Attempted to cache null key or value");
            return;
        }
        
        lock.writeLock().lock();
        try {
            Instant expiryTime = Instant.now().plus(ttl);
            CacheEntry entry = new CacheEntry(value, expiryTime);
            
            // Check if we need to evict entries
            if (cache.size() >= maxSize && !cache.containsKey(key)) {
                evictLeastRecentlyUsed();
            }
            
            cache.put(key, entry);
            updateAccessOrder(key);
            
            logger.debug("Cached entry: key={}, ttl={}, expiryTime={}", key, ttl, expiryTime);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void put(String key, Object value) {
        put(key, value, defaultTtl);
    }
    
    @Override
    public boolean remove(String key) {
        lock.writeLock().lock();
        try {
            boolean removed = cache.remove(key) != null;
            if (removed) {
                accessOrder.remove(key);
                logger.debug("Removed cache entry: {}", key);
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public int removePattern(String pattern) {
        lock.writeLock().lock();
        try {
            String regex = pattern.replace("*", ".*").replace("?", ".");
            List<String> keysToRemove = cache.keySet().stream()
                .filter(key -> key.matches(regex))
                .toList();
            
            for (String key : keysToRemove) {
                cache.remove(key);
                accessOrder.remove(key);
            }
            
            logger.debug("Removed {} cache entries matching pattern: {}", keysToRemove.size(), pattern);
            return keysToRemove.size();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            int size = cache.size();
            cache.clear();
            accessOrder.clear();
            logger.debug("Cleared cache, removed {} entries", size);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public int size() {
        return cache.size();
    }
    
    @Override
    public boolean containsKey(String key) {
        lock.readLock().lock();
        try {
            CacheEntry entry = cache.get(key);
            return entry != null && !entry.isExpired();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public CacheStatistics getStatistics() {
        return CacheStatistics.builder()
            .hitCount(hitCount.get())
            .missCount(missCount.get())
            .evictionCount(evictionCount.get())
            .size(cache.size())
            .build();
    }
    
    @Override
    public void cleanup() {
        lock.writeLock().lock();
        try {
            List<String> expiredKeys = cache.entrySet().stream()
                .filter(entry -> entry.getValue().isExpired())
                .map(Map.Entry::getKey)
                .toList();
            
            for (String key : expiredKeys) {
                cache.remove(key);
                accessOrder.remove(key);
            }
            
            if (!expiredKeys.isEmpty()) {
                logger.debug("Cleaned up {} expired cache entries", expiredKeys.size());
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private void updateAccessOrder(String key) {
        accessOrder.put(key, System.nanoTime());
    }
    
    private void evictLeastRecentlyUsed() {
        if (accessOrder.isEmpty()) {
            return;
        }
        
        String lruKey = accessOrder.keySet().iterator().next();
        cache.remove(lruKey);
        accessOrder.remove(lruKey);
        evictionCount.incrementAndGet();
        
        logger.debug("Evicted LRU cache entry: {}", lruKey);
    }
    
    /**
     * Cache entry with expiry time
     */
    private static class CacheEntry {
        private final Object value;
        private final Instant expiryTime;
        
        public CacheEntry(Object value, Instant expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }
        
        public Object getValue() {
            return value;
        }
        
        public boolean isExpired() {
            return Instant.now().isAfter(expiryTime);
        }
        
        public Instant getExpiryTime() {
            return expiryTime;
        }
    }
}

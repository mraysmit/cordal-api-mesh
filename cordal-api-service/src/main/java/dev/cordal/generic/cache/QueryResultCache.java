package dev.cordal.generic.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.cordal.common.cache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Type-safe cache wrapper for query results
 * Eliminates unsafe casts by providing proper type handling
 */
@Singleton
public class QueryResultCache {
    private static final Logger logger = LoggerFactory.getLogger(QueryResultCache.class);
    private static final TypeReference<List<Map<String, Object>>> QUERY_RESULT_TYPE = 
        new TypeReference<List<Map<String, Object>>>() {};
    
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;
    
    @Inject
    public QueryResultCache(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Get query results from cache with type safety
     */
    public Optional<List<Map<String, Object>>> get(String cacheName, String key) {
        try {
            // First try to get as the expected type
            Optional<List> rawResult = cacheManager.get(cacheName, key, List.class);
            
            if (rawResult.isEmpty()) {
                return Optional.empty();
            }
            
            List<?> rawList = rawResult.get();
            
            // Validate that the list contains the expected type
            if (rawList.isEmpty()) {
                return Optional.of(List.of());
            }
            
            // Check first element to ensure it's a Map
            Object firstElement = rawList.get(0);
            if (!(firstElement instanceof Map)) {
                logger.warn("Cache contains unexpected type: {} for key: {}", 
                           firstElement.getClass().getSimpleName(), key);
                return Optional.empty();
            }
            
            // Safe to cast since we've validated the type
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> typedResult = (List<Map<String, Object>>) rawList;
            
            return Optional.of(typedResult);
            
        } catch (Exception e) {
            logger.warn("Error retrieving from cache for key: {}", key, e);
            return Optional.empty();
        }
    }
    
    /**
     * Put query results into cache
     */
    public void put(String cacheName, String key, List<Map<String, Object>> value, long ttlMs) {
        try {
            Duration ttl = Duration.ofMillis(ttlMs);
            cacheManager.put(cacheName, key, value, ttl);
        } catch (Exception e) {
            logger.warn("Error storing to cache for key: {}", key, e);
        }
    }
    
    /**
     * Alternative implementation using JSON serialization for complete type safety
     * This method completely eliminates any casting by using JSON serialization
     */
    public Optional<List<Map<String, Object>>> getWithSerialization(String cacheName, String key) {
        try {
            Optional<String> jsonResult = cacheManager.get(cacheName, key + "_json", String.class);
            
            if (jsonResult.isEmpty()) {
                return Optional.empty();
            }
            
            // Deserialize with proper type information
            List<Map<String, Object>> result = objectMapper.readValue(jsonResult.get(), QUERY_RESULT_TYPE);
            return Optional.of(result);
            
        } catch (Exception e) {
            logger.warn("Error deserializing from cache for key: {}", key, e);
            return Optional.empty();
        }
    }
    
    /**
     * Put query results into cache using JSON serialization
     */
    public void putWithSerialization(String cacheName, String key, List<Map<String, Object>> value, long ttlMs) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            Duration ttl = Duration.ofMillis(ttlMs);
            cacheManager.put(cacheName, key + "_json", jsonValue, ttl);
        } catch (Exception e) {
            logger.warn("Error serializing to cache for key: {}", key, e);
        }
    }
    
    /**
     * Check if cache contains key
     */
    public boolean containsKey(String cacheName, String key) {
        return cacheManager.get(cacheName, key, Object.class).isPresent();
    }
    
    /**
     * Remove from cache
     */
    public void remove(String cacheName, String key) {
        cacheManager.remove(cacheName, key);
        // Also remove JSON version if it exists
        cacheManager.remove(cacheName, key + "_json");
    }
}

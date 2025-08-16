package dev.cordal.common.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CacheManager
 */
class CacheManagerTest {

    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        CacheManager.CacheConfiguration config = new CacheManager.CacheConfiguration(10, 60, 1);
        cacheManager = new CacheManager(config);
    }

    @AfterEach
    void tearDown() {
        cacheManager.shutdown();
    }

    @Test
    void testPutAndGet() {
        cacheManager.put("cache1", "key1", "value1", Duration.ofMinutes(5));
        
        Optional<String> result = cacheManager.get("cache1", "key1", String.class);
        assertTrue(result.isPresent());
        assertEquals("value1", result.get());
    }

    @Test
    void testMultipleCaches() {
        cacheManager.put("cache1", "key1", "value1");
        cacheManager.put("cache2", "key1", "value2");
        
        Optional<String> result1 = cacheManager.get("cache1", "key1", String.class);
        Optional<String> result2 = cacheManager.get("cache2", "key1", String.class);
        
        assertTrue(result1.isPresent());
        assertTrue(result2.isPresent());
        assertEquals("value1", result1.get());
        assertEquals("value2", result2.get());
    }

    @Test
    void testRemove() {
        cacheManager.put("cache1", "key1", "value1");
        assertTrue(cacheManager.get("cache1", "key1", String.class).isPresent());
        
        boolean removed = cacheManager.remove("cache1", "key1");
        assertTrue(removed);
        assertFalse(cacheManager.get("cache1", "key1", String.class).isPresent());
    }

    @Test
    void testInvalidatePattern() {
        cacheManager.put("cache1", "user:123:profile", "profile1");
        cacheManager.put("cache1", "user:123:settings", "settings1");
        cacheManager.put("cache1", "user:456:profile", "profile2");
        cacheManager.put("cache1", "other:data", "data1");
        
        int invalidated = cacheManager.invalidate("cache1", "user:123:*");
        assertEquals(2, invalidated);
        
        assertFalse(cacheManager.get("cache1", "user:123:profile", String.class).isPresent());
        assertFalse(cacheManager.get("cache1", "user:123:settings", String.class).isPresent());
        assertTrue(cacheManager.get("cache1", "user:456:profile", String.class).isPresent());
        assertTrue(cacheManager.get("cache1", "other:data", String.class).isPresent());
    }

    @Test
    void testClear() {
        cacheManager.put("cache1", "key1", "value1");
        cacheManager.put("cache1", "key2", "value2");
        
        cacheManager.clear("cache1");
        
        assertFalse(cacheManager.get("cache1", "key1", String.class).isPresent());
        assertFalse(cacheManager.get("cache1", "key2", String.class).isPresent());
    }

    @Test
    void testClearAll() {
        cacheManager.put("cache1", "key1", "value1");
        cacheManager.put("cache2", "key1", "value2");
        
        cacheManager.clearAll();
        
        assertFalse(cacheManager.get("cache1", "key1", String.class).isPresent());
        assertFalse(cacheManager.get("cache2", "key1", String.class).isPresent());
    }

    @Test
    void testGetStatistics() {
        cacheManager.put("cache1", "key1", "value1");
        cacheManager.get("cache1", "key1", String.class); // Hit
        cacheManager.get("cache1", "nonexistent", String.class); // Miss
        
        CacheStatistics stats = cacheManager.getStatistics("cache1");
        assertNotNull(stats);
        assertEquals(1, stats.getHitCount());
        assertEquals(1, stats.getMissCount());
        assertEquals(0.5, stats.getHitRate(), 0.01);
    }

    @Test
    void testGetAllStatistics() {
        cacheManager.put("cache1", "key1", "value1");
        cacheManager.put("cache2", "key1", "value2");
        
        Map<String, CacheStatistics> allStats = cacheManager.getAllStatistics();
        assertTrue(allStats.containsKey("cache1"));
        assertTrue(allStats.containsKey("cache2"));
    }

    @Test
    void testCacheExists() {
        assertFalse(cacheManager.cacheExists("cache1"));
        
        cacheManager.put("cache1", "key1", "value1");
        assertTrue(cacheManager.cacheExists("cache1"));
    }

    @Test
    void testGetCacheNames() {
        assertTrue(cacheManager.getCacheNames().isEmpty());
        
        cacheManager.put("cache1", "key1", "value1");
        cacheManager.put("cache2", "key1", "value2");
        
        assertEquals(2, cacheManager.getCacheNames().size());
        assertTrue(cacheManager.getCacheNames().contains("cache1"));
        assertTrue(cacheManager.getCacheNames().contains("cache2"));
    }

    @Test
    void testNonExistentCache() {
        // Operations on non-existent cache should handle gracefully
        assertFalse(cacheManager.remove("nonexistent", "key1"));
        assertEquals(0, cacheManager.invalidate("nonexistent", "pattern"));
        assertNull(cacheManager.getStatistics("nonexistent"));
    }

    @Test
    void testDefaultConfiguration() {
        CacheManager defaultManager = new CacheManager();
        defaultManager.put("test", "key", "value");
        
        Optional<String> result = defaultManager.get("test", "key", String.class);
        assertTrue(result.isPresent());
        assertEquals("value", result.get());
        
        defaultManager.shutdown();
    }
}

package dev.cordal.common.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InMemoryCacheProvider
 */
class InMemoryCacheProviderTest {

    private InMemoryCacheProvider cache;

    @BeforeEach
    void setUp() {
        cache = new InMemoryCacheProvider(3, Duration.ofSeconds(1)); // Small cache for testing
    }

    @Test
    void testPutAndGet() {
        cache.put("key1", "value1", Duration.ofMinutes(5));
        
        Optional<String> result = cache.get("key1", String.class);
        assertTrue(result.isPresent());
        assertEquals("value1", result.get());
    }

    @Test
    void testGetNonExistentKey() {
        Optional<String> result = cache.get("nonexistent", String.class);
        assertFalse(result.isPresent());
    }

    @Test
    void testTtlExpiration() throws InterruptedException {
        cache.put("key1", "value1", Duration.ofMillis(100));
        
        // Should be present immediately
        assertTrue(cache.get("key1", String.class).isPresent());
        
        // Wait for expiration
        Thread.sleep(150);
        
        // Should be expired and removed
        assertFalse(cache.get("key1", String.class).isPresent());
    }

    @Test
    void testLruEviction() {
        // Fill cache to capacity
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");
        
        // Access key1 to make it recently used
        cache.get("key1", String.class);
        
        // Add another item, should evict key2 (least recently used)
        cache.put("key4", "value4");
        
        assertTrue(cache.get("key1", String.class).isPresent());
        assertFalse(cache.get("key2", String.class).isPresent()); // Should be evicted
        assertTrue(cache.get("key3", String.class).isPresent());
        assertTrue(cache.get("key4", String.class).isPresent());
    }

    @Test
    void testRemove() {
        cache.put("key1", "value1");
        assertTrue(cache.containsKey("key1"));
        
        boolean removed = cache.remove("key1");
        assertTrue(removed);
        assertFalse(cache.containsKey("key1"));
        
        // Removing non-existent key should return false
        boolean removedAgain = cache.remove("key1");
        assertFalse(removedAgain);
    }

    @Test
    void testRemovePattern() {
        // Use a larger cache for this test to avoid LRU eviction
        InMemoryCacheProvider largeCache = new InMemoryCacheProvider(10, Duration.ofSeconds(1));

        largeCache.put("user:123:profile", "profile1");
        largeCache.put("user:123:settings", "settings1");
        largeCache.put("user:456:profile", "profile2");
        largeCache.put("other:data", "data1");

        int removed = largeCache.removePattern("user:123:*");
        assertEquals(2, removed);

        assertFalse(largeCache.containsKey("user:123:profile"));
        assertFalse(largeCache.containsKey("user:123:settings"));
        assertTrue(largeCache.containsKey("user:456:profile"));
        assertTrue(largeCache.containsKey("other:data"));
    }

    @Test
    void testClear() {
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        assertEquals(2, cache.size());
        
        cache.clear();
        assertEquals(0, cache.size());
        assertFalse(cache.containsKey("key1"));
        assertFalse(cache.containsKey("key2"));
    }

    @Test
    void testSize() {
        assertEquals(0, cache.size());
        
        cache.put("key1", "value1");
        assertEquals(1, cache.size());
        
        cache.put("key2", "value2");
        assertEquals(2, cache.size());
        
        cache.remove("key1");
        assertEquals(1, cache.size());
    }

    @Test
    void testContainsKey() {
        assertFalse(cache.containsKey("key1"));
        
        cache.put("key1", "value1");
        assertTrue(cache.containsKey("key1"));
        
        cache.remove("key1");
        assertFalse(cache.containsKey("key1"));
    }

    @Test
    void testStatistics() {
        CacheStatistics stats = cache.getStatistics();
        assertEquals(0, stats.getHitCount());
        assertEquals(0, stats.getMissCount());
        
        // Cache miss
        cache.get("nonexistent", String.class);
        stats = cache.getStatistics();
        assertEquals(0, stats.getHitCount());
        assertEquals(1, stats.getMissCount());
        
        // Cache hit
        cache.put("key1", "value1");
        cache.get("key1", String.class);
        stats = cache.getStatistics();
        assertEquals(1, stats.getHitCount());
        assertEquals(1, stats.getMissCount());
        assertEquals(0.5, stats.getHitRate(), 0.01);
    }

    @Test
    void testCleanup() throws InterruptedException {
        cache.put("key1", "value1", Duration.ofMillis(50));
        cache.put("key2", "value2", Duration.ofMinutes(5));
        
        assertEquals(2, cache.size());
        
        // Wait for first key to expire
        Thread.sleep(100);
        
        // Cleanup should remove expired entries
        cache.cleanup();
        assertEquals(1, cache.size());
        assertTrue(cache.containsKey("key2"));
        assertFalse(cache.containsKey("key1"));
    }

    @Test
    void testTypeSafety() {
        cache.put("stringKey", "stringValue");
        cache.put("intKey", 42);
        
        Optional<String> stringResult = cache.get("stringKey", String.class);
        assertTrue(stringResult.isPresent());
        assertEquals("stringValue", stringResult.get());
        
        Optional<Integer> intResult = cache.get("intKey", Integer.class);
        assertTrue(intResult.isPresent());
        assertEquals(42, intResult.get());
        
        // Wrong type should return empty
        Optional<Integer> wrongType = cache.get("stringKey", Integer.class);
        assertFalse(wrongType.isPresent());
    }

    @Test
    void testNullHandling() {
        // Null key or value should be handled gracefully
        cache.put(null, "value");
        cache.put("key", null);
        
        assertEquals(0, cache.size()); // Should not add null entries
    }
}

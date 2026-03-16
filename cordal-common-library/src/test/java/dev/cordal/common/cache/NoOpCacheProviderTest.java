package dev.cordal.common.cache;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class NoOpCacheProviderTest {

    @Test
    void testNeverStoresEntries() {
        NoOpCacheProvider provider = new NoOpCacheProvider();

        provider.put("k1", "v1");
        provider.put("k2", "v2", Duration.ofSeconds(30));

        Optional<String> value1 = provider.get("k1", String.class);
        Optional<String> value2 = provider.get("k2", String.class);

        assertTrue(value1.isEmpty());
        assertTrue(value2.isEmpty());
        assertFalse(provider.containsKey("k1"));
        assertEquals(0, provider.size());
    }

    @Test
    void testMutatingOperationsAreNoOps() {
        NoOpCacheProvider provider = new NoOpCacheProvider();

        assertFalse(provider.remove("missing"));
        assertEquals(0, provider.removePattern("user:*"));
        assertDoesNotThrow(provider::clear);
        assertDoesNotThrow(provider::cleanup);
        assertDoesNotThrow(provider::close);
    }

    @Test
    void testStatisticsTrackMisses() {
        NoOpCacheProvider provider = new NoOpCacheProvider();

        provider.get("a", String.class);
        provider.get("b", String.class);

        CacheStatistics stats = provider.getStatistics();
        assertEquals(0, stats.getHitCount());
        assertEquals(2, stats.getMissCount());
        assertEquals(0, stats.getEvictionCount());
        assertEquals(0, stats.getSize());
    }
}
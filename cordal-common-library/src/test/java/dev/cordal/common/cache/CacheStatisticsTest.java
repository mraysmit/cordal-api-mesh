package dev.cordal.common.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CacheStatistics
 */
class CacheStatisticsTest {

    @Test
    void testBasicStatistics() {
        CacheStatistics stats = CacheStatistics.builder()
            .hitCount(100)
            .missCount(50)
            .evictionCount(10)
            .size(1000)
            .build();

        assertEquals(100, stats.getHitCount());
        assertEquals(50, stats.getMissCount());
        assertEquals(10, stats.getEvictionCount());
        assertEquals(1000, stats.getSize());
        assertEquals(150, stats.getTotalRequests());
    }

    @Test
    void testHitRate() {
        CacheStatistics stats = CacheStatistics.builder()
            .hitCount(80)
            .missCount(20)
            .evictionCount(5)
            .size(500)
            .build();

        assertEquals(0.8, stats.getHitRate(), 0.001);
        assertEquals(0.2, stats.getMissRate(), 0.001);
    }

    @Test
    void testZeroRequests() {
        CacheStatistics stats = CacheStatistics.builder()
            .hitCount(0)
            .missCount(0)
            .evictionCount(0)
            .size(0)
            .build();

        assertEquals(0.0, stats.getHitRate());
        assertEquals(0.0, stats.getMissRate());
        assertEquals(0, stats.getTotalRequests());
    }

    @Test
    void testOnlyHits() {
        CacheStatistics stats = CacheStatistics.builder()
            .hitCount(100)
            .missCount(0)
            .evictionCount(0)
            .size(50)
            .build();

        assertEquals(1.0, stats.getHitRate());
        assertEquals(0.0, stats.getMissRate());
    }

    @Test
    void testOnlyMisses() {
        CacheStatistics stats = CacheStatistics.builder()
            .hitCount(0)
            .missCount(100)
            .evictionCount(0)
            .size(50)
            .build();

        assertEquals(0.0, stats.getHitRate());
        assertEquals(1.0, stats.getMissRate());
    }

    @Test
    void testEqualsAndHashCode() {
        java.time.Instant fixedTime = java.time.Instant.parse("2025-01-01T00:00:00Z");

        CacheStatistics stats1 = CacheStatistics.builder()
            .hitCount(100).missCount(50).evictionCount(10).size(1000).timestamp(fixedTime).build();
        CacheStatistics stats2 = CacheStatistics.builder()
            .hitCount(100).missCount(50).evictionCount(10).size(1000).timestamp(fixedTime).build();
        CacheStatistics stats3 = CacheStatistics.builder()
            .hitCount(200).missCount(50).evictionCount(10).size(1000).timestamp(fixedTime).build();

        assertEquals(stats1, stats2);
        assertEquals(stats1.hashCode(), stats2.hashCode());
        assertNotEquals(stats1, stats3);
        assertNotEquals(stats1.hashCode(), stats3.hashCode());
    }

    @Test
    void testToString() {
        CacheStatistics stats = CacheStatistics.builder()
            .hitCount(75).missCount(25).evictionCount(5).size(500).build();
        String toString = stats.toString();

        assertTrue(toString.contains("75"));
        assertTrue(toString.contains("25"));
        assertTrue(toString.contains("5"));
        assertTrue(toString.contains("500"));
        assertTrue(toString.contains("75.00%")); // Hit rate as percentage
    }

    @Test
    void testNegativeValues() {
        // Constructor should handle negative values gracefully
        CacheStatistics stats = CacheStatistics.builder()
            .hitCount(-1).missCount(-1).evictionCount(-1).size(-1).build();

        assertEquals(-1, stats.getHitCount());
        assertEquals(-1, stats.getMissCount());
        assertEquals(-1, stats.getEvictionCount());
        assertEquals(-1, stats.getSize());
        assertEquals(-2, stats.getTotalRequests());

        // Hit rate should handle negative values
        assertTrue(Double.isNaN(stats.getHitRate()) || stats.getHitRate() >= 0);
    }

    @Test
    void testLargeNumbers() {
        long largeHits = Long.MAX_VALUE / 2;
        long largeMisses = Long.MAX_VALUE / 2;

        CacheStatistics stats = CacheStatistics.builder()
            .hitCount(largeHits).missCount(largeMisses).evictionCount(0).size(1000).build();

        assertEquals(largeHits, stats.getHitCount());
        assertEquals(largeMisses, stats.getMissCount());
        assertEquals(largeHits + largeMisses, stats.getTotalRequests());
        assertEquals(0.5, stats.getHitRate(), 0.001);
    }

    @Test
    void testPrecisionEdgeCases() {
        // Test with very small hit rate
        CacheStatistics stats1 = CacheStatistics.builder()
            .hitCount(1).missCount(999999).evictionCount(0).size(100).build();
        assertTrue(stats1.getHitRate() > 0);
        assertTrue(stats1.getHitRate() < 0.001);

        // Test with very high hit rate
        CacheStatistics stats2 = CacheStatistics.builder()
            .hitCount(999999).missCount(1).evictionCount(0).size(100).build();
        assertTrue(stats2.getHitRate() > 0.999);
        assertTrue(stats2.getHitRate() < 1.0);
    }

    @Test
    void testImmutability() {
        CacheStatistics stats = CacheStatistics.builder()
            .hitCount(100).missCount(50).evictionCount(10).size(1000).build();

        // Verify that all getters return the same values
        assertEquals(100, stats.getHitCount());
        assertEquals(50, stats.getMissCount());
        assertEquals(10, stats.getEvictionCount());
        assertEquals(1000, stats.getSize());

        // Values should not change
        assertEquals(100, stats.getHitCount());
        assertEquals(50, stats.getMissCount());
    }
}

package dev.cordal.common.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cache provider that never stores entries.
 * Useful when caching is disabled operationally but cache call sites remain active.
 */
public class NoOpCacheProvider implements CacheProvider {

    private final AtomicLong missCount = new AtomicLong(0);

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        missCount.incrementAndGet();
        return Optional.empty();
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        // Intentionally no-op.
    }

    @Override
    public void put(String key, Object value) {
        // Intentionally no-op.
    }

    @Override
    public boolean remove(String key) {
        return false;
    }

    @Override
    public int removePattern(String pattern) {
        return 0;
    }

    @Override
    public void clear() {
        // Intentionally no-op.
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean containsKey(String key) {
        return false;
    }

    @Override
    public CacheStatistics getStatistics() {
        return CacheStatistics.builder()
                .hitCount(0)
                .missCount(missCount.get())
                .evictionCount(0)
                .size(0)
                .build();
    }

    @Override
    public void cleanup() {
        // Intentionally no-op.
    }
}
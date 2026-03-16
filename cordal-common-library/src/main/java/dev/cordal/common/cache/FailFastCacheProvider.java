package dev.cordal.common.cache;

import java.time.Duration;
import java.util.Optional;

/**
 * Cache provider that always fails on access.
 * Useful in test environments where any cache interaction should be treated as a defect.
 */
public final class FailFastCacheProvider implements CacheProvider {

    private static UnsupportedOperationException fail(String operation) {
        return new UnsupportedOperationException(
                "FailFastCacheProvider: cache operation '" + operation + "' is disabled by configuration"
        );
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        throw fail("get");
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        throw fail("put(ttl)");
    }

    @Override
    public void put(String key, Object value) {
        throw fail("put");
    }

    @Override
    public boolean remove(String key) {
        throw fail("remove");
    }

    @Override
    public int removePattern(String pattern) {
        throw fail("removePattern");
    }

    @Override
    public void clear() {
        throw fail("clear");
    }

    @Override
    public int size() {
        throw fail("size");
    }

    @Override
    public boolean containsKey(String key) {
        throw fail("containsKey");
    }

    @Override
    public CacheStatistics getStatistics() {
        throw fail("getStatistics");
    }

    @Override
    public void cleanup() {
        throw fail("cleanup");
    }
}
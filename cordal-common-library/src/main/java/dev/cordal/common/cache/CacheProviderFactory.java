package dev.cordal.common.cache;

import java.time.Duration;

/**
 * Factory that creates {@link CacheProvider} instances for named caches.
 * <p>
 * Bind a custom implementation of this interface in your Guice module to replace
 * the default {@link InMemoryCacheProvider} with any backing store (e.g. Redis,
 * Caffeine, or a no-op provider for testing).
 *
 * <pre>{@code
 * // In your Guice module:
 * bind(CacheProviderFactory.class).toInstance(
 *     (name, maxSize, defaultTtl) -> new RedisCacheProvider(redisClient, name, defaultTtl)
 * );
 * }</pre>
 */
@FunctionalInterface
public interface CacheProviderFactory {

    /**
     * Create a new {@link CacheProvider} for the named cache.
     *
     * @param cacheName  logical name of the cache being created
     * @param maxSize    maximum number of entries before eviction
     * @param defaultTtl default time-to-live applied when no TTL is specified per entry
     * @return a ready-to-use {@link CacheProvider}
     */
    CacheProvider create(String cacheName, int maxSize, Duration defaultTtl);
}

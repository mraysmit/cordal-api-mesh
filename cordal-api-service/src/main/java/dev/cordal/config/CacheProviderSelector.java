package dev.cordal.config;

import dev.cordal.common.cache.CacheProviderFactory;
import dev.cordal.common.cache.FailFastCacheProvider;
import dev.cordal.common.cache.NoOpCacheProvider;

/**
 * Pure provider-selection utility to keep provider mapping testable without Guice injector startup.
 */
final class CacheProviderSelector {

    private CacheProviderSelector() {
    }

    static CacheProviderFactory select(String provider, CacheProviderFactory defaultFactory) {
        String normalized = normalize(provider);
        return switch (normalized) {
            case "noop" -> (name, maxSize, defaultTtl) -> new NoOpCacheProvider();
            case "failfast" -> (name, maxSize, defaultTtl) -> new FailFastCacheProvider();
            case "inmemory" -> defaultFactory;
            default -> defaultFactory;
        };
    }

    static boolean isKnownProvider(String provider) {
        String normalized = normalize(provider);
        return "noop".equals(normalized) || "failfast".equals(normalized) || "inmemory".equals(normalized);
    }

    private static String normalize(String provider) {
        if (provider == null || provider.isBlank()) {
            return "inmemory";
        }
        return provider.trim().toLowerCase();
    }
}
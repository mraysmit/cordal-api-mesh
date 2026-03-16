package dev.cordal.config;

import dev.cordal.common.cache.CacheProvider;
import dev.cordal.common.cache.CacheProviderFactory;
import dev.cordal.common.cache.FailFastCacheProvider;
import dev.cordal.common.cache.NoOpCacheProvider;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class CacheProviderSelectorTest {

    @Test
    void shouldSelectNoOpProvider() {
        CacheProviderFactory defaultFactory = (name, maxSize, defaultTtl) -> null;

        CacheProviderFactory selected = CacheProviderSelector.select("noop", defaultFactory);
        CacheProvider provider = selected.create("cache", 10, Duration.ofSeconds(1));

        assertThat(provider).isInstanceOf(NoOpCacheProvider.class);
    }

    @Test
    void shouldSelectFailFastProvider() {
        CacheProviderFactory defaultFactory = (name, maxSize, defaultTtl) -> null;

        CacheProviderFactory selected = CacheProviderSelector.select("failfast", defaultFactory);
        CacheProvider provider = selected.create("cache", 10, Duration.ofSeconds(1));

        assertThat(provider).isInstanceOf(FailFastCacheProvider.class);
    }

    @Test
    void shouldUseDefaultFactoryForInMemory() {
        CacheProviderFactory defaultFactory = (name, maxSize, defaultTtl) -> new NoOpCacheProvider();

        CacheProviderFactory selected = CacheProviderSelector.select("inmemory", defaultFactory);

        assertThat(selected).isSameAs(defaultFactory);
    }

    @Test
    void shouldUseDefaultFactoryForUnknownProvider() {
        CacheProviderFactory defaultFactory = (name, maxSize, defaultTtl) -> new NoOpCacheProvider();

        CacheProviderFactory selected = CacheProviderSelector.select("unknown", defaultFactory);

        assertThat(selected).isSameAs(defaultFactory);
    }

    @Test
    void shouldTreatNullOrBlankAsInMemory() {
        CacheProviderFactory defaultFactory = (name, maxSize, defaultTtl) -> new NoOpCacheProvider();

        assertThat(CacheProviderSelector.select(null, defaultFactory)).isSameAs(defaultFactory);
        assertThat(CacheProviderSelector.select("   ", defaultFactory)).isSameAs(defaultFactory);
    }

    @Test
    void shouldIdentifyKnownProviders() {
        assertThat(CacheProviderSelector.isKnownProvider("noop")).isTrue();
        assertThat(CacheProviderSelector.isKnownProvider("failfast")).isTrue();
        assertThat(CacheProviderSelector.isKnownProvider("inmemory")).isTrue();
        assertThat(CacheProviderSelector.isKnownProvider("unknown")).isFalse();
        assertThat(CacheProviderSelector.isKnownProvider(null)).isTrue();
    }
}
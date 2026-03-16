package dev.cordal.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GenericApiConfig Cache Provider Tests")
class GenericApiConfigCacheProviderTest {

    @AfterEach
    void tearDown() {
        System.clearProperty("generic.config.file");
        System.clearProperty("cache.provider");
    }

    @Test
    @DisplayName("Should load cache provider from YAML")
    void testLoadCacheProviderFromYaml() {
        System.setProperty("generic.config.file", "application-cache-provider-test.yml");

        GenericApiConfig config = GenericApiConfig.loadFromFile();

        assertThat(config.getCacheProvider()).isEqualTo("noop");
        assertThat(config.getCacheSettings().getProvider()).isEqualTo("noop");
    }

    @Test
    @DisplayName("Should default cache provider to inmemory when not configured")
    void testDefaultCacheProvider() {
        System.setProperty("generic.config.file", "application-test.yml");

        GenericApiConfig config = GenericApiConfig.loadFromFile();

        assertThat(config.getCacheProvider()).isEqualTo("inmemory");
    }

    @Test
    @DisplayName("Should let system property override YAML cache provider")
    void testSystemPropertyOverridesYamlProvider() {
        System.setProperty("generic.config.file", "application-cache-provider-test.yml");
        System.setProperty("cache.provider", "failfast");

        GenericApiConfig config = GenericApiConfig.loadFromFile();

        assertThat(config.getCacheProvider()).isEqualTo("failfast");
    }
}
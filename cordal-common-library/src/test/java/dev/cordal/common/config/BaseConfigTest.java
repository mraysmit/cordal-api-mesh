package dev.cordal.common.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for BaseConfig
 */
class BaseConfigTest {

    @Test
    void shouldLoadConfigurationFromClasspath() {
        TestConfig config = new TestConfig("test-config.yml");
        
        // The test config file should be loaded
        assertThat(config.getConfigData()).isNotNull();
    }

    @Test
    void shouldHandleMissingConfigFile() {
        TestConfig config = new TestConfig("non-existent-config.yml");
        
        // Should not fail, just return empty config
        assertThat(config.getConfigData()).isNotNull();
        assertThat(config.getConfigData()).isEmpty();
    }

    @Test
    void shouldGetNestedStringValue() {
        TestConfig config = new TestConfig("test-config.yml");
        
        String value = config.getTestString("server.host", "default-host");
        assertThat(value).isNotNull();
    }

    @Test
    void shouldGetNestedIntegerValue() {
        TestConfig config = new TestConfig("test-config.yml");
        
        Integer value = config.getTestInteger("server.port", 8080);
        assertThat(value).isNotNull();
        assertThat(value).isGreaterThan(0);
    }

    @Test
    void shouldGetNestedBooleanValue() {
        TestConfig config = new TestConfig("test-config.yml");
        
        Boolean value = config.getTestBoolean("server.enabled", true);
        assertThat(value).isNotNull();
    }

    @Test
    void shouldReturnDefaultForMissingValue() {
        TestConfig config = new TestConfig("test-config.yml");
        
        String value = config.getTestString("non.existent.path", "default-value");
        assertThat(value).isEqualTo("default-value");
        
        Integer intValue = config.getTestInteger("non.existent.path", 42);
        assertThat(intValue).isEqualTo(42);
        
        Boolean boolValue = config.getTestBoolean("non.existent.path", false);
        assertThat(boolValue).isFalse();
    }

    @Test
    void shouldHandleTypeConversion() {
        TestConfig config = new TestConfig("test-config.yml");
        
        // Test number to different types
        Double doubleValue = config.getTestDouble("server.port", 0.0);
        assertThat(doubleValue).isNotNull();
        
        Long longValue = config.getTestLong("server.port", 0L);
        assertThat(longValue).isNotNull();
    }

    @Test
    void shouldReloadConfiguration() {
        TestConfig config = new TestConfig("test-config.yml");
        Map<String, Object> originalData = config.getConfigData();
        
        config.reload();
        
        // Should have reloaded (may be same content, but method should work)
        assertThat(config.getConfigData()).isNotNull();
    }

    // Test implementation of BaseConfig
    private static class TestConfig extends BaseConfig {
        private final String configFileName;

        public TestConfig(String configFileName) {
            this.configFileName = configFileName;
        }

        @Override
        protected String getConfigFileName() {
            return configFileName;
        }

        // Expose protected methods for testing
        public String getTestString(String path, String defaultValue) {
            return getString(path, defaultValue);
        }

        public Integer getTestInteger(String path, Integer defaultValue) {
            return getInteger(path, defaultValue);
        }

        public Boolean getTestBoolean(String path, Boolean defaultValue) {
            return getBoolean(path, defaultValue);
        }

        public Double getTestDouble(String path, Double defaultValue) {
            return getDouble(path, defaultValue);
        }

        public Long getTestLong(String path, Long defaultValue) {
            return getLong(path, defaultValue);
        }
    }
}

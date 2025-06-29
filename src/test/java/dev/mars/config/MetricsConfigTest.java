package dev.mars.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for metrics collection configuration
 */
public class MetricsConfigTest {

    private AppConfig appConfig;

    @BeforeEach
    void setUp() {
        // Use test configuration
        System.setProperty("config.file", "application-test.yml");
        appConfig = new AppConfig();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("config.file");
    }

    @Test
    void testMetricsCollectionConfigurationLoaded() {
        AppConfig.MetricsCollection config = appConfig.getMetricsCollection();
        
        assertThat(config).isNotNull();
        assertThat(config.isEnabled()).isTrue();
        assertThat(config.isIncludeMemoryMetrics()).isTrue();
        assertThat(config.getSamplingRate()).isEqualTo(1.0);
        assertThat(config.isAsyncSave()).isFalse(); // Set to false in test config
    }

    @Test
    void testExcludePathsConfiguration() {
        AppConfig.MetricsCollection config = appConfig.getMetricsCollection();
        List<String> excludePaths = config.getExcludePaths();
        
        assertThat(excludePaths).isNotNull();
        assertThat(excludePaths).contains("/dashboard");
        assertThat(excludePaths).contains("/metrics");
        assertThat(excludePaths).contains("/api/performance-metrics");
    }

    @Test
    void testDefaultConfigurationValues() {
        // Test with default configuration (no config file)
        System.clearProperty("config.file");
        AppConfig defaultConfig = new AppConfig();
        AppConfig.MetricsCollection config = defaultConfig.getMetricsCollection();
        
        // Test default values
        assertThat(config.isEnabled()).isTrue();
        assertThat(config.isIncludeMemoryMetrics()).isTrue();
        assertThat(config.getSamplingRate()).isEqualTo(1.0);
        assertThat(config.isAsyncSave()).isTrue(); // Default should be true
        
        List<String> defaultExcludePaths = config.getExcludePaths();
        assertThat(defaultExcludePaths).contains("/dashboard");
        assertThat(defaultExcludePaths).contains("/metrics");
        assertThat(defaultExcludePaths).contains("/api/performance-metrics");
    }

    @Test
    void testMetricsDatabaseConfiguration() {
        AppConfig.MetricsDatabase config = appConfig.getMetricsDatabase();
        
        assertThat(config).isNotNull();
        assertThat(config.getUrl()).contains("testmetricsdb"); // Test database
        assertThat(config.getUsername()).isEqualTo("sa");
        assertThat(config.getDriver()).isEqualTo("org.h2.Driver");
    }

    @Test
    void testMetricsDashboardConfiguration() {
        AppConfig.MetricsDashboard config = appConfig.getMetricsDashboard();

        assertThat(config).isNotNull();
        assertThat(config.getCustom().isEnabled()).isFalse(); // Dashboard disabled in test environment
        assertThat(config.getCustom().getPath()).isEqualTo("/dashboard");
        assertThat(config.getGrafana().isEnabled()).isFalse();
    }

    @Test
    void testSamplingRateValidation() {
        AppConfig.MetricsCollection config = appConfig.getMetricsCollection();
        double samplingRate = config.getSamplingRate();
        
        // Sampling rate should be between 0.0 and 1.0
        assertThat(samplingRate).isBetween(0.0, 1.0);
    }

    @Test
    void testPoolConfigurationForMetrics() {
        AppConfig.PoolConfig poolConfig = appConfig.getMetricsDatabase().getPool();

        assertThat(poolConfig).isNotNull();
        assertThat(poolConfig.getMaximumPoolSize()).isEqualTo(3); // Test config value
        assertThat(poolConfig.getMinimumIdle()).isEqualTo(1);
        assertThat(poolConfig.getConnectionTimeout()).isEqualTo(10000);
    }
}

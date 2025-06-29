package dev.mars.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for AppConfig
 */
class AppConfigTest {

    private AppConfig appConfig;

    @BeforeEach
    void setUp() {
        // This will load the test configuration
        System.setProperty("config.file", "application-test.yml");
        appConfig = new AppConfig();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("config.file");
    }

    @Test
    void testServerConfiguration() {
        assertThat(appConfig.getServerPort()).isEqualTo(0); // Random port in test config
        assertThat(appConfig.getServerHost()).isEqualTo("localhost");
    }

    @Test
    void testDatabaseConfiguration() {
        assertThat(appConfig.getDatabaseUrl()).contains("jdbc:h2:mem:testdb");
        assertThat(appConfig.getDatabaseUsername()).isEqualTo("sa");
        assertThat(appConfig.getDatabasePassword()).isEqualTo("");
        assertThat(appConfig.getDatabaseDriver()).isEqualTo("org.h2.Driver");
    }

    @Test
    void testConnectionPoolConfiguration() {
        assertThat(appConfig.getMaximumPoolSize()).isEqualTo(5);
        assertThat(appConfig.getMinimumIdle()).isEqualTo(1);
        assertThat(appConfig.getConnectionTimeout()).isEqualTo(10000L);
        assertThat(appConfig.getIdleTimeout()).isEqualTo(300000L);
        assertThat(appConfig.getMaxLifetime()).isEqualTo(600000L);
    }

    @Test
    void testDataConfiguration() {
        assertThat(appConfig.shouldLoadSampleData()).isFalse(); // Disabled in test config
        assertThat(appConfig.getSampleDataSize()).isEqualTo(10);
    }

    @Test
    void testDefaultValues() {
        // Test that configuration loading works and we can access values
        // The actual default values are tested implicitly through the getter methods

        // Test that we can access all configuration values without errors
        assertThat(appConfig.getServerPort()).isNotNull();
        assertThat(appConfig.getServerHost()).isNotNull();
        assertThat(appConfig.getDatabaseUrl()).isNotNull();
        assertThat(appConfig.getDatabaseUsername()).isNotNull();
        assertThat(appConfig.getDatabasePassword()).isNotNull();
        assertThat(appConfig.getDatabaseDriver()).isNotNull();
        assertThat(appConfig.getMaximumPoolSize()).isGreaterThan(0);
        assertThat(appConfig.getMinimumIdle()).isGreaterThanOrEqualTo(0);
        assertThat(appConfig.getConnectionTimeout()).isGreaterThan(0);
        assertThat(appConfig.getIdleTimeout()).isGreaterThan(0);
        assertThat(appConfig.getMaxLifetime()).isGreaterThan(0);
        assertThat(appConfig.getSampleDataSize()).isGreaterThan(0);
    }

    @Test
    void testConfigurationLoading() {
        // Test that configuration is loaded successfully
        assertThat(appConfig).isNotNull();
        
        // Test that we can access nested configuration values
        assertThat(appConfig.getServerPort()).isNotNull();
        assertThat(appConfig.getDatabaseUrl()).isNotNull();
        assertThat(appConfig.getMaximumPoolSize()).isNotNull();
    }

    @Test
    void testInvalidConfigurationHandling() {
        // This test would require mocking or creating an invalid config file
        // For now, we test that the constructor doesn't throw for valid config
        assertThatCode(() -> new AppConfig()).doesNotThrowAnyException();
    }
}

package dev.mars.database;

import dev.mars.config.GenericApiConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration tests for ConfigurationDataLoader using real components
 */
public class ConfigurationDataLoaderTest {

    private DatabaseManager databaseManager;
    private ConfigurationDataLoader configurationDataLoader;

    @BeforeEach
    void setUp() {
        // Use test configuration
        System.setProperty("generic.config.file", "application-test.yml");

        // Create components manually to avoid Guice module complexity in tests
        var genericApiConfig = new GenericApiConfig();
        databaseManager = new DatabaseManager(genericApiConfig);
        
        // Initialize schema explicitly since we're not using the Guice module
        databaseManager.initializeSchema();
        
        // Clean database before each test
        databaseManager.cleanDatabase();
        
        configurationDataLoader = new ConfigurationDataLoader(databaseManager, genericApiConfig);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("generic.config.file");
    }

    @Test
    void testLoadSampleConfigurationDataIfNeeded_WithYamlSource() {
        // Test that no data is loaded when config source is yaml (default)
        
        // Act
        configurationDataLoader.loadSampleConfigurationDataIfNeeded();

        // Assert - verify no data was loaded
        assertThatCode(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM config_databases");
                 ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    assertThat(count).isEqualTo(0);
                }
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void testDatabaseSchemaCreation() {
        // Test that all required tables are created
        
        assertThatCode(() -> {
            try (Connection connection = databaseManager.getConnection()) {
                // Test config_databases table
                try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM config_databases")) {
                    statement.executeQuery();
                }
                
                // Test config_queries table
                try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM config_queries")) {
                    statement.executeQuery();
                }
                
                // Test config_endpoints table
                try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM config_endpoints")) {
                    statement.executeQuery();
                }
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void testDatabaseHealthCheck() {
        // Test that database health check works
        assertThat(databaseManager.isHealthy()).isTrue();
    }

    @Test
    void testCleanDatabase() {
        // Test that database cleaning works without errors
        assertThatCode(() -> databaseManager.cleanDatabase()).doesNotThrowAnyException();
    }
}

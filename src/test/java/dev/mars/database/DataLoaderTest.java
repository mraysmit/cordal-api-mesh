package dev.mars.database;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.mars.config.AppConfig;
import dev.mars.config.GuiceModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for DataLoader using real components
 */
public class DataLoaderTest {

    private Injector injector;
    private DataLoader dataLoader;
    private DatabaseManager databaseManager;
    private AppConfig appConfig;

    @BeforeEach
    void setUp() {
        // Use test configuration
        System.setProperty("config.file", "application-test.yml");

        // Create components manually to avoid Guice module complexity in tests
        appConfig = new AppConfig();
        var databaseConfig = new dev.mars.config.DatabaseConfig(appConfig);
        databaseManager = new DatabaseManager(databaseConfig);
        // Initialize schema explicitly since we're not using the Guice module
        databaseManager.initializeSchema();
        // Clean database before each test
        databaseManager.cleanDatabase();
        dataLoader = new DataLoader(databaseManager, appConfig);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("config.file");
    }

    @Test
    void testLoadSampleDataIfNeededDisabled() {
        // The test configuration has loadSampleData set to false
        // So this should not load any data

        // Act
        dataLoader.loadSampleDataIfNeeded();

        // Assert - verify no data was loaded by checking count
        assertThatCode(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM stock_trades");
                 ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    // Should be 0 since sample data loading is disabled in test config
                    assertThat(count).isEqualTo(0);
                }
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void testDataLoaderConfiguration() {
        // Test that the configuration is correctly loaded
        assertThat(appConfig.shouldLoadSampleData()).isFalse(); // Disabled in test config
        assertThat(appConfig.getSampleDataSize()).isEqualTo(10); // Test config value
    }

    @Test
    void testDataLoaderExists() {
        // Test that DataLoader can be instantiated and basic functionality works
        assertThat(dataLoader).isNotNull();

        // Test that it can access the database manager
        assertThatCode(() -> {
            try (Connection connection = databaseManager.getConnection()) {
                assertThat(connection).isNotNull();
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void testDatabaseConnection() throws SQLException {
        // Test that the data loader can connect to the database
        try (Connection connection = databaseManager.getConnection()) {
            assertThat(connection).isNotNull();
            assertThat(connection.isClosed()).isFalse();

            // Test that we can execute a simple query
            try (PreparedStatement statement = connection.prepareStatement("SELECT 1");
                 ResultSet resultSet = statement.executeQuery()) {

                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt(1)).isEqualTo(1);
            }
        }
    }
}

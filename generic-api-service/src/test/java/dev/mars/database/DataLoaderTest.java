package dev.mars.database;

import dev.mars.test.TestDatabaseManager;
import dev.mars.test.TestDataLoader;
import dev.mars.config.GenericApiConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for TestDataLoader using real components
 */
public class DataLoaderTest {

    private TestDataLoader dataLoader;
    private TestDatabaseManager databaseManager;
    private GenericApiConfig genericApiConfig;

    @BeforeEach
    void setUp() {
        // Use test configuration
        System.setProperty("generic.config.file", "application-test.yml");

        // Create components manually to avoid Guice module complexity in tests
        genericApiConfig = new GenericApiConfig();
        databaseManager = new TestDatabaseManager(genericApiConfig);
        // Initialize schema explicitly since we're not using the Guice module
        databaseManager.initializeSchema();
        // Clean database before each test
        databaseManager.cleanDatabase();
        dataLoader = new TestDataLoader(databaseManager, genericApiConfig);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("generic.config.file");
    }

    @Test
    void testLoadSampleDataIfNeeded() {
        // The DataLoader now loads a fixed number of sample records

        // Act
        dataLoader.loadSampleDataIfNeeded();

        // Assert - verify data was loaded by checking count
        assertThatCode(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM stock_trades");
                 ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    // DataLoader now loads a fixed number of sample records (100)
                    assertThat(count).isEqualTo(100);
                }
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void testDataLoaderConfiguration() {
        // Test that the configuration is correctly loaded
        // DataLoader now uses fixed sample size, so just test that it exists
        assertThat(genericApiConfig).isNotNull();
        assertThat(genericApiConfig.getDatabaseUrl()).isNotNull();
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

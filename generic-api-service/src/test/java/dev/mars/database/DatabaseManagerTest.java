package dev.mars.database;

import dev.mars.test.TestDatabaseManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for TestDatabaseManager using real components
 */
public class DatabaseManagerTest {

    private TestDatabaseManager databaseManager;

    @BeforeEach
    void setUp() {
        // Use test configuration
        System.setProperty("generic.config.file", "application-test.yml");

        // Create components manually to avoid Guice module complexity in tests
        var genericApiConfig = new dev.mars.config.GenericApiConfig();
        databaseManager = new TestDatabaseManager(genericApiConfig);
        // Initialize schema explicitly since we're not using the Guice module
        databaseManager.initializeSchema();
        // Clean database before each test
        databaseManager.cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("generic.config.file");
    }

    @Test
    void testInitializeSchema() {
        // The schema is already initialized in the constructor
        // Just verify that we can get a connection and the schema exists
        assertThatCode(() -> {
            try (Connection connection = databaseManager.getConnection()) {
                assertThat(connection).isNotNull();
                assertThat(connection.isClosed()).isFalse();
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void testGetConnection() throws SQLException {
        // Act
        Connection result = databaseManager.getConnection();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isClosed()).isFalse();

        // Clean up
        result.close();
    }

    @Test
    void testIsHealthySuccess() {
        // Act
        boolean result = databaseManager.isHealthy();

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void testMultipleConnections() throws SQLException {
        // Test that we can get multiple connections
        Connection conn1 = databaseManager.getConnection();
        Connection conn2 = databaseManager.getConnection();

        assertThat(conn1).isNotNull();
        assertThat(conn2).isNotNull();
        assertThat(conn1).isNotSameAs(conn2); // Should be different connection instances

        // Clean up
        conn1.close();
        conn2.close();
    }

    @Test
    void testSchemaExists() throws SQLException {
        // Verify that the stock_trades table exists
        try (Connection connection = databaseManager.getConnection()) {
            var metaData = connection.getMetaData();
            var tables = metaData.getTables(null, null, "STOCK_TRADES", null);

            assertThat(tables.next()).isTrue(); // Table should exist
        }
    }
}

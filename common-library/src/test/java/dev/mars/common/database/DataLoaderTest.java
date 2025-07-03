package dev.mars.common.database;

import dev.mars.common.config.DatabaseConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for DataLoader in common-library
 */
public class DataLoaderTest {

    private DataLoader dataLoader;
    private TestDatabaseManager databaseManager;
    private TestDataLoaderConfig config;

    @BeforeEach
    void setUp() {
        // Create test configuration
        config = new TestDataLoaderConfig();
        
        // Create test database manager
        DatabaseConfig dbConfig = createTestDatabaseConfig();
        databaseManager = new TestDatabaseManager(dbConfig);
        
        // Initialize schema and clean database
        databaseManager.initializeSchema();
        databaseManager.cleanDatabase();
        
        // Create DataLoader
        dataLoader = new DataLoader(databaseManager, config);
    }

    @AfterEach
    void tearDown() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    @Test
    void testLoadSampleDataIfNeeded() {
        // Act
        dataLoader.loadSampleDataIfNeeded();

        // Assert - verify data was loaded by checking count
        assertThatCode(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM stock_trades");
                 ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    assertThat(count).isEqualTo(config.getSampleDataSize());
                }
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void testDataLoaderConfiguration() {
        // Test that the configuration is correctly used
        assertThat(config).isNotNull();
        assertThat(config.getDatabaseUrl()).isNotNull();
        assertThat(config.isSampleDataLoadingEnabled()).isTrue();
        assertThat(config.getSampleDataSize()).isEqualTo(50); // Test config value
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

    @Test
    void testSampleDataLoadingDisabled() {
        // Create config with sample data loading disabled
        TestDataLoaderConfig disabledConfig = new TestDataLoaderConfig();
        disabledConfig.setSampleDataLoadingEnabled(false);
        
        DataLoader disabledDataLoader = new DataLoader(databaseManager, disabledConfig);
        
        // Act
        disabledDataLoader.loadSampleDataIfNeeded();
        
        // Assert - verify no data was loaded
        assertThatCode(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM stock_trades");
                 ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    assertThat(count).isEqualTo(0);
                }
            }
        }).doesNotThrowAnyException();
    }

    private DatabaseConfig createTestDatabaseConfig() {
        DatabaseConfig config = new DatabaseConfig();
        config.setName("test-database");
        config.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        config.setUsername("sa");
        config.setPassword("");
        config.setDriver("org.h2.Driver");
        return config;
    }

    /**
     * Test implementation of DataLoaderConfig
     */
    private static class TestDataLoaderConfig implements DataLoaderConfig {
        private boolean sampleDataLoadingEnabled = true;
        private int sampleDataSize = 50;

        @Override
        public String getDatabaseUrl() {
            return "jdbc:h2:mem:testdb";
        }

        @Override
        public boolean isSampleDataLoadingEnabled() {
            return sampleDataLoadingEnabled;
        }

        @Override
        public int getSampleDataSize() {
            return sampleDataSize;
        }

        public void setSampleDataLoadingEnabled(boolean enabled) {
            this.sampleDataLoadingEnabled = enabled;
        }

        public void setSampleDataSize(int size) {
            this.sampleDataSize = size;
        }
    }

    /**
     * Test implementation of BaseDatabaseManager for testing
     */
    private static class TestDatabaseManager extends BaseDatabaseManager {
        
        public TestDatabaseManager(DatabaseConfig databaseConfig) {
            super(databaseConfig);
        }

        @Override
        public void initializeSchema() {
            try {
                String createStockTradesTableSql = """
                    CREATE TABLE IF NOT EXISTS stock_trades (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        symbol VARCHAR(10) NOT NULL,
                        trade_type VARCHAR(4) NOT NULL CHECK (trade_type IN ('BUY', 'SELL')),
                        quantity INTEGER NOT NULL CHECK (quantity > 0),
                        price DECIMAL(10,2) NOT NULL CHECK (price > 0),
                        total_value DECIMAL(15,2) NOT NULL,
                        trade_date_time TIMESTAMP NOT NULL,
                        trader_id VARCHAR(50) NOT NULL,
                        exchange VARCHAR(20) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                    )
                    """;
                
                executeSql(createStockTradesTableSql);
                
            } catch (SQLException e) {
                throw new RuntimeException("Failed to initialize test database schema", e);
            }
        }

        public void cleanDatabase() {
            try {
                executeSql("DELETE FROM stock_trades");
                executeSql("ALTER TABLE stock_trades ALTER COLUMN id RESTART WITH 1");
            } catch (SQLException e) {
                throw new RuntimeException("Failed to clean test database", e);
            }
        }
    }
}

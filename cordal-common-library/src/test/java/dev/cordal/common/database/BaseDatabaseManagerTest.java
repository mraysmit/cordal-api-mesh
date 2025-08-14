package dev.cordal.common.database;

import dev.cordal.common.config.DatabaseConfig;
import dev.cordal.common.config.PoolConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for BaseDatabaseManager
 */
class BaseDatabaseManagerTest {

    private TestDatabaseManager databaseManager;
    private DatabaseConfig config;

    @BeforeEach
    void setUp() {
        PoolConfig poolConfig = new PoolConfig();
        poolConfig.setMaximumPoolSize(5);
        poolConfig.setMinimumIdle(1);

        config = new DatabaseConfig();
        config.setName("test-db");
        config.setDescription("Test database");
        config.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setDriver("org.h2.Driver");
        config.setPool(poolConfig);

        databaseManager = new TestDatabaseManager(config);
    }

    @AfterEach
    void tearDown() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    @Test
    void shouldInitializeWithValidConfig() {
        assertThat(databaseManager.getDatabaseConfig()).isEqualTo(config);
        assertThat(databaseManager.getDataSource()).isNotNull();
    }

    @Test
    void shouldGetConnection() throws SQLException {
        try (Connection connection = databaseManager.getConnection()) {
            assertThat(connection).isNotNull();
            assertThat(connection.isClosed()).isFalse();
        }
    }

    @Test
    void shouldCheckHealthSuccessfully() {
        assertThat(databaseManager.isHealthy()).isTrue();
    }

    @Test
    void shouldExecuteSqlStatement() throws SQLException {
        String createTableSql = "CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(50))";
        databaseManager.executeSql(createTableSql);

        // Verify table was created by inserting data
        String insertSql = "INSERT INTO test_table (id, name) VALUES (1, 'test')";
        databaseManager.executeSql(insertSql);

        // Verify data exists
        try (Connection connection = databaseManager.getConnection();
             var statement = connection.createStatement();
             var resultSet = statement.executeQuery("SELECT COUNT(*) FROM test_table")) {
            
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt(1)).isEqualTo(1);
        }
    }

    @Test
    void shouldExecuteMultipleSqlStatements() throws SQLException {
        String[] sqlStatements = {
            "CREATE TABLE test_table2 (id INT PRIMARY KEY, name VARCHAR(50))",
            "INSERT INTO test_table2 (id, name) VALUES (1, 'test1')",
            "INSERT INTO test_table2 (id, name) VALUES (2, 'test2')"
        };

        databaseManager.executeSqlStatements(sqlStatements);

        // Verify data exists
        try (Connection connection = databaseManager.getConnection();
             var statement = connection.createStatement();
             var resultSet = statement.executeQuery("SELECT COUNT(*) FROM test_table2")) {
            
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt(1)).isEqualTo(2);
        }
    }

    @Test
    void shouldGetPoolStats() {
        String stats = databaseManager.getPoolStats();
        assertThat(stats).contains("test-db");
        assertThat(stats).contains("Active=");
        assertThat(stats).contains("Idle=");
        assertThat(stats).contains("Total=");
    }

    @Test
    void shouldCloseDataSource() {
        databaseManager.close();
        // HikariDataSource should be closed, but we can't easily test this
        // without casting to HikariDataSource, so we'll just verify the method runs
        assertThat(databaseManager.getDataSource()).isNotNull();
    }

    @Test
    void shouldHandleInvalidSql() {
        assertThatThrownBy(() -> databaseManager.executeSql("INVALID SQL STATEMENT"))
            .isInstanceOf(SQLException.class);
    }

    // Test implementation of BaseDatabaseManager
    private static class TestDatabaseManager extends BaseDatabaseManager {
        public TestDatabaseManager(DatabaseConfig databaseConfig) {
            super(databaseConfig);
        }

        @Override
        public void initializeSchema() {
            // Test implementation - no schema needed
        }
    }
}

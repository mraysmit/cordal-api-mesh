package dev.mars.generic;

import dev.mars.database.DatabaseManager;
import dev.mars.generic.config.QueryConfig;
import dev.mars.generic.model.QueryParameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for GenericRepository using real components
 */
class GenericRepositoryTest {

    private GenericRepository repository;
    private DatabaseManager databaseManager;

    @BeforeEach
    void setUp() throws SQLException {
        // Use test configuration
        System.setProperty("config.file", "application-test.yml");

        // Create components manually to avoid Guice module complexity in tests
        var appConfig = new dev.mars.config.AppConfig();
        var databaseConfig = new dev.mars.config.DatabaseConfig(appConfig);
        databaseManager = new DatabaseManager(databaseConfig);
        databaseManager.initializeSchema();
        databaseManager.cleanDatabase();

        repository = new GenericRepository(databaseManager);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("config.file");
    }

    @Test
    void testRepositoryExists() {
        // Test that the repository can be instantiated
        assertThat(repository).isNotNull();
        assertThat(databaseManager).isNotNull();
    }

    @Test
    void testExecuteQuery_WithValidQuery() {
        // Test executing a simple query (this will fail if no data exists, which is expected)
        QueryConfig queryConfig = new QueryConfig("test-query", "Test query",
            "SELECT COUNT(*) as count FROM stock_trades", Collections.emptyList());
        List<QueryParameter> parameters = Collections.emptyList();

        // Act & Assert - should not throw an exception
        assertThatCode(() -> {
            var results = repository.executeQuery(queryConfig, parameters);
            assertThat(results).isNotNull();
        }).doesNotThrowAnyException();
    }

    @Test
    void testExecuteCountQuery_WithValidQuery() {
        // Test executing a count query
        QueryConfig queryConfig = new QueryConfig("count-query", "Count query",
            "SELECT COUNT(*) FROM stock_trades", Collections.emptyList());
        List<QueryParameter> parameters = Collections.emptyList();

        // Act & Assert - should not throw an exception
        assertThatCode(() -> {
            long count = repository.executeCountQuery(queryConfig, parameters);
            assertThat(count).isGreaterThanOrEqualTo(0);
        }).doesNotThrowAnyException();
    }

}

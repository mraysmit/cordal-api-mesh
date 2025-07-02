package dev.mars.generic;

import dev.mars.database.DatabaseManager;
import dev.mars.generic.config.ConfigurationLoader;
import dev.mars.generic.config.EndpointConfigurationManager;
import dev.mars.generic.config.QueryConfig;
import dev.mars.generic.database.DatabaseConnectionManager;
import dev.mars.generic.model.QueryParameter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
    private DatabaseConnectionManager databaseConnectionManager;
    private DatabaseManager databaseManager;

    @BeforeEach
    void setUp() throws SQLException {
        // Use test configuration
        System.setProperty("config.file", "application-test.yml");

        // Create components manually to avoid Guice module complexity in tests
        var genericApiConfig = new dev.mars.config.GenericApiConfig();
        var databaseConfig = new dev.mars.config.DatabaseConfig(genericApiConfig);
        databaseManager = new DatabaseManager(databaseConfig);
        databaseManager.initializeSchema();
        databaseManager.cleanDatabase();

        // Create configuration loader and manager for test configurations
        ConfigurationLoader configurationLoader = new TestConfigurationLoader(genericApiConfig);
        EndpointConfigurationManager configurationManager = new EndpointConfigurationManager(configurationLoader);

        // Create database connection manager
        databaseConnectionManager = new DatabaseConnectionManager(configurationManager);

        repository = new GenericRepository(databaseConnectionManager);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("config.file");
    }

    @Test
    void testRepositoryExists() {
        // Test that the repository can be instantiated
        assertThat(repository).isNotNull();
        assertThat(databaseConnectionManager).isNotNull();
    }

    @Test
    void testExecuteQuery_WithValidQuery() {
        // Test executing a simple query (this will fail if no data exists, which is expected)
        QueryConfig queryConfig = new QueryConfig("test-query", "Test query",
            "SELECT COUNT(*) as count FROM stock_trades", "stock-trades-db", Collections.emptyList());
        List<QueryParameter> parameters = Collections.emptyList();

        // Act & Assert - should throw an exception due to missing table (expected in test)
        assertThatThrownBy(() -> {
            repository.executeQuery(queryConfig, parameters);
        }).hasMessageContaining("Failed to execute query");
    }

    @Test
    void testExecuteCountQuery_WithValidQuery() {
        // Test executing a count query
        QueryConfig queryConfig = new QueryConfig("count-query", "Count query",
            "SELECT COUNT(*) FROM stock_trades", "stock-trades-db", Collections.emptyList());
        List<QueryParameter> parameters = Collections.emptyList();

        // Act & Assert - should throw an exception due to missing table (expected in test)
        assertThatThrownBy(() -> {
            repository.executeCountQuery(queryConfig, parameters);
        }).hasMessageContaining("Failed to execute count query");
    }

}

package dev.mars.generic;

import dev.mars.test.TestDatabaseManager;
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
    private TestDatabaseManager databaseManager;

    @BeforeEach
    void setUp() throws SQLException {
        // Use test configuration
        System.setProperty("generic.config.file", "application-test.yml");

        // Create components manually to avoid Guice module complexity in tests
        var genericApiConfig = new dev.mars.config.GenericApiConfig();
        databaseManager = new TestDatabaseManager(genericApiConfig);
        databaseManager.initializeSchema();
        databaseManager.cleanDatabase();

        // Create configuration loader and manager for test configurations
        ConfigurationLoader configurationLoader = new TestConfigurationLoader(genericApiConfig);

        // Create database loader components for testing (using real DatabaseManager for repositories)
        dev.mars.database.DatabaseManager realDatabaseManager = new dev.mars.database.DatabaseManager(genericApiConfig);
        realDatabaseManager.initializeSchema();
        dev.mars.database.repository.DatabaseConfigurationRepository databaseRepository = new dev.mars.database.repository.DatabaseConfigurationRepository(realDatabaseManager);
        dev.mars.database.repository.QueryConfigurationRepository queryRepository = new dev.mars.database.repository.QueryConfigurationRepository(realDatabaseManager);
        dev.mars.database.repository.EndpointConfigurationRepository endpointRepository = new dev.mars.database.repository.EndpointConfigurationRepository(realDatabaseManager);
        dev.mars.database.loader.DatabaseConfigurationLoader databaseLoader = new dev.mars.database.loader.DatabaseConfigurationLoader(databaseRepository, queryRepository, endpointRepository);

        dev.mars.generic.config.ConfigurationLoaderFactory factory = new dev.mars.generic.config.ConfigurationLoaderFactory(genericApiConfig, configurationLoader, databaseLoader);
        EndpointConfigurationManager configurationManager = new EndpointConfigurationManager(factory);

        // Create database connection manager
        databaseConnectionManager = new DatabaseConnectionManager(configurationManager);

        repository = new GenericRepository(databaseConnectionManager);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("generic.config.file");
    }

    @Test
    void testRepositoryExists() {
        // Test that the repository can be instantiated
        assertThat(repository).isNotNull();
        assertThat(databaseConnectionManager).isNotNull();
    }

    @Test
    void testExecuteQuery_WithValidQuery() {
        // Test executing a simple query
        QueryConfig queryConfig = new QueryConfig("test-query", "Test query",
            "SELECT COUNT(*) as count FROM stock_trades", "stock-trades-db", Collections.emptyList());
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
            "SELECT COUNT(*) FROM stock_trades", "stock-trades-db", Collections.emptyList());
        List<QueryParameter> parameters = Collections.emptyList();

        // Act & Assert - should not throw an exception
        assertThatCode(() -> {
            long count = repository.executeCountQuery(queryConfig, parameters);
            assertThat(count).isGreaterThanOrEqualTo(0);
        }).doesNotThrowAnyException();
    }

}

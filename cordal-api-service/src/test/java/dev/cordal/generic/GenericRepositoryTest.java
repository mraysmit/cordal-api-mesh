package dev.cordal.generic;

import dev.cordal.test.TestDatabaseManager;
import dev.cordal.generic.config.ConfigurationLoader;
import dev.cordal.generic.config.EndpointConfigurationManager;
import dev.cordal.generic.config.QueryConfig;
import dev.cordal.generic.database.DatabaseConnectionManager;
import dev.cordal.generic.model.QueryParameter;
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
        var genericApiConfig = new dev.cordal.config.GenericApiConfig();
        databaseManager = new TestDatabaseManager(genericApiConfig);
        databaseManager.initializeSchema();
        databaseManager.cleanDatabase();

        // Create configuration loader and manager for test configurations
        ConfigurationLoader configurationLoader = new TestConfigurationLoader(genericApiConfig);

        // Create database loader components for testing (using real DatabaseManager for repositories)
        dev.cordal.database.DatabaseManager realDatabaseManager = new dev.cordal.database.DatabaseManager(genericApiConfig);
        realDatabaseManager.initializeSchema();
        dev.cordal.database.repository.DatabaseConfigurationRepository databaseRepository = new dev.cordal.database.repository.DatabaseConfigurationRepository(realDatabaseManager);
        dev.cordal.database.repository.QueryConfigurationRepository queryRepository = new dev.cordal.database.repository.QueryConfigurationRepository(realDatabaseManager);
        dev.cordal.database.repository.EndpointConfigurationRepository endpointRepository = new dev.cordal.database.repository.EndpointConfigurationRepository(realDatabaseManager);
        dev.cordal.database.loader.DatabaseConfigurationLoader databaseLoader = new dev.cordal.database.loader.DatabaseConfigurationLoader(databaseRepository, queryRepository, endpointRepository);

        dev.cordal.generic.config.ConfigurationLoaderFactory factory = new dev.cordal.generic.config.ConfigurationLoaderFactory(genericApiConfig, configurationLoader, databaseLoader);
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

package dev.cordal.util;

import dev.cordal.generic.config.ApiEndpointConfig;
import dev.cordal.generic.config.DatabaseConfig;
import dev.cordal.generic.config.EndpointConfigurationManager;
import dev.cordal.generic.config.QueryConfig;
import dev.cordal.generic.database.DatabaseConnectionManager;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigurationValidatorTest {

    @Test
    void validateConfigurationChainShouldReportMissingDependencies() {
        EndpointConfigurationManager configurationManager = mock(EndpointConfigurationManager.class);
        DatabaseConnectionManager databaseConnectionManager = mock(DatabaseConnectionManager.class);
        ConfigurationValidator validator = new ConfigurationValidator(configurationManager, databaseConnectionManager);

        DatabaseConfig databaseConfig = new DatabaseConfig();
        databaseConfig.setName("primary");

        QueryConfig validQuery = new QueryConfig();
        validQuery.setName("orders-query");
        validQuery.setDatabase("primary");

        QueryConfig missingDatabaseQuery = new QueryConfig();
        missingDatabaseQuery.setName("missing-db-query");
        missingDatabaseQuery.setDatabase("archive");

        QueryConfig noDatabaseQuery = new QueryConfig();
        noDatabaseQuery.setName("no-db-query");
        noDatabaseQuery.setDatabase(" ");

        ApiEndpointConfig validEndpoint = new ApiEndpointConfig();
        validEndpoint.setQuery("orders-query");

        ApiEndpointConfig missingQueryEndpoint = new ApiEndpointConfig();
        missingQueryEndpoint.setQuery("unknown-query");

        ApiEndpointConfig noQueryEndpoint = new ApiEndpointConfig();

        ApiEndpointConfig paginatedEndpoint = new ApiEndpointConfig();
        paginatedEndpoint.setQuery("orders-query");
        paginatedEndpoint.setCountQuery("missing-count-query");
        ApiEndpointConfig.PaginationConfig pagination = new ApiEndpointConfig.PaginationConfig();
        pagination.setEnabled(true);
        paginatedEndpoint.setPagination(pagination);

        when(configurationManager.getAllDatabaseConfigurations()).thenReturn(Map.of("primary", databaseConfig));
        when(configurationManager.getAllQueryConfigurations()).thenReturn(Map.of(
            "orders-query", validQuery,
            "missing-db-query", missingDatabaseQuery,
            "no-db-query", noDatabaseQuery
        ));
        when(configurationManager.getAllEndpointConfigurations()).thenReturn(Map.of(
            "orders", validEndpoint,
            "missing-query", missingQueryEndpoint,
            "no-query", noQueryEndpoint,
            "paged-orders", paginatedEndpoint
        ));

        ValidationResult result = validator.validateConfigurationChain();

        assertThat(result.getSuccesses()).contains(
            "Endpoint 'orders' -> query 'orders-query' [OK]",
            "Endpoint 'paged-orders' -> query 'orders-query' [OK]",
            "Query 'orders-query' -> database 'primary' [OK]"
        );
        assertThat(result.getErrors()).contains(
            "Endpoint 'missing-query' references non-existent query: unknown-query",
            "Endpoint 'no-query' has no query defined",
            "Endpoint 'paged-orders' references non-existent count query: missing-count-query",
            "Query 'missing-db-query' references non-existent database: archive",
            "Query 'no-db-query' has no database defined"
        );
    }

    @Test
    void validateDatabaseSchemaShouldWarnWhenDatabaseIsUnavailable() {
        EndpointConfigurationManager configurationManager = mock(EndpointConfigurationManager.class);
        DatabaseConnectionManager databaseConnectionManager = mock(DatabaseConnectionManager.class);
        ConfigurationValidator validator = new ConfigurationValidator(configurationManager, databaseConnectionManager);

        DatabaseConfig databaseConfig = new DatabaseConfig();
        databaseConfig.setName("primary");
        databaseConfig.setUrl("jdbc:h2:mem:test;SCHEMA=PUBLIC");

        QueryConfig query = new QueryConfig();
        query.setName("orders-query");
        query.setDatabase("primary");

        when(configurationManager.getAllQueryConfigurations()).thenReturn(Map.of("orders-query", query));
        when(configurationManager.getAllDatabaseConfigurations()).thenReturn(Map.of("primary", databaseConfig));
        when(databaseConnectionManager.isDatabaseAvailable("primary")).thenReturn(false);
        when(databaseConnectionManager.getDatabaseFailureReason("primary")).thenReturn("connection refused");

        ValidationResult result = validator.validateDatabaseSchema();

        assertThat(result.getWarnings()).containsExactly(
            "Database 'primary' is unavailable - connection refused (endpoints using this database will return service unavailable errors)"
        );
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void validateDatabaseSchemaShouldReportDatasourceAndConnectionFailures() throws SQLException {
        EndpointConfigurationManager configurationManager = mock(EndpointConfigurationManager.class);
        DatabaseConnectionManager databaseConnectionManager = mock(DatabaseConnectionManager.class);
        ConfigurationValidator validator = new ConfigurationValidator(configurationManager, databaseConnectionManager);

        DatabaseConfig primaryDatabase = new DatabaseConfig();
        primaryDatabase.setName("primary");
        primaryDatabase.setUrl("jdbc:h2:mem:primary;SCHEMA=PUBLIC");

        DatabaseConfig reportingDatabase = new DatabaseConfig();
        reportingDatabase.setName("reporting");
        reportingDatabase.setUrl("jdbc:h2:mem:reporting;SCHEMA=PUBLIC");

        QueryConfig primaryQuery = new QueryConfig();
        primaryQuery.setName("orders-query");
        primaryQuery.setDatabase("primary");

        QueryConfig reportingQuery = new QueryConfig();
        reportingQuery.setName("reports-query");
        reportingQuery.setDatabase("reporting");

        DataSource brokenDataSource = mock(DataSource.class);
        when(brokenDataSource.getConnection()).thenThrow(new SQLException("socket closed"));

        when(configurationManager.getAllQueryConfigurations()).thenReturn(Map.of(
            "orders-query", primaryQuery,
            "reports-query", reportingQuery
        ));
        when(configurationManager.getAllDatabaseConfigurations()).thenReturn(Map.of(
            "primary", primaryDatabase,
            "reporting", reportingDatabase
        ));
        when(databaseConnectionManager.isDatabaseAvailable("primary")).thenReturn(true);
        when(databaseConnectionManager.isDatabaseAvailable("reporting")).thenReturn(true);
        when(databaseConnectionManager.getDataSource("primary")).thenReturn(null);
        when(databaseConnectionManager.getDataSource("reporting")).thenReturn(brokenDataSource);

        ValidationResult result = validator.validateDatabaseSchema();

        assertThat(result.getErrors()).contains(
            "Database 'primary' data source not available",
            "Failed to connect to database 'reporting': socket closed"
        );
        assertThat(result.getSuccesses()).isEmpty();
    }
}
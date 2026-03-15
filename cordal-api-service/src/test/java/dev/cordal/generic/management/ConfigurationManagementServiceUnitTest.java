package dev.cordal.generic.management;

import dev.cordal.database.repository.DatabaseConfigurationRepository;
import dev.cordal.database.repository.EndpointConfigurationRepository;
import dev.cordal.database.repository.QueryConfigurationRepository;
import dev.cordal.dto.ConfigurationSourceInfoResponse;
import dev.cordal.dto.ConfigurationStatisticsResponse;
import dev.cordal.generic.config.ApiEndpointConfig;
import dev.cordal.generic.config.ConfigurationLoaderFactory;
import dev.cordal.generic.config.DatabaseConfig;
import dev.cordal.generic.config.EndpointConfigurationManager;
import dev.cordal.generic.config.QueryConfig;
import dev.cordal.generic.dto.ConfigurationCollectionResponse;
import dev.cordal.generic.dto.ConfigurationOperationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConfigurationManagementServiceUnitTest {

    private DatabaseConfigurationRepository databaseRepository;
    private QueryConfigurationRepository queryRepository;
    private EndpointConfigurationRepository endpointRepository;
    private ConfigurationLoaderFactory configurationLoaderFactory;
    private EndpointConfigurationManager configurationManager;
    private ConfigurationManagementService service;

    @BeforeEach
    void setUp() {
        databaseRepository = mock(DatabaseConfigurationRepository.class);
        queryRepository = mock(QueryConfigurationRepository.class);
        endpointRepository = mock(EndpointConfigurationRepository.class);
        configurationLoaderFactory = mock(ConfigurationLoaderFactory.class);
        configurationManager = mock(EndpointConfigurationManager.class);
        service = new ConfigurationManagementService(
            databaseRepository,
            queryRepository,
            endpointRepository,
            configurationLoaderFactory,
            configurationManager
        );
    }

    @Test
    void shouldReturnCollectionsLookupsAndSourceInfo() {
        DatabaseConfig database = new DatabaseConfig();
        QueryConfig query = new QueryConfig();
        query.setName("orders-query");
        ApiEndpointConfig endpoint = new ApiEndpointConfig();
        endpoint.setPath("/api/orders");

        when(configurationLoaderFactory.getConfigurationSource()).thenReturn("yaml");
        when(configurationLoaderFactory.isDatabaseSource()).thenReturn(false);
        when(configurationManager.getAllDatabaseConfigurations()).thenReturn(Map.of("orders-db", database));
        when(configurationManager.getAllQueryConfigurations()).thenReturn(Map.of("orders-query", query));
        when(configurationManager.getAllEndpointConfigurations()).thenReturn(Map.of("orders-endpoint", endpoint));

        ConfigurationCollectionResponse<DatabaseConfig> databases = service.getAllDatabaseConfigurations();
        ConfigurationCollectionResponse<QueryConfig> queries = service.getAllQueryConfigurations();
        ConfigurationCollectionResponse<ApiEndpointConfig> endpoints = service.getAllEndpointConfigurations();
        ConfigurationSourceInfoResponse sourceInfo = service.getConfigurationSourceInfo();

        assertThat(databases.getSource()).isEqualTo("yaml");
        assertThat(databases.getCount()).isEqualTo(1);
        assertThat(databases.getConfigurations()).containsEntry("orders-db", database);
        assertThat(service.getDatabaseConfiguration("orders-db")).contains(database);
        assertThat(service.getDatabaseConfiguration("missing-db")).isEmpty();

        assertThat(queries.getCount()).isEqualTo(1);
        assertThat(queries.getConfigurations()).containsEntry("orders-query", query);
        assertThat(service.getQueryConfiguration("orders-query")).contains(query);
        assertThat(service.getQueryConfiguration("missing-query")).isEmpty();

        assertThat(endpoints.getCount()).isEqualTo(1);
        assertThat(endpoints.getConfigurations()).containsEntry("orders-endpoint", endpoint);
        assertThat(service.getEndpointConfiguration("orders-endpoint")).contains(endpoint);
        assertThat(service.getEndpointConfiguration("missing-endpoint")).isEmpty();

        assertThat(service.isConfigurationManagementAvailable()).isFalse();
        assertThat(sourceInfo.currentSource()).isEqualTo("yaml");
        assertThat(sourceInfo.managementAvailable()).isFalse();
        assertThat(sourceInfo.supportedSources()).containsExactly("yaml", "database");
    }

    @Test
    void shouldReturnFilteredCollectionsAndStatistics() {
        QueryConfig ordersQuery = new QueryConfig();
        ordersQuery.setName("orders-query");
        QueryConfig reportingQuery = new QueryConfig();
        reportingQuery.setName("reporting-query");

        ApiEndpointConfig ordersEndpoint = new ApiEndpointConfig();
        ordersEndpoint.setPath("/api/orders");
        ApiEndpointConfig unnamedEndpoint = new ApiEndpointConfig();

        when(configurationLoaderFactory.getConfigurationSource()).thenReturn("database");
        when(queryRepository.loadByDatabase("orders-db")).thenReturn(List.of(ordersQuery, reportingQuery));
        when(endpointRepository.loadByQuery("orders-query")).thenReturn(List.of(ordersEndpoint, unnamedEndpoint));
        when(databaseRepository.getCount()).thenReturn(2);
        when(queryRepository.getCount()).thenReturn(3);
        when(endpointRepository.getCount()).thenReturn(4);

        ConfigurationCollectionResponse<QueryConfig> queriesByDatabase = service.getQueryConfigurationsByDatabase("orders-db");
        ConfigurationCollectionResponse<ApiEndpointConfig> endpointsByQuery = service.getEndpointConfigurationsByQuery("orders-query");
        ConfigurationStatisticsResponse statistics = service.getConfigurationStatistics();

        assertThat(queriesByDatabase.getDatabase()).isEqualTo("orders-db");
        assertThat(queriesByDatabase.getConfigurations()).containsKeys("orders-query", "reporting-query");

        assertThat(endpointsByQuery.getQuery()).isEqualTo("orders-query");
        assertThat(endpointsByQuery.getConfigurations()).containsKeys("/api/orders", "unknown");

        assertThat(statistics.source()).isEqualTo("database");
        assertThat(statistics.statistics().databases().total()).isEqualTo(2);
        assertThat(statistics.statistics().queries().total()).isEqualTo(3);
        assertThat(statistics.statistics().endpoints().total()).isEqualTo(4);
        assertThat(statistics.summary().totalConfigurations()).isEqualTo(9);
    }

    @Test
    void shouldCreateUpdateAndDeleteConfigurationsWhenUsingDatabaseSource() {
        DatabaseConfig database = new DatabaseConfig();
        QueryConfig query = new QueryConfig();
        ApiEndpointConfig endpoint = new ApiEndpointConfig();

        when(configurationLoaderFactory.isDatabaseSource()).thenReturn(true);
        when(configurationLoaderFactory.getConfigurationSource()).thenReturn("database");
        when(databaseRepository.exists("orders-db")).thenReturn(false);
        when(queryRepository.exists("orders-query")).thenReturn(true);
        when(endpointRepository.exists("orders-endpoint")).thenReturn(false);
        when(databaseRepository.delete("orders-db")).thenReturn(true);
        when(queryRepository.delete("orders-query")).thenReturn(false);
        when(endpointRepository.delete("orders-endpoint")).thenReturn(true);

        ConfigurationOperationResponse createdDatabase = service.saveDatabaseConfiguration("orders-db", database);
        ConfigurationOperationResponse updatedQuery = service.saveQueryConfiguration("orders-query", query);
        ConfigurationOperationResponse createdEndpoint = service.saveEndpointConfiguration("orders-endpoint", endpoint);
        ConfigurationOperationResponse deletedDatabase = service.deleteDatabaseConfiguration("orders-db");
        ConfigurationOperationResponse deletedQuery = service.deleteQueryConfiguration("orders-query");
        ConfigurationOperationResponse deletedEndpoint = service.deleteEndpointConfiguration("orders-endpoint");

        assertThat(createdDatabase.isCreated()).isTrue();
        assertThat(updatedQuery.isUpdated()).isTrue();
        assertThat(createdEndpoint.isCreated()).isTrue();
        assertThat(deletedDatabase.getFound()).isTrue();
        assertThat(deletedQuery.getFound()).isFalse();
        assertThat(deletedEndpoint.getFound()).isTrue();

        verify(databaseRepository).save("orders-db", database);
        verify(queryRepository).save("orders-query", query);
        verify(endpointRepository).save("orders-endpoint", endpoint);
    }

    @Test
    void shouldRejectMutationsWhenSourceIsNotDatabase() {
        DatabaseConfig database = new DatabaseConfig();
        QueryConfig query = new QueryConfig();
        ApiEndpointConfig endpoint = new ApiEndpointConfig();

        when(configurationLoaderFactory.isDatabaseSource()).thenReturn(false);
        when(configurationLoaderFactory.getConfigurationSource()).thenReturn("yaml");

        assertThatThrownBy(() -> service.saveDatabaseConfiguration("orders-db", database))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Current source: yaml");
        assertThatThrownBy(() -> service.deleteDatabaseConfiguration("orders-db"))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.saveQueryConfiguration("orders-query", query))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.deleteQueryConfiguration("orders-query"))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.saveEndpointConfiguration("orders-endpoint", endpoint))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.deleteEndpointConfiguration("orders-endpoint"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldWrapRepositoryFailures() {
        DatabaseConfig database = new DatabaseConfig();
        ApiEndpointConfig endpoint = new ApiEndpointConfig();

        when(configurationLoaderFactory.isDatabaseSource()).thenReturn(true);
        when(configurationLoaderFactory.getConfigurationSource()).thenReturn("database");
        when(databaseRepository.exists("orders-db")).thenReturn(true);
        doThrow(new RuntimeException("db-save-failed")).when(databaseRepository).save("orders-db", database);
        when(queryRepository.delete("orders-query")).thenThrow(new RuntimeException("query-delete-failed"));
        when(endpointRepository.exists("orders-endpoint")).thenReturn(true);
        doThrow(new RuntimeException("endpoint-save-failed")).when(endpointRepository).save("orders-endpoint", endpoint);
        when(databaseRepository.getCount()).thenThrow(new RuntimeException("stats-failed"));

        assertThatThrownBy(() -> service.saveDatabaseConfiguration("orders-db", database))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to save database configuration: db-save-failed");
        assertThatThrownBy(() -> service.deleteQueryConfiguration("orders-query"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to delete query configuration: query-delete-failed");
        assertThatThrownBy(() -> service.saveEndpointConfiguration("orders-endpoint", endpoint))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to save endpoint configuration: endpoint-save-failed");
        assertThatThrownBy(service::getConfigurationStatistics)
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to get configuration statistics: stats-failed");
    }
}
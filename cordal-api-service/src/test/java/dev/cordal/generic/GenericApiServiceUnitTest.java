package dev.cordal.generic;

import dev.cordal.common.exception.ApiException;
import dev.cordal.generic.config.ApiEndpointConfig;
import dev.cordal.generic.config.DatabaseConfig;
import dev.cordal.generic.config.EndpointConfigurationManager;
import dev.cordal.generic.config.QueryConfig;
import dev.cordal.generic.dto.ConfigurationParametersResponse;
import dev.cordal.generic.dto.ConfigurationSchemaResponse;
import dev.cordal.generic.database.DatabaseConnectionManager;
import dev.cordal.generic.dto.ConfigurationSummaryResponse;
import dev.cordal.generic.dto.ConfigurationValidationResponse;
import dev.cordal.generic.dto.DatabaseConnectionsResponse;
import dev.cordal.generic.dto.RequestParameters;
import dev.cordal.generic.model.GenericResponse;
import dev.cordal.generic.model.QueryParameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GenericApiServiceUnitTest {

    private GenericRepository genericRepository;
    private EndpointConfigurationManager configurationManager;
    private DatabaseConnectionManager databaseConnectionManager;
    private GenericApiService service;

    @BeforeEach
    void setUp() {
        genericRepository = mock(GenericRepository.class);
        configurationManager = mock(EndpointConfigurationManager.class);
        databaseConnectionManager = mock(DatabaseConnectionManager.class);
        service = new GenericApiService(genericRepository, configurationManager, databaseConnectionManager);
    }

    @Test
    void executeEndpointShouldDelegateRequestParametersVariant() {
        ApiEndpointConfig endpoint = paginatedEndpoint("orders-query", null, 50);
        QueryConfig query = paginatedQuery("orders-query", "orders-db");

        when(configurationManager.getEndpointConfig("orders")).thenReturn(Optional.of(endpoint));
        when(configurationManager.getQueryConfig("orders-query")).thenReturn(Optional.of(query));
        when(databaseConnectionManager.isDatabaseAvailable("orders-db")).thenReturn(true);
        when(genericRepository.executeQuery(eq(query), any())).thenReturn(List.of(Map.of("id", 1)));

        RequestParameters parameters = new RequestParameters().add("page", 1).add("size", 10);

        GenericResponse response = service.executeEndpoint("orders", parameters);

        assertThat(response.getType()).isEqualTo("PAGED");
        verify(genericRepository).executeQuery(eq(query), argThat(queryParameters ->
            queryParameters.size() == 2
                && "limit".equals(queryParameters.get(0).getName())
                && Integer.valueOf(10).equals(queryParameters.get(0).getValue())
                && "offset".equals(queryParameters.get(1).getName())
                && Integer.valueOf(10).equals(queryParameters.get(1).getValue())
        ));
    }

    @Test
    void executeEndpointShouldRejectUnavailableDatabase() {
        ApiEndpointConfig endpoint = singleEndpoint("orders-query");
        QueryConfig query = query("orders-query", "orders-db", List.of());

        when(configurationManager.getEndpointConfig("orders")).thenReturn(Optional.of(endpoint));
        when(configurationManager.getQueryConfig("orders-query")).thenReturn(Optional.of(query));
        when(databaseConnectionManager.isDatabaseAvailable("orders-db")).thenReturn(false);
        when(databaseConnectionManager.getDatabaseFailureReason("orders-db")).thenReturn("timeout");

        assertThatThrownBy(() -> service.executeEndpoint("orders", Map.of()))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("temporarily unavailable")
            .extracting(throwable -> ((ApiException) throwable).getStatusCode())
            .isEqualTo(503);
    }

    @Test
    void executeEndpointShouldRejectMissingRequiredParameter() {
        ApiEndpointConfig endpoint = singleEndpoint("orders-query");
        QueryConfig query = query("orders-query", "orders-db", List.of(requiredQueryParameter("id", "LONG")));

        when(configurationManager.getEndpointConfig("orders")).thenReturn(Optional.of(endpoint));
        when(configurationManager.getQueryConfig("orders-query")).thenReturn(Optional.of(query));
        when(databaseConnectionManager.isDatabaseAvailable("orders-db")).thenReturn(true);

        assertThatThrownBy(() -> service.executeEndpoint("orders", Map.of()))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Required parameter missing: id");
    }

    @Test
    void executeEndpointShouldBuildPagedResponseAndTrimCountParameters() {
        ApiEndpointConfig endpoint = paginatedEndpoint("orders-query", "orders-count-query", 50);
        QueryConfig query = query("orders-query", "orders-db", List.of(
            requiredQueryParameter("customerId", "STRING"),
            requiredQueryParameter("limit", "INTEGER"),
            requiredQueryParameter("offset", "INTEGER")
        ));
        QueryConfig countQuery = query("orders-count-query", "orders-db", List.of(requiredQueryParameter("customerId", "STRING")));

        when(configurationManager.getEndpointConfig("orders")).thenReturn(Optional.of(endpoint));
        when(configurationManager.getQueryConfig("orders-query")).thenReturn(Optional.of(query));
        when(configurationManager.getQueryConfig("orders-count-query")).thenReturn(Optional.of(countQuery));
        when(databaseConnectionManager.isDatabaseAvailable("orders-db")).thenReturn(true);
        when(genericRepository.executeQuery(eq(query), any())).thenReturn(List.of(Map.of("id", 1), Map.of("id", 2)));
        when(genericRepository.executeCountQuery(eq(countQuery), any())).thenReturn(17L);

        GenericResponse response = service.executeEndpoint("orders", Map.of("customerId", "abc", "page", 2, "size", 5));

        assertThat(response.getType()).isEqualTo("PAGED");
        assertThat(response.getPagination().getPage()).isEqualTo(2);
        assertThat(response.getPagination().getSize()).isEqualTo(5);
        assertThat(response.getPagination().getTotalElements()).isEqualTo(17);

        verify(genericRepository).executeCountQuery(eq(countQuery), argThat((List<QueryParameter> params) ->
            params.size() == 1
                && "customerId".equals(params.get(0).getName())
                && params.get(0).getPosition() == 1
        ));
    }

    @Test
    void executeEndpointShouldReturnSingleOrListBasedOnResultSize() {
        ApiEndpointConfig endpoint = singleEndpoint("orders-query");
        QueryConfig query = query("orders-query", "orders-db", List.of());

        when(configurationManager.getEndpointConfig("orders")).thenReturn(Optional.of(endpoint));
        when(configurationManager.getQueryConfig("orders-query")).thenReturn(Optional.of(query));
        when(databaseConnectionManager.isDatabaseAvailable("orders-db")).thenReturn(true);
        when(genericRepository.executeQuery(eq(query), any()))
            .thenReturn(List.of(Map.of("id", 1)))
            .thenReturn(List.of(Map.of("id", 1), Map.of("id", 2)));

        GenericResponse single = service.executeEndpoint("orders", Map.of());
        GenericResponse list = service.executeEndpoint("orders", Map.of());

        assertThat(single.getType()).isEqualTo("SINGLE");
        assertThat(list.getType()).isEqualTo("LIST");
    }

    @Test
    void executeEndpointShouldFailWhenNoDataFound() {
        ApiEndpointConfig endpoint = singleEndpoint("orders-query");
        QueryConfig query = query("orders-query", "orders-db", List.of());

        when(configurationManager.getEndpointConfig("orders")).thenReturn(Optional.of(endpoint));
        when(configurationManager.getQueryConfig("orders-query")).thenReturn(Optional.of(query));
        when(databaseConnectionManager.isDatabaseAvailable("orders-db")).thenReturn(true);
        when(genericRepository.executeQuery(eq(query), any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.executeEndpoint("orders", Map.of()))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("No data found");
    }

    @Test
    void executeEndpointShouldValidatePaginationInputs() {
        ApiEndpointConfig endpoint = paginatedEndpoint("orders-query", null, 50);
        QueryConfig query = paginatedQuery("orders-query", "orders-db");

        when(configurationManager.getEndpointConfig("orders")).thenReturn(Optional.of(endpoint));
        when(configurationManager.getQueryConfig("orders-query")).thenReturn(Optional.of(query));
        when(databaseConnectionManager.isDatabaseAvailable("orders-db")).thenReturn(true);

        assertThatThrownBy(() -> service.executeEndpoint("orders", Map.of("page", "abc")))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Invalid integer value for parameter: page");

        assertThatThrownBy(() -> service.executeEndpoint("orders", Map.of("page", -1, "size", 5)))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Page number cannot be negative");

        assertThatThrownBy(() -> service.executeEndpoint("orders", Map.of("page", 0, "size", 99)))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Page size cannot exceed 50");

        assertThatThrownBy(() -> service.executeEndpoint("orders", Map.of("page", true)))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Invalid parameter type for: page");

        assertThatThrownBy(() -> service.executeEndpoint("orders", Map.of("page", 0, "size", 0)))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Page size must be positive");
    }

    @Test
    void executeEndpointShouldFailWhenCountQueryConfigurationIsMissing() {
        ApiEndpointConfig endpoint = paginatedEndpoint("orders-query", "orders-count-query", 50);
        QueryConfig query = query("orders-query", "orders-db", List.of(
            requiredQueryParameter("customerId", "STRING"),
            requiredQueryParameter("limit", "INTEGER"),
            requiredQueryParameter("offset", "INTEGER")
        ));

        when(configurationManager.getEndpointConfig("orders")).thenReturn(Optional.of(endpoint));
        when(configurationManager.getQueryConfig("orders-query")).thenReturn(Optional.of(query));
        when(configurationManager.getQueryConfig("orders-count-query")).thenReturn(Optional.empty());
        when(databaseConnectionManager.isDatabaseAvailable("orders-db")).thenReturn(true);
        when(genericRepository.executeQuery(eq(query), any())).thenReturn(List.of(Map.of("id", 1)));

        assertThatThrownBy(() -> service.executeEndpoint("orders", Map.of("customerId", "abc", "page", 0, "size", 5)))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Count query not found: orders-count-query");
    }

    @Test
    void executeEndpointAsyncShouldPropagateFailures() {
        ApiEndpointConfig endpoint = singleEndpoint("orders-query");
        QueryConfig query = query("orders-query", "orders-db", List.of());

        when(configurationManager.getEndpointConfig("orders")).thenReturn(Optional.of(endpoint));
        when(configurationManager.getQueryConfig("orders-query")).thenReturn(Optional.of(query));
        when(databaseConnectionManager.isDatabaseAvailable("orders-db")).thenReturn(false);
        when(databaseConnectionManager.getDatabaseFailureReason("orders-db")).thenReturn("unreachable");

        assertThatThrownBy(() -> service.executeEndpointAsync("orders", Map.of()).join())
            .isInstanceOf(CompletionException.class)
            .hasCauseInstanceOf(ApiException.class);
    }

    @Test
    void availabilityMethodsShouldFilterEndpointsByDatabaseState() {
        ApiEndpointConfig availableEndpoint = singleEndpoint("available-query");
        ApiEndpointConfig unavailableEndpoint = singleEndpoint("broken-query");
        ApiEndpointConfig missingQueryEndpoint = singleEndpoint("missing-query");

        QueryConfig availableQuery = query("available-query", "live-db", List.of());
        QueryConfig unavailableQuery = query("broken-query", "down-db", List.of());

        when(configurationManager.getAllEndpointConfigurations()).thenReturn(Map.of(
            "available", availableEndpoint,
            "unavailable", unavailableEndpoint,
            "missing", missingQueryEndpoint
        ));
        when(configurationManager.getEndpointConfig("available")).thenReturn(Optional.of(availableEndpoint));
        when(configurationManager.getEndpointConfig("unavailable")).thenReturn(Optional.of(unavailableEndpoint));
        when(configurationManager.getEndpointConfig("missing")).thenReturn(Optional.of(missingQueryEndpoint));
        when(configurationManager.getQueryConfig("available-query")).thenReturn(Optional.of(availableQuery));
        when(configurationManager.getQueryConfig("broken-query")).thenReturn(Optional.of(unavailableQuery));
        when(configurationManager.getQueryConfig("missing-query")).thenReturn(Optional.empty());
        when(databaseConnectionManager.isDatabaseAvailable("live-db")).thenReturn(true);
        when(databaseConnectionManager.isDatabaseAvailable("down-db")).thenReturn(false);
        when(databaseConnectionManager.getDatabaseFailureReason("down-db")).thenReturn("network error");

        assertThat(service.getAvailableEndpoints()).containsOnlyKeys("available");
        assertThat(service.getUnavailableEndpoints())
            .containsKey("unavailable")
            .doesNotContainKey("available");
        assertThat(service.isEndpointAvailable("available")).isTrue();
        assertThat(service.isEndpointAvailable("unavailable")).isFalse();
        assertThat(service.isEndpointAvailable("missing")).isFalse();
    }

    @Test
    void summaryAndConnectionMethodsShouldAggregateReferences() {
        ApiEndpointConfig pagedEndpoint = paginatedEndpoint("orders-query", "orders-count-query", 25);
        pagedEndpoint.setMethod("GET");
        pagedEndpoint.setParameters(List.of(endpointParameter("customerId", "PATH", true)));

        ApiEndpointConfig postEndpoint = singleEndpoint("create-order-query");
        postEndpoint.setMethod("POST");

        QueryConfig ordersQuery = query("orders-query", "orders-db", List.of(requiredQueryParameter("customerId", "STRING")));
        QueryConfig countQuery = query("orders-count-query", "orders-db", List.of());
        QueryConfig createQuery = query("create-order-query", "write-db", List.of(requiredQueryParameter("amount", "INTEGER")));

        when(configurationManager.getAllEndpointConfigurations()).thenReturn(Map.of("orders", pagedEndpoint, "create-order", postEndpoint));
        when(configurationManager.getAllQueryConfigurations()).thenReturn(Map.of(
            "orders-query", ordersQuery,
            "orders-count-query", countQuery,
            "create-order-query", createQuery
        ));
        when(configurationManager.getQueryConfig("orders-query")).thenReturn(Optional.of(ordersQuery));
        when(configurationManager.getQueryConfig("orders-count-query")).thenReturn(Optional.of(countQuery));
        when(configurationManager.getQueryConfig("create-order-query")).thenReturn(Optional.of(createQuery));

        DatabaseConnectionsResponse connections = service.getEndpointDatabaseConnections();
        ConfigurationSummaryResponse summary = service.getEndpointConfigurationSummary();

        assertThat(connections.getConnections()).containsEntry("orders", "orders-db").containsEntry("create-order", "write-db");
        assertThat(connections.getReferencedDatabases()).containsExactlyInAnyOrder("orders-db", "write-db");
        assertThat(summary.getByMethod()).containsEntry("GET", 1).containsEntry("POST", 1);
        assertThat(summary.getWithPagination()).isEqualTo(1);
        assertThat(summary.getReferencedQueries()).containsExactlyInAnyOrder("orders-query", "orders-count-query", "create-order-query");
        assertThat(summary.getReferencedDatabases()).containsExactlyInAnyOrder("orders-db", "write-db");
    }

    @Test
    void schemaAndParameterApisShouldExposeTypedAndLegacyViews() {
        ApiEndpointConfig endpointWithParameters = singleEndpoint("orders-query");
        endpointWithParameters.setParameters(List.of(endpointParameter("customerId", "PATH", true)));

        ApiEndpointConfig endpointWithoutParameters = singleEndpoint("reports-query");
        QueryConfig queryWithParameters = query("orders-query", "orders-db", List.of(requiredQueryParameter("customerId", "STRING")));
        QueryConfig queryWithoutParameters = query("reports-query", "reports-db", List.of());

        when(configurationManager.getAllEndpointConfigurations()).thenReturn(Map.of(
            "orders", endpointWithParameters,
            "reports", endpointWithoutParameters
        ));
        when(configurationManager.getAllQueryConfigurations()).thenReturn(Map.of(
            "orders-query", queryWithParameters,
            "reports-query", queryWithoutParameters
        ));

        ConfigurationSchemaResponse endpointSchema = service.getEndpointConfigurationSchema();
        ConfigurationSchemaResponse querySchema = service.getQueryConfigurationSchema();
        ConfigurationSchemaResponse databaseSchema = service.getDatabaseConfigurationSchema();
        ConfigurationParametersResponse<ApiEndpointConfig.EndpointParameter> endpointParameters = service.getEndpointParameters();
        ConfigurationParametersResponse<QueryConfig.QueryParameter> queryParameters = service.getQueryParameters();

        assertThat(endpointSchema.getConfigType()).isEqualTo("endpoints");
        assertThat(endpointSchema.hasField("path")).isTrue();
        assertThat(endpointSchema.getField("path").isRequired()).isTrue();
        assertThat(querySchema.hasField("sql")).isTrue();
        assertThat(databaseSchema.hasField("url")).isTrue();
        assertThat(databaseSchema.getRequiredFields()).extracting(ConfigurationSchemaResponse.SchemaField::getName)
            .contains("name", "url", "username", "password", "driver");

        assertThat(endpointParameters.getConfigType()).isEqualTo("endpoints");
        assertThat(endpointParameters.getTotalConfigurations()).isEqualTo(2);
        assertThat(endpointParameters.getConfigurationsWithParameters()).isEqualTo(1);
        assertThat(endpointParameters.getParametersFor("orders")).hasSize(1);
        assertThat(queryParameters.getConfigType()).isEqualTo("queries");
        assertThat(queryParameters.getTotalParameterCount()).isEqualTo(1);
        assertThat(queryParameters.hasParameters("orders-query")).isTrue();
        assertThat(queryParameters.hasParameters("reports-query")).isFalse();

        assertThat(service.getEndpointConfigurationSchemaMap()).containsEntry("configType", "endpoints");
        assertThat(service.getQueryConfigurationSchemaMap()).containsEntry("configType", "queries");
        assertThat(service.getDatabaseConfigurationSchemaMap()).containsEntry("configType", "databases");
        assertThat(service.getEndpointParametersMap()).containsEntry("configType", "endpoints");
        assertThat(service.getQueryParametersMap()).containsEntry("configType", "queries");
    }

    @Test
    void queryAndDatabaseApisShouldAggregateConnectionsSummariesAndDetails() {
        QueryConfig ordersQuery = query("orders-query", "orders-db", List.of(requiredQueryParameter("customerId", "STRING")));
        QueryConfig reportsQuery = query("reports-query", "analytics-db", List.of());

        DatabaseConfig ordersDatabase = new DatabaseConfig();
        ordersDatabase.setUrl("jdbc:h2:mem:orders");
        ordersDatabase.setUsername("sa");
        ordersDatabase.setDriver("org.h2.Driver");
        ordersDatabase.setDescription("Orders database");
        DatabaseConfig.PoolConfig poolConfig = new DatabaseConfig.PoolConfig();
        poolConfig.setMaximumPoolSize(15);
        ordersDatabase.setPool(poolConfig);

        DatabaseConfig analyticsDatabase = new DatabaseConfig();
        analyticsDatabase.setUrl("jdbc:postgresql://localhost/analytics");
        analyticsDatabase.setUsername("analytics");
        analyticsDatabase.setDriver("org.postgresql.Driver");
        analyticsDatabase.setDescription("Analytics database");

        when(configurationManager.getAllQueryConfigurations()).thenReturn(Map.of(
            "orders-query", ordersQuery,
            "reports-query", reportsQuery
        ));
        when(configurationManager.getAllDatabaseConfigurations()).thenReturn(Map.of(
            "orders-db", ordersDatabase,
            "analytics-db", analyticsDatabase
        ));
        when(configurationManager.getQueryConfig("orders-query")).thenReturn(Optional.of(ordersQuery));
        when(configurationManager.getDatabaseConfig("orders-db")).thenReturn(Optional.of(ordersDatabase));

        DatabaseConnectionsResponse connections = service.getQueryDatabaseConnections();
        ConfigurationSummaryResponse querySummary = service.getQueryConfigurationSummary();
        Map<String, Object> databaseParameters = service.getDatabaseParameters();
        Map<String, Object> databaseConnections = service.getDatabaseConnections();
        Map<String, Object> databaseSummary = service.getDatabaseConfigurationSummary();

        assertThat(service.getAllQueryConfigurations()).containsOnlyKeys("orders-query", "reports-query");
        assertThat(service.getQueryConfiguration("orders-query")).contains(ordersQuery);
        assertThat(service.getAllDatabaseConfigurations()).containsOnlyKeys("orders-db", "analytics-db");
        assertThat(service.getDatabaseConfiguration("orders-db")).contains(ordersDatabase);

        assertThat(connections.getConfigType()).isEqualTo("queries");
        assertThat(connections.getConnections()).containsEntry("orders-query", "orders-db");
        assertThat(connections.getReferencedDatabases()).containsExactlyInAnyOrder("orders-db", "analytics-db");
        assertThat(connections.getConfigurationsWithDatabases()).isEqualTo(2);
        assertThat(service.getQueryDatabaseConnectionsMap()).containsEntry("configType", "queries");

        assertThat(querySummary.getConfigType()).isEqualTo("queries");
        assertThat(querySummary.getWithParameters()).isEqualTo(1);
        assertThat(querySummary.getParameterCounts()).containsEntry("orders-query", 1);
        assertThat(querySummary.getReferencedDatabases()).containsExactlyInAnyOrder("orders-db", "analytics-db");
        assertThat(service.getQueryConfigurationSummaryMap()).containsEntry("configType", "queries");

        @SuppressWarnings("unchecked")
        Map<String, DatabaseConfig.PoolConfig> poolConfigurations =
            (Map<String, DatabaseConfig.PoolConfig>) databaseParameters.get("poolConfigurations");
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> configuredConnections =
            (Map<String, Map<String, Object>>) databaseConnections.get("connections");
        @SuppressWarnings("unchecked")
        Map<String, String> driverTypes = (Map<String, String>) databaseSummary.get("driverTypes");
        @SuppressWarnings("unchecked")
        List<String> uniqueDrivers = (List<String>) databaseSummary.get("uniqueDrivers");

        assertThat(databaseParameters).containsEntry("configType", "databases");
        assertThat(databaseParameters).containsEntry("totalDatabases", 2);
        assertThat(databaseParameters).containsEntry("databasesWithPoolConfig", 1);
        assertThat(poolConfigurations).containsKey("orders-db");

        assertThat(databaseConnections).containsEntry("configType", "databases");
        assertThat(configuredConnections).containsKey("orders-db");
        assertThat(databaseSummary).containsEntry("configType", "databases");
        assertThat(databaseSummary).containsEntry("totalCount", 2);
        assertThat(databaseSummary).containsEntry("withPoolConfig", 1);
        assertThat(driverTypes).containsEntry("orders-db", "org.h2.Driver");
        assertThat(uniqueDrivers).contains("org.h2.Driver", "org.postgresql.Driver");
    }

    @Test
    void relationshipApisShouldExposeLinksAndValidationErrors() {
        ApiEndpointConfig endpoint = paginatedEndpoint("orders-query", "missing-count-query", 25);
        endpoint.setDescription("Orders endpoint");

        QueryConfig query = query("orders-query", "orders-db", List.of(requiredQueryParameter("customerId", "STRING")));
        DatabaseConfig database = new DatabaseConfig();
        database.setUrl("jdbc:h2:mem:orders");
        database.setDriver("org.h2.Driver");

        when(configurationManager.getAllEndpointConfigurations()).thenReturn(Map.of("orders", endpoint));
        when(configurationManager.getAllQueryConfigurations()).thenReturn(Map.of("orders-query", query));
        when(configurationManager.getAllDatabaseConfigurations()).thenReturn(Map.of("orders-db", database));
        when(configurationManager.getQueryConfig("orders-query")).thenReturn(Optional.of(query));
        when(configurationManager.hasQuery("orders-query")).thenReturn(true);
        when(configurationManager.hasQuery("missing-count-query")).thenReturn(false);
        when(configurationManager.hasDatabase("orders-db")).thenReturn(true);

        Map<String, Object> relationships = service.getConfigurationRelationships();
        Map<String, Object> relationshipValidation = service.validateConfigurationRelationships();

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> endpointRelationships =
            (Map<String, Map<String, Object>>) relationships.get("endpoints");
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> queryRelationships =
            (Map<String, Map<String, Object>>) relationships.get("queries");
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) relationships.get("summary");
        @SuppressWarnings("unchecked")
        List<String> relationshipErrors = (List<String>) relationshipValidation.get("errors");

        assertThat(endpointRelationships).containsKey("orders");
        assertThat(endpointRelationships.get("orders"))
            .containsEntry("query", "orders-query")
            .containsEntry("database", "orders-db")
            .containsEntry("countQuery", "missing-count-query");
        assertThat(queryRelationships).containsKey("orders-query");
        assertThat(summary).containsEntry("totalEndpoints", 1)
            .containsEntry("totalQueries", 1)
            .containsEntry("totalDatabases", 1);

        assertThat(relationshipValidation).containsEntry("status", "INVALID");
        assertThat(relationshipValidation).containsEntry("errorCount", 1);
        assertThat(relationshipErrors)
            .contains("Endpoint 'orders' references non-existent count query: missing-count-query");
    }

    @Test
    void validateConfigurationsShouldAggregateDetailedIssues() {
        ApiEndpointConfig endpoint = new ApiEndpointConfig();
        endpoint.setPath("");
        endpoint.setMethod("");
        endpoint.setQuery("missing-query");

        QueryConfig query = new QueryConfig();
        query.setName("orders-query");
        query.setSql("");
        query.setDatabase("missing-db");

        DatabaseConfig database = new DatabaseConfig();
        database.setUrl("");
        database.setDriver("");

        when(configurationManager.getAllEndpointConfigurations()).thenReturn(Map.of("orders", endpoint));
        when(configurationManager.getAllQueryConfigurations()).thenReturn(Map.of("orders-query", query));
        when(configurationManager.getAllDatabaseConfigurations()).thenReturn(Map.of("orders-db", database));
        when(configurationManager.hasQuery("missing-query")).thenReturn(false);
        when(configurationManager.hasDatabase("missing-db")).thenReturn(false);
        doThrow(new RuntimeException("base validation failed")).when(configurationManager).validateConfigurations();

        ConfigurationValidationResponse response = service.validateConfigurations();

        assertThat(response.isInvalid()).isTrue();
        assertThat(response.getErrors()).contains(
            "base validation failed",
            "Endpoint 'orders' has no path defined",
            "Endpoint 'orders' has no HTTP method defined",
            "Query 'orders-query' has no SQL defined",
            "Database 'orders-db' has no URL defined",
            "Endpoint 'orders' references non-existent query: missing-query",
            "Query 'orders-query' references non-existent database: missing-db"
        );
        assertThat(response.getWarnings()).isEmpty();
    }

    @Test
    void validateSpecificConfigurationMethodsShouldReturnWarnings() {
        ApiEndpointConfig endpoint = new ApiEndpointConfig();
        endpoint.setPath("/orders");
        endpoint.setMethod("GET");
        endpoint.setQuery("orders-query");

        QueryConfig query = new QueryConfig();
        query.setName("orders-query");
        query.setSql("SELECT 1");
        query.setDatabase("orders-db");

        DatabaseConfig database = new DatabaseConfig();
        database.setUrl("jdbc:h2:mem:test");
        database.setDriver("org.h2.Driver");

        when(configurationManager.getAllEndpointConfigurations()).thenReturn(Map.of("orders", endpoint));
        when(configurationManager.getAllQueryConfigurations()).thenReturn(Map.of("orders-query", query));
        when(configurationManager.getAllDatabaseConfigurations()).thenReturn(Map.of("orders-db", database));

        ConfigurationValidationResponse endpointResponse = service.validateEndpointConfigurations();
        ConfigurationValidationResponse queryResponse = service.validateQueryConfigurations();
        ConfigurationValidationResponse databaseResponse = service.validateDatabaseConfigurations();

        assertThat(endpointResponse.getWarnings()).contains(
            "Endpoint 'orders' has no description",
            "Endpoint 'orders' has no description"
        );
        assertThat(queryResponse.getWarnings()).contains(
            "Query 'orders-query' has no description",
            "Query 'orders-query' has no description"
        );
        assertThat(databaseResponse.getWarnings()).contains(
            "Database 'orders-db' has no username defined",
            "Database 'orders-db' has no description"
        );
    }

    @Test
    void validateEndpointConnectivityShouldReturnErrorPayloadOnFailure() {
        when(configurationManager.getAllEndpointConfigurations()).thenThrow(new RuntimeException("boom"));

        Map<String, Object> result = service.validateEndpointConnectivity();

        assertThat(result.get("status")).isEqualTo("ERROR");
        assertThat(result.get("error").toString()).contains("boom");
    }

    private static ApiEndpointConfig singleEndpoint(String queryName) {
        ApiEndpointConfig config = new ApiEndpointConfig();
        config.setPath("/orders");
        config.setMethod("GET");
        config.setQuery(queryName);
        config.setDescription("Orders endpoint");
        return config;
    }

    private static ApiEndpointConfig paginatedEndpoint(String queryName, String countQueryName, int maxSize) {
        ApiEndpointConfig config = singleEndpoint(queryName);
        config.setCountQuery(countQueryName);
        config.setParameters(List.of(endpointParameter("page", "QUERY", false), endpointParameter("size", "QUERY", false)));

        ApiEndpointConfig.PaginationConfig paginationConfig = new ApiEndpointConfig.PaginationConfig();
        paginationConfig.setEnabled(true);
        paginationConfig.setDefaultSize(20);
        paginationConfig.setMaxSize(maxSize);
        config.setPagination(paginationConfig);
        return config;
    }

    private static ApiEndpointConfig.EndpointParameter endpointParameter(String name, String source, boolean required) {
        ApiEndpointConfig.EndpointParameter parameter = new ApiEndpointConfig.EndpointParameter();
        parameter.setName(name);
        parameter.setType("STRING");
        parameter.setSource(source);
        parameter.setRequired(required);
        return parameter;
    }

    private static QueryConfig paginatedQuery(String name, String database) {
        return query(name, database, List.of(
            requiredQueryParameter("limit", "INTEGER"),
            requiredQueryParameter("offset", "INTEGER")
        ));
    }

    private static QueryConfig query(String name, String database, List<QueryConfig.QueryParameter> parameters) {
        QueryConfig config = new QueryConfig();
        config.setName(name);
        config.setDescription("Query " + name);
        config.setSql("SELECT 1");
        config.setDatabase(database);
        config.setParameters(parameters);
        return config;
    }

    private static QueryConfig.QueryParameter requiredQueryParameter(String name, String type) {
        QueryConfig.QueryParameter parameter = new QueryConfig.QueryParameter();
        parameter.setName(name);
        parameter.setType(type);
        parameter.setRequired(true);
        return parameter;
    }
}
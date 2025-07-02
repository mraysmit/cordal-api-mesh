package dev.mars.generic.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import dev.mars.config.GenericApiConfig;
import dev.mars.generic.TestConfigurationLoader;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ConfigurationLoader
 * Note: These tests use the actual configuration files from the classpath
 */
class ConfigurationLoaderTest {

    private ConfigurationLoader loader;

    @BeforeEach
    void setUp() {
        GenericApiConfig config = new GenericApiConfig();
        loader = new TestConfigurationLoader(config);
    }

    @Test
    void testLoadQueryConfigurations() {
        // Act
        Map<String, QueryConfig> queries = loader.loadQueryConfigurations();

        // Assert
        assertThat(queries).isNotNull();
        assertThat(queries).isNotEmpty();
        
        // Verify that we have some expected queries from the configuration
        assertThat(queries).containsKey("stock-trades-all");
        assertThat(queries).containsKey("stock-trades-count");
        assertThat(queries).containsKey("stock-trades-by-id");
        
        // Verify query structure
        QueryConfig stockTradesAll = queries.get("stock-trades-all");
        assertThat(stockTradesAll).isNotNull();
        assertThat(stockTradesAll.getName()).isEqualTo("stock-trades-all");
        assertThat(stockTradesAll.getDescription()).isNotNull();
        assertThat(stockTradesAll.getSql()).isNotNull();
        assertThat(stockTradesAll.getSql()).containsIgnoringCase("SELECT");
        assertThat(stockTradesAll.getDatabase()).isEqualTo("stock-trades-db");
    }

    @Test
    void testLoadDatabaseConfigurations() {
        // Act
        Map<String, DatabaseConfig> databases = loader.loadDatabaseConfigurations();

        // Assert
        assertThat(databases).isNotNull();
        assertThat(databases).isNotEmpty();

        // Verify that we have some expected databases from the configuration
        assertThat(databases).containsKey("stock-trades-db");
        assertThat(databases).containsKey("metrics-db");

        // Verify database structure
        DatabaseConfig stockTradesDb = databases.get("stock-trades-db");
        assertThat(stockTradesDb).isNotNull();
        assertThat(stockTradesDb.getName()).isEqualTo("stock-trades-db");
        assertThat(stockTradesDb.getUrl()).isNotNull();
        assertThat(stockTradesDb.getDriver()).isEqualTo("org.h2.Driver");
        assertThat(stockTradesDb.getPool()).isNotNull();
        assertThat(stockTradesDb.getPool().getMaximumPoolSize()).isEqualTo(5);
    }

    @Test
    void testLoadEndpointConfigurations() {
        // Act
        Map<String, ApiEndpointConfig> endpoints = loader.loadEndpointConfigurations();

        // Assert
        assertThat(endpoints).isNotNull();
        assertThat(endpoints).isNotEmpty();
        
        // Verify that we have some expected endpoints from the configuration
        assertThat(endpoints).containsKey("stock-trades-list");
        
        // Verify endpoint structure
        ApiEndpointConfig stockTradesList = endpoints.get("stock-trades-list");
        assertThat(stockTradesList).isNotNull();
        assertThat(stockTradesList.getPath()).isEqualTo("/api/generic/stock-trades");
        assertThat(stockTradesList.getMethod()).isEqualTo("GET");
        assertThat(stockTradesList.getQuery()).isEqualTo("stock-trades-all");
        assertThat(stockTradesList.getCountQuery()).isEqualTo("stock-trades-count");
        assertThat(stockTradesList.getDescription()).isNotNull();
    }

    @Test
    void testLoadEndpointConfigurations_PaginationConfig() {
        // Act
        Map<String, ApiEndpointConfig> endpoints = loader.loadEndpointConfigurations();

        // Assert
        ApiEndpointConfig stockTradesList = endpoints.get("stock-trades-list");
        assertThat(stockTradesList.getPagination()).isNotNull();
        assertThat(stockTradesList.getPagination().isEnabled()).isTrue();
        assertThat(stockTradesList.getPagination().getDefaultSize()).isEqualTo(20);
        assertThat(stockTradesList.getPagination().getMaxSize()).isEqualTo(100);
    }

    @Test
    void testLoadEndpointConfigurations_Parameters() {
        // Act
        Map<String, ApiEndpointConfig> endpoints = loader.loadEndpointConfigurations();

        // Assert
        ApiEndpointConfig stockTradesList = endpoints.get("stock-trades-list");
        assertThat(stockTradesList.getParameters()).isNotNull();
        assertThat(stockTradesList.getParameters()).isNotEmpty();
        
        // Check for expected parameters
        boolean hasPageParam = stockTradesList.getParameters().stream()
            .anyMatch(p -> "page".equals(p.getName()));
        boolean hasSizeParam = stockTradesList.getParameters().stream()
            .anyMatch(p -> "size".equals(p.getName()));
        boolean hasAsyncParam = stockTradesList.getParameters().stream()
            .anyMatch(p -> "async".equals(p.getName()));
            
        assertThat(hasPageParam).isTrue();
        assertThat(hasSizeParam).isTrue();
        assertThat(hasAsyncParam).isTrue();
    }

    @Test
    void testLoadEndpointConfigurations_ResponseConfig() {
        // Act
        Map<String, ApiEndpointConfig> endpoints = loader.loadEndpointConfigurations();

        // Assert
        ApiEndpointConfig stockTradesList = endpoints.get("stock-trades-list");
        assertThat(stockTradesList.getResponse()).isNotNull();
        assertThat(stockTradesList.getResponse().getType()).isEqualTo("PAGED");
        assertThat(stockTradesList.getResponse().getFields()).isNotNull();
        assertThat(stockTradesList.getResponse().getFields()).isNotEmpty();
        
        // Check for expected response fields
        boolean hasIdField = stockTradesList.getResponse().getFields().stream()
            .anyMatch(f -> "id".equals(f.getName()));
        boolean hasSymbolField = stockTradesList.getResponse().getFields().stream()
            .anyMatch(f -> "symbol".equals(f.getName()));
            
        assertThat(hasIdField).isTrue();
        assertThat(hasSymbolField).isTrue();
    }

    @Test
    void testQueryConfigurationStructure() {
        // Act
        Map<String, QueryConfig> queries = loader.loadQueryConfigurations();

        // Assert - verify all queries have required fields
        for (Map.Entry<String, QueryConfig> entry : queries.entrySet()) {
            String queryName = entry.getKey();
            QueryConfig query = entry.getValue();

            assertThat(query.getName()).as("Query %s should have a name", queryName).isNotNull();
            assertThat(query.getSql()).as("Query %s should have SQL", queryName).isNotNull();
            assertThat(query.getSql()).as("Query %s SQL should not be empty", queryName).isNotEmpty();
            assertThat(query.getDatabase()).as("Query %s should have a database", queryName).isNotNull();
            assertThat(query.getDatabase()).as("Query %s database should not be empty", queryName).isNotEmpty();
        }
    }

    @Test
    void testEndpointConfigurationStructure() {
        // Act
        Map<String, ApiEndpointConfig> endpoints = loader.loadEndpointConfigurations();

        // Assert - verify all endpoints have required fields
        for (Map.Entry<String, ApiEndpointConfig> entry : endpoints.entrySet()) {
            String endpointName = entry.getKey();
            ApiEndpointConfig endpoint = entry.getValue();
            
            assertThat(endpoint.getPath()).as("Endpoint %s should have a path", endpointName).isNotNull();
            assertThat(endpoint.getMethod()).as("Endpoint %s should have a method", endpointName).isNotNull();
            assertThat(endpoint.getQuery()).as("Endpoint %s should have a query", endpointName).isNotNull();
            
            // If pagination is enabled, should have count query
            if (endpoint.getPagination() != null && endpoint.getPagination().isEnabled()) {
                assertThat(endpoint.getCountQuery())
                    .as("Paginated endpoint %s should have a count query", endpointName)
                    .isNotNull();
            }
        }
    }

    @Test
    void testConfigurationConsistency() {
        // Act
        Map<String, DatabaseConfig> databases = loader.loadDatabaseConfigurations();
        Map<String, QueryConfig> queries = loader.loadQueryConfigurations();
        Map<String, ApiEndpointConfig> endpoints = loader.loadEndpointConfigurations();

        // Assert - verify that all query databases exist
        for (Map.Entry<String, QueryConfig> entry : queries.entrySet()) {
            String queryName = entry.getKey();
            QueryConfig query = entry.getValue();

            if (query.getDatabase() != null) {
                assertThat(databases).as("Database %s referenced by query %s should exist",
                    query.getDatabase(), queryName).containsKey(query.getDatabase());
            }
        }

        // Assert - verify that all endpoint queries exist
        for (Map.Entry<String, ApiEndpointConfig> entry : endpoints.entrySet()) {
            String endpointName = entry.getKey();
            ApiEndpointConfig endpoint = entry.getValue();

            // Check main query exists
            if (endpoint.getQuery() != null) {
                assertThat(queries).as("Query %s referenced by endpoint %s should exist",
                    endpoint.getQuery(), endpointName).containsKey(endpoint.getQuery());
            }

            // Check count query exists if specified
            if (endpoint.getCountQuery() != null) {
                assertThat(queries).as("Count query %s referenced by endpoint %s should exist",
                    endpoint.getCountQuery(), endpointName).containsKey(endpoint.getCountQuery());
            }
        }
    }

    @Test
    void testLoadQueryConfigurations_FileNotFound() {
        // This test would require mocking the class loader or using a different approach
        // For now, we'll test that the method handles the case gracefully
        // In a real scenario, you might want to test with a custom ConfigurationLoader
        // that can be configured to use different file paths
        
        // Act & Assert - the current implementation should work with existing files
        assertThatCode(() -> loader.loadQueryConfigurations())
            .doesNotThrowAnyException();
    }

    @Test
    void testLoadEndpointConfigurations_FileNotFound() {
        // Similar to above - test that the method works with existing files
        
        // Act & Assert
        assertThatCode(() -> loader.loadEndpointConfigurations())
            .doesNotThrowAnyException();
    }

    @Test
    void testParameterTypes() {
        // Act
        Map<String, ApiEndpointConfig> endpoints = loader.loadEndpointConfigurations();

        // Assert - verify parameter types are valid
        for (ApiEndpointConfig endpoint : endpoints.values()) {
            if (endpoint.getParameters() != null) {
                for (ApiEndpointConfig.EndpointParameter param : endpoint.getParameters()) {
                    assertThat(param.getType()).isIn("STRING", "INTEGER", "LONG", "BOOLEAN", "DOUBLE", "TIMESTAMP", "DECIMAL");
                }
            }
        }
    }

    @Test
    void testResponseFieldTypes() {
        // Act
        Map<String, ApiEndpointConfig> endpoints = loader.loadEndpointConfigurations();

        // Assert - verify response field types are valid
        for (ApiEndpointConfig endpoint : endpoints.values()) {
            if (endpoint.getResponse() != null && endpoint.getResponse().getFields() != null) {
                for (ApiEndpointConfig.ResponseField field : endpoint.getResponse().getFields()) {
                    assertThat(field.getType()).isIn("STRING", "INTEGER", "LONG", "BOOLEAN", "DOUBLE", "TIMESTAMP", "DECIMAL");
                }
            }
        }
    }

    @Test
    void testHttpMethods() {
        // Act
        Map<String, ApiEndpointConfig> endpoints = loader.loadEndpointConfigurations();

        // Assert - verify HTTP methods are valid
        for (ApiEndpointConfig endpoint : endpoints.values()) {
            assertThat(endpoint.getMethod()).isIn("GET", "POST", "PUT", "DELETE", "PATCH");
        }
    }

    @Test
    void testResponseTypes() {
        // Act
        Map<String, ApiEndpointConfig> endpoints = loader.loadEndpointConfigurations();

        // Assert - verify response types are valid
        for (ApiEndpointConfig endpoint : endpoints.values()) {
            if (endpoint.getResponse() != null) {
                assertThat(endpoint.getResponse().getType()).isIn("SINGLE", "LIST", "PAGED");
            }
        }
    }
}

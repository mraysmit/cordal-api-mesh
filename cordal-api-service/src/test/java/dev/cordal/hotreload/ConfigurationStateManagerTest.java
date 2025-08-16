package dev.cordal.hotreload;

import dev.cordal.generic.config.ApiEndpointConfig;
import dev.cordal.generic.config.QueryConfig;
import dev.cordal.generic.config.DatabaseConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for ConfigurationStateManager Phase 1 implementation
 */
class ConfigurationStateManagerTest {

    private ConfigurationStateManager stateManager;

    @BeforeEach
    void setUp() {
        stateManager = new ConfigurationStateManager();
    }

    @Test
    void shouldCreateAndRetrieveSnapshots() {
        // Create test configurations
        Map<String, DatabaseConfig> databases = createTestDatabases();
        Map<String, QueryConfig> queries = createTestQueries();
        Map<String, ApiEndpointConfig> endpoints = createTestEndpoints();

        // Create snapshot
        String version = stateManager.createSnapshot(databases, queries, endpoints);
        assertThat(version).isNotNull();

        // Retrieve snapshot
        Optional<ConfigurationSnapshot> snapshot = stateManager.getSnapshot(version);
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().getVersion()).isEqualTo(version);
        assertThat(snapshot.get().getDatabases()).hasSize(1);
        assertThat(snapshot.get().getQueries()).hasSize(1);
        assertThat(snapshot.get().getEndpoints()).hasSize(1);
    }

    @Test
    void shouldTrackCurrentSnapshot() {
        // Initially no current snapshot
        Optional<ConfigurationSnapshot> current = stateManager.getCurrentSnapshot();
        assertThat(current).isEmpty();

        // Create snapshot
        Map<String, DatabaseConfig> databases = createTestDatabases();
        Map<String, QueryConfig> queries = createTestQueries();
        Map<String, ApiEndpointConfig> endpoints = createTestEndpoints();

        String version = stateManager.createSnapshot(databases, queries, endpoints);

        // Should now have current snapshot
        current = stateManager.getCurrentSnapshot();
        assertThat(current).isPresent();
        assertThat(current.get().getVersion()).isEqualTo(version);
    }

    @Test
    void shouldRestoreSnapshots() {
        // Create first snapshot
        Map<String, DatabaseConfig> databases1 = createTestDatabases();
        Map<String, QueryConfig> queries1 = createTestQueries();
        Map<String, ApiEndpointConfig> endpoints1 = createTestEndpoints();
        String version1 = stateManager.createSnapshot(databases1, queries1, endpoints1);

        // Create second snapshot with different data
        Map<String, DatabaseConfig> databases2 = new HashMap<>();
        databases2.put("db2", createDatabaseConfig("db2", "jdbc:h2:mem:db2"));
        String version2 = stateManager.createSnapshot(databases2, new HashMap<>(), new HashMap<>());

        // Current should be version2
        Optional<ConfigurationSnapshot> current = stateManager.getCurrentSnapshot();
        assertThat(current.get().getVersion()).isEqualTo(version2);

        // Restore version1
        Optional<ConfigurationSnapshot> restored = stateManager.restoreSnapshot(version1);
        assertThat(restored).isPresent();
        assertThat(restored.get().getVersion()).isEqualTo(version1);

        // Current should now be version1
        current = stateManager.getCurrentSnapshot();
        assertThat(current.get().getVersion()).isEqualTo(version1);
    }

    @Test
    void shouldCalculateDeltaForNewConfiguration() {
        // Create initial snapshot
        Map<String, DatabaseConfig> databases = createTestDatabases();
        Map<String, QueryConfig> queries = createTestQueries();
        Map<String, ApiEndpointConfig> endpoints = createTestEndpoints();
        stateManager.createSnapshot(databases, queries, endpoints);

        // Create new configuration with additions
        Map<String, DatabaseConfig> newDatabases = new HashMap<>(databases);
        newDatabases.put("db2", createDatabaseConfig("db2", "jdbc:h2:mem:db2"));

        Map<String, QueryConfig> newQueries = new HashMap<>(queries);
        newQueries.put("query2", createQueryConfig("query2", "db2", "SELECT * FROM table2"));

        Map<String, ApiEndpointConfig> newEndpoints = new HashMap<>(endpoints);
        newEndpoints.put("endpoint2", createEndpointConfig("endpoint2", "/api/test2", "query2"));

        // Calculate delta
        ConfigurationSnapshot currentSnapshot = stateManager.getCurrentSnapshot().get();
        ConfigurationDelta delta = stateManager.calculateDelta(currentSnapshot, newDatabases, newQueries, newEndpoints);

        // Verify delta
        assertThat(delta.hasChanges()).isTrue();
        assertThat(delta.addedDatabases).hasSize(1);
        assertThat(delta.addedQueries).hasSize(1);
        assertThat(delta.addedEndpoints).hasSize(1);
        assertThat(delta.modifiedDatabases).isEmpty();
        assertThat(delta.removedDatabases).isEmpty();
    }

    @Test
    void shouldCalculateDeltaForRemovedConfiguration() {
        // Create initial snapshot with multiple items
        Map<String, DatabaseConfig> databases = createTestDatabases();
        databases.put("db2", createDatabaseConfig("db2", "jdbc:h2:mem:db2"));
        
        Map<String, QueryConfig> queries = createTestQueries();
        queries.put("query2", createQueryConfig("query2", "db2", "SELECT * FROM table2"));
        
        Map<String, ApiEndpointConfig> endpoints = createTestEndpoints();
        endpoints.put("endpoint2", createEndpointConfig("endpoint2", "/api/test2", "query2"));
        
        stateManager.createSnapshot(databases, queries, endpoints);

        // Create new configuration with removals
        Map<String, DatabaseConfig> newDatabases = new HashMap<>();
        newDatabases.put("testdb", databases.get("testdb")); // Keep only first database

        Map<String, QueryConfig> newQueries = new HashMap<>();
        newQueries.put("testquery", queries.get("testquery")); // Keep only first query

        Map<String, ApiEndpointConfig> newEndpoints = new HashMap<>();
        newEndpoints.put("testendpoint", endpoints.get("testendpoint")); // Keep only first endpoint

        // Calculate delta
        ConfigurationSnapshot currentSnapshot = stateManager.getCurrentSnapshot().get();
        ConfigurationDelta delta = stateManager.calculateDelta(currentSnapshot, newDatabases, newQueries, newEndpoints);

        // Verify delta
        assertThat(delta.hasChanges()).isTrue();
        assertThat(delta.removedDatabases).containsExactly("db2");
        assertThat(delta.removedQueries).containsExactly("query2");
        assertThat(delta.removedEndpoints).containsExactly("endpoint2");
        assertThat(delta.addedDatabases).isEmpty();
        assertThat(delta.modifiedDatabases).isEmpty();
    }

    @Test
    void shouldValidateDependencies() {
        // Create configurations with valid dependencies
        Map<String, DatabaseConfig> databases = createTestDatabases();
        Map<String, QueryConfig> queries = createTestQueries();
        Map<String, ApiEndpointConfig> endpoints = createTestEndpoints();

        // Create delta with valid additions
        ConfigurationDelta delta = new ConfigurationDelta();
        delta.addedDatabases.put("db2", createDatabaseConfig("db2", "jdbc:h2:mem:db2"));
        delta.addedQueries.put("query2", createQueryConfig("query2", "db2", "SELECT * FROM table2"));
        delta.addedEndpoints.put("endpoint2", createEndpointConfig("endpoint2", "/api/test2", "query2"));

        // Add new items to all configurations for validation
        Map<String, DatabaseConfig> allDatabases = new HashMap<>(databases);
        allDatabases.putAll(delta.addedDatabases);
        
        Map<String, QueryConfig> allQueries = new HashMap<>(queries);
        allQueries.putAll(delta.addedQueries);
        
        Map<String, ApiEndpointConfig> allEndpoints = new HashMap<>(endpoints);
        allEndpoints.putAll(delta.addedEndpoints);

        // Validate dependencies
        ConfigurationValidationResult result = stateManager.validateDependencies(delta, allDatabases, allQueries, allEndpoints);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void shouldDetectInvalidDependencies() {
        // Create delta with invalid dependencies
        ConfigurationDelta delta = new ConfigurationDelta();
        delta.addedQueries.put("query2", createQueryConfig("query2", "nonexistent_db", "SELECT * FROM table2"));
        delta.addedEndpoints.put("endpoint2", createEndpointConfig("endpoint2", "/api/test2", "nonexistent_query"));

        // Validate dependencies with empty configurations
        ConfigurationValidationResult result = stateManager.validateDependencies(
            delta, new HashMap<>(), new HashMap<>(), new HashMap<>());

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(2);
        assertThat(result.getErrors().get(0)).contains("nonexistent_db");
        assertThat(result.getErrors().get(1)).contains("nonexistent_query");
    }

    @Test
    void shouldProvideStatistics() {
        ConfigurationStateStatistics stats = stateManager.getStatistics();
        assertThat(stats.getTotalSnapshots()).isEqualTo(0);
        assertThat(stats.getCurrentVersion()).isNull();

        // Create a snapshot
        stateManager.createSnapshot(createTestDatabases(), createTestQueries(), createTestEndpoints());

        stats = stateManager.getStatistics();
        assertThat(stats.getTotalSnapshots()).isEqualTo(1);
        assertThat(stats.getCurrentVersion()).isNotNull();
        assertThat(stats.getCurrentTimestamp()).isNotNull();
    }

    // Helper methods to create test configurations
    private Map<String, DatabaseConfig> createTestDatabases() {
        Map<String, DatabaseConfig> databases = new HashMap<>();
        databases.put("testdb", createDatabaseConfig("testdb", "jdbc:h2:mem:testdb"));
        return databases;
    }

    private Map<String, QueryConfig> createTestQueries() {
        Map<String, QueryConfig> queries = new HashMap<>();
        queries.put("testquery", createQueryConfig("testquery", "testdb", "SELECT * FROM test"));
        return queries;
    }

    private Map<String, ApiEndpointConfig> createTestEndpoints() {
        Map<String, ApiEndpointConfig> endpoints = new HashMap<>();
        endpoints.put("testendpoint", createEndpointConfig("testendpoint", "/api/test", "testquery"));
        return endpoints;
    }

    private DatabaseConfig createDatabaseConfig(String name, String url) {
        DatabaseConfig config = new DatabaseConfig();
        config.setName(name);
        config.setUrl(url);
        config.setUsername("sa");
        config.setPassword("");
        config.setDriver("org.h2.Driver");
        return config;
    }

    private QueryConfig createQueryConfig(String name, String database, String sql) {
        QueryConfig config = new QueryConfig();
        config.setName(name);
        config.setDatabase(database);
        config.setSql(sql);
        return config;
    }

    private ApiEndpointConfig createEndpointConfig(String name, String path, String query) {
        ApiEndpointConfig config = new ApiEndpointConfig();
        config.setPath(path);
        config.setMethod("GET");
        config.setQuery(query);
        return config;
    }
}

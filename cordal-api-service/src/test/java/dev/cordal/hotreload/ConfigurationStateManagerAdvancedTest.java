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
 * Advanced tests for ConfigurationStateManager to prove real-world scenarios
 */
class ConfigurationStateManagerAdvancedTest {

    private ConfigurationStateManager stateManager;

    @BeforeEach
    void setUp() {
        stateManager = new ConfigurationStateManager();
    }

    @Test
    void shouldHandleComplexConfigurationEvolution() {
        System.out.println("=== Testing Complex Configuration Evolution ===");

        // Phase 1: Initial configuration
        Map<String, DatabaseConfig> databases1 = createDatabaseConfigs("userdb", "productdb");
        Map<String, QueryConfig> queries1 = createQueryConfigs(
            Map.of("get_users", "userdb", "get_products", "productdb")
        );
        Map<String, ApiEndpointConfig> endpoints1 = createEndpointConfigs(
            Map.of("users_api", "get_users", "products_api", "get_products")
        );

        String version1 = stateManager.createSnapshot(databases1, queries1, endpoints1);
        System.out.println("Phase 1 - Initial config: " + version1);
        System.out.println("  Databases: " + databases1.keySet());
        System.out.println("  Queries: " + queries1.keySet());
        System.out.println("  Endpoints: " + endpoints1.keySet());

        // Phase 2: Add analytics database and related queries/endpoints
        Map<String, DatabaseConfig> databases2 = new HashMap<>(databases1);
        databases2.put("analyticsdb", createDatabaseConfig("analyticsdb", "jdbc:h2:mem:analytics"));

        Map<String, QueryConfig> queries2 = new HashMap<>(queries1);
        queries2.put("get_user_analytics", createQueryConfig("get_user_analytics", "analyticsdb", "SELECT * FROM user_stats"));
        queries2.put("get_product_analytics", createQueryConfig("get_product_analytics", "analyticsdb", "SELECT * FROM product_stats"));

        Map<String, ApiEndpointConfig> endpoints2 = new HashMap<>(endpoints1);
        endpoints2.put("user_analytics_api", createEndpointConfig("user_analytics_api", "/api/analytics/users", "get_user_analytics"));
        endpoints2.put("product_analytics_api", createEndpointConfig("product_analytics_api", "/api/analytics/products", "get_product_analytics"));

        String version2 = stateManager.createSnapshot(databases2, queries2, endpoints2);
        System.out.println("Phase 2 - Added analytics: " + version2);

        // Calculate delta from phase 1 to phase 2
        ConfigurationSnapshot snapshot1 = stateManager.getSnapshot(version1).get();
        ConfigurationDelta delta12 = stateManager.calculateDelta(snapshot1, databases2, queries2, endpoints2);

        assertThat(delta12.addedDatabases).hasSize(1);
        assertThat(delta12.addedQueries).hasSize(2);
        assertThat(delta12.addedEndpoints).hasSize(2);
        assertThat(delta12.modifiedDatabases).isEmpty();
        assertThat(delta12.removedDatabases).isEmpty();

        System.out.println("  Delta 1->2: " + delta12);

        // Phase 3: Modify existing query and remove product functionality
        Map<String, DatabaseConfig> databases3 = new HashMap<>(databases2);
        databases3.remove("productdb"); // Remove product database

        Map<String, QueryConfig> queries3 = new HashMap<>(queries2);
        queries3.remove("get_products");
        queries3.remove("get_product_analytics");
        // Modify user query
        queries3.put("get_users", createQueryConfig("get_users", "userdb", "SELECT id, name, email FROM users WHERE active = 1"));

        Map<String, ApiEndpointConfig> endpoints3 = new HashMap<>(endpoints2);
        endpoints3.remove("products_api");
        endpoints3.remove("product_analytics_api");

        String version3 = stateManager.createSnapshot(databases3, queries3, endpoints3);
        System.out.println("Phase 3 - Removed products, modified users: " + version3);

        // Calculate delta from phase 2 to phase 3
        ConfigurationSnapshot snapshot2 = stateManager.getSnapshot(version2).get();
        ConfigurationDelta delta23 = stateManager.calculateDelta(snapshot2, databases3, queries3, endpoints3);

        assertThat(delta23.removedDatabases).containsExactly("productdb");
        assertThat(delta23.removedQueries).containsExactlyInAnyOrder("get_products", "get_product_analytics");
        assertThat(delta23.removedEndpoints).containsExactlyInAnyOrder("products_api", "product_analytics_api");
        assertThat(delta23.modifiedQueries).containsKey("get_users");

        System.out.println("  Delta 2->3: " + delta23);

        // Verify we can restore any version
        stateManager.restoreSnapshot(version1);
        assertThat(stateManager.getCurrentSnapshot().get().getVersion()).isEqualTo(version1);

        stateManager.restoreSnapshot(version2);
        assertThat(stateManager.getCurrentSnapshot().get().getVersion()).isEqualTo(version2);

        System.out.println("Configuration evolution test completed successfully");
    }

    @Test
    void shouldValidateComplexDependencyChains() {
        System.out.println("=== Testing Complex Dependency Validation ===");

        // Create a complex configuration with multiple dependency chains
        Map<String, DatabaseConfig> databases = createDatabaseConfigs("userdb", "orderdb", "inventorydb");
        
        Map<String, QueryConfig> queries = new HashMap<>();
        queries.put("get_users", createQueryConfig("get_users", "userdb", "SELECT * FROM users"));
        queries.put("get_user_orders", createQueryConfig("get_user_orders", "orderdb", "SELECT * FROM orders WHERE user_id = ?"));
        queries.put("get_order_items", createQueryConfig("get_order_items", "inventorydb", "SELECT * FROM order_items WHERE order_id = ?"));
        queries.put("get_inventory", createQueryConfig("get_inventory", "inventorydb", "SELECT * FROM inventory"));

        Map<String, ApiEndpointConfig> endpoints = new HashMap<>();
        endpoints.put("users_api", createEndpointConfig("users_api", "/api/users", "get_users"));
        endpoints.put("user_orders_api", createEndpointConfig("user_orders_api", "/api/users/{id}/orders", "get_user_orders"));
        endpoints.put("order_items_api", createEndpointConfig("order_items_api", "/api/orders/{id}/items", "get_order_items"));
        endpoints.put("inventory_api", createEndpointConfig("inventory_api", "/api/inventory", "get_inventory"));

        String version = stateManager.createSnapshot(databases, queries, endpoints);
        System.out.println("Created complex configuration: " + version);

        // Test valid additions
        ConfigurationDelta validDelta = new ConfigurationDelta();
        validDelta.addedDatabases.put("analyticsdb", createDatabaseConfig("analyticsdb", "jdbc:h2:mem:analytics"));
        validDelta.addedQueries.put("get_analytics", createQueryConfig("get_analytics", "analyticsdb", "SELECT * FROM analytics"));
        validDelta.addedEndpoints.put("analytics_api", createEndpointConfig("analytics_api", "/api/analytics", "get_analytics"));

        Map<String, DatabaseConfig> allDatabases = new HashMap<>(databases);
        allDatabases.putAll(validDelta.addedDatabases);
        Map<String, QueryConfig> allQueries = new HashMap<>(queries);
        allQueries.putAll(validDelta.addedQueries);
        Map<String, ApiEndpointConfig> allEndpoints = new HashMap<>(endpoints);
        allEndpoints.putAll(validDelta.addedEndpoints);

        ConfigurationValidationResult validResult = stateManager.validateDependencies(validDelta, allDatabases, allQueries, allEndpoints);
        assertThat(validResult.isValid()).isTrue();
        System.out.println("Valid additions passed validation");

        // Test invalid dependency - query references non-existent database
        ConfigurationDelta invalidDelta1 = new ConfigurationDelta();
        invalidDelta1.addedQueries.put("bad_query", createQueryConfig("bad_query", "nonexistent_db", "SELECT * FROM nowhere"));
        invalidDelta1.addedEndpoints.put("bad_api", createEndpointConfig("bad_api", "/api/bad", "bad_query"));

        Map<String, QueryConfig> badQueries = new HashMap<>(queries);
        badQueries.putAll(invalidDelta1.addedQueries);
        Map<String, ApiEndpointConfig> badEndpoints = new HashMap<>(endpoints);
        badEndpoints.putAll(invalidDelta1.addedEndpoints);

        ConfigurationValidationResult invalidResult1 = stateManager.validateDependencies(invalidDelta1, databases, badQueries, badEndpoints);
        assertThat(invalidResult1.isValid()).isFalse();
        assertThat(invalidResult1.getErrors()).hasSize(1);
        assertThat(invalidResult1.getErrors().get(0)).contains("nonexistent_db");
        System.out.println("Invalid database reference correctly detected: " + invalidResult1.getErrors().get(0));

        // Test invalid dependency - endpoint references non-existent query
        ConfigurationDelta invalidDelta2 = new ConfigurationDelta();
        invalidDelta2.addedEndpoints.put("orphan_api", createEndpointConfig("orphan_api", "/api/orphan", "nonexistent_query"));

        Map<String, ApiEndpointConfig> orphanEndpoints = new HashMap<>(endpoints);
        orphanEndpoints.putAll(invalidDelta2.addedEndpoints);

        ConfigurationValidationResult invalidResult2 = stateManager.validateDependencies(invalidDelta2, databases, queries, orphanEndpoints);
        assertThat(invalidResult2.isValid()).isFalse();
        assertThat(invalidResult2.getErrors()).hasSize(1);
        assertThat(invalidResult2.getErrors().get(0)).contains("nonexistent_query");
        System.out.println("Invalid query reference correctly detected: " + invalidResult2.getErrors().get(0));

        // Test removal validation - try to remove database that's still in use
        ConfigurationDelta removalDelta = new ConfigurationDelta();
        removalDelta.removedDatabases.add("userdb"); // Still used by get_users query

        ConfigurationValidationResult removalResult = stateManager.validateDependencies(removalDelta, databases, queries, endpoints);
        assertThat(removalResult.isValid()).isFalse();
        assertThat(removalResult.getErrors()).hasSize(1);
        assertThat(removalResult.getErrors().get(0)).contains("userdb");
        System.out.println("Invalid database removal correctly detected: " + removalResult.getErrors().get(0));

        System.out.println("Complex dependency validation test completed successfully");
    }

    @Test
    void shouldManageSnapshotHistoryCorrectly() {
        System.out.println("=== Testing Snapshot History Management ===");

        // Create more snapshots than the history limit (10)
        for (int i = 1; i <= 12; i++) {
            Map<String, DatabaseConfig> databases = createDatabaseConfigs("db" + i);
            Map<String, QueryConfig> queries = createQueryConfigs(Map.of("query" + i, "db" + i));
            Map<String, ApiEndpointConfig> endpoints = createEndpointConfigs(Map.of("endpoint" + i, "query" + i));

            String version = stateManager.createSnapshot(databases, queries, endpoints);
            System.out.println("Created snapshot " + i + ": " + version);

            // Small delay to ensure different timestamps
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        ConfigurationStateStatistics stats = stateManager.getStatistics();
        assertThat(stats.getTotalSnapshots()).isEqualTo(10); // Should be limited to 10
        assertThat(stats.getCurrentVersion()).isNotNull();

        // Verify we can access the 10 most recent snapshots
        for (String version : stateManager.getAvailableVersions()) {
            Optional<ConfigurationSnapshot> snapshot = stateManager.getSnapshot(version);
            assertThat(snapshot).isPresent();
            System.out.println("Available snapshot: " + version + " (timestamp: " + snapshot.get().getTimestamp() + ")");
        }

        System.out.println("Snapshot history management test completed - maintaining " + stats.getTotalSnapshots() + " snapshots");
    }

    @Test
    void shouldCalculateAccurateDeltaSummaries() {
        System.out.println("=== Testing Delta Summary Calculations ===");

        // Create initial configuration
        Map<String, DatabaseConfig> initialDatabases = createDatabaseConfigs("db1", "db2");
        Map<String, QueryConfig> initialQueries = createQueryConfigs(Map.of("q1", "db1", "q2", "db2"));
        Map<String, ApiEndpointConfig> initialEndpoints = createEndpointConfigs(Map.of("e1", "q1", "e2", "q2"));

        String initialVersion = stateManager.createSnapshot(initialDatabases, initialQueries, initialEndpoints);
        ConfigurationSnapshot initialSnapshot = stateManager.getSnapshot(initialVersion).get();

        // Create modified configuration with mixed changes
        Map<String, DatabaseConfig> newDatabases = new HashMap<>(initialDatabases);
        newDatabases.put("db3", createDatabaseConfig("db3", "jdbc:h2:mem:db3")); // Added
        newDatabases.remove("db2"); // Removed
        // db1 modified (change URL)
        newDatabases.put("db1", createDatabaseConfig("db1", "jdbc:h2:mem:db1_modified"));

        Map<String, QueryConfig> newQueries = new HashMap<>(initialQueries);
        newQueries.put("q3", createQueryConfig("q3", "db3", "SELECT * FROM table3")); // Added
        newQueries.remove("q2"); // Removed
        // q1 modified (change SQL)
        newQueries.put("q1", createQueryConfig("q1", "db1", "SELECT id, name FROM table1 WHERE active = 1"));

        Map<String, ApiEndpointConfig> newEndpoints = new HashMap<>(initialEndpoints);
        newEndpoints.put("e3", createEndpointConfig("e3", "/api/endpoint3", "q3")); // Added
        newEndpoints.remove("e2"); // Removed
        // e1 modified (change path)
        newEndpoints.put("e1", createEndpointConfig("e1", "/api/modified-endpoint1", "q1"));

        ConfigurationDelta delta = stateManager.calculateDelta(initialSnapshot, newDatabases, newQueries, newEndpoints);
        ConfigurationDelta.ConfigurationDeltaSummary summary = delta.getSummary();

        System.out.println("Delta summary: " + summary);

        // Verify summary counts
        assertThat(summary.getAddedDatabases()).isEqualTo(1);
        assertThat(summary.getModifiedDatabases()).isEqualTo(1);
        assertThat(summary.getRemovedDatabases()).isEqualTo(1);

        assertThat(summary.getAddedQueries()).isEqualTo(1);
        assertThat(summary.getModifiedQueries()).isEqualTo(1);
        assertThat(summary.getRemovedQueries()).isEqualTo(1);

        assertThat(summary.getAddedEndpoints()).isEqualTo(1);
        assertThat(summary.getModifiedEndpoints()).isEqualTo(1);
        assertThat(summary.getRemovedEndpoints()).isEqualTo(1);

        assertThat(summary.getTotalChanges()).isEqualTo(9);

        // Verify affected items
        assertThat(delta.getAffectedDatabases()).containsExactlyInAnyOrder("db1", "db2", "db3");
        assertThat(delta.getAffectedQueries()).containsExactlyInAnyOrder("q1", "q2", "q3");
        assertThat(delta.getAffectedEndpoints()).containsExactlyInAnyOrder("e1", "e2", "e3");

        System.out.println("Delta summary calculation test completed successfully");
    }

    // Helper methods
    private Map<String, DatabaseConfig> createDatabaseConfigs(String... names) {
        Map<String, DatabaseConfig> configs = new HashMap<>();
        for (String name : names) {
            configs.put(name, createDatabaseConfig(name, "jdbc:h2:mem:" + name));
        }
        return configs;
    }

    private Map<String, QueryConfig> createQueryConfigs(Map<String, String> queryToDatabase) {
        Map<String, QueryConfig> configs = new HashMap<>();
        for (Map.Entry<String, String> entry : queryToDatabase.entrySet()) {
            configs.put(entry.getKey(), createQueryConfig(entry.getKey(), entry.getValue(), "SELECT * FROM " + entry.getKey()));
        }
        return configs;
    }

    private Map<String, ApiEndpointConfig> createEndpointConfigs(Map<String, String> endpointToQuery) {
        Map<String, ApiEndpointConfig> configs = new HashMap<>();
        for (Map.Entry<String, String> entry : endpointToQuery.entrySet()) {
            configs.put(entry.getKey(), createEndpointConfig(entry.getKey(), "/api/" + entry.getKey(), entry.getValue()));
        }
        return configs;
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

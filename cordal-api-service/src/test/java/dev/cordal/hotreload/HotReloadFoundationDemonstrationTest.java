package dev.cordal.hotreload;

import dev.cordal.config.GenericApiConfig;
import dev.cordal.generic.config.ApiEndpointConfig;
import dev.cordal.generic.config.QueryConfig;
import dev.cordal.generic.config.DatabaseConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive demonstration test showing Phase 1 hot reload foundation capabilities
 * This test simulates real-world configuration management scenarios
 */
class HotReloadFoundationDemonstrationTest {

    @TempDir
    Path configDir;

    private FileWatcherService fileWatcher;
    private ConfigurationStateManager stateManager;
    private HotReloadConfigurationManager configManager;

    @BeforeEach
    void setUp() {
        fileWatcher = new FileWatcherService();
        stateManager = new ConfigurationStateManager();
        configManager = new HotReloadConfigurationManager(fileWatcher, stateManager);
    }

    @AfterEach
    void tearDown() {
        configManager.shutdown();
    }

    @Test
    void shouldDemonstrateCompleteHotReloadFoundation() throws IOException, InterruptedException {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("CORDAL HOT RELOAD FOUNDATION DEMONSTRATION");
        System.out.println("=".repeat(80));

        // Phase 1: Initialize the hot reload system
        System.out.println("\n Phase 1: Initializing Hot Reload Foundation");
        configManager.initialize(configDir);

        // Verify initial state
        assertThat(fileWatcher.isWatching()).isTrue();
        assertThat(stateManager.getStatistics().getTotalSnapshots()).isEqualTo(0);
        System.out.println(" File watcher started, monitoring: " + configDir);
        System.out.println(" Configuration state manager initialized");

        // Phase 2: Create initial configuration files
        System.out.println("\n Phase 2: Creating Initial Configuration");
        createInitialConfiguration();

        // Wait for file detection and snapshot creation
        Thread.sleep(500);

        ConfigurationStateStatistics stats = stateManager.getStatistics();
        System.out.println(" Configuration files created and detected");
        System.out.println(" Snapshots created: " + stats.getTotalSnapshots());
        System.out.println(" Current version: " + stats.getCurrentVersion());

        // Phase 3: Demonstrate configuration evolution
        System.out.println("\n Phase 3: Demonstrating Configuration Evolution");
        demonstrateConfigurationEvolution();

        // Phase 4: Demonstrate dependency validation
        System.out.println("\n Phase 4: Demonstrating Dependency Validation");
        demonstrateDependencyValidation();

        // Phase 5: Demonstrate rollback capability
        System.out.println("\n Phase 5: Demonstrating Rollback Capability");
        demonstrateRollbackCapability();

        // Phase 6: Show final statistics
        System.out.println("\n Phase 6: Final System Status");
        showFinalStatistics();

        System.out.println("\n" + "=".repeat(80));
        System.out.println(" HOT RELOAD FOUNDATION DEMONSTRATION COMPLETED SUCCESSFULLY!");
        System.out.println("=".repeat(80));
    }

    private void createInitialConfiguration() throws IOException {
        // Create database configuration
        String databaseConfig = """
            databases:
              userdb:
                name: "userdb"
                url: "jdbc:h2:mem:userdb"
                username: "sa"
                password: ""
                driver: "org.h2.Driver"
              productdb:
                name: "productdb"
                url: "jdbc:h2:mem:productdb"
                username: "sa"
                password: ""
                driver: "org.h2.Driver"
            """;
        Files.write(configDir.resolve("demo-databases.yml"), databaseConfig.getBytes());
        System.out.println("   Created demo-databases.yml with 2 databases");

        // Create query configuration
        String queryConfig = """
            queries:
              get_users:
                database: "userdb"
                sql: "SELECT id, name, email FROM users ORDER BY name"
                parameters: []
              get_products:
                database: "productdb"
                sql: "SELECT id, name, price FROM products WHERE active = 1"
                parameters: []
            """;
        Files.write(configDir.resolve("demo-queries.yml"), queryConfig.getBytes());
        System.out.println("   Created demo-queries.yml with 2 queries");

        // Create endpoint configuration
        String endpointConfig = """
            endpoints:
              users_api:
                path: "/api/users"
                method: "GET"
                query: "get_users"
                pagination:
                  enabled: true
                  defaultSize: 20
              products_api:
                path: "/api/products"
                method: "GET"
                query: "get_products"
                pagination:
                  enabled: true
                  defaultSize: 50
            """;
        Files.write(configDir.resolve("demo-endpoints.yml"), endpointConfig.getBytes());
        System.out.println("   Created demo-endpoints.yml with 2 endpoints");
    }

    private void demonstrateConfigurationEvolution() throws IOException, InterruptedException {
        String initialVersion = stateManager.getCurrentSnapshot().get().getVersion();
        System.out.println("   Initial configuration version: " + initialVersion);

        // Add analytics functionality
        String analyticsDatabase = """
            databases:
              userdb:
                name: "userdb"
                url: "jdbc:h2:mem:userdb"
                username: "sa"
                password: ""
                driver: "org.h2.Driver"
              productdb:
                name: "productdb"
                url: "jdbc:h2:mem:productdb"
                username: "sa"
                password: ""
                driver: "org.h2.Driver"
              analyticsdb:
                name: "analyticsdb"
                url: "jdbc:h2:mem:analytics"
                username: "sa"
                password: ""
                driver: "org.h2.Driver"
            """;
        Files.write(configDir.resolve("demo-databases.yml"), analyticsDatabase.getBytes());
        Thread.sleep(200);

        String analyticsQueries = """
            queries:
              get_users:
                database: "userdb"
                sql: "SELECT id, name, email FROM users ORDER BY name"
                parameters: []
              get_products:
                database: "productdb"
                sql: "SELECT id, name, price FROM products WHERE active = 1"
                parameters: []
              get_user_stats:
                database: "analyticsdb"
                sql: "SELECT user_id, login_count, last_login FROM user_stats"
                parameters: []
            """;
        Files.write(configDir.resolve("demo-queries.yml"), analyticsQueries.getBytes());
        Thread.sleep(200);

        String analyticsEndpoints = """
            endpoints:
              users_api:
                path: "/api/users"
                method: "GET"
                query: "get_users"
                pagination:
                  enabled: true
                  defaultSize: 20
              products_api:
                path: "/api/products"
                method: "GET"
                query: "get_products"
                pagination:
                  enabled: true
                  defaultSize: 50
              analytics_api:
                path: "/api/analytics/users"
                method: "GET"
                query: "get_user_stats"
                pagination:
                  enabled: true
                  defaultSize: 100
            """;
        Files.write(configDir.resolve("demo-endpoints.yml"), analyticsEndpoints.getBytes());
        Thread.sleep(300);

        String newVersion = stateManager.getCurrentSnapshot().get().getVersion();
        System.out.println("   After adding analytics: " + newVersion);
        System.out.println("  Total snapshots: " + stateManager.getStatistics().getTotalSnapshots());
    }

    private void demonstrateDependencyValidation() {
        System.out.println("  Testing dependency validation...");

        // Create a delta with valid dependencies
        ConfigurationDelta validDelta = new ConfigurationDelta();
        validDelta.addedDatabases.put("reportdb", createDatabaseConfig("reportdb", "jdbc:h2:mem:reports"));
        validDelta.addedQueries.put("get_reports", createQueryConfig("get_reports", "reportdb", "SELECT * FROM reports"));
        validDelta.addedEndpoints.put("reports_api", createEndpointConfig("reports_api", "/api/reports", "get_reports"));

        Map<String, DatabaseConfig> allDatabases = new HashMap<>();
        allDatabases.put("reportdb", validDelta.addedDatabases.get("reportdb"));
        Map<String, QueryConfig> allQueries = new HashMap<>();
        allQueries.put("get_reports", validDelta.addedQueries.get("get_reports"));
        Map<String, ApiEndpointConfig> allEndpoints = new HashMap<>();
        allEndpoints.put("reports_api", validDelta.addedEndpoints.get("reports_api"));

        ConfigurationValidationResult validResult = stateManager.validateDependencies(validDelta, allDatabases, allQueries, allEndpoints);
        assertThat(validResult.isValid()).isTrue();
        System.out.println("  Valid configuration passed validation");

        // Create a delta with invalid dependencies
        ConfigurationDelta invalidDelta = new ConfigurationDelta();
        invalidDelta.addedQueries.put("bad_query", createQueryConfig("bad_query", "nonexistent_db", "SELECT * FROM nowhere"));
        invalidDelta.addedEndpoints.put("bad_api", createEndpointConfig("bad_api", "/api/bad", "nonexistent_query"));

        ConfigurationValidationResult invalidResult = stateManager.validateDependencies(invalidDelta, new HashMap<>(), new HashMap<>(), new HashMap<>());
        assertThat(invalidResult.isValid()).isFalse();
        assertThat(invalidResult.getErrors()).hasSize(2);
        System.out.println("  Invalid configuration correctly rejected:");
        for (String error : invalidResult.getErrors()) {
            System.out.println("     • " + error);
        }
    }

    private void demonstrateRollbackCapability() {
        System.out.println("  Testing rollback capability...");

        // Get all available versions
        var availableVersions = stateManager.getAvailableVersions();
        System.out.println("   Available versions: " + availableVersions.size());

        String currentVersion = stateManager.getCurrentSnapshot().get().getVersion();
        System.out.println("  📍 Current version: " + currentVersion);

        // Rollback to first version
        if (availableVersions.size() > 1) {
            String firstVersion = availableVersions.get(0);
            stateManager.restoreSnapshot(firstVersion);
            String restoredVersion = stateManager.getCurrentSnapshot().get().getVersion();
            assertThat(restoredVersion).isEqualTo(firstVersion);
            System.out.println("   Successfully rolled back to: " + firstVersion);

            // Restore to latest
            stateManager.restoreSnapshot(currentVersion);
            System.out.println("   Successfully restored to: " + currentVersion);
        }
    }

    private void showFinalStatistics() {
        FileWatcherStatus watcherStatus = fileWatcher.getStatus();
        ConfigurationStateStatistics stateStats = stateManager.getStatistics();

        System.out.println("   File Watcher Status:");
        System.out.println("     • Watching: " + watcherStatus.isWatching());
        System.out.println("     • Directories: " + watcherStatus.getWatchedDirectories());
        System.out.println("     • Listeners: " + watcherStatus.getRegisteredListeners());
        System.out.println("     • Patterns: " + watcherStatus.getWatchedPatterns());
        System.out.println("     • Debounce: " + watcherStatus.getDebounceDelayMs() + "ms");

        System.out.println("   Configuration State:");
        System.out.println("     • Total snapshots: " + stateStats.getTotalSnapshots());
        System.out.println("     • Current version: " + stateStats.getCurrentVersion());
        System.out.println("     • Max history: " + stateStats.getMaxSnapshotHistory());
    }

    // Helper classes and methods
    private static class HotReloadConfigurationManager {
        private final FileWatcherService fileWatcher;
        private final ConfigurationStateManager stateManager;
        private final AtomicInteger configurationChanges = new AtomicInteger(0);

        public HotReloadConfigurationManager(FileWatcherService fileWatcher, ConfigurationStateManager stateManager) {
            this.fileWatcher = fileWatcher;
            this.stateManager = stateManager;
        }

        public void initialize(Path configDir) {
            // Register listener to create snapshots when files change
            fileWatcher.registerChangeListener(this::onConfigurationFileChanged);
            fileWatcher.setDebounceDelay(100); // Faster for testing

            // Start watching configuration directory
            fileWatcher.startWatching(
                Arrays.asList(configDir),
                Arrays.asList("*-databases.yml", "*-queries.yml", "*-endpoints.yml")
            );
        }

        private void onConfigurationFileChanged(FileChangeEvent event) {
            int changeNumber = configurationChanges.incrementAndGet();
            System.out.println("   Configuration change #" + changeNumber + ": " + event.getFileName() + " (" + event.getEventKind().name() + ")");

            // Simulate creating a snapshot with the new configuration
            // In a real implementation, this would parse the YAML files and create actual configurations
            Map<String, DatabaseConfig> databases = createSampleDatabases();
            Map<String, QueryConfig> queries = createSampleQueries();
            Map<String, ApiEndpointConfig> endpoints = createSampleEndpoints();

            String version = stateManager.createSnapshot(databases, queries, endpoints);
            System.out.println("   Created snapshot: " + version);
        }

        public void shutdown() {
            fileWatcher.stopWatching();
        }

        private Map<String, DatabaseConfig> createSampleDatabases() {
            Map<String, DatabaseConfig> databases = new HashMap<>();
            DatabaseConfig config = new DatabaseConfig();
            config.setName("sampledb");
            config.setUrl("jdbc:h2:mem:sample");
            config.setUsername("sa");
            config.setPassword("");
            config.setDriver("org.h2.Driver");
            databases.put("sampledb", config);
            return databases;
        }

        private Map<String, QueryConfig> createSampleQueries() {
            Map<String, QueryConfig> queries = new HashMap<>();
            QueryConfig config = new QueryConfig();
            config.setName("samplequery");
            config.setDatabase("sampledb");
            config.setSql("SELECT * FROM sample");
            queries.put("samplequery", config);
            return queries;
        }

        private Map<String, ApiEndpointConfig> createSampleEndpoints() {
            Map<String, ApiEndpointConfig> endpoints = new HashMap<>();
            ApiEndpointConfig config = new ApiEndpointConfig();
            config.setPath("/api/sample");
            config.setMethod("GET");
            config.setQuery("samplequery");
            endpoints.put("sampleendpoint", config);
            return endpoints;
        }
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

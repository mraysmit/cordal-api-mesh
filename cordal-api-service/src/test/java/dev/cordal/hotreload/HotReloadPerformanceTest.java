package dev.cordal.hotreload;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance tests for the hot reload foundation to ensure it can handle
 * high-frequency changes and large configuration sets efficiently
 */
class HotReloadPerformanceTest {

    @TempDir
    Path tempDir;

    private FileWatcherService fileWatcherService;
    private ConfigurationStateManager stateManager;

    @BeforeEach
    void setUp() {
        fileWatcherService = new FileWatcherService();
        stateManager = new ConfigurationStateManager();
    }

    @AfterEach
    void tearDown() {
        if (fileWatcherService.isWatching()) {
            fileWatcherService.stopWatching();
        }
    }

    @Test
    void shouldHandleHighFrequencyFileChanges() throws IOException, InterruptedException {
        System.out.println("\n🚀 Performance Test: High-Frequency File Changes");
        
        AtomicInteger eventCount = new AtomicInteger(0);
        AtomicLong totalProcessingTime = new AtomicLong(0);
        CountDownLatch latch = new CountDownLatch(1); // We expect debouncing to reduce events
        
        ConfigurationChangeListener listener = event -> {
            long startTime = System.nanoTime();
            
            // Simulate processing time
            eventCount.incrementAndGet();
            
            long processingTime = System.nanoTime() - startTime;
            totalProcessingTime.addAndGet(processingTime);
            
            System.out.println("  📝 Processed event: " + event.getFileName() + " (processing time: " + 
                             (processingTime / 1_000_000) + "ms)");
            latch.countDown();
        };

        fileWatcherService.registerChangeListener(listener);
        fileWatcherService.setDebounceDelay(200); // 200ms debounce
        
        fileWatcherService.startWatching(
            Arrays.asList(tempDir),
            Arrays.asList("*.yml")
        );

        Path testFile = tempDir.resolve("high-frequency-test.yml");
        
        // Generate rapid file changes (should be debounced)
        long startTime = System.currentTimeMillis();
        int totalChanges = 50;
        
        for (int i = 0; i < totalChanges; i++) {
            String content = "version: " + i + "\ntimestamp: " + System.currentTimeMillis() + "\n";
            Files.write(testFile, content.getBytes(), 
                       i == 0 ? StandardOpenOption.CREATE : StandardOpenOption.TRUNCATE_EXISTING);
            Thread.sleep(10); // 10ms between changes
        }
        
        long changeGenerationTime = System.currentTimeMillis() - startTime;
        
        // Wait for debounced events
        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        
        long totalTime = System.currentTimeMillis() - startTime;
        
        System.out.println("  📊 Performance Results:");
        System.out.println("     • Total file changes: " + totalChanges);
        System.out.println("     • Change generation time: " + changeGenerationTime + "ms");
        System.out.println("     • Events processed: " + eventCount.get());
        System.out.println("     • Total processing time: " + totalTime + "ms");
        System.out.println("     • Average processing time per event: " + 
                         (totalProcessingTime.get() / Math.max(1, eventCount.get()) / 1_000_000) + "ms");
        
        // Verify debouncing worked (should have much fewer events than changes)
        assertThat(eventCount.get()).isLessThan(totalChanges);
        System.out.println("  ✅ Debouncing effective: " + totalChanges + " changes → " + eventCount.get() + " events");
    }

    @Test
    void shouldHandleLargeConfigurationSnapshots() {
        System.out.println("\n🚀 Performance Test: Large Configuration Snapshots");
        
        long startTime = System.currentTimeMillis();
        
        // Create large configuration sets
        int databaseCount = 100;
        int queryCount = 500;
        int endpointCount = 1000;
        
        var databases = createLargeDatabaseSet(databaseCount);
        var queries = createLargeQuerySet(queryCount, databases);
        var endpoints = createLargeEndpointSet(endpointCount, queries);
        
        long configCreationTime = System.currentTimeMillis() - startTime;
        
        // Create snapshot
        startTime = System.currentTimeMillis();
        String version = stateManager.createSnapshot(databases, queries, endpoints);
        long snapshotCreationTime = System.currentTimeMillis() - startTime;
        
        // Calculate delta with modified configuration
        startTime = System.currentTimeMillis();
        var modifiedDatabases = new HashMap<>(databases);
        modifiedDatabases.put("new_db", createDatabaseConfig("new_db", "jdbc:h2:mem:new_db"));
        
        var snapshot = stateManager.getCurrentSnapshot().get();
        var delta = stateManager.calculateDelta(snapshot, modifiedDatabases, queries, endpoints);
        long deltaCalculationTime = System.currentTimeMillis() - startTime;
        
        // Validate dependencies
        startTime = System.currentTimeMillis();
        var validationResult = stateManager.validateDependencies(delta, modifiedDatabases, queries, endpoints);
        long validationTime = System.currentTimeMillis() - startTime;
        
        System.out.println("  📊 Large Configuration Performance:");
        System.out.println("     • Databases: " + databaseCount);
        System.out.println("     • Queries: " + queryCount);
        System.out.println("     • Endpoints: " + endpointCount);
        System.out.println("     • Config creation time: " + configCreationTime + "ms");
        System.out.println("     • Snapshot creation time: " + snapshotCreationTime + "ms");
        System.out.println("     • Delta calculation time: " + deltaCalculationTime + "ms");
        System.out.println("     • Validation time: " + validationTime + "ms");
        
        assertThat(validationResult.isValid()).isTrue();
        assertThat(delta.addedDatabases).hasSize(1);
        
        // Performance assertions (reasonable thresholds)
        assertThat(snapshotCreationTime).isLessThan(1000); // Should be under 1 second
        assertThat(deltaCalculationTime).isLessThan(500);  // Should be under 500ms
        assertThat(validationTime).isLessThan(200);        // Should be under 200ms
        
        System.out.println("  ✅ Large configuration handling within performance thresholds");
    }

    @Test
    void shouldHandleMultipleSimultaneousFileChanges() throws IOException, InterruptedException {
        System.out.println("\n🚀 Performance Test: Multiple Simultaneous File Changes");
        
        AtomicInteger eventCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3); // Expect 3 different files
        
        ConfigurationChangeListener listener = event -> {
            eventCount.incrementAndGet();
            System.out.println("  📝 Event: " + event.getFileName() + " (" + event.getFileType() + ")");
            latch.countDown();
        };

        fileWatcherService.registerChangeListener(listener);
        fileWatcherService.setDebounceDelay(100);
        
        fileWatcherService.startWatching(
            Arrays.asList(tempDir),
            Arrays.asList("*.yml")
        );

        long startTime = System.currentTimeMillis();
        
        // Create multiple files simultaneously
        Path databaseFile = tempDir.resolve("perf-databases.yml");
        Path queryFile = tempDir.resolve("perf-queries.yml");
        Path endpointFile = tempDir.resolve("perf-endpoints.yml");
        
        // Write files in quick succession
        Files.write(databaseFile, createLargeDatabaseYaml(50).getBytes());
        Files.write(queryFile, createLargeQueryYaml(100).getBytes());
        Files.write(endpointFile, createLargeEndpointYaml(200).getBytes());
        
        boolean allEventsReceived = latch.await(3, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - startTime;
        
        assertThat(allEventsReceived).isTrue();
        assertThat(eventCount.get()).isEqualTo(3);
        
        System.out.println("  📊 Simultaneous File Changes Performance:");
        System.out.println("     • Files created: 3");
        System.out.println("     • Events received: " + eventCount.get());
        System.out.println("     • Total time: " + totalTime + "ms");
        System.out.println("  ✅ All simultaneous file changes detected correctly");
    }

    @Test
    void shouldMaintainPerformanceWithManySnapshots() {
        System.out.println("\n🚀 Performance Test: Many Snapshots Management");
        
        long startTime = System.currentTimeMillis();
        
        // Create many snapshots (more than the history limit)
        int snapshotCount = 25; // More than the 10 snapshot limit
        
        for (int i = 1; i <= snapshotCount; i++) {
            var databases = createSmallDatabaseSet(i);
            var queries = createSmallQuerySet(i, databases);
            var endpoints = createSmallEndpointSet(i, queries);
            
            stateManager.createSnapshot(databases, queries, endpoints);
            
            // Small delay to ensure different timestamps
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        long snapshotCreationTime = System.currentTimeMillis() - startTime;
        
        // Test snapshot retrieval performance
        startTime = System.currentTimeMillis();
        var availableVersions = stateManager.getAvailableVersions();
        long retrievalTime = System.currentTimeMillis() - startTime;
        
        // Test statistics performance
        startTime = System.currentTimeMillis();
        var statistics = stateManager.getStatistics();
        long statisticsTime = System.currentTimeMillis() - startTime;
        
        System.out.println("  📊 Snapshot Management Performance:");
        System.out.println("     • Snapshots created: " + snapshotCount);
        System.out.println("     • Snapshots retained: " + statistics.getTotalSnapshots());
        System.out.println("     • Available versions: " + availableVersions.size());
        System.out.println("     • Creation time: " + snapshotCreationTime + "ms");
        System.out.println("     • Retrieval time: " + retrievalTime + "ms");
        System.out.println("     • Statistics time: " + statisticsTime + "ms");
        
        // Verify history management
        assertThat(statistics.getTotalSnapshots()).isEqualTo(10); // Should be limited to 10
        assertThat(availableVersions).hasSize(10);
        
        // Performance assertions
        assertThat(retrievalTime).isLessThan(50);    // Should be very fast
        assertThat(statisticsTime).isLessThan(10);   // Should be very fast
        
        System.out.println("  ✅ Snapshot history management efficient and within limits");
    }

    // Helper methods for creating test data
    private HashMap<String, dev.cordal.generic.config.DatabaseConfig> createLargeDatabaseSet(int count) {
        var databases = new HashMap<String, dev.cordal.generic.config.DatabaseConfig>();
        for (int i = 1; i <= count; i++) {
            databases.put("db" + i, createDatabaseConfig("db" + i, "jdbc:h2:mem:db" + i));
        }
        return databases;
    }

    private HashMap<String, dev.cordal.generic.config.QueryConfig> createLargeQuerySet(int count, 
            HashMap<String, dev.cordal.generic.config.DatabaseConfig> databases) {
        var queries = new HashMap<String, dev.cordal.generic.config.QueryConfig>();
        var dbNames = databases.keySet().toArray(new String[0]);
        
        for (int i = 1; i <= count; i++) {
            String dbName = dbNames[i % dbNames.length];
            queries.put("query" + i, createQueryConfig("query" + i, dbName, "SELECT * FROM table" + i));
        }
        return queries;
    }

    private HashMap<String, dev.cordal.generic.config.ApiEndpointConfig> createLargeEndpointSet(int count,
            HashMap<String, dev.cordal.generic.config.QueryConfig> queries) {
        var endpoints = new HashMap<String, dev.cordal.generic.config.ApiEndpointConfig>();
        var queryNames = queries.keySet().toArray(new String[0]);
        
        for (int i = 1; i <= count; i++) {
            String queryName = queryNames[i % queryNames.length];
            endpoints.put("endpoint" + i, createEndpointConfig("endpoint" + i, "/api/endpoint" + i, queryName));
        }
        return endpoints;
    }

    private HashMap<String, dev.cordal.generic.config.DatabaseConfig> createSmallDatabaseSet(int suffix) {
        var databases = new HashMap<String, dev.cordal.generic.config.DatabaseConfig>();
        databases.put("db" + suffix, createDatabaseConfig("db" + suffix, "jdbc:h2:mem:db" + suffix));
        return databases;
    }

    private HashMap<String, dev.cordal.generic.config.QueryConfig> createSmallQuerySet(int suffix,
            HashMap<String, dev.cordal.generic.config.DatabaseConfig> databases) {
        var queries = new HashMap<String, dev.cordal.generic.config.QueryConfig>();
        String dbName = databases.keySet().iterator().next();
        queries.put("query" + suffix, createQueryConfig("query" + suffix, dbName, "SELECT * FROM table" + suffix));
        return queries;
    }

    private HashMap<String, dev.cordal.generic.config.ApiEndpointConfig> createSmallEndpointSet(int suffix,
            HashMap<String, dev.cordal.generic.config.QueryConfig> queries) {
        var endpoints = new HashMap<String, dev.cordal.generic.config.ApiEndpointConfig>();
        String queryName = queries.keySet().iterator().next();
        endpoints.put("endpoint" + suffix, createEndpointConfig("endpoint" + suffix, "/api/endpoint" + suffix, queryName));
        return endpoints;
    }

    private String createLargeDatabaseYaml(int count) {
        StringBuilder yaml = new StringBuilder("databases:\n");
        for (int i = 1; i <= count; i++) {
            yaml.append("  db").append(i).append(":\n");
            yaml.append("    name: \"db").append(i).append("\"\n");
            yaml.append("    url: \"jdbc:h2:mem:db").append(i).append("\"\n");
            yaml.append("    username: \"sa\"\n");
            yaml.append("    password: \"\"\n");
            yaml.append("    driver: \"org.h2.Driver\"\n");
        }
        return yaml.toString();
    }

    private String createLargeQueryYaml(int count) {
        StringBuilder yaml = new StringBuilder("queries:\n");
        for (int i = 1; i <= count; i++) {
            yaml.append("  query").append(i).append(":\n");
            yaml.append("    database: \"db").append((i % 50) + 1).append("\"\n");
            yaml.append("    sql: \"SELECT * FROM table").append(i).append("\"\n");
            yaml.append("    parameters: []\n");
        }
        return yaml.toString();
    }

    private String createLargeEndpointYaml(int count) {
        StringBuilder yaml = new StringBuilder("endpoints:\n");
        for (int i = 1; i <= count; i++) {
            yaml.append("  endpoint").append(i).append(":\n");
            yaml.append("    path: \"/api/endpoint").append(i).append("\"\n");
            yaml.append("    method: \"GET\"\n");
            yaml.append("    query: \"query").append((i % 100) + 1).append("\"\n");
        }
        return yaml.toString();
    }

    private dev.cordal.generic.config.DatabaseConfig createDatabaseConfig(String name, String url) {
        var config = new dev.cordal.generic.config.DatabaseConfig();
        config.setName(name);
        config.setUrl(url);
        config.setUsername("sa");
        config.setPassword("");
        config.setDriver("org.h2.Driver");
        return config;
    }

    private dev.cordal.generic.config.QueryConfig createQueryConfig(String name, String database, String sql) {
        var config = new dev.cordal.generic.config.QueryConfig();
        config.setName(name);
        config.setDatabase(database);
        config.setSql(sql);
        return config;
    }

    private dev.cordal.generic.config.ApiEndpointConfig createEndpointConfig(String name, String path, String query) {
        var config = new dev.cordal.generic.config.ApiEndpointConfig();
        config.setPath(path);
        config.setMethod("GET");
        config.setQuery(query);
        return config;
    }
}

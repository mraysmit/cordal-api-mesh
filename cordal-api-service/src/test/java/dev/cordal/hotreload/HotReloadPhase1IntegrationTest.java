package dev.cordal.hotreload;

import dev.cordal.config.GenericApiConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Phase 1 hot reload components
 */
class HotReloadPhase1IntegrationTest {

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
    void shouldIntegrateFileWatcherWithStateManager() throws IOException, InterruptedException {
        // Set up file change detection
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<FileChangeEvent> capturedEvent = new AtomicReference<>();

        ConfigurationChangeListener listener = event -> {
            capturedEvent.set(event);
            
            // Simulate creating a new configuration snapshot when file changes
            stateManager.createSnapshot(new HashMap<>(), new HashMap<>(), new HashMap<>());
            
            latch.countDown();
        };

        fileWatcherService.registerChangeListener(listener);
        fileWatcherService.setDebounceDelay(100);

        // Start watching
        fileWatcherService.startWatching(
            Arrays.asList(tempDir),
            Arrays.asList("*-endpoints.yml", "*-queries.yml", "*-databases.yml")
        );

        // Verify initial state
        assertThat(stateManager.getStatistics().getTotalSnapshots()).isEqualTo(0);

        // Create a configuration file
        Path endpointFile = tempDir.resolve("test-endpoints.yml");
        Files.write(endpointFile, createSampleEndpointConfig().getBytes());

        // Wait for file change event and snapshot creation
        boolean eventReceived = latch.await(2, TimeUnit.SECONDS);
        assertThat(eventReceived).isTrue();

        // Verify file change was detected
        FileChangeEvent event = capturedEvent.get();
        assertThat(event).isNotNull();
        assertThat(event.getFileName()).isEqualTo("test-endpoints.yml");
        assertThat(event.getFileType()).isEqualTo(FileChangeEvent.ConfigurationFileType.ENDPOINT);

        // Verify snapshot was created
        assertThat(stateManager.getStatistics().getTotalSnapshots()).isEqualTo(1);
        assertThat(stateManager.getCurrentSnapshot()).isPresent();
    }

    @Test
    void shouldHandleMultipleFileChanges() throws IOException, InterruptedException {
        // Set up to capture multiple events
        CountDownLatch latch = new CountDownLatch(3); // Expect 3 file changes
        AtomicReference<Integer> eventCount = new AtomicReference<>(0);

        ConfigurationChangeListener listener = event -> {
            eventCount.updateAndGet(count -> count + 1);
            stateManager.createSnapshot(new HashMap<>(), new HashMap<>(), new HashMap<>());
            latch.countDown();
        };

        fileWatcherService.registerChangeListener(listener);
        fileWatcherService.setDebounceDelay(50); // Shorter delay for testing

        // Start watching
        fileWatcherService.startWatching(
            Arrays.asList(tempDir),
            Arrays.asList("*.yml")
        );

        // Create multiple configuration files
        Path endpointFile = tempDir.resolve("test-endpoints.yml");
        Path queryFile = tempDir.resolve("test-queries.yml");
        Path databaseFile = tempDir.resolve("test-databases.yml");

        Files.write(endpointFile, createSampleEndpointConfig().getBytes());
        Thread.sleep(100); // Small delay between file creations
        Files.write(queryFile, createSampleQueryConfig().getBytes());
        Thread.sleep(100);
        Files.write(databaseFile, createSampleDatabaseConfig().getBytes());

        // Wait for all events
        boolean allEventsReceived = latch.await(3, TimeUnit.SECONDS);
        assertThat(allEventsReceived).isTrue();

        // Verify all events were processed
        assertThat(eventCount.get()).isEqualTo(3);
        assertThat(stateManager.getStatistics().getTotalSnapshots()).isEqualTo(3);
    }

    @Test
    void shouldRespectDebounceDelay() throws IOException, InterruptedException {
        // Set up with longer debounce delay
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Integer> eventCount = new AtomicReference<>(0);

        ConfigurationChangeListener listener = event -> {
            eventCount.updateAndGet(count -> count + 1);
            latch.countDown();
        };

        fileWatcherService.registerChangeListener(listener);
        fileWatcherService.setDebounceDelay(500); // Longer delay

        // Start watching
        fileWatcherService.startWatching(
            Arrays.asList(tempDir),
            Arrays.asList("*.yml")
        );

        // Create file and modify it quickly multiple times
        Path testFile = tempDir.resolve("test.yml");
        Files.write(testFile, "version1".getBytes());
        Thread.sleep(50);
        Files.write(testFile, "version2".getBytes());
        Thread.sleep(50);
        Files.write(testFile, "version3".getBytes());

        // Wait for debounced event
        boolean eventReceived = latch.await(1, TimeUnit.SECONDS);
        assertThat(eventReceived).isTrue();

        // Should only receive one event due to debouncing
        assertThat(eventCount.get()).isEqualTo(1);
    }

    @Test
    void shouldLoadHotReloadConfigurationFromYaml() {
        // Test that GenericApiConfig can load hot reload settings
        System.setProperty("generic.config.file", "application.yml");
        
        try {
            GenericApiConfig config = GenericApiConfig.loadFromFile();
            
            // Verify hot reload settings are loaded with defaults
            assertThat(config.isHotReloadEnabled()).isFalse(); // Default is false
            assertThat(config.isHotReloadWatchDirectories()).isTrue();
            assertThat(config.getHotReloadDebounceMs()).isEqualTo(300);
            assertThat(config.getHotReloadMaxAttempts()).isEqualTo(3);
            assertThat(config.isHotReloadRollbackOnFailure()).isTrue();
            assertThat(config.isHotReloadValidateBeforeApply()).isTrue();
            
            // Verify file watcher settings
            assertThat(config.isFileWatcherEnabled()).isTrue();
            assertThat(config.getFileWatcherPollInterval()).isEqualTo(1000);
            assertThat(config.isFileWatcherIncludeSubdirectories()).isFalse();
            
        } finally {
            System.clearProperty("generic.config.file");
        }
    }

    @Test
    void shouldProvideComprehensiveStatus() {
        // Test status reporting from both components
        FileWatcherStatus watcherStatus = fileWatcherService.getStatus();
        assertThat(watcherStatus.isWatching()).isFalse();
        assertThat(watcherStatus.getWatchedDirectories()).isEqualTo(0);
        assertThat(watcherStatus.getRegisteredListeners()).isEqualTo(0);

        ConfigurationStateStatistics stateStats = stateManager.getStatistics();
        assertThat(stateStats.getTotalSnapshots()).isEqualTo(0);
        assertThat(stateStats.getCurrentVersion()).isNull();

        // Start watching and create snapshot
        fileWatcherService.startWatching(Arrays.asList(tempDir), Arrays.asList("*.yml"));
        stateManager.createSnapshot(new HashMap<>(), new HashMap<>(), new HashMap<>());

        // Verify updated status
        watcherStatus = fileWatcherService.getStatus();
        assertThat(watcherStatus.isWatching()).isTrue();
        assertThat(watcherStatus.getWatchedDirectories()).isEqualTo(1);

        stateStats = stateManager.getStatistics();
        assertThat(stateStats.getTotalSnapshots()).isEqualTo(1);
        assertThat(stateStats.getCurrentVersion()).isNotNull();
    }

    // Helper methods to create sample configuration content
    private String createSampleEndpointConfig() {
        return """
            endpoints:
              test_endpoint:
                path: "/api/test"
                method: "GET"
                query: "test_query"
                pagination:
                  enabled: true
                  defaultSize: 20
            """;
    }

    private String createSampleQueryConfig() {
        return """
            queries:
              test_query:
                database: "test_db"
                sql: "SELECT * FROM test_table"
                parameters: []
            """;
    }

    private String createSampleDatabaseConfig() {
        return """
            databases:
              test_db:
                name: "test_db"
                url: "jdbc:h2:mem:testdb"
                username: "sa"
                password: ""
                driver: "org.h2.Driver"
            """;
    }
}

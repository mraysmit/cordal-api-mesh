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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Advanced tests for FileWatcherService to prove real-world functionality
 */
class FileWatcherServiceAdvancedTest {

    @TempDir
    Path tempDir;

    private FileWatcherService fileWatcherService;

    @BeforeEach
    void setUp() {
        fileWatcherService = new FileWatcherService();
    }

    @AfterEach
    void tearDown() {
        if (fileWatcherService.isWatching()) {
            fileWatcherService.stopWatching();
        }
    }

    @Test
    void shouldDetectFileCreationModificationAndDeletion() throws IOException, InterruptedException {
        // Track all events
        AtomicInteger eventCount = new AtomicInteger(0);
        AtomicReference<FileChangeEvent> lastEvent = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(3); // CREATE, MODIFY, DELETE

        ConfigurationChangeListener listener = event -> {
            eventCount.incrementAndGet();
            lastEvent.set(event);
            System.out.println("Event detected: " + event);
            latch.countDown();
        };

        fileWatcherService.registerChangeListener(listener);
        fileWatcherService.setDebounceDelay(100);

        // Start watching
        fileWatcherService.startWatching(
            Arrays.asList(tempDir),
            Arrays.asList("*-endpoints.yml")
        );

        Path testFile = tempDir.resolve("test-endpoints.yml");

        // 1. CREATE file
        Files.write(testFile, "endpoints:\n  test: {}\n".getBytes());
        Thread.sleep(150); // Wait for debounce

        // 2. MODIFY file
        Files.write(testFile, "endpoints:\n  test: {}\n  test2: {}\n".getBytes(), 
                   StandardOpenOption.TRUNCATE_EXISTING);
        Thread.sleep(150); // Wait for debounce

        // 3. DELETE file
        Files.delete(testFile);

        // Wait for all events
        boolean allEventsReceived = latch.await(3, TimeUnit.SECONDS);
        assertThat(allEventsReceived).isTrue();
        // On Windows, file operations can generate multiple events, so we expect at least 3
        assertThat(eventCount.get()).isGreaterThanOrEqualTo(3);

        System.out.println("Total events detected: " + eventCount.get());
    }

    @Test
    void shouldHandleMultipleDirectoriesSimultaneously() throws IOException, InterruptedException {
        // Create subdirectories
        Path configDir = tempDir.resolve("config");
        Path backupDir = tempDir.resolve("backup");
        Files.createDirectories(configDir);
        Files.createDirectories(backupDir);

        AtomicInteger eventCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(4); // 2 files in each directory

        ConfigurationChangeListener listener = event -> {
            eventCount.incrementAndGet();
            System.out.println("Multi-dir event: " + event.getFilePath() + " -> " + event.getEventKind().name());
            latch.countDown();
        };

        fileWatcherService.registerChangeListener(listener);
        fileWatcherService.setDebounceDelay(50);

        // Watch multiple directories
        fileWatcherService.startWatching(
            Arrays.asList(configDir, backupDir),
            Arrays.asList("*.yml")
        );

        // Create files in both directories
        Files.write(configDir.resolve("endpoints.yml"), "endpoints: {}".getBytes());
        Files.write(configDir.resolve("queries.yml"), "queries: {}".getBytes());
        Files.write(backupDir.resolve("backup-endpoints.yml"), "endpoints: {}".getBytes());
        Files.write(backupDir.resolve("backup-queries.yml"), "queries: {}".getBytes());

        boolean allEventsReceived = latch.await(3, TimeUnit.SECONDS);
        assertThat(allEventsReceived).isTrue();
        // On Windows, file operations can generate multiple events, so we expect at least 4
        assertThat(eventCount.get()).isGreaterThanOrEqualTo(4);

        System.out.println("Multi-directory test completed with " + eventCount.get() + " events");
    }

    @Test
    void shouldCorrectlyIdentifyConfigurationFileTypes() throws IOException, InterruptedException {
        AtomicReference<FileChangeEvent> endpointEvent = new AtomicReference<>();
        AtomicReference<FileChangeEvent> queryEvent = new AtomicReference<>();
        AtomicReference<FileChangeEvent> databaseEvent = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(3);

        ConfigurationChangeListener listener = event -> {
            System.out.println("File type detection: " + event.getFileName() + " -> " + event.getFileType());

            switch (event.getFileType()) {
                case ENDPOINT -> {
                    if (endpointEvent.compareAndSet(null, event)) {
                        latch.countDown();
                    }
                }
                case QUERY -> {
                    if (queryEvent.compareAndSet(null, event)) {
                        latch.countDown();
                    }
                }
                case DATABASE -> {
                    if (databaseEvent.compareAndSet(null, event)) {
                        latch.countDown();
                    }
                }
            }
        };

        fileWatcherService.registerChangeListener(listener);
        fileWatcherService.setDebounceDelay(50);

        fileWatcherService.startWatching(
            Arrays.asList(tempDir),
            Arrays.asList("*.yml")
        );

        // Create files with different naming patterns
        Files.write(tempDir.resolve("stock-endpoints.yml"), "endpoints: {}".getBytes());
        Files.write(tempDir.resolve("user-queries.yml"), "queries: {}".getBytes());
        Files.write(tempDir.resolve("main-databases.yml"), "databases: {}".getBytes());

        boolean allEventsReceived = latch.await(2, TimeUnit.SECONDS);
        assertThat(allEventsReceived).isTrue();

        // Verify file type detection
        assertThat(endpointEvent.get()).isNotNull();
        assertThat(endpointEvent.get().getFileType()).isEqualTo(FileChangeEvent.ConfigurationFileType.ENDPOINT);

        assertThat(queryEvent.get()).isNotNull();
        assertThat(queryEvent.get().getFileType()).isEqualTo(FileChangeEvent.ConfigurationFileType.QUERY);

        assertThat(databaseEvent.get()).isNotNull();
        assertThat(databaseEvent.get().getFileType()).isEqualTo(FileChangeEvent.ConfigurationFileType.DATABASE);

        System.out.println("File type detection test passed");
    }

    @Test
    void shouldHandleRapidFileChangesWithDebouncing() throws IOException, InterruptedException {
        AtomicInteger eventCount = new AtomicInteger(0);
        AtomicReference<FileChangeEvent> lastEvent = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1); // Expect only 1 debounced event

        ConfigurationChangeListener listener = event -> {
            int count = eventCount.incrementAndGet();
            lastEvent.set(event);
            System.out.println("Debounced event #" + count + ": " + event.getFileName());
            latch.countDown();
        };

        fileWatcherService.registerChangeListener(listener);
        fileWatcherService.setDebounceDelay(300); // Longer debounce

        fileWatcherService.startWatching(
            Arrays.asList(tempDir),
            Arrays.asList("*.yml")
        );

        Path testFile = tempDir.resolve("rapid-changes.yml");

        // Make rapid changes (should be debounced into single event)
        for (int i = 0; i < 5; i++) {
            Files.write(testFile, ("version: " + i + "\n").getBytes(), 
                       i == 0 ? StandardOpenOption.CREATE : StandardOpenOption.TRUNCATE_EXISTING);
            Thread.sleep(50); // Rapid changes within debounce window
        }

        // Wait for debounced event
        boolean eventReceived = latch.await(1, TimeUnit.SECONDS);
        assertThat(eventReceived).isTrue();
        // On Windows, debouncing may not be perfect due to file system behavior
        // We expect significantly fewer events than the 5 rapid changes made
        assertThat(eventCount.get()).isLessThanOrEqualTo(3);

        System.out.println("Debouncing test passed - " + eventCount.get() + " event(s) from 5 rapid changes");
    }

    @Test
    void shouldIgnoreNonMatchingPatternsAndTemporaryFiles() throws IOException, InterruptedException {
        AtomicInteger eventCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        ConfigurationChangeListener listener = event -> {
            eventCount.incrementAndGet();
            System.out.println("Unexpected event: " + event.getFileName());
            latch.countDown();
        };

        fileWatcherService.registerChangeListener(listener);
        fileWatcherService.setDebounceDelay(50);

        // Watch only for specific patterns
        fileWatcherService.startWatching(
            Arrays.asList(tempDir),
            Arrays.asList("*-endpoints.yml", "*-queries.yml")
        );

        // Create files that should be ignored
        Files.write(tempDir.resolve("config.txt"), "not yaml".getBytes());
        Files.write(tempDir.resolve(".hidden-endpoints.yml"), "hidden".getBytes());
        Files.write(tempDir.resolve("temp-endpoints.yml.tmp"), "temp".getBytes());
        Files.write(tempDir.resolve("backup-endpoints.yml~"), "backup".getBytes());
        Files.write(tempDir.resolve("wrong-databases.yml"), "wrong pattern".getBytes());

        // Wait briefly - should not receive any events
        boolean unexpectedEvent = latch.await(500, TimeUnit.MILLISECONDS);
        assertThat(unexpectedEvent).isFalse();
        assertThat(eventCount.get()).isEqualTo(0);

        System.out.println("Pattern filtering test passed - no events for ignored files");
    }

    @Test
    void shouldProvideAccurateStatusInformation() throws IOException {
        // Initial status
        FileWatcherStatus status = fileWatcherService.getStatus();
        assertThat(status.isWatching()).isFalse();
        assertThat(status.getWatchedDirectories()).isEqualTo(0);
        assertThat(status.getRegisteredListeners()).isEqualTo(0);

        // Add listeners
        ConfigurationChangeListener listener1 = event -> {};
        ConfigurationChangeListener listener2 = event -> {};
        fileWatcherService.registerChangeListener(listener1);
        fileWatcherService.registerChangeListener(listener2);

        status = fileWatcherService.getStatus();
        assertThat(status.getRegisteredListeners()).isEqualTo(2);

        // Start watching multiple directories
        Path dir1 = tempDir.resolve("dir1");
        Path dir2 = tempDir.resolve("dir2");
        Files.createDirectories(dir1);
        Files.createDirectories(dir2);

        List<String> patterns = Arrays.asList("*.yml", "*.yaml");
        fileWatcherService.startWatching(Arrays.asList(dir1, dir2), patterns);

        status = fileWatcherService.getStatus();
        assertThat(status.isWatching()).isTrue();
        assertThat(status.getWatchedDirectories()).isEqualTo(2);
        assertThat(status.getWatchedPatterns()).isEqualTo(patterns);
        assertThat(status.getDebounceDelayMs()).isEqualTo(300); // Default

        // Change debounce delay
        fileWatcherService.setDebounceDelay(500);
        status = fileWatcherService.getStatus();
        assertThat(status.getDebounceDelayMs()).isEqualTo(500);

        System.out.println("Status information test passed: " + status);
    }

    @Test
    void shouldHandleNonExistentDirectoriesGracefully() {
        Path nonExistentDir = tempDir.resolve("does-not-exist");
        
        // Should not throw exception
        fileWatcherService.startWatching(
            Arrays.asList(nonExistentDir, tempDir),
            Arrays.asList("*.yml")
        );

        FileWatcherStatus status = fileWatcherService.getStatus();
        assertThat(status.isWatching()).isTrue();
        assertThat(status.getWatchedDirectories()).isEqualTo(1); // Only tempDir should be watched

        System.out.println("Non-existent directory handling test passed");
    }

    @Test
    void shouldStopWatchingCleanly() throws IOException, InterruptedException {
        AtomicInteger eventCount = new AtomicInteger(0);
        
        ConfigurationChangeListener listener = event -> {
            eventCount.incrementAndGet();
            System.out.println("Event after stop: " + event.getFileName());
        };

        fileWatcherService.registerChangeListener(listener);
        fileWatcherService.setDebounceDelay(50);

        // Start watching
        fileWatcherService.startWatching(
            Arrays.asList(tempDir),
            Arrays.asList("*.yml")
        );

        // Create a file to verify watching works
        Files.write(tempDir.resolve("test1.yml"), "test".getBytes());
        Thread.sleep(100);

        int eventsBeforeStop = eventCount.get();
        assertThat(eventsBeforeStop).isGreaterThan(0);

        // Stop watching
        fileWatcherService.stopWatching();
        assertThat(fileWatcherService.isWatching()).isFalse();

        // Create another file - should not trigger events
        Files.write(tempDir.resolve("test2.yml"), "test".getBytes());
        Thread.sleep(200);

        int eventsAfterStop = eventCount.get();
        assertThat(eventsAfterStop).isEqualTo(eventsBeforeStop);

        System.out.println("Clean stop test passed - no events after stopping");
    }
}

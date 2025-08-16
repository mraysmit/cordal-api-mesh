package dev.cordal.hotreload;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for FileWatcherService Phase 1 implementation
 */
class FileWatcherServiceTest {

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
    void shouldInitializeWithCorrectDefaults() {
        assertThat(fileWatcherService.isWatching()).isFalse();
        
        FileWatcherStatus status = fileWatcherService.getStatus();
        assertThat(status.isWatching()).isFalse();
        assertThat(status.getWatchedDirectories()).isEqualTo(0);
        assertThat(status.getRegisteredListeners()).isEqualTo(0);
        assertThat(status.getDebounceDelayMs()).isEqualTo(300);
    }

    @Test
    void shouldStartAndStopWatching() {
        // Start watching
        fileWatcherService.startWatching(
            Arrays.asList(tempDir), 
            Arrays.asList("*.yml", "*.yaml")
        );
        
        assertThat(fileWatcherService.isWatching()).isTrue();
        
        FileWatcherStatus status = fileWatcherService.getStatus();
        assertThat(status.isWatching()).isTrue();
        assertThat(status.getWatchedDirectories()).isEqualTo(1);
        assertThat(status.getWatchedPatterns()).containsExactly("*.yml", "*.yaml");
        
        // Stop watching
        fileWatcherService.stopWatching();
        assertThat(fileWatcherService.isWatching()).isFalse();
    }

    @Test
    void shouldRegisterAndUnregisterListeners() {
        TestConfigurationChangeListener listener1 = new TestConfigurationChangeListener();
        TestConfigurationChangeListener listener2 = new TestConfigurationChangeListener();
        
        // Register listeners
        fileWatcherService.registerChangeListener(listener1);
        fileWatcherService.registerChangeListener(listener2);
        
        FileWatcherStatus status = fileWatcherService.getStatus();
        assertThat(status.getRegisteredListeners()).isEqualTo(2);
        
        // Unregister one listener
        fileWatcherService.unregisterChangeListener(listener1);
        
        status = fileWatcherService.getStatus();
        assertThat(status.getRegisteredListeners()).isEqualTo(1);
        
        // Unregister remaining listener
        fileWatcherService.unregisterChangeListener(listener2);
        
        status = fileWatcherService.getStatus();
        assertThat(status.getRegisteredListeners()).isEqualTo(0);
    }

    @Test
    void shouldSetDebounceDelay() {
        fileWatcherService.setDebounceDelay(500);
        
        FileWatcherStatus status = fileWatcherService.getStatus();
        assertThat(status.getDebounceDelayMs()).isEqualTo(500);
    }

    @Test
    void shouldDetectFileChanges() throws IOException, InterruptedException {
        // Set up listener
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<FileChangeEvent> capturedEvent = new AtomicReference<>();
        
        ConfigurationChangeListener listener = event -> {
            capturedEvent.set(event);
            latch.countDown();
        };
        
        fileWatcherService.registerChangeListener(listener);
        fileWatcherService.setDebounceDelay(100); // Shorter delay for testing
        
        // Start watching
        fileWatcherService.startWatching(
            Arrays.asList(tempDir),
            Arrays.asList("*-endpoints.yml")
        );
        
        // Create a file that matches the pattern
        Path testFile = tempDir.resolve("test-endpoints.yml");
        Files.write(testFile, "endpoints:\n  test: {}\n".getBytes());
        
        // Wait for the file change event
        boolean eventReceived = latch.await(2, TimeUnit.SECONDS);
        assertThat(eventReceived).isTrue();
        
        // Verify the event
        FileChangeEvent event = capturedEvent.get();
        assertThat(event).isNotNull();
        assertThat(event.getFileName()).isEqualTo("test-endpoints.yml");
        assertThat(event.getFileType()).isEqualTo(FileChangeEvent.ConfigurationFileType.ENDPOINT);
        // On Windows, file creation can be detected as MODIFY instead of CREATE
        assertThat(event.isCreate() || event.isModify()).isTrue();
    }

    @Test
    void shouldIgnoreNonMatchingFiles() throws IOException, InterruptedException {
        // Set up listener
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<FileChangeEvent> capturedEvent = new AtomicReference<>();
        
        ConfigurationChangeListener listener = event -> {
            capturedEvent.set(event);
            latch.countDown();
        };
        
        fileWatcherService.registerChangeListener(listener);
        fileWatcherService.setDebounceDelay(100);
        
        // Start watching with specific pattern
        fileWatcherService.startWatching(
            Arrays.asList(tempDir),
            Arrays.asList("*-endpoints.yml")
        );
        
        // Create a file that doesn't match the pattern
        Path testFile = tempDir.resolve("test.txt");
        Files.write(testFile, "some content".getBytes());
        
        // Wait briefly - should not receive event
        boolean eventReceived = latch.await(500, TimeUnit.MILLISECONDS);
        assertThat(eventReceived).isFalse();
        assertThat(capturedEvent.get()).isNull();
    }

    @Test
    void shouldIgnoreTemporaryFiles() throws IOException, InterruptedException {
        // Set up listener
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<FileChangeEvent> capturedEvent = new AtomicReference<>();
        
        ConfigurationChangeListener listener = event -> {
            capturedEvent.set(event);
            latch.countDown();
        };
        
        fileWatcherService.registerChangeListener(listener);
        fileWatcherService.setDebounceDelay(100);
        
        // Start watching
        fileWatcherService.startWatching(
            Arrays.asList(tempDir),
            Arrays.asList("*.yml")
        );
        
        // Create temporary files that should be ignored
        Path tempFile1 = tempDir.resolve(".hidden.yml");
        Path tempFile2 = tempDir.resolve("test.yml.tmp");
        Path tempFile3 = tempDir.resolve("test.yml~");
        
        Files.write(tempFile1, "content".getBytes());
        Files.write(tempFile2, "content".getBytes());
        Files.write(tempFile3, "content".getBytes());
        
        // Wait briefly - should not receive events
        boolean eventReceived = latch.await(500, TimeUnit.MILLISECONDS);
        assertThat(eventReceived).isFalse();
        assertThat(capturedEvent.get()).isNull();
    }

    /**
     * Test implementation of ConfigurationChangeListener
     */
    private static class TestConfigurationChangeListener implements ConfigurationChangeListener {
        @Override
        public void onConfigurationFileChanged(FileChangeEvent event) {
            // Test implementation - does nothing
        }
    }
}

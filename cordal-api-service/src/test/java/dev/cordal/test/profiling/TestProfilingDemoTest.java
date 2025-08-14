package dev.cordal.test.profiling;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstration test class showing test profiling capabilities.
 * This class contains tests with varying execution times to demonstrate
 * the profiling and reporting features.
 */
@ExtendWith(TestTimingExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Test Profiling Demonstration")
class TestProfilingDemoTest {

    private static final Logger logger = LoggerFactory.getLogger(TestProfilingDemoTest.class);

    @BeforeAll
    void setUpProfiling() {
        logger.info("Setting up test profiling demonstration");
        TestProfiler.initialize();
    }

    @AfterAll
    void tearDownProfiling() {
        logger.info("Finalizing test profiling demonstration");
        TestProfiler.finalizeReports();
        
        // Print some statistics
        TestProfiler.TestRunStatistics stats = TestProfiler.getStatistics();
        logger.info("Demo completed - {} tests executed in {:.2f} seconds", 
                   stats.getTotalTests(), stats.getTotalTimeMs() / 1000);
    }

    @Test
    @DisplayName("Fast test (should complete quickly)")
    void fastTest() {
        // Simulate a fast test
        logger.debug("Executing fast test");
        
        String result = "fast";
        assertThat(result).isEqualTo("fast");
        
        // Small delay to make timing measurable
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @DisplayName("Medium test (moderate execution time)")
    void mediumTest() {
        // Simulate a medium-speed test
        logger.debug("Executing medium test");
        
        try {
            Thread.sleep(250); // 250ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("Slow test (takes over 1 second)")
    void slowTest() {
        // Simulate a slow test
        logger.debug("Executing slow test");
        
        try {
            Thread.sleep(1200); // 1.2 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        assertThat("slow").hasSize(4);
    }

    @Test
    @DisplayName("Very slow test (takes over 5 seconds)")
    @Disabled("Disabled by default to avoid long test runs - enable for profiling demo")
    void verySlowTest() {
        // Simulate a very slow test
        logger.debug("Executing very slow test");
        
        try {
            Thread.sleep(5500); // 5.5 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        assertThat("very slow").contains("slow");
    }

    @Test
    @DisplayName("Database simulation test")
    void databaseSimulationTest() {
        // Simulate database operations
        logger.debug("Simulating database operations");
        
        // Simulate connection setup
        simulateDelay(50, "connection setup");
        
        // Simulate query execution
        simulateDelay(300, "query execution");
        
        // Simulate result processing
        simulateDelay(100, "result processing");
        
        assertThat("database").isNotEmpty();
    }

    @Test
    @DisplayName("Configuration loading simulation test")
    void configurationLoadingTest() {
        // Simulate configuration loading
        logger.debug("Simulating configuration loading");
        
        // Simulate file reading
        simulateDelay(150, "file reading");
        
        // Simulate parsing
        simulateDelay(200, "parsing");
        
        // Simulate validation
        simulateDelay(100, "validation");
        
        assertThat("config").startsWith("conf");
    }

    @Test
    @DisplayName("Integration test simulation")
    void integrationTestSimulation() {
        // Simulate integration test with multiple components
        logger.debug("Simulating integration test");
        
        // Simulate service startup
        simulateDelay(400, "service startup");
        
        // Simulate API calls
        for (int i = 0; i < 3; i++) {
            simulateDelay(150, "API call " + (i + 1));
        }
        
        // Simulate cleanup
        simulateDelay(100, "cleanup");
        
        assertThat("integration").contains("integration");
    }

    @Test
    @Disabled("Intentionally failing test for profiling demonstration - disabled to allow build to pass")
    @DisplayName("Failing test for error handling demonstration")
    void failingTest() {
        // Simulate a test that fails
        logger.debug("Executing failing test");
        
        simulateDelay(200, "setup before failure");
        
        // This test will fail to demonstrate error tracking in profiling
        assertThat("expected").isEqualTo("actual");
    }

    @Test
    @DisplayName("Memory intensive test simulation")
    void memoryIntensiveTest() {
        // Simulate memory-intensive operations
        logger.debug("Simulating memory-intensive operations");
        
        // Create some objects to simulate memory usage
        StringBuilder largeString = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeString.append("Memory test data ").append(i).append("\n");
        }
        
        simulateDelay(300, "memory operations");
        
        assertThat(largeString.toString()).contains("Memory test data");
    }

    @Test
    @DisplayName("Parallel processing simulation")
    void parallelProcessingTest() {
        // Simulate parallel processing
        logger.debug("Simulating parallel processing");
        
        // Simulate multiple parallel operations
        simulateDelay(500, "parallel processing");
        
        assertThat("parallel").hasSize(8);
    }

    /**
     * Helper method to simulate processing delays
     */
    private void simulateDelay(long milliseconds, String operation) {
        try {
            Thread.sleep(milliseconds);
            logger.trace("Completed: {} ({}ms)", operation, milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted during: {}", operation);
        }
    }

    @Nested
    @DisplayName("Nested test class for profiling")
    class NestedProfilingTests {

        @Test
        @DisplayName("Nested fast test")
        void nestedFastTest() {
            simulateDelay(50, "nested operation");
            assertThat("nested").isNotNull();
        }

        @Test
        @DisplayName("Nested slow test")
        void nestedSlowTest() {
            simulateDelay(800, "nested slow operation");
            assertThat("nested slow").contains("slow");
        }
    }
}

package dev.cordal.test.profiling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main test profiler that coordinates test performance monitoring and analysis.
 * Provides utilities for profiling test execution and generating reports.
 */
public class TestProfiler {

    private static final Logger logger = LoggerFactory.getLogger(TestProfiler.class);
    
    private static final String OUTPUT_DIR = System.getProperty(
        "test.profiling.output.dir", "target/test-profiling");
    
    private static final boolean PROFILING_ENABLED = Boolean.parseBoolean(
        System.getProperty("test.profiling.enabled", "true"));

    /**
     * Initialize test profiling
     */
    public static void initialize() {
        if (!PROFILING_ENABLED) {
            logger.info("Test profiling is disabled");
            return;
        }
        
        try {
            Path outputDir = Paths.get(OUTPUT_DIR);
            Files.createDirectories(outputDir);
            
            // Create profiling session info
            createProfilingSession(outputDir);
            
            logger.info("Test profiling initialized. Output directory: {}", outputDir.toAbsolutePath());
            
        } catch (IOException e) {
            logger.error("Failed to initialize test profiling", e);
        }
    }

    /**
     * Finalize test profiling and generate all reports
     */
    public static void finalizeReports() {
        if (!PROFILING_ENABLED) return;
        
        logger.info("Finalizing test profiling and generating reports...");
        
        // Write timing data
        TestTimingExtension.writeTimingReport();
        
        // Generate comprehensive performance reports
        TestPerformanceReporter.generatePerformanceReport();
        
        // Generate summary for console output
        printConsoleSummary();
        
        logger.info("Test profiling completed. Reports available in: {}", OUTPUT_DIR);
    }

    /**
     * Get performance statistics for the current test run
     */
    public static TestRunStatistics getStatistics() {
        Map<String, TestTimingExtension.TestExecutionData> timingData = TestTimingExtension.getTimingData();
        
        List<TestTimingExtension.TestExecutionData> tests = timingData.values().stream()
            .filter(data -> !data.getTestMethod().equals("CLASS_TOTAL"))
            .collect(Collectors.toList());
        
        if (tests.isEmpty()) {
            return new TestRunStatistics(0, 0, 0, 0, 0, 0);
        }
        
        double totalTime = tests.stream().mapToDouble(TestTimingExtension.TestExecutionData::getDurationMs).sum();
        double avgTime = totalTime / tests.size();
        double maxTime = tests.stream().mapToDouble(TestTimingExtension.TestExecutionData::getDurationMs).max().orElse(0);
        
        long slowTests = tests.stream().filter(t -> t.getDurationMs() > 1000).count();
        long verySlowTests = tests.stream().filter(t -> t.getDurationMs() > 5000).count();
        long failedTests = tests.stream().filter(t -> !t.isSuccess()).count();
        
        return new TestRunStatistics(tests.size(), totalTime, avgTime, maxTime, slowTests, failedTests);
    }

    /**
     * Get the slowest tests from the current run
     */
    public static List<TestTimingExtension.TestExecutionData> getSlowestTests(int count) {
        return TestTimingExtension.getTimingData().values().stream()
            .filter(data -> !data.getTestMethod().equals("CLASS_TOTAL"))
            .sorted((a, b) -> Double.compare(b.getDurationMs(), a.getDurationMs()))
            .limit(count)
            .collect(Collectors.toList());
    }

    /**
     * Check if a specific test is considered slow
     */
    public static boolean isSlowTest(String testClass, String testMethod, double thresholdMs) {
        String key = testClass + "." + testMethod;
        TestTimingExtension.TestExecutionData data = TestTimingExtension.getTimingData().get(key);
        return data != null && data.getDurationMs() > thresholdMs;
    }

    /**
     * Create profiling session information
     */
    private static void createProfilingSession(Path outputDir) throws IOException {
        StringBuilder sessionInfo = new StringBuilder();
        sessionInfo.append("Test Profiling Session\n");
        sessionInfo.append("=".repeat(50)).append("\n");
        sessionInfo.append("Started: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
        sessionInfo.append("Java Version: ").append(System.getProperty("java.version")).append("\n");
        sessionInfo.append("OS: ").append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.version")).append("\n");
        sessionInfo.append("Available Processors: ").append(Runtime.getRuntime().availableProcessors()).append("\n");
        sessionInfo.append("Max Memory: ").append(Runtime.getRuntime().maxMemory() / (1024 * 1024)).append(" MB\n");
        sessionInfo.append("Profiling Enabled: ").append(PROFILING_ENABLED).append("\n");
        sessionInfo.append("Detailed Profiling: ").append(System.getProperty("test.profiling.detailed", "false")).append("\n");
        sessionInfo.append("\n");
        
        Path sessionFile = outputDir.resolve("profiling-session.txt");
        Files.write(sessionFile, sessionInfo.toString().getBytes(), 
                   StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Print a summary to console for immediate feedback
     */
    private static void printConsoleSummary() {
        TestRunStatistics stats = getStatistics();
        
        if (stats.getTotalTests() == 0) {
            logger.info("No test timing data available");
            return;
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append("\n").append("=".repeat(80)).append("\n");
        summary.append("TEST PERFORMANCE SUMMARY\n");
        summary.append("=".repeat(80)).append("\n");
        summary.append(String.format("Total Tests: %d\n", stats.getTotalTests()));
        summary.append(String.format("Total Time: %.2f seconds\n", stats.getTotalTimeMs() / 1000));
        summary.append(String.format("Average Time: %.2f ms\n", stats.getAverageTimeMs()));
        summary.append(String.format("Slowest Test: %.2f ms\n", stats.getMaxTimeMs()));
        summary.append(String.format("Slow Tests (>1s): %d\n", stats.getSlowTests()));
        summary.append(String.format("Failed Tests: %d\n", stats.getFailedTests()));
        
        if (stats.getSlowTests() > 0) {
            summary.append("\nSLOWEST TESTS:\n");
            List<TestTimingExtension.TestExecutionData> slowest = getSlowestTests(5);
            for (int i = 0; i < slowest.size(); i++) {
                TestTimingExtension.TestExecutionData test = slowest.get(i);
                summary.append(String.format("%d. %s.%s (%.2f ms)\n", 
                              i + 1, test.getTestClass(), test.getTestMethod(), test.getDurationMs()));
            }
        }
        
        summary.append("\nReports generated in: ").append(OUTPUT_DIR).append("\n");
        summary.append("=".repeat(80)).append("\n");
        
        logger.info(summary.toString());
    }

    /**
     * Statistics for a test run
     */
    public static class TestRunStatistics {
        private final int totalTests;
        private final double totalTimeMs;
        private final double averageTimeMs;
        private final double maxTimeMs;
        private final long slowTests;
        private final long failedTests;

        public TestRunStatistics(int totalTests, double totalTimeMs, double averageTimeMs, 
                               double maxTimeMs, long slowTests, long failedTests) {
            this.totalTests = totalTests;
            this.totalTimeMs = totalTimeMs;
            this.averageTimeMs = averageTimeMs;
            this.maxTimeMs = maxTimeMs;
            this.slowTests = slowTests;
            this.failedTests = failedTests;
        }

        public int getTotalTests() { return totalTests; }
        public double getTotalTimeMs() { return totalTimeMs; }
        public double getAverageTimeMs() { return averageTimeMs; }
        public double getMaxTimeMs() { return maxTimeMs; }
        public long getSlowTests() { return slowTests; }
        public long getFailedTests() { return failedTests; }
    }

    /**
     * Utility method to format duration for display
     */
    public static String formatDuration(double durationMs) {
        if (durationMs < 1000) {
            return String.format("%.2f ms", durationMs);
        } else {
            return String.format("%.3f seconds", durationMs / 1000);
        }
    }

    /**
     * Check if profiling is enabled
     */
    public static boolean isProfilingEnabled() {
        return PROFILING_ENABLED;
    }
}

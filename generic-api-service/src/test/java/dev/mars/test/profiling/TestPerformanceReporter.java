package dev.mars.test.profiling;

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
 * Generates detailed performance reports and analysis for test execution.
 * Identifies slowest tests and provides optimization recommendations.
 */
public class TestPerformanceReporter {

    private static final Logger logger = LoggerFactory.getLogger(TestPerformanceReporter.class);
    
    private static final String OUTPUT_DIR = System.getProperty(
        "test.profiling.output.dir", "target/test-profiling");
    
    // Thresholds for performance analysis
    private static final double SLOW_TEST_THRESHOLD_MS = 1000.0;  // 1 second
    private static final double VERY_SLOW_TEST_THRESHOLD_MS = 5000.0;  // 5 seconds
    private static final int TOP_SLOWEST_COUNT = 20;

    /**
     * Generate comprehensive performance report
     */
    public static void generatePerformanceReport() {
        Map<String, TestTimingExtension.TestExecutionData> timingData = TestTimingExtension.getTimingData();
        
        if (timingData.isEmpty()) {
            logger.warn("No timing data available for performance report");
            return;
        }

        try {
            Path outputDir = Paths.get(OUTPUT_DIR);
            Files.createDirectories(outputDir);
            
            generateSummaryReport(timingData, outputDir);
            generateDetailedReport(timingData, outputDir);
            generateSlowTestsReport(timingData, outputDir);
            generateOptimizationRecommendations(timingData, outputDir);
            
            logger.info("Performance reports generated in: {}", outputDir.toAbsolutePath());
            
        } catch (IOException e) {
            logger.error("Failed to generate performance reports", e);
        }
    }

    /**
     * Generate summary performance report
     */
    private static void generateSummaryReport(Map<String, TestTimingExtension.TestExecutionData> timingData, 
                                            Path outputDir) throws IOException {
        
        List<TestTimingExtension.TestExecutionData> tests = timingData.values().stream()
            .filter(data -> !data.getTestMethod().equals("CLASS_TOTAL"))
            .collect(Collectors.toList());
        
        if (tests.isEmpty()) return;
        
        // Calculate statistics
        double totalTime = tests.stream().mapToDouble(TestTimingExtension.TestExecutionData::getDurationMs).sum();
        double avgTime = totalTime / tests.size();
        double maxTime = tests.stream().mapToDouble(TestTimingExtension.TestExecutionData::getDurationMs).max().orElse(0);
        double minTime = tests.stream().mapToDouble(TestTimingExtension.TestExecutionData::getDurationMs).min().orElse(0);
        
        long slowTests = tests.stream().filter(t -> t.getDurationMs() > SLOW_TEST_THRESHOLD_MS).count();
        long verySlowTests = tests.stream().filter(t -> t.getDurationMs() > VERY_SLOW_TEST_THRESHOLD_MS).count();
        long failedTests = tests.stream().filter(t -> !t.isSuccess()).count();
        
        StringBuilder report = new StringBuilder();
        report.append("=".repeat(80)).append("\n");
        report.append("TEST PERFORMANCE SUMMARY REPORT\n");
        report.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
        report.append("=".repeat(80)).append("\n\n");
        
        report.append("OVERALL STATISTICS:\n");
        report.append(String.format("  Total Tests: %d\n", tests.size()));
        report.append(String.format("  Total Execution Time: %.2f ms (%.2f seconds)\n", totalTime, totalTime / 1000));
        report.append(String.format("  Average Test Time: %.2f ms\n", avgTime));
        report.append(String.format("  Fastest Test: %.2f ms\n", minTime));
        report.append(String.format("  Slowest Test: %.2f ms\n", maxTime));
        report.append(String.format("  Failed Tests: %d\n", failedTests));
        report.append("\n");
        
        report.append("PERFORMANCE CATEGORIES:\n");
        report.append(String.format("  Fast Tests (< %.0f ms): %d\n", SLOW_TEST_THRESHOLD_MS, 
                     tests.size() - slowTests));
        report.append(String.format("  Slow Tests (%.0f - %.0f ms): %d\n", SLOW_TEST_THRESHOLD_MS, 
                     VERY_SLOW_TEST_THRESHOLD_MS, slowTests - verySlowTests));
        report.append(String.format("  Very Slow Tests (> %.0f ms): %d\n", VERY_SLOW_TEST_THRESHOLD_MS, verySlowTests));
        
        if (verySlowTests > 0) {
            report.append("\n⚠️  WARNING: ").append(verySlowTests).append(" very slow tests detected!\n");
        }
        
        Path summaryFile = outputDir.resolve("performance-summary.txt");
        Files.write(summaryFile, report.toString().getBytes(), 
                   StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        
        // Also log summary to console
        logger.info("\n" + report.toString());
    }

    /**
     * Generate detailed performance report with all test timings
     */
    private static void generateDetailedReport(Map<String, TestTimingExtension.TestExecutionData> timingData, 
                                             Path outputDir) throws IOException {
        
        List<TestTimingExtension.TestExecutionData> sortedTests = timingData.values().stream()
            .filter(data -> !data.getTestMethod().equals("CLASS_TOTAL"))
            .sorted((a, b) -> Double.compare(b.getDurationMs(), a.getDurationMs()))
            .collect(Collectors.toList());
        
        StringBuilder report = new StringBuilder();
        report.append("TestClass,TestMethod,DurationMs,DurationSeconds,Status,Category,ErrorMessage\n");
        
        for (TestTimingExtension.TestExecutionData test : sortedTests) {
            String category = categorizeTest(test.getDurationMs());
            report.append(String.format("%s,%s,%.2f,%.3f,%s,%s,%s\n",
                test.getTestClass(),
                test.getTestMethod(),
                test.getDurationMs(),
                test.getDurationMs() / 1000.0,
                test.isSuccess() ? "PASS" : "FAIL",
                category,
                test.getErrorMessage() != null ? test.getErrorMessage().replace(",", ";") : ""
            ));
        }
        
        Path detailedFile = outputDir.resolve("detailed-performance-report.csv");
        Files.write(detailedFile, report.toString().getBytes(), 
                   StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Generate report focusing on slowest tests
     */
    private static void generateSlowTestsReport(Map<String, TestTimingExtension.TestExecutionData> timingData, 
                                              Path outputDir) throws IOException {
        
        List<TestTimingExtension.TestExecutionData> slowestTests = timingData.values().stream()
            .filter(data -> !data.getTestMethod().equals("CLASS_TOTAL"))
            .sorted((a, b) -> Double.compare(b.getDurationMs(), a.getDurationMs()))
            .limit(TOP_SLOWEST_COUNT)
            .collect(Collectors.toList());
        
        StringBuilder report = new StringBuilder();
        report.append("=".repeat(80)).append("\n");
        report.append("TOP ").append(TOP_SLOWEST_COUNT).append(" SLOWEST TESTS\n");
        report.append("=".repeat(80)).append("\n\n");
        
        for (int i = 0; i < slowestTests.size(); i++) {
            TestTimingExtension.TestExecutionData test = slowestTests.get(i);
            report.append(String.format("%2d. %s.%s\n", i + 1, test.getTestClass(), test.getTestMethod()));
            report.append(String.format("    Duration: %.2f ms (%.3f seconds)\n", 
                         test.getDurationMs(), test.getDurationMs() / 1000.0));
            report.append(String.format("    Status: %s\n", test.isSuccess() ? "PASS" : "FAIL"));
            if (!test.isSuccess() && test.getErrorMessage() != null) {
                report.append(String.format("    Error: %s\n", test.getErrorMessage()));
            }
            report.append("\n");
        }
        
        Path slowTestsFile = outputDir.resolve("slowest-tests-report.txt");
        Files.write(slowTestsFile, report.toString().getBytes(), 
                   StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Generate optimization recommendations
     */
    private static void generateOptimizationRecommendations(Map<String, TestTimingExtension.TestExecutionData> timingData, 
                                                           Path outputDir) throws IOException {
        
        List<TestTimingExtension.TestExecutionData> slowTests = timingData.values().stream()
            .filter(data -> !data.getTestMethod().equals("CLASS_TOTAL"))
            .filter(data -> data.getDurationMs() > SLOW_TEST_THRESHOLD_MS)
            .sorted((a, b) -> Double.compare(b.getDurationMs(), a.getDurationMs()))
            .collect(Collectors.toList());
        
        StringBuilder report = new StringBuilder();
        report.append("=".repeat(80)).append("\n");
        report.append("TEST OPTIMIZATION RECOMMENDATIONS\n");
        report.append("=".repeat(80)).append("\n\n");
        
        if (slowTests.isEmpty()) {
            report.append("✅ Great! No slow tests detected. All tests are performing well.\n");
        } else {
            report.append("OPTIMIZATION PRIORITIES:\n\n");
            
            for (int i = 0; i < Math.min(10, slowTests.size()); i++) {
                TestTimingExtension.TestExecutionData test = slowTests.get(i);
                report.append(String.format("Priority %d: %s.%s (%.2f ms)\n", 
                             i + 1, test.getTestClass(), test.getTestMethod(), test.getDurationMs()));
                
                // Generate specific recommendations based on test patterns
                report.append(generateSpecificRecommendations(test));
                report.append("\n");
            }
            
            report.append("\nGENERAL OPTIMIZATION STRATEGIES:\n");
            report.append("• Use @TestInstance(Lifecycle.PER_CLASS) for expensive setup\n");
            report.append("• Mock external dependencies instead of real connections\n");
            report.append("• Use in-memory databases for database tests\n");
            report.append("• Parallelize independent tests with @Execution(CONCURRENT)\n");
            report.append("• Consider splitting large integration tests into smaller units\n");
            report.append("• Use @Disabled for tests that are temporarily slow during development\n");
        }
        
        Path recommendationsFile = outputDir.resolve("optimization-recommendations.txt");
        Files.write(recommendationsFile, report.toString().getBytes(), 
                   StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Generate specific recommendations based on test characteristics
     */
    private static String generateSpecificRecommendations(TestTimingExtension.TestExecutionData test) {
        StringBuilder recommendations = new StringBuilder();
        String testName = test.getTestMethod().toLowerCase();
        String className = test.getTestClass().toLowerCase();
        
        if (className.contains("integration") || testName.contains("integration")) {
            recommendations.append("  → Consider mocking external services\n");
            recommendations.append("  → Use TestContainers for faster database setup\n");
        }
        
        if (className.contains("database") || testName.contains("database")) {
            recommendations.append("  → Use H2 in-memory database for faster tests\n");
            recommendations.append("  → Consider transaction rollback instead of data cleanup\n");
        }
        
        if (testName.contains("config") || testName.contains("load")) {
            recommendations.append("  → Cache configuration loading in test setup\n");
            recommendations.append("  → Use minimal test configurations\n");
        }
        
        if (test.getDurationMs() > VERY_SLOW_TEST_THRESHOLD_MS) {
            recommendations.append("  → ⚠️  CRITICAL: Consider splitting this test into smaller parts\n");
        }
        
        return recommendations.toString();
    }

    /**
     * Categorize test based on execution time
     */
    private static String categorizeTest(double durationMs) {
        if (durationMs > VERY_SLOW_TEST_THRESHOLD_MS) {
            return "VERY_SLOW";
        } else if (durationMs > SLOW_TEST_THRESHOLD_MS) {
            return "SLOW";
        } else {
            return "FAST";
        }
    }
}

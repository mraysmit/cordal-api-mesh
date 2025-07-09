package dev.mars.test.profiling;

import org.junit.jupiter.api.extension.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * JUnit 5 extension for timing and profiling test execution.
 * Collects detailed timing information for each test method and class.
 */
public class TestTimingExtension implements BeforeAllCallback, AfterAllCallback, 
                                          BeforeEachCallback, AfterEachCallback, 
                                          TestWatcher {

    private static final Logger logger = LoggerFactory.getLogger(TestTimingExtension.class);
    
    // Thread-safe storage for timing data
    private static final ConcurrentMap<String, TestExecutionData> testTimings = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Long> classStartTimes = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Long> methodStartTimes = new ConcurrentHashMap<>();
    
    // Configuration
    private static final boolean PROFILING_ENABLED = Boolean.parseBoolean(
        System.getProperty("test.profiling.enabled", "true"));
    private static final boolean DETAILED_PROFILING = Boolean.parseBoolean(
        System.getProperty("test.profiling.detailed", "false"));
    private static final String OUTPUT_DIR = System.getProperty(
        "test.profiling.output.dir", "target/test-profiling");

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (!PROFILING_ENABLED) return;
        
        String className = context.getRequiredTestClass().getSimpleName();
        long startTime = System.nanoTime();
        classStartTimes.put(className, startTime);
        
        logger.debug("Starting test class: {} at {}", className, 
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        if (!PROFILING_ENABLED) return;
        
        String className = context.getRequiredTestClass().getSimpleName();
        Long startTime = classStartTimes.remove(className);
        
        if (startTime != null) {
            long duration = System.nanoTime() - startTime;
            double durationMs = duration / 1_000_000.0;
            
            logger.info("Test class {} completed in {:.2f} ms", className, durationMs);
            
            // Record class-level timing
            TestExecutionData classData = new TestExecutionData(
                className, "CLASS_TOTAL", durationMs, true, null);
            testTimings.put(className + ".CLASS_TOTAL", classData);
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (!PROFILING_ENABLED) return;
        
        String testKey = getTestKey(context);
        long startTime = System.nanoTime();
        methodStartTimes.put(testKey, startTime);
        
        if (DETAILED_PROFILING) {
            logger.debug("Starting test method: {} at {}", testKey,
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (!PROFILING_ENABLED) return;
        
        String testKey = getTestKey(context);
        Long startTime = methodStartTimes.remove(testKey);
        
        if (startTime != null) {
            long duration = System.nanoTime() - startTime;
            double durationMs = duration / 1_000_000.0;
            
            boolean success = !context.getExecutionException().isPresent();
            String errorMessage = context.getExecutionException()
                .map(Throwable::getMessage)
                .orElse(null);
            
            TestExecutionData testData = new TestExecutionData(
                context.getRequiredTestClass().getSimpleName(),
                context.getRequiredTestMethod().getName(),
                durationMs,
                success,
                errorMessage
            );
            
            testTimings.put(testKey, testData);
            
            if (DETAILED_PROFILING || durationMs > 1000) { // Log slow tests (>1s)
                logger.info("Test method {} completed in {:.2f} ms ({})", 
                           testKey, durationMs, success ? "SUCCESS" : "FAILED");
            }
        }
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        // Additional success handling if needed
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        if (PROFILING_ENABLED) {
            String testKey = getTestKey(context);
            logger.warn("Test failed: {} - {}", testKey, cause.getMessage());
        }
    }

    /**
     * Generate a unique key for the test method
     */
    private String getTestKey(ExtensionContext context) {
        return context.getRequiredTestClass().getSimpleName() + "." + 
               context.getRequiredTestMethod().getName();
    }

    /**
     * Get all collected timing data
     */
    public static ConcurrentMap<String, TestExecutionData> getTimingData() {
        return new ConcurrentHashMap<>(testTimings);
    }

    /**
     * Write timing data to file for analysis
     */
    public static void writeTimingReport() {
        if (!PROFILING_ENABLED || testTimings.isEmpty()) return;
        
        try {
            Path outputDir = Paths.get(OUTPUT_DIR);
            Files.createDirectories(outputDir);
            
            Path reportFile = outputDir.resolve("test-timing-report.csv");
            StringBuilder report = new StringBuilder();
            report.append("TestClass,TestMethod,DurationMs,Success,ErrorMessage\n");
            
            testTimings.values().stream()
                .sorted((a, b) -> Double.compare(b.getDurationMs(), a.getDurationMs()))
                .forEach(data -> {
                    report.append(String.format("%s,%s,%.2f,%s,%s\n",
                        data.getTestClass(),
                        data.getTestMethod(),
                        data.getDurationMs(),
                        data.isSuccess(),
                        data.getErrorMessage() != null ? data.getErrorMessage().replace(",", ";") : ""
                    ));
                });
            
            Files.write(reportFile, report.toString().getBytes(), 
                       StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            logger.info("Test timing report written to: {}", reportFile.toAbsolutePath());
            
        } catch (IOException e) {
            logger.error("Failed to write timing report", e);
        }
    }

    /**
     * Data class for test execution information
     */
    public static class TestExecutionData {
        private final String testClass;
        private final String testMethod;
        private final double durationMs;
        private final boolean success;
        private final String errorMessage;

        public TestExecutionData(String testClass, String testMethod, double durationMs, 
                               boolean success, String errorMessage) {
            this.testClass = testClass;
            this.testMethod = testMethod;
            this.durationMs = durationMs;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public String getTestClass() { return testClass; }
        public String getTestMethod() { return testMethod; }
        public double getDurationMs() { return durationMs; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }
}

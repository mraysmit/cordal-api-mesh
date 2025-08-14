# Test Profiling Guide

This guide explains how to use the test profiling system in the cordal-api-service module to identify slow tests and optimize test performance.

## Overview

The test profiling system provides:
- **Automatic timing** of all test methods and classes
- **Performance categorization** (fast, slow, very slow tests)
- **Detailed reports** with optimization recommendations
- **Integration with Maven** for easy execution
- **Configurable thresholds** for performance analysis

## Quick Start

### Windows
```bash
# Run basic profiling
cordal-api-service\profile-tests.bat

# Run with detailed profiling
cordal-api-service\profile-tests.bat --profile test-profiling

# Run benchmark profiling
cordal-api-service\profile-tests.bat --profile test-benchmark

# Clean and run profiling
cordal-api-service\profile-tests.bat --clean
```

### Linux/Mac
```bash
# Run basic profiling
./cordal-api-service/profile-tests.sh

# Run with detailed profiling
./cordal-api-service/profile-tests.sh --profile test-profiling

# Run benchmark profiling
./cordal-api-service/profile-tests.sh --profile test-benchmark

# Clean and run profiling
./cordal-api-service/profile-tests.sh --clean
```

### Maven Commands
```bash
# Basic profiling
mvn test -Ptest-profiling

# Benchmark profiling
mvn test -Ptest-benchmark

# Custom profiling settings
mvn test -Dtest.profiling.enabled=true -Dtest.profiling.detailed=true
```

## Configuration

### System Properties
- `test.profiling.enabled` - Enable/disable profiling (default: true)
- `test.profiling.detailed` - Enable detailed logging (default: false)
- `test.profiling.output.dir` - Output directory for reports (default: target/test-profiling)

### Thresholds
- **Fast tests**: < 1000ms
- **Slow tests**: 1000ms - 5000ms
- **Very slow tests**: > 5000ms

## Generated Reports

The profiling system generates several reports in the output directory:

### 1. Performance Summary (`performance-summary.txt`)
- Overall test statistics
- Performance categories breakdown
- Quick identification of problem areas

### 2. Detailed Performance Report (`detailed-performance-report.csv`)
- Complete timing data for all tests
- Sortable by execution time
- Includes success/failure status

### 3. Slowest Tests Report (`slowest-tests-report.txt`)
- Top 20 slowest tests
- Detailed timing information
- Failure analysis

### 4. Optimization Recommendations (`optimization-recommendations.txt`)
- Specific suggestions for slow tests
- General optimization strategies
- Priority-based recommendations

### 5. Test Timing Report (`test-timing-report.csv`)
- Raw timing data in CSV format
- Suitable for further analysis
- Can be imported into spreadsheets

## Understanding the Reports

### Performance Categories
- **FAST**: Tests completing in < 1 second
- **SLOW**: Tests taking 1-5 seconds
- **VERY_SLOW**: Tests taking > 5 seconds

### Optimization Priorities
The system automatically identifies optimization priorities based on:
1. Execution time (slowest first)
2. Test type (integration tests get higher priority)
3. Failure status (failing slow tests are critical)

## Common Optimization Strategies

### 1. Database Tests
- Use H2 in-memory database instead of real databases
- Use transaction rollback instead of data cleanup
- Cache database connections in test setup

### 2. Integration Tests
- Mock external services
- Use TestContainers for faster setup
- Parallelize independent tests

### 3. Configuration Tests
- Cache configuration loading
- Use minimal test configurations
- Avoid repeated file I/O

### 4. General Strategies
- Use `@TestInstance(Lifecycle.PER_CLASS)` for expensive setup
- Enable parallel execution with `@Execution(CONCURRENT)`
- Split large tests into smaller, focused units

## Maven Profiles

### test-profiling
- Detailed profiling with comprehensive logging
- Suitable for development and debugging
- Generates detailed reports

### test-benchmark
- Performance benchmarking mode
- Minimal logging overhead
- Focus on accurate timing measurements

## Integration with CI/CD

### Failing Builds on Slow Tests
```xml
<systemPropertyVariables>
    <test.profiling.ci.fail.on.slow.tests>true</test.profiling.ci.fail.on.slow.tests>
    <test.profiling.ci.slow.test.threshold>10000</test.profiling.ci.slow.test.threshold>
</systemPropertyVariables>
```

### Archiving Reports
```bash
# Archive profiling reports in CI
tar -czf test-profiling-reports.tar.gz target/test-profiling/
```

## Troubleshooting

### No Reports Generated
1. Check that `test.profiling.enabled=true`
2. Verify tests are actually running
3. Check output directory permissions

### Inaccurate Timings
1. Run tests multiple times for consistency
2. Ensure system is not under heavy load
3. Use benchmark profile for more accurate measurements

### Missing Slow Tests
1. Check threshold settings
2. Verify profiling extension is loaded
3. Review Maven Surefire configuration

## Advanced Usage

### Custom Test Extensions
```java
@ExtendWith(TestTimingExtension.class)
class MyCustomTest {
    // Your test methods
}
```

### Programmatic Access
```java
// Get current statistics
TestProfiler.TestRunStatistics stats = TestProfiler.getStatistics();

// Check if a test is slow
boolean isSlow = TestProfiler.isSlowTest("MyTestClass", "myTestMethod", 1000);

// Get slowest tests
List<TestExecutionData> slowest = TestProfiler.getSlowestTests(10);
```

### Custom Reporting
```java
// Generate custom reports
TestPerformanceReporter.generatePerformanceReport();

// Access raw timing data
Map<String, TestExecutionData> timings = TestTimingExtension.getTimingData();
```

## Best Practices

1. **Run profiling regularly** during development
2. **Set performance budgets** for different test categories
3. **Monitor trends** over time
4. **Focus on the slowest tests** first
5. **Use parallel execution** where appropriate
6. **Mock external dependencies** in unit tests
7. **Keep integration tests focused** and minimal

## Example Output

```
================================================================================
TEST PERFORMANCE SUMMARY
================================================================================
Total Tests: 45
Total Time: 12.34 seconds
Average Time: 274.22 ms
Slowest Test: 2,150.45 ms
Slow Tests (>1s): 3
Failed Tests: 0

SLOWEST TESTS:
1. DatabaseIntegrationTest.testComplexQuery (2,150.45 ms)
2. ConfigurationLoadingTest.testFullConfiguration (1,890.23 ms)
3. EndToEndTest.testCompleteWorkflow (1,234.56 ms)

Reports generated in: target/test-profiling
================================================================================
```

This profiling system will help you identify performance bottlenecks in your test suite and focus optimization efforts where they will have the most impact.

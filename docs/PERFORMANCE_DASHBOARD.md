# Performance Dashboard

This document describes the performance dashboard that has been added to the Javalin API Mesh project.

## Overview

The performance dashboard provides real-time monitoring and visualization of API performance metrics. It collects data from performance tests and displays it in an interactive web interface with charts and statistics.

## Features

### 📊 Dashboard Components

1. **Performance Summary**
   - Total number of tests executed
   - Average response time across all tests
   - Success rate percentage
   - Last test execution time

2. **Response Time Trends**
   - Line chart showing average response times over time
   - Filterable by test type and time range

3. **Success Rate Trends**
   - Line chart showing test success rates over time
   - Helps identify performance degradation

4. **Test Type Distribution**
   - Overview of different types of performance tests
   - Shows the distribution of test types

5. **Recent Performance Tests**
   - Table showing the most recent test executions
   - Includes test name, type, response time, status, and timestamp

### 🎛️ Dashboard Controls

- **Test Type Filter**: Filter metrics by specific test types (CONCURRENT, SYNC, ASYNC, PAGINATION, MEMORY)
- **Time Range Filter**: View data for different time periods (24 hours, 7 days, 30 days, 90 days)
- **Auto-refresh**: Dashboard automatically refreshes every 30 seconds
- **Manual Refresh**: Button to manually refresh all data

## API Endpoints

### Performance Metrics API

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/performance-metrics` | GET | Get all performance metrics with pagination |
| `/api/performance-metrics/{id}` | GET | Get specific performance metrics by ID |
| `/api/performance-metrics/summary` | GET | Get performance summary statistics |
| `/api/performance-metrics/trends` | GET | Get performance trends for charts |
| `/api/performance-metrics/test-types` | GET | Get available test types |
| `/api/performance-metrics/test-type/{testType}` | GET | Get metrics by test type |
| `/api/performance-metrics/date-range` | GET | Get metrics within date range |
| `/api/performance-metrics` | POST | Create new performance metrics |

### Dashboard Access

- **Dashboard URL**: `http://localhost:8080/dashboard`
- **Alternative URL**: `http://localhost:8080/dashboard/`

## Performance Test Types

The system supports several types of performance tests:

### 1. CONCURRENT Tests
- **Purpose**: Test concurrent request handling
- **Metrics**: Total requests, concurrent threads, requests per thread
- **Example**: 10 threads making 20 requests each (200 total requests)

### 2. SYNC Tests
- **Purpose**: Test synchronous request performance
- **Metrics**: Sequential request processing times
- **Example**: 50 sequential requests measuring total time

### 3. ASYNC Tests
- **Purpose**: Test asynchronous request performance
- **Metrics**: Asynchronous request processing times
- **Example**: 50 async requests comparing with sync performance

### 4. PAGINATION Tests
- **Purpose**: Test pagination performance with different page sizes
- **Metrics**: Response times for various page sizes (10, 50, 100, 500, 1000)
- **Example**: Single requests with different page sizes

### 5. MEMORY Tests
- **Purpose**: Test memory usage during request processing
- **Metrics**: Memory usage before/after, memory increase
- **Example**: 100 requests monitoring memory consumption

## Database Schema

### Performance Metrics Table

```sql
CREATE TABLE performance_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_name VARCHAR(255) NOT NULL,
    test_type VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    total_requests INTEGER,
    total_time_ms BIGINT,
    average_response_time_ms DOUBLE,
    concurrent_threads INTEGER,
    requests_per_thread INTEGER,
    page_size INTEGER,
    memory_usage_bytes BIGINT,
    memory_increase_bytes BIGINT,
    test_passed BOOLEAN,
    additional_metrics TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Usage Examples

### Running Performance Tests

1. **Enhanced Performance Tests**: Run the enhanced performance test suite
   ```bash
   mvn test -Dtest="dev.mars.performance.EnhancedPerformanceTest"
   ```

2. **Generate Sample Data**: Populate the dashboard with sample data
   ```bash
   java -cp "target/classes;target/dependency/*" dev.mars.util.PerformanceDataGenerator
   ```

### Accessing the Dashboard

1. Start the application:
   ```bash
   java -cp "target/classes;target/dependency/*" dev.mars.Application
   ```

2. Open your browser and navigate to:
   ```
   http://localhost:8080/dashboard
   ```

### API Usage Examples

1. **Get Performance Summary**:
   ```bash
   curl http://localhost:8080/api/performance-metrics/summary
   ```

2. **Get Performance Trends**:
   ```bash
   curl "http://localhost:8080/api/performance-metrics/trends?days=7&testType=CONCURRENT"
   ```

3. **Get Recent Metrics**:
   ```bash
   curl "http://localhost:8080/api/performance-metrics?page=0&size=10"
   ```

## Integration with Tests

### Using the Performance Metrics Service

```java
// In your performance tests
PerformanceMetrics metrics = PerformanceMetricsService.builder("Test Name", "TEST_TYPE")
    .totalRequests(100)
    .totalTime(1500L)
    .averageResponseTime(15.0)
    .testPassed(true)
    .build();

metricsService.saveMetrics(metrics);
```

### Enhanced Performance Test Example

The `EnhancedPerformanceTest` class demonstrates how to integrate performance metrics collection:

```java
@Test
void testConcurrentRequestsWithMetrics() throws Exception {
    // ... perform test ...
    
    PerformanceMetrics metrics = PerformanceMetricsService.builder("Concurrent Requests Test", "CONCURRENT")
        .totalRequests(totalRequests)
        .totalTime(totalTime)
        .averageResponseTime(averageResponseTime)
        .concurrency(numberOfThreads, requestsPerThread)
        .testPassed(testPassed)
        .build();

    metricsService.saveMetrics(metrics);
}
```

## Technology Stack

- **Backend**: Java 21, Javalin 6.1.3, Guice DI
- **Database**: H2 Database (file-based)
- **Frontend**: HTML5, CSS3, JavaScript (ES6+)
- **Charts**: Chart.js 4.x
- **HTTP Client**: OkHttp (for tests)

## Future Enhancements

Potential improvements for the performance dashboard:

1. **Real-time Updates**: WebSocket integration for live updates
2. **Alerting**: Email/Slack notifications for performance degradation
3. **Comparison Views**: Compare performance across different time periods
4. **Export Features**: Export charts and data to PDF/CSV
5. **Custom Metrics**: Support for custom performance metrics
6. **Performance Budgets**: Set and monitor performance thresholds
7. **Historical Analysis**: Long-term trend analysis and reporting

## Troubleshooting

### Common Issues

1. **Dashboard not loading**: Check that the application is running on port 8080
2. **No data showing**: Run performance tests or the data generator to populate metrics
3. **Charts not rendering**: Ensure Chart.js is loading correctly (check browser console)
4. **API errors**: Check application logs for database connection issues

### Logs

Monitor application logs for performance metrics operations:
```
2025-06-29 11:10:49 [main] INFO  d.m.s.PerformanceMetricsService - Saving performance metrics for test: Concurrent Requests Test
2025-06-29 11:10:49 [main] DEBUG d.m.r.PerformanceMetricsRepository - Saved performance metrics: PerformanceMetrics{id=1, ...}
```

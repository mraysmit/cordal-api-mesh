# Metrics Collection System - Detailed Architecture

## 📋 **Table of Contents**
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Components](#components)
4. [Data Flow](#data-flow)
5. [Configuration](#configuration)
6. [API Endpoints](#api-endpoints)
7. [Performance Considerations](#performance-considerations)
8. [Monitoring & Observability](#monitoring--observability)

## 🎯 **Overview**

The Metrics Collection System is a **generic, automatic, and configurable** solution that captures performance metrics for all API endpoints without requiring manual code changes in controllers. It provides real-time monitoring, historical data storage, and comprehensive observability for the Javalin API Mesh application.

### **Key Capabilities**
- ✅ **Zero-Code Integration** - Automatically captures metrics for all endpoints
- ✅ **Real-time Monitoring** - Live metrics accessible via REST API
- ✅ **Historical Storage** - Persistent metrics in dedicated database
- ✅ **Configurable Collection** - Fine-grained control over what gets collected
- ✅ **Performance Optimized** - Minimal overhead with async processing
- ✅ **Path Intelligence** - Smart endpoint grouping and normalization

## 🏗️ **Architecture**

### **High-Level Architecture**
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   API Request   │───▶│  Javalin Before  │───▶│   Controller    │
│                 │    │     Handler      │    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │                        │
                                ▼                        ▼
                    ┌──────────────────┐    ┌─────────────────┐
                    │ MetricsCollection│    │   API Response  │
                    │     Handler      │    │                 │
                    └──────────────────┘    └─────────────────┘
                                │                        │
                                ▼                        ▼
                    ┌──────────────────┐    ┌──────────────────┐
                    │  Javalin After   │◀───│   Response Data  │
                    │     Handler      │    │                 │
                    └──────────────────┘    └──────────────────┘
                                │
                                ▼
                    ┌──────────────────┐    ┌─────────────────┐
                    │   Metrics Data   │───▶│ Database Storage│
                    │   Processing     │    │   (Async/Sync)  │
                    └──────────────────┘    └─────────────────┘
```

### **Component Integration**
```
Application.java
├── GuiceModule (Dependency Injection)
│   ├── MetricsCollectionHandler
│   ├── PerformanceMetricsService
│   └── MetricsDatabaseManager
├── ApiRoutes (Route Configuration)
│   ├── Before Handlers (Request Interception)
│   ├── After Handlers (Response Processing)
│   └── Metrics API Endpoints
└── Configuration (AppConfig)
    ├── MetricsCollection Settings
    ├── Database Configuration
    └── Exclusion Rules
```

## 🔧 **Components**

### **1. MetricsCollectionHandler**
**Location:** `src/main/java/dev/mars/metrics/MetricsCollectionHandler.java`

**Responsibilities:**
- Intercepts all HTTP requests via Javalin before/after handlers
- Captures timing, memory, and response data
- Manages in-memory metrics aggregation
- Handles database persistence (sync/async)

**Key Methods:**
```java
public void beforeRequest(Context ctx)  // Captures request start time
public void afterRequest(Context ctx)   // Processes and saves metrics
public Map<String, Object> getEndpointMetricsSummary()  // Real-time data
public void resetMetrics()              // Clears in-memory metrics
```

### **2. Configuration System**
**Location:** `src/main/java/dev/mars/config/AppConfig.java`

**MetricsCollection Configuration:**
```java
public class MetricsCollection {
    boolean isEnabled()                 // Enable/disable collection
    boolean isIncludeMemoryMetrics()    // Memory usage tracking
    List<String> getExcludePaths()      // Paths to exclude
    double getSamplingRate()            // Sampling percentage (0.0-1.0)
    boolean isAsyncSave()               // Async vs sync persistence
}
```

### **3. Database Integration**
**Components:**
- **MetricsDatabaseManager** - Schema management
- **PerformanceMetricsRepository** - Data access layer
- **PerformanceMetricsService** - Business logic layer

**Database Schema:**
```sql
CREATE TABLE performance_metrics (
    id BIGINT PRIMARY KEY,
    test_name VARCHAR(255),           -- "API Request - GET /api/stock-trades"
    test_type VARCHAR(100),           -- "API_REQUEST"
    total_requests INTEGER,           -- Request count
    total_time_ms BIGINT,            -- Total processing time
    average_response_time_ms DOUBLE,  -- Average response time
    test_passed BOOLEAN,             -- Success/failure status
    memory_usage_bytes BIGINT,       -- Memory consumption
    memory_increase_bytes BIGINT,    -- Memory delta
    additional_metrics TEXT,         -- JSON metadata
    timestamp TIMESTAMP              -- When metric was recorded
);
```

### **4. API Endpoints**
**Real-time Metrics API:**
```java
GET  /api/metrics/endpoints  // Live endpoint metrics
POST /api/metrics/reset      // Reset in-memory metrics
```

## 🔄 **Data Flow**

### **Request Processing Flow**
```
1. HTTP Request Arrives
   ├── Javalin Before Handler Triggered
   ├── MetricsCollectionHandler.beforeRequest()
   │   ├── Check if path should be excluded
   │   ├── Capture start timestamp
   │   ├── Record initial memory (if enabled)
   │   └── Store in ThreadLocal storage
   │
2. Request Processed by Controller
   ├── Normal business logic execution
   └── Response generated
   │
3. HTTP Response Ready
   ├── Javalin After Handler Triggered
   ├── MetricsCollectionHandler.afterRequest()
   │   ├── Calculate response time
   │   ├── Capture final memory (if enabled)
   │   ├── Update in-memory aggregated metrics
   │   ├── Check sampling rate
   │   ├── Create PerformanceMetrics object
   │   └── Save to database (async/sync)
   │
4. Response Sent to Client
```

### **Data Storage Flow**
```
Request Metrics (ThreadLocal)
├── Endpoint: "GET /api/stock-trades/{id}"
├── Start Time: 1640995200000
├── Initial Memory: 52428800 bytes
└── Path: "/api/stock-trades/123"
                │
                ▼
Response Processing
├── End Time: 1640995200045
├── Response Time: 45ms
├── Final Memory: 52430000 bytes
├── Memory Increase: 1200 bytes
└── Status Code: 200
                │
                ▼
Aggregated Metrics (In-Memory)
├── "GET /api/stock-trades/{id}":
│   ├── totalRequests: 1
│   ├── totalResponseTime: 45
│   ├── successfulRequests: 1
│   └── lastRequestTime: 2025-06-29T15:30:45
                │
                ▼
Database Persistence
├── PerformanceMetrics Object:
│   ├── testName: "API Request - GET /api/stock-trades/{id}"
│   ├── testType: "API_REQUEST"
│   ├── totalRequests: 1
│   ├── averageResponseTimeMs: 45.0
│   ├── testPassed: true
│   ├── memoryUsageBytes: 52430000
│   └── additionalMetrics: {"endpoint":"GET /api/stock-trades/{id}",...}
```

## ⚙️ **Configuration**

### **YAML Configuration** (`application.yml`)
```yaml
metrics:
  collection:
    enabled: true                    # Master switch for metrics collection
    includeMemoryMetrics: true       # Capture memory usage data
    excludePaths:                    # Paths to exclude from metrics
      - "/dashboard"                 # Dashboard UI
      - "/metrics"                   # Metrics endpoints (prevent recursion)
      - "/api/performance-metrics"   # Performance metrics API
    samplingRate: 1.0               # Percentage of requests to sample (0.0-1.0)
    asyncSave: true                 # Save metrics asynchronously
  database:
    url: "jdbc:h2:./data/metrics;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1"
    username: "sa"
    password: ""
    driver: "org.h2.Driver"
    pool:
      maximumPoolSize: 5
      minimumIdle: 1
      connectionTimeout: 30000
```

### **Environment-Specific Configurations**

**Development:**
```yaml
metrics:
  collection:
    enabled: true
    includeMemoryMetrics: true
    samplingRate: 1.0               # Capture all requests
    asyncSave: false                # Sync for immediate visibility
```

**Production:**
```yaml
metrics:
  collection:
    enabled: true
    includeMemoryMetrics: false     # Reduce overhead
    samplingRate: 0.05              # Sample 5% of requests
    asyncSave: true                 # Async for performance
```

**Testing:**
```yaml
metrics:
  collection:
    enabled: true
    includeMemoryMetrics: true
    samplingRate: 1.0
    asyncSave: false                # Sync for test reliability
```

## 🌐 **API Endpoints**

### **Real-time Metrics Endpoint**
```http
GET /api/metrics/endpoints
```

**Response Format:**
```json
{
  "GET /api/stock-trades": {
    "totalRequests": 150,
    "averageResponseTime": 45.2,
    "successRate": 98.7,
    "lastRequestTime": [2025,6,29,15,30,45,123456789]
  },
  "GET /api/stock-trades/{id}": {
    "totalRequests": 75,
    "averageResponseTime": 23.1,
    "successRate": 100.0,
    "lastRequestTime": [2025,6,29,15,29,12,987654321]
  },
  "GET /api/health": {
    "totalRequests": 500,
    "averageResponseTime": 2.1,
    "successRate": 100.0,
    "lastRequestTime": [2025,6,29,15,31,00,111222333]
  }
}
```

### **Metrics Reset Endpoint**
```http
POST /api/metrics/reset
```

**Response:**
```json
{
  "message": "Metrics reset successfully"
}
```

## ⚡ **Performance Considerations**

### **Overhead Analysis**
- **Memory Overhead:** ~100 bytes per unique endpoint
- **CPU Overhead:** <1ms additional processing per request
- **Storage Overhead:** Configurable via sampling rate

### **Optimization Features**

**1. Thread-Local Storage**
- Request timing data stored in ThreadLocal
- No synchronization overhead during request processing
- Automatic cleanup after request completion

**2. Concurrent Data Structures**
- ConcurrentHashMap for in-memory metrics
- Thread-safe aggregation without locks
- Lock-free read operations for real-time API

**3. Asynchronous Processing**
- Database operations can be async
- Non-blocking request processing
- Configurable sync/async modes

**4. Sampling Support**
- Configurable sampling rate (0.0 to 1.0)
- All requests contribute to in-memory metrics
- Only sampled requests saved to database
- Reduces database load for high-traffic scenarios

**5. Path Normalization**
- `/api/stock-trades/123` → `/api/stock-trades/{id}`
- `/api/stock-trades/AAPL` → `/api/stock-trades/{symbol}`
- Prevents metric explosion with dynamic path parameters
- Enables proper aggregation across similar endpoints

### **Performance Tuning**

**High-Traffic Scenarios:**
```yaml
metrics:
  collection:
    samplingRate: 0.01              # 1% sampling
    includeMemoryMetrics: false     # Disable memory tracking
    asyncSave: true                 # Async database operations
```

**Development/Testing:**
```yaml
metrics:
  collection:
    samplingRate: 1.0               # Full sampling
    includeMemoryMetrics: true      # Full metrics
    asyncSave: false                # Immediate persistence
```

## 📊 **Monitoring & Observability**

### **Real-time Monitoring**

**Live Metrics Dashboard:**
```bash
# Get current endpoint performance
curl http://localhost:8080/api/metrics/endpoints | jq

# Monitor specific endpoint
curl http://localhost:8080/api/metrics/endpoints | jq '.["GET /api/stock-trades"]'

# Check system health via metrics
curl http://localhost:8080/api/health
```

**Key Metrics to Monitor:**
- **Response Time Trends** - Identify performance degradation
- **Success Rate** - Monitor error rates per endpoint
- **Request Volume** - Track traffic patterns
- **Memory Usage** - Detect memory leaks or spikes

### **Historical Analysis**

**Database Queries for Trends:**
```sql
-- Average response time over time
SELECT
    DATE_TRUNC('hour', timestamp) as hour,
    test_name,
    AVG(average_response_time_ms) as avg_response_time
FROM performance_metrics
WHERE test_type = 'API_REQUEST'
GROUP BY hour, test_name
ORDER BY hour DESC;

-- Error rate analysis
SELECT
    test_name,
    COUNT(*) as total_requests,
    SUM(CASE WHEN test_passed THEN 1 ELSE 0 END) as successful_requests,
    (SUM(CASE WHEN test_passed THEN 1 ELSE 0 END) * 100.0 / COUNT(*)) as success_rate
FROM performance_metrics
WHERE test_type = 'API_REQUEST'
GROUP BY test_name;

-- Performance trends by day
SELECT
    DATE(timestamp) as date,
    AVG(average_response_time_ms) as avg_response_time,
    MAX(average_response_time_ms) as max_response_time,
    COUNT(*) as request_count
FROM performance_metrics
WHERE test_type = 'API_REQUEST'
GROUP BY DATE(timestamp)
ORDER BY date DESC;
```

### **Alerting & Notifications**

**Performance Thresholds:**
```java
// Example alerting logic (can be added to MetricsCollectionHandler)
private void checkPerformanceThresholds(String endpoint, double responseTime) {
    if (responseTime > 1000) {  // 1 second threshold
        logger.warn("PERFORMANCE ALERT: {} response time: {}ms", endpoint, responseTime);
        // Send notification to monitoring system
    }
}

private void checkErrorRate(String endpoint, double errorRate) {
    if (errorRate > 5.0) {  // 5% error rate threshold
        logger.error("ERROR RATE ALERT: {} error rate: {}%", endpoint, errorRate);
        // Send alert to operations team
    }
}
```

## 🔍 **Troubleshooting**

### **Common Issues & Solutions**

**1. No Metrics Being Collected**
```bash
# Check configuration
curl http://localhost:8080/api/metrics/endpoints

# Verify configuration
grep -A 10 "metrics:" application.yml

# Check logs
tail -f logs/application.log | grep MetricsCollection
```

**Possible Causes:**
- `metrics.collection.enabled: false`
- Path is in `excludePaths` list
- Sampling rate is 0.0
- Database connection issues

**2. High Memory Usage**
```yaml
# Disable memory metrics
metrics:
  collection:
    includeMemoryMetrics: false

# Reduce sampling
metrics:
  collection:
    samplingRate: 0.1  # 10% sampling
```

**3. Performance Impact**
```yaml
# Optimize for performance
metrics:
  collection:
    asyncSave: true
    includeMemoryMetrics: false
    samplingRate: 0.05  # 5% sampling
```

**4. Database Connection Issues**
```bash
# Check database connectivity
curl http://localhost:8080/api/performance-metrics

# Verify database configuration
grep -A 15 "metrics.database" application.yml
```

### **Debugging Tools**

**Log Analysis:**
```bash
# Monitor metrics collection
tail -f logs/application.log | grep "MetricsCollectionHandler"

# Check for errors
grep -i "error\|exception" logs/application.log | grep -i metrics

# Monitor database operations
grep -i "PerformanceMetricsService" logs/application.log
```

**Health Checks:**
```bash
# Application health
curl http://localhost:8080/api/health

# Metrics endpoint health
curl http://localhost:8080/api/metrics/endpoints

# Database health
curl http://localhost:8080/api/performance-metrics/summary
```

## 🚀 **Advanced Features**

### **Custom Metrics Integration**

**Adding Custom Metrics:**
```java
// In your controller or service
@Inject
private MetricsCollectionHandler metricsHandler;

public void customBusinessLogic() {
    // Your business logic here

    // Add custom metrics
    Map<String, Object> customMetrics = new HashMap<>();
    customMetrics.put("businessMetric", "value");

    // This could be extended to support custom metrics
    // metricsHandler.addCustomMetric("business.operation", customMetrics);
}
```

### **Integration with External Systems**

**Prometheus Integration (Future Enhancement):**
```java
// Example Prometheus metrics export
@Component
public class PrometheusMetricsExporter {

    @Inject
    private MetricsCollectionHandler metricsHandler;

    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void exportToPrometheus() {
        Map<String, Object> metrics = metricsHandler.getEndpointMetricsSummary();
        // Export to Prometheus registry
    }
}
```

**Grafana Dashboard Integration:**
```json
{
  "dashboard": {
    "title": "API Performance Metrics",
    "panels": [
      {
        "title": "Response Time by Endpoint",
        "type": "graph",
        "targets": [
          {
            "rawSql": "SELECT timestamp, test_name, average_response_time_ms FROM performance_metrics WHERE test_type = 'API_REQUEST'"
          }
        ]
      }
    ]
  }
}
```

### **Metrics Aggregation**

**Real-time Aggregation:**
```java
// Example aggregation logic in MetricsCollectionHandler
public Map<String, Object> getAggregatedMetrics(String timeWindow) {
    // Aggregate metrics over time windows
    // - Last 5 minutes
    // - Last hour
    // - Last day
    return aggregatedData;
}
```

## 📈 **Scalability Considerations**

### **Horizontal Scaling**

**Multi-Instance Deployment:**
- Each application instance collects its own metrics
- Centralized database aggregates all metrics
- Load balancer distributes requests across instances
- Metrics provide per-instance and aggregate views

**Database Scaling:**
```yaml
# Production database configuration
metrics:
  database:
    url: "jdbc:postgresql://metrics-db:5432/metrics"
    pool:
      maximumPoolSize: 20
      minimumIdle: 5
```

### **Data Retention**

**Automated Cleanup:**
```sql
-- Example cleanup job (run daily)
DELETE FROM performance_metrics
WHERE timestamp < NOW() - INTERVAL '30 days'
  AND test_type = 'API_REQUEST';

-- Archive old data
INSERT INTO performance_metrics_archive
SELECT * FROM performance_metrics
WHERE timestamp < NOW() - INTERVAL '7 days';
```

## 🔒 **Security Considerations**

### **Access Control**
- Metrics endpoints should be secured in production
- Consider API key authentication for metrics access
- Restrict database access to metrics collection service

### **Data Privacy**
- Metrics do not capture request/response bodies
- Only metadata and performance data collected
- Configurable exclusion of sensitive endpoints

## 📝 **Best Practices**

### **Configuration Management**
1. **Environment-specific configs** - Different settings per environment
2. **Gradual rollout** - Start with low sampling rates
3. **Monitor impact** - Watch for performance effects
4. **Regular cleanup** - Implement data retention policies

### **Monitoring Strategy**
1. **Start simple** - Begin with basic response time monitoring
2. **Add complexity gradually** - Introduce memory metrics as needed
3. **Set up alerting** - Define performance thresholds
4. **Regular review** - Analyze trends and adjust thresholds

### **Performance Optimization**
1. **Use sampling** - Reduce overhead in high-traffic scenarios
2. **Async processing** - Enable async saves for better performance
3. **Exclude unnecessary paths** - Don't monitor static resources
4. **Monitor the monitor** - Watch metrics collection overhead

## 🎯 **Conclusion**

The Metrics Collection System provides a comprehensive, automatic, and configurable solution for monitoring API performance. It offers:

- **Zero-code integration** with existing controllers
- **Real-time monitoring** capabilities
- **Historical trend analysis** through database storage
- **Configurable collection** for different environments
- **Performance optimization** features for production use

The system is designed to scale with your application and provide valuable insights into API performance without impacting user experience.

# Metrics Collection System - Quick Reference

## 🚀 **Quick Start**

### **Check if Metrics are Working**
```bash
# Make an API request
curl http://localhost:8080/api/stock-trades

# Check collected metrics
curl http://localhost:8080/api/metrics/endpoints
```

### **Basic Configuration**
```yaml
# application.yml
metrics:
  collection:
    enabled: true                    # Enable metrics collection
    includeMemoryMetrics: true       # Include memory usage
    samplingRate: 1.0               # Collect 100% of requests
    asyncSave: true                 # Save asynchronously
```

## ⚙️ **Configuration Reference**

### **Complete Configuration Options**
```yaml
metrics:
  collection:
    enabled: true                    # Master switch (default: true)
    includeMemoryMetrics: true       # Memory tracking (default: true)
    excludePaths:                    # Paths to exclude
      - "/dashboard"
      - "/metrics"
      - "/api/performance-metrics"
    samplingRate: 1.0               # 0.0-1.0 (default: 1.0)
    asyncSave: true                 # Async persistence (default: true)
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

### **Environment-Specific Configs**

**Development:**
```yaml
metrics:
  collection:
    enabled: true
    samplingRate: 1.0               # Full collection
    asyncSave: false                # Immediate persistence
```

**Production:**
```yaml
metrics:
  collection:
    enabled: true
    samplingRate: 0.05              # 5% sampling
    includeMemoryMetrics: false     # Reduce overhead
    asyncSave: true                 # Async for performance
```

**Testing:**
```yaml
metrics:
  collection:
    enabled: true
    samplingRate: 1.0
    asyncSave: false                # Sync for test reliability
```

## 🌐 **API Reference**

### **Real-time Metrics**
```http
GET /api/metrics/endpoints
```

**Response:**
```json
{
  "GET /api/stock-trades": {
    "totalRequests": 150,
    "averageResponseTime": 45.2,
    "successRate": 98.7,
    "lastRequestTime": [2025,6,29,15,30,45,123456789]
  }
}
```

### **Reset Metrics**
```http
POST /api/metrics/reset
```

**Response:**
```json
{
  "message": "Metrics reset successfully"
}
```

## 📊 **Monitoring Commands**

### **Real-time Monitoring**
```bash
# Get all endpoint metrics
curl http://localhost:8080/api/metrics/endpoints | jq

# Monitor specific endpoint
curl http://localhost:8080/api/metrics/endpoints | jq '.["GET /api/stock-trades"]'

# Watch metrics in real-time
watch -n 5 'curl -s http://localhost:8080/api/metrics/endpoints | jq'

# Check application health
curl http://localhost:8080/api/health
```

### **Database Queries**
```sql
-- Recent API metrics
SELECT test_name, average_response_time_ms, test_passed, timestamp 
FROM performance_metrics 
WHERE test_type = 'API_REQUEST' 
ORDER BY timestamp DESC 
LIMIT 10;

-- Average response times by endpoint
SELECT test_name, AVG(average_response_time_ms) as avg_time, COUNT(*) as requests
FROM performance_metrics 
WHERE test_type = 'API_REQUEST'
GROUP BY test_name;

-- Error rates by endpoint
SELECT 
    test_name,
    COUNT(*) as total,
    SUM(CASE WHEN test_passed THEN 1 ELSE 0 END) as successful,
    (SUM(CASE WHEN test_passed THEN 1 ELSE 0 END) * 100.0 / COUNT(*)) as success_rate
FROM performance_metrics 
WHERE test_type = 'API_REQUEST'
GROUP BY test_name;
```

## 🔍 **Troubleshooting**

### **No Metrics Collected**
```bash
# Check if metrics are enabled
grep -A 5 "metrics:" application.yml

# Verify endpoint is not excluded
curl http://localhost:8080/api/metrics/endpoints

# Check logs
tail -f logs/application.log | grep MetricsCollection
```

### **High Memory Usage**
```yaml
# Disable memory metrics
metrics:
  collection:
    includeMemoryMetrics: false
```

### **Performance Issues**
```yaml
# Reduce overhead
metrics:
  collection:
    samplingRate: 0.1               # 10% sampling
    asyncSave: true                 # Async saves
    includeMemoryMetrics: false     # Disable memory tracking
```

### **Database Issues**
```bash
# Check database connectivity
curl http://localhost:8080/api/performance-metrics

# Verify database config
grep -A 10 "metrics.database" application.yml
```

## 📈 **Performance Tuning**

### **High-Traffic Scenarios**
```yaml
metrics:
  collection:
    samplingRate: 0.01              # 1% sampling
    includeMemoryMetrics: false
    asyncSave: true
```

### **Development/Debug**
```yaml
metrics:
  collection:
    samplingRate: 1.0               # Full sampling
    includeMemoryMetrics: true
    asyncSave: false                # Immediate persistence
```

### **Load Testing**
```yaml
metrics:
  collection:
    enabled: false                  # Disable during load tests
```

## 🧪 **Testing**

### **Run Metrics Tests**
```bash
# Configuration tests
mvn test -Dtest="MetricsConfigTest"

# Integration tests (needs port fix)
mvn test -Dtest="MetricsCollectionTest"

# All tests
mvn test
```

### **Manual Testing**
```bash
# Start application
java -cp "target/classes;target/dependency/*" dev.mars.Application

# Generate test traffic
for i in {1..10}; do
  curl http://localhost:8080/api/stock-trades
  curl http://localhost:8080/api/health
done

# Check collected metrics
curl http://localhost:8080/api/metrics/endpoints | jq
```

## 🔧 **Common Tasks**

### **Enable Metrics Collection**
```yaml
metrics:
  collection:
    enabled: true
```

### **Disable for Specific Paths**
```yaml
metrics:
  collection:
    excludePaths:
      - "/dashboard"
      - "/static"
      - "/health"
```

### **Reduce Collection Overhead**
```yaml
metrics:
  collection:
    samplingRate: 0.1               # 10% sampling
    includeMemoryMetrics: false
    asyncSave: true
```

### **Reset In-Memory Metrics**
```bash
curl -X POST http://localhost:8080/api/metrics/reset
```

### **Export Metrics Data**
```bash
# Export to JSON
curl http://localhost:8080/api/metrics/endpoints > metrics.json

# Export database metrics
curl http://localhost:8080/api/performance-metrics > historical_metrics.json
```

## 📝 **Log Analysis**

### **Monitor Metrics Collection**
```bash
# Watch metrics handler logs
tail -f logs/application.log | grep "MetricsCollectionHandler"

# Check for errors
grep -i "error\|exception" logs/application.log | grep -i metrics

# Monitor database operations
grep "PerformanceMetricsService" logs/application.log
```

### **Performance Monitoring**
```bash
# Monitor response times
grep "Completed metrics collection" logs/application.log | tail -10

# Check memory usage
grep "memory" logs/application.log | tail -5
```

## 🚨 **Alerts & Thresholds**

### **Response Time Alerts**
```bash
# Check for slow endpoints (>1000ms)
curl http://localhost:8080/api/metrics/endpoints | jq 'to_entries[] | select(.value.averageResponseTime > 1000)'
```

### **Error Rate Alerts**
```bash
# Check for high error rates (<95% success)
curl http://localhost:8080/api/metrics/endpoints | jq 'to_entries[] | select(.value.successRate < 95)'
```

### **Traffic Monitoring**
```bash
# Check request volumes
curl http://localhost:8080/api/metrics/endpoints | jq 'to_entries[] | {endpoint: .key, requests: .value.totalRequests}'
```

## 🔗 **Integration Examples**

### **Prometheus Export**
```bash
# Example metrics export format
curl http://localhost:8080/api/metrics/endpoints | jq -r '
to_entries[] | 
"api_response_time{endpoint=\"\(.key)\"} \(.value.averageResponseTime)
api_request_total{endpoint=\"\(.key)\"} \(.value.totalRequests)
api_success_rate{endpoint=\"\(.key)\"} \(.value.successRate)"'
```

### **Grafana Dashboard Query**
```sql
-- For Grafana dashboard
SELECT 
    timestamp,
    test_name as endpoint,
    average_response_time_ms as response_time
FROM performance_metrics 
WHERE test_type = 'API_REQUEST'
  AND timestamp > NOW() - INTERVAL '1 hour'
ORDER BY timestamp;
```

## 📚 **Additional Resources**

- **Architecture Document:** `METRICS_COLLECTION_ARCHITECTURE.md`
- **Implementation Guide:** `METRICS_COLLECTION_IMPLEMENTATION.md`
- **Test Documentation:** `METRICS_COLLECTION_TESTS.md`
- **User Guide:** `METRICS_COLLECTION.md`

## 🎯 **Key Points**

- ✅ **Zero-code integration** - No controller changes needed
- ✅ **Automatic collection** - All endpoints monitored by default
- ✅ **Configurable** - Fine-tune for your environment
- ✅ **Real-time access** - Live metrics via REST API
- ✅ **Historical storage** - Database persistence for trends
- ✅ **Performance optimized** - Minimal overhead with async processing

# Endpoint Connectivity Validation Guide

## 🎯 **Overview**

The Endpoint Connectivity Validation system provides comprehensive HTTP testing of all configured API endpoints to ensure they are working correctly and performing well. This validation makes actual HTTP requests to endpoints and validates responses, providing real-world testing of your API configuration.

## ⚙️ **Configuration**

### **Enable Endpoint Validation**

Configure endpoint validation in `application.yml`:

```yaml
validation:
  runOnStartup: true          # Enable validation during startup
  validateOnly: false         # Continue with normal startup after validation
  validateEndpoints: true     # Include endpoint connectivity testing
```

### **Configuration Options**

- **`validateEndpoints: true`** - Include HTTP endpoint testing in validation
- **`validateEndpoints: false`** - Skip endpoint testing (useful for standalone validation)

## 🔍 **How It Works**

### **1. HTTP Request Testing**

The system makes actual HTTP requests to all configured endpoints:

```
GET http://localhost:8080/api/postgres/trades
GET http://localhost:8080/api/postgres/trades/count
GET http://localhost:8080/api/postgres/trades/symbol/AAPL
GET http://localhost:8080/api/analytics/daily-volume
```

### **2. Smart Parameter Substitution**

Automatically substitutes sample values for testing:

#### **Path Parameters**
- `{id}` → `1`
- `{symbol}` → `AAPL`
- `{trader_id}` → `TRADER_001`
- `{exchange}` → `NASDAQ`
- `{trade_type}` → `BUY`
- `{databaseName}` → `postgres-trades`

#### **Query Parameters**
- **Pagination**: `?page=0&size=2`
- **Date Ranges**: `?start_date=2024-01-01&end_date=2024-12-31`
- **Analytics**: `?start_date=2024-01-01&end_date=2024-12-31`

### **3. Response Analysis**

#### **Status Code Interpretation**
- **200-299**: ✅ Success - endpoint working correctly
- **400**: ⚠️ Bad Request - acceptable for parameter-dependent endpoints
- **404**: ❌ Not Found - endpoint not registered (error)
- **500+**: ❌ Server Error - application or database issue (error)

#### **Performance Monitoring**
- Measures response time for each endpoint
- Identifies slow-responding endpoints (>1000ms)
- Reports timeout issues (>10 seconds)

## 🌐 **Validation API**

### **Endpoint Connectivity Testing**

```bash
# Test all endpoint connectivity
curl http://localhost:8080/api/generic/config/validate/endpoint-connectivity
```

### **Response Format**

```json
{
  "status": "INVALID",
  "baseUrl": "http://localhost:8080",
  "totalEndpoints": 22,
  "successCount": 12,
  "errorCount": 10,
  "successes": [
    "Endpoint 'postgres-trades-list' -> GET /api/postgres/trades [200] (51ms)",
    "Endpoint 'postgres-trades-count' -> GET /api/postgres/trades/count [200] (6ms)",
    "Endpoint 'postgres-trades-by-symbol' -> GET /api/postgres/trades/symbol/{symbol} [200] (14ms)"
  ],
  "errors": [
    "Endpoint 'analytics-daily-volume' -> GET /api/analytics/daily-volume [500 - Server Error] (75ms)",
    "Endpoint 'test-unavailable-endpoint' -> GET /api/test/unavailable [404 - Not Found] - Endpoint not registered"
  ],
  "timestamp": 1751796214132
}
```

### **Response Fields**

- **`status`**: Overall validation status (`VALID` or `INVALID`)
- **`baseUrl`**: Base URL used for testing
- **`totalEndpoints`**: Total number of configured endpoints
- **`successCount`**: Number of successful endpoint tests
- **`errorCount`**: Number of failed endpoint tests
- **`successes`**: Array of successful endpoint test results
- **`errors`**: Array of failed endpoint test results with error details
- **`timestamp`**: Validation execution timestamp

## 🚀 **Usage Scenarios**

### **1. Startup Validation**

Enable endpoint testing during application startup:

```yaml
validation:
  runOnStartup: true
  validateEndpoints: true
```

**Behavior:**
- Runs after server startup
- Tests all endpoints with HTTP requests
- Continues normal operation if validation passes
- Logs detailed results for monitoring

### **2. On-Demand Testing**

Test endpoints manually via API:

```bash
# Test endpoint connectivity
curl http://localhost:8080/api/generic/config/validate/endpoint-connectivity

# View results in Swagger UI
open http://localhost:8080/swagger
```

### **3. CI/CD Integration**

```bash
#!/bin/bash
# deployment-validation.sh

echo "Starting application for endpoint validation..."
java -jar generic-api-service.jar &
APP_PID=$!

# Wait for application to start
sleep 10

# Test endpoint connectivity
curl -f http://localhost:8080/api/generic/config/validate/endpoint-connectivity

if [ $? -eq 0 ]; then
    echo "✅ Endpoint validation passed"
else
    echo "❌ Endpoint validation failed"
    kill $APP_PID
    exit 1
fi

# Continue with deployment...
kill $APP_PID
```

### **4. Monitoring and Alerting**

```bash
# Monitor endpoint health
while true; do
    RESULT=$(curl -s http://localhost:8080/api/generic/config/validate/endpoint-connectivity | jq '.status')
    if [ "$RESULT" != '"VALID"' ]; then
        echo "ALERT: Endpoint validation failed at $(date)"
        # Send alert notification
    fi
    sleep 300  # Check every 5 minutes
done
```

## 🔧 **Integration Examples**

### **Swagger UI Integration**

All validation APIs are available in Swagger UI:
1. Navigate to http://localhost:8080/swagger
2. Find "Configuration Validation" section
3. Test `/api/generic/config/validate/endpoint-connectivity`
4. View detailed response with success/error breakdown

### **Application Monitoring**

```java
// Custom monitoring integration
@RestController
public class HealthController {
    
    @Autowired
    private GenericApiService genericApiService;
    
    @GetMapping("/health/endpoints")
    public ResponseEntity<Map<String, Object>> checkEndpointHealth() {
        Map<String, Object> result = genericApiService.validateEndpointConnectivity();
        
        if ("VALID".equals(result.get("status"))) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(503).body(result);
        }
    }
}
```

## 🚨 **Error Handling**

### **Common Error Scenarios**

#### **Database Connection Issues**
```json
{
  "errors": [
    "Endpoint 'stock-trades-list' -> GET /api/generic/stock-trades [500 - Server Error] (8ms)"
  ]
}
```
**Solution**: Check database connectivity and application logs.

#### **Endpoint Not Registered**
```json
{
  "errors": [
    "Endpoint 'test-endpoint' -> GET /api/test/endpoint [404 - Not Found] - Endpoint not registered"
  ]
}
```
**Solution**: Verify endpoint configuration and ensure database is available.

#### **Slow Response Times**
```json
{
  "successes": [
    "Endpoint 'analytics-query' -> GET /api/analytics/data [200] (2500ms)"
  ]
}
```
**Solution**: Optimize database queries or increase server resources.

### **Timeout Handling**

The system uses a 10-second timeout for HTTP requests:
- Requests taking longer than 10 seconds are marked as timeouts
- Timeout errors are reported in the `errors` array
- Consider optimizing slow endpoints or increasing timeout if needed

## 📊 **Performance Monitoring**

### **Response Time Analysis**

The validation provides detailed performance metrics:
- **Fast endpoints**: <100ms response time
- **Normal endpoints**: 100-500ms response time
- **Slow endpoints**: 500-1000ms response time
- **Very slow endpoints**: >1000ms response time

### **Performance Optimization**

Use validation results to identify performance issues:
1. **Database Query Optimization**: Slow endpoints often indicate inefficient queries
2. **Connection Pool Tuning**: Multiple slow endpoints may indicate connection pool issues
3. **Index Creation**: Missing database indexes can cause slow query performance
4. **Resource Scaling**: Consistently slow responses may require more server resources

## 📚 **Related Documentation**

- [Configuration Validation Guide](CONFIGURATION_VALIDATION.md) - Complete validation system
- [Architecture Guide](ARCHITECTURE_GUIDE.md) - System architecture overview
- [Bootstrap Demo](BOOTSTRAP_DEMO.md) - System demonstration including validation
- [Configuration Schema Reference](CONFIGURATION_SCHEMA_REFERENCE.md) - YAML configuration guide

## 🔧 **Best Practices**

- **Enable in Production**: Use `runOnStartup: true` with `validateEndpoints: true` in production
- **Monitor Response Times**: Regularly check endpoint performance via validation API
- **Automate Testing**: Include endpoint validation in CI/CD pipelines
- **Alert on Failures**: Set up monitoring alerts for validation failures
- **Performance Baselines**: Establish baseline response times for performance monitoring
- **Regular Testing**: Run endpoint validation periodically to catch issues early

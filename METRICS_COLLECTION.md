# Generic Metrics Collection System

This document describes the generic metrics collection system that automatically captures performance metrics for all API endpoints.

## Overview

The metrics collection system automatically captures the following metrics for every API request:
- **Response Time**: How long each request takes to process
- **Request Count**: Number of requests per endpoint
- **Success Rate**: Percentage of successful requests (2xx-3xx status codes)
- **Memory Usage**: Optional memory consumption tracking
- **Endpoint-specific Metrics**: Aggregated statistics per endpoint

## Features

### ✅ **Automatic Collection**
- No code changes required in controllers
- Automatically attached to all API endpoints
- Real-time metrics aggregation

### ✅ **Configurable**
- Enable/disable metrics collection
- Exclude specific paths
- Configurable sampling rate
- Memory metrics toggle
- Async vs sync saving

### ✅ **Performance Optimized**
- Minimal overhead on request processing
- Asynchronous metrics saving
- Sampling support for high-traffic scenarios
- Thread-safe implementation

### ✅ **Integration Ready**
- Works with existing PerformanceMetrics infrastructure
- Compatible with dashboard and Grafana
- RESTful API for metrics access

## Configuration

### YAML Configuration (`application.yml`)

```yaml
metrics:
  collection:
    enabled: true                    # Enable/disable metrics collection
    includeMemoryMetrics: true       # Include memory usage metrics
    excludePaths:                    # Paths to exclude from metrics
      - "/dashboard"
      - "/metrics"
      - "/api/performance-metrics"
    samplingRate: 1.0               # 1.0 = 100%, 0.1 = 10% of requests
    asyncSave: true                 # Save metrics asynchronously
```

### Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | boolean | `true` | Enable/disable metrics collection |
| `includeMemoryMetrics` | boolean | `true` | Include memory usage in metrics |
| `excludePaths` | list | `["/dashboard", "/metrics", "/api/performance-metrics"]` | Paths to exclude |
| `samplingRate` | double | `1.0` | Percentage of requests to sample (0.0-1.0) |
| `asyncSave` | boolean | `true` | Save metrics asynchronously |

## API Endpoints

### Real-time Metrics Summary
```http
GET /api/metrics/endpoints
```

Returns aggregated metrics for all endpoints:
```json
{
  "GET /api/stock-trades": {
    "totalRequests": 150,
    "averageResponseTime": 45.2,
    "successRate": 98.7,
    "lastRequestTime": "2025-06-29T10:30:45"
  },
  "GET /api/stock-trades/{id}": {
    "totalRequests": 75,
    "averageResponseTime": 23.1,
    "successRate": 100.0,
    "lastRequestTime": "2025-06-29T10:29:12"
  }
}
```

### Reset Metrics
```http
POST /api/metrics/reset
```

Clears all in-memory endpoint metrics:
```json
{
  "message": "Metrics reset successfully"
}
```

## How It Works

### 1. **Request Interception**
- `beforeRequest()` captures start time and initial state
- `afterRequest()` calculates metrics and saves data

### 2. **Path Normalization**
- `/api/stock-trades/123` → `/api/stock-trades/{id}`
- `/api/stock-trades/AAPL` → `/api/stock-trades/{symbol}`
- Enables proper aggregation across similar endpoints

### 3. **Dual Storage**
- **In-Memory**: Real-time aggregated metrics for immediate access
- **Database**: Individual request metrics for historical analysis

### 4. **Sampling**
- Configurable sampling rate to reduce overhead
- All requests contribute to in-memory metrics
- Only sampled requests are saved to database

## Integration Examples

### Viewing Real-time Metrics
```bash
# Get current endpoint metrics
curl http://localhost:8080/api/metrics/endpoints

# Reset metrics
curl -X POST http://localhost:8080/api/metrics/reset
```

### Monitoring High-Traffic Scenarios
```yaml
# For high-traffic applications, use sampling
metrics:
  collection:
    enabled: true
    samplingRate: 0.1  # Sample 10% of requests
    asyncSave: true
```

### Development vs Production
```yaml
# Development - Full metrics
metrics:
  collection:
    enabled: true
    includeMemoryMetrics: true
    samplingRate: 1.0

# Production - Optimized
metrics:
  collection:
    enabled: true
    includeMemoryMetrics: false
    samplingRate: 0.05  # 5% sampling
    asyncSave: true
```

## Performance Impact

### Minimal Overhead
- **Memory**: ~100 bytes per unique endpoint
- **CPU**: <1ms additional processing per request
- **Storage**: Configurable via sampling rate

### Optimization Features
- Thread-local storage for request data
- Concurrent data structures for thread safety
- Asynchronous database operations
- Configurable sampling to reduce load

## Troubleshooting

### Common Issues

1. **No Metrics Collected**
   - Check `metrics.collection.enabled: true`
   - Verify path is not in `excludePaths`
   - Check sampling rate > 0

2. **High Memory Usage**
   - Disable memory metrics: `includeMemoryMetrics: false`
   - Reduce sampling rate
   - Reset metrics periodically

3. **Performance Impact**
   - Enable async saving: `asyncSave: true`
   - Reduce sampling rate
   - Exclude high-traffic paths

### Debugging
```bash
# Check configuration
curl http://localhost:8080/api/metrics/endpoints

# Monitor logs for metrics collection
tail -f logs/application.log | grep MetricsCollection
```

## Architecture

### Components
- **MetricsCollectionHandler**: Core collection logic
- **AppConfig.MetricsCollection**: Configuration management
- **ApiRoutes**: Integration with Javalin handlers
- **PerformanceMetricsService**: Database persistence

### Data Flow
```
Request → beforeRequest() → Controller → afterRequest() → Metrics Storage
                ↓                              ↓
        Capture Start Time              Calculate & Save
```

## Future Enhancements

- **Custom Metrics**: Support for application-specific metrics
- **Alerting**: Threshold-based notifications
- **Export**: Prometheus/Grafana integration
- **Retention**: Automatic cleanup of old metrics
- **Clustering**: Distributed metrics aggregation

# Health Monitoring Guide - Javalin API Mesh

## Overview

The Javalin API Mesh provides comprehensive health monitoring capabilities designed for production deployments, container orchestration, and automated monitoring systems.

## Health Check Endpoints

### 1. Basic Health Check
**Endpoint**: `GET /api/health`
**Purpose**: Simple health status for basic monitoring

```bash
curl http://localhost:8080/api/health
```

**Response**:
```json
{
  "status": "UP",
  "timestamp": 1703123456789,
  "service": "generic-api-service"
}
```

### 2. Comprehensive Health Status
**Endpoint**: `GET /api/management/health`
**Purpose**: Detailed health information including databases and configuration

```bash
curl http://localhost:8080/api/management/health
```

**Response**:
```json
{
  "timestamp": "2024-01-15T10:30:45.123Z",
  "overall": "UP",
  "service": {
    "status": "UP",
    "uptime": "2d 5h 30m 45s",
    "memoryUsage": {
      "maxMemoryMB": 512,
      "totalMemoryMB": 256,
      "usedMemoryMB": 128,
      "freeMemoryMB": 128,
      "usagePercentage": 50
    },
    "threadCount": 25
  },
  "databases": {
    "stocktrades": {
      "databaseName": "stocktrades",
      "status": "UP",
      "message": "Connection successful",
      "checkTime": "2024-01-15T10:30:45.123Z",
      "responseTimeMs": 15
    }
  },
  "configuration": {
    "status": "UP",
    "databasesLoaded": 2,
    "queriesLoaded": 8,
    "endpointsLoaded": 12,
    "lastValidation": "SUCCESS"
  }
}
```

### 3. Database Health
**Endpoint**: `GET /api/management/health/databases`
**Purpose**: Health status of all configured databases

```bash
curl http://localhost:8080/api/management/health/databases
```

**Specific Database**: `GET /api/management/health/databases/{databaseName}`
```bash
curl http://localhost:8080/api/management/health/databases/stocktrades
```

### 4. Readiness Check
**Endpoint**: `GET /api/management/readiness`
**Purpose**: Kubernetes readiness probe - service ready to accept traffic

```bash
curl http://localhost:8080/api/management/readiness
```

**Response**:
```json
{
  "status": "READY",
  "checks": {
    "configuration": "OK",
    "databases": "OK",
    "memory": "OK"
  },
  "timestamp": "2024-01-15T10:30:45.123Z"
}
```

**Status Codes**:
- `200 OK`: Service is ready
- `503 Service Unavailable`: Service is not ready

### 5. Liveness Check
**Endpoint**: `GET /api/management/liveness`
**Purpose**: Kubernetes liveness probe - service is alive and responsive

```bash
curl http://localhost:8080/api/management/liveness
```

**Response**:
```json
{
  "status": "UP",
  "checks": {
    "application": "UP",
    "memory": "OK",
    "threads": "OK"
  },
  "timestamp": "2024-01-15T10:30:45.123Z"
}
```

**Status Codes**:
- `200 OK`: Service is alive
- `503 Service Unavailable`: Service should be restarted

## Deployment Verification Endpoints

### 1. Deployment Information
**Endpoint**: `GET /api/management/deployment`
**Purpose**: Verify deployment configuration and environment

```bash
curl http://localhost:8080/api/management/deployment
```

**Response**:
```json
{
  "status": "DEPLOYED",
  "jarPath": "/app/generic-api-service-1.0-SNAPSHOT-executable.jar",
  "jarType": "fat-jar",
  "javaVersion": "21.0.1",
  "javaVendor": "Eclipse Adoptium",
  "javaHome": "/opt/java/openjdk",
  "osName": "Linux",
  "osVersion": "5.4.0",
  "osArch": "amd64",
  "applicationName": "Generic API Service",
  "version": "1.0-SNAPSHOT",
  "startTime": "2024-01-15T08:00:00.000Z",
  "workingDirectory": "/app"
}
```

### 2. JAR Information
**Endpoint**: `GET /api/management/jar`
**Purpose**: Detailed JAR and dependency information

```bash
curl http://localhost:8080/api/management/jar
```

**Response**:
```json
{
  "jarPath": "/app/generic-api-service-1.0-SNAPSHOT-executable.jar",
  "jarType": "fat-jar",
  "implementationTitle": "Generic API Service",
  "implementationVersion": "1.0-SNAPSHOT",
  "implementationVendor": "dev.mars",
  "classPath": "/app/generic-api-service-1.0-SNAPSHOT-executable.jar",
  "libraryPath": "/usr/java/packages/lib:/usr/lib64:/lib64:/lib:/usr/lib",
  "modulePath": null
}
```

## Container Integration

### Docker Health Checks

#### Dockerfile Health Check
```dockerfile
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/management/liveness || exit 1
```

#### Docker Compose Health Check
```yaml
services:
  generic-api:
    image: javalin-api-mesh/generic-api:latest
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/management/liveness"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
```

### Kubernetes Probes

#### Liveness Probe
```yaml
livenessProbe:
  httpGet:
    path: /api/management/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 30
  timeoutSeconds: 10
  failureThreshold: 3
```

#### Readiness Probe
```yaml
readinessProbe:
  httpGet:
    path: /api/management/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3
```

#### Startup Probe
```yaml
startupProbe:
  httpGet:
    path: /api/management/readiness
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 30
```

## Monitoring Integration

### Prometheus Metrics (Metrics Service)
```bash
# Metrics endpoint (if enabled)
curl http://localhost:8081/metrics

# Custom metrics dashboard
curl http://localhost:8081/dashboard
```

### External Monitoring Systems

#### Nagios Check
```bash
#!/bin/bash
# check_javalin_health.sh
response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/management/liveness)
if [ "$response" = "200" ]; then
    echo "OK - Service is healthy"
    exit 0
else
    echo "CRITICAL - Service health check failed (HTTP $response)"
    exit 2
fi
```

#### Zabbix Item
```json
{
  "name": "Javalin API Health",
  "type": "HTTP agent",
  "url": "http://localhost:8080/api/management/health",
  "request_method": "GET",
  "timeout": "10s",
  "value_type": "Text",
  "preprocessing": [
    {
      "type": "JSONPath",
      "params": "$.overall"
    }
  ]
}
```

#### DataDog Check
```python
from datadog_checks.base import AgentCheck

class JavalinHealthCheck(AgentCheck):
    def check(self, instance):
        url = instance.get('url', 'http://localhost:8080/api/management/health')
        
        try:
            response = self.http.get(url)
            response.raise_for_status()
            
            health_data = response.json()
            overall_status = health_data.get('overall', 'UNKNOWN')
            
            if overall_status == 'UP':
                self.service_check('javalin.health', AgentCheck.OK)
            elif overall_status == 'DEGRADED':
                self.service_check('javalin.health', AgentCheck.WARNING)
            else:
                self.service_check('javalin.health', AgentCheck.CRITICAL)
                
            # Memory metrics
            memory = health_data.get('service', {}).get('memoryUsage', {})
            self.gauge('javalin.memory.usage_percentage', memory.get('usagePercentage', 0))
            
        except Exception as e:
            self.service_check('javalin.health', AgentCheck.CRITICAL, message=str(e))
```

## Health Status Interpretation

### Overall Health States

#### UP
- All systems operational
- All databases accessible
- Configuration loaded successfully
- Memory usage within normal limits

#### DEGRADED
- Service is running but with issues
- Some databases may be unavailable
- Non-critical components failing
- Service can still handle requests

#### DOWN
- Critical failure detected
- Configuration loading failed
- Service cannot handle requests
- Requires immediate attention

### Database Health States

#### UP
- Connection successful
- Database responsive
- Query execution working

#### DOWN
- Connection failed
- Database unreachable
- Authentication issues

### Memory Health Thresholds

- **OK**: < 80% usage
- **WARNING**: 80-90% usage
- **CRITICAL**: 90-95% usage
- **FAILURE**: > 95% usage

## Alerting Strategies

### Critical Alerts (Immediate Response)
- Overall health: DOWN
- Liveness check: DOWN
- Memory usage: > 95%
- All databases: DOWN

### Warning Alerts (Monitor Closely)
- Overall health: DEGRADED
- Memory usage: 80-90%
- Some databases: DOWN
- High response times

### Informational Alerts
- Service restart detected
- Configuration reloaded
- New deployment detected

## Troubleshooting Health Issues

### Common Health Check Failures

#### 1. Service Not Ready
```bash
# Check readiness details
curl http://localhost:8080/api/management/readiness

# Check configuration loading
curl http://localhost:8080/api/management/health | jq '.configuration'

# Check database connectivity
curl http://localhost:8080/api/management/health/databases
```

#### 2. Database Connection Issues
```bash
# Check specific database
curl http://localhost:8080/api/management/health/databases/stocktrades

# Check database configuration
curl http://localhost:8080/api/management/config/databases
```

#### 3. Memory Issues
```bash
# Check memory usage
curl http://localhost:8080/api/management/health | jq '.service.memoryUsage'

# Check JAR type (optimized JARs use less memory)
curl http://localhost:8080/api/management/jar | jq '.jarType'
```

#### 4. Deployment Verification
```bash
# Check deployment info
curl http://localhost:8080/api/management/deployment

# Verify JAR type and version
curl http://localhost:8080/api/management/jar
```

## Best Practices

1. **Use appropriate probes** - Liveness for restart, readiness for traffic
2. **Set proper timeouts** - Allow sufficient time for startup
3. **Monitor trends** - Track health metrics over time
4. **Alert on patterns** - Multiple failures, degraded performance
5. **Test health checks** - Verify they work in all scenarios
6. **Document thresholds** - Clear criteria for each health state
7. **Automate responses** - Restart unhealthy containers
8. **Log health events** - Track health state changes

## Integration Examples

See [DEPLOYMENT_EXAMPLES.md](DEPLOYMENT_EXAMPLES.md) for complete deployment examples with health monitoring integration.

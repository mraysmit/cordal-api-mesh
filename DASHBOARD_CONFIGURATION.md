# Dashboard Configuration Guide

This guide explains how to configure and switch between the custom dashboard and Grafana integration for performance monitoring.

## 🏗️ **Architecture Overview**

The application now supports **dual dashboard architecture**:

### **Separate Databases**
- **Main Database**: `./data/stocktrades.mv.db` - Stock trades and application data
- **Metrics Database**: `./data/metrics.mv.db` - Performance metrics, system metrics, application metrics

### **Dashboard Options**
1. **Custom Dashboard**: Built-in HTML/CSS/JavaScript dashboard
2. **Grafana Integration**: Prometheus metrics endpoint for Grafana visualization

## ⚙️ **Configuration Files**

### **Default Configuration** (`application.yml`)
```yaml
metrics:
  database:
    url: "jdbc:h2:./data/metrics;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1"
    username: "sa"
    password: ""
    driver: "org.h2.Driver"
    pool:
      maximumPoolSize: 5
      minimumIdle: 1
  dashboard:
    custom:
      enabled: true          # ✅ Custom dashboard enabled
      path: "/dashboard"
    grafana:
      enabled: false         # ❌ Grafana disabled
      url: "http://localhost:3000"
      prometheus:
        enabled: false       # ❌ Prometheus metrics disabled
        port: 9090
        path: "/metrics"
```

### **Grafana Configuration** (`application-grafana.yml`)
```yaml
metrics:
  dashboard:
    custom:
      enabled: false         # ❌ Custom dashboard disabled
      path: "/dashboard"
    grafana:
      enabled: true          # ✅ Grafana enabled
      url: "http://localhost:3000"
      prometheus:
        enabled: true        # ✅ Prometheus metrics enabled
        port: 9090
        path: "/metrics"
```

## 🚀 **Running Different Configurations**

### **Option 1: Custom Dashboard Mode (Default)**
```bash
# Start with custom dashboard
java -cp "target/classes;target/dependency/*" dev.mars.Application

# Access dashboard at:
# http://localhost:8080/dashboard
```

**Features Available:**
- ✅ Interactive HTML dashboard at `/dashboard`
- ✅ Real-time performance metrics
- ✅ Charts and visualizations
- ✅ REST API endpoints
- ❌ Prometheus metrics endpoint disabled

### **Option 2: Grafana Mode**
```bash
# Start with Grafana configuration
java -cp "target/classes;target/dependency/*" -Dspring.profiles.active=grafana dev.mars.Application

# Or copy application-grafana.yml to application.yml and restart
```

**Features Available:**
- ❌ Custom dashboard disabled
- ✅ Prometheus metrics at `/metrics`
- ✅ REST API endpoints
- ✅ Ready for Grafana integration

### **Option 3: Both Enabled (Hybrid)**
```yaml
metrics:
  dashboard:
    custom:
      enabled: true          # ✅ Both enabled
    grafana:
      enabled: true
      prometheus:
        enabled: true
```

## 📊 **Database Schema**

### **Metrics Database Tables**

#### **performance_metrics**
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

#### **system_metrics**
```sql
CREATE TABLE system_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_name VARCHAR(255) NOT NULL,
    metric_value DOUBLE NOT NULL,
    metric_type VARCHAR(50) NOT NULL,
    tags TEXT,
    timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### **application_metrics**
```sql
CREATE TABLE application_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    endpoint VARCHAR(255) NOT NULL,
    method VARCHAR(10) NOT NULL,
    status_code INTEGER NOT NULL,
    response_time_ms BIGINT NOT NULL,
    request_size_bytes BIGINT,
    response_size_bytes BIGINT,
    user_agent TEXT,
    ip_address VARCHAR(45),
    timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 🔧 **API Endpoints**

### **Performance Metrics API**
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/performance-metrics` | GET | Get all metrics with pagination |
| `/api/performance-metrics/summary` | GET | Get performance summary |
| `/api/performance-metrics/trends` | GET | Get trend data for charts |
| `/api/performance-metrics/test-types` | GET | Get available test types |

### **Dashboard Endpoints**
| Configuration | Endpoint | Description |
|---------------|----------|-------------|
| Custom Dashboard | `/dashboard` | Interactive HTML dashboard |
| Grafana Mode | `/metrics` | Prometheus metrics endpoint |

## 🧪 **Testing the Setup**

### **1. Test Custom Dashboard**
```bash
# Start with default config
java -cp "target/classes;target/dependency/*" dev.mars.Application

# Test endpoints
curl http://localhost:8080/api/performance-metrics/summary
curl http://localhost:8080/dashboard

# Run performance tests to generate data
mvn test -Dtest="dev.mars.performance.EnhancedPerformanceTest"
```

### **2. Test Grafana Integration**
```bash
# Update application.yml to enable Grafana mode:
# metrics.dashboard.grafana.enabled: true
# metrics.dashboard.grafana.prometheus.enabled: true

# Restart application
java -cp "target/classes;target/dependency/*" dev.mars.Application

# Test Prometheus endpoint
curl http://localhost:8080/metrics
```

### **3. Test Database Separation**
```bash
# Check main database
curl http://localhost:8080/api/stock-trades

# Check metrics database  
curl http://localhost:8080/api/performance-metrics

# Verify separate database files exist:
# ./data/stocktrades.mv.db
# ./data/metrics.mv.db
```

## 🐳 **Docker Compose for Grafana**

Create `docker-compose.yml` for full Grafana setup:

```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - METRICS_DASHBOARD_GRAFANA_ENABLED=true
      - METRICS_DASHBOARD_GRAFANA_PROMETHEUS_ENABLED=true
    volumes:
      - ./data:/app/data

  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana-storage:/var/lib/grafana

volumes:
  grafana-storage:
```

### **Prometheus Configuration** (`prometheus.yml`)
```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'javalin-api'
    static_configs:
      - targets: ['app:8080']
    metrics_path: '/metrics'
    scrape_interval: 5s
```

## 🔄 **Switching Between Modes**

### **Method 1: Configuration File**
1. Edit `src/main/resources/application.yml`
2. Change `metrics.dashboard.custom.enabled` and `metrics.dashboard.grafana.enabled`
3. Restart application

### **Method 2: Environment Variables**
```bash
# Enable custom dashboard
export METRICS_DASHBOARD_CUSTOM_ENABLED=true
export METRICS_DASHBOARD_GRAFANA_ENABLED=false

# Enable Grafana
export METRICS_DASHBOARD_CUSTOM_ENABLED=false
export METRICS_DASHBOARD_GRAFANA_ENABLED=true
export METRICS_DASHBOARD_GRAFANA_PROMETHEUS_ENABLED=true
```

### **Method 3: System Properties**
```bash
java -cp "target/classes;target/dependency/*" \
  -Dmetrics.dashboard.custom.enabled=false \
  -Dmetrics.dashboard.grafana.enabled=true \
  -Dmetrics.dashboard.grafana.prometheus.enabled=true \
  dev.mars.Application
```

## 📈 **Performance Monitoring Workflow**

### **Development Mode** (Custom Dashboard)
1. Enable custom dashboard
2. Run performance tests
3. View results at `/dashboard`
4. Quick debugging and analysis

### **Production Mode** (Grafana)
1. Enable Grafana + Prometheus
2. Set up Grafana dashboards
3. Configure alerts and notifications
4. Long-term monitoring and analysis

## 🚨 **Troubleshooting**

### **Common Issues**

1. **Port conflicts**: Ensure ports 8080, 3000, 9090 are available
2. **Database locks**: Stop all Java processes before restart
3. **Missing data**: Run performance tests to populate metrics
4. **Configuration not loading**: Check YAML syntax and file paths

### **Verification Commands**
```bash
# Check if application is running
curl http://localhost:8080/api/health

# Check metrics database
curl http://localhost:8080/api/performance-metrics

# Check Prometheus endpoint (if enabled)
curl http://localhost:8080/metrics

# Check database files
ls -la data/
```

This flexible configuration allows you to choose the best monitoring solution for your needs while maintaining data separation and clean architecture!

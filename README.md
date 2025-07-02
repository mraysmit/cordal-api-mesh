# Javalin API Mesh

A highly modularized, YAML-configured REST API mesh with automatic performance monitoring and dynamic endpoint creation.

## 🏗️ **Architecture Overview**

The project is organized into multiple independent modules with shared common utilities:

```
javalin-api-mesh/
├── common-library/          # 🔧 Shared utilities and base classes
├── generic-api-service/     # 🌐 Dynamic API service (Port 8080)
├── metrics-service/         # 📊 Performance monitoring (Port 8081)
├── integration-tests/       # 🧪 Cross-module integration testing
├── javalin-api-mesh/config/ # ⚙️ Shared YAML configuration files
└── pom.xml                 # 📦 Parent POM with shared dependencies
```

## ✨ **Key Features**

### **🌐 Generic API Service**
- **YAML-Driven APIs** - Define REST endpoints without coding
- **Dynamic Query Configuration** - SQL queries externalized to YAML
- **Multi-Database Support** - Connect different endpoints to different databases
- **Swagger Integration** - Auto-generated API documentation
- **Configuration Management APIs** - Runtime configuration inspection and validation

### **📊 Metrics Service**
- **Automatic Metrics Collection** - Zero-code performance monitoring
- **Real-time Analytics** - Live performance dashboards
- **Historical Data Storage** - Persistent metrics with trend analysis
- **Grafana Integration** - Export to Prometheus/Grafana
- **Custom Dashboards** - Built-in performance visualization

### **🔧 Common Library**
- **Shared Database Management** - Common database utilities and connection pooling
- **Configuration Framework** - YAML configuration loading and validation
- **Exception Handling** - Standardized error handling across modules
- **Application Framework** - Base classes for Javalin applications
- **Metrics Collection Framework** - Reusable performance monitoring components

### **🧪 Integration Testing**
- **Cross-Module Testing** - Validate inter-service communication
- **Configuration Validation** - Test YAML configuration loading
- **Performance Testing** - Load testing and metrics validation
- **Real Database Testing** - Integration tests with actual databases

## 🌐 **API Endpoints**

### **Generic API Service** (Port 8080)

The Generic API Service provides YAML-configured REST endpoints for data access:

#### **Stock Trades API**
- `GET /api/generic/stock-trades` - Get all stock trades with pagination
  - Query parameters: `page` (default: 1), `size` (default: 20), `async` (default: false)
- `GET /api/generic/stock-trades/{id}` - Get stock trade by ID
- `GET /api/generic/stock-trades/symbol/{symbol}` - Get stock trades by symbol with pagination
- `GET /api/generic/stock-trades/trader/{trader_id}` - Get stock trades by trader ID
- `GET /api/generic/stock-trades/date-range` - Get stock trades by date range
  - Query parameters: `start_date`, `end_date`, `page`, `size`

#### **Configuration Management API**
- `GET /api/generic/config/validate` - Validate all configurations
- `GET /api/generic/config/validate/endpoints` - Validate endpoint configurations
- `GET /api/generic/config/validate/queries` - Validate query configurations
- `GET /api/generic/config/validate/databases` - Validate database configurations
- `GET /api/generic/config/validate/relationships` - Validate configuration relationships
- `GET /api/generic/config/endpoints` - List all configured endpoints
- `GET /api/generic/config/queries` - List all configured queries
- `GET /api/generic/config/databases` - List all configured databases

#### **Documentation & Health**
- `GET /swagger` - Swagger UI for API documentation
- `GET /api-docs` - OpenAPI specification
- `GET /api/health` - Health check endpoint

### **Metrics Service** (Port 8081)

The Metrics Service provides performance monitoring and analytics:

#### **Performance Metrics API**
- `GET /api/performance-metrics` - Get all performance metrics with pagination
- `GET /api/performance-metrics/{id}` - Get specific performance metrics by ID
- `GET /api/performance-metrics/summary` - Get performance summary statistics
- `GET /api/metrics/endpoints` - Get real-time endpoint metrics
- `GET /api/metrics/system` - Get system performance metrics

#### **Dashboard & Monitoring**
- `GET /dashboard` - Performance monitoring dashboard
- `GET /api/metrics/prometheus` - Prometheus metrics endpoint (if enabled)
- `GET /swagger` - Swagger UI for metrics API documentation

## ⚙️ **Configuration System**

The system uses a sophisticated YAML-based configuration approach with three main configuration files:

### **1. Database Connections** (`javalin-api-mesh/config/databases.yml`)
```yaml
databases:
  stock-trades-db:
    name: "stock-trades-db"
    description: "Main database for stock trading data"
    url: "jdbc:h2:./data/stocktrades;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1"
    username: "sa"
    password: ""
    driver: "org.h2.Driver"
    pool:
      minimumIdle: 2
      maximumPoolSize: 10
      connectionTimeout: 30000
      idleTimeout: 600000
      maxLifetime: 1800000
```

### **2. SQL Queries** (`javalin-api-mesh/config/queries.yml`)
```yaml
queries:
  stock-trades-all:
    name: "stock-trades-all"
    description: "Get all stock trades with pagination"
    sql: "SELECT * FROM stock_trades ORDER BY trade_date_time DESC LIMIT ? OFFSET ?"
    database: "stock-trades-db"
    parameters:
      - name: "limit"
        type: "INTEGER"
        required: true
      - name: "offset"
        type: "INTEGER"
        required: true
```

### **3. API Endpoints** (`javalin-api-mesh/config/api-endpoints.yml`)
```yaml
endpoints:
  stock-trades-list:
    path: "/api/generic/stock-trades"
    method: "GET"
    query: "stock-trades-all"
    countQuery: "stock-trades-count"
    description: "Get all stock trades with pagination"
    pagination:
      enabled: true
      defaultSize: 20
      maxSize: 100
    parameters:
      - name: "page"
        type: "INTEGER"
        source: "QUERY"
        required: false
        defaultValue: "1"
```

### **Configuration Benefits**
- ✅ **Zero-Code API Creation** - Add new endpoints without programming
- ✅ **Multi-Database Support** - Different endpoints can use different databases
- ✅ **Automatic Validation** - Configuration integrity checks at startup
- ✅ **Runtime Inspection** - APIs to view and validate configurations
- ✅ **Environment Flexibility** - Easy configuration changes per environment

## 🚀 **Getting Started**

### **Prerequisites**
- Java 21 or higher
- Maven 3.8 or higher

### **Build & Run**

#### **1. Build All Modules**
```bash
# Clone the repository
git clone <repository-url>
cd javalin-api-mesh

# Build all modules
mvn clean install
```

#### **2. Run Generic API Service** (Port 8080)
```bash
cd generic-api-service
mvn exec:java -Dexec.mainClass="dev.mars.generic.GenericApiApplication"
```

#### **3. Run Metrics Service** (Port 8081)
```bash
cd metrics-service
mvn exec:java -Dexec.mainClass="dev.mars.metrics.MetricsApplication"
```

#### **4. Access the Applications**
- **Generic API Service**: http://localhost:8080
- **Swagger Documentation**: http://localhost:8080/swagger
- **Metrics Service**: http://localhost:8081
- **Performance Dashboard**: http://localhost:8081/dashboard

### **Testing**

#### **Run All Tests**
```bash
mvn test
```

#### **Run Module-Specific Tests**
```bash
# Generic API Service tests
mvn test -pl generic-api-service

# Metrics Service tests
mvn test -pl metrics-service

# Integration tests
mvn test -pl integration-tests
```

#### **Run Specific Test Classes**
```bash
mvn test -Dtest=GenericApiApplicationTest
mvn test -Dtest=ConfigurationIntegrationTest
```

## 📡 **Sample API Calls**

### **Generic API Service** (Port 8080)

#### **Stock Trades API**
```bash
# Get all stock trades with pagination
curl "http://localhost:8080/api/generic/stock-trades?page=1&size=10"

# Get stock trade by ID
curl "http://localhost:8080/api/generic/stock-trades/1"

# Get stock trades by symbol
curl "http://localhost:8080/api/generic/stock-trades/symbol/AAPL?page=1&size=5"

# Get stock trades by trader
curl "http://localhost:8080/api/generic/stock-trades/trader/TRADER001?page=1&size=10"

# Get stock trades by date range
curl "http://localhost:8080/api/generic/stock-trades/date-range?start_date=2024-01-01&end_date=2024-12-31&page=1&size=20"

# Async operations
curl "http://localhost:8080/api/generic/stock-trades?async=true&page=1&size=10"
```

#### **Configuration Management**
```bash
# Validate all configurations
curl "http://localhost:8080/api/generic/config/validate"

# List all endpoints
curl "http://localhost:8080/api/generic/config/endpoints"

# List all queries
curl "http://localhost:8080/api/generic/config/queries"

# Health check
curl "http://localhost:8080/api/health"
```

### **Metrics Service** (Port 8081)

#### **Performance Metrics**
```bash
# Get performance metrics
curl "http://localhost:8081/api/performance-metrics?page=1&size=10"

# Get performance summary
curl "http://localhost:8081/api/performance-metrics/summary"

# Get real-time endpoint metrics
curl "http://localhost:8081/api/metrics/endpoints"

# Get system metrics
curl "http://localhost:8081/api/metrics/system"
```

## 🗄️ **Database Architecture**

The system uses **separate H2 file-based databases** for different concerns:

### **Application Data Database**
- **Location**: `./data/stocktrades.mv.db`
- **Purpose**: Stock trades and business data
- **Schema**: Automatically created on startup

```sql
CREATE TABLE stock_trades (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(10) NOT NULL,
    trade_type VARCHAR(4) NOT NULL CHECK (trade_type IN ('BUY', 'SELL')),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    price DECIMAL(10,2) NOT NULL CHECK (price > 0),
    total_value DECIMAL(15,2) NOT NULL,
    trade_date_time TIMESTAMP NOT NULL,
    trader_id VARCHAR(50) NOT NULL,
    exchange VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### **Metrics Database**
- **Location**: `./data/metrics.mv.db`
- **Purpose**: Performance metrics and monitoring data
- **Schema**: Automatically created by metrics service

```sql
CREATE TABLE performance_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_name VARCHAR(255) NOT NULL,
    test_type VARCHAR(50) NOT NULL,
    response_time_ms BIGINT NOT NULL,
    success BOOLEAN NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    additional_data TEXT
);
```

## 📊 **Monitoring & Observability**

### **Automatic Metrics Collection**
- ✅ **Response Times** - Automatic tracking for all endpoints
- ✅ **Success Rates** - HTTP status code monitoring
- ✅ **Memory Usage** - Optional memory consumption tracking
- ✅ **Request Counts** - Per-endpoint request statistics

### **Performance Dashboard**
- 📈 **Real-time Charts** - Live performance visualization
- 📊 **Historical Trends** - Performance over time analysis
- 🎯 **Endpoint Analytics** - Per-endpoint performance breakdown
- 🔍 **System Metrics** - Memory, CPU, and system health

### **Grafana Integration**
- 📡 **Prometheus Metrics** - Export metrics to Prometheus
- 📊 **Custom Dashboards** - Grafana dashboard templates
- 🚨 **Alerting** - Performance threshold monitoring

## 🛠️ **Technologies Used**

### **Core Framework**
- **Javalin 6.1.3** - Lightweight web framework
- **Google Guice 7.0.0** - Dependency injection
- **Jackson 2.17.1** - JSON serialization
- **SnakeYAML 2.2** - YAML configuration

### **Database & Persistence**
- **H2 Database 2.2.224** - Embedded database
- **HikariCP 5.1.0** - Connection pooling

### **Testing & Quality**
- **JUnit 5.10.2** - Testing framework
- **Logback 1.5.6** - Logging framework

### **Documentation & Monitoring**
- **Swagger/OpenAPI** - API documentation
- **Custom Performance Dashboard** - Built-in monitoring
- **Prometheus Integration** - Metrics export

## 📚 **Additional Documentation**

- [Architecture Guide](ARCHITECTURE_GUIDE.md) - Detailed system architecture
- [Configuration Schema Reference](CONFIGURATION_SCHEMA_REFERENCE.md) - Complete YAML configuration guide
- [Common Library Implementation](COMMON_LIBRARY_IMPLEMENTATION.md) - Shared utilities guide
- [Metrics Collection Architecture](METRICS_COLLECTION_ARCHITECTURE.md) - Performance monitoring details
- [Metrics Collection Implementation](METRICS_COLLECTION_IMPLEMENTATION.md) - Implementation guide
- [Metrics Collection Quick Reference](METRICS_COLLECTION_QUICK_REFERENCE.md) - Quick start guide
- [Dashboard Configuration](DASHBOARD_CONFIGURATION.md) - Dashboard setup and configuration
- [Performance Dashboard](PERFORMANCE_DASHBOARD.md) - Dashboard features and usage
- [Test Documentation](TEST_DOCUMENTATION.md) - Testing guide and best practices
- [Metrics Collection Tests](METRICS_COLLECTION_TESTS.md) - Metrics testing documentation
- [Configuration Examples](javalin-api-mesh/config/) - YAML configuration samples

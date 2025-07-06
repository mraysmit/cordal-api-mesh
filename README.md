# Javalin API Mesh 😍

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
😊
## ✨ **Key Features**

### **🌐 Generic API Service**
- **YAML-Driven APIs** - Define REST endpoints without coding
- **Dynamic Query Configuration** - SQL queries externalized to YAML
- **Multi-Database Support** - Connect different endpoints to different databases
- **Swagger Integration** - Auto-generated API documentation
- **Configuration Management APIs** - Runtime configuration inspection and validation
- **Comprehensive Validation** - Configuration, database schema, and endpoint connectivity validation
- **Bootstrap Validation** - Startup validation with configurable options
- **Endpoint Testing** - HTTP connectivity testing with performance monitoring

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

#### **Dynamic API Endpoints**
The Generic API Service provides YAML or database-configured REST endpoints. Available endpoints depend on your configuration source:

**When using YAML configuration:**
- `GET /api/generic/stock-trades` - Get all stock trades with pagination
  - Query parameters: `page` (default: 1), `size` (default: 20), `async` (default: false)
- `GET /api/generic/stock-trades/{id}` - Get stock trade by ID
- `GET /api/generic/stock-trades/symbol/{symbol}` - Get stock trades by symbol with pagination
- `GET /api/generic/stock-trades/trader/{trader_id}` - Get stock trades by trader ID
- `GET /api/generic/stock-trades/date-range` - Get stock trades by date range
  - Query parameters: `start_date`, `end_date`, `page`, `size`

**When using database configuration:**
- `GET /api/config/databases` - List all database configurations
- `GET /api/config/queries` - List all query configurations
- `GET /api/config/endpoints` - List all endpoint configurations

#### **Configuration Management API**
- `GET /api/generic/config/validate` - Validate all configurations
- `GET /api/generic/config/validate/endpoints` - Validate endpoint configurations
- `GET /api/generic/config/validate/queries` - Validate query configurations
- `GET /api/generic/config/validate/databases` - Validate database configurations
- `GET /api/generic/config/validate/relationships` - Validate configuration relationships
- `GET /api/generic/config/validate/endpoint-connectivity` - Test HTTP endpoint connectivity
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

The system supports two configuration sources: **YAML files** (default) and **database storage**. This flexible approach allows you to choose the configuration method that best fits your deployment and management needs.

### **Configuration Source Selection**

Configure the source in `application.yml`:
```yaml
config:
  source: yaml      # Options: yaml, database
  paths:            # YAML file paths (used when source=yaml)
    databases: "config/stocktrades-databases.yml"
    queries: "config/stocktrades-queries.yml"
    endpoints: "config/stocktrades-api-endpoints.yml"
```

- **`yaml`** (default) - Load configurations from YAML files
- **`database`** - Load configurations from database tables

### **YAML Configuration (Traditional Approach)**

The system uses a sophisticated YAML-based configuration approach with three main configuration files:

#### **1. Database Connections** (`javalin-api-mesh/config/databases.yml`)
```yaml
databases:
  api-service-config-db:
    name: "api-service-config-db"
    description: "Main database for API service configuration data"
    url: "jdbc:h2:./data/api-service-config;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1"
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

#### **2. SQL Queries** (`javalin-api-mesh/config/queries.yml`)
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

#### **3. API Endpoints** (`javalin-api-mesh/config/api-endpoints.yml`)
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

### **Database Configuration (New Approach)**

When `config.source: database`, configurations are stored in database tables instead of YAML files. This approach provides:

- **Centralized Management** - All configurations in one database
- **Runtime Updates** - Modify configurations without file system access
- **Version Control** - Database-level configuration versioning
- **Multi-Environment** - Environment-specific configuration management

#### **Database Schema**

The system automatically creates these tables in the `api-service-config` database:

**`config_databases`** - Database connection configurations
```sql
CREATE TABLE config_databases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    url VARCHAR(500) NOT NULL,
    username VARCHAR(255),
    password VARCHAR(255),
    driver VARCHAR(255) NOT NULL,
    maximum_pool_size INTEGER DEFAULT 10,
    minimum_idle INTEGER DEFAULT 2,
    connection_timeout BIGINT DEFAULT 30000,
    idle_timeout BIGINT DEFAULT 600000,
    max_lifetime BIGINT DEFAULT 1800000,
    leak_detection_threshold BIGINT DEFAULT 60000,
    connection_test_query VARCHAR(255) DEFAULT 'SELECT 1',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**`config_queries`** - SQL query definitions
```sql
CREATE TABLE config_queries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    database_name VARCHAR(255) NOT NULL,
    sql_query TEXT NOT NULL,
    query_type VARCHAR(50) DEFAULT 'SELECT',
    timeout_seconds INTEGER DEFAULT 30,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**`config_endpoints`** - API endpoint configurations
```sql
CREATE TABLE config_endpoints (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    path VARCHAR(500) NOT NULL,
    method VARCHAR(10) NOT NULL,
    query_name VARCHAR(255) NOT NULL,
    response_format VARCHAR(50) DEFAULT 'json',
    cache_enabled BOOLEAN DEFAULT false,
    cache_ttl_seconds INTEGER DEFAULT 300,
    rate_limit_enabled BOOLEAN DEFAULT false,
    rate_limit_requests INTEGER DEFAULT 100,
    rate_limit_window_seconds INTEGER DEFAULT 60,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### **Sample Configuration Data**

When using database source, the system loads sample configurations:

**Database Configurations:**
- `api-service-config-db` - Main configuration database
- `metrics-db` - Performance metrics database

**Query Configurations:**
- `get-all-databases` - Retrieve all database configurations
- `get-all-queries` - Retrieve all query configurations
- `get-all-endpoints` - Retrieve all endpoint configurations

**Endpoint Configurations:**
- `GET /api/config/databases` - List database configurations
- `GET /api/config/queries` - List query configurations
- `GET /api/config/endpoints` - List endpoint configurations

#### **Configuration Management APIs**

When using database source, these APIs provide access to stored configurations:

- `GET /api/config/databases` - List all database configurations
- `GET /api/config/queries` - List all query configurations
- `GET /api/config/endpoints` - List all endpoint configurations

### **Configuration Benefits**

**YAML Configuration:**
- ✅ **Zero-Code API Creation** - Add new endpoints without programming
- ✅ **Multi-Database Support** - Different endpoints can use different databases
- ✅ **Automatic Validation** - Configuration integrity checks at startup
- ✅ **Runtime Inspection** - APIs to view and validate configurations
- ✅ **Environment Flexibility** - Easy configuration changes per environment
- ✅ **Version Control** - Track configuration changes in Git
- ✅ **File-Based Management** - Simple text file editing

**Database Configuration:**
- ✅ **Centralized Storage** - All configurations in one database
- ✅ **Runtime Updates** - Modify configurations without file system access
- ✅ **Multi-Environment Support** - Environment-specific configuration management
- ✅ **Database-Level Security** - Leverage database access controls
- ✅ **Audit Trail** - Track configuration changes with timestamps
- ✅ **Scalable Management** - Handle large numbers of configurations efficiently
- ✅ **API-Driven Updates** - Programmatic configuration management

## ✅ **Configuration Validation**

The system provides comprehensive configuration validation to ensure all configurations are correct before deployment:

### **Validation Modes**

#### **1. Startup Validation**
Run validation during normal application startup:
```yaml
# application.yml
validation:
  runOnStartup: true   # Enable validation on every startup
  validateOnly: false  # Continue with normal startup after validation
```

#### **2. Standalone Validation**
Run validation only and exit (perfect for CI/CD pipelines):
```yaml
# application.yml
validation:
  runOnStartup: false  # Not needed when validateOnly is true
  validateOnly: true   # Run validation and exit without starting server
```

#### **3. Command Line Validation**
Override configuration with command line arguments:
```bash
# Run validation only and exit
java -jar generic-api-service.jar --validate-only

# Alternative short form
java -jar generic-api-service.jar --validate
```

### **Validation Process**
- **Configuration Chain Validation** - Verifies endpoints → queries → databases relationships
- **Database Schema Validation** - Checks table existence, field availability, and query compatibility
- **Endpoint Connectivity Validation** - Tests HTTP endpoints with actual requests and performance monitoring
- **Detailed Error Reporting** - ASCII tables with clear, readable validation results
- **Fatal Error Handling** - Application exits with proper error codes on validation failure

### **Use Cases**
- **CI/CD Integration** - Validate configurations in deployment pipelines
- **Development Verification** - Quickly check configuration setup during development
- **Production Pre-checks** - Validate configurations before starting services
- **Troubleshooting** - Identify configuration issues without full application startup

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

- [Architecture Guide](docs/ARCHITECTURE_GUIDE.md) - Detailed system architecture
- [Configuration Schema Reference](docs/CONFIGURATION_SCHEMA_REFERENCE.md) - Complete YAML configuration guide
- [Configuration Validation](docs/CONFIGURATION_VALIDATION.md) - Configuration validation guide and best practices
- [Endpoint Validation](docs/ENDPOINT_VALIDATION.md) - HTTP endpoint connectivity testing guide
- [Bootstrap Demo](docs/BOOTSTRAP_DEMO.md) - System demonstration and validation showcase
- [Common Library Implementation](docs/COMMON_LIBRARY_IMPLEMENTATION.md) - Shared utilities guide
- [Metrics Collection Architecture](docs/METRICS_COLLECTION_ARCHITECTURE.md) - Performance monitoring details
- [Metrics Collection Implementation](docs/METRICS_COLLECTION_IMPLEMENTATION.md) - Implementation guide
- [Metrics Collection Quick Reference](docs/METRICS_COLLECTION_QUICK_REFERENCE.md) - Quick start guide
- [Dashboard Configuration](docs/DASHBOARD_CONFIGURATION.md) - Dashboard setup and configuration
- [Performance Dashboard](docs/PERFORMANCE_DASHBOARD.md) - Dashboard features and usage
- [Test Documentation](docs/TEST_DOCUMENTATION.md) - Testing guide and best practices
- [Metrics Collection Tests](docs/METRICS_COLLECTION_TESTS.md) - Metrics testing documentation
- [Configuration Examples](config/) - YAML configuration samples

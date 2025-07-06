# Javalin API Mesh - Architecture Guide

## 🏗️ **Project Architecture**

The Javalin API Mesh project follows a modular architecture with a common library providing shared utilities and patterns across all modules.

```
javalin-api-mesh/
├── common-library/          # 🔧 Shared utilities and base classes
├── generic-api-service/     # 🌐 Dynamic API service with YAML configuration
├── metrics-service/         # 📊 Performance monitoring and metrics collection
├── integration-tests/       # 🧪 Cross-module integration testing
└── pom.xml                 # 📦 Parent POM with shared dependencies
```

## 🔧 **Common Library**

The common library (`common-library`) provides shared functionality used across all modules:

### **Database Layer**
- **`BaseDatabaseManager`** - Abstract base for database operations
- **`MetricsDatabaseManager`** - Metrics-specific database implementation
- **`StockTradesDatabaseManager`** - Stock trades database implementation
- **`DatabaseConnectionFactory`** - HikariCP data source factory
- **`DatabaseConfig`** & **`PoolConfig`** - Database configuration models

### **Configuration Framework**
- **`BaseConfig`** - Abstract configuration with YAML loading
- **`ServerConfig`** - Common server configuration
- **`ConfigurationLoader`** - YAML configuration utilities

### **Models & DTOs**
- **`PerformanceMetrics`** - Performance monitoring data model
- **`PagedResponse<T>`** - Generic pagination wrapper

### **Exception Handling**
- **`ApiException`** - Common API exception with factory methods
- **`BaseGlobalExceptionHandler`** - Standardized error handling

### **Application Framework**
- **`BaseJavalinApplication`** - Abstract Javalin application base class
- **`BaseMetricsCollectionHandler`** - Metrics collection framework

## 🌐 **Generic API Service**

Dynamic REST API service that creates endpoints from YAML configuration files.

### **Key Features**
- **Dynamic Endpoint Creation** - APIs defined in YAML files
- **Database Query Configuration** - SQL queries externalized to YAML
- **Multiple Database Support** - Connect to different databases per endpoint
- **Swagger Integration** - Auto-generated API documentation
- **Configuration Management APIs** - Runtime configuration inspection
- **Comprehensive Validation** - Configuration, database schema, and endpoint connectivity validation
- **Bootstrap Validation** - Startup validation with configurable options

### **Architecture**
- **`GenericApiApplication`** - Main application (extends `BaseJavalinApplication`)
- **`GenericApiConfig`** - Configuration management (extends `BaseConfig`)
- **`ConfigurationLoader`** - YAML configuration loading
- **`SwaggerConfig`** - API documentation setup
- **`ConfigurationValidator`** - Comprehensive validation system
- **`EndpointConfigurationManager`** - Dynamic endpoint management

### **Configuration Structure**
```yaml
# Database connections
databases:
  - name: "main-db"
    url: "jdbc:h2:mem:maindb"
    # ... connection details

# SQL queries
queries:
  - name: "get-all-trades"
    database: "main-db"
    sql: "SELECT * FROM stock_trades"
    # ... query configuration

# API endpoints
endpoints:
  - path: "/api/trades"
    method: "GET"
    query: "get-all-trades"
    # ... endpoint configuration
```

### **Validation System**

Comprehensive validation framework ensuring configuration integrity:

#### **Configuration Chain Validation**
- **Endpoint → Query** - Validates endpoint references to queries
- **Query → Database** - Validates query references to databases
- **Cross-Reference Integrity** - Ensures complete configuration chains

#### **Database Schema Validation**
- **Connectivity Testing** - Verifies database connections
- **Table Existence** - Checks referenced tables exist
- **Column Validation** - Validates query column references
- **Parameter Binding** - Tests query parameter compatibility

#### **Endpoint Connectivity Validation**
- **HTTP Request Testing** - Makes actual requests to endpoints
- **Response Validation** - Checks status codes and response times
- **Parameter Substitution** - Tests with sample parameters
- **Performance Monitoring** - Measures endpoint response times

#### **Validation Modes**
```yaml
validation:
  runOnStartup: true          # Enable validation during startup
  validateOnly: false         # Run validation and continue (or exit)
  validateEndpoints: true     # Include HTTP endpoint testing
```

#### **Validation APIs**
- `GET /api/generic/config/validate` - Comprehensive validation
- `GET /api/generic/config/validate/endpoints` - Endpoint validation
- `GET /api/generic/config/validate/queries` - Query validation
- `GET /api/generic/config/validate/databases` - Database validation
- `GET /api/generic/config/validate/endpoint-connectivity` - HTTP testing

## 📊 **Metrics Service**

Performance monitoring and metrics collection service.

### **Key Features**
- **Real-time Metrics Collection** - Automatic request/response monitoring
- **Performance Analytics** - Response times, throughput, error rates
- **Custom Dashboards** - Built-in performance visualization
- **Grafana Integration** - Export metrics to Grafana/Prometheus
- **Historical Data** - Store and analyze performance trends

### **Architecture**
- **`MetricsApplication`** - Main application (extends `BaseJavalinApplication`)
- **`MetricsConfig`** - Configuration (extends `BaseConfig`, implements `MetricsCollectionConfig`)
- **`MetricsCollectionHandler`** - Real-time metrics collection
- **`PerformanceMetricsService`** - Metrics data management

### **Metrics Collection**
- **Automatic Collection** - Intercepts all HTTP requests
- **Configurable Sampling** - Control collection rate and excluded paths
- **Memory Monitoring** - Track memory usage and leaks
- **Async Processing** - Non-blocking metrics storage

## 🧪 **Integration Tests**

Comprehensive testing suite that validates cross-module functionality.

### **Test Coverage**
- **Module Communication** - Inter-service API calls
- **Database Integration** - Cross-module data consistency
- **Configuration Validation** - YAML configuration loading
- **Performance Testing** - Load testing and metrics validation
- **Error Handling** - Exception propagation and handling

## 🚀 **Getting Started**

### **Prerequisites**
- Java 21+
- Maven 3.8+

### **Build & Run**
```bash
# Build all modules
mvn clean install

# Run Generic API Service
cd generic-api-service
mvn exec:java -Dexec.mainClass="dev.mars.generic.GenericApiApplication"

# Run Metrics Service
cd metrics-service
mvn exec:java -Dexec.mainClass="dev.mars.metrics.MetricsApplication"

# Run Integration Tests
mvn test -pl integration-tests
```

### **Default Endpoints**

**Generic API Service** (Port 8080):
- `GET /api/generic/stock-trades` - Stock trades data
- `GET /api/postgres/trades` - PostgreSQL stock trades
- `GET /api/analytics/daily-volume` - Analytics endpoints
- `GET /swagger` - API documentation
- `GET /api/generic/config/validate` - Configuration validation
- `GET /api/generic/config/validate/endpoint-connectivity` - Endpoint testing
- `GET /api/management/dashboard` - Configuration management dashboard

**Metrics Service** (Port 8081):
- `GET /api/performance-metrics` - Performance data
- `GET /api/metrics/endpoints` - Real-time endpoint metrics
- `GET /dashboard` - Performance monitoring dashboard
- `GET /swagger` - API documentation

## 🔧 **Development Patterns**

### **Creating a New Service**

1. **Extend BaseJavalinApplication**:
```java
public class MyService extends BaseJavalinApplication {
    @Override
    protected Module getGuiceModule() { return new MyGuiceModule(); }
    
    @Override
    protected ServerConfig getServerConfig() { return config.getServer(); }
    
    @Override
    protected void configureRoutes() { /* Define routes */ }
    
    @Override
    protected String getApplicationName() { return "My Service"; }
}
```

2. **Extend BaseConfig**:
```java
public class MyConfig extends BaseConfig {
    @Override
    protected String getConfigFileName() { return "my-service.yml"; }
    
    // Add service-specific configuration methods
}
```

3. **Use Common Database Manager**:
```java
public class MyDatabaseManager extends BaseDatabaseManager {
    public MyDatabaseManager(DatabaseConfig config) { super(config); }
    
    @Override
    public void initializeSchema() { /* Custom schema */ }
}
```

### **Configuration Best Practices**
- Use YAML for all configuration
- Externalize database connections
- Implement comprehensive configuration validation
- Enable startup validation in production environments
- Use endpoint connectivity testing for API verification
- Provide sensible defaults
- Document configuration options
- Test configuration changes with validation APIs

### **Testing Guidelines**
- Write unit tests for common library components
- Create integration tests for cross-module functionality
- Use real databases in tests (avoid mocking when possible)
- Test configuration loading and validation
- Verify error handling and edge cases

## 📚 **Additional Resources**

- [Configuration Validation Guide](CONFIGURATION_VALIDATION.md) - Comprehensive validation system
- [Common Library Implementation Guide](COMMON_LIBRARY_IMPLEMENTATION.md)
- [Configuration Schema Reference](CONFIGURATION_SCHEMA_REFERENCE.md) - YAML configuration guide
- [Bootstrap Demo](BOOTSTRAP_DEMO.md) - System demonstration and validation
- [API Configuration Examples](generic-api-service/src/main/resources/config/)
- [Metrics Dashboard Guide](metrics-service/README.md)
- [Integration Testing Guide](integration-tests/README.md)

## 🤝 **Contributing**

1. Follow the established architecture patterns
2. Use the common library for shared functionality
3. Write comprehensive tests
4. Update documentation for new features
5. Maintain backward compatibility

# Configuration Schema Reference

This document provides a comprehensive reference for all configuration options used in the Javalin API Mesh project.

## 📋 **Configuration Sources Overview**

The system supports two configuration sources:

### **1. YAML Files** (Default)
Located in `javalin-api-mesh/config/`:
- **`databases.yml`** - Database connection configurations
- **`queries.yml`** - SQL query definitions with parameters
- **`api-endpoints.yml`** - REST API endpoint configurations

### **2. Database Tables** (New)
Stored in the `api-service-config` database:
- **`config_databases`** - Database connection configurations
- **`config_queries`** - SQL query definitions with parameters
- **`config_endpoints`** - REST API endpoint configurations

## ⚙️ **Configuration Source Selection**

Configure the source in `application.yml`:

```yaml
config:
  source: yaml      # Options: yaml, database
  paths:            # YAML file paths (used when source=yaml)
    databases: "config/databases.yml"
    queries: "config/queries.yml"
    endpoints: "config/api-endpoints.yml"
```

- **`yaml`** (default) - Load configurations from YAML files
- **`database`** - Load configurations from database tables

## 🗄️ **Database Configuration Schema**

### **YAML Configuration** (`databases.yml`)

#### **Structure**
```yaml
databases:
  <database-key>:
    name: string                    # Database identifier
    description: string             # Human-readable description
    url: string                     # JDBC connection URL
    username: string                # Database username
    password: string                # Database password
    driver: string                  # JDBC driver class name
    pool:                          # HikariCP connection pool settings
      minimumIdle: integer         # Minimum idle connections
      maximumPoolSize: integer     # Maximum pool size
      connectionTimeout: integer   # Connection timeout (ms)
      idleTimeout: integer         # Idle timeout (ms)
      maxLifetime: integer         # Maximum connection lifetime (ms)
```

### **Example**
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
      
  metrics-db:
    name: "metrics-db"
    description: "Database for performance metrics and monitoring data"
    url: "jdbc:h2:./data/metrics;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1"
    username: "sa"
    password: ""
    driver: "org.h2.Driver"
    pool:
      minimumIdle: 1
      maximumPoolSize: 5
      connectionTimeout: 30000
      idleTimeout: 600000
      maxLifetime: 1800000
```

### **Supported Database Types**
- **H2** - `org.h2.Driver`
- **MySQL** - `com.mysql.cj.jdbc.Driver`
- **PostgreSQL** - `org.postgresql.Driver`
- **SQL Server** - `com.microsoft.sqlserver.jdbc.SQLServerDriver`

## 🔍 **Query Configuration Schema** (`queries.yml`)

### **Structure**
```yaml
queries:
  <query-key>:
    name: string                    # Query identifier
    description: string             # Human-readable description
    sql: string                     # SQL query with ? placeholders
    database: string                # Reference to database key
    parameters:                     # Array of parameter definitions
      - name: string                # Parameter name
        type: string                # Parameter type (see types below)
        required: boolean           # Whether parameter is required
```

### **Parameter Types**
- `STRING` - Text values
- `INTEGER` - Whole numbers
- `LONG` - Large whole numbers
- `DECIMAL` - Decimal numbers
- `BOOLEAN` - True/false values
- `TIMESTAMP` - Date/time values

### **Example**
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
        
  stock-trades-count:
    name: "stock-trades-count"
    description: "Count all stock trades"
    sql: "SELECT COUNT(*) FROM stock_trades"
    database: "stock-trades-db"
    parameters: []
    
  stock-trades-by-symbol:
    name: "stock-trades-by-symbol"
    sql: "SELECT * FROM stock_trades WHERE symbol = ? ORDER BY trade_date_time DESC LIMIT ? OFFSET ?"
    database: "stock-trades-db"
    parameters:
      - name: "symbol"
        type: "STRING"
        required: true
      - name: "limit"
        type: "INTEGER"
        required: true
      - name: "offset"
        type: "INTEGER"
        required: true
```

## 🌐 **API Endpoint Configuration Schema** (`api-endpoints.yml`)

### **Structure**
```yaml
endpoints:
  <endpoint-key>:
    path: string                    # URL path with optional {param} placeholders
    method: string                  # HTTP method (GET, POST, PUT, DELETE)
    query: string                   # Reference to query key
    countQuery: string              # Reference to count query (for pagination)
    description: string             # Human-readable description
    pagination:                     # Pagination configuration
      enabled: boolean              # Enable pagination
      defaultSize: integer          # Default page size
      maxSize: integer              # Maximum page size
    parameters:                     # Array of parameter definitions
      - name: string                # Parameter name
        type: string                # Parameter type
        source: string              # Parameter source (QUERY, PATH, BODY)
        required: boolean           # Whether parameter is required
        defaultValue: string        # Default value if not provided
    response:                       # Response configuration
      type: string                  # Response type (SINGLE, PAGED, LIST)
      fields:                       # Array of response field definitions
        - name: string              # Field name
          type: string              # Field type
```

### **Parameter Sources**
- `QUERY` - URL query parameters (?param=value)
- `PATH` - URL path parameters ({param})
- `BODY` - Request body parameters

### **Response Types**
- `SINGLE` - Single object response
- `PAGED` - Paginated response with metadata
- `LIST` - Array of objects

### **Example**
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
      - name: "size"
        type: "INTEGER"
        source: "QUERY"
        required: false
        defaultValue: "20"
      - name: "async"
        type: "BOOLEAN"
        source: "QUERY"
        required: false
        defaultValue: "false"
    response:
      type: "PAGED"
      fields:
        - name: "id"
          type: "LONG"
        - name: "symbol"
          type: "STRING"
        - name: "quantity"
          type: "INTEGER"
        - name: "price"
          type: "DECIMAL"
        - name: "trade_date_time"
          type: "TIMESTAMP"
        - name: "trader_id"
          type: "STRING"
          
  stock-trade-by-id:
    path: "/api/generic/stock-trades/{id}"
    method: "GET"
    query: "stock-trade-by-id"
    description: "Get stock trade by ID"
    parameters:
      - name: "id"
        type: "LONG"
        source: "PATH"
        required: true
      - name: "async"
        type: "BOOLEAN"
        source: "QUERY"
        required: false
        defaultValue: "false"
    response:
      type: "SINGLE"
      fields:
        - name: "id"
          type: "LONG"
        - name: "symbol"
          type: "STRING"
        - name: "quantity"
          type: "INTEGER"
        - name: "price"
          type: "DECIMAL"
        - name: "trade_date_time"
          type: "TIMESTAMP"
        - name: "trader_id"
          type: "STRING"
```

## ⚙️ **Application Configuration Schema**

### **Generic API Service** (`generic-api-service/src/main/resources/application.yml`)
```yaml
server:
  host: string                      # Server host (default: localhost)
  port: integer                     # Server port (default: 8080)

database:
  url: string                       # Primary database URL
  username: string                  # Database username
  password: string                  # Database password
  driver: string                    # JDBC driver

swagger:
  enabled: boolean                  # Enable Swagger UI (default: true)
  path: string                      # Swagger UI path (default: /swagger)

config:
  paths:
    databases: string               # Path to databases.yml
    queries: string                 # Path to queries.yml
    endpoints: string               # Path to api-endpoints.yml

data:
  loadSampleData: boolean           # Load sample data on startup
  sampleDataSize: integer           # Number of sample records
```

### **Metrics Service** (`metrics-service/src/main/resources/application.yml`)
```yaml
server:
  host: string                      # Server host (default: localhost)
  port: integer                     # Server port (default: 8081)

metrics:
  collection:
    enabled: boolean                # Enable metrics collection
    includeMemoryMetrics: boolean   # Include memory usage metrics
    excludePaths:                   # Paths to exclude from metrics
      - string
    samplingRate: float             # Sampling rate (0.0-1.0)
    asyncSave: boolean              # Save metrics asynchronously
  database:
    url: string                     # Metrics database URL
    username: string                # Database username
    password: string                # Database password
    driver: string                  # JDBC driver
    pool:
      maximumPoolSize: integer
      minimumIdle: integer
      connectionTimeout: integer
  dashboard:
    custom:
      enabled: boolean              # Enable custom dashboard
      path: string                  # Dashboard path
    grafana:
      enabled: boolean              # Enable Grafana integration
      url: string                   # Grafana URL
      prometheus:
        enabled: boolean            # Enable Prometheus metrics
        port: integer               # Prometheus port
        path: string                # Prometheus metrics path
```

## 🔗 **Configuration Relationships**

### **Validation Rules**
1. **Endpoint → Query**: Every endpoint's `query` field must reference an existing query key
2. **Query → Database**: Every query's `database` field must reference an existing database key
3. **Count Query**: If pagination is enabled, `countQuery` must reference an existing query
4. **Parameter Consistency**: Query parameters must match endpoint parameter definitions

### **Best Practices**
- Use descriptive names for keys and descriptions
- Keep SQL queries simple and focused
- Use appropriate parameter types for validation
- Configure reasonable pagination limits
- Include proper database connection pooling
- Use environment-specific configuration files for different deployments

## 📚 **Related Documentation**
- [Architecture Guide](ARCHITECTURE_GUIDE.md)
- [Configuration Examples](config/)
- [Dashboard Configuration](DASHBOARD_CONFIGURATION.md)
- [Metrics Collection Architecture](METRICS_COLLECTION_ARCHITECTURE.md)

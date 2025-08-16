# CORDAL Quick Start Guide
## Configuration Orchestrated REST Dynamic API Layer

**Version:** 1.0
**Date:** 2025-03-05
**Author:** Mark Andrew Ray-Smith Cityline Ltd

## What is CORDAL?

CORDAL is a **generic, configuration-driven REST API framework** built on Java 21 and Javalin 6.1.3. Create dynamic REST APIs for **any domain** through YAML configuration files instead of writing code.

## Core vs. Example Architecture

### **CORDAL CORE SYSTEM** (Generic Framework)
- `cordal-api-service/` - Generic REST API framework
- `cordal-common-library/` - Shared utilities and models  
- `cordal-metrics-service/` - Performance monitoring
- `generic-config/` - Core configuration files
- `scripts/` - Build and deployment scripts

### **EXAMPLE IMPLEMENTATIONS** (Stock Trades Demo)
- `cordal-integration-tests/src/test/java/dev/cordal/integration/examples/` - Example classes
- `cordal-integration-tests/src/test/resources/config/` - Example configurations
- `cordal-integration-tests/src/test/resources/sql/` - Example SQL scripts

> **Important**: Stock trades functionality is **NOT part of the core system**. It's purely an example to demonstrate framework usage.

## Quick Start (5 Minutes)

### Prerequisites
- **Java 21+** (JDK required)
- **Maven 3.6.0+**
- **PostgreSQL** (optional, for production)

### 1. Clone and Build
```bash
# Clone the repository
git clone <repository-url>
cd cordal-api-mesh

# Build all modules
./scripts/build-executable-jars.sh
```

### 2. Start Services
```bash
# Start all services (API + Metrics)
./scripts/start-all-services.sh

# Or start individual services
./scripts/start-generic-api-service.sh
./scripts/start-metrics-service.sh
```

### 3. Verify Installation
```bash
# Check API health
curl http://localhost:8080/api/health

# View performance dashboard
open http://localhost:8080/dashboard

# Check metrics
curl http://localhost:8080/api/metrics/endpoints
```

## Configure Your Domain

### Step 1: Database Configuration
Create `generic-config/your-domain-databases.yml`:

```yaml
databases:
  your_database:
    name: "your_database"
    url: "jdbc:h2:./data/your-data;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1"
    username: "sa"
    password: ""
    driver: "org.h2.Driver"
    pool:
      maximumPoolSize: 10
      minimumIdle: 2
      connectionTimeout: 30000
```

### Step 2: Query Configuration
Create `generic-config/your-domain-queries.yml`:

```yaml
queries:
  get_all_your_entities:
    database: "your_database"
    sql: |
      SELECT id, name, description, created_date 
      FROM your_table 
      ORDER BY created_date DESC 
      LIMIT ? OFFSET ?
    parameters:
      - name: "limit"
        type: "INTEGER"
        required: true
      - name: "offset"
        type: "INTEGER"
        required: true

  count_your_entities:
    database: "your_database"
    sql: "SELECT COUNT(*) as total FROM your_table"
    parameters: []
```

### Step 3: API Endpoint Configuration
Create `generic-config/your-domain-endpoints.yml`:

```yaml
endpoints:
  your_entities_list:
    path: "/api/your-entities"
    method: "GET"
    description: "Get all your entities with pagination"
    query: "get_all_your_entities"
    countQuery: "count_your_entities"
    pagination:
      enabled: true
      defaultSize: 20
      maxSize: 100
    parameters:
      - name: "page"
        type: "INTEGER"
        required: false
        defaultValue: 0
        description: "Page number (0-based)"
      - name: "size"
        type: "INTEGER"
        required: false
        defaultValue: 20
        description: "Page size"
    response:
      type: "PAGED"
```

### Step 4: Restart and Test
```bash
# Restart the API service to load new configuration
./scripts/start-generic-api-service.sh

# Test your new endpoint
curl "http://localhost:8080/api/your-entities?page=0&size=10"
```

## Key Features

### Automatic Metrics Collection
- **Zero-code monitoring** of all API requests
- **Real-time performance dashboard** at `/dashboard`
- **Response time tracking**, success rates, memory usage
- **Configurable sampling** for production environments

### Configuration Validation
```bash
# Validate configuration without starting
./scripts/start-generic-api-service.sh --validate-only

# Test validation features
./scripts/test-validation-flags.sh
```

### Multiple Database Support
- **H2** (embedded, perfect for development)
- **PostgreSQL** (production-ready)
- **Connection pooling** with HikariCP
- **Multiple databases** in single application

### Health Monitoring
```bash
# Basic health check
curl http://localhost:8080/api/health

# Kubernetes-ready probes
curl http://localhost:8080/api/management/liveness
curl http://localhost:8080/api/management/readiness
```

## Common Use Cases

### 1. Simple CRUD API
```yaml
# Database: your-crud-databases.yml
databases:
  crud_db:
    url: "jdbc:h2:./data/crud"
    # ... connection details

# Queries: your-crud-queries.yml  
queries:
  get_items: 
    sql: "SELECT * FROM items ORDER BY id LIMIT ? OFFSET ?"
  create_item:
    sql: "INSERT INTO items (name, description) VALUES (?, ?)"

# Endpoints: your-crud-endpoints.yml
endpoints:
  list_items:
    path: "/api/items"
    method: "GET"
    query: "get_items"
    pagination:
      enabled: true
```

### 2. Analytics Dashboard Backend
```yaml
# Queries for analytics
queries:
  daily_metrics:
    sql: |
      SELECT DATE(created_at) as date, COUNT(*) as count 
      FROM events 
      WHERE created_at >= ? 
      GROUP BY DATE(created_at)
      
# Endpoint for charts
endpoints:
  analytics_daily:
    path: "/api/analytics/daily"
    method: "GET"
    query: "daily_metrics"
```

### 3. Multi-tenant Application
```yaml
# Tenant-specific queries
queries:
  tenant_data:
    sql: |
      SELECT * FROM tenant_table 
      WHERE tenant_id = ? 
      ORDER BY created_date DESC

# Tenant endpoint
endpoints:
  tenant_api:
    path: "/api/tenant/{tenant_id}/data"
    method: "GET"
    query: "tenant_data"
    parameters:
      - name: "tenant_id"
        type: "STRING"
        source: "PATH"
        required: true
```

## Next Steps

1. **Explore Examples**: Check `cordal-integration-tests/` for comprehensive examples
2. **Read Full Guide**: See `docs/CORDAL_COMPREHENSIVE_GUIDE.md` for advanced features
3. **Run Tests**: Execute `mvn test` to see integration tests in action
4. **Performance Testing**: Use the built-in performance dashboard
5. **Production Deployment**: Configure PostgreSQL and deploy with Docker/Kubernetes

## Additional Resources

- **Comprehensive Guide**: `docs/CORDAL_COMPREHENSIVE_GUIDE.md`
- **Integration Tests**: `cordal-integration-tests/`
- **Build Scripts**: `scripts/`
- **Configuration Examples**: `generic-config/`

## Troubleshooting

### Common Issues

**Configuration not loading?**
```bash
# Validate configuration
./scripts/start-generic-api-service.sh --validate-only
```

**Database connection issues?**
```bash
# Check H2 console
./scripts/h2-console.sh
# Access at: http://localhost:8082
```

**Performance issues?**
```bash
# Check metrics dashboard
open http://localhost:8080/dashboard
```

**Build issues?**
```bash
# Clean build
mvn clean install
./scripts/build-executable-jars.sh --dev
```



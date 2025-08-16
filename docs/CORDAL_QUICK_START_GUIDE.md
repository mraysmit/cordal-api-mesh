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

## System Architecture & Configuration

### Understanding CORDAL's Configuration System

CORDAL uses a **hierarchical configuration system** where the main `application.yml` file serves as the **entry point** that defines how and where domain-specific configurations are discovered and loaded.

#### Configuration Flow
```
application.yml (Entry Point)
    ↓ defines directories & patterns
generic-config/ directory scanning
    ↓ discovers files matching patterns
Domain Configuration Files
    ├── *-databases.yml    (Database connections)
    ├── *-queries.yml      (SQL queries)
    └── *-endpoints.yml    (API endpoints)
```

#### Complete Main Application Configuration Example

Here's a **complete, production-ready example** of the `application.yml` file:

**File**: `cordal-api-service/src/main/resources/application.yml`

```yaml
# =============================================================================
# CORDAL API Service - Main Configuration File
# This is the PRIMARY ENTRY POINT that controls the entire system
# =============================================================================

# Application Identity
application:
  name: cordal-api-service

# HTTP Server Configuration
server:
  host: localhost                    # Use 0.0.0.0 for external access
  port: 8080                        # Main API port

# System Database Configuration (for CORDAL's internal use)
# This is separate from your domain databases defined in generic-config/
database:
  # File-based H2 database (recommended for development)
  url: jdbc:h2:../data/api-service-config;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1
  # For production PostgreSQL, use:
  # url: jdbc:postgresql://localhost:5432/cordal_config
  username: sa
  password: ""
  driver: org.h2.Driver
  createIfMissing: true              # Create database if it doesn't exist

  # Connection pool settings (optional)
  pool:
    maximumPoolSize: 10
    minimumIdle: 2
    connectionTimeout: 30000
    idleTimeout: 600000
    maxLifetime: 1800000

# API Documentation (Swagger/OpenAPI)
swagger:
  enabled: true                      # Enable Swagger UI
  path: /swagger                     # Access at http://localhost:8080/swagger

# Intelligent Caching System
cache:
  enabled: true                      # Enable query result caching
  defaultTtlSeconds: 300            # 5 minutes default cache TTL
  maxSize: 1000                     # Maximum entries per cache
  cleanupIntervalSeconds: 60        # Cache cleanup interval

# =============================================================================
# CONFIGURATION DISCOVERY - This is where CORDAL finds your domain configs
# =============================================================================
config:
  source: yaml                      # Options: yaml, database
  loadFromYaml: false              # Set true to populate database from YAML files

  # WHERE to scan for domain configuration files
  directories:
    - "generic-config"              # For IDE execution from project root
    - "../generic-config"           # For command-line execution from service directory
    # Add more directories as needed:
    # - "/etc/cordal/config"        # System-wide configuration
    # - "./custom-config"           # Custom configuration directory

  # WHAT files to look for (glob patterns)
  patterns:
    databases: ["*-database.yml", "*-databases.yml"]
    queries: ["*-query.yml", "*-queries.yml"]
    endpoints: ["*-endpoint.yml", "*-endpoints.yml", "*-api.yml"]

  # Hot Reload Configuration (Advanced Feature)
  hotReload:
    enabled: false                  # Set true to enable dynamic config updates
    watchDirectories: true          # Monitor configuration directories
    debounceMs: 300                # Delay before applying changes
    maxReloadAttempts: 3           # Retry limit for failed reloads
    rollbackOnFailure: true        # Auto-rollback on validation failure
    validateBeforeApply: true      # Validate before applying changes

  # File System Monitoring
  fileWatcher:
    enabled: true                   # Enable file system monitoring
    pollInterval: 1000             # Fallback polling interval (ms)
    includeSubdirectories: false   # Monitor subdirectories

# =============================================================================
# CONFIGURATION VALIDATION SYSTEM
# =============================================================================
validation:
  # Run validation during normal application startup
  runOnStartup: true               # Validate configurations on every startup

  # Run only validation and exit (for CI/CD pipelines)
  validateOnly: false              # Set true to run validation checks and exit

  # Include endpoint connectivity testing in validation
  validateEndpoints: false         # Set true to test HTTP endpoint accessibility
                                  # (slower startup but more thorough validation)

# =============================================================================
# DEVELOPMENT & DEBUGGING OPTIONS
# =============================================================================
# Uncomment and modify these sections for development/debugging

# CORS Configuration (for web frontend development)
# server:
#   cors:
#     enabled: true
#     allowedOrigins: ["http://localhost:3000", "http://localhost:8080"]
#     allowedMethods: ["GET", "POST", "PUT", "DELETE", "OPTIONS"]
#     allowedHeaders: ["*"]

# Development Logging
# logging:
#   level:
#     dev.cordal: DEBUG              # Enable debug logging for CORDAL components
#     dev.cordal.config: TRACE       # Detailed configuration loading logs
#     dev.cordal.generic: DEBUG      # Generic API service debug logs

# Request/Response Logging (for debugging)
# server:
#   dev:
#     logging: true                  # Enable request/response logging
#     requestLogging: true           # Log all incoming requests

# =============================================================================
# PRODUCTION CONFIGURATION EXAMPLES
# =============================================================================
# For production deployment, consider these settings:

# Production Database (PostgreSQL example)
# database:
#   url: jdbc:postgresql://db-server:5432/cordal_config
#   username: ${DB_USERNAME}         # Use environment variables
#   password: ${DB_PASSWORD}
#   driver: org.postgresql.Driver
#   pool:
#     maximumPoolSize: 20
#     minimumIdle: 5
#     connectionTimeout: 30000

# Production Server Settings
# server:
#   host: 0.0.0.0                   # Accept connections from any IP
#   port: ${SERVER_PORT:8080}        # Use environment variable with default

# Production Caching (larger limits)
# cache:
#   enabled: true
#   defaultTtlSeconds: 1800          # 30 minutes for production
#   maxSize: 10000                   # Larger cache for production
#   cleanupIntervalSeconds: 300      # Less frequent cleanup

# Production Validation (faster startup)
# validation:
#   runOnStartup: true
#   validateOnly: false
#   validateEndpoints: false         # Skip HTTP testing for faster startup
```

This complete example shows:
- **All available configuration options** with explanations
- **Development vs Production settings** with examples
- **Environment variable usage** for sensitive data
- **Performance tuning options** for different environments
- **Security considerations** for production deployment

#### Configuration Override Hierarchy

1. **System Properties** (Highest Priority)
   ```bash
   -Dgeneric.config.file=custom-application.yml
   -Dconfig.directories=custom-config,../custom-config
   ```

2. **application.yml Settings** (Medium Priority)
   - `config.directories` - Define scan directories
   - `config.patterns.*` - Define file naming patterns

3. **Built-in Defaults** (Lowest Priority)
   - Directories: `["generic-config", "../generic-config"]`
   - Database patterns: `["*-database.yml", "*-databases.yml"]`

#### Startup Process

1. **Load Main Config**: System loads `application.yml` from classpath
2. **Directory Discovery**: Reads `config.directories` to know where to scan
3. **File Pattern Matching**: Uses `config.patterns.*` to find domain files
4. **Configuration Loading**: Loads and parses all matching YAML files
5. **Validation**: Validates configuration chain (endpoints → queries → databases)
6. **Server Start**: Starts HTTP server with dynamically generated endpoints

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

### Step 2.5: Enable Caching (Optional)
Add caching to your queries for improved performance:

```yaml
# Update your queries with cache configuration
queries:
  get_all_your_entities:
    database: "your_database"
    sql: "SELECT * FROM your_table ORDER BY id LIMIT ? OFFSET ?"
    cache:
      enabled: true
      ttl: 300  # Cache for 5 minutes
      maxSize: 1000
    parameters:
      - name: "limit"
        type: "INTEGER"
        required: true
      - name: "offset"
        type: "INTEGER"
        required: true
```

**Cache Benefits:**
- **25x faster response times** for cached queries
- **Reduced database load** for frequently accessed data
- **Automatic cache management** with TTL expiration

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

# Test cache performance
curl "http://localhost:8080/api/your-entities?page=0&size=10"  # First request (cache miss)
curl "http://localhost:8080/api/your-entities?page=0&size=10"  # Second request (cache hit - much faster!)
```

### Step 5: Cache Management (Optional)
Monitor and manage your cache:

```bash
# View cache statistics
curl "http://localhost:8080/api/cache/stats"

# Clear specific cache entries
curl -X DELETE "http://localhost:8080/api/cache/clear?pattern=your_entities*"

# Clear all cache
curl -X DELETE "http://localhost:8080/api/cache/clear-all"
```

## Key Features

### Intelligent Caching System
- **Automatic query result caching** with configurable TTL (Time-To-Live)
- **Performance optimization** - cache hits can be 25x faster than database queries
- **Cache management APIs** for monitoring, invalidation, and statistics
- **Zero-code integration** - enable caching through YAML configuration

### Automatic Metrics Collection
- **Zero-code monitoring** of all API requests
- **Real-time performance dashboard** at `/dashboard`
- **Response time tracking**, success rates, memory usage
- **Configurable sampling** for production environments

### Configuration Validation

CORDAL includes a comprehensive **multi-stage validation system**:

#### Validation Stages
1. **Syntax Validation** - YAML syntax and structure
2. **Dependency Validation** - Endpoints → Queries → Databases chain
3. **Schema Validation** - SQL queries against database schemas
4. **Connectivity Validation** - HTTP endpoint testing

#### Validation Commands
```bash
# Validate configuration without starting server
./scripts/start-generic-api-service.sh --validate-only

# Test validation features
./scripts/test-validation-flags.sh

# Validate with endpoint connectivity testing
# (Set validation.validateEndpoints=true in application.yml)
```

#### Validation Configuration
Control validation behavior in `application.yml`:
```yaml
validation:
  runOnStartup: true        # Validate on every startup
  validateOnly: false       # Set true for validation-only mode
  validateEndpoints: false  # Test actual HTTP connectivity
```

#### Common Validation Errors
- **Missing Dependencies**: Endpoint references non-existent query
- **Database Connection**: Invalid database configuration
- **SQL Syntax**: Malformed SQL in query definitions
- **Parameter Mismatch**: Endpoint parameters don't match query parameters

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



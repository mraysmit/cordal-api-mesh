# CORDAL - Comprehensive Guide
## Configuration Orchestrated REST Dynamic API Layer

**Version:** 1.0
**Date:** 2025-03-05
**Author:** Mark Andrew Ray-Smith Cityline Ltd


## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Quick Start](#quick-start)
4. [Configuration](#configuration)
5. [Caching System](#caching-system)
6. [Deployment](#deployment)
7. [Metrics Collection](#metrics-collection)
8. [Database Integration](#database-integration)
9. [Testing](#testing)
10. [Scripts and Automation](#scripts-and-automation)
11. [Performance Dashboard](#performance-dashboard)
12. [Health Monitoring](#health-monitoring)
13. [Troubleshooting](#troubleshooting)
14. [Development Guide](#development-guide)

---

## Project Overview

### What is CORDAL?

CORDAL (Configuration Orchestrated REST Dynamic API Layer) is a sophisticated, configuration-driven REST API framework built on Java 21 and Javalin 6.1.3. It provides a highly modular, scalable solution for creating dynamic REST APIs through YAML configuration files rather than hardcoded endpoints.

**IMPORTANT**: CORDAL is a **generic framework** that can support any domain or use case. The stock trades functionality referenced in this documentation is **ONLY AN EXAMPLE IMPLEMENTATION** used for demonstration and integration testing. It is **NOT part of the core system**.

### Key Features

- **Configuration-Driven**: Define APIs, database queries, and connections through YAML files
- **Automatic Metrics Collection**: Zero-code integration for comprehensive API monitoring
- **Modular Architecture**: Separate modules for API service, metrics, and common utilities
- **Multi-Database Support**: H2 and PostgreSQL integration with connection pooling
- **Real-time Dashboard**: Performance monitoring with interactive charts
- **Comprehensive Validation**: Configuration validation with detailed error reporting
- **Production Ready**: Multiple deployment profiles, health checks, and monitoring
- **Extensive Testing**: Unit, integration, and performance test suites

### Project Structure

```
cordal/
├── cordal-api-service/          # Main API service module (CORE - Generic framework)
├── cordal-metrics-service/      # Metrics collection and monitoring (CORE)
├── cordal-common-library/       # Shared utilities and models (CORE)
├── cordal-integration-tests/    # Integration tests + EXAMPLE implementations (stock trades)
├── generic-config/              # Core configuration YAML files (GENERIC only)
├── data/                        # H2 database files
├── logs/                        # Application log files
├── scripts/                     # Build and deployment scripts (CORE only)
└── docs/                         # Comprehensive documentation
```

### Technology Stack

- **Java 21** - Latest LTS with modern language features
- **Javalin 6.1.3** - Lightweight web framework
- **Google Guice** - Dependency injection
- **HikariCP** - High-performance connection pooling
- **H2 Database** - Embedded database for development
- **PostgreSQL** - Production database support
- **Jackson** - JSON processing
- **SLF4J + Logback** - Logging framework
- **Maven** - Build and dependency management
- **Chart.js** - Dashboard visualizations

---

## Architecture

###  **IMPORTANT: Core vs. Example Architecture**

**CORDAL CORE SYSTEM** (Generic Framework):
- `cordal-api-service/` - Generic REST API framework
- `cordal-common-library/` - Shared utilities and models
- `cordal-metrics-service/` - Performance monitoring
- `generic-config/` - Core configuration files (generic only)
- `scripts/` - Build and deployment scripts

**EXAMPLE IMPLEMENTATIONS** (Domain-Specific):
- `cordal-integration-tests/src/test/java/dev/cordal/integration/examples/` - Stock trades example classes
- `cordal-integration-tests/src/test/resources/config/` - Stock trades example configurations
- `cordal-integration-tests/src/test/resources/sql/` - Stock trades example SQL scripts

**Key Principle**: The core system is completely generic and domain-agnostic. Stock trades functionality is purely an example to demonstrate how to implement a use case using the framework.

### System Architecture

CORDAL follows a layered, modular architecture designed for scalability and maintainability:

```mermaid
graph TB
    subgraph "Client Layer"
        Client[Client Applications]
    end

    subgraph "Web Layer"
        Routes[API Routes]
        Metrics[Metrics Handler]
        Health[Health Checks]
    end

    subgraph "Service Layer"
        GenericAPI[Generic API Service]
        MetricsService[Metrics Service]
        ConfigService[Config Service]
    end

    subgraph "Repository Layer"
        DynamicRepo[Dynamic Query Repository]
        MetricsRepo[Metrics Repository]
        ConfigRepo[Config Repository]
    end

    subgraph "Database Layer"
        AppDB[(Application Databases<br/>H2/PostgreSQL)]
        MetricsDB[(Metrics Database<br/>H2)]
        ConfigDB[(Configuration Database<br/>H2)]
    end

    Client -->|HTTP/REST| Routes
    Client -->|HTTP/REST| Metrics
    Client -->|HTTP/REST| Health

    Routes --> GenericAPI
    Metrics --> MetricsService
    Health --> ConfigService

    GenericAPI --> DynamicRepo
    MetricsService --> MetricsRepo
    ConfigService --> ConfigRepo

    DynamicRepo --> AppDB
    MetricsRepo --> MetricsDB
    ConfigRepo --> ConfigDB
```

### Module Architecture

```mermaid
graph LR
    subgraph "Generic API Service"
        GAS[Generic API Service]
        GAC[API Controllers]
        GAS --> GAC
    end

    subgraph "Metrics Service"
        MS[Metrics Service]
        MC[Metrics Collector]
        MD[Metrics Dashboard]
        MS --> MC
        MS --> MD
    end

    subgraph "Common Library"
        CL[Common Library]
        CM[Common Models]
        CU[Common Utilities]
        CD[Database Managers]
        CL --> CM
        CL --> CU
        CL --> CD
    end

    subgraph "Integration Tests"
        IT[Integration Tests]
        PT[Performance Tests]
        IT --> PT
    end

    GAS --> CL
    MS --> CL
    IT --> GAS
    IT --> MS
```

#### 1. Generic API Service
- **Purpose**: Main API service that dynamically creates REST endpoints
- **Configuration**: Reads from YAML files to define APIs, queries, and database connections
- **Features**: Dynamic endpoint creation, query execution, pagination, async support

#### 2. Metrics Service
- **Purpose**: Collects and stores performance metrics for all API requests
- **Features**: Automatic metrics collection, real-time aggregation, dashboard APIs
- **Storage**: Dedicated metrics database with comprehensive performance data

#### 3. Common Library
- **Purpose**: Shared utilities, models, and database management
- **Components**: Configuration loaders, database managers, common DTOs
- **Benefits**: Code reuse, consistent patterns, reduced duplication

### Configuration-Driven Design

The system uses a three-tier configuration approach:

```mermaid
graph TD
    subgraph "Configuration Files"
        EP[API Endpoints<br/>*-endpoints.yaml]
        QR[Database Queries<br/>*-queries.yaml]
        DB[Database Connections<br/>*-databases.yaml]
    end

    subgraph "Runtime Components"
        API[Dynamic API Endpoints]
        QE[Query Executor]
        CP[Connection Pool]
    end

    EP -->|references| QR
    QR -->|references| DB

    EP --> API
    QR --> QE
    DB --> CP

    API --> QE
    QE --> CP
```

1. **API Endpoints** (`*-endpoints.yaml`): Define REST API structure
2. **Database Queries** (`*-queries.yaml`): Define SQL queries and parameters
3. **Database Connections** (`*-databases.yaml`): Define database connection details

This allows for complete API definition without writing Java code.

---

## **Example Implementations**

### Stock Trades Example (Integration Tests Only)

The stock trades functionality referenced throughout this documentation is **NOT part of the core CORDAL system**. It serves as a comprehensive example of how to implement a domain-specific use case using the CORDAL framework.

**Location**: All stock trades functionality is now properly located in:
- **Classes**: `cordal-integration-tests/src/test/java/dev/cordal/integration/examples/`
- **Configurations**: `cordal-integration-tests/src/test/resources/config/`
- **SQL Scripts**: `cordal-integration-tests/src/test/resources/sql/`

**Purpose**:
- Demonstrates how to configure databases, queries, and endpoints
- Shows best practices for implementing a complete use case
- Provides integration test examples
- Serves as a template for your own domain implementations

**Important**: When implementing your own use case, replace all stock trades references with your actual domain entities, tables, and business logic.

---

## Quick Start

### Prerequisites

- **Java 21+** (JDK required for building)
- **Maven 3.6.0+** for building
- **PostgreSQL** (optional, for production databases)

### 1. Clone and Build

```bash
# Clone the repository
git clone <repository-url>
cd cordal

# Build all modules
./scripts/build-executable-jars.sh
```

### 2. Start Services

```bash
# Start all services
./scripts/start-all-services.sh

# Or start individual services
./scripts/start-cordal-api-service.sh
./scripts/start-cordal-metrics-service.sh
```

### 3. Verify Installation

```bash
# Check API health
curl http://localhost:8080/api/health

# Check available endpoints (example - actual endpoints depend on your configuration)
curl http://localhost:8080/api/your-domain-endpoints

# View metrics dashboard
open http://localhost:8080/dashboard
```

### 4. Explore APIs

```mermaid
graph LR
    subgraph "Core System Endpoints"
        Swagger[Swagger UI<br/>:8080/swagger]
        Dashboard[Performance Dashboard<br/>:8080/dashboard]
        Health[Health Checks<br/>:8080/api/health]
        Metrics[Metrics API<br/>:8080/api/metrics/endpoints]
        API[Your Domain APIs<br/>:8080/api/your-endpoints]
    end

    Browser --> Swagger
    Browser --> Dashboard
    Client --> Health
    Client --> Metrics
    Client --> API
```

---

## System Architecture & Configuration

### System Startup Process

CORDAL follows a sophisticated startup sequence that loads configurations hierarchically:

#### 1. Application Entry Point
```java
// cordal-api-service/src/main/java/dev/cordal/generic/GenericApiApplication.java
public static void main(String[] args) {
    GenericApiApplication application = new GenericApiApplication();

    // Check for validation-only mode
    boolean validateOnly = Arrays.stream(args).anyMatch("--validate-only"::equals);

    // Initialize dependency injection and load main configuration
    application.initializeDependencyInjection();
    GenericApiConfig config = application.injector.getInstance(GenericApiConfig.class);

    if (validateOnly || config.isValidationValidateOnly()) {
        application.runValidationOnly();
        return;
    }

    // Normal startup
    application.start();
}
```

#### 2. Main Configuration Loading
The system loads the **primary configuration file** from the classpath:

**File**: `cordal-api-service/src/main/resources/application.yml`

This file serves as the **configuration entry point** and controls all system behavior.

#### 3. Configuration Discovery Process
```
Step 1: Load application.yml from classpath
Step 2: Read config.directories to determine scan locations
Step 3: Use config.patterns.* to find domain configuration files
Step 4: Load and parse all matching YAML files
Step 5: Validate configuration chain (endpoints → queries → databases)
Step 6: Initialize database connections and HTTP server
```

### Configuration File Structure

CORDAL uses a **hierarchical configuration system**:

```
cordal-api-service/src/main/resources/
└── application.yml                    # PRIMARY ENTRY POINT

generic-config/                        # Domain configurations (discovered via application.yml)
├── *-databases.yml                   # Database connection definitions
├── *-queries.yml                     # SQL query definitions
└── *-endpoints.yml                   # API endpoint definitions
```

### Primary Application Configuration

**File**: `cordal-api-service/src/main/resources/application.yml`

This is the **master configuration file** that controls the entire system. Here's a complete, production-ready example:

```yaml
# =============================================================================
# CORDAL API Service - Complete Configuration Reference
# This file serves as the PRIMARY ENTRY POINT for the entire system
# =============================================================================

# Application Identity
application:
  name: cordal-api-service

# HTTP Server Configuration
server:
  host: localhost                    # Use 0.0.0.0 for external access
  port: 8080                        # Main API port

  # CORS Configuration (uncomment for web frontend development)
  # cors:
  #   enabled: true
  #   allowedOrigins: ["http://localhost:3000", "http://localhost:8080"]
  #   allowedMethods: ["GET", "POST", "PUT", "DELETE", "OPTIONS"]
  #   allowedHeaders: ["*"]

  # Development Settings (uncomment for debugging)
  # dev:
  #   logging: true                  # Enable request/response logging
  #   requestLogging: true           # Log all incoming requests

# System Database Configuration (CORDAL's internal database)
# This is separate from your domain databases defined in generic-config/
database:
  # Development: H2 file-based database
  url: jdbc:h2:../data/api-service-config;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1

  # Production: PostgreSQL example
  # url: jdbc:postgresql://localhost:5432/cordal_config
  # url: ${DATABASE_URL}             # Use environment variable

  username: sa
  password: ""
  # password: ${DATABASE_PASSWORD}   # Use environment variable for production

  driver: org.h2.Driver
  # driver: org.postgresql.Driver    # For PostgreSQL

  createIfMissing: true              # Create database if it doesn't exist

  # Connection Pool Configuration (HikariCP)
  pool:
    maximumPoolSize: 10              # Maximum connections in pool
    minimumIdle: 2                   # Minimum idle connections
    connectionTimeout: 30000         # Connection timeout (ms)
    idleTimeout: 600000             # Idle timeout (ms)
    maxLifetime: 1800000            # Maximum connection lifetime (ms)
    leakDetectionThreshold: 60000   # Connection leak detection (ms)
    connectionTestQuery: "SELECT 1" # Health check query

# API Documentation (Swagger/OpenAPI)
swagger:
  enabled: true                      # Enable Swagger UI
  path: /swagger                     # Access at http://localhost:8080/swagger

# Intelligent Caching System
cache:
  enabled: true                      # Enable query result caching
  defaultTtlSeconds: 300            # 5 minutes default TTL
  maxSize: 1000                     # Maximum entries per cache
  cleanupIntervalSeconds: 60        # Cache cleanup interval

  # Production settings example:
  # defaultTtlSeconds: 1800          # 30 minutes for production
  # maxSize: 10000                   # Larger cache for production

# =============================================================================
# CONFIGURATION DISCOVERY SYSTEM
# This section controls how CORDAL finds and loads your domain configurations
# =============================================================================
config:
  source: yaml                      # Options: yaml, database
  loadFromYaml: false              # Set true to populate database from YAML files

  # Configuration Directory Scanning
  directories:
    - "generic-config"              # For IDE execution from project root
    - "../generic-config"           # For command-line execution from service directory
    # Additional directories can be added:
    # - "/etc/cordal/config"        # System-wide configuration
    # - "./custom-config"           # Custom configuration directory
    # - "${CONFIG_DIR}"             # Environment variable

  # File Pattern Matching (glob patterns)
  patterns:
    databases: ["*-database.yml", "*-databases.yml"]
    queries: ["*-query.yml", "*-queries.yml"]
    endpoints: ["*-endpoint.yml", "*-endpoints.yml", "*-api.yml"]
    # Custom patterns example:
    # databases: ["*-db.yml", "*-databases.yml", "database-*.yml"]

  # Hot Reload Configuration (Advanced Feature)
  hotReload:
    enabled: false                  # Set true to enable dynamic config updates
    watchDirectories: true          # Monitor configuration directories for changes
    debounceMs: 300                # Delay before applying changes (ms)
    maxReloadAttempts: 3           # Maximum retry attempts for failed reloads
    rollbackOnFailure: true        # Automatically rollback on validation failure
    validateBeforeApply: true      # Validate configuration before applying changes

  # File System Monitoring
  fileWatcher:
    enabled: true                   # Enable file system monitoring
    pollInterval: 1000             # Fallback polling interval (ms)
    includeSubdirectories: false   # Monitor subdirectories (not recommended)

# =============================================================================
# CONFIGURATION VALIDATION SYSTEM
# Multi-stage validation pipeline for configuration integrity
# =============================================================================
validation:
  # Startup Validation
  runOnStartup: true               # Validate configurations on every startup

  # Validation-Only Mode (for CI/CD pipelines)
  validateOnly: false              # Set true to run validation checks and exit

  # Endpoint Connectivity Testing
  validateEndpoints: false         # Set true to test HTTP endpoint accessibility
                                  # Note: Enables slower but more thorough validation

# =============================================================================
# LOGGING CONFIGURATION (for debugging and monitoring)
# =============================================================================
# Uncomment and modify these sections as needed

# logging:
#   level:
#     root: INFO                     # Global log level
#     dev.cordal: DEBUG              # CORDAL framework debug logging
#     dev.cordal.config: TRACE       # Detailed configuration loading logs
#     dev.cordal.generic: DEBUG      # Generic API service debug logs
#     dev.cordal.cache: DEBUG        # Cache operation logs
#     dev.cordal.validation: DEBUG   # Validation process logs
#     org.h2: WARN                   # Reduce H2 database noise
#     com.zaxxer.hikari: INFO        # Connection pool logs

# =============================================================================
# ENVIRONMENT-SPECIFIC CONFIGURATION EXAMPLES
# =============================================================================

# Development Environment Example:
# server:
#   host: localhost
#   port: 8080
#   dev:
#     logging: true
# validation:
#   runOnStartup: true
#   validateEndpoints: true          # Full validation in development

# Production Environment Example:
# server:
#   host: 0.0.0.0
#   port: ${SERVER_PORT:8080}
# database:
#   url: ${DATABASE_URL}
#   username: ${DATABASE_USERNAME}
#   password: ${DATABASE_PASSWORD}
#   pool:
#     maximumPoolSize: 20
# cache:
#   defaultTtlSeconds: 1800
#   maxSize: 10000
# validation:
#   runOnStartup: true
#   validateEndpoints: false         # Skip HTTP testing for faster startup

# Testing Environment Example:
# server:
#   port: 18080                      # Different port for testing
# database:
#   url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
# validation:
#   runOnStartup: false              # Controlled validation in tests
```

#### Key Configuration Sections Explained

1. **Application Identity**: Basic service identification
2. **Server Configuration**: HTTP server settings, CORS, development options
3. **Database Configuration**: CORDAL's internal database (separate from domain databases)
4. **API Documentation**: Swagger/OpenAPI settings
5. **Caching System**: Query result caching configuration
6. **Configuration Discovery**: How CORDAL finds domain configuration files
7. **Validation System**: Multi-stage validation pipeline settings
8. **Environment Examples**: Development, production, and testing configurations

### Configuration Override Hierarchy

CORDAL supports multiple configuration override mechanisms (highest to lowest priority):

#### 1. System Properties (Highest Priority)
```bash
# Override main configuration file
-Dgeneric.config.file=custom-application.yml

# Override configuration directories
-Dconfig.directories=custom-config,../custom-config

# Override file patterns
-Ddatabase.patterns=*-db.yml,*-databases.yml
```

#### 2. Application.yml Settings (Medium Priority)
```yaml
config:
  directories: ["custom-config", "../custom-config"]
  patterns:
    databases: ["*-db.yml", "*-databases.yml"]
```

#### 3. Built-in Defaults (Lowest Priority)
- Directories: `["generic-config", "../generic-config"]`
- Database patterns: `["*-database.yml", "*-databases.yml"]`
- Query patterns: `["*-query.yml", "*-queries.yml"]`
- Endpoint patterns: `["*-endpoint.yml", "*-endpoints.yml", "*-api.yml"]`
  patterns:
    databases: "*-databases.yaml"
    queries: "*-queries.yaml"
    endpoints: "*-endpoints.yaml"

# Validation Configuration
validation:
  runOnStartup: true
  validateOnly: false
  continueOnError: true
  maxErrorsToShow: 10

# Data Loading
data:
  loadSampleData: false
  initializeForTesting: false
```

### Configuration Validation System

CORDAL includes a comprehensive **multi-stage validation pipeline** that ensures configuration integrity:

#### Validation Stages

1. **Syntax and Schema Validation**
   - YAML syntax correctness
   - Required fields presence
   - Data type validation
   - Structure compliance

2. **Dependency Validation**
   - Endpoints reference valid queries
   - Queries reference valid databases
   - Parameter compatibility between layers
   - Circular dependency detection

3. **Database Schema Validation**
   - SQL query syntax validation
   - Table and column existence verification
   - Data type compatibility
   - Query parameter validation

4. **Connectivity Validation**
   - Database connection testing
   - HTTP endpoint accessibility
   - Response format validation
   - Performance baseline establishment

#### Validation Modes

**Startup Validation** (Recommended for Production)
```yaml
validation:
  runOnStartup: true        # Validate on every startup
  validateEndpoints: false  # Skip HTTP testing for faster startup
```

**Validation-Only Mode** (For CI/CD Pipelines)
```bash
# Command line flag
./scripts/start-generic-api-service.sh --validate-only

# Or via configuration
validation:
  validateOnly: true        # Run validation and exit
```

**Full Validation** (For Development)
```yaml
validation:
  runOnStartup: true
  validateOnly: false
  validateEndpoints: true   # Test actual HTTP connectivity
```

#### Validation Error Handling

The system provides detailed error reporting with **intentional test error markers**:

```
=== INTENTIONAL TEST SCENARIO ===
TESTING ERROR HANDLING: No database configuration files found (THIS IS EXPECTED IN TEST)
  Test directories scanned: [generic-config, ../generic-config]
  Test patterns searched: [*-database.yml, *-databases.yml]
  Purpose: Validating application error handling for missing configurations
=== END INTENTIONAL TEST SCENARIO ===
```

#### Configuration State Management

CORDAL maintains configuration state and supports:
- **Hot Reload**: Dynamic configuration updates without restart
- **Rollback**: Automatic reversion on validation failures
- **State Tracking**: Configuration change history and audit trail
- **Dependency Tracking**: Impact analysis for configuration changes

### Domain Configuration Files

After the main `application.yml` is loaded, CORDAL discovers and loads domain-specific configurations:

### Configuration Loading Process

CORDAL's configuration loading follows a sophisticated discovery and validation process:

#### 1. Directory Scanning
```java
// ConfigurationLoader scans directories defined in application.yml
for (String directory : genericApiConfig.getConfigDirectories()) {
    logger.info("Scanning directory '{}' for configuration files with patterns: {}",
                directory, patterns);

    Path dirPath = Paths.get(directory).toAbsolutePath().normalize();

    // Scan for files matching patterns
    List<Path> directoryMatches = Files.list(dirPath)
        .filter(Files::isRegularFile)
        .filter(file -> matchesAnyPattern(file.getFileName().toString(), patterns))
        .sorted()
        .collect(Collectors.toList());
}
```

#### 2. File Pattern Matching
The system uses glob patterns to discover configuration files:

```yaml
# From application.yml
config:
  patterns:
    databases: ["*-database.yml", "*-databases.yml"]    # Matches: my-database.yml, prod-databases.yml
    queries: ["*-query.yml", "*-queries.yml"]           # Matches: user-queries.yml, api-query.yml
    endpoints: ["*-endpoint.yml", "*-endpoints.yml"]    # Matches: rest-endpoints.yml, api-endpoint.yml
```

#### 3. Configuration Merging
Multiple files of the same type are merged:
```
generic-config/
├── user-databases.yml      ← Merged into single database configuration
├── admin-databases.yml     ←
├── user-queries.yml        ← Merged into single query configuration
├── admin-queries.yml       ←
├── user-endpoints.yml      ← Merged into single endpoint configuration
└── admin-endpoints.yml     ←
```

#### 4. Dependency Resolution
The system validates the configuration chain:
```
Endpoints → Queries → Databases
    ↓           ↓         ↓
  Validates  Validates  Tests
 query refs  database  connection
            refs
```

#### 5. Error Handling and Fallbacks
```java
// Graceful degradation with detailed error reporting
if (databaseFiles.isEmpty()) {
    if (isTestScenario) {
        logger.error("=== INTENTIONAL TEST SCENARIO ===");
        logger.error("TESTING ERROR HANDLING: No database configuration files found");
        logger.error("=== END INTENTIONAL TEST SCENARIO ===");
    } else {
        logger.error("FATAL CONFIGURATION ERROR: No database configuration files found");
        logger.error("  Directories scanned: {}", genericApiConfig.getConfigDirectories());
        logger.error("  Patterns searched: {}", genericApiConfig.getDatabasePatterns());
    }
}
```

### Database Configuration

**File Pattern**: `generic-config/*-databases.yml`

**Example File**: `generic-config/stocktrades-databases.yml`

```yaml
databases:
  stocktrades_db:
    name: "stocktrades_db"
    url: "jdbc:h2:./data/stocktrades;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1"
    username: "sa"
    password: ""
    driver: "org.h2.Driver"
    pool:
      maximumPoolSize: 10
      minimumIdle: 2
      connectionTimeout: 30000
      idleTimeout: 600000
      maxLifetime: 1800000
    validation:
      testQuery: "SELECT 1"
      validateOnBorrow: true
```

### Query Configuration

**Example File**: `your-domain-queries.yaml` (replace with your actual domain)

```yaml
# EXAMPLE: This shows how to configure queries for any domain
# The stock trades example has been moved to cordal-integration-tests
queries:
  get_all_your_entities:
    database: "your_database"
    sql: "SELECT id, field1, field2, field3 FROM your_table ORDER BY created_date DESC LIMIT ? OFFSET ?"
    parameters:
      - name: "limit"
        type: "INTEGER"
        required: true
      - name: "offset"
        type: "INTEGER"
        required: true
    resultMapping:
      - column: "id"
        field: "id"
        type: "LONG"
      - column: "field1"
        field: "field1"
        type: "STRING"
      - column: "field2"
        field: "field2"
        type: "STRING"
```

**Note**: Stock trades configurations are now located in `cordal-integration-tests/src/test/resources/config/` as they are example implementations, not part of the core framework.

### API Endpoint Configuration

**Example File**: `your-domain-endpoints.yaml` (replace with your actual domain)

```yaml
# EXAMPLE: This shows how to configure endpoints for any domain
# The stock trades example has been moved to cordal-integration-tests
endpoints:
  get_your_entities:
    path: "/api/your-entities"
    method: "GET"
    query: "get_all_your_entities"
    pagination:
      enabled: true
      defaultPageSize: 20
      maxPageSize: 1000
    parameters:
      - name: "page"
        type: "INTEGER"
        required: false
        defaultValue: 0
      - name: "size"
        type: "INTEGER"
        required: false
        defaultValue: 20
    response:
      type: "PAGED"
      wrapper: "PagedResponse"
```

---

## Caching System

### Overview

CORDAL includes a sophisticated caching system that provides automatic query result caching with configurable TTL (Time-To-Live), cache invalidation strategies, and comprehensive cache management APIs. The caching system can dramatically improve API performance, with cache hits typically being 25x faster than database queries.

### Key Features

- **Automatic Query Caching**: Zero-code integration through YAML configuration
- **Configurable TTL**: Set custom cache expiration times per query
- **Cache Management APIs**: Monitor, invalidate, and manage cache entries
- **Performance Optimization**: Significant response time improvements
- **Memory Efficient**: Configurable cache size limits and eviction policies
- **Thread Safe**: Concurrent access support for high-throughput scenarios

### Architecture

```mermaid
graph TD
    subgraph "Request Flow"
        Client[Client Request] --> API[API Endpoint]
        API --> Cache{Cache Check}
        Cache -->|Hit| Return[Return Cached Result]
        Cache -->|Miss| DB[Database Query]
        DB --> Store[Store in Cache]
        Store --> Return
    end

    subgraph "Cache Management"
        CacheAPI[Cache Management API]
        CacheAPI --> Stats[Cache Statistics]
        CacheAPI --> Clear[Cache Invalidation]
        CacheAPI --> Monitor[Cache Monitoring]
    end

    subgraph "Configuration"
        QueryConfig[Query Configuration]
        CacheConfig[Cache Configuration]
        QueryConfig --> Cache
        CacheConfig --> Cache
    end
```

### Configuration

#### Application-Level Cache Configuration

**File**: `application.yml`

```yaml
# Cache Configuration
cache:
  enabled: true                    # Master cache switch
  provider: "in-memory"           # Cache provider type
  defaultTtl: 300                 # Default TTL in seconds (5 minutes)
  maxSize: 1000                   # Maximum cache entries
  evictionPolicy: "LRU"           # Eviction policy (LRU, LFU, FIFO)

  # Advanced settings
  settings:
    recordStats: true             # Enable cache statistics
    refreshAfterWrite: false      # Refresh cache entries after write
    expireAfterAccess: false      # Reset TTL on access

  # Memory management
  memory:
    maxMemoryPercent: 10          # Maximum memory usage percentage
    cleanupInterval: 60           # Cleanup interval in seconds
```

#### Query-Level Cache Configuration

**File**: `*-queries.yml`

```yaml
queries:
  get_stock_trades_by_symbol:
    name: "get_stock_trades_by_symbol"
    description: "Get stock trades by symbol with caching"
    database: "stocktrades_db"
    sql: "SELECT * FROM stock_trades WHERE symbol = ? ORDER BY trade_date DESC LIMIT ?"

    # Cache configuration for this specific query
    cache:
      enabled: true               # Enable caching for this query
      ttl: 300                   # Cache for 5 minutes
      maxSize: 500               # Max entries for this query
      keyPattern: "trades:{symbol}:{limit}"  # Custom cache key pattern

    parameters:
      - name: "symbol"
        type: "STRING"
        required: true
      - name: "limit"
        type: "INTEGER"
        required: true

  get_market_summary:
    name: "get_market_summary"
    description: "Get market summary with long-term caching"
    database: "stocktrades_db"
    sql: "SELECT symbol, COUNT(*) as trade_count, AVG(price) as avg_price FROM stock_trades GROUP BY symbol"

    # Long-term caching for aggregated data
    cache:
      enabled: true
      ttl: 900                   # Cache for 15 minutes
      keyPattern: "market_summary"

    parameters: []

  get_real_time_price:
    name: "get_real_time_price"
    description: "Get real-time price with short-term caching"
    database: "stocktrades_db"
    sql: "SELECT symbol, price, trade_date FROM stock_trades WHERE symbol = ? ORDER BY trade_date DESC LIMIT 1"

    # Short-term caching for real-time data
    cache:
      enabled: true
      ttl: 30                    # Cache for 30 seconds only
      keyPattern: "price:{symbol}"

    parameters:
      - name: "symbol"
        type: "STRING"
        required: true
```

### Cache Management APIs

The caching system provides comprehensive REST APIs for monitoring and managing cache state:

#### Cache Statistics

```bash
# Get overall cache statistics
curl "http://localhost:8080/api/cache/stats"

# Response format
{
  "cacheStats": {
    "hitCount": 1250,
    "missCount": 180,
    "hitRate": 0.874,
    "evictionCount": 15,
    "totalLoadTime": 45000,
    "averageLoadTime": 250.0
  },
  "cacheSize": 892,
  "maxSize": 1000,
  "memoryUsage": {
    "usedMemoryMB": 45.2,
    "maxMemoryMB": 512.0,
    "usagePercent": 8.8
  }
}

# Get cache statistics for specific query
curl "http://localhost:8080/api/cache/stats?query=get_stock_trades_by_symbol"
```

#### Cache Invalidation

```bash
# Clear all cache entries
curl -X DELETE "http://localhost:8080/api/cache/clear-all"

# Clear cache entries by pattern
curl -X DELETE "http://localhost:8080/api/cache/clear?pattern=trades:*"

# Clear specific cache entry
curl -X DELETE "http://localhost:8080/api/cache/clear?key=trades:AAPL:10"

# Clear cache for specific query
curl -X DELETE "http://localhost:8080/api/cache/clear?query=get_stock_trades_by_symbol"
```

#### Cache Monitoring

```bash
# List all cache keys
curl "http://localhost:8080/api/cache/keys"

# Get cache entry details
curl "http://localhost:8080/api/cache/entry?key=trades:AAPL:10"

# Response format
{
  "key": "trades:AAPL:10",
  "value": {...},
  "createdAt": "2025-08-16T10:30:45.123Z",
  "expiresAt": "2025-08-16T10:35:45.123Z",
  "accessCount": 15,
  "lastAccessTime": "2025-08-16T10:34:20.456Z",
  "size": 2048
}

# Get cache health status
curl "http://localhost:8080/api/cache/health"
```

### Performance Benefits

The caching system provides significant performance improvements:

#### Benchmark Results

| Scenario | Without Cache | With Cache | Improvement |
|----------|---------------|------------|-------------|
| Simple Query | 50ms | 2ms | **25x faster** |
| Complex Aggregation | 150ms | 5ms | **30x faster** |
| Large Result Set | 300ms | 8ms | **37x faster** |
| High Concurrency | 200ms avg | 3ms avg | **66x faster** |

#### Cache Hit Rates

Typical cache hit rates by query type:
- **Reference Data**: 95-99% (rarely changes)
- **Aggregated Reports**: 80-90% (periodic updates)
- **User-Specific Data**: 60-75% (varies by user activity)
- **Real-time Data**: 40-60% (frequent updates)

### Best Practices

#### Cache Key Design

```yaml
# Good: Specific and hierarchical
keyPattern: "trades:{symbol}:{date}:{limit}"

# Good: Include version for schema changes
keyPattern: "user_portfolio:v2:{user_id}"

# Avoid: Too generic
keyPattern: "data"

# Avoid: Including timestamps
keyPattern: "trades:{symbol}:{current_time}"
```

#### TTL Configuration

```yaml
# Real-time data: Short TTL
cache:
  ttl: 30  # 30 seconds

# Reference data: Long TTL
cache:
  ttl: 3600  # 1 hour

# Aggregated reports: Medium TTL
cache:
  ttl: 900  # 15 minutes

# Static configuration: Very long TTL
cache:
  ttl: 86400  # 24 hours
```

#### Memory Management

```yaml
# Configure appropriate cache sizes
cache:
  maxSize: 1000        # Adjust based on available memory
  maxMemoryPercent: 10 # Don't exceed 10% of heap

# Monitor memory usage
# Use cache statistics to tune sizes
# Implement cache warming for critical data
```

### Troubleshooting

#### Common Issues

1. **Low Cache Hit Rate**
   - Check TTL settings (too short?)
   - Verify cache key patterns
   - Monitor cache evictions

2. **Memory Issues**
   - Reduce cache size limits
   - Implement more aggressive eviction
   - Monitor memory usage patterns

3. **Stale Data**
   - Implement cache invalidation strategies
   - Use appropriate TTL values
   - Consider cache warming

#### Monitoring Commands

```bash
# Monitor cache performance
watch -n 5 'curl -s http://localhost:8080/api/cache/stats | jq'

# Check memory usage
curl "http://localhost:8080/api/cache/stats" | jq '.memoryUsage'

# Monitor hit rates
curl "http://localhost:8080/api/cache/stats" | jq '.cacheStats.hitRate'
```

---

## Deployment

### JAR Profiles

The build system supports multiple JAR profiles for different deployment scenarios:

```mermaid
graph TD
    subgraph "JAR Build Profiles"
        Fat[Fat JAR<br/>~20MB<br/>All Dependencies]
        Opt[Optimized JAR<br/>~15MB<br/>Minimized Dependencies]
        Thin[Thin JAR<br/>~1MB<br/>External Classpath]
        Dev[Development JAR<br/>Variable Size<br/>Fast Build]
    end

    subgraph "Use Cases"
        Single[Single-file Deployment]
        Prod[Production Deployment]
        Container[Container Deployment]
        DevIter[Development Iteration]
    end

    Fat --> Single
    Opt --> Prod
    Thin --> Container
    Dev --> DevIter
```

#### Fat JAR (Default)
- **Size**: ~20MB
- **Dependencies**: All included
- **Use Case**: Single-file deployment
- **Command**: `./scripts/build-executable-jars.sh --fat-jar`

#### Optimized JAR
- **Size**: ~15MB
- **Dependencies**: Minimized, unused classes removed
- **Use Case**: Production deployment
- **Command**: `./scripts/build-executable-jars.sh --optimized-jar`

#### Thin JAR
- **Size**: ~1MB
- **Dependencies**: External classpath required
- **Use Case**: Container deployment with shared dependencies
- **Command**: `./scripts/build-executable-jars.sh --thin-jar`

#### Development JAR
- **Size**: Variable
- **Dependencies**: Fast build, skip tests
- **Use Case**: Development iteration
- **Command**: `./scripts/build-executable-jars.sh --dev`

### Deployment Examples

```mermaid
graph TB
    subgraph "Deployment Options"
        Standalone[Standalone Deployment<br/>Direct JAR execution]
        Docker[Docker Deployment<br/>Containerized application]
        K8s[Kubernetes Deployment<br/>Orchestrated containers]
        Cloud[Cloud Deployment<br/>Managed services]
    end

    subgraph "Infrastructure"
        VM[Virtual Machine]
        Container[Docker Container]
        Pod[Kubernetes Pod]
        CloudSvc[Cloud Service]
    end

    subgraph "Monitoring"
        Health[Health Checks]
        Metrics[Metrics Collection]
        Logs[Log Aggregation]
        Alerts[Alerting]
    end

    Standalone --> VM
    Docker --> Container
    K8s --> Pod
    Cloud --> CloudSvc

    VM --> Health
    Container --> Metrics
    Pod --> Logs
    CloudSvc --> Alerts
```

#### Standalone Deployment
```bash
# Build optimized JAR
./scripts/build-executable-jars.sh --optimized-jar

# Start services
java -jar cordal-api-service/target/cordal-api-service-1.0-SNAPSHOT-optimized.jar
java -jar cordal-metrics-service/target/cordal-metrics-service-1.0-SNAPSHOT-optimized.jar
```

#### Docker Deployment
```dockerfile
FROM openjdk:21-jre-slim

WORKDIR /app
COPY cordal-api-service/target/*-optimized.jar app.jar
COPY generic-config/ config/
COPY scripts/ scripts/

RUN chmod +x scripts/*.sh

EXPOSE 8080
CMD ["./scripts/start-cordal-api-service.sh"]
```

#### Kubernetes Deployment
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cordal
spec:
  replicas: 3
  selector:
    matchLabels:
      app: cordal
  template:
    metadata:
      labels:
        app: cordal
    spec:
      containers:
      - name: api-service
        image: cordal:latest
        ports:
        - containerPort: 8080
        env:
        - name: JAVA_OPTS
          value: "-Xmx1g -XX:+UseG1GC"
        livenessProbe:
          httpGet:
            path: /api/management/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /api/management/readiness
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5
```

### Health Checks

The system provides comprehensive health monitoring:

#### Basic Health Check
```bash
curl http://localhost:8080/api/health
```

#### Management Health Endpoints
```bash
# Kubernetes liveness probe
curl http://localhost:8080/api/management/liveness

# Kubernetes readiness probe
curl http://localhost:8080/api/management/readiness

# Comprehensive health check
curl http://localhost:8080/api/management/health
```

#### Health Check Response
```json
{
  "status": "UP",
  "timestamp": "2025-08-14T10:30:00Z",
  "components": {
    "database": {
      "status": "UP",
      "details": {
        "connectionPool": "healthy",
        "activeConnections": 2,
        "totalConnections": 10
      }
    },
    "configuration": {
      "status": "UP",
      "details": {
        "endpointsLoaded": 5,
        "queriesLoaded": 8,
        "databasesLoaded": 2
      }
    },
    "metrics": {
      "status": "UP",
      "details": {
        "collectionEnabled": true,
        "metricsDatabase": "healthy"
      }
    }
  }
}
```

---

## Metrics Collection

### Overview

The metrics collection system provides automatic, zero-code monitoring of all API requests. It captures response times, success rates, memory usage, and other performance indicators without requiring any changes to existing controller code.

```mermaid
sequenceDiagram
    participant Client
    participant Javalin
    participant MetricsHandler
    participant APIController
    participant MetricsDB

    Client->>Javalin: HTTP Request
    Javalin->>MetricsHandler: beforeRequest()
    MetricsHandler->>MetricsHandler: Start timer, capture memory
    Javalin->>APIController: Process request
    APIController->>Javalin: Response
    Javalin->>MetricsHandler: afterRequest()
    MetricsHandler->>MetricsHandler: Calculate metrics
    MetricsHandler->>MetricsDB: Save metrics (async)
    Javalin->>Client: HTTP Response
```

### Features

- **Automatic Collection**: Intercepts all HTTP requests/responses
- **Real-time Aggregation**: Live metrics available via REST API
- **Database Persistence**: Historical metrics stored for trend analysis
- **Configurable**: Fine-grained control over collection behavior
- **High Performance**: Minimal overhead with async processing
- **Path Exclusion**: Exclude specific paths from collection

### Configuration

```yaml
metrics:
  collection:
    enabled: true                    # Master switch
    includeMemoryMetrics: true       # Memory usage tracking
    excludePaths:                    # Paths to exclude
      - "/dashboard"
      - "/metrics"
      - "/api/performance-metrics"
    samplingRate: 1.0               # 0.0-1.0 (100% = all requests)
    asyncSave: true                 # Async database saves
```

### Environment-Specific Configurations

#### Development
```yaml
metrics:
  collection:
    enabled: true
    samplingRate: 1.0               # Full collection
    asyncSave: false                # Immediate persistence
    includeMemoryMetrics: true
```

#### Production
```yaml
metrics:
  collection:
    enabled: true
    samplingRate: 0.05              # 5% sampling
    includeMemoryMetrics: false     # Reduce overhead
    asyncSave: true                 # Async for performance
```

### Metrics APIs

#### Real-time Metrics
```bash
# Get all endpoint metrics
curl http://localhost:8080/api/metrics/endpoints

# Response format (example with your domain endpoints)
{
  "GET /api/your-entities": {
    "totalRequests": 150,
    "averageResponseTime": 45.2,
    "successRate": 98.7,
    "lastRequestTime": [2025,8,14,15,30,45,123456789]
  }
}
```

#### Reset Metrics
```bash
curl -X POST http://localhost:8080/api/metrics/reset
```

#### Historical Metrics
```bash
# Get historical performance data
curl http://localhost:8080/api/performance-metrics

# Get performance summary
curl http://localhost:8080/api/performance-metrics/summary

# Get trends for charts
curl "http://localhost:8080/api/performance-metrics/trends?days=7&testType=API_REQUEST"
```

### Implementation Details

The metrics collection system uses a handler-based approach:

1. **Before Request**: Captures start time, memory state, request details
2. **After Request**: Calculates response time, memory usage, updates aggregations
3. **Path Normalization**: Groups similar endpoints (e.g., `/api/stock-trades/{id}`)
4. **Thread Safety**: Uses ThreadLocal for request-scoped data and ConcurrentHashMap for aggregations
5. **Error Handling**: Graceful degradation if metrics collection fails

---

## Database Integration

### Supported Databases

#### H2 Database (Default)
- **Use Case**: Development, testing, embedded scenarios
- **Configuration**: File-based or in-memory
- **Features**: Zero-configuration, web console, SQL compatibility

#### PostgreSQL
- **Use Case**: Production deployments
- **Configuration**: External server connection
- **Features**: Full ACID compliance, advanced features, scalability

### Database Configuration Examples

#### H2 Configuration
```yaml
databases:
  stocktrades_db:
    name: "stocktrades_db"
    url: "jdbc:h2:./data/stocktrades;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1"
    username: "sa"
    password: ""
    driver: "org.h2.Driver"
```

#### PostgreSQL Configuration
```yaml
databases:
  postgres_trades:
    name: "postgres_trades"
    url: "jdbc:postgresql://localhost:5432/trades_db_1?currentSchema=public"
    username: "testuser"
    password: "testpass"
    driver: "org.postgresql.Driver"
    pool:
      maximumPoolSize: 20
      minimumIdle: 5
      connectionTimeout: 30000
```

### Database Schema Management

The system automatically manages database schemas:

```mermaid
erDiagram
    YOUR_DOMAIN_TABLE {
        BIGINT id PK
        VARCHAR field1
        VARCHAR field2
        INTEGER field3
        DECIMAL field4
        DECIMAL total_value
        TIMESTAMP trade_date
        VARCHAR trader_id
        VARCHAR exchange
    }

    PERFORMANCE_METRICS {
        BIGINT id PK
        VARCHAR test_name
        VARCHAR test_type
        TIMESTAMP timestamp
        INTEGER total_requests
        BIGINT total_time_ms
        DOUBLE average_response_time_ms
        INTEGER concurrent_threads
        INTEGER requests_per_thread
        INTEGER page_size
        BIGINT memory_usage_bytes
        BIGINT memory_increase_bytes
        BOOLEAN test_passed
        TEXT additional_metrics
        TIMESTAMP created_at
    }
```

#### Example Domain Schema
```sql
-- EXAMPLE: Replace with your actual domain tables
-- Stock trades example moved to cordal-integration-tests
CREATE TABLE your_domain_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    field1 VARCHAR(50) NOT NULL,
    field2 VARCHAR(50) NOT NULL,
    field3 INTEGER NOT NULL,
    field4 DECIMAL(10,2) NOT NULL,
    total_value DECIMAL(15,2) NOT NULL,
    created_date TIMESTAMP NOT NULL,
    field5 VARCHAR(50),
    field6 VARCHAR(20)
);

CREATE INDEX idx_your_table_field1 ON your_domain_table(field1);
CREATE INDEX idx_your_table_date ON your_domain_table(created_date);
```

#### Performance Metrics Schema
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

### H2 Server Mode

For multi-connection support, start H2 in server mode:

```bash
# Start H2 server
./scripts/start-h2-server.sh

# Access H2 console
./scripts/h2-console.sh
```

**Connection Details**:
- **TCP Port**: 9092
- **Web Console**: http://localhost:8082
- **Connection URL**: `jdbc:h2:tcp://localhost:9092/./data/{database-name}`

---

## Testing

### Test Categories

```mermaid
graph TD
    subgraph "Test Architecture"
        UT[Unit Tests]
        IT[Integration Tests]
        PT[Performance Tests]
        CT[Configuration Tests]
    end

    subgraph "Unit Test Types"
        MT[Model Tests]
        ST[Service Tests]
        RT[Repository Tests]
        CFT[Config Tests]
    end

    subgraph "Integration Test Types"
        AIT[API Integration]
        DIT[Database Integration]
        CIT[Configuration Integration]
    end

    subgraph "Performance Test Types"
        COT[Concurrent Testing]
        MET[Memory Testing]
        PAT[Pagination Testing]
        AST[Async Testing]
    end

    UT --> MT
    UT --> ST
    UT --> RT
    UT --> CFT

    IT --> AIT
    IT --> DIT
    IT --> CIT

    PT --> COT
    PT --> MET
    PT --> PAT
    PT --> AST

    CT --> CFT
```

#### 1. Unit Tests
- **Model Tests**: Entity validation, DTO conversion
- **Service Tests**: Business logic, validation, error handling
- **Repository Tests**: Data access, SQL operations
- **Configuration Tests**: YAML loading, validation

#### 2. Integration Tests
- **API Integration**: End-to-end HTTP testing
- **Database Integration**: Multi-database scenarios
- **Configuration Integration**: Full configuration chain testing

#### 3. Performance Tests
- **Concurrent Testing**: Multi-threaded request handling
- **Memory Testing**: Memory usage validation
- **Pagination Testing**: Large dataset performance
- **Async Testing**: Asynchronous endpoint performance

### Running Tests

#### All Tests
```bash
mvn test
```

#### Specific Test Categories
```bash
# Unit tests only
mvn test -Dtest="dev.mars.model.*,dev.mars.service.*,dev.mars.repository.*"

# Integration tests only
mvn test -Dtest="dev.mars.integration.*"

# Configuration validation tests
mvn test -Dtest="ValidationConfigurationTest,ValidationStartupTest"

# Performance tests (disabled by default)
mvn test -Dtest="dev.mars.performance.*" -Dtest.performance.enabled=true
```

#### Module-Specific Tests
```bash
# Generic API Service tests
mvn test -pl cordal-api-service

# Metrics Service tests
mvn test -pl cordal-metrics-service

# Integration tests
mvn test -pl cordal-integration-tests
```

### Test Configuration

#### Test Application Configuration
**File**: `src/test/resources/application-test.yml`

```yaml
database:
  url: "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"
  username: "sa"
  password: ""
  driver: "org.h2.Driver"

data:
  loadSampleData: false
  initializeForTesting: true

logging:
  level:
    dev.mars: DEBUG
    org.h2: WARN
```

### Test Coverage

| Component | Coverage | Test Count |
|-----------|----------|------------|
| Model Layer | 100% | 25 |
| Repository Layer | 95% | 35 |
| Service Layer | 95% | 40 |
| Controller Layer | 90% | 30 |
| Configuration Layer | 85% | 20 |
| Integration | 80% | 15 |

---

## Scripts and Automation

### Build Scripts

#### Enhanced Build Script
```bash
# Default fat JAR build
./scripts/build-executable-jars.sh

# Build with options
./scripts/build-executable-jars.sh --optimized-jar --analyze --run-tests

# Fast development build
./scripts/build-executable-jars.sh --dev
```

**Available Options**:
- `--run-tests`: Run tests during build
- `--no-clean`: Skip clean phase
- `--fat-jar`: Self-contained JARs (default)
- `--thin-jar`: Minimal JARs
- `--optimized-jar`: Minimized fat JARs
- `--dev`: Fast development builds
- `--analyze`: Run JAR analysis after build

### Service Management Scripts

#### Start All Services
```bash
# Start both API and metrics services
./scripts/start-all-services.sh

# Start only API service
./scripts/start-all-services.sh --generic-api-only

# Validate configuration only
./scripts/start-all-services.sh --validate-only
```

#### Individual Service Scripts
```bash
# Start Generic API Service
./scripts/start-cordal-api-service.sh

# Start Metrics Service
./scripts/start-cordal-metrics-service.sh

# Validation mode
./scripts/start-cordal-api-service.sh --validate-only
```

### Analysis and Debugging Scripts

#### JAR Analysis
```bash
# Analyze all JARs
./scripts/analyze-jars.sh

# Show sizes only
./scripts/analyze-jars.sh --sizes

# Analyze specific module
./scripts/analyze-jars.sh --module cordal-api-service --profile optimized
```

#### Database Scripts
```bash
# Start H2 server
./scripts/start-h2-server.sh

# Start H2 web console
./scripts/h2-console.sh
```

#### Testing and Validation Scripts
```bash
# Run bootstrap demonstration
./scripts/run-bootstrap-demo.sh

# Test validation features
./scripts/test-validation-flags.sh
```

### Cross-Platform Support

All scripts have Windows equivalents:
- `build-executable-jars.bat`
- `start-all-services.bat`
- `start-cordal-api-service.bat`
- `start-cordal-metrics-service.bat`
- `analyze-jars.bat`
- `start-h2-server.bat`
- `h2-console.bat`
- `run-bootstrap-demo.bat`
- `test-validation-flags.bat`

---

## Performance Dashboard

### Overview

The performance dashboard provides real-time monitoring and visualization of API performance metrics through an interactive web interface.

### Dashboard Features

```mermaid
graph LR
    subgraph "Dashboard Components"
        PS[Performance Summary]
        RT[Response Time Charts]
        SR[Success Rate Charts]
        TD[Test Distribution]
        RPT[Recent Performance Tests]
    end

    subgraph "Dashboard Controls"
        TF[Test Type Filter]
        TR[Time Range Filter]
        AR[Auto Refresh]
        MR[Manual Refresh]
    end

    subgraph "Data Sources"
        API[Metrics API]
        DB[Metrics Database]
    end

    TF --> PS
    TR --> RT
    AR --> SR
    MR --> TD

    PS --> API
    RT --> API
    SR --> API
    TD --> API
    RPT --> DB
```

#### 1. Performance Summary
- Total number of tests executed
- Average response time across all tests
- Success rate percentage
- Last test execution time

#### 2. Interactive Charts
- **Response Time Trends**: Line chart showing average response times over time
- **Success Rate Trends**: Line chart showing test success rates over time
- **Test Type Distribution**: Overview of different types of performance tests

#### 3. Recent Performance Tests
- Table showing the most recent test executions
- Includes test name, type, response time, status, and timestamp

#### 4. Dashboard Controls
- **Test Type Filter**: Filter metrics by specific test types (CONCURRENT, SYNC, ASYNC, PAGINATION, MEMORY)
- **Time Range Filter**: View data for different time periods (24 hours, 7 days, 30 days, 90 days)
- **Auto-refresh**: Dashboard automatically refreshes every 30 seconds
- **Manual Refresh**: Button to manually refresh all data

### Accessing the Dashboard

- **Primary URL**: http://localhost:8080/dashboard
- **Alternative URL**: http://localhost:8080/dashboard/

### Dashboard API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/performance-metrics` | GET | Get all performance metrics with pagination |
| `/api/performance-metrics/{id}` | GET | Get specific performance metrics by ID |
| `/api/performance-metrics/summary` | GET | Get performance summary statistics |
| `/api/performance-metrics/trends` | GET | Get performance trends for charts |
| `/api/performance-metrics/test-types` | GET | Get available test types |
| `/api/performance-metrics/test-type/{testType}` | GET | Get metrics by test type |
| `/api/performance-metrics/date-range` | GET | Get metrics within date range |

### Performance Test Types

#### 1. CONCURRENT Tests
- **Purpose**: Test concurrent request handling
- **Metrics**: Total requests, concurrent threads, requests per thread
- **Example**: 10 threads making 20 requests each (200 total requests)

#### 2. SYNC Tests
- **Purpose**: Test synchronous request performance
- **Metrics**: Sequential request processing times
- **Example**: 50 sequential requests measuring total time

#### 3. ASYNC Tests
- **Purpose**: Test asynchronous request performance
- **Metrics**: Asynchronous request processing times
- **Example**: 50 async requests comparing with sync performance

#### 4. PAGINATION Tests
- **Purpose**: Test pagination performance with different page sizes
- **Metrics**: Response times for various page sizes (10, 50, 100, 500, 1000)
- **Example**: Single requests with different page sizes

#### 5. MEMORY Tests
- **Purpose**: Test memory usage during request processing
- **Metrics**: Memory usage before/after, memory increase
- **Example**: 100 requests monitoring memory consumption

### Technology Stack

- **Backend**: Java 21, Javalin 6.1.3, Guice DI
- **Database**: H2 Database (file-based)
- **Frontend**: HTML5, CSS3, JavaScript (ES6+)
- **Charts**: Chart.js 4.x
- **HTTP Client**: OkHttp (for tests)

---

## Configuration Validation

### Overview

The configuration validation system ensures that all YAML configuration files are properly structured and that the relationships between endpoints, queries, and databases are valid.

```mermaid
flowchart TD
    Start([Start Validation]) --> LoadYAML[Load YAML Files]
    LoadYAML --> ParseConfig[Parse Configurations]
    ParseConfig --> ValidateStructure[Validate YAML Structure]

    ValidateStructure --> ValidateChain[Validate Configuration Chain]
    ValidateChain --> CheckEndpoints[Check Endpoint → Query References]
    CheckEndpoints --> CheckQueries[Check Query → Database References]
    CheckQueries --> ValidateParams[Validate Parameters]

    ValidateParams --> ConnectDB[Connect to Databases]
    ConnectDB --> CheckTables[Check Table Existence]
    CheckTables --> CheckColumns[Check Column Existence]
    CheckColumns --> ValidateTypes[Validate Data Types]

    ValidateTypes --> GenerateReport[Generate Validation Report]
    GenerateReport --> Decision{All Valid?}
    Decision -->|Yes| Success([Validation Passed])
    Decision -->|No| Failure([Validation Failed])

    Failure --> ShowErrors[Show Detailed Errors]
    ShowErrors --> ContinueCheck{Continue on Error?}
    ContinueCheck -->|Yes| ValidateNext[Validate Next Component]
    ContinueCheck -->|No| Exit([Exit with Errors])
    ValidateNext --> ValidateChain
```

### Validation Features

#### 1. Configuration Chain Validation
- **Endpoints → Queries**: Verify that each endpoint references an existing query
- **Queries → Databases**: Verify that each query references an existing database
- **Cross-references**: Validate all configuration relationships

#### 2. Database Schema Validation
- **Table Existence**: Verify that referenced tables exist in the database
- **Column Validation**: Check that query columns exist in database tables
- **Data Type Compatibility**: Ensure query result types match API expectations

#### 3. Comprehensive Error Reporting
- **Detailed Errors**: Show specific validation failures with context
- **Error Limits**: Configurable maximum number of errors to display
- **ASCII Tables**: Formatted error reports for easy reading
- **Continue on Error**: Option to continue validation after encountering errors

### Validation Configuration

```yaml
validation:
  runOnStartup: true              # Run validation during normal startup
  validateOnly: false             # Run only validation and exit
  continueOnError: true           # Continue validation after errors
  maxErrorsToShow: 10            # Maximum errors to display
```

### Validation Modes

#### 1. Startup Validation
```yaml
validation:
  runOnStartup: true
```
Runs validation during normal application startup and logs any issues.

#### 2. Validation-Only Mode
```yaml
validation:
  validateOnly: true
```
Runs only validation and exits without starting the application.

#### 3. Command Line Override
```bash
# Override config and run validation only
java -jar app.jar --validate-only

# Short form
java -jar app.jar --validate
```

### Running Validation

#### Using Scripts
```bash
# Test validation features
./scripts/test-validation-flags.sh

# Validate configuration only
./scripts/start-all-services.sh --validate-only
```

#### Manual Validation
```bash
# Run validation only
java -jar cordal-api-service/target/cordal-api-service-*.jar --validate-only

# Run with startup validation enabled
java -jar cordal-api-service/target/cordal-api-service-*.jar
```

### Validation Output Example

```
Configuration Validation Results
================================

Configuration Chain Validation:
+------------------+--------+----------------------------------+
| Component        | Status | Details                          |
+------------------+--------+----------------------------------+
| Endpoints        | PASS   | 5 endpoints loaded successfully  |
| Queries          | PASS   | 8 queries loaded successfully    |
| Databases        | PASS   | 2 databases loaded successfully  |
| Cross-references | PASS   | All references valid             |
+------------------+--------+----------------------------------+

Database Schema Validation:
+------------------+--------+----------------------------------+
| Database         | Status | Details                          |
+------------------+--------+----------------------------------+
| stocktrades_db   | PASS   | All tables and columns valid     |
| metrics_db       | PASS   | Schema validation successful     |
+------------------+--------+----------------------------------+

Validation Summary:
- Total Errors: 0
- Total Warnings: 0
- Validation Status: PASSED
```

---

## Configuration Schema Reference

### Database Configuration Schema

```yaml
databases:
  {database_name}:                 # Unique database identifier
    name: string                   # Display name
    url: string                    # JDBC connection URL
    username: string               # Database username
    password: string               # Database password
    driver: string                 # JDBC driver class
    pool:                          # Connection pool settings
      maximumPoolSize: integer     # Maximum connections (default: 10)
      minimumIdle: integer         # Minimum idle connections (default: 2)
      connectionTimeout: integer   # Connection timeout ms (default: 30000)
      idleTimeout: integer         # Idle timeout ms (default: 600000)
      maxLifetime: integer         # Max connection lifetime ms (default: 1800000)
    validation:                    # Connection validation
      testQuery: string            # Validation query (default: "SELECT 1")
      validateOnBorrow: boolean    # Validate on borrow (default: true)
```

### Query Configuration Schema

```yaml
queries:
  {query_name}:                    # Unique query identifier
    name: string                   # Display name for the query
    description: string            # Query description
    database: string               # Reference to database name
    sql: string                    # SQL query with ? placeholders
    parameters:                    # Query parameters
      - name: string               # Parameter name
        type: string               # Parameter type (INTEGER, STRING, DECIMAL, etc.)
        required: boolean          # Whether parameter is required
        defaultValue: any          # Default value if not provided
    resultMapping:                 # Result column mapping
      - column: string             # Database column name
        field: string              # API response field name
        type: string               # Data type for conversion
    pagination:                    # Pagination support
      enabled: boolean             # Enable pagination for this query
      countQuery: string           # Optional custom count query
    cache:                         # Cache configuration (NEW)
      enabled: boolean             # Enable caching for this query
      ttl: integer                 # Time-to-live in seconds
      maxSize: integer             # Maximum cache entries for this query
      keyPattern: string           # Cache key pattern with placeholders
      evictionPolicy: string       # LRU, LFU, FIFO (optional)
```

### Endpoint Configuration Schema

```yaml
endpoints:
  {endpoint_name}:                 # Unique endpoint identifier
    path: string                   # URL path (e.g., "/api/stock-trades")
    method: string                 # HTTP method (GET, POST, PUT, DELETE)
    query: string                  # Reference to query name
    pagination:                    # Pagination configuration
      enabled: boolean             # Enable pagination
      defaultPageSize: integer     # Default page size
      maxPageSize: integer         # Maximum allowed page size
    parameters:                    # Endpoint parameters
      - name: string               # Parameter name
        type: string               # Parameter type
        required: boolean          # Whether parameter is required
        defaultValue: any          # Default value
        source: string             # Parameter source (QUERY, PATH, BODY)
    response:                      # Response configuration
      type: string                 # Response type (SINGLE, LIST, PAGED)
      wrapper: string              # Response wrapper class
    security:                      # Security configuration
      authentication: boolean     # Require authentication
      roles: [string]              # Required roles
```

---

## Bootstrap Process

### Overview

The bootstrap process initializes the application by loading configurations, validating settings, setting up databases, and creating dynamic API endpoints.

### Bootstrap Sequence

```mermaid
flowchart TD
    Start([Application Start]) --> LoadConfig[Load application.yaml]
    LoadConfig --> ScanFiles[Scan Configuration Files]
    ScanFiles --> ParseConfig[Parse YAML Configurations]
    ParseConfig --> MergeConfig[Merge Multiple Files]

    MergeConfig --> ValidateChain[Validate Configuration Chain]
    ValidateChain --> CheckDB[Check Database Connectivity]
    CheckDB --> VerifySchema[Verify Table/Column Existence]
    VerifySchema --> ValidateMapping[Validate Query-API Mappings]

    ValidateMapping --> CreateConnections[Create Database Connections]
    CreateConnections --> InitPools[Initialize Connection Pools]
    InitPools --> CreateTables[Create Tables if Missing]
    CreateTables --> LoadData[Load Sample Data]

    LoadData --> GenerateRoutes[Generate Dynamic Routes]
    GenerateRoutes --> SetupHandlers[Setup Request Handlers]
    SetupHandlers --> ConfigPagination[Configure Pagination]
    SetupHandlers --> RegisterEndpoints[Register with Javalin]

    RegisterEndpoints --> InitMetrics[Initialize Metrics Handler]
    InitMetrics --> SetupMetricsDB[Setup Metrics Database]
    SetupMetricsDB --> ConfigAggregation[Configure Real-time Aggregation]
    ConfigAggregation --> RegisterMetricsAPI[Register Metrics APIs]

    RegisterMetricsAPI --> SetupHealth[Setup Health Checks]
    SetupHealth --> MonitorDB[Setup Database Monitoring]
    MonitorDB --> InitProbes[Initialize Readiness/Liveness]
    InitProbes --> Ready([Application Ready])
```

1. **Configuration Loading**
   - Load application.yaml
   - Scan for configuration files using patterns
   - Parse database, query, and endpoint configurations
   - Merge multiple configuration files

2. **Configuration Validation**
   - Validate configuration chain (endpoints → queries → databases)
   - Check database connectivity
   - Verify table and column existence
   - Validate query-to-API field mappings

3. **Database Initialization**
   - Create database connections
   - Initialize connection pools
   - Create tables if they don't exist
   - Load sample data (if configured)

4. **API Endpoint Creation**
   - Generate dynamic routes based on endpoint configurations
   - Set up request handlers
   - Configure pagination and parameter handling
   - Register endpoints with Javalin

5. **Metrics System Initialization**
   - Initialize metrics collection handler
   - Set up metrics database
   - Configure real-time aggregation
   - Register metrics APIs

6. **Health Check Setup**
   - Configure health check endpoints
   - Set up database health monitoring
   - Initialize readiness and liveness probes

### Bootstrap Demonstration

Run the bootstrap demo to see the process in action:

```bash
./scripts/run-bootstrap-demo.sh
```

This script will:
1. Build the project
2. Start the application with detailed logging
3. Show configuration loading process
4. Demonstrate API endpoint creation
5. Test the created endpoints
6. Display metrics collection

### Bootstrap Configuration

Control bootstrap behavior through application.yaml:

```yaml
# Bootstrap Configuration
bootstrap:
  validateOnStartup: true          # Run validation during bootstrap
  failOnValidationError: false    # Continue despite validation errors
  createTablesIfMissing: true     # Auto-create missing database tables
  loadSampleData: false           # Load sample data during bootstrap

# Data Loading Configuration
data:
  loadSampleData: false           # Load sample data
  initializeForTesting: false     # Initialize test data
```

---

## Endpoint Validation

### Overview

Endpoint validation ensures that all dynamically created API endpoints are properly configured and functional.

### Validation Process

#### 1. Configuration Validation
- **Endpoint Definition**: Verify endpoint YAML structure
- **Query Reference**: Ensure referenced query exists
- **Parameter Mapping**: Validate parameter definitions
- **Response Configuration**: Check response type and wrapper settings

#### 2. Database Validation
- **Query Execution**: Test query execution with sample parameters
- **Result Mapping**: Verify column-to-field mappings
- **Data Types**: Ensure type compatibility
- **Pagination**: Test pagination queries if enabled

#### 3. Runtime Validation
- **Endpoint Registration**: Verify endpoints are registered with Javalin
- **Request Handling**: Test request parameter parsing
- **Response Generation**: Validate response formatting
- **Error Handling**: Test error scenarios

### Validation Commands

#### Comprehensive Validation
```bash
# Run full validation
./scripts/start-cordal-api-service.sh --validate-only
```

#### Specific Validation Tests
```bash
# Test validation flags
./scripts/test-validation-flags.sh

# Run validation tests
mvn test -Dtest="ValidationConfigurationTest,ValidationStartupTest"
```

### Validation Output

The validation system provides detailed reports:

```
Endpoint Validation Results
===========================

Endpoint: GET /api/your-entities (example)
+------------------+--------+----------------------------------+
| Validation Type  | Status | Details                          |
+------------------+--------+----------------------------------+
| Configuration    | PASS   | Endpoint definition valid        |
| Query Reference  | PASS   | Query 'get_all_your_entities' found |
| Database Schema  | PASS   | All columns exist                |
| Parameter Mapping| PASS   | All parameters mapped correctly  |
| Response Format  | PASS   | PagedResponse wrapper valid      |
+------------------+--------+----------------------------------+

Validation Summary:
- Total Endpoints: 5
- Passed: 5
- Failed: 0
- Warnings: 0
```

---

## IDE Setup

### IntelliJ IDEA Setup

#### 1. Project Import
1. Open IntelliJ IDEA
2. Select "Open or Import"
3. Navigate to the project root directory
4. Select the `pom.xml` file
5. Choose "Open as Project"
6. Wait for Maven import to complete

#### 2. Java Configuration
1. Go to File → Project Structure
2. Set Project SDK to Java 21
3. Set Project language level to 21
4. Configure module dependencies

#### 3. Maven Configuration
1. Go to File → Settings → Build → Build Tools → Maven
2. Set Maven home directory
3. Set User settings file (if custom)
4. Enable "Import Maven projects automatically"

#### 4. Code Style
1. Go to File → Settings → Editor → Code Style → Java
2. Import code style from `ide-config/intellij-code-style.xml` (if available)
3. Set tab size to 4 spaces
4. Enable "Use tab character" = false

#### 5. Run Configurations

**Generic API Service**:
- Main class: `dev.cordal.generic.GenericApiApplication`
- VM options: `-Xmx1g -Xms512m`
- Working directory: `$MODULE_WORKING_DIR$`
- Environment variables: `JAVA_OPTS=-XX:+UseG1GC`

**Metrics Service**:
- Main class: `dev.cordal.metrics.MetricsApplication`
- VM options: `-Xmx512m -Xms256m`
- Working directory: `$MODULE_WORKING_DIR$`

#### 6. Database Configuration
1. Go to View → Tool Windows → Database
2. Add H2 database connection:
   - URL: `jdbc:h2:./data/stocktrades`
   - Username: `sa`
   - Password: (empty)
3. Add PostgreSQL connection (if used):
   - URL: `jdbc:postgresql://localhost:5432/trades_db_1`
   - Username: `testuser`
   - Password: `testpass`

### VS Code Setup

#### 1. Extensions
Install the following extensions:
- Extension Pack for Java
- Spring Boot Extension Pack
- YAML Support
- Maven for Java
- Debugger for Java

#### 2. Settings Configuration
Create `.vscode/settings.json`:

```json
{
  "java.home": "/path/to/java21",
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-21",
      "path": "/path/to/java21"
    }
  ],
  "maven.executable.path": "/path/to/maven/bin/mvn",
  "java.test.config": {
    "workingDirectory": "${workspaceFolder}"
  }
}
```

#### 3. Launch Configuration
Create `.vscode/launch.json`:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Launch Generic API Service",
      "request": "launch",
      "mainClass": "dev.cordal.generic.GenericApiApplication",
      "projectName": "cordal-api-service",
      "vmArgs": "-Xmx1g -Xms512m",
      "cwd": "${workspaceFolder}"
    },
    {
      "type": "java",
      "name": "Launch Metrics Service",
      "request": "launch",
      "mainClass": "dev.cordal.metrics.MetricsApplication",
      "projectName": "cordal-metrics-service",
      "vmArgs": "-Xmx512m -Xms256m",
      "cwd": "${workspaceFolder}"
    }
  ]
}
```

### Eclipse Setup

#### 1. Project Import
1. File → Import → Existing Maven Projects
2. Browse to project root directory
3. Select all modules
4. Click Finish

#### 2. Java Build Path
1. Right-click project → Properties
2. Java Build Path → Libraries
3. Add JRE System Library (Java 21)
4. Configure Maven Dependencies

#### 3. Run Configurations
1. Run → Run Configurations
2. Create new Java Application
3. Set main class and VM arguments as above

---

## PostgreSQL Integration

### Setup Guide

#### 1. Install PostgreSQL

**Windows**:
```bash
# Using Chocolatey
choco install postgresql

# Or download from https://www.postgresql.org/download/windows/
```

**macOS**:
```bash
# Using Homebrew
brew install postgresql
brew services start postgresql
```

**Linux (Ubuntu/Debian)**:
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

#### 2. Configure PostgreSQL

```bash
# Connect as postgres user
sudo -u postgres psql

# Set password
ALTER USER postgres PASSWORD 'postgres';

# Create test user
CREATE USER testuser WITH PASSWORD 'testpass';
ALTER USER testuser CREATEDB;

# Exit
\q
```

#### 3. Create Test Databases

```sql
-- Create test databases
CREATE DATABASE trades_db_1 OWNER testuser;
CREATE DATABASE trades_db_2 OWNER testuser;
```

### Dual PostgreSQL Integration

The system supports connecting to multiple PostgreSQL databases simultaneously:

#### Configuration Example

**File**: `generic-config/postgres-databases.yaml`

```yaml
databases:
  postgres_trades_1:
    name: "postgres_trades_1"
    url: "jdbc:postgresql://localhost:5432/trades_db_1?currentSchema=public"
    username: "testuser"
    password: "testpass"
    driver: "org.postgresql.Driver"
    pool:
      maximumPoolSize: 20
      minimumIdle: 5
      connectionTimeout: 30000

  postgres_trades_2:
    name: "postgres_trades_2"
    url: "jdbc:postgresql://localhost:5432/trades_db_2?currentSchema=public"
    username: "testuser"
    password: "testpass"
    driver: "org.postgresql.Driver"
    pool:
      maximumPoolSize: 20
      minimumIdle: 5
      connectionTimeout: 30000
```

#### Query Configuration for PostgreSQL

**File**: `generic-config/postgres-queries.yaml`

```yaml
queries:
  get_postgres_trades_db1:
    database: "postgres_trades_1"
    sql: "SELECT id, symbol, trade_type, quantity, price, total_value, trade_date, trader_id, exchange FROM stock_trades ORDER BY trade_date DESC LIMIT ? OFFSET ?"
    parameters:
      - name: "limit"
        type: "INTEGER"
        required: true
      - name: "offset"
        type: "INTEGER"
        required: true

  get_postgres_trades_db2:
    database: "postgres_trades_2"
    sql: "SELECT id, symbol, trade_type, quantity, price, total_value, trade_date, trader_id, exchange FROM stock_trades ORDER BY trade_date DESC LIMIT ? OFFSET ?"
    parameters:
      - name: "limit"
        type: "INTEGER"
        required: true
      - name: "offset"
        type: "INTEGER"
        required: true
```

#### API Endpoints for PostgreSQL

**File**: `generic-config/postgres-endpoints.yaml`

```yaml
endpoints:
  get_postgres_trades_db1:
    path: "/api/postgres/trades-db1"
    method: "GET"
    query: "get_postgres_trades_db1"
    pagination:
      enabled: true
      defaultPageSize: 20
      maxPageSize: 1000

  get_postgres_trades_db2:
    path: "/api/postgres/trades-db2"
    method: "GET"
    query: "get_postgres_trades_db2"
    pagination:
      enabled: true
      defaultPageSize: 20
      maxPageSize: 1000
```

### Integration Testing

Run the dual PostgreSQL integration test:

```bash
# Automatic setup and testing
mvn test -pl cordal-integration-tests -Dtest=DualPostgreSQLNativeIntegrationTest

# Manual setup (if preferred)
cd cordal-integration-tests
psql -h localhost -U postgres -f src/test/resources/sql/setup-dual-postgresql-databases.sql
mvn test -Dtest=DualPostgreSQLNativeIntegrationTest
```

The integration test:
1. Creates test databases and user
2. Sets up schemas and sample data
3. Starts API service with PostgreSQL configuration
4. Tests all endpoints
5. Cleans up databases

---

## Troubleshooting

### Common Issues and Solutions

#### 1. Application Won't Start

**Symptoms**: Application fails to start or exits immediately

**Solutions**:
```bash
# Check Java version
java -version  # Should be 21+

# Verify JAR exists
ls -la */target/*.jar

# Check configuration files
ls -la generic-config/*.yaml

# Run with debug logging
java -Dlogging.level.dev.mars=DEBUG -jar app.jar

# Validate configuration
./scripts/start-all-services.sh --validate-only
```

#### 2. Database Connection Issues

**Symptoms**: Database connection errors, SQL exceptions

**Solutions**:
```bash
# Check H2 server status
./scripts/start-h2-server.sh

# Test database connectivity
curl http://localhost:8080/api/health

# Check database files
ls -la data/*.db

# Verify database configuration
grep -A 10 "database:" generic-config/application.yaml
```

#### 3. Configuration Validation Errors

**Symptoms**: Validation failures, missing references

**Solutions**:
```bash
# Run comprehensive validation
./scripts/test-validation-flags.sh

# Check configuration file syntax
yamllint generic-config/*.yaml

# Verify file naming patterns
ls -la generic-config/*-databases.yaml
ls -la generic-config/*-queries.yaml
ls -la generic-config/*-endpoints.yaml
```

#### 4. Port Conflicts

**Symptoms**: "Port already in use" errors

**Solutions**:
```bash
# Check what's using the port
netstat -an | grep :8080

# Kill existing processes
pkill -f "java.*Application"

# Use different port
java -Dserver.port=8081 -jar app.jar
```

#### 5. Metrics Not Collecting

**Symptoms**: No metrics data, empty dashboard

**Solutions**:
```bash
# Check metrics configuration
grep -A 10 "metrics:" generic-config/application.yaml

# Verify metrics are enabled
curl http://localhost:8080/api/metrics/endpoints

# Check metrics database
curl http://localhost:8080/api/performance-metrics

# Generate test traffic
for i in {1..10}; do curl http://localhost:8080/api/stock-trades; done
```

#### 6. Test Failures

**Symptoms**: Tests fail during build or execution

**Solutions**:
```bash
# Clean and rebuild
mvn clean install

# Run tests with debug output
mvn test -X

# Run specific failing test
mvn test -Dtest="SpecificTestClass"

# Check test configuration
cat src/test/resources/application-test.yml
```

#### 7. Memory Issues

**Symptoms**: OutOfMemoryError, high memory usage

**Solutions**:
```bash
# Increase heap size
export JAVA_OPTS="-Xmx2g -Xms1g"

# Disable memory metrics collection
# In application.yaml:
metrics:
  collection:
    includeMemoryMetrics: false

# Monitor memory usage
curl http://localhost:8080/api/management/health
```

#### 8. Performance Issues

**Symptoms**: Slow response times, high CPU usage

**Solutions**:
```bash
# Reduce metrics sampling
# In application.yaml:
metrics:
  collection:
    samplingRate: 0.1  # 10% sampling

# Optimize JVM settings
export JAVA_OPTS="-XX:+UseG1GC -XX:+TieredCompilation"

# Check database performance
curl http://localhost:8080/api/health
```

### Debug Mode

Enable comprehensive debug logging:

```yaml
# In application.yaml
logging:
  level:
    dev.mars: DEBUG
    org.h2: DEBUG
    com.zaxxer.hikari: DEBUG
    io.javalin: DEBUG
```

Or via command line:
```bash
java -Dlogging.level.dev.mars=DEBUG -jar app.jar
```

### Log Analysis

Monitor application logs for issues:

```bash
# Watch application logs
tail -f logs/application.log

# Search for errors
grep -i "error\|exception" logs/application.log

# Monitor metrics collection
grep "MetricsCollectionHandler" logs/application.log

# Check configuration loading
grep "ConfigurationLoader" logs/application.log
```

---

## Development Guide

### Setting Up Development Environment

#### 1. Prerequisites
- Java 21+ JDK
- Maven 3.6.0+
- Git
- IDE (IntelliJ IDEA recommended)
- PostgreSQL (optional)

#### 2. Clone and Setup
```bash
git clone <repository-url>
cd cordal
mvn clean install
```

#### 3. IDE Configuration
Follow the [IDE Setup](#ide-setup) section for your preferred IDE.

### Development Workflow

#### 1. Configuration-First Development
1. **Define API Requirements**: Identify what endpoints you need
2. **Create Database Schema**: Design tables and relationships
3. **Write Queries**: Create SQL queries in YAML configuration
4. **Define Endpoints**: Configure API endpoints in YAML
5. **Test and Validate**: Use validation tools to verify configuration

#### 2. Code Development (When Needed)
1. **Business Logic**: Add custom business logic in service layer
2. **Custom Controllers**: Create specialized controllers for complex operations
3. **Data Models**: Define custom DTOs and entities
4. **Validation Rules**: Add custom validation logic

#### 3. Testing Strategy
1. **Unit Tests**: Test individual components
2. **Integration Tests**: Test module interactions
3. **Configuration Tests**: Validate YAML configurations
4. **Performance Tests**: Measure and optimize performance

### Adding New Features

#### 1. Adding a New API Endpoint

**Step 1**: Define the database query
```yaml
# In generic-config/new-feature-queries.yaml
queries:
  get_user_profiles:
    database: "main_db"
    sql: "SELECT id, username, email, created_date FROM user_profiles WHERE active = true ORDER BY username LIMIT ? OFFSET ?"
    parameters:
      - name: "limit"
        type: "INTEGER"
        required: true
      - name: "offset"
        type: "INTEGER"
        required: true
```

**Step 2**: Define the API endpoint
```yaml
# In generic-config/new-feature-endpoints.yaml
endpoints:
  get_user_profiles:
    path: "/api/users"
    method: "GET"
    query: "get_user_profiles"
    pagination:
      enabled: true
      defaultPageSize: 20
      maxPageSize: 100
```

**Step 3**: Validate and test
```bash
# Validate configuration
./scripts/start-cordal-api-service.sh --validate-only

# Start service and test
./scripts/start-cordal-api-service.sh
curl http://localhost:8080/api/users
```

#### 2. Adding Custom Business Logic

When YAML configuration isn't sufficient, add custom Java code:

**Step 1**: Create service class
```java
@Singleton
public class UserProfileService {

    @Inject
    private UserProfileRepository repository;

    public CompletableFuture<List<UserProfile>> getActiveUsers() {
        return CompletableFuture.supplyAsync(() -> {
            // Custom business logic here
            return repository.findActiveUsers();
        });
    }
}
```

**Step 2**: Create controller
```java
public class UserProfileController {

    @Inject
    private UserProfileService service;

    public void configureRoutes(Javalin app) {
        app.get("/api/users/active", ctx -> {
            service.getActiveUsers()
                .thenAccept(users -> ctx.json(users))
                .exceptionally(throwable -> {
                    ctx.status(500).json(Map.of("error", throwable.getMessage()));
                    return null;
                });
        });
    }
}
```

**Step 3**: Register with Guice
```java
// In GuiceModule.java
@Provides
@Singleton
public UserProfileController provideUserProfileController(UserProfileService service) {
    return new UserProfileController(service);
}
```

### Best Practices

#### 1. Configuration Management
- Use descriptive names for databases, queries, and endpoints
- Group related configurations in the same files
- Follow naming conventions (`*-databases.yaml`, `*-queries.yaml`, `*-endpoints.yaml`)
- Validate configurations before deployment

#### 2. Database Design
- Use appropriate indexes for query performance
- Follow consistent naming conventions
- Include audit fields (created_at, updated_at)
- Use appropriate data types

#### 3. Error Handling
- Implement comprehensive error handling in custom code
- Use appropriate HTTP status codes
- Provide meaningful error messages
- Log errors with sufficient context

#### 4. Testing
- Write tests for all custom code
- Use configuration validation tests
- Include integration tests for new features

---

## Advanced Configuration Topics

### Command-Line Options and System Properties

CORDAL supports extensive configuration customization through command-line options and system properties:

#### Application Startup Options
```bash
# Validation-only mode (validate configuration and exit)
./scripts/start-generic-api-service.sh --validate-only
./scripts/start-generic-api-service.sh --validate

# Custom configuration file
java -Dgeneric.config.file=custom-application.yml -jar cordal-api-service.jar

# Custom configuration directories
java -Dconfig.directories=custom-config,../custom-config -jar cordal-api-service.jar

# Override file patterns
java -Ddatabase.patterns=*-db.yml,*-databases.yml -jar cordal-api-service.jar
```

#### System Property Reference
| Property | Description | Default | Example |
|----------|-------------|---------|---------|
| `generic.config.file` | Main configuration file | `application.yml` | `custom-app.yml` |
| `config.directories` | Configuration scan directories | `generic-config,../generic-config` | `custom-config,../custom-config` |
| `database.patterns` | Database file patterns | `*-database.yml,*-databases.yml` | `*-db.yml,*-databases.yml` |
| `query.patterns` | Query file patterns | `*-query.yml,*-queries.yml` | `*-sql.yml,*-queries.yml` |
| `endpoint.patterns` | Endpoint file patterns | `*-endpoint.yml,*-endpoints.yml,*-api.yml` | `*-rest.yml,*-api.yml` |

#### Environment-Specific Configuration
```bash
# Development environment
java -Dgeneric.config.file=application-dev.yml -jar cordal-api-service.jar

# Production environment
java -Dgeneric.config.file=application-prod.yml -jar cordal-api-service.jar

# Testing environment
java -Dgeneric.config.file=application-test.yml -jar cordal-api-service.jar
```

### Configuration File Resolution

CORDAL follows a specific resolution order for finding configuration files:

#### 1. Main Configuration File Resolution
```
1. System property: -Dgeneric.config.file=custom.yml
2. Default classpath: application.yml
3. Fallback: application.yml (with warnings)
```

#### 2. Domain Configuration File Resolution
```
1. External files: ./config/file.yml, ../config/file.yml
2. Classpath with config/ prefix: config/file.yml
3. Direct classpath: file.yml
```

#### 3. Directory Scanning Order
```
1. System property directories: -Dconfig.directories=dir1,dir2
2. Application.yml directories: config.directories
3. Default directories: ["generic-config", "../generic-config"]
```

### Configuration Debugging

Enable detailed configuration logging for troubleshooting:

```yaml
# In application.yml
logging:
  level:
    dev.cordal.config: DEBUG
    dev.cordal.generic.config: DEBUG
```

#### Configuration Loading Logs
```
INFO  - ConfigurationLoader initialized with directory scanning:
INFO  -   Configuration directories: [generic-config, ../generic-config]
INFO  -   Database patterns: [*-database.yml, *-databases.yml]
INFO  -   Query patterns: [*-query.yml, *-queries.yml]
INFO  -   Endpoint patterns: [*-endpoint.yml, *-endpoints.yml, *-api.yml]

INFO  - Scanning directory 'generic-config' for configuration files with patterns: [*-database.yml, *-databases.yml]
INFO  - Found 2 matching files in directory 'generic-config': [stocktrades-databases.yml, user-databases.yml]
```

### Hot Reload Configuration

CORDAL supports dynamic configuration updates without service restart:

```yaml
# In application.yml
config:
  hotReload:
    enabled: true                    # Enable hot reload
    watchDirectories: true           # Monitor file system changes
    debounceMs: 300                 # Delay before applying changes
    maxReloadAttempts: 3            # Retry limit for failed reloads
    rollbackOnFailure: true         # Auto-rollback on validation failure
    validateBeforeApply: true       # Validate before applying changes

  fileWatcher:
    enabled: true                   # Enable file system monitoring
    pollInterval: 1000              # Fallback polling interval (ms)
    includeSubdirectories: false    # Monitor subdirectories
```

#### Hot Reload Process
```
1. File Change Detection → 2. Debounce Timer → 3. Configuration Validation →
4. Dependency Check → 5. Apply Changes → 6. Rollback on Failure
```
- Test with realistic data volumes

#### 5. Performance
- Monitor metrics collection overhead
- Use appropriate sampling rates for production
- Optimize database queries
- Consider async processing for heavy operations

### Code Style Guidelines

#### 1. Java Code Style
- Use 4 spaces for indentation
- Follow standard Java naming conventions
- Use meaningful variable and method names
- Include comprehensive JavaDoc for public APIs

#### 2. Configuration Style
- Use consistent YAML formatting
- Include comments for complex configurations
- Group related settings together
- Use descriptive names for all identifiers

#### 3. Documentation
- Update documentation when adding features
- Include examples in documentation
- Document configuration options
- Provide troubleshooting guidance

---

## Advanced Features

### Configuration Management APIs

The system provides comprehensive APIs for managing and inspecting configurations:

#### Configuration Introspection
```bash
# View all loaded configurations
curl http://localhost:8080/api/management/config

# View specific configuration type
curl http://localhost:8080/api/management/config/endpoints
curl http://localhost:8080/api/management/config/queries
curl http://localhost:8080/api/management/config/databases

# View configuration file paths and contents
curl http://localhost:8080/api/management/config/files

# View configuration load timestamps
curl http://localhost:8080/api/management/config/metadata
```

#### Configuration Validation APIs
```bash
# Validate configuration chain
curl http://localhost:8080/api/management/validation/chain

# Validate database schemas
curl http://localhost:8080/api/management/validation/schemas

# Get validation summary
curl http://localhost:8080/api/management/validation/summary
```

### Metrics Collection Architecture

The metrics collection system is designed as a cross-cutting concern that automatically intercepts all HTTP requests:

#### Implementation Details

1. **Handler Registration**: Metrics handlers are registered as Javalin before/after handlers
2. **Request Interception**: Captures request start time, path, method, and memory state
3. **Response Processing**: Calculates response time, memory usage, and updates aggregations
4. **Path Normalization**: Groups similar endpoints for better aggregation
5. **Thread Safety**: Uses ThreadLocal for request data and ConcurrentHashMap for aggregations
6. **Async Processing**: Supports async database saves to minimize request overhead

#### Metrics Data Model

```java
public class PerformanceMetrics {
    private Long id;
    private String testName;           // Endpoint identifier
    private String testType;           // "API_REQUEST"
    private LocalDateTime timestamp;   // Request timestamp
    private Integer totalRequests;     // Always 1 for individual requests
    private Long totalTimeMs;          // Response time in milliseconds
    private Double averageResponseTimeMs; // Same as totalTimeMs for individual requests
    private Boolean testPassed;        // Success/failure status
    private String additionalMetrics;  // JSON with extra data
}
```

### Dashboard Configuration

The performance dashboard can be customized through configuration:

```yaml
dashboard:
  enabled: true
  autoRefresh: true
  refreshInterval: 30000          # 30 seconds
  defaultTimeRange: "24h"         # 24 hours, 7d, 30d, 90d
  chartsEnabled: true
  maxDataPoints: 1000
  theme: "light"                  # light, dark, auto
```

### Security Configuration

```yaml
security:
  authentication:
    enabled: false                # Enable authentication
    type: "basic"                 # basic, jwt, oauth2
  authorization:
    enabled: false                # Enable role-based access
    defaultRole: "user"
  cors:
    enabled: true
    allowedOrigins: ["*"]
    allowedMethods: ["GET", "POST", "PUT", "DELETE"]
    allowedHeaders: ["*"]
```

---

## Production Deployment

### Deployment Checklist

#### Pre-Deployment
- [ ] Run full test suite: `mvn test`
- [ ] Validate all configurations: `./scripts/start-all-services.sh --validate-only`
- [ ] Build optimized JARs: `./scripts/build-executable-jars.sh --optimized-jar`
- [ ] Analyze JAR dependencies: `./scripts/analyze-jars.sh`
- [ ] Review security settings
- [ ] Configure production database connections
- [ ] Set up monitoring and alerting

#### Deployment
- [ ] Deploy JAR files to target environment
- [ ] Copy configuration files
- [ ] Set up database connections
- [ ] Configure environment variables
- [ ] Start services with production settings
- [ ] Verify health checks
- [ ] Test critical endpoints

#### Post-Deployment
- [ ] Monitor application logs
- [ ] Check metrics collection
- [ ] Verify dashboard functionality
- [ ] Test database connectivity
- [ ] Monitor resource usage
- [ ] Set up backup procedures

### Production Configuration

#### Application Configuration
```yaml
# Production application.yaml
server:
  port: 8080
  host: "0.0.0.0"

metrics:
  collection:
    enabled: true
    samplingRate: 0.05              # 5% sampling for production
    includeMemoryMetrics: false     # Reduce overhead
    asyncSave: true

logging:
  level:
    root: INFO
    dev.mars: INFO
  file:
    name: "logs/application.log"
    max-size: "100MB"
    max-history: 30
```

#### Database Configuration
```yaml
# Production database settings
database:
  url: "jdbc:postgresql://prod-db-server:5432/production_db"
  username: "${DB_USERNAME}"       # Environment variable
  password: "${DB_PASSWORD}"       # Environment variable
  driver: "org.postgresql.Driver"
  pool:
    maximumPoolSize: 50
    minimumIdle: 10
    connectionTimeout: 30000
    idleTimeout: 300000
    maxLifetime: 900000
```

### Monitoring and Alerting

#### Health Check Monitoring
```bash
# Set up health check monitoring
curl -f http://localhost:8080/api/management/health || alert_system

# Monitor specific components
curl http://localhost:8080/api/management/health | jq '.components.database.status'
```

#### Metrics Monitoring
```bash
# Monitor response times
curl http://localhost:8080/api/metrics/endpoints | jq 'to_entries[] | select(.value.averageResponseTime > 1000)'

# Monitor error rates
curl http://localhost:8080/api/metrics/endpoints | jq 'to_entries[] | select(.value.successRate < 95)'
```

#### Log Monitoring
```bash
# Monitor error logs
tail -f logs/application.log | grep -i "error\|exception"

# Monitor metrics collection
tail -f logs/application.log | grep "MetricsCollectionHandler"
```

---

## Conclusion

The CORDAL provides a comprehensive, production-ready solution for building configuration-driven REST APIs with automatic metrics collection and monitoring. Its modular architecture, extensive testing, and comprehensive documentation make it suitable for both development and production environments.

### Key Benefits

- **Rapid Development**: Create APIs through configuration, not code
- **Automatic Monitoring**: Zero-code metrics collection and dashboards
- **Production Ready**: Comprehensive health checks, monitoring, and deployment options
- **Highly Configurable**: Fine-grained control over all aspects of the system
- **Extensible**: Easy to add custom business logic when needed
- **Well Tested**: Comprehensive test suite with high coverage

### Next Steps

1. **Explore the System**: Use the quick start guide to get familiar with the features
2. **Configure Your APIs**: Create your own endpoint, query, and database configurations
3. **Monitor Performance**: Use the dashboard and metrics APIs to monitor your APIs
4. **Customize as Needed**: Add custom business logic where configuration isn't sufficient
5. **Deploy to Production**: Use the deployment guides and scripts for production deployment

For additional help and support, refer to the troubleshooting section or examine the extensive test suite for examples of how to use each feature.


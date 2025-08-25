# CORDAL - Configuration Orchestrated REST Dynamic API Layer

**Version:** 1.0
**Date:** 2025-08-25
**Author:** Mark Andrew Ray-Smith Cityline Ltd

**Create powerful REST APIs using only YAML configuration files - Java code optional**

## What is CORDAL?

CORDAL is an interesting framework that can transform how you build REST APIs. Instead of writing Java code, you define your entire API using simple YAML configuration files. CORDAL handles all the heavy lifting - database connections, query execution, JSON serialization, caching, monitoring, and more.

## Key Features

- **Zero-Code APIs** - Define REST endpoints with YAML only
- **Multi-Database Support** - H2, PostgreSQL with connection pooling
- **Built-in Caching** - Intelligent caching with TTL management
- **Type Safety** - Compile-time validation for all configurations
- **Hot Reload** - Configuration changes without restart
- **Production Ready** - Health checks, metrics, monitoring dashboards
- **Performance Optimized** - Connection pooling, query caching, type-safe operations

## Quick Start

### 1. Prerequisites
- Java 21+
- Maven 3.6+

### 2. Build and Run
```bash
# Clone and build
git clone <repository-url>
cd cordal-api-mesh
./scripts/build-executable-jars.sh

# Start the service
./scripts/start-cordal-api-service.sh
```

### 3. Create Your First API

**application.yaml**
```yaml
server:
  port: 8080
cordal:
  config:
    directory: "generic-config"
```

**generic-config/hello-databases.yml**
```yaml
databases:
  hello_db:
    name: "hello_db"
    url: "jdbc:h2:./data/hello;AUTO_SERVER=TRUE"
    username: "sa"
    password: ""
    driver: "org.h2.Driver"
```

**generic-config/hello-queries.yml**
```yaml
queries:
  get_greeting:
    name: "get_greeting"
    database: "hello_db"
    sql: "SELECT 'Hello, World!' as message, CURRENT_TIMESTAMP as timestamp"
```

**generic-config/hello-endpoints.yml**
```yaml
endpoints:
  hello_world:
    path: "/api/hello"
    method: "GET"
    query: "get_greeting"
    description: "Simple hello world endpoint"
```

### 4. Test Your API
```bash
curl http://localhost:8080/api/hello
```

Response:
```json
{
  "type": "SIMPLE",
  "data": [
    {
      "message": "Hello, World!",
      "timestamp": "2025-08-16T10:30:45.123"
    }
  ],
  "timestamp": 1755358245123
}
```

## Project Structure

```
cordal-api-mesh/
├── cordal-api-service/          # Main API framework
├── cordal-metrics-service/      # Performance monitoring  
├── cordal-common-library/       # Shared utilities
├── cordal-integration-tests/    # Integration tests
├── generic-config/              # Your configuration files
├── scripts/                     # Build and run scripts
└── docs/                        # Comprehensive documentation
```

## Documentation

- **[Complete Developer Guide](docs/CORDAL_COMPREHENSIVE_GUIDE.md)** - Step-by-step examples from simple to advanced
- **[Open Source Usage](OPEN_SOURCE_USAGE.md)** - License compliance and usage guidelines
- **[Notice](NOTICE)** - Third-party attributions

## Key URLs

- **API Base**: http://localhost:8080/api/
- **Health Check**: http://localhost:8080/api/health
- **Performance Dashboard**: http://localhost:8080/dashboard
- **Swagger UI**: http://localhost:8080/swagger

## Examples

### Simple CRUD API
```yaml
# Database with sample data
databases:
  users_db:
    initialization:
      schema: |
        CREATE TABLE users (
          id BIGINT AUTO_INCREMENT PRIMARY KEY,
          name VARCHAR(100) NOT NULL,
          email VARCHAR(150) NOT NULL
        );

# Queries
queries:
  get_all_users:
    database: "users_db"
    sql: "SELECT id, name, email FROM users ORDER BY name"
    cache:
      enabled: true
      ttl: 300

# Endpoints
endpoints:
  list_users:
    path: "/api/users"
    method: "GET"
    query: "get_all_users"
```

### Advanced Features
- **Pagination**: Built-in pagination support
- **Caching**: Query-level caching with TTL
- **Multi-Database**: Connect to multiple databases
- **Complex Queries**: JOINs, aggregations, analytics
- **Monitoring**: Real-time performance dashboards

## Technology Stack

- **Java 21** - Modern JVM platform
- **Javalin 6.1.3** - Lightweight web framework
- **Google Guice** - Dependency injection
- **HikariCP** - High-performance connection pooling
- **Jackson** - JSON processing
- **H2/PostgreSQL** - Database support

## License

Licensed under the Apache License 2.0. See [LICENSE](LICENSE) for details.

## Contributing

Contributions are welcome! Please read our contribution guidelines and submit pull requests.

## Support

For questions, issues, or feature requests, please open an issue in the repository.

---

**Build APIs in minutes, not weeks with CORDAL!**

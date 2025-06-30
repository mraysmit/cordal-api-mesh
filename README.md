# Javalin API Mesh

A highly modularized Javalin-based REST API Mesh.

## Features

- **RESTful API** 
- **H2 File-based Database**
- **Pagination Support** for large datasets
- **Async Operations** using CompletableFuture
- **Dependency Injection** with Google Guice
- **YAML Configuration** for easy environment management
- **Comprehensive Logging** with Logback
- **Exception Handling** with custom error responses
- **Connection Pooling** with HikariCP
- **Sample Data Loading** (configurable)

## Architecture

The project follows a layered architecture with proper separation of concerns and uses a generic API system configured via YAML:

```
├── Application.java           # Main entry point
├── config/                   # Configuration classes
│   ├── AppConfig.java        # YAML configuration loader
│   ├── DatabaseConfig.java   # Database connection setup
│   └── GuiceModule.java      # Dependency injection setup
├── controller/               # REST controllers
│   └── PerformanceMetricsController.java
├── service/                  # Business logic layer
│   └── PerformanceMetricsService.java
├── repository/               # Data access layer
│   └── PerformanceMetricsRepository.java
├── model/                    # Entity classes
│   └── PerformanceMetrics.java
├── dto/                      # Data transfer objects
│   └── PagedResponse.java
├── database/                 # Database management
│   ├── DatabaseManager.java
│   └── DataLoader.java
├── exception/                # Exception handling
│   ├── ApiException.java
│   └── GlobalExceptionHandler.java
├── generic/                  # Generic API system
│   ├── GenericApiController.java
│   ├── GenericApiService.java
│   ├── GenericRepository.java
│   └── config/               # Configuration management
└── routes/                   # Route configuration
    └── ApiRoutes.java
```

## API Endpoints

### Generic API System

The application now uses a generic API system configured via YAML files. Stock trades and other APIs are defined in configuration files rather than hard-coded controllers.

#### Stock Trades (Generic API)

- `GET /api/generic/stock-trades` - Get all stock trades with pagination
  - Query parameters: `page` (default: 0), `size` (default: 20), `async` (default: false)

- `GET /api/generic/stock-trades/{id}` - Get stock trade by ID

- `GET /api/generic/stock-trades/symbol/{symbol}` - Get stock trades by symbol with pagination
  - Query parameters: `page` (default: 0), `size` (default: 20), `async` (default: false)

- `GET /api/generic/stock-trades/trader/{trader_id}` - Get stock trades by trader ID

- `GET /api/generic/stock-trades/date-range` - Get stock trades by date range

#### Configuration Management

- `GET /api/generic/config/validate` - Validate all configurations
- `GET /api/generic/config/validate/endpoints` - Validate endpoint configurations
- `GET /api/generic/config/validate/queries` - Validate query configurations
- `GET /api/generic/config/validate/databases` - Validate database configurations
- `GET /api/generic/config/validate/relationships` - Validate configuration relationships

### Health Check

- `GET /api/health` - Health check endpoint

## Configuration

### Application Configuration

The application is configured via `src/main/resources/application.yml`:

```yaml
server:
  port: 8080
  host: "localhost"

database:
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

data:
  loadSampleData: true
  sampleDataSize: 100
```

### Generic API Configuration

The application uses a generic API system with three main configuration files:

#### 1. API Endpoints (`src/main/resources/config/api-endpoints.yml`)
Defines REST API endpoints, their paths, methods, and parameters.

#### 2. Database Queries (`src/main/resources/config/queries.yml`)
Defines named SQL queries with parameters and validation.

#### 3. Database Connections (`src/main/resources/config/databases.yml`)
Defines database connection configurations.

This approach allows for:
- **Externalized Configuration**: API endpoints and queries are defined in YAML files
- **Dynamic API Creation**: New APIs can be added without code changes
- **Configuration Validation**: Built-in validation ensures configuration integrity
- **Relationship Management**: Automatic validation of relationships between endpoints, queries, and databases

## Running the Application

### Prerequisites

- Java 21 or higher
- Maven 3.6 or higher

### Build and Run

1. Clone the repository
2. Build the project:
   ```bash
   mvn clean compile
   ```

3. Run the application:
   ```bash
   mvn exec:java -Dexec.mainClass="dev.mars.Application"
   ```

4. The application will start on `http://localhost:8080`

### Testing

Run the tests:
```bash
mvn test
```

## Sample API Calls

### Get all stock trades
```bash
curl "http://localhost:8080/api/stock-trades?page=0&size=10"
```

### Get stock trade by ID
```bash
curl "http://localhost:8080/api/stock-trades/1"
```

### Get stock trades by symbol
```bash
curl "http://localhost:8080/api/stock-trades/symbol/AAPL?page=0&size=5"
```

### Async operations
```bash
curl "http://localhost:8080/api/stock-trades?async=true&page=0&size=10"
```

### Health check
```bash
curl "http://localhost:8080/api/health"
```

## Database

The application uses H2 file-based database stored in `./data/stocktrades.mv.db`. The database schema is automatically created on startup, and sample data is loaded if configured.

### Database Schema

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

## Logging

Logs are written to both console and `logs/application.log` with daily rotation. Log levels can be configured in `application.yml`.

## Technologies Used

- **Javalin 6.1.3** - Lightweight web framework
- **H2 Database 2.2.224** - Embedded database
- **HikariCP 5.1.0** - Connection pooling
- **Jackson 2.17.1** - JSON serialization
- **Google Guice 7.0.0** - Dependency injection
- **SnakeYAML 2.2** - YAML configuration
- **Logback 1.5.6** - Logging framework
- **JUnit 5.10.2** - Testing framework

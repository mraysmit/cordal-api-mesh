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

The project follows a layered architecture with proper separation of concerns:

```
├── Application.java           # Main entry point
├── config/                   # Configuration classes
│   ├── AppConfig.java        # YAML configuration loader
│   ├── DatabaseConfig.java   # Database connection setup
│   └── GuiceModule.java      # Dependency injection setup
├── controller/               # REST controllers
│   └── StockTradeController.java
├── service/                  # Business logic layer
│   └── StockTradeService.java
├── repository/               # Data access layer
│   └── StockTradeRepository.java
├── model/                    # Entity classes
│   └── StockTrade.java
├── dto/                      # Data transfer objects
│   ├── StockTradeDto.java
│   └── PagedResponse.java
├── database/                 # Database management
│   ├── DatabaseManager.java
│   └── DataLoader.java
├── exception/                # Exception handling
│   ├── ApiException.java
│   └── GlobalExceptionHandler.java
└── routes/                   # Route configuration
    └── ApiRoutes.java
```

## API Endpoints

### Stock Trades

- `GET /api/stock-trades` - Get all stock trades with pagination
  - Query parameters: `page` (default: 0), `size` (default: 20), `async` (default: false)
  
- `GET /api/stock-trades/{id}` - Get stock trade by ID
  - Query parameters: `async` (default: false)
  
- `GET /api/stock-trades/symbol/{symbol}` - Get stock trades by symbol with pagination
  - Query parameters: `page` (default: 0), `size` (default: 20), `async` (default: false)

### Health Check

- `GET /api/health` - Health check endpoint

## Configuration

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

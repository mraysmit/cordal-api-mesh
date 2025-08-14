# Dual PostgreSQL Integration Test

## Overview

This comprehensive integration test demonstrates the **CORDAL** framework's capability to standardize access to multiple similar PostgreSQL databases through a unified REST API interface. The test showcases the framework's core value proposition: **zero-code API creation** for database standardization.

## Test Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Integration Test Architecture                 │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐    ┌─────────────────┐                    │
│  │   PostgreSQL    │    │   PostgreSQL    │                    │
│  │   Container 1   │    │   Container 2   │                    │
│  │  (trades_db_1)  │    │  (trades_db_2)  │                    │
│  └─────────────────┘    └─────────────────┘                    │
│           │                       │                             │
│           └───────────┬───────────┘                             │
│                       │                                         │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │            Dynamic Configuration Generator              │ │
│  │  • Database Configurations (YAML)                      │ │
│  │  • Query Configurations (YAML)                         │ │
│  │  • API Endpoint Configurations (YAML)                  │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                       │                                         │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │              Generic API Service                        │ │
│  │  • Loads configurations dynamically                    │ │
│  │  • Creates REST endpoints automatically                │ │
│  │  • Provides unified API for both databases             │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                       │                                         │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                REST API Client                          │ │
│  │  • Tests all endpoints as external consumer            │ │
│  │  • Validates responses and data consistency            │ │
│  │  • Performs cross-database comparisons                 │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## Test Phases

### Phase 1: Infrastructure Setup
- **PostgreSQL Testcontainers**: Adds PostgreSQL testcontainers dependency
- **Test Data Generator**: Creates realistic stock trade data with varied patterns
- **Container Management**: Handles lifecycle of dual PostgreSQL containers

### Phase 2: Database Container Management  
- **Container Creation**: Spins up two PostgreSQL containers with different databases
- **Schema Initialization**: Creates identical `stock_trades` tables with proper indexes
- **Data Population**: Inserts 100 realistic stock trade records into each database

### Phase 3: Configuration Generation
- **Dynamic Database Config**: Generates YAML configurations for both PostgreSQL instances
- **Query Configuration**: Creates standardized SQL queries for both databases
- **Endpoint Configuration**: Generates REST API endpoints for unified access

### Phase 4: Integration Test Implementation
- **API Service Startup**: Starts Generic API Service with dual database configuration
- **Health Validation**: Ensures service is responding and configurations are loaded
- **Endpoint Discovery**: Validates that all expected endpoints are available

### Phase 5: API Client Testing
- **REST API Client**: Comprehensive testing of all generated endpoints
- **Pagination Testing**: Validates pagination works correctly for both databases
- **Filtering Testing**: Tests symbol-based and trader-based filtering
- **Response Validation**: Ensures API responses match expected formats

### Phase 6: Data Validation and Comparison
- **Database Consistency**: Validates data integrity within each database
- **Cross-Database Comparison**: Compares data structures and distributions
- **API-Database Validation**: Ensures API responses match database content
- **Performance Monitoring**: Tracks response times and system performance

### Phase 7: Error Handling and Edge Cases
- **Database Diagnostics**: Tests connectivity and generates diagnostic information
- **API Error Handling**: Validates proper error responses for invalid requests
- **Performance Testing**: Basic load testing with concurrent requests
- **Resource Management**: Ensures proper cleanup of all resources

### Phase 8: Documentation and Examples
- **Test Documentation**: Comprehensive documentation of test architecture
- **Reusable Utilities**: Extracted utilities for future integration tests
- **Performance Insights**: Analysis of test execution times and bottlenecks

## Key Features Demonstrated

### 🎯 **Zero-Code API Creation**
The test demonstrates how the framework can create fully functional REST APIs without writing any code:
1. Define database connections in YAML
2. Define SQL queries in YAML  
3. Define API endpoints in YAML
4. Framework automatically creates working REST APIs

### 🔄 **Database Standardization**
Shows how multiple similar databases can be accessed through a unified interface:
- Both databases have identical schemas but different data
- Single API provides standardized access to both
- Consistent response formats across all endpoints
- Unified pagination and filtering capabilities

### 📊 **Dynamic Configuration**
Demonstrates runtime configuration generation:
- Configurations generated based on container connection info
- No hardcoded database URLs or ports
- Fully self-contained test with no external dependencies

### 🧪 **Comprehensive Testing**
Validates all aspects of the framework:
- Database connectivity and schema validation
- Configuration loading and validation
- API endpoint functionality and performance
- Data consistency and cross-database comparison
- Error handling and edge cases

## Generated API Endpoints

The test generates the following REST API endpoints for each database:

### Database 1 Endpoints (`/api/trades-db-1/`)
- `GET /api/trades-db-1/stock-trades` - Get all stock trades with pagination
- `GET /api/trades-db-1/stock-trades/symbol/{symbol}` - Filter by stock symbol
- `GET /api/trades-db-1/stock-trades/trader/{trader_id}` - Filter by trader ID

### Database 2 Endpoints (`/api/trades-db-2/`)
- `GET /api/trades-db-2/stock-trades` - Get all stock trades with pagination
- `GET /api/trades-db-2/stock-trades/symbol/{symbol}` - Filter by stock symbol
- `GET /api/trades-db-2/stock-trades/trader/{trader_id}` - Filter by trader ID

### Configuration Management Endpoints
- `GET /api/generic/config/validate` - Validate all configurations
- `GET /api/generic/config/endpoints` - List all configured endpoints
- `GET /api/generic/config/queries` - List all configured queries
- `GET /api/generic/config/databases` - List all configured databases

## Test Data

Each database contains 100 realistic stock trade records with:
- **24 different stock symbols** (AAPL, GOOGL, MSFT, etc.)
- **18 different trader IDs** (TRADER001, ALGO_TRADER_A, etc.)
- **5 different exchanges** (NYSE, NASDAQ, AMEX, etc.)
- **Realistic price ranges** based on stock type
- **Varied quantities** with bias toward smaller trades
- **Trade dates** within the last 30 days
- **Proper constraints** (total_value = quantity × price)

## Running the Test

### Prerequisites
- Java 21 or higher
- Maven 3.8 or higher
- Docker (for TestContainers)

### Execution
```bash
# From the cordal-integration-tests directory
mvn test -Dtest=DualPostgreSQLIntegrationTest

# Or from the project root
mvn test -pl cordal-integration-tests -Dtest=DualPostgreSQLIntegrationTest
```

### Expected Output
The test produces detailed logging showing:
- Container startup and database initialization
- Configuration generation and file creation
- API service startup and endpoint registration
- Comprehensive API testing results
- Data validation and consistency checks
- Performance metrics and timing analysis

## Success Criteria

✅ **Container Management**
- Two PostgreSQL containers start successfully
- Both databases are initialized with identical schemas
- 100 sample records are inserted into each database

✅ **Configuration Generation**
- YAML configurations are generated dynamically
- All configuration files are valid and complete
- Application configuration references generated files correctly

✅ **API Service Integration**
- Generic API Service starts with dual database configuration
- All expected endpoints are registered and accessible
- Swagger documentation is generated correctly

✅ **API Functionality**
- All REST endpoints work for both databases
- Pagination works correctly with proper metadata
- Filtering by symbol and trader works as expected
- Response formats are consistent and valid

✅ **Data Validation**
- Data consistency is validated within each database
- Cross-database structure comparison passes
- API responses match database content exactly
- Performance metrics are within acceptable ranges

✅ **Resource Management**
- All containers are stopped and cleaned up properly
- Temporary configuration files are removed
- No resource leaks or hanging processes

## Performance Expectations

Based on typical test runs:
- **Total execution time**: 2-4 minutes
- **Container startup**: 30-60 seconds
- **Schema and data creation**: 10-20 seconds
- **Configuration generation**: < 1 second
- **API service startup**: 15-30 seconds
- **API testing**: 10-20 seconds
- **Data validation**: 5-10 seconds

## Troubleshooting

### Common Issues

**Docker not available**
- Ensure Docker is installed and running
- Check that your user has permission to access Docker

**Port conflicts**
- The test uses dynamic ports for PostgreSQL containers
- API service runs on port 19080 (configurable)

**Memory issues**
- TestContainers requires sufficient memory for PostgreSQL containers
- Increase Docker memory allocation if needed

**Slow performance**
- Container startup can be slow on some systems
- Consider using Docker image caching
- Check system resources (CPU, memory, disk)

### Debug Logging

Enable debug logging for more detailed output:
```bash
mvn test -Dtest=DualPostgreSQLIntegrationTest -Dlogback.configurationFile=logback-debug.xml
```

## Integration with CI/CD

This test is designed to run in CI/CD environments:
- Uses TestContainers for isolated database instances
- No external dependencies or manual setup required
- Comprehensive validation with clear pass/fail criteria
- Detailed logging for troubleshooting failures
- Proper resource cleanup prevents environment pollution

## Value Demonstration

This integration test powerfully demonstrates the **CORDAL** framework's value:

1. **Rapid Development**: Create REST APIs in minutes, not days
2. **Database Standardization**: Unify access to similar databases
3. **Zero Maintenance**: Configuration-driven approach reduces code maintenance
4. **Scalability**: Easy to add new databases or modify existing ones
5. **Reliability**: Comprehensive validation ensures robust deployments
6. **Observability**: Built-in performance monitoring and health checks

The test serves as both validation and documentation, showing exactly how the framework can solve real-world database integration challenges.

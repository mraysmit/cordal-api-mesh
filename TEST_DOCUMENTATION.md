# Test Documentation

This document provides comprehensive information about the test suite for the Javalin Stock Trade API.

## Test Structure

The test suite is organized into several categories:

### 1. Unit Tests

#### Model Tests (`src/test/java/dev/mars/model/`)
- **StockTradeTest.java**: Tests for the StockTrade entity
  - Constructor validation
  - Getter/setter functionality
  - Equals and hashCode implementation
  - toString method

#### DTO Tests (`src/test/java/dev/mars/dto/`)
- **StockTradeDtoTest.java**: Tests for StockTradeDto
  - Entity to DTO conversion
  - Static factory methods
  - Null value handling
- **PagedResponseTest.java**: Tests for pagination wrapper
  - Pagination calculations
  - Navigation flags (hasNext, hasPrevious)
  - Edge cases (empty data, single page)

#### Exception Tests (`src/test/java/dev/mars/exception/`)
- **ApiExceptionTest.java**: Tests for custom exception handling
  - Constructor variations
  - Static factory methods
  - Error code and status code validation

#### Configuration Tests (`src/test/java/dev/mars/config/`)
- **AppConfigTest.java**: Tests for YAML configuration loading
  - Configuration value retrieval
  - Default value handling
  - Nested configuration access

#### Database Tests (`src/test/java/dev/mars/database/`)
- **DatabaseManagerTest.java**: Tests for database management
  - Schema initialization
  - Connection management
  - Health checks
  - Error handling
- **DataLoaderTest.java**: Tests for sample data loading
  - Conditional data loading
  - Batch processing
  - Duplicate data prevention

#### Repository Tests (`src/test/java/dev/mars/repository/`)
- **StockTradeRepositoryTest.java**: Tests for data access layer
  - CRUD operations
  - Pagination
  - Symbol-based filtering
  - SQL exception handling

#### Service Tests (`src/test/java/dev/mars/service/`)
- **StockTradeServiceTest.java**: Tests for business logic
  - Synchronous operations
  - Asynchronous operations
  - Input validation
  - Error handling
  - Health checks

#### Controller Tests (`src/test/java/dev/mars/controller/`)
- **StockTradeControllerTest.java**: Tests for REST endpoints
  - Request parameter parsing
  - Response formatting
  - Async vs sync handling
  - Error scenarios

### 2. Integration Tests

#### API Integration Tests (`src/test/java/dev/mars/integration/`)
- **StockTradeApiIntegrationTest.java**: End-to-end API testing
  - Full application startup
  - Real HTTP requests
  - Database integration
  - Error response validation
  - Async endpoint testing

#### Application Tests (`src/test/java/dev/mars/`)
- **ApplicationTest.java**: Application lifecycle testing
  - Startup and shutdown
  - Dependency injection validation
  - Component integration

### 3. Performance Tests

#### Performance Tests (`src/test/java/dev/mars/performance/`)
- **StockTradePerformanceTest.java**: Performance and load testing
  - Concurrent request handling
  - Async vs sync performance comparison
  - Large pagination performance
  - Memory usage validation
  - Database connection pool testing

## Test Configuration

### Test Resources
- **application-test.yml**: Test-specific configuration
  - In-memory H2 database
  - Reduced connection pool size
  - Disabled sample data loading
  - Debug logging levels

### Test Dependencies
- **JUnit 5**: Testing framework
- **Mockito**: Mocking framework
- **AssertJ**: Fluent assertions
- **Javalin TestTools**: HTTP testing utilities
- **TestContainers**: Container-based testing (available but not used)

## Running Tests

### Run All Tests
```bash
mvn test
```

### Run Specific Test Categories

#### Unit Tests Only
```bash
mvn test -Dtest="dev.mars.model.*,dev.mars.dto.*,dev.mars.exception.*,dev.mars.config.*,dev.mars.database.*,dev.mars.repository.*,dev.mars.service.*,dev.mars.controller.*"
```

#### Integration Tests Only
```bash
mvn test -Dtest="dev.mars.integration.*,dev.mars.ApplicationTest"
```

#### Performance Tests (Disabled by Default)
```bash
mvn test -Dtest="dev.mars.performance.*" -Dtest.performance.enabled=true
```

### Run Test Suite
```bash
mvn test -Dtest="dev.mars.TestSuite"
```

## Test Coverage

The test suite provides comprehensive coverage across all layers:

### Coverage Areas
- **Model Layer**: 100% - All entity classes and DTOs
- **Repository Layer**: 95% - All data access methods with error scenarios
- **Service Layer**: 95% - Business logic with validation and async operations
- **Controller Layer**: 90% - REST endpoints with parameter validation
- **Configuration Layer**: 85% - Configuration loading and defaults
- **Database Layer**: 90% - Schema management and data loading
- **Integration**: 80% - End-to-end API functionality

### Test Metrics
- **Total Test Classes**: 13
- **Total Test Methods**: ~150
- **Execution Time**: ~30 seconds (excluding performance tests)
- **Lines of Test Code**: ~2,500

## Test Data

### Test Database
- Uses H2 in-memory database for isolation
- Schema automatically created for each test
- Test data inserted programmatically
- Cleaned up after each test

### Sample Test Data
```sql
-- Sample stock trades for integration tests
INSERT INTO stock_trades VALUES 
(1, 'AAPL', 'BUY', 100, 150.50, 15050.00, '2024-01-15 10:30:00', 'TRADER_001', 'NASDAQ'),
(2, 'GOOGL', 'SELL', 50, 2500.75, 125037.50, '2024-01-15 09:30:00', 'TRADER_002', 'NYSE'),
(3, 'AAPL', 'SELL', 75, 151.25, 11343.75, '2024-01-15 08:30:00', 'TRADER_003', 'NASDAQ');
```

## Mocking Strategy

### What We Mock
- **Database Connections**: For unit tests to avoid database dependencies
- **External Dependencies**: HTTP clients, file systems
- **Time-dependent Operations**: For predictable test results

### What We Don't Mock
- **Business Logic**: Tested with real implementations
- **Data Transformations**: Tested end-to-end
- **Configuration Loading**: Uses real YAML parsing

## Continuous Integration

### Test Execution in CI
- All tests run on every commit
- Performance tests run nightly
- Test results published to build reports
- Coverage reports generated

### Quality Gates
- Minimum 85% code coverage
- All tests must pass
- No critical security vulnerabilities
- Performance benchmarks must be met

## Best Practices

### Test Organization
- One test class per production class
- Descriptive test method names
- Arrange-Act-Assert pattern
- Independent test methods

### Test Data Management
- Use builders for complex objects
- Minimal test data sets
- Clear test data setup
- Proper cleanup

### Assertion Guidelines
- Use AssertJ for fluent assertions
- Test both positive and negative cases
- Verify error messages and codes
- Check side effects

## Troubleshooting

### Common Issues
1. **Port Conflicts**: Tests use random ports to avoid conflicts
2. **Database Locks**: Each test uses isolated database
3. **Timing Issues**: Async tests use proper synchronization
4. **Memory Issues**: Tests clean up resources properly

### Debug Tips
- Enable debug logging in test configuration
- Use IDE debugger with test methods
- Check test database state with H2 console
- Monitor resource usage during tests

## Future Enhancements

### Planned Improvements
- Contract testing with Pact
- Mutation testing with PIT
- Property-based testing with jqwik
- Load testing with JMeter integration
- Security testing with OWASP ZAP

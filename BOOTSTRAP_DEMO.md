# System Bootstrap Demonstration

This document describes the comprehensive system bootstrap demonstration that validates and tests the entire Javalin API Mesh system.

## Overview

The System Bootstrap Demonstration (`SystemBootstrapDemo`) is a comprehensive testing and validation tool that:

1. **Starts the Generic API Service** programmatically
2. **Tests all management API endpoints** systematically
3. **🆕 Performs dynamic API validation** with comprehensive configuration and database schema validation
4. **Checks for Metrics Service availability** and tests if running
5. **Provides detailed results and statistics** with formatted output
6. **Gracefully shuts down services** after demonstration

## Features

### 🚀 Service Management
- **Automatic Service Startup**: Programmatically starts the Generic API Service
- **Health Check Verification**: Waits for services to be ready before testing
- **Graceful Shutdown**: Properly stops services after demonstration

### 🔧 API Testing
- **Management API Testing**: Tests all 13 management endpoints systematically
- **Metrics API Testing**: Tests metrics endpoints if Metrics Service is available
- **Response Validation**: Validates HTTP status codes and response content
- **Performance Metrics**: Measures and reports response times

### 🆕 Dynamic API Validation
- **Configuration Chain Validation**: Validates endpoint → query → database dependencies
- **Database Schema Validation**: Verifies table existence, column availability, and field mapping
- **SQL Query Analysis**: Parses SQL queries to extract table and column references
- **Parameter Validation**: Ensures SQL placeholders match defined parameters
- **Comprehensive Reporting**: Detailed validation results with success/error counts

### ✅ Standalone Configuration Validation
- **Startup Validation**: Optional validation during normal application startup
- **Validate-Only Mode**: Run validation checks and exit without starting server
- **Command Line Validation**: Override configuration with `--validate-only` flag
- **CI/CD Integration**: Perfect for deployment pipeline validation
- **Error Reporting**: Detailed error messages with ASCII table formatting

### 📊 Results Display
- **Formatted Tables**: ASCII tables showing endpoint status, response times, and success rates
- **Summary Statistics**: Overall success rates and average response times
- **Detailed Logging**: Comprehensive logging with clear status indicators
- **Error Reporting**: Clear error messages and troubleshooting guidance

## How to Run

### Option 1: Direct Maven Command
```bash
mvn exec:java -pl generic-api-service -Dexec.mainClass=dev.mars.bootstrap.SystemBootstrapDemo
```

### Option 2: Using Provided Scripts

#### Windows
```cmd
run-bootstrap-demo.bat
```

#### Unix/Linux/macOS
```bash
./run-bootstrap-demo.sh
```

## Bootstrap Process Flow

The bootstrap demonstration follows this sequence:

### 1. Service Initialization
- Initializes HTTP client for API testing
- Starts Generic API Service on port 8080
- Waits for service to be ready (health check)

### 2. Management API Testing
Tests all 13 management endpoints:
- **Configuration Management**: metadata, paths, contents, endpoints, queries, databases
- **Usage Statistics**: overall, endpoints, queries, databases
- **Health Monitoring**: general health, database health
- **Management Dashboard**: comprehensive dashboard view

### 3. 🆕 Dynamic API Validation

#### Part 1: Configuration Chain Validation
- **Load Configurations**: Loads all YAML configuration files
- **Endpoint → Query Validation**: Verifies each endpoint references an existing query
- **Query → Database Validation**: Verifies each query references an existing database
- **Count Query Validation**: Validates count queries for paginated endpoints

#### Part 2: Database Schema Validation
- **Table Existence**: Verifies tables referenced in queries exist in the database
- **Column Validation**: Checks that all columns referenced in queries exist in tables
- **Parameter Mapping**: Ensures SQL parameter placeholders match defined parameters
- **Field Type Validation**: Validates data types and constraints

### 🆕 Standalone Configuration Validation

In addition to the bootstrap demo validation, the system now supports standalone validation modes:

#### Validation During Startup
```yaml
# application.yml
validation:
  runOnStartup: true   # Run validation during normal startup
  validateOnly: false  # Continue with server startup after validation
```

#### Validation Only Mode
```yaml
# application.yml
validation:
  runOnStartup: false  # Not needed when validateOnly is true
  validateOnly: true   # Run validation and exit (no server startup)
```

#### Command Line Validation
```bash
# Run validation only and exit
java -jar generic-api-service.jar --validate-only

# With Maven
mvn exec:java -Dexec.mainClass="dev.mars.generic.GenericApiApplication" -Dexec.args="--validate-only"
```

**Use Cases:**
- **CI/CD Pipelines**: Validate configurations before deployment
- **Development**: Quick configuration verification during development
- **Production**: Pre-startup validation to catch issues early

### 4. Metrics Service Detection
- Checks if Metrics Service is running on port 8081
- Tests metrics endpoints if available
- Provides instructions for starting Metrics Service if not running

### 5. Results Summary
- Displays comprehensive summary of all tests and validations
- Shows service status and access URLs
- Provides next steps and recommendations

## Validation Results

### Configuration Chain Validation
The bootstrap validates the complete configuration dependency chain:

```
API Endpoint → Query Config → Database Config
     ✅             ✅              ✅
```

**Example Results:**
- ✅ Endpoint 'stock-trades-list' → query 'stock-trades-all' [OK]
- ✅ Query 'stock-trades-all' → database 'stocktrades' [OK]

### Database Schema Validation
Validates the database schema compatibility:

```
Query → Database Table → Table Columns → Field Mapping
  ✅          ✅              ✅             ✅
```

**Example Results:**
- ✅ Query 'stock-trades-all' → table 'stock_trades' [EXISTS]
- ✅ Query 'stock-trades-all' → column 'symbol' [EXISTS]
- ✅ Query 'stock-trades-all' parameter count matches: 2 parameters

## Sample Output

```
================================================================================
>>> STARTING SYSTEM BOOTSTRAP DEMONSTRATION
================================================================================
[START] Starting Generic API Service...
[OK] Generic API Service started on port 8080
[WAIT] Waiting for Generic API Service to be ready...
[OK] Generic API Service is ready

[TEST] Testing Management API endpoints...
+-----------------------------------------------------+--------+----------+---------+----------+
| Endpoint                                            | Status | Time(ms) | Success | Size(B)  |
+-----------------------------------------------------+--------+----------+---------+----------+
| /api/management/config/metadata                     |    200 |       16 |      OK |     1072 |
| /api/management/config/paths                        |    200 |        5 |      OK |      168 |
| /api/management/config/contents                     |    200 |        5 |      OK |      385 |
| ... (10 more endpoints)                             |    ... |      ... |     ... |      ... |
+-----------------------------------------------------+--------+----------+---------+----------+
[SUMMARY] 13/13 endpoints successful (100.0%), Avg response time: 7.2ms

================================================================================
>>> DYNAMIC API VALIDATION
================================================================================
[PART 1] Configuration Validation
[CHECK] Loaded 1 databases, 9 queries, 5 endpoints
[CHECK] Validating endpoint -> query dependencies...
[CHECK] Validating query -> database dependencies...

[RESULTS] Configuration Chain Validation Results:
+------------------------------------------------------------------------------+
| Type: Configuration Chain  | Success:      14 | Errors:      0 |
+------------------------------------------------------------------------------+
| SUCCESSES:
| [OK] Endpoint 'stock-trades-list' -> query 'stock-trades-all' [OK]
| [OK] Query 'stock-trades-all' -> database 'stocktrades' [OK]
| ... (12 more successful validations)
+------------------------------------------------------------------------------+
[SUCCESS] Configuration Chain validation passed

[PART 2] Database Schema Validation
[CHECK] Validating database 'stocktrades' with 9 queries...

[RESULTS] Database Schema Validation Results:
+------------------------------------------------------------------------------+
| Type: Database Schema      | Success:      66 | Errors:      0 |
+------------------------------------------------------------------------------+
| SUCCESSES:
| [OK] Query 'stock-trades-all' -> table 'stock_trades' [EXISTS]
| [OK] Query 'stock-trades-all' -> column 'symbol' [EXISTS]
| ... (64 more successful validations)
+------------------------------------------------------------------------------+
[SUCCESS] Database Schema validation passed

================================================================================
>>> SYSTEM BOOTSTRAP DEMONSTRATION SUMMARY
================================================================================
[STATUS] Services Status:
   + Generic API Service: http://localhost:8080 [OK]
   + Metrics Service:     http://localhost:8081 [NOT RUNNING]

[TESTED] Management APIs Tested:
   + Configuration Management [OK]
   + Usage Statistics [OK]
   + Health Monitoring [OK]
   + Dashboard [OK]

[VALIDATED] Dynamic API Validation:
   + Configuration Chain: 14/14 validations passed [OK]
   + Database Schema: 66/66 validations passed [OK]

[ACCESS] Access URLs:
   + Generic API Swagger: http://localhost:8080/swagger
   + Management Dashboard: http://localhost:8080/api/management/dashboard
   + Metrics Dashboard: http://localhost:8081/dashboard (Start Metrics Service first)
================================================================================
[SUCCESS] Bootstrap demonstration completed successfully!
```

## Access URLs

After running the bootstrap demo, you can access:

- **Generic API Swagger**: http://localhost:8080/swagger
- **Management Dashboard**: http://localhost:8080/api/management/dashboard
- **All Management APIs**: http://localhost:8080/api/management/*
- **Dynamic APIs**: http://localhost:8080/api/generic/*

## Troubleshooting

### Port Already in Use
If you get a "port already in use" error:
1. Stop any running services on port 8080
2. Use `netstat -ano | findstr :8080` (Windows) or `lsof -i :8080` (Unix) to find the process
3. Kill the process and retry

### Configuration Validation Errors
If configuration validation fails:
1. Check that all YAML files exist in the `generic-config/` directory
2. Verify endpoint queries reference existing query names
3. Verify query databases reference existing database names
4. Check the logs for specific validation error messages

### Database Schema Validation Errors
If database schema validation fails:
1. Ensure the database is accessible and tables exist
2. Check that SQL queries reference correct table and column names
3. Verify database connection configuration
4. Review the detailed validation error messages

## Integration with CI/CD

The bootstrap demo can be integrated into CI/CD pipelines:

```bash
# Run bootstrap validation as part of build process
mvn clean compile
mvn exec:java -pl generic-api-service -Dexec.mainClass=dev.mars.bootstrap.SystemBootstrapDemo

# Check exit code for success/failure
if [ $? -eq 0 ]; then
    echo "Bootstrap validation passed"
else
    echo "Bootstrap validation failed"
    exit 1
fi
```

## Next Steps

After running the bootstrap demo:

1. **Start Metrics Service** (if not already running):
   ```bash
   cd metrics-service
   mvn exec:java -Dexec.mainClass="dev.mars.metrics.MetricsApplication"
   ```

2. **Test Dynamic APIs** using the Swagger UI at http://localhost:8080/swagger

3. **Monitor System Health** using the Management Dashboard

4. **Review Validation Results** to ensure all configurations are correct

5. **Run Integration Tests** to verify end-to-end functionality

## Technical Implementation

### SystemBootstrapDemo Class Structure

The bootstrap demo is implemented in `dev.mars.bootstrap.SystemBootstrapDemo` with the following key components:

#### Core Methods
- `main()`: Entry point that orchestrates the entire bootstrap process
- `initializeHttpClient()`: Sets up HTTP client for API testing
- `startGenericApiService()`: Programmatically starts the Generic API Service
- `waitForServiceReady()`: Health check verification with timeout
- `testManagementApis()`: Tests all management endpoints
- `performDynamicApiValidation()`: **NEW** - Comprehensive API validation
- `testMetricsApis()`: Tests metrics endpoints if available
- `displaySummary()`: Shows comprehensive results summary

#### Dynamic API Validation Methods
- `initializeConfigurationComponents()`: Initializes configuration loaders
- `validateConfigurationChain()`: Validates endpoint → query → database chain
- `validateDatabaseSchema()`: Validates database tables, columns, and field mapping
- `validateQuerySchema()`: Validates individual query against database schema
- `extractTableNamesFromSql()`: Parses SQL to extract table references
- `extractColumnNamesFromSql()`: Parses SQL to extract column references
- `validateTableColumns()`: Verifies column existence in database tables
- `validateQueryParameters()`: Ensures parameter count matches SQL placeholders

#### Validation Result Handling
- `ValidationResult` class: Container for validation results with success/error tracking
- `displayValidationResults()`: Formats and displays validation results in ASCII tables
- Comprehensive error reporting with specific failure details

### Configuration Dependencies

The bootstrap demo validates these configuration relationships:

```yaml
# stocktrades-api-endpoints.yml
endpoints:
  stock-trades-list:
    query: stock-trades-all  # Must exist in stocktrades-queries.yml

# stocktrades-queries.yml
queries:
  stock-trades-all:
    database: stocktrades    # Must exist in stocktrades-databases.yml
    sql: "SELECT * FROM stock_trades"  # Table must exist in database

# stocktrades-databases.yml
databases:
  stocktrades:
    url: "jdbc:h2:./data/stocktrades"  # Database must be accessible
```

### Validation Algorithms

#### SQL Parsing Algorithm
The bootstrap uses regex patterns to extract database objects from SQL:

```java
// Extract table names from FROM and JOIN clauses
Pattern tablePattern = Pattern.compile("(?i)(?:FROM|JOIN)\\s+([a-zA-Z_][a-zA-Z0-9_]*)");

// Extract column names from SELECT clause
Pattern selectPattern = Pattern.compile("(?i)SELECT\\s+(.*?)\\s+FROM");

// Extract column names from WHERE clause
Pattern wherePattern = Pattern.compile("(?i)WHERE\\s+(.*?)(?:ORDER|GROUP|LIMIT|$)");
```

#### Database Metadata Validation
Uses JDBC DatabaseMetaData to verify schema:

```java
// Check table existence
ResultSet tables = metaData.getTables(null, null, tableName.toUpperCase(), new String[]{"TABLE"});

// Check column existence
ResultSet columns = metaData.getColumns(null, null, tableName.toUpperCase(), null);
```

## Performance Metrics

### Typical Bootstrap Performance
- **Service Startup**: ~2-3 seconds
- **Management API Testing**: ~100-200ms total (13 endpoints)
- **Configuration Validation**: ~50-100ms
- **Database Schema Validation**: ~200-500ms (depends on query complexity)
- **Total Bootstrap Time**: ~3-5 seconds

### Validation Coverage
- **Configuration Files**: 100% coverage of all YAML files
- **Endpoint Dependencies**: 100% validation of endpoint → query relationships
- **Query Dependencies**: 100% validation of query → database relationships
- **Database Schema**: 100% validation of table and column existence
- **Parameter Mapping**: 100% validation of SQL parameter placeholders

## Error Scenarios and Handling

### Configuration Errors
1. **Missing Query Reference**: Endpoint references non-existent query
2. **Missing Database Reference**: Query references non-existent database
3. **Missing Count Query**: Paginated endpoint missing count query
4. **Invalid YAML Syntax**: Malformed configuration files

### Database Schema Errors
1. **Missing Table**: Query references non-existent table
2. **Missing Column**: Query references non-existent column
3. **Parameter Mismatch**: SQL placeholders don't match defined parameters
4. **Connection Failure**: Database not accessible or connection failed

### Service Errors
1. **Port Conflict**: Service port already in use
2. **Startup Timeout**: Service takes too long to start
3. **Health Check Failure**: Service starts but health check fails
4. **API Response Error**: Management APIs return unexpected responses

## Best Practices

### Running the Bootstrap Demo
1. **Clean Environment**: Ensure no conflicting services are running
2. **Fresh Build**: Run `mvn clean compile` before bootstrap
3. **Log Review**: Check logs for any warnings or errors
4. **Resource Cleanup**: Bootstrap automatically cleans up resources

### Configuration Management
1. **Consistent Naming**: Use consistent naming conventions across configurations
2. **Reference Validation**: Always validate references before deployment
3. **Schema Evolution**: Update validation when database schema changes
4. **Documentation**: Keep configuration documentation up to date

### Integration Testing
1. **Automated Validation**: Include bootstrap in CI/CD pipelines
2. **Environment Consistency**: Use same validation across all environments
3. **Failure Handling**: Implement proper error handling and reporting
4. **Performance Monitoring**: Track bootstrap performance over time

## Future Enhancements

### Planned Improvements
1. **Field Type Validation**: Validate SQL field types against database schema
2. **Query Performance Analysis**: Analyze query performance and suggest optimizations
3. **API Response Schema Validation**: Validate API response formats
4. **Configuration Diff Analysis**: Compare configurations across environments
5. **Automated Fix Suggestions**: Suggest fixes for common configuration errors

### Integration Possibilities
1. **IDE Integration**: Plugin for real-time validation in development
2. **Git Hooks**: Pre-commit validation of configuration changes
3. **Monitoring Integration**: Real-time validation in production environments
4. **Documentation Generation**: Auto-generate API documentation from configurations

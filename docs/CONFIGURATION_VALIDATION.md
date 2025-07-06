# Configuration Validation Guide

This document provides a comprehensive guide to the configuration validation system in the Javalin API Mesh project.

## 🎯 **Overview**

The configuration validation system ensures that all YAML configurations are correct, consistent, and compatible with the target database schemas before the application starts. This prevents runtime errors and provides early feedback on configuration issues.

## ⚙️ **Configuration**

### **Validation Settings**

Configure validation behavior in `application.yml`:

```yaml
validation:
  runOnStartup: false             # Run validation during normal application startup
  validateOnly: false             # Run only validation and exit (no server startup)
  validateEndpoints: true         # Include endpoint connectivity testing in validation
```

### **Default Behavior**
- **runOnStartup**: `false` - No validation during startup (backward compatible)
- **validateOnly**: `false` - Normal application startup behavior
- **validateEndpoints**: `true` - Include HTTP endpoint testing when validation runs

## 🚀 **Usage Modes**

### **1. Startup Validation**

Enable validation during normal application startup:

```yaml
# application.yml
validation:
  runOnStartup: true              # Enable validation on every startup
  validateOnly: false             # Continue with normal startup after validation
  validateEndpoints: true         # Include endpoint connectivity testing
```

**Behavior:**
- Runs comprehensive validation before starting the server
- Tests endpoint connectivity after server startup (if validateEndpoints: true)
- Continues with normal application startup if validation passes
- Exits with error code 1 if validation fails
- Perfect for production environments to catch configuration issues early

### **2. Standalone Validation**

Run validation only and exit (no server startup):

```yaml
# application.yml
validation:
  runOnStartup: false             # Not needed when validateOnly is true
  validateOnly: true              # Run validation and exit
  validateEndpoints: false        # Skip endpoint testing (no server startup)
```

**Behavior:**
- Runs validation checks only (configuration and database schema)
- Skips endpoint connectivity testing (since no server is started)
- Exits immediately after validation (no server startup)
- Returns exit code 0 on success, 1 on failure
- Perfect for CI/CD pipelines and configuration verification

### **3. Command Line Override**

Override configuration with command line arguments:

```bash
# Run validation only and exit
java -jar generic-api-service.jar --validate-only

# Alternative short form
java -jar generic-api-service.jar --validate

# With Maven
mvn exec:java -Dexec.mainClass="dev.mars.generic.GenericApiApplication" -Dexec.args="--validate-only"
```

**Behavior:**
- Command line arguments override YAML configuration
- Useful for ad-hoc validation without changing configuration files
- Takes precedence over `validation.validateOnly` setting

## 🔍 **Validation Process**

The validation system performs three main types of validation:

### **1. Configuration Chain Validation**

Verifies the integrity of configuration relationships:

#### **Endpoint → Query Validation**
- Checks that every endpoint's `query` field references an existing query
- Validates that required queries are defined
- Reports missing or invalid query references

#### **Query → Database Validation**
- Checks that every query's `database` field references an existing database
- Validates that required databases are configured
- Reports missing or invalid database references

#### **Cross-Reference Integrity**
- Ensures all configuration relationships form a valid chain
- Validates pagination count queries if specified
- Checks parameter consistency between endpoints and queries

### **2. Database Schema Validation**

Verifies compatibility with actual database schemas:

#### **Database Connectivity**
- Tests connections to all configured databases
- Validates connection parameters and credentials
- Reports connection failures with detailed error messages

#### **Table Existence**
- Checks that all tables referenced in SQL queries exist
- Validates table names and schema access
- Reports missing tables with specific query references

#### **Column Validation**
- Verifies that all columns referenced in queries exist in target tables
- Checks column names and data types
- Reports missing or incompatible columns

#### **Query Parameter Validation**
- Validates query parameters and their types
- Checks parameter binding compatibility
- Reports parameter mismatches and type errors

### **3. Endpoint Connectivity Validation**

Tests actual HTTP endpoint functionality (when server is running):

#### **HTTP Request Testing**
- Makes actual HTTP requests to all configured endpoints
- Tests with sample parameters and pagination settings
- Measures response times and validates status codes
- Reports connectivity issues and server errors

#### **Smart Parameter Substitution**
- Automatically substitutes path parameters with sample values
- Adds appropriate query parameters for testing
- Handles pagination parameters for paginated endpoints
- Includes date range parameters for analytics endpoints

#### **Response Analysis**
- **200-299**: Success - endpoint working correctly
- **400**: Bad Request - acceptable for parameter-dependent endpoints
- **404**: Not Found - endpoint not registered (error)
- **500+**: Server Error - application or database issue (error)

#### **Performance Monitoring**
- Measures response times for each endpoint
- Identifies slow-responding endpoints
- Provides baseline performance metrics
- Reports timeout issues

## 📊 **Validation Output**

### **Success Output**
```
================================================================================
>>> CONFIGURATION VALIDATION MODE
================================================================================
[PART 1] Configuration Chain Validation
+------------------------------------------------------------------------------+
| Type: Configuration Chain | Success:      15 | Errors:       0 |
+------------------------------------------------------------------------------+
| SUCCESSES:
| [OK] Endpoint 'stock-trades-list' -> query 'stock-trades-all' [OK]
| [OK] Query 'stock-trades-all' -> database 'stocktrades' [OK]
| [OK] Query 'stock-trades-count' -> database 'stocktrades' [OK]
| ... and 12 more successful validations
+------------------------------------------------------------------------------+
[SUCCESS] Configuration Chain validation passed

[PART 2] Database Schema Validation
+------------------------------------------------------------------------------+
| Type: Database Schema     | Success:       8 | Errors:       0 |
+------------------------------------------------------------------------------+
| SUCCESSES:
| [OK] Query 'stock-trades-all' -> table 'stock_trades' [EXISTS]
| [OK] Database 'stocktrades' schema validation completed
| [OK] Query parameters validated for 'stock-trades-all'
| ... and 5 more successful validations
+------------------------------------------------------------------------------+
[SUCCESS] Database Schema validation passed

[PART 3] Endpoint Connectivity Validation
+------------------------------------------------------------------------------+
| Type: Endpoint Connectivity | Success:      12 | Errors:       0 |
+------------------------------------------------------------------------------+
| SUCCESSES:
| [OK] Endpoint 'postgres-trades-list' -> GET /api/postgres/trades [200] (51ms)
| [OK] Endpoint 'postgres-trades-count' -> GET /api/postgres/trades/count [200] (6ms)
| [OK] Endpoint 'postgres-trades-by-symbol' -> GET /api/postgres/trades/symbol/{symbol} [200] (14ms)
| ... and 9 more successful validations
+------------------------------------------------------------------------------+
[SUCCESS] Endpoint Connectivity validation passed
================================================================================
>>> VALIDATION COMPLETED - APPLICATION EXITING
================================================================================
```

### **Error Output**
```
================================================================================
>>> CONFIGURATION VALIDATION MODE
================================================================================
[PART 1] Configuration Chain Validation
+------------------------------------------------------------------------------+
| Type: Configuration Chain | Success:      12 | Errors:       3 |
+------------------------------------------------------------------------------+
| ERRORS:
| [ERROR] Endpoint 'invalid-endpoint' references non-existent query: missing-query
| [ERROR] Query 'broken-query' references non-existent database: missing-db
| [ERROR] Query 'param-mismatch' has no database defined
| SUCCESSES:
| [OK] Endpoint 'stock-trades-list' -> query 'stock-trades-all' [OK]
| ... and 11 more successful validations
+------------------------------------------------------------------------------+
[FAILED] Configuration Chain validation failed with 3 errors

FATAL: Configuration validation failed
Application startup aborted due to validation failure
```

## 🛠️ **Integration Examples**

### **CI/CD Pipeline Integration**

```bash
#!/bin/bash
# deployment-validation.sh

echo "Validating configuration before deployment..."

# Run configuration validation
java -jar generic-api-service.jar --validate-only

if [ $? -eq 0 ]; then
    echo "✅ Configuration validation passed - proceeding with deployment"
else
    echo "❌ Configuration validation failed - aborting deployment"
    exit 1
fi

# Continue with deployment...
echo "Starting application deployment..."
```

### **Docker Integration**

```dockerfile
# Dockerfile
FROM openjdk:21-jre-slim

COPY generic-api-service.jar /app/
COPY config/ /app/config/

WORKDIR /app

# Validate configuration during build
RUN java -jar generic-api-service.jar --validate-only

# Start application normally
CMD ["java", "-jar", "generic-api-service.jar"]
```

### **Development Workflow**

```bash
# Quick configuration check during development
alias validate-config='mvn exec:java -Dexec.mainClass="dev.mars.generic.GenericApiApplication" -Dexec.args="--validate-only"'

# Use in development
validate-config
```

## 🌐 **Validation APIs**

The system provides REST APIs for on-demand validation:

### **Comprehensive Validation**
```bash
# Run all validation checks
curl http://localhost:8080/api/generic/config/validate

# Validate specific components
curl http://localhost:8080/api/generic/config/validate/endpoints
curl http://localhost:8080/api/generic/config/validate/queries
curl http://localhost:8080/api/generic/config/validate/databases
curl http://localhost:8080/api/generic/config/validate/relationships
```

### **Endpoint Connectivity Testing**
```bash
# Test all endpoint connectivity
curl http://localhost:8080/api/generic/config/validate/endpoint-connectivity
```

**Response Format:**
```json
{
  "status": "VALID",
  "baseUrl": "http://localhost:8080",
  "totalEndpoints": 22,
  "successCount": 12,
  "errorCount": 10,
  "successes": [
    "Endpoint 'postgres-trades-list' -> GET /api/postgres/trades [200] (51ms)",
    "Endpoint 'postgres-trades-count' -> GET /api/postgres/trades/count [200] (6ms)"
  ],
  "errors": [
    "Endpoint 'analytics-daily-volume' -> GET /api/analytics/daily-volume [500 - Server Error] (75ms)",
    "Endpoint 'test-unavailable-endpoint' -> GET /api/test/unavailable [404 - Not Found] - Endpoint not registered"
  ],
  "timestamp": 1751796214132
}
```

### **Integration with Swagger UI**
All validation APIs are documented and testable via Swagger UI:
- **Swagger UI**: http://localhost:8080/swagger
- Navigate to "Configuration Validation" section
- Test validation endpoints interactively

## 🚨 **Error Handling**

### **Exit Codes**
- **0**: Validation successful
- **1**: Validation failed or fatal error

### **Error Categories**

#### **Configuration Errors**
- Missing configuration files
- Invalid YAML syntax
- Missing required fields
- Invalid configuration relationships

#### **Schema Errors**
- Database connection failures
- Missing database tables
- Missing table columns
- Parameter type mismatches

#### **Endpoint Connectivity Errors**
- HTTP 404 errors (endpoint not registered)
- HTTP 500+ errors (server or database issues)
- Connection timeouts
- Network connectivity issues

### **Logging**
- **INFO**: Successful validations and progress updates
- **ERROR**: Validation failures and detailed error messages
- **FATAL**: Critical errors that prevent application startup

## 📚 **Related Documentation**

- [Configuration Schema Reference](CONFIGURATION_SCHEMA_REFERENCE.md) - Complete configuration guide
- [Architecture Guide](ARCHITECTURE_GUIDE.md) - System architecture overview
- [Bootstrap Demo](BOOTSTRAP_DEMO.md) - System demonstration including validation
- [Test Documentation](TEST_DOCUMENTATION.md) - Testing validation functionality

## 🔧 **Troubleshooting**

### **Common Issues**

#### **Configuration File Not Found**
```
FATAL CONFIGURATION ERROR: Required configuration file not found
  File: ./config/databases.yml
  Action: Ensure the database configuration file exists and is accessible
```
**Solution**: Check file paths in `application.yml` and ensure configuration files exist.

#### **Database Connection Failed**
```
[ERROR] Failed to connect to database 'stocktrades': Connection refused
```
**Solution**: Verify database connection parameters and ensure database is running.

#### **Missing Table**
```
[ERROR] Query 'stock-trades-all' references non-existent table: stock_trades
```
**Solution**: Create missing tables or update query to reference existing tables.

#### **Endpoint Connectivity Failed**
```
[ERROR] Endpoint 'postgres-trades-list' -> GET /api/postgres/trades [500 - Server Error] (75ms)
```
**Solution**: Check database connectivity and application logs for specific error details.

#### **Endpoint Not Found**
```
[ERROR] Endpoint 'test-endpoint' -> GET /api/test/endpoint [404 - Not Found] - Endpoint not registered
```
**Solution**: Verify endpoint configuration and ensure database is available for endpoint registration.

### **Best Practices**
- Always run validation in CI/CD pipelines before deployment
- Use `runOnStartup: true` in production environments
- Test configuration changes with `--validate-only` during development
- Use endpoint connectivity validation to verify API functionality
- Monitor endpoint response times for performance issues
- Keep configuration files in version control
- Use environment-specific configuration files for different deployments
- Test endpoint validation APIs via Swagger UI during development

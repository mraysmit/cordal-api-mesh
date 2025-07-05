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
```

### **Default Behavior**
- **runOnStartup**: `false` - No validation during startup (backward compatible)
- **validateOnly**: `false` - Normal application startup behavior

## 🚀 **Usage Modes**

### **1. Startup Validation**

Enable validation during normal application startup:

```yaml
# application.yml
validation:
  runOnStartup: true              # Enable validation on every startup
  validateOnly: false             # Continue with normal startup after validation
```

**Behavior:**
- Runs comprehensive validation before starting the server
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
```

**Behavior:**
- Runs validation checks only
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

The validation system performs two main types of validation:

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

### **Best Practices**
- Always run validation in CI/CD pipelines before deployment
- Use `runOnStartup: true` in production environments
- Test configuration changes with `--validate-only` during development
- Keep configuration files in version control
- Use environment-specific configuration files for different deployments

# Deployment Guide - Javalin API Mesh

## Overview

This guide covers deployment options for the Javalin API Mesh project, which consists of two main services:
- **Generic API Service** (Port 8080) - Dynamic API service with YAML configuration
- **Metrics Service** (Port 8081) - Performance metrics collection and monitoring

## Phase 2 Improvements ✅

### Executable JAR Support
- **Fat JARs**: Self-contained executable JARs with all dependencies included
- **No Classpath Issues**: All dependencies bundled, no external JAR management needed
- **Direct Execution**: Run with simple `java -jar` command

### Deployment Scripts
- **Cross-Platform**: Windows (.bat) and Unix/Linux (.sh) scripts
- **Flexible Options**: Start individual services or all services together
- **Configuration Validation**: Built-in validation mode for configuration checking
- **Background Mode**: Support for daemon/background execution

## Quick Start

### 1. Build Executable JARs

**Windows:**
```cmd
scripts\build-executable-jars.bat
```

**Unix/Linux:**
```bash
./scripts/build-executable-jars.sh
```

### 2. Start All Services

**Windows:**
```cmd
scripts\start-all-services.bat
```

**Unix/Linux:**
```bash
./scripts/start-all-services.sh
```

### 3. Access Services

- **Generic API Service**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger
- **Metrics Service**: http://localhost:8081
- **Metrics Dashboard**: http://localhost:8081/dashboard

## Deployment Options

### Option 1: Executable JAR Files (Recommended)

#### Advantages
- ✅ Self-contained with all dependencies
- ✅ No classpath configuration needed
- ✅ Easy to deploy and distribute
- ✅ Consistent across environments

#### JAR Sizes
- **Generic API Service**: ~21.10 MB (includes PostgreSQL + Swagger UI)
- **Metrics Service**: ~15.77 MB (minimal dependencies)

#### Manual Execution
```bash
# Generic API Service
cd generic-api-service
java -jar target/generic-api-service-1.0-SNAPSHOT-executable.jar

# Metrics Service  
cd metrics-service
java -jar target/metrics-service-1.0-SNAPSHOT-executable.jar
```

### Option 2: Thin JAR Files + Classpath

#### Advantages
- ✅ Smaller JAR files (~50-250 KB)
- ✅ Shared dependencies across services
- ✅ Faster builds when dependencies don't change

#### Disadvantages
- ❌ Complex classpath management
- ❌ Dependency version conflicts possible
- ❌ More complex deployment

#### Execution
```bash
# Requires Maven to manage classpath
mvn exec:java -Dexec.mainClass="dev.mars.generic.GenericApiApplication"
```

## Script Reference

### Build Scripts

#### `build-executable-jars.bat/.sh`
```bash
# Build with default settings (clean, skip tests)
./scripts/build-executable-jars.sh

# Build and run tests
./scripts/build-executable-jars.sh --run-tests

# Build without cleaning
./scripts/build-executable-jars.sh --no-clean

# Build with specific Maven profile
./scripts/build-executable-jars.sh --profile test-profiling
```

### Service Startup Scripts

#### `start-generic-api-service.bat/.sh`
```bash
# Start normally
./scripts/start-generic-api-service.sh

# Validate configuration only
./scripts/start-generic-api-service.sh --validate-only

# Show help
./scripts/start-generic-api-service.sh --help
```

#### `start-metrics-service.bat/.sh`
```bash
# Start normally
./scripts/start-metrics-service.sh

# Show help
./scripts/start-metrics-service.sh --help
```

#### `start-all-services.bat/.sh`
```bash
# Start both services
./scripts/start-all-services.sh

# Start only Generic API Service
./scripts/start-all-services.sh --generic-api-only

# Start only Metrics Service
./scripts/start-all-services.sh --metrics-only

# Validate configurations only
./scripts/start-all-services.sh --validate-only

# Start in background (Unix/Linux only)
./scripts/start-all-services.sh --background
```

## Environment Configuration

### JVM Options
Set via environment variable:
```bash
# Windows
set JVM_OPTS=-Xms1g -Xmx4g -XX:+UseG1GC

# Unix/Linux
export JVM_OPTS="-Xms1g -Xmx4g -XX:+UseG1GC"
```

### Application Options
Set via environment variable:
```bash
# Windows
set APP_OPTS=--validate-only

# Unix/Linux
export APP_OPTS="--validate-only"
```

## Production Deployment

### System Requirements
- **Java**: OpenJDK 21 or later
- **Memory**: Minimum 1GB RAM (2GB+ recommended)
- **Storage**: 100MB for JARs + data/logs
- **Network**: Ports 8080, 8081 available

### Recommended JVM Settings
```bash
# Generic API Service (higher memory for configuration processing)
JVM_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication"

# Metrics Service (lower memory requirements)
JVM_OPTS="-Xms256m -Xmx1g -XX:+UseG1GC -XX:+UseStringDeduplication"
```

### Service Management

#### Systemd (Linux)
Create service files in `/etc/systemd/system/`:

```ini
# generic-api-service.service
[Unit]
Description=Generic API Service
After=network.target

[Service]
Type=simple
User=apiservice
WorkingDirectory=/opt/javalin-api-mesh/generic-api-service
ExecStart=/opt/javalin-api-mesh/scripts/start-generic-api-service.sh
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

#### Windows Service
Use tools like NSSM (Non-Sucking Service Manager) or create Windows services.

### Monitoring and Logging

#### Log Files
- **Generic API Service**: `logs/generic-api-service.log`
- **Metrics Service**: `logs/metrics-service.log`

#### Health Checks
- **Generic API**: `GET http://localhost:8080/api/health`
- **Metrics Service**: `GET http://localhost:8081/health`

## Troubleshooting

### Common Issues

#### "JAR not found" Error
```bash
# Solution: Build the project first
./scripts/build-executable-jars.sh
```

#### Port Already in Use
```bash
# Check what's using the port
netstat -an | grep :8080
lsof -i :8080

# Kill the process or change port in application.yml
```

#### Java Not Found
```bash
# Install Java 21+
# Add to PATH
export JAVA_HOME=/path/to/java21
export PATH=$JAVA_HOME/bin:$PATH
```

#### Configuration Validation Failures
```bash
# Run validation only to see detailed errors
./scripts/start-generic-api-service.sh --validate-only
```

### Performance Tuning

#### Memory Settings
- Monitor with JVM flags: `-XX:+PrintGCDetails -XX:+PrintGCTimeStamps`
- Adjust heap size based on usage patterns
- Use G1GC for better latency

#### Database Connections
- Tune HikariCP settings in application.yml
- Monitor connection pool usage
- Consider connection limits

## Migration from Phase 1

### What Changed
- ✅ Added executable JAR support
- ✅ Created deployment scripts
- ✅ Enhanced startup options
- ✅ Improved error handling

### Backward Compatibility
- ✅ All existing functionality preserved
- ✅ Thin JARs still available
- ✅ Maven exec:java still works
- ✅ Configuration files unchanged

The Phase 2 improvements provide a much better deployment experience while maintaining full backward compatibility with existing deployment methods.

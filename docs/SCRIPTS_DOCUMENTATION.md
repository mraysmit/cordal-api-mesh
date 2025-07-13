# Scripts Documentation - Javalin API Mesh

## Overview

This document provides comprehensive documentation for all scripts in the `scripts/` directory. The scripts are designed to support development, building, deployment, and maintenance of the Javalin API Mesh project.

## Script Categories

### 🏗️ **Build Scripts**
- [`build-executable-jars.sh`](#build-executable-jarssh) - Enhanced build script with multiple JAR profiles
- [`build-executable-jars.bat`](#build-executable-jarsbat) - Windows version of the build script

### 🚀 **Service Startup Scripts**
- [`start-all-services.sh`](#start-all-servicessh) - Start all services (Unix/Linux)
- [`start-all-services.bat`](#start-all-servicesbat) - Start all services (Windows)
- [`start-generic-api-service.sh`](#start-generic-api-servicesh) - Start Generic API Service (Unix/Linux)
- [`start-generic-api-service.bat`](#start-generic-api-servicebat) - Start Generic API Service (Windows)
- [`start-metrics-service.sh`](#start-metrics-servicesh) - Start Metrics Service (Unix/Linux)
- [`start-metrics-service.bat`](#start-metrics-servicebat) - Start Metrics Service (Windows)

### 🔍 **Analysis & Debugging Scripts**
- [`analyze-jars.sh`](#analyze-jarssh) - JAR analysis tool (Unix/Linux)
- [`analyze-jars.bat`](#analyze-jarsbat) - JAR analysis tool (Windows)

### 🗄️ **Database Scripts**
- [`start-h2-server.sh`](#start-h2-serversh) - Start H2 database server (Unix/Linux)
- [`start-h2-server.bat`](#start-h2-serverbat) - Start H2 database server (Windows)
- [`h2-console.sh`](#h2-consolesh) - Start H2 web console (Unix/Linux)
- [`h2-console.bat`](#h2-consolebat) - Start H2 web console (Windows)

### 🧪 **Testing & Validation Scripts**
- [`run-bootstrap-demo.sh`](#run-bootstrap-demosh) - Bootstrap demonstration (Unix/Linux)
- [`run-bootstrap-demo.bat`](#run-bootstrap-demobat) - Bootstrap demonstration (Windows)
- [`test-validation-flags.sh`](#test-validation-flagssh) - Test validation features (Unix/Linux)
- [`test-validation-flags.bat`](#test-validation-flagsbat) - Test validation features (Windows)

---

## Build Scripts

### `build-executable-jars.sh`

**Purpose**: Enhanced build script that creates executable JARs with multiple distribution profiles.

**Location**: `scripts/build-executable-jars.sh`

**Usage**:
```bash
./scripts/build-executable-jars.sh [OPTIONS]
```

**Options**:
- `--run-tests` - Run tests during build (default: skip tests)
- `--no-clean` - Skip clean phase (default: run clean)
- `--profile PROFILE` - Activate specific Maven profile
- `--fat-jar` - Build fat JARs with all dependencies (default)
- `--thin-jar` - Build thin JARs with minimal dependencies
- `--optimized-jar` - Build optimized JARs (fat + minimized)
- `--dev` - Build development JARs (fast build, skip tests)
- `--analyze` - Run JAR analysis after build
- `--help, -h` - Show help message

**Distribution Profiles**:
- **fat-jar (default)**: Self-contained JARs with all dependencies (~20MB)
- **thin-jar**: Minimal JARs requiring external classpath (~1MB)
- **optimized-jar**: Fat JARs with unused classes removed (~15MB)
- **dev**: Fast development builds, skip tests

**Examples**:
```bash
# Default fat JAR build
./scripts/build-executable-jars.sh

# Build optimized JARs with analysis
./scripts/build-executable-jars.sh --optimized-jar --analyze

# Fast development build
./scripts/build-executable-jars.sh --dev

# Build with tests
./scripts/build-executable-jars.sh --run-tests
```

**Output**: Creates executable JARs in `{module}/target/` directories with naming pattern:
- `{module}-{version}-{profile}.jar`

---

## Service Startup Scripts

### `start-all-services.sh`

**Purpose**: Master startup script that launches both Generic API Service and Metrics Service.

**Location**: `scripts/start-all-services.sh`

**Usage**:
```bash
./scripts/start-all-services.sh [OPTIONS]
```

**Options**:
- `--generic-api-only` - Start only Generic API Service
- `--metrics-only` - Start only Metrics Service
- `--validate-only, --validate` - Run configuration validation only and exit
- `--help, -h` - Show help message

**Features**:
- Automatic JAR detection (supports multiple profiles)
- Health check verification
- Graceful shutdown handling
- Configuration validation mode
- Cross-platform compatibility

**Examples**:
```bash
# Start both services
./scripts/start-all-services.sh

# Start only API service
./scripts/start-all-services.sh --generic-api-only

# Validate configurations only
./scripts/start-all-services.sh --validate-only
```

### `start-generic-api-service.sh`

**Purpose**: Starts the Generic API Service with comprehensive configuration options.

**Location**: `scripts/start-generic-api-service.sh`

**Usage**:
```bash
./scripts/start-generic-api-service.sh [OPTIONS]
```

**Options**:
- `--validate-only, --validate` - Run configuration validation only
- `--help, -h` - Show help message

**Features**:
- Multi-profile JAR support (executable, optimized, dev, thin)
- JVM optimization settings
- Signal handling for graceful shutdown
- Configuration validation mode
- Working directory management

**JVM Settings**:
- Memory: `-Xmx1g -Xms512m`
- GC: `-XX:+UseG1GC`
- JIT: `-XX:+TieredCompilation`

**Examples**:
```bash
# Start service normally
./scripts/start-generic-api-service.sh

# Validate configuration only
./scripts/start-generic-api-service.sh --validate-only
```

### `start-metrics-service.sh`

**Purpose**: Starts the Metrics Service with optimized settings for metrics collection.

**Location**: `scripts/start-metrics-service.sh`

**Usage**:
```bash
./scripts/start-metrics-service.sh [OPTIONS]
```

**Options**:
- `--help, -h` - Show help message

**Features**:
- Multi-profile JAR support
- Optimized JVM settings for metrics workload
- Signal handling for graceful shutdown
- Automatic JAR detection

**JVM Settings**:
- Memory: `-Xmx512m -Xms256m`
- GC: `-XX:+UseG1GC`
- Performance: `-XX:+OptimizeStringConcat`

---

## Analysis & Debugging Scripts

### `analyze-jars.sh`

**Purpose**: Comprehensive JAR analysis tool for understanding dependencies, sizes, and contents.

**Location**: `scripts/analyze-jars.sh`

**Usage**:
```bash
./scripts/analyze-jars.sh [OPTIONS]
```

**Options**:
- `-h, --help` - Show help message
- `-s, --sizes` - Show JAR sizes only
- `-d, --dependencies` - Show dependency analysis
- `-c, --contents` - Show JAR contents
- `-a, --all` - Show all analysis (default)
- `-m, --module MODULE` - Analyze specific module
- `-p, --profile PROFILE` - Analyze specific profile JARs

**Analysis Types**:
1. **Size Analysis**: File sizes in human-readable format
2. **Dependency Analysis**: Top packages and class counts
3. **Content Analysis**: Entry counts, file types, package breakdown

**Examples**:
```bash
# Analyze all JARs
./scripts/analyze-jars.sh

# Show sizes only
./scripts/analyze-jars.sh --sizes

# Analyze specific module and profile
./scripts/analyze-jars.sh --module generic-api-service --profile optimized

# Show dependency breakdown
./scripts/analyze-jars.sh --dependencies
```

**Sample Output**:
```
JAR Sizes:
  generic-api-service-1.0-SNAPSHOT-executable.jar: 21 MB
  metrics-service-1.0-SNAPSHOT-executable.jar: 15 MB
  common-library-1.0-SNAPSHOT.jar: 49 KB
```

---

## Database Scripts

### `start-h2-server.sh`

**Purpose**: Starts H2 database server in TCP mode for multi-connection support.

**Location**: `scripts/start-h2-server.sh`

**Usage**:
```bash
./scripts/start-h2-server.sh
```

**Configuration**:
- **TCP Port**: 9092
- **Base Directory**: `./data`
- **External Connections**: Allowed
- **Auto-create**: Enabled

**Features**:
- Automatic H2 JAR detection from Maven repository
- Data directory creation
- TCP server mode for concurrent connections

**Connection String**:
```
jdbc:h2:tcp://localhost:9092/./data/{database-name}
```

### `h2-console.sh`

**Purpose**: Starts H2 web console for database management and querying.

**Location**: `scripts/h2-console.sh`

**Usage**:
```bash
./scripts/h2-console.sh
```

**Configuration**:
- **Web Port**: 8082
- **URL**: http://localhost:8082
- **Username**: sa
- **Password**: (empty)

**Connection Examples**:
- API Service Config DB: `jdbc:h2:tcp://localhost:9092/./data/api-service-config`
- Metrics DB: `jdbc:h2:tcp://localhost:9092/./data/metrics`

---

## Testing & Validation Scripts

### `run-bootstrap-demo.sh`

**Purpose**: Demonstrates the system bootstrap process with comprehensive testing.

**Location**: `scripts/run-bootstrap-demo.sh`

**Usage**:
```bash
./scripts/run-bootstrap-demo.sh
```

**Process**:
1. Build the project
2. Run bootstrap demonstration
3. Show system startup and API testing
4. Display results and cleanup

**Features**:
- Automated build verification
- Bootstrap process demonstration
- API endpoint testing
- Error handling and reporting

### `test-validation-flags.sh`

**Purpose**: Tests and demonstrates configuration validation features.

**Location**: `scripts/test-validation-flags.sh`

**Usage**:
```bash
./scripts/test-validation-flags.sh
```

**Validation Features Tested**:
1. **YAML Configuration Flags**:
   - `validation.runOnStartup`: Run validation during normal app startup
   - `validation.validateOnly`: Run only validation and exit

2. **Command Line Arguments**:
   - `--validate-only`: Override config and run validation only
   - `--validate`: Same as --validate-only (shorter form)

3. **Validation Process**:
   - Configuration Chain Validation (endpoints → queries → databases)
   - Database Schema Validation (tables, fields, query compatibility)
   - Comprehensive error reporting with ASCII tables

---

## Windows Script Equivalents

All Unix/Linux scripts have Windows batch file equivalents with `.bat` extensions:

- `build-executable-jars.bat`
- `start-all-services.bat`
- `start-generic-api-service.bat`
- `start-metrics-service.bat`
- `analyze-jars.bat`
- `start-h2-server.bat`
- `h2-console.bat`
- `run-bootstrap-demo.bat`
- `test-validation-flags.bat`

**Windows-Specific Features**:
- Batch file syntax and error handling
- Windows path handling
- `pause` commands for user interaction
- Windows service integration support

---

## Common Script Features

### Error Handling
- Comprehensive error checking and reporting
- Graceful failure with helpful error messages
- Exit codes for automation integration

### Cross-Platform Support
- Separate Unix/Linux and Windows versions
- Platform-specific optimizations
- Consistent functionality across platforms

### Configuration Management
- Automatic configuration detection
- Validation and verification
- Environment-specific settings

### Logging and Output
- Structured output with clear formatting
- Progress indicators and status messages
- ASCII art headers for visual clarity

### Signal Handling
- Graceful shutdown on SIGTERM/SIGINT
- Cleanup procedures
- Process management

---

## Usage Patterns

### Development Workflow
```bash
# 1. Build development JARs
./scripts/build-executable-jars.sh --dev

# 2. Start services for development
./scripts/start-all-services.sh

# 3. Analyze JARs during development
./scripts/analyze-jars.sh --sizes
```

### Production Deployment
```bash
# 1. Build optimized JARs
./scripts/build-executable-jars.sh --optimized-jar --analyze

# 2. Validate configuration
./scripts/start-all-services.sh --validate-only

# 3. Start services
./scripts/start-all-services.sh
```

### Debugging and Analysis
```bash
# 1. Analyze JAR contents
./scripts/analyze-jars.sh --all

# 2. Test validation features
./scripts/test-validation-flags.sh

# 3. Run bootstrap demo
./scripts/run-bootstrap-demo.sh
```

---

## Best Practices

1. **Always validate** configurations before production deployment
2. **Use appropriate JAR types** for different environments
3. **Monitor resource usage** with the metrics service
4. **Test scripts** in development environment first
5. **Check exit codes** in automation scripts
6. **Use signal handling** for graceful shutdowns
7. **Analyze JARs** to understand dependencies and sizes

---

## Troubleshooting

### Common Issues

#### Script Permission Errors (Unix/Linux)
```bash
chmod +x scripts/*.sh
```

#### Java Not Found
- Ensure Java 21+ is installed
- Check JAVA_HOME environment variable
- Verify java is in PATH

#### JAR Not Found
- Run build script first: `./scripts/build-executable-jars.sh`
- Check target directories for JAR files
- Verify Maven build completed successfully

#### Port Already in Use
- Check for running services: `netstat -an | grep :8080`
- Kill existing processes or use different ports
- Wait for services to fully shutdown

#### H2 Database Issues
- Start H2 server: `./scripts/start-h2-server.sh`
- Check data directory permissions
- Verify H2 JAR in Maven repository

---

## Integration with CI/CD

### Jenkins Pipeline Example
```groovy
pipeline {
    stages {
        stage('Build') {
            steps {
                sh './scripts/build-executable-jars.sh --optimized-jar'
            }
        }
        stage('Validate') {
            steps {
                sh './scripts/start-all-services.sh --validate-only'
            }
        }
        stage('Analyze') {
            steps {
                sh './scripts/analyze-jars.sh --all'
            }
        }
    }
}
```

### Docker Integration
```dockerfile
COPY scripts/ /app/scripts/
RUN chmod +x /app/scripts/*.sh
CMD ["/app/scripts/start-all-services.sh"]
```

---

## Script Dependencies

### System Requirements
- **Java**: JDK 21 or later
- **Maven**: 3.6.0 or later
- **curl**: For health checks and API testing
- **bash**: For Unix/Linux scripts (version 4.0+)
- **PowerShell**: For Windows scripts (version 5.0+)

### Maven Dependencies
Scripts automatically detect and use Maven dependencies:
- H2 Database JAR from `~/.m2/repository/com/h2database/h2/`
- Project JARs from `{module}/target/` directories

### Environment Variables
Optional environment variables for customization:
- `JAVA_HOME`: Java installation directory
- `MAVEN_HOME`: Maven installation directory
- `JAVALIN_CONFIG_DIR`: Custom configuration directory
- `JAVALIN_DATA_DIR`: Custom data directory
- `JAVALIN_LOG_LEVEL`: Logging level (DEBUG, INFO, WARN, ERROR)

---

## Script Configuration Files

### Build Configuration
Scripts read configuration from:
- `pom.xml`: Maven profiles and properties
- `application.yaml`: Application configuration
- Environment variables

### Service Configuration
Service startup scripts use:
- JVM optimization settings
- Application-specific parameters
- Health check configurations
- Signal handling setup

---

## Advanced Usage

### Custom JVM Options
```bash
# Set custom JVM options
export JAVA_OPTS="-Xmx2g -XX:+UseZGC -XX:+UnlockExperimentalVMOptions"
./scripts/start-generic-api-service.sh
```

### Custom Application Options
```bash
# Pass custom application arguments
export APP_OPTS="--config-dir=/custom/config --log-level=DEBUG"
./scripts/start-generic-api-service.sh
```

### Profile-Specific Builds
```bash
# Build with specific Maven profile
./scripts/build-executable-jars.sh --profile production

# Build multiple profiles
./scripts/build-executable-jars.sh --fat-jar
./scripts/build-executable-jars.sh --optimized-jar
./scripts/analyze-jars.sh --all
```

### Automated Health Checks
```bash
# Health check with timeout
timeout 30 bash -c 'while ! curl -s http://localhost:8080/api/health; do sleep 1; done'

# Readiness check for Kubernetes
curl -f http://localhost:8080/api/management/readiness || exit 1

# Liveness check for monitoring
curl -f http://localhost:8080/api/management/liveness || exit 1
```

---

## Script Maintenance

### Version Compatibility
- Scripts are tested with Java 21+
- Maven 3.6.0+ required for proper dependency resolution
- H2 database version 2.3.232 (configurable in pom.xml)

### Updates and Modifications
When modifying scripts:
1. Update both Unix/Linux and Windows versions
2. Test on target platforms
3. Update documentation
4. Verify error handling
5. Check signal handling (Unix/Linux)

### Testing Scripts
```bash
# Test all scripts
for script in scripts/*.sh; do
    echo "Testing $script"
    bash -n "$script" && echo "✓ Syntax OK" || echo "✗ Syntax Error"
done

# Test Windows scripts (on Windows)
for /f %i in ('dir /b scripts\*.bat') do (
    echo Testing %i
    call scripts\%i --help >nul 2>&1 && echo ✓ OK || echo ✗ Error
)
```

---

## Performance Considerations

### Build Performance
- **Development builds**: Use `--dev` profile for fastest builds
- **Parallel builds**: Maven uses parallel compilation by default
- **Incremental builds**: Use `--no-clean` to skip clean phase
- **Test skipping**: Default behavior skips tests for faster builds

### Runtime Performance
- **JVM tuning**: Scripts include optimized JVM settings
- **Memory allocation**: Different settings for API vs Metrics services
- **Garbage collection**: G1GC enabled for better performance
- **JIT compilation**: Tiered compilation enabled

### Resource Usage
- **Generic API Service**: ~1GB max heap, ~512MB initial
- **Metrics Service**: ~512MB max heap, ~256MB initial
- **H2 Database**: Minimal memory footprint
- **JAR sizes**: 15-21MB for executable JARs

---

## Security Considerations

### Script Security
- Scripts validate input parameters
- No hardcoded credentials
- Proper file permission handling
- Signal handling for graceful shutdown

### Database Security
- H2 server allows local connections by default
- No default passwords (empty password)
- TCP connections configurable
- Data directory permissions

### Network Security
- Services bind to localhost by default
- Configurable host binding
- Health check endpoints available
- No sensitive data in logs

---

## Monitoring and Logging

### Script Logging
- Structured output with timestamps
- Error messages with context
- Progress indicators
- Exit codes for automation

### Application Logging
- Logback configuration
- Configurable log levels
- File and console output
- Log rotation support

### Health Monitoring
Scripts integrate with health endpoints:
- `/api/health` - Basic health check
- `/api/management/health` - Comprehensive health
- `/api/management/readiness` - Kubernetes readiness
- `/api/management/liveness` - Kubernetes liveness

---

## Future Enhancements

### Planned Features
- **Kubernetes deployment scripts**
- **Docker Compose integration**
- **Automated testing scripts**
- **Performance benchmarking tools**
- **Log aggregation scripts**
- **Backup and restore utilities**

### Community Contributions
- Script improvements welcome
- Cross-platform testing needed
- Documentation updates
- Performance optimizations
- New deployment targets

---

## Support and Troubleshooting

### Getting Help
1. Check script help: `./script-name.sh --help`
2. Review logs in console output
3. Check application logs in `logs/` directory
4. Verify Java and Maven versions
5. Test with minimal configuration

### Common Solutions
- **Permission denied**: `chmod +x scripts/*.sh`
- **Java not found**: Set JAVA_HOME and PATH
- **Maven not found**: Install Maven 3.6.0+
- **Port conflicts**: Check running processes
- **Build failures**: Clean and rebuild

### Reporting Issues
When reporting script issues, include:
- Operating system and version
- Java version (`java -version`)
- Maven version (`mvn -version`)
- Script command used
- Complete error output
- Environment variables

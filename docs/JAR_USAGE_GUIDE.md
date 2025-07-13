# JAR Usage Guide - Javalin API Mesh

## Overview

This guide explains the different types of JARs available in the Javalin API Mesh project and when to use each one. The project supports multiple distribution profiles to meet different deployment needs.

## JAR Types and Profiles

### 1. Fat JAR (Default) - `fat-jar` profile
**File Pattern**: `*-executable.jar`

#### Characteristics
- **Size**: ~20-25 MB per service
- **Dependencies**: All dependencies included
- **Startup Time**: Fast (no classpath resolution)
- **Deployment**: Single file deployment

#### When to Use
- ✅ **Production deployments** - Most reliable
- ✅ **Docker containers** - Single file simplicity
- ✅ **Cloud deployments** - No external dependencies
- ✅ **Quick testing** - Just run with `java -jar`

#### Build Command
```bash
./scripts/build-executable-jars.sh --fat-jar
# or (default)
./scripts/build-executable-jars.sh
```

#### Execution
```bash
java -jar generic-api-service/target/generic-api-service-1.0-SNAPSHOT-executable.jar
java -jar metrics-service/target/metrics-service-1.0-SNAPSHOT-executable.jar
```

### 2. Thin JAR - `thin-jar` profile
**File Pattern**: `*-thin.jar`

#### Characteristics
- **Size**: ~1-2 MB per service
- **Dependencies**: External classpath required
- **Startup Time**: Slower (classpath resolution)
- **Deployment**: Requires dependency management

#### When to Use
- ✅ **Shared environments** - Multiple services share dependencies
- ✅ **Development** - Faster builds when dependencies don't change
- ✅ **Microservice clusters** - Reduced storage when many services deployed
- ❌ **Not recommended for production** - Complex dependency management

#### Build Command
```bash
./scripts/build-executable-jars.sh --thin-jar
```

#### Execution
```bash
# Requires Maven to manage classpath
cd generic-api-service
mvn exec:java -Dexec.mainClass="dev.mars.generic.GenericApiApplication"
```

### 3. Optimized JAR - `optimized-jar` profile
**File Pattern**: `*-optimized.jar`

#### Characteristics
- **Size**: ~15-18 MB per service
- **Dependencies**: All dependencies included, unused classes removed
- **Startup Time**: Fast
- **Deployment**: Single file, smaller than fat JAR

#### When to Use
- ✅ **Resource-constrained environments** - Limited storage/memory
- ✅ **Edge deployments** - Bandwidth-sensitive deployments
- ✅ **Container optimization** - Smaller image sizes
- ⚠️ **Careful testing required** - Minimization might remove needed classes

#### Build Command
```bash
./scripts/build-executable-jars.sh --optimized-jar
```

#### Execution
```bash
java -jar generic-api-service/target/generic-api-service-1.0-SNAPSHOT-optimized.jar
java -jar metrics-service/target/metrics-service-1.0-SNAPSHOT-optimized.jar
```

### 4. Development JAR - `dev` profile
**File Pattern**: `*-dev.jar`

#### Characteristics
- **Size**: ~20-25 MB per service
- **Dependencies**: All dependencies included
- **Build Time**: Fastest (skips tests by default)
- **Optimizations**: None (fastest build)

#### When to Use
- ✅ **Local development** - Quick iteration cycles
- ✅ **Testing builds** - Rapid prototyping
- ✅ **CI/CD pipelines** - Fast feedback loops
- ❌ **Not for production** - No optimizations applied

#### Build Command
```bash
./scripts/build-executable-jars.sh --dev
```

#### Execution
```bash
java -jar generic-api-service/target/generic-api-service-1.0-SNAPSHOT-dev.jar
java -jar metrics-service/target/metrics-service-1.0-SNAPSHOT-dev.jar
```

## JAR Analysis Tools

### Analyze JAR Contents
```bash
# Analyze all JARs
./scripts/analyze-jars.sh

# Analyze specific profile
./scripts/analyze-jars.sh --profile executable

# Analyze specific module
./scripts/analyze-jars.sh --module generic-api-service

# Show sizes only
./scripts/analyze-jars.sh --sizes

# Show dependency breakdown
./scripts/analyze-jars.sh --dependencies
```

### Manual JAR Inspection
```bash
# List JAR contents
jar -tf generic-api-service-1.0-SNAPSHOT-executable.jar

# Extract JAR for inspection
mkdir jar-contents
cd jar-contents
jar -xf ../generic-api-service-1.0-SNAPSHOT-executable.jar

# Check manifest
jar -xf ../generic-api-service-1.0-SNAPSHOT-executable.jar META-INF/MANIFEST.MF
cat META-INF/MANIFEST.MF
```

## Deployment Scenarios

### Scenario 1: Single Server Deployment
**Recommendation**: Fat JAR
```bash
# Build
./scripts/build-executable-jars.sh --fat-jar

# Deploy
scp generic-api-service/target/*-executable.jar server:/opt/app/
scp metrics-service/target/*-executable.jar server:/opt/app/

# Run
ssh server "cd /opt/app && java -jar generic-api-service-1.0-SNAPSHOT-executable.jar &"
ssh server "cd /opt/app && java -jar metrics-service-1.0-SNAPSHOT-executable.jar &"
```

### Scenario 2: Docker Deployment
**Recommendation**: Optimized JAR
```dockerfile
FROM openjdk:21-jre-slim
COPY generic-api-service-1.0-SNAPSHOT-optimized.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Scenario 3: Kubernetes Deployment
**Recommendation**: Fat JAR or Optimized JAR
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: generic-api-service
spec:
  containers:
  - name: api
    image: myregistry/generic-api-service:latest
    command: ["java", "-jar", "/app/generic-api-service-1.0-SNAPSHOT-executable.jar"]
```

### Scenario 4: Development Environment
**Recommendation**: Development JAR
```bash
# Quick development cycle
./scripts/build-executable-jars.sh --dev
./scripts/start-all-services.sh
```

## Performance Considerations

### Startup Time Comparison
- **Fat JAR**: ~3-5 seconds
- **Optimized JAR**: ~3-5 seconds
- **Thin JAR**: ~5-8 seconds (classpath resolution)
- **Development JAR**: ~3-5 seconds

### Memory Usage
- **Fat JAR**: Standard memory usage
- **Optimized JAR**: 10-15% less memory (unused classes removed)
- **Thin JAR**: Similar to fat JAR
- **Development JAR**: Standard memory usage

### Build Time Comparison
- **Fat JAR**: ~30-45 seconds
- **Optimized JAR**: ~45-60 seconds (minimization overhead)
- **Thin JAR**: ~20-30 seconds
- **Development JAR**: ~15-25 seconds (tests skipped)

## Troubleshooting

### Common Issues

#### "ClassNotFoundException" with Optimized JARs
**Cause**: Minimization removed required classes
**Solution**: Use fat JAR or add exclusions to minimization

#### "NoClassDefFoundError" with Thin JARs
**Cause**: Missing dependencies in classpath
**Solution**: Use Maven exec plugin or switch to fat JAR

#### Large JAR Sizes
**Cause**: Unnecessary dependencies included
**Solution**: Use optimized JAR or review dependencies

### Health Check Integration
All JAR types include health check endpoints:
```bash
# Check service health
curl http://localhost:8080/api/management/health
curl http://localhost:8081/api/health

# Check specific database health
curl http://localhost:8080/api/management/health/databases/stocktrades
```

## Best Practices

1. **Use Fat JARs for production** - Most reliable and predictable
2. **Use Optimized JARs for containers** - Smaller image sizes
3. **Use Development JARs for local development** - Fastest build times
4. **Always test optimized JARs thoroughly** - Minimization can remove needed classes
5. **Analyze JARs before deployment** - Understand what's included
6. **Monitor startup times and memory usage** - Choose profile based on requirements

## Next Steps

- See [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) for detailed deployment instructions
- See [HEALTH_MONITORING.md](HEALTH_MONITORING.md) for health check configuration
- Run `./scripts/analyze-jars.sh --help` for analysis tool options

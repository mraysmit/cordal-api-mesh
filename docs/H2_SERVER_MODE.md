# H2 Database Server Mode Configuration

This document explains how to run H2 databases in server mode to allow multiple connections in the javalin-api-mesh application.

## Overview

The application uses H2 databases for both configuration storage and metrics collection. By default, H2 runs in embedded mode, which limits connections to a single application instance. Server mode allows multiple applications or tools to connect to the same database simultaneously.

## Current Configuration

The application is configured to use H2 in TCP server mode:

### Generic API Service Database
```yaml
# generic-api-service/src/main/resources/application.yml
database:
  url: jdbc:h2:tcp://localhost:9092/./data/api-service-config;DB_CLOSE_DELAY=-1
```

### Metrics Service Database
```yaml
# metrics-service/src/main/resources/application.yml
metricsDatabase:
  url: jdbc:h2:tcp://localhost:9092/./data/metrics;DB_CLOSE_DELAY=-1
```

## Running H2 Server

### Option 1: Manual Server Startup (Recommended)

Use the provided scripts to start the H2 server:

#### Windows
```bash
# Start H2 TCP server
scripts/start-h2-server.bat

# Start H2 web console (optional)
scripts/h2-console.bat
```

#### Linux/Mac
```bash
# Make scripts executable
chmod +x scripts/start-h2-server.sh

# Start H2 TCP server
./scripts/start-h2-server.sh

# Start H2 web console (optional - create similar script)
java -cp ~/.m2/repository/com/h2database/h2/2.2.224/h2-2.2.224.jar org.h2.tools.Server -web -webAllowOthers -webPort 8082
```

### Option 2: Using Maven to Start H2 Server

```bash
# Start H2 server using Maven dependency
mvn exec:java -Dexec.mainClass="org.h2.tools.Server" -Dexec.args="-tcp -tcpAllowOthers -tcpPort 9092 -baseDir ./data -ifNotExists"
```

### Option 3: Programmatic Server Management

The application includes H2 server management APIs:

#### Check Server Status
```bash
GET http://localhost:8080/api/h2-server/status
```

#### Start H2 Servers
```bash
POST http://localhost:8080/api/h2-server/start
```

#### Stop H2 Servers
```bash
POST http://localhost:8080/api/h2-server/stop
```

#### Individual Server Control
```bash
# TCP Server
POST http://localhost:8080/api/h2-server/tcp/start
POST http://localhost:8080/api/h2-server/tcp/stop

# Web Console
POST http://localhost:8080/api/h2-server/web/start
POST http://localhost:8080/api/h2-server/web/stop
```

## Connection Details

### TCP Server
- **Port**: 9092
- **Base Directory**: `./data`
- **Connection URL Format**: `jdbc:h2:tcp://localhost:9092/./data/[database-name]`

### Web Console
- **Port**: 8082
- **Access URL**: http://localhost:8082
- **Username**: sa
- **Password**: (leave empty)

### Database Connections
- **API Service Config**: `jdbc:h2:tcp://localhost:9092/./data/api-service-config`
- **Metrics Database**: `jdbc:h2:tcp://localhost:9092/./data/metrics`

## Configuration Options

### Server Configuration Parameters

The H2 server can be configured with various parameters:

- `-tcp`: Enable TCP server
- `-tcpAllowOthers`: Allow external connections
- `-tcpPort 9092`: Set TCP port
- `-baseDir ./data`: Set base directory for databases
- `-ifNotExists`: Create database if it doesn't exist
- `-web`: Enable web console
- `-webAllowOthers`: Allow external web console access
- `-webPort 8082`: Set web console port

### Application Configuration

You can switch between embedded and server mode by modifying the database URLs:

#### Embedded Mode (Single Connection)
```yaml
database:
  url: jdbc:h2:./data/api-service-config;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1
```

#### Server Mode (Multiple Connections)
```yaml
database:
  url: jdbc:h2:tcp://localhost:9092/./data/api-service-config;DB_CLOSE_DELAY=-1
```

## Troubleshooting

### Common Issues

1. **Port Already in Use**
   - Check if another H2 server is running: `netstat -an | grep 9092`
   - Kill existing process or use different port

2. **Connection Refused**
   - Ensure H2 server is started before application
   - Check firewall settings
   - Verify correct port and host configuration

3. **Database Not Found**
   - Ensure base directory exists and is writable
   - Check database file permissions
   - Use `-ifNotExists` parameter when starting server

4. **H2 Jar Not Found**
   - Run `mvn dependency:resolve` to download dependencies
   - Check Maven repository path: `~/.m2/repository/com/h2database/h2/`

### Verification Steps

1. **Check Server Status**
   ```bash
   curl http://localhost:8080/api/h2-server/status
   ```

2. **Test Database Connection**
   - Open web console: http://localhost:8082
   - Use connection URL: `jdbc:h2:tcp://localhost:9092/./data/api-service-config`
   - Username: `sa`, Password: (empty)

3. **View Database Tables**
   ```sql
   SHOW TABLES;
   SELECT * FROM INFORMATION_SCHEMA.TABLES;
   ```

## Best Practices

1. **Start H2 Server First**: Always start the H2 server before starting your applications
2. **Use Consistent Ports**: Keep TCP and web console ports consistent across environments
3. **Monitor Connections**: Use web console to monitor active connections and database status
4. **Backup Databases**: Regular backup of database files in the `./data` directory
5. **Security**: In production, disable external connections and use authentication

## Production Considerations

For production environments:

1. **Security**: Remove `-tcpAllowOthers` and `-webAllowOthers` flags
2. **Authentication**: Configure H2 user authentication
3. **SSL**: Enable SSL for TCP connections
4. **Monitoring**: Set up monitoring for H2 server process
5. **Backup**: Implement automated database backup strategy
6. **Resource Limits**: Configure appropriate memory and connection limits

## Alternative Database Options

While H2 is excellent for development and testing, consider these alternatives for production:

- **PostgreSQL**: Full-featured relational database
- **MySQL**: Popular open-source database
- **SQLite**: Lightweight file-based database
- **Cloud Databases**: AWS RDS, Google Cloud SQL, Azure Database

The application's configuration-driven approach makes it easy to switch database providers by updating the JDBC URL and driver configuration.

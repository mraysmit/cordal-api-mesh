package dev.cordal.integration.postgresql.container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages PostgreSQL containers for dual-database integration testing
 * Handles lifecycle of multiple PostgreSQL containers with different databases
 */
public class PostgreSQLContainerManager {
    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLContainerManager.class);
    
    private static final String POSTGRESQL_IMAGE = "postgres:15-alpine";
    private static final String DEFAULT_USERNAME = "testuser";
    private static final String DEFAULT_PASSWORD = "testpass";
    
    private final Map<String, PostgreSQLContainer<?>> containers;
    private final Map<String, DatabaseConnectionInfo> connectionInfos;
    
    public PostgreSQLContainerManager() {
        this.containers = new HashMap<>();
        this.connectionInfos = new HashMap<>();
    }
    
    /**
     * Create and configure a PostgreSQL container for a specific database
     * 
     * @param databaseName Name of the database (used as container identifier)
     * @param databaseSchema Name of the database schema to create
     * @return The configured PostgreSQL container
     */
    public PostgreSQLContainer<?> createContainer(String databaseName, String databaseSchema) {
        logger.info("Creating PostgreSQL container for database: {}", databaseName);
        
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(DockerImageName.parse(POSTGRESQL_IMAGE))
                .withDatabaseName(databaseSchema)
                .withUsername(DEFAULT_USERNAME)
                .withPassword(DEFAULT_PASSWORD)
                .withReuse(false) // Don't reuse containers to ensure clean state
                .waitingFor(Wait.forListeningPort())
                .withStartupTimeout(Duration.ofMinutes(2));
        
        // Configure container for better performance in tests
        container.withEnv("POSTGRES_INITDB_ARGS", "--auth-host=trust");
        container.withCommand("postgres", "-c", "fsync=off", "-c", "synchronous_commit=off");
        
        containers.put(databaseName, container);
        
        logger.info("PostgreSQL container created for database: {}", databaseName);
        return container;
    }
    
    /**
     * Start a specific PostgreSQL container
     * 
     * @param databaseName Name of the database container to start
     * @throws IllegalArgumentException if container doesn't exist
     */
    public void startContainer(String databaseName) {
        PostgreSQLContainer<?> container = containers.get(databaseName);
        if (container == null) {
            throw new IllegalArgumentException("Container not found for database: " + databaseName);
        }
        
        logger.info("Starting PostgreSQL container for database: {}", databaseName);
        container.start();
        
        // Store connection information
        DatabaseConnectionInfo connectionInfo = new DatabaseConnectionInfo(
                container.getJdbcUrl(),
                container.getUsername(),
                container.getPassword(),
                container.getDatabaseName(),
                container.getHost(),
                container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)
        );
        
        connectionInfos.put(databaseName, connectionInfo);
        
        logger.info("PostgreSQL container started for database: {} on port: {}", 
                   databaseName, container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT));
    }
    
    /**
     * Start all configured containers
     */
    public void startAllContainers() {
        logger.info("Starting all PostgreSQL containers");
        
        for (String databaseName : containers.keySet()) {
            startContainer(databaseName);
        }
        
        logger.info("All PostgreSQL containers started successfully");
    }
    
    /**
     * Stop a specific PostgreSQL container
     * 
     * @param databaseName Name of the database container to stop
     */
    public void stopContainer(String databaseName) {
        PostgreSQLContainer<?> container = containers.get(databaseName);
        if (container != null && container.isRunning()) {
            logger.info("Stopping PostgreSQL container for database: {}", databaseName);
            container.stop();
            connectionInfos.remove(databaseName);
        }
    }
    
    /**
     * Stop all containers and clean up resources
     */
    public void stopAllContainers() {
        logger.info("Stopping all PostgreSQL containers");
        
        for (String databaseName : containers.keySet()) {
            stopContainer(databaseName);
        }
        
        containers.clear();
        connectionInfos.clear();
        
        logger.info("All PostgreSQL containers stopped and cleaned up");
    }
    
    /**
     * Get connection information for a specific database
     * 
     * @param databaseName Name of the database
     * @return Connection information
     * @throws IllegalArgumentException if database not found or not started
     */
    public DatabaseConnectionInfo getConnectionInfo(String databaseName) {
        DatabaseConnectionInfo connectionInfo = connectionInfos.get(databaseName);
        if (connectionInfo == null) {
            throw new IllegalArgumentException("Connection info not found for database: " + databaseName + 
                                             ". Make sure the container is started.");
        }
        return connectionInfo;
    }
    
    /**
     * Get a database connection for a specific database
     * 
     * @param databaseName Name of the database
     * @return Database connection
     * @throws SQLException if connection fails
     */
    public Connection getConnection(String databaseName) throws SQLException {
        DatabaseConnectionInfo connectionInfo = getConnectionInfo(databaseName);
        
        logger.debug("Creating connection to database: {} at {}", databaseName, connectionInfo.jdbcUrl());
        
        return DriverManager.getConnection(
                connectionInfo.jdbcUrl(),
                connectionInfo.username(),
                connectionInfo.password()
        );
    }
    
    /**
     * Check if a container is running
     * 
     * @param databaseName Name of the database
     * @return true if container is running, false otherwise
     */
    public boolean isContainerRunning(String databaseName) {
        PostgreSQLContainer<?> container = containers.get(databaseName);
        return container != null && container.isRunning();
    }
    
    /**
     * Get all configured database names
     * 
     * @return Set of database names
     */
    public java.util.Set<String> getDatabaseNames() {
        return containers.keySet();
    }
    
    /**
     * Wait for all containers to be ready
     * 
     * @param timeoutSeconds Maximum time to wait in seconds
     * @throws RuntimeException if containers don't become ready in time
     */
    public void waitForAllContainersReady(int timeoutSeconds) {
        logger.info("Waiting for all PostgreSQL containers to be ready (timeout: {}s)", timeoutSeconds);
        
        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeoutSeconds * 1000L;
        
        for (String databaseName : containers.keySet()) {
            PostgreSQLContainer<?> container = containers.get(databaseName);
            if (container != null && container.isRunning()) {
                long remainingTime = timeoutMillis - (System.currentTimeMillis() - startTime);
                if (remainingTime <= 0) {
                    throw new RuntimeException("Timeout waiting for containers to be ready");
                }
                
                // Test connection to ensure container is fully ready
                try (Connection connection = getConnection(databaseName)) {
                    connection.createStatement().execute("SELECT 1");
                    logger.debug("Container for database {} is ready", databaseName);
                } catch (SQLException e) {
                    throw new RuntimeException("Container for database " + databaseName + " is not ready", e);
                }
            }
        }
        
        logger.info("All PostgreSQL containers are ready");
    }
    
    /**
     * Record containing database connection information
     */
    public record DatabaseConnectionInfo(
            String jdbcUrl,
            String username,
            String password,
            String databaseName,
            String host,
            Integer port
    ) {
        /**
         * Get JDBC URL with specific schema
         */
        public String getJdbcUrlWithSchema(String schema) {
            return jdbcUrl + "?currentSchema=" + schema;
        }
        
        /**
         * Get connection string for configuration files
         */
        public String getConfigurationConnectionString() {
            return String.format("jdbc:postgresql://%s:%d/%s?currentSchema=public", 
                                host, port, databaseName);
        }
    }
}

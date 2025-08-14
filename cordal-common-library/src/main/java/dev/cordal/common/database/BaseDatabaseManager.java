package dev.cordal.common.database;

import com.zaxxer.hikari.HikariDataSource;
import dev.cordal.common.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Abstract base class for database managers
 * Provides common database operations and connection management
 */
public abstract class BaseDatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(BaseDatabaseManager.class);
    
    protected final HikariDataSource dataSource;
    protected final DatabaseConfig databaseConfig;

    public BaseDatabaseManager(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
        this.dataSource = DatabaseConnectionFactory.createDataSource(databaseConfig);
        logger.info("BaseDatabaseManager initialized for database: {}", databaseConfig.getName());
    }

    /**
     * Abstract method for initializing database schema
     * Each implementation should define its own schema
     */
    public abstract void initializeSchema();

    /**
     * Get a connection from the data source
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Check if the database is accessible
     */
    public boolean isHealthy() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("SELECT 1");
            return true;

        } catch (SQLException e) {
            logger.error("Database health check failed for {}", databaseConfig.getName(), e);
            return false;
        }
    }

    /**
     * Execute a SQL statement (for schema creation, etc.)
     */
    protected void executeSql(String sql) throws SQLException {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            
            statement.execute(sql);
            logger.debug("Executed SQL successfully: {}", sql.substring(0, Math.min(50, sql.length())));
            
        } catch (SQLException e) {
            logger.error("Failed to execute SQL: {}", sql.substring(0, Math.min(50, sql.length())), e);
            throw e;
        }
    }

    /**
     * Execute multiple SQL statements
     */
    protected void executeSqlStatements(String... sqlStatements) throws SQLException {
        for (String sql : sqlStatements) {
            executeSql(sql);
        }
    }

    /**
     * Get the data source
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Get the database configuration
     */
    public DatabaseConfig getDatabaseConfig() {
        return databaseConfig;
    }

    /**
     * Close the data source
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed for: {}", databaseConfig.getName());
        }
    }

    /**
     * Get connection pool statistics
     */
    public String getPoolStats() {
        if (dataSource != null) {
            return String.format("Pool[%s]: Active=%d, Idle=%d, Total=%d, Waiting=%d",
                databaseConfig.getName(),
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariPoolMXBean().getIdleConnections(),
                dataSource.getHikariPoolMXBean().getTotalConnections(),
                dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
        }
        return "Pool statistics not available";
    }
}

package dev.cordal.common.database;

import dev.cordal.common.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Data manager for stock trades database
 * This class wraps a DataSource to work with the StockTradesSchemaManager and DataLoader
 * It implements BaseDatabaseManager interface to be compatible with existing infrastructure
 */
public class StockTradesDataManager extends BaseDatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(StockTradesDataManager.class);
    
    private final DataSource externalDataSource;
    private final String databaseName;
    
    public StockTradesDataManager(DataSource dataSource, String databaseName) {
        super(createDummyConfig(databaseName));
        this.externalDataSource = dataSource;
        this.databaseName = databaseName;
        logger.info("StockTradesDataManager initialized for database: {}", databaseName);
    }
    
    /**
     * Create a dummy config for the parent constructor
     * The actual DataSource is provided externally
     */
    private static DatabaseConfig createDummyConfig(String databaseName) {
        DatabaseConfig config = new DatabaseConfig();
        config.setName(databaseName);
        config.setUrl("external"); // Placeholder since we use external DataSource
        config.setUsername("external");
        config.setPassword("");
        config.setDriver("external");
        return config;
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        return externalDataSource.getConnection();
    }
    
    @Override
    public void initializeSchema() {
        // Schema initialization is handled by StockTradesSchemaManager
        logger.info("Schema initialization for {} should be handled by StockTradesSchemaManager", databaseName);
    }
    
    @Override
    public boolean isHealthy() {
        try (Connection connection = getConnection()) {
            return connection.isValid(5); // 5 second timeout
        } catch (SQLException e) {
            logger.error("Health check failed for database: {}", databaseName, e);
            return false;
        }
    }
    
    @Override
    public void close() {
        // DataSource lifecycle is managed externally by DatabaseConnectionManager
        logger.info("Close called for StockTradesDataManager ({}), DataSource managed externally", databaseName);
    }
    
    /**
     * Get the database name
     */
    public String getDatabaseName() {
        return databaseName;
    }
}

package dev.mars.test;

import dev.mars.common.database.DataLoader;
import dev.mars.common.database.DataLoaderConfig;
import dev.mars.common.database.StockTradesDataManager;
import dev.mars.common.database.StockTradesSchemaManager;
import dev.mars.generic.database.DatabaseConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;

/**
 * Initializer for the stock trades database - FOR TESTING ONLY
 * Handles schema creation and sample data loading for the stocktrades database
 * This ensures stock trades data is created in the correct database, not in api-service-config
 * This class should only be used in test environments to set up test data
 * Note: Performance metrics are managed by the metrics-service, not here
 */
@Singleton
public class StockTradesInitializer {
    private static final Logger logger = LoggerFactory.getLogger(StockTradesInitializer.class);
    
    private static final String STOCKTRADES_DATABASE = "stocktrades";
    
    private final DatabaseConnectionManager databaseConnectionManager;
    private StockTradesDataManager stockTradesDataManager;
    private StockTradesSchemaManager schemaManager;
    private DataLoader dataLoader;
    
    @Inject
    public StockTradesInitializer(DatabaseConnectionManager databaseConnectionManager) {
        this.databaseConnectionManager = databaseConnectionManager;
    }
    
    /**
     * Initialize the stock trades database for testing
     * This should be called after the DatabaseConnectionManager is ready
     */
    public void initialize() {
        logger.info("Initializing stock trades database for testing");
        
        try {
            // Check if stocktrades database is configured
            if (!databaseConnectionManager.getDatabaseNames().contains(STOCKTRADES_DATABASE)) {
                logger.warn("Stock trades database '{}' is not configured, skipping initialization", STOCKTRADES_DATABASE);
                return;
            }
            
            // Get the DataSource for stocktrades database
            DataSource stockTradesDataSource = databaseConnectionManager.getDataSource(STOCKTRADES_DATABASE);
            
            // Create data manager for stocktrades database
            stockTradesDataManager = new StockTradesDataManager(stockTradesDataSource, STOCKTRADES_DATABASE);
            
            // Create schema manager and initialize schema
            schemaManager = new StockTradesSchemaManager(stockTradesDataManager);
            schemaManager.initializeSchema();
            
            // Create data loader for sample data
            StockTradesDataLoaderConfig config = new StockTradesDataLoaderConfig();
            dataLoader = new DataLoader(stockTradesDataManager, config);
            
            // Load sample data if needed
            dataLoader.loadSampleDataIfNeeded();
            
            logger.info("Stock trades database initialized successfully for testing");
            
        } catch (Exception e) {
            logger.error("Failed to initialize stock trades database for testing", e);
            // Don't throw exception here - let the application start even if stock trades DB fails
            // This allows the API service to work even if external databases are not available
        }
    }
    
    /**
     * Check if the stock trades database is healthy
     */
    public boolean isStockTradesDatabaseHealthy() {
        if (schemaManager == null) {
            return false;
        }
        return schemaManager.isHealthy();
    }
    
    /**
     * Clean the stock trades database (for testing)
     */
    public void cleanStockTradesDatabase() {
        if (schemaManager != null) {
            schemaManager.cleanDatabase();
        }
    }
    
    /**
     * Configuration for stock trades data loader - FOR TESTING ONLY
     */
    private static class StockTradesDataLoaderConfig implements DataLoaderConfig {
        
        @Override
        public String getDatabaseUrl() {
            return "stocktrades"; // Database name for logging
        }
        
        @Override
        public boolean isSampleDataLoadingEnabled() {
            return true; // Enable sample data loading for testing
        }
        
        @Override
        public int getSampleDataSize() {
            return 100; // Load 100 sample stock trades for testing
        }
    }
}

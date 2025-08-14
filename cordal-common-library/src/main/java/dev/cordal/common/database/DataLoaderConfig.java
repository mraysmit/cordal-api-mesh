package dev.cordal.common.database;

/**
 * Configuration interface for DataLoader
 * Provides abstraction for configuration dependencies
 */
public interface DataLoaderConfig {
    
    /**
     * Get the database URL for logging purposes
     * @return database URL
     */
    String getDatabaseUrl();
    
    /**
     * Check if sample data loading is enabled
     * @return true if sample data should be loaded, false otherwise
     */
    default boolean isSampleDataLoadingEnabled() {
        return true; // Default to enabled for backward compatibility
    }
    
    /**
     * Get the sample data size
     * @return number of sample records to load
     */
    default int getSampleDataSize() {
        return 100; // Default sample size
    }
}

package dev.mars.test;

import dev.mars.common.database.DataLoader;
import dev.mars.common.database.DataLoaderConfig;
import dev.mars.config.GenericApiConfig;

/**
 * Test data loader for testing purposes only
 * Uses the common-library DataLoader with test configuration
 */
public class TestDataLoader {
    
    private final DataLoader dataLoader;
    
    public TestDataLoader(TestDatabaseManager testDatabaseManager, GenericApiConfig genericApiConfig) {
        TestDataLoaderConfig config = new TestDataLoaderConfig(genericApiConfig);
        this.dataLoader = new DataLoader(testDatabaseManager, config);
    }
    
    /**
     * Load sample data for testing
     */
    public void loadSampleDataIfNeeded() {
        dataLoader.loadSampleDataIfNeeded();
    }
    
    /**
     * Test implementation of DataLoaderConfig
     */
    private static class TestDataLoaderConfig implements DataLoaderConfig {
        private final GenericApiConfig genericApiConfig;
        
        public TestDataLoaderConfig(GenericApiConfig genericApiConfig) {
            this.genericApiConfig = genericApiConfig;
        }
        
        @Override
        public String getDatabaseUrl() {
            return genericApiConfig.getDatabaseUrl();
        }
        
        @Override
        public boolean isSampleDataLoadingEnabled() {
            return true; // Always enable for testing
        }
        
        @Override
        public int getSampleDataSize() {
            return 100; // Test sample size
        }
    }
}

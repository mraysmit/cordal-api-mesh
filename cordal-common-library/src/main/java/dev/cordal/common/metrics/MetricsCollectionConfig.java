package dev.cordal.common.metrics;

import java.util.List;

/**
 * Configuration interface for metrics collection
 * Common metrics configuration used across all modules
 */
public interface MetricsCollectionConfig {
    
    /**
     * Check if metrics collection is enabled
     */
    boolean isEnabled();
    
    /**
     * Check if metrics should be saved asynchronously
     */
    boolean isAsyncSave();
    
    /**
     * Get the sampling rate for metrics collection (0.0 to 1.0)
     */
    double getSamplingRate();
    
    /**
     * Get list of paths to exclude from metrics collection
     */
    List<String> getExcludePaths();
}

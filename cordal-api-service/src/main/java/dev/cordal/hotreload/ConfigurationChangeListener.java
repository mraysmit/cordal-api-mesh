package dev.cordal.hotreload;

/**
 * Interface for listening to configuration file changes
 */
public interface ConfigurationChangeListener {
    
    /**
     * Called when a configuration file has been changed
     * 
     * @param event The file change event containing details about the change
     */
    void onConfigurationFileChanged(FileChangeEvent event);
}

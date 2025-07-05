package dev.mars.generic.config;

import dev.mars.config.GenericApiConfig;
import dev.mars.database.loader.DatabaseConfigurationLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Factory for creating the appropriate configuration loader based on the configuration source
 */
@Singleton
public class ConfigurationLoaderFactory {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationLoaderFactory.class);

    private final GenericApiConfig genericApiConfig;
    private final ConfigurationLoader yamlConfigurationLoader;
    private final DatabaseConfigurationLoader databaseConfigurationLoader;

    @Inject
    public ConfigurationLoaderFactory(GenericApiConfig genericApiConfig,
                                    ConfigurationLoader yamlConfigurationLoader,
                                    DatabaseConfigurationLoader databaseConfigurationLoader) {
        this.genericApiConfig = genericApiConfig;
        this.yamlConfigurationLoader = yamlConfigurationLoader;
        this.databaseConfigurationLoader = databaseConfigurationLoader;
        logger.info("Configuration loader factory initialized");
    }

    /**
     * Create the appropriate configuration loader based on the configuration source
     */
    public ConfigurationLoaderInterface createConfigurationLoader() {
        String configSource = genericApiConfig.getConfigSource();
        logger.info("Creating configuration loader for source: {}", configSource);

        switch (configSource.toLowerCase()) {
            case "yaml":
                logger.info("Using YAML configuration loader");
                return yamlConfigurationLoader;
                
            case "database":
                logger.info("Using database configuration loader");
                return databaseConfigurationLoader;
                
            default:
                logger.warn("Unknown configuration source '{}', defaulting to YAML", configSource);
                return yamlConfigurationLoader;
        }
    }

    /**
     * Get the current configuration source
     */
    public String getConfigurationSource() {
        return genericApiConfig.getConfigSource();
    }

    /**
     * Check if the current source is YAML
     */
    public boolean isYamlSource() {
        return "yaml".equalsIgnoreCase(genericApiConfig.getConfigSource());
    }

    /**
     * Check if the current source is database
     */
    public boolean isDatabaseSource() {
        return "database".equalsIgnoreCase(genericApiConfig.getConfigSource());
    }
}

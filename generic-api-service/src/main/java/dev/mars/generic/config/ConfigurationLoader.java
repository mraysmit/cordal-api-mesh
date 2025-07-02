package dev.mars.generic.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.inject.Inject;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import dev.mars.config.GenericApiConfig;

/**
 * Loads YAML configuration files for queries and API endpoints
 */
@Singleton
public class ConfigurationLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationLoader.class);

    private final ObjectMapper yamlMapper;
    private final GenericApiConfig genericApiConfig;

    @Inject
    public ConfigurationLoader(GenericApiConfig genericApiConfig) {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.genericApiConfig = genericApiConfig;

        // Log the configuration paths that will be used
        logger.info("ConfigurationLoader initialized with paths:");
        logger.info("  - Databases config path: {}", genericApiConfig.getDatabasesConfigPath());
        logger.info("  - Queries config path: {}", genericApiConfig.getQueriesConfigPath());
        logger.info("  - Endpoints config path: {}", genericApiConfig.getEndpointsConfigPath());
    }

    /**
     * Resolve a configuration resource with fallback paths
     */
    private InputStream resolveConfigResource(String configPath) {
        logger.debug("Attempting to load resource: {}", configPath);

        // Try direct path first
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configPath);

        // Try with config/ prefix if not found
        if (inputStream == null && !configPath.startsWith("config/")) {
            String altPath = "config/" + configPath;
            logger.debug("Resource not found, trying alternative path: {}", altPath);
            inputStream = getClass().getClassLoader().getResourceAsStream(altPath);
        }

        // Try without config/ prefix if not found
        if (inputStream == null && configPath.startsWith("config/")) {
            String altPath = configPath.substring(7);
            logger.debug("Resource not found, trying alternative path: {}", altPath);
            inputStream = getClass().getClassLoader().getResourceAsStream(altPath);
        }

        return inputStream;
    }
    
    /**
     * Load query configurations from YAML file
     */
    public Map<String, QueryConfig> loadQueryConfigurations() {
        String configPath = genericApiConfig.getQueriesConfigPath();
        logger.info("Loading query configurations from path: {}", configPath);

        try (InputStream inputStream = resolveConfigResource(configPath)) {
            if (inputStream == null) {
                logger.error("Configuration file not found: {}", configPath);
                // Return empty map instead of throwing exception
                logger.warn("Returning empty query configuration map");
                return new HashMap<>();
            }

            QueriesWrapper wrapper = yamlMapper.readValue(inputStream, QueriesWrapper.class);
            Map<String, QueryConfig> queries = wrapper.getQueries();

            logger.info("Successfully loaded {} query configurations from {}", queries.size(), configPath);

            // Log each query for debugging
            queries.forEach((key, config) -> {
                logger.info("  - Query '{}': {} (database: {})", key, config.getName(), config.getDatabase());
            });

            return queries;

        } catch (Exception e) {
            logger.error("Failed to load query configurations", e);
            // Return empty map instead of throwing exception
            logger.warn("Returning empty query configuration map due to error");
            return new HashMap<>();
        }
    }
    
    /**
     * Load database configurations from YAML file
     */
    public Map<String, DatabaseConfig> loadDatabaseConfigurations() {
        String configPath = genericApiConfig.getDatabasesConfigPath();
        logger.info("Loading database configurations from path: {}", configPath);

        try (InputStream inputStream = resolveConfigResource(configPath)) {
            if (inputStream == null) {
                logger.error("Configuration file not found: {}", configPath);
                // Return empty map instead of throwing exception
                logger.warn("Returning empty database configuration map");
                return new HashMap<>();
            }

            DatabasesWrapper wrapper = yamlMapper.readValue(inputStream, DatabasesWrapper.class);
            Map<String, DatabaseConfig> databases = wrapper.getDatabases();

            logger.info("Successfully loaded {} database configurations from {}", databases.size(), configPath);

            // Log each database for debugging
            databases.forEach((key, config) -> {
                logger.info("  - Database '{}': {} (url: {})", key, config.getName(), config.getUrl());
            });

            return databases;

        } catch (Exception e) {
            logger.error("Failed to load database configurations", e);
            // Return empty map instead of throwing exception
            logger.warn("Returning empty database configuration map due to error");
            return new HashMap<>();
        }
    }

    /**
     * Load API endpoint configurations from YAML file
     */
    public Map<String, ApiEndpointConfig> loadEndpointConfigurations() {
        String configPath = genericApiConfig.getEndpointsConfigPath();
        logger.info("Loading endpoint configurations from path: {}", configPath);

        try (InputStream inputStream = resolveConfigResource(configPath)) {
            if (inputStream == null) {
                logger.error("Configuration file not found: {}", configPath);
                // Return empty map instead of throwing exception
                logger.warn("Returning empty endpoint configuration map");
                return new HashMap<>();
            }

            EndpointsWrapper wrapper = yamlMapper.readValue(inputStream, EndpointsWrapper.class);
            Map<String, ApiEndpointConfig> endpoints = wrapper.getEndpoints();

            logger.info("Successfully loaded {} endpoint configurations from {}", endpoints.size(), configPath);

            // Log each endpoint for debugging
            endpoints.forEach((key, config) -> {
                logger.info("  - Endpoint '{}': {} {} (query: {})", key, config.getMethod(), config.getPath(), config.getQuery());
            });

            return endpoints;

        } catch (Exception e) {
            logger.error("Failed to load endpoint configurations", e);
            // Return empty map instead of throwing exception
            logger.warn("Returning empty endpoint configuration map due to error");
            return new HashMap<>();
        }
    }
    
    /**
     * Wrapper class for queries YAML structure
     */
    public static class QueriesWrapper {
        private Map<String, QueryConfig> queries;
        
        public Map<String, QueryConfig> getQueries() {
            return queries;
        }
        
        public void setQueries(Map<String, QueryConfig> queries) {
            this.queries = queries;
        }
    }
    
    /**
     * Wrapper class for endpoints YAML structure
     */
    public static class EndpointsWrapper {
        private Map<String, ApiEndpointConfig> endpoints;

        public Map<String, ApiEndpointConfig> getEndpoints() {
            return endpoints;
        }

        public void setEndpoints(Map<String, ApiEndpointConfig> endpoints) {
            this.endpoints = endpoints;
        }
    }

    /**
     * Wrapper class for databases YAML structure
     */
    public static class DatabasesWrapper {
        private Map<String, DatabaseConfig> databases;

        public Map<String, DatabaseConfig> getDatabases() {
            return databases;
        }

        public void setDatabases(Map<String, DatabaseConfig> databases) {
            this.databases = databases;
        }
    }
}

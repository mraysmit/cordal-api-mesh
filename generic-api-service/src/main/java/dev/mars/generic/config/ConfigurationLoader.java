package dev.mars.generic.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.inject.Inject;
import java.io.InputStream;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.Map;
import java.util.HashMap;
import dev.mars.config.GenericApiConfig;



/**
 * Loads YAML configuration files for queries and API endpoints
 */
@Singleton
public class ConfigurationLoader implements ConfigurationLoaderInterface {
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

        // First, try to load as external file if path contains relative or absolute indicators
        if (configPath.startsWith("./") || configPath.startsWith("../") || configPath.startsWith("/") ||
            (configPath.length() > 1 && configPath.charAt(1) == ':')) {
            try {
                // Resolve the path relative to the current working directory
                Path filePath = Paths.get(configPath).toAbsolutePath().normalize();
                logger.debug("Trying to load external file: {}", filePath);

                if (Files.exists(filePath)) {
                    logger.info("Successfully loading external file: {}", filePath);
                    return new FileInputStream(filePath.toFile());
                } else {
                    logger.debug("External file not found: {}", filePath);
                }
            } catch (Exception e) {
                logger.debug("Failed to load external file: {}", configPath, e);
            }
        }

        // Try direct classpath path
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configPath);
        if (inputStream != null) {
            logger.debug("Loaded from classpath: {}", configPath);
            return inputStream;
        }

        // Try with config/ prefix if not found
        if (!configPath.startsWith("config/")) {
            String altPath = "config/" + configPath;
            logger.debug("Resource not found, trying alternative classpath path: {}", altPath);
            inputStream = getClass().getClassLoader().getResourceAsStream(altPath);
            if (inputStream != null) {
                logger.debug("Loaded from classpath with config/ prefix: {}", altPath);
                return inputStream;
            }
        }

        // Try without config/ prefix if not found
        if (configPath.startsWith("config/")) {
            String altPath = configPath.substring(7);
            logger.debug("Resource not found, trying alternative classpath path: {}", altPath);
            inputStream = getClass().getClassLoader().getResourceAsStream(altPath);
            if (inputStream != null) {
                logger.debug("Loaded from classpath without config/ prefix: {}", altPath);
                return inputStream;
            }
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
                logger.error("FATAL CONFIGURATION ERROR: Required configuration file not found");
                logger.error("  File: {}", configPath);
                logger.error("  Type: Query configurations");
                logger.error("  Impact: Application cannot start without query configurations");
                logger.error("  Action: Ensure the query configuration file exists and is accessible");
                logger.error("Application startup aborted due to missing configuration file");
                System.exit(1);
            }

            QueriesWrapper wrapper = yamlMapper.readValue(inputStream, QueriesWrapper.class);
            Map<String, QueryConfig> queries = wrapper.getQueries();

            if (queries == null || queries.isEmpty()) {
                logger.error("FATAL CONFIGURATION ERROR: No query configurations found in file");
                logger.error("  File: {}", configPath);
                logger.error("  Type: Query configurations");
                logger.error("  Issue: File exists but contains no valid query definitions");
                logger.error("  Impact: Application cannot start without query configurations");
                logger.error("  Action: Verify the file contains valid YAML with query definitions");
                logger.error("Application startup aborted due to empty configuration file");
                System.exit(1);
            }

            logger.info("Successfully loaded {} query configurations from {}", queries.size(), configPath);

            // Log each query for debugging
            queries.forEach((key, config) -> {
                logger.info("  - Query '{}': {} (database: {})", key, config.getName(), config.getDatabase());
            });

            return queries;

        } catch (Exception e) {
            logger.error("FATAL CONFIGURATION ERROR: Failed to load query configurations");
            logger.error("  File: {}", configPath);
            logger.error("  Type: Query configurations");
            logger.error("  Error: {}", e.getMessage());
            logger.error("  Impact: Application cannot start without valid query configurations");
            logger.error("  Action: Check file format, syntax, and accessibility");
            logger.error("Application startup aborted due to configuration loading failure", e);
            System.exit(1);
            return null; // Never reached, but needed for compilation
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
                logger.error("FATAL CONFIGURATION ERROR: Required configuration file not found");
                logger.error("  File: {}", configPath);
                logger.error("  Type: Database configurations");
                logger.error("  Impact: Application cannot start without database configurations");
                logger.error("  Action: Ensure the database configuration file exists and is accessible");
                logger.error("Application startup aborted due to missing configuration file");
                System.exit(1);
            }

            DatabasesWrapper wrapper = yamlMapper.readValue(inputStream, DatabasesWrapper.class);
            Map<String, DatabaseConfig> databases = wrapper.getDatabases();

            if (databases == null || databases.isEmpty()) {
                logger.error("FATAL CONFIGURATION ERROR: No database configurations found in file");
                logger.error("  File: {}", configPath);
                logger.error("  Type: Database configurations");
                logger.error("  Issue: File exists but contains no valid database definitions");
                logger.error("  Impact: Application cannot start without database configurations");
                logger.error("  Action: Verify the file contains valid YAML with database definitions");
                logger.error("Application startup aborted due to empty configuration file");
                System.exit(1);
            }

            logger.info("Successfully loaded {} database configurations from {}", databases.size(), configPath);

            // Log each database for debugging
            databases.forEach((key, config) -> {
                logger.info("  - Database '{}': {} (url: {})", key, config.getName(), config.getUrl());
            });

            return databases;

        } catch (Exception e) {
            logger.error("FATAL CONFIGURATION ERROR: Failed to load database configurations");
            logger.error("  File: {}", configPath);
            logger.error("  Type: Database configurations");
            logger.error("  Error: {}", e.getMessage());
            logger.error("  Impact: Application cannot start without valid database configurations");
            logger.error("  Action: Check file format, syntax, and accessibility");
            logger.error("Application startup aborted due to configuration loading failure", e);
            System.exit(1);
            return null; // Never reached, but needed for compilation
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
                logger.error("FATAL CONFIGURATION ERROR: Required configuration file not found");
                logger.error("  File: {}", configPath);
                logger.error("  Type: Endpoint configurations");
                logger.error("  Impact: Application cannot start without endpoint configurations");
                logger.error("  Action: Ensure the endpoint configuration file exists and is accessible");
                logger.error("Application startup aborted due to missing configuration file");
                System.exit(1);
            }

            EndpointsWrapper wrapper = yamlMapper.readValue(inputStream, EndpointsWrapper.class);
            Map<String, ApiEndpointConfig> endpoints = wrapper.getEndpoints();

            if (endpoints == null || endpoints.isEmpty()) {
                logger.error("FATAL CONFIGURATION ERROR: No endpoint configurations found in file");
                logger.error("  File: {}", configPath);
                logger.error("  Type: Endpoint configurations");
                logger.error("  Issue: File exists but contains no valid endpoint definitions");
                logger.error("  Impact: Application cannot start without endpoint configurations");
                logger.error("  Action: Verify the file contains valid YAML with endpoint definitions");
                logger.error("Application startup aborted due to empty configuration file");
                System.exit(1);
            }

            logger.info("Successfully loaded {} endpoint configurations from {}", endpoints.size(), configPath);

            // Log each endpoint for debugging
            endpoints.forEach((key, config) -> {
                logger.info("  - Endpoint '{}': {} {} (query: {})", key, config.getMethod(), config.getPath(), config.getQuery());
            });

            return endpoints;

        } catch (Exception e) {
            logger.error("FATAL CONFIGURATION ERROR: Failed to load endpoint configurations");
            logger.error("  File: {}", configPath);
            logger.error("  Type: Endpoint configurations");
            logger.error("  Error: {}", e.getMessage());
            logger.error("  Impact: Application cannot start without valid endpoint configurations");
            logger.error("  Action: Check file format, syntax, and accessibility");
            logger.error("Application startup aborted due to configuration loading failure", e);
            System.exit(1);
            return null; // Never reached, but needed for compilation
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

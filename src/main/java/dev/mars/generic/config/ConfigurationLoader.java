package dev.mars.generic.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.InputStream;
import java.util.Map;

/**
 * Loads YAML configuration files for queries and API endpoints
 */
@Singleton
public class ConfigurationLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationLoader.class);
    
    private final ObjectMapper yamlMapper;
    
    public ConfigurationLoader() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }
    
    /**
     * Load query configurations from YAML file
     */
    public Map<String, QueryConfig> loadQueryConfigurations() {
        logger.info("Loading query configurations from queries.yml");
        
        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("config/queries.yml")) {
            
            if (inputStream == null) {
                throw new RuntimeException("queries.yml not found in classpath");
            }
            
            QueriesWrapper wrapper = yamlMapper.readValue(inputStream, QueriesWrapper.class);
            Map<String, QueryConfig> queries = wrapper.getQueries();
            
            logger.info("Loaded {} query configurations", queries.size());
            
            // Log each query for debugging
            queries.forEach((key, config) -> {
                logger.debug("Loaded query '{}': {}", key, config.getName());
            });
            
            return queries;
            
        } catch (Exception e) {
            logger.error("Failed to load query configurations", e);
            throw new RuntimeException("Failed to load query configurations", e);
        }
    }
    
    /**
     * Load API endpoint configurations from YAML file
     */
    public Map<String, ApiEndpointConfig> loadEndpointConfigurations() {
        logger.info("Loading endpoint configurations from api-endpoints.yml");
        
        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("config/api-endpoints.yml")) {
            
            if (inputStream == null) {
                throw new RuntimeException("api-endpoints.yml not found in classpath");
            }
            
            EndpointsWrapper wrapper = yamlMapper.readValue(inputStream, EndpointsWrapper.class);
            Map<String, ApiEndpointConfig> endpoints = wrapper.getEndpoints();
            
            logger.info("Loaded {} endpoint configurations", endpoints.size());
            
            // Log each endpoint for debugging
            endpoints.forEach((key, config) -> {
                logger.debug("Loaded endpoint '{}': {} {}", key, config.getMethod(), config.getPath());
            });
            
            return endpoints;
            
        } catch (Exception e) {
            logger.error("Failed to load endpoint configurations", e);
            throw new RuntimeException("Failed to load endpoint configurations", e);
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
}

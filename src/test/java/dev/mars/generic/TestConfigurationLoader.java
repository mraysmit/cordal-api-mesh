package dev.mars.generic;

import dev.mars.generic.config.ConfigurationLoader;

/**
 * Test configuration loader that loads test-specific configurations
 */
public class TestConfigurationLoader extends ConfigurationLoader {
    
    @Override
    public java.util.Map<String, dev.mars.generic.config.DatabaseConfig> loadDatabaseConfigurations() {
        try (java.io.InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("config/test-databases.yml")) {
            
            if (inputStream == null) {
                throw new RuntimeException("test-databases.yml not found in classpath");
            }
            
            com.fasterxml.jackson.databind.ObjectMapper yamlMapper = 
                new com.fasterxml.jackson.databind.ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
            DatabasesWrapper wrapper = yamlMapper.readValue(inputStream, DatabasesWrapper.class);
            return wrapper.getDatabases();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to load test database configurations", e);
        }
    }
    
    @Override
    public java.util.Map<String, dev.mars.generic.config.QueryConfig> loadQueryConfigurations() {
        try (java.io.InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("config/test-queries.yml")) {
            
            if (inputStream == null) {
                throw new RuntimeException("test-queries.yml not found in classpath");
            }
            
            com.fasterxml.jackson.databind.ObjectMapper yamlMapper = 
                new com.fasterxml.jackson.databind.ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
            QueriesWrapper wrapper = yamlMapper.readValue(inputStream, QueriesWrapper.class);
            return wrapper.getQueries();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to load test query configurations", e);
        }
    }
    
    @Override
    public java.util.Map<String, dev.mars.generic.config.ApiEndpointConfig> loadEndpointConfigurations() {
        try (java.io.InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("config/test-api-endpoints.yml")) {
            
            if (inputStream == null) {
                throw new RuntimeException("test-api-endpoints.yml not found in classpath");
            }
            
            com.fasterxml.jackson.databind.ObjectMapper yamlMapper = 
                new com.fasterxml.jackson.databind.ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
            EndpointsWrapper wrapper = yamlMapper.readValue(inputStream, EndpointsWrapper.class);
            return wrapper.getEndpoints();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to load test endpoint configurations", e);
        }
    }
}

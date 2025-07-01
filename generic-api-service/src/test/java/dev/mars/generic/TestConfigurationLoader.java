package dev.mars.generic;

import dev.mars.generic.config.ConfigurationLoader;
import dev.mars.config.GenericApiConfig;

/**
 * Test configuration loader that loads test-specific configurations
 */
public class TestConfigurationLoader extends ConfigurationLoader {

    public TestConfigurationLoader(GenericApiConfig config) {
        super(config);
    }
    
    @Override
    public java.util.Map<String, dev.mars.generic.config.DatabaseConfig> loadDatabaseConfigurations() {
        java.io.InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("test-databases.yml");

        if (inputStream == null) {
            // Fallback to regular databases.yml if test-databases.yml not found
            inputStream = getClass().getClassLoader().getResourceAsStream("config/databases.yml");
            if (inputStream == null) {
                throw new RuntimeException("Neither test-databases.yml nor databases.yml found in classpath");
            }
        }

        try {
            
            com.fasterxml.jackson.databind.ObjectMapper yamlMapper = 
                new com.fasterxml.jackson.databind.ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
            DatabasesWrapper wrapper = yamlMapper.readValue(inputStream, DatabasesWrapper.class);
            return wrapper.getDatabases();

        } catch (Exception e) {
            throw new RuntimeException("Failed to load test database configurations", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (java.io.IOException e) {
                    // Ignore close errors
                }
            }
        }
    }
    
    @Override
    public java.util.Map<String, dev.mars.generic.config.QueryConfig> loadQueryConfigurations() {
        java.io.InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("test-queries.yml");

        if (inputStream == null) {
            // Fallback to regular queries.yml if test-queries.yml not found
            inputStream = getClass().getClassLoader().getResourceAsStream("config/queries.yml");
            if (inputStream == null) {
                throw new RuntimeException("Neither test-queries.yml nor queries.yml found in classpath");
            }
        }

        try {
            
            com.fasterxml.jackson.databind.ObjectMapper yamlMapper = 
                new com.fasterxml.jackson.databind.ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
            QueriesWrapper wrapper = yamlMapper.readValue(inputStream, QueriesWrapper.class);
            return wrapper.getQueries();

        } catch (Exception e) {
            throw new RuntimeException("Failed to load test query configurations", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (java.io.IOException e) {
                    // Ignore close errors
                }
            }
        }
    }
    
    @Override
    public java.util.Map<String, dev.mars.generic.config.ApiEndpointConfig> loadEndpointConfigurations() {
        java.io.InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("test-api-endpoints.yml");

        if (inputStream == null) {
            // Fallback to regular api-endpoints.yml if test-api-endpoints.yml not found
            inputStream = getClass().getClassLoader().getResourceAsStream("config/api-endpoints.yml");
            if (inputStream == null) {
                throw new RuntimeException("Neither test-api-endpoints.yml nor api-endpoints.yml found in classpath");
            }
        }

        try {
            
            com.fasterxml.jackson.databind.ObjectMapper yamlMapper = 
                new com.fasterxml.jackson.databind.ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
            EndpointsWrapper wrapper = yamlMapper.readValue(inputStream, EndpointsWrapper.class);
            return wrapper.getEndpoints();

        } catch (Exception e) {
            throw new RuntimeException("Failed to load test endpoint configurations", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (java.io.IOException e) {
                    // Ignore close errors
                }
            }
        }
    }
}

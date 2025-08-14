package dev.cordal.generic;

import dev.cordal.generic.config.ConfigurationLoader;
import dev.cordal.config.GenericApiConfig;
import java.util.HashMap;

/**
 * Test configuration loader that loads test-specific configurations
 */
public class TestConfigurationLoader extends ConfigurationLoader {

    public TestConfigurationLoader(GenericApiConfig config) {
        super(config);
    }
    
    @Override
    public java.util.Map<String, dev.cordal.generic.config.DatabaseConfig> loadDatabaseConfigurations() {
        java.io.InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("test-databases.yml");

        if (inputStream == null) {
            // Fallback to regular databases.yml if test-databases.yml not found
            inputStream = getClass().getClassLoader().getResourceAsStream("config/databases.yml");
            if (inputStream == null) {
                // Return empty map instead of throwing exception
                return new HashMap<>();
            }
        }

        try {
            
            com.fasterxml.jackson.databind.ObjectMapper yamlMapper =
                new com.fasterxml.jackson.databind.ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
            DatabasesWrapper wrapper = yamlMapper.readValue(inputStream, DatabasesWrapper.class);
            return wrapper.getDatabases();

        } catch (Exception e) {
            // Return empty map instead of throwing exception
            return new HashMap<>();
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
    public java.util.Map<String, dev.cordal.generic.config.QueryConfig> loadQueryConfigurations() {
        java.io.InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("test-queries.yml");

        if (inputStream == null) {
            // Fallback to regular queries.yml if test-queries.yml not found
            inputStream = getClass().getClassLoader().getResourceAsStream("config/queries.yml");
            if (inputStream == null) {
                // Return empty map instead of throwing exception
                return new HashMap<>();
            }
        }

        try {
            
            com.fasterxml.jackson.databind.ObjectMapper yamlMapper = 
                new com.fasterxml.jackson.databind.ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
            QueriesWrapper wrapper = yamlMapper.readValue(inputStream, QueriesWrapper.class);
            return wrapper.getQueries();

        } catch (Exception e) {
            // Return empty map instead of throwing exception
            return new HashMap<>();
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
    public java.util.Map<String, dev.cordal.generic.config.ApiEndpointConfig> loadEndpointConfigurations() {
        java.io.InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("test-api-endpoints.yml");

        if (inputStream == null) {
            // Fallback to regular api-endpoints.yml if test-api-endpoints.yml not found
            inputStream = getClass().getClassLoader().getResourceAsStream("config/api-endpoints.yml");
            if (inputStream == null) {
                // Return empty map instead of throwing exception
                return new HashMap<>();
            }
        }

        try {
            
            com.fasterxml.jackson.databind.ObjectMapper yamlMapper = 
                new com.fasterxml.jackson.databind.ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
            EndpointsWrapper wrapper = yamlMapper.readValue(inputStream, EndpointsWrapper.class);
            return wrapper.getEndpoints();

        } catch (Exception e) {
            // Return empty map instead of throwing exception
            return new HashMap<>();
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

package dev.mars.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;

/**
 * Configuration class for Generic API Service
 */
public class GenericApiConfig {
    private static final Logger logger = LoggerFactory.getLogger(GenericApiConfig.class);

    private ServerConfig server = new ServerConfig();
    private DatabaseSettings database = new DatabaseSettings();
    private SwaggerSettings swagger = new SwaggerSettings();

    public GenericApiConfig() {
        // Default constructor
    }

    public static GenericApiConfig loadFromFile() {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

            // Check for test configuration first
            String configFile = System.getProperty("config.file", "application.yml");
            logger.info("Loading configuration from: {}", configFile);

            InputStream inputStream = GenericApiConfig.class.getClassLoader().getResourceAsStream(configFile);

            if (inputStream != null) {
                // Read as Map first to avoid recursive constructor calls
                Map<String, Object> configMap = mapper.readValue(inputStream, Map.class);
                GenericApiConfig config = new GenericApiConfig();

                // Manually populate the configuration
                if (configMap.containsKey("server")) {
                    Map<String, Object> serverMap = (Map<String, Object>) configMap.get("server");
                    if (serverMap.containsKey("host")) {
                        config.server.setHost((String) serverMap.get("host"));
                    }
                    if (serverMap.containsKey("port")) {
                        config.server.setPort((Integer) serverMap.get("port"));
                    }
                }

                if (configMap.containsKey("database")) {
                    Map<String, Object> dbMap = (Map<String, Object>) configMap.get("database");
                    if (dbMap.containsKey("url")) {
                        config.database.setUrl((String) dbMap.get("url"));
                    }
                    if (dbMap.containsKey("username")) {
                        config.database.setUsername((String) dbMap.get("username"));
                    }
                    if (dbMap.containsKey("password")) {
                        config.database.setPassword((String) dbMap.get("password"));
                    }
                    if (dbMap.containsKey("driver")) {
                        config.database.setDriver((String) dbMap.get("driver"));
                    }
                }

                if (configMap.containsKey("swagger")) {
                    Map<String, Object> swaggerMap = (Map<String, Object>) configMap.get("swagger");
                    if (swaggerMap.containsKey("enabled")) {
                        config.swagger.setEnabled((Boolean) swaggerMap.get("enabled"));
                    }
                    if (swaggerMap.containsKey("path")) {
                        config.swagger.setPath((String) swaggerMap.get("path"));
                    }
                }

                logger.info("Configuration loaded successfully from {}", configFile);
                return config;
            } else {
                logger.warn("{} not found, using default configuration", configFile);
                return new GenericApiConfig();
            }
        } catch (Exception e) {
            logger.error("Failed to load configuration, using defaults", e);
            return new GenericApiConfig();
        }
    }
    
    // Getters
    public String getServerHost() {
        return server.host;
    }
    
    public int getServerPort() {
        return server.port;
    }
    
    public String getDatabaseUrl() {
        return database.url;
    }
    
    public String getDatabaseUsername() {
        return database.username;
    }
    
    public String getDatabasePassword() {
        return database.password;
    }
    
    public String getDatabaseDriver() {
        return database.driver;
    }
    
    public boolean isSwaggerEnabled() {
        return swagger.enabled;
    }
    
    public String getSwaggerPath() {
        return swagger.path;
    }
    
    // Inner classes for configuration structure
    public static class ServerConfig {
        private String host = "localhost";
        private int port = 8080;
        
        // Getters and setters
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
    }
    
    public static class DatabaseSettings {
        private String url = "jdbc:h2:./data/stocktrades;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";
        private String username = "sa";
        private String password = "";
        private String driver = "org.h2.Driver";
        
        // Getters and setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getDriver() { return driver; }
        public void setDriver(String driver) { this.driver = driver; }
    }
    
    public static class SwaggerSettings {
        private boolean enabled = true;
        private String path = "/swagger";
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }
}

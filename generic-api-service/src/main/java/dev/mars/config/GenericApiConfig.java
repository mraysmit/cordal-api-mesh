package dev.mars.config;

import dev.mars.common.config.BaseConfig;
import dev.mars.common.config.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration class for Generic API Service
 * Extends BaseConfig for common configuration patterns
 */
public class GenericApiConfig extends BaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(GenericApiConfig.class);

    private ServerConfig server;
    private DatabaseSettings database = new DatabaseSettings();
    private SwaggerSettings swagger = new SwaggerSettings();
    private ConfigPaths config = new ConfigPaths();

    public GenericApiConfig() {
        super();
        initializeFromConfig();
    }

    private void initializeFromConfig() {
        // Initialize server configuration from loaded config
        String host = getString("server.host", "localhost");
        Integer port = getInteger("server.port", 8080);
        logger.info("GenericApiConfig - Loading server configuration: host={}, port={}", host, port);
        server = new ServerConfig(host, port);
        logger.info("GenericApiConfig - Server configuration created: {}", server);

        // Load other configuration values
        loadDatabaseConfig();
        loadSwaggerConfig();
        loadConfigPaths();
    }

    private void loadDatabaseConfig() {
        String url = getString("database.url", "jdbc:h2:./data/stocktrades;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1");
        String username = getString("database.username", "sa");
        String password = getString("database.password", "");
        String driver = getString("database.driver", "org.h2.Driver");

        database.url = url;
        database.username = username;
        database.password = password;
        database.driver = driver;
    }

    private void loadSwaggerConfig() {
        Boolean enabled = getBoolean("swagger.enabled", true);
        String path = getString("swagger.path", "/swagger");

        swagger.enabled = enabled;
        swagger.path = path;
    }

    private void loadConfigPaths() {
        // Try to load from config.paths first (new structure)
        String databasesPath = getString("config.paths.databases", null);
        String queriesPath = getString("config.paths.queries", null);
        String endpointsPath = getString("config.paths.endpoints", null);

        // Fall back to old structure if not found
        if (databasesPath == null) {
            databasesPath = getString("config.databasesPath", "config/databases.yml");
        }

        if (queriesPath == null) {
            queriesPath = getString("config.queriesPath", "config/queries.yml");
        }

        if (endpointsPath == null) {
            endpointsPath = getString("config.endpointsPath", "config/api-endpoints.yml");
        }

        config.setDatabasesPath(databasesPath);
        config.setQueriesPath(queriesPath);
        config.setEndpointsPath(endpointsPath);

        logger.info("Configured paths: databases={}, queries={}, endpoints={}",
                    databasesPath, queriesPath, endpointsPath);
    }

    @Override
    protected String getConfigFileName() {
        // Check for custom config file system property (for testing)
        String configFile = System.getProperty("generic.config.file", "application.yml");
        logger.info("GenericApiConfig - Using config file: {} (from system property: {})",
                   configFile, System.getProperty("generic.config.file"));

        // Verify the file exists in classpath
        if (getClass().getClassLoader().getResource(configFile) == null) {
            logger.warn("Config file {} not found in classpath, falling back to default", configFile);
            return "application.yml";
        }

        return configFile;
    }

    public static GenericApiConfig loadFromFile() {
        logger.info("Creating GenericApiConfig instance");
        return new GenericApiConfig();
    }

    // Helper methods for configuration reading
    protected String getString(String path, String defaultValue) {
        return getNestedValue(path, String.class, defaultValue);
    }

    protected Integer getInteger(String path, Integer defaultValue) {
        return getNestedValue(path, Integer.class, defaultValue);
    }

    protected Boolean getBoolean(String path, Boolean defaultValue) {
        return getNestedValue(path, Boolean.class, defaultValue);
    }

    protected Double getDouble(String path, Double defaultValue) {
        return getNestedValue(path, Double.class, defaultValue);
    }



    
    // Getters
    public ServerConfig getServerConfig() {
        return server;
    }

    public String getServerHost() {
        return server.getHost();
    }

    public int getServerPort() {
        return server.getPort();
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

    public String getDatabasesConfigPath() {
        return config.databasesPath;
    }

    public String getQueriesConfigPath() {
        return config.queriesPath;
    }

    public String getEndpointsConfigPath() {
        return config.endpointsPath;
    }
    
    // Inner classes for configuration structure
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

    public static class ConfigPaths {
        private String databasesPath = "config/databases.yml";
        private String queriesPath = "config/queries.yml";
        private String endpointsPath = "config/api-endpoints.yml";

        // Getters and setters
        public String getDatabasesPath() { return databasesPath; }
        public void setDatabasesPath(String databasesPath) { this.databasesPath = databasesPath; }
        public String getQueriesPath() { return queriesPath; }
        public void setQueriesPath(String queriesPath) { this.queriesPath = queriesPath; }
        public String getEndpointsPath() { return endpointsPath; }
        public void setEndpointsPath(String endpointsPath) { this.endpointsPath = endpointsPath; }
    }
}

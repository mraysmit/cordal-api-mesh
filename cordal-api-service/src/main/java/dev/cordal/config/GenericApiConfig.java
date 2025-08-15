package dev.cordal.config;

import dev.cordal.common.config.BaseConfig;
import dev.cordal.common.config.ServerConfig;
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
    private ValidationSettings validation = new ValidationSettings();

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
        loadValidationConfig();
    }

    private void loadDatabaseConfig() {
        String url = getString("database.url", "jdbc:h2:./data/api-service-config;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1");
        String username = getString("database.username", "sa");
        String password = getString("database.password", "");
        String driver = getString("database.driver", "org.h2.Driver");
        Boolean createIfMissing = getBoolean("database.createIfMissing", true);

        database.url = url;
        database.username = username;
        database.password = password;
        database.driver = driver;
        database.createIfMissing = createIfMissing;
    }

    private void loadSwaggerConfig() {
        Boolean enabled = getBoolean("swagger.enabled", true);
        String path = getString("swagger.path", "/swagger");

        swagger.enabled = enabled;
        swagger.path = path;
    }

    private void loadConfigPaths() {
        // Load configuration source option
        String configSource = getString("config.source", "yaml");
        config.setSource(configSource);

        // Load configuration data loading flag
        Boolean loadFromYaml = getBoolean("config.loadFromYaml", false);
        config.setLoadFromYaml(loadFromYaml);

        // Load directory scanning configuration
        loadDirectoryConfiguration();
        loadPatternConfiguration();

        logger.info("Configuration source: {}", configSource);
        logger.info("Load configuration from YAML: {}", loadFromYaml);
        logger.info("Configuration directories: {}", config.getDirectories());
        logger.info("Database patterns: {}", config.getDatabasePatterns());
        logger.info("Query patterns: {}", config.getQueryPatterns());
        logger.info("Endpoint patterns: {}", config.getEndpointPatterns());
    }

    private void loadDirectoryConfiguration() {
        // Check for system property override (used by testing and IDE execution)
        String systemPropertyDirs = System.getProperty("config.directories");
        if (systemPropertyDirs == null) {
            // Also check the legacy property name for backward compatibility
            systemPropertyDirs = System.getProperty("generic.config.directories");
        }

        java.util.List<String> directories;

        if (systemPropertyDirs != null) {
            directories = java.util.Arrays.asList(systemPropertyDirs.split(","));
            logger.info("Using config directories from system property: {}", directories);
        } else {
            // Load directories from configuration file
            directories = getStringList("config.directories");
            if (directories == null || directories.isEmpty()) {
                // Default to both possible locations for compatibility
                directories = java.util.Arrays.asList("generic-config", "../generic-config");
                logger.info("No config directories specified, using defaults: {}", directories);
            }
        }
        config.setDirectories(directories);
    }

    private void loadPatternConfiguration() {
        // Load naming patterns for each configuration type
        // Check for system property overrides first (used by testing)
        String systemDatabasePatterns = System.getProperty("database.patterns");
        java.util.List<String> databasePatterns;
        if (systemDatabasePatterns != null) {
            databasePatterns = java.util.Arrays.asList(systemDatabasePatterns.split(","));
            logger.info("Using database patterns from system property: {}", databasePatterns);
        } else {
            databasePatterns = getStringList("config.patterns.databases");
            if (databasePatterns == null || databasePatterns.isEmpty()) {
                databasePatterns = java.util.Arrays.asList("*-database.yml", "*-databases.yml");
            }
        }
        config.setDatabasePatterns(databasePatterns);

        String systemQueryPatterns = System.getProperty("query.patterns");
        java.util.List<String> queryPatterns;
        if (systemQueryPatterns != null) {
            queryPatterns = java.util.Arrays.asList(systemQueryPatterns.split(","));
            logger.info("Using query patterns from system property: {}", queryPatterns);
        } else {
            queryPatterns = getStringList("config.patterns.queries");
            if (queryPatterns == null || queryPatterns.isEmpty()) {
                queryPatterns = java.util.Arrays.asList("*-query.yml", "*-queries.yml");
            }
        }
        config.setQueryPatterns(queryPatterns);

        String systemEndpointPatterns = System.getProperty("endpoint.patterns");
        java.util.List<String> endpointPatterns;
        if (systemEndpointPatterns != null) {
            endpointPatterns = java.util.Arrays.asList(systemEndpointPatterns.split(","));
            logger.info("Using endpoint patterns from system property: {}", endpointPatterns);
        } else {
            endpointPatterns = getStringList("config.patterns.endpoints");
            if (endpointPatterns == null || endpointPatterns.isEmpty()) {
                endpointPatterns = java.util.Arrays.asList("*-endpoint.yml", "*-endpoints.yml", "*-api.yml");
            }
        }
        config.setEndpointPatterns(endpointPatterns);
    }

    private void loadValidationConfig() {
        // Load validation configuration
        Boolean runOnStartup = getBoolean("validation.runOnStartup", false);
        Boolean validateOnly = getBoolean("validation.validateOnly", false);
        Boolean validateEndpoints = getBoolean("validation.validateEndpoints", true);

        validation.setRunOnStartup(runOnStartup);
        validation.setValidateOnly(validateOnly);
        validation.setValidateEndpoints(validateEndpoints);

        logger.info("Validation configuration: runOnStartup={}, validateOnly={}, validateEndpoints={}",
                   runOnStartup, validateOnly, validateEndpoints);
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

    @SuppressWarnings("unchecked")
    protected java.util.List<String> getStringList(String path) {
        try {
            Object value = getNestedValue(path, Object.class, null);
            if (value instanceof java.util.List) {
                return (java.util.List<String>) value;
            }
            return null;
        } catch (Exception e) {
            logger.debug("Failed to get string list for path: {}", path, e);
            return null;
        }
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

    public boolean isDatabaseCreateIfMissing() {
        return database.createIfMissing;
    }
    
    public boolean isSwaggerEnabled() {
        return swagger.enabled;
    }
    
    public String getSwaggerPath() {
        return swagger.path;
    }

    // Configuration directory and pattern getters
    public java.util.List<String> getConfigDirectories() {
        return config.getDirectories();
    }

    public java.util.List<String> getDatabasePatterns() {
        return config.getDatabasePatterns();
    }

    public java.util.List<String> getQueryPatterns() {
        return config.getQueryPatterns();
    }

    public java.util.List<String> getEndpointPatterns() {
        return config.getEndpointPatterns();
    }

    public String getConfigSource() {
        return config.source;
    }

    public boolean isLoadConfigFromYaml() {
        return config.loadFromYaml;
    }

    public ValidationSettings getValidationSettings() {
        return validation;
    }

    public boolean isValidationRunOnStartup() {
        return validation.runOnStartup;
    }

    public boolean isValidationValidateOnly() {
        return validation.validateOnly;
    }

    public boolean isValidationValidateEndpoints() {
        return validation.validateEndpoints;
    }

    // Inner classes for configuration structure
    public static class DatabaseSettings {
        private String url = "jdbc:h2:./data/api-service-config;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";
        private String username = "sa";
        private String password = "";
        private String driver = "org.h2.Driver";
        private boolean createIfMissing = true;

        // Getters and setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getDriver() { return driver; }
        public void setDriver(String driver) { this.driver = driver; }
        public boolean isCreateIfMissing() { return createIfMissing; }
        public void setCreateIfMissing(boolean createIfMissing) { this.createIfMissing = createIfMissing; }
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
        private String source = "yaml";
        private boolean loadFromYaml = false;
        private java.util.List<String> directories = java.util.Arrays.asList("generic-config", "../generic-config");
        private java.util.List<String> databasePatterns = java.util.Arrays.asList("*-database.yml", "*-databases.yml");
        private java.util.List<String> queryPatterns = java.util.Arrays.asList("*-query.yml", "*-queries.yml");
        private java.util.List<String> endpointPatterns = java.util.Arrays.asList("*-endpoint.yml", "*-endpoints.yml", "*-api.yml");

        // Getters and setters
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public boolean isLoadFromYaml() { return loadFromYaml; }
        public void setLoadFromYaml(boolean loadFromYaml) { this.loadFromYaml = loadFromYaml; }

        public java.util.List<String> getDirectories() { return directories; }
        public void setDirectories(java.util.List<String> directories) { this.directories = directories; }

        public java.util.List<String> getDatabasePatterns() { return databasePatterns; }
        public void setDatabasePatterns(java.util.List<String> databasePatterns) { this.databasePatterns = databasePatterns; }

        public java.util.List<String> getQueryPatterns() { return queryPatterns; }
        public void setQueryPatterns(java.util.List<String> queryPatterns) { this.queryPatterns = queryPatterns; }

        public java.util.List<String> getEndpointPatterns() { return endpointPatterns; }
        public void setEndpointPatterns(java.util.List<String> endpointPatterns) { this.endpointPatterns = endpointPatterns; }
    }

    public static class ValidationSettings {
        private boolean runOnStartup = false;
        private boolean validateOnly = false;
        private boolean validateEndpoints = true;

        // Getters and setters
        public boolean isRunOnStartup() { return runOnStartup; }
        public void setRunOnStartup(boolean runOnStartup) { this.runOnStartup = runOnStartup; }
        public boolean isValidateOnly() { return validateOnly; }
        public void setValidateOnly(boolean validateOnly) { this.validateOnly = validateOnly; }
        public boolean isValidateEndpoints() { return validateEndpoints; }
        public void setValidateEndpoints(boolean validateEndpoints) { this.validateEndpoints = validateEndpoints; }
    }
}

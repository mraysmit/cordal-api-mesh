package dev.cordal.common.config;

/**
 * Configuration model for database connections
 * Common configuration class used across all modules
 */
public class DatabaseConfig {
    private String name;
    private String description;
    private String url;
    private String username;
    private String password;
    private String driver;
    private PoolConfig pool;

    // Default constructor
    public DatabaseConfig() {}

    // Constructor with all fields
    public DatabaseConfig(String name, String description, String url, String username, 
                         String password, String driver, PoolConfig pool) {
        this.name = name;
        this.description = description;
        this.url = url;
        this.username = username;
        this.password = password;
        this.driver = driver;
        this.pool = pool;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public PoolConfig getPool() {
        return pool;
    }

    public void setPool(PoolConfig pool) {
        this.pool = pool;
    }

    @Override
    public String toString() {
        return "DatabaseConfig{" +
               "name='" + name + '\'' +
               ", description='" + description + '\'' +
               ", url='" + url + '\'' +
               ", username='" + username + '\'' +
               ", driver='" + driver + '\'' +
               ", pool=" + pool +
               '}';
    }
}

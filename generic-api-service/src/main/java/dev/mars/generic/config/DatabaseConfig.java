package dev.mars.generic.config;

import java.util.Objects;

/**
 * Configuration model for database connections
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatabaseConfig that = (DatabaseConfig) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(description, that.description) &&
               Objects.equals(url, that.url) &&
               Objects.equals(username, that.username) &&
               Objects.equals(password, that.password) &&
               Objects.equals(driver, that.driver) &&
               Objects.equals(pool, that.pool);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, url, username, password, driver, pool);
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

    /**
     * Database connection pool configuration
     */
    public static class PoolConfig {
        private int maximumPoolSize = 10;
        private int minimumIdle = 2;
        private long connectionTimeout = 30000;
        private long idleTimeout = 600000;
        private long maxLifetime = 1800000;
        private long leakDetectionThreshold = 60000;
        private String connectionTestQuery = "SELECT 1";

        // Default constructor
        public PoolConfig() {}

        // Constructor with all fields
        public PoolConfig(int maximumPoolSize, int minimumIdle, long connectionTimeout,
                         long idleTimeout, long maxLifetime, long leakDetectionThreshold,
                         String connectionTestQuery) {
            this.maximumPoolSize = maximumPoolSize;
            this.minimumIdle = minimumIdle;
            this.connectionTimeout = connectionTimeout;
            this.idleTimeout = idleTimeout;
            this.maxLifetime = maxLifetime;
            this.leakDetectionThreshold = leakDetectionThreshold;
            this.connectionTestQuery = connectionTestQuery;
        }

        // Getters and Setters
        public int getMaximumPoolSize() {
            return maximumPoolSize;
        }

        public void setMaximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }

        public int getMinimumIdle() {
            return minimumIdle;
        }

        public void setMinimumIdle(int minimumIdle) {
            this.minimumIdle = minimumIdle;
        }

        public long getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        public long getIdleTimeout() {
            return idleTimeout;
        }

        public void setIdleTimeout(long idleTimeout) {
            this.idleTimeout = idleTimeout;
        }

        public long getMaxLifetime() {
            return maxLifetime;
        }

        public void setMaxLifetime(long maxLifetime) {
            this.maxLifetime = maxLifetime;
        }

        public long getLeakDetectionThreshold() {
            return leakDetectionThreshold;
        }

        public void setLeakDetectionThreshold(long leakDetectionThreshold) {
            this.leakDetectionThreshold = leakDetectionThreshold;
        }

        public String getConnectionTestQuery() {
            return connectionTestQuery;
        }

        public void setConnectionTestQuery(String connectionTestQuery) {
            this.connectionTestQuery = connectionTestQuery;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PoolConfig that = (PoolConfig) o;
            return maximumPoolSize == that.maximumPoolSize &&
                   minimumIdle == that.minimumIdle &&
                   connectionTimeout == that.connectionTimeout &&
                   idleTimeout == that.idleTimeout &&
                   maxLifetime == that.maxLifetime &&
                   leakDetectionThreshold == that.leakDetectionThreshold &&
                   Objects.equals(connectionTestQuery, that.connectionTestQuery);
        }

        @Override
        public int hashCode() {
            return Objects.hash(maximumPoolSize, minimumIdle, connectionTimeout, 
                              idleTimeout, maxLifetime, leakDetectionThreshold, connectionTestQuery);
        }

        @Override
        public String toString() {
            return "PoolConfig{" +
                   "maximumPoolSize=" + maximumPoolSize +
                   ", minimumIdle=" + minimumIdle +
                   ", connectionTimeout=" + connectionTimeout +
                   ", idleTimeout=" + idleTimeout +
                   ", maxLifetime=" + maxLifetime +
                   ", leakDetectionThreshold=" + leakDetectionThreshold +
                   ", connectionTestQuery='" + connectionTestQuery + '\'' +
                   '}';
        }
    }
}

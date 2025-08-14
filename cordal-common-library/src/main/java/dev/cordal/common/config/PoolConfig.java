package dev.cordal.common.config;

/**
 * Configuration model for database connection pool settings
 * Common pool configuration used across all modules
 */
public class PoolConfig {
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

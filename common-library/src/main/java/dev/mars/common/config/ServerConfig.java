package dev.mars.common.config;

/**
 * Common server configuration class
 * Used across all modules for consistent server settings
 */
public class ServerConfig {
    private String host = "localhost";
    private int port = 8080;
    private boolean enableCors = true;
    private boolean enableDevLogging = true;
    private boolean enableRequestLogging = true;

    // Default constructor
    public ServerConfig() {}

    // Constructor with basic settings
    public ServerConfig(String host, int port) {
        this.host = host;
        this.port = port;
    }

    // Constructor with all settings
    public ServerConfig(String host, int port, boolean enableCors, boolean enableDevLogging, boolean enableRequestLogging) {
        this.host = host;
        this.port = port;
        this.enableCors = enableCors;
        this.enableDevLogging = enableDevLogging;
        this.enableRequestLogging = enableRequestLogging;
    }

    // Getters and setters
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isEnableCors() {
        return enableCors;
    }

    public void setEnableCors(boolean enableCors) {
        this.enableCors = enableCors;
    }

    public boolean isEnableDevLogging() {
        return enableDevLogging;
    }

    public void setEnableDevLogging(boolean enableDevLogging) {
        this.enableDevLogging = enableDevLogging;
    }

    public boolean isEnableRequestLogging() {
        return enableRequestLogging;
    }

    public void setEnableRequestLogging(boolean enableRequestLogging) {
        this.enableRequestLogging = enableRequestLogging;
    }

    @Override
    public String toString() {
        return "ServerConfig{" +
               "host='" + host + '\'' +
               ", port=" + port +
               ", enableCors=" + enableCors +
               ", enableDevLogging=" + enableDevLogging +
               ", enableRequestLogging=" + enableRequestLogging +
               '}';
    }
}

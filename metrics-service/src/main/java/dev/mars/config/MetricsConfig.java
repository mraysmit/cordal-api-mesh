package dev.mars.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Configuration class for Metrics Service
 */
public class MetricsConfig {
    private static final Logger logger = LoggerFactory.getLogger(MetricsConfig.class);

    private ServerConfig server = new ServerConfig();
    private MetricsDatabaseSettings metricsDatabase = new MetricsDatabaseSettings();
    private MetricsCollectionSettings metricsCollection = new MetricsCollectionSettings();
    private MetricsDashboardSettings metricsDashboard = new MetricsDashboardSettings();
    
    public MetricsConfig() {
        // Default constructor - configuration will be loaded by static method
    }

    public static MetricsConfig loadFromFile() {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

            // Check for test configuration first
            String configFile = System.getProperty("config.file", "application.yml");
            logger.info("Loading metrics configuration from: {}", configFile);

            InputStream inputStream = MetricsConfig.class.getClassLoader().getResourceAsStream(configFile);

            if (inputStream != null) {
                // Read as Map first to avoid recursive constructor calls
                java.util.Map<String, Object> configMap = mapper.readValue(inputStream, java.util.Map.class);
                MetricsConfig config = new MetricsConfig();

                // Manually populate the configuration
                if (configMap.containsKey("server")) {
                    java.util.Map<String, Object> serverMap = (java.util.Map<String, Object>) configMap.get("server");
                    if (serverMap.containsKey("host")) {
                        config.server.setHost((String) serverMap.get("host"));
                    }
                    if (serverMap.containsKey("port")) {
                        config.server.setPort((Integer) serverMap.get("port"));
                    }
                }

                if (configMap.containsKey("metricsDatabase")) {
                    java.util.Map<String, Object> dbMap = (java.util.Map<String, Object>) configMap.get("metricsDatabase");
                    if (dbMap.containsKey("url")) {
                        config.metricsDatabase.setUrl((String) dbMap.get("url"));
                    }
                    if (dbMap.containsKey("username")) {
                        config.metricsDatabase.setUsername((String) dbMap.get("username"));
                    }
                    if (dbMap.containsKey("password")) {
                        config.metricsDatabase.setPassword((String) dbMap.get("password"));
                    }
                    if (dbMap.containsKey("driver")) {
                        config.metricsDatabase.setDriver((String) dbMap.get("driver"));
                    }
                }

                if (configMap.containsKey("metricsCollection")) {
                    java.util.Map<String, Object> collectionMap = (java.util.Map<String, Object>) configMap.get("metricsCollection");
                    if (collectionMap.containsKey("enabled")) {
                        config.metricsCollection.setEnabled((Boolean) collectionMap.get("enabled"));
                    }
                    if (collectionMap.containsKey("asyncSave")) {
                        config.metricsCollection.setAsyncSave((Boolean) collectionMap.get("asyncSave"));
                    }
                    if (collectionMap.containsKey("samplingRate")) {
                        Object rate = collectionMap.get("samplingRate");
                        if (rate instanceof Double) {
                            config.metricsCollection.setSamplingRate((Double) rate);
                        } else if (rate instanceof Integer) {
                            config.metricsCollection.setSamplingRate(((Integer) rate).doubleValue());
                        }
                    }
                }

                logger.info("Metrics configuration loaded successfully from {}", configFile);
                return config;
            } else {
                logger.warn("{} not found, using default configuration", configFile);
                return new MetricsConfig();
            }
        } catch (Exception e) {
            logger.error("Failed to load configuration, using defaults", e);
            return new MetricsConfig();
        }
    }
    
    // Getters
    public String getServerHost() {
        return server.host;
    }
    
    public int getServerPort() {
        return server.port;
    }
    
    public MetricsDatabaseSettings getMetricsDatabase() {
        return metricsDatabase;
    }
    
    public MetricsCollectionSettings getMetricsCollection() {
        return metricsCollection;
    }
    
    public MetricsDashboardSettings getMetricsDashboard() {
        return metricsDashboard;
    }
    
    // Inner classes for configuration structure
    public static class ServerConfig {
        private String host = "localhost";
        private int port = 8081; // Different port for metrics service
        
        // Getters and setters
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
    }
    
    public static class MetricsDatabaseSettings {
        private String url = "jdbc:h2:./data/metrics;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";
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
    
    public static class MetricsCollectionSettings {
        private boolean enabled = true;
        private boolean asyncSave = true;
        private double samplingRate = 1.0;
        private List<String> excludePaths = Arrays.asList("/dashboard", "/metrics", "/api/performance-metrics");

        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isAsyncSave() { return asyncSave; }
        public void setAsyncSave(boolean asyncSave) { this.asyncSave = asyncSave; }
        public double getSamplingRate() { return samplingRate; }
        public void setSamplingRate(double samplingRate) { this.samplingRate = samplingRate; }
        public List<String> getExcludePaths() { return excludePaths; }
        public void setExcludePaths(List<String> excludePaths) { this.excludePaths = excludePaths; }
    }
    
    public static class MetricsDashboardSettings {
        private CustomDashboard custom = new CustomDashboard();
        private GrafanaSettings grafana = new GrafanaSettings();
        
        public CustomDashboard getCustom() { return custom; }
        public void setCustom(CustomDashboard custom) { this.custom = custom; }
        public GrafanaSettings getGrafana() { return grafana; }
        public void setGrafana(GrafanaSettings grafana) { this.grafana = grafana; }
        
        public static class CustomDashboard {
            private boolean enabled = true;
            private String path = "/dashboard";
            
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            public String getPath() { return path; }
            public void setPath(String path) { this.path = path; }
        }
        
        public static class GrafanaSettings {
            private boolean enabled = false;
            private String url = "http://localhost:3000";
            private PrometheusSettings prometheus = new PrometheusSettings();
            
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            public String getUrl() { return url; }
            public void setUrl(String url) { this.url = url; }
            public PrometheusSettings getPrometheus() { return prometheus; }
            public void setPrometheus(PrometheusSettings prometheus) { this.prometheus = prometheus; }
            
            public static class PrometheusSettings {
                private boolean enabled = false;
                private String path = "/metrics";
                
                public boolean isEnabled() { return enabled; }
                public void setEnabled(boolean enabled) { this.enabled = enabled; }
                public String getPath() { return path; }
                public void setPath(String path) { this.path = path; }
            }
        }
    }
}

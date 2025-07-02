package dev.mars.config;

import dev.mars.common.config.BaseConfig;
import dev.mars.common.config.ServerConfig;
import dev.mars.common.metrics.MetricsCollectionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration class for Metrics Service
 * Extends BaseConfig and implements MetricsCollectionConfig
 */
public class MetricsConfig extends BaseConfig implements MetricsCollectionConfig {
    private static final Logger logger = LoggerFactory.getLogger(MetricsConfig.class);

    private ServerConfig server;
    private MetricsDatabaseSettings metricsDatabase;
    private MetricsCollectionSettings metricsCollection;
    private MetricsDashboardSettings metricsDashboard;

    public MetricsConfig() {
        super();
        initializeFromConfig();
    }

    private void initializeFromConfig() {
        // Initialize server configuration from loaded config
        String host = getString("server.host", "localhost");
        Integer port = getInteger("server.port", 8081);
        server = new ServerConfig(host, port);

        // Initialize other configurations
        metricsDatabase = new MetricsDatabaseSettings();
        metricsCollection = new MetricsCollectionSettings();
        metricsDashboard = new MetricsDashboardSettings();

        // Load configuration values into the settings objects
        loadServerConfig();
        loadMetricsDatabaseConfig();
        loadMetricsCollectionConfig();
        loadMetricsDashboardConfig();
    }

    @Override
    protected String getConfigFileName() {
        // Check for custom config file system property (for testing)
        String configFile = System.getProperty("metrics.config.file", "application.yml");
        logger.info("MetricsConfig - Using config file: {} (from system property: {})",
                   configFile, System.getProperty("metrics.config.file"));
        return configFile;
    }

    public static MetricsConfig loadFromFile() {
        logger.info("Creating MetricsConfig instance");
        return new MetricsConfig();
    }

    private void loadServerConfig() {
        // Server config is already loaded in initializeFromConfig()
    }

    private void loadMetricsDatabaseConfig() {
        String url = getString("metricsDatabase.url", "jdbc:h2:./data/metrics;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1");
        String username = getString("metricsDatabase.username", "sa");
        String password = getString("metricsDatabase.password", "");
        String driver = getString("metricsDatabase.driver", "org.h2.Driver");

        metricsDatabase.setUrl(url);
        metricsDatabase.setUsername(username);
        metricsDatabase.setPassword(password);
        metricsDatabase.setDriver(driver);
    }

    private void loadMetricsCollectionConfig() {
        Boolean enabled = getBoolean("metricsCollection.enabled", true);
        Boolean asyncSave = getBoolean("metricsCollection.asyncSave", true);
        Double samplingRate = getDouble("metricsCollection.samplingRate", 1.0);

        metricsCollection.setEnabled(enabled);
        metricsCollection.setAsyncSave(asyncSave);
        metricsCollection.setSamplingRate(samplingRate);
    }

    private void loadMetricsDashboardConfig() {
        Boolean customEnabled = getBoolean("metricsDashboard.custom.enabled", true);
        String customPath = getString("metricsDashboard.custom.path", "/dashboard");
        Boolean grafanaEnabled = getBoolean("metricsDashboard.grafana.enabled", false);

        metricsCollection.setEnabled(customEnabled);
        metricsDashboard.getCustom().setEnabled(customEnabled);
        metricsDashboard.getCustom().setPath(customPath);
        metricsDashboard.getGrafana().setEnabled(grafanaEnabled);
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

    public MetricsDatabaseSettings getMetricsDatabase() {
        return metricsDatabase;
    }

    public MetricsCollectionSettings getMetricsCollection() {
        return metricsCollection;
    }

    public MetricsDashboardSettings getMetricsDashboard() {
        return metricsDashboard;
    }

    // MetricsCollectionConfig interface implementation
    @Override
    public boolean isEnabled() {
        return metricsCollection.isEnabled();
    }

    @Override
    public boolean isAsyncSave() {
        return metricsCollection.isAsyncSave();
    }

    @Override
    public double getSamplingRate() {
        return metricsCollection.getSamplingRate();
    }

    @Override
    public List<String> getExcludePaths() {
        return metricsCollection.getExcludePaths();
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

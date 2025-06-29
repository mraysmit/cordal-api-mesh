package dev.mars.config;

import org.yaml.snakeyaml.Yaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Application configuration class that loads settings from YAML
 */
public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    
    private final Map<String, Object> config;
    
    public AppConfig() {
        this.config = loadConfiguration();
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadConfiguration() {
        // Check for custom config file system property
        String configFile = System.getProperty("config.file", "application.yml");
        logger.info("Loading configuration from: {}", configFile);

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configFile)) {
            if (inputStream == null) {
                throw new RuntimeException(configFile + " not found in classpath");
            }

            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(inputStream);
            logger.info("Configuration loaded successfully from: {}", configFile);
            return config;
        } catch (Exception e) {
            logger.error("Failed to load configuration from: {}", configFile, e);
            throw new RuntimeException("Failed to load configuration from: " + configFile, e);
        }
    }
    
    // Server configuration
    public int getServerPort() {
        return getNestedValue("server.port", Integer.class, 8080);
    }
    
    public String getServerHost() {
        return getNestedValue("server.host", String.class, "localhost");
    }
    
    // Database configuration
    public String getDatabaseUrl() {
        return getNestedValue("database.url", String.class, "jdbc:h2:./data/stocktrades");
    }
    
    public String getDatabaseUsername() {
        return getNestedValue("database.username", String.class, "sa");
    }
    
    public String getDatabasePassword() {
        return getNestedValue("database.password", String.class, "");
    }
    
    public String getDatabaseDriver() {
        return getNestedValue("database.driver", String.class, "org.h2.Driver");
    }
    
    // Connection pool configuration
    public int getMaximumPoolSize() {
        return getNestedValue("database.pool.maximumPoolSize", Integer.class, 10);
    }
    
    public int getMinimumIdle() {
        return getNestedValue("database.pool.minimumIdle", Integer.class, 2);
    }
    
    public long getConnectionTimeout() {
        return getNestedValue("database.pool.connectionTimeout", Long.class, 30000L);
    }
    
    public long getIdleTimeout() {
        return getNestedValue("database.pool.idleTimeout", Long.class, 600000L);
    }
    
    public long getMaxLifetime() {
        return getNestedValue("database.pool.maxLifetime", Long.class, 1800000L);
    }
    
    // Data configuration
    public boolean shouldLoadSampleData() {
        return getNestedValue("data.loadSampleData", Boolean.class, false);
    }
    
    public int getSampleDataSize() {
        return getNestedValue("data.sampleDataSize", Integer.class, 100);
    }

    // Metrics database configuration
    public MetricsDatabase getMetricsDatabase() {
        return new MetricsDatabase();
    }

    // Metrics collection configuration
    public MetricsCollection getMetricsCollection() {
        return new MetricsCollection();
    }

    // Metrics dashboard configuration
    public MetricsDashboard getMetricsDashboard() {
        return new MetricsDashboard();
    }

    // Nested configuration classes
    public class MetricsDatabase {
        public String getUrl() {
            return getNestedValue("metrics.database.url", String.class, "jdbc:h2:./data/metrics");
        }

        public String getUsername() {
            return getNestedValue("metrics.database.username", String.class, "sa");
        }

        public String getPassword() {
            return getNestedValue("metrics.database.password", String.class, "");
        }

        public String getDriver() {
            return getNestedValue("metrics.database.driver", String.class, "org.h2.Driver");
        }

        public PoolConfig getPool() {
            return new PoolConfig("metrics.database.pool");
        }
    }

    public class MetricsCollection {
        public boolean isEnabled() {
            return getNestedValue("metrics.collection.enabled", Boolean.class, true);
        }

        public boolean isIncludeMemoryMetrics() {
            return getNestedValue("metrics.collection.includeMemoryMetrics", Boolean.class, true);
        }

        @SuppressWarnings("unchecked")
        public List<String> getExcludePaths() {
            return getNestedValue("metrics.collection.excludePaths", List.class,
                List.of("/dashboard", "/metrics", "/api/performance-metrics"));
        }

        public double getSamplingRate() {
            return getNestedValue("metrics.collection.samplingRate", Double.class, 1.0);
        }

        public boolean isAsyncSave() {
            return getNestedValue("metrics.collection.asyncSave", Boolean.class, true);
        }
    }

    public class MetricsDashboard {
        public CustomDashboard getCustom() {
            return new CustomDashboard();
        }

        public GrafanaDashboard getGrafana() {
            return new GrafanaDashboard();
        }
    }

    public class CustomDashboard {
        public boolean isEnabled() {
            return getNestedValue("metrics.dashboard.custom.enabled", Boolean.class, true);
        }

        public String getPath() {
            return getNestedValue("metrics.dashboard.custom.path", String.class, "/dashboard");
        }
    }

    public class GrafanaDashboard {
        public boolean isEnabled() {
            return getNestedValue("metrics.dashboard.grafana.enabled", Boolean.class, false);
        }

        public String getUrl() {
            return getNestedValue("metrics.dashboard.grafana.url", String.class, "http://localhost:3000");
        }

        public PrometheusConfig getPrometheus() {
            return new PrometheusConfig();
        }
    }

    public class PrometheusConfig {
        public boolean isEnabled() {
            return getNestedValue("metrics.dashboard.grafana.prometheus.enabled", Boolean.class, false);
        }

        public int getPort() {
            return getNestedValue("metrics.dashboard.grafana.prometheus.port", Integer.class, 9090);
        }

        public String getPath() {
            return getNestedValue("metrics.dashboard.grafana.prometheus.path", String.class, "/metrics");
        }
    }

    public class PoolConfig {
        private final String prefix;

        public PoolConfig(String prefix) {
            this.prefix = prefix;
        }

        public int getMaximumPoolSize() {
            return getNestedValue(prefix + ".maximumPoolSize", Integer.class, 10);
        }

        public int getMinimumIdle() {
            return getNestedValue(prefix + ".minimumIdle", Integer.class, 2);
        }

        public long getConnectionTimeout() {
            return getNestedValue(prefix + ".connectionTimeout", Long.class, 30000L);
        }

        public long getIdleTimeout() {
            return getNestedValue(prefix + ".idleTimeout", Long.class, 600000L);
        }

        public long getMaxLifetime() {
            return getNestedValue(prefix + ".maxLifetime", Long.class, 1800000L);
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> T getNestedValue(String path, Class<T> type, T defaultValue) {
        try {
            String[] keys = path.split("\\.");
            Object current = config;
            
            for (String key : keys) {
                if (current instanceof Map) {
                    current = ((Map<String, Object>) current).get(key);
                } else {
                    return defaultValue;
                }
            }
            
            if (current != null) {
                // Handle type conversion for numeric types
                if (type == Long.class && current instanceof Number) {
                    return type.cast(((Number) current).longValue());
                } else if (type == Integer.class && current instanceof Number) {
                    return type.cast(((Number) current).intValue());
                } else if (type.isInstance(current)) {
                    return type.cast(current);
                }
            }

            return defaultValue;
        } catch (Exception e) {
            logger.warn("Failed to get configuration value for path: {}, using default: {}", path, defaultValue);
            return defaultValue;
        }
    }
}

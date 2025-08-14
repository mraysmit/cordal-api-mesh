package dev.cordal.generic.management;

import dev.cordal.generic.config.EndpointConfigurationManager;
import dev.cordal.generic.database.DatabaseConnectionManager;
import dev.cordal.generic.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service to monitor health of database connections and system components
 */
@Singleton
public class HealthMonitoringService {
    private static final Logger logger = LoggerFactory.getLogger(HealthMonitoringService.class);
    
    private final DatabaseConnectionManager databaseConnectionManager;
    private final EndpointConfigurationManager configurationManager;
    private final Map<String, DatabaseHealthStatus> databaseHealthCache;
    private final long HEALTH_CHECK_TIMEOUT_MS = 5000; // 5 seconds
    
    @Inject
    public HealthMonitoringService(DatabaseConnectionManager databaseConnectionManager,
                                 EndpointConfigurationManager configurationManager) {
        this.databaseConnectionManager = databaseConnectionManager;
        this.configurationManager = configurationManager;
        this.databaseHealthCache = new ConcurrentHashMap<>();
        
        logger.info("Health monitoring service initialized");
    }
    
    /**
     * Get comprehensive health status
     */
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> health = new HashMap<>();
        
        health.put("timestamp", Instant.now());
        health.put("service", getServiceHealth());
        health.put("databases", getDatabasesHealth());
        health.put("configuration", getConfigurationHealth());
        health.put("overall", getOverallHealth());
        
        return health;
    }
    
    /**
     * Get service health
     */
    public Map<String, Object> getServiceHealth() {
        Map<String, Object> serviceHealth = new HashMap<>();
        
        serviceHealth.put("status", "UP");
        serviceHealth.put("uptime", getUptime());
        serviceHealth.put("memoryUsage", getMemoryUsage());
        serviceHealth.put("threadCount", Thread.activeCount());
        
        return serviceHealth;
    }
    
    /**
     * Get databases health status
     */
    public Map<String, Object> getDatabasesHealth() {
        Map<String, Object> databasesHealth = new HashMap<>();
        Map<String, DatabaseConfig> databases = configurationManager.getAllDatabaseConfigurations();
        
        for (String databaseName : databases.keySet()) {
            databasesHealth.put(databaseName, checkDatabaseHealth(databaseName));
        }
        
        return databasesHealth;
    }
    
    /**
     * Check individual database health
     */
    public DatabaseHealthStatus checkDatabaseHealth(String databaseName) {
        // Check cache first (with 30-second TTL)
        DatabaseHealthStatus cached = databaseHealthCache.get(databaseName);
        if (cached != null && cached.isValid(30000)) {
            return cached;
        }
        
        DatabaseHealthStatus status = performDatabaseHealthCheck(databaseName);
        databaseHealthCache.put(databaseName, status);
        
        return status;
    }
    
    private DatabaseHealthStatus performDatabaseHealthCheck(String databaseName) {
        Instant startTime = Instant.now();
        
        try {
            DataSource dataSource = databaseConnectionManager.getDataSource(databaseName);
            if (dataSource == null) {
                return new DatabaseHealthStatus(databaseName, "DOWN", 
                    "DataSource not found", startTime, 0);
            }
            
            // Test connection with timeout
            CompletableFuture<Boolean> connectionTest = CompletableFuture.supplyAsync(() -> {
                try (Connection connection = dataSource.getConnection()) {
                    // Simple validation query
                    return connection.isValid(3); // 3 second timeout
                } catch (SQLException e) {
                    logger.warn("Database health check failed for {}: {}", databaseName, e.getMessage());
                    return false;
                }
            });
            
            boolean isHealthy = connectionTest.get(HEALTH_CHECK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            long responseTime = java.time.Duration.between(startTime, Instant.now()).toMillis();
            
            if (isHealthy) {
                return new DatabaseHealthStatus(databaseName, "UP", 
                    "Connection successful", startTime, responseTime);
            } else {
                return new DatabaseHealthStatus(databaseName, "DOWN", 
                    "Connection validation failed", startTime, responseTime);
            }
            
        } catch (Exception e) {
            long responseTime = java.time.Duration.between(startTime, Instant.now()).toMillis();
            logger.error("Database health check error for {}: {}", databaseName, e.getMessage());
            return new DatabaseHealthStatus(databaseName, "DOWN", 
                "Health check error: " + e.getMessage(), startTime, responseTime);
        }
    }
    
    /**
     * Get configuration health
     */
    public Map<String, Object> getConfigurationHealth() {
        Map<String, Object> configHealth = new HashMap<>();
        
        try {
            Map<String, DatabaseConfig> databases = configurationManager.getAllDatabaseConfigurations();
            Map<String, ?> queries = configurationManager.getAllQueryConfigurations();
            Map<String, ?> endpoints = configurationManager.getAllEndpointConfigurations();
            
            configHealth.put("status", "UP");
            configHealth.put("databasesLoaded", databases.size());
            configHealth.put("queriesLoaded", queries.size());
            configHealth.put("endpointsLoaded", endpoints.size());
            configHealth.put("lastValidation", "SUCCESS");
            
        } catch (Exception e) {
            logger.error("Configuration health check failed", e);
            configHealth.put("status", "DOWN");
            configHealth.put("error", e.getMessage());
        }
        
        return configHealth;
    }
    
    /**
     * Get overall health status
     */
    public String getOverallHealth() {
        try {
            // Check if any critical components are down
            Map<String, Object> databasesHealth = getDatabasesHealth();
            Map<String, Object> configHealth = getConfigurationHealth();
            
            // If configuration is down, overall is down
            if ("DOWN".equals(configHealth.get("status"))) {
                return "DOWN";
            }
            
            // Check if any databases are down
            boolean anyDatabaseDown = databasesHealth.values().stream()
                .anyMatch(health -> {
                    if (health instanceof DatabaseHealthStatus) {
                        return "DOWN".equals(((DatabaseHealthStatus) health).getStatus());
                    }
                    return false;
                });
            
            if (anyDatabaseDown) {
                return "DEGRADED";
            }
            
            return "UP";
            
        } catch (Exception e) {
            logger.error("Overall health check failed", e);
            return "DOWN";
        }
    }
    
    /**
     * Get system uptime
     */
    private String getUptime() {
        long uptimeMs = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        return String.format("%dd %dh %dm %ds", 
            days, hours % 24, minutes % 60, seconds % 60);
    }
    
    /**
     * Get memory usage information
     */
    private Map<String, Object> getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        Map<String, Object> memory = new HashMap<>();
        memory.put("maxMemoryMB", maxMemory / (1024 * 1024));
        memory.put("totalMemoryMB", totalMemory / (1024 * 1024));
        memory.put("usedMemoryMB", usedMemory / (1024 * 1024));
        memory.put("freeMemoryMB", freeMemory / (1024 * 1024));
        memory.put("usagePercentage", Math.round((double) usedMemory / totalMemory * 100));

        return memory;
    }

    /**
     * Get deployment verification information
     */
    public Map<String, Object> getDeploymentInfo() {
        Map<String, Object> deploymentInfo = new HashMap<>();

        try {
            // JAR information
            String jarPath = getJarPath();
            deploymentInfo.put("jarPath", jarPath);
            deploymentInfo.put("jarType", determineJarType(jarPath));

            // Java runtime information
            deploymentInfo.put("javaVersion", System.getProperty("java.version"));
            deploymentInfo.put("javaVendor", System.getProperty("java.vendor"));
            deploymentInfo.put("javaHome", System.getProperty("java.home"));

            // System information
            deploymentInfo.put("osName", System.getProperty("os.name"));
            deploymentInfo.put("osVersion", System.getProperty("os.version"));
            deploymentInfo.put("osArch", System.getProperty("os.arch"));

            // Application information
            deploymentInfo.put("applicationName", "Generic API Service");
            deploymentInfo.put("version", getApplicationVersion());
            deploymentInfo.put("startTime", getStartTime());
            deploymentInfo.put("workingDirectory", System.getProperty("user.dir"));

            deploymentInfo.put("status", "DEPLOYED");

        } catch (Exception e) {
            logger.error("Error getting deployment info", e);
            deploymentInfo.put("status", "ERROR");
            deploymentInfo.put("error", e.getMessage());
        }

        return deploymentInfo;
    }

    /**
     * Get JAR information and dependencies
     */
    public Map<String, Object> getJarInfo() {
        Map<String, Object> jarInfo = new HashMap<>();

        try {
            String jarPath = getJarPath();
            jarInfo.put("jarPath", jarPath);
            jarInfo.put("jarType", determineJarType(jarPath));

            // Manifest information
            Package pkg = this.getClass().getPackage();
            jarInfo.put("implementationTitle", pkg.getImplementationTitle());
            jarInfo.put("implementationVersion", pkg.getImplementationVersion());
            jarInfo.put("implementationVendor", pkg.getImplementationVendor());

            // Classpath information
            jarInfo.put("classPath", System.getProperty("java.class.path"));
            jarInfo.put("libraryPath", System.getProperty("java.library.path"));

            // Module information
            jarInfo.put("modulePath", System.getProperty("jdk.module.path"));

        } catch (Exception e) {
            logger.error("Error getting JAR info", e);
            jarInfo.put("error", e.getMessage());
        }

        return jarInfo;
    }

    /**
     * Perform readiness check for deployment verification
     */
    public Map<String, Object> getReadinessCheck() {
        Map<String, Object> readiness = new HashMap<>();

        try {
            boolean isReady = true;
            Map<String, String> checks = new HashMap<>();

            // Check configuration loading
            try {
                Map<String, DatabaseConfig> databases = configurationManager.getAllDatabaseConfigurations();
                checks.put("configuration", databases.isEmpty() ? "NO_DATABASES" : "OK");
                if (databases.isEmpty()) isReady = false;
            } catch (Exception e) {
                checks.put("configuration", "FAILED: " + e.getMessage());
                isReady = false;
            }

            // Check database connectivity
            try {
                Map<String, Object> databasesHealth = getDatabasesHealth();
                boolean anyDown = databasesHealth.values().stream()
                    .anyMatch(health -> health instanceof DatabaseHealthStatus &&
                             "DOWN".equals(((DatabaseHealthStatus) health).getStatus()));
                checks.put("databases", anyDown ? "SOME_DOWN" : "OK");
                if (anyDown) isReady = false;
            } catch (Exception e) {
                checks.put("databases", "FAILED: " + e.getMessage());
                isReady = false;
            }

            // Check memory availability
            Map<String, Object> memory = getMemoryUsage();
            long memoryUsage = (Long) memory.get("usagePercentage");
            checks.put("memory", memoryUsage > 90 ? "HIGH_USAGE" : "OK");
            if (memoryUsage > 95) isReady = false;

            readiness.put("status", isReady ? "READY" : "NOT_READY");
            readiness.put("checks", checks);
            readiness.put("timestamp", Instant.now());

        } catch (Exception e) {
            logger.error("Error performing readiness check", e);
            readiness.put("status", "NOT_READY");
            readiness.put("error", e.getMessage());
        }

        return readiness;
    }

    /**
     * Perform liveness check for deployment verification
     */
    public Map<String, Object> getLivenessCheck() {
        Map<String, Object> liveness = new HashMap<>();

        try {
            boolean isAlive = true;
            Map<String, String> checks = new HashMap<>();

            // Check if application is responsive
            checks.put("application", "UP");

            // Check memory usage (critical threshold)
            Map<String, Object> memory = getMemoryUsage();
            long memoryUsage = (Long) memory.get("usagePercentage");
            checks.put("memory", memoryUsage > 95 ? "CRITICAL" : "OK");
            if (memoryUsage > 98) isAlive = false;

            // Check thread count
            int threadCount = Thread.activeCount();
            checks.put("threads", threadCount > 1000 ? "HIGH" : "OK");
            if (threadCount > 2000) isAlive = false;

            liveness.put("status", isAlive ? "UP" : "DOWN");
            liveness.put("checks", checks);
            liveness.put("timestamp", Instant.now());

        } catch (Exception e) {
            logger.error("Error performing liveness check", e);
            liveness.put("status", "DOWN");
            liveness.put("error", e.getMessage());
        }

        return liveness;
    }

    private String getJarPath() {
        try {
            return this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String determineJarType(String jarPath) {
        if (jarPath.contains("executable")) return "fat-jar";
        if (jarPath.contains("thin")) return "thin-jar";
        if (jarPath.contains("optimized")) return "optimized-jar";
        if (jarPath.contains("dev")) return "dev-jar";
        return "unknown";
    }

    private String getApplicationVersion() {
        Package pkg = this.getClass().getPackage();
        return pkg.getImplementationVersion() != null ? pkg.getImplementationVersion() : "1.0-SNAPSHOT";
    }

    private String getStartTime() {
        long startTime = java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime();
        return Instant.ofEpochMilli(startTime).toString();
    }
    
    /**
     * Database health status holder
     */
    public static class DatabaseHealthStatus {
        private final String databaseName;
        private final String status;
        private final String message;
        private final Instant checkTime;
        private final long responseTimeMs;
        
        public DatabaseHealthStatus(String databaseName, String status, String message, 
                                  Instant checkTime, long responseTimeMs) {
            this.databaseName = databaseName;
            this.status = status;
            this.message = message;
            this.checkTime = checkTime;
            this.responseTimeMs = responseTimeMs;
        }
        
        public boolean isValid(long ttlMs) {
            return java.time.Duration.between(checkTime, Instant.now()).toMillis() < ttlMs;
        }
        
        // Getters
        public String getDatabaseName() { return databaseName; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public Instant getCheckTime() { return checkTime; }
        public long getResponseTimeMs() { return responseTimeMs; }
    }
}

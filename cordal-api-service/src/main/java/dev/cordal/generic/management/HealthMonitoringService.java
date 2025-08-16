package dev.cordal.generic.management;

import dev.cordal.generic.config.EndpointConfigurationManager;
import dev.cordal.generic.database.DatabaseConnectionManager;
import dev.cordal.generic.config.DatabaseConfig;
import dev.cordal.generic.dto.*;
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
     * Get comprehensive health status with type-safe response
     */
    public HealthStatusResponse getHealthStatus() {
        ServiceHealthResponse service = getServiceHealth();
        Map<String, DatabaseHealthResponse> databases = getDatabasesHealth();
        ConfigurationHealthResponse configuration = getConfigurationHealth();
        String overall = getOverallHealth();

        return HealthStatusResponse.of(service, databases, configuration, overall);
    }

    /**
     * Get comprehensive health status (DEPRECATED - use type-safe version)
     * @deprecated Use getHealthStatus() for type safety
     */
    @Deprecated
    public Map<String, Object> getHealthStatusMap() {
        return getHealthStatus().toMap();
    }
    
    /**
     * Get service health with type-safe response
     */
    public ServiceHealthResponse getServiceHealth() {
        String uptime = getUptime();
        MemoryUsageResponse memoryUsage = getMemoryUsage();
        int threadCount = Thread.activeCount();

        return ServiceHealthResponse.up(uptime, memoryUsage, threadCount);
    }

    /**
     * Get service health (DEPRECATED - use type-safe version)
     * @deprecated Use getServiceHealth() for type safety
     */
    @Deprecated
    public Map<String, Object> getServiceHealthMap() {
        return getServiceHealth().toMap();
    }
    
    /**
     * Get databases health status with type-safe response
     */
    public Map<String, DatabaseHealthResponse> getDatabasesHealth() {
        Map<String, DatabaseHealthResponse> databasesHealth = new HashMap<>();
        Map<String, DatabaseConfig> databases = configurationManager.getAllDatabaseConfigurations();

        for (String databaseName : databases.keySet()) {
            DatabaseHealthStatus status = checkDatabaseHealth(databaseName);
            databasesHealth.put(databaseName, DatabaseHealthResponse.from(status));
        }

        return databasesHealth;
    }

    /**
     * Get databases health status (DEPRECATED - use type-safe version)
     * @deprecated Use getDatabasesHealth() for type safety
     */
    @Deprecated
    public Map<String, Object> getDatabasesHealthMap() {
        Map<String, DatabaseHealthResponse> healthResponses = getDatabasesHealth();
        Map<String, Object> map = new HashMap<>();
        healthResponses.forEach((name, health) -> map.put(name, health.toMap()));
        return map;
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
     * Get configuration health with type-safe response
     */
    public ConfigurationHealthResponse getConfigurationHealth() {
        try {
            Map<String, DatabaseConfig> databases = configurationManager.getAllDatabaseConfigurations();
            Map<String, ?> queries = configurationManager.getAllQueryConfigurations();
            Map<String, ?> endpoints = configurationManager.getAllEndpointConfigurations();

            return ConfigurationHealthResponse.up(
                databases.size(),
                queries.size(),
                endpoints.size()
            );

        } catch (Exception e) {
            logger.error("Configuration health check failed", e);
            return ConfigurationHealthResponse.down(e.getMessage());
        }
    }

    /**
     * Get configuration health (DEPRECATED - use type-safe version)
     * @deprecated Use getConfigurationHealth() for type safety
     */
    @Deprecated
    public Map<String, Object> getConfigurationHealthMap() {
        return getConfigurationHealth().toMap();
    }
    
    /**
     * Get overall health status
     */
    public String getOverallHealth() {
        try {
            // Check if any critical components are down
            Map<String, DatabaseHealthResponse> databasesHealth = getDatabasesHealth();
            ConfigurationHealthResponse configHealth = getConfigurationHealth();

            // If configuration is down, overall is down
            if (configHealth.isDown()) {
                return "DOWN";
            }

            // Check if any databases are down
            boolean anyDatabaseDown = databasesHealth.values().stream()
                .anyMatch(DatabaseHealthResponse::isDown);
            
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
     * Get memory usage information with type-safe response
     */
    public MemoryUsageResponse getMemoryUsage() {
        return MemoryUsageResponse.fromRuntime();
    }

    /**
     * Get memory usage information (DEPRECATED - use type-safe version)
     * @deprecated Use getMemoryUsage() for type safety
     */
    @Deprecated
    private Map<String, Object> getMemoryUsageMap() {
        return getMemoryUsage().toMap();
    }

    /**
     * Get deployment verification information with type-safe response
     */
    public DeploymentInfoResponse getDeploymentInfo() {
        try {
            String jarPath = getJarPath();
            String applicationVersion = getApplicationVersion();
            String buildTime = getStartTime(); // Using start time as build time for now
            String gitCommit = "unknown"; // Could be extracted from manifest if available

            return DeploymentInfoResponse.fromSystem(
                jarPath,
                "Generic API Service",
                applicationVersion,
                buildTime,
                gitCommit
            );

        } catch (Exception e) {
            logger.error("Error getting deployment info", e);
            // Return a minimal deployment info with error details
            return DeploymentInfoResponse.fromSystem(
                "unknown",
                "Generic API Service",
                "unknown",
                "unknown",
                "error: " + e.getMessage()
            );
        }
    }

    /**
     * Get deployment verification information (DEPRECATED - use type-safe version)
     * @deprecated Use getDeploymentInfo() for type safety
     */
    @Deprecated
    public Map<String, Object> getDeploymentInfoMap() {
        return getDeploymentInfo().toMap();
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
     * Perform readiness check for deployment verification with type-safe response
     */
    public ReadinessCheckResponse getReadinessCheck() {
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
                Map<String, DatabaseHealthResponse> databasesHealth = getDatabasesHealth();
                boolean anyDown = databasesHealth.values().stream()
                    .anyMatch(DatabaseHealthResponse::isDown);
                checks.put("databases", anyDown ? "SOME_DOWN" : "OK");
                if (anyDown) isReady = false;
            } catch (Exception e) {
                checks.put("databases", "FAILED: " + e.getMessage());
                isReady = false;
            }

            // Check memory availability
            MemoryUsageResponse memory = getMemoryUsage();
            long memoryUsage = memory.getUsagePercentage();
            checks.put("memory", memoryUsage > 90 ? "HIGH_USAGE" : "OK");
            if (memoryUsage > 95) isReady = false;

            return isReady ? ReadinessCheckResponse.up(checks, "Service is ready")
                          : ReadinessCheckResponse.down(checks, "Service is not ready");

        } catch (Exception e) {
            logger.error("Error performing readiness check", e);
            return ReadinessCheckResponse.down("Error performing readiness check: " + e.getMessage());
        }
    }

    /**
     * Perform readiness check for deployment verification (DEPRECATED - use type-safe version)
     * @deprecated Use getReadinessCheck() for type safety
     */
    @Deprecated
    public Map<String, Object> getReadinessCheckMap() {
        return getReadinessCheck().toMap();
    }

    /**
     * Perform liveness check for deployment verification with type-safe response
     */
    public ReadinessCheckResponse getLivenessCheck() {
        try {
            boolean isAlive = true;
            Map<String, String> checks = new HashMap<>();

            // Check if application is responsive
            checks.put("application", "UP");

            // Check memory usage (critical threshold)
            MemoryUsageResponse memory = getMemoryUsage();
            long memoryUsage = memory.getUsagePercentage();
            checks.put("memory", memoryUsage > 95 ? "CRITICAL" : "OK");
            if (memoryUsage > 98) isAlive = false;

            // Check thread count
            int threadCount = Thread.activeCount();
            checks.put("threads", threadCount > 1000 ? "HIGH" : "OK");
            if (threadCount > 2000) isAlive = false;

            return isAlive ? ReadinessCheckResponse.up(checks, "Service is alive")
                          : ReadinessCheckResponse.down(checks, "Service is not alive");

        } catch (Exception e) {
            logger.error("Error performing liveness check", e);
            return ReadinessCheckResponse.down("Error performing liveness check: " + e.getMessage());
        }
    }

    /**
     * Perform liveness check for deployment verification (DEPRECATED - use type-safe version)
     * @deprecated Use getLivenessCheck() for type safety
     */
    @Deprecated
    public Map<String, Object> getLivenessCheckMap() {
        return getLivenessCheck().toMap();
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

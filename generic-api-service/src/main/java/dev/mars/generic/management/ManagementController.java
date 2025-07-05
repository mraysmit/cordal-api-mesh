package dev.mars.generic.management;

import dev.mars.generic.GenericApiService;
import dev.mars.generic.config.EndpointConfigurationManager;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive management controller for configuration visibility, health monitoring, and usage statistics
 */
@Singleton
public class ManagementController {
    private static final Logger logger = LoggerFactory.getLogger(ManagementController.class);
    
    private final ConfigurationMetadataService metadataService;
    private final UsageStatisticsService statisticsService;
    private final HealthMonitoringService healthService;
    private final GenericApiService genericApiService;
    private final EndpointConfigurationManager configurationManager;
    
    @Inject
    public ManagementController(ConfigurationMetadataService metadataService,
                              UsageStatisticsService statisticsService,
                              HealthMonitoringService healthService,
                              GenericApiService genericApiService,
                              EndpointConfigurationManager configurationManager) {
        this.metadataService = metadataService;
        this.statisticsService = statisticsService;
        this.healthService = healthService;
        this.genericApiService = genericApiService;
        this.configurationManager = configurationManager;
        
        logger.info("Management controller initialized");
    }
    
    // ========== CONFIGURATION METADATA ENDPOINTS ==========
    
    /**
     * Get configuration metadata including paths and load times
     */
    public void getConfigurationMetadata(Context ctx) {
        logger.debug("Getting configuration metadata");
        try {
            Map<String, Object> metadata = metadataService.getConfigurationMetadata();
            ctx.json(metadata);
        } catch (Exception e) {
            logger.error("Error getting configuration metadata", e);
            ctx.status(500).json(Map.of("error", "Failed to get configuration metadata: " + e.getMessage()));
        }
    }
    
    /**
     * Get configuration file paths
     */
    public void getConfigurationPaths(Context ctx) {
        logger.debug("Getting configuration paths");
        try {
            Map<String, Object> paths = metadataService.getConfigurationPaths();
            ctx.json(paths);
        } catch (Exception e) {
            logger.error("Error getting configuration paths", e);
            ctx.status(500).json(Map.of("error", "Failed to get configuration paths: " + e.getMessage()));
        }
    }
    
    /**
     * Get configuration file contents
     */
    public void getConfigurationFileContents(Context ctx) {
        logger.debug("Getting configuration file contents");
        try {
            Map<String, Object> contents = metadataService.getConfigurationFileContents();
            ctx.json(contents);
        } catch (Exception e) {
            logger.error("Error getting configuration file contents", e);
            ctx.status(500).json(Map.of("error", "Failed to get configuration file contents: " + e.getMessage()));
        }
    }
    
    // ========== CONFIGURATION VIEW ENDPOINTS ==========
    
    /**
     * Get all configured endpoints with details
     */
    public void getConfiguredEndpoints(Context ctx) {
        logger.debug("Getting configured endpoints");
        try {
            var endpoints = configurationManager.getAllEndpointConfigurations();
            Map<String, Object> response = new HashMap<>();
            response.put("count", endpoints.size());
            response.put("endpoints", endpoints);
            ctx.json(response);
        } catch (Exception e) {
            logger.error("Error getting configured endpoints", e);
            ctx.status(500).json(Map.of("error", "Failed to get configured endpoints: " + e.getMessage()));
        }
    }
    
    /**
     * Get all configured queries with details
     */
    public void getConfiguredQueries(Context ctx) {
        logger.debug("Getting configured queries");
        try {
            var queries = configurationManager.getAllQueryConfigurations();
            Map<String, Object> response = new HashMap<>();
            response.put("count", queries.size());
            response.put("queries", queries);
            ctx.json(response);
        } catch (Exception e) {
            logger.error("Error getting configured queries", e);
            ctx.status(500).json(Map.of("error", "Failed to get configured queries: " + e.getMessage()));
        }
    }
    
    /**
     * Get all configured databases with details
     */
    public void getConfiguredDatabases(Context ctx) {
        logger.debug("Getting configured databases");
        try {
            var databases = configurationManager.getAllDatabaseConfigurations();
            Map<String, Object> response = new HashMap<>();
            response.put("count", databases.size());
            response.put("databases", databases);
            ctx.json(response);
        } catch (Exception e) {
            logger.error("Error getting configured databases", e);
            ctx.status(500).json(Map.of("error", "Failed to get configured databases: " + e.getMessage()));
        }
    }
    
    // ========== USAGE STATISTICS ENDPOINTS ==========
    
    /**
     * Get comprehensive usage statistics
     */
    public void getUsageStatistics(Context ctx) {
        logger.debug("Getting usage statistics");
        try {
            Map<String, Object> stats = statisticsService.getUsageStatistics();
            ctx.json(stats);
        } catch (Exception e) {
            logger.error("Error getting usage statistics", e);
            ctx.status(500).json(Map.of("error", "Failed to get usage statistics: " + e.getMessage()));
        }
    }
    
    /**
     * Get endpoint usage statistics
     */
    public void getEndpointStatistics(Context ctx) {
        logger.debug("Getting endpoint statistics");
        try {
            var stats = statisticsService.getEndpointStatistics();
            ctx.json(stats);
        } catch (Exception e) {
            logger.error("Error getting endpoint statistics", e);
            ctx.status(500).json(Map.of("error", "Failed to get endpoint statistics: " + e.getMessage()));
        }
    }
    
    /**
     * Get query usage statistics
     */
    public void getQueryStatistics(Context ctx) {
        logger.debug("Getting query statistics");
        try {
            var stats = statisticsService.getQueryStatistics();
            ctx.json(stats);
        } catch (Exception e) {
            logger.error("Error getting query statistics", e);
            ctx.status(500).json(Map.of("error", "Failed to get query statistics: " + e.getMessage()));
        }
    }
    
    /**
     * Get database usage statistics
     */
    public void getDatabaseStatistics(Context ctx) {
        logger.debug("Getting database statistics");
        try {
            var stats = statisticsService.getDatabaseStatistics();
            ctx.json(stats);
        } catch (Exception e) {
            logger.error("Error getting database statistics", e);
            ctx.status(500).json(Map.of("error", "Failed to get database statistics: " + e.getMessage()));
        }
    }
    
    // ========== HEALTH MONITORING ENDPOINTS ==========
    
    /**
     * Get comprehensive health status
     */
    public void getHealthStatus(Context ctx) {
        logger.debug("Getting health status");
        try {
            Map<String, Object> health = healthService.getHealthStatus();
            
            // Set HTTP status based on overall health
            String overallHealth = (String) health.get("overall");
            if ("DOWN".equals(overallHealth)) {
                ctx.status(503); // Service Unavailable
            } else if ("DEGRADED".equals(overallHealth)) {
                ctx.status(200); // OK but with warnings
            } else {
                ctx.status(200); // OK
            }
            
            ctx.json(health);
        } catch (Exception e) {
            logger.error("Error getting health status", e);
            ctx.status(500).json(Map.of("error", "Failed to get health status: " + e.getMessage()));
        }
    }
    
    /**
     * Get database health status
     */
    public void getDatabaseHealth(Context ctx) {
        logger.debug("Getting database health");
        try {
            Map<String, Object> databaseHealth = healthService.getDatabasesHealth();
            ctx.json(databaseHealth);
        } catch (Exception e) {
            logger.error("Error getting database health", e);
            ctx.status(500).json(Map.of("error", "Failed to get database health: " + e.getMessage()));
        }
    }
    
    /**
     * Get specific database health
     */
    public void getSpecificDatabaseHealth(Context ctx) {
        String databaseName = ctx.pathParam("databaseName");
        logger.debug("Getting health for database: {}", databaseName);
        
        try {
            var healthStatus = healthService.checkDatabaseHealth(databaseName);
            
            // Set HTTP status based on database health
            if ("DOWN".equals(healthStatus.getStatus())) {
                ctx.status(503); // Service Unavailable
            } else {
                ctx.status(200); // OK
            }
            
            ctx.json(healthStatus);
        } catch (Exception e) {
            logger.error("Error getting health for database {}", databaseName, e);
            ctx.status(500).json(Map.of("error", "Failed to get database health: " + e.getMessage()));
        }
    }
    
    // ========== COMPREHENSIVE DASHBOARD ENDPOINT ==========
    
    /**
     * Get comprehensive management dashboard data
     */
    public void getManagementDashboard(Context ctx) {
        logger.debug("Getting management dashboard data");
        try {
            Map<String, Object> dashboard = new HashMap<>();
            
            // Configuration overview
            dashboard.put("configuration", Map.of(
                "metadata", metadataService.getConfigurationMetadata(),
                "endpoints", Map.of("count", configurationManager.getAllEndpointConfigurations().size()),
                "queries", Map.of("count", configurationManager.getAllQueryConfigurations().size()),
                "databases", Map.of("count", configurationManager.getAllDatabaseConfigurations().size())
            ));
            
            // Usage statistics
            dashboard.put("usage", statisticsService.getUsageStatistics());
            
            // Health status
            dashboard.put("health", healthService.getHealthStatus());
            
            // System info
            dashboard.put("system", Map.of(
                "timestamp", java.time.Instant.now(),
                "version", "1.0.0", // Could be loaded from properties
                "environment", System.getProperty("environment", "development")
            ));
            
            ctx.json(dashboard);
        } catch (Exception e) {
            logger.error("Error getting management dashboard", e);
            ctx.status(500).json(Map.of("error", "Failed to get management dashboard: " + e.getMessage()));
        }
    }
}

package dev.mars.generic.management;

import dev.mars.generic.config.DatabaseConfig;
import dev.mars.generic.config.QueryConfig;
import dev.mars.generic.config.ApiEndpointConfig;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for configuration management operations
 * Provides CRUD endpoints for database, query, and endpoint configurations
 */
@Singleton
public class ConfigurationManagementController {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManagementController.class);

    private final ConfigurationManagementService configurationManagementService;

    @Inject
    public ConfigurationManagementController(ConfigurationManagementService configurationManagementService) {
        this.configurationManagementService = configurationManagementService;
        logger.info("Configuration management controller initialized");
    }

    // ========== DATABASE CONFIGURATION ENDPOINTS ==========

    /**
     * GET /api/management/config/databases - Get all database configurations
     */
    public void getAllDatabaseConfigurations(Context ctx) {
        logger.debug("Getting all database configurations");
        try {
            Map<String, Object> result = configurationManagementService.getAllDatabaseConfigurations();
            ctx.json(result);
        } catch (Exception e) {
            logger.error("Error getting all database configurations", e);
            ctx.status(500).json(Map.of("error", "Failed to get database configurations: " + e.getMessage()));
        }
    }

    /**
     * GET /api/management/config/databases/{name} - Get database configuration by name
     */
    public void getDatabaseConfiguration(Context ctx) {
        String name = ctx.pathParam("name");
        logger.debug("Getting database configuration: {}", name);
        
        try {
            Optional<DatabaseConfig> config = configurationManagementService.getDatabaseConfiguration(name);
            
            if (config.isPresent()) {
                ctx.json(Map.of(
                    "name", name,
                    "found", true,
                    "configuration", config.get()
                ));
            } else {
                ctx.status(404).json(Map.of(
                    "name", name,
                    "found", false,
                    "error", "Database configuration not found"
                ));
            }
        } catch (Exception e) {
            logger.error("Error getting database configuration: {}", name, e);
            ctx.status(500).json(Map.of("error", "Failed to get database configuration: " + e.getMessage()));
        }
    }

    /**
     * POST /api/management/config/databases/{name} - Create or update database configuration
     */
    public void saveDatabaseConfiguration(Context ctx) {
        String name = ctx.pathParam("name");
        logger.debug("Saving database configuration: {}", name);
        
        try {
            DatabaseConfig config = ctx.bodyAsClass(DatabaseConfig.class);
            Map<String, Object> result = configurationManagementService.saveDatabaseConfiguration(name, config);
            ctx.json(result);
        } catch (IllegalStateException e) {
            logger.warn("Configuration management not available: {}", e.getMessage());
            ctx.status(400).json(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error saving database configuration: {}", name, e);
            ctx.status(500).json(Map.of("error", "Failed to save database configuration: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/management/config/databases/{name} - Delete database configuration
     */
    public void deleteDatabaseConfiguration(Context ctx) {
        String name = ctx.pathParam("name");
        logger.debug("Deleting database configuration: {}", name);
        
        try {
            Map<String, Object> result = configurationManagementService.deleteDatabaseConfiguration(name);
            ctx.json(result);
        } catch (IllegalStateException e) {
            logger.warn("Configuration management not available: {}", e.getMessage());
            ctx.status(400).json(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error deleting database configuration: {}", name, e);
            ctx.status(500).json(Map.of("error", "Failed to delete database configuration: " + e.getMessage()));
        }
    }

    // ========== QUERY CONFIGURATION ENDPOINTS ==========

    /**
     * GET /api/management/config/queries - Get all query configurations
     */
    public void getAllQueryConfigurations(Context ctx) {
        logger.debug("Getting all query configurations");
        try {
            Map<String, Object> result = configurationManagementService.getAllQueryConfigurations();
            ctx.json(result);
        } catch (Exception e) {
            logger.error("Error getting all query configurations", e);
            ctx.status(500).json(Map.of("error", "Failed to get query configurations: " + e.getMessage()));
        }
    }

    /**
     * GET /api/management/config/queries/{name} - Get query configuration by name
     */
    public void getQueryConfiguration(Context ctx) {
        String name = ctx.pathParam("name");
        logger.debug("Getting query configuration: {}", name);
        
        try {
            Optional<QueryConfig> config = configurationManagementService.getQueryConfiguration(name);
            
            if (config.isPresent()) {
                ctx.json(Map.of(
                    "name", name,
                    "found", true,
                    "configuration", config.get()
                ));
            } else {
                ctx.status(404).json(Map.of(
                    "name", name,
                    "found", false,
                    "error", "Query configuration not found"
                ));
            }
        } catch (Exception e) {
            logger.error("Error getting query configuration: {}", name, e);
            ctx.status(500).json(Map.of("error", "Failed to get query configuration: " + e.getMessage()));
        }
    }

    /**
     * GET /api/management/config/queries/by-database/{databaseName} - Get queries by database
     */
    public void getQueryConfigurationsByDatabase(Context ctx) {
        String databaseName = ctx.pathParam("databaseName");
        logger.debug("Getting query configurations for database: {}", databaseName);
        
        try {
            Map<String, Object> result = configurationManagementService.getQueryConfigurationsByDatabase(databaseName);
            ctx.json(result);
        } catch (Exception e) {
            logger.error("Error getting query configurations for database: {}", databaseName, e);
            ctx.status(500).json(Map.of("error", "Failed to get query configurations: " + e.getMessage()));
        }
    }

    /**
     * POST /api/management/config/queries/{name} - Create or update query configuration
     */
    public void saveQueryConfiguration(Context ctx) {
        String name = ctx.pathParam("name");
        logger.debug("Saving query configuration: {}", name);
        
        try {
            QueryConfig config = ctx.bodyAsClass(QueryConfig.class);
            Map<String, Object> result = configurationManagementService.saveQueryConfiguration(name, config);
            ctx.json(result);
        } catch (IllegalStateException e) {
            logger.warn("Configuration management not available: {}", e.getMessage());
            ctx.status(400).json(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error saving query configuration: {}", name, e);
            ctx.status(500).json(Map.of("error", "Failed to save query configuration: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/management/config/queries/{name} - Delete query configuration
     */
    public void deleteQueryConfiguration(Context ctx) {
        String name = ctx.pathParam("name");
        logger.debug("Deleting query configuration: {}", name);
        
        try {
            Map<String, Object> result = configurationManagementService.deleteQueryConfiguration(name);
            ctx.json(result);
        } catch (IllegalStateException e) {
            logger.warn("Configuration management not available: {}", e.getMessage());
            ctx.status(400).json(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error deleting query configuration: {}", name, e);
            ctx.status(500).json(Map.of("error", "Failed to delete query configuration: " + e.getMessage()));
        }
    }

    // ========== ENDPOINT CONFIGURATION ENDPOINTS ==========

    /**
     * GET /api/management/config/endpoints - Get all endpoint configurations
     */
    public void getAllEndpointConfigurations(Context ctx) {
        logger.debug("Getting all endpoint configurations");
        try {
            Map<String, Object> result = configurationManagementService.getAllEndpointConfigurations();
            ctx.json(result);
        } catch (Exception e) {
            logger.error("Error getting all endpoint configurations", e);
            ctx.status(500).json(Map.of("error", "Failed to get endpoint configurations: " + e.getMessage()));
        }
    }

    /**
     * GET /api/management/config/endpoints/{name} - Get endpoint configuration by name
     */
    public void getEndpointConfiguration(Context ctx) {
        String name = ctx.pathParam("name");
        logger.debug("Getting endpoint configuration: {}", name);
        
        try {
            Optional<ApiEndpointConfig> config = configurationManagementService.getEndpointConfiguration(name);
            
            if (config.isPresent()) {
                ctx.json(Map.of(
                    "name", name,
                    "found", true,
                    "configuration", config.get()
                ));
            } else {
                ctx.status(404).json(Map.of(
                    "name", name,
                    "found", false,
                    "error", "Endpoint configuration not found"
                ));
            }
        } catch (Exception e) {
            logger.error("Error getting endpoint configuration: {}", name, e);
            ctx.status(500).json(Map.of("error", "Failed to get endpoint configuration: " + e.getMessage()));
        }
    }

    /**
     * GET /api/management/config/endpoints/by-query/{queryName} - Get endpoints by query
     */
    public void getEndpointConfigurationsByQuery(Context ctx) {
        String queryName = ctx.pathParam("queryName");
        logger.debug("Getting endpoint configurations for query: {}", queryName);
        
        try {
            Map<String, Object> result = configurationManagementService.getEndpointConfigurationsByQuery(queryName);
            ctx.json(result);
        } catch (Exception e) {
            logger.error("Error getting endpoint configurations for query: {}", queryName, e);
            ctx.status(500).json(Map.of("error", "Failed to get endpoint configurations: " + e.getMessage()));
        }
    }

    /**
     * POST /api/management/config/endpoints/{name} - Create or update endpoint configuration
     */
    public void saveEndpointConfiguration(Context ctx) {
        String name = ctx.pathParam("name");
        logger.debug("Saving endpoint configuration: {}", name);

        try {
            ApiEndpointConfig config = ctx.bodyAsClass(ApiEndpointConfig.class);
            Map<String, Object> result = configurationManagementService.saveEndpointConfiguration(name, config);
            ctx.json(result);
        } catch (IllegalStateException e) {
            logger.warn("Configuration management not available: {}", e.getMessage());
            ctx.status(400).json(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error saving endpoint configuration: {}", name, e);
            ctx.status(500).json(Map.of("error", "Failed to save endpoint configuration: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/management/config/endpoints/{name} - Delete endpoint configuration
     */
    public void deleteEndpointConfiguration(Context ctx) {
        String name = ctx.pathParam("name");
        logger.debug("Deleting endpoint configuration: {}", name);

        try {
            Map<String, Object> result = configurationManagementService.deleteEndpointConfiguration(name);
            ctx.json(result);
        } catch (IllegalStateException e) {
            logger.warn("Configuration management not available: {}", e.getMessage());
            ctx.status(400).json(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error deleting endpoint configuration: {}", name, e);
            ctx.status(500).json(Map.of("error", "Failed to delete endpoint configuration: " + e.getMessage()));
        }
    }

    // ========== CONFIGURATION MANAGEMENT ENDPOINTS ==========

    /**
     * GET /api/management/config/statistics - Get configuration statistics
     */
    public void getConfigurationStatistics(Context ctx) {
        logger.debug("Getting configuration statistics");
        try {
            Map<String, Object> result = configurationManagementService.getConfigurationStatistics();
            ctx.json(result);
        } catch (Exception e) {
            logger.error("Error getting configuration statistics", e);
            ctx.status(500).json(Map.of("error", "Failed to get configuration statistics: " + e.getMessage()));
        }
    }

    /**
     * GET /api/management/config/source - Get configuration source information
     */
    public void getConfigurationSourceInfo(Context ctx) {
        logger.debug("Getting configuration source information");
        try {
            Map<String, Object> result = configurationManagementService.getConfigurationSourceInfo();
            ctx.json(result);
        } catch (Exception e) {
            logger.error("Error getting configuration source information", e);
            ctx.status(500).json(Map.of("error", "Failed to get configuration source information: " + e.getMessage()));
        }
    }

    /**
     * GET /api/management/config/availability - Check if configuration management is available
     */
    public void getConfigurationManagementAvailability(Context ctx) {
        logger.debug("Checking configuration management availability");
        try {
            boolean available = configurationManagementService.isConfigurationManagementAvailable();
            String source = configurationManagementService.getConfigurationSourceInfo().get("currentSource").toString();

            ctx.json(Map.of(
                "available", available,
                "source", source,
                "message", available ?
                    "Configuration management is available" :
                    "Configuration management requires database source. Current source: " + source
            ));
        } catch (Exception e) {
            logger.error("Error checking configuration management availability", e);
            ctx.status(500).json(Map.of("error", "Failed to check configuration management availability: " + e.getMessage()));
        }
    }
}

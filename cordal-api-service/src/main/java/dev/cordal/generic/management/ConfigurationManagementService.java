package dev.cordal.generic.management;

import dev.cordal.database.repository.DatabaseConfigurationRepository;
import dev.cordal.database.repository.QueryConfigurationRepository;
import dev.cordal.database.repository.EndpointConfigurationRepository;
import dev.cordal.generic.config.DatabaseConfig;
import dev.cordal.generic.config.QueryConfig;
import dev.cordal.generic.config.ApiEndpointConfig;
import dev.cordal.generic.config.ConfigurationLoaderFactory;
import dev.cordal.generic.config.EndpointConfigurationManager;
import dev.cordal.dto.ConfigurationStatisticsResponse;
import dev.cordal.dto.ConfigurationSourceInfoResponse;
import dev.cordal.dto.ConfigurationListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.*;

/**
 * Service for managing configurations stored in the database
 * Provides CRUD operations for database, query, and endpoint configurations
 */
@Singleton
public class ConfigurationManagementService {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManagementService.class);

    private final DatabaseConfigurationRepository databaseRepository;
    private final QueryConfigurationRepository queryRepository;
    private final EndpointConfigurationRepository endpointRepository;
    private final ConfigurationLoaderFactory configurationLoaderFactory;
    private final EndpointConfigurationManager configurationManager;

    @Inject
    public ConfigurationManagementService(DatabaseConfigurationRepository databaseRepository,
                                        QueryConfigurationRepository queryRepository,
                                        EndpointConfigurationRepository endpointRepository,
                                        ConfigurationLoaderFactory configurationLoaderFactory,
                                        EndpointConfigurationManager configurationManager) {
        this.databaseRepository = databaseRepository;
        this.queryRepository = queryRepository;
        this.endpointRepository = endpointRepository;
        this.configurationLoaderFactory = configurationLoaderFactory;
        this.configurationManager = configurationManager;
        logger.info("Configuration management service initialized");
    }

    // ========== DATABASE CONFIGURATION MANAGEMENT ==========

    /**
     * Get all database configurations
     */
    public Map<String, Object> getAllDatabaseConfigurations() {
        logger.debug("Getting all database configurations");

        // Use configuration manager to get configurations from current source (YAML or database)
        Map<String, DatabaseConfig> configurations = configurationManager.getAllDatabaseConfigurations();

        return Map.of(
            "count", configurations.size(),
            "source", configurationLoaderFactory.getConfigurationSource(),
            "timestamp", Instant.now(),
            "databases", configurations
        );
    }

    /**
     * Get database configuration by name
     */
    public Optional<DatabaseConfig> getDatabaseConfiguration(String name) {
        logger.debug("Getting database configuration: {}", name);
        // Use configuration manager to get configuration from current source
        Map<String, DatabaseConfig> allConfigs = configurationManager.getAllDatabaseConfigurations();
        return Optional.ofNullable(allConfigs.get(name));
    }

    /**
     * Create or update database configuration
     */
    public Map<String, Object> saveDatabaseConfiguration(String name, DatabaseConfig config) {
        logger.info("Saving database configuration: {}", name);
        
        if (!configurationLoaderFactory.isDatabaseSource()) {
            throw new IllegalStateException("Configuration management is only available when using database source. Current source: " + 
                                          configurationLoaderFactory.getConfigurationSource());
        }
        
        try {
            boolean existed = databaseRepository.exists(name);
            databaseRepository.save(name, config);
            
            return Map.of(
                "success", true,
                "action", existed ? "updated" : "created",
                "name", name,
                "timestamp", Instant.now()
            );
            
        } catch (Exception e) {
            logger.error("Failed to save database configuration: {}", name, e);
            throw new RuntimeException("Failed to save database configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Delete database configuration
     */
    public Map<String, Object> deleteDatabaseConfiguration(String name) {
        logger.info("Deleting database configuration: {}", name);
        
        if (!configurationLoaderFactory.isDatabaseSource()) {
            throw new IllegalStateException("Configuration management is only available when using database source. Current source: " + 
                                          configurationLoaderFactory.getConfigurationSource());
        }
        
        try {
            boolean deleted = databaseRepository.delete(name);
            
            return Map.of(
                "success", deleted,
                "action", "deleted",
                "name", name,
                "found", deleted,
                "timestamp", Instant.now()
            );
            
        } catch (Exception e) {
            logger.error("Failed to delete database configuration: {}", name, e);
            throw new RuntimeException("Failed to delete database configuration: " + e.getMessage(), e);
        }
    }

    // ========== QUERY CONFIGURATION MANAGEMENT ==========

    /**
     * Get all query configurations
     */
    public Map<String, Object> getAllQueryConfigurations() {
        logger.debug("Getting all query configurations");

        // Use configuration manager to get configurations from current source (YAML or database)
        Map<String, QueryConfig> configurations = configurationManager.getAllQueryConfigurations();

        return Map.of(
            "count", configurations.size(),
            "source", configurationLoaderFactory.getConfigurationSource(),
            "timestamp", Instant.now(),
            "queries", configurations
        );
    }

    /**
     * Get query configuration by name
     */
    public Optional<QueryConfig> getQueryConfiguration(String name) {
        logger.debug("Getting query configuration: {}", name);
        // Use configuration manager to get configuration from current source
        Map<String, QueryConfig> allConfigs = configurationManager.getAllQueryConfigurations();
        return Optional.ofNullable(allConfigs.get(name));
    }

    /**
     * Get query configurations by database
     */
    public Map<String, Object> getQueryConfigurationsByDatabase(String databaseName) {
        logger.debug("Getting query configurations for database: {}", databaseName);
        
        List<QueryConfig> configurations = queryRepository.loadByDatabase(databaseName);
        
        return Map.of(
            "count", configurations.size(),
            "database", databaseName,
            "source", configurationLoaderFactory.getConfigurationSource(),
            "timestamp", Instant.now(),
            "queries", configurations
        );
    }

    /**
     * Create or update query configuration
     */
    public Map<String, Object> saveQueryConfiguration(String name, QueryConfig config) {
        logger.info("Saving query configuration: {}", name);
        
        if (!configurationLoaderFactory.isDatabaseSource()) {
            throw new IllegalStateException("Configuration management is only available when using database source. Current source: " + 
                                          configurationLoaderFactory.getConfigurationSource());
        }
        
        try {
            boolean existed = queryRepository.exists(name);
            queryRepository.save(name, config);
            
            return Map.of(
                "success", true,
                "action", existed ? "updated" : "created",
                "name", name,
                "timestamp", Instant.now()
            );
            
        } catch (Exception e) {
            logger.error("Failed to save query configuration: {}", name, e);
            throw new RuntimeException("Failed to save query configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Delete query configuration
     */
    public Map<String, Object> deleteQueryConfiguration(String name) {
        logger.info("Deleting query configuration: {}", name);
        
        if (!configurationLoaderFactory.isDatabaseSource()) {
            throw new IllegalStateException("Configuration management is only available when using database source. Current source: " + 
                                          configurationLoaderFactory.getConfigurationSource());
        }
        
        try {
            boolean deleted = queryRepository.delete(name);
            
            return Map.of(
                "success", deleted,
                "action", "deleted",
                "name", name,
                "found", deleted,
                "timestamp", Instant.now()
            );
            
        } catch (Exception e) {
            logger.error("Failed to delete query configuration: {}", name, e);
            throw new RuntimeException("Failed to delete query configuration: " + e.getMessage(), e);
        }
    }

    // ========== ENDPOINT CONFIGURATION MANAGEMENT ==========

    /**
     * Get all endpoint configurations
     */
    public Map<String, Object> getAllEndpointConfigurations() {
        logger.debug("Getting all endpoint configurations");

        // Use configuration manager to get configurations from current source (YAML or database)
        Map<String, ApiEndpointConfig> configurations = configurationManager.getAllEndpointConfigurations();

        return Map.of(
            "count", configurations.size(),
            "source", configurationLoaderFactory.getConfigurationSource(),
            "timestamp", Instant.now(),
            "endpoints", configurations
        );
    }

    /**
     * Get endpoint configuration by name
     */
    public Optional<ApiEndpointConfig> getEndpointConfiguration(String name) {
        logger.debug("Getting endpoint configuration: {}", name);
        // Use configuration manager to get configuration from current source
        Map<String, ApiEndpointConfig> allConfigs = configurationManager.getAllEndpointConfigurations();
        return Optional.ofNullable(allConfigs.get(name));
    }

    /**
     * Get endpoint configurations by query
     */
    public Map<String, Object> getEndpointConfigurationsByQuery(String queryName) {
        logger.debug("Getting endpoint configurations for query: {}", queryName);
        
        List<ApiEndpointConfig> configurations = endpointRepository.loadByQuery(queryName);
        
        return Map.of(
            "count", configurations.size(),
            "query", queryName,
            "source", configurationLoaderFactory.getConfigurationSource(),
            "timestamp", Instant.now(),
            "endpoints", configurations
        );
    }

    /**
     * Create or update endpoint configuration
     */
    public Map<String, Object> saveEndpointConfiguration(String name, ApiEndpointConfig config) {
        logger.info("Saving endpoint configuration: {}", name);
        
        if (!configurationLoaderFactory.isDatabaseSource()) {
            throw new IllegalStateException("Configuration management is only available when using database source. Current source: " + 
                                          configurationLoaderFactory.getConfigurationSource());
        }
        
        try {
            boolean existed = endpointRepository.exists(name);
            endpointRepository.save(name, config);
            
            return Map.of(
                "success", true,
                "action", existed ? "updated" : "created",
                "name", name,
                "timestamp", Instant.now()
            );
            
        } catch (Exception e) {
            logger.error("Failed to save endpoint configuration: {}", name, e);
            throw new RuntimeException("Failed to save endpoint configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Delete endpoint configuration
     */
    public Map<String, Object> deleteEndpointConfiguration(String name) {
        logger.info("Deleting endpoint configuration: {}", name);
        
        if (!configurationLoaderFactory.isDatabaseSource()) {
            throw new IllegalStateException("Configuration management is only available when using database source. Current source: " + 
                                          configurationLoaderFactory.getConfigurationSource());
        }
        
        try {
            boolean deleted = endpointRepository.delete(name);
            
            return Map.of(
                "success", deleted,
                "action", "deleted",
                "name", name,
                "found", deleted,
                "timestamp", Instant.now()
            );
            
        } catch (Exception e) {
            logger.error("Failed to delete endpoint configuration: {}", name, e);
            throw new RuntimeException("Failed to delete endpoint configuration: " + e.getMessage(), e);
        }
    }

    // ========== CONFIGURATION STATISTICS ==========

    /**
     * Get configuration statistics
     */
    public ConfigurationStatisticsResponse getConfigurationStatistics() {
        logger.debug("Getting configuration statistics");

        try {
            int databaseCount = databaseRepository.getCount();
            int queryCount = queryRepository.getCount();
            int endpointCount = endpointRepository.getCount();

            return new ConfigurationStatisticsResponse(
                configurationLoaderFactory.getConfigurationSource(),
                Instant.now(),
                new ConfigurationStatisticsResponse.StatisticsData(
                    new ConfigurationStatisticsResponse.DatabaseStats(databaseCount),
                    new ConfigurationStatisticsResponse.QueryStats(queryCount),
                    new ConfigurationStatisticsResponse.EndpointStats(endpointCount)
                ),
                new ConfigurationStatisticsResponse.SummaryData(
                    databaseCount + queryCount + endpointCount,
                    databaseCount,
                    queryCount,
                    endpointCount
                )
            );

        } catch (Exception e) {
            logger.error("Failed to get configuration statistics", e);
            throw new RuntimeException("Failed to get configuration statistics: " + e.getMessage(), e);
        }
    }

    /**
     * Check if configuration management is available
     */
    public boolean isConfigurationManagementAvailable() {
        return configurationLoaderFactory.isDatabaseSource();
    }

    /**
     * Get configuration source information
     */
    public ConfigurationSourceInfoResponse getConfigurationSourceInfo() {
        return new ConfigurationSourceInfoResponse(
            configurationLoaderFactory.getConfigurationSource(),
            isConfigurationManagementAvailable(),
            List.of("yaml", "database"),
            Instant.now()
        );
    }
}

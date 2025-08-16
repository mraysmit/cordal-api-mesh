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
import dev.cordal.generic.dto.ConfigurationCollectionResponse;
import dev.cordal.generic.dto.ConfigurationOperationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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
     * Get all database configurations with type-safe response
     */
    public ConfigurationCollectionResponse<DatabaseConfig> getAllDatabaseConfigurations() {
        logger.debug("Getting all database configurations");

        // Use configuration manager to get configurations from current source (YAML or database)
        Map<String, DatabaseConfig> configurations = configurationManager.getAllDatabaseConfigurations();

        return ConfigurationCollectionResponse.of(
            configurationLoaderFactory.getConfigurationSource(),
            configurations
        );
    }

    /**
     * Get all database configurations (DEPRECATED - use type-safe version)
     * @deprecated Use getAllDatabaseConfigurations() for type safety
     */
    @Deprecated
    public Map<String, Object> getAllDatabaseConfigurationsMap() {
        return getAllDatabaseConfigurations().toMap();
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
     * Create or update database configuration with type-safe response
     */
    public ConfigurationOperationResponse saveDatabaseConfiguration(String name, DatabaseConfig config) {
        logger.info("Saving database configuration: {}", name);

        if (!configurationLoaderFactory.isDatabaseSource()) {
            throw new IllegalStateException("Configuration management is only available when using database source. Current source: " +
                                          configurationLoaderFactory.getConfigurationSource());
        }

        try {
            boolean existed = databaseRepository.exists(name);
            databaseRepository.save(name, config);

            return existed ? ConfigurationOperationResponse.updated(name)
                          : ConfigurationOperationResponse.created(name);

        } catch (Exception e) {
            logger.error("Failed to save database configuration: {}", name, e);
            throw new RuntimeException("Failed to save database configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Create or update database configuration (DEPRECATED - use type-safe version)
     * @deprecated Use saveDatabaseConfiguration(String, DatabaseConfig) for type safety
     */
    @Deprecated
    public Map<String, Object> saveDatabaseConfigurationMap(String name, DatabaseConfig config) {
        return saveDatabaseConfiguration(name, config).toMap();
    }

    /**
     * Delete database configuration with type-safe response
     */
    public ConfigurationOperationResponse deleteDatabaseConfiguration(String name) {
        logger.info("Deleting database configuration: {}", name);

        if (!configurationLoaderFactory.isDatabaseSource()) {
            throw new IllegalStateException("Configuration management is only available when using database source. Current source: " +
                                          configurationLoaderFactory.getConfigurationSource());
        }

        try {
            boolean deleted = databaseRepository.delete(name);
            return ConfigurationOperationResponse.deleted(name, deleted);

        } catch (Exception e) {
            logger.error("Failed to delete database configuration: {}", name, e);
            throw new RuntimeException("Failed to delete database configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Delete database configuration (DEPRECATED - use type-safe version)
     * @deprecated Use deleteDatabaseConfiguration(String) for type safety
     */
    @Deprecated
    public Map<String, Object> deleteDatabaseConfigurationMap(String name) {
        return deleteDatabaseConfiguration(name).toMap();
    }

    // ========== QUERY CONFIGURATION MANAGEMENT ==========

    /**
     * Get all query configurations with type-safe response
     */
    public ConfigurationCollectionResponse<QueryConfig> getAllQueryConfigurations() {
        logger.debug("Getting all query configurations");

        // Use configuration manager to get configurations from current source (YAML or database)
        Map<String, QueryConfig> configurations = configurationManager.getAllQueryConfigurations();

        return ConfigurationCollectionResponse.of(
            configurationLoaderFactory.getConfigurationSource(),
            configurations
        );
    }

    /**
     * Get all query configurations (DEPRECATED - use type-safe version)
     * @deprecated Use getAllQueryConfigurations() for type safety
     */
    @Deprecated
    public Map<String, Object> getAllQueryConfigurationsMap() {
        return getAllQueryConfigurations().toMap();
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
     * Get query configurations by database with type-safe response
     */
    public ConfigurationCollectionResponse<QueryConfig> getQueryConfigurationsByDatabase(String databaseName) {
        logger.debug("Getting query configurations for database: {}", databaseName);

        List<QueryConfig> configurations = queryRepository.loadByDatabase(databaseName);

        // Convert List to Map for ConfigurationListResponse
        Map<String, QueryConfig> configMap = configurations.stream()
            .collect(Collectors.toMap(
                QueryConfig::getName,
                config -> config,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));

        return ConfigurationCollectionResponse.forDatabase(
            configurationLoaderFactory.getConfigurationSource(),
            configMap,
            databaseName
        );
    }

    /**
     * Get query configurations by database (DEPRECATED - use type-safe version)
     * @deprecated Use getQueryConfigurationsByDatabase(String) for type safety
     */
    @Deprecated
    public Map<String, Object> getQueryConfigurationsByDatabaseMap(String databaseName) {
        return getQueryConfigurationsByDatabase(databaseName).toMap();
    }

    /**
     * Create or update query configuration with type-safe response
     */
    public ConfigurationOperationResponse saveQueryConfiguration(String name, QueryConfig config) {
        logger.info("Saving query configuration: {}", name);

        if (!configurationLoaderFactory.isDatabaseSource()) {
            throw new IllegalStateException("Configuration management is only available when using database source. Current source: " +
                                          configurationLoaderFactory.getConfigurationSource());
        }

        try {
            boolean existed = queryRepository.exists(name);
            queryRepository.save(name, config);

            return existed ? ConfigurationOperationResponse.updated(name)
                          : ConfigurationOperationResponse.created(name);

        } catch (Exception e) {
            logger.error("Failed to save query configuration: {}", name, e);
            throw new RuntimeException("Failed to save query configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Create or update query configuration (DEPRECATED - use type-safe version)
     * @deprecated Use saveQueryConfiguration(String, QueryConfig) for type safety
     */
    @Deprecated
    public Map<String, Object> saveQueryConfigurationMap(String name, QueryConfig config) {
        return saveQueryConfiguration(name, config).toMap();
    }

    /**
     * Delete query configuration with type-safe response
     */
    public ConfigurationOperationResponse deleteQueryConfiguration(String name) {
        logger.info("Deleting query configuration: {}", name);

        if (!configurationLoaderFactory.isDatabaseSource()) {
            throw new IllegalStateException("Configuration management is only available when using database source. Current source: " +
                                          configurationLoaderFactory.getConfigurationSource());
        }

        try {
            boolean deleted = queryRepository.delete(name);
            return ConfigurationOperationResponse.deleted(name, deleted);

        } catch (Exception e) {
            logger.error("Failed to delete query configuration: {}", name, e);
            throw new RuntimeException("Failed to delete query configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Delete query configuration (DEPRECATED - use type-safe version)
     * @deprecated Use deleteQueryConfiguration(String) for type safety
     */
    @Deprecated
    public Map<String, Object> deleteQueryConfigurationMap(String name) {
        return deleteQueryConfiguration(name).toMap();
    }

    // ========== ENDPOINT CONFIGURATION MANAGEMENT ==========

    /**
     * Get all endpoint configurations with type-safe response
     */
    public ConfigurationCollectionResponse<ApiEndpointConfig> getAllEndpointConfigurations() {
        logger.debug("Getting all endpoint configurations");

        // Use configuration manager to get configurations from current source (YAML or database)
        Map<String, ApiEndpointConfig> configurations = configurationManager.getAllEndpointConfigurations();

        return ConfigurationCollectionResponse.of(
            configurationLoaderFactory.getConfigurationSource(),
            configurations
        );
    }

    /**
     * Get all endpoint configurations (DEPRECATED - use type-safe version)
     * @deprecated Use getAllEndpointConfigurations() for type safety
     */
    @Deprecated
    public Map<String, Object> getAllEndpointConfigurationsMap() {
        return getAllEndpointConfigurations().toMap();
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
     * Get endpoint configurations by query with type-safe response
     */
    public ConfigurationCollectionResponse<ApiEndpointConfig> getEndpointConfigurationsByQuery(String queryName) {
        logger.debug("Getting endpoint configurations for query: {}", queryName);

        List<ApiEndpointConfig> configurations = endpointRepository.loadByQuery(queryName);

        // Convert List to Map for ConfigurationCollectionResponse
        // Since ApiEndpointConfig doesn't have getName(), we'll use the path as the key
        Map<String, ApiEndpointConfig> configMap = configurations.stream()
            .collect(Collectors.toMap(
                config -> config.getPath() != null ? config.getPath() : "unknown",
                config -> config,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));

        return ConfigurationCollectionResponse.forQuery(
            configurationLoaderFactory.getConfigurationSource(),
            configMap,
            queryName
        );
    }

    /**
     * Get endpoint configurations by query (DEPRECATED - use type-safe version)
     * @deprecated Use getEndpointConfigurationsByQuery(String) for type safety
     */
    @Deprecated
    public Map<String, Object> getEndpointConfigurationsByQueryMap(String queryName) {
        return getEndpointConfigurationsByQuery(queryName).toMap();
    }

    /**
     * Create or update endpoint configuration with type-safe response
     */
    public ConfigurationOperationResponse saveEndpointConfiguration(String name, ApiEndpointConfig config) {
        logger.info("Saving endpoint configuration: {}", name);

        if (!configurationLoaderFactory.isDatabaseSource()) {
            throw new IllegalStateException("Configuration management is only available when using database source. Current source: " +
                                          configurationLoaderFactory.getConfigurationSource());
        }

        try {
            boolean existed = endpointRepository.exists(name);
            endpointRepository.save(name, config);

            return existed ? ConfigurationOperationResponse.updated(name)
                          : ConfigurationOperationResponse.created(name);

        } catch (Exception e) {
            logger.error("Failed to save endpoint configuration: {}", name, e);
            throw new RuntimeException("Failed to save endpoint configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Create or update endpoint configuration (DEPRECATED - use type-safe version)
     * @deprecated Use saveEndpointConfiguration(String, ApiEndpointConfig) for type safety
     */
    @Deprecated
    public Map<String, Object> saveEndpointConfigurationMap(String name, ApiEndpointConfig config) {
        return saveEndpointConfiguration(name, config).toMap();
    }

    /**
     * Delete endpoint configuration with type-safe response
     */
    public ConfigurationOperationResponse deleteEndpointConfiguration(String name) {
        logger.info("Deleting endpoint configuration: {}", name);

        if (!configurationLoaderFactory.isDatabaseSource()) {
            throw new IllegalStateException("Configuration management is only available when using database source. Current source: " +
                                          configurationLoaderFactory.getConfigurationSource());
        }

        try {
            boolean deleted = endpointRepository.delete(name);
            return ConfigurationOperationResponse.deleted(name, deleted);

        } catch (Exception e) {
            logger.error("Failed to delete endpoint configuration: {}", name, e);
            throw new RuntimeException("Failed to delete endpoint configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Delete endpoint configuration (DEPRECATED - use type-safe version)
     * @deprecated Use deleteEndpointConfiguration(String) for type safety
     */
    @Deprecated
    public Map<String, Object> deleteEndpointConfigurationMap(String name) {
        return deleteEndpointConfiguration(name).toMap();
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

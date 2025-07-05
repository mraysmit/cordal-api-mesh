package dev.mars.database.loader;

import dev.mars.database.repository.DatabaseConfigurationRepository;
import dev.mars.database.repository.QueryConfigurationRepository;
import dev.mars.database.repository.EndpointConfigurationRepository;
import dev.mars.generic.config.DatabaseConfig;
import dev.mars.generic.config.QueryConfig;
import dev.mars.generic.config.ApiEndpointConfig;
import dev.mars.generic.config.ConfigurationLoaderInterface;
import dev.mars.common.exception.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

/**
 * Configuration loader that reads configurations from H2 database instead of YAML files
 * This is an alternative to ConfigurationLoader for database-based configuration storage
 */
@Singleton
public class DatabaseConfigurationLoader implements ConfigurationLoaderInterface {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfigurationLoader.class);

    private final DatabaseConfigurationRepository databaseRepository;
    private final QueryConfigurationRepository queryRepository;
    private final EndpointConfigurationRepository endpointRepository;

    @Inject
    public DatabaseConfigurationLoader(DatabaseConfigurationRepository databaseRepository,
                                     QueryConfigurationRepository queryRepository,
                                     EndpointConfigurationRepository endpointRepository) {
        this.databaseRepository = databaseRepository;
        this.queryRepository = queryRepository;
        this.endpointRepository = endpointRepository;
        logger.info("Database configuration loader initialized");
    }

    /**
     * Load database configurations from the database
     */
    public Map<String, DatabaseConfig> loadDatabaseConfigurations() {
        logger.info("Loading database configurations from database");
        
        try {
            Map<String, DatabaseConfig> configurations = databaseRepository.loadAll();
            
            if (configurations.isEmpty()) {
                logger.error("FATAL CONFIGURATION ERROR: No database configurations found in database");
                logger.error("  Source: Database tables");
                logger.error("  Type: Database configurations");
                logger.error("  Issue: Database contains no valid database definitions");
                logger.error("  Impact: Application cannot start without database configurations");
                logger.error("  Action: Ensure database contains valid configuration data or switch to YAML source");
                logger.error("Application startup aborted due to empty configuration database");
                throw new dev.mars.common.exception.ConfigurationException("No database configurations found in database tables");
            }

            logger.info("Successfully loaded {} database configurations from database", configurations.size());

            // Log each database for debugging
            configurations.forEach((key, config) -> {
                logger.info("  - Database '{}': {} (driver: {})", key, config.getUrl(), config.getDriver());
            });

            return configurations;

        } catch (Exception e) {
            logger.error("FATAL CONFIGURATION ERROR: Failed to load database configurations from database");
            logger.error("  Source: Database tables");
            logger.error("  Type: Database configurations");
            logger.error("  Error: {}", e.getMessage());
            logger.error("  Impact: Application cannot start without valid database configurations");
            logger.error("  Action: Check database connectivity and table structure");
            logger.error("Application startup aborted due to configuration loading failure");
            throw new ConfigurationException("Failed to load database configurations from database: " + e.getMessage(), e);
        }
    }

    /**
     * Load query configurations from the database
     */
    public Map<String, QueryConfig> loadQueryConfigurations() {
        logger.info("Loading query configurations from database");
        
        try {
            Map<String, QueryConfig> configurations = queryRepository.loadAll();
            
            if (configurations.isEmpty()) {
                logger.error("FATAL CONFIGURATION ERROR: No query configurations found in database");
                logger.error("  Source: Database tables");
                logger.error("  Type: Query configurations");
                logger.error("  Issue: Database contains no valid query definitions");
                logger.error("  Impact: Application cannot start without query configurations");
                logger.error("  Action: Ensure database contains valid configuration data or switch to YAML source");
                logger.error("Application startup aborted due to empty configuration database");
                throw new dev.mars.common.exception.ConfigurationException("No query configurations found in database tables");
            }

            logger.info("Successfully loaded {} query configurations from database", configurations.size());

            // Log each query for debugging
            configurations.forEach((key, config) -> {
                logger.info("  - Query '{}': database={}, sql={}", key, config.getDatabase(), 
                           config.getSql().length() > 50 ? config.getSql().substring(0, 50) + "..." : config.getSql());
            });

            return configurations;

        } catch (Exception e) {
            logger.error("FATAL CONFIGURATION ERROR: Failed to load query configurations from database");
            logger.error("  Source: Database tables");
            logger.error("  Type: Query configurations");
            logger.error("  Error: {}", e.getMessage());
            logger.error("  Impact: Application cannot start without valid query configurations");
            logger.error("  Action: Check database connectivity and table structure");
            logger.error("Application startup aborted due to configuration loading failure", e);
            throw new dev.mars.common.exception.ConfigurationException(
                "Failed to load query configurations from database: " + e.getMessage(), e);
        }
    }

    /**
     * Load endpoint configurations from the database
     */
    public Map<String, ApiEndpointConfig> loadEndpointConfigurations() {
        logger.info("Loading endpoint configurations from database");
        
        try {
            Map<String, ApiEndpointConfig> configurations = endpointRepository.loadAll();
            
            if (configurations.isEmpty()) {
                logger.error("FATAL CONFIGURATION ERROR: No endpoint configurations found in database");
                logger.error("  Source: Database tables");
                logger.error("  Type: Endpoint configurations");
                logger.error("  Issue: Database contains no valid endpoint definitions");
                logger.error("  Impact: Application cannot start without endpoint configurations");
                logger.error("  Action: Ensure database contains valid configuration data or switch to YAML source");
                logger.error("Application startup aborted due to empty configuration database");
                throw new dev.mars.common.exception.ConfigurationException("No endpoint configurations found in database tables");
            }

            logger.info("Successfully loaded {} endpoint configurations from database", configurations.size());

            // Log each endpoint for debugging
            configurations.forEach((key, config) -> {
                logger.info("  - Endpoint '{}': {} {} (query: {})", key, config.getMethod(), config.getPath(), config.getQuery());
            });

            return configurations;

        } catch (Exception e) {
            logger.error("FATAL CONFIGURATION ERROR: Failed to load endpoint configurations from database");
            logger.error("  Source: Database tables");
            logger.error("  Type: Endpoint configurations");
            logger.error("  Error: {}", e.getMessage());
            logger.error("  Impact: Application cannot start without valid endpoint configurations");
            logger.error("  Action: Check database connectivity and table structure");
            logger.error("Application startup aborted due to configuration loading failure", e);
            throw new dev.mars.common.exception.ConfigurationException(
                "Failed to load endpoint configurations from database: " + e.getMessage(), e);
        }
    }

    /**
     * Get configuration statistics
     */
    public ConfigurationStats getConfigurationStats() {
        logger.debug("Getting configuration statistics from database");
        
        try {
            int databaseCount = databaseRepository.getCount();
            int queryCount = queryRepository.getCount();
            int endpointCount = endpointRepository.getCount();
            
            ConfigurationStats stats = new ConfigurationStats(databaseCount, queryCount, endpointCount);
            logger.debug("Configuration stats: {}", stats);
            
            return stats;
            
        } catch (Exception e) {
            logger.error("Failed to get configuration statistics", e);
            throw new RuntimeException("Failed to get configuration statistics", e);
        }
    }

    /**
     * Configuration statistics holder
     */
    public static class ConfigurationStats {
        private final int databaseCount;
        private final int queryCount;
        private final int endpointCount;

        public ConfigurationStats(int databaseCount, int queryCount, int endpointCount) {
            this.databaseCount = databaseCount;
            this.queryCount = queryCount;
            this.endpointCount = endpointCount;
        }

        public int getDatabaseCount() {
            return databaseCount;
        }

        public int getQueryCount() {
            return queryCount;
        }

        public int getEndpointCount() {
            return endpointCount;
        }

        @Override
        public String toString() {
            return String.format("ConfigurationStats{databases=%d, queries=%d, endpoints=%d}", 
                               databaseCount, queryCount, endpointCount);
        }
    }
}

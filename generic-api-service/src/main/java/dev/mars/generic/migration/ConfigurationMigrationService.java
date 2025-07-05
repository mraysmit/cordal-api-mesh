package dev.mars.generic.migration;

import dev.mars.database.repository.DatabaseConfigurationRepository;
import dev.mars.database.repository.QueryConfigurationRepository;
import dev.mars.database.repository.EndpointConfigurationRepository;
import dev.mars.generic.config.ConfigurationLoader;
import dev.mars.generic.config.ConfigurationLoaderFactory;
import dev.mars.generic.config.DatabaseConfig;
import dev.mars.generic.config.QueryConfig;
import dev.mars.generic.config.ApiEndpointConfig;
import dev.mars.database.loader.DatabaseConfigurationLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.*;

/**
 * Service for migrating and synchronizing configurations between YAML and database sources
 */
@Singleton
public class ConfigurationMigrationService {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationMigrationService.class);

    private final DatabaseConfigurationRepository databaseRepository;
    private final QueryConfigurationRepository queryRepository;
    private final EndpointConfigurationRepository endpointRepository;
    private final ConfigurationLoader yamlLoader;
    private final DatabaseConfigurationLoader databaseLoader;
    private final ConfigurationLoaderFactory configurationLoaderFactory;

    @Inject
    public ConfigurationMigrationService(DatabaseConfigurationRepository databaseRepository,
                                       QueryConfigurationRepository queryRepository,
                                       EndpointConfigurationRepository endpointRepository,
                                       ConfigurationLoader yamlLoader,
                                       DatabaseConfigurationLoader databaseLoader,
                                       ConfigurationLoaderFactory configurationLoaderFactory) {
        this.databaseRepository = databaseRepository;
        this.queryRepository = queryRepository;
        this.endpointRepository = endpointRepository;
        this.yamlLoader = yamlLoader;
        this.databaseLoader = databaseLoader;
        this.configurationLoaderFactory = configurationLoaderFactory;
        logger.info("Configuration migration service initialized");
    }

    // ========== YAML TO DATABASE MIGRATION ==========

    /**
     * Migrate all configurations from YAML to database
     */
    public MigrationResult migrateYamlToDatabase() {
        logger.info("Starting migration from YAML to database");
        
        MigrationResult result = new MigrationResult("YAML", "DATABASE");
        
        try {
            // Load configurations from YAML
            Map<String, DatabaseConfig> yamlDatabases = yamlLoader.loadDatabaseConfigurations();
            Map<String, QueryConfig> yamlQueries = yamlLoader.loadQueryConfigurations();
            Map<String, ApiEndpointConfig> yamlEndpoints = yamlLoader.loadEndpointConfigurations();
            
            logger.info("Loaded from YAML: {} databases, {} queries, {} endpoints", 
                       yamlDatabases.size(), yamlQueries.size(), yamlEndpoints.size());
            
            // Migrate databases
            result.databaseResults = migrateDatabaseConfigurations(yamlDatabases);
            
            // Migrate queries
            result.queryResults = migrateQueryConfigurations(yamlQueries);
            
            // Migrate endpoints
            result.endpointResults = migrateEndpointConfigurations(yamlEndpoints);
            
            result.success = true;
            result.completedAt = Instant.now();
            
            logger.info("Migration completed successfully: {} databases, {} queries, {} endpoints migrated",
                       result.databaseResults.created + result.databaseResults.updated,
                       result.queryResults.created + result.queryResults.updated,
                       result.endpointResults.created + result.endpointResults.updated);
            
        } catch (Exception e) {
            logger.error("Migration failed", e);
            result.success = false;
            result.error = e.getMessage();
            result.completedAt = Instant.now();
        }
        
        return result;
    }

    /**
     * Migrate database configurations from YAML to database
     */
    private MigrationItemResult migrateDatabaseConfigurations(Map<String, DatabaseConfig> yamlDatabases) {
        logger.info("Migrating {} database configurations", yamlDatabases.size());
        
        MigrationItemResult result = new MigrationItemResult();
        
        for (Map.Entry<String, DatabaseConfig> entry : yamlDatabases.entrySet()) {
            String name = entry.getKey();
            DatabaseConfig config = entry.getValue();
            
            try {
                boolean existed = databaseRepository.exists(name);
                databaseRepository.save(name, config);
                
                if (existed) {
                    result.updated++;
                    logger.debug("Updated database configuration: {}", name);
                } else {
                    result.created++;
                    logger.debug("Created database configuration: {}", name);
                }
                
            } catch (Exception e) {
                result.failed++;
                result.errors.add("Failed to migrate database '" + name + "': " + e.getMessage());
                logger.error("Failed to migrate database configuration: {}", name, e);
            }
        }
        
        logger.info("Database migration completed: {} created, {} updated, {} failed", 
                   result.created, result.updated, result.failed);
        return result;
    }

    /**
     * Migrate query configurations from YAML to database
     */
    private MigrationItemResult migrateQueryConfigurations(Map<String, QueryConfig> yamlQueries) {
        logger.info("Migrating {} query configurations", yamlQueries.size());
        
        MigrationItemResult result = new MigrationItemResult();
        
        for (Map.Entry<String, QueryConfig> entry : yamlQueries.entrySet()) {
            String name = entry.getKey();
            QueryConfig config = entry.getValue();
            
            try {
                boolean existed = queryRepository.exists(name);
                queryRepository.save(name, config);
                
                if (existed) {
                    result.updated++;
                    logger.debug("Updated query configuration: {}", name);
                } else {
                    result.created++;
                    logger.debug("Created query configuration: {}", name);
                }
                
            } catch (Exception e) {
                result.failed++;
                result.errors.add("Failed to migrate query '" + name + "': " + e.getMessage());
                logger.error("Failed to migrate query configuration: {}", name, e);
            }
        }
        
        logger.info("Query migration completed: {} created, {} updated, {} failed", 
                   result.created, result.updated, result.failed);
        return result;
    }

    /**
     * Migrate endpoint configurations from YAML to database
     */
    private MigrationItemResult migrateEndpointConfigurations(Map<String, ApiEndpointConfig> yamlEndpoints) {
        logger.info("Migrating {} endpoint configurations", yamlEndpoints.size());
        
        MigrationItemResult result = new MigrationItemResult();
        
        for (Map.Entry<String, ApiEndpointConfig> entry : yamlEndpoints.entrySet()) {
            String name = entry.getKey();
            ApiEndpointConfig config = entry.getValue();
            
            try {
                boolean existed = endpointRepository.exists(name);
                endpointRepository.save(name, config);
                
                if (existed) {
                    result.updated++;
                    logger.debug("Updated endpoint configuration: {}", name);
                } else {
                    result.created++;
                    logger.debug("Created endpoint configuration: {}", name);
                }
                
            } catch (Exception e) {
                result.failed++;
                result.errors.add("Failed to migrate endpoint '" + name + "': " + e.getMessage());
                logger.error("Failed to migrate endpoint configuration: {}", name, e);
            }
        }
        
        logger.info("Endpoint migration completed: {} created, {} updated, {} failed", 
                   result.created, result.updated, result.failed);
        return result;
    }

    // ========== DATABASE TO YAML EXPORT ==========

    /**
     * Export all configurations from database to YAML format
     */
    public ExportResult exportDatabaseToYaml() {
        logger.info("Starting export from database to YAML format");
        
        ExportResult result = new ExportResult();
        
        try {
            // Load configurations from database
            Map<String, DatabaseConfig> dbDatabases = databaseRepository.loadAll();
            Map<String, QueryConfig> dbQueries = queryRepository.loadAll();
            Map<String, ApiEndpointConfig> dbEndpoints = endpointRepository.loadAll();
            
            logger.info("Loaded from database: {} databases, {} queries, {} endpoints", 
                       dbDatabases.size(), dbQueries.size(), dbEndpoints.size());
            
            // Convert to YAML format
            result.databasesYaml = convertDatabaseConfigurationsToYaml(dbDatabases);
            result.queriesYaml = convertQueryConfigurationsToYaml(dbQueries);
            result.endpointsYaml = convertEndpointConfigurationsToYaml(dbEndpoints);
            
            result.success = true;
            result.exportedAt = Instant.now();
            result.databaseCount = dbDatabases.size();
            result.queryCount = dbQueries.size();
            result.endpointCount = dbEndpoints.size();
            
            logger.info("Export completed successfully: {} databases, {} queries, {} endpoints exported",
                       result.databaseCount, result.queryCount, result.endpointCount);
            
        } catch (Exception e) {
            logger.error("Export failed", e);
            result.success = false;
            result.error = e.getMessage();
            result.exportedAt = Instant.now();
        }
        
        return result;
    }

    /**
     * Convert database configurations to YAML format
     */
    private String convertDatabaseConfigurationsToYaml(Map<String, DatabaseConfig> databases) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("# Database Configurations (exported from database)\n");
        yaml.append("# Generated at: ").append(Instant.now()).append("\n\n");
        yaml.append("databases:\n");

        for (Map.Entry<String, DatabaseConfig> entry : databases.entrySet()) {
            String name = entry.getKey();
            DatabaseConfig config = entry.getValue();

            yaml.append("  ").append(name).append(":\n");
            yaml.append("    name: \"").append(config.getName()).append("\"\n");
            if (config.getDescription() != null) {
                yaml.append("    description: \"").append(config.getDescription()).append("\"\n");
            }
            yaml.append("    url: \"").append(config.getUrl()).append("\"\n");
            if (config.getUsername() != null) {
                yaml.append("    username: \"").append(config.getUsername()).append("\"\n");
            }
            if (config.getPassword() != null) {
                yaml.append("    password: \"").append(config.getPassword()).append("\"\n");
            }
            yaml.append("    driver: \"").append(config.getDriver()).append("\"\n");

            if (config.getPool() != null) {
                DatabaseConfig.PoolConfig pool = config.getPool();
                yaml.append("    pool:\n");
                yaml.append("      maximumPoolSize: ").append(pool.getMaximumPoolSize()).append("\n");
                yaml.append("      minimumIdle: ").append(pool.getMinimumIdle()).append("\n");
                yaml.append("      connectionTimeout: ").append(pool.getConnectionTimeout()).append("\n");
                yaml.append("      idleTimeout: ").append(pool.getIdleTimeout()).append("\n");
                yaml.append("      maxLifetime: ").append(pool.getMaxLifetime()).append("\n");
                yaml.append("      leakDetectionThreshold: ").append(pool.getLeakDetectionThreshold()).append("\n");
                if (pool.getConnectionTestQuery() != null) {
                    yaml.append("      connectionTestQuery: \"").append(pool.getConnectionTestQuery()).append("\"\n");
                }
            }
            yaml.append("\n");
        }

        return yaml.toString();
    }

    /**
     * Convert query configurations to YAML format
     */
    private String convertQueryConfigurationsToYaml(Map<String, QueryConfig> queries) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("# Query Configurations (exported from database)\n");
        yaml.append("# Generated at: ").append(Instant.now()).append("\n\n");
        yaml.append("queries:\n");

        for (Map.Entry<String, QueryConfig> entry : queries.entrySet()) {
            String name = entry.getKey();
            QueryConfig config = entry.getValue();

            yaml.append("  ").append(name).append(":\n");
            yaml.append("    name: \"").append(config.getName()).append("\"\n");
            if (config.getDescription() != null) {
                yaml.append("    description: \"").append(config.getDescription()).append("\"\n");
            }
            yaml.append("    database: \"").append(config.getDatabase()).append("\"\n");

            // Handle multi-line SQL
            String sql = config.getSql();
            if (sql.contains("\n")) {
                yaml.append("    sql: |\n");
                for (String line : sql.split("\n")) {
                    yaml.append("      ").append(line).append("\n");
                }
            } else {
                yaml.append("    sql: \"").append(sql).append("\"\n");
            }

            // Parameters
            yaml.append("    parameters:");
            if (config.getParameters() != null && !config.getParameters().isEmpty()) {
                yaml.append("\n");
                for (QueryConfig.QueryParameter param : config.getParameters()) {
                    yaml.append("      - name: \"").append(param.getName()).append("\"\n");
                    yaml.append("        type: \"").append(param.getType()).append("\"\n");
                    yaml.append("        required: ").append(param.isRequired()).append("\n");
                }
            } else {
                yaml.append(" []\n");
            }
            yaml.append("\n");
        }

        return yaml.toString();
    }

    /**
     * Convert endpoint configurations to YAML format
     */
    private String convertEndpointConfigurationsToYaml(Map<String, ApiEndpointConfig> endpoints) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("# Endpoint Configurations (exported from database)\n");
        yaml.append("# Generated at: ").append(Instant.now()).append("\n\n");
        yaml.append("endpoints:\n");

        for (Map.Entry<String, ApiEndpointConfig> entry : endpoints.entrySet()) {
            String name = entry.getKey();
            ApiEndpointConfig config = entry.getValue();

            yaml.append("  ").append(name).append(":\n");
            yaml.append("    path: \"").append(config.getPath()).append("\"\n");
            yaml.append("    method: \"").append(config.getMethod()).append("\"\n");
            if (config.getDescription() != null) {
                yaml.append("    description: \"").append(config.getDescription()).append("\"\n");
            }
            yaml.append("    query: \"").append(config.getQuery()).append("\"\n");

            // Note: Complex nested structures (pagination, parameters, response) are not stored in database
            // This is a limitation of the current database schema
            yaml.append("    # Note: Complex configuration details (pagination, parameters, response) not available from database\n");
            yaml.append("    # These would need to be manually configured if migrating back to YAML\n");
            yaml.append("\n");
        }

        return yaml.toString();
    }

    // ========== SYNCHRONIZATION ==========

    /**
     * Compare configurations between YAML and database sources
     */
    public SynchronizationReport compareConfigurations() {
        logger.info("Comparing configurations between YAML and database sources");

        SynchronizationReport report = new SynchronizationReport();

        try {
            // Load from both sources
            Map<String, DatabaseConfig> yamlDatabases = yamlLoader.loadDatabaseConfigurations();
            Map<String, QueryConfig> yamlQueries = yamlLoader.loadQueryConfigurations();
            Map<String, ApiEndpointConfig> yamlEndpoints = yamlLoader.loadEndpointConfigurations();

            Map<String, DatabaseConfig> dbDatabases = databaseRepository.loadAll();
            Map<String, QueryConfig> dbQueries = queryRepository.loadAll();
            Map<String, ApiEndpointConfig> dbEndpoints = endpointRepository.loadAll();

            // Compare databases
            report.databaseComparison = compareConfigurationMaps(yamlDatabases.keySet(), dbDatabases.keySet(), "databases");

            // Compare queries
            report.queryComparison = compareConfigurationMaps(yamlQueries.keySet(), dbQueries.keySet(), "queries");

            // Compare endpoints
            report.endpointComparison = compareConfigurationMaps(yamlEndpoints.keySet(), dbEndpoints.keySet(), "endpoints");

            report.success = true;
            report.comparedAt = Instant.now();

            logger.info("Configuration comparison completed successfully");

        } catch (Exception e) {
            logger.error("Configuration comparison failed", e);
            report.success = false;
            report.error = e.getMessage();
            report.comparedAt = Instant.now();
        }

        return report;
    }

    /**
     * Compare two sets of configuration names
     */
    private ConfigurationComparison compareConfigurationMaps(Set<String> yamlKeys, Set<String> dbKeys, String type) {
        ConfigurationComparison comparison = new ConfigurationComparison();

        // Find items only in YAML
        comparison.onlyInYaml = new HashSet<>(yamlKeys);
        comparison.onlyInYaml.removeAll(dbKeys);

        // Find items only in database
        comparison.onlyInDatabase = new HashSet<>(dbKeys);
        comparison.onlyInDatabase.removeAll(yamlKeys);

        // Find items in both
        comparison.inBoth = new HashSet<>(yamlKeys);
        comparison.inBoth.retainAll(dbKeys);

        logger.debug("{} comparison: {} only in YAML, {} only in database, {} in both",
                    type, comparison.onlyInYaml.size(), comparison.onlyInDatabase.size(), comparison.inBoth.size());

        return comparison;
    }

    /**
     * Get migration status and information
     */
    public Map<String, Object> getMigrationStatus() {
        try {
            // Get current configuration source
            String currentSource = configurationLoaderFactory.getConfigurationSource();

            // Get counts from both sources
            Map<String, DatabaseConfig> yamlDatabases = yamlLoader.loadDatabaseConfigurations();
            Map<String, QueryConfig> yamlQueries = yamlLoader.loadQueryConfigurations();
            Map<String, ApiEndpointConfig> yamlEndpoints = yamlLoader.loadEndpointConfigurations();

            int dbDatabaseCount = databaseRepository.getCount();
            int dbQueryCount = queryRepository.getCount();
            int dbEndpointCount = endpointRepository.getCount();

            return Map.of(
                "currentSource", currentSource,
                "migrationAvailable", true,
                "yamlCounts", Map.of(
                    "databases", yamlDatabases.size(),
                    "queries", yamlQueries.size(),
                    "endpoints", yamlEndpoints.size(),
                    "total", yamlDatabases.size() + yamlQueries.size() + yamlEndpoints.size()
                ),
                "databaseCounts", Map.of(
                    "databases", dbDatabaseCount,
                    "queries", dbQueryCount,
                    "endpoints", dbEndpointCount,
                    "total", dbDatabaseCount + dbQueryCount + dbEndpointCount
                ),
                "timestamp", Instant.now()
            );

        } catch (Exception e) {
            logger.error("Failed to get migration status", e);
            return Map.of(
                "migrationAvailable", false,
                "error", e.getMessage(),
                "timestamp", Instant.now()
            );
        }
    }

    // ========== DATA CLASSES ==========

    public static class MigrationResult {
        public String sourceType;
        public String targetType;
        public boolean success;
        public String error;
        public Instant startedAt;
        public Instant completedAt;
        public MigrationItemResult databaseResults;
        public MigrationItemResult queryResults;
        public MigrationItemResult endpointResults;

        public MigrationResult(String sourceType, String targetType) {
            this.sourceType = sourceType;
            this.targetType = targetType;
            this.startedAt = Instant.now();
            this.databaseResults = new MigrationItemResult();
            this.queryResults = new MigrationItemResult();
            this.endpointResults = new MigrationItemResult();
        }

        public int getTotalCreated() {
            return databaseResults.created + queryResults.created + endpointResults.created;
        }

        public int getTotalUpdated() {
            return databaseResults.updated + queryResults.updated + endpointResults.updated;
        }

        public int getTotalFailed() {
            return databaseResults.failed + queryResults.failed + endpointResults.failed;
        }

        public List<String> getAllErrors() {
            List<String> allErrors = new ArrayList<>();
            allErrors.addAll(databaseResults.errors);
            allErrors.addAll(queryResults.errors);
            allErrors.addAll(endpointResults.errors);
            return allErrors;
        }
    }

    public static class MigrationItemResult {
        public int created = 0;
        public int updated = 0;
        public int failed = 0;
        public List<String> errors = new ArrayList<>();
    }

    public static class ExportResult {
        public boolean success;
        public String error;
        public Instant exportedAt;
        public int databaseCount;
        public int queryCount;
        public int endpointCount;
        public String databasesYaml;
        public String queriesYaml;
        public String endpointsYaml;
    }

    public static class SynchronizationReport {
        public boolean success;
        public String error;
        public Instant comparedAt;
        public ConfigurationComparison databaseComparison;
        public ConfigurationComparison queryComparison;
        public ConfigurationComparison endpointComparison;
    }

    public static class ConfigurationComparison {
        public Set<String> onlyInYaml = new HashSet<>();
        public Set<String> onlyInDatabase = new HashSet<>();
        public Set<String> inBoth = new HashSet<>();
    }
}

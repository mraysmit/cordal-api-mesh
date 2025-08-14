package dev.cordal.generic.migration;

import dev.cordal.generic.migration.ConfigurationMigrationService.*;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

/**
 * REST controller for configuration migration and synchronization operations
 */
@Singleton
public class ConfigurationMigrationController {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationMigrationController.class);

    private final ConfigurationMigrationService migrationService;

    @Inject
    public ConfigurationMigrationController(ConfigurationMigrationService migrationService) {
        this.migrationService = migrationService;
        logger.info("Configuration migration controller initialized");
    }

    // ========== MIGRATION ENDPOINTS ==========

    /**
     * POST /api/management/migration/yaml-to-database - Migrate configurations from YAML to database
     */
    public void migrateYamlToDatabase(Context ctx) {
        logger.info("Starting YAML to database migration");
        
        try {
            MigrationResult result = migrationService.migrateYamlToDatabase();
            
            Map<String, Object> response = Map.of(
                "success", result.success,
                "sourceType", result.sourceType,
                "targetType", result.targetType,
                "startedAt", result.startedAt,
                "completedAt", result.completedAt,
                "summary", Map.of(
                    "totalCreated", result.getTotalCreated(),
                    "totalUpdated", result.getTotalUpdated(),
                    "totalFailed", result.getTotalFailed()
                ),
                "details", Map.of(
                    "databases", Map.of(
                        "created", result.databaseResults.created,
                        "updated", result.databaseResults.updated,
                        "failed", result.databaseResults.failed
                    ),
                    "queries", Map.of(
                        "created", result.queryResults.created,
                        "updated", result.queryResults.updated,
                        "failed", result.queryResults.failed
                    ),
                    "endpoints", Map.of(
                        "created", result.endpointResults.created,
                        "updated", result.endpointResults.updated,
                        "failed", result.endpointResults.failed
                    )
                ),
                "errors", result.getAllErrors()
            );
            
            if (result.success) {
                ctx.json(response);
            } else {
                response = Map.of(
                    "success", false,
                    "error", result.error,
                    "startedAt", result.startedAt,
                    "completedAt", result.completedAt
                );
                ctx.status(500).json(response);
            }
            
        } catch (Exception e) {
            logger.error("Error during YAML to database migration", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "Migration failed: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/management/migration/export-database-to-yaml - Export database configurations to YAML format
     */
    public void exportDatabaseToYaml(Context ctx) {
        logger.info("Exporting database configurations to YAML format");
        
        try {
            ExportResult result = migrationService.exportDatabaseToYaml();
            
            if (result.success) {
                Map<String, Object> response = Map.of(
                    "success", true,
                    "exportedAt", result.exportedAt,
                    "counts", Map.of(
                        "databases", result.databaseCount,
                        "queries", result.queryCount,
                        "endpoints", result.endpointCount,
                        "total", result.databaseCount + result.queryCount + result.endpointCount
                    ),
                    "yaml", Map.of(
                        "databases", result.databasesYaml,
                        "queries", result.queriesYaml,
                        "endpoints", result.endpointsYaml
                    )
                );
                ctx.json(response);
            } else {
                ctx.status(500).json(Map.of(
                    "success", false,
                    "error", result.error,
                    "exportedAt", result.exportedAt
                ));
            }
            
        } catch (Exception e) {
            logger.error("Error during database to YAML export", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "Export failed: " + e.getMessage()
            ));
        }
    }

    // ========== SYNCHRONIZATION ENDPOINTS ==========

    /**
     * GET /api/management/migration/compare - Compare configurations between YAML and database
     */
    public void compareConfigurations(Context ctx) {
        logger.info("Comparing configurations between YAML and database");
        
        try {
            SynchronizationReport report = migrationService.compareConfigurations();
            
            if (report.success) {
                Map<String, Object> response = Map.of(
                    "success", true,
                    "comparedAt", report.comparedAt,
                    "comparison", Map.of(
                        "databases", Map.of(
                            "onlyInYaml", report.databaseComparison.onlyInYaml,
                            "onlyInDatabase", report.databaseComparison.onlyInDatabase,
                            "inBoth", report.databaseComparison.inBoth,
                            "summary", Map.of(
                                "yamlOnly", report.databaseComparison.onlyInYaml.size(),
                                "databaseOnly", report.databaseComparison.onlyInDatabase.size(),
                                "both", report.databaseComparison.inBoth.size()
                            )
                        ),
                        "queries", Map.of(
                            "onlyInYaml", report.queryComparison.onlyInYaml,
                            "onlyInDatabase", report.queryComparison.onlyInDatabase,
                            "inBoth", report.queryComparison.inBoth,
                            "summary", Map.of(
                                "yamlOnly", report.queryComparison.onlyInYaml.size(),
                                "databaseOnly", report.queryComparison.onlyInDatabase.size(),
                                "both", report.queryComparison.inBoth.size()
                            )
                        ),
                        "endpoints", Map.of(
                            "onlyInYaml", report.endpointComparison.onlyInYaml,
                            "onlyInDatabase", report.endpointComparison.onlyInDatabase,
                            "inBoth", report.endpointComparison.inBoth,
                            "summary", Map.of(
                                "yamlOnly", report.endpointComparison.onlyInYaml.size(),
                                "databaseOnly", report.endpointComparison.onlyInDatabase.size(),
                                "both", report.endpointComparison.inBoth.size()
                            )
                        )
                    )
                );
                ctx.json(response);
            } else {
                ctx.status(500).json(Map.of(
                    "success", false,
                    "error", report.error,
                    "comparedAt", report.comparedAt
                ));
            }
            
        } catch (Exception e) {
            logger.error("Error during configuration comparison", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "Comparison failed: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/management/migration/status - Get migration status and information
     */
    public void getMigrationStatus(Context ctx) {
        logger.debug("Getting migration status");
        
        try {
            Map<String, Object> status = migrationService.getMigrationStatus();
            ctx.json(status);
        } catch (Exception e) {
            logger.error("Error getting migration status", e);
            ctx.status(500).json(Map.of(
                "migrationAvailable", false,
                "error", "Failed to get migration status: " + e.getMessage()
            ));
        }
    }

    // ========== UTILITY ENDPOINTS ==========

    /**
     * GET /api/management/migration/yaml/databases - Get YAML database configurations as JSON
     */
    public void getYamlDatabaseConfigurations(Context ctx) {
        logger.debug("Getting YAML database configurations");
        
        try {
            ExportResult result = migrationService.exportDatabaseToYaml();
            if (result.success) {
                ctx.contentType("text/plain").result(result.databasesYaml);
            } else {
                ctx.status(500).json(Map.of("error", result.error));
            }
        } catch (Exception e) {
            logger.error("Error getting YAML database configurations", e);
            ctx.status(500).json(Map.of("error", "Failed to get YAML configurations: " + e.getMessage()));
        }
    }

    /**
     * GET /api/management/migration/yaml/queries - Get YAML query configurations as JSON
     */
    public void getYamlQueryConfigurations(Context ctx) {
        logger.debug("Getting YAML query configurations");
        
        try {
            ExportResult result = migrationService.exportDatabaseToYaml();
            if (result.success) {
                ctx.contentType("text/plain").result(result.queriesYaml);
            } else {
                ctx.status(500).json(Map.of("error", result.error));
            }
        } catch (Exception e) {
            logger.error("Error getting YAML query configurations", e);
            ctx.status(500).json(Map.of("error", "Failed to get YAML configurations: " + e.getMessage()));
        }
    }

    /**
     * GET /api/management/migration/yaml/endpoints - Get YAML endpoint configurations as JSON
     */
    public void getYamlEndpointConfigurations(Context ctx) {
        logger.debug("Getting YAML endpoint configurations");
        
        try {
            ExportResult result = migrationService.exportDatabaseToYaml();
            if (result.success) {
                ctx.contentType("text/plain").result(result.endpointsYaml);
            } else {
                ctx.status(500).json(Map.of("error", result.error));
            }
        } catch (Exception e) {
            logger.error("Error getting YAML endpoint configurations", e);
            ctx.status(500).json(Map.of("error", "Failed to get YAML configurations: " + e.getMessage()));
        }
    }
}

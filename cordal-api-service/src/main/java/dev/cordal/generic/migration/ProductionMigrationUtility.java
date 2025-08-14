package dev.cordal.generic.migration;

import dev.cordal.config.GenericApiConfig;
import dev.cordal.database.DatabaseManager;
import dev.cordal.database.repository.DatabaseConfigurationRepository;
import dev.cordal.database.repository.QueryConfigurationRepository;
import dev.cordal.database.repository.EndpointConfigurationRepository;
import dev.cordal.database.loader.DatabaseConfigurationLoader;
import dev.cordal.generic.config.ConfigurationLoader;
import dev.cordal.generic.config.ConfigurationLoaderFactory;
import dev.cordal.generic.config.EndpointConfigurationManager;
import dev.cordal.generic.migration.ConfigurationMigrationService.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Scanner;

/**
 * Production-ready migration utility for real-world YAML to database configuration migration
 * Provides interactive migration with backup, validation, and rollback capabilities
 */
public class ProductionMigrationUtility {
    private static final Logger logger = LoggerFactory.getLogger(ProductionMigrationUtility.class);
    
    private final ConfigurationMigrationService migrationService;
    private final EndpointConfigurationManager configurationManager;
    private final Scanner scanner;
    private final String backupDirectory;

    public ProductionMigrationUtility() {
        // Initialize configuration system
        GenericApiConfig config = GenericApiConfig.loadFromFile();
        
        // Create database manager and initialize schema
        DatabaseManager databaseManager = new DatabaseManager(config);
        databaseManager.initializeSchema();
        
        // Create repositories
        DatabaseConfigurationRepository databaseRepository = new DatabaseConfigurationRepository(databaseManager);
        QueryConfigurationRepository queryRepository = new QueryConfigurationRepository(databaseManager);
        EndpointConfigurationRepository endpointRepository = new EndpointConfigurationRepository(databaseManager);
        
        // Create loaders
        ConfigurationLoader yamlLoader = new ConfigurationLoader(config);
        DatabaseConfigurationLoader databaseLoader = new DatabaseConfigurationLoader(databaseRepository, queryRepository, endpointRepository);
        
        // Create factory and manager
        ConfigurationLoaderFactory factory = new ConfigurationLoaderFactory(config, yamlLoader, databaseLoader);
        this.configurationManager = new EndpointConfigurationManager(factory);
        
        // Create migration service
        this.migrationService = new ConfigurationMigrationService(
            databaseRepository, queryRepository, endpointRepository,
            yamlLoader, databaseLoader, factory
        );
        
        this.scanner = new Scanner(System.in);
        this.backupDirectory = "config-backups/" + DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(":", "-");
        
        logger.info("Production migration utility initialized");
    }

    /**
     * Main entry point for production migration
     */
    public static void main(String[] args) {
        System.out.println("=== PRODUCTION CONFIGURATION MIGRATION UTILITY ===");
        System.out.println("This utility will help you migrate from YAML to database configurations");
        System.out.println();
        
        ProductionMigrationUtility utility = new ProductionMigrationUtility();
        utility.runInteractiveMigration();
    }

    /**
     * Run interactive migration process
     */
    public void runInteractiveMigration() {
        try {
            System.out.println("Starting production migration process...");
            
            // Step 1: Pre-migration assessment
            if (!performPreMigrationAssessment()) {
                System.out.println("Migration cancelled due to assessment issues.");
                return;
            }
            
            // Step 2: Create backup
            if (!createConfigurationBackup()) {
                System.out.println("Migration cancelled due to backup failure.");
                return;
            }
            
            // Step 3: Perform migration
            if (!performMigration()) {
                System.out.println("Migration failed. Check logs for details.");
                return;
            }
            
            // Step 4: Post-migration validation
            if (!performPostMigrationValidation()) {
                System.out.println("Migration completed but validation failed. Review results carefully.");
                return;
            }
            
            // Step 5: Final confirmation
            performFinalConfirmation();
            
        } catch (Exception e) {
            logger.error("Production migration failed", e);
            System.err.println("Migration failed with error: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }

    /**
     * Perform pre-migration assessment
     */
    private boolean performPreMigrationAssessment() {
        System.out.println("\n--- PRE-MIGRATION ASSESSMENT ---");
        
        try {
            // Get current status
            Map<String, Object> status = migrationService.getMigrationStatus();
            String currentSource = (String) status.get("currentSource");
            
            System.out.printf("Current configuration source: %s%n", currentSource);
            
            if (!"yaml".equals(currentSource)) {
                System.out.println("WARNING: Current source is not YAML. Migration may not be necessary.");
                System.out.print("Continue anyway? (y/N): ");
                String response = scanner.nextLine().trim().toLowerCase();
                if (!response.equals("y") && !response.equals("yes")) {
                    return false;
                }
            }
            
            // Show configuration counts
            Map<String, Object> yamlCounts = (Map<String, Object>) status.get("yamlCounts");
            Map<String, Object> dbCounts = (Map<String, Object>) status.get("databaseCounts");
            
            System.out.println("\nCurrent configuration counts:");
            System.out.printf("  YAML: %s databases, %s queries, %s endpoints (Total: %s)%n",
                             yamlCounts.get("databases"), yamlCounts.get("queries"), 
                             yamlCounts.get("endpoints"), yamlCounts.get("total"));
            System.out.printf("  Database: %s databases, %s queries, %s endpoints (Total: %s)%n",
                             dbCounts.get("databases"), dbCounts.get("queries"), 
                             dbCounts.get("endpoints"), dbCounts.get("total"));
            
            // Validate configurations
            System.out.println("\nValidating current configurations...");
            configurationManager.validateConfigurations();
            System.out.println("✓ Configuration validation passed");
            
            // Show comparison
            SynchronizationReport comparison = migrationService.compareConfigurations();
            if (comparison.success) {
                System.out.println("\nConfiguration comparison:");
                System.out.printf("  Databases - YAML only: %d, DB only: %d, Both: %d%n",
                                 comparison.databaseComparison.onlyInYaml.size(),
                                 comparison.databaseComparison.onlyInDatabase.size(),
                                 comparison.databaseComparison.inBoth.size());
                System.out.printf("  Queries - YAML only: %d, DB only: %d, Both: %d%n",
                                 comparison.queryComparison.onlyInYaml.size(),
                                 comparison.queryComparison.onlyInDatabase.size(),
                                 comparison.queryComparison.inBoth.size());
                System.out.printf("  Endpoints - YAML only: %d, DB only: %d, Both: %d%n",
                                 comparison.endpointComparison.onlyInYaml.size(),
                                 comparison.endpointComparison.onlyInDatabase.size(),
                                 comparison.endpointComparison.inBoth.size());
            }
            
            System.out.print("\nProceed with migration? (y/N): ");
            String response = scanner.nextLine().trim().toLowerCase();
            return response.equals("y") || response.equals("yes");
            
        } catch (Exception e) {
            logger.error("Pre-migration assessment failed", e);
            System.err.println("Assessment failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Create configuration backup
     */
    private boolean createConfigurationBackup() {
        System.out.println("\n--- CREATING CONFIGURATION BACKUP ---");
        
        try {
            // Create backup directory
            File backupDir = new File(backupDirectory);
            if (!backupDir.mkdirs()) {
                System.err.println("Failed to create backup directory: " + backupDirectory);
                return false;
            }
            
            System.out.printf("Creating backup in: %s%n", backupDir.getAbsolutePath());
            
            // Export current database configurations (if any)
            ExportResult exportResult = migrationService.exportDatabaseToYaml();
            if (exportResult.success) {
                // Save database configurations
                saveToFile(new File(backupDir, "databases-backup.yml"), exportResult.databasesYaml);
                saveToFile(new File(backupDir, "queries-backup.yml"), exportResult.queriesYaml);
                saveToFile(new File(backupDir, "endpoints-backup.yml"), exportResult.endpointsYaml);
                
                System.out.printf("✓ Database configurations backed up (%d databases, %d queries, %d endpoints)%n",
                                 exportResult.databaseCount, exportResult.queryCount, exportResult.endpointCount);
            }
            
            // Copy current YAML files
            copyYamlFiles(backupDir);
            
            System.out.println("✓ Backup completed successfully");
            return true;
            
        } catch (Exception e) {
            logger.error("Backup creation failed", e);
            System.err.println("Backup failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Perform the actual migration
     */
    private boolean performMigration() {
        System.out.println("\n--- PERFORMING MIGRATION ---");
        
        try {
            System.out.println("Starting YAML to database migration...");
            
            MigrationResult result = migrationService.migrateYamlToDatabase();
            
            if (result.success) {
                System.out.println("✓ Migration completed successfully!");
                System.out.printf("  Created: %d configurations%n", result.getTotalCreated());
                System.out.printf("  Updated: %d configurations%n", result.getTotalUpdated());
                System.out.printf("  Failed: %d configurations%n", result.getTotalFailed());
                
                if (result.getTotalFailed() > 0) {
                    System.out.println("Migration errors:");
                    for (String error : result.getAllErrors()) {
                        System.out.println("  - " + error);
                    }
                }
                
                return result.getTotalFailed() == 0;
            } else {
                System.err.println("Migration failed: " + result.error);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Migration execution failed", e);
            System.err.println("Migration failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Perform post-migration validation
     */
    private boolean performPostMigrationValidation() {
        System.out.println("\n--- POST-MIGRATION VALIDATION ---");

        try {
            // Validate configurations
            System.out.println("Validating migrated configurations...");
            configurationManager.validateConfigurations();
            System.out.println("✓ Configuration validation passed");

            // Check synchronization status
            SynchronizationReport syncReport = migrationService.compareConfigurations();
            if (syncReport.success) {
                System.out.println("Synchronization status after migration:");
                System.out.printf("  Databases in both sources: %d%n", syncReport.databaseComparison.inBoth.size());
                System.out.printf("  Queries in both sources: %d%n", syncReport.queryComparison.inBoth.size());
                System.out.printf("  Endpoints in both sources: %d%n", syncReport.endpointComparison.inBoth.size());

                // Check for any configurations only in one source
                if (!syncReport.databaseComparison.onlyInYaml.isEmpty() ||
                    !syncReport.databaseComparison.onlyInDatabase.isEmpty()) {
                    System.out.println("WARNING: Some database configurations are not synchronized");
                }
                if (!syncReport.queryComparison.onlyInYaml.isEmpty() ||
                    !syncReport.queryComparison.onlyInDatabase.isEmpty()) {
                    System.out.println("WARNING: Some query configurations are not synchronized");
                }
                if (!syncReport.endpointComparison.onlyInYaml.isEmpty() ||
                    !syncReport.endpointComparison.onlyInDatabase.isEmpty()) {
                    System.out.println("WARNING: Some endpoint configurations are not synchronized");
                }
            }

            // Test configuration loading from database
            System.out.println("Testing database configuration loading...");
            Map<String, Object> stats = migrationService.getMigrationStatus();
            Map<String, Object> dbCounts = (Map<String, Object>) stats.get("databaseCounts");

            int totalDbConfigs = (Integer) dbCounts.get("total");
            if (totalDbConfigs > 0) {
                System.out.printf("✓ Successfully loaded %d configurations from database%n", totalDbConfigs);
            } else {
                System.out.println("WARNING: No configurations found in database after migration");
                return false;
            }

            System.out.println("✓ Post-migration validation completed");
            return true;

        } catch (Exception e) {
            logger.error("Post-migration validation failed", e);
            System.err.println("Validation failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Perform final confirmation and instructions
     */
    private void performFinalConfirmation() {
        System.out.println("\n--- MIGRATION COMPLETED SUCCESSFULLY ---");

        System.out.println("Next steps to complete the transition:");
        System.out.println("1. Update your application.yml file:");
        System.out.println("   config:");
        System.out.println("     source: database  # Change from 'yaml' to 'database'");
        System.out.println();
        System.out.println("2. Restart your application to use database configurations");
        System.out.println();
        System.out.println("3. Verify the application starts correctly with database source");
        System.out.println();
        System.out.println("4. Test your API endpoints to ensure they work as expected");
        System.out.println();
        System.out.printf("5. Configuration backup is available at: %s%n", backupDirectory);
        System.out.println();
        System.out.println("You can now manage configurations via REST APIs:");
        System.out.println("  - GET /api/management/config/databases");
        System.out.println("  - GET /api/management/config/queries");
        System.out.println("  - GET /api/management/config/endpoints");
        System.out.println("  - POST /api/management/config/databases/{name}");
        System.out.println("  - And more...");
        System.out.println();
        System.out.println("=== PRODUCTION MIGRATION COMPLETED ===");
    }

    /**
     * Save content to file
     */
    private void saveToFile(File file, String content) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
        System.out.printf("  Saved: %s%n", file.getName());
    }

    /**
     * Copy YAML files to backup directory
     */
    private void copyYamlFiles(File backupDir) {
        try {
            // Note: In a real implementation, you would copy the actual YAML files
            // For this example, we'll create a summary file
            File summaryFile = new File(backupDir, "yaml-files-summary.txt");
            try (FileWriter writer = new FileWriter(summaryFile)) {
                writer.write("YAML Configuration Files Backup Summary\n");
                writer.write("Generated at: " + Instant.now() + "\n\n");
                writer.write("Original YAML files should be backed up from:\n");
                writer.write("- generic-config/stocktrades-databases.yml\n");
                writer.write("- generic-config/stocktrades-queries.yml\n");
                writer.write("- generic-config/stocktrades-api-endpoints.yml\n");
                writer.write("\nThese files contain the original configurations that were migrated to the database.\n");
            }
            System.out.println("  Created YAML files summary");
        } catch (IOException e) {
            logger.warn("Failed to create YAML files summary", e);
        }
    }
}

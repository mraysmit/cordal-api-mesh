package dev.cordal.generic.migration;

import dev.cordal.config.GenericApiConfig;
import dev.cordal.database.DatabaseManager;
import dev.cordal.database.repository.DatabaseConfigurationRepository;
import dev.cordal.database.repository.QueryConfigurationRepository;
import dev.cordal.database.repository.EndpointConfigurationRepository;
import dev.cordal.database.loader.DatabaseConfigurationLoader;
import dev.cordal.generic.config.ConfigurationLoader;
import dev.cordal.generic.config.ConfigurationLoaderFactory;
import dev.cordal.generic.migration.ConfigurationMigrationService.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Test configuration migration and synchronization functionality
 */
class ConfigurationMigrationTest {

    private ConfigurationMigrationService migrationService;
    private DatabaseManager databaseManager;
    private GenericApiConfig config;

    @BeforeEach
    void setUp() {
        // Create test configuration
        System.setProperty("generic.config.file", "application-test.yml");
        config = GenericApiConfig.loadFromFile();
        
        // Create database manager and initialize schema
        databaseManager = new DatabaseManager(config);
        databaseManager.initializeSchema();
        
        // Create repositories
        DatabaseConfigurationRepository databaseRepository = new DatabaseConfigurationRepository(databaseManager);
        QueryConfigurationRepository queryRepository = new QueryConfigurationRepository(databaseManager);
        EndpointConfigurationRepository endpointRepository = new EndpointConfigurationRepository(databaseManager);
        
        // Create loaders
        ConfigurationLoader yamlLoader = new ConfigurationLoader(config);
        DatabaseConfigurationLoader databaseLoader = new DatabaseConfigurationLoader(databaseRepository, queryRepository, endpointRepository);
        
        // Create factory
        ConfigurationLoaderFactory factory = new ConfigurationLoaderFactory(config, yamlLoader, databaseLoader);
        
        // Create migration service
        migrationService = new ConfigurationMigrationService(
            databaseRepository, queryRepository, endpointRepository,
            yamlLoader, databaseLoader, factory
        );
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("generic.config.file");
    }

    @Test
    void testGetMigrationStatus() {
        // Test getting migration status
        Map<String, Object> status = migrationService.getMigrationStatus();
        
        assertThat(status).isNotNull();
        assertThat(status).containsKey("currentSource");
        assertThat(status).containsKey("migrationAvailable");
        assertThat(status).containsKey("yamlCounts");
        assertThat(status).containsKey("databaseCounts");
        assertThat(status).containsKey("timestamp");
        
        // Should be YAML source by default in test
        assertThat(status.get("currentSource")).isEqualTo("yaml");
        assertThat(status.get("migrationAvailable")).isEqualTo(true);
        
        // Check counts structure
        Map<String, Object> yamlCounts = (Map<String, Object>) status.get("yamlCounts");
        assertThat(yamlCounts).containsKey("databases");
        assertThat(yamlCounts).containsKey("queries");
        assertThat(yamlCounts).containsKey("endpoints");
        assertThat(yamlCounts).containsKey("total");
        
        Map<String, Object> databaseCounts = (Map<String, Object>) status.get("databaseCounts");
        assertThat(databaseCounts).containsKey("databases");
        assertThat(databaseCounts).containsKey("queries");
        assertThat(databaseCounts).containsKey("endpoints");
        assertThat(databaseCounts).containsKey("total");
    }

    @Test
    void testMigrateYamlToDatabase() {
        // Test migrating YAML configurations to database
        MigrationResult result = migrationService.migrateYamlToDatabase();
        
        assertThat(result).isNotNull();
        assertThat(result.sourceType).isEqualTo("YAML");
        assertThat(result.targetType).isEqualTo("DATABASE");
        assertThat(result.success).isTrue();
        assertThat(result.startedAt).isNotNull();
        assertThat(result.completedAt).isNotNull();
        
        // Check migration results
        assertThat(result.databaseResults).isNotNull();
        assertThat(result.queryResults).isNotNull();
        assertThat(result.endpointResults).isNotNull();
        
        // Should have migrated some configurations from test YAML files
        int totalMigrated = result.getTotalCreated() + result.getTotalUpdated();
        assertThat(totalMigrated).isGreaterThan(0);
        
        // Should have no failures for valid test configurations
        assertThat(result.getTotalFailed()).isEqualTo(0);
        assertThat(result.getAllErrors()).isEmpty();
    }

    @Test
    void testExportDatabaseToYaml() {
        // First migrate some data to database
        migrationService.migrateYamlToDatabase();
        
        // Test exporting database configurations to YAML
        ExportResult result = migrationService.exportDatabaseToYaml();
        
        assertThat(result).isNotNull();
        assertThat(result.success).isTrue();
        assertThat(result.exportedAt).isNotNull();
        
        // Check counts
        assertThat(result.databaseCount).isGreaterThanOrEqualTo(0);
        assertThat(result.queryCount).isGreaterThanOrEqualTo(0);
        assertThat(result.endpointCount).isGreaterThanOrEqualTo(0);
        
        // Check YAML content
        assertThat(result.databasesYaml).isNotNull();
        assertThat(result.queriesYaml).isNotNull();
        assertThat(result.endpointsYaml).isNotNull();
        
        // YAML should contain proper headers
        assertThat(result.databasesYaml).contains("# Database Configurations (exported from database)");
        assertThat(result.queriesYaml).contains("# Query Configurations (exported from database)");
        assertThat(result.endpointsYaml).contains("# Endpoint Configurations (exported from database)");
        
        // YAML should contain databases section
        assertThat(result.databasesYaml).contains("databases:");
        assertThat(result.queriesYaml).contains("queries:");
        assertThat(result.endpointsYaml).contains("endpoints:");
    }

    @Test
    void testCompareConfigurations() {
        // Test comparing configurations between YAML and database
        SynchronizationReport report = migrationService.compareConfigurations();

        assertThat(report).isNotNull();
        assertThat(report.success).isTrue();
        assertThat(report.comparedAt).isNotNull();

        // Check comparison results
        assertThat(report.databaseComparison).isNotNull();
        assertThat(report.queryComparison).isNotNull();
        assertThat(report.endpointComparison).isNotNull();

        // Initially, database might be empty or might have data from previous tests
        // The important thing is that the comparison works and returns valid data
        assertThat(report.databaseComparison.onlyInYaml).isNotNull();
        assertThat(report.databaseComparison.onlyInDatabase).isNotNull();
        assertThat(report.databaseComparison.inBoth).isNotNull();

        // After migration, should be in both
        migrationService.migrateYamlToDatabase();
        SynchronizationReport reportAfterMigration = migrationService.compareConfigurations();

        assertThat(reportAfterMigration.success).isTrue();
        // After migration, configurations should be in both sources
        assertThat(reportAfterMigration.databaseComparison.inBoth).isNotEmpty();
        assertThat(reportAfterMigration.queryComparison.inBoth).isNotEmpty();
        assertThat(reportAfterMigration.endpointComparison.inBoth).isNotEmpty();
    }

    @Test
    void testYamlExportFormat() {
        // First migrate some data to database
        migrationService.migrateYamlToDatabase();
        
        // Test YAML export format
        ExportResult result = migrationService.exportDatabaseToYaml();
        
        assertThat(result.success).isTrue();
        
        // Check database YAML format
        String databasesYaml = result.databasesYaml;
        assertThat(databasesYaml).contains("databases:");
        assertThat(databasesYaml).contains("name:");
        assertThat(databasesYaml).contains("url:");
        assertThat(databasesYaml).contains("driver:");
        
        // Check query YAML format
        String queriesYaml = result.queriesYaml;
        assertThat(queriesYaml).contains("queries:");
        assertThat(queriesYaml).contains("name:");
        assertThat(queriesYaml).contains("database:");
        assertThat(queriesYaml).contains("sql:");
        assertThat(queriesYaml).contains("parameters:");
        
        // Check endpoint YAML format
        String endpointsYaml = result.endpointsYaml;
        assertThat(endpointsYaml).contains("endpoints:");
        assertThat(endpointsYaml).contains("path:");
        assertThat(endpointsYaml).contains("method:");
        assertThat(endpointsYaml).contains("query:");
    }

    @Test
    void testMigrationResultSummary() {
        // Test migration result summary calculations
        MigrationResult result = migrationService.migrateYamlToDatabase();
        
        assertThat(result.success).isTrue();
        
        // Test summary calculations
        int totalCreated = result.getTotalCreated();
        int totalUpdated = result.getTotalUpdated();
        int totalFailed = result.getTotalFailed();
        
        assertThat(totalCreated).isEqualTo(
            result.databaseResults.created + result.queryResults.created + result.endpointResults.created
        );
        assertThat(totalUpdated).isEqualTo(
            result.databaseResults.updated + result.queryResults.updated + result.endpointResults.updated
        );
        assertThat(totalFailed).isEqualTo(
            result.databaseResults.failed + result.queryResults.failed + result.endpointResults.failed
        );
        
        // All errors should be collected
        assertThat(result.getAllErrors()).hasSize(totalFailed);
    }
}

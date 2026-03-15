package dev.cordal.hotreload;

import dev.cordal.config.GenericApiConfig;
import dev.cordal.generic.config.ApiEndpointConfig;
import dev.cordal.generic.config.QueryConfig;
import dev.cordal.generic.config.DatabaseConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for Phase 2 hot reload core logic components
 */
class HotReloadPhase2Test {

    private TestGenericApiConfig config;
    
    private FileWatcherService fileWatcher;
    private ConfigurationStateManager stateManager;
    private ValidationPipeline validationPipeline;
    private DynamicEndpointRegistry endpointRegistry;
    private AtomicUpdateManager atomicUpdateManager;
    private ConfigurationReloadManager reloadManager;

    @BeforeEach
    void setUp() {
        // Initialize components
        config = new TestGenericApiConfig();
        fileWatcher = new FileWatcherService();
        stateManager = new ConfigurationStateManager();
        validationPipeline = new ValidationPipeline(stateManager);
        endpointRegistry = new DynamicEndpointRegistry();
        atomicUpdateManager = new AtomicUpdateManager(endpointRegistry);

        reloadManager = new ConfigurationReloadManager(
            fileWatcher, stateManager, validationPipeline,
            atomicUpdateManager, config
        );
    }

    @Test
    void shouldInitializeReloadManagerSuccessfully() {
        System.out.println("\n🧪 Test: ConfigurationReloadManager Initialization");
        
        // Initialize the reload manager
        reloadManager.initialize();
        
        // Verify status
        ReloadStatusInfo status = reloadManager.getStatus();
        assertThat(status.isEnabled()).isTrue();
        assertThat(status.getStatus()).isEqualTo(ConfigurationReloadManager.ReloadStatus.WATCHING);
        assertThat(status.isHealthy()).isTrue();
        
        System.out.println("✅ Reload manager initialized successfully");
        System.out.println("   Status: " + status);
        
        // Cleanup
        reloadManager.shutdown();
    }

    @Test
    void shouldValidateConfigurationChanges() {
        System.out.println("\n🧪 Test: Configuration Validation Pipeline");
        
        // Create test configuration delta
        ConfigurationDelta delta = createTestDelta();
        ConfigurationSet newConfig = createTestConfigurationSet();
        
        // Run validation
        ValidationResult result = validationPipeline.validate(delta, newConfig);
        
        // Verify validation results
        assertThat(result).isNotNull();
        assertThat(result.getStageResults()).isNotEmpty();
        
        System.out.println("✅ Validation pipeline completed");
        System.out.println("   Result: " + result);
        System.out.println("   Stages: " + result.getStageResults().keySet());
        
        // Check individual stages
        assertThat(result.getStageResult("syntax")).isNotNull();
        assertThat(result.getStageResult("dependencies")).isNotNull();
        assertThat(result.getStageResult("connectivity")).isNotNull();
        assertThat(result.getStageResult("endpoints")).isNotNull();
        
        System.out.println("   All validation stages executed");
    }

    @Test
    void shouldManageDynamicEndpoints() {
        System.out.println("\n🧪 Test: Dynamic Endpoint Registry");

        // Note: Actual endpoint registration requires Javalin to be initialized
        // We test the registry capabilities without Javalin for unit testing

        // Get initial statistics
        EndpointRegistryStatistics initialStats = endpointRegistry.getStatistics();
        assertThat(initialStats.getTotalEndpoints()).isEqualTo(0);

        System.out.println("✅ Initial registry state verified");
        System.out.println("   • Total endpoints: " + initialStats.getTotalEndpoints());
        System.out.println("   • Active endpoints: " + initialStats.getActiveEndpoints());

        // Test atomic update operations
        boolean atomicStarted = endpointRegistry.beginAtomicUpdate();
        assertThat(atomicStarted).isTrue();

        System.out.println("✅ Atomic update operation started");
        
        endpointRegistry.commitAtomicUpdate();
        System.out.println("✅ Atomic update operation committed");

        // Test validation
        EndpointValidationResult validation = endpointRegistry.validateAllEndpoints();
        assertThat(validation).isNotNull();

        System.out.println("✅ Endpoint validation completed");
        System.out.println("   • Total endpoints validated: " + validation.getTotalEndpoints());
        System.out.println("   • Valid endpoints: " + validation.getValidCount());
        System.out.println("   • All valid: " + validation.isAllValid());

        // Test rollback capability
        boolean rollbackStarted = endpointRegistry.beginAtomicUpdate();
        assertThat(rollbackStarted).isTrue();

        endpointRegistry.rollbackAtomicUpdate();
        System.out.println("✅ Atomic update rollback completed");
    }

    @Test
    void shouldPerformAtomicUpdates() {
        System.out.println("\n🧪 Test: Atomic Update Manager");
        
        // Create test configuration changes
        ConfigurationDelta delta = createTestDelta();
        ConfigurationSet newConfig = createTestConfigurationSet();
        
        // Verify no update in progress initially
        assertThat(atomicUpdateManager.isUpdateInProgress()).isFalse();
        
        // Perform atomic update
        AtomicUpdateResult result = atomicUpdateManager.applyChanges(delta, newConfig);
        
        // Verify update completed
        assertThat(atomicUpdateManager.isUpdateInProgress()).isFalse();
        assertThat(result).isNotNull();
        assertThat(result.getUpdateId()).isNotNull();
        
        System.out.println("✅ Atomic update completed");
        System.out.println("   Result: " + result);
        System.out.println("   Update ID: " + result.getUpdateId());
        
        if (result.getDatabaseResult() != null) {
            System.out.println("   Database changes: " + result.getDatabaseResult());
        }
        
        if (result.getEndpointResult() != null) {
            System.out.println("   Endpoint changes: " + result.getEndpointResult());
        }
        
        // Get statistics
        AtomicUpdateStatistics stats = atomicUpdateManager.getStatistics();
        System.out.println("   Update statistics: " + stats);
    }

    @Test
    void shouldHandleManualReloadRequest() {
        System.out.println("\n🧪 Test: Manual Reload Request");
        
        // Initialize reload manager
        reloadManager.initialize();
        
        try {
            // Create manual reload request
            ReloadRequest request = ReloadRequest.manual();
            
            System.out.println("   Triggering manual reload: " + request);
            
            // Trigger reload
            ReloadResult result = reloadManager.triggerReload(request);
            
            assertThat(result).isNotNull();
            System.out.println("✅ Manual reload completed");
            System.out.println("   Result: " + result);
            
            // Verify status after reload
            ReloadStatusInfo status = reloadManager.getStatus();
            System.out.println("   Status after reload: " + status);
            
        } finally {
            reloadManager.shutdown();
        }
    }

    @Test
    void shouldHandleValidationOnlyRequest() {
        System.out.println("\n🧪 Test: Validation-Only Request");
        
        // Initialize reload manager
        reloadManager.initialize();
        
        try {
            // Create validation-only request
            ReloadRequest request = ReloadRequest.validationOnly();
            
            System.out.println("   Triggering validation-only reload: " + request);
            
            // Trigger validation
            ReloadResult result = reloadManager.triggerReload(request);
            
            assertThat(result).isNotNull();
            System.out.println("✅ Validation-only reload completed");
            System.out.println("   Result: " + result);
            
        } finally {
            reloadManager.shutdown();
        }
    }

    @Test
    void shouldProvideComprehensiveStatus() {
        System.out.println("\n🧪 Test: Comprehensive Status Information");
        
        // Initialize reload manager
        reloadManager.initialize();
        
        try {
            // Get comprehensive status
            ReloadStatusInfo status = reloadManager.getStatus();
            
            assertThat(status).isNotNull();
            assertThat(status.isEnabled()).isTrue();
            assertThat(status.getFileWatcherStatus()).isNotNull();
            assertThat(status.getStateStatistics()).isNotNull();
            
            System.out.println("✅ Comprehensive status retrieved");
            System.out.println("   Enabled: " + status.isEnabled());
            System.out.println("   Status: " + status.getStatus());
            System.out.println("   Healthy: " + status.isHealthy());
            System.out.println("   Reload attempts: " + status.getTotalReloadAttempts());
            System.out.println("   File watcher: " + status.getFileWatcherStatus());
            System.out.println("   State statistics: " + status.getStateStatistics());
            
        } finally {
            reloadManager.shutdown();
        }
    }

    // Helper methods
    private ConfigurationDelta createTestDelta() {
        ConfigurationDelta delta = new ConfigurationDelta();
        
        // Add test database
        DatabaseConfig dbConfig = createTestDatabaseConfig("test_db", "jdbc:h2:mem:test");
        delta.addedDatabases.put("test_db", dbConfig);
        
        // Add test query
        QueryConfig queryConfig = createTestQueryConfig("test_query", "test_db", "SELECT * FROM test_table");
        delta.addedQueries.put("test_query", queryConfig);
        
        // Add test endpoint
        ApiEndpointConfig endpointConfig = createTestEndpointConfig("test_endpoint", "/api/test", "test_query");
        delta.addedEndpoints.put("test_endpoint", endpointConfig);
        
        return delta;
    }
    
    private ConfigurationSet createTestConfigurationSet() {
        Map<String, DatabaseConfig> databases = new HashMap<>();
        databases.put("test_db", createTestDatabaseConfig("test_db", "jdbc:h2:mem:test"));
        
        Map<String, QueryConfig> queries = new HashMap<>();
        queries.put("test_query", createTestQueryConfig("test_query", "test_db", "SELECT * FROM test_table"));
        
        Map<String, ApiEndpointConfig> endpoints = new HashMap<>();
        endpoints.put("test_endpoint", createTestEndpointConfig("test_endpoint", "/api/test", "test_query"));
        
        return new ConfigurationSet(databases, queries, endpoints);
    }
    
    private DatabaseConfig createTestDatabaseConfig(String name, String url) {
        DatabaseConfig config = new DatabaseConfig();
        config.setName(name);
        config.setUrl(url);
        config.setUsername("sa");
        config.setPassword("");
        config.setDriver("org.h2.Driver");
        return config;
    }
    
    private QueryConfig createTestQueryConfig(String name, String database, String sql) {
        QueryConfig config = new QueryConfig();
        config.setName(name);
        config.setDatabase(database);
        config.setSql(sql);
        return config;
    }
    
    private ApiEndpointConfig createTestEndpointConfig(String name, String path, String query) {
        ApiEndpointConfig config = new ApiEndpointConfig();
        config.setPath(path);
        config.setMethod("GET");
        config.setQuery(query);
        return config;
    }

    // Test implementations
    private static class TestGenericApiConfig extends GenericApiConfig {
        @Override
        public boolean isHotReloadEnabled() { return true; }

        @Override
        public boolean isHotReloadWatchDirectories() { return false; }

        @Override
        public long getHotReloadDebounceMs() { return 100L; }

        @Override
        public int getHotReloadMaxAttempts() { return 3; }

        @Override
        public boolean isHotReloadRollbackOnFailure() { return true; }

        @Override
        public boolean isHotReloadValidateBeforeApply() { return true; }
    }

}

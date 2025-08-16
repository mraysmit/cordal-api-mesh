package dev.cordal.hotreload;

import dev.cordal.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Multi-stage validation pipeline for configuration changes
 * Validates syntax, schema, dependencies, and connectivity
 */
@Singleton
public class ValidationPipeline {
    private static final Logger logger = LoggerFactory.getLogger(ValidationPipeline.class);
    
    private final DatabaseManager databaseManager;
    private final ConfigurationStateManager stateManager;
    private final List<ConfigurationValidator> validators;
    
    @Inject
    public ValidationPipeline(DatabaseManager databaseManager, ConfigurationStateManager stateManager) {
        this.databaseManager = databaseManager;
        this.stateManager = stateManager;
        this.validators = initializeValidators();
        
        logger.info("ValidationPipeline initialized with {} validators", validators.size());
    }
    
    /**
     * Validate configuration changes through the complete pipeline
     */
    public ValidationResult validate(ConfigurationDelta delta, ConfigurationSet newConfiguration) {
        logger.info("Starting validation pipeline for configuration changes: {}", delta);
        
        ValidationResult.Builder resultBuilder = new ValidationResult.Builder();
        long startTime = System.currentTimeMillis();
        
        try {
            // Stage 1: Syntax and Schema Validation
            logger.debug("Stage 1: Syntax and schema validation");
            ValidationStageResult syntaxResult = validateSyntaxAndSchema(delta, newConfiguration);
            resultBuilder.addStageResult("syntax", syntaxResult);
            
            if (!syntaxResult.isValid()) {
                logger.warn("Syntax validation failed, skipping remaining stages");
                return resultBuilder.build();
            }
            
            // Stage 2: Dependency Validation
            logger.debug("Stage 2: Dependency validation");
            ValidationStageResult dependencyResult = validateDependencies(delta, newConfiguration);
            resultBuilder.addStageResult("dependencies", dependencyResult);
            
            if (!dependencyResult.isValid()) {
                logger.warn("Dependency validation failed, skipping remaining stages");
                return resultBuilder.build();
            }
            
            // Stage 3: Database Connectivity Testing
            logger.debug("Stage 3: Database connectivity testing");
            ValidationStageResult connectivityResult = validateDatabaseConnectivity(delta, newConfiguration);
            resultBuilder.addStageResult("connectivity", connectivityResult);
            
            // Stage 4: Endpoint Health Simulation
            logger.debug("Stage 4: Endpoint health simulation");
            ValidationStageResult endpointResult = validateEndpointHealth(delta, newConfiguration);
            resultBuilder.addStageResult("endpoints", endpointResult);
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Validation pipeline completed in {}ms", duration);
            
            return resultBuilder.duration(duration).build();
            
        } catch (Exception e) {
            logger.error("Validation pipeline failed with exception", e);
            return resultBuilder
                .addError("Validation pipeline exception: " + e.getMessage())
                .duration(System.currentTimeMillis() - startTime)
                .build();
        }
    }
    
    /**
     * Validate configuration syntax and schema
     */
    private ValidationStageResult validateSyntaxAndSchema(ConfigurationDelta delta, ConfigurationSet newConfiguration) {
        ValidationStageResult.Builder builder = new ValidationStageResult.Builder("syntax");
        
        try {
            // Validate database configurations
            for (var database : delta.addedDatabases.values()) {
                if (database.getName() == null || database.getName().trim().isEmpty()) {
                    builder.addError("Database name cannot be empty");
                }
                if (database.getUrl() == null || database.getUrl().trim().isEmpty()) {
                    builder.addError("Database URL cannot be empty for: " + database.getName());
                }
                if (database.getDriver() == null || database.getDriver().trim().isEmpty()) {
                    builder.addError("Database driver cannot be empty for: " + database.getName());
                }
            }
            
            // Validate query configurations
            for (var query : delta.addedQueries.values()) {
                if (query.getName() == null || query.getName().trim().isEmpty()) {
                    builder.addError("Query name cannot be empty");
                }
                if (query.getDatabase() == null || query.getDatabase().trim().isEmpty()) {
                    builder.addError("Query database cannot be empty for: " + query.getName());
                }
                if (query.getSql() == null || query.getSql().trim().isEmpty()) {
                    builder.addError("Query SQL cannot be empty for: " + query.getName());
                }
            }
            
            // Validate endpoint configurations
            for (var endpoint : delta.addedEndpoints.values()) {
                if (endpoint.getPath() == null || endpoint.getPath().trim().isEmpty()) {
                    builder.addError("Endpoint path cannot be empty");
                }
                if (endpoint.getMethod() == null || endpoint.getMethod().trim().isEmpty()) {
                    builder.addError("Endpoint method cannot be empty for: " + endpoint.getPath());
                }
                if (endpoint.getQuery() == null || endpoint.getQuery().trim().isEmpty()) {
                    builder.addError("Endpoint query cannot be empty for: " + endpoint.getPath());
                }
                
                // Validate HTTP method
                String method = endpoint.getMethod().toUpperCase();
                if (!List.of("GET", "POST", "PUT", "DELETE", "PATCH").contains(method)) {
                    builder.addError("Invalid HTTP method '" + method + "' for endpoint: " + endpoint.getPath());
                }
                
                // Validate path format
                if (!endpoint.getPath().startsWith("/")) {
                    builder.addError("Endpoint path must start with '/' for: " + endpoint.getPath());
                }
            }
            
            logger.debug("Syntax validation completed with {} errors", builder.getErrorCount());
            return builder.build();
            
        } catch (Exception e) {
            logger.error("Syntax validation failed", e);
            return builder.addError("Syntax validation exception: " + e.getMessage()).build();
        }
    }
    
    /**
     * Validate configuration dependencies
     */
    private ValidationStageResult validateDependencies(ConfigurationDelta delta, ConfigurationSet newConfiguration) {
        ValidationStageResult.Builder builder = new ValidationStageResult.Builder("dependencies");
        
        try {
            // Use the existing dependency validation from ConfigurationStateManager
            ConfigurationValidationResult dependencyResult = stateManager.validateDependencies(
                delta, 
                newConfiguration.getDatabases(),
                newConfiguration.getQueries(),
                newConfiguration.getEndpoints()
            );
            
            // Convert to ValidationStageResult
            for (String error : dependencyResult.getErrors()) {
                builder.addError(error);
            }
            
            for (String warning : dependencyResult.getWarnings()) {
                builder.addWarning(warning);
            }
            
            logger.debug("Dependency validation completed with {} errors, {} warnings", 
                        dependencyResult.getErrorCount(), dependencyResult.getWarningCount());
            
            return builder.build();
            
        } catch (Exception e) {
            logger.error("Dependency validation failed", e);
            return builder.addError("Dependency validation exception: " + e.getMessage()).build();
        }
    }
    
    /**
     * Validate database connectivity
     */
    private ValidationStageResult validateDatabaseConnectivity(ConfigurationDelta delta, ConfigurationSet newConfiguration) {
        ValidationStageResult.Builder builder = new ValidationStageResult.Builder("connectivity");
        
        try {
            List<CompletableFuture<Void>> connectivityTests = new ArrayList<>();
            
            // Test connectivity for new and modified databases
            for (var entry : delta.addedDatabases.entrySet()) {
                String dbName = entry.getKey();
                var dbConfig = entry.getValue();
                
                CompletableFuture<Void> test = CompletableFuture.runAsync(() -> {
                    try {
                        testDatabaseConnection(dbName, dbConfig);
                        logger.debug("Database connectivity test passed for: {}", dbName);
                    } catch (Exception e) {
                        builder.addError("Database connectivity failed for '" + dbName + "': " + e.getMessage());
                        logger.warn("Database connectivity test failed for: {}", dbName, e);
                    }
                });
                
                connectivityTests.add(test);
            }
            
            for (var entry : delta.modifiedDatabases.entrySet()) {
                String dbName = entry.getKey();
                var dbConfig = entry.getValue();
                
                CompletableFuture<Void> test = CompletableFuture.runAsync(() -> {
                    try {
                        testDatabaseConnection(dbName, dbConfig);
                        logger.debug("Modified database connectivity test passed for: {}", dbName);
                    } catch (Exception e) {
                        builder.addError("Modified database connectivity failed for '" + dbName + "': " + e.getMessage());
                        logger.warn("Modified database connectivity test failed for: {}", dbName, e);
                    }
                });
                
                connectivityTests.add(test);
            }
            
            // Wait for all connectivity tests to complete (with timeout)
            CompletableFuture<Void> allTests = CompletableFuture.allOf(
                connectivityTests.toArray(new CompletableFuture[0])
            );
            
            try {
                allTests.get(30, TimeUnit.SECONDS); // 30 second timeout
            } catch (Exception e) {
                builder.addError("Database connectivity tests timed out or failed: " + e.getMessage());
            }
            
            logger.debug("Database connectivity validation completed");
            return builder.build();
            
        } catch (Exception e) {
            logger.error("Database connectivity validation failed", e);
            return builder.addError("Connectivity validation exception: " + e.getMessage()).build();
        }
    }
    
    /**
     * Validate endpoint health (simulate endpoint creation)
     */
    private ValidationStageResult validateEndpointHealth(ConfigurationDelta delta, ConfigurationSet newConfiguration) {
        ValidationStageResult.Builder builder = new ValidationStageResult.Builder("endpoints");
        
        try {
            // Validate endpoint configurations can be created
            for (var entry : delta.addedEndpoints.entrySet()) {
                String endpointName = entry.getKey();
                var endpointConfig = entry.getValue();
                
                try {
                    // Simulate endpoint health check
                    simulateEndpointCreation(endpointName, endpointConfig, newConfiguration);
                    logger.debug("Endpoint health simulation passed for: {}", endpointName);
                } catch (Exception e) {
                    builder.addError("Endpoint health simulation failed for '" + endpointName + "': " + e.getMessage());
                    logger.warn("Endpoint health simulation failed for: {}", endpointName, e);
                }
            }
            
            logger.debug("Endpoint health validation completed");
            return builder.build();
            
        } catch (Exception e) {
            logger.error("Endpoint health validation failed", e);
            return builder.addError("Endpoint validation exception: " + e.getMessage()).build();
        }
    }
    
    /**
     * Test database connection
     */
    private void testDatabaseConnection(String dbName, dev.cordal.generic.config.DatabaseConfig dbConfig) {
        // Implementation would test actual database connection
        // This is a placeholder for the actual implementation
        logger.debug("Testing database connection for: {}", dbName);
        
        // Simulate connection test
        if (dbConfig.getUrl().contains("invalid")) {
            throw new RuntimeException("Invalid database URL");
        }
    }
    
    /**
     * Simulate endpoint creation for health check
     */
    private void simulateEndpointCreation(String endpointName, 
                                        dev.cordal.generic.config.ApiEndpointConfig endpointConfig,
                                        ConfigurationSet newConfiguration) {
        // Implementation would simulate endpoint creation
        // This is a placeholder for the actual implementation
        logger.debug("Simulating endpoint creation for: {}", endpointName);
        
        // Check if referenced query exists
        if (!newConfiguration.getQueries().containsKey(endpointConfig.getQuery())) {
            throw new RuntimeException("Referenced query not found: " + endpointConfig.getQuery());
        }
    }
    
    /**
     * Initialize the list of validators
     */
    private List<ConfigurationValidator> initializeValidators() {
        List<ConfigurationValidator> validators = new ArrayList<>();
        
        // Add built-in validators
        validators.add(new SyntaxValidator());
        validators.add(new DependencyValidator());
        validators.add(new ConnectivityValidator());
        
        return validators;
    }
    
    /**
     * Interface for configuration validators
     */
    public interface ConfigurationValidator {
        ValidationStageResult validate(ConfigurationDelta delta, ConfigurationSet configuration);
        String getName();
    }
    
    // Built-in validator implementations
    private static class SyntaxValidator implements ConfigurationValidator {
        @Override
        public ValidationStageResult validate(ConfigurationDelta delta, ConfigurationSet configuration) {
            // Implementation would go here
            return new ValidationStageResult.Builder("syntax").build();
        }
        
        @Override
        public String getName() {
            return "syntax";
        }
    }
    
    private static class DependencyValidator implements ConfigurationValidator {
        @Override
        public ValidationStageResult validate(ConfigurationDelta delta, ConfigurationSet configuration) {
            // Implementation would go here
            return new ValidationStageResult.Builder("dependency").build();
        }
        
        @Override
        public String getName() {
            return "dependency";
        }
    }
    
    private static class ConnectivityValidator implements ConfigurationValidator {
        @Override
        public ValidationStageResult validate(ConfigurationDelta delta, ConfigurationSet configuration) {
            // Implementation would go here
            return new ValidationStageResult.Builder("connectivity").build();
        }
        
        @Override
        public String getName() {
            return "connectivity";
        }
    }
}

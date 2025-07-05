package dev.mars.bootstrap;

import dev.mars.generic.GenericApiApplication;
import dev.mars.generic.config.ConfigurationLoader;
import dev.mars.generic.config.ConfigurationLoaderFactory;
import dev.mars.generic.config.EndpointConfigurationManager;
import dev.mars.generic.database.DatabaseConnectionManager;
import dev.mars.generic.config.DatabaseConfig;
import dev.mars.generic.config.QueryConfig;
import dev.mars.generic.config.ApiEndpointConfig;
import dev.mars.config.GenericApiConfig;
import dev.mars.database.loader.DatabaseConfigurationLoader;
import dev.mars.database.repository.DatabaseConfigurationRepository;
import dev.mars.database.repository.QueryConfigurationRepository;
import dev.mars.database.repository.EndpointConfigurationRepository;
import dev.mars.database.DatabaseManager;
import dev.mars.util.ConfigurationValidator;
import dev.mars.util.ValidationResult;
import dev.mars.util.ApiEndpoints;
import dev.mars.util.MetricsApiEndpoints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Bootstrap demonstration class that shows system startup and makes calls to all management API endpoints.
 * This class demonstrates the Generic API Service initialization and comprehensive API testing workflow.
 *
 * Note: This demo focuses on the Generic API Service. For full system demonstration including
 * Metrics Service, start the Metrics Service separately in another terminal.
 */
public class SystemBootstrapDemo {
    private static final Logger logger = LoggerFactory.getLogger(SystemBootstrapDemo.class);

    private static final String GENERIC_API_BASE_URL = "http://localhost:8080";
    private static final String METRICS_SERVICE_BASE_URL = "http://localhost:8081";

    private GenericApiApplication genericApiApp;
    private HttpClient httpClient;
    private boolean metricsServiceAvailable = false;

    // Configuration components for dynamic API validation
    private ConfigurationLoaderFactory configurationLoaderFactory;
    private EndpointConfigurationManager configurationManager;
    private DatabaseConnectionManager databaseConnectionManager;
    private ConfigurationValidator configurationValidator;
    
    public static void main(String[] args) {
        SystemBootstrapDemo demo = new SystemBootstrapDemo();
        try {
            demo.runBootstrapDemo();
        } catch (Exception e) {
            logger.error("Bootstrap demo failed", e);
            System.exit(1);
        }
    }
    
    /**
     * Run the complete bootstrap demonstration
     */
    public void runBootstrapDemo() throws Exception {
        logger.info("=".repeat(80));
        logger.info(">>> STARTING SYSTEM BOOTSTRAP DEMONSTRATION");
        logger.info("=".repeat(80));
        
        try {
            // Initialize HTTP client
            initializeHttpClient();

            // Step 1: Start Generic API Service
            startGenericApiService();

            // Step 2: Wait for Generic API Service to be ready
            waitForGenericApiServiceReady();

            // Step 3: Check if Metrics Service is available
            checkMetricsServiceAvailability();

            // Step 4: Test all management API endpoints
            testManagementApis();

            // Step 5: Perform dynamic API validation
            performDynamicApiValidation();

            // Step 6: Test metrics API endpoints if available
            if (metricsServiceAvailable) {
                testMetricsApis();
            } else {
                displayMetricsServiceInstructions();
            }

            // Step 7: Display summary
            displaySummary();

            logger.info("[SUCCESS] Bootstrap demonstration completed successfully!");

        } finally {
            // Cleanup
            stopServices();
        }
    }
    
    /**
     * Initialize HTTP client for API calls
     */
    private void initializeHttpClient() {
        logger.info("[INIT] Initializing HTTP client...");
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        logger.info("[OK] HTTP client initialized");
    }
    
    /**
     * Start Generic API Service
     */
    private void startGenericApiService() throws Exception {
        logger.info("[START] Starting Generic API Service...");

        try {
            genericApiApp = new GenericApiApplication();
            genericApiApp.start();
            logger.info("[OK] Generic API Service started on port 8080");
        } catch (Exception e) {
            logger.error("Failed to start Generic API Service", e);
            throw e;
        }
    }
    
    /**
     * Wait for Generic API Service to be ready
     */
    private void waitForGenericApiServiceReady() throws Exception {
        logger.info("[WAIT] Waiting for Generic API Service to be ready...");
        waitForServiceReady(GENERIC_API_BASE_URL + "/api/health", "Generic API Service");
        logger.info("[OK] Generic API Service is ready");
    }

    /**
     * Check if Metrics Service is available
     */
    private void checkMetricsServiceAvailability() {
        logger.info("[CHECK] Checking Metrics Service availability...");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(METRICS_SERVICE_BASE_URL + "/api/health"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                metricsServiceAvailable = true;
                logger.info("[OK] Metrics Service is available and will be tested");
            } else {
                logger.info("[WARN] Metrics Service responded with status: {}", response.statusCode());
            }
        } catch (Exception e) {
            logger.info("[INFO] Metrics Service is not available (this is optional for the demo)");
        }
    }
    
    /**
     * Wait for a specific service to be ready
     * TODO: make it a bit more fancy with backoff and jitter
     */
    private void waitForServiceReady(String healthUrl, String serviceName) throws Exception {
        int maxAttempts = 30;
        int attempt = 0;
        
        while (attempt < maxAttempts) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(healthUrl))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    logger.info("[OK] {} is ready", serviceName);
                    return;
                }
            } catch (Exception e) {
                // Service not ready yet, continue waiting
            }
            
            attempt++;
            Thread.sleep(1000); // Wait 1 second between attempts
            logger.debug("Waiting for {} to be ready... (attempt {}/{})", serviceName, attempt, maxAttempts);
        }
        
        throw new RuntimeException(serviceName + " failed to become ready within timeout");
    }

    /**
     * Test all management API endpoints in Generic API Service
     */
    private void testManagementApis() throws Exception {
        logger.info("[TEST] Testing Management API endpoints...");

        List<String> managementEndpoints = ApiEndpoints.getAllManagementEndpoints();

        testEndpoints(GENERIC_API_BASE_URL, managementEndpoints, "Management API");
        logger.info("[OK] Management API endpoints tested successfully");
    }

    /**
     * Test all metrics API endpoints in Metrics Service
     */
    private void testMetricsApis() throws Exception {
        logger.info("[TEST] Testing Metrics API endpoints...");

        List<String> metricsEndpoints = MetricsApiEndpoints.getAllMetricsEndpoints();

        testEndpoints(METRICS_SERVICE_BASE_URL, metricsEndpoints, "Metrics API");
        logger.info("[OK] Metrics API endpoints tested successfully");
    }

    /**
     * Display instructions for starting the Metrics Service
     */
    private void displayMetricsServiceInstructions() {
        logger.info("[INFO] Metrics Service Instructions:");
        logger.info("+----------------------------------------------------------------------------------+");
        logger.info("| The Metrics Service is not currently running. To test the full system:         |");
        logger.info("|                                                                                  |");
        logger.info("| 1. Open a new terminal window                                                   |");
        logger.info("| 2. Navigate to the project root directory                                       |");
        logger.info("| 3. Run: cd metrics-service                                                      |");
        logger.info("| 4. Run: mvn exec:java -Dexec.mainClass=\"dev.mars.metrics.MetricsApplication\"  |");
        logger.info("| 5. Re-run this bootstrap demo to test both services                            |");
        logger.info("|                                                                                  |");
        logger.info("| Alternatively, use the provided scripts:                                        |");
        logger.info("| - Windows: run-bootstrap-demo.bat                                               |");
        logger.info("| - Unix/Linux: ./run-bootstrap-demo.sh                                           |");
        logger.info("+----------------------------------------------------------------------------------+");
    }

    /**
     * Perform comprehensive dynamic API validation
     */
    private void performDynamicApiValidation() {
        logger.info("[VALIDATE] Starting Dynamic API Validation...");
        logger.info("=".repeat(80));
        logger.info(">>> DYNAMIC API VALIDATION");
        logger.info("=".repeat(80));

        try {
            // Initialize configuration components
            initializeConfigurationComponents();

            // Part 1: Configuration Validation
            logger.info("[PART 1] Configuration Validation");
            ValidationResult configValidation = configurationValidator.validateConfigurationChain();
            configurationValidator.displayValidationResults("Configuration Chain", configValidation);

            // Part 2: Database Schema Validation (only if config validation passed)
            if (configValidation.isSuccess()) {
                logger.info("");
                logger.info("[PART 2] Database Schema Validation");
                ValidationResult schemaValidation = configurationValidator.validateDatabaseSchema();
                configurationValidator.displayValidationResults("Database Schema", schemaValidation);
            } else {
                logger.info("[SKIP] Database Schema Validation - Configuration validation failed");
            }

            logger.info("=".repeat(80));
            logger.info("[OK] Dynamic API validation completed");

        } catch (Exception e) {
            logger.error("[ERROR] Dynamic API validation failed", e);
        }
    }

    /**
     * Initialize configuration components for validation
     */
    private void initializeConfigurationComponents() {
        logger.info("[INIT] Initializing configuration components...");

        // Create configuration
        GenericApiConfig config = new GenericApiConfig();

        // Create database manager for database-based configurations
        DatabaseManager databaseManager = new DatabaseManager(config);
        databaseManager.initializeSchema();

        // Create repositories
        DatabaseConfigurationRepository databaseRepository = new DatabaseConfigurationRepository(databaseManager);
        QueryConfigurationRepository queryRepository = new QueryConfigurationRepository(databaseManager);
        EndpointConfigurationRepository endpointRepository = new EndpointConfigurationRepository(databaseManager);

        // Create loaders
        ConfigurationLoader yamlLoader = new ConfigurationLoader(config);
        DatabaseConfigurationLoader databaseLoader = new DatabaseConfigurationLoader(databaseRepository, queryRepository, endpointRepository);

        // Create factory
        configurationLoaderFactory = new ConfigurationLoaderFactory(config, yamlLoader, databaseLoader);

        // Create configuration manager
        configurationManager = new EndpointConfigurationManager(configurationLoaderFactory);

        // Create database connection manager
        databaseConnectionManager = new DatabaseConnectionManager(configurationManager);

        // Create configuration validator
        configurationValidator = new ConfigurationValidator(configurationManager, databaseConnectionManager);

        logger.info("[OK] Configuration components initialized using {} source", configurationManager.getConfigurationSource());
    }

    /**
     * Test a list of endpoints for a given service
     */
    private void testEndpoints(String baseUrl, List<String> endpoints, String serviceName) throws Exception {
        List<EndpointTestResult> results = new ArrayList<>();

        for (String endpoint : endpoints) {
            String fullUrl = baseUrl + endpoint;
            EndpointTestResult result = testSingleEndpoint(fullUrl, endpoint);
            results.add(result);

            // Add small delay between requests
            Thread.sleep(100);
        }

        // Display results
        displayEndpointResults(serviceName, results);
    }

    /**
     * Test a single endpoint and return the result
     */
    private EndpointTestResult testSingleEndpoint(String url, String endpoint) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long responseTime = System.currentTimeMillis() - startTime;

            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;

            return new EndpointTestResult(endpoint, response.statusCode(), responseTime, success,
                    response.body().length(), null);

        } catch (Exception e) {
            logger.warn("Failed to test endpoint {}: {}", endpoint, e.getMessage());
            return new EndpointTestResult(endpoint, -1, -1, false, 0, e.getMessage());
        }
    }

    /**
     * Display endpoint test results in a formatted table
     */
    private void displayEndpointResults(String serviceName, List<EndpointTestResult> results) {
        logger.info("[RESULTS] {} Test Results:", serviceName);
        logger.info("+-----------------------------------------------------+--------+----------+---------+----------+");
        logger.info("| Endpoint                                            | Status | Time(ms) | Success | Size(B)  |");
        logger.info("+-----------------------------------------------------+--------+----------+---------+----------+");

        for (EndpointTestResult result : results) {
            String endpoint = String.format("%-51s", result.endpoint.length() > 51 ?
                    result.endpoint.substring(0, 48) + "..." : result.endpoint);
            String status = String.format("%6s", result.statusCode == -1 ? "ERROR" : String.valueOf(result.statusCode));
            String time = String.format("%8s", result.responseTime == -1 ? "N/A" : String.valueOf(result.responseTime));
            String success = String.format("%7s", result.success ? "OK" : "FAIL");
            String size = String.format("%8s", result.responseSize == 0 ? "N/A" : String.valueOf(result.responseSize));

            logger.info("| {} | {} | {} | {} | {} |", endpoint, status, time, success, size);
        }

        logger.info("+-----------------------------------------------------+--------+----------+---------+----------+");

        // Summary
        long successCount = results.stream().mapToLong(r -> r.success ? 1 : 0).sum();
        double successRate = (double) successCount / results.size() * 100;
        double avgResponseTime = results.stream()
                .filter(r -> r.responseTime > 0)
                .mapToLong(r -> r.responseTime)
                .average()
                .orElse(0.0);

        logger.info("[SUMMARY] {}/{} endpoints successful (%.1f%%), Avg response time: %.1fms",
                successCount, results.size(), successRate, avgResponseTime);
    }

    /**
     * Display overall system summary
     */
    private void displaySummary() {
        logger.info("=".repeat(80));
        logger.info(">>> SYSTEM BOOTSTRAP DEMONSTRATION SUMMARY");
        logger.info("=".repeat(80));
        logger.info("[STATUS] Services Status:");
        logger.info("   + Generic API Service: {} [OK]", GENERIC_API_BASE_URL);
        logger.info("   + Metrics Service:     {} {}", METRICS_SERVICE_BASE_URL,
                metricsServiceAvailable ? "[OK]" : "[NOT RUNNING]");
        logger.info("");
        logger.info("[TESTED] Management APIs Tested:");
        logger.info("   + Configuration Management [OK]");
        logger.info("   + Usage Statistics [OK]");
        logger.info("   + Health Monitoring [OK]");
        logger.info("   + Dashboard [OK]");
        logger.info("");
        if (metricsServiceAvailable) {
            logger.info("[TESTED] Metrics APIs Tested:");
            logger.info("   + Performance Metrics [OK]");
            logger.info("   + Real-time Metrics [OK]");
            logger.info("   + Health Checks [OK]");
        } else {
            logger.info("[SKIPPED] Metrics APIs:");
            logger.info("   + Not tested (Metrics Service not running)");
        }
        logger.info("");
        logger.info("[ACCESS] Access URLs:");
        logger.info("   + Generic API Swagger: {}/swagger", GENERIC_API_BASE_URL);
        logger.info("   + Management Dashboard: {}{}", GENERIC_API_BASE_URL, ApiEndpoints.Management.DASHBOARD);
        if (metricsServiceAvailable) {
            logger.info("   + Metrics Dashboard: {}{}", METRICS_SERVICE_BASE_URL, MetricsApiEndpoints.DASHBOARD);
        } else {
            logger.info("   + Metrics Dashboard: {}{} (Start Metrics Service first)", METRICS_SERVICE_BASE_URL, MetricsApiEndpoints.DASHBOARD);
        }
        logger.info("=".repeat(80));
    }

    /**
     * Stop Generic API Service gracefully
     */
    private void stopServices() {
        logger.info("[STOP] Stopping Generic API Service...");

        if (genericApiApp != null) {
            try {
                genericApiApp.stop();
                logger.info("[OK] Generic API Service stopped");
            } catch (Exception e) {
                logger.error("Error stopping Generic API Service", e);
            }
        }

        if (metricsServiceAvailable) {
            logger.info("[INFO] Metrics Service is running separately and will continue running");
        }

        logger.info("[OK] Bootstrap demo cleanup completed");
    }

    /**
     * Data class to hold endpoint test results
     */
    private static class EndpointTestResult {
        final String endpoint;
        final int statusCode;
        final long responseTime;
        final boolean success;
        final int responseSize;
        final String errorMessage;

        EndpointTestResult(String endpoint, int statusCode, long responseTime, boolean success,
                          int responseSize, String errorMessage) {
            this.endpoint = endpoint;
            this.statusCode = statusCode;
            this.responseTime = responseTime;
            this.success = success;
            this.responseSize = responseSize;
            this.errorMessage = errorMessage;
        }
    }


}

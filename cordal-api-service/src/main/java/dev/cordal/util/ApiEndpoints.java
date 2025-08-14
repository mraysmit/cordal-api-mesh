package dev.cordal.util;

import java.util.List;
import java.util.Map;

/**
 * Centralized registry for all API endpoint paths to avoid hardcoding across the application.
 * This class provides constants and utility methods for API endpoint management.
 */
public final class ApiEndpoints {

    // ========== BASE PATHS ==========
    public static final String API_BASE = "/api";
    public static final String GENERIC_BASE = API_BASE + "/generic";
    public static final String MANAGEMENT_BASE = API_BASE + "/management";
    public static final String HEALTH_BASE = API_BASE + "/health";

    // ========== HEALTH ENDPOINTS ==========
    public static final String HEALTH = HEALTH_BASE;
    public static final String GENERIC_HEALTH = GENERIC_BASE + "/health";

    // ========== H2 SERVER MANAGEMENT ENDPOINTS ==========
    public static final String H2_SERVER_BASE = API_BASE + "/h2-server";
    public static final String H2_SERVER_STATUS = H2_SERVER_BASE + "/status";
    public static final String H2_SERVER_START = H2_SERVER_BASE + "/start";
    public static final String H2_SERVER_STOP = H2_SERVER_BASE + "/stop";
    public static final String H2_SERVER_TCP_START = H2_SERVER_BASE + "/tcp/start";
    public static final String H2_SERVER_TCP_STOP = H2_SERVER_BASE + "/tcp/stop";
    public static final String H2_SERVER_WEB_START = H2_SERVER_BASE + "/web/start";
    public static final String H2_SERVER_WEB_STOP = H2_SERVER_BASE + "/web/stop";

    // ========== GENERIC API ENDPOINTS ==========
    public static final String GENERIC_ENDPOINTS = GENERIC_BASE + "/endpoints";
    public static final String GENERIC_ENDPOINT_BY_NAME = GENERIC_BASE + "/endpoints/{endpointName}";
    public static final String GENERIC_CONFIG = GENERIC_BASE + "/config";

    // ========== CONFIGURATION MANAGEMENT ENDPOINTS ==========
    public static final class Management {
        // Configuration Metadata
        public static final String CONFIG_METADATA = MANAGEMENT_BASE + "/config/metadata";
        public static final String CONFIG_PATHS = MANAGEMENT_BASE + "/config/paths";
        public static final String CONFIG_CONTENTS = MANAGEMENT_BASE + "/config/contents";
        
        // Configuration Views
        public static final String CONFIG_ENDPOINTS = MANAGEMENT_BASE + "/config/endpoints";
        public static final String CONFIG_QUERIES = MANAGEMENT_BASE + "/config/queries";
        public static final String CONFIG_DATABASES = MANAGEMENT_BASE + "/config/databases";
        
        // Usage Statistics
        public static final String STATISTICS = MANAGEMENT_BASE + "/statistics";
        public static final String STATISTICS_ENDPOINTS = MANAGEMENT_BASE + "/statistics/endpoints";
        public static final String STATISTICS_QUERIES = MANAGEMENT_BASE + "/statistics/queries";
        public static final String STATISTICS_DATABASES = MANAGEMENT_BASE + "/statistics/databases";
        
        // Health Monitoring
        public static final String HEALTH = MANAGEMENT_BASE + "/health";
        public static final String HEALTH_DATABASES = MANAGEMENT_BASE + "/health/databases";
        public static final String HEALTH_DATABASE_SPECIFIC = MANAGEMENT_BASE + "/health/databases/{databaseName}";

        // Deployment Verification
        public static final String DEPLOYMENT_INFO = MANAGEMENT_BASE + "/deployment";
        public static final String JAR_INFO = MANAGEMENT_BASE + "/jar";
        public static final String READINESS = MANAGEMENT_BASE + "/readiness";
        public static final String LIVENESS = MANAGEMENT_BASE + "/liveness";

        // Dashboard
        public static final String DASHBOARD = MANAGEMENT_BASE + "/dashboard";
    }

    // ========== CONFIGURATION MANAGEMENT ENDPOINTS ==========
    public static final class ConfigManagement {
        // Database Configuration Management
        public static final String DATABASES = MANAGEMENT_BASE + "/config-mgmt/databases";
        public static final String DATABASE_BY_NAME = MANAGEMENT_BASE + "/config-mgmt/databases/{name}";

        // Query Configuration Management
        public static final String QUERIES = MANAGEMENT_BASE + "/config-mgmt/queries";
        public static final String QUERY_BY_NAME = MANAGEMENT_BASE + "/config-mgmt/queries/{name}";
        public static final String QUERIES_BY_DATABASE = MANAGEMENT_BASE + "/config-mgmt/queries/by-database/{databaseName}";

        // Endpoint Configuration Management
        public static final String ENDPOINTS = MANAGEMENT_BASE + "/config-mgmt/endpoints";
        public static final String ENDPOINT_BY_NAME = MANAGEMENT_BASE + "/config-mgmt/endpoints/{name}";
        public static final String ENDPOINTS_BY_QUERY = MANAGEMENT_BASE + "/config-mgmt/endpoints/by-query/{queryName}";

        // Configuration Management Utilities
        public static final String STATISTICS = MANAGEMENT_BASE + "/config-mgmt/statistics";
        public static final String SOURCE_INFO = MANAGEMENT_BASE + "/config-mgmt/source";
        public static final String AVAILABILITY = MANAGEMENT_BASE + "/config-mgmt/availability";
    }

    // ========== CONFIGURATION MIGRATION ENDPOINTS ==========
    public static final class Migration {
        // Migration Operations
        public static final String YAML_TO_DATABASE = MANAGEMENT_BASE + "/migration/yaml-to-database";
        public static final String EXPORT_DATABASE_TO_YAML = MANAGEMENT_BASE + "/migration/export-database-to-yaml";

        // Synchronization Operations
        public static final String COMPARE = MANAGEMENT_BASE + "/migration/compare";
        public static final String STATUS = MANAGEMENT_BASE + "/migration/status";

        // YAML Export Utilities
        public static final String YAML_DATABASES = MANAGEMENT_BASE + "/migration/yaml/databases";
        public static final String YAML_QUERIES = MANAGEMENT_BASE + "/migration/yaml/queries";
        public static final String YAML_ENDPOINTS = MANAGEMENT_BASE + "/migration/yaml/endpoints";
    }

    // ========== CONFIGURATION VALIDATION ENDPOINTS ==========
    public static final class Validation {
        public static final String VALIDATE_ALL = GENERIC_BASE + "/config/validate";
        public static final String VALIDATE_ENDPOINTS = GENERIC_BASE + "/config/validate/endpoints";
        public static final String VALIDATE_QUERIES = GENERIC_BASE + "/config/validate/queries";
        public static final String VALIDATE_DATABASES = GENERIC_BASE + "/config/validate/databases";
        public static final String VALIDATE_RELATIONSHIPS = GENERIC_BASE + "/config/validate/relationships";
        public static final String VALIDATE_ENDPOINT_CONNECTIVITY = GENERIC_BASE + "/config/validate/endpoint-connectivity";
    }

    // ========== GRANULAR CONFIGURATION ENDPOINTS ==========
    public static final class Config {
        // Endpoints
        public static final String ENDPOINTS_SCHEMA = GENERIC_BASE + "/config/endpoints/schema";
        public static final String ENDPOINTS_PARAMETERS = GENERIC_BASE + "/config/endpoints/parameters";
        public static final String ENDPOINTS_DATABASE_CONNECTIONS = GENERIC_BASE + "/config/endpoints/database-connections";
        public static final String ENDPOINTS_SUMMARY = GENERIC_BASE + "/config/endpoints/summary";
        
        // Queries
        public static final String QUERIES_SCHEMA = GENERIC_BASE + "/config/queries/schema";
        public static final String QUERIES_PARAMETERS = GENERIC_BASE + "/config/queries/parameters";
        public static final String QUERIES_DATABASE_CONNECTIONS = GENERIC_BASE + "/config/queries/database-connections";
        public static final String QUERIES_SUMMARY = GENERIC_BASE + "/config/queries/summary";
        
        // Databases
        public static final String DATABASES_SCHEMA = GENERIC_BASE + "/config/databases/schema";
        public static final String DATABASES_PARAMETERS = GENERIC_BASE + "/config/databases/parameters";
        public static final String DATABASES_CONNECTIONS = GENERIC_BASE + "/config/databases/connections";
        public static final String DATABASES_SUMMARY = GENERIC_BASE + "/config/databases/summary";
        
        // Individual Items
        public static final String QUERIES_BY_NAME = GENERIC_BASE + "/config/queries/{queryName}";
        public static final String DATABASES_BY_NAME = GENERIC_BASE + "/config/databases/{databaseName}";
        public static final String RELATIONSHIPS = GENERIC_BASE + "/config/relationships";
    }

    // ========== ENDPOINT GROUPS FOR TESTING ==========
    public static final class Groups {
        /**
         * All management API endpoints for testing
         */
        public static final List<String> MANAGEMENT_ENDPOINTS = List.of(
            // Configuration Management
            Management.CONFIG_METADATA,
            Management.CONFIG_PATHS,
            Management.CONFIG_CONTENTS,
            Management.CONFIG_ENDPOINTS,
            Management.CONFIG_QUERIES,
            Management.CONFIG_DATABASES,
            
            // Usage Statistics
            Management.STATISTICS,
            Management.STATISTICS_ENDPOINTS,
            Management.STATISTICS_QUERIES,
            Management.STATISTICS_DATABASES,
            
            // Health Monitoring
            Management.HEALTH,
            Management.HEALTH_DATABASES,
            
            // Dashboard
            Management.DASHBOARD
        );

        /**
         * All configuration validation endpoints
         */
        public static final List<String> VALIDATION_ENDPOINTS = List.of(
            Validation.VALIDATE_ALL,
            Validation.VALIDATE_ENDPOINTS,
            Validation.VALIDATE_QUERIES,
            Validation.VALIDATE_DATABASES,
            Validation.VALIDATE_RELATIONSHIPS,
            Validation.VALIDATE_ENDPOINT_CONNECTIVITY
        );

        /**
         * All granular configuration endpoints
         */
        public static final List<String> CONFIG_ENDPOINTS = List.of(
            Config.ENDPOINTS_SCHEMA,
            Config.ENDPOINTS_PARAMETERS,
            Config.ENDPOINTS_DATABASE_CONNECTIONS,
            Config.ENDPOINTS_SUMMARY,
            Config.QUERIES_SCHEMA,
            Config.QUERIES_PARAMETERS,
            Config.QUERIES_DATABASE_CONNECTIONS,
            Config.QUERIES_SUMMARY,
            Config.DATABASES_SCHEMA,
            Config.DATABASES_PARAMETERS,
            Config.DATABASES_CONNECTIONS,
            Config.DATABASES_SUMMARY,
            Config.QUERIES_BY_NAME,
            Config.DATABASES_BY_NAME,
            Config.RELATIONSHIPS
        );
    }

    // ========== UTILITY METHODS ==========
    
    /**
     * Get all management endpoints as a list
     */
    public static List<String> getAllManagementEndpoints() {
        return Groups.MANAGEMENT_ENDPOINTS;
    }

    /**
     * Get endpoint categories for documentation
     */
    public static Map<String, List<String>> getEndpointsByCategory() {
        return Map.of(
            "Management", Groups.MANAGEMENT_ENDPOINTS,
            "Validation", Groups.VALIDATION_ENDPOINTS,
            "Configuration", Groups.CONFIG_ENDPOINTS
        );
    }

    /**
     * Check if an endpoint is a management endpoint
     */
    public static boolean isManagementEndpoint(String path) {
        return path != null && path.startsWith(MANAGEMENT_BASE);
    }

    /**
     * Check if an endpoint is a configuration endpoint
     */
    public static boolean isConfigurationEndpoint(String path) {
        return path != null && path.startsWith(GENERIC_BASE + "/config");
    }

    // Private constructor to prevent instantiation
    private ApiEndpoints() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}

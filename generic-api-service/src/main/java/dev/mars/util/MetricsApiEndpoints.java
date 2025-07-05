package dev.mars.util;

import java.util.List;

/**
 * Centralized registry for Metrics Service API endpoint paths.
 * This class provides constants for metrics service endpoints used in testing and integration.
 */
public final class MetricsApiEndpoints {

    // ========== BASE PATHS ==========
    public static final String API_BASE = "/api";
    public static final String DASHBOARD_BASE = "/dashboard";

    // ========== HEALTH ENDPOINTS ==========
    public static final String HEALTH = API_BASE + "/health";

    // ========== PERFORMANCE METRICS ENDPOINTS ==========
    public static final String PERFORMANCE_METRICS = API_BASE + "/performance-metrics";
    public static final String PERFORMANCE_METRICS_SUMMARY = API_BASE + "/performance-metrics/summary";
    public static final String PERFORMANCE_METRICS_TRENDS = API_BASE + "/performance-metrics/trends";
    public static final String PERFORMANCE_METRICS_TEST_TYPES = API_BASE + "/performance-metrics/test-types";

    // ========== REAL-TIME METRICS ENDPOINTS ==========
    public static final String METRICS_ENDPOINTS = API_BASE + "/metrics/endpoints";

    // ========== DASHBOARD ENDPOINTS ==========
    public static final String DASHBOARD = DASHBOARD_BASE;

    // ========== ENDPOINT GROUPS FOR TESTING ==========
    public static final class Groups {
        /**
         * All metrics API endpoints for testing
         */
        public static final List<String> METRICS_ENDPOINTS = List.of(
            // Health Check
            HEALTH,
            
            // Performance Metrics
            PERFORMANCE_METRICS,
            PERFORMANCE_METRICS_SUMMARY,
            PERFORMANCE_METRICS_TRENDS,
            PERFORMANCE_METRICS_TEST_TYPES,
            
            // Real-time Metrics
            MetricsApiEndpoints.METRICS_ENDPOINTS
        );

        /**
         * All dashboard endpoints
         */
        public static final List<String> DASHBOARD_ENDPOINTS = List.of(
            DASHBOARD
        );
    }

    // ========== UTILITY METHODS ==========
    
    /**
     * Get all metrics endpoints as a list
     */
    public static List<String> getAllMetricsEndpoints() {
        return Groups.METRICS_ENDPOINTS;
    }

    /**
     * Check if an endpoint is a metrics endpoint
     */
    public static boolean isMetricsEndpoint(String path) {
        return path != null && (path.startsWith(API_BASE) || path.startsWith(DASHBOARD_BASE));
    }

    // Private constructor to prevent instantiation
    private MetricsApiEndpoints() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}

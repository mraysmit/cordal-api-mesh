package dev.mars.metrics;

import com.google.inject.Module;
import dev.mars.common.application.BaseJavalinApplication;
import dev.mars.common.config.ServerConfig;
import dev.mars.common.database.MetricsDatabaseManager;
import dev.mars.config.MetricsConfig;
import dev.mars.config.MetricsGuiceModule;
import dev.mars.controller.PerformanceMetricsController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Main application class for the Metrics Service
 * Extends BaseJavalinApplication for common functionality
 */
public class MetricsApplication extends BaseJavalinApplication {
    private static final Logger logger = LoggerFactory.getLogger(MetricsApplication.class);

    public static void main(String[] args) {
        try {
            MetricsApplication application = new MetricsApplication();
            application.start();

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(application::stop));

        } catch (Exception e) {
            logger.error("Failed to start Metrics application", e);
            System.exit(1);
        }
    }

    @Override
    protected Module getGuiceModule() {
        return new MetricsGuiceModule();
    }

    @Override
    protected ServerConfig getServerConfig() {
        MetricsConfig config = injector.getInstance(MetricsConfig.class);
        return config.getServerConfig();
    }

    @Override
    protected String getApplicationName() {
        return "Metrics Service";
    }

    @Override
    protected void performPreStartupInitialization() {
        // Initialize metrics database
        MetricsDatabaseManager dbManager = injector.getInstance(MetricsDatabaseManager.class);
        dbManager.initializeSchema();
    }
    @Override
    protected void configureRoutes() {
        logger.info("Configuring routes");
        
        PerformanceMetricsController performanceMetricsController = injector.getInstance(PerformanceMetricsController.class);
        MetricsCollectionHandler metricsCollectionHandler = injector.getInstance(MetricsCollectionHandler.class);
        MetricsConfig config = injector.getInstance(MetricsConfig.class);
        
        // Health check endpoint
        app.get("/api/health", ctx -> {
            ctx.json(Map.of(
                "status", "UP",
                "timestamp", System.currentTimeMillis(),
                "service", "metrics-service"
            ));
        });

        // Performance metrics endpoints (specific routes first, then parameterized routes)
        app.get("/api/performance-metrics/test-types", performanceMetricsController::getAvailableTestTypes);
        app.get("/api/performance-metrics/summary", performanceMetricsController::getPerformanceSummary);
        app.get("/api/performance-metrics/trends", performanceMetricsController::getPerformanceTrends);
        app.get("/api/performance-metrics/date-range", performanceMetricsController::getPerformanceMetricsByDateRange);
        app.get("/api/performance-metrics/test-type/{testType}", performanceMetricsController::getPerformanceMetricsByTestType);
        app.get("/api/performance-metrics/{id}", performanceMetricsController::getPerformanceMetricsById);
        app.get("/api/performance-metrics", performanceMetricsController::getAllPerformanceMetrics);
        app.post("/api/performance-metrics", performanceMetricsController::createPerformanceMetrics);

        // Real-time metrics collection endpoints
        app.get("/api/metrics/endpoints", ctx -> {
            ctx.json(metricsCollectionHandler.getEndpointMetricsSummary());
        });

        app.post("/api/metrics/reset", ctx -> {
            metricsCollectionHandler.resetMetrics();
            ctx.json(Map.of("message", "Metrics reset successfully"));
        });

        // Dashboard static files (conditionally enabled)
        if (config.getMetricsDashboard().getCustom().isEnabled()) {
            String dashboardPath = config.getMetricsDashboard().getCustom().getPath();
            logger.info("Enabling custom dashboard at: {}", dashboardPath);

            app.get(dashboardPath, ctx -> {
                ctx.contentType("text/html");
                ctx.result(getDashboardHtml());
            });

            // Also handle trailing slash
            if (!dashboardPath.endsWith("/")) {
                app.get(dashboardPath + "/", ctx -> {
                    ctx.contentType("text/html");
                    ctx.result(getDashboardHtml());
                });
            }
        } else {
            logger.info("Custom dashboard is disabled");
        }

        // Grafana integration endpoints (conditionally enabled)
        if (config.getMetricsDashboard().getGrafana().isEnabled()) {
            logger.info("Grafana integration is enabled");

            if (config.getMetricsDashboard().getGrafana().getPrometheus().isEnabled()) {
                String prometheusPath = config.getMetricsDashboard().getGrafana().getPrometheus().getPath();
                logger.info("Enabling Prometheus metrics endpoint at: {}", prometheusPath);

                app.get(prometheusPath, ctx -> {
                    ctx.contentType("text/plain");
                    ctx.result(getPrometheusMetrics());
                });
            }
        }

        // Add metrics collection handlers for all requests
        app.before(ctx -> {
            // Start metrics collection
            metricsCollectionHandler.beforeRequest(ctx);

            // Request logging
            logger.debug("Incoming request: {} {}", ctx.method(), ctx.path());
            logger.debug("Query parameters: {}", ctx.queryParamMap());
        });

        // Add metrics collection and response logging
        app.after(ctx -> {
            // Complete metrics collection
            metricsCollectionHandler.afterRequest(ctx);

            // Response logging
            logger.debug("Response: {} {} - Status: {}",
                        ctx.method(), ctx.path(), ctx.status());
        });
        
        logger.info("Routes configured");
    }
    @Override
    protected void displayApplicationSpecificEndpoints(String baseUrl) {
        MetricsConfig config = injector.getInstance(MetricsConfig.class);

        // Performance Metrics API
        logger.info("📊 PERFORMANCE METRICS API:");
        logger.info("   ├─ Get All:          GET  {}/api/performance-metrics", baseUrl);
        logger.info("   ├─ Get by ID:        GET  {}/api/performance-metrics/{{id}}", baseUrl);
        logger.info("   ├─ Create:           POST {}/api/performance-metrics", baseUrl);
        logger.info("   ├─ Get Summary:      GET  {}/api/performance-metrics/summary", baseUrl);
        logger.info("   ├─ Get Trends:       GET  {}/api/performance-metrics/trends", baseUrl);
        logger.info("   ├─ Get Test Types:   GET  {}/api/performance-metrics/test-types", baseUrl);
        logger.info("   ├─ Get by Type:      GET  {}/api/performance-metrics/test-type/{{testType}}", baseUrl);
        logger.info("   └─ Get Date Range:   GET  {}/api/performance-metrics/date-range", baseUrl);
        logger.info("");

        // Real-time Metrics Collection
        logger.info("⚡ REAL-TIME METRICS:");
        logger.info("   ├─ Endpoint Summary: GET  {}/api/metrics/endpoints", baseUrl);
        logger.info("   └─ Reset Metrics:    POST {}/api/metrics/reset", baseUrl);
        logger.info("");

        // Dashboard Endpoints
        logger.info("📱 DASHBOARDS:");
        if (config.getMetricsDashboard().getCustom().isEnabled()) {
            String dashboardPath = config.getMetricsDashboard().getCustom().getPath();
            logger.info("   ├─ Custom Dashboard: GET  {}{}", baseUrl, dashboardPath);
        } else {
            logger.info("   ├─ Custom Dashboard: ❌ DISABLED");
        }

        if (config.getMetricsDashboard().getGrafana().isEnabled()) {
            logger.info("   └─ Grafana Mode:     ✅ ENABLED");
        } else {
            logger.info("   └─ Grafana Mode:     ❌ DISABLED");
        }
        logger.info("");

        // Configuration Information
        logger.info("⚙️  CONFIGURATION:");
        logger.info("   ├─ Metrics Collection: {}", config.getMetricsCollection().isEnabled() ? "✅ ENABLED" : "❌ DISABLED");
        logger.info("   ├─ Async Save:         {}", config.getMetricsCollection().isAsyncSave() ? "✅ ENABLED" : "❌ DISABLED");
        logger.info("   ├─ Sampling Rate:      {}", config.getMetricsCollection().getSamplingRate());
        logger.info("   ├─ Custom Dashboard:   {}", config.getMetricsDashboard().getCustom().isEnabled() ? "✅ ENABLED" : "❌ DISABLED");
        logger.info("   ├─ Grafana Mode:       {}", config.getMetricsDashboard().getGrafana().isEnabled() ? "✅ ENABLED" : "❌ DISABLED");
        logger.info("   └─ Prometheus:         {}",
            config.getMetricsDashboard().getGrafana().isEnabled() &&
            config.getMetricsDashboard().getGrafana().getPrometheus().isEnabled() ? "✅ ENABLED" : "❌ DISABLED");
        logger.info("");
        logger.info("🚀 Metrics Service is ready!");
    }

    /**
     * Initialize the application without starting the server (for testing)
     */
    public void initializeForTesting() {
        // Initialize dependency injection
        initializeDependencyInjection();

        // Get server configuration
        ServerConfig serverConfig = getServerConfig();

        // Perform any pre-startup initialization
        performPreStartupInitialization();

        // Create and configure Javalin app
        createJavalinApp(serverConfig);

        // Configure routes
        configureRoutes();

        // Don't start the server - let JavalinTest handle that
    }


    /**
     * Get the dashboard HTML content (simplified version)
     */
    private String getDashboardHtml() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Metrics Dashboard</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 40px; }
                    h1 { color: #333; }
                    .metric { margin: 20px 0; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }
                </style>
            </head>
            <body>
                <h1>Metrics Service Dashboard</h1>
                <div class="metric">
                    <h3>Service Status</h3>
                    <p>Metrics Service is running and collecting performance data.</p>
                </div>
                <div class="metric">
                    <h3>API Endpoints</h3>
                    <ul>
                        <li><a href="/api/performance-metrics/summary">Performance Summary</a></li>
                        <li><a href="/api/metrics/endpoints">Real-time Endpoint Metrics</a></li>
                    </ul>
                </div>
            </body>
            </html>
            """;
    }

    /**
     * Get Prometheus metrics in text format (simplified version)
     */
    private String getPrometheusMetrics() {
        return """
            # HELP performance_test_total Total number of performance tests
            # TYPE performance_test_total counter
            performance_test_total 0

            # HELP performance_test_duration_seconds Performance test duration
            # TYPE performance_test_duration_seconds histogram
            performance_test_duration_seconds_sum 0
            performance_test_duration_seconds_count 0

            # HELP performance_test_success_rate Performance test success rate
            # TYPE performance_test_success_rate gauge
            performance_test_success_rate 1.0
            """;
    }
}

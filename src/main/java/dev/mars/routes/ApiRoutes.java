package dev.mars.routes;

import dev.mars.config.AppConfig;
import dev.mars.controller.PerformanceMetricsController;
import dev.mars.controller.StockTradeController;
import dev.mars.metrics.MetricsCollectionHandler;
import io.javalin.Javalin;
import io.javalin.apibuilder.ApiBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

/**
 * API routes configuration
 */
@Singleton
public class ApiRoutes {
    private static final Logger logger = LoggerFactory.getLogger(ApiRoutes.class);

    private final StockTradeController stockTradeController;
    private final PerformanceMetricsController performanceMetricsController;
    private final MetricsCollectionHandler metricsCollectionHandler;
    private final AppConfig appConfig;

    @Inject
    public ApiRoutes(StockTradeController stockTradeController,
                     PerformanceMetricsController performanceMetricsController,
                     MetricsCollectionHandler metricsCollectionHandler,
                     AppConfig appConfig) {
        this.stockTradeController = stockTradeController;
        this.performanceMetricsController = performanceMetricsController;
        this.metricsCollectionHandler = metricsCollectionHandler;
        this.appConfig = appConfig;
    }
    
    /**
     * Configure all API routes
     */
    public void configure(Javalin app) {
        logger.info("Configuring API routes");

        // Health check endpoint
        app.get("/api/health", stockTradeController::getHealthStatus);

        // Stock trades endpoints
        app.get("/api/stock-trades", stockTradeController::getAllStockTrades);
        app.get("/api/stock-trades/{id}", stockTradeController::getStockTradeById);
        app.get("/api/stock-trades/symbol/{symbol}", stockTradeController::getStockTradesBySymbol);

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
        if (appConfig.getMetricsDashboard().getCustom().isEnabled()) {
            String dashboardPath = appConfig.getMetricsDashboard().getCustom().getPath();
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
        if (appConfig.getMetricsDashboard().getGrafana().isEnabled()) {
            logger.info("Grafana integration is enabled");

            if (appConfig.getMetricsDashboard().getGrafana().getPrometheus().isEnabled()) {
                String prometheusPath = appConfig.getMetricsDashboard().getGrafana().getPrometheus().getPath();
                logger.info("Enabling Prometheus metrics endpoint at: {}", prometheusPath);

                app.get(prometheusPath, ctx -> {
                    ctx.contentType("text/plain");
                    ctx.result(getPrometheusMetrics());
                });
            }
        }

        // Add metrics collection and request logging
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

        logger.info("API routes configured successfully");
    }

    /**
     * Get the dashboard HTML content
     */
    private String getDashboardHtml() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Performance Dashboard - Javalin API Mesh</title>
                <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }

                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        background-color: #f5f5f5;
                        color: #333;
                        line-height: 1.6;
                    }

                    .header {
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        color: white;
                        padding: 2rem 0;
                        text-align: center;
                        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                    }

                    .header h1 {
                        font-size: 2.5rem;
                        margin-bottom: 0.5rem;
                    }

                    .header p {
                        font-size: 1.1rem;
                        opacity: 0.9;
                    }

                    .container {
                        max-width: 1200px;
                        margin: 0 auto;
                        padding: 2rem;
                    }

                    .dashboard-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
                        gap: 2rem;
                        margin-bottom: 2rem;
                    }

                    .card {
                        background: white;
                        border-radius: 12px;
                        padding: 1.5rem;
                        box-shadow: 0 4px 6px rgba(0,0,0,0.1);
                        transition: transform 0.2s ease, box-shadow 0.2s ease;
                    }

                    .card:hover {
                        transform: translateY(-2px);
                        box-shadow: 0 8px 25px rgba(0,0,0,0.15);
                    }

                    .card h3 {
                        color: #667eea;
                        margin-bottom: 1rem;
                        font-size: 1.3rem;
                    }

                    .metric {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        padding: 0.75rem 0;
                        border-bottom: 1px solid #eee;
                    }

                    .metric:last-child {
                        border-bottom: none;
                    }

                    .metric-label {
                        font-weight: 500;
                        color: #666;
                    }

                    .metric-value {
                        font-weight: bold;
                        color: #333;
                        font-size: 1.1rem;
                    }

                    .chart-container {
                        position: relative;
                        height: 300px;
                        margin-top: 1rem;
                    }

                    .controls {
                        background: white;
                        border-radius: 12px;
                        padding: 1.5rem;
                        margin-bottom: 2rem;
                        box-shadow: 0 4px 6px rgba(0,0,0,0.1);
                    }

                    .controls h3 {
                        color: #667eea;
                        margin-bottom: 1rem;
                    }

                    .control-group {
                        display: flex;
                        gap: 1rem;
                        align-items: center;
                        flex-wrap: wrap;
                    }

                    .control-group label {
                        font-weight: 500;
                        color: #666;
                    }

                    .control-group select, .control-group input {
                        padding: 0.5rem;
                        border: 2px solid #e1e5e9;
                        border-radius: 6px;
                        font-size: 1rem;
                        transition: border-color 0.2s ease;
                    }

                    .control-group select:focus, .control-group input:focus {
                        outline: none;
                        border-color: #667eea;
                    }

                    .btn {
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        color: white;
                        border: none;
                        padding: 0.75rem 1.5rem;
                        border-radius: 6px;
                        cursor: pointer;
                        font-size: 1rem;
                        font-weight: 500;
                        transition: transform 0.2s ease, box-shadow 0.2s ease;
                    }

                    .btn:hover {
                        transform: translateY(-1px);
                        box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
                    }

                    .loading {
                        text-align: center;
                        padding: 2rem;
                        color: #666;
                    }

                    .error {
                        background: #fee;
                        color: #c33;
                        padding: 1rem;
                        border-radius: 6px;
                        margin: 1rem 0;
                    }

                    .success-rate {
                        color: #28a745;
                    }

                    .response-time {
                        color: #17a2b8;
                    }

                    .test-count {
                        color: #6f42c1;
                    }

                    @media (max-width: 768px) {
                        .container {
                            padding: 1rem;
                        }

                        .header h1 {
                            font-size: 2rem;
                        }

                        .control-group {
                            flex-direction: column;
                            align-items: stretch;
                        }

                        .control-group > * {
                            width: 100%;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>Performance Dashboard</h1>
                    <p>Real-time monitoring of API performance metrics</p>
                </div>

                <div class="container">
                    <div class="controls">
                        <h3>Dashboard Controls</h3>
                        <div class="control-group">
                            <label for="testTypeFilter">Test Type:</label>
                            <select id="testTypeFilter">
                                <option value="">All Test Types</option>
                            </select>

                            <label for="daysFilter">Time Range:</label>
                            <select id="daysFilter">
                                <option value="1">Last 24 hours</option>
                                <option value="7" selected>Last 7 days</option>
                                <option value="30">Last 30 days</option>
                                <option value="90">Last 90 days</option>
                            </select>

                            <button class="btn" onclick="refreshDashboard()">Refresh</button>
                        </div>
                    </div>

                    <div class="dashboard-grid">
                        <div class="card">
                            <h3>Performance Summary</h3>
                            <div id="summaryContent">
                                <div class="loading">Loading summary...</div>
                            </div>
                        </div>

                        <div class="card">
                            <h3>Response Time Trends</h3>
                            <div class="chart-container">
                                <canvas id="responseTimeChart"></canvas>
                            </div>
                        </div>

                        <div class="card">
                            <h3>Success Rate Trends</h3>
                            <div class="chart-container">
                                <canvas id="successRateChart"></canvas>
                            </div>
                        </div>

                        <div class="card">
                            <h3>Test Type Distribution</h3>
                            <div class="chart-container">
                                <canvas id="testTypeChart"></canvas>
                            </div>
                        </div>
                    </div>

                    <div class="card">
                        <h3>Recent Performance Tests</h3>
                        <div id="recentTestsContent">
                            <div class="loading">Loading recent tests...</div>
                        </div>
                    </div>
                </div>

                <script>
                    // Global variables for charts
                    let responseTimeChart, successRateChart, testTypeChart;

                    // Initialize dashboard
                    document.addEventListener('DOMContentLoaded', function() {
                        initializeDashboard();
                    });

                    async function initializeDashboard() {
                        try {
                            await loadTestTypes();
                            await refreshDashboard();
                        } catch (error) {
                            console.error('Failed to initialize dashboard:', error);
                            showError('Failed to initialize dashboard');
                        }
                    }

                    async function loadTestTypes() {
                        try {
                            const response = await fetch('/api/performance-metrics/test-types');
                            const data = await response.json();

                            const select = document.getElementById('testTypeFilter');
                            select.innerHTML = '<option value="">All Test Types</option>';

                            data.testTypes.forEach(testType => {
                                const option = document.createElement('option');
                                option.value = testType;
                                option.textContent = testType;
                                select.appendChild(option);
                            });
                        } catch (error) {
                            console.error('Failed to load test types:', error);
                        }
                    }

                    async function refreshDashboard() {
                        try {
                            await Promise.all([
                                loadSummary(),
                                loadTrends(),
                                loadRecentTests()
                            ]);
                        } catch (error) {
                            console.error('Failed to refresh dashboard:', error);
                            showError('Failed to refresh dashboard');
                        }
                    }

                    async function loadSummary() {
                        try {
                            const response = await fetch('/api/performance-metrics/summary');
                            const data = await response.json();

                            const summaryHtml = `
                                <div class="metric">
                                    <span class="metric-label">Total Tests</span>
                                    <span class="metric-value test-count">${data.totalTests}</span>
                                </div>
                                <div class="metric">
                                    <span class="metric-label">Average Response Time</span>
                                    <span class="metric-value response-time">${data.averageResponseTime} ms</span>
                                </div>
                                <div class="metric">
                                    <span class="metric-label">Success Rate</span>
                                    <span class="metric-value success-rate">${data.successRate}%</span>
                                </div>
                                <div class="metric">
                                    <span class="metric-label">Last Test</span>
                                    <span class="metric-value">${new Date(data.lastTestTime).toLocaleString()}</span>
                                </div>
                            `;

                            document.getElementById('summaryContent').innerHTML = summaryHtml;
                        } catch (error) {
                            console.error('Failed to load summary:', error);
                            document.getElementById('summaryContent').innerHTML = '<div class="error">Failed to load summary</div>';
                        }
                    }

                    async function loadTrends() {
                        try {
                            const testType = document.getElementById('testTypeFilter').value;
                            const days = document.getElementById('daysFilter').value;

                            const url = `/api/performance-metrics/trends?days=${days}${testType ? '&testType=' + testType : ''}`;
                            const response = await fetch(url);
                            const data = await response.json();

                            updateResponseTimeChart(data);
                            updateSuccessRateChart(data);
                        } catch (error) {
                            console.error('Failed to load trends:', error);
                            showError('Failed to load trends');
                        }
                    }

                    async function loadRecentTests() {
                        try {
                            const response = await fetch('/api/performance-metrics?page=0&size=10');
                            const data = await response.json();

                            let testsHtml = '';
                            if (data.data.length === 0) {
                                testsHtml = '<div class="loading">No performance tests found</div>';
                            } else {
                                testsHtml = '<table style="width: 100%; border-collapse: collapse;">';
                                testsHtml += '<tr style="background: #f8f9fa; font-weight: bold;"><th style="padding: 0.75rem; text-align: left; border-bottom: 2px solid #dee2e6;">Test Name</th><th style="padding: 0.75rem; text-align: left; border-bottom: 2px solid #dee2e6;">Type</th><th style="padding: 0.75rem; text-align: left; border-bottom: 2px solid #dee2e6;">Response Time</th><th style="padding: 0.75rem; text-align: left; border-bottom: 2px solid #dee2e6;">Status</th><th style="padding: 0.75rem; text-align: left; border-bottom: 2px solid #dee2e6;">Date</th></tr>';

                                data.data.forEach(test => {
                                    const status = test.testPassed ? '<span style="color: #28a745;">✓ Passed</span>' : '<span style="color: #dc3545;">✗ Failed</span>';
                                    const responseTime = test.averageResponseTimeMs ? `${test.averageResponseTimeMs.toFixed(2)} ms` : 'N/A';

                                    testsHtml += `
                                        <tr style="border-bottom: 1px solid #dee2e6;">
                                            <td style="padding: 0.75rem;">${test.testName}</td>
                                            <td style="padding: 0.75rem;">${test.testType}</td>
                                            <td style="padding: 0.75rem;">${responseTime}</td>
                                            <td style="padding: 0.75rem;">${status}</td>
                                            <td style="padding: 0.75rem;">${new Date(test.timestamp).toLocaleString()}</td>
                                        </tr>
                                    `;
                                });

                                testsHtml += '</table>';
                            }

                            document.getElementById('recentTestsContent').innerHTML = testsHtml;
                        } catch (error) {
                            console.error('Failed to load recent tests:', error);
                            document.getElementById('recentTestsContent').innerHTML = '<div class="error">Failed to load recent tests</div>';
                        }
                    }

                    function updateResponseTimeChart(data) {
                        const ctx = document.getElementById('responseTimeChart').getContext('2d');

                        if (responseTimeChart) {
                            responseTimeChart.destroy();
                        }

                        const dates = data.dates || [];
                        const responseTimes = dates.map(date => data.averageResponseTimes[date] || 0);

                        responseTimeChart = new Chart(ctx, {
                            type: 'line',
                            data: {
                                labels: dates,
                                datasets: [{
                                    label: 'Average Response Time (ms)',
                                    data: responseTimes,
                                    borderColor: '#17a2b8',
                                    backgroundColor: 'rgba(23, 162, 184, 0.1)',
                                    borderWidth: 2,
                                    fill: true,
                                    tension: 0.4
                                }]
                            },
                            options: {
                                responsive: true,
                                maintainAspectRatio: false,
                                scales: {
                                    y: {
                                        beginAtZero: true,
                                        title: {
                                            display: true,
                                            text: 'Response Time (ms)'
                                        }
                                    },
                                    x: {
                                        title: {
                                            display: true,
                                            text: 'Date'
                                        }
                                    }
                                },
                                plugins: {
                                    legend: {
                                        display: false
                                    }
                                }
                            }
                        });
                    }

                    function updateSuccessRateChart(data) {
                        const ctx = document.getElementById('successRateChart').getContext('2d');

                        if (successRateChart) {
                            successRateChart.destroy();
                        }

                        const dates = data.dates || [];
                        const successRates = dates.map(date => data.successRates[date] || 0);

                        successRateChart = new Chart(ctx, {
                            type: 'line',
                            data: {
                                labels: dates,
                                datasets: [{
                                    label: 'Success Rate (%)',
                                    data: successRates,
                                    borderColor: '#28a745',
                                    backgroundColor: 'rgba(40, 167, 69, 0.1)',
                                    borderWidth: 2,
                                    fill: true,
                                    tension: 0.4
                                }]
                            },
                            options: {
                                responsive: true,
                                maintainAspectRatio: false,
                                scales: {
                                    y: {
                                        beginAtZero: true,
                                        max: 100,
                                        title: {
                                            display: true,
                                            text: 'Success Rate (%)'
                                        }
                                    },
                                    x: {
                                        title: {
                                            display: true,
                                            text: 'Date'
                                        }
                                    }
                                },
                                plugins: {
                                    legend: {
                                        display: false
                                    }
                                }
                            }
                        });
                    }

                    function showError(message) {
                        const errorDiv = document.createElement('div');
                        errorDiv.className = 'error';
                        errorDiv.textContent = message;
                        document.querySelector('.container').insertBefore(errorDiv, document.querySelector('.dashboard-grid'));

                        setTimeout(() => {
                            errorDiv.remove();
                        }, 5000);
                    }

                    // Auto-refresh every 30 seconds
                    setInterval(refreshDashboard, 30000);
                </script>
            </body>
            </html>
            """;
    }

    /**
     * Get Prometheus metrics in text format
     */
    private String getPrometheusMetrics() {
        // This is a placeholder for Prometheus metrics
        // In a real implementation, you would use Micrometer or similar
        StringBuilder metrics = new StringBuilder();

        try {
            // Example metrics - replace with actual implementation
            metrics.append("# HELP performance_test_total Total number of performance tests\n");
            metrics.append("# TYPE performance_test_total counter\n");
            metrics.append("performance_test_total 27\n\n");

            metrics.append("# HELP performance_test_duration_seconds Performance test duration\n");
            metrics.append("# TYPE performance_test_duration_seconds histogram\n");
            metrics.append("performance_test_duration_seconds_sum 1.5\n");
            metrics.append("performance_test_duration_seconds_count 27\n\n");

            metrics.append("# HELP performance_test_success_rate Performance test success rate\n");
            metrics.append("# TYPE performance_test_success_rate gauge\n");
            metrics.append("performance_test_success_rate 0.925\n\n");

        } catch (Exception e) {
            logger.error("Failed to generate Prometheus metrics", e);
            metrics.append("# Error generating metrics\n");
        }

        return metrics.toString();
    }
}

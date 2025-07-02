package dev.mars.common.database;

import dev.mars.common.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * Database manager specifically for metrics data
 * Extends BaseDatabaseManager with metrics-specific schema
 */
public class MetricsDatabaseManager extends BaseDatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(MetricsDatabaseManager.class);

    public MetricsDatabaseManager(DatabaseConfig databaseConfig) {
        super(databaseConfig);
    }

    @Override
    public void initializeSchema() {
        logger.info("Initializing metrics database schema");
        
        String createPerformanceMetricsTableSql = """
            CREATE TABLE IF NOT EXISTS performance_metrics (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                test_name VARCHAR(255) NOT NULL,
                test_type VARCHAR(100) NOT NULL,
                timestamp TIMESTAMP NOT NULL,
                total_requests INTEGER,
                total_time_ms BIGINT,
                average_response_time_ms DOUBLE,
                concurrent_threads INTEGER,
                requests_per_thread INTEGER,
                page_size INTEGER,
                memory_usage_bytes BIGINT,
                memory_increase_bytes BIGINT,
                test_passed BOOLEAN,
                additional_metrics TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String createSystemMetricsTableSql = """
            CREATE TABLE IF NOT EXISTS system_metrics (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                metric_name VARCHAR(255) NOT NULL,
                metric_value DOUBLE NOT NULL,
                metric_unit VARCHAR(50),
                timestamp TIMESTAMP NOT NULL,
                additional_data TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String createApplicationMetricsTableSql = """
            CREATE TABLE IF NOT EXISTS application_metrics (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                application_name VARCHAR(255) NOT NULL,
                metric_type VARCHAR(100) NOT NULL,
                metric_data TEXT NOT NULL,
                timestamp TIMESTAMP NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

        try {
            executeSqlStatements(
                createPerformanceMetricsTableSql,
                createSystemMetricsTableSql,
                createApplicationMetricsTableSql
            );
            
            logger.info("Metrics database schema initialized successfully");

        } catch (SQLException e) {
            logger.error("Failed to initialize metrics database schema", e);
            throw new RuntimeException("Failed to initialize metrics database schema", e);
        }
    }

    /**
     * Clean all metrics data (useful for testing)
     */
    public void cleanMetricsData() {
        logger.info("Cleaning metrics database");
        
        String deletePerformanceMetricsSql = "DELETE FROM performance_metrics";
        String resetPerformanceMetricsSequenceSql = "ALTER TABLE performance_metrics ALTER COLUMN id RESTART WITH 1";
        
        String deleteSystemMetricsSql = "DELETE FROM system_metrics";
        String resetSystemMetricsSequenceSql = "ALTER TABLE system_metrics ALTER COLUMN id RESTART WITH 1";
        
        String deleteApplicationMetricsSql = "DELETE FROM application_metrics";
        String resetApplicationMetricsSequenceSql = "ALTER TABLE application_metrics ALTER COLUMN id RESTART WITH 1";

        try {
            executeSqlStatements(
                deletePerformanceMetricsSql,
                resetPerformanceMetricsSequenceSql,
                deleteSystemMetricsSql,
                resetSystemMetricsSequenceSql,
                deleteApplicationMetricsSql,
                resetApplicationMetricsSequenceSql
            );
            
            logger.info("Metrics database cleaned successfully");

        } catch (SQLException e) {
            logger.error("Failed to clean metrics database", e);
            throw new RuntimeException("Failed to clean metrics database", e);
        }
    }
}

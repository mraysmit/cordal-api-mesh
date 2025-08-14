package dev.cordal.database;

import dev.cordal.config.MetricsDatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database manager for metrics database
 */
@Singleton
public class MetricsDatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(MetricsDatabaseManager.class);
    
    private final DataSource dataSource;
    
    @Inject
    public MetricsDatabaseManager(MetricsDatabaseConfig databaseConfig) {
        this.dataSource = databaseConfig.getDataSource();
        initializeSchema();
    }
    
    /**
     * Get the metrics database data source
     */
    public DataSource getDataSource() {
        return dataSource;
    }
    
    /**
     * Initialize the metrics database schema
     */
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
                metric_type VARCHAR(50) NOT NULL,
                tags TEXT,
                timestamp TIMESTAMP NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        
        String createApplicationMetricsTableSql = """
            CREATE TABLE IF NOT EXISTS application_metrics (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                endpoint VARCHAR(255) NOT NULL,
                method VARCHAR(10) NOT NULL,
                status_code INTEGER NOT NULL,
                response_time_ms BIGINT NOT NULL,
                request_size_bytes BIGINT,
                response_size_bytes BIGINT,
                user_agent TEXT,
                ip_address VARCHAR(45),
                timestamp TIMESTAMP NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        
        // Performance metrics indexes
        String createIndexSql1 = "CREATE INDEX IF NOT EXISTS idx_performance_metrics_test_type ON performance_metrics(test_type)";
        String createIndexSql2 = "CREATE INDEX IF NOT EXISTS idx_performance_metrics_timestamp ON performance_metrics(timestamp)";
        String createIndexSql3 = "CREATE INDEX IF NOT EXISTS idx_performance_metrics_test_name ON performance_metrics(test_name)";
        
        // System metrics indexes
        String createIndexSql4 = "CREATE INDEX IF NOT EXISTS idx_system_metrics_name_timestamp ON system_metrics(metric_name, timestamp)";
        String createIndexSql5 = "CREATE INDEX IF NOT EXISTS idx_system_metrics_type ON system_metrics(metric_type)";
        
        // Application metrics indexes
        String createIndexSql6 = "CREATE INDEX IF NOT EXISTS idx_application_metrics_endpoint ON application_metrics(endpoint)";
        String createIndexSql7 = "CREATE INDEX IF NOT EXISTS idx_application_metrics_timestamp ON application_metrics(timestamp)";
        String createIndexSql8 = "CREATE INDEX IF NOT EXISTS idx_application_metrics_status ON application_metrics(status_code)";
        
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            // Create tables
            statement.execute(createPerformanceMetricsTableSql);
            logger.info("Performance metrics table created/verified");
            
            statement.execute(createSystemMetricsTableSql);
            logger.info("System metrics table created/verified");
            
            statement.execute(createApplicationMetricsTableSql);
            logger.info("Application metrics table created/verified");
            
            // Create indexes
            statement.execute(createIndexSql1);
            statement.execute(createIndexSql2);
            statement.execute(createIndexSql3);
            logger.info("Performance metrics indexes created/verified");
            
            statement.execute(createIndexSql4);
            statement.execute(createIndexSql5);
            logger.info("System metrics indexes created/verified");
            
            statement.execute(createIndexSql6);
            statement.execute(createIndexSql7);
            statement.execute(createIndexSql8);
            logger.info("Application metrics indexes created/verified");
            
        } catch (SQLException e) {
            logger.error("Failed to initialize metrics database schema", e);
            throw new RuntimeException("Failed to initialize metrics database schema", e);
        }
    }
    
    /**
     * Clean all data from the metrics database (for testing purposes)
     */
    public void cleanDatabase() {
        logger.info("Cleaning metrics database for testing");

        String deletePerformanceMetricsSql = "DELETE FROM performance_metrics";
        String deleteSystemMetricsSql = "DELETE FROM system_metrics";
        String deleteApplicationMetricsSql = "DELETE FROM application_metrics";
        
        String resetPerformanceMetricsSequenceSql = "ALTER TABLE performance_metrics ALTER COLUMN id RESTART WITH 1";
        String resetSystemMetricsSequenceSql = "ALTER TABLE system_metrics ALTER COLUMN id RESTART WITH 1";
        String resetApplicationMetricsSequenceSql = "ALTER TABLE application_metrics ALTER COLUMN id RESTART WITH 1";

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(deletePerformanceMetricsSql);
            statement.execute(resetPerformanceMetricsSequenceSql);
            
            statement.execute(deleteSystemMetricsSql);
            statement.execute(resetSystemMetricsSequenceSql);
            
            statement.execute(deleteApplicationMetricsSql);
            statement.execute(resetApplicationMetricsSequenceSql);
            
            logger.info("Metrics database cleaned successfully");

        } catch (SQLException e) {
            logger.error("Failed to clean metrics database", e);
            throw new RuntimeException("Failed to clean metrics database", e);
        }
    }
    
    /**
     * Get database connection for manual operations
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    /**
     * Check if the database is accessible
     */
    public boolean isHealthy() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("SELECT 1");
            return true;
        } catch (SQLException e) {
            logger.error("Metrics database health check failed", e);
            return false;
        }
    }
}

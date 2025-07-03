package dev.mars.repository;

import dev.mars.database.MetricsDatabaseManager;
import dev.mars.common.model.PerformanceMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for performance metrics data access
 */
@Singleton
public class PerformanceMetricsRepository {
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMetricsRepository.class);

    private final MetricsDatabaseManager metricsDatabaseManager;

    @Inject
    public PerformanceMetricsRepository(MetricsDatabaseManager metricsDatabaseManager) {
        this.metricsDatabaseManager = metricsDatabaseManager;
    }
    
    /**
     * Save performance metrics to database
     */
    public PerformanceMetrics save(PerformanceMetrics metrics) {
        String sql = """
            INSERT INTO performance_metrics (
                test_name, test_type, timestamp, total_requests, total_time_ms,
                average_response_time_ms, concurrent_threads, requests_per_thread,
                page_size, memory_usage_bytes, memory_increase_bytes, test_passed,
                additional_metrics
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection connection = metricsDatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            statement.setString(1, metrics.getTestName());
            statement.setString(2, metrics.getTestType());
            statement.setTimestamp(3, Timestamp.valueOf(metrics.getTimestamp()));
            statement.setObject(4, metrics.getTotalRequests());
            statement.setObject(5, metrics.getTotalTimeMs());
            statement.setObject(6, metrics.getAverageResponseTimeMs());
            statement.setObject(7, metrics.getConcurrentThreads());
            statement.setObject(8, metrics.getRequestsPerThread());
            statement.setObject(9, metrics.getPageSize());
            statement.setObject(10, metrics.getMemoryUsageBytes());
            statement.setObject(11, metrics.getMemoryIncreaseBytes());
            statement.setObject(12, metrics.getTestPassed());
            statement.setString(13, metrics.getAdditionalMetrics());
            
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating performance metrics failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    metrics.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating performance metrics failed, no ID obtained.");
                }
            }
            
            logger.debug("Saved performance metrics: {}", metrics);
            return metrics;
            
        } catch (SQLException e) {
            logger.error("Error saving performance metrics", e);
            throw new RuntimeException("Failed to save performance metrics", e);
        }
    }
    
    /**
     * Find performance metrics by ID
     */
    public Optional<PerformanceMetrics> findById(Long id) {
        String sql = "SELECT * FROM performance_metrics WHERE id = ?";
        
        try (Connection connection = metricsDatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setLong(1, id);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSetToPerformanceMetrics(resultSet));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error finding performance metrics by ID: {}", id, e);
            throw new RuntimeException("Failed to find performance metrics", e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Find all performance metrics with pagination
     */
    public List<PerformanceMetrics> findAll(int page, int size) {
        String sql = """
            SELECT * FROM performance_metrics 
            ORDER BY timestamp DESC 
            LIMIT ? OFFSET ?
            """;
        
        List<PerformanceMetrics> metrics = new ArrayList<>();
        
        try (Connection connection = metricsDatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, size);
            statement.setInt(2, page * size);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    metrics.add(mapResultSetToPerformanceMetrics(resultSet));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error finding all performance metrics", e);
            throw new RuntimeException("Failed to find performance metrics", e);
        }
        
        return metrics;
    }
    
    /**
     * Find performance metrics by test type
     */
    public List<PerformanceMetrics> findByTestType(String testType, int page, int size) {
        String sql = """
            SELECT * FROM performance_metrics 
            WHERE test_type = ? 
            ORDER BY timestamp DESC 
            LIMIT ? OFFSET ?
            """;
        
        List<PerformanceMetrics> metrics = new ArrayList<>();
        
        try (Connection connection = metricsDatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, testType);
            statement.setInt(2, size);
            statement.setInt(3, page * size);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    metrics.add(mapResultSetToPerformanceMetrics(resultSet));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error finding performance metrics by test type: {}", testType, e);
            throw new RuntimeException("Failed to find performance metrics", e);
        }
        
        return metrics;
    }
    
    /**
     * Find performance metrics within date range
     */
    public List<PerformanceMetrics> findByDateRange(LocalDateTime startDate, LocalDateTime endDate, int page, int size) {
        String sql = """
            SELECT * FROM performance_metrics 
            WHERE timestamp BETWEEN ? AND ? 
            ORDER BY timestamp DESC 
            LIMIT ? OFFSET ?
            """;
        
        List<PerformanceMetrics> metrics = new ArrayList<>();
        
        try (Connection connection = metricsDatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setTimestamp(1, Timestamp.valueOf(startDate));
            statement.setTimestamp(2, Timestamp.valueOf(endDate));
            statement.setInt(3, size);
            statement.setInt(4, page * size);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    metrics.add(mapResultSetToPerformanceMetrics(resultSet));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error finding performance metrics by date range", e);
            throw new RuntimeException("Failed to find performance metrics", e);
        }
        
        return metrics;
    }
    
    /**
     * Count total performance metrics
     */
    public long count() {
        String sql = "SELECT COUNT(*) FROM performance_metrics";
        
        try (Connection connection = metricsDatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
            
        } catch (SQLException e) {
            logger.error("Error counting performance metrics", e);
            throw new RuntimeException("Failed to count performance metrics", e);
        }
        
        return 0;
    }
    
    /**
     * Get distinct test types
     */
    public List<String> getDistinctTestTypes() {
        String sql = "SELECT DISTINCT test_type FROM performance_metrics ORDER BY test_type";
        
        List<String> testTypes = new ArrayList<>();
        
        try (Connection connection = metricsDatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            
            while (resultSet.next()) {
                testTypes.add(resultSet.getString("test_type"));
            }
            
        } catch (SQLException e) {
            logger.error("Error getting distinct test types", e);
            throw new RuntimeException("Failed to get test types", e);
        }
        
        return testTypes;
    }
    
    /**
     * Map ResultSet to PerformanceMetrics object
     */
    private PerformanceMetrics mapResultSetToPerformanceMetrics(ResultSet resultSet) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics();
        
        metrics.setId(resultSet.getLong("id"));
        metrics.setTestName(resultSet.getString("test_name"));
        metrics.setTestType(resultSet.getString("test_type"));
        
        Timestamp timestamp = resultSet.getTimestamp("timestamp");
        if (timestamp != null) {
            metrics.setTimestamp(timestamp.toLocalDateTime());
        }
        
        metrics.setTotalRequests(getIntegerOrNull(resultSet, "total_requests"));
        metrics.setTotalTimeMs(getLongOrNull(resultSet, "total_time_ms"));
        metrics.setAverageResponseTimeMs(getDoubleOrNull(resultSet, "average_response_time_ms"));
        metrics.setConcurrentThreads(getIntegerOrNull(resultSet, "concurrent_threads"));
        metrics.setRequestsPerThread(getIntegerOrNull(resultSet, "requests_per_thread"));
        metrics.setPageSize(getIntegerOrNull(resultSet, "page_size"));
        metrics.setMemoryUsageBytes(getLongOrNull(resultSet, "memory_usage_bytes"));
        metrics.setMemoryIncreaseBytes(getLongOrNull(resultSet, "memory_increase_bytes"));
        metrics.setTestPassed(getBooleanOrNull(resultSet, "test_passed"));
        metrics.setAdditionalMetrics(resultSet.getString("additional_metrics"));
        
        return metrics;
    }
    
    private Integer getIntegerOrNull(ResultSet resultSet, String columnName) throws SQLException {
        int value = resultSet.getInt(columnName);
        return resultSet.wasNull() ? null : value;
    }
    
    private Long getLongOrNull(ResultSet resultSet, String columnName) throws SQLException {
        long value = resultSet.getLong(columnName);
        return resultSet.wasNull() ? null : value;
    }
    
    private Double getDoubleOrNull(ResultSet resultSet, String columnName) throws SQLException {
        double value = resultSet.getDouble(columnName);
        return resultSet.wasNull() ? null : value;
    }
    
    private Boolean getBooleanOrNull(ResultSet resultSet, String columnName) throws SQLException {
        boolean value = resultSet.getBoolean(columnName);
        return resultSet.wasNull() ? null : value;
    }
}

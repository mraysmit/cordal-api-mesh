package dev.cordal.generic.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service to track usage statistics for endpoints, queries, and database connections
 */
@Singleton
public class UsageStatisticsService {
    private static final Logger logger = LoggerFactory.getLogger(UsageStatisticsService.class);
    
    private final Map<String, EndpointStatistics> endpointStats;
    private final Map<String, QueryStatistics> queryStats;
    private final Map<String, DatabaseStatistics> databaseStats;
    private final Instant serviceStartTime;
    
    public UsageStatisticsService() {
        this.endpointStats = new ConcurrentHashMap<>();
        this.queryStats = new ConcurrentHashMap<>();
        this.databaseStats = new ConcurrentHashMap<>();
        this.serviceStartTime = Instant.now();
        
        logger.info("Usage statistics service initialized at {}", serviceStartTime);
    }
    
    /**
     * Record endpoint usage
     */
    public void recordEndpointUsage(String endpointName, long executionTimeMs, boolean success) {
        endpointStats.computeIfAbsent(endpointName, k -> new EndpointStatistics(k))
                    .recordUsage(executionTimeMs, success);
        
        logger.debug("Recorded endpoint usage: {} ({}ms, success: {})", 
                    endpointName, executionTimeMs, success);
    }
    
    /**
     * Record query usage
     */
    public void recordQueryUsage(String queryName, String databaseName, long executionTimeMs, boolean success, int rowsReturned) {
        queryStats.computeIfAbsent(queryName, k -> new QueryStatistics(k))
                  .recordUsage(databaseName, executionTimeMs, success, rowsReturned);
        
        logger.debug("Recorded query usage: {} on {} ({}ms, {} rows, success: {})", 
                    queryName, databaseName, executionTimeMs, rowsReturned, success);
    }
    
    /**
     * Record database connection usage
     */
    public void recordDatabaseUsage(String databaseName, long connectionTimeMs, boolean success) {
        databaseStats.computeIfAbsent(databaseName, k -> new DatabaseStatistics(k))
                     .recordUsage(connectionTimeMs, success);
        
        logger.debug("Recorded database usage: {} ({}ms, success: {})", 
                    databaseName, connectionTimeMs, success);
    }
    
    /**
     * Get comprehensive usage statistics
     */
    public Map<String, Object> getUsageStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("serviceStartTime", serviceStartTime);
        stats.put("uptime", java.time.Duration.between(serviceStartTime, Instant.now()).toString());
        stats.put("endpoints", getEndpointStatistics());
        stats.put("queries", getQueryStatistics());
        stats.put("databases", getDatabaseStatistics());
        stats.put("summary", getSummaryStatistics());
        
        return stats;
    }
    
    /**
     * Get endpoint statistics
     */
    public Map<String, EndpointStatistics> getEndpointStatistics() {
        return new HashMap<>(endpointStats);
    }
    
    /**
     * Get query statistics
     */
    public Map<String, QueryStatistics> getQueryStatistics() {
        return new HashMap<>(queryStats);
    }
    
    /**
     * Get database statistics
     */
    public Map<String, DatabaseStatistics> getDatabaseStatistics() {
        return new HashMap<>(databaseStats);
    }
    
    /**
     * Get summary statistics
     */
    public Map<String, Object> getSummaryStatistics() {
        long totalEndpointCalls = endpointStats.values().stream()
                                               .mapToLong(s -> s.getTotalCalls())
                                               .sum();
        
        long totalQueryExecutions = queryStats.values().stream()
                                              .mapToLong(s -> s.getTotalExecutions())
                                              .sum();
        
        long totalDatabaseConnections = databaseStats.values().stream()
                                                     .mapToLong(s -> s.getTotalConnections())
                                                     .sum();
        
        return Map.of(
            "totalEndpointCalls", totalEndpointCalls,
            "totalQueryExecutions", totalQueryExecutions,
            "totalDatabaseConnections", totalDatabaseConnections,
            "activeEndpoints", endpointStats.size(),
            "activeQueries", queryStats.size(),
            "activeDatabases", databaseStats.size()
        );
    }
    
    /**
     * Endpoint statistics holder
     */
    public static class EndpointStatistics {
        private final String endpointName;
        private final AtomicLong totalCalls = new AtomicLong(0);
        private final AtomicLong successfulCalls = new AtomicLong(0);
        private final AtomicLong failedCalls = new AtomicLong(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private volatile long minExecutionTime = Long.MAX_VALUE;
        private volatile long maxExecutionTime = 0;
        private volatile Instant firstCall;
        private volatile Instant lastCall;
        
        public EndpointStatistics(String endpointName) {
            this.endpointName = endpointName;
        }
        
        public void recordUsage(long executionTimeMs, boolean success) {
            totalCalls.incrementAndGet();
            totalExecutionTime.addAndGet(executionTimeMs);
            
            if (success) {
                successfulCalls.incrementAndGet();
            } else {
                failedCalls.incrementAndGet();
            }
            
            // Update min/max execution times
            if (executionTimeMs < minExecutionTime) {
                minExecutionTime = executionTimeMs;
            }
            if (executionTimeMs > maxExecutionTime) {
                maxExecutionTime = executionTimeMs;
            }
            
            // Update timestamps
            Instant now = Instant.now();
            if (firstCall == null) {
                firstCall = now;
            }
            lastCall = now;
        }
        
        // Getters
        public String getEndpointName() { return endpointName; }
        public long getTotalCalls() { return totalCalls.get(); }
        public long getSuccessfulCalls() { return successfulCalls.get(); }
        public long getFailedCalls() { return failedCalls.get(); }
        public double getSuccessRate() { 
            long total = totalCalls.get();
            return total > 0 ? (double) successfulCalls.get() / total * 100 : 0.0;
        }
        public double getAverageExecutionTime() {
            long total = totalCalls.get();
            return total > 0 ? (double) totalExecutionTime.get() / total : 0.0;
        }
        public long getMinExecutionTime() { return minExecutionTime == Long.MAX_VALUE ? 0 : minExecutionTime; }
        public long getMaxExecutionTime() { return maxExecutionTime; }
        public Instant getFirstCall() { return firstCall; }
        public Instant getLastCall() { return lastCall; }
    }
    
    /**
     * Query statistics holder
     */
    public static class QueryStatistics {
        private final String queryName;
        private final AtomicLong totalExecutions = new AtomicLong(0);
        private final AtomicLong successfulExecutions = new AtomicLong(0);
        private final AtomicLong failedExecutions = new AtomicLong(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private final AtomicLong totalRowsReturned = new AtomicLong(0);
        private final Map<String, AtomicLong> databaseUsage = new ConcurrentHashMap<>();
        private volatile Instant firstExecution;
        private volatile Instant lastExecution;
        
        public QueryStatistics(String queryName) {
            this.queryName = queryName;
        }
        
        public void recordUsage(String databaseName, long executionTimeMs, boolean success, int rowsReturned) {
            totalExecutions.incrementAndGet();
            totalExecutionTime.addAndGet(executionTimeMs);
            totalRowsReturned.addAndGet(rowsReturned);
            
            if (success) {
                successfulExecutions.incrementAndGet();
            } else {
                failedExecutions.incrementAndGet();
            }
            
            // Track database usage
            databaseUsage.computeIfAbsent(databaseName, k -> new AtomicLong(0)).incrementAndGet();
            
            // Update timestamps
            Instant now = Instant.now();
            if (firstExecution == null) {
                firstExecution = now;
            }
            lastExecution = now;
        }
        
        // Getters
        public String getQueryName() { return queryName; }
        public long getTotalExecutions() { return totalExecutions.get(); }
        public long getSuccessfulExecutions() { return successfulExecutions.get(); }
        public long getFailedExecutions() { return failedExecutions.get(); }
        public double getSuccessRate() {
            long total = totalExecutions.get();
            return total > 0 ? (double) successfulExecutions.get() / total * 100 : 0.0;
        }
        public double getAverageExecutionTime() {
            long total = totalExecutions.get();
            return total > 0 ? (double) totalExecutionTime.get() / total : 0.0;
        }
        public double getAverageRowsReturned() {
            long total = totalExecutions.get();
            return total > 0 ? (double) totalRowsReturned.get() / total : 0.0;
        }
        public Map<String, Long> getDatabaseUsage() {
            Map<String, Long> usage = new HashMap<>();
            databaseUsage.forEach((db, count) -> usage.put(db, count.get()));
            return usage;
        }
        public Instant getFirstExecution() { return firstExecution; }
        public Instant getLastExecution() { return lastExecution; }
    }
    
    /**
     * Database statistics holder
     */
    public static class DatabaseStatistics {
        private final String databaseName;
        private final AtomicLong totalConnections = new AtomicLong(0);
        private final AtomicLong successfulConnections = new AtomicLong(0);
        private final AtomicLong failedConnections = new AtomicLong(0);
        private final AtomicLong totalConnectionTime = new AtomicLong(0);
        private volatile Instant firstConnection;
        private volatile Instant lastConnection;
        
        public DatabaseStatistics(String databaseName) {
            this.databaseName = databaseName;
        }
        
        public void recordUsage(long connectionTimeMs, boolean success) {
            totalConnections.incrementAndGet();
            totalConnectionTime.addAndGet(connectionTimeMs);
            
            if (success) {
                successfulConnections.incrementAndGet();
            } else {
                failedConnections.incrementAndGet();
            }
            
            // Update timestamps
            Instant now = Instant.now();
            if (firstConnection == null) {
                firstConnection = now;
            }
            lastConnection = now;
        }
        
        // Getters
        public String getDatabaseName() { return databaseName; }
        public long getTotalConnections() { return totalConnections.get(); }
        public long getSuccessfulConnections() { return successfulConnections.get(); }
        public long getFailedConnections() { return failedConnections.get(); }
        public double getSuccessRate() {
            long total = totalConnections.get();
            return total > 0 ? (double) successfulConnections.get() / total * 100 : 0.0;
        }
        public double getAverageConnectionTime() {
            long total = totalConnections.get();
            return total > 0 ? (double) totalConnectionTime.get() / total : 0.0;
        }
        public Instant getFirstConnection() { return firstConnection; }
        public Instant getLastConnection() { return lastConnection; }
    }
}

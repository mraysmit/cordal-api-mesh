package dev.cordal.dto;

import java.time.Instant;

/**
 * Type-safe response for configuration statistics
 */
public record ConfigurationStatisticsResponse(
    String source,
    Instant timestamp,
    StatisticsData statistics,
    SummaryData summary
) {
    
    public record StatisticsData(
        DatabaseStats databases,
        QueryStats queries,
        EndpointStats endpoints
    ) {}
    
    public record DatabaseStats(
        int total
    ) {}
    
    public record QueryStats(
        int total
    ) {}
    
    public record EndpointStats(
        int total
    ) {}
    
    public record SummaryData(
        int totalConfigurations,
        int databasesCount,
        int queriesCount,
        int endpointsCount
    ) {}
}

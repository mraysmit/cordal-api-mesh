package dev.cordal.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Type-safe response for configuration lists (databases, queries, endpoints)
 */
public record ConfigurationListResponse<T>(
    int count,
    String source,
    Instant timestamp,
    Map<String, T> configurations
) {}

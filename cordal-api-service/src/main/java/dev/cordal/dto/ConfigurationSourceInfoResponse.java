package dev.cordal.dto;

import java.time.Instant;
import java.util.List;

/**
 * Type-safe response for configuration source information
 */
public record ConfigurationSourceInfoResponse(
    String currentSource,
    boolean managementAvailable,
    List<String> supportedSources,
    Instant timestamp
) {}

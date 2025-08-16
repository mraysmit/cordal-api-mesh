package dev.cordal.hotreload;

import java.time.Instant;

/**
 * Result of an endpoint registration/unregistration operation
 */
public class EndpointRegistrationResult {
    private final boolean success;
    private final String message;
    private final String error;
    private final Instant timestamp;
    
    private EndpointRegistrationResult(boolean success, String message, String error) {
        this.success = success;
        this.message = message;
        this.error = error;
        this.timestamp = Instant.now();
    }
    
    public static EndpointRegistrationResult success(String message) {
        return new EndpointRegistrationResult(true, message, null);
    }
    
    public static EndpointRegistrationResult failure(String error) {
        return new EndpointRegistrationResult(false, null, error);
    }
    
    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getError() { return error; }
    public Instant getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return String.format("EndpointRegistrationResult{success=%s, message='%s', error='%s'}",
                           success, message, error);
    }
}

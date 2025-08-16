package dev.cordal.hotreload;

import java.time.Instant;

/**
 * Result of an atomic configuration update operation
 */
public class AtomicUpdateResult {
    private final String updateId;
    private final boolean success;
    private final String message;
    private final String error;
    private final Instant timestamp;
    private final DatabaseUpdateResult databaseResult;
    private final EndpointUpdateResult endpointResult;
    private final ValidationResult validationResult;
    
    private AtomicUpdateResult(Builder builder) {
        this.updateId = builder.updateId;
        this.success = builder.success;
        this.message = builder.message;
        this.error = builder.error;
        this.timestamp = Instant.now();
        this.databaseResult = builder.databaseResult;
        this.endpointResult = builder.endpointResult;
        this.validationResult = builder.validationResult;
    }
    
    // Getters
    public String getUpdateId() { return updateId; }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getError() { return error; }
    public Instant getTimestamp() { return timestamp; }
    public DatabaseUpdateResult getDatabaseResult() { return databaseResult; }
    public EndpointUpdateResult getEndpointResult() { return endpointResult; }
    public ValidationResult getValidationResult() { return validationResult; }
    
    @Override
    public String toString() {
        return String.format("AtomicUpdateResult{id='%s', success=%s, message='%s', error='%s'}",
                           updateId, success, message, error);
    }
    
    /**
     * Builder for AtomicUpdateResult
     */
    public static class Builder {
        private final String updateId;
        private boolean success = false;
        private String message;
        private String error;
        private DatabaseUpdateResult databaseResult;
        private EndpointUpdateResult endpointResult;
        private ValidationResult validationResult;
        
        public Builder(String updateId) {
            this.updateId = updateId;
        }
        
        public Builder success(String message) {
            this.success = true;
            this.message = message;
            return this;
        }
        
        public Builder failure(String error) {
            this.success = false;
            this.error = error;
            return this;
        }
        
        public Builder databaseResult(DatabaseUpdateResult databaseResult) {
            this.databaseResult = databaseResult;
            return this;
        }
        
        public Builder endpointResult(EndpointUpdateResult endpointResult) {
            this.endpointResult = endpointResult;
            return this;
        }
        
        public Builder validationResult(ValidationResult validationResult) {
            this.validationResult = validationResult;
            return this;
        }
        
        public AtomicUpdateResult build() {
            return new AtomicUpdateResult(this);
        }
    }
}

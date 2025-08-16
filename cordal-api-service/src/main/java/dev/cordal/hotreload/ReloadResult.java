package dev.cordal.hotreload;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Result of a configuration reload operation
 */
public class ReloadResult {
    private final boolean success;
    private final String message;
    private final Instant timestamp;
    private final Duration duration;
    private final ConfigurationDelta delta;
    private final AtomicUpdateResult updateResult;
    private final List<String> errors;
    private final List<String> warnings;
    
    private ReloadResult(Builder builder) {
        this.success = builder.success;
        this.message = builder.message;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.duration = builder.duration;
        this.delta = builder.delta;
        this.updateResult = builder.updateResult;
        this.errors = builder.errors != null ? builder.errors : List.of();
        this.warnings = builder.warnings != null ? builder.warnings : List.of();
    }
    
    /**
     * Create a successful reload result
     */
    public static ReloadResult success(String message) {
        return new Builder()
            .success(true)
            .message(message)
            .build();
    }
    
    /**
     * Create a successful reload result with delta
     */
    public static ReloadResult success(String message, ConfigurationDelta delta) {
        return new Builder()
            .success(true)
            .message(message)
            .delta(delta)
            .build();
    }
    
    /**
     * Create a successful reload result with delta and update result
     */
    public static ReloadResult success(String message, ConfigurationDelta delta, AtomicUpdateResult updateResult) {
        return new Builder()
            .success(true)
            .message(message)
            .delta(delta)
            .updateResult(updateResult)
            .build();
    }
    
    /**
     * Create a failed reload result
     */
    public static ReloadResult failure(String message) {
        return new Builder()
            .success(false)
            .message(message)
            .errors(List.of(message))
            .build();
    }
    
    /**
     * Create a failed reload result with errors
     */
    public static ReloadResult failure(String message, List<String> errors) {
        return new Builder()
            .success(false)
            .message(message)
            .errors(errors)
            .build();
    }
    
    /**
     * Create a failed reload result with single error
     */
    public static ReloadResult failure(String message, String error) {
        return new Builder()
            .success(false)
            .message(message)
            .errors(List.of(error))
            .build();
    }
    
    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Instant getTimestamp() { return timestamp; }
    public Duration getDuration() { return duration; }
    public ConfigurationDelta getDelta() { return delta; }
    public AtomicUpdateResult getUpdateResult() { return updateResult; }
    public List<String> getErrors() { return errors; }
    public List<String> getWarnings() { return warnings; }
    
    public boolean hasChanges() {
        return delta != null && delta.hasChanges();
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ReloadResult{");
        sb.append("success=").append(success);
        sb.append(", message='").append(message).append('\'');
        sb.append(", timestamp=").append(timestamp);
        
        if (duration != null) {
            sb.append(", duration=").append(duration.toMillis()).append("ms");
        }
        
        if (delta != null) {
            sb.append(", changes=").append(delta.getTotalChanges());
        }
        
        if (hasErrors()) {
            sb.append(", errors=").append(errors.size());
        }
        
        if (hasWarnings()) {
            sb.append(", warnings=").append(warnings.size());
        }
        
        sb.append('}');
        return sb.toString();
    }
    
    /**
     * Builder for ReloadResult
     */
    public static class Builder {
        private boolean success;
        private String message;
        private Instant timestamp;
        private Duration duration;
        private ConfigurationDelta delta;
        private AtomicUpdateResult updateResult;
        private List<String> errors;
        private List<String> warnings;
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder duration(Duration duration) {
            this.duration = duration;
            return this;
        }
        
        public Builder delta(ConfigurationDelta delta) {
            this.delta = delta;
            return this;
        }
        
        public Builder updateResult(AtomicUpdateResult updateResult) {
            this.updateResult = updateResult;
            return this;
        }
        
        public Builder errors(List<String> errors) {
            this.errors = errors;
            return this;
        }
        
        public Builder warnings(List<String> warnings) {
            this.warnings = warnings;
            return this;
        }
        
        public ReloadResult build() {
            return new ReloadResult(this);
        }
    }
}

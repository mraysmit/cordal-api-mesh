package dev.cordal.hotreload;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Represents a request to reload configuration
 */
public class ReloadRequest {
    private final String requestId;
    private final ReloadTrigger trigger;
    private final Instant timestamp;
    private final List<String> specificFiles;
    private final boolean validateOnly;
    private final boolean force;
    
    private ReloadRequest(Builder builder) {
        this.requestId = builder.requestId != null ? builder.requestId : generateRequestId();
        this.trigger = builder.trigger;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.specificFiles = builder.specificFiles;
        this.validateOnly = builder.validateOnly;
        this.force = builder.force;
    }
    
    /**
     * Create a reload request from a file change event
     */
    public static ReloadRequest fromFileChange(FileChangeEvent event) {
        return new Builder()
            .trigger(ReloadTrigger.FILE_CHANGE)
            .specificFiles(List.of(event.getFilePath().toString()))
            .build();
    }
    
    /**
     * Create a manual reload request
     */
    public static ReloadRequest manual() {
        return new Builder()
            .trigger(ReloadTrigger.MANUAL)
            .build();
    }
    
    /**
     * Create a validation-only request
     */
    public static ReloadRequest validationOnly() {
        return new Builder()
            .trigger(ReloadTrigger.VALIDATION)
            .validateOnly(true)
            .build();
    }
    
    /**
     * Create a forced reload request (bypasses circuit breaker)
     */
    public static ReloadRequest forced() {
        return new Builder()
            .trigger(ReloadTrigger.FORCED)
            .force(true)
            .build();
    }
    
    // Getters
    public String getRequestId() { return requestId; }
    public ReloadTrigger getTrigger() { return trigger; }
    public Instant getTimestamp() { return timestamp; }
    public List<String> getSpecificFiles() { return specificFiles; }
    public boolean isValidateOnly() { return validateOnly; }
    public boolean isForce() { return force; }
    
    @Override
    public String toString() {
        return String.format("ReloadRequest{id='%s', trigger=%s, files=%s, validateOnly=%s, force=%s}",
                           requestId, trigger, specificFiles, validateOnly, force);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReloadRequest that = (ReloadRequest) o;
        return Objects.equals(requestId, that.requestId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(requestId);
    }
    
    private static String generateRequestId() {
        return "req-" + Instant.now().toEpochMilli() + "-" + System.nanoTime() % 10000;
    }
    
    /**
     * Builder for ReloadRequest
     */
    public static class Builder {
        private String requestId;
        private ReloadTrigger trigger = ReloadTrigger.MANUAL;
        private Instant timestamp;
        private List<String> specificFiles = List.of();
        private boolean validateOnly = false;
        private boolean force = false;
        
        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }
        
        public Builder trigger(ReloadTrigger trigger) {
            this.trigger = trigger;
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder specificFiles(List<String> specificFiles) {
            this.specificFiles = specificFiles != null ? specificFiles : List.of();
            return this;
        }
        
        public Builder validateOnly(boolean validateOnly) {
            this.validateOnly = validateOnly;
            return this;
        }
        
        public Builder force(boolean force) {
            this.force = force;
            return this;
        }
        
        public ReloadRequest build() {
            return new ReloadRequest(this);
        }
    }
    
    /**
     * Enumeration of reload trigger types
     */
    public enum ReloadTrigger {
        FILE_CHANGE,    // Triggered by file system change
        MANUAL,         // Triggered manually via API
        VALIDATION,     // Validation-only request
        FORCED,         // Forced reload (bypasses safety checks)
        SCHEDULED       // Triggered by scheduler (future use)
    }
}

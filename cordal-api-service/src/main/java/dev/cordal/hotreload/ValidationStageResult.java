package dev.cordal.hotreload;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of a single validation stage
 */
public class ValidationStageResult {
    private final String stageName;
    private final List<String> errors;
    private final List<String> warnings;
    private final boolean valid;
    private final long durationMs;
    
    private ValidationStageResult(Builder builder) {
        this.stageName = builder.stageName;
        this.errors = List.copyOf(builder.errors);
        this.warnings = List.copyOf(builder.warnings);
        this.valid = builder.errors.isEmpty();
        this.durationMs = builder.durationMs;
    }
    
    public String getStageName() { return stageName; }
    public List<String> getErrors() { return errors; }
    public List<String> getWarnings() { return warnings; }
    public boolean isValid() { return valid; }
    public long getDurationMs() { return durationMs; }
    
    public boolean hasErrors() { return !errors.isEmpty(); }
    public boolean hasWarnings() { return !warnings.isEmpty(); }
    public int getErrorCount() { return errors.size(); }
    public int getWarningCount() { return warnings.size(); }
    
    @Override
    public String toString() {
        return String.format("ValidationStageResult{stage='%s', valid=%s, errors=%d, warnings=%d, duration=%dms}",
                           stageName, valid, errors.size(), warnings.size(), durationMs);
    }
    
    /**
     * Builder for ValidationStageResult
     */
    public static class Builder {
        private final String stageName;
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private long durationMs = 0;
        private long startTime = System.currentTimeMillis();
        
        public Builder(String stageName) {
            this.stageName = stageName;
        }
        
        public Builder addError(String error) {
            errors.add(error);
            return this;
        }
        
        public Builder addWarning(String warning) {
            warnings.add(warning);
            return this;
        }
        
        public Builder duration(long durationMs) {
            this.durationMs = durationMs;
            return this;
        }
        
        public int getErrorCount() {
            return errors.size();
        }
        
        public int getWarningCount() {
            return warnings.size();
        }
        
        public ValidationStageResult build() {
            if (durationMs == 0) {
                durationMs = System.currentTimeMillis() - startTime;
            }
            return new ValidationStageResult(this);
        }
    }
}

package dev.cordal.hotreload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of the complete validation pipeline
 */
public class ValidationResult {
    private final Map<String, ValidationStageResult> stageResults;
    private final List<String> errors;
    private final List<String> warnings;
    private final long durationMs;
    private final boolean valid;
    
    private ValidationResult(Builder builder) {
        this.stageResults = Map.copyOf(builder.stageResults);
        this.errors = List.copyOf(builder.errors);
        this.warnings = List.copyOf(builder.warnings);
        this.durationMs = builder.durationMs;
        this.valid = builder.errors.isEmpty();
    }
    
    public Map<String, ValidationStageResult> getStageResults() { return stageResults; }
    public List<String> getErrors() { return errors; }
    public List<String> getWarnings() { return warnings; }
    public long getDurationMs() { return durationMs; }
    public boolean isValid() { return valid; }
    
    public boolean hasErrors() { return !errors.isEmpty(); }
    public boolean hasWarnings() { return !warnings.isEmpty(); }
    public int getErrorCount() { return errors.size(); }
    public int getWarningCount() { return warnings.size(); }
    
    public ValidationStageResult getStageResult(String stageName) {
        return stageResults.get(stageName);
    }
    
    public boolean isStageValid(String stageName) {
        ValidationStageResult result = stageResults.get(stageName);
        return result != null && result.isValid();
    }
    
    @Override
    public String toString() {
        return String.format("ValidationResult{valid=%s, stages=%d, errors=%d, warnings=%d, duration=%dms}",
                           valid, stageResults.size(), errors.size(), warnings.size(), durationMs);
    }
    
    /**
     * Builder for ValidationResult
     */
    public static class Builder {
        private final Map<String, ValidationStageResult> stageResults = new HashMap<>();
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private long durationMs = 0;
        
        public Builder addStageResult(String stageName, ValidationStageResult result) {
            stageResults.put(stageName, result);
            errors.addAll(result.getErrors());
            warnings.addAll(result.getWarnings());
            return this;
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
        
        public ValidationResult build() {
            return new ValidationResult(this);
        }
    }
}

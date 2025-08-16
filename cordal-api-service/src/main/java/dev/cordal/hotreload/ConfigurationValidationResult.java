package dev.cordal.hotreload;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of configuration validation
 */
public class ConfigurationValidationResult {
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    
    public void addError(String error) {
        errors.add(error);
    }
    
    public void addWarning(String warning) {
        warnings.add(warning);
    }
    
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
    
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    public boolean isValid() {
        return !hasErrors();
    }
    
    public int getErrorCount() {
        return errors.size();
    }
    
    public int getWarningCount() {
        return warnings.size();
    }
    
    @Override
    public String toString() {
        return String.format("ConfigurationValidationResult{valid=%s, errors=%d, warnings=%d}",
                           isValid(), getErrorCount(), getWarningCount());
    }
}

package dev.mars.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Validation result container for configuration and schema validation
 */
public class ValidationResult {
    private final List<String> errors = new ArrayList<>();
    private final List<String> successes = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public void addError(String error) {
        errors.add(error);
    }

    public void addSuccess(String success) {
        successes.add(success);
    }

    public void addWarning(String warning) {
        warnings.add(warning);
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getSuccesses() {
        return successes;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public int getErrorCount() {
        return errors.size();
    }

    public int getSuccessCount() {
        return successes.size();
    }

    public int getWarningCount() {
        return warnings.size();
    }

    public boolean isSuccess() {
        return errors.isEmpty();
    }
}

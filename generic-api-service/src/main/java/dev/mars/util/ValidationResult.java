package dev.mars.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Validation result container for configuration and schema validation
 */
public class ValidationResult {
    private final List<String> errors = new ArrayList<>();
    private final List<String> successes = new ArrayList<>();

    public void addError(String error) {
        errors.add(error);
    }

    public void addSuccess(String success) {
        successes.add(success);
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getSuccesses() {
        return successes;
    }

    public int getErrorCount() {
        return errors.size();
    }

    public int getSuccessCount() {
        return successes.size();
    }

    public boolean isSuccess() {
        return errors.isEmpty();
    }
}

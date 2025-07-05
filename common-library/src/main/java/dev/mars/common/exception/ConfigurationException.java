package dev.mars.common.exception;

/**
 * Exception thrown when configuration loading or validation fails.
 * This exception is used instead of System.exit() calls to allow proper
 * error handling in tests and other contexts.
 */
public class ConfigurationException extends RuntimeException {
    
    public ConfigurationException(String message) {
        super(message);
    }
    
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ConfigurationException(Throwable cause) {
        super(cause);
    }
}

package dev.mars.common.exception;

/**
 * Custom API exception for handling application-specific errors
 * Common exception class used across all modules
 */
public class ApiException extends RuntimeException {
    private final String errorCode;
    private final int statusCode;

    public ApiException(String errorCode, String message, int statusCode) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }

    public ApiException(String errorCode, String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    // Static factory methods for common error types
    public static ApiException badRequest(String message) {
        return new ApiException("BAD_REQUEST", message, 400);
    }

    public static ApiException notFound(String message) {
        return new ApiException("NOT_FOUND", message, 404);
    }

    public static ApiException internalError(String message) {
        return new ApiException("INTERNAL_ERROR", message, 500);
    }

    public static ApiException internalError(String message, Throwable cause) {
        return new ApiException("INTERNAL_ERROR", message, 500, cause);
    }

    public static ApiException conflict(String message) {
        return new ApiException("CONFLICT", message, 409);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException("UNAUTHORIZED", message, 401);
    }

    public static ApiException forbidden(String message) {
        return new ApiException("FORBIDDEN", message, 403);
    }

    @Override
    public String toString() {
        return "ApiException{" +
               "errorCode='" + errorCode + '\'' +
               ", statusCode=" + statusCode +
               ", message='" + getMessage() + '\'' +
               '}';
    }
}

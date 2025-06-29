package dev.mars.exception;

/**
 * Custom API exception for handling application-specific errors
 */
public class ApiException extends RuntimeException {
    private final int statusCode;
    private final String errorCode;

    public ApiException(String message) {
        super(message);
        this.statusCode = 500;
        this.errorCode = "INTERNAL_ERROR";
    }

    public ApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = "API_ERROR";
    }

    public ApiException(String message, int statusCode, String errorCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 500;
        this.errorCode = "INTERNAL_ERROR";
    }

    public ApiException(String message, Throwable cause, int statusCode, String errorCode) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    // Static factory methods for common errors
    public static ApiException notFound(String message) {
        return new ApiException(message, 404, "NOT_FOUND");
    }

    public static ApiException badRequest(String message) {
        return new ApiException(message, 400, "BAD_REQUEST");
    }

    public static ApiException internalError(String message) {
        return new ApiException(message, 500, "INTERNAL_ERROR");
    }

    public static ApiException internalError(String message, Throwable cause) {
        return new ApiException(message, cause, 500, "INTERNAL_ERROR");
    }
}

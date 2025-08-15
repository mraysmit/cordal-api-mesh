package dev.cordal.integration.postgresql.client;

/**
 * Result of an API test operation that may succeed or fail intentionally
 * This class is designed to handle expected error scenarios in testing without throwing exceptions
 * that would confuse observers about whether errors are intentional or real failures.
 */
public class ApiTestResult {
    private final boolean success;
    private final int statusCode;
    private final String message;
    private final String responseBody;
    private final boolean expectedError;
    
    /**
     * Create a successful API test result
     */
    public static ApiTestResult success(int statusCode, String responseBody) {
        return new ApiTestResult(true, statusCode, "Success", responseBody, false);
    }
    
    /**
     * Create an expected error API test result (for intentional error testing)
     */
    public static ApiTestResult expectedError(int statusCode, String message, String responseBody) {
        return new ApiTestResult(false, statusCode, message, responseBody, true);
    }
    
    /**
     * Create an unexpected error API test result (for real failures)
     */
    public static ApiTestResult unexpectedError(int statusCode, String message, String responseBody) {
        return new ApiTestResult(false, statusCode, message, responseBody, false);
    }
    
    /**
     * Create an exception-based error result
     */
    public static ApiTestResult exception(Exception e) {
        return new ApiTestResult(false, -1, "Exception: " + e.getMessage(), null, false);
    }
    
    private ApiTestResult(boolean success, int statusCode, String message, String responseBody, boolean expectedError) {
        this.success = success;
        this.statusCode = statusCode;
        this.message = message;
        this.responseBody = responseBody;
        this.expectedError = expectedError;
    }
    
    /**
     * @return true if the API call was successful
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * @return true if this was an expected error (intentional test scenario)
     */
    public boolean isExpectedError() {
        return expectedError;
    }
    
    /**
     * @return true if this was an unexpected error (real failure)
     */
    public boolean isUnexpectedError() {
        return !success && !expectedError;
    }
    
    /**
     * @return HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }
    
    /**
     * @return Result message
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * @return Response body (may be null)
     */
    public String getResponseBody() {
        return responseBody;
    }
    
    /**
     * @return true if the status code indicates a client error (4xx)
     */
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }
    
    /**
     * @return true if the status code indicates a server error (5xx)
     */
    public boolean isServerError() {
        return statusCode >= 500 && statusCode < 600;
    }
    
    @Override
    public String toString() {
        return String.format("ApiTestResult{success=%s, statusCode=%d, message='%s', expectedError=%s}", 
                           success, statusCode, message, expectedError);
    }
}

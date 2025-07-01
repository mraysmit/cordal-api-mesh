package dev.mars.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ApiException
 */
class ApiExceptionTest {

    @Test
    void testDefaultConstructor() {
        ApiException exception = new ApiException("Test message");
        
        assertThat(exception.getMessage()).isEqualTo("Test message");
        assertThat(exception.getStatusCode()).isEqualTo(500);
        assertThat(exception.getErrorCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void testConstructorWithStatusCode() {
        ApiException exception = new ApiException("Bad request", 400);
        
        assertThat(exception.getMessage()).isEqualTo("Bad request");
        assertThat(exception.getStatusCode()).isEqualTo(400);
        assertThat(exception.getErrorCode()).isEqualTo("API_ERROR");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void testConstructorWithStatusCodeAndErrorCode() {
        ApiException exception = new ApiException("Not found", 404, "RESOURCE_NOT_FOUND");
        
        assertThat(exception.getMessage()).isEqualTo("Not found");
        assertThat(exception.getStatusCode()).isEqualTo(404);
        assertThat(exception.getErrorCode()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void testConstructorWithCause() {
        RuntimeException cause = new RuntimeException("Root cause");
        ApiException exception = new ApiException("Wrapper message", cause);
        
        assertThat(exception.getMessage()).isEqualTo("Wrapper message");
        assertThat(exception.getStatusCode()).isEqualTo(500);
        assertThat(exception.getErrorCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void testConstructorWithCauseStatusCodeAndErrorCode() {
        RuntimeException cause = new RuntimeException("Root cause");
        ApiException exception = new ApiException("Wrapper message", cause, 503, "SERVICE_UNAVAILABLE");
        
        assertThat(exception.getMessage()).isEqualTo("Wrapper message");
        assertThat(exception.getStatusCode()).isEqualTo(503);
        assertThat(exception.getErrorCode()).isEqualTo("SERVICE_UNAVAILABLE");
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void testNotFoundStaticMethod() {
        ApiException exception = ApiException.notFound("Resource not found");
        
        assertThat(exception.getMessage()).isEqualTo("Resource not found");
        assertThat(exception.getStatusCode()).isEqualTo(404);
        assertThat(exception.getErrorCode()).isEqualTo("NOT_FOUND");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void testBadRequestStaticMethod() {
        ApiException exception = ApiException.badRequest("Invalid input");
        
        assertThat(exception.getMessage()).isEqualTo("Invalid input");
        assertThat(exception.getStatusCode()).isEqualTo(400);
        assertThat(exception.getErrorCode()).isEqualTo("BAD_REQUEST");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void testInternalErrorStaticMethod() {
        ApiException exception = ApiException.internalError("Server error");
        
        assertThat(exception.getMessage()).isEqualTo("Server error");
        assertThat(exception.getStatusCode()).isEqualTo(500);
        assertThat(exception.getErrorCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void testInternalErrorStaticMethodWithCause() {
        RuntimeException cause = new RuntimeException("Database error");
        ApiException exception = ApiException.internalError("Server error", cause);
        
        assertThat(exception.getMessage()).isEqualTo("Server error");
        assertThat(exception.getStatusCode()).isEqualTo(500);
        assertThat(exception.getErrorCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void testExceptionInheritance() {
        ApiException exception = new ApiException("Test");
        
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception).isInstanceOf(Exception.class);
        assertThat(exception).isInstanceOf(Throwable.class);
    }
}

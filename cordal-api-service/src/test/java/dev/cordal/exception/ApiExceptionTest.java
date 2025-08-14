package dev.cordal.exception;

import dev.cordal.common.exception.ApiException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ApiException
 */
class ApiExceptionTest {

    @Test
    void testConstructorWithAllParameters() {
        ApiException exception = new ApiException("TEST_ERROR", "Test message", 500);

        assertThat(exception.getMessage()).isEqualTo("Test message");
        assertThat(exception.getStatusCode()).isEqualTo(500);
        assertThat(exception.getErrorCode()).isEqualTo("TEST_ERROR");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void testConstructorWithCause() {
        RuntimeException cause = new RuntimeException("Root cause");
        ApiException exception = new ApiException("TEST_ERROR", "Test message", 400, cause);

        assertThat(exception.getMessage()).isEqualTo("Test message");
        assertThat(exception.getStatusCode()).isEqualTo(400);
        assertThat(exception.getErrorCode()).isEqualTo("TEST_ERROR");
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
        ApiException exception = new ApiException("TEST_ERROR", "Test message", 500);

        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception).isInstanceOf(Exception.class);
        assertThat(exception).isInstanceOf(Throwable.class);
    }
}

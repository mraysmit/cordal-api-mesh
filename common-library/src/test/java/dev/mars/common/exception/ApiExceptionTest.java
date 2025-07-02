package dev.mars.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ApiException
 */
class ApiExceptionTest {

    @Test
    void shouldCreateApiExceptionWithAllParameters() {
        ApiException exception = new ApiException("TEST_ERROR", "Test message", 400);

        assertThat(exception.getErrorCode()).isEqualTo("TEST_ERROR");
        assertThat(exception.getMessage()).isEqualTo("Test message");
        assertThat(exception.getStatusCode()).isEqualTo(400);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateApiExceptionWithCause() {
        RuntimeException cause = new RuntimeException("Root cause");
        ApiException exception = new ApiException("TEST_ERROR", "Test message", 500, cause);

        assertThat(exception.getErrorCode()).isEqualTo("TEST_ERROR");
        assertThat(exception.getMessage()).isEqualTo("Test message");
        assertThat(exception.getStatusCode()).isEqualTo(500);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void shouldCreateBadRequestException() {
        ApiException exception = ApiException.badRequest("Invalid input");

        assertThat(exception.getErrorCode()).isEqualTo("BAD_REQUEST");
        assertThat(exception.getMessage()).isEqualTo("Invalid input");
        assertThat(exception.getStatusCode()).isEqualTo(400);
    }

    @Test
    void shouldCreateNotFoundException() {
        ApiException exception = ApiException.notFound("Resource not found");

        assertThat(exception.getErrorCode()).isEqualTo("NOT_FOUND");
        assertThat(exception.getMessage()).isEqualTo("Resource not found");
        assertThat(exception.getStatusCode()).isEqualTo(404);
    }

    @Test
    void shouldCreateInternalErrorException() {
        ApiException exception = ApiException.internalError("Internal server error");

        assertThat(exception.getErrorCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(exception.getMessage()).isEqualTo("Internal server error");
        assertThat(exception.getStatusCode()).isEqualTo(500);
    }

    @Test
    void shouldCreateInternalErrorExceptionWithCause() {
        RuntimeException cause = new RuntimeException("Database error");
        ApiException exception = ApiException.internalError("Internal server error", cause);

        assertThat(exception.getErrorCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(exception.getMessage()).isEqualTo("Internal server error");
        assertThat(exception.getStatusCode()).isEqualTo(500);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void shouldCreateConflictException() {
        ApiException exception = ApiException.conflict("Resource already exists");

        assertThat(exception.getErrorCode()).isEqualTo("CONFLICT");
        assertThat(exception.getMessage()).isEqualTo("Resource already exists");
        assertThat(exception.getStatusCode()).isEqualTo(409);
    }

    @Test
    void shouldCreateUnauthorizedException() {
        ApiException exception = ApiException.unauthorized("Authentication required");

        assertThat(exception.getErrorCode()).isEqualTo("UNAUTHORIZED");
        assertThat(exception.getMessage()).isEqualTo("Authentication required");
        assertThat(exception.getStatusCode()).isEqualTo(401);
    }

    @Test
    void shouldCreateForbiddenException() {
        ApiException exception = ApiException.forbidden("Access denied");

        assertThat(exception.getErrorCode()).isEqualTo("FORBIDDEN");
        assertThat(exception.getMessage()).isEqualTo("Access denied");
        assertThat(exception.getStatusCode()).isEqualTo(403);
    }

    @Test
    void shouldHaveToStringMethod() {
        ApiException exception = new ApiException("TEST_ERROR", "Test message", 400);

        String toString = exception.toString();
        assertThat(toString).contains("TEST_ERROR");
        assertThat(toString).contains("Test message");
        assertThat(toString).contains("400");
    }
}

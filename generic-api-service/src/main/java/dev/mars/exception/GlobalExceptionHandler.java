package dev.mars.exception;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the application
 */
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public static void configure(Javalin app) {
        // Handle ApiException
        app.exception(ApiException.class, (exception, ctx) -> {
            logger.error("API Exception: {}", exception.getMessage(), exception);
            
            Map<String, Object> errorResponse = createErrorResponse(
                exception.getErrorCode(),
                exception.getMessage(),
                ctx.path()
            );
            
            ctx.status(exception.getStatusCode()).json(errorResponse);
        });

        // Handle IllegalArgumentException
        app.exception(IllegalArgumentException.class, (exception, ctx) -> {
            logger.error("Illegal Argument Exception: {}", exception.getMessage(), exception);
            
            Map<String, Object> errorResponse = createErrorResponse(
                "BAD_REQUEST",
                exception.getMessage(),
                ctx.path()
            );
            
            ctx.status(400).json(errorResponse);
        });

        // Handle generic exceptions
        app.exception(Exception.class, (exception, ctx) -> {
            logger.error("Unexpected exception: {}", exception.getMessage(), exception);
            
            Map<String, Object> errorResponse = createErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                ctx.path()
            );
            
            ctx.status(500).json(errorResponse);
        });

        // Handle 404 Not Found
        app.error(404, ctx -> {
            Map<String, Object> errorResponse = createErrorResponse(
                "NOT_FOUND",
                "The requested resource was not found",
                ctx.path()
            );
            
            ctx.json(errorResponse);
        });
    }

    private static Map<String, Object> createErrorResponse(String errorCode, String message, String path) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", errorCode);
        errorResponse.put("message", message);
        errorResponse.put("path", path);
        errorResponse.put("timestamp", System.currentTimeMillis());
        return errorResponse;
    }
}

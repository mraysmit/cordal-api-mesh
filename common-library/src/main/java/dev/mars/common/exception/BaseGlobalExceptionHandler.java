package dev.mars.common.exception;

import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Base global exception handler providing common exception handling patterns
 * Common exception handling framework used across all modules
 */
public class BaseGlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(BaseGlobalExceptionHandler.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Configure common exception handlers for a Javalin app
     */
    public static void configure(Javalin app) {
        configureApiExceptionHandler(app);
        configureIllegalArgumentExceptionHandler(app);
        configureGenericExceptionHandler(app);
        configureNotFoundHandler(app);
    }

    /**
     * Configure ApiException handler
     */
    public static void configureApiExceptionHandler(Javalin app) {
        app.exception(ApiException.class, (exception, ctx) -> {
            logger.error("API Exception: {}", exception.getMessage(), exception);
            
            Map<String, Object> errorResponse = createErrorResponse(
                exception.getErrorCode(),
                exception.getMessage(),
                ctx.path()
            );
            
            ctx.status(exception.getStatusCode()).json(errorResponse);
        });
    }

    /**
     * Configure IllegalArgumentException handler
     */
    public static void configureIllegalArgumentExceptionHandler(Javalin app) {
        app.exception(IllegalArgumentException.class, (exception, ctx) -> {
            logger.error("Illegal Argument Exception: {}", exception.getMessage(), exception);
            
            Map<String, Object> errorResponse = createErrorResponse(
                "BAD_REQUEST",
                exception.getMessage(),
                ctx.path()
            );
            
            ctx.status(400).json(errorResponse);
        });
    }

    /**
     * Configure generic exception handler
     */
    public static void configureGenericExceptionHandler(Javalin app) {
        app.exception(Exception.class, (exception, ctx) -> {
            logger.error("Unexpected exception: {}", exception.getMessage(), exception);
            
            Map<String, Object> errorResponse = createErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                ctx.path()
            );
            
            ctx.status(500).json(errorResponse);
        });
    }

    /**
     * Configure 404 Not Found handler
     */
    public static void configureNotFoundHandler(Javalin app) {
        app.error(404, ctx -> {
            Map<String, Object> errorResponse = createErrorResponse(
                "NOT_FOUND",
                "The requested resource was not found",
                ctx.path()
            );
            
            ctx.json(errorResponse);
        });
    }

    /**
     * Create a standardized error response
     */
    public static Map<String, Object> createErrorResponse(String errorCode, String message, String path) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("errorCode", errorCode);
        errorResponse.put("message", message);
        errorResponse.put("path", path);
        errorResponse.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
        return errorResponse;
    }

    /**
     * Create a standardized error response with additional details
     */
    public static Map<String, Object> createErrorResponse(String errorCode, String message, String path, Map<String, Object> details) {
        Map<String, Object> errorResponse = createErrorResponse(errorCode, message, path);
        if (details != null && !details.isEmpty()) {
            errorResponse.put("details", details);
        }
        return errorResponse;
    }
}

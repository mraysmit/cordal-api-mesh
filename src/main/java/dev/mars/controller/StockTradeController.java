package dev.mars.controller;

import dev.mars.dto.PagedResponse;
import dev.mars.dto.StockTradeDto;
import dev.mars.exception.ApiException;
import dev.mars.service.StockTradeService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for StockTrade API endpoints
 */
@Singleton
public class StockTradeController {
    private static final Logger logger = LoggerFactory.getLogger(StockTradeController.class);
    
    private final StockTradeService stockTradeService;
    
    @Inject
    public StockTradeController(StockTradeService stockTradeService) {
        this.stockTradeService = stockTradeService;
    }
    
    /**
     * GET /api/stock-trades - Get all stock trades with pagination
     */
    public void getAllStockTrades(Context ctx) {
        logger.debug("GET /api/stock-trades - Getting all stock trades");
        
        try {
            int page = parseIntParameter(ctx, "page", 0);
            int size = parseIntParameter(ctx, "size", 20);
            boolean async = parseBooleanParameter(ctx, "async", false);
            
            logger.debug("Request parameters - page: {}, size: {}, async: {}", page, size, async);
            
            if (async) {
                handleAsyncRequest(ctx, stockTradeService.getAllStockTradesAsync(page, size));
            } else {
                PagedResponse<StockTradeDto> response = stockTradeService.getAllStockTrades(page, size);
                ctx.json(response);
            }
            
        } catch (Exception e) {
            logger.error("Error in getAllStockTrades", e);
            throw e;
        }
    }
    
    /**
     * GET /api/stock-trades/{id} - Get stock trade by ID
     */
    public void getStockTradeById(Context ctx) {
        logger.debug("GET /api/stock-trades/{id} - Getting stock trade by ID");
        
        try {
            Long id = parsePathParameter(ctx, "id");
            boolean async = parseBooleanParameter(ctx, "async", false);
            
            logger.debug("Request parameters - id: {}, async: {}", id, async);
            
            if (async) {
                handleAsyncRequest(ctx, stockTradeService.getStockTradeByIdAsync(id));
            } else {
                StockTradeDto response = stockTradeService.getStockTradeById(id);
                ctx.json(response);
            }
            
        } catch (Exception e) {
            logger.error("Error in getStockTradeById", e);
            throw e;
        }
    }
    
    /**
     * GET /api/stock-trades/symbol/{symbol} - Get stock trades by symbol with pagination
     */
    public void getStockTradesBySymbol(Context ctx) {
        logger.debug("GET /api/stock-trades/symbol/{symbol} - Getting stock trades by symbol");
        
        try {
            String symbol = ctx.pathParam("symbol");
            int page = parseIntParameter(ctx, "page", 0);
            int size = parseIntParameter(ctx, "size", 20);
            boolean async = parseBooleanParameter(ctx, "async", false);
            
            logger.debug("Request parameters - symbol: {}, page: {}, size: {}, async: {}", 
                        symbol, page, size, async);
            
            if (async) {
                handleAsyncRequest(ctx, stockTradeService.getStockTradesBySymbolAsync(symbol, page, size));
            } else {
                PagedResponse<StockTradeDto> response = stockTradeService.getStockTradesBySymbol(symbol, page, size);
                ctx.json(response);
            }
            
        } catch (Exception e) {
            logger.error("Error in getStockTradesBySymbol", e);
            throw e;
        }
    }
    
    /**
     * GET /api/health - Health check endpoint
     */
    public void getHealthStatus(Context ctx) {
        logger.debug("GET /api/health - Health check");
        
        try {
            handleAsyncRequest(ctx, stockTradeService.getHealthStatusAsync()
                .thenApply(healthy -> {
                    if (healthy) {
                        return new HealthResponse("UP", "Service is healthy");
                    } else {
                        ctx.status(503);
                        return new HealthResponse("DOWN", "Service is unhealthy");
                    }
                }));
            
        } catch (Exception e) {
            logger.error("Error in getHealthStatus", e);
            ctx.status(503).json(new HealthResponse("DOWN", "Health check failed"));
        }
    }
    
    private int parseIntParameter(Context ctx, String paramName, int defaultValue) {
        String paramValue = ctx.queryParam(paramName);
        if (paramValue == null || paramValue.trim().isEmpty()) {
            return defaultValue;
        }
        
        try {
            return Integer.parseInt(paramValue);
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("Invalid " + paramName + " parameter: " + paramValue);
        }
    }
    
    private boolean parseBooleanParameter(Context ctx, String paramName, boolean defaultValue) {
        String paramValue = ctx.queryParam(paramName);
        if (paramValue == null || paramValue.trim().isEmpty()) {
            return defaultValue;
        }
        
        return Boolean.parseBoolean(paramValue);
    }
    
    private Long parsePathParameter(Context ctx, String paramName) {
        String paramValue = ctx.pathParam(paramName);
        if (paramValue == null || paramValue.trim().isEmpty()) {
            throw ApiException.badRequest("Missing path parameter: " + paramName);
        }
        
        try {
            return Long.parseLong(paramValue);
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("Invalid " + paramName + " parameter: " + paramValue);
        }
    }
    
    private <T> void handleAsyncRequest(Context ctx, CompletableFuture<T> future) {
        try {
            T result = future.get(); // Block until the future completes
            ctx.json(result);
        } catch (Exception e) {
            logger.error("Async request failed", e);
            if (e.getCause() instanceof ApiException) {
                throw (ApiException) e.getCause();
            } else {
                throw ApiException.internalError("Async operation failed", e);
            }
        }
    }
    
    // Inner class for health response
    public static class HealthResponse {
        private final String status;
        private final String message;
        private final long timestamp;
        
        public HealthResponse(String status, String message) {
            this.status = status;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
    }
}

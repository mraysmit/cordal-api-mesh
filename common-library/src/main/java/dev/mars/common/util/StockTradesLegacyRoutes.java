package dev.mars.common.util;

import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for registering legacy stock trades routes
 * Moved from generic-api-service to common-library for reusability
 */
public class StockTradesLegacyRoutes {
    private static final Logger logger = LoggerFactory.getLogger(StockTradesLegacyRoutes.class);

    /**
     * Register legacy stock trades routes
     * @param app Javalin application instance
     * @param genericApiController Controller to handle the requests
     */
    public static void registerStockTradesRoutes(Javalin app, Object genericApiController) {
        logger.info("Registering legacy stock trades routes");

        // Stock trades generic endpoints (configured via YAML)
        // IMPORTANT: Specific routes must be registered before generic routes with path parameters
        app.get("/api/generic/stock-trades", ctx -> {
            try {
                // Use reflection to call handleEndpointRequest method
                java.lang.reflect.Method method = genericApiController.getClass()
                    .getMethod("handleEndpointRequest", io.javalin.http.Context.class, String.class);
                method.invoke(genericApiController, ctx, "stock-trades-list");
            } catch (Exception e) {
                logger.error("Failed to handle stock-trades endpoint", e);
                ctx.status(500).json("Internal server error");
            }
        });

        app.get("/api/generic/stock-trades/symbol/{symbol}", ctx -> {
            try {
                java.lang.reflect.Method method = genericApiController.getClass()
                    .getMethod("handleEndpointRequest", io.javalin.http.Context.class, String.class);
                method.invoke(genericApiController, ctx, "stock-trades-by-symbol");
            } catch (Exception e) {
                logger.error("Failed to handle stock-trades-by-symbol endpoint", e);
                ctx.status(500).json("Internal server error");
            }
        });

        app.get("/api/generic/stock-trades/trader/{trader_id}", ctx -> {
            try {
                java.lang.reflect.Method method = genericApiController.getClass()
                    .getMethod("handleEndpointRequest", io.javalin.http.Context.class, String.class);
                method.invoke(genericApiController, ctx, "stock-trades-by-trader");
            } catch (Exception e) {
                logger.error("Failed to handle stock-trades-by-trader endpoint", e);
                ctx.status(500).json("Internal server error");
            }
        });

        app.get("/api/generic/stock-trades/date-range", ctx -> {
            try {
                java.lang.reflect.Method method = genericApiController.getClass()
                    .getMethod("handleEndpointRequest", io.javalin.http.Context.class, String.class);
                method.invoke(genericApiController, ctx, "stock-trades-by-date-range");
            } catch (Exception e) {
                logger.error("Failed to handle stock-trades-by-date-range endpoint", e);
                ctx.status(500).json("Internal server error");
            }
        });

        // Generic {id} route must be last to avoid catching specific routes
        app.get("/api/generic/stock-trades/{id}", ctx -> {
            try {
                java.lang.reflect.Method method = genericApiController.getClass()
                    .getMethod("handleEndpointRequest", io.javalin.http.Context.class, String.class);
                method.invoke(genericApiController, ctx, "stock-trades-by-id");
            } catch (Exception e) {
                logger.error("Failed to handle stock-trades-by-id endpoint", e);
                ctx.status(500).json("Internal server error");
            }
        });

        logger.info("Legacy stock trades routes registered successfully");
    }

    /**
     * Display legacy stock trades endpoints information
     * @param baseUrl Base URL for the application
     */
    public static void displayStockTradesEndpoints(String baseUrl) {
        logger.info("🔧 LEGACY STOCK TRADES API:");
        logger.info("   ├─ Stock Trades:     GET  {}/api/generic/stock-trades", baseUrl);
        logger.info("   ├─ Trade by ID:      GET  {}/api/generic/stock-trades/{{id}}", baseUrl);
        logger.info("   ├─ By Symbol:        GET  {}/api/generic/stock-trades/symbol/{{symbol}}", baseUrl);
        logger.info("   ├─ By Trader:        GET  {}/api/generic/stock-trades/trader/{{trader_id}}", baseUrl);
        logger.info("   └─ Date Range:       GET  {}/api/generic/stock-trades/date-range", baseUrl);
        logger.info("");
    }
}

package dev.cordal.api;

import com.google.inject.Inject;
import dev.cordal.config.H2ServerConfig;
import dev.cordal.common.database.H2ServerManager;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API controller for H2 database server management
 * Provides endpoints to start, stop, and monitor H2 server
 */
public class H2ServerController {
    private static final Logger logger = LoggerFactory.getLogger(H2ServerController.class);
    
    private final H2ServerConfig h2ServerConfig;
    
    @Inject
    public H2ServerController(H2ServerConfig h2ServerConfig) {
        this.h2ServerConfig = h2ServerConfig;
    }
    
    /**
     * Get H2 server status
     * GET /api/h2-server/status
     */
    public void getServerStatus(Context ctx) {
        try {
            H2ServerManager serverManager = h2ServerConfig.getServerManager();
            
            Map<String, Object> status = new HashMap<>();
            status.put("embeddedServerStarted", h2ServerConfig.isServerRunning());
            status.put("tcpServerRunning", serverManager.isTcpServerRunning());
            status.put("webServerRunning", serverManager.isWebServerRunning());
            status.put("tcpServerStatus", serverManager.getTcpServerStatus());
            status.put("webServerStatus", serverManager.getWebServerStatus());
            status.put("serverInfo", serverManager.getServerInfo());
            
            ctx.json(status);
            
        } catch (Exception e) {
            logger.error("Error getting H2 server status", e);
            ctx.status(500).json(Map.of(
                "error", "Failed to get server status",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Start H2 TCP server
     * POST /api/h2-server/tcp/start
     */
    public void startTcpServer(Context ctx) {
        try {
            H2ServerManager serverManager = h2ServerConfig.getServerManager();
            serverManager.startTcpServer();
            
            ctx.json(Map.of(
                "success", true,
                "message", "H2 TCP server started successfully",
                "status", serverManager.getTcpServerStatus()
            ));
            
        } catch (SQLException e) {
            logger.error("Error starting H2 TCP server", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "Failed to start H2 TCP server",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Stop H2 TCP server
     * POST /api/h2-server/tcp/stop
     */
    public void stopTcpServer(Context ctx) {
        try {
            H2ServerManager serverManager = h2ServerConfig.getServerManager();
            serverManager.stopTcpServer();
            
            ctx.json(Map.of(
                "success", true,
                "message", "H2 TCP server stopped successfully"
            ));
            
        } catch (Exception e) {
            logger.error("Error stopping H2 TCP server", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "Failed to stop H2 TCP server",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Start H2 web console
     * POST /api/h2-server/web/start
     */
    public void startWebServer(Context ctx) {
        try {
            H2ServerManager serverManager = h2ServerConfig.getServerManager();
            serverManager.startWebServer();
            
            ctx.json(Map.of(
                "success", true,
                "message", "H2 web console started successfully",
                "status", serverManager.getWebServerStatus()
            ));
            
        } catch (SQLException e) {
            logger.error("Error starting H2 web console", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "Failed to start H2 web console",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Stop H2 web console
     * POST /api/h2-server/web/stop
     */
    public void stopWebServer(Context ctx) {
        try {
            H2ServerManager serverManager = h2ServerConfig.getServerManager();
            serverManager.stopWebServer();
            
            ctx.json(Map.of(
                "success", true,
                "message", "H2 web console stopped successfully"
            ));
            
        } catch (Exception e) {
            logger.error("Error stopping H2 web console", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "Failed to stop H2 web console",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Start both H2 servers
     * POST /api/h2-server/start
     */
    public void startServers(Context ctx) {
        try {
            h2ServerConfig.startEmbeddedServer();
            
            H2ServerManager serverManager = h2ServerConfig.getServerManager();
            ctx.json(Map.of(
                "success", true,
                "message", "H2 servers started successfully",
                "serverInfo", serverManager.getServerInfo()
            ));
            
        } catch (Exception e) {
            logger.error("Error starting H2 servers", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "Failed to start H2 servers",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Stop both H2 servers
     * POST /api/h2-server/stop
     */
    public void stopServers(Context ctx) {
        try {
            h2ServerConfig.stopEmbeddedServer();
            
            ctx.json(Map.of(
                "success", true,
                "message", "H2 servers stopped successfully"
            ));
            
        } catch (Exception e) {
            logger.error("Error stopping H2 servers", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "Failed to stop H2 servers",
                "message", e.getMessage()
            ));
        }
    }
}

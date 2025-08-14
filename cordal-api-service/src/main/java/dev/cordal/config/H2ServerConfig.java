package dev.cordal.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.cordal.common.database.H2ServerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * Configuration and management for H2 database server
 * Handles starting/stopping H2 server based on application configuration
 */
@Singleton
public class H2ServerConfig {
    private static final Logger logger = LoggerFactory.getLogger(H2ServerConfig.class);
    
    private final GenericApiConfig genericApiConfig;
    private H2ServerManager serverManager;
    private boolean serverStarted = false;
    
    @Inject
    public H2ServerConfig(GenericApiConfig genericApiConfig) {
        this.genericApiConfig = genericApiConfig;
        this.serverManager = new H2ServerManager(); // Use default settings
        
        // Check if we need to start embedded H2 server
        if (shouldStartEmbeddedServer()) {
            startEmbeddedServer();
        }
    }
    
    /**
     * Determine if we should start an embedded H2 server
     * Based on the database URL configuration
     */
    private boolean shouldStartEmbeddedServer() {
        String dbUrl = genericApiConfig.getDatabaseUrl();
        
        // Check if URL indicates we want to use TCP mode but no external server
        // This is a heuristic - you might want to add explicit configuration
        boolean isTcpMode = dbUrl.contains("tcp://localhost:");
        boolean isEmbeddedMode = !dbUrl.contains("tcp://") || dbUrl.contains("tcp://localhost:");
        
        logger.info("Database URL analysis:");
        logger.info("- URL: {}", dbUrl);
        logger.info("- TCP Mode: {}", isTcpMode);
        logger.info("- Embedded Mode: {}", isEmbeddedMode);
        
        // For now, don't auto-start embedded server to avoid conflicts
        // Users can manually start server or use the scripts
        return false;
    }
    
    /**
     * Start embedded H2 server
     */
    public void startEmbeddedServer() {
        if (serverStarted) {
            logger.warn("H2 server already started");
            return;
        }
        
        try {
            logger.info("Starting embedded H2 server...");
            serverManager.startServers();
            serverStarted = true;
            
            logger.info("Embedded H2 server started successfully");
            logger.info(serverManager.getServerInfo());
            
            // Add shutdown hook to stop server gracefully
            Runtime.getRuntime().addShutdownHook(new Thread(this::stopEmbeddedServer));
            
        } catch (SQLException e) {
            logger.error("Failed to start embedded H2 server", e);
            throw new RuntimeException("Failed to start embedded H2 server", e);
        }
    }
    
    /**
     * Stop embedded H2 server
     */
    public void stopEmbeddedServer() {
        if (!serverStarted) {
            return;
        }
        
        logger.info("Stopping embedded H2 server...");
        serverManager.stopServers();
        serverStarted = false;
        logger.info("Embedded H2 server stopped");
    }
    
    /**
     * Get H2 server manager for manual control
     */
    public H2ServerManager getServerManager() {
        return serverManager;
    }
    
    /**
     * Check if embedded server is running
     */
    public boolean isServerRunning() {
        return serverStarted && (serverManager.isTcpServerRunning() || serverManager.isWebServerRunning());
    }
    
    /**
     * Get server status information
     */
    public String getServerStatus() {
        if (!serverStarted) {
            return "Embedded H2 server not started";
        }
        
        return serverManager.getServerInfo();
    }
}

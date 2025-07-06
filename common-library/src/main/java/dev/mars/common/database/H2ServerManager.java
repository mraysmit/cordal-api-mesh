package dev.mars.common.database;

import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * Manages H2 database server lifecycle
 * Provides methods to start and stop H2 TCP server for multiple connections
 */
public class H2ServerManager {
    private static final Logger logger = LoggerFactory.getLogger(H2ServerManager.class);
    
    private Server tcpServer;
    private Server webServer;
    private final int tcpPort;
    private final int webPort;
    private final String baseDir;
    private final boolean allowExternalConnections;
    
    /**
     * Create H2 server manager with default settings
     */
    public H2ServerManager() {
        this(9092, 8082, "./data", true);
    }
    
    /**
     * Create H2 server manager with custom settings
     * 
     * @param tcpPort TCP port for database connections
     * @param webPort Web port for H2 console
     * @param baseDir Base directory for database files
     * @param allowExternalConnections Whether to allow external connections
     */
    public H2ServerManager(int tcpPort, int webPort, String baseDir, boolean allowExternalConnections) {
        this.tcpPort = tcpPort;
        this.webPort = webPort;
        this.baseDir = baseDir;
        this.allowExternalConnections = allowExternalConnections;
    }
    
    /**
     * Start H2 TCP server for database connections
     */
    public void startTcpServer() throws SQLException {
        if (tcpServer != null && tcpServer.isRunning(false)) {
            logger.warn("H2 TCP server is already running on port {}", tcpPort);
            return;
        }
        
        logger.info("Starting H2 TCP server on port {} with base directory: {}", tcpPort, baseDir);
        
        String[] args = allowExternalConnections 
            ? new String[]{"-tcp", "-tcpAllowOthers", "-tcpPort", String.valueOf(tcpPort), "-baseDir", baseDir, "-ifNotExists"}
            : new String[]{"-tcp", "-tcpPort", String.valueOf(tcpPort), "-baseDir", baseDir, "-ifNotExists"};
            
        tcpServer = Server.createTcpServer(args);
        tcpServer.start();
        
        logger.info("H2 TCP server started successfully on port {}", tcpPort);
        logger.info("Database connection URL format: jdbc:h2:tcp://localhost:{}/./data/[database-name]", tcpPort);
    }
    
    /**
     * Start H2 web console for database management
     */
    public void startWebServer() throws SQLException {
        if (webServer != null && webServer.isRunning(false)) {
            logger.warn("H2 web server is already running on port {}", webPort);
            return;
        }
        
        logger.info("Starting H2 web console on port {}", webPort);
        
        String[] args = allowExternalConnections 
            ? new String[]{"-web", "-webAllowOthers", "-webPort", String.valueOf(webPort)}
            : new String[]{"-web", "-webPort", String.valueOf(webPort)};
            
        webServer = Server.createWebServer(args);
        webServer.start();
        
        logger.info("H2 web console started successfully on port {}", webPort);
        logger.info("Access web console at: http://localhost:{}", webPort);
    }
    
    /**
     * Start both TCP and web servers
     */
    public void startServers() throws SQLException {
        startTcpServer();
        startWebServer();
    }
    
    /**
     * Stop H2 TCP server
     */
    public void stopTcpServer() {
        if (tcpServer != null && tcpServer.isRunning(false)) {
            logger.info("Stopping H2 TCP server on port {}", tcpPort);
            tcpServer.stop();
            tcpServer = null;
            logger.info("H2 TCP server stopped");
        }
    }
    
    /**
     * Stop H2 web server
     */
    public void stopWebServer() {
        if (webServer != null && webServer.isRunning(false)) {
            logger.info("Stopping H2 web console on port {}", webPort);
            webServer.stop();
            webServer = null;
            logger.info("H2 web console stopped");
        }
    }
    
    /**
     * Stop both TCP and web servers
     */
    public void stopServers() {
        stopTcpServer();
        stopWebServer();
    }
    
    /**
     * Check if TCP server is running
     */
    public boolean isTcpServerRunning() {
        return tcpServer != null && tcpServer.isRunning(false);
    }
    
    /**
     * Check if web server is running
     */
    public boolean isWebServerRunning() {
        return webServer != null && webServer.isRunning(false);
    }
    
    /**
     * Get TCP server status information
     */
    public String getTcpServerStatus() {
        if (isTcpServerRunning()) {
            return String.format("H2 TCP Server running on port %d (URL: jdbc:h2:tcp://localhost:%d/./data/[database-name])", 
                                tcpPort, tcpPort);
        } else {
            return "H2 TCP Server not running";
        }
    }
    
    /**
     * Get web server status information
     */
    public String getWebServerStatus() {
        if (isWebServerRunning()) {
            return String.format("H2 Web Console running on port %d (URL: http://localhost:%d)", webPort, webPort);
        } else {
            return "H2 Web Console not running";
        }
    }
    
    /**
     * Get server configuration information
     */
    public String getServerInfo() {
        return String.format("""
            H2 Server Configuration:
            - TCP Port: %d
            - Web Port: %d
            - Base Directory: %s
            - External Connections: %s
            - TCP Server Status: %s
            - Web Server Status: %s
            """, 
            tcpPort, webPort, baseDir, 
            allowExternalConnections ? "Allowed" : "Local only",
            isTcpServerRunning() ? "Running" : "Stopped",
            isWebServerRunning() ? "Running" : "Stopped");
    }
}

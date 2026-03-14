package dev.cordal.api;

import dev.cordal.common.database.H2ServerManager;
import dev.cordal.config.H2ServerConfig;
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class H2ServerControllerUnitTest {

    private H2ServerConfig config;
    private H2ServerManager manager;
    private H2ServerController controller;
    private Context ctx;

    @BeforeEach
    void setUp() {
        config = mock(H2ServerConfig.class);
        manager = mock(H2ServerManager.class);
        controller = new H2ServerController(config);
        ctx = mock(Context.class);
        when(ctx.status(any(Integer.class))).thenReturn(ctx);
        when(config.getServerManager()).thenReturn(manager);
    }

    @Test
    void statusEndpointShouldReturnServerState() {
        when(config.isServerRunning()).thenReturn(true);
        when(manager.isTcpServerRunning()).thenReturn(true);
        when(manager.isWebServerRunning()).thenReturn(false);
        when(manager.getTcpServerStatus()).thenReturn("RUNNING");
        when(manager.getWebServerStatus()).thenReturn("STOPPED");
        when(manager.getServerInfo()).thenReturn("tcp://localhost:9092");

        controller.getServerStatus(ctx);

        verify(ctx).json(argThat(payload -> payload instanceof Map<?, ?> map
            && Boolean.TRUE.equals(map.get("embeddedServerStarted"))
            && "RUNNING".equals(map.get("tcpServerStatus"))));
    }

    @Test
    void statusEndpointShouldHandleFailures() {
        when(config.getServerManager()).thenThrow(new RuntimeException("boom-status"));

        controller.getServerStatus(ctx);

        verify(ctx).status(500);
    }

    @Test
    void tcpEndpointsShouldHandleSuccessAndFailures() throws Exception {
        when(manager.getTcpServerStatus()).thenReturn("RUNNING");
        doNothing().doThrow(new SQLException("tcp failed")).when(manager).startTcpServer();
        doNothing().doThrow(new RuntimeException("stop failed")).when(manager).stopTcpServer();

        controller.startTcpServer(ctx);
        controller.stopTcpServer(ctx);
        controller.startTcpServer(ctx);
        controller.stopTcpServer(ctx);

        verify(ctx).json(argThat(payload -> payload instanceof Map<?, ?> map
            && Boolean.TRUE.equals(map.get("success"))));
        verify(ctx).status(500);
    }

    @Test
    void webEndpointsShouldHandleSuccessAndFailures() throws Exception {
        when(manager.getWebServerStatus()).thenReturn("RUNNING");
        doNothing().doThrow(new SQLException("web failed")).when(manager).startWebServer();
        doNothing().doThrow(new RuntimeException("stop failed")).when(manager).stopWebServer();

        controller.startWebServer(ctx);
        controller.stopWebServer(ctx);
        controller.startWebServer(ctx);
        controller.stopWebServer(ctx);

        verify(ctx).json(argThat(payload -> payload instanceof Map<?, ?> map
            && Boolean.TRUE.equals(map.get("success"))));
        verify(ctx).status(500);
    }

    @Test
    void combinedEndpointsShouldHandleSuccessAndFailures() {
        when(manager.getServerInfo()).thenReturn("web://localhost:8082");
        doNothing().doThrow(new RuntimeException("start failed")).when(config).startEmbeddedServer();
        doNothing().doThrow(new RuntimeException("stop failed")).when(config).stopEmbeddedServer();

        controller.startServers(ctx);
        controller.stopServers(ctx);
        controller.startServers(ctx);
        controller.stopServers(ctx);

        verify(ctx).json(argThat(payload -> payload instanceof Map<?, ?> map
            && Boolean.TRUE.equals(map.get("success"))));
        verify(ctx).status(500);
    }
}
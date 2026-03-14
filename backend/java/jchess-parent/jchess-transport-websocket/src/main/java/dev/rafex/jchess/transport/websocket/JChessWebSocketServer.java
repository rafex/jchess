package dev.rafex.jchess.transport.websocket;

import dev.rafex.jchess.application.EngineFacade;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeHandler;

public final class JChessWebSocketServer {
    private final EngineFacade engineFacade;
    private final int port;

    public JChessWebSocketServer(EngineFacade engineFacade, int port) {
        this.engineFacade = engineFacade;
        this.port = port;
    }

    public void startAndBlock() throws Exception {
        Server server = new Server(port);
        ContextHandler contextHandler = new ContextHandler("/");
        WebSocketSessionRegistry registry = new WebSocketSessionRegistry();
        WebSocketUpgradeHandler upgradeHandler = WebSocketUpgradeHandler.from(server, contextHandler, container ->
                container.addMapping("/ws", (upgradeRequest, upgradeResponse, callback) -> new SessionWebSocket(engineFacade, registry))
        );
        contextHandler.setHandler(upgradeHandler);
        server.setHandler(contextHandler);
        server.start();
        server.join();
    }
}

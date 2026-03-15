package dev.rafex.jchess.transport.http;

import dev.rafex.ether.http.jetty12.JettyRouteRegistry;
import dev.rafex.ether.http.jetty12.JettyServerConfig;
import dev.rafex.ether.http.jetty12.JettyServerFactory;
import dev.rafex.ether.http.jetty12.JettyServerRunner;
import dev.rafex.ether.json.JsonCodecBuilder;
import dev.rafex.jchess.application.EngineFacade;
import dev.rafex.jchess.transport.websocket.SessionWebSocket;
import dev.rafex.jchess.transport.websocket.WebSocketSessionRegistry;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeHandler;

public final class JChessHttpServer {
    private final EngineFacade engineFacade;
    private final int port;

    public JChessHttpServer(EngineFacade engineFacade, int port) {
        this.engineFacade = engineFacade;
        this.port = port;
    }

    public void startAndBlock() throws Exception {
        var jsonCodec = JsonCodecBuilder.create().build();
        var registry = new WebSocketSessionRegistry();

        var routes = new JettyRouteRegistry();
        routes.add("/health", new HealthHandler(jsonCodec));
        routes.add("/api/v1/themes", new ThemesHandler(jsonCodec));
        routes.add("/api/v1/themes/*", new ThemesHandler(jsonCodec));
        routes.add("/api/v1/games", new GamesHandler(engineFacade, registry, jsonCodec));
        routes.add("/api/v1/games/*", new GamesHandler(engineFacade, registry, jsonCodec));
        routes.add("/*", new NotFoundHandler(jsonCodec));

        JettyServerConfig config = new JettyServerConfig(port, 200, 8, 30000, "jchess-http", "dev");
        JettyServerRunner runner = JettyServerFactory.create(config, routes, jsonCodec);

        ContextHandler webSocketContext = new ContextHandler("/ws");
        WebSocketUpgradeHandler webSocketHandler = WebSocketUpgradeHandler.from(runner.server(), webSocketContext, container ->
                container.addMapping("/", (upgradeRequest, upgradeResponse, callback) -> new SessionWebSocket(engineFacade, registry, jsonCodec))
        );
        webSocketContext.setHandler(webSocketHandler);

        Handler current = runner.server().getHandler();
        ContextHandler apiContext = new ContextHandler("/");
        apiContext.setHandler(current);
        ContextHandlerCollection contexts = new ContextHandlerCollection();
        contexts.setHandlers(new Handler[]{webSocketContext, apiContext});
        runner.server().setHandler(contexts);

        runner.start();
        runner.await();
    }
}

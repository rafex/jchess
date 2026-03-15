package dev.rafex.jchess.transport.http;

import dev.rafex.ether.http.core.HttpExchange;
import dev.rafex.ether.http.core.Route;
import dev.rafex.ether.http.jetty12.NonBlockingResourceHandler;
import dev.rafex.ether.json.JsonCodec;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HealthHandler extends NonBlockingResourceHandler {

    public HealthHandler(JsonCodec jsonCodec) {
        super(jsonCodec);
    }

    @Override
    protected String basePath() {
        return "/health";
    }

    @Override
    protected List<Route> routes() {
        return List.of(Route.of("/", Set.of("GET")));
    }

    @Override
    public boolean get(HttpExchange exchange) {
        exchange.json(200, Map.of("status", "UP", "timestamp", Instant.now().toString()));
        return true;
    }
}

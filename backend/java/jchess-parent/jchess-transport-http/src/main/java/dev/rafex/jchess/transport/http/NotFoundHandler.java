package dev.rafex.jchess.transport.http;

import dev.rafex.ether.http.core.HttpExchange;
import dev.rafex.ether.http.core.Route;
import dev.rafex.ether.http.jetty12.NonBlockingResourceHandler;
import dev.rafex.ether.json.JsonCodec;

import java.util.List;
import java.util.Set;

public final class NotFoundHandler extends NonBlockingResourceHandler {
    private static final Set<String> ALL_METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    public NotFoundHandler(JsonCodec jsonCodec) {
        super(jsonCodec);
    }

    @Override
    protected String basePath() {
        return "/";
    }

    @Override
    protected List<Route> routes() {
        return List.of(Route.of("/**", ALL_METHODS));
    }

    @Override
    public boolean get(HttpExchange exchange) {
        exchange.json(404, java.util.Map.of("error", "not_found"));
        return true;
    }

    @Override
    public boolean post(HttpExchange exchange) {
        return get(exchange);
    }

    @Override
    public boolean put(HttpExchange exchange) {
        return get(exchange);
    }

    @Override
    public boolean patch(HttpExchange exchange) {
        return get(exchange);
    }

    @Override
    public boolean delete(HttpExchange exchange) {
        return get(exchange);
    }

    @Override
    public boolean options(HttpExchange exchange) {
        return get(exchange);
    }
}

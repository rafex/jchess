package dev.rafex.jchess.transport.http;

import dev.rafex.ether.http.core.HttpExchange;
import dev.rafex.ether.http.core.Route;
import dev.rafex.ether.http.jetty12.NonBlockingResourceHandler;
import dev.rafex.ether.json.JsonCodec;
import dev.rafex.jchess.core.engine.BoardAsciiRenderer;
import dev.rafex.jchess.transport.api.ApiEnvelope;
import dev.rafex.jchess.transport.api.ApiPresenter;

import java.util.List;
import java.util.Set;

public final class ThemesHandler extends NonBlockingResourceHandler {
    private final ApiPresenter presenter;
    private final BoardAsciiRenderer boardAsciiRenderer;

    public ThemesHandler(JsonCodec jsonCodec) {
        super(jsonCodec);
        this.presenter = new ApiPresenter();
        this.boardAsciiRenderer = new BoardAsciiRenderer();
    }

    @Override
    protected String basePath() {
        return "/api/v1/themes";
    }

    @Override
    protected List<Route> routes() {
        return List.of(Route.of("/", Set.of("GET")));
    }

    @Override
    public boolean get(HttpExchange exchange) {
        exchange.json(200, ApiEnvelope.ok("themes",
                presenter.themes(boardAsciiRenderer.availableThemes()).stream().map(theme -> theme.toMap()).toList()).toMap());
        return true;
    }
}

package dev.rafex.jchess.transport.http;

import com.fasterxml.jackson.databind.JsonNode;
import dev.rafex.ether.http.core.HttpExchange;
import dev.rafex.ether.http.core.Route;
import dev.rafex.ether.http.jetty12.NonBlockingResourceHandler;
import dev.rafex.ether.json.JsonCodec;
import dev.rafex.jchess.application.EngineFacade;
import dev.rafex.jchess.domain.model.GameSessionAccess;
import dev.rafex.jchess.domain.model.GameSnapshot;
import dev.rafex.jchess.domain.model.GameStartRequest;
import dev.rafex.jchess.domain.model.LlmProvider;
import dev.rafex.jchess.domain.model.MoveRequest;
import dev.rafex.jchess.domain.model.ParticipantType;
import dev.rafex.jchess.domain.model.Side;
import dev.rafex.jchess.transport.api.ApiEnvelope;
import dev.rafex.jchess.transport.api.ApiPresenter;
import dev.rafex.jchess.transport.api.ConnectionStateView;
import dev.rafex.jchess.transport.websocket.WebSocketSessionRegistry;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class GamesHandler extends NonBlockingResourceHandler {
    private final EngineFacade engineFacade;
    private final WebSocketSessionRegistry registry;
    private final HttpSupport httpSupport;
    private final ApiPresenter presenter;

    public GamesHandler(EngineFacade engineFacade, WebSocketSessionRegistry registry, JsonCodec jsonCodec) {
        super(jsonCodec);
        this.engineFacade = engineFacade;
        this.registry = registry;
        this.httpSupport = new HttpSupport(jsonCodec);
        this.presenter = new ApiPresenter();
    }

    @Override
    protected String basePath() {
        return "/api/v1/games";
    }

    @Override
    protected List<Route> routes() {
        return List.of(
                Route.of("/", Set.of("POST")),
                Route.of("/{sessionId}", Set.of("GET")),
                Route.of("/{sessionId}/join", Set.of("POST")),
                Route.of("/{sessionId}/moves", Set.of("POST")),
                Route.of("/{sessionId}/resign", Set.of("POST")),
                Route.of("/{sessionId}/pgn", Set.of("GET"))
        );
    }

    @Override
    public boolean get(HttpExchange exchange) {
        try {
            String sessionId = exchange.pathParam("sessionId");
            if (exchange.path().endsWith("/pgn")) {
                exchange.json(200, ApiEnvelope.ok("pgn",
                        Map.of("sessionId", sessionId, "pgn", engineFacade.exportPgn(UUID.fromString(sessionId)))).toMap());
                return true;
            }
            GameSnapshot snapshot = engineFacade.loadGame(UUID.fromString(sessionId));
            exchange.json(200, ApiEnvelope.ok("game_state",
                    presenter.game(snapshot, connectionState(snapshot.sessionId())).toMap(false)).toMap());
            return true;
        } catch (IllegalArgumentException ex) {
            httpSupport.error(exchange, 400, "bad_request", ex.getMessage());
            return true;
        } catch (IllegalStateException ex) {
            httpSupport.error(exchange, 409, "conflict", ex.getMessage());
            return true;
        }
    }

    @Override
    public boolean post(HttpExchange exchange) {
        try {
            JsonNode body = httpSupport.readJson(exchange);
            String path = exchange.path();
            if ("/api/v1/games/".equals(path) || "/api/v1/games".equals(path)) {
                GameSessionAccess access = engineFacade.startGameAccess(new GameStartRequest(
                        parseSide(body.path("color").asText(null)),
                        parseOpponent(body.path("opponent").asText(null)),
                        LlmProvider.fromCliValue(textOrNull(body, "llm")),
                        textOrDefault(body, "whitePlayerName", "white"),
                        textOrDefault(body, "blackPlayerName", "black")
                ));
                httpSupport.created(exchange, ApiEnvelope.ok("game_created",
                        presenter.access(access, connectionState(access.snapshot().sessionId())).toMap()).toMap());
                return true;
            }

            UUID sessionId = UUID.fromString(exchange.pathParam("sessionId"));
            if (path.endsWith("/join")) {
                GameSessionAccess access = engineFacade.joinGame(sessionId, requiredText(body, "playerToken"));
                httpSupport.ok(exchange, ApiEnvelope.ok("player_joined",
                        presenter.access(access, connectionState(sessionId)).toMap()).toMap());
                return true;
            }
            if (path.endsWith("/moves")) {
                GameSnapshot snapshot = engineFacade.submitMove(sessionId, MoveRequest.of(
                        requiredText(body, "playerToken"),
                        requiredText(body, "from"),
                        requiredText(body, "to"),
                        textOrNull(body, "promotion")
                ));
                String type = snapshot.status().name().equals("FINISHED") ? "game_finished" : "move_submitted";
                httpSupport.ok(exchange, ApiEnvelope.ok(type,
                        presenter.game(snapshot, connectionState(sessionId)).toMap(false)).toMap());
                return true;
            }
            if (path.endsWith("/resign")) {
                GameSessionAccess access = engineFacade.joinGame(sessionId, requiredText(body, "playerToken"));
                GameSnapshot snapshot = engineFacade.resignGame(sessionId, access.requester().side());
                httpSupport.ok(exchange, ApiEnvelope.ok("game_finished",
                        presenter.game(snapshot, connectionState(sessionId)).toMap(false)).toMap());
                return true;
            }
            httpSupport.error(exchange, 404, "not_found", "resource not found");
            return true;
        } catch (IllegalArgumentException ex) {
            httpSupport.error(exchange, 400, "bad_request", ex.getMessage());
            return true;
        } catch (IllegalStateException ex) {
            httpSupport.error(exchange, 409, "conflict", ex.getMessage());
            return true;
        }
    }

    private ConnectionStateView connectionState(UUID sessionId) {
        return new ConnectionStateView(registry.isConnected(sessionId, Side.WHITE), registry.isConnected(sessionId, Side.BLACK));
    }

    private String requiredText(JsonNode body, String field) {
        String value = textOrNull(body, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing " + field);
        }
        return value;
    }

    private String textOrDefault(JsonNode body, String field, String fallback) {
        String value = textOrNull(body, field);
        return value == null || value.isBlank() ? fallback : value;
    }

    private String textOrNull(JsonNode body, String field) {
        JsonNode node = body.path(field);
        return node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    private ParticipantType parseOpponent(String value) {
        return value == null || value.isBlank()
                ? ParticipantType.HUMAN
                : ParticipantType.valueOf(value.trim().toUpperCase());
    }

    private Side parseSide(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("auto")) {
            return null;
        }
        return Side.valueOf(value.trim().toUpperCase());
    }
}

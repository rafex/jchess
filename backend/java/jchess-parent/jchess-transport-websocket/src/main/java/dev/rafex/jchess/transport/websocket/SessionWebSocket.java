package dev.rafex.jchess.transport.websocket;

import com.fasterxml.jackson.core.type.TypeReference;
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
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketOpen;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.util.Map;
import java.util.UUID;

@WebSocket
public final class SessionWebSocket {
    private final EngineFacade engineFacade;
    private final WebSocketSessionRegistry registry;
    private final JsonCodec jsonCodec;
    private final ApiPresenter apiPresenter;
    private Session session;

    public SessionWebSocket(EngineFacade engineFacade, WebSocketSessionRegistry registry, JsonCodec jsonCodec) {
        this.engineFacade = engineFacade;
        this.registry = registry;
        this.jsonCodec = jsonCodec;
        this.apiPresenter = new ApiPresenter();
    }

    @OnWebSocketOpen
    public void onOpen(Session session) {
        this.session = session;
        send(session, ApiEnvelope.ok("connected", Map.of("message", "ready", "protocol", "jchess.v1")));
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        WebSocketSessionRegistry.Registration registration = registry.registration(session);
        registry.leave(session);
        if (registration != null) {
            GameSnapshot snapshot = engineFacade.loadGame(registration.gameId());
            registry.broadcast(registration.gameId(), json(ApiEnvelope.ok("player_disconnected",
                    apiPresenter.game(snapshot, connectionState(registration.gameId())).toMap(false))));
        }
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        try {
            Map<String, Object> payload = jsonCodec.readValue(message, new TypeReference<Map<String, Object>>() {
            });
            String type = string(payload.get("type"));
            switch (type) {
                case "start_game" -> handleStartGame(payload);
                case "join_game" -> handleJoinGame(payload);
                case "move" -> handleMove(payload);
                case "undo" -> handleUndo(payload);
                case "resign" -> handleResign(payload);
                case "state" -> handleState(payload);
                case "heartbeat" -> send(session, ApiEnvelope.ok("heartbeat_ack", Map.of("ok", true)));
                default -> send(session, ApiEnvelope.error("bad_request", 400, "unsupported_type", "unsupported message type"));
            }
        } catch (IllegalArgumentException ex) {
            send(session, ApiEnvelope.error("bad_request", 400, "bad_request", ex.getMessage()));
        } catch (IllegalStateException ex) {
            send(session, ApiEnvelope.error("conflict", 409, "conflict", ex.getMessage()));
        } catch (RuntimeException ex) {
            send(session, ApiEnvelope.error("internal_error", 500, "internal_error", ex.getMessage()));
        }
    }

    private void handleStartGame(Map<String, Object> payload) {
        GameSessionAccess access = engineFacade.startGameAccess(new GameStartRequest(
                parseSide(payload.get("color")),
                parseOpponent(payload.get("opponent")),
                LlmProvider.fromCliValue(string(payload.get("llm"))),
                stringOrDefault(payload.get("whitePlayerName"), "white"),
                stringOrDefault(payload.get("blackPlayerName"), "black")
        ));
        send(session, ApiEnvelope.ok("game_created", apiPresenter.access(access, connectionState(access.snapshot().sessionId())).toMap()));
    }

    private void handleJoinGame(Map<String, Object> payload) {
        UUID sessionId = UUID.fromString(required(payload, "sessionId"));
        String playerToken = required(payload, "playerToken");
        GameSessionAccess access = engineFacade.joinGame(sessionId, playerToken);
        registry.join(sessionId, access.requester().side(), playerToken, session);
        send(session, ApiEnvelope.ok("player_joined", apiPresenter.access(access, connectionState(sessionId)).toMap()));
        registry.broadcast(sessionId, json(ApiEnvelope.ok("game_state", apiPresenter.game(access.snapshot(), connectionState(sessionId)).toMap(false))));
    }

    private void handleMove(Map<String, Object> payload) {
        UUID sessionId = UUID.fromString(required(payload, "sessionId"));
        String playerToken = resolvePlayerToken(payload, session);
        GameSnapshot snapshot = engineFacade.submitMove(sessionId, MoveRequest.of(
                playerToken,
                required(payload, "from"),
                required(payload, "to"),
                string(payload.get("promotion"))
        ));
        registry.broadcast(sessionId, json(ApiEnvelope.ok("move_submitted", apiPresenter.game(snapshot, connectionState(sessionId)).toMap(false))));
        if (snapshot.status().name().equals("FINISHED")) {
            registry.broadcast(sessionId, json(ApiEnvelope.ok("game_finished", apiPresenter.game(snapshot, connectionState(sessionId)).toMap(false))));
        } else {
            registry.broadcast(sessionId, json(ApiEnvelope.ok("turn_changed", Map.of("sessionId", sessionId.toString(), "turn", snapshot.position().sideToMove().name(), "version", snapshot.version()))));
        }
    }

    private void handleUndo(Map<String, Object> payload) {
        UUID sessionId = UUID.fromString(required(payload, "sessionId"));
        GameSnapshot snapshot = engineFacade.undoLastMove(sessionId);
        registry.broadcast(sessionId, json(ApiEnvelope.ok("move_undone", apiPresenter.game(snapshot, connectionState(sessionId)).toMap(false))));
    }

    private void handleResign(Map<String, Object> payload) {
        UUID sessionId = UUID.fromString(required(payload, "sessionId"));
        String playerToken = resolvePlayerToken(payload, session);
        GameSessionAccess access = engineFacade.joinGame(sessionId, playerToken);
        GameSnapshot snapshot = engineFacade.resignGame(sessionId, access.requester().side());
        registry.broadcast(sessionId, json(ApiEnvelope.ok("game_finished", apiPresenter.game(snapshot, connectionState(sessionId)).toMap(false))));
    }

    private void handleState(Map<String, Object> payload) {
        UUID sessionId = UUID.fromString(required(payload, "sessionId"));
        GameSnapshot snapshot = engineFacade.loadGame(sessionId);
        send(session, ApiEnvelope.ok("game_state", apiPresenter.game(snapshot, connectionState(sessionId)).toMap(false)));
    }

    private ConnectionStateView connectionState(UUID sessionId) {
        return new ConnectionStateView(registry.isConnected(sessionId, Side.WHITE), registry.isConnected(sessionId, Side.BLACK));
    }

    private String resolvePlayerToken(Map<String, Object> payload, Session session) {
        String fromPayload = string(payload.get("playerToken"));
        if (fromPayload != null && !fromPayload.isBlank()) {
            return fromPayload;
        }
        WebSocketSessionRegistry.Registration registration = registry.registration(session);
        if (registration == null) {
            throw new IllegalArgumentException("missing playerToken");
        }
        return registration.playerToken();
    }

    private String required(Map<String, Object> payload, String key) {
        String value = string(payload.get(key));
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing " + key);
        }
        return value;
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String stringOrDefault(Object value, String fallback) {
        String string = string(value);
        return string == null || string.isBlank() ? fallback : string;
    }

    private ParticipantType parseOpponent(Object value) {
        String actual = string(value);
        return actual == null || actual.isBlank()
                ? ParticipantType.HUMAN
                : ParticipantType.valueOf(actual.trim().toUpperCase());
    }

    private Side parseSide(Object value) {
        String actual = string(value);
        if (actual == null || actual.isBlank() || actual.equalsIgnoreCase("auto")) {
            return null;
        }
        return Side.valueOf(actual.trim().toUpperCase());
    }

    private void send(Session target, ApiEnvelope envelope) {
        if (target != null && target.isOpen()) {
            target.sendText(json(envelope), Callback.NOOP);
        }
    }

    private String json(ApiEnvelope envelope) {
        return jsonCodec.toJson(envelope.toMap());
    }
}

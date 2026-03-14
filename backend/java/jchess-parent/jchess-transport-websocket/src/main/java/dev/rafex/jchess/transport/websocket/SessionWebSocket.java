package dev.rafex.jchess.transport.websocket;

import dev.rafex.jchess.application.EngineFacade;
import dev.rafex.jchess.domain.model.GameSnapshot;
import dev.rafex.jchess.domain.model.GameStartRequest;
import dev.rafex.jchess.domain.model.LlmProvider;
import dev.rafex.jchess.domain.model.ParticipantType;
import dev.rafex.jchess.domain.model.Side;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketOpen;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@WebSocket
public final class SessionWebSocket {
    private final EngineFacade engineFacade;
    private final WebSocketSessionRegistry registry;
    private Session session;

    public SessionWebSocket(EngineFacade engineFacade, WebSocketSessionRegistry registry) {
        this.engineFacade = engineFacade;
        this.registry = registry;
    }

    @OnWebSocketOpen
    public void onOpen(Session session) {
        this.session = session;
        send(json("connected", "\"message\":\"ready\""));
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        registry.leave(session);
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        try {
            Map<String, String> payload = parseFlatJson(message);
            String type = payload.getOrDefault("type", "");
            switch (type) {
                case "start_game" -> {
                    GameSnapshot snapshot = engineFacade.startGame(new GameStartRequest(
                            parseSide(payload.get("color")),
                            parseOpponent(payload.get("opponent")),
                            LlmProvider.fromCliValue(payload.get("llm")),
                            payload.getOrDefault("whitePlayerName", "white"),
                            payload.getOrDefault("blackPlayerName", "black")
                    ));
                    send(snapshotEvent("game_started", snapshot));
                }
                case "join_game" -> {
                    UUID gameId = UUID.fromString(required(payload, "sessionId"));
                    Side side = parseSide(required(payload, "side"));
                    registry.join(gameId, side, session);
                    send(snapshotEvent("joined_game", engineFacade.loadGame(gameId)));
                }
                case "move" -> {
                    UUID gameId = UUID.fromString(required(payload, "sessionId"));
                    GameSnapshot snapshot = engineFacade.submitMove(gameId, required(payload, "notation"));
                    registry.broadcast(gameId, snapshotEvent("move_played", snapshot));
                }
                case "undo" -> {
                    UUID gameId = UUID.fromString(required(payload, "sessionId"));
                    GameSnapshot snapshot = engineFacade.undoLastMove(gameId);
                    registry.broadcast(gameId, snapshotEvent("move_undone", snapshot));
                }
                case "resign" -> {
                    UUID gameId = UUID.fromString(required(payload, "sessionId"));
                    GameSnapshot snapshot = engineFacade.resignGame(gameId, parseSide(payload.get("side")));
                    registry.broadcast(gameId, snapshotEvent("game_finished", snapshot));
                }
                case "state" -> {
                    UUID gameId = UUID.fromString(required(payload, "sessionId"));
                    send(snapshotEvent("game_state", engineFacade.loadGame(gameId)));
                }
                case "heartbeat" -> send(json("heartbeat", "\"ok\":true"));
                default -> send(json("illegal_move", "\"message\":\"unsupported message type\""));
            }
        } catch (RuntimeException ex) {
            send(json("illegal_move", "\"message\":\"" + escape(ex.getMessage()) + "\""));
        }
    }

    private Map<String, String> parseFlatJson(String json) {
        Map<String, String> values = new HashMap<>();
        String body = json.trim();
        if (body.startsWith("{")) {
            body = body.substring(1);
        }
        if (body.endsWith("}")) {
            body = body.substring(0, body.length() - 1);
        }
        for (String entry : body.split(",")) {
            int separator = entry.indexOf(':');
            if (separator <= 0) {
                continue;
            }
            String key = strip(entry.substring(0, separator));
            String value = strip(entry.substring(separator + 1));
            values.put(key, value);
        }
        return values;
    }

    private String required(Map<String, String> payload, String key) {
        String value = payload.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing " + key);
        }
        return value;
    }

    private String strip(String raw) {
        return raw.trim().replaceAll("^\"|\"$", "");
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

    private String snapshotEvent(String type, GameSnapshot snapshot) {
        return json(type,
                "\"sessionId\":\"" + snapshot.sessionId() + "\","
                        + "\"status\":\"" + snapshot.status() + "\","
                        + "\"result\":\"" + snapshot.result() + "\","
                        + "\"endReason\":\"" + snapshot.endReason() + "\","
                        + "\"turn\":\"" + snapshot.position().sideToMove() + "\","
                        + "\"version\":" + snapshot.version() + ","
                        + "\"fen\":\"" + escape(snapshot.position().toFen()) + "\","
                        + "\"board\":\"" + escape(snapshot.boardAscii()) + "\","
                        + "\"pgn\":\"" + escape(snapshot.pgn()) + "\"");
    }

    private String json(String type, String body) {
        return "{\"type\":\"" + type + "\"," + body + "}";
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private void send(String json) {
        if (session != null && session.isOpen()) {
            session.sendText(json, Callback.NOOP);
        }
    }
}

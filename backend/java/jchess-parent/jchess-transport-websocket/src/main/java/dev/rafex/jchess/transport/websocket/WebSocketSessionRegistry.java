package dev.rafex.jchess.transport.websocket;

import dev.rafex.jchess.domain.model.Side;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WebSocketSessionRegistry {
    private final Map<UUID, Map<Side, Session>> sessionsByGame = new ConcurrentHashMap<>();
    private final Map<Session, Registration> registrations = new ConcurrentHashMap<>();

    public void join(UUID gameId, Side side, String playerToken, Session session) {
        sessionsByGame.computeIfAbsent(gameId, ignored -> new ConcurrentHashMap<>()).put(side, session);
        registrations.put(session, new Registration(gameId, side, playerToken));
    }

    public void leave(Session session) {
        Registration registration = registrations.remove(session);
        if (registration == null) {
            sessionsByGame.values().forEach(map -> map.values().removeIf(existing -> existing == session));
            return;
        }
        sessionsByGame.getOrDefault(registration.gameId(), Map.of()).remove(registration.side(), session);
    }

    public void broadcast(UUID gameId, String json) {
        sessionsByGame.getOrDefault(gameId, Map.of()).values().forEach(session -> {
            if (session.isOpen()) {
                session.sendText(json, Callback.NOOP);
            }
        });
    }

    public boolean hasSide(UUID gameId, Side side) {
        return sessionsByGame.getOrDefault(gameId, Map.of()).containsKey(side);
    }

    public boolean isConnected(UUID gameId, Side side) {
        Session session = sessionsByGame.getOrDefault(gameId, Map.of()).get(side);
        return session != null && session.isOpen();
    }

    public Registration registration(Session session) {
        return registrations.get(session);
    }

    public record Registration(UUID gameId, Side side, String playerToken) {
    }
}

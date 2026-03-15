package dev.rafex.jchess.transport.websocket;

import dev.rafex.ether.json.JsonCodecBuilder;
import dev.rafex.jchess.application.EngineFacade;
import dev.rafex.jchess.domain.model.EngineInfo;
import dev.rafex.jchess.domain.model.GameSessionAccess;
import dev.rafex.jchess.domain.model.GameSnapshot;
import dev.rafex.jchess.domain.model.GameStartRequest;
import dev.rafex.jchess.domain.model.GameSummary;
import dev.rafex.jchess.domain.model.Move;
import dev.rafex.jchess.domain.model.MoveRequest;
import dev.rafex.jchess.domain.model.Position;
import dev.rafex.jchess.domain.model.Side;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class SessionWebSocketTest {

    @Test
    void shouldRejectUndoWithoutPlayerToken() {
        CapturedSession capturedSession = new CapturedSession();
        Session session = capturedSession.proxy();
        SessionWebSocket socket = new SessionWebSocket(new StubEngineFacade(), new WebSocketSessionRegistry(), JsonCodecBuilder.create().build());
        socket.onOpen(session);
        capturedSession.messages().clear();

        socket.onMessage(session, """
                {"type":"undo","sessionId":"00000000-0000-0000-0000-000000000001"}
                """);

        String response = capturedSession.messages().getFirst();
        assertTrue(response.contains("\"type\":\"bad_request\""), response);
        assertTrue(response.contains("playerToken"), response);
    }

    private static final class CapturedSession {
        private final List<String> messages = new ArrayList<>();

        Session proxy() {
            return (Session) Proxy.newProxyInstance(
                    Session.class.getClassLoader(),
                    new Class[]{Session.class},
                    (proxy, method, args) -> {
                        Object result = switch (method.getName()) {
                            case "isOpen" -> true;
                            case "sendText" -> {
                                messages.add((String) args[0]);
                                yield null;
                            }
                            case "close", "disconnect", "demand", "sendBinary", "sendPartialBinary", "sendPartialText",
                                 "sendPing", "sendPong", "addIdleTimeoutListener" -> null;
                            case "isSecure" -> false;
                            default -> null;
                        };
                        if (result != null) {
                            return result;
                        }
                        Class<?> returnType = method.getReturnType();
                        if (returnType == boolean.class) {
                            return false;
                        }
                        if (returnType == int.class) {
                            return 0;
                        }
                        if (returnType == long.class) {
                            return 0L;
                        }
                        if (returnType == double.class) {
                            return 0D;
                        }
                        if (returnType == float.class) {
                            return 0F;
                        }
                        return null;
                    }
            );
        }

        List<String> messages() {
            return messages;
        }
    }

    private static final class StubEngineFacade implements EngineFacade {
        @Override
        public EngineInfo engineInfo() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Position initialPosition() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Move chooseMove(Position position) {
            throw new UnsupportedOperationException();
        }

        @Override
        public GameSnapshot startGame(GameStartRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public GameSessionAccess startGameAccess(GameStartRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public GameSnapshot loadGame(UUID sessionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<GameSummary> listGames(int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public GameSessionAccess joinGame(UUID sessionId, String playerToken) {
            throw new UnsupportedOperationException();
        }

        @Override
        public GameSnapshot submitMove(UUID sessionId, String notation) {
            throw new UnsupportedOperationException();
        }

        @Override
        public GameSnapshot submitMove(UUID sessionId, MoveRequest moveRequest) {
            throw new UnsupportedOperationException();
        }

        @Override
        public GameSnapshot undoLastMove(UUID sessionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public GameSnapshot resignGame(UUID sessionId, Side side) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String exportPgn(UUID sessionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Move> legalMoves(Position position) {
            throw new UnsupportedOperationException();
        }
    }
}

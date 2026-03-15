package dev.rafex.jchess.application;

import dev.rafex.jchess.domain.model.GameState;
import dev.rafex.jchess.domain.model.GameEndReason;
import dev.rafex.jchess.domain.model.GameResult;
import dev.rafex.jchess.domain.model.GameSummary;
import dev.rafex.jchess.domain.model.LlmProvider;
import dev.rafex.jchess.domain.model.Side;
import dev.rafex.jchess.ports.outbound.EngineTelemetry;
import dev.rafex.jchess.ports.outbound.GameRepository;
import dev.rafex.jchess.ports.outbound.MachineMovePort;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class JChessFactory {
    private JChessFactory() {
    }

    public static EngineFacade createDefaultEngine() {
        return new ChessEngineService(new InMemoryGameRepository(), new ConsoleEngineTelemetry(), new NoOpMachineMovePort());
    }

    private static final class InMemoryGameRepository implements GameRepository {
        private final Map<UUID, GameState> sessions = new ConcurrentHashMap<>();

        @Override
        public void initialize() {
        }

        @Override
        public void save(GameState gameState) {
            sessions.put(gameState.session().sessionId(), gameState);
        }

        @Override
        public Optional<GameState> findById(UUID sessionId) {
            return Optional.ofNullable(sessions.get(sessionId));
        }

        @Override
        public List<GameSummary> listRecent(int limit) {
            return sessions.values().stream()
                    .sorted((left, right) -> right.session().updatedAt().compareTo(left.session().updatedAt()))
                    .limit(limit)
                    .map(state -> new GameSummary(
                            state.session().sessionId(),
                            state.session().status(),
                            state.session().result(),
                            state.session().endReason(),
                            state.session().whitePlayerName(),
                            state.session().blackPlayerName(),
                            state.session().currentPosition().sideToMove(),
                            state.moves().size(),
                            state.session().createdAt(),
                            state.session().updatedAt()
                    ))
                    .toList();
        }

        @Override
        public boolean saveIfVersionMatches(GameState gameState, long expectedVersion) {
            return sessions.compute(gameState.session().sessionId(), (id, current) -> {
                if (current == null || current.session().version() != expectedVersion) {
                    return current;
                }
                return gameState;
            }) == gameState;
        }
    }

    private static final class NoOpMachineMovePort implements MachineMovePort {
        @Override
        public Optional<String> suggestMove(GameState gameState, java.util.List<String> legalMovesEnglish, LlmProvider provider) {
            return Optional.empty();
        }
    }

    private static final class ConsoleEngineTelemetry implements EngineTelemetry {
        @Override
        public void record(String event, String detail) {
            System.out.printf("[jchess] %s :: %s%n", event, detail);
        }
    }
}

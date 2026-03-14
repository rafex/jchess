package dev.rafex.jchess.application;

import dev.rafex.jchess.domain.model.GameStartRequest;
import dev.rafex.jchess.domain.model.GameState;
import dev.rafex.jchess.domain.model.LlmProvider;
import dev.rafex.jchess.domain.model.ParticipantType;
import dev.rafex.jchess.domain.model.Side;
import dev.rafex.jchess.ports.outbound.EngineTelemetry;
import dev.rafex.jchess.ports.outbound.GameRepository;
import dev.rafex.jchess.ports.outbound.MachineMovePort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChessEngineServiceTest {

    @Test
    void shouldStartGameAndPersistSession() {
        InMemoryGameRepository repository = new InMemoryGameRepository();
        ChessEngineService service = new ChessEngineService(repository, new NoOpTelemetry(), new NoOpMachineMovePort());

        var snapshot = service.startGame(new GameStartRequest(Side.WHITE, ParticipantType.HUMAN, null, "Alice", "Bob"));

        assertEquals(Side.WHITE, snapshot.humanSide());
        assertEquals(20, snapshot.legalMovesEnglish().size());
        assertTrue(snapshot.boardAscii().contains("r n b q k b n r".replace(" ", "")) == false);
        assertTrue(repository.findById(snapshot.sessionId()).isPresent());
    }

    @Test
    void shouldAcceptEnglishAndSpanishNotation() {
        InMemoryGameRepository repository = new InMemoryGameRepository();
        ChessEngineService service = new ChessEngineService(repository, new NoOpTelemetry(), new NoOpMachineMovePort());

        var started = service.startGame(new GameStartRequest(Side.WHITE, ParticipantType.HUMAN, null, "Alice", "Bob"));
        var afterEnglish = service.submitMove(started.sessionId(), "Nf3");

        assertEquals("Nf3", afterEnglish.moves().getFirst().canonicalNotation());

        var startedSpanish = service.startGame(new GameStartRequest(Side.WHITE, ParticipantType.HUMAN, null, "Alice", "Bob"));
        var afterSpanish = service.submitMove(startedSpanish.sessionId(), "Cf3)");

        assertEquals("Nf3", afterSpanish.moves().getFirst().canonicalNotation());
    }

    @Test
    void shouldAutoPlayMachineMoveWhenConfigured() {
        InMemoryGameRepository repository = new InMemoryGameRepository();
        ChessEngineService service = new ChessEngineService(repository, new NoOpTelemetry(), new FixedMachineMovePort("e5"));

        var started = service.startGame(new GameStartRequest(Side.WHITE, ParticipantType.MACHINE, LlmProvider.GROQ, "Alice", "Bot"));
        var afterMove = service.submitMove(started.sessionId(), "e4");

        assertEquals(2, afterMove.moves().size());
        assertEquals("e5", afterMove.moves().getLast().submittedNotation());
        assertFalse(afterMove.legalMovesEnglish().isEmpty());
    }

    @Test
    void shouldUndoAndExportPgn() {
        InMemoryGameRepository repository = new InMemoryGameRepository();
        ChessEngineService service = new ChessEngineService(repository, new NoOpTelemetry(), new NoOpMachineMovePort());

        var started = service.startGame(new GameStartRequest(Side.WHITE, ParticipantType.HUMAN, null, "Alice", "Bob"));
        var played = service.submitMove(started.sessionId(), "e4");
        var undone = service.undoLastMove(started.sessionId());

        assertEquals(1, played.moves().size());
        assertEquals(0, undone.moves().size());
        assertTrue(service.exportPgn(started.sessionId()).contains("[White \"Alice\"]"));
    }

    private static final class InMemoryGameRepository implements GameRepository {
        private final Map<UUID, GameState> store = new ConcurrentHashMap<>();

        @Override
        public void initialize() {
        }

        @Override
        public void save(GameState gameState) {
            store.put(gameState.session().sessionId(), gameState);
        }

        @Override
        public Optional<GameState> findById(UUID sessionId) {
            return Optional.ofNullable(store.get(sessionId));
        }

        @Override
        public boolean saveIfVersionMatches(GameState gameState, long expectedVersion) {
            GameState current = store.get(gameState.session().sessionId());
            if (current == null || current.session().version() != expectedVersion) {
                return false;
            }
            store.put(gameState.session().sessionId(), gameState);
            return true;
        }
    }

    private static final class NoOpTelemetry implements EngineTelemetry {
        @Override
        public void record(String event, String detail) {
        }
    }

    private static final class NoOpMachineMovePort implements MachineMovePort {
        @Override
        public Optional<String> suggestMove(GameState gameState, List<String> legalMovesEnglish, LlmProvider provider) {
            return Optional.empty();
        }
    }

    private record FixedMachineMovePort(String move) implements MachineMovePort {
        @Override
        public Optional<String> suggestMove(GameState gameState, List<String> legalMovesEnglish, LlmProvider provider) {
            return Optional.of(move);
        }
    }
}

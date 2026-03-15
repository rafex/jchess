package dev.rafex.jchess.adapter.sqlite;

import dev.rafex.jchess.domain.model.GameSession;
import dev.rafex.jchess.domain.model.GameState;
import dev.rafex.jchess.domain.model.GameEndReason;
import dev.rafex.jchess.domain.model.GameResult;
import dev.rafex.jchess.domain.model.GameStatus;
import dev.rafex.jchess.domain.model.MachineGameMode;
import dev.rafex.jchess.domain.model.MachineLevel;
import dev.rafex.jchess.domain.model.ParticipantType;
import dev.rafex.jchess.domain.model.Position;
import dev.rafex.jchess.domain.model.RecordedMove;
import dev.rafex.jchess.domain.model.Side;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SqliteGameRepositoryTest {

    @Test
    void shouldPersistAndLoadSession(@TempDir Path tempDir) {
        SqliteGameRepository repository = new SqliteGameRepository(tempDir.resolve("jchess-test.db"));
        repository.initialize();

        UUID sessionId = UUID.randomUUID();
        GameState gameState = new GameState(
                new GameSession(
                        sessionId,
                        ParticipantType.HUMAN,
                        ParticipantType.MACHINE,
                        Side.WHITE,
                        null,
                        MachineGameMode.CASUAL,
                        MachineLevel.MEDIUM,
                        "5+0",
                        Position.initial(),
                        GameStatus.ACTIVE,
                        GameResult.IN_PROGRESS,
                        GameEndReason.NONE,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Alice",
                        "Bot",
                        "white-token",
                        "black-token",
                        300_000,
                        300_000,
                        Instant.now(),
                        0,
                        Instant.now(),
                        Instant.now()
                ),
                List.of(new RecordedMove(1, Side.WHITE, "Nf3", "Nf3", "g1f3",
                        Position.initial().toFen(),
                        "rnbqkbnr/pppppppp/8/8/8/5N2/PPPPPPPP/RNBQKB1R b KQkq - 1 1",
                        Instant.now()))
        );

        repository.save(gameState);

        GameState loaded = repository.findById(sessionId).orElseThrow();

        assertEquals(sessionId, loaded.session().sessionId());
        assertEquals(1, loaded.moves().size());
        assertEquals("Nf3", loaded.moves().getFirst().canonicalNotation());
        assertEquals("Alice", loaded.session().whitePlayerName());
        assertTrue(loaded.session().currentPosition().toFen().startsWith("rnbqkbnr/pppppppp"));
    }
}

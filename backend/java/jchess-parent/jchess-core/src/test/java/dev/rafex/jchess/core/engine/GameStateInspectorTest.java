package dev.rafex.jchess.core.engine;

import dev.rafex.jchess.domain.model.GameEndReason;
import dev.rafex.jchess.domain.model.GameResult;
import dev.rafex.jchess.domain.model.Position;
import dev.rafex.jchess.domain.model.RecordedMove;
import dev.rafex.jchess.domain.model.Side;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GameStateInspectorTest {
    private final GameStateInspector inspector = new GameStateInspector();

    @Test
    void shouldDetectCheckmate() {
        Position position = FenCodec.parse("7k/6Q1/6K1/8/8/8/8/8 b - - 0 1");

        GameTermination termination = inspector.inspect(position, List.of());

        assertEquals(GameResult.WHITE_WIN, termination.result());
        assertEquals(GameEndReason.CHECKMATE, termination.endReason());
    }

    @Test
    void shouldDetectStalemate() {
        Position position = FenCodec.parse("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1");

        GameTermination termination = inspector.inspect(position, List.of());

        assertEquals(GameResult.DRAW, termination.result());
        assertEquals(GameEndReason.STALEMATE, termination.endReason());
    }

    @Test
    void shouldDetectThreefoldRepetition() {
        Position position = Position.initial();
        String fen = position.toFen();
        List<RecordedMove> moves = List.of(
                new RecordedMove(1, Side.WHITE, "Nf3", "Nf3", "g1f3", fen, fen, Instant.now()),
                new RecordedMove(2, Side.BLACK, "Nf6", "Nf6", "g8f6", fen, fen, Instant.now())
        );

        GameTermination termination = inspector.inspect(position, moves);

        assertEquals(GameEndReason.THREEFOLD_REPETITION, termination.endReason());
    }
}

package dev.rafex.jchess.core.engine;

import dev.rafex.jchess.domain.model.Move;
import dev.rafex.jchess.domain.model.Position;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DefaultLegalMoveGeneratorTest {
    private final DefaultLegalMoveGenerator generator = new DefaultLegalMoveGenerator();
    private final PositionUpdater positionUpdater = new PositionUpdater();

    @Test
    void shouldGenerateTwentyLegalMovesFromInitialPosition() {
        List<Move> moves = generator.generateLegalMoves(Position.initial());

        assertEquals(20, moves.size());
    }

    @Test
    void shouldIncludeEnPassantCaptureWhenAvailable() {
        Position position = FenCodec.parse("rnbqkbnr/pppp1ppp/8/4pP2/8/8/PPPP2PP/RNBQKBNR w KQkq e6 0 3");

        List<Move> moves = generator.generateLegalMoves(position);

        assertTrue(moves.stream().anyMatch(move -> move.uci().equals("f5e6")));
    }

    @Test
    void shouldIncludeCastlingMovesWhenPathIsClearAndSafe() {
        Position position = FenCodec.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");

        List<Move> moves = generator.generateLegalMoves(position);

        assertTrue(moves.stream().anyMatch(move -> move.uci().equals("e1g1")));
        assertTrue(moves.stream().anyMatch(move -> move.uci().equals("e1c1")));
    }

    @Test
    void shouldApplyPawnAdvanceAndUpdateFen() {
        Position initial = Position.initial();
        Move move = generator.generateLegalMoves(initial).stream()
                .filter(candidate -> candidate.uci().equals("e2e4"))
                .findFirst()
                .orElseThrow();

        Position updated = positionUpdater.apply(initial, move);

        assertEquals("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1", FenCodec.toFen(updated));
    }
}

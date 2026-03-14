package dev.rafex.jchess.core.engine;

import dev.rafex.jchess.domain.model.Move;
import dev.rafex.jchess.domain.model.NotationLanguage;
import dev.rafex.jchess.domain.model.Position;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class MoveNotationServiceTest {
    private final DefaultLegalMoveGenerator generator = new DefaultLegalMoveGenerator();
    private final MoveNotationService notationService = new MoveNotationService();

    @Test
    void shouldRenderKnightNotationInEnglishAndSpanish() {
        Position position = Position.initial();
        List<Move> legalMoves = generator.generateLegalMoves(position);
        Move move = legalMoves.stream().filter(candidate -> candidate.uci().equals("g1f3")).findFirst().orElseThrow();

        assertEquals("Nf3", notationService.toNotation(position, move, NotationLanguage.ENGLISH, legalMoves));
        assertEquals("Cf3", notationService.toNotation(position, move, NotationLanguage.SPANISH, legalMoves));
    }

    @Test
    void shouldParseEnglishSpanishAndCoordinateNotation() {
        Position position = Position.initial();
        List<Move> legalMoves = generator.generateLegalMoves(position);

        assertEquals("g1f3", notationService.parse(position, "Nf3", legalMoves).uci());
        assertEquals("g1f3", notationService.parse(position, "Cf3)", legalMoves).uci());
        assertEquals("e2e4", notationService.parse(position, "e2e4", legalMoves).uci());
    }

    @Test
    void shouldPreferPawnForBareDestinationSquare() {
        Position position = FenCodec.parse("rnb1kbnr/pppp1ppp/4pq2/8/4P3/2N5/PPPP1PPP/R1BQKBNR w KQkq - 2 3");
        List<Move> legalMoves = generator.generateLegalMoves(position);

        assertEquals("d2d3", notationService.parse(position, "d3", legalMoves).uci());
        assertEquals("d2d3", notationService.parse(position, "Pd3", legalMoves).uci());
    }

    @Test
    void shouldMarkCheckInNotation() {
        Position position = FenCodec.parse("4k3/8/8/8/8/8/8/6RK w - - 0 1");
        List<Move> legalMoves = generator.generateLegalMoves(position);
        Move move = legalMoves.stream().filter(candidate -> candidate.uci().equals("g1g8")).findFirst().orElseThrow();

        assertEquals("Rg8+", notationService.toNotation(position, move, NotationLanguage.ENGLISH, legalMoves));
    }
}

package dev.rafex.jchess.core.engine;

import dev.rafex.jchess.domain.model.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class FenCodecTest {

    @Test
    void shouldRoundTripInitialFen() {
        Position position = Position.initial();

        String fen = FenCodec.toFen(position);

        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", fen);
        assertEquals(fen, FenCodec.toFen(FenCodec.parse(fen)));
    }
}

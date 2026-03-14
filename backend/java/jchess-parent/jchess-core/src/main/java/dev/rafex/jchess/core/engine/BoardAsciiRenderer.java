package dev.rafex.jchess.core.engine;

import dev.rafex.jchess.domain.model.Piece;
import dev.rafex.jchess.domain.model.Position;
import dev.rafex.jchess.domain.model.Square;

public final class BoardAsciiRenderer {
    public String render(Position position) {
        StringBuilder builder = new StringBuilder();
        for (int rank = 7; rank >= 0; rank--) {
            builder.append(rank + 1).append(" | ");
            for (int file = 0; file < 8; file++) {
                Piece piece = position.board().pieceAt(new Square(file, rank)).orElse(null);
                builder.append(piece == null ? '.' : piece.fenSymbol()).append(' ');
            }
            builder.append('\n');
        }
        builder.append("    ----------------\n");
        builder.append("    a b c d e f g h\n");
        return builder.toString();
    }
}

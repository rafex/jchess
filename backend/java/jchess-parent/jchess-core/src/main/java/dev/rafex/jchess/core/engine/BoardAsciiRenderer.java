package dev.rafex.jchess.core.engine;

import dev.rafex.jchess.domain.model.Piece;
import dev.rafex.jchess.domain.model.Position;
import dev.rafex.jchess.domain.model.Side;
import dev.rafex.jchess.domain.model.Square;

public final class BoardAsciiRenderer {
    private static final String RESET = "\u001B[0m";
    private static final String DARK_SQUARE = "\u001B[48;5;238m";
    private static final String LIGHT_SQUARE = "\u001B[48;5;250m";
    private static final String WHITE_PIECE = "\u001B[1;38;5;27m";
    private static final String BLACK_PIECE = "\u001B[1;38;5;160m";
    private static final String FRAME = "\u001B[38;5;39m";
    private static final String FILES = "\u001B[38;5;45m";

    public String render(Position position) {
        StringBuilder builder = new StringBuilder();
        builder.append(FRAME).append("    ╔═════╤═════╤═════╤═════╤═════╤═════╤═════╤═════╗").append(RESET).append('\n');

        for (int rank = 7; rank >= 0; rank--) {
            builder.append(FRAME).append(' ').append(rank + 1).append("  ║").append(RESET);
            for (int file = 0; file < 8; file++) {
                Piece piece = position.board().pieceAt(new Square(file, rank)).orElse(null);
                String squareColor = ((file + rank) & 1) == 0 ? LIGHT_SQUARE : DARK_SQUARE;
                builder.append(squareColor).append(renderCell(piece)).append(RESET);
                if (file < 7) {
                    builder.append(FRAME).append("│").append(RESET);
                }
            }
            builder.append(FRAME).append("║").append(RESET).append('\n');
            if (rank > 0) {
                builder.append(FRAME).append("    ╟─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────╢").append(RESET).append('\n');
            }
        }
        builder.append(FRAME).append("    ╚═════╧═════╧═════╧═════╧═════╧═════╧═════╧═════╝").append(RESET).append('\n');
        builder.append(FILES).append("       a     b     c     d     e     f     g     h").append(RESET).append('\n');
        return builder.toString();
    }

    private String renderCell(Piece piece) {
        String symbol = piece == null ? "  " : token(piece);
        String foreground = piece == null ? "" : (piece.side() == Side.WHITE ? WHITE_PIECE : BLACK_PIECE);
        return " " + foreground + symbol + RESET + " ";
    }

    private String token(Piece piece) {
        String prefix = piece.side() == Side.WHITE ? "W" : "B";
        String suffix = switch (piece.type()) {
            case KING -> "K";
            case QUEEN -> "Q";
            case ROOK -> "R";
            case BISHOP -> "B";
            case KNIGHT -> "N";
            case PAWN -> "P";
        };
        return prefix + suffix;
    }
}

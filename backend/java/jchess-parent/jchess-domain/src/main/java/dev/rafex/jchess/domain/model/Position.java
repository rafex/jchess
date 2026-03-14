package dev.rafex.jchess.domain.model;

import java.util.Objects;

public record Position(
        Board board,
        Side sideToMove,
        CastlingRights castlingRights,
        Square enPassantTarget,
        int halfmoveClock,
        int fullmoveNumber
) {
    public Position {
        board = Objects.requireNonNull(board, "board must not be null");
        sideToMove = Objects.requireNonNull(sideToMove, "sideToMove must not be null");
        castlingRights = Objects.requireNonNull(castlingRights, "castlingRights must not be null");

        if (halfmoveClock < 0) {
            throw new IllegalArgumentException("halfmoveClock must be >= 0");
        }
        if (fullmoveNumber < 1) {
            throw new IllegalArgumentException("fullmoveNumber must be >= 1");
        }
    }

    public static Position initial() {
        return new Position(Board.initial(), Side.WHITE, CastlingRights.initial(), null, 0, 1);
    }

    public Square kingSquare(Side side) {
        return board.findKing(side)
                .orElseThrow(() -> new IllegalStateException("king not found for side " + side));
    }

    public String toFen() {
        return board.toFenBoard() + " "
                + sideToMove.fenToken() + " "
                + castlingRights.toFenToken() + " "
                + (enPassantTarget == null ? "-" : enPassantTarget.toAlgebraic()) + " "
                + halfmoveClock + " "
                + fullmoveNumber;
    }
}

package dev.rafex.jchess.core.engine;

import dev.rafex.jchess.domain.model.Move;
import dev.rafex.jchess.domain.model.Piece;
import dev.rafex.jchess.domain.model.PieceType;
import dev.rafex.jchess.domain.model.Position;

public final class MoveInspector {
    private MoveInspector() {
    }

    public static boolean isKingSideCastle(Position position, Move move) {
        Piece piece = position.board().pieceAt(move.from()).orElse(null);
        return piece != null
                && piece.type() == PieceType.KING
                && move.to().file() - move.from().file() == 2;
    }

    public static boolean isQueenSideCastle(Position position, Move move) {
        Piece piece = position.board().pieceAt(move.from()).orElse(null);
        return piece != null
                && piece.type() == PieceType.KING
                && move.from().file() - move.to().file() == 2;
    }

    public static boolean isCapture(Position position, Move move) {
        Piece movingPiece = position.board().pieceAt(move.from()).orElse(null);
        if (movingPiece == null) {
            return false;
        }

        if (position.board().pieceAt(move.to()).isPresent()) {
            return true;
        }

        return movingPiece.type() == PieceType.PAWN
                && position.enPassantTarget() != null
                && position.enPassantTarget().equals(move.to())
                && move.from().file() != move.to().file();
    }
}

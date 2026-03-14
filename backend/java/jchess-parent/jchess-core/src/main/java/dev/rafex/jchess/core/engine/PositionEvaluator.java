package dev.rafex.jchess.core.engine;

import dev.rafex.jchess.domain.model.Piece;
import dev.rafex.jchess.domain.model.PieceType;
import dev.rafex.jchess.domain.model.Position;
import dev.rafex.jchess.domain.model.Side;
import dev.rafex.jchess.domain.model.Square;

import java.util.List;

public final class PositionEvaluator {
    private final DefaultLegalMoveGenerator legalMoveGenerator = new DefaultLegalMoveGenerator();
    private final PositionAnalyzer positionAnalyzer = new PositionAnalyzer();

    public int evaluate(Position position) {
        int material = 0;
        for (int index = 0; index < 64; index++) {
            Piece piece = position.board().pieceAt(Square.fromIndex(index)).orElse(null);
            if (piece == null) {
                continue;
            }
            int value = pieceValue(piece.type()) + pieceSquareBonus(piece, Square.fromIndex(index));
            material += piece.side() == Side.WHITE ? value : -value;
        }

        List<dev.rafex.jchess.domain.model.Move> currentMoves = legalMoveGenerator.generateLegalMoves(position);
        Position mirrored = new Position(
                position.board(),
                position.sideToMove().opposite(),
                position.castlingRights(),
                position.enPassantTarget(),
                position.halfmoveClock(),
                position.fullmoveNumber()
        );
        List<dev.rafex.jchess.domain.model.Move> opponentMoves = legalMoveGenerator.generateLegalMoves(mirrored);

        int mobility = (currentMoves.size() - opponentMoves.size()) * 5;
        int checkBonus = positionAnalyzer.isKingInCheck(position, Side.BLACK) ? 20 : 0;
        int checkPenalty = positionAnalyzer.isKingInCheck(position, Side.WHITE) ? 20 : 0;
        return material + mobility + checkBonus - checkPenalty;
    }

    private int pieceValue(PieceType pieceType) {
        return switch (pieceType) {
            case PAWN -> 100;
            case KNIGHT -> 320;
            case BISHOP -> 330;
            case ROOK -> 500;
            case QUEEN -> 900;
            case KING -> 20_000;
        };
    }

    private int pieceSquareBonus(Piece piece, Square square) {
        int centerDistance = Math.abs(3 - square.file()) + Math.abs(3 - square.rank());
        int bonus = 6 - centerDistance;
        if (piece.type() == PieceType.PAWN) {
            bonus += piece.side() == Side.WHITE ? square.rank() * 3 : (7 - square.rank()) * 3;
        }
        return Math.max(bonus, 0);
    }
}

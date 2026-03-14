package dev.rafex.jchess.core.engine;

import dev.rafex.jchess.domain.model.Piece;
import dev.rafex.jchess.domain.model.PieceType;
import dev.rafex.jchess.domain.model.Position;
import dev.rafex.jchess.domain.model.Side;
import dev.rafex.jchess.domain.model.Square;

public final class PositionAnalyzer {
    private static final int[][] KNIGHT_OFFSETS = {
            {1, 2}, {2, 1}, {2, -1}, {1, -2},
            {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}
    };
    private static final int[][] KING_OFFSETS = {
            {1, 0}, {1, 1}, {0, 1}, {-1, 1},
            {-1, 0}, {-1, -1}, {0, -1}, {1, -1}
    };
    private static final int[][] BISHOP_DIRECTIONS = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    private static final int[][] ROOK_DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public boolean isKingInCheck(Position position, Side side) {
        return isSquareAttacked(position, position.kingSquare(side), side.opposite());
    }

    public boolean isSquareAttacked(Position position, Square target, Side attacker) {
        int pawnDirection = attacker == Side.WHITE ? 1 : -1;
        for (int fileDelta : new int[]{-1, 1}) {
            Square square = target.offset(-fileDelta, -pawnDirection);
            if (square != null) {
                Piece piece = position.board().pieceAt(square).orElse(null);
                if (piece != null && piece.side() == attacker && piece.type() == PieceType.PAWN) {
                    return true;
                }
            }
        }

        if (hasLeaperAttack(position, target, attacker, PieceType.KNIGHT, KNIGHT_OFFSETS)) {
            return true;
        }

        if (hasSlidingAttack(position, target, attacker, new PieceType[]{PieceType.BISHOP, PieceType.QUEEN}, BISHOP_DIRECTIONS)) {
            return true;
        }

        if (hasSlidingAttack(position, target, attacker, new PieceType[]{PieceType.ROOK, PieceType.QUEEN}, ROOK_DIRECTIONS)) {
            return true;
        }

        return hasLeaperAttack(position, target, attacker, PieceType.KING, KING_OFFSETS);
    }

    private boolean hasLeaperAttack(Position position, Square target, Side attacker, PieceType pieceType, int[][] offsets) {
        for (int[] offset : offsets) {
            Square square = target.offset(offset[0], offset[1]);
            if (square == null) {
                continue;
            }
            Piece piece = position.board().pieceAt(square).orElse(null);
            if (piece != null && piece.side() == attacker && piece.type() == pieceType) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSlidingAttack(
            Position position,
            Square target,
            Side attacker,
            PieceType[] attackers,
            int[][] directions
    ) {
        for (int[] direction : directions) {
            Square square = target.offset(direction[0], direction[1]);
            while (square != null) {
                Piece piece = position.board().pieceAt(square).orElse(null);
                if (piece != null) {
                    if (piece.side() == attacker) {
                        for (PieceType attackerType : attackers) {
                            if (piece.type() == attackerType) {
                                return true;
                            }
                        }
                    }
                    break;
                }
                square = square.offset(direction[0], direction[1]);
            }
        }
        return false;
    }
}

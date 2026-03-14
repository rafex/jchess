package dev.rafex.jchess.core.engine;

import dev.rafex.jchess.domain.model.Move;
import dev.rafex.jchess.domain.model.Piece;
import dev.rafex.jchess.domain.model.PieceType;
import dev.rafex.jchess.domain.model.Position;
import dev.rafex.jchess.domain.model.Side;
import dev.rafex.jchess.domain.model.Square;

import java.util.ArrayList;
import java.util.List;

public final class DefaultLegalMoveGenerator implements LegalMoveGenerator {
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

    private final PositionUpdater positionUpdater = new PositionUpdater();
    private final PositionAnalyzer positionAnalyzer = new PositionAnalyzer();

    @Override
    public List<Move> generateLegalMoves(Position position) {
        List<Move> legalMoves = new ArrayList<>();
        Side side = position.sideToMove();

        for (int index = 0; index < 64; index++) {
            Square square = Square.fromIndex(index);
            Piece piece = position.board().pieceAt(square).orElse(null);
            if (piece == null || piece.side() != side) {
                continue;
            }

            for (Move candidate : generatePseudoLegalMoves(position, square, piece)) {
                Position nextPosition = positionUpdater.apply(position, candidate);
                if (!positionAnalyzer.isKingInCheck(nextPosition, side)) {
                    legalMoves.add(candidate);
                }
            }
        }

        return List.copyOf(legalMoves);
    }

    private List<Move> generatePseudoLegalMoves(Position position, Square from, Piece piece) {
        return switch (piece.type()) {
            case PAWN -> generatePawnMoves(position, from, piece.side());
            case KNIGHT -> generateLeaperMoves(position, from, piece.side(), KNIGHT_OFFSETS);
            case BISHOP -> generateSlidingMoves(position, from, piece.side(), BISHOP_DIRECTIONS);
            case ROOK -> generateSlidingMoves(position, from, piece.side(), ROOK_DIRECTIONS);
            case QUEEN -> generateSlidingMoves(position, from, piece.side(), mergeDirections());
            case KING -> generateKingMoves(position, from, piece.side());
        };
    }

    private List<Move> generatePawnMoves(Position position, Square from, Side side) {
        List<Move> moves = new ArrayList<>();
        int direction = side == Side.WHITE ? 1 : -1;
        int startRank = side == Side.WHITE ? 1 : 6;
        int promotionRank = side == Side.WHITE ? 7 : 0;

        Square oneStep = from.offset(0, direction);
        if (oneStep != null && position.board().pieceAt(oneStep).isEmpty()) {
            addPawnMove(moves, from, oneStep, promotionRank);

            Square twoStep = from.offset(0, direction * 2);
            if (from.rank() == startRank && twoStep != null && position.board().pieceAt(twoStep).isEmpty()) {
                moves.add(new Move(from, twoStep));
            }
        }

        for (int fileDelta : new int[]{-1, 1}) {
            Square target = from.offset(fileDelta, direction);
            if (target == null) {
                continue;
            }

            Piece targetPiece = position.board().pieceAt(target).orElse(null);
            boolean regularCapture = targetPiece != null && targetPiece.side() != side;
            boolean enPassantCapture = position.enPassantTarget() != null && position.enPassantTarget().equals(target);

            if (regularCapture || enPassantCapture) {
                addPawnMove(moves, from, target, promotionRank);
            }
        }

        return moves;
    }

    private void addPawnMove(List<Move> moves, Square from, Square to, int promotionRank) {
        if (to.rank() == promotionRank) {
            moves.add(new Move(from, to, PieceType.QUEEN));
            moves.add(new Move(from, to, PieceType.ROOK));
            moves.add(new Move(from, to, PieceType.BISHOP));
            moves.add(new Move(from, to, PieceType.KNIGHT));
            return;
        }
        moves.add(new Move(from, to));
    }

    private List<Move> generateLeaperMoves(Position position, Square from, Side side, int[][] offsets) {
        List<Move> moves = new ArrayList<>();
        for (int[] offset : offsets) {
            Square target = from.offset(offset[0], offset[1]);
            if (target == null) {
                continue;
            }

            Piece targetPiece = position.board().pieceAt(target).orElse(null);
            if (targetPiece == null || targetPiece.side() != side) {
                moves.add(new Move(from, target));
            }
        }
        return moves;
    }

    private List<Move> generateSlidingMoves(Position position, Square from, Side side, int[][] directions) {
        List<Move> moves = new ArrayList<>();

        for (int[] direction : directions) {
            Square target = from.offset(direction[0], direction[1]);
            while (target != null) {
                Piece targetPiece = position.board().pieceAt(target).orElse(null);
                if (targetPiece == null) {
                    moves.add(new Move(from, target));
                } else {
                    if (targetPiece.side() != side) {
                        moves.add(new Move(from, target));
                    }
                    break;
                }
                target = target.offset(direction[0], direction[1]);
            }
        }

        return moves;
    }

    private List<Move> generateKingMoves(Position position, Square from, Side side) {
        List<Move> moves = new ArrayList<>(generateLeaperMoves(position, from, side, KING_OFFSETS));

        if (positionAnalyzer.isKingInCheck(position, side)) {
            return moves;
        }

        if (side == Side.WHITE) {
            addCastlingIfAvailable(position, moves, from, side, true, "f1", "g1");
            addCastlingIfAvailable(position, moves, from, side, false, "d1", "c1", "b1");
        } else {
            addCastlingIfAvailable(position, moves, from, side, true, "f8", "g8");
            addCastlingIfAvailable(position, moves, from, side, false, "d8", "c8", "b8");
        }

        return moves;
    }

    private void addCastlingIfAvailable(
            Position position,
            List<Move> moves,
            Square kingSquare,
            Side side,
            boolean kingSide,
            String... traversedSquares
    ) {
        boolean allowed = side == Side.WHITE
                ? (kingSide ? position.castlingRights().whiteKingSide() : position.castlingRights().whiteQueenSide())
                : (kingSide ? position.castlingRights().blackKingSide() : position.castlingRights().blackQueenSide());

        if (!allowed) {
            return;
        }

        for (String algebraic : traversedSquares) {
            Square square = Square.fromAlgebraic(algebraic);
            if (position.board().pieceAt(square).isPresent()) {
                return;
            }
        }

        String[] kingPath = kingSide
                ? new String[]{side == Side.WHITE ? "f1" : "f8", side == Side.WHITE ? "g1" : "g8"}
                : new String[]{side == Side.WHITE ? "d1" : "d8", side == Side.WHITE ? "c1" : "c8"};

        for (String algebraic : kingPath) {
            Square square = Square.fromAlgebraic(algebraic);
            if (positionAnalyzer.isSquareAttacked(position, square, side.opposite())) {
                return;
            }
        }

        Square destination = Square.fromAlgebraic(kingPath[1]);
        moves.add(new Move(kingSquare, destination));
    }
    private int[][] mergeDirections() {
        int[][] directions = new int[BISHOP_DIRECTIONS.length + ROOK_DIRECTIONS.length][2];
        System.arraycopy(BISHOP_DIRECTIONS, 0, directions, 0, BISHOP_DIRECTIONS.length);
        System.arraycopy(ROOK_DIRECTIONS, 0, directions, BISHOP_DIRECTIONS.length, ROOK_DIRECTIONS.length);
        return directions;
    }
}

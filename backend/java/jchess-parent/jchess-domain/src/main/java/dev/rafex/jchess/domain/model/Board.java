package dev.rafex.jchess.domain.model;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public final class Board {
    private final Piece[] squares;

    private Board(Piece[] squares) {
        this.squares = squares;
    }

    public static Board empty() {
        return new Board(new Piece[64]);
    }

    public static Board initial() {
        Board board = Board.empty();

        for (int file = 0; file < 8; file++) {
            board = board.withPiece(new Square(file, 1), new Piece(Side.WHITE, PieceType.PAWN));
            board = board.withPiece(new Square(file, 6), new Piece(Side.BLACK, PieceType.PAWN));
        }

        board = board.withPiece(Square.fromAlgebraic("a1"), new Piece(Side.WHITE, PieceType.ROOK));
        board = board.withPiece(Square.fromAlgebraic("b1"), new Piece(Side.WHITE, PieceType.KNIGHT));
        board = board.withPiece(Square.fromAlgebraic("c1"), new Piece(Side.WHITE, PieceType.BISHOP));
        board = board.withPiece(Square.fromAlgebraic("d1"), new Piece(Side.WHITE, PieceType.QUEEN));
        board = board.withPiece(Square.fromAlgebraic("e1"), new Piece(Side.WHITE, PieceType.KING));
        board = board.withPiece(Square.fromAlgebraic("f1"), new Piece(Side.WHITE, PieceType.BISHOP));
        board = board.withPiece(Square.fromAlgebraic("g1"), new Piece(Side.WHITE, PieceType.KNIGHT));
        board = board.withPiece(Square.fromAlgebraic("h1"), new Piece(Side.WHITE, PieceType.ROOK));

        board = board.withPiece(Square.fromAlgebraic("a8"), new Piece(Side.BLACK, PieceType.ROOK));
        board = board.withPiece(Square.fromAlgebraic("b8"), new Piece(Side.BLACK, PieceType.KNIGHT));
        board = board.withPiece(Square.fromAlgebraic("c8"), new Piece(Side.BLACK, PieceType.BISHOP));
        board = board.withPiece(Square.fromAlgebraic("d8"), new Piece(Side.BLACK, PieceType.QUEEN));
        board = board.withPiece(Square.fromAlgebraic("e8"), new Piece(Side.BLACK, PieceType.KING));
        board = board.withPiece(Square.fromAlgebraic("f8"), new Piece(Side.BLACK, PieceType.BISHOP));
        board = board.withPiece(Square.fromAlgebraic("g8"), new Piece(Side.BLACK, PieceType.KNIGHT));
        board = board.withPiece(Square.fromAlgebraic("h8"), new Piece(Side.BLACK, PieceType.ROOK));

        return board;
    }

    public Optional<Piece> pieceAt(Square square) {
        Objects.requireNonNull(square, "square must not be null");
        return Optional.ofNullable(squares[square.index()]);
    }

    public Board withPiece(Square square, Piece piece) {
        Objects.requireNonNull(square, "square must not be null");
        Piece[] copy = Arrays.copyOf(squares, squares.length);
        copy[square.index()] = piece;
        return new Board(copy);
    }

    public Board withoutPiece(Square square) {
        return withPiece(square, null);
    }

    public Optional<Square> findKing(Side side) {
        for (int index = 0; index < squares.length; index++) {
            Piece piece = squares[index];
            if (piece != null && piece.side() == side && piece.type() == PieceType.KING) {
                return Optional.of(Square.fromIndex(index));
            }
        }
        return Optional.empty();
    }

    public String toFenBoard() {
        StringBuilder builder = new StringBuilder();

        for (int rank = 7; rank >= 0; rank--) {
            int emptyCount = 0;
            for (int file = 0; file < 8; file++) {
                Piece piece = squares[rank * 8 + file];
                if (piece == null) {
                    emptyCount++;
                    continue;
                }
                if (emptyCount > 0) {
                    builder.append(emptyCount);
                    emptyCount = 0;
                }
                builder.append(piece.fenSymbol());
            }
            if (emptyCount > 0) {
                builder.append(emptyCount);
            }
            if (rank > 0) {
                builder.append('/');
            }
        }

        return builder.toString();
    }
}

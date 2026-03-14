package dev.rafex.jchess.core.engine;

import dev.rafex.jchess.domain.model.Board;
import dev.rafex.jchess.domain.model.CastlingRights;
import dev.rafex.jchess.domain.model.Move;
import dev.rafex.jchess.domain.model.Piece;
import dev.rafex.jchess.domain.model.PieceType;
import dev.rafex.jchess.domain.model.Position;
import dev.rafex.jchess.domain.model.Side;
import dev.rafex.jchess.domain.model.Square;

public final class PositionUpdater {
    public Position apply(Position position, Move move) {
        Piece movingPiece = position.board().pieceAt(move.from())
                .orElseThrow(() -> new IllegalArgumentException("no piece at " + move.from().toAlgebraic()));

        Piece capturedPiece = position.board().pieceAt(move.to()).orElse(null);
        boolean enPassantCapture = movingPiece.type() == PieceType.PAWN
                && position.enPassantTarget() != null
                && move.to().equals(position.enPassantTarget())
                && move.from().file() != move.to().file()
                && capturedPiece == null;

        Board board = position.board().withoutPiece(move.from());

        if (enPassantCapture) {
            Square capturedPawnSquare = new Square(move.to().file(), move.from().rank());
            board = board.withoutPiece(capturedPawnSquare);
        } else if (capturedPiece != null) {
            board = board.withoutPiece(move.to());
        }

        Piece placedPiece = move.promotion() == null
                ? movingPiece
                : new Piece(movingPiece.side(), move.promotion());
        board = board.withPiece(move.to(), placedPiece);

        if (movingPiece.type() == PieceType.KING && Math.abs(move.to().file() - move.from().file()) == 2) {
            board = applyCastlingRookMove(board, movingPiece.side(), move.to().file() > move.from().file());
        }

        CastlingRights castlingRights = updateCastlingRights(position.castlingRights(), move, movingPiece, capturedPiece);
        Square enPassantTarget = determineEnPassantTarget(move, movingPiece);
        int halfmoveClock = determineHalfmoveClock(position.halfmoveClock(), movingPiece, capturedPiece, enPassantCapture);
        int fullmoveNumber = position.sideToMove() == Side.BLACK ? position.fullmoveNumber() + 1 : position.fullmoveNumber();

        return new Position(
                board,
                position.sideToMove().opposite(),
                castlingRights,
                enPassantTarget,
                halfmoveClock,
                fullmoveNumber
        );
    }

    private Board applyCastlingRookMove(Board board, Side side, boolean kingSide) {
        Square rookFrom = side == Side.WHITE
                ? Square.fromAlgebraic(kingSide ? "h1" : "a1")
                : Square.fromAlgebraic(kingSide ? "h8" : "a8");
        Square rookTo = side == Side.WHITE
                ? Square.fromAlgebraic(kingSide ? "f1" : "d1")
                : Square.fromAlgebraic(kingSide ? "f8" : "d8");

        Piece rook = board.pieceAt(rookFrom)
                .orElseThrow(() -> new IllegalStateException("rook missing during castling"));

        return board.withoutPiece(rookFrom).withPiece(rookTo, rook);
    }

    private CastlingRights updateCastlingRights(
            CastlingRights rights,
            Move move,
            Piece movingPiece,
            Piece capturedPiece
    ) {
        boolean whiteKing = rights.whiteKingSide();
        boolean whiteQueen = rights.whiteQueenSide();
        boolean blackKing = rights.blackKingSide();
        boolean blackQueen = rights.blackQueenSide();

        if (movingPiece.type() == PieceType.KING) {
            if (movingPiece.side() == Side.WHITE) {
                whiteKing = false;
                whiteQueen = false;
            } else {
                blackKing = false;
                blackQueen = false;
            }
        }

        if (movingPiece.type() == PieceType.ROOK) {
            if (move.from().equals(Square.fromAlgebraic("h1"))) {
                whiteKing = false;
            } else if (move.from().equals(Square.fromAlgebraic("a1"))) {
                whiteQueen = false;
            } else if (move.from().equals(Square.fromAlgebraic("h8"))) {
                blackKing = false;
            } else if (move.from().equals(Square.fromAlgebraic("a8"))) {
                blackQueen = false;
            }
        }

        if (capturedPiece != null && capturedPiece.type() == PieceType.ROOK) {
            if (move.to().equals(Square.fromAlgebraic("h1"))) {
                whiteKing = false;
            } else if (move.to().equals(Square.fromAlgebraic("a1"))) {
                whiteQueen = false;
            } else if (move.to().equals(Square.fromAlgebraic("h8"))) {
                blackKing = false;
            } else if (move.to().equals(Square.fromAlgebraic("a8"))) {
                blackQueen = false;
            }
        }

        return new CastlingRights(whiteKing, whiteQueen, blackKing, blackQueen);
    }

    private Square determineEnPassantTarget(Move move, Piece movingPiece) {
        if (movingPiece.type() != PieceType.PAWN) {
            return null;
        }

        if (Math.abs(move.to().rank() - move.from().rank()) == 2) {
            return new Square(move.from().file(), (move.from().rank() + move.to().rank()) / 2);
        }

        return null;
    }

    private int determineHalfmoveClock(int previous, Piece movingPiece, Piece capturedPiece, boolean enPassantCapture) {
        if (movingPiece.type() == PieceType.PAWN || capturedPiece != null || enPassantCapture) {
            return 0;
        }
        return previous + 1;
    }
}

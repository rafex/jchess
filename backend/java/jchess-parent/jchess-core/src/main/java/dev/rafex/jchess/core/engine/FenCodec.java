package dev.rafex.jchess.core.engine;

import dev.rafex.jchess.domain.model.Board;
import dev.rafex.jchess.domain.model.CastlingRights;
import dev.rafex.jchess.domain.model.Piece;
import dev.rafex.jchess.domain.model.PieceType;
import dev.rafex.jchess.domain.model.Position;
import dev.rafex.jchess.domain.model.Side;
import dev.rafex.jchess.domain.model.Square;

public final class FenCodec {
    private FenCodec() {
    }

    public static Position parse(String fen) {
        if (fen == null || fen.isBlank()) {
            throw new IllegalArgumentException("fen must not be blank");
        }

        String[] parts = fen.trim().split("\\s+");
        if (parts.length != 6) {
            throw new IllegalArgumentException("fen must contain 6 fields");
        }

        Board board = parseBoard(parts[0]);
        Side sideToMove = "w".equals(parts[1]) ? Side.WHITE : Side.BLACK;
        CastlingRights castlingRights = parseCastlingRights(parts[2]);
        Square enPassantTarget = "-".equals(parts[3]) ? null : Square.fromAlgebraic(parts[3]);
        int halfmoveClock = Integer.parseInt(parts[4]);
        int fullmoveNumber = Integer.parseInt(parts[5]);

        return new Position(board, sideToMove, castlingRights, enPassantTarget, halfmoveClock, fullmoveNumber);
    }

    public static String toFen(Position position) {
        return position.toFen();
    }

    private static Board parseBoard(String boardToken) {
        String[] ranks = boardToken.split("/");
        if (ranks.length != 8) {
            throw new IllegalArgumentException("fen board must have 8 ranks");
        }

        Board board = Board.empty();
        for (int fenRank = 0; fenRank < 8; fenRank++) {
            String rankToken = ranks[fenRank];
            int boardRank = 7 - fenRank;
            int file = 0;

            for (int i = 0; i < rankToken.length(); i++) {
                char token = rankToken.charAt(i);
                if (Character.isDigit(token)) {
                    file += token - '0';
                    continue;
                }

                Side side = Character.isUpperCase(token) ? Side.WHITE : Side.BLACK;
                PieceType type = PieceType.fromFenSymbol(token);
                board = board.withPiece(new Square(file, boardRank), new Piece(side, type));
                file++;
            }

            if (file != 8) {
                throw new IllegalArgumentException("invalid fen rank: " + rankToken);
            }
        }

        return board;
    }

    private static CastlingRights parseCastlingRights(String token) {
        if ("-".equals(token)) {
            return CastlingRights.none();
        }

        boolean whiteKing = token.indexOf('K') >= 0;
        boolean whiteQueen = token.indexOf('Q') >= 0;
        boolean blackKing = token.indexOf('k') >= 0;
        boolean blackQueen = token.indexOf('q') >= 0;
        return new CastlingRights(whiteKing, whiteQueen, blackKing, blackQueen);
    }
}

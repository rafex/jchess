package dev.rafex.jchess.domain.model;

public record CastlingRights(
        boolean whiteKingSide,
        boolean whiteQueenSide,
        boolean blackKingSide,
        boolean blackQueenSide
) {
    public static CastlingRights initial() {
        return new CastlingRights(true, true, true, true);
    }

    public static CastlingRights none() {
        return new CastlingRights(false, false, false, false);
    }

    public String toFenToken() {
        StringBuilder builder = new StringBuilder();
        if (whiteKingSide) {
            builder.append('K');
        }
        if (whiteQueenSide) {
            builder.append('Q');
        }
        if (blackKingSide) {
            builder.append('k');
        }
        if (blackQueenSide) {
            builder.append('q');
        }
        return builder.isEmpty() ? "-" : builder.toString();
    }
}

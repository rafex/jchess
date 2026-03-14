package dev.rafex.jchess.domain.model;

public enum PieceType {
    PAWN('p'),
    KNIGHT('n'),
    BISHOP('b'),
    ROOK('r'),
    QUEEN('q'),
    KING('k');

    private final char fenSymbol;

    PieceType(char fenSymbol) {
        this.fenSymbol = fenSymbol;
    }

    public char fenSymbol() {
        return fenSymbol;
    }

    public static PieceType fromFenSymbol(char symbol) {
        char normalized = Character.toLowerCase(symbol);
        for (PieceType type : values()) {
            if (type.fenSymbol == normalized) {
                return type;
            }
        }
        throw new IllegalArgumentException("unsupported piece symbol: " + symbol);
    }
}

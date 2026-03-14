package dev.rafex.jchess.domain.model;

import java.util.Objects;

public record Piece(Side side, PieceType type) {

    public Piece {
        side = Objects.requireNonNull(side, "side must not be null");
        type = Objects.requireNonNull(type, "type must not be null");
    }

    public char fenSymbol() {
        char symbol = type.fenSymbol();
        return side == Side.WHITE ? Character.toUpperCase(symbol) : symbol;
    }
}

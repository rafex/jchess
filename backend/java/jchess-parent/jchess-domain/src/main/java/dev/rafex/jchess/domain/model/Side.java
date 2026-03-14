package dev.rafex.jchess.domain.model;

public enum Side {
    WHITE,
    BLACK;

    public Side opposite() {
        return this == WHITE ? BLACK : WHITE;
    }

    public String fenToken() {
        return this == WHITE ? "w" : "b";
    }
}

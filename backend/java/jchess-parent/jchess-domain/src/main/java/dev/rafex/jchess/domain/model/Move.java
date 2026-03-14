package dev.rafex.jchess.domain.model;

import java.util.Objects;

public record Move(Square from, Square to, PieceType promotion, boolean isNullMove) {

    public Move {
        if (isNullMove) {
            from = Square.fromAlgebraic("a1");
            to = Square.fromAlgebraic("a1");
            promotion = null;
        } else {
            from = Objects.requireNonNull(from, "from must not be null");
            to = Objects.requireNonNull(to, "to must not be null");
            if (from.equals(to)) {
                throw new IllegalArgumentException("from and to must be different");
            }
        }
    }

    public Move(Square from, Square to, PieceType promotion) {
        this(from, to, promotion, false);
    }

    public Move(Square from, Square to) {
        this(from, to, null, false);
    }

    public static Move nullMove() {
        return new Move(null, null, null, true);
    }

    public String uci() {
        if (isNullMove) {
            return "0000";
        }

        String promotionSuffix = promotion == null ? "" : String.valueOf(Character.toLowerCase(promotion.fenSymbol()));
        return from.toAlgebraic() + to.toAlgebraic() + promotionSuffix;
    }
}

package dev.rafex.jchess.domain.model;

import java.util.Locale;
import java.util.Objects;

public record MoveRequest(
        String playerToken,
        String from,
        String to,
        PieceType promotion
) {
    public MoveRequest {
        playerToken = Objects.requireNonNull(playerToken, "playerToken must not be null").trim();
        from = normalizeSquare(from, "from");
        to = normalizeSquare(to, "to");
        if (playerToken.isBlank()) {
            throw new IllegalArgumentException("playerToken must not be blank");
        }
    }

    public static MoveRequest of(String playerToken, String from, String to, String promotion) {
        return new MoveRequest(playerToken, from, to, parsePromotion(promotion));
    }

    private static String normalizeSquare(String value, String label) {
        String normalized = Objects.requireNonNull(value, label + " must not be null").trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[a-h][1-8]$")) {
            throw new IllegalArgumentException("invalid square for " + label + ": " + value);
        }
        return normalized;
    }

    private static PieceType parsePromotion(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "q", "queen", "reina" -> PieceType.QUEEN;
            case "r", "rook", "torre" -> PieceType.ROOK;
            case "b", "bishop", "alfil" -> PieceType.BISHOP;
            case "n", "knight", "caballo" -> PieceType.KNIGHT;
            default -> throw new IllegalArgumentException("unsupported promotion piece: " + value);
        };
    }
}

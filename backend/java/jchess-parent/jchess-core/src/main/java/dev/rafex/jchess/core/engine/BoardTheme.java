package dev.rafex.jchess.core.engine;

import java.util.Map;
import java.util.Objects;

public record BoardTheme(
        String name,
        String description,
        int minimumCellWidth,
        String topLeft,
        String topRight,
        String bottomLeft,
        String bottomRight,
        String horizontal,
        String vertical,
        String joinTop,
        String joinMiddle,
        String joinBottom,
        String leftJoin,
        String rightJoin,
        String frameColor,
        String fileColor,
        String lightSquareBackground,
        String darkSquareBackground,
        String whitePieceColor,
        String blackPieceColor,
        String emptyToken,
        Map<String, String> tokens
) {
    public BoardTheme {
        name = Objects.requireNonNull(name, "name");
        description = Objects.requireNonNull(description, "description");
        tokens = Map.copyOf(Objects.requireNonNull(tokens, "tokens"));
        if (minimumCellWidth < 1) {
            throw new IllegalArgumentException("minimumCellWidth must be >= 1");
        }
    }

    public String token(String key) {
        return tokens.getOrDefault(key, emptyToken);
    }
}

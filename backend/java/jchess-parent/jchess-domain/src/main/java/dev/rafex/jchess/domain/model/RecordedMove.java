package dev.rafex.jchess.domain.model;

import java.time.Instant;
import java.util.Objects;

public record RecordedMove(
        int ply,
        Side side,
        String submittedNotation,
        String canonicalNotation,
        String uci,
        String fenBefore,
        String fenAfter,
        Instant playedAt
) {
    public RecordedMove {
        if (ply < 1) {
            throw new IllegalArgumentException("ply must be >= 1");
        }
        side = Objects.requireNonNull(side, "side must not be null");
        submittedNotation = Objects.requireNonNull(submittedNotation, "submittedNotation must not be null");
        canonicalNotation = Objects.requireNonNull(canonicalNotation, "canonicalNotation must not be null");
        uci = Objects.requireNonNull(uci, "uci must not be null");
        fenBefore = Objects.requireNonNull(fenBefore, "fenBefore must not be null");
        fenAfter = Objects.requireNonNull(fenAfter, "fenAfter must not be null");
        playedAt = Objects.requireNonNull(playedAt, "playedAt must not be null");
    }
}

package dev.rafex.jchess.domain.model;

import java.util.Objects;

public record GameSessionAccess(
        GameSnapshot snapshot,
        GamePlayerAccess whitePlayer,
        GamePlayerAccess blackPlayer,
        GamePlayerAccess requester
) {
    public GameSessionAccess {
        snapshot = Objects.requireNonNull(snapshot, "snapshot must not be null");
        whitePlayer = Objects.requireNonNull(whitePlayer, "whitePlayer must not be null");
        blackPlayer = Objects.requireNonNull(blackPlayer, "blackPlayer must not be null");
    }
}

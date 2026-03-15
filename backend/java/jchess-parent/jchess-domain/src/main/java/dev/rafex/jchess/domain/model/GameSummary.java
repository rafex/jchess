package dev.rafex.jchess.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record GameSummary(
        UUID sessionId,
        GameStatus status,
        GameResult result,
        GameEndReason endReason,
        String whitePlayerName,
        String blackPlayerName,
        Side turn,
        int moveCount,
        Instant createdAt,
        Instant updatedAt
) {
    public GameSummary {
        sessionId = Objects.requireNonNull(sessionId, "sessionId must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
        result = Objects.requireNonNull(result, "result must not be null");
        endReason = Objects.requireNonNull(endReason, "endReason must not be null");
        whitePlayerName = Objects.requireNonNull(whitePlayerName, "whitePlayerName must not be null");
        blackPlayerName = Objects.requireNonNull(blackPlayerName, "blackPlayerName must not be null");
        turn = Objects.requireNonNull(turn, "turn must not be null");
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}

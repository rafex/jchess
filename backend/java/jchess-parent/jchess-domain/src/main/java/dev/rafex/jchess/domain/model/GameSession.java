package dev.rafex.jchess.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record GameSession(
        UUID sessionId,
        ParticipantType whiteParticipant,
        ParticipantType blackParticipant,
        Side preferredHumanSide,
        LlmProvider llmProvider,
        Position currentPosition,
        GameStatus status,
        GameResult result,
        GameEndReason endReason,
        String whitePlayerName,
        String blackPlayerName,
        long version,
        Instant createdAt
) {
    public GameSession {
        sessionId = Objects.requireNonNull(sessionId, "sessionId must not be null");
        whiteParticipant = Objects.requireNonNull(whiteParticipant, "whiteParticipant must not be null");
        blackParticipant = Objects.requireNonNull(blackParticipant, "blackParticipant must not be null");
        currentPosition = Objects.requireNonNull(currentPosition, "currentPosition must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
        result = Objects.requireNonNull(result, "result must not be null");
        endReason = Objects.requireNonNull(endReason, "endReason must not be null");
        whitePlayerName = Objects.requireNonNull(whitePlayerName, "whitePlayerName must not be null");
        blackPlayerName = Objects.requireNonNull(blackPlayerName, "blackPlayerName must not be null");
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public ParticipantType participantFor(Side side) {
        return side == Side.WHITE ? whiteParticipant : blackParticipant;
    }

    public boolean machineToMove() {
        return participantFor(currentPosition.sideToMove()) == ParticipantType.MACHINE;
    }

    public GameSession withState(Position position, GameStatus nextStatus, GameResult nextResult, GameEndReason nextEndReason) {
        return new GameSession(
                sessionId,
                whiteParticipant,
                blackParticipant,
                preferredHumanSide,
                llmProvider,
                position,
                nextStatus,
                nextResult,
                nextEndReason,
                whitePlayerName,
                blackPlayerName,
                version + 1,
                createdAt
        );
    }
}

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
        UUID whitePlayerId,
        UUID blackPlayerId,
        String whitePlayerName,
        String blackPlayerName,
        String whitePlayerToken,
        String blackPlayerToken,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
    public GameSession {
        sessionId = Objects.requireNonNull(sessionId, "sessionId must not be null");
        whiteParticipant = Objects.requireNonNull(whiteParticipant, "whiteParticipant must not be null");
        blackParticipant = Objects.requireNonNull(blackParticipant, "blackParticipant must not be null");
        currentPosition = Objects.requireNonNull(currentPosition, "currentPosition must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
        result = Objects.requireNonNull(result, "result must not be null");
        endReason = Objects.requireNonNull(endReason, "endReason must not be null");
        whitePlayerId = Objects.requireNonNull(whitePlayerId, "whitePlayerId must not be null");
        blackPlayerId = Objects.requireNonNull(blackPlayerId, "blackPlayerId must not be null");
        whitePlayerName = Objects.requireNonNull(whitePlayerName, "whitePlayerName must not be null");
        blackPlayerName = Objects.requireNonNull(blackPlayerName, "blackPlayerName must not be null");
        whitePlayerToken = Objects.requireNonNull(whitePlayerToken, "whitePlayerToken must not be null");
        blackPlayerToken = Objects.requireNonNull(blackPlayerToken, "blackPlayerToken must not be null");
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public ParticipantType participantFor(Side side) {
        return side == Side.WHITE ? whiteParticipant : blackParticipant;
    }

    public boolean machineToMove() {
        return participantFor(currentPosition.sideToMove()) == ParticipantType.MACHINE;
    }

    public UUID playerIdFor(Side side) {
        return side == Side.WHITE ? whitePlayerId : blackPlayerId;
    }

    public String playerNameFor(Side side) {
        return side == Side.WHITE ? whitePlayerName : blackPlayerName;
    }

    public String playerTokenFor(Side side) {
        return side == Side.WHITE ? whitePlayerToken : blackPlayerToken;
    }

    public Side sideForToken(String playerToken) {
        if (playerToken == null || playerToken.isBlank()) {
            throw new IllegalArgumentException("missing player token");
        }
        if (whitePlayerToken.equals(playerToken)) {
            return Side.WHITE;
        }
        if (blackPlayerToken.equals(playerToken)) {
            return Side.BLACK;
        }
        throw new IllegalArgumentException("invalid player token");
    }

    public GamePlayerAccess playerAccess(Side side) {
        return new GamePlayerAccess(
                playerIdFor(side),
                side,
                participantFor(side),
                playerNameFor(side),
                playerTokenFor(side)
        );
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
                whitePlayerId,
                blackPlayerId,
                whitePlayerName,
                blackPlayerName,
                whitePlayerToken,
                blackPlayerToken,
                version + 1,
                createdAt,
                Instant.now()
        );
    }
}

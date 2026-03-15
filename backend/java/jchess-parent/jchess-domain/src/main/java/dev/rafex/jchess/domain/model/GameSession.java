package dev.rafex.jchess.domain.model;

import java.time.Instant;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

public record GameSession(
        UUID sessionId,
        ParticipantType whiteParticipant,
        ParticipantType blackParticipant,
        Side preferredHumanSide,
        LlmProvider llmProvider,
        MachineGameMode machineMode,
        MachineLevel machineLevel,
        String timeControl,
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
        long whiteClockMs,
        long blackClockMs,
        Instant clockStartedAt,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
    public GameSession {
        sessionId = Objects.requireNonNull(sessionId, "sessionId must not be null");
        whiteParticipant = Objects.requireNonNull(whiteParticipant, "whiteParticipant must not be null");
        blackParticipant = Objects.requireNonNull(blackParticipant, "blackParticipant must not be null");
        machineMode = machineMode == null ? MachineGameMode.CASUAL : machineMode;
        machineLevel = machineLevel == null ? MachineLevel.MEDIUM : machineLevel;
        currentPosition = Objects.requireNonNull(currentPosition, "currentPosition must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
        result = Objects.requireNonNull(result, "result must not be null");
        endReason = Objects.requireNonNull(endReason, "endReason must not be null");
        timeControl = timeControl == null || timeControl.isBlank() ? "5+0" : timeControl;
        whitePlayerId = Objects.requireNonNull(whitePlayerId, "whitePlayerId must not be null");
        blackPlayerId = Objects.requireNonNull(blackPlayerId, "blackPlayerId must not be null");
        whitePlayerName = Objects.requireNonNull(whitePlayerName, "whitePlayerName must not be null");
        blackPlayerName = Objects.requireNonNull(blackPlayerName, "blackPlayerName must not be null");
        whitePlayerToken = Objects.requireNonNull(whitePlayerToken, "whitePlayerToken must not be null");
        blackPlayerToken = Objects.requireNonNull(blackPlayerToken, "blackPlayerToken must not be null");
        if (whiteClockMs < 0 || blackClockMs < 0) {
            throw new IllegalArgumentException("clock values must not be negative");
        }
        clockStartedAt = Objects.requireNonNull(clockStartedAt, "clockStartedAt must not be null");
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

    public long remainingClockMs(Side side, Instant now) {
        long base = side == Side.WHITE ? whiteClockMs : blackClockMs;
        if (status != GameStatus.ACTIVE || currentPosition.sideToMove() != side) {
            return base;
        }
        long elapsed = Math.max(0L, Duration.between(clockStartedAt, now).toMillis());
        return Math.max(0L, base - elapsed);
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
        return withStateAndClock(position, nextStatus, nextResult, nextEndReason, whiteClockMs, blackClockMs, clockStartedAt);
    }

    public GameSession withStateAndClock(
            Position position,
            GameStatus nextStatus,
            GameResult nextResult,
            GameEndReason nextEndReason,
            long nextWhiteClockMs,
            long nextBlackClockMs,
            Instant nextClockStartedAt
    ) {
        return new GameSession(
                sessionId,
                whiteParticipant,
                blackParticipant,
                preferredHumanSide,
                llmProvider,
                machineMode,
                machineLevel,
                timeControl,
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
                nextWhiteClockMs,
                nextBlackClockMs,
                nextClockStartedAt,
                version + 1,
                createdAt,
                Instant.now()
        );
    }
}

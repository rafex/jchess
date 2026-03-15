package dev.rafex.jchess.domain.model;

import java.util.Objects;
import java.util.UUID;

public record GamePlayerAccess(
        UUID playerId,
        Side side,
        ParticipantType participantType,
        String displayName,
        String playerToken
) {
    public GamePlayerAccess {
        playerId = Objects.requireNonNull(playerId, "playerId must not be null");
        side = Objects.requireNonNull(side, "side must not be null");
        participantType = Objects.requireNonNull(participantType, "participantType must not be null");
        displayName = Objects.requireNonNull(displayName, "displayName must not be null");
        playerToken = Objects.requireNonNull(playerToken, "playerToken must not be null");
    }
}

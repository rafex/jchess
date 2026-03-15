package dev.rafex.jchess.transport.api;

import dev.rafex.jchess.domain.model.ParticipantType;
import dev.rafex.jchess.domain.model.Side;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record ApiPlayerDto(
        UUID playerId,
        Side side,
        ParticipantType participantType,
        String displayName,
        boolean connected,
        String playerToken
) {
    public Map<String, Object> toMap(boolean includeToken) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("playerId", playerId.toString());
        map.put("side", side.name());
        map.put("participantType", participantType.name());
        map.put("displayName", displayName);
        map.put("connected", connected);
        if (includeToken) {
            map.put("playerToken", playerToken);
        }
        return map;
    }
}

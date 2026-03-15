package dev.rafex.jchess.transport.api;

import dev.rafex.jchess.domain.model.GameSummary;

import java.util.LinkedHashMap;
import java.util.Map;

public record ApiGameSummaryDto(
        String sessionId,
        String status,
        String result,
        String endReason,
        String whitePlayerName,
        String blackPlayerName,
        String turn,
        int moveCount,
        String createdAt,
        String updatedAt
) {
    public static ApiGameSummaryDto from(GameSummary summary) {
        return new ApiGameSummaryDto(
                summary.sessionId().toString(),
                summary.status().name(),
                summary.result().name(),
                summary.endReason().name(),
                summary.whitePlayerName(),
                summary.blackPlayerName(),
                summary.turn().name(),
                summary.moveCount(),
                summary.createdAt().toString(),
                summary.updatedAt().toString()
        );
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("sessionId", sessionId);
        map.put("status", status);
        map.put("result", result);
        map.put("endReason", endReason);
        map.put("whitePlayerName", whitePlayerName);
        map.put("blackPlayerName", blackPlayerName);
        map.put("turn", turn);
        map.put("moveCount", moveCount);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }
}

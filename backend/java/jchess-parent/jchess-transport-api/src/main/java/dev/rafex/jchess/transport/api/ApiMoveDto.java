package dev.rafex.jchess.transport.api;

import dev.rafex.jchess.domain.model.RecordedMove;

import java.util.LinkedHashMap;
import java.util.Map;

public record ApiMoveDto(
        int ply,
        String side,
        String submittedNotation,
        String canonicalNotation,
        String uci,
        String fenBefore,
        String fenAfter,
        String playedAt
) {
    public static ApiMoveDto from(RecordedMove move) {
        return new ApiMoveDto(
                move.ply(),
                move.side().name(),
                move.submittedNotation(),
                move.canonicalNotation(),
                move.uci(),
                move.fenBefore(),
                move.fenAfter(),
                move.playedAt().toString()
        );
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ply", ply);
        map.put("side", side);
        map.put("submittedNotation", submittedNotation);
        map.put("canonicalNotation", canonicalNotation);
        map.put("uci", uci);
        map.put("fenBefore", fenBefore);
        map.put("fenAfter", fenAfter);
        map.put("playedAt", playedAt);
        return map;
    }
}

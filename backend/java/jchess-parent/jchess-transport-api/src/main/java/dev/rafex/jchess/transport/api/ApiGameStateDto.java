package dev.rafex.jchess.transport.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ApiGameStateDto(
        String sessionId,
        String status,
        String result,
        String endReason,
        String turn,
        String humanSide,
        String machineMode,
        String machineLevel,
        String timeControl,
        long whiteClockMs,
        long blackClockMs,
        String fen,
        long version,
        String createdAt,
        String updatedAt,
        List<ApiPlayerDto> players,
        List<ApiMoveDto> moves,
        List<String> legalMovesEnglish,
        List<String> legalMovesSpanish,
        List<String> legalMovesUci,
        String boardAscii,
        String pgn
) {
    public Map<String, Object> toMap(boolean includeTokens) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("sessionId", sessionId);
        map.put("status", status);
        map.put("result", result);
        map.put("endReason", endReason);
        map.put("turn", turn);
        map.put("humanSide", humanSide);
        map.put("machineMode", machineMode);
        map.put("machineLevel", machineLevel);
        map.put("timeControl", timeControl);
        map.put("whiteClockMs", whiteClockMs);
        map.put("blackClockMs", blackClockMs);
        map.put("fen", fen);
        map.put("version", version);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        map.put("players", players.stream().map(player -> player.toMap(includeTokens)).toList());
        map.put("moves", moves.stream().map(ApiMoveDto::toMap).toList());
        map.put("legalMovesEnglish", legalMovesEnglish);
        map.put("legalMovesSpanish", legalMovesSpanish);
        map.put("legalMovesUci", legalMovesUci);
        map.put("boardAscii", boardAscii);
        map.put("pgn", pgn);
        return map;
    }
}

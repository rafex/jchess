package dev.rafex.jchess.transport.api;

import java.util.LinkedHashMap;
import java.util.Map;

public record ApiSessionAccessDto(
        ApiGameStateDto game,
        ApiPlayerDto requester
) {
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("game", game.toMap(false));
        map.put("requester", requester == null ? null : requester.toMap(true));
        return map;
    }
}

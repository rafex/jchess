package dev.rafex.jchess.transport.api;

import java.util.LinkedHashMap;
import java.util.Map;

public record ApiThemeDto(String name, String description) {
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("description", description);
        return map;
    }
}

package dev.rafex.jchess.transport.api;

import java.util.LinkedHashMap;
import java.util.Map;

public record ApiErrorDto(int status, String code, String message) {
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", status);
        map.put("code", code);
        map.put("message", message);
        return map;
    }
}

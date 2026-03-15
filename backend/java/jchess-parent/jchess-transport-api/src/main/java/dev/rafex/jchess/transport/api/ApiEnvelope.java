package dev.rafex.jchess.transport.api;

import java.util.LinkedHashMap;
import java.util.Map;

public record ApiEnvelope(
        int v,
        String type,
        Object data,
        ApiErrorDto error
) {
    public static ApiEnvelope ok(String type, Object data) {
        return new ApiEnvelope(1, type, data, null);
    }

    public static ApiEnvelope error(String type, int status, String code, String message) {
        return new ApiEnvelope(1, type, null, new ApiErrorDto(status, code, message));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("v", v);
        map.put("type", type);
        if (data != null) {
            map.put("data", data);
        }
        if (error != null) {
            map.put("error", error.toMap());
        }
        return map;
    }
}

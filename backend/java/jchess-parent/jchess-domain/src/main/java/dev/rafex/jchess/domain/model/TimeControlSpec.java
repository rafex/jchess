package dev.rafex.jchess.domain.model;

public record TimeControlSpec(int minutes, int incrementSeconds, long initialMs) {
    public static TimeControlSpec parse(String value) {
        String normalized = value == null || value.isBlank() ? "5+0" : value.trim();
        String[] parts = normalized.split("\\+");
        int minutes = parseInt(parts, 0, 5);
        int increment = parseInt(parts, 1, 0);
        return new TimeControlSpec(minutes, increment, minutes * 60_000L);
    }

    private static int parseInt(String[] parts, int index, int fallback) {
        if (index >= parts.length || parts[index].isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(parts[index]);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}

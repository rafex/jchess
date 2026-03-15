package dev.rafex.jchess.core.engine;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BoardThemeParser {
    private BoardThemeParser() {
    }

    public static BoardTheme parse(String text) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String rawLine : text.split("\\R")) {
            String line = rawLine.trim();
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            int separator = line.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            values.put(line.substring(0, separator).trim(), line.substring(separator + 1));
        }

        Map<String, String> tokens = new HashMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getKey().startsWith("token.")) {
                tokens.put(entry.getKey().substring("token.".length()), decode(entry.getValue()));
            }
        }

        return new BoardTheme(
                required(values, "name").trim().toLowerCase(),
                required(values, "description").trim(),
                Integer.parseInt(values.getOrDefault("cell_width", "2").trim()),
                decode(values.getOrDefault("border.top_left", "╔")),
                decode(values.getOrDefault("border.top_right", "╗")),
                decode(values.getOrDefault("border.bottom_left", "╚")),
                decode(values.getOrDefault("border.bottom_right", "╝")),
                decode(values.getOrDefault("border.horizontal", "═")),
                decode(values.getOrDefault("border.vertical", "║")),
                decode(values.getOrDefault("border.join_top", "╤")),
                decode(values.getOrDefault("border.join_middle", "┼")),
                decode(values.getOrDefault("border.join_bottom", "╧")),
                decode(values.getOrDefault("border.left_join", "╟")),
                decode(values.getOrDefault("border.right_join", "╢")),
                ansi(values.get("color.frame")),
                ansi(values.get("color.files")),
                ansiBackground(values.get("color.light_bg")),
                ansiBackground(values.get("color.dark_bg")),
                ansi(values.get("color.white_piece")),
                ansi(values.get("color.black_piece")),
                decode(values.getOrDefault("token.empty", " ")),
                tokens
        );
    }

    private static String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing key in board theme: " + key);
        }
        return value;
    }

    private static String ansi(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("none")) {
            return "";
        }
        return "\u001B[" + value.trim() + "m";
    }

    private static String ansiBackground(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("none")) {
            return "";
        }
        return "\u001B[48;5;" + value.trim() + "m";
    }

    private static String decode(String value) {
        return value
                .replace("\\u2500", "─")
                .replace("\\u2502", "│")
                .replace("\\u250c", "┌")
                .replace("\\u2510", "┐")
                .replace("\\u2514", "└")
                .replace("\\u2518", "┘")
                .replace("\\u251c", "├")
                .replace("\\u2524", "┤")
                .replace("\\u252c", "┬")
                .replace("\\u2534", "┴")
                .replace("\\u253c", "┼")
                .replace("\\n", "\n");
    }
}

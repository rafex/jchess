package dev.rafex.jchess.domain.model;

public enum LlmProvider {
    DEEPSEEK,
    GROQ;

    public static LlmProvider fromCliValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return switch (value.trim().toLowerCase()) {
            case "deepseek" -> DEEPSEEK;
            case "groq" -> GROQ;
            default -> throw new IllegalArgumentException("unsupported llm provider: " + value);
        };
    }
}

package dev.rafex.jchess.domain.model;

import java.time.Duration;

public enum MachineLevel {
    EASY(2, Duration.ofMillis(400), Duration.ofSeconds(1)),
    MEDIUM(3, Duration.ofMillis(1200), Duration.ofSeconds(3)),
    HARD(4, Duration.ofMillis(2500), Duration.ofSeconds(6)),
    ADVANCED(4, Duration.ofSeconds(5), Duration.ofSeconds(10)),
    MASTER(5, Duration.ofSeconds(8), Duration.ofSeconds(15));

    private final int depth;
    private final Duration searchBudget;
    private final Duration targetThinkTime;

    MachineLevel(int depth, Duration searchBudget, Duration targetThinkTime) {
        this.depth = depth;
        this.searchBudget = searchBudget;
        this.targetThinkTime = targetThinkTime;
    }

    public int depth() {
        return depth;
    }

    public Duration searchBudget() {
        return searchBudget;
    }

    public Duration targetThinkTime() {
        return targetThinkTime;
    }

    public static MachineLevel fromValue(String value) {
        if (value == null || value.isBlank()) {
            return MEDIUM;
        }
        return MachineLevel.valueOf(value.trim().toUpperCase());
    }
}

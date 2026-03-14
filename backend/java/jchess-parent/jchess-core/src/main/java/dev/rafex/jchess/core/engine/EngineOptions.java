package dev.rafex.jchess.core.engine;

import java.time.Duration;

public record EngineOptions(int depth, Duration timeBudget) {
    public EngineOptions {
        if (depth < 1) {
            throw new IllegalArgumentException("depth must be >= 1");
        }
        timeBudget = timeBudget == null ? Duration.ofSeconds(2) : timeBudget;
    }

    public static EngineOptions defaults() {
        return new EngineOptions(3, Duration.ofSeconds(2));
    }
}

package dev.rafex.jchess.core.engine;

import dev.rafex.jchess.domain.model.GameEndReason;
import dev.rafex.jchess.domain.model.GameResult;
import dev.rafex.jchess.domain.model.GameStatus;

public record GameTermination(GameStatus status, GameResult result, GameEndReason endReason) {
    public static GameTermination inProgress() {
        return new GameTermination(GameStatus.ACTIVE, GameResult.IN_PROGRESS, GameEndReason.NONE);
    }
}

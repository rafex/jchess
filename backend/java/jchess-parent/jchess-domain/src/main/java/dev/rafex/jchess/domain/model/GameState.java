package dev.rafex.jchess.domain.model;

import java.util.List;
import java.util.Objects;

public record GameState(GameSession session, List<RecordedMove> moves) {
    public GameState {
        session = Objects.requireNonNull(session, "session must not be null");
        moves = List.copyOf(Objects.requireNonNull(moves, "moves must not be null"));
    }
}

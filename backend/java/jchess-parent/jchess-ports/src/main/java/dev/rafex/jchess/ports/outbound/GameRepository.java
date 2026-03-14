package dev.rafex.jchess.ports.outbound;

import dev.rafex.jchess.domain.model.GameState;

import java.util.Optional;
import java.util.UUID;

public interface GameRepository {

    void initialize();

    void save(GameState gameState);

    Optional<GameState> findById(UUID sessionId);

    boolean saveIfVersionMatches(GameState gameState, long expectedVersion);
}

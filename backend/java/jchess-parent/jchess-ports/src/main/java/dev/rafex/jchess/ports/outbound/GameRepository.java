package dev.rafex.jchess.ports.outbound;

import dev.rafex.jchess.domain.model.GameState;
import dev.rafex.jchess.domain.model.GameSummary;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameRepository {

    void initialize();

    void save(GameState gameState);

    Optional<GameState> findById(UUID sessionId);

    List<GameSummary> listRecent(int limit);

    boolean saveIfVersionMatches(GameState gameState, long expectedVersion);
}

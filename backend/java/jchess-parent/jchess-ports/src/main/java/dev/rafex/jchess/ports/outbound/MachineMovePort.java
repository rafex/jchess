package dev.rafex.jchess.ports.outbound;

import dev.rafex.jchess.domain.model.GameState;
import dev.rafex.jchess.domain.model.LlmProvider;

import java.util.List;
import java.util.Optional;

public interface MachineMovePort {

    Optional<String> suggestMove(GameState gameState, List<String> legalMovesEnglish, LlmProvider provider);
}

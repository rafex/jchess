package dev.rafex.jchess.ports.inbound;

import dev.rafex.jchess.domain.model.GameSnapshot;
import dev.rafex.jchess.domain.model.GameStartRequest;
import dev.rafex.jchess.domain.model.Side;

import java.util.UUID;

public interface GameSessionUseCase {

    GameSnapshot startGame(GameStartRequest request);

    GameSnapshot loadGame(UUID sessionId);

    GameSnapshot submitMove(UUID sessionId, String notation);

    GameSnapshot undoLastMove(UUID sessionId);

    GameSnapshot resignGame(UUID sessionId, Side side);

    String exportPgn(UUID sessionId);
}

package dev.rafex.jchess.ports.inbound;

import dev.rafex.jchess.domain.model.GameSnapshot;
import dev.rafex.jchess.domain.model.GameSummary;
import dev.rafex.jchess.domain.model.GameSessionAccess;
import dev.rafex.jchess.domain.model.GameStartRequest;
import dev.rafex.jchess.domain.model.MoveRequest;
import dev.rafex.jchess.domain.model.Side;

import java.util.UUID;
import java.util.List;

public interface GameSessionUseCase {

    GameSnapshot startGame(GameStartRequest request);

    GameSessionAccess startGameAccess(GameStartRequest request);

    GameSnapshot loadGame(UUID sessionId);

    List<GameSummary> listGames(int limit);

    GameSessionAccess joinGame(UUID sessionId, String playerToken);

    GameSnapshot submitMove(UUID sessionId, String notation);

    GameSnapshot submitMove(UUID sessionId, MoveRequest moveRequest);

    GameSnapshot undoLastMove(UUID sessionId);

    GameSnapshot resignGame(UUID sessionId, Side side);

    String exportPgn(UUID sessionId);
}

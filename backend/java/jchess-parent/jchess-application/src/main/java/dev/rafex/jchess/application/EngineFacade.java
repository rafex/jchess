package dev.rafex.jchess.application;

import dev.rafex.jchess.domain.model.EngineInfo;
import dev.rafex.jchess.domain.model.GameSnapshot;
import dev.rafex.jchess.domain.model.GameStartRequest;
import dev.rafex.jchess.domain.model.Move;
import dev.rafex.jchess.domain.model.Position;
import dev.rafex.jchess.domain.model.Side;
import dev.rafex.jchess.ports.inbound.EngineQueryUseCase;
import dev.rafex.jchess.ports.inbound.GameSessionUseCase;
import dev.rafex.jchess.ports.inbound.MoveCalculationUseCase;

import java.util.UUID;

public interface EngineFacade extends EngineQueryUseCase, MoveCalculationUseCase, GameSessionUseCase {

    @Override
    EngineInfo engineInfo();

    @Override
    Position initialPosition();

    @Override
    Move chooseMove(Position position);

    @Override
    GameSnapshot startGame(GameStartRequest request);

    @Override
    GameSnapshot loadGame(UUID sessionId);

    @Override
    GameSnapshot submitMove(UUID sessionId, String notation);

    @Override
    GameSnapshot undoLastMove(UUID sessionId);

    @Override
    GameSnapshot resignGame(UUID sessionId, Side side);

    @Override
    String exportPgn(UUID sessionId);
}

package dev.rafex.jchess.ports.inbound;

import dev.rafex.jchess.domain.model.Move;
import dev.rafex.jchess.domain.model.Position;

import java.util.List;

public interface MoveCalculationUseCase {

    Move chooseMove(Position position);

    List<Move> legalMoves(Position position);
}

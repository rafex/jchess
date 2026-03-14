package dev.rafex.jchess.core.engine;

import dev.rafex.jchess.domain.model.Move;
import dev.rafex.jchess.domain.model.Position;

import java.util.List;

public interface LegalMoveGenerator {

    List<Move> generateLegalMoves(Position position);
}

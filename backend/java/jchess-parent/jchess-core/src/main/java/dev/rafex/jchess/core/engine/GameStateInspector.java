package dev.rafex.jchess.core.engine;

import dev.rafex.jchess.domain.model.GameEndReason;
import dev.rafex.jchess.domain.model.GameResult;
import dev.rafex.jchess.domain.model.Move;
import dev.rafex.jchess.domain.model.Position;
import dev.rafex.jchess.domain.model.RecordedMove;
import dev.rafex.jchess.domain.model.Side;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GameStateInspector {
    private final DefaultLegalMoveGenerator legalMoveGenerator = new DefaultLegalMoveGenerator();
    private final PositionAnalyzer positionAnalyzer = new PositionAnalyzer();

    public GameTermination inspect(Position position, List<RecordedMove> moves) {
        if (position.halfmoveClock() >= 100) {
            return new GameTermination(dev.rafex.jchess.domain.model.GameStatus.FINISHED, GameResult.DRAW, GameEndReason.FIFTY_MOVE_RULE);
        }
        if (isThreefoldRepetition(position, moves)) {
            return new GameTermination(dev.rafex.jchess.domain.model.GameStatus.FINISHED, GameResult.DRAW, GameEndReason.THREEFOLD_REPETITION);
        }

        List<Move> legalMoves = legalMoveGenerator.generateLegalMoves(position);
        if (!legalMoves.isEmpty()) {
            return GameTermination.inProgress();
        }

        if (positionAnalyzer.isKingInCheck(position, position.sideToMove())) {
            return new GameTermination(
                    dev.rafex.jchess.domain.model.GameStatus.FINISHED,
                    position.sideToMove() == Side.WHITE ? GameResult.BLACK_WIN : GameResult.WHITE_WIN,
                    GameEndReason.CHECKMATE
            );
        }

        return new GameTermination(dev.rafex.jchess.domain.model.GameStatus.FINISHED, GameResult.DRAW, GameEndReason.STALEMATE);
    }

    private boolean isThreefoldRepetition(Position current, List<RecordedMove> moves) {
        Map<String, Integer> counts = new HashMap<>();
        counts.merge(normalizeFen(current.toFen()), 1, Integer::sum);
        for (RecordedMove move : moves) {
            counts.merge(normalizeFen(move.fenAfter()), 1, Integer::sum);
        }
        return counts.values().stream().anyMatch(count -> count >= 3);
    }

    private String normalizeFen(String fen) {
        String[] fields = fen.split(" ");
        return String.join(" ", fields[0], fields[1], fields[2], fields[3]);
    }
}

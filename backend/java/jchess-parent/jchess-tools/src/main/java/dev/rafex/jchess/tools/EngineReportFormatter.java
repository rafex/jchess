package dev.rafex.jchess.tools;

import dev.rafex.jchess.application.EngineFacade;
import dev.rafex.jchess.core.engine.FenCodec;
import dev.rafex.jchess.domain.model.Move;

public final class EngineReportFormatter {
    private EngineReportFormatter() {
    }

    public static String formatSummary(EngineFacade engineFacade) {
        var info = engineFacade.engineInfo();
        var position = engineFacade.initialPosition();
        var legalMoves = engineFacade.legalMoves(position);
        Move bestMove = engineFacade.chooseMove(position);

        return """
                Engine: %s
                Version: %s
                Description: %s
                Initial FEN: %s
                Side to move: %s
                Legal moves: %d
                Best move: %s
                """.formatted(
                info.name(),
                info.version(),
                info.description(),
                FenCodec.toFen(position),
                position.sideToMove(),
                legalMoves.size(),
                bestMove.uci()
        );
    }
}

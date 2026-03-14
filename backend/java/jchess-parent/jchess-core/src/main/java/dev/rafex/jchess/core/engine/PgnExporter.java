package dev.rafex.jchess.core.engine;

import dev.rafex.jchess.domain.model.GameResult;
import dev.rafex.jchess.domain.model.GameState;
import dev.rafex.jchess.domain.model.RecordedMove;

import java.util.ArrayList;
import java.util.List;

public final class PgnExporter {
    public String export(GameState gameState) {
        List<String> plies = new ArrayList<>();
        for (RecordedMove move : gameState.moves()) {
            if (move.ply() % 2 == 1) {
                plies.add(((move.ply() + 1) / 2) + ". " + move.canonicalNotation());
            } else {
                int lastIndex = plies.size() - 1;
                plies.set(lastIndex, plies.get(lastIndex) + " " + move.canonicalNotation());
            }
        }

        return """
                [Event "jchess session"]
                [Site "local"]
                [White "%s"]
                [Black "%s"]
                [Result "%s"]

                %s
                """.formatted(
                gameState.session().whitePlayerName(),
                gameState.session().blackPlayerName(),
                pgnResult(gameState.session().result()),
                String.join(" ", plies)
        ).trim();
    }

    private String pgnResult(GameResult result) {
        return switch (result) {
            case WHITE_WIN -> "1-0";
            case BLACK_WIN -> "0-1";
            case DRAW -> "1/2-1/2";
            case IN_PROGRESS -> "*";
        };
    }
}

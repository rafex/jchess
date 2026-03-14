package dev.rafex.jchess.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record GameSnapshot(
        UUID sessionId,
        GameStatus status,
        GameResult result,
        GameEndReason endReason,
        Position position,
        Side humanSide,
        ParticipantType whiteParticipant,
        ParticipantType blackParticipant,
        String whitePlayerName,
        String blackPlayerName,
        long version,
        List<RecordedMove> moves,
        List<String> legalMovesEnglish,
        List<String> legalMovesSpanish,
        String boardAscii,
        String pgn
) {
    public GameSnapshot {
        sessionId = Objects.requireNonNull(sessionId, "sessionId must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
        result = Objects.requireNonNull(result, "result must not be null");
        endReason = Objects.requireNonNull(endReason, "endReason must not be null");
        position = Objects.requireNonNull(position, "position must not be null");
        whiteParticipant = Objects.requireNonNull(whiteParticipant, "whiteParticipant must not be null");
        blackParticipant = Objects.requireNonNull(blackParticipant, "blackParticipant must not be null");
        whitePlayerName = Objects.requireNonNull(whitePlayerName, "whitePlayerName must not be null");
        blackPlayerName = Objects.requireNonNull(blackPlayerName, "blackPlayerName must not be null");
        moves = List.copyOf(Objects.requireNonNull(moves, "moves must not be null"));
        legalMovesEnglish = List.copyOf(Objects.requireNonNull(legalMovesEnglish, "legalMovesEnglish must not be null"));
        legalMovesSpanish = List.copyOf(Objects.requireNonNull(legalMovesSpanish, "legalMovesSpanish must not be null"));
        boardAscii = Objects.requireNonNull(boardAscii, "boardAscii must not be null");
        pgn = Objects.requireNonNull(pgn, "pgn must not be null");
    }
}

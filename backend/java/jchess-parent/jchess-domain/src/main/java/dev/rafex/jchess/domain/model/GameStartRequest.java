package dev.rafex.jchess.domain.model;

public record GameStartRequest(
        Side requestedHumanSide,
        ParticipantType opponentType,
        LlmProvider llmProvider,
        String whitePlayerName,
        String blackPlayerName
) {
    public GameStartRequest {
        if (opponentType == null) {
            opponentType = ParticipantType.HUMAN;
        }
        whitePlayerName = whitePlayerName == null || whitePlayerName.isBlank() ? "white" : whitePlayerName;
        blackPlayerName = blackPlayerName == null || blackPlayerName.isBlank() ? "black" : blackPlayerName;
    }
}

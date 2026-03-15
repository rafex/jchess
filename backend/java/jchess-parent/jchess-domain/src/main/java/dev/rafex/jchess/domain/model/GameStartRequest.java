package dev.rafex.jchess.domain.model;

public record GameStartRequest(
        Side requestedHumanSide,
        ParticipantType opponentType,
        LlmProvider llmProvider,
        String timeControl,
        String whitePlayerName,
        String blackPlayerName
) {
    public GameStartRequest {
        if (opponentType == null) {
            opponentType = ParticipantType.HUMAN;
        }
        timeControl = timeControl == null || timeControl.isBlank() ? "5+0" : timeControl;
        whitePlayerName = whitePlayerName == null || whitePlayerName.isBlank() ? "white" : whitePlayerName;
        blackPlayerName = blackPlayerName == null || blackPlayerName.isBlank() ? "black" : blackPlayerName;
    }
}

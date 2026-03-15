package dev.rafex.jchess.domain.model;

public record GameStartRequest(
        Side requestedHumanSide,
        ParticipantType opponentType,
        LlmProvider llmProvider,
        MachineGameMode machineMode,
        MachineLevel machineLevel,
        String timeControl,
        String whitePlayerName,
        String blackPlayerName
) {
    public GameStartRequest {
        if (opponentType == null) {
            opponentType = ParticipantType.HUMAN;
        }
        machineMode = machineMode == null ? MachineGameMode.CASUAL : machineMode;
        machineLevel = machineLevel == null ? MachineLevel.MEDIUM : machineLevel;
        timeControl = timeControl == null || timeControl.isBlank() ? "5+0" : timeControl;
        whitePlayerName = whitePlayerName == null || whitePlayerName.isBlank() ? "white" : whitePlayerName;
        blackPlayerName = blackPlayerName == null || blackPlayerName.isBlank() ? "black" : blackPlayerName;
    }
}

package dev.rafex.jchess.transport.api;

import dev.rafex.jchess.core.engine.BoardTheme;
import dev.rafex.jchess.domain.model.GamePlayerAccess;
import dev.rafex.jchess.domain.model.GameSessionAccess;
import dev.rafex.jchess.domain.model.GameSnapshot;
import dev.rafex.jchess.domain.model.Side;

import java.util.List;

public final class ApiPresenter {

    public ApiGameStateDto game(GameSnapshot snapshot, ConnectionStateView connectionStateView) {
        return new ApiGameStateDto(
                snapshot.sessionId().toString(),
                snapshot.status().name(),
                snapshot.result().name(),
                snapshot.endReason().name(),
                snapshot.position().sideToMove().name(),
                snapshot.humanSide() == null ? null : snapshot.humanSide().name(),
                snapshot.machineMode(),
                snapshot.machineLevel(),
                snapshot.timeControl(),
                snapshot.whiteClockMs(),
                snapshot.blackClockMs(),
                snapshot.position().toFen(),
                snapshot.version(),
                snapshot.createdAt().toString(),
                snapshot.updatedAt().toString(),
                List.of(
                        new ApiPlayerDto(snapshot.whitePlayerId(), Side.WHITE, snapshot.whiteParticipant(), snapshot.whitePlayerName(), connectionStateView.whiteConnected(), null),
                        new ApiPlayerDto(snapshot.blackPlayerId(), Side.BLACK, snapshot.blackParticipant(), snapshot.blackPlayerName(), connectionStateView.blackConnected(), null)
                ),
                snapshot.moves().stream().map(ApiMoveDto::from).toList(),
                snapshot.legalMovesEnglish(),
                snapshot.legalMovesSpanish(),
                snapshot.legalMovesUci(),
                snapshot.boardAscii(),
                snapshot.pgn()
        );
    }

    public ApiSessionAccessDto access(GameSessionAccess access, ConnectionStateView connectionStateView) {
        ApiGameStateDto snapshot = game(access.snapshot(), connectionStateView);
        ApiGameStateDto game = new ApiGameStateDto(
                snapshot.sessionId(),
                snapshot.status(),
                snapshot.result(),
                snapshot.endReason(),
                snapshot.turn(),
                snapshot.humanSide(),
                snapshot.machineMode(),
                snapshot.machineLevel(),
                snapshot.timeControl(),
                snapshot.whiteClockMs(),
                snapshot.blackClockMs(),
                snapshot.fen(),
                snapshot.version(),
                snapshot.createdAt(),
                snapshot.updatedAt(),
                List.of(
                        player(access.whitePlayer(), connectionStateView),
                        player(access.blackPlayer(), connectionStateView)
                ),
                snapshot.moves(),
                snapshot.legalMovesEnglish(),
                snapshot.legalMovesSpanish(),
                snapshot.legalMovesUci(),
                snapshot.boardAscii(),
                snapshot.pgn()
        );
        ApiPlayerDto requester = access.requester() == null ? null : player(access.requester(), connectionStateView);
        return new ApiSessionAccessDto(game, requester);
    }

    public List<ApiThemeDto> themes(List<BoardTheme> themes) {
        return themes.stream().map(theme -> new ApiThemeDto(theme.name(), theme.description())).toList();
    }

    public List<ApiGameSummaryDto> summaries(List<dev.rafex.jchess.domain.model.GameSummary> games) {
        return games.stream().map(ApiGameSummaryDto::from).toList();
    }

    private ApiPlayerDto player(GamePlayerAccess access, ConnectionStateView connectionStateView) {
        boolean connected = access.side() == Side.WHITE ? connectionStateView.whiteConnected() : connectionStateView.blackConnected();
        return new ApiPlayerDto(access.playerId(), access.side(), access.participantType(), access.displayName(), connected, access.playerToken());
    }
}

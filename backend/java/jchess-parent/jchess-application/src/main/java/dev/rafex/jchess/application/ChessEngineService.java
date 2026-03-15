package dev.rafex.jchess.application;

import dev.rafex.jchess.core.engine.BoardAsciiRenderer;
import dev.rafex.jchess.core.engine.DefaultLegalMoveGenerator;
import dev.rafex.jchess.core.engine.EngineOptions;
import dev.rafex.jchess.core.engine.FenCodec;
import dev.rafex.jchess.core.engine.GameStateInspector;
import dev.rafex.jchess.core.engine.GameTermination;
import dev.rafex.jchess.core.engine.LegalMoveGenerator;
import dev.rafex.jchess.core.engine.MoveNotationService;
import dev.rafex.jchess.core.engine.PgnExporter;
import dev.rafex.jchess.core.engine.PositionUpdater;
import dev.rafex.jchess.core.engine.SearchEngine;
import dev.rafex.jchess.domain.model.EngineInfo;
import dev.rafex.jchess.domain.model.GameEndReason;
import dev.rafex.jchess.domain.model.GamePlayerAccess;
import dev.rafex.jchess.domain.model.GameResult;
import dev.rafex.jchess.domain.model.GameSession;
import dev.rafex.jchess.domain.model.GameSessionAccess;
import dev.rafex.jchess.domain.model.GameSnapshot;
import dev.rafex.jchess.domain.model.GameStartRequest;
import dev.rafex.jchess.domain.model.GameState;
import dev.rafex.jchess.domain.model.GameStatus;
import dev.rafex.jchess.domain.model.LlmProvider;
import dev.rafex.jchess.domain.model.Move;
import dev.rafex.jchess.domain.model.MoveRequest;
import dev.rafex.jchess.domain.model.NotationLanguage;
import dev.rafex.jchess.domain.model.ParticipantType;
import dev.rafex.jchess.domain.model.PieceType;
import dev.rafex.jchess.domain.model.Position;
import dev.rafex.jchess.domain.model.RecordedMove;
import dev.rafex.jchess.domain.model.Side;
import dev.rafex.jchess.domain.model.Square;
import dev.rafex.jchess.ports.outbound.EngineTelemetry;
import dev.rafex.jchess.ports.outbound.GameRepository;
import dev.rafex.jchess.ports.outbound.MachineMovePort;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class ChessEngineService implements EngineFacade {
    private final EngineTelemetry telemetry;
    private final LegalMoveGenerator legalMoveGenerator;
    private final MoveNotationService moveNotationService;
    private final GameRepository gameRepository;
    private final MachineMovePort machineMovePort;
    private final SearchEngine searchEngine;
    private final PositionUpdater positionUpdater;
    private final GameStateInspector gameStateInspector;
    private final BoardAsciiRenderer boardAsciiRenderer;
    private final PgnExporter pgnExporter;
    private final EngineOptions engineOptions;

    public ChessEngineService(GameRepository gameRepository, EngineTelemetry telemetry, MachineMovePort machineMovePort) {
        this(
                telemetry,
                new DefaultLegalMoveGenerator(),
                new MoveNotationService(),
                gameRepository,
                machineMovePort,
                new SearchEngine(),
                new PositionUpdater(),
                new GameStateInspector(),
                new BoardAsciiRenderer(),
                new PgnExporter(),
                EngineOptions.defaults()
        );
    }

    public ChessEngineService(
            EngineTelemetry telemetry,
            LegalMoveGenerator legalMoveGenerator,
            MoveNotationService moveNotationService,
            GameRepository gameRepository,
            MachineMovePort machineMovePort,
            SearchEngine searchEngine,
            PositionUpdater positionUpdater,
            GameStateInspector gameStateInspector,
            BoardAsciiRenderer boardAsciiRenderer,
            PgnExporter pgnExporter,
            EngineOptions engineOptions
    ) {
        this.telemetry = telemetry;
        this.legalMoveGenerator = legalMoveGenerator;
        this.moveNotationService = moveNotationService;
        this.gameRepository = gameRepository;
        this.machineMovePort = machineMovePort;
        this.searchEngine = searchEngine;
        this.positionUpdater = positionUpdater;
        this.gameStateInspector = gameStateInspector;
        this.boardAsciiRenderer = boardAsciiRenderer;
        this.pgnExporter = pgnExporter;
        this.engineOptions = engineOptions;
    }

    @Override
    public EngineInfo engineInfo() {
        return new EngineInfo(
                "jchess",
                "0.2.0-SNAPSHOT",
                "Framework-free Java 21 chess engine with alpha-beta search, SQLite sessions, CLI workflows and WebSocket transport."
        );
    }

    @Override
    public Position initialPosition() {
        return Position.initial();
    }

    @Override
    public Move chooseMove(Position position) {
        Move selected = searchEngine.chooseMove(position, engineOptions);
        telemetry.record("move.selected", selected.uci());
        return selected;
    }

    @Override
    public List<Move> legalMoves(Position position) {
        telemetry.record("move.requested", FenCodec.toFen(position));
        return legalMoveGenerator.generateLegalMoves(position);
    }

    @Override
    public GameSnapshot startGame(GameStartRequest request) {
        return startGameAccess(request).snapshot();
    }

    @Override
    public GameSessionAccess startGameAccess(GameStartRequest request) {
        gameRepository.initialize();
        Side humanSide = request.requestedHumanSide() == null
                ? (ThreadLocalRandom.current().nextBoolean() ? Side.WHITE : Side.BLACK)
                : request.requestedHumanSide();

        ParticipantType opponentType = request.opponentType() == null ? ParticipantType.HUMAN : request.opponentType();
        ParticipantType whiteParticipant = humanSide == Side.WHITE ? ParticipantType.HUMAN : opponentType;
        ParticipantType blackParticipant = humanSide == Side.BLACK ? ParticipantType.HUMAN : opponentType;

        Instant createdAt = Instant.now();
        GameSession session = new GameSession(
                UUID.randomUUID(),
                whiteParticipant,
                blackParticipant,
                humanSide,
                request.llmProvider(),
                Position.initial(),
                GameStatus.ACTIVE,
                GameResult.IN_PROGRESS,
                GameEndReason.NONE,
                UUID.randomUUID(),
                UUID.randomUUID(),
                request.whitePlayerName(),
                request.blackPlayerName(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                0,
                createdAt,
                createdAt
        );

        GameState preparedState = autoPlayMachineTurnIfNeeded(new GameState(session, List.of()));
        persist(preparedState, -1);
        telemetry.record("session.started", preparedState.session().sessionId().toString());
        return sessionAccess(preparedState, preparedState.session().playerAccess(humanSide));
    }

    @Override
    public GameSnapshot loadGame(UUID sessionId) {
        gameRepository.initialize();
        return snapshot(loadGameState(sessionId));
    }

    @Override
    public GameSessionAccess joinGame(UUID sessionId, String playerToken) {
        gameRepository.initialize();
        GameState state = loadGameState(sessionId);
        Side side = state.session().sideForToken(playerToken);
        return sessionAccess(state, state.session().playerAccess(side));
    }

    @Override
    public GameSnapshot submitMove(UUID sessionId, String notation) {
        gameRepository.initialize();
        GameState current = requireActive(loadGameState(sessionId));
        ensureHumanTurn(current);

        Position position = current.session().currentPosition();
        List<Move> legalMoves = legalMoves(position);
        Move resolved = moveNotationService.parse(position, notation, legalMoves);
        GameState afterHumanMove = appendMove(current, notation, resolved);
        GameState finalState = autoPlayMachineTurnIfNeeded(afterHumanMove);

        persist(finalState, current.session().version());
        telemetry.record("move.saved", sessionId + " :: " + resolved.uci());
        return snapshot(finalState);
    }

    @Override
    public GameSnapshot submitMove(UUID sessionId, MoveRequest moveRequest) {
        gameRepository.initialize();
        GameState current = requireActive(loadGameState(sessionId));
        Side actingSide = current.session().sideForToken(moveRequest.playerToken());
        ensureSideToMove(current, actingSide);

        Position position = current.session().currentPosition();
        List<Move> legalMoves = legalMoves(position);
        Move resolved = resolveStructuredMove(moveRequest, legalMoves);
        GameState nextState = appendMove(current, moveRequest.from() + moveRequest.to(), resolved);
        GameState finalState = autoPlayMachineTurnIfNeeded(nextState);

        persist(finalState, current.session().version());
        telemetry.record("move.saved", sessionId + " :: " + resolved.uci());
        return snapshot(finalState);
    }

    @Override
    public GameSnapshot undoLastMove(UUID sessionId) {
        gameRepository.initialize();
        GameState current = loadGameState(sessionId);
        if (current.moves().isEmpty()) {
            throw new IllegalStateException("no moves to undo");
        }

        int newSize = current.session().machineToMove() && current.moves().size() >= 2 ? current.moves().size() - 2 : current.moves().size() - 1;
        List<RecordedMove> truncated = new ArrayList<>(current.moves().subList(0, Math.max(newSize, 0)));
        Position restoredPosition = truncated.isEmpty()
                ? Position.initial()
                : FenCodec.parse(truncated.getLast().fenAfter());
        GameTermination termination = gameStateInspector.inspect(restoredPosition, truncated);
        GameSession restoredSession = current.session().withState(
                restoredPosition,
                termination.status(),
                termination.result(),
                termination.endReason()
        );
        GameState updated = new GameState(restoredSession, truncated);
        persist(updated, current.session().version());
        return snapshot(updated);
    }

    @Override
    public GameSnapshot resignGame(UUID sessionId, Side side) {
        gameRepository.initialize();
        GameState current = requireActive(loadGameState(sessionId));
        Side resigningSide = side == null ? current.session().preferredHumanSide() : side;
        GameSession resignedSession = current.session().withState(
                current.session().currentPosition(),
                GameStatus.FINISHED,
                resigningSide == Side.WHITE ? GameResult.BLACK_WIN : GameResult.WHITE_WIN,
                GameEndReason.RESIGNATION
        );
        GameState updated = new GameState(resignedSession, current.moves());
        persist(updated, current.session().version());
        return snapshot(updated);
    }

    @Override
    public String exportPgn(UUID sessionId) {
        gameRepository.initialize();
        return pgnExporter.export(loadGameState(sessionId));
    }

    private GameState requireActive(GameState current) {
        if (current.session().status() != GameStatus.ACTIVE) {
            throw new IllegalStateException("game session is not active");
        }
        return current;
    }

    private void ensureHumanTurn(GameState current) {
        Side sideToMove = current.session().currentPosition().sideToMove();
        Side humanSide = current.session().preferredHumanSide();
        if (humanSide != null && sideToMove != humanSide && current.session().participantFor(sideToMove) == ParticipantType.MACHINE) {
            throw new IllegalStateException("it is not your turn; waiting for " + sideToMove);
        }
    }

    private void ensureSideToMove(GameState current, Side actingSide) {
        Side sideToMove = current.session().currentPosition().sideToMove();
        if (sideToMove != actingSide) {
            throw new IllegalStateException("it is not your turn; waiting for " + sideToMove);
        }
        if (current.session().participantFor(actingSide) == ParticipantType.MACHINE) {
            throw new IllegalStateException("machine-controlled side cannot be moved manually");
        }
    }

    private GameState autoPlayMachineTurnIfNeeded(GameState gameState) {
        GameState current = gameState;
        while (current.session().status() == GameStatus.ACTIVE && current.session().machineToMove()) {
            Position position = current.session().currentPosition();
            List<Move> legalMoves = legalMoves(position);
            if (legalMoves.isEmpty()) {
                current = applyTermination(current, gameStateInspector.inspect(position, current.moves()));
                break;
            }

            Move move = selectMachineMove(current, legalMoves);
            String san = moveNotationService.toNotation(position, move, NotationLanguage.ENGLISH, legalMoves);
            current = appendMove(current, san, move);
        }
        return current;
    }

    private Move selectMachineMove(GameState gameState, List<Move> legalMoves) {
        LlmProvider provider = gameState.session().llmProvider();
        Position position = gameState.session().currentPosition();
        List<String> englishNotations = moveNotationService.toNotations(position, legalMoves, NotationLanguage.ENGLISH);

        if (provider != null) {
            Optional<String> suggestion = machineMovePort.suggestMove(gameState, englishNotations, provider);
            if (suggestion.isPresent()) {
                try {
                    return moveNotationService.parse(position, suggestion.get(), legalMoves);
                } catch (RuntimeException ignored) {
                }
            }
        }

        return chooseMove(position);
    }

    private Move resolveStructuredMove(MoveRequest moveRequest, List<Move> legalMoves) {
        Square from = Square.fromAlgebraic(moveRequest.from());
        Square to = Square.fromAlgebraic(moveRequest.to());
        PieceType promotion = moveRequest.promotion();

        return legalMoves.stream()
                .filter(move -> move.from().equals(from))
                .filter(move -> move.to().equals(to))
                .filter(move -> move.promotion() == promotion)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("illegal move: " + moveRequest.from() + moveRequest.to()));
    }

    private GameState appendMove(GameState current, String submittedNotation, Move move) {
        Position before = current.session().currentPosition();
        Position after = positionUpdater.apply(before, move);
        List<Move> currentLegalMoves = legalMoves(before);
        String canonicalNotation = moveNotationService.toNotation(before, move, NotationLanguage.ENGLISH, currentLegalMoves);
        RecordedMove recordedMove = new RecordedMove(
                current.moves().size() + 1,
                before.sideToMove(),
                submittedNotation,
                canonicalNotation,
                move.uci(),
                before.toFen(),
                after.toFen(),
                Instant.now()
        );

        List<RecordedMove> updatedMoves = new ArrayList<>(current.moves());
        updatedMoves.add(recordedMove);
        GameTermination termination = gameStateInspector.inspect(after, updatedMoves);
        GameSession updatedSession = current.session().withState(after, termination.status(), termination.result(), termination.endReason());
        return new GameState(updatedSession, updatedMoves);
    }

    private GameState applyTermination(GameState current, GameTermination termination) {
        return new GameState(
                current.session().withState(
                        current.session().currentPosition(),
                        termination.status(),
                        termination.result(),
                        termination.endReason()
                ),
                current.moves()
        );
    }

    private void persist(GameState gameState, long expectedVersion) {
        boolean saved = expectedVersion < 0
                ? saveWithoutCheck(gameState)
                : gameRepository.saveIfVersionMatches(gameState, expectedVersion);
        if (!saved) {
            throw new IllegalStateException("concurrent update detected for session " + gameState.session().sessionId());
        }
    }

    private boolean saveWithoutCheck(GameState gameState) {
        gameRepository.save(gameState);
        return true;
    }

    private GameState loadGameState(UUID sessionId) {
        return gameRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("session not found: " + sessionId));
    }

    private GameSnapshot snapshot(GameState gameState) {
        Position position = gameState.session().currentPosition();
        List<Move> legalMoves = gameState.session().status() == GameStatus.ACTIVE ? legalMoves(position) : List.of();

        return new GameSnapshot(
                gameState.session().sessionId(),
                gameState.session().status(),
                gameState.session().result(),
                gameState.session().endReason(),
                position,
                gameState.session().preferredHumanSide(),
                gameState.session().whiteParticipant(),
                gameState.session().blackParticipant(),
                gameState.session().whitePlayerId(),
                gameState.session().blackPlayerId(),
                gameState.session().whitePlayerName(),
                gameState.session().blackPlayerName(),
                gameState.session().version(),
                gameState.session().createdAt(),
                gameState.session().updatedAt(),
                gameState.moves(),
                moveNotationService.toNotations(position, legalMoves, NotationLanguage.ENGLISH),
                moveNotationService.toNotations(position, legalMoves, NotationLanguage.SPANISH),
                legalMoves.stream().map(Move::uci).toList(),
                boardAsciiRenderer.render(position),
                pgnExporter.export(gameState)
        );
    }

    private GameSessionAccess sessionAccess(GameState gameState, GamePlayerAccess requester) {
        return new GameSessionAccess(
                snapshot(gameState),
                gameState.session().playerAccess(Side.WHITE),
                gameState.session().playerAccess(Side.BLACK),
                requester
        );
    }
}

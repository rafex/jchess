package dev.rafex.jchess.cli;

import dev.rafex.jchess.adapter.llm.HttpMachineMoveClient;
import dev.rafex.jchess.adapter.sqlite.SqliteGameRepository;
import dev.rafex.jchess.application.ChessEngineService;
import dev.rafex.jchess.application.EngineFacade;
import dev.rafex.jchess.core.engine.BoardAsciiRenderer;
import dev.rafex.jchess.core.engine.BoardTheme;
import dev.rafex.jchess.domain.model.GameSnapshot;
import dev.rafex.jchess.domain.model.GameStartRequest;
import dev.rafex.jchess.domain.model.LlmProvider;
import dev.rafex.jchess.domain.model.ParticipantType;
import dev.rafex.jchess.domain.model.Side;
import dev.rafex.jchess.ports.outbound.EngineTelemetry;
import dev.rafex.jchess.transport.http.JChessHttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class JChessCliApplication {
    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[38;5;45m";
    private static final String GOLD = "\u001B[38;5;220m";
    private static final String ORANGE = "\u001B[38;5;208m";
    private static final String GREEN = "\u001B[38;5;82m";

    private JChessCliApplication() {
    }

    public static void main(String[] args) throws Exception {
        printBanner();
        CliCommand cliCommand = CliCommand.parse(args);
        EngineFacade engineFacade = createEngine(cliCommand.databasePath());
        BoardAsciiRenderer boardRenderer = new BoardAsciiRenderer();

        switch (cliCommand.action()) {
            case HELP -> System.out.println(helpText());
            case LIST_THEMES -> printThemes(boardRenderer, cliCommand.boardTheme());
            case SERVER_START -> new JChessHttpServer(engineFacade, cliCommand.port()).startAndBlock();
            case GAME_NEW -> handleGameNew(engineFacade, boardRenderer, cliCommand);
            case GAME_SHOW -> renderFullSnapshot(engineFacade.loadGame(requireSession(cliCommand)), boardRenderer, cliCommand.boardTheme());
            case GAME_MOVE -> renderFullSnapshot(engineFacade.submitMove(requireSession(cliCommand), requireValue(cliCommand.move(), "move")), boardRenderer, cliCommand.boardTheme());
            case GAME_PLAY -> handleGamePlay(engineFacade, boardRenderer, cliCommand);
            case GAME_UNDO -> renderFullSnapshot(engineFacade.undoLastMove(requireSession(cliCommand)), boardRenderer, cliCommand.boardTheme());
            case GAME_RESIGN -> renderFullSnapshot(engineFacade.resignGame(requireSession(cliCommand), cliCommand.color()), boardRenderer, cliCommand.boardTheme());
            case GAME_PGN -> System.out.println(engineFacade.exportPgn(requireSession(cliCommand)));
        }
    }

    private static void handleGameNew(EngineFacade engineFacade, BoardAsciiRenderer boardRenderer, CliCommand cliCommand) throws IOException {
        GameSnapshot snapshot = engineFacade.startGame(new GameStartRequest(
                cliCommand.color(),
                cliCommand.opponent(),
                cliCommand.llmProvider(),
                "5+0",
                cliCommand.whitePlayerName(),
                cliCommand.blackPlayerName()
        ));
        renderFullSnapshot(snapshot, boardRenderer, cliCommand.boardTheme());
        if (cliCommand.interactive()) {
            runInteractiveLoop(engineFacade, boardRenderer, snapshot.sessionId(), cliCommand.boardTheme());
        }
    }

    private static void handleGamePlay(EngineFacade engineFacade, BoardAsciiRenderer boardRenderer, CliCommand cliCommand) throws IOException {
        UUID sessionId = cliCommand.sessionId();
        if (sessionId == null && cliCommand.startGame()) {
            GameSnapshot snapshot = engineFacade.startGame(new GameStartRequest(
                    cliCommand.color(),
                    cliCommand.opponent(),
                    cliCommand.llmProvider(),
                    "5+0",
                    cliCommand.whitePlayerName(),
                    cliCommand.blackPlayerName()
            ));
            sessionId = snapshot.sessionId();
        }

        require(sessionId != null, "`game play` requires an existing session or `--new`");
        runInteractiveLoop(engineFacade, boardRenderer, sessionId, cliCommand.boardTheme());
    }

    private static EngineFacade createEngine(Path databasePath) {
        SqliteGameRepository repository = new SqliteGameRepository(databasePath);
        repository.initialize();
        return new ChessEngineService(repository, new ConsoleTelemetry(), new HttpMachineMoveClient());
    }

    private static void runInteractiveLoop(EngineFacade engineFacade, BoardAsciiRenderer boardRenderer, UUID sessionId, String initialTheme) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String currentTheme = initialTheme;
        System.out.println("Interactive commands: help, status, board, moves, fen, theme, themes, undo, resign, pgn, exit");

        while (true) {
            GameSnapshot current = engineFacade.loadGame(sessionId);
            renderFullSnapshot(current, boardRenderer, currentTheme);
            if (current.status().name().equals("FINISHED")) {
                return;
            }

            System.out.print("jchess> ");
            String line = reader.readLine();
            if (line == null) {
                return;
            }

            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            try {
                String normalized = trimmed.toLowerCase(Locale.ROOT);
                if (normalized.equals("exit") || normalized.equals("quit")) {
                    return;
                }
                if (normalized.equals("help")) {
                    System.out.println(interactiveHelpText());
                    continue;
                }
                if (normalized.equals("status")) {
                    renderCompact(engineFacade.loadGame(sessionId));
                    continue;
                }
                if (normalized.equals("board")) {
                    System.out.println(boardRenderer.render(engineFacade.loadGame(sessionId).position(), currentTheme));
                    continue;
                }
                if (normalized.equals("moves")) {
                    System.out.println(String.join(", ", engineFacade.loadGame(sessionId).legalMovesEnglish()));
                    continue;
                }
                if (normalized.equals("fen")) {
                    System.out.println(engineFacade.loadGame(sessionId).position().toFen());
                    continue;
                }
                if (normalized.equals("themes")) {
                    printThemes(boardRenderer, currentTheme);
                    continue;
                }
                if (normalized.equals("theme")) {
                    System.out.println("Current board theme: " + currentTheme);
                    continue;
                }
                if (normalized.startsWith("theme ")) {
                    String requestedTheme = trimmed.substring(6).trim();
                    require(!requestedTheme.isBlank(), "missing board theme name");
                    boardRenderer.render(current.position(), requestedTheme);
                    currentTheme = requestedTheme;
                    System.out.println("Board theme switched to `" + currentTheme + "`");
                    continue;
                }
                if (normalized.equals("undo")) {
                    renderFullSnapshot(engineFacade.undoLastMove(sessionId), boardRenderer, currentTheme);
                    continue;
                }
                if (normalized.equals("resign")) {
                    renderFullSnapshot(engineFacade.resignGame(sessionId, null), boardRenderer, currentTheme);
                    continue;
                }
                if (normalized.equals("pgn")) {
                    System.out.println(engineFacade.exportPgn(sessionId));
                    continue;
                }

                String move = normalized.startsWith("move ") ? trimmed.substring(5).trim() : trimmed;
                renderFullSnapshot(engineFacade.submitMove(sessionId, move), boardRenderer, currentTheme);
            } catch (RuntimeException ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }
    }

    private static void renderFullSnapshot(GameSnapshot snapshot, BoardAsciiRenderer boardRenderer, String boardTheme) {
        renderCompact(snapshot);
        System.out.println(boardRenderer.render(snapshot.position(), boardTheme));
        System.out.println("Legal moves (EN): " + String.join(", ", snapshot.legalMovesEnglish()));
        System.out.println("Legal moves (ES): " + String.join(", ", snapshot.legalMovesSpanish()));
        if (!snapshot.moves().isEmpty()) {
            var lastMove = snapshot.moves().getLast();
            System.out.println("Last move: " + lastMove.canonicalNotation() + " (" + lastMove.uci() + ")");
        }
        System.out.println();
    }

    private static void renderCompact(GameSnapshot snapshot) {
        System.out.println("Session: " + snapshot.sessionId() + "  Version: " + snapshot.version());
        System.out.println("Status: " + snapshot.status() + "  Result: " + snapshot.result() + "  End: " + snapshot.endReason());
        System.out.println("Players: " + snapshot.whitePlayerName() + " vs " + snapshot.blackPlayerName());
        System.out.println("Turn: " + snapshot.position().sideToMove() + "  Human side: " + snapshot.humanSide());
        System.out.println("FEN: " + snapshot.position().toFen());
        System.out.println("Moves played: " + snapshot.moves().size());
    }

    private static void printThemes(BoardAsciiRenderer boardRenderer, String currentTheme) {
        System.out.println("Available board themes:");
        for (BoardTheme theme : boardRenderer.availableThemes()) {
            String marker = theme.name().equalsIgnoreCase(currentTheme) ? "*" : " ";
            System.out.printf(" %s %-14s %s%n", marker, theme.name(), theme.description());
        }
        System.out.println("Custom themes are loaded from ./boards and ~/.jchess/boards");
    }

    private static UUID requireSession(CliCommand cliCommand) {
        require(cliCommand.sessionId() != null, "this command requires <session> or --session");
        return cliCommand.sessionId();
    }

    private static String requireValue(String value, String label) {
        require(value != null && !value.isBlank(), "missing " + label);
        return value;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String helpText() {
        return """
                Usage:
                  jchess game new [options]
                  jchess game show <session> [--db <path>]
                  jchess game move <session> <move> [--db <path>]
                  jchess game play <session> [--db <path>]
                  jchess game play --new [options]
                  jchess game undo <session> [--db <path>]
                  jchess game resign <session> [--color <white|black>] [--db <path>]
                  jchess game pgn <session> [--db <path>]
                  jchess themes
                  jchess server start [--port <number>] [--db <path>]

                Short aliases:
                  jchess new [options]
                  jchess show <session>
                  jchess move <session> <move>
                  jchess play <session>
                  jchess undo <session>
                  jchess resign <session>
                  jchess pgn <session>
                  jchess themes
                  jchess serve

                Game options:
                  --color <white|black>               Preferred human color
                  --opponent <human|machine>          Opponent type
                  --llm <deepseek|groq>               LLM provider for machine mode
                  --board-theme <name>                Board theme (default: letters)
                  --white-name <name>                 White player display name
                  --black-name <name>                 Black player display name
                  --interactive                       Enter interactive mode after creating a game
                  --new                               Create a new game inside `game play`
                  --db <path>                         SQLite database path (default: ./jchess.db)
                  --port <number>                     Server port (default: 8080)

                Board themes:
                  Use `jchess themes` to list bundled styles.
                  Custom themes are loaded from `./boards` and `~/.jchess/boards`.

                Legacy compatibility:
                  --start-game, --move, --interactive, --undo, --resign, --pgn, --server
                """;
    }

    private static void printBanner() {
        System.out.println(CYAN + """
                  ██╗ ██████╗██╗  ██╗███████╗███████╗███████╗
                  ██║██╔════╝██║  ██║██╔════╝██╔════╝██╔════╝
                  ██║██║     ███████║█████╗  ███████╗███████╗
             ██   ██║██║     ██╔══██║██╔══╝  ╚════██║╚════██║
             ╚█████╔╝╚██████╗██║  ██║███████╗███████║███████║
              ╚════╝  ╚═════╝╚═╝  ╚═╝╚══════╝╚══════╝╚══════╝
                """ + RESET);
        System.out.println(GOLD + "  by rafex" + RESET + "  " + ORANGE + "Created: 2026-03-13" + RESET);
        System.out.println(GREEN + "  Java 21 chess engine • CLI • SQLite • WebSocket" + RESET);
        System.out.println();
    }

    private static String interactiveHelpText() {
        return """
                Interactive commands:
                  move <notation>   Play a move, e.g. `move Nf3` or just `Nf3`
                  status            Show summary
                  board             Show ASCII board
                  moves             Show legal moves
                  fen               Show current FEN
                  theme             Show current board theme
                  theme <name>      Switch board theme
                  themes            List board themes
                  undo              Undo the last ply or exchange
                  resign            Resign the game
                  pgn               Print PGN
                  help              Show this help
                  exit              Leave interactive mode
                """;
    }

    private static final class ConsoleTelemetry implements EngineTelemetry {
        @Override
        public void record(String event, String detail) {
            System.out.printf("[jchess] %s :: %s%n", event, detail);
        }
    }

    private enum Action {
        HELP,
        LIST_THEMES,
        GAME_NEW,
        GAME_SHOW,
        GAME_MOVE,
        GAME_PLAY,
        GAME_UNDO,
        GAME_RESIGN,
        GAME_PGN,
        SERVER_START
    }

    private record CliCommand(
            Action action,
            boolean startGame,
            boolean interactive,
            UUID sessionId,
            String move,
            Side color,
            ParticipantType opponent,
            LlmProvider llmProvider,
            Path databasePath,
            int port,
            String boardTheme,
            String whitePlayerName,
            String blackPlayerName
    ) {
        private static CliCommand parse(String[] args) {
            if (args.length == 0) {
                return legacyFallback(args);
            }

            List<String> tokens = Arrays.asList(args);
            String first = tokens.getFirst();
            if (first.startsWith("--")) {
                return legacyFallback(args);
            }

            return parseSubcommand(tokens);
        }

        private static CliCommand parseSubcommand(List<String> tokens) {
            String first = tokens.getFirst().toLowerCase(Locale.ROOT);
            return switch (first) {
                case "game" -> parseGameSubcommand(tokens.subList(1, tokens.size()));
                case "server" -> parseServerSubcommand(tokens.subList(1, tokens.size()));
                case "new", "show", "move", "play", "undo", "resign", "pgn" -> parseGameSubcommand(tokens);
                case "themes" -> ParsedOptions.parseOptions(tokens.subList(1, tokens.size())).toCommand(Action.LIST_THEMES, false, null, null);
                case "serve" -> parseServerSubcommand(prepend("start", tokens.subList(1, tokens.size())));
                case "help" -> help();
                default -> throw new IllegalArgumentException("unsupported command: " + first);
            };
        }

        private static CliCommand parseGameSubcommand(List<String> rawTokens) {
            List<String> tokens = normalizeAlias(rawTokens);
            String verb = tokens.isEmpty() ? "help" : tokens.getFirst().toLowerCase(Locale.ROOT);
            ParsedOptions parsed = ParsedOptions.parseOptions(tokens.subList(Math.min(1, tokens.size()), tokens.size()));

            return switch (verb) {
                case "new" -> parsed.toCommand(Action.GAME_NEW, true, null, null);
                case "show" -> parsed.toCommand(Action.GAME_SHOW, false, positionalSession(parsed, 0), null);
                case "move" -> parsed.toCommand(Action.GAME_MOVE, false, positionalSession(parsed, 0), positionalValue(parsed, 1, "move"));
                case "play" -> parsed.toCommand(Action.GAME_PLAY, parsed.flags().contains("new"), parsed.optionalSession(0), null);
                case "undo" -> parsed.toCommand(Action.GAME_UNDO, false, positionalSession(parsed, 0), null);
                case "resign" -> parsed.toCommand(Action.GAME_RESIGN, false, positionalSession(parsed, 0), null);
                case "pgn" -> parsed.toCommand(Action.GAME_PGN, false, positionalSession(parsed, 0), null);
                case "themes" -> parsed.toCommand(Action.LIST_THEMES, false, null, null);
                case "help" -> help();
                default -> throw new IllegalArgumentException("unsupported game command: " + verb);
            };
        }

        private static CliCommand parseServerSubcommand(List<String> rawTokens) {
            List<String> tokens = rawTokens.isEmpty() ? List.of("start") : rawTokens;
            String verb = tokens.getFirst().toLowerCase(Locale.ROOT);
            ParsedOptions parsed = ParsedOptions.parseOptions(tokens.subList(Math.min(1, tokens.size()), tokens.size()));
            if (!verb.equals("start")) {
                throw new IllegalArgumentException("unsupported server command: " + verb);
            }
            return parsed.toCommand(Action.SERVER_START, false, null, null);
        }

        private static CliCommand legacyFallback(String[] args) {
            ParsedOptions parsed = ParsedOptions.parseOptions(List.of(args));
            if (parsed.flags().contains("server")) {
                return parsed.toCommand(Action.SERVER_START, false, null, null);
            }
            if (parsed.flags().contains("start-game") || parsed.flags().contains("s")) {
                return parsed.toCommand(Action.GAME_NEW, true, parsed.sessionId(), parsed.values().get("move"));
            }
            if (parsed.flags().contains("interactive")) {
                return parsed.toCommand(Action.GAME_PLAY, false, parsed.sessionId(), null);
            }
            if (parsed.values().containsKey("move")) {
                return parsed.toCommand(Action.GAME_MOVE, false, parsed.sessionId(), parsed.values().get("move"));
            }
            if (parsed.flags().contains("undo")) {
                return parsed.toCommand(Action.GAME_UNDO, false, parsed.sessionId(), null);
            }
            if (parsed.flags().contains("resign")) {
                return parsed.toCommand(Action.GAME_RESIGN, false, parsed.sessionId(), null);
            }
            if (parsed.flags().contains("pgn")) {
                return parsed.toCommand(Action.GAME_PGN, false, parsed.sessionId(), null);
            }
            return help();
        }

        private static CliCommand help() {
            return new CliCommand(
                    Action.HELP,
                    false,
                    false,
                    null,
                    null,
                    null,
                    ParticipantType.HUMAN,
                    null,
                    Path.of("jchess.db"),
                    8080,
                    "letters",
                    "white",
                    "black"
            );
        }

        private static UUID positionalSession(ParsedOptions parsed, int index) {
            UUID explicit = parsed.sessionId();
            if (explicit != null) {
                return explicit;
            }
            String value = positionalValue(parsed, index, "session");
            return UUID.fromString(value);
        }

        private static String positionalValue(ParsedOptions parsed, int index, String label) {
            require(parsed.positionals().size() > index, "missing " + label);
            return parsed.positionals().get(index);
        }

        private static List<String> normalizeAlias(List<String> tokens) {
            if (tokens.isEmpty()) {
                return List.of("help");
            }
            String first = tokens.getFirst().toLowerCase(Locale.ROOT);
            return switch (first) {
                case "show", "move", "play", "undo", "resign", "pgn", "new" -> tokens;
                default -> tokens;
            };
        }
        private static List<String> prepend(String first, List<String> rest) {
            java.util.ArrayList<String> tokens = new java.util.ArrayList<>();
            tokens.add(first);
            tokens.addAll(rest);
            return List.copyOf(tokens);
        }
    }

    private record ParsedOptions(Map<String, String> values, java.util.Set<String> flags, List<String> positionals) {
        private static ParsedOptions parseOptions(List<String> tokens) {
            Map<String, String> values = new HashMap<>();
            java.util.Set<String> flags = new java.util.HashSet<>();
            java.util.ArrayList<String> positionals = new java.util.ArrayList<>();

            for (int index = 0; index < tokens.size(); index++) {
                String token = tokens.get(index);
                if (!token.startsWith("--") && !token.startsWith("-")) {
                    positionals.add(token);
                    continue;
                }

                String normalized = token.startsWith("--") ? token.substring(2) : token.substring(1);
                if (isBooleanFlag(normalized)) {
                    flags.add(normalized);
                    continue;
                }

                require(index + 1 < tokens.size(), "missing value for " + token);
                values.put(normalized, tokens.get(++index));
            }

            return new ParsedOptions(Map.copyOf(values), java.util.Set.copyOf(flags), List.copyOf(positionals));
        }

        private CliCommand toCommand(Action action, boolean startGame, UUID sessionId, String move) {
            return new CliCommand(
                    action,
                    startGame,
                    flags.contains("interactive"),
                    sessionId == null ? sessionId() : sessionId,
                    move,
                    values.containsKey("color") ? Side.valueOf(values.get("color").toUpperCase(Locale.ROOT)) : null,
                    values.containsKey("opponent") ? ParticipantType.valueOf(values.get("opponent").toUpperCase(Locale.ROOT)) : ParticipantType.HUMAN,
                    LlmProvider.fromCliValue(values.get("llm")),
                    Path.of(values.getOrDefault("db", "jchess.db")),
                    Integer.parseInt(values.getOrDefault("port", "8080")),
                    values.getOrDefault("board-theme", "letters"),
                    values.getOrDefault("white-name", "white"),
                    values.getOrDefault("black-name", "black")
            );
        }

        private UUID sessionId() {
            return values.containsKey("session") ? UUID.fromString(values.get("session")) : null;
        }

        private UUID optionalSession(int index) {
            if (sessionId() != null) {
                return sessionId();
            }
            if (positionals.size() <= index) {
                return null;
            }
            return UUID.fromString(positionals.get(index));
        }

        private static boolean isBooleanFlag(String flag) {
            return switch (flag) {
                case "start-game", "s", "interactive", "undo", "resign", "pgn", "server", "new" -> true;
                default -> false;
            };
        }
    }
}

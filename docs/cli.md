# CLI Guide

## Recommended Commands

Create a game:

```bash
java -jar backend/java/jchess-parent/jchess-cli/target/jchess-cli-0.1.0-SNAPSHOT.jar game new --color white
```

Interactive play:

```bash
java -jar backend/java/jchess-parent/jchess-cli/target/jchess-cli-0.1.0-SNAPSHOT.jar game play --new --opponent machine --llm groq
```

Show an existing session:

```bash
java -jar backend/java/jchess-parent/jchess-cli/target/jchess-cli-0.1.0-SNAPSHOT.jar game show <session-id>
```

Submit a move:

```bash
java -jar backend/java/jchess-parent/jchess-cli/target/jchess-cli-0.1.0-SNAPSHOT.jar game move <session-id> Nf3
```

Undo or resign:

```bash
java -jar backend/java/jchess-parent/jchess-cli/target/jchess-cli-0.1.0-SNAPSHOT.jar game undo <session-id>
java -jar backend/java/jchess-parent/jchess-cli/target/jchess-cli-0.1.0-SNAPSHOT.jar game resign <session-id>
```

Export PGN:

```bash
java -jar backend/java/jchess-parent/jchess-cli/target/jchess-cli-0.1.0-SNAPSHOT.jar game pgn <session-id>
```

Start server:

```bash
java -jar backend/java/jchess-parent/jchess-cli/target/jchess-cli-0.1.0-SNAPSHOT.jar server start --port 8080
```

## Legacy Compatibility

The old flag-based interface still works:

- `--start-game`
- `--move`
- `--interactive`
- `--undo`
- `--resign`
- `--pgn`
- `--server`


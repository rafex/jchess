# jchess

`jchess` is a Java 21 chess engine workspace built around a hexagonal architecture, standard Java libraries, SQLite-backed game sessions, a command-line interface, and a Jetty 12 WebSocket transport.

## Repository Layout

- `backend/`: backend entrypoint Makefile and Java workspace.
- `backend/java/`: Java-specific developer documentation and commands.
- `backend/java/jchess-parent/`: Maven multi-module backend.
- `docs/`: project documentation, architecture notes, and CLI/server usage.

## Quick Start

Build the backend:

```bash
make build
```

Run tests:

```bash
make test
```

Start a new game from the CLI:

```bash
make cli-new
```

Run the WebSocket server:

```bash
make server
```

## Main Commands

- `make build`
- `make test`
- `make verify`
- `make cli`
- `make cli-new`
- `make server`

## Documentation

- [Project Overview](./docs/README.md)
- [Architecture](./docs/architecture.md)
- [CLI Guide](./docs/cli.md)
- [Development Guide](./docs/development.md)


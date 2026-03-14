# Architecture

## Summary

`jchess` follows a hexagonal architecture.

## Modules

- `jchess-domain`: immutable chess model, sessions, moves, and game metadata.
- `jchess-ports`: inbound and outbound interfaces.
- `jchess-application`: use-case orchestration.
- `jchess-core`: move generation, notation, state inspection, search, rendering, and PGN export.
- `jchess-adapter-sqlite`: SQLite persistence for sessions and moves.
- `jchess-adapter-llm`: machine-move integration over HTTP.
- `jchess-transport-websocket`: Jetty 12 WebSocket server.
- `jchess-tools`: formatting helpers.
- `jchess-cli`: executable CLI entrypoint.

## Flow

1. The CLI or WebSocket transport calls an inbound use case in `jchess-application`.
2. The application layer coordinates search, notation, persistence, and game-state inspection.
3. Core logic computes legal moves, search decisions, terminal states, and formatting.
4. Adapters persist sessions or reach external providers.

## Design Constraints

- Java 21.
- No frameworks for application/runtime structure.
- Standard library first.
- Ready to evolve toward GraalVM native image.


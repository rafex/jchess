# Machine Levels

This document describes how machine-vs-human play is currently configured in `jchess`.

## Modes

Machine games currently support two modes:

- `CASUAL`
  - no clock
  - intended for relaxed play
  - avoids unfair pressure caused by nearly-instant machine replies
- `COMPETITIVE`
  - backend-authoritative clock
  - backend-controlled target think time for the machine
  - search budget and response target depend on level

## Current Level Configuration

The current source of truth is:
- [MachineLevel.java](/Users/rafex/repository/github/rafex/jchess/backend/java/jchess-parent/jchess-domain/src/main/java/dev/rafex/jchess/domain/model/MachineLevel.java)

Each level currently defines:

- search depth
- search budget
- target think time

Current values:

| Level | Depth | Search Budget | Target Think Time |
|---|---:|---:|---:|
| `EASY` | 2 | 400 ms | 1 s |
| `MEDIUM` | 3 | 1200 ms | 3 s |
| `HARD` | 4 | 2500 ms | 6 s |
| `ADVANCED` | 4 | 5 s | 10 s |
| `MASTER` | 5 | 8 s | 15 s |

## Meaning Of Each Value

### Search Depth

This is the alpha-beta depth used by the local search engine.

Relevant code:
- [SearchEngine.java](/Users/rafex/repository/github/rafex/jchess/backend/java/jchess-parent/jchess-core/src/main/java/dev/rafex/jchess/core/engine/SearchEngine.java)
- [EngineOptions.java](/Users/rafex/repository/github/rafex/jchess/backend/java/jchess-parent/jchess-core/src/main/java/dev/rafex/jchess/core/engine/EngineOptions.java)

### Search Budget

This is the internal budget used by the local search engine while it is evaluating moves.

It limits how long the search may spend before returning a move.

### Target Think Time

This is the user-facing timing target for `COMPETITIVE` machine play.

`jchess` measures how long the machine actually spent producing a move and, if needed, delays the reply so the total visible think time reaches the configured target.

That logic is applied in:
- [ChessEngineService.java](/Users/rafex/repository/github/rafex/jchess/backend/java/jchess-parent/jchess-application/src/main/java/dev/rafex/jchess/application/ChessEngineService.java)

## Backend Clock Ownership

For `COMPETITIVE` machine games, the clock is backend-authoritative:

- the session stores the time control
- the session stores white and black remaining milliseconds
- the session updates the clock when a move is submitted
- timeout can finish the game

Relevant code:
- [GameSession.java](/Users/rafex/repository/github/rafex/jchess/backend/java/jchess-parent/jchess-domain/src/main/java/dev/rafex/jchess/domain/model/GameSession.java)
- [TimeControlSpec.java](/Users/rafex/repository/github/rafex/jchess/backend/java/jchess-parent/jchess-domain/src/main/java/dev/rafex/jchess/domain/model/TimeControlSpec.java)
- [ChessEngineService.java](/Users/rafex/repository/github/rafex/jchess/backend/java/jchess-parent/jchess-application/src/main/java/dev/rafex/jchess/application/ChessEngineService.java)
- [SqliteGameRepository.java](/Users/rafex/repository/github/rafex/jchess/backend/java/jchess-parent/jchess-adapter-sqlite/src/main/java/dev/rafex/jchess/adapter/sqlite/SqliteGameRepository.java)

## Frontend Entry Points

The machine mode and level are currently selected in:
- [StartGamePanel.vue](/Users/rafex/repository/github/rafex/jchess/frontend/src/components/StartGamePanel.vue)

They are sent through:
- [api.js](/Users/rafex/repository/github/rafex/jchess/frontend/src/lib/api.js)

They are persisted in frontend session state through:
- [sessionStore.js](/Users/rafex/repository/github/rafex/jchess/frontend/src/lib/sessionStore.js)

The frontend does not currently expose a separate settings screen or config file for this.

## Current Product Interpretation

These levels are currently product-oriented presets, not official FIDE or engine-benchmark categories.

In practice:

- `EASY`
  - more forgiving for humans
  - quick and shallow
- `MEDIUM`
  - general-purpose balance
- `HARD`
  - stronger local search with noticeable thinking time
- `ADVANCED`
  - stronger pressure and longer response window
- `MASTER`
  - highest current preset with the deepest search and longest visible think time

## Current Limitations

Today these presets are hardcoded in source and are not yet:

- configurable from a settings file
- configurable from admin UI
- calibrated against a formal Elo model
- calibrated against measured human master move times from an external dataset

That means they should be treated as practical gameplay presets rather than scientific timing profiles.

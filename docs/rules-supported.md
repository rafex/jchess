# Supported Rules

This document lists the chess rules that `jchess` currently supports in the engine.

Reference used for comparison:
- [Reglas del ajedrez](https://ajedrez.fandom.com/es/wiki/Reglas_del_ajedrez)

## Board And Initial Setup

The engine supports:

- Standard 8x8 board coordinates.
- Standard initial piece placement.
- Correct side to move from the initial position.
- Correct castling rights and en passant state through FEN.

Relevant code:
- [Position.java](/Users/rafex/repository/github/rafex/jchess/backend/java/jchess-parent/jchess-domain/src/main/java/dev/rafex/jchess/domain/model/Position.java)
- [Board.java](/Users/rafex/repository/github/rafex/jchess/backend/java/jchess-parent/jchess-domain/src/main/java/dev/rafex/jchess/domain/model/Board.java)
- [FenCodec.java](/Users/rafex/repository/github/rafex/jchess/backend/java/jchess-parent/jchess-core/src/main/java/dev/rafex/jchess/core/engine/FenCodec.java)

## Piece Movement

The engine generates legal moves for:

- King
- Queen
- Rook
- Bishop
- Knight
- Pawn

This includes normal movement and capture rules, and rejects moves that land on a piece of the same side.

Relevant code:
- [DefaultLegalMoveGenerator.java](/Users/rafex/repository/github/rafex/jchess/backend/java/jchess-parent/jchess-core/src/main/java/dev/rafex/jchess/core/engine/DefaultLegalMoveGenerator.java)

## Legal Move Validation

The engine filters pseudo-legal moves into legal moves and prevents:

- Leaving your own king in check
- Moving when the resulting position is illegal
- Structured illegal moves such as invalid `from` / `to` combinations
- Invalid SAN-style move resolution

Relevant code:
- [DefaultLegalMoveGenerator.java](/Users/rafex/repository/github/rafex/jchess/backend/java/jchess-parent/jchess-core/src/main/java/dev/rafex/jchess/core/engine/DefaultLegalMoveGenerator.java)
- [MoveNotationService.java](/Users/rafex/repository/github/rafex/jchess/backend/java/jchess-parent/jchess-core/src/main/java/dev/rafex/jchess/core/engine/MoveNotationService.java)
- [ChessEngineService.java](/Users/rafex/repository/github/rafex/jchess/backend/java/jchess-parent/jchess-application/src/main/java/dev/rafex/jchess/application/ChessEngineService.java)

## Special Rules

The engine supports:

- Castling
  - blocked squares are checked
  - attacked transit squares are checked
  - legal rights are tracked in position state
- En passant
  - only available in the valid move window
  - en passant target square is tracked through position updates
- Pawn promotion
  - promotion to queen, rook, bishop, or knight
  - supported in backend and web UI

Relevant code:
- [PositionUpdater.java](/Users/rafex/repository/github/rafex/jchess/backend/java/jchess-parent/jchess-core/src/main/java/dev/rafex/jchess/core/engine/PositionUpdater.java)
- [DefaultLegalMoveGenerator.java](/Users/rafex/repository/github/rafex/jchess/backend/java/jchess-parent/jchess-core/src/main/java/dev/rafex/jchess/core/engine/DefaultLegalMoveGenerator.java)
- [PromotionDialog.vue](/Users/rafex/repository/github/rafex/jchess/frontend/src/components/PromotionDialog.vue)

## End Of Game Detection

The engine currently detects:

- Checkmate
- Stalemate
- Threefold repetition
- Fifty-move rule
- Resignation
- Time forfeit for backend-authoritative timed games

Relevant code:
- [GameStateInspector.java](/Users/rafex/repository/github/rafex/jchess/backend/java/jchess-parent/jchess-core/src/main/java/dev/rafex/jchess/core/engine/GameStateInspector.java)
- [ChessEngineService.java](/Users/rafex/repository/github/rafex/jchess/backend/java/jchess-parent/jchess-application/src/main/java/dev/rafex/jchess/application/ChessEngineService.java)
- [GameEndReason.java](/Users/rafex/repository/github/rafex/jchess/backend/java/jchess-parent/jchess-domain/src/main/java/dev/rafex/jchess/domain/model/GameEndReason.java)

## Notation Support

The engine supports:

- FEN import and export
- PGN export
- SAN-style move parsing in English
- SAN-style move parsing in Spanish
- Structured moves with `from`, `to`, and optional `promotion`

Relevant code:
- [FenCodec.java](/Users/rafex/repository/github/rafex/jchess/backend/java/jchess-parent/jchess-core/src/main/java/dev/rafex/jchess/core/engine/FenCodec.java)
- [PgnExporter.java](/Users/rafex/repository/github/rafex/jchess/backend/java/jchess-parent/jchess-core/src/main/java/dev/rafex/jchess/core/engine/PgnExporter.java)
- [MoveNotationService.java](/Users/rafex/repository/github/rafex/jchess/backend/java/jchess-parent/jchess-core/src/main/java/dev/rafex/jchess/core/engine/MoveNotationService.java)

## Timed Play

`jchess` now supports backend-authoritative clocks for online and machine games:

- time control stored in the session
- white and black remaining time stored in persistence
- increment applied after a legal move
- timeout converted into a finished game state

Offline local play in the browser still uses a frontend-managed clock by design.

Relevant code:
- [TimeControlSpec.java](/Users/rafex/repository/github/rafex/jchess/backend/java/jchess-parent/jchess-domain/src/main/java/dev/rafex/jchess/domain/model/TimeControlSpec.java)
- [GameSession.java](/Users/rafex/repository/github/rafex/jchess/backend/java/jchess-parent/jchess-domain/src/main/java/dev/rafex/jchess/domain/model/GameSession.java)
- [ChessEngineService.java](/Users/rafex/repository/github/rafex/jchess/backend/java/jchess-parent/jchess-application/src/main/java/dev/rafex/jchess/application/ChessEngineService.java)

## Current Scope

This document only lists rules that are currently implemented.

It does not claim full FIDE tournament procedure support. For example, arbiter procedures such as touch-move handling, formal illegal-move penalties, or score-sheet procedures are outside the current engine scope.

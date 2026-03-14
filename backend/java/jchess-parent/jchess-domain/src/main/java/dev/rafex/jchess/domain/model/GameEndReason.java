package dev.rafex.jchess.domain.model;

public enum GameEndReason {
    CHECKMATE,
    STALEMATE,
    FIFTY_MOVE_RULE,
    THREEFOLD_REPETITION,
    RESIGNATION,
    DRAW_AGREEMENT,
    NONE
}

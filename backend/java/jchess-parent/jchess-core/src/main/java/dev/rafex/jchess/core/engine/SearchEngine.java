package dev.rafex.jchess.core.engine;

import dev.rafex.jchess.domain.model.Move;
import dev.rafex.jchess.domain.model.Position;
import dev.rafex.jchess.domain.model.Side;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SearchEngine {
    private final DefaultLegalMoveGenerator legalMoveGenerator = new DefaultLegalMoveGenerator();
    private final PositionUpdater positionUpdater = new PositionUpdater();
    private final PositionEvaluator evaluator = new PositionEvaluator();
    private final PositionAnalyzer positionAnalyzer = new PositionAnalyzer();

    public Move chooseMove(Position position, EngineOptions options) {
        List<Move> legalMoves = legalMoveGenerator.generateLegalMoves(position);
        if (legalMoves.isEmpty()) {
            return Move.nullMove();
        }

        Instant deadline = Instant.now().plus(options.timeBudget());
        Move bestMove = legalMoves.getFirst();
        int bestScore = position.sideToMove() == Side.WHITE ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        for (Move move : orderMoves(position, legalMoves)) {
            Position next = positionUpdater.apply(position, move);
            int score = alphaBeta(next, options.depth() - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, deadline);
            if (position.sideToMove() == Side.WHITE ? score > bestScore : score < bestScore) {
                bestScore = score;
                bestMove = move;
            }
            if (Instant.now().isAfter(deadline)) {
                break;
            }
        }

        return bestMove;
    }

    private int alphaBeta(Position position, int depth, int alpha, int beta, Instant deadline) {
        if (depth <= 0 || Instant.now().isAfter(deadline)) {
            return evaluator.evaluate(position);
        }

        List<Move> legalMoves = legalMoveGenerator.generateLegalMoves(position);
        if (legalMoves.isEmpty()) {
            if (positionAnalyzer.isKingInCheck(position, position.sideToMove())) {
                return position.sideToMove() == Side.WHITE ? -100_000 - depth : 100_000 + depth;
            }
            return 0;
        }

        if (position.sideToMove() == Side.WHITE) {
            int score = Integer.MIN_VALUE;
            for (Move move : orderMoves(position, legalMoves)) {
                score = Math.max(score, alphaBeta(positionUpdater.apply(position, move), depth - 1, alpha, beta, deadline));
                alpha = Math.max(alpha, score);
                if (alpha >= beta) {
                    break;
                }
            }
            return score;
        }

        int score = Integer.MAX_VALUE;
        for (Move move : orderMoves(position, legalMoves)) {
            score = Math.min(score, alphaBeta(positionUpdater.apply(position, move), depth - 1, alpha, beta, deadline));
            beta = Math.min(beta, score);
            if (alpha >= beta) {
                break;
            }
        }
        return score;
    }

    private List<Move> orderMoves(Position position, List<Move> legalMoves) {
        List<Move> ordered = new ArrayList<>(legalMoves);
        ordered.sort(Comparator.comparingInt((Move move) -> moveScore(position, move)).reversed());
        return ordered;
    }

    private int moveScore(Position position, Move move) {
        int score = 0;
        if (MoveInspector.isCapture(position, move)) {
            score += 1000;
        }
        if (move.promotion() != null) {
            score += 800;
        }
        if (MoveInspector.isKingSideCastle(position, move) || MoveInspector.isQueenSideCastle(position, move)) {
            score += 200;
        }
        score += 14 - Math.abs(3 - move.to().file()) - Math.abs(3 - move.to().rank());
        return score;
    }
}

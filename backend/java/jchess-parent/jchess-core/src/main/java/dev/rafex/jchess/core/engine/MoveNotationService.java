package dev.rafex.jchess.core.engine;

import dev.rafex.jchess.domain.model.Move;
import dev.rafex.jchess.domain.model.NotationLanguage;
import dev.rafex.jchess.domain.model.Piece;
import dev.rafex.jchess.domain.model.PieceType;
import dev.rafex.jchess.domain.model.Position;
import dev.rafex.jchess.domain.model.Side;
import dev.rafex.jchess.domain.model.Square;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class MoveNotationService {
    private final PositionUpdater positionUpdater = new PositionUpdater();
    private final PositionAnalyzer positionAnalyzer = new PositionAnalyzer();
    private final DefaultLegalMoveGenerator legalMoveGenerator = new DefaultLegalMoveGenerator();

    public Move parse(Position position, String notation, List<Move> legalMoves) {
        String normalized = normalize(notation);
        List<Move> matches = new ArrayList<>();

        for (Move move : legalMoves) {
            Set<String> accepted = acceptedNotations(position, move, legalMoves);
            if (accepted.contains(normalized)) {
                matches.add(move);
            }
        }

        if (matches.isEmpty()) {
            throw new IllegalArgumentException("illegal or unsupported move notation: " + notation);
        }
        if (matches.size() > 1) {
            throw new IllegalArgumentException("ambiguous move notation: " + notation);
        }

        return matches.getFirst();
    }

    public String toNotation(Position position, Move move, NotationLanguage language, List<Move> legalMoves) {
        Piece piece = position.board().pieceAt(move.from())
                .orElseThrow(() -> new IllegalArgumentException("no piece at " + move.from().toAlgebraic()));

        if (MoveInspector.isKingSideCastle(position, move)) {
            return "O-O";
        }
        if (MoveInspector.isQueenSideCastle(position, move)) {
            return "O-O-O";
        }

        StringBuilder builder = new StringBuilder();
        boolean capture = MoveInspector.isCapture(position, move);

        if (piece.type() != PieceType.PAWN) {
            builder.append(pieceLetter(piece.type(), language));
            builder.append(disambiguation(position, move, legalMoves));
        } else if (capture) {
            builder.append((char) ('a' + move.from().file()));
        }

        if (capture) {
            builder.append('x');
        }

        builder.append(move.to().toAlgebraic());

        if (move.promotion() != null) {
            builder.append('=').append(pieceLetter(move.promotion(), language));
        }

        Position nextPosition = positionUpdater.apply(position, move);
        List<Move> opponentMoves = legalMoveGenerator.generateLegalMoves(nextPosition);
        boolean inCheck = positionAnalyzer.isKingInCheck(nextPosition, nextPosition.sideToMove());
        if (opponentMoves.isEmpty() && inCheck) {
            builder.append('#');
        } else if (inCheck) {
            builder.append('+');
        }

        return builder.toString();
    }

    public List<String> toNotations(Position position, List<Move> legalMoves, NotationLanguage language) {
        List<String> notations = new ArrayList<>(legalMoves.size());
        for (Move move : legalMoves) {
            notations.add(toNotation(position, move, language, legalMoves));
        }
        return List.copyOf(notations);
    }

    private Set<String> acceptedNotations(Position position, Move move, List<Move> legalMoves) {
        Set<String> accepted = new HashSet<>();
        Piece piece = position.board().pieceAt(move.from()).orElseThrow();

        accepted.add(normalize(move.uci()));
        accepted.add(normalize(toNotation(position, move, NotationLanguage.ENGLISH, legalMoves)));
        accepted.add(normalize(toNotation(position, move, NotationLanguage.SPANISH, legalMoves)));
        accepted.add(normalize(toNotation(position, move, NotationLanguage.ENGLISH, legalMoves).replace("x", "")));
        accepted.add(normalize(toNotation(position, move, NotationLanguage.SPANISH, legalMoves).replace("x", "")));

        if (piece.type() == PieceType.PAWN) {
            accepted.add(normalize(simpleCoordinate(move)));
            accepted.add(normalize("P" + simpleCoordinate(move)));
            accepted.add(normalize("P" + toNotation(position, move, NotationLanguage.ENGLISH, legalMoves)));
            accepted.add(normalize("P" + toNotation(position, move, NotationLanguage.SPANISH, legalMoves)));
        }

        return accepted;
    }

    private String simpleCoordinate(Move move) {
        return move.to().toAlgebraic();
    }

    private String disambiguation(Position position, Move move, List<Move> legalMoves) {
        Piece piece = position.board().pieceAt(move.from()).orElseThrow();
        List<Move> collisions = new ArrayList<>();
        for (Move candidate : legalMoves) {
            if (candidate.equals(move)) {
                continue;
            }
            Piece candidatePiece = position.board().pieceAt(candidate.from()).orElse(null);
            if (candidatePiece == null || candidatePiece.type() != piece.type()) {
                continue;
            }
            if (candidate.to().equals(move.to())) {
                collisions.add(candidate);
            }
        }

        if (collisions.isEmpty()) {
            return "";
        }

        boolean sameFile = collisions.stream().anyMatch(candidate -> candidate.from().file() == move.from().file());
        boolean sameRank = collisions.stream().anyMatch(candidate -> candidate.from().rank() == move.from().rank());

        if (!sameFile) {
            return String.valueOf((char) ('a' + move.from().file()));
        }
        if (!sameRank) {
            return String.valueOf(move.from().rank() + 1);
        }

        return move.from().toAlgebraic();
    }

    private String normalize(String notation) {
        if (notation == null) {
            return "";
        }

        return notation
                .trim()
                .replace("0-0-0", "O-O-O")
                .replace("0-0", "O-O")
                .replace("+", "")
                .replace("#", "")
                .replace("(", "")
                .replace(")", "")
                .replace("!", "")
                .replace("?", "")
                .replace("-", "")
                .replace(" ", "")
                .toUpperCase(Locale.ROOT);
    }

    private String pieceLetter(PieceType pieceType, NotationLanguage language) {
        return switch (language) {
            case ENGLISH -> switch (pieceType) {
                case PAWN -> "";
                case KNIGHT -> "N";
                case BISHOP -> "B";
                case ROOK -> "R";
                case QUEEN -> "Q";
                case KING -> "K";
            };
            case SPANISH -> switch (pieceType) {
                case PAWN -> "";
                case KNIGHT -> "C";
                case BISHOP -> "A";
                case ROOK -> "T";
                case QUEEN -> "D";
                case KING -> "R";
            };
        };
    }
}

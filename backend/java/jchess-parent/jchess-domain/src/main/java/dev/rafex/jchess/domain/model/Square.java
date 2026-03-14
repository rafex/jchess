package dev.rafex.jchess.domain.model;

public record Square(int file, int rank) {

    public Square {
        if (file < 0 || file > 7) {
            throw new IllegalArgumentException("file must be between 0 and 7");
        }
        if (rank < 0 || rank > 7) {
            throw new IllegalArgumentException("rank must be between 0 and 7");
        }
    }

    public static Square fromAlgebraic(String value) {
        if (value == null || value.length() != 2) {
            throw new IllegalArgumentException("square must use algebraic notation");
        }

        int file = value.charAt(0) - 'a';
        int rank = value.charAt(1) - '1';
        return new Square(file, rank);
    }

    public static Square fromIndex(int index) {
        if (index < 0 || index > 63) {
            throw new IllegalArgumentException("index must be between 0 and 63");
        }
        return new Square(index % 8, index / 8);
    }

    public int index() {
        return rank * 8 + file;
    }

    public boolean isOnBoard() {
        return file >= 0 && file < 8 && rank >= 0 && rank < 8;
    }

    public Square offset(int fileDelta, int rankDelta) {
        int nextFile = file + fileDelta;
        int nextRank = rank + rankDelta;
        if (nextFile < 0 || nextFile > 7 || nextRank < 0 || nextRank > 7) {
            return null;
        }
        return new Square(nextFile, nextRank);
    }

    public String toAlgebraic() {
        return String.valueOf((char) ('a' + file)) + (rank + 1);
    }
}

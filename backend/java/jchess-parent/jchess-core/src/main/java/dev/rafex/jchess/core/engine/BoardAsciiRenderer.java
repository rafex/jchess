package dev.rafex.jchess.core.engine;

import dev.rafex.jchess.domain.model.Piece;
import dev.rafex.jchess.domain.model.PieceType;
import dev.rafex.jchess.domain.model.Position;
import dev.rafex.jchess.domain.model.Side;
import dev.rafex.jchess.domain.model.Square;

import java.util.List;

public final class BoardAsciiRenderer {
    private static final String RESET = "\u001B[0m";

    private final BoardThemeCatalog themeCatalog;

    public BoardAsciiRenderer() {
        this(new BoardThemeCatalog());
    }

    public BoardAsciiRenderer(BoardThemeCatalog themeCatalog) {
        this.themeCatalog = themeCatalog;
    }

    public String render(Position position) {
        return render(position, BoardThemeCatalog.DEFAULT_THEME);
    }

    public String render(Position position, String themeName) {
        return render(position, themeCatalog.theme(themeName));
    }

    public List<BoardTheme> availableThemes() {
        return themeCatalog.themes();
    }

    public String render(Position position, BoardTheme theme) {
        int cellWidth = cellWidth(theme);
        String horizontalSegment = repeat(theme.horizontal(), cellWidth + 2);

        StringBuilder builder = new StringBuilder();
        builder.append(theme.frameColor()).append("    ")
                .append(theme.topLeft())
                .append(join(horizontalSegment, theme.joinTop(), 8))
                .append(theme.topRight())
                .append(RESET)
                .append('\n');

        for (int rank = 7; rank >= 0; rank--) {
            builder.append(theme.frameColor()).append(' ').append(rank + 1).append("  ").append(theme.vertical()).append(RESET);
            for (int file = 0; file < 8; file++) {
                Piece piece = position.board().pieceAt(new Square(file, rank)).orElse(null);
                String background = squareBackground(theme, file, rank);
                builder.append(background)
                        .append(' ')
                        .append(renderToken(theme, piece, cellWidth, background))
                        .append(' ')
                        .append(RESET);
                if (file < 7) {
                    builder.append(theme.frameColor()).append(theme.vertical()).append(RESET);
                }
            }
            builder.append(theme.frameColor()).append(theme.vertical()).append(RESET).append('\n');
            if (rank > 0) {
                builder.append(theme.frameColor()).append("    ")
                        .append(theme.leftJoin())
                        .append(join(horizontalSegment, theme.joinMiddle(), 8))
                        .append(theme.rightJoin())
                        .append(RESET)
                        .append('\n');
            }
        }

        builder.append(theme.frameColor()).append("    ")
                .append(theme.bottomLeft())
                .append(join(horizontalSegment, theme.joinBottom(), 8))
                .append(theme.bottomRight())
                .append(RESET)
                .append('\n');

        builder.append(theme.fileColor()).append("      ");
        for (int file = 0; file < 8; file++) {
            builder.append(center(String.valueOf((char) ('a' + file)), cellWidth + 2));
            if (file < 7) {
                builder.append(' ');
            }
        }
        builder.append(RESET).append('\n');
        return builder.toString();
    }

    private int cellWidth(BoardTheme theme) {
        int longest = Math.max(theme.minimumCellWidth(), theme.emptyToken().length());
        for (String token : theme.tokens().values()) {
            longest = Math.max(longest, token.length());
        }
        return longest;
    }

    private String squareBackground(BoardTheme theme, int file, int rank) {
        return ((file + rank) & 1) == 0 ? theme.lightSquareBackground() : theme.darkSquareBackground();
    }

    private String renderToken(BoardTheme theme, Piece piece, int cellWidth, String background) {
        if (piece == null) {
            return center(theme.emptyToken(), cellWidth);
        }

        String colorPrefix = piece.side() == Side.WHITE ? "white_" : "black_";
        String type = switch (piece.type()) {
            case KING -> "king";
            case QUEEN -> "queen";
            case ROOK -> "rook";
            case BISHOP -> "bishop";
            case KNIGHT -> "knight";
            case PAWN -> "pawn";
        };
        String color = piece.side() == Side.WHITE ? theme.whitePieceColor() : theme.blackPieceColor();
        return center(color + theme.token(colorPrefix + type) + RESET + background, cellWidth);
    }

    private String center(String value, int width) {
        String plain = value == null ? "" : value;
        int visibleWidth = visibleWidth(plain);
        if (visibleWidth >= width) {
            return plain;
        }

        int totalPadding = width - visibleWidth;
        int left = totalPadding / 2;
        int right = totalPadding - left;
        return " ".repeat(left) + plain + " ".repeat(right);
    }

    private int visibleWidth(String value) {
        return value.replaceAll("\u001B\\[[;\\d]*m", "").length();
    }

    private String join(String segment, String join, int count) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < count; index++) {
            builder.append(segment);
            if (index < count - 1) {
                builder.append(join);
            }
        }
        return builder.toString();
    }

    private String repeat(String value, int count) {
        return value.repeat(Math.max(count, 0));
    }
}

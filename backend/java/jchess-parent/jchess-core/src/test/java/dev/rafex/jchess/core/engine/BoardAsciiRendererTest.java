package dev.rafex.jchess.core.engine;

import dev.rafex.jchess.domain.model.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BoardAsciiRendererTest {

    @Test
    void shouldLoadBundledBoardThemes() {
        BoardThemeCatalog catalog = new BoardThemeCatalog();

        assertTrue(catalog.themeNames().contains("letters"));
        assertTrue(catalog.themeNames().contains("unicode"));
        assertTrue(catalog.themeNames().contains("matrix"));
        assertTrue(catalog.themeNames().size() >= 13);
    }

    @Test
    void shouldRenderPositionWithNamedTheme() {
        BoardAsciiRenderer renderer = new BoardAsciiRenderer();

        String board = renderer.render(Position.initial(), "blueprint");

        assertTrue(board.contains("a"));
        assertTrue(board.contains("8"));
        assertFalse(board.isBlank());
    }
}

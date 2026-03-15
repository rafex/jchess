package dev.rafex.jchess.core.engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class BoardThemeCatalog {
    public static final String DEFAULT_THEME = "letters";

    private static final String RESOURCE_INDEX = "boards/index.txt";

    private final Map<String, BoardTheme> themes;

    public BoardThemeCatalog() {
        this.themes = loadThemes();
    }

    public BoardTheme defaultTheme() {
        return theme(DEFAULT_THEME);
    }

    public BoardTheme theme(String name) {
        String resolved = name == null || name.isBlank() ? DEFAULT_THEME : name.trim().toLowerCase();
        BoardTheme theme = themes.get(resolved);
        if (theme == null) {
            throw new IllegalArgumentException("unknown board theme: " + name + ". Available: " + String.join(", ", themeNames()));
        }
        return theme;
    }

    public List<String> themeNames() {
        return themes.keySet().stream().sorted().toList();
    }

    public List<BoardTheme> themes() {
        return themes.values().stream().sorted(Comparator.comparing(BoardTheme::name)).toList();
    }

    private Map<String, BoardTheme> loadThemes() {
        Map<String, BoardTheme> loaded = new LinkedHashMap<>();
        loadBundledThemes(loaded);
        loadExternalThemes(loaded, Path.of(System.getProperty("user.home"), ".jchess", "boards"));
        loadExternalThemes(loaded, Path.of("boards"));
        return loaded;
    }

    private void loadBundledThemes(Map<String, BoardTheme> loaded) {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(RESOURCE_INDEX)) {
            if (inputStream == null) {
                throw new IllegalStateException("missing bundled board theme index");
            }
            List<String> files = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)
                    .lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank() && !line.startsWith("#"))
                    .toList();
            for (String file : files) {
                String resource = "boards/" + file;
                try (InputStream themeStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
                    if (themeStream == null) {
                        throw new IllegalStateException("missing board theme resource " + resource);
                    }
                    BoardTheme theme = BoardThemeParser.parse(new String(themeStream.readAllBytes(), StandardCharsets.UTF_8));
                    loaded.put(theme.name(), theme);
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("failed to load bundled board themes", ex);
        }
    }

    private void loadExternalThemes(Map<String, BoardTheme> loaded, Path directory) {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (var stream = Files.list(directory)) {
            stream.filter(path -> path.getFileName().toString().endsWith(".txt"))
                    .sorted()
                    .forEach(path -> {
                        try {
                            BoardTheme theme = BoardThemeParser.parse(Files.readString(path, StandardCharsets.UTF_8));
                            loaded.put(theme.name(), theme);
                        } catch (IOException ex) {
                            throw new UncheckedIOException("failed to read board theme " + path, ex);
                        }
                    });
        } catch (IOException ex) {
            throw new UncheckedIOException("failed to load board themes from " + directory, ex);
        }
    }
}

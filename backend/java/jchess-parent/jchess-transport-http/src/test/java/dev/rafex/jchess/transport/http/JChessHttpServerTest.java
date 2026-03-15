package dev.rafex.jchess.transport.http;

import com.fasterxml.jackson.databind.JsonNode;
import dev.rafex.ether.json.JsonCodecBuilder;
import dev.rafex.jchess.adapter.sqlite.SqliteGameRepository;
import dev.rafex.jchess.application.ChessEngineService;
import dev.rafex.jchess.application.EngineFacade;
import dev.rafex.jchess.domain.model.GameState;
import dev.rafex.jchess.domain.model.LlmProvider;
import dev.rafex.jchess.ports.outbound.EngineTelemetry;
import dev.rafex.jchess.ports.outbound.MachineMovePort;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JChessHttpServerTest {

    @Test
    void shouldExposeHealthThemesAndCreateGame(@TempDir Path tempDir) throws Exception {
        JChessHttpServer.ServerRuntime runtime = startRuntime(tempDir);
        int port = ((ServerConnector) runtime.runner().server().getConnectors()[0]).getLocalPort();
        HttpClient client = HttpClient.newHttpClient();
        var json = JsonCodecBuilder.create().build();

        try {
            HttpResponse<String> health = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/health")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, health.statusCode());
            assertEquals("UP", json.readTree(health.body()).get("status").asText());

            HttpResponse<String> themes = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/themes")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, themes.statusCode());
            JsonNode themesJson = json.readTree(themes.body());
            assertEquals("themes", themesJson.get("type").asText());
            assertTrue(themesJson.get("data").isArray());

            HttpResponse<String> created = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/games"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString("""
                                    {"color":"white","opponent":"human","whitePlayerName":"Alice","blackPlayerName":"Bob"}
                                    """))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(201, created.statusCode());
            JsonNode createdJson = json.readTree(created.body());
            assertEquals("game_created", createdJson.get("type").asText());
            assertTrue(createdJson.at("/data/requester/playerToken").asText().length() > 10);
        } finally {
            runtime.runner().stop();
        }
    }

    @Test
    void shouldExposeBothPlayerTokensForLocalHotseat(@TempDir Path tempDir) throws Exception {
        JChessHttpServer.ServerRuntime runtime = startRuntime(tempDir);
        int port = ((ServerConnector) runtime.runner().server().getConnectors()[0]).getLocalPort();
        HttpClient client = HttpClient.newHttpClient();
        var json = JsonCodecBuilder.create().build();

        try {
            HttpResponse<String> created = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/games"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString("""
                                    {"color":"white","opponent":"human","whitePlayerName":"Alice","blackPlayerName":"Bob","localHotseat":true}
                                    """))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            JsonNode createdJson = json.readTree(created.body());
            assertTrue(createdJson.at("/data/game/players/0/playerToken").asText().length() > 10);
            assertTrue(createdJson.at("/data/game/players/1/playerToken").asText().length() > 10);
        } finally {
            runtime.runner().stop();
        }
    }

    @Test
    void shouldHandleCorsPreflight(@TempDir Path tempDir) throws Exception {
        JChessHttpServer.ServerRuntime runtime = startRuntime(tempDir);
        int port = ((ServerConnector) runtime.runner().server().getConnectors()[0]).getLocalPort();
        HttpClient client = HttpClient.newHttpClient();

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/games"))
                    .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                    .header("Origin", "http://localhost:5173")
                    .header("Access-Control-Request-Method", "POST")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(204, response.statusCode());
            assertEquals("http://localhost:5173", response.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
            assertTrue(response.headers().firstValue("Access-Control-Allow-Methods").orElse("").contains("POST"));
        } finally {
            runtime.runner().stop();
        }
    }

    @Test
    void shouldListRecentGames(@TempDir Path tempDir) throws Exception {
        JChessHttpServer.ServerRuntime runtime = startRuntime(tempDir);
        int port = ((ServerConnector) runtime.runner().server().getConnectors()[0]).getLocalPort();
        HttpClient client = HttpClient.newHttpClient();
        var json = JsonCodecBuilder.create().build();

        try {
            client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/games"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString("""
                                    {"color":"white","opponent":"human","whitePlayerName":"Alice","blackPlayerName":"Bob"}
                                    """))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            HttpResponse<String> listed = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/games?limit=5"))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            assertEquals(200, listed.statusCode());
            JsonNode listedJson = json.readTree(listed.body());
            assertEquals("games_list", listedJson.get("type").asText());
            assertTrue(listedJson.at("/data/games").isArray());
            assertEquals(1, listedJson.at("/data/games").size());
            assertEquals("Alice", listedJson.at("/data/games/0/whitePlayerName").asText());
        } finally {
            runtime.runner().stop();
        }
    }

    private JChessHttpServer.ServerRuntime startRuntime(Path tempDir) throws Exception {
        EngineFacade engine = new ChessEngineService(
                new SqliteGameRepository(tempDir.resolve("jchess-http-test.db")),
                new NoOpTelemetry(),
                new NoOpMachineMovePort()
        );
        JChessHttpServer server = new JChessHttpServer(engine, 0);
        JChessHttpServer.ServerRuntime runtime = server.createRuntime();
        runtime.runner().start();
        return runtime;
    }

    private static final class NoOpTelemetry implements EngineTelemetry {
        @Override
        public void record(String event, String detail) {
        }
    }

    private static final class NoOpMachineMovePort implements MachineMovePort {
        @Override
        public Optional<String> suggestMove(GameState gameState, List<String> legalMovesEnglish, LlmProvider provider) {
            return Optional.empty();
        }
    }
}

package dev.rafex.jchess.adapter.llm;

import dev.rafex.jchess.domain.model.GameState;
import dev.rafex.jchess.domain.model.LlmProvider;
import dev.rafex.jchess.ports.outbound.MachineMovePort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HttpMachineMoveClient implements MachineMovePort {
    private static final Pattern CONTENT_PATTERN = Pattern.compile("\"content\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);

    private final HttpClient httpClient;

    public HttpMachineMoveClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    public HttpMachineMoveClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public Optional<String> suggestMove(GameState gameState, List<String> legalMovesEnglish, LlmProvider provider) {
        if (provider == null) {
            return Optional.empty();
        }

        String apiKey = apiKey(provider);
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        String requestBody = requestBody(gameState, legalMovesEnglish, provider);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint(provider)))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }
            return extractContent(response.body());
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private String endpoint(LlmProvider provider) {
        return switch (provider) {
            case DEEPSEEK -> "https://api.deepseek.com/chat/completions";
            case GROQ -> "https://api.groq.com/openai/v1/chat/completions";
        };
    }

    private String apiKey(LlmProvider provider) {
        return switch (provider) {
            case DEEPSEEK -> System.getenv("DEEPSEEK_API_KEY");
            case GROQ -> System.getenv("GROQ_API_KEY");
        };
    }

    private String model(LlmProvider provider) {
        return switch (provider) {
            case DEEPSEEK -> "deepseek-chat";
            case GROQ -> "llama-3.3-70b-versatile";
        };
    }

    private String requestBody(GameState gameState, List<String> legalMovesEnglish, LlmProvider provider) {
        String prompt = """
                You are choosing one legal chess move for a game engine.
                Return only one move from this legal move list, without commentary.
                Prefer SAN or coordinate notation accepted by chess software.
                Session: %s
                FEN: %s
                Moves played: %s
                Legal moves: %s
                """.formatted(
                gameState.session().sessionId(),
                gameState.session().currentPosition().toFen(),
                gameState.moves(),
                String.join(", ", legalMovesEnglish)
        );

        return """
                {
                  "model": "%s",
                  "temperature": 0.2,
                  "max_tokens": 16,
                  "messages": [
                    {"role": "system", "content": "You are a deterministic chess move selector. Reply with exactly one legal move."},
                    {"role": "user", "content": "%s"}
                  ]
                }
                """.formatted(model(provider), escapeJson(prompt));
    }

    private Optional<String> extractContent(String body) {
        Matcher matcher = CONTENT_PATTERN.matcher(body);
        while (matcher.find()) {
            String content = matcher.group(1)
                    .replace("\\n", " ")
                    .replace("\\\"", "\"")
                    .trim();
            if (!content.isBlank()) {
                return Optional.of(content);
            }
        }
        return Optional.empty();
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}

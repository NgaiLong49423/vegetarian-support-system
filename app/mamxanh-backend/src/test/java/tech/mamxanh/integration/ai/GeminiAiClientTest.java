package tech.mamxanh.integration.ai;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.genai.types.HttpOptions;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GeminiAiClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void reportsMissingConfigurationWithoutMakingAProviderRequest() {
        try (var client = new GeminiAiClient("  ")) {
            assertThatThrownBy(() -> client.generateRecipeText("prompt", List.of(1L)))
                    .isInstanceOf(GeminiAiClient.AiProviderException.class)
                    .hasMessage("Gemini is not configured");
        }
    }

    @Test
    void treatsNullApiKeyAsMissingConfiguration() {
        try (var client = new GeminiAiClient(null)) {
            assertThatThrownBy(() -> client.generateRecipeText("prompt", List.of()))
                    .isInstanceOf(GeminiAiClient.AiProviderException.class);
        }
    }

    @Test
    void sendsStructuredGenerationRequestAndReturnsProviderText() throws IOException {
        startServer(200, "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"description\\\":\\\"Món ngon\\\",\\\"steps\\\":[]}\"}],\"role\":\"model\"},\"finishReason\":\"STOP\"}]}");

        try (var client = configuredClient()) {
            String result = client.generateRecipeText("Mô tả món", List.of(2L, 9L));
            assertThat(result).isEqualTo("{\"description\":\"Món ngon\",\"steps\":[]}");
        }
    }

    @Test
    void rejectsBlankProviderResponse() throws IOException {
        startServer(200, "{\"candidates\":[{\"content\":{\"parts\":[],\"role\":\"model\"}}]}");

        try (var client = configuredClient()) {
            assertThatThrownBy(() -> client.generateRecipeText("prompt", List.of(1L)))
                    .isInstanceOf(GeminiAiClient.AiProviderException.class)
                    .hasMessage("Gemini returned an empty response");
        }
    }

    @Test
    void wrapsProviderHttpFailures() throws IOException {
        startServer(400, "{\"error\":{\"code\":400,\"message\":\"invalid request\",\"status\":\"INVALID_ARGUMENT\"}}");

        try (var client = configuredClient()) {
            assertThatThrownBy(() -> client.generateRecipeText("prompt", List.of(1L)))
                    .isInstanceOf(GeminiAiClient.AiProviderException.class)
                    .hasMessage("Gemini request failed");
        }
    }

    private GeminiAiClient configuredClient() {
        var options = HttpOptions.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .apiVersion("v1beta").timeout(3_000).build();
        return new GeminiAiClient("test-key", options);
    }

    @Test
    void recommendationSchemaReturnsProviderTokenMetadata() throws IOException {
        startServer(200, """
                {"candidates":[{"content":{"parts":[{"text":"{\\"recipeIds\\":[2]}"}],"role":"model"}}],
                 "usageMetadata":{"promptTokenCount":40,"candidatesTokenCount":8,"totalTokenCount":48}}
                """);
        try (var client = configuredClient()) {
            var result = client.recommendRecipes("Choose a pairing", List.of(2L, 3L), 6);
            assertThat(result.json()).isEqualTo("{\"recipeIds\":[2]}");
            assertThat(result.promptTokens()).isEqualTo(40);
            assertThat(result.responseTokens()).isEqualTo(8);
            assertThat(result.totalTokens()).isEqualTo(48);
        }
    }

    @Test
    void truncatedRecommendationIsNotAcceptedAsSuccessfulGeneration() throws IOException {
        startServer(200, """
                {"candidates":[{"content":{"parts":[{"text":"{\\"recipeIds\\":[2]"}],"role":"model"},
                  "finishReason":"MAX_TOKENS"}]}
                """);
        try (var client = configuredClient()) {
            assertThatThrownBy(() -> client.recommendRecipes("pairing", List.of(2L), 6))
                    .isInstanceOf(GeminiAiClient.AiProviderException.class)
                    .hasMessage("Gemini recommendation output is incomplete");
        }
    }

    @Test
    void recommendationHandlesMissingConfigurationEmptyOutputAndProviderFailure() throws IOException {
        try (var client = new GeminiAiClient(null)) {
            assertThatThrownBy(() -> client.recommendRecipes("pairing", List.of(2L), 6))
                    .isInstanceOf(GeminiAiClient.AiProviderException.class);
        }
        startServer(200, "{\"candidates\":[{\"content\":{\"parts\":[],\"role\":\"model\"}}]}");
        try (var client = configuredClient()) {
            assertThatThrownBy(() -> client.recommendRecipes("pairing", List.of(2L), 6))
                    .isInstanceOf(GeminiAiClient.AiProviderException.class);
        }
        server.stop(0);
        startServer(503, "{\"error\":{\"code\":503,\"message\":\"unavailable\",\"status\":\"UNAVAILABLE\"}}");
        try (var client = configuredClient()) {
            assertThatThrownBy(() -> client.recommendRecipes("pairing", List.of(2L), 6))
                    .isInstanceOf(GeminiAiClient.AiProviderException.class);
        }
    }

    @Test
    void chatReturnsAnswerAndTokenMetadata() throws IOException {
        startServer(200, """
                {"candidates":[{"content":{"parts":[{"text":"{\\"action\\":\\"ANSWER\\",\\"answer\\":\\"Dùng sữa đậu nành.\\",\\"recommendations\\":[]}"}],"role":"model"},"finishReason":"STOP"}],
                 "usageMetadata":{"promptTokenCount":24,"candidatesTokenCount":9,"totalTokenCount":33}}
                """);

        try (var client = configuredClient()) {
            var result = client.answerChat("Trả lời về món chay", "Có thể thay sữa bò bằng gì?");
            assertThat(result.structuredJson()).contains("Dùng sữa đậu nành.");
            assertThat(result.promptTokens()).isEqualTo(24);
            assertThat(result.responseTokens()).isEqualTo(9);
            assertThat(result.totalTokens()).isEqualTo(33);
        }
    }

    @Test
    void chatRejectsIncompleteOrEmptyResponsesAndWrapsProviderErrors() throws IOException {
        try (var client = new GeminiAiClient(null)) {
            assertThatThrownBy(() -> client.answerChat("system", "question"))
                    .isInstanceOf(GeminiAiClient.AiProviderException.class)
                    .hasMessage("Gemini is not configured");
        }

        startServer(200, """
                {"candidates":[{"content":{"parts":[{"text":"partial"}],"role":"model"},"finishReason":"MAX_TOKENS"}]}
                """);
        try (var client = configuredClient()) {
            assertThatThrownBy(() -> client.answerChat("system", "question"))
                    .isInstanceOf(GeminiAiClient.AiProviderException.class)
                    .hasMessage("Gemini returned an incomplete chat response");
        }

        server.stop(0);
        startServer(200, "{\"candidates\":[{\"content\":{\"parts\":[],\"role\":\"model\"},\"finishReason\":\"STOP\"}]}");
        try (var client = configuredClient()) {
            assertThatThrownBy(() -> client.answerChat("system", "question"))
                    .isInstanceOf(GeminiAiClient.AiProviderException.class)
                    .hasMessage("Gemini returned an incomplete chat response");
        }

        server.stop(0);
        startServer(503, "{\"error\":{\"code\":503,\"message\":\"unavailable\",\"status\":\"UNAVAILABLE\"}}");
        try (var client = configuredClient()) {
            assertThatThrownBy(() -> client.answerChat("system", "question"))
                    .isInstanceOf(GeminiAiClient.AiProviderException.class)
                    .hasMessage("Gemini chat request failed");
        }
    }

    private void startServer(int status, String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
    }
}

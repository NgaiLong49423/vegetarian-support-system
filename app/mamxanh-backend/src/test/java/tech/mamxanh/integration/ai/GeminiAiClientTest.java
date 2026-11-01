package tech.mamxanh.integration.ai;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.genai.types.HttpOptions;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class GeminiAiClientTest {
    private HttpServer server;
    private final AtomicInteger requests = new AtomicInteger();
    private final java.util.concurrent.atomic.AtomicReference<String> requestedPath = new java.util.concurrent.atomic.AtomicReference<>();

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void sharedGeminiClientUsesTheNfrBoundedProviderTimeout() {
        assertThat(GeminiAiClient.DEFAULT_TIMEOUT_MILLIS).isEqualTo(9_000);
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
            assertThat(requestedPath.get()).endsWith("/models/gemini-3.8-flash:generateContent");
        }
    }

    @Test
    void recipeGenerationReservesEnoughOutputAndKeepsProviderDefaultThinking() throws IOException {
        startServer(200, "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"description\\\":\\\"Món ngon\\\",\\\"steps\\\":[]}\"}],\"role\":\"model\"},\"finishReason\":\"STOP\"}]}");

        try (var client = configuredClient()) {
            client.generateRecipeText("Viết hướng dẫn chi tiết cho 8 nguyên liệu", List.of(1L, 2L));
        }

        var request = tools.jackson.databind.json.JsonMapper.builder().build().readTree(lastRequest.get());
        assertThat(request.path("generationConfig").path("maxOutputTokens").asInt()).isGreaterThanOrEqualTo(8192);
        assertThat(request.path("generationConfig").has("thinkingConfig")).isFalse();
    }

    @Test
    void rejectsTruncatedRecipeJsonBeforePassingItToTheRecipeService() throws IOException {
        startServer(200, """
                {"candidates":[{"content":{"parts":[{"text":"{\\"description\\":\\"Món ngon\\",\\"steps\\":["}],"role":"model"},
                  "finishReason":"MAX_TOKENS"}]}
                """);

        try (var client = configuredClient()) {
            assertThatThrownBy(() -> client.generateRecipeText("Hướng dẫn chi tiết", List.of(1L)))
                    .isInstanceOf(GeminiAiClient.AiProviderException.class)
                    .hasMessage("Gemini recipe output is incomplete")
                    .extracting(error -> ((GeminiAiClient.AiProviderException) error).kind())
                    .isEqualTo(GeminiAiClient.FailureKind.INVALID_RESPONSE);
            assertThat(requests.get()).isEqualTo(1);
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

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"MAX_TOKENS", "SAFETY", "RECITATION"})
    void rejectsUnfinishedRecipeOutputEvenWhenTextIsValidJson(String finishReason) throws IOException {
        startServer(200, "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"description\\\":\\\"Synthetic text\\\",\\\"steps\\\":[]}\"}],\"role\":\"model\"},\"finishReason\":\""
                + finishReason + "\"}]}");
        try (var client = configuredClient()) {
            assertThatThrownBy(() -> client.generateRecipeText("synthetic prompt", List.of(1L)))
                    .isInstanceOfSatisfying(GeminiAiClient.AiProviderException.class,
                            failure -> assertThat(failure.kind()).isEqualTo(GeminiAiClient.FailureKind.INVALID_RESPONSE));
        }
        assertThat(requests).hasValue(1);
    }

    @Test
    void recipeAssistancePreservesThinkingAndCachedMetadata() throws IOException {
        startServer(200, """
                {"candidates":[{"content":{"parts":[{"text":"{}"}],"role":"model"},"finishReason":"STOP"}],
                 "usageMetadata":{"promptTokenCount":150,"candidatesTokenCount":300,"totalTokenCount":500,
                   "thoughtsTokenCount":50,"cachedContentTokenCount":20}}
                """);
        try (var client = configuredClient()) {
            var result = client.generateRecipeContent("Recipe assistance", List.of(1L));
            assertThat(result.usage()).isEqualTo(new AiTokenUsage(AiTokenUsage.MODEL,"STANDARD",150,300,500,50,20,0));
        }
    }

    @Test
    void incompleteRecipeResponseIsNotSuccessfulEvenWhenJsonParses() throws IOException {
        startServer(200, """
                {"candidates":[{"content":{"parts":[{"text":"{}"}],"role":"model"},"finishReason":"MAX_TOKENS"}]}
                """);
        try (var client = configuredClient()) {
            assertThatThrownBy(() -> client.generateRecipeContent("Recipe assistance", List.of(1L)))
                    .isInstanceOf(GeminiAiClient.AiProviderException.class);
        }
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

    private final AtomicReference<String> lastRequest = new AtomicReference<>();

    private void startServer(int status, String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            requestedPath.set(exchange.getRequestURI().getPath());
            lastRequest.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
    }

    @Test
    void providerQuotaFailureMakesOneAttemptAndLogsOnlySafeMetadata(CapturedOutput output) throws IOException {
        startServer(429, "{\"error\":{\"code\":429,\"message\":\"private-provider-body\",\"status\":\"RESOURCE_EXHAUSTED\"}}");
        try (var client = configuredClient()) {
            assertThatThrownBy(() -> client.answerChat("private-system", "private-question"))
                    .isInstanceOfSatisfying(GeminiAiClient.AiProviderException.class, failure -> {
                        assertThat(failure.kind()).isEqualTo(GeminiAiClient.FailureKind.HTTP);
                        assertThat(failure.httpStatus()).isEqualTo(429);
                    });
        }
        assertThat(requests.get()).isEqualTo(1);
        assertThat(output.getOut()).contains("operation=CHAT", "model=gemini-3.8-flash",
                "category=HTTP", "httpStatus=429", "elapsedMs=")
                .doesNotContain("private-system", "private-question", "private-provider-body", "test-key");
    }

    @Test
    void timeoutReturnsWithoutSdkRetryBackoff(CapturedOutput output) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            try { Thread.sleep(1500); }
            catch (InterruptedException failure) { Thread.currentThread().interrupt(); }
            exchange.close();
        });
        server.start();
        var options = HttpOptions.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .timeout(150).build();
        try (var client = new GeminiAiClient("test-key", options)) {
            long started = System.nanoTime();
            assertThatThrownBy(() -> client.answerChat("private-system", "private-question"))
                    .isInstanceOfSatisfying(GeminiAiClient.AiProviderException.class,
                            failure -> assertThat(failure.kind()).isEqualTo(GeminiAiClient.FailureKind.TIMEOUT));
            assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(3));
        }
        assertThat(requests.get()).isEqualTo(1);
        assertThat(output.getOut()).contains("category=TIMEOUT").doesNotContain("private-question", "test-key");
    }

    @Test
    void oversizedCallerTimeoutCannotExceedTheApplicationDeadline() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            try { Thread.sleep(12_000); }
            catch (InterruptedException failure) { Thread.currentThread().interrupt(); }
            exchange.close();
        });
        server.start();
        var options = HttpOptions.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .timeout(60_000).build();
        try (var client = new GeminiAiClient("test-key", options)) {
            long started = System.nanoTime();
            assertThatThrownBy(() -> client.answerChat("system", "question"))
                    .isInstanceOfSatisfying(GeminiAiClient.AiProviderException.class,
                            failure -> assertThat(failure.kind()).isEqualTo(GeminiAiClient.FailureKind.TIMEOUT));
            assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(10));
        }
        assertThat(requests.get()).isEqualTo(1);
    }

    @Test
    void earlyUnavailableResponseCanRecoverOnOneRetryWithoutLoggingPrivateContent(CapturedOutput output) throws IOException {
        startSequenceServer(503, 0, false);
        try (var client = configuredClient()) {
            assertThat(client.answerChat("private-system", "private-question").structuredJson()).contains("Recovered");
        }
        assertThat(requests).hasValue(2);
        assertThat(output.getOut()).contains("attempt=2", "httpStatus=503")
                .doesNotContain("private-system", "private-question", "private-provider-body", "test-key");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {502, 503, 504})
    void persistentTransientFailureStopsAfterTwoHttpAttempts(int code) throws IOException {
        startServer(code, "{\"error\":{\"code\":" + code + ",\"message\":\"private-provider-body\",\"status\":\"UNAVAILABLE\"}}");
        try (var client = configuredClient()) {
            assertThatThrownBy(() -> client.answerChat("system", "question"))
                    .isInstanceOfSatisfying(GeminiAiClient.AiProviderException.class,
                            failure -> assertThat(failure.httpStatus()).isEqualTo(code));
        }
        assertThat(requests).hasValue(2);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {400, 403, 404})
    void clientFailuresAreNotRetried(int code) throws IOException {
        startServer(code, "{\"error\":{\"code\":" + code + ",\"message\":\"invalid\",\"status\":\"INVALID_ARGUMENT\"}}");
        try (var client = configuredClient()) {
            assertThatThrownBy(() -> client.answerChat("system", "question"))
                    .isInstanceOf(GeminiAiClient.AiProviderException.class);
        }
        assertThat(requests).hasValue(1);
    }

    @Test
    void lateUnavailableResponseDoesNotStartARetryWithoutEnoughBudget() throws IOException {
        startSequenceServer(503, 4_800, false);
        try (var client = longTransportTimeoutClient()) {
            assertThatThrownBy(() -> client.answerChat("system", "question"))
                    .isInstanceOfSatisfying(GeminiAiClient.AiProviderException.class,
                            failure -> assertThat(failure.httpStatus()).isEqualTo(503));
        }
        assertThat(requests).hasValue(1);
    }

    @Test
    void retryAndBackoffCannotExtendTheOverallDeadline() throws IOException {
        startSequenceServer(503, 0, true);
        try (var client = longTransportTimeoutClient()) {
            long started = System.nanoTime();
            assertThatThrownBy(() -> client.answerChat("system", "question"))
                    .isInstanceOfSatisfying(GeminiAiClient.AiProviderException.class,
                            failure -> assertThat(failure.kind()).isEqualTo(GeminiAiClient.FailureKind.TIMEOUT));
            assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(10));
        }
        assertThat(requests).hasValue(2);
    }

    private GeminiAiClient longTransportTimeoutClient() {
        return new GeminiAiClient("test-key", HttpOptions.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort()).timeout(60_000).build());
    }

    private void startSequenceServer(int firstStatus, long firstDelayMillis, boolean slowSecond) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            int count = requests.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            try { Thread.sleep(count == 1 ? firstDelayMillis : slowSecond ? 12_000 : 0); }
            catch (InterruptedException failure) { Thread.currentThread().interrupt(); }
            String body = count == 1 ? "{\"error\":{\"code\":" + firstStatus + ",\"message\":\"private-provider-body\",\"status\":\"UNAVAILABLE\"}}"
                    : "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"action\\\":\\\"ANSWER\\\",\\\"answer\\\":\\\"Recovered\\\",\\\"recommendations\\\":[]}\"}],\"role\":\"model\"},\"finishReason\":\"STOP\"}]}";
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(count == 1 ? firstStatus : 200, response.length);
            try (var stream = exchange.getResponseBody()) { stream.write(response); }
        });
        server.start();
    }
}

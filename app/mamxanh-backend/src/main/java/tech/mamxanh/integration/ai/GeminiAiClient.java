package tech.mamxanh.integration.ai;

import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.types.HttpRetryOptions;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import com.google.genai.types.ThinkingConfig;
import com.google.genai.types.ThinkingLevel;
import java.util.List;
import java.util.Map;
import java.io.InterruptedIOException;
import java.util.function.Supplier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GeminiAiClient implements AiClient, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(GeminiAiClient.class);
    // Reserve application overhead within the NFR-18 ten-second provider budget.
    private static final int REQUEST_TIMEOUT_MS = 9_000;
    private static final int MIN_RETRY_BUDGET_MS = 2_000;
    // User-authorized FR-10 branch trial; the confirmed technology baseline remains 3.8 Flash.
    private static final String MODEL = "gemini-3.1-flash-lite";
    private final Client client;
    private final ExecutorService requests = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("gemini-request-", 0).factory());

    @Autowired
    public GeminiAiClient(@Value("${GEMINI_API_KEY:}") String apiKey) {
        this(apiKey, HttpOptions.builder().build());
    }

    GeminiAiClient(String apiKey, HttpOptions httpOptions) {
        this.client = apiKey == null || apiKey.isBlank() ? null : Client.builder()
                .apiKey(apiKey.strip())
                .httpOptions(httpOptions.toBuilder()
                        .timeout(Math.min(httpOptions.timeout().orElse(REQUEST_TIMEOUT_MS), REQUEST_TIMEOUT_MS))
                        // The outer application deadline owns retries; disable independent SDK backoff.
                        .retryOptions(HttpRetryOptions.builder().attempts(1).build()).build())
                .build();
    }

    @Override
    public String generateRecipeText(String prompt, List<Long> allowedIngredientIds) {
        Schema textSchema = Schema.builder().type(Type.Known.STRING).build();
        Schema ingredientIdsSchema = Schema.builder().type(Type.Known.ARRAY)
                .items(Schema.builder().type(Type.Known.INTEGER)).build();
        Schema stepSchema = Schema.builder().type(Type.Known.OBJECT)
                .properties(Map.of("text", textSchema, "ingredientIds", ingredientIdsSchema))
                .required(List.of("text", "ingredientIds")).build();
        Schema schema = Schema.builder().type(Type.Known.OBJECT)
                .properties(Map.of("description", textSchema,
                        "steps", Schema.builder().type(Type.Known.ARRAY).items(stepSchema).build()))
                .required(List.of("description", "steps")).build();
        GenerateContentConfig config = GenerateContentConfig.builder()
                .responseMimeType("application/json")
                .responseSchema(schema)
                .candidateCount(1)
                .maxOutputTokens(2048)
                .build();
        return request("RECIPE_TEXT", "Gemini request failed", () -> {
            var response = client.models.generateContent(MODEL,
                    prompt + "\nAllowed ingredient IDs: " + allowedIngredientIds, config);
            String finishReason = response.candidates().orElse(List.of()).stream().findFirst()
                    .flatMap(candidate -> candidate.finishReason()).map(Object::toString).orElse("STOP");
            if (!"STOP".equals(finishReason)) throw AiProviderException.invalidResponse("Gemini recipe output is incomplete");
            String text = response.text();
            if (text == null || text.isBlank()) throw AiProviderException.invalidResponse("Gemini returned an empty response");
            return text;
        });
    }

    @Override
    public void close() {
        requests.shutdownNow();
        if (client != null) client.close();
    }

    @Override
    public RecommendationResult recommendRecipes(String prompt, List<Long> allowedRecipeIds, int limit) {
        return recommendSequence(prompt, allowedRecipeIds, limit);
    }

    @Override
    public ChatResult answerChat(String systemInstruction, String prompt) {
        Schema recommendationSchema = Schema.builder().type(Type.Known.OBJECT)
                .properties(Map.of("recipeId", Schema.builder().type(Type.Known.INTEGER).build(),
                        "reason", Schema.builder().type(Type.Known.STRING).build()))
                .required(List.of("recipeId", "reason")).build();
        Schema schema = Schema.builder().type(Type.Known.OBJECT)
                .properties(Map.of("action", Schema.builder().type(Type.Known.STRING).build(),
                        "answer", Schema.builder().type(Type.Known.STRING)
                                .maxLength((long) AiClient.MAX_CHAT_ANSWER_LENGTH).build(),
                        "recommendations", Schema.builder().type(Type.Known.ARRAY)
                                .maxItems(4L).items(recommendationSchema).build()))
                .required(List.of("action", "answer", "recommendations")).build();
        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(systemInstruction)))
                .responseMimeType("application/json")
                .responseSchema(schema)
                .candidateCount(1)
                .maxOutputTokens(2048)
                .thinkingConfig(ThinkingConfig.builder().thinkingLevel(ThinkingLevel.Known.LOW).build())
                .build();
        return request("CHAT", "Gemini chat request failed", () -> {
            var response = client.models.generateContent(MODEL, prompt, config);
            String finishReason = response.candidates().orElse(List.of()).stream().findFirst()
                    .flatMap(candidate -> candidate.finishReason()).map(Object::toString).orElse("STOP");
            String text = response.text();
            if (!"STOP".equals(finishReason) || text == null || text.isBlank()) {
                throw AiProviderException.invalidResponse("Gemini returned an incomplete chat response");
            }
            var usage = response.usageMetadata();
            return new ChatResult(null, text.strip(),
                    usage.flatMap(value -> value.promptTokenCount()).orElse(null),
                    usage.flatMap(value -> value.candidatesTokenCount()).orElse(null),
                    usage.flatMap(value -> value.totalTokenCount()).orElse(null));
        });
    }

    @Override
    public RecommendationResult recommendRecipeSequence(String prompt, List<Long> allowedRecipeIds, int slots) {
        return recommendSequence(prompt, allowedRecipeIds, slots);
    }

    private RecommendationResult recommendSequence(String prompt, List<Long> allowedRecipeIds, int limit) {
        Schema schema = Schema.builder().type(Type.Known.OBJECT)
                .properties(Map.of("recipeIds", Schema.builder().type(Type.Known.ARRAY)
                        .maxItems((long) limit).items(Schema.builder().type(Type.Known.INTEGER)).build()))
                .required(List.of("recipeIds")).build();
        GenerateContentConfig config = GenerateContentConfig.builder().responseMimeType("application/json")
                .responseSchema(schema).candidateCount(1).maxOutputTokens(2048)
                .thinkingConfig(ThinkingConfig.builder().thinkingLevel(ThinkingLevel.Known.LOW).build()).build();
        return request("RECOMMENDATIONS", "Gemini recommendation request failed", () -> {
            var response = client.models.generateContent(MODEL,
                    prompt + "\nChoose at most " + limit + " IDs from " + allowedRecipeIds, config);
            String finishReason = response.candidates().orElse(List.of()).stream().findFirst()
                    .flatMap(candidate -> candidate.finishReason()).map(Object::toString).orElse("STOP");
            if (!"STOP".equals(finishReason)) throw AiProviderException.invalidResponse("Gemini recommendation output is incomplete");
            String text = response.text();
            if (text == null || text.isBlank()) throw AiProviderException.invalidResponse("Gemini returned an empty response");
            var usage = response.usageMetadata();
            return new RecommendationResult(text,
                    usage.flatMap(value -> value.promptTokenCount()).orElse(null),
                    usage.flatMap(value -> value.candidatesTokenCount()).orElse(null),
                    usage.flatMap(value -> value.totalTokenCount()).orElse(null));
        });
    }

    @Override
    public RecommendationResult analyzeNutritionRecommendations(String prompt, List<Long> allowedRecipeIds, int limit) {
        Schema schema = Schema.builder().type(Type.Known.OBJECT)
                .properties(Map.of(
                        "analysis", Schema.builder().type(Type.Known.STRING).build(),
                        "recipeIds", Schema.builder().type(Type.Known.ARRAY).maxItems((long) limit)
                                .items(Schema.builder().type(Type.Known.INTEGER).build()).build()))
                .required(List.of("analysis", "recipeIds")).build();
        GenerateContentConfig config = GenerateContentConfig.builder().responseMimeType("application/json")
                .responseSchema(schema).candidateCount(1).maxOutputTokens(2048)
                .thinkingConfig(ThinkingConfig.builder().thinkingLevel(ThinkingLevel.Known.LOW).build()).build();
        return request("NUTRITION", "Gemini nutrition analysis failed", () -> {
            var response = client.models.generateContent(MODEL, prompt + "\nChoose at most " + limit
                    + " IDs from " + allowedRecipeIds, config);
            String finishReason = response.candidates().orElse(List.of()).stream().findFirst()
                    .flatMap(candidate -> candidate.finishReason()).map(Object::toString).orElse("STOP");
            if (!"STOP".equals(finishReason)) throw AiProviderException.invalidResponse("Gemini nutrition analysis is incomplete");
            String text = response.text();
            if (text == null || text.isBlank()) throw AiProviderException.invalidResponse("Gemini returned an empty nutrition analysis");
            var usage = response.usageMetadata();
            return new RecommendationResult(text, usage.flatMap(value -> value.promptTokenCount()).orElse(null),
                    usage.flatMap(value -> value.candidatesTokenCount()).orElse(null),
                    usage.flatMap(value -> value.totalTokenCount()).orElse(null));
        });
    }

    private <T> T request(String operation, String failureMessage, Supplier<T> action) {
        long started = System.nanoTime();
        try {
            if (client == null) throw new AiProviderException("Gemini is not configured", null,
                    FailureKind.CONFIGURATION, null);
            // Both attempts and their backoff share one deadline; SDK retries remain disabled.
            long deadline = started + TimeUnit.MILLISECONDS.toNanos(REQUEST_TIMEOUT_MS);
            var pending = requests.submit(() -> attemptWithBoundedRetry(operation, action, deadline));
            try {
                return pending.get(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            } catch (TimeoutException failure) {
                pending.cancel(true);
                throw new AiProviderException(failureMessage, null, FailureKind.TIMEOUT, null);
            } catch (InterruptedException failure) {
                pending.cancel(true);
                Thread.currentThread().interrupt();
                throw new AiProviderException(failureMessage, null, FailureKind.TRANSPORT, null);
            } catch (ExecutionException failure) {
                if (failure.getCause() instanceof RuntimeException runtime) throw runtime;
                if (failure.getCause() instanceof Error error) throw error;
                throw new AiProviderException(failureMessage, null, FailureKind.TRANSPORT, null);
            }
        } catch (RuntimeException failure) {
            AiProviderException mapped = failure instanceof AiProviderException provider ? provider
                    : new AiProviderException(failureMessage, failure, classify(failure), httpStatus(failure));
            // SDK exception messages/stack traces may contain prompts, credentials or response bodies.
            LOG.warn("Gemini request failed operation={} model={} category={} httpStatus={} elapsedMs={}",
                    operation, MODEL, mapped.kind(), mapped.httpStatus(), (System.nanoTime() - started) / 1_000_000);
            throw mapped;
        }
    }

    private <T> T attemptWithBoundedRetry(String operation, Supplier<T> action, long deadline) {
        for (int attempt = 1; ; attempt++) {
            try {
                return action.get();
            } catch (RuntimeException failure) {
                Integer status = httpStatus(failure);
                long delayMillis = 1_000 + ThreadLocalRandom.current().nextLong(251);
                boolean retryable = status != null && (status == 502 || status == 503 || status == 504);
                // Leave at least two seconds for a useful second attempt. Never retry quota/validation errors.
                if (attempt >= 2 || !retryable || Thread.currentThread().isInterrupted()
                        || deadline - System.nanoTime() <= TimeUnit.MILLISECONDS.toNanos(delayMillis + MIN_RETRY_BUDGET_MS)) {
                    throw failure;
                }
                LOG.info("Gemini transient failure; retrying operation={} model={} httpStatus={} attempt=2",
                        operation, MODEL, status);
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new AiProviderException("Gemini retry interrupted", null, FailureKind.TRANSPORT, null);
                }
                if (Thread.currentThread().isInterrupted()
                        || deadline - System.nanoTime() < TimeUnit.MILLISECONDS.toNanos(MIN_RETRY_BUDGET_MS)) {
                    throw failure;
                }
            }
        }
    }

    private static FailureKind classify(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof InterruptedIOException) return FailureKind.TIMEOUT;
            if (cause instanceof ApiException) return FailureKind.HTTP;
        }
        return FailureKind.TRANSPORT;
    }

    private static Integer httpStatus(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ApiException api) return api.code();
        }
        return null;
    }

    public enum FailureKind { CONFIGURATION, TIMEOUT, HTTP, TRANSPORT, INVALID_RESPONSE }

    public static final class AiProviderException extends RuntimeException {
        private final FailureKind kind;
        private final Integer httpStatus;

        public AiProviderException(String message) { this(message, null); }
        public AiProviderException(String message, Throwable cause) {
            this(message, cause, classify(cause), GeminiAiClient.httpStatus(cause));
        }
        private AiProviderException(String message, Throwable cause, FailureKind kind, Integer httpStatus) {
            super(message, cause);
            this.kind = kind;
            this.httpStatus = httpStatus;
        }
        public static AiProviderException invalidResponse(String message) {
            return new AiProviderException(message, null, FailureKind.INVALID_RESPONSE, null);
        }
        public FailureKind kind() { return kind; }
        public Integer httpStatus() { return httpStatus; }
    }
}

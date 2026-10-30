package tech.mamxanh.integration.ai;

import com.google.genai.Client;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GeminiAiClient implements AiClient, AutoCloseable {
    private static final String MODEL = "gemini-3.8-flash";
    static final int DEFAULT_TIMEOUT_MILLIS = 60_000;
    private final Client client;

    @Autowired
    public GeminiAiClient(@Value("${GEMINI_API_KEY:}") String apiKey) {
        this(apiKey, HttpOptions.builder().timeout(DEFAULT_TIMEOUT_MILLIS).build());
    }

    GeminiAiClient(String apiKey, HttpOptions httpOptions) {
        this.client = apiKey == null || apiKey.isBlank() ? null : Client.builder()
                .apiKey(apiKey.strip())
                .httpOptions(httpOptions)
                .build();
    }

    @Override
    public String generateRecipeText(String prompt, List<Long> enteredIngredientIds) {
        if (client == null) {
            throw new AiProviderException("Gemini is not configured");
        }
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
                .maxOutputTokens(8192)
                .build();
        try {
            var response = client.models.generateContent(MODEL,
                    prompt + "\nIngredient IDs entered by the author: " + enteredIngredientIds, config);
            String finishReason = response.candidates().orElse(List.of()).stream().findFirst()
                    .flatMap(candidate -> candidate.finishReason()).map(Object::toString).orElse("STOP");
            if (!"STOP".equals(finishReason)) {
                throw new AiProviderException("Gemini recipe output is incomplete: " + finishReason);
            }
            String text = response.text();
            if (text == null || text.isBlank()) throw new AiProviderException("Gemini returned an empty response");
            return text;
        } catch (AiProviderException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AiProviderException("Gemini request failed", exception);
        }
    }

    @Override
    public void close() {
        if (client != null) client.close();
    }

    @Override
    public RecommendationResult recommendRecipes(String prompt, List<Long> allowedRecipeIds, int limit) {
        return recommendSequence(prompt, allowedRecipeIds, limit);
    }

    @Override
    public ChatResult answerChat(String systemInstruction, String prompt) {
        if (client == null) throw new AiProviderException("Gemini is not configured");
        Schema recommendationSchema = Schema.builder().type(Type.Known.OBJECT)
                .properties(Map.of("recipeId", Schema.builder().type(Type.Known.INTEGER).build(),
                        "reason", Schema.builder().type(Type.Known.STRING).build()))
                .required(List.of("recipeId", "reason")).build();
        Schema schema = Schema.builder().type(Type.Known.OBJECT)
                .properties(Map.of("action", Schema.builder().type(Type.Known.STRING).build(),
                        "answer", Schema.builder().type(Type.Known.STRING).build(),
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
        try {
            var response = client.models.generateContent(MODEL, prompt, config);
            String finishReason = response.candidates().orElse(List.of()).stream().findFirst()
                    .flatMap(candidate -> candidate.finishReason()).map(Object::toString).orElse("STOP");
            String text = response.text();
            if (!"STOP".equals(finishReason) || text == null || text.isBlank()) {
                throw new AiProviderException("Gemini returned an incomplete chat response");
            }
            var usage = response.usageMetadata();
            return new ChatResult(null, text.strip(),
                    usage.flatMap(value -> value.promptTokenCount()).orElse(null),
                    usage.flatMap(value -> value.candidatesTokenCount()).orElse(null),
                    usage.flatMap(value -> value.totalTokenCount()).orElse(null));
        } catch (AiProviderException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AiProviderException("Gemini chat request failed", exception);
        }
    }

    @Override
    public RecommendationResult recommendRecipeSequence(String prompt, List<Long> allowedRecipeIds, int slots) {
        return recommendSequence(prompt, allowedRecipeIds, slots);
    }

    private RecommendationResult recommendSequence(String prompt, List<Long> allowedRecipeIds, int limit) {
        if (client == null) throw new AiProviderException("Gemini is not configured");
        Schema schema = Schema.builder().type(Type.Known.OBJECT)
                .properties(Map.of("recipeIds", Schema.builder().type(Type.Known.ARRAY)
                        .maxItems((long) limit).items(Schema.builder().type(Type.Known.INTEGER)).build()))
                .required(List.of("recipeIds")).build();
        GenerateContentConfig config = GenerateContentConfig.builder().responseMimeType("application/json")
                .responseSchema(schema).candidateCount(1).maxOutputTokens(2048)
                .thinkingConfig(ThinkingConfig.builder().thinkingLevel(ThinkingLevel.Known.LOW).build()).build();
        try {
            var response = client.models.generateContent(MODEL,
                    prompt + "\nChoose at most " + limit + " IDs from " + allowedRecipeIds, config);
            String finishReason = response.candidates().orElse(List.of()).stream().findFirst()
                    .flatMap(candidate -> candidate.finishReason()).map(Object::toString).orElse("STOP");
            if (!"STOP".equals(finishReason)) throw new AiProviderException("Gemini recommendation output is incomplete");
            String text = response.text();
            if (text == null || text.isBlank()) throw new AiProviderException("Gemini returned an empty response");
            var usage = response.usageMetadata();
            return new RecommendationResult(text,
                    usage.flatMap(value -> value.promptTokenCount()).orElse(null),
                    usage.flatMap(value -> value.candidatesTokenCount()).orElse(null),
                    usage.flatMap(value -> value.totalTokenCount()).orElse(null));
        } catch (AiProviderException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AiProviderException("Gemini recommendation request failed", exception);
        }
    }

    @Override
    public RecommendationResult analyzeNutritionRecommendations(String prompt, List<Long> allowedRecipeIds, int limit) {
        if (client == null) throw new AiProviderException("Gemini is not configured");
        Schema schema = Schema.builder().type(Type.Known.OBJECT)
                .properties(Map.of(
                        "analysis", Schema.builder().type(Type.Known.STRING).build(),
                        "recipeIds", Schema.builder().type(Type.Known.ARRAY).maxItems((long) limit)
                                .items(Schema.builder().type(Type.Known.INTEGER).build()).build()))
                .required(List.of("analysis", "recipeIds")).build();
        GenerateContentConfig config = GenerateContentConfig.builder().responseMimeType("application/json")
                .responseSchema(schema).candidateCount(1).maxOutputTokens(2048)
                .thinkingConfig(ThinkingConfig.builder().thinkingLevel(ThinkingLevel.Known.LOW).build()).build();
        try {
            var response = client.models.generateContent(MODEL, prompt + "\nChoose at most " + limit
                    + " IDs from " + allowedRecipeIds, config);
            String finishReason = response.candidates().orElse(List.of()).stream().findFirst()
                    .flatMap(candidate -> candidate.finishReason()).map(Object::toString).orElse("STOP");
            if (!"STOP".equals(finishReason)) throw new AiProviderException("Gemini nutrition analysis is incomplete");
            String text = response.text();
            if (text == null || text.isBlank()) throw new AiProviderException("Gemini returned an empty nutrition analysis");
            var usage = response.usageMetadata();
            return new RecommendationResult(text, usage.flatMap(value -> value.promptTokenCount()).orElse(null),
                    usage.flatMap(value -> value.candidatesTokenCount()).orElse(null),
                    usage.flatMap(value -> value.totalTokenCount()).orElse(null));
        } catch (AiProviderException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AiProviderException("Gemini nutrition analysis failed", exception);
        }
    }

    public static final class AiProviderException extends RuntimeException {
        public AiProviderException(String message) { super(message); }
        public AiProviderException(String message, Throwable cause) { super(message, cause); }
    }
}

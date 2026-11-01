package tech.mamxanh.integration.ai;

import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.integration.ai.GeminiAiClient.AiProviderException;
import tech.mamxanh.integration.ai.adk.AdkSingleStepTaskRunner;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.subscription.service.AiChatSettingsService;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Unified AI Client backed by the official Google ADK Java Runtime.
 * All generation and classification tasks run through LlmAgent + InMemoryRunner,
 * adhering to the G5 migration target architecture.
 */
@Component
@Primary
public class AdkAiClient implements AiClient, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AdkAiClient.class);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration CHAT_SCOPE_TIMEOUT = Duration.ofSeconds(6);
    private static final Duration CHAT_ANSWER_TIMEOUT = Duration.ofSeconds(7);

    private final AdkSingleStepTaskRunner taskRunner;
    private final AiPromptLoader promptLoader;
    private final GeminiAiClient fallbackClient;
    private final String migrationMode;
    private final CurrentUserService users;
    private final AiChatSettingsService chatSettings;

    @Autowired
    public AdkAiClient(AdkSingleStepTaskRunner taskRunner,
                       @Autowired(required = false) GeminiAiClient fallbackClient,
                       @Value("${mamxanh.ai.migration.mode:ADK_PRIMARY}") String migrationMode,
                       CurrentUserService users,
                       AiChatSettingsService chatSettings) {
        this.taskRunner = taskRunner;
        this.promptLoader = new AiPromptLoader();
        this.fallbackClient = fallbackClient;
        this.migrationMode = migrationMode != null ? migrationMode : "ADK_PRIMARY";
        this.users = users;
        this.chatSettings = chatSettings;
    }

    public AdkAiClient(AdkSingleStepTaskRunner taskRunner, GeminiAiClient fallbackClient, String migrationMode) {
        this(taskRunner, fallbackClient, migrationMode, null, null);
    }

    public AdkAiClient(AdkSingleStepTaskRunner taskRunner, GeminiAiClient fallbackClient) {
        this(taskRunner, fallbackClient, "ADK_PRIMARY");
    }

    private boolean isLegacyFallback() {
        return "LEGACY_FALLBACK".equalsIgnoreCase(migrationMode) && fallbackClient != null;
    }

    private Authentication currentAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private String selectedModel() {
        if (users == null || chatSettings == null) return taskRunner.model();
        Long userId = users.findAuthenticatedUserId().orElse(null);
        return userId == null ? taskRunner.model() : chatSettings.resolve(userId, null);
    }

    @Override
    public String generateRecipeText(String prompt, List<Long> enteredIngredientIds) {
        return generateRecipeContent(prompt, enteredIngredientIds).json();
    }

    @Override
    public RecommendationResult generateRecipeContent(String prompt, List<Long> enteredIngredientIds) {
        if (isLegacyFallback()) {
            return fallbackClient.generateRecipeContent(prompt, enteredIngredientIds);
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

        String fullPrompt = prompt + "\nIngredient IDs entered by the author: " + enteredIngredientIds;
        try {
            var result = taskRunner.executeTaskWithResult(
                    currentAuth(), selectedModel(), "RECIPE_TEXT", promptLoader.loadSystemPrompt(), fullPrompt,
                    schema, 8192, DEFAULT_TIMEOUT);

            if (result.text() == null || result.text().isBlank()) {
                throw AiProviderException.invalidResponse("ADK recipe output is incomplete");
            }
            var usage = result.usage();
            return new RecommendationResult(result.text(), usage.promptTokens(), usage.responseTokens(), usage.totalTokens(), usage);
        } catch (AppException | ApiException e) {
            throw e;
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            LOG.warn("ADK generateRecipeContent execution failed: {}", e.getMessage());
            throw new AiProviderException("ADK generateRecipeContent failed", e);
        }
    }

    @Override
    public RecommendationResult recommendRecipes(String prompt, List<Long> allowedRecipeIds, int limit) {
        return recommendRecipeSequence(prompt, allowedRecipeIds, limit);
    }

    @Override
    public RecommendationResult recommendRecipeSequence(String prompt, List<Long> allowedRecipeIds, int slots) {
        if (isLegacyFallback()) {
            return fallbackClient.recommendRecipeSequence(prompt, allowedRecipeIds, slots);
        }

        Schema schema = Schema.builder().type(Type.Known.OBJECT)
                .properties(Map.of("recipeIds", Schema.builder().type(Type.Known.ARRAY)
                        .maxItems((long) slots).items(Schema.builder().type(Type.Known.INTEGER)).build()))
                .required(List.of("recipeIds")).build();

        String fullPrompt = prompt + "\nChoose at most " + slots + " IDs from " + allowedRecipeIds;
        try {
            var result = taskRunner.executeTaskWithResult(
                    currentAuth(), selectedModel(), "RECOMMENDATIONS", promptLoader.loadSystemPrompt(), fullPrompt,
                    schema, 2048, DEFAULT_TIMEOUT);

            if (result.text() == null || result.text().isBlank()) {
                throw AiProviderException.invalidResponse("ADK recommendation output is incomplete");
            }
            var usage = result.usage();
            return new RecommendationResult(result.text(), usage.promptTokens(), usage.responseTokens(), usage.totalTokens(), usage);
        } catch (AppException | ApiException e) {
            throw e;
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            LOG.warn("ADK recommendRecipeSequence execution failed: {}", e.getMessage());
            throw new AiProviderException("ADK recommendation failed", e);
        }
    }

    @Override
    public RecommendationResult analyzeNutritionRecommendations(String prompt, List<Long> allowedRecipeIds, int limit) {
        if (isLegacyFallback()) {
            return fallbackClient.analyzeNutritionRecommendations(prompt, allowedRecipeIds, limit);
        }

        Schema schema = Schema.builder().type(Type.Known.OBJECT)
                .properties(Map.of(
                        "analysis", Schema.builder().type(Type.Known.STRING).build(),
                        "recipeIds", Schema.builder().type(Type.Known.ARRAY).maxItems((long) limit)
                                .items(Schema.builder().type(Type.Known.INTEGER).build()).build()))
                .required(List.of("analysis", "recipeIds")).build();

        String fullPrompt = prompt + "\nChoose at most " + limit + " IDs from " + allowedRecipeIds;
        try {
            var result = taskRunner.executeTaskWithResult(
                    currentAuth(), selectedModel(), "NUTRITION", promptLoader.loadSystemPrompt(), fullPrompt,
                    schema, 2048, DEFAULT_TIMEOUT);

            if (result.text() == null || result.text().isBlank()) {
                throw AiProviderException.invalidResponse("ADK nutrition analysis is incomplete");
            }
            var usage = result.usage();
            return new RecommendationResult(result.text(), usage.promptTokens(), usage.responseTokens(), usage.totalTokens(), usage);
        } catch (AppException | ApiException e) {
            throw e;
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            LOG.warn("ADK analyzeNutritionRecommendations execution failed: {}", e.getMessage());
            throw new AiProviderException("ADK nutrition analysis failed", e);
        }
    }

    @Override
    public ChatResult classifyChatScope(String systemInstruction, String prompt) {
        return classifyChatScope(taskRunner.model(), systemInstruction, prompt);
    }

    @Override
    public ChatResult classifyChatScope(String modelId, String systemInstruction, String prompt) {
        if (isLegacyFallback()) {
            return fallbackClient.classifyChatScope(modelId, systemInstruction, prompt);
        }

        Schema schema = Schema.builder().type(Type.Known.OBJECT)
                .properties(Map.of("decisionId", Schema.builder().type(Type.Known.STRING).build(),
                        "reasonId", Schema.builder().type(Type.Known.STRING).build()))
                .required(List.of("decisionId", "reasonId")).build();

        try {
            var result = taskRunner.executeTaskWithResult(
                    currentAuth(), modelId, "CHAT_SCOPE", systemInstruction, prompt,
                    schema, 128, CHAT_SCOPE_TIMEOUT);

            String text = result.text();
            var usage = result.usage();
            if (text == null || text.isBlank()) {
                LOG.warn("ADK chat scope response incomplete; returning fail-closed classification");
                return new ChatResult(null,
                        "{\"decisionId\":\"MX_CHAT_SENSITIVE\",\"reasonId\":\"OTHER_HIGH_RISK\"}",
                        usage.promptTokens(), usage.responseTokens(), usage.totalTokens(), usage);
            }
            return new ChatResult(null, text.strip(), usage.promptTokens(), usage.responseTokens(), usage.totalTokens(), usage);
        } catch (Exception e) {
            LOG.warn("ADK chat scope classification failed: {}", e.getMessage());
            var emptyUsage = new AiTokenUsage(modelId != null ? modelId : taskRunner.model(), "STANDARD", null, null, null, null, null, null);
            return new ChatResult(null,
                    "{\"decisionId\":\"MX_CHAT_SENSITIVE\",\"reasonId\":\"OTHER_HIGH_RISK\"}",
                    null, null, null, emptyUsage);
        }
    }

    @Override
    public ChatResult answerChat(String systemInstruction, String prompt) {
        return answerChat(taskRunner.model(), systemInstruction, prompt);
    }

    @Override
    public ChatResult answerChat(String modelId, String systemInstruction, String prompt) {
        if (isLegacyFallback()) {
            return fallbackClient.answerChat(modelId, systemInstruction, prompt);
        }

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

        try {
            var result = taskRunner.executeTaskWithResult(
                    currentAuth(), modelId, "CHAT", systemInstruction, prompt,
                    schema, 2048, CHAT_ANSWER_TIMEOUT);

            String text = result.text();
            if (text == null || text.isBlank()) {
                throw AiProviderException.invalidResponse("ADK returned an incomplete chat response");
            }
            var usage = result.usage();
            return new ChatResult(null, text.strip(), usage.promptTokens(), usage.responseTokens(), usage.totalTokens(), usage);
        } catch (AppException e) {
            throw e;
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            LOG.warn("ADK answerChat execution failed: {}", e.getMessage());
            throw new AiProviderException("ADK chat request failed", e);
        }
    }

    @Override
    public ChatResult planAssistantTools(String systemInstruction, String prompt) {
        return planAssistantTools(taskRunner.model(), systemInstruction, prompt);
    }

    @Override
    public ChatResult planAssistantTools(String modelId, String systemInstruction, String prompt) {
        if (isLegacyFallback()) {
            return fallbackClient.planAssistantTools(modelId, systemInstruction, prompt);
        }

        Schema toolCallSchema = Schema.builder().type(Type.Known.OBJECT)
                .properties(Map.of("name", Schema.builder().type(Type.Known.STRING).build(),
                        "arguments", Schema.builder().type(Type.Known.OBJECT)
                                .properties(Map.of("page", Schema.builder().type(Type.Known.STRING)
                                        .enum_("HOME", "EXPLORE", "MEAL_PLAN", "SHOPPING_LIST", "AI_PLANS",
                                                "AI_CREDIT_TOP_UP", "PROFILE", "DIETARY_PREFERENCES", "HEALTH_PROFILE",
                                                "RECIPE_CREATE", "EXPERT_DIRECTORY", "COMMUNITY", "TRANSACTION_HISTORY")
                                        .build()))
                                .build()))
                .required(List.of("name", "arguments")).build();
        Schema schema = Schema.builder().type(Type.Known.OBJECT)
                .properties(Map.of("answer", Schema.builder().type(Type.Known.STRING)
                                .maxLength((long) AiClient.MAX_CHAT_ANSWER_LENGTH).build(),
                        "toolCalls", Schema.builder().type(Type.Known.ARRAY).maxItems(5L).items(toolCallSchema).build()))
                .required(List.of("answer", "toolCalls")).build();

        try {
            var result = taskRunner.executeTaskWithResult(
                    currentAuth(), modelId, "CHAT", systemInstruction, prompt,
                    schema, 2048, CHAT_ANSWER_TIMEOUT);

            String text = result.text();
            if (text == null || text.isBlank()) {
                throw AiProviderException.invalidResponse("ADK returned an incomplete tool plan");
            }
            var usage = result.usage();
            return new ChatResult(null, text.strip(), usage.promptTokens(), usage.responseTokens(), usage.totalTokens(), usage);
        } catch (AppException e) {
            throw e;
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            LOG.warn("ADK planAssistantTools execution failed: {}", e.getMessage());
            throw new AiProviderException("ADK tool planning request failed", e);
        }
    }

    @Override
    public void close() {
        if (fallbackClient != null) {
            fallbackClient.close();
        }
    }
}

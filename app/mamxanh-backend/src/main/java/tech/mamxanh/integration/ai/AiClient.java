package tech.mamxanh.integration.ai;

import java.util.List;
import java.time.Duration;

public interface AiClient {
    /** Maximum number of characters allowed in an AI chat answer (and stored assistant turn). */
    int MAX_CHAT_ANSWER_LENGTH = 8_000;

    /**
     * @deprecated Use {@link #generateRecipeContent(String, List)} instead to capture full token telemetry.
     */
    @Deprecated(since = "G5", forRemoval = false)
    default String generateRecipeText(String prompt, List<Long> enteredIngredientIds) {
        RecommendationResult result = generateRecipeContent(prompt, enteredIngredientIds);
        return result != null ? result.json() : null;
    }

    RecommendationResult generateRecipeContent(String prompt, List<Long> enteredIngredientIds);

    RecommendationResult recommendRecipes(String prompt, List<Long> allowedRecipeIds, int limit);

    RecommendationResult analyzeNutritionRecommendations(String prompt, List<Long> allowedRecipeIds, int limit);

    /** Classifies the latest chat request without generating a user-facing answer. */
    ChatResult classifyChatScope(String systemInstruction, String prompt);

    default ChatResult classifyChatScope(String systemInstruction, String prompt, Duration timeout) {
        return classifyChatScope(systemInstruction, prompt);
    }

    default ChatResult classifyChatScope(String modelId, String systemInstruction, String prompt) {
        return classifyChatScope(systemInstruction, prompt);
    }

    /** Classifies within the caller's remaining monotonic-turn budget when supported. */
    default ChatResult classifyChatScope(String modelId, String systemInstruction, String prompt, Duration timeout) {
        return classifyChatScope(modelId, systemInstruction, prompt);
    }

    ChatResult answerChat(String systemInstruction, String prompt);
    default ChatResult answerChat(String modelId, String systemInstruction, String prompt) {
        return answerChat(systemInstruction, prompt);
    }
    default ChatResult planAssistantTools(String systemInstruction, String prompt) {
        return answerChat(systemInstruction, prompt);
    }
    default ChatResult planAssistantTools(String modelId, String systemInstruction, String prompt) {
        return planAssistantTools(systemInstruction, prompt);
    }
    /** Selects an ordered sequence; IDs may repeat when the caller explicitly permits it. */
    default RecommendationResult recommendRecipeSequence(String prompt, List<Long> allowedRecipeIds, int slots) {
        return recommendRecipes(prompt, allowedRecipeIds, slots);
    }

    record RecommendationResult(String json, Integer promptTokens, Integer responseTokens, Integer totalTokens,
            AiTokenUsage usage) {
        public RecommendationResult(String json, Integer promptTokens, Integer responseTokens, Integer totalTokens) {
            this(json, promptTokens, responseTokens, totalTokens,
                    AiTokenUsage.legacy(promptTokens, responseTokens, totalTokens));
        }
    }
    record ChatResult(String answer, String structuredJson, Integer promptTokens,
            Integer responseTokens, Integer totalTokens, AiTokenUsage usage) {
        public ChatResult(String answer, String structuredJson, Integer promptTokens,
                Integer responseTokens, Integer totalTokens) {
            this(answer, structuredJson, promptTokens, responseTokens, totalTokens,
                    AiTokenUsage.legacy(promptTokens, responseTokens, totalTokens));
        }
        public ChatResult(String answer, Integer promptTokens, Integer responseTokens, Integer totalTokens) {
            this(answer, null, promptTokens, responseTokens, totalTokens);
        }
    }
}

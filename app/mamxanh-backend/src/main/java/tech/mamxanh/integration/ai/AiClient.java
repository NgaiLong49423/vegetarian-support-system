package tech.mamxanh.integration.ai;

import java.util.List;

public interface AiClient {
    /** Maximum number of characters allowed in an AI chat answer (and stored assistant turn). */
    int MAX_CHAT_ANSWER_LENGTH = 8_000;
    String generateRecipeText(String prompt, List<Long> enteredIngredientIds);

    default RecommendationResult generateRecipeContent(String prompt, List<Long> enteredIngredientIds) {
        return new RecommendationResult(generateRecipeText(prompt, enteredIngredientIds), null, null, null);
    }

    RecommendationResult recommendRecipes(String prompt, List<Long> allowedRecipeIds, int limit);

    RecommendationResult analyzeNutritionRecommendations(String prompt, List<Long> allowedRecipeIds, int limit);

    /** Classifies the latest chat request without generating a user-facing answer. */
    ChatResult classifyChatScope(String systemInstruction, String prompt);

    default ChatResult classifyChatScope(String modelId, String systemInstruction, String prompt) {
        return classifyChatScope(systemInstruction, prompt);
    }

    ChatResult answerChat(String systemInstruction, String prompt);
    default ChatResult answerChat(String modelId, String systemInstruction, String prompt) {
        return answerChat(systemInstruction, prompt);
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

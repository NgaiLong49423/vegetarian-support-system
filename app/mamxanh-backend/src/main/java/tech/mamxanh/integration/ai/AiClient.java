package tech.mamxanh.integration.ai;

import java.util.List;

public interface AiClient {
    int MAX_CHAT_ANSWER_LENGTH = 8_000;
    RecommendationResult generateRecipeDraft(String prompt, List<Long> allowedIngredientIds);

    String generateRecipeText(String prompt, List<Long> allowedIngredientIds);

    RecommendationResult recommendRecipes(String prompt, List<Long> allowedRecipeIds, int limit);

    RecommendationResult analyzeNutritionRecommendations(String prompt, List<Long> allowedRecipeIds, int limit);

    ChatResult answerChat(String systemInstruction, String prompt);
    /** Selects an ordered sequence; IDs may repeat when the caller explicitly permits it. */
    default RecommendationResult recommendRecipeSequence(String prompt, List<Long> allowedRecipeIds, int slots) {
        return recommendRecipes(prompt, allowedRecipeIds, slots);
    }

    record RecommendationResult(String json, Integer promptTokens, Integer responseTokens, Integer totalTokens) { }
    record ChatResult(String answer, String structuredJson, Integer promptTokens,
            Integer responseTokens, Integer totalTokens) {
        public ChatResult(String answer, Integer promptTokens, Integer responseTokens, Integer totalTokens) {
            this(answer, null, promptTokens, responseTokens, totalTokens);
        }
    }
}

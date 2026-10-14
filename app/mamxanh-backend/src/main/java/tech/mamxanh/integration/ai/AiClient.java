package tech.mamxanh.integration.ai;

import java.util.List;

public interface AiClient {
    String generateRecipeText(String prompt, List<Long> allowedIngredientIds);

    RecommendationResult recommendRecipes(String prompt, List<Long> allowedRecipeIds, int limit);

    ChatResult answerChat(String systemInstruction, String prompt);

    /** Selects an ordered sequence; IDs may repeat when the caller explicitly permits it. */
    default RecommendationResult recommendRecipeSequence(String prompt, List<Long> allowedRecipeIds, int slots) {
        return recommendRecipes(prompt, allowedRecipeIds, slots);
    }

    record RecommendationResult(String json, Integer promptTokens, Integer responseTokens, Integer totalTokens) { }
    record ChatResult(String answer, Integer promptTokens, Integer responseTokens, Integer totalTokens) { }
}

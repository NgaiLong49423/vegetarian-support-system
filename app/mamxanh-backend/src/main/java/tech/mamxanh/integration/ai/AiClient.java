package tech.mamxanh.integration.ai;

import java.util.List;

public interface AiClient {
    String generateRecipeText(String prompt, List<Long> allowedIngredientIds);

    RecommendationResult recommendRecipes(String prompt, List<Long> allowedRecipeIds, int limit);

    RecommendationResult analyzeNutritionRecommendations(String prompt, List<Long> allowedRecipeIds, int limit);

    record RecommendationResult(String json, Integer promptTokens, Integer responseTokens, Integer totalTokens) { }
}

package tech.mamxanh.mealplan.dto.response;

import java.util.List;

public record MealPlanNutritionRecommendationsResponse(long entryId, String analysis, List<Recommendation> recommendations) {
    public record Recommendation(long recipeId, String title, boolean complete, List<String> ingredientsMissingData) { }
}

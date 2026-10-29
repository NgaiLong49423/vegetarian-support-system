package tech.mamxanh.recipe.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** Preview only. Ingredient IDs/units and calorie calculations come from the catalog. */
public record RecipeAiDraftResponse(String title, String description, String instructions,
        String dishCategory, String vegetarianType, String difficulty, int servings,
        int prepTimeMinutes, int cookTimeMinutes, String mealType, int targetCaloriesPerServing,
        BigDecimal totalCalories, BigDecimal caloriesPerServing, List<Item> ingredients) {
    @io.swagger.v3.oas.annotations.media.Schema(name = "RecipeAiDraftIngredient")
    public record Item(long ingredientId, int unitId, String name, BigDecimal quantity,
            String unitCode, String unitName, BigDecimal calories, BigDecimal energyKcalPer100g,
            String sourceName) {
        @Override public String toString() { return "RecipeAiDraftIngredient[redacted]"; }
    }
    @Override public String toString() { return "RecipeAiDraftResponse[redacted]"; }
}

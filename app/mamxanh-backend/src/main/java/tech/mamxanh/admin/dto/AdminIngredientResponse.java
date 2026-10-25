package tech.mamxanh.admin.dto;

import java.time.LocalDate;
import tech.mamxanh.nutrition.dto.response.IngredientNutritionResponse;
import tech.mamxanh.nutrition.dto.response.IngredientResponse;

/** Administrator-only ingredient view with recipe usage impact. */
public record AdminIngredientResponse(Long id, String name, String ingredientGroup, String sourceName,
        String imageUrl, String sourceUrl, LocalDate referenceDate, boolean nutritionSupported, boolean active,
        IngredientNutritionResponse nutrition, long recipeUsageCount) {
    public static AdminIngredientResponse from(IngredientResponse ingredient, long recipeUsageCount) {
        return new AdminIngredientResponse(ingredient.id(), ingredient.name(), ingredient.ingredientGroup(),
                ingredient.sourceName(), ingredient.imageUrl(), ingredient.sourceUrl(), ingredient.referenceDate(),
                ingredient.nutritionSupported(), ingredient.active(), ingredient.nutrition(), recipeUsageCount);
    }
}

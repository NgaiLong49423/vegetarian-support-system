package tech.mamxanh.recipe.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecipeAiDraftRequest(
        @NotBlank @Size(min = 3, max = 120) String title,
        @NotBlank @Size(max = 2000) String rawIngredients,
        @NotNull MealType mealType,
        @NotNull @Min(100) @Max(1500) Integer targetCaloriesPerServing,
        @NotNull @Min(1) @Max(50) Integer servings,
        @NotNull CookingMethod cookingMethod) {
    @io.swagger.v3.oas.annotations.media.Schema(enumAsRef = true, name = "RecipeAiDraftMealType")
    public enum MealType { BREAKFAST, LUNCH, DINNER, SNACK, CUSTOM }
    @io.swagger.v3.oas.annotations.media.Schema(enumAsRef = true, name = "RecipeAiDraftCookingMethod")
    public enum CookingMethod { STIR_FRY, SOUP, BRAISED, BOILED, SALAD }
    public RecipeAiDraftRequest {
        title = title == null ? null : title.strip();
        rawIngredients = rawIngredients == null ? null : rawIngredients.strip();
    }
    @Override public String toString() { return "RecipeAiDraftRequest[redacted]"; }
}

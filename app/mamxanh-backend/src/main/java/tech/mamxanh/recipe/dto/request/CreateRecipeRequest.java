package tech.mamxanh.recipe.dto.request;

import java.util.List;
import tech.mamxanh.recipe.entity.RecipeCodes.Difficulty;
import tech.mamxanh.recipe.entity.RecipeCodes.DishCategory;
import tech.mamxanh.recipe.entity.RecipeCodes.VegetarianType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRecipeRequest(
        @NotBlank @Size(min = 3, max = 120) String title,
        String description,
        @NotBlank @Size(min = 10, max = 5000) String instructions,
        @NotNull DishCategory dishCategory,
        @NotNull VegetarianType vegetarianType,
        @NotNull Difficulty difficulty,
        @NotNull @Min(1) @Max(50) Integer servings,
        @NotNull @Min(0) @Max(1440) Integer prepTimeMinutes,
        @NotNull @Min(0) @Max(1440) Integer cookTimeMinutes,
        @Size(max = 2048) String youtubeUrl,
        @NotNull @Size(min = 1, max = 50) List<@Valid RecipeIngredientInput> ingredients,
        @Size(max = 5) List<@Valid RecipeMediaInput> media) {
}

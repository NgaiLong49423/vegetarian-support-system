package tech.mamxanh.recipe.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RecipeDetailResponse(
        Long recipeId,
        String title,
        String description,
        String instructions,
        String dishCategory,
        String dishCategoryLabel,
        String vegetarianType,
        String vegetarianTypeLabel,
        String difficulty,
        String difficultyLabel,
        Integer servings,
        Integer prepTimeMinutes,
        Integer cookTimeMinutes,
        String youtubeUrl,
        LocalDateTime publishedAt,
        List<Ingredient> ingredients,
        boolean nutritionComplete,
        List<String> ingredientsWithoutNutrition,
        List<Media> media) {

    public record Ingredient(Long ingredientId, String name, BigDecimal quantity, Integer unitId,
            String unitCode, String unitName) {}

    public record Media(String blobUrl, String mimeType, Integer displayOrder, boolean cover) {}
}

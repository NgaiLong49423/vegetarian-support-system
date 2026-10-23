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
        Author author,
        List<Ingredient> ingredients,
        boolean nutritionComplete,
        List<String> ingredientsWithoutNutrition,
        Nutrition nutrition,
        Statistics statistics,
        List<Media> media) {

    public record Ingredient(Long ingredientId, String name, String ingredientGroup, String imageUrl,
            BigDecimal quantity, Integer unitId, String unitCode, String unitName,
            boolean nutritionSupported, IngredientNutrition nutrition) {}

    public record IngredientNutrition(BigDecimal energyKcalPer100g, BigDecimal proteinGramsPer100g,
            BigDecimal carbohydrateGramsPer100g, BigDecimal totalFatGramsPer100g,
            BigDecimal fiberGramsPer100g, BigDecimal calciumMgPer100g, BigDecimal ironMgPer100g,
            BigDecimal vitaminB12MicrogramsPer100g, BigDecimal zincMgPer100g) {}

    public record Author(Long userId, String displayName, String avatarUrl) { }

    public record Nutrition(boolean complete, List<String> ingredientsMissingData,
            java.util.Map<String, BigDecimal> total, java.util.Map<String, BigDecimal> perServing) { }

    public record Statistics(long likes, long dislikes, long reactionCount, BigDecimal likePercentage, long viewCount) { }

    public record Media(String blobUrl, String mimeType, Integer displayOrder, boolean cover) {}
}

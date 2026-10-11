package tech.mamxanh.recipe.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record RecipePostResponse(
        long id,
        long authorId,
        String authorName,
        String authorAvatarUrl,
        String title,
        String description,
        String instructions,
        String dishCategory,
        String vegetarianType,
        String difficulty,
        int servings,
        int prepTimeMin,
        int cookTimeMin,
        String youtubeUrl,
        String status,
        List<Media> media,
        List<Ingredient> ingredients,
        long likes,
        long dislikes,
        BigDecimal likePercentage,
        long viewCount) {

    public record Media(String url, String mimeType, int displayOrder, boolean cover) { }
    public record Ingredient(Long ingredientId, String name, String customName, int unitId,
            String unitCode, String unitName, BigDecimal quantity) { }
}

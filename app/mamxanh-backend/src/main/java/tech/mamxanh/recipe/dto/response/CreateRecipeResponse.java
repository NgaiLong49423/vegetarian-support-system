package tech.mamxanh.recipe.dto.response;

import java.time.LocalDateTime;

public record CreateRecipeResponse(Long recipeId, String title, String status, LocalDateTime publishedAt) {
}

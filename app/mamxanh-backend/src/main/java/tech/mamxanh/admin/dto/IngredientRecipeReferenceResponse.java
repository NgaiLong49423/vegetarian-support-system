package tech.mamxanh.admin.dto;

import java.time.LocalDateTime;

public record IngredientRecipeReferenceResponse(long recipeId, String title, String status, LocalDateTime publishedAt) {
}

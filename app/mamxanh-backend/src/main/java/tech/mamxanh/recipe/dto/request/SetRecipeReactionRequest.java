package tech.mamxanh.recipe.dto.request;

import jakarta.validation.constraints.NotNull;
import tech.mamxanh.recipe.entity.RecipeReactionType;

public record SetRecipeReactionRequest(
        @NotNull(message = "Chọn LIKE hoặc DISLIKE.") RecipeReactionType reactionType) {
}

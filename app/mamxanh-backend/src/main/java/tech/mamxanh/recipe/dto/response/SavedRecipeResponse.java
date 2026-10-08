package tech.mamxanh.recipe.dto.response;

import java.time.LocalDateTime;

public record SavedRecipeResponse(long recipeId, String title, String coverUrl, String authorName,
        boolean available, String unavailableMessage, LocalDateTime savedAt) { }

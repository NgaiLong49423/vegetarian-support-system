package tech.mamxanh.recipe.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record AiWeeklyMenuPreviewResponse(List<Slot> slots) {
    public record Slot(int dayOffset, String mealType, long recipeId, String recipeTitle,
            @Schema(nullable = true) String coverUrl, String dishCategory, String vegetarianType, String reason) { }
}

package tech.mamxanh.recipe.dto.response;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

public record AiWeeklyMenuPreviewResponse(List<Slot> slots) {
    public record Slot(int dayOffset, String mealType, long recipeId, String recipeTitle,
            @Schema(nullable = true) String coverUrl, String dishCategory, String vegetarianType, String reason) { }
}

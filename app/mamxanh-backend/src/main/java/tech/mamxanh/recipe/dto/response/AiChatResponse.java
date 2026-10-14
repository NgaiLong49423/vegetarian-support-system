package tech.mamxanh.recipe.dto.response;

import tech.mamxanh.recipe.dto.request.AiChatRequest.ContextType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record AiChatResponse(ContextType contextType,
        @Schema(description = "Null for GENERAL context.") Long recipeId,
        @Schema(description = "AI answer or explicitly requested draft. Treat as reference information.") String answer,
        @Schema(description = "Real public RecipePosts selected from the Backend catalog; empty when the user did not ask to find recipes.")
        List<RecipeRecommendation> recommendations) {

    public record RecipeRecommendation(long id, String title, String description, String coverUrl,
            int totalTimeMinutes, String difficulty, String reason) { }
}

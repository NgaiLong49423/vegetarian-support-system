package tech.mamxanh.recipe.dto.response;

import tech.mamxanh.recipe.dto.request.AiChatRequest.ContextType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import tech.mamxanh.integration.ai.AiClient;

public record AiChatResponse(ContextType contextType,
        @Schema(description = "Null for GENERAL context.") Long recipeId,
        @Schema(maxLength = AiClient.MAX_CHAT_ANSWER_LENGTH,
                description = "AI answer or explicitly requested draft. Treat as reference information.") String answer,
        @Schema(description = "Real public RecipePosts selected from the Backend catalog; empty when the user did not ask to find recipes.")
        List<RecipeRecommendation> recommendations,
        @Schema(description = "Optional local workflow link. Routing guidance is returned without a Gemini call; currently /ke-hoach?view=for-you or /dang-cong-thuc.")
        String workflowUrl) {

    public AiChatResponse(ContextType contextType, Long recipeId, String answer, List<RecipeRecommendation> recommendations) {
        this(contextType, recipeId, answer, recommendations, null);
    }

    @Override
    public String toString() { return "AiChatResponse[redacted]"; }

    public record RecipeRecommendation(long id, String title, String description, String coverUrl,
            int totalTimeMinutes, String difficulty, String reason) { }
}

package tech.mamxanh.recipe.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import tech.mamxanh.integration.ai.AiClient;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ConsentScope;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ContextType;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ExecutionMode;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AiChatResponse(ContextType contextType,
        @Schema(description = "Null for GENERAL context.") Long recipeId,
        @Schema(maxLength = AiClient.MAX_CHAT_ANSWER_LENGTH,
                description = "AI answer or explicitly requested draft. Treat as reference information.") String answer,
        @Schema(description = "Real public RecipePosts selected from the Backend catalog; empty when the user did not ask to find recipes.")
        List<RecipeRecommendation> recommendations,
        @Schema(description = "Optional local workflow link. Routing guidance is returned without a Gemini call; currently /ke-hoach?view=for-you or /dang-cong-thuc.")
        String workflowUrl,
        @Schema(description = "Opaque Backend-managed conversation session; send it with the next request.") UUID sessionId,
        ExecutionMode executionMode,
        UUID runId,
        List<AssistantStep> steps,
        ConsentRequest consentRequest,
        PendingConfirmation pendingConfirmation,
        List<Source> sources) {

    public AiChatResponse(ContextType contextType, Long recipeId, String answer, List<RecipeRecommendation> recommendations) {
        this(contextType, recipeId, answer, recommendations, null, null, ExecutionMode.CONFIRM,
                null, List.of(), null, null, List.of());
    }

    public AiChatResponse(ContextType contextType, Long recipeId, String answer,
            List<RecipeRecommendation> recommendations, String workflowUrl) {
        this(contextType, recipeId, answer, recommendations, workflowUrl, null, ExecutionMode.CONFIRM,
                null, List.of(), null, null, List.of());
    }

    @Override
    public String toString() { return "AiChatResponse[redacted]"; }

    public record RecipeRecommendation(long id, String title, String description, String coverUrl,
            int totalTimeMinutes, String difficulty, String reason) { }

    public record AssistantStep(int sequence, String tool, String status, String summary,
            Map<String, Object> result) { }

    public record ConsentRequest(List<ConsentScope> scopes, String recipient) { }

    public record PendingConfirmation(UUID confirmationId, String summary, Instant expiresAt,
            boolean requiresExplicitConfirmation) { }

    public record Source(String type, String id, String title, String excerpt, String uri) { }
}

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
        List<Source> sources,
        @Schema(description = "Active chat engine for this session (LEGACY_ASSISTANT or ADK_SHOPPING_PILOT).") String engine,
        @Schema(description = "Overall run status (COMPLETED, AWAITING_CONSENT, AWAITING_INPUT, AWAITING_CONFIRMATION, PARTIAL_FAILURE, REJECTED, FAILED).") String runStatus,
        @Schema(description = "Session inactivity expiration timestamp.") Instant sessionExpiresAt,
        @Schema(description = "Knowledge catalog/index revision used by the pilot.") String knowledgeRevision) {

    public AiChatResponse(ContextType contextType, Long recipeId, String answer, List<RecipeRecommendation> recommendations) {
        this(contextType, recipeId, answer, recommendations, null, null, ExecutionMode.CONFIRM,
                null, List.of(), null, null, List.of(), "LEGACY_ASSISTANT", "COMPLETED", null, null);
    }

    public AiChatResponse(ContextType contextType, Long recipeId, String answer,
            List<RecipeRecommendation> recommendations, String workflowUrl) {
        this(contextType, recipeId, answer, recommendations, workflowUrl, null, ExecutionMode.CONFIRM,
                null, List.of(), null, null, List.of(), "LEGACY_ASSISTANT", "COMPLETED", null, null);
    }

    public AiChatResponse(ContextType contextType, Long recipeId, String answer,
            List<RecipeRecommendation> recommendations, String workflowUrl, UUID sessionId,
            ExecutionMode executionMode, UUID runId, List<AssistantStep> steps,
            ConsentRequest consentRequest, PendingConfirmation pendingConfirmation, List<Source> sources) {
        this(contextType, recipeId, answer, recommendations, workflowUrl, sessionId, executionMode,
                runId, steps, consentRequest, pendingConfirmation, sources,
                "LEGACY_ASSISTANT",
                consentRequest != null ? "AWAITING_CONSENT"
                        : pendingConfirmation != null ? "AWAITING_CONFIRMATION"
                        : steps != null && steps.stream().anyMatch(s -> "FAILED".equals(s.status())) ? "FAILED" : "COMPLETED",
                null, null);
    }

    @Override
    public String toString() { return "AiChatResponse[redacted]"; }

    public record RecipeRecommendation(long id, String title, String description, String coverUrl,
            int totalTimeMinutes, String difficulty, String reason) { }

    public record AssistantStep(int sequence, String tool, String status, String summary,
            Map<String, Object> result) { }

    public record ConsentRequest(List<ConsentScope> scopes, String recipient) { }

    public record PendingConfirmation(UUID confirmationId, String summary, Instant expiresAt,
            boolean requiresExplicitConfirmation,
            @Schema(description = "Preview of the normalized Shopping List name pending confirmation.") String proposedListName) {
        public PendingConfirmation(UUID confirmationId, String summary, Instant expiresAt,
                boolean requiresExplicitConfirmation) {
            this(confirmationId, summary, expiresAt, requiresExplicitConfirmation, null);
        }
    }

    public record Source(String type, String id, String title, String excerpt, String uri,
            @Schema(description = "Version of the registered knowledge source, when applicable.") String version) {
        public Source(String type, String id, String title, String excerpt, String uri) {
            this(type, id, title, excerpt, uri, null);
        }
    }
}

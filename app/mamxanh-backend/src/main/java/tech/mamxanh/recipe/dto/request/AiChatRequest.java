package tech.mamxanh.recipe.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import tech.mamxanh.integration.ai.AiClient;

import java.util.List;
import java.util.UUID;

public record AiChatRequest(
        @NotNull @Schema(description = "GENERAL for cooking questions; RECIPE for a server-loaded public RecipePost.") ContextType contextType,
        @Min(1) @Schema(description = "Required for RECIPE context and omitted for GENERAL context.") Long recipeId,
        @NotBlank @Size(max = 1000) @Schema(maxLength = 1000) String question,
        @Size(max = 8) @Schema(description = "Most recent conversation turns, up to eight items, kept only for this request.")
        List<@Valid Turn> history,
        @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
        @Schema(description = "Session-cookie UUID required for Guest rate limiting; ignored for authenticated Members.")
        String anonymousViewerId,
        @Schema(description = "Opaque AI conversation session identifier returned by the Backend.") UUID sessionId,
        @Schema(description = "Execution mode for this request. Defaults to CONFIRM when omitted.") ExecutionMode executionMode,
        @Valid ConsentDecision consentDecision,
        @Schema(description = "Server-issued confirmation ID for continuing or rejecting a pending write plan.") UUID confirmationId,
        @Schema(description = "Explicit decision for the pending write plan.") ConfirmationDecision confirmationDecision) {

    public AiChatRequest(ContextType contextType, Long recipeId, String question, List<Turn> history) {
        this(contextType, recipeId, question, history, null, null, null, null, null, null);
    }

    public AiChatRequest(ContextType contextType, Long recipeId, String question, List<Turn> history,
            String anonymousViewerId) {
        this(contextType, recipeId, question, history, anonymousViewerId, null, null, null, null, null);
    }

    @Override
    public String toString() { return "AiChatRequest[redacted]"; }

    public enum ContextType { GENERAL, RECIPE }
    public enum ExecutionMode { QA, CONFIRM, AUTO }
    public enum ConsentAction { GRANT, REVOKE }
    public enum ConsentCategory { BASIC_PROFILE, DIETARY_PREFERENCES, HEALTH_PROFILE }
    public enum ConsentPurpose { AI_CHAT, PROFILE_READ, PROFILE_UPDATE, DIETARY_READ, DIETARY_UPDATE, HEALTH_PROFILE_READ, HEALTH_PROFILE_UPDATE }
    public enum ConfirmationDecision { APPROVE, REJECT }
    public enum Speaker { USER, ASSISTANT }

    public record ConsentDecision(@NotNull ConsentAction action,
            @Size(max = 7) List<@Valid ConsentScope> scopes) {
        @AssertTrue(message = "Phạm vi consent phải ghép đúng loại dữ liệu và mục đích.")
        @JsonIgnore
        @Schema(hidden = true)
        public boolean isScopeCombinationValid() {
            if (action == null) return true;
            if (action == ConsentAction.GRANT && (scopes == null || scopes.isEmpty())) return false;
            if (scopes == null) return true;
            return scopes.stream().allMatch(scope -> scope != null && scope.category() != null && scope.purpose() != null
                    && (scope.purpose() == ConsentPurpose.AI_CHAT
                    || switch (scope.category()) {
                        case BASIC_PROFILE -> scope.purpose() == ConsentPurpose.PROFILE_READ || scope.purpose() == ConsentPurpose.PROFILE_UPDATE;
                        case DIETARY_PREFERENCES -> scope.purpose() == ConsentPurpose.DIETARY_READ || scope.purpose() == ConsentPurpose.DIETARY_UPDATE;
                        case HEALTH_PROFILE -> scope.purpose() == ConsentPurpose.HEALTH_PROFILE_READ || scope.purpose() == ConsentPurpose.HEALTH_PROFILE_UPDATE;
                    }));
        }
    }

    public record ConsentScope(@NotNull ConsentCategory category, @NotNull ConsentPurpose purpose) { }

    public record Turn(@NotNull Speaker speaker,
            @NotBlank @Size(max = AiClient.MAX_CHAT_ANSWER_LENGTH)
            @Schema(maxLength = AiClient.MAX_CHAT_ANSWER_LENGTH,
                    description = "USER content is limited to 1000 characters; ASSISTANT content may contain a complete answer up to 8000 characters.")
            String content,
            @Size(max = 4)
            @Schema(description = "IDs of up to four recipe cards shown in this ASSISTANT turn. Untrusted hints used only to avoid repeats; never authorization or recipe data.")
            List<@NotNull @Min(1) Long> recommendedRecipeIds) {
        public Turn(Speaker speaker, String content) { this(speaker, content, null); }

        @AssertTrue(message = "Chỉ lượt trả lời mới có danh sách công thức đã gợi ý.")
        @JsonIgnore
        @Schema(hidden = true)
        public boolean isRecipeHistoryValid() {
            return speaker == Speaker.ASSISTANT || recommendedRecipeIds == null || recommendedRecipeIds.isEmpty();
        }

        @Override
        public String toString() { return "Turn[redacted]"; }

        @AssertTrue(message = "Nội dung người dùng trong lịch sử tối đa 1.000 ký tự.")
        @JsonIgnore
        @Schema(hidden = true)
        public boolean isContentLengthValid() {
            return speaker != Speaker.USER || content == null || content.length() <= 1000;
        }
    }
}

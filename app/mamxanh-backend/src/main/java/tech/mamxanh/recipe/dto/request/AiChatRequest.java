package tech.mamxanh.recipe.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import tech.mamxanh.integration.ai.AiClient;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record AiChatRequest(
        @NotNull @Schema(description = "GENERAL for cooking questions; RECIPE for a server-loaded public RecipePost.") ContextType contextType,
        @Min(1) @Schema(description = "Required for RECIPE context and omitted for GENERAL context.") Long recipeId,
        @NotBlank @Size(max = 1000) @Schema(maxLength = 1000) String question,
        @Size(max = 8) @Schema(description = "Most recent conversation turns, up to eight items, kept only for this request.")
        List<@Valid Turn> history,
        @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
        @Schema(description = "Session-cookie UUID required for Guest rate limiting; ignored for authenticated Members.")
        String anonymousViewerId) {

    public AiChatRequest(ContextType contextType, Long recipeId, String question, List<Turn> history) {
        this(contextType, recipeId, question, history, null);
    }

    @Override
    public String toString() { return "AiChatRequest[redacted]"; }

    public enum ContextType { GENERAL, RECIPE }
    public enum Speaker { USER, ASSISTANT }

    public record Turn(@NotNull Speaker speaker,
            @NotBlank @Size(max = AiClient.MAX_CHAT_ANSWER_LENGTH)
            @Schema(maxLength = AiClient.MAX_CHAT_ANSWER_LENGTH,
                    description = "USER content is limited to 1000 characters; ASSISTANT content may contain a complete answer up to 8000 characters.")
            String content) {
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

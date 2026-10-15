package tech.mamxanh.recipe.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record AiChatRequest(
        @NotNull @Schema(description = "GENERAL for cooking questions; RECIPE for a server-loaded public RecipePost.") ContextType contextType,
        @Min(1) @Schema(description = "Required for RECIPE context and omitted for GENERAL context.") Long recipeId,
        @NotBlank @Size(max = 1000) @Schema(maxLength = 1000) String question,
        @Size(max = 8) @Schema(description = "Most recent conversation turns, up to eight items, kept only for this request.")
        List<@Valid Turn> history) {

    public enum ContextType { GENERAL, RECIPE }
    public enum Speaker { USER, ASSISTANT }

    public record Turn(@NotNull Speaker speaker, @NotBlank @Size(max = 1000) @Schema(maxLength = 1000) String content) { }
}

package tech.mamxanh.recipe.dto.response;

import tech.mamxanh.recipe.dto.request.AiChatRequest.ContextType;
import io.swagger.v3.oas.annotations.media.Schema;

public record AiChatResponse(ContextType contextType,
        @Schema(description = "Null for GENERAL context.") Long recipeId,
        @Schema(description = "AI-generated answer. Treat as reference information.") String answer) { }

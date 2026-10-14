package tech.mamxanh.recipe.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.recipe.dto.request.AiChatRequest;
import tech.mamxanh.recipe.dto.response.AiChatResponse;
import tech.mamxanh.recipe.service.AiChatService;

@RestController
@RequestMapping("/api/v1/recipes/ai/chat")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AiChatController {
    private final AiChatService service;

    public AiChatController(AiChatService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Ask the vegetarian cooking assistant",
            description = "Requires an active Member. General context or the latest public RecipePost is loaded server-side. "
                    + "Conversation history is request-scoped and is not persisted. Guest access will be enabled with FR-02 rate limiting.")
    @ApiResponse(responseCode = "200", description = "AI answer generated.")
    @ApiResponse(responseCode = "400", description = "Invalid question, history, or context fields.")
    @ApiResponse(responseCode = "401", description = "An authenticated Member session is required.")
    @ApiResponse(responseCode = "404", description = "The recipe does not exist or is not public.")
    @ApiResponse(responseCode = "502", description = "Gemini returned an incomplete or invalid answer.")
    @ApiResponse(responseCode = "503", description = "Gemini is unavailable or timed out.")
    public AiChatResponse answer(@Valid @RequestBody AiChatRequest request) {
        return service.answer(request);
    }
}

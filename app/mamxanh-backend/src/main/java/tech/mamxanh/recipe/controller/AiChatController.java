package tech.mamxanh.recipe.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.recipe.dto.request.AiChatRequest;
import tech.mamxanh.recipe.dto.response.AiChatResponse;
import tech.mamxanh.recipe.service.AiChatService;
import tech.mamxanh.recipe.service.GuestChatRateLimiter;

@RestController
@RequestMapping("/api/v1/recipes/ai/chat")
public class AiChatController {
    private final AiChatService service;

    public AiChatController(AiChatService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Ask the vegetarian cooking assistant",
            description = "Guest and active Member use the same chatbot. Guest sends a first-party cookie UUID in X-Guest-Chat-Id; "
                    + "without it the peer IP shares a fallback window. Guest limits are 3 requests per calendar day per identity and 60 per peer IP, "
                    + "resetting on each new day. General context or the latest public RecipePost is loaded server-side. "
                    + "Recipe discovery selects only current public RecipePosts from the Backend catalog; explicitly requested recipe drafts remain chat text only. "
                    + "Conversation history is request-scoped and is not persisted.",
            security = {@SecurityRequirement, @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)})
    @ApiResponse(responseCode = "200", description = "AI answer and, when relevant, selected public recipe cards generated.")
    @ApiResponse(responseCode = "400", description = "Invalid question, history, or context fields.")
    @ApiResponse(responseCode = "401", description = "A supplied Bearer token is invalid or expired.")
    @ApiResponse(responseCode = "429", description = "GUEST_CHAT_RATE_LIMITED; no call is forwarded to Gemini.",
            headers = @Header(name = "Retry-After", description = "Seconds until retry is allowed.", schema = @Schema(type = "integer")))
    @ApiResponse(responseCode = "404", description = "The recipe does not exist or is not public.")
    @ApiResponse(responseCode = "502", description = "Gemini returned an incomplete or invalid answer.")
    @ApiResponse(responseCode = "503", description = "Gemini is unavailable or timed out.")
    public ResponseEntity<AiChatResponse> answer(@Valid @RequestBody AiChatRequest request,
            @Parameter(description = "Optional Guest UUID from the first-party anonymous cookie. Not an authentication credential.")
            @RequestHeader(value = GuestChatRateLimiter.ID_HEADER, required = false) String anonymousId,
            HttpServletRequest servletRequest) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.answer(request, anonymousId, servletRequest.getRemoteAddr()));
    }
}

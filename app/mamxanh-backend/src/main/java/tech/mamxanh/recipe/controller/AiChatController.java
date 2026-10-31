package tech.mamxanh.recipe.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.recipe.dto.request.AiChatRequest;
import tech.mamxanh.recipe.dto.response.AiChatResponse;
import tech.mamxanh.recipe.service.AiChatService;
import tech.mamxanh.recipe.service.GuestChatRateLimiter;

@RestController
@RequestMapping("/api/v1/recipes/ai/chat")
public class AiChatController {
    private final AiChatService service;
    private final GuestChatRateLimiter guestRateLimiter;

    public AiChatController(AiChatService service, GuestChatRateLimiter guestRateLimiter) {
        this.service = service;
        this.guestRateLimiter = guestRateLimiter;
    }

    @PostMapping
    @Operation(summary = "Ask the vegetarian cooking assistant",
            security = {@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH), @SecurityRequirement(name = "")},
            description = "Guest and active Member use the same chatbot. Guest must supply a session-cookie UUID in anonymousViewerId; "
                    + "Guest is limited to 20,000 provider-reported tokens per anonymous session and a rolling limit of 10 requests per minute independently for cookie and peer IP. "
                    + "General context or the latest public RecipePost is loaded server-side. "
                    + "Recipe discovery selects only current public RecipePosts from the Backend catalog; explicitly requested recipe drafts remain chat text only. "
                    + "Legacy conversation history is request-scoped. Orchestration clients may use a bounded Backend session with consent and confirmation; prompts are not persisted. Guest has no account dietary context. "
                    + "Explicit weekly-menu and recipe-authoring requests are checked for entitlement and routed to their dedicated workflow without calling Gemini. "
                    + "Follow-up recipe searches use current public data; assistant recipe IDs are untrusted exclusion hints only.")
    @ApiResponse(responseCode = "200", description = "AI answer and, when relevant, selected public recipe cards generated.")
    @ApiResponse(responseCode = "400", description = "Invalid question, history, or context fields.")
    @ApiResponse(responseCode = "401", description = "Bearer token invalid/expired, or a Guest requests a Member-only workflow; no provider call.")
    @ApiResponse(responseCode = "403", description = "Account/role denied, or workflow requires a higher subscription. AI_SUBSCRIPTION_REQUIRED includes requiredTier and upgradeUrl; no provider call.")
    @ApiResponse(responseCode = "429", description = "Guest exceeded the cookie/IP rate limit or the current Guest/Member token quota. Retry-After is provided for rate-limit responses.",
            headers = @Header(name = "Retry-After", description = "Seconds until Guest can try again.",
                    schema = @Schema(types = {"integer"}, minimum = "1")))
    @ApiResponse(responseCode = "404", description = "The recipe does not exist or is not public.")
    @ApiResponse(responseCode = "502", description = "Gemini returned an incomplete or invalid answer.")
    @ApiResponse(responseCode = "503", description = "Gemini is unavailable or timed out.")
    public AiChatResponse answer(@Valid @RequestBody AiChatRequest request, Authentication authentication,
            HttpServletRequest httpRequest) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            guestRateLimiter.check(request.anonymousViewerId(), httpRequest.getRemoteAddr());
        }
        return service.answer(request);
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Delete the current temporary AI conversation session",
            security = {@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH), @SecurityRequirement(name = "")},
            description = "Deletes ephemeral consent and pending-action state for the session owned by this member or anonymous viewer. It does not undo completed business actions.")
    @ApiResponse(responseCode = "204", description = "Session deleted or already expired.")
    @ApiResponse(responseCode = "400", description = "Invalid session identifier or viewer identifier.")
    public ResponseEntity<Void> deleteSession(@PathVariable java.util.UUID sessionId,
            @RequestParam(required = false) String anonymousViewerId, Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            if (anonymousViewerId == null || !anonymousViewerId.matches(
                    "(?i)[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")) {
                throw new tech.mamxanh.common.exception.AppException(
                        tech.mamxanh.common.exception.ErrorCode.VALIDATION_FAILED,
                        "Phiên trợ lý không hợp lệ hoặc đã hết hạn.");
            }
        }
        service.deleteSession(sessionId, anonymousViewerId);
        return ResponseEntity.noContent().build();
    }
}

package tech.mamxanh.recipe.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final GuestChatRateLimiter guestRateLimiter;

    public AiChatController(AiChatService service, GuestChatRateLimiter guestRateLimiter) {
        this.service = service;
        this.guestRateLimiter = guestRateLimiter;
    }

    @PostMapping
    @Operation(summary = "Ask the vegetarian cooking assistant",
            security = {@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH), @SecurityRequirement(name = "")},
            description = "Guest and active Member use the same chatbot. Guest must supply a session-cookie UUID in anonymousViewerId; "
                    + "a rolling limit of 10 requests per minute applies independently to cookie and peer IP. No daily quota. "
                    + "General context or the latest public RecipePost is loaded server-side. "
                    + "Recipe discovery selects only current public RecipePosts from the Backend catalog; explicitly requested recipe drafts remain chat text only. "
                    + "Conversation history is request-scoped and is not persisted. Guest has no account dietary context. "
                    + "Explicit weekly-menu and recipe-authoring requests are checked for entitlement and routed to their dedicated workflow without calling Gemini. "
                    + "Follow-up recipe searches use current public data; assistant recipe IDs are untrusted exclusion hints only.")
    @ApiResponse(responseCode = "200", description = "AI answer and, when relevant, selected public recipe cards generated.")
    @ApiResponse(responseCode = "400", description = "Invalid question, history, or context fields.")
    @ApiResponse(responseCode = "401", description = "Bearer token invalid/expired, or a Guest requests a Member-only workflow; no provider call.")
    @ApiResponse(responseCode = "403", description = "Account/role denied, or workflow requires a higher subscription. AI_SUBSCRIPTION_REQUIRED includes requiredTier and upgradeUrl; no provider call.")
    @ApiResponse(responseCode = "429", description = "Guest exceeded the cookie or peer-IP limit; Retry-After gives the wait in seconds.",
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
}

package tech.mamxanh.subscription.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.subscription.dto.request.AiModelPreferenceRequest;
import tech.mamxanh.subscription.dto.response.AiChatSettingsResponse;
import tech.mamxanh.subscription.service.AiChatSettingsService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai/chat-settings")
public class AiChatSettingsController {
    private final AiChatSettingsService service;

    public AiChatSettingsController(AiChatSettingsService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Get current AI chat quota and available Gemini models")
    public AiChatSettingsResponse get(Authentication authentication,
            @RequestParam(required = false) String anonymousViewerId) {
        return service.get(userId(authentication), userId(authentication) == null ? validateGuestId(anonymousViewerId) : null);
    }

    @PutMapping("/model")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "Choose a Gemini model for the current account")
    public AiChatSettingsResponse select(Authentication authentication, @Valid @RequestBody AiModelPreferenceRequest request) {
        Long id = userId(authentication);
        if (id == null) throw new AppException(ErrorCode.UNAUTHENTICATED);
        return service.select(id, request.modelId());
    }

    private static Long userId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken
                || "anonymousUser".equals(authentication.getPrincipal())) return null;
        try { return Long.parseLong(authentication.getName()); }
        catch (NumberFormatException exception) { throw new AppException(ErrorCode.UNAUTHENTICATED); }
    }

    private static String validateGuestId(String id) {
        try { return UUID.fromString(id).toString(); }
        catch (RuntimeException exception) { throw new AppException(ErrorCode.VALIDATION_FAILED); }
    }
}

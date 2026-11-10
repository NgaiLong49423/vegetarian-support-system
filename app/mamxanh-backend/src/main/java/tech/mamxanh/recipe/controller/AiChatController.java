package tech.mamxanh.recipe.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private tech.mamxanh.recipe.service.AiShoppingExecutionJournal journal;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private tech.mamxanh.auth.service.CurrentUserService users;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private tech.mamxanh.recipe.service.AiAssistantExecutionJournal actionJournal;

    public record ActionExecution(String actionId, String tool, String state, String summary, Object result) { }

    @GetMapping("/action-executions/{requestId}")
    @Operation(summary = "Recover this member's recorded assistant actions after a lost response",
            description = "An empty result or an indeterminate state never authorizes replay.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    public ResponseEntity<java.util.List<ActionExecution>> actionExecutions(@PathVariable java.util.UUID requestId) {
        long ownerId = users.requireActiveMember().id();
        if (actionJournal == null) throw new tech.mamxanh.common.exception.AppException(
                tech.mamxanh.common.exception.ErrorCode.AI_TELEMETRY_UNAVAILABLE);
        var result = actionJournal.findByRequest(ownerId, requestId).stream().map(entry -> {
            var data = entry.resultData() instanceof tech.mamxanh.recipe.service.AiAssistantToolRegistry.ToolResult toolResult
                    ? toolResult : null;
            return new ActionExecution(entry.actionId(), entry.toolName(), entry.state().name(),
                    data == null ? null : data.summary(), data == null ? null : data.result());
        }).toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result);
    }

    @GetMapping("/executions/{clientRequestId}")
    @Operation(summary = "Look up this member's Shopping List write by client request ID; UNKNOWN never authorizes replay",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    public ResponseEntity<tech.mamxanh.recipe.service.AiShoppingExecutionJournal.Result> execution(
            @PathVariable java.util.UUID clientRequestId) {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(journal.find(users.requireActiveMember().id(), clientRequestId));
    }

    public AiChatController(AiChatService service, GuestChatRateLimiter guestRateLimiter) {
        this.service = service;
        this.guestRateLimiter = guestRateLimiter;
    }

    @PostMapping("/ui-actions/ack")
    @Operation(summary = "Acknowledge a recipe form action after the interface applies or rejects it",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    public ResponseEntity<AiChatRequest.FormActionAck> acknowledgeFormAction(
            @Valid @RequestBody AiChatRequest.FormActionAck acknowledgement) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.acknowledgeFormAction(acknowledgement));
    }

    @PostMapping
    @Operation(summary = "Ask the vegetarian cooking assistant",
            security = {@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH), @SecurityRequirement(name = "")},
            description = "Guest and active Member use the same chatbot. Guest must supply a session-cookie UUID in anonymousViewerId; "
                    + "Guest is limited to 50,000 provider-reported tokens per calendar month per anonymous identity and a rolling limit of 10 requests per minute independently for cookie and peer IP. "
                    + "General context or the latest public RecipePost is loaded server-side. "
                    + "Recipe discovery selects only current public RecipePosts from the Backend catalog; explicitly requested recipe drafts remain chat text only. "
                    + "Legacy conversation history is request-scoped. Orchestration clients may use a bounded Backend session with consent and confirmation; prompts are not persisted. Guest has no account dietary context. "
                    + "Explicit weekly-menu and recipe-authoring requests are checked for entitlement and routed to their dedicated workflow without calling Gemini. "
                    + "Follow-up recipe searches use current public data; assistant recipe IDs are untrusted exclusion hints only.")
    @ApiResponse(responseCode = "200", description = "AI answer and, when relevant, selected public recipe cards generated.")
    @ApiResponse(responseCode = "400", description = "Invalid question, history, or context fields.")
    @ApiResponse(responseCode = "401", description = "Bearer token invalid/expired, or a Guest requests a Member-only workflow; no provider call.")
    @ApiResponse(responseCode = "403", description = "Account or role denied; no provider call.")
    @ApiResponse(responseCode = "429", description = "Guest exceeded the cookie/IP rate limit or the current Guest/Member token quota. Retry-After is provided for rate-limit responses.",
            headers = @Header(name = "Retry-After", description = "Seconds until Guest can try again.",
                    schema = @Schema(types = {"integer"}, minimum = "1")))
    @ApiResponse(responseCode = "404", description = "The recipe does not exist or is not public.")
    @ApiResponse(responseCode = "502", description = "Gemini returned an incomplete or invalid answer.")
    @ApiResponse(responseCode = "503", description = "Gemini is unavailable or timed out.")
    public ResponseEntity<AiChatResponse> answer(
            @Valid @RequestBody AiChatRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        AiChatRequest effectiveRequest = prepareEffectiveRequest(request, authentication, httpRequest);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.answer(effectiveRequest));
    }

    private AiChatRequest prepareEffectiveRequest(AiChatRequest request, Authentication authentication, HttpServletRequest httpRequest) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            String guestHeader = httpRequest.getHeader(GuestChatRateLimiter.ID_HEADER);
            String guestId = (guestHeader != null && !guestHeader.isBlank())
                    ? guestHeader.strip()
                    : (request.anonymousViewerId() != null && !request.anonymousViewerId().isBlank()
                            ? request.anonymousViewerId().strip()
                            : null);
            guestRateLimiter.check(guestId, httpRequest.getRemoteAddr());
            return new AiChatRequest(request.contextType(), request.recipeId(), request.question(),
                    request.history(), guestId, request.sessionId(), request.executionMode(),
                    request.consentDecision(), request.confirmationId(), request.confirmationDecision(),
                    request.clientRequestId(), request.controlAction(), request.formContext());
        }
        return request;
    }

    @PostMapping(value = "/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Ask the vegetarian cooking assistant with real-time SSE streaming",
            security = {@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH), @SecurityRequirement(name = "")})
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter stream(
            @Valid @RequestBody AiChatRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        AiChatRequest effectiveRequest = prepareEffectiveRequest(request, authentication, httpRequest);
        var emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(
                tech.mamxanh.integration.ai.AiTurnDeadline.SSE_TIMEOUT.toMillis());
        var cancellationSignal = service.registerStreamCancellation(effectiveRequest);
        SecurityContext capturedContext = SecurityContextHolder.createEmptyContext();
        capturedContext.setAuthentication(authentication);
        java.util.concurrent.atomic.AtomicBoolean terminal = new java.util.concurrent.atomic.AtomicBoolean();
        Runnable cancelOnDisconnect = () -> {
            if (!terminal.compareAndSet(false, true)) return;
            SecurityContext previous = SecurityContextHolder.getContext();
            SecurityContextHolder.setContext(capturedContext);
            try {
                service.answer(asCancellationRequest(effectiveRequest));
            } catch (RuntimeException cancellationFailure) {
                org.slf4j.LoggerFactory.getLogger(AiChatController.class).warn(
                        "Could not stop disconnected AI stream sessionId={}", effectiveRequest.sessionId());
            } finally {
                SecurityContextHolder.setContext(previous);
            }
        };
        emitter.onError(ignored -> cancelOnDisconnect.run());
        emitter.onTimeout(cancelOnDisconnect);
        emitter.onCompletion(cancelOnDisconnect);
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            SecurityContext previous = SecurityContextHolder.getContext();
            SecurityContextHolder.setContext(capturedContext);
            try {
                service.stream(effectiveRequest, event -> {
                    if ("complete".equals(event.type()) || "error".equals(event.type())) {
                        terminal.set(true);
                    }
                    try {
                        emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                .name(event.type())
                                .data(event.data(), org.springframework.http.MediaType.APPLICATION_JSON));
                    } catch (Exception ex) {
                        cancelOnDisconnect.run();
                    }
                }, cancellationSignal);
                terminal.set(true);
                emitter.complete();
            } catch (Exception failure) {
                terminal.set(true);
                try {
                    emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                            .name("error")
                            .data(java.util.Map.of("message", failure.getMessage(), "status", "FAILED"),
                                    org.springframework.http.MediaType.APPLICATION_JSON));
                } catch (Exception ignored) {}
                emitter.completeWithError(failure);
            } finally {
                service.releaseStreamCancellation(effectiveRequest, cancellationSignal);
                SecurityContextHolder.setContext(previous);
            }
        });
        return emitter;
    }

    private static AiChatRequest asCancellationRequest(AiChatRequest request) {
        return new AiChatRequest(request.contextType(), request.recipeId(),
                request.question() != null ? request.question() : "Dừng tác vụ",
                request.history() != null ? request.history() : java.util.List.of(),
                request.anonymousViewerId(), request.sessionId(), request.executionMode(),
                request.consentDecision(), request.confirmationId(), request.confirmationDecision(),
                request.clientRequestId(), AiChatRequest.ControlAction.CANCEL);
    }

    @PostMapping("/cancel")
    @Operation(summary = "Cancel an active AI turn by sessionId or clientRequestId, or cancel a staged confirmation",
            description = "Send clientRequestId when the stream has not returned a sessionId yet. Cancellation prevents later Agent tool steps; a tool operation already started may finish and committed changes are preserved.",
            security = {@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH), @SecurityRequirement(name = "")})
    public ResponseEntity<AiChatResponse> cancel(
            @Valid @RequestBody AiChatRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        AiChatRequest effectiveRequest = prepareEffectiveRequest(request, authentication, httpRequest);
        AiChatRequest cancelRequest = new AiChatRequest(effectiveRequest.contextType(), effectiveRequest.recipeId(),
                effectiveRequest.question() != null ? effectiveRequest.question() : "Dừng tác vụ",
                effectiveRequest.history() != null ? effectiveRequest.history() : java.util.List.of(),
                effectiveRequest.anonymousViewerId(), effectiveRequest.sessionId(), effectiveRequest.executionMode(),
                effectiveRequest.consentDecision(), effectiveRequest.confirmationId(), effectiveRequest.confirmationDecision(),
                effectiveRequest.clientRequestId(), AiChatRequest.ControlAction.CANCEL);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.answer(cancelRequest));
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

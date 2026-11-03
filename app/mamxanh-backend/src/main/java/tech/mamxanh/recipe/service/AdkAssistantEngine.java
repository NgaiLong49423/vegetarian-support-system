package tech.mamxanh.recipe.service;

import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.models.BaseLlm;
import com.google.adk.models.Gemini;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.FunctionCall;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.FunctionResponse;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.ai.AiClient;
import tech.mamxanh.integration.ai.AiPromptLoader;
import tech.mamxanh.integration.ai.AiTokenUsage;
import tech.mamxanh.integration.ai.AiUsageEvent;
import tech.mamxanh.integration.ai.GeminiAiClient.AiProviderException;
import tech.mamxanh.integration.ai.adk.AdkModelGateway;
import tech.mamxanh.integration.ai.adk.MemberAccountingPolicy;
import tech.mamxanh.recipe.dto.request.AiChatRequest;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ConfirmationDecision;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ConsentAction;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ConsentCategory;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ConsentPurpose;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ConsentScope;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ExecutionMode;
import tech.mamxanh.recipe.dto.request.AiChatRequest.Speaker;
import tech.mamxanh.recipe.dto.response.AiChatResponse;
import tech.mamxanh.recipe.dto.response.AiChatResponse.AssistantStep;
import tech.mamxanh.recipe.dto.response.AiChatResponse.ConsentRequest;
import tech.mamxanh.recipe.dto.response.AiChatResponse.PendingConfirmation;
import tech.mamxanh.recipe.dto.response.AiChatResponse.Source;
import tech.mamxanh.subscription.service.AiChatSettingsService;
import tech.mamxanh.subscription.service.AiTokenQuotaService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * Unified Google ADK Orchestration Engine for Mâm Xanh AI Assistant.
 * Provides a two-way Agentic Loop (User -> Model -> ScopedTool -> ToolResult -> Model -> Grounded Response)
 * with role-based tool scoping, session-bound state, execution truthfulness, and safety boundaries.
 */
@Service
public class AdkAssistantEngine implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AdkAssistantEngine.class);
    public static final String ENGINE = AiAssistantSessionStore.ENGINE_ADK;
    public static final Duration RUN_TIMEOUT = Duration.ofSeconds(30);
    public static final Duration STEP_TIMEOUT = Duration.ofSeconds(10);
    public static final int MAX_LLM_CALLS = 6;
    public static final int MAX_CONTEXT_BYTES = 64_000;

    private static final String CLASSIFICATION_UNAVAILABLE_ANSWER =
            "Hiện mình chưa thể kiểm tra phạm vi câu hỏi. Câu hỏi chưa được chuyển sang bước trả lời; bạn vui lòng thử lại sau ít phút.";
    private static final Pattern PERSONAL_DATA = Pattern.compile(
            "(?iu)([\\w.+-]+@[\\w.-]+\\.[a-z]{2,}|(?:\\+?84|0)(?:[ .-]?\\d){8,10}|(?:hồ sơ|thông tin) (?:cá nhân|sức khỏe|suc khoe)|hồ sơ.{0,20}(?:tôi|mình)|(?:tôi|mình).{0,20}(?:hồ sơ|sức khỏe|BMI|calo)|sức khỏe (?:của )?(?:tôi|mình)|BMI (?:của )?(?:tôi|mình)|tên (?:hiển thị )?(?:của )?(?:tôi|mình)|(?:đổi|sửa|cập nhật) (?:tên|bio)|bio (?:của )?(?:tôi|mình)|dị ứng|bệnh nền|cân nặng|chiều cao|(?:chế độ ăn|tùy chọn ăn uống) của (?:tôi|mình)|(?:tôi|mình).{0,20}(?:ăn chay|dị ứng|chế độ ăn|tùy chọn ăn uống))");

    @FunctionalInterface
    public interface LlmFactory {
        BaseLlm create(String modelId);
    }

    private final CurrentUserService users;
    private final AiTokenQuotaService aiQuota;
    private final AiChatSettingsService chatSettings;
    private final AiAssistantSessionStore sessionStore;
    private final AiAssistantToolRegistry toolRegistry;
    private final AdkDomainTools domainTools;
    private final ApprovedAiKnowledgeRetriever knowledge;
    private final AiPromptLoader promptLoader;
    private final AiClient aiClient;
    private final ObjectMapper mapper;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final LlmFactory llmFactory;
    private final Map<UUID, EngineSessionState> engineSessions = new ConcurrentHashMap<>();

    @Autowired
    public AdkAssistantEngine(
            CurrentUserService users,
            AiTokenQuotaService aiQuota,
            AiChatSettingsService chatSettings,
            AiAssistantSessionStore sessionStore,
            AiAssistantToolRegistry toolRegistry,
            @Autowired(required = false) AdkDomainTools domainTools,
            ApprovedAiKnowledgeRetriever knowledge,
            @Autowired(required = false) AiPromptLoader promptLoader,
            AiClient aiClient,
            ObjectMapper mapper,
            ApplicationEventPublisher events,
            Clock clock,
            @Autowired(required = false) LlmFactory llmFactory,
            @Value("${GEMINI_API_KEY:}") String configuredApiKey) {
        this.users = users;
        this.aiQuota = aiQuota;
        this.chatSettings = chatSettings;
        this.sessionStore = sessionStore;
        this.toolRegistry = toolRegistry;
        this.domainTools = domainTools;
        this.knowledge = knowledge;
        this.promptLoader = promptLoader != null ? promptLoader : new AiPromptLoader();
        this.aiClient = aiClient;
        this.mapper = mapper;
        this.events = events;
        this.clock = clock != null ? clock : Clock.systemUTC();
        this.llmFactory = llmFactory != null ? llmFactory : createDefaultLlmFactory(configuredApiKey);
    }

    public AdkAssistantEngine(
            CurrentUserService users,
            AiTokenQuotaService aiQuota,
            AiChatSettingsService chatSettings,
            AiAssistantSessionStore sessionStore,
            AiAssistantToolRegistry toolRegistry,
            ApprovedAiKnowledgeRetriever knowledge,
            AiPromptLoader promptLoader,
            AiClient aiClient,
            ObjectMapper mapper,
            ApplicationEventPublisher events,
            Clock clock,
            LlmFactory llmFactory,
            String configuredApiKey) {
        this(users, aiQuota, chatSettings, sessionStore, toolRegistry, null, knowledge, promptLoader, aiClient, mapper, events, clock, llmFactory, configuredApiKey);
    }

    public AiChatResponse execute(AiChatRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Nội dung câu hỏi không được để trống.");
        }
        if (request.controlAction() != null) {
            return cancel(request);
        }

        Long userId = users.findAuthenticatedUserId().isPresent() ? users.requireActiveMember().id() : null;
        String owner = userId == null ? "guest:" + request.anonymousViewerId() : "user:" + userId;
        String role = userId == null ? "GUEST" : users.requireActiveMember().role().name();
        Authentication auth = createAuthentication(userId, role);

        evictExpiredSessions();

        var sessionHandle = request.sessionId() == null
                ? sessionStore.open(null, owner, ENGINE)
                : sessionStore.open(request.sessionId(), owner, ENGINE);
        UUID sessionId = sessionHandle.id();

        Instant initialExpiresAt = sessionHandle.expiresAt();
        EngineSessionState state = engineSessions.computeIfAbsent(sessionId,
                id -> new EngineSessionState(id, userId, owner, auth, initialExpiresAt));
        state.expiresAt = sessionHandle.expiresAt();

        // 1. Consent Revocation check
        Set<ConsentScope> activeConsents = new HashSet<>(sessionHandle.consentScopes());
        if (request.consentDecision() != null) {
            var decision = request.consentDecision();
            Set<ConsentScope> submitted = Set.copyOf(decision.scopes() == null ? List.of() : decision.scopes());
            sessionHandle = sessionStore.applyConsent(sessionId, owner,
                    decision.action() == ConsentAction.GRANT, submitted);
            activeConsents = new HashSet<>(sessionHandle.consentScopes());
            state.expiresAt = sessionHandle.expiresAt();
            if (decision.action() == ConsentAction.REVOKE) {
                state.consentRevoked = true;
                sessionStore.discardPending(sessionId, owner);
                state.pendingConfirmation = null;
                return buildResponse(request, sessionId, request.executionMode(), "CONSENT_REVOKED",
                        List.of(), List.of(), null, null,
                        "Đã thu hồi sự đồng ý dữ liệu; những lượt tiếp theo sẽ không gửi dữ liệu này tới Google Gemini.");
            }
            state.consentRevoked = false;
        }

        ExecutionMode mode = request.executionMode() == null ? ExecutionMode.CONFIRM : request.executionMode();

        // 2. Handle Confirmation Decision (Human-in-the-loop resume/reject)
        if (request.confirmationDecision() != null || request.confirmationId() != null) {
            return handleConfirmationDecision(request, userId, owner, auth, sessionHandle, state, mode);
        }

        // If there was an unhandled pending confirmation and user asks a new question, discard the pending confirmation
        if (state.pendingConfirmation != null) {
            sessionStore.discardPending(sessionId, owner);
            state.pendingConfirmation = null;
        }

        // 3. Check Personal Data Consent requirements
        String userHistory = request.history() == null ? "" : request.history().stream()
                .filter(turn -> turn.speaker() == Speaker.USER && turn.content() != null)
                .map(AiChatRequest.Turn::content)
                .reduce("", (a, b) -> a + " " + b);
        ConsentCategory detectedPersonalCategory = personalCategory(request.question() + " " + userHistory);
        if (detectedPersonalCategory != null) {
            ConsentScope requiredScope = new ConsentScope(detectedPersonalCategory, ConsentPurpose.AI_CHAT);
            if (!activeConsents.contains(requiredScope)) {
                return buildResponse(request, sessionId, mode, "AWAITING_CONSENT",
                        List.of(), List.of(), new ConsentRequest(List.of(requiredScope), "Google Gemini"), null,
                        "Để gửi nội dung có dữ liệu cá nhân tới Google Gemini, vui lòng đồng ý đúng phạm vi trước.");
            }
        }

        // 4. Preflight Scope Classification (FR-51 / BR-09 / BR-41)
        AiChatResponse refusal = classify(request, sessionId, mode, userId);
        if (refusal != null) {
            return refusal;
        }

        // 5. Execute Turn via ADK Two-Way Agentic Loop
        return runAdkTurn(request, userId, owner, auth, sessionHandle, state, mode, activeConsents,
                Content.fromParts(Part.fromText(request.question())), false, null);
    }

    private AiChatResponse runAdkTurn(
            AiChatRequest request,
            Long userId,
            String owner,
            Authentication auth,
            AiAssistantSessionStore.SessionHandle sessionHandle,
            EngineSessionState state,
            ExecutionMode mode,
            Set<ConsentScope> activeConsents,
            Content inputContent,
            boolean approvalResume,
            String approvedConfirmationCallId) {
        return runAdkTurn(request, userId, owner, auth, sessionHandle, state, mode, activeConsents,
                inputContent, approvalResume, approvedConfirmationCallId, new ArrayList<>(), new ArrayList<>());
    }

    private AiChatResponse runAdkTurn(
            AiChatRequest request,
            Long userId,
            String owner,
            Authentication auth,
            AiAssistantSessionStore.SessionHandle sessionHandle,
            EngineSessionState state,
            ExecutionMode mode,
            Set<ConsentScope> activeConsents,
            Content inputContent,
            boolean approvalResume,
            String approvedConfirmationCallId,
            List<AssistantStep> initialSteps,
            List<Source> initialSources) {

        String resolvedModel = resolveModel(userId);
        List<AssistantStep> steps = initialSteps != null ? initialSteps : new ArrayList<>();
        List<Source> sources = initialSources != null ? initialSources : new ArrayList<>();
        AtomicReference<PendingConfirmation> stagedConfirmation = new AtomicReference<>();

        try {
            state.ensureRunner(resolvedModel, auth, mode, activeConsents, steps, sources, stagedConfirmation);
        } catch (RuntimeException unavailable) {
            LOG.warn("Failed to initialize ADK runner: {}", unavailable.getMessage());
            return buildResponse(request, sessionHandle.id(), mode, "FAILED", List.of(), List.of(), null, null,
                    "Không thể khởi tạo AI với model hoặc cấu hình hiện tại. Vui lòng thử lại sau.");
        }

        if (!state.busy.compareAndSet(false, true)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Phiên trợ lý đang xử lý một lượt chạy khác. Vui lòng đợi lượt hiện tại hoàn tất.");
        }

        List<Event> emittedEvents;
        try {
            emittedEvents = state.runner.runAsync(owner, state.adkSessionId, inputContent,
                    RunConfig.builder().maxLlmCalls(MAX_LLM_CALLS).toolExecutionMode(RunConfig.ToolExecutionMode.SEQUENTIAL).build())
                    .takeUntil(Flowable.timer(RUN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                            .flatMap(ignored -> Flowable.error(new java.util.concurrent.TimeoutException("Run deadline exceeded"))))
                    .toList().blockingGet();
        } catch (RuntimeException failure) {
            LOG.warn("ADK turn execution failed: {}", failure.getMessage());
            state.busy.set(false);
            return handleRunFailure(request, sessionHandle.id(), mode, steps, sources, failure);
        } finally {
            state.busy.set(false);
        }

        // Check if a tool staged a confirmation in CONFIRM mode
        PendingConfirmation pending = stagedConfirmation.get();
        if (pending != null) {
            state.pendingConfirmation = pending;
            return buildResponse(request, sessionHandle.id(), mode, "AWAITING_CONFIRMATION",
                    steps, sources, null, pending,
                    "Mình đã chuẩn bị thao tác \"" + pending.proposedListName() + "\". Bạn vui lòng kiểm tra và bấm Xác nhận để hoàn tất.");
        }

        // Extract Gemini final natural language answer from the event stream
        String answer = extractFinalAnswer(emittedEvents);
        if (answer.isBlank()) {
            if (!steps.isEmpty()) {
                answer = steps.getLast().summary() != null ? steps.getLast().summary() : "Thao tác đã được thực hiện thành công.";
            } else {
                answer = "Mình đã xử lý yêu cầu của bạn.";
            }
        }

        // Grounding with knowledge base if no tools were invoked
        if (steps.isEmpty()) {
            List<Source> retrievedSources = knowledge.retrieve(request.question());
            if (!retrievedSources.isEmpty()) {
                sources.addAll(retrievedSources);
            }
        }

        return buildResponse(request, sessionHandle.id(), mode, "COMPLETED", steps, sources, null, null, answer);
    }

    private AiChatResponse handleConfirmationDecision(
            AiChatRequest request,
            Long userId,
            String owner,
            Authentication auth,
            AiAssistantSessionStore.SessionHandle sessionHandle,
            EngineSessionState state,
            ExecutionMode mode) {

        PendingConfirmation pending = state.pendingConfirmation;
        if (request.confirmationId() == null || request.confirmationDecision() == null
                || pending == null || !pending.confirmationId().equals(request.confirmationId())) {
            sessionStore.discardPending(sessionHandle.id(), owner);
            state.pendingConfirmation = null;
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Yêu cầu xác nhận không hợp lệ hoặc đã hết hạn.");
        }

        state.pendingConfirmation = null;
        sessionStore.takePending(sessionHandle.id(), owner, request.confirmationId());

        if (request.confirmationDecision() == ConfirmationDecision.REJECT) {
            return buildResponse(request, sessionHandle.id(), mode, "COMPLETED",
                    List.of(), List.of(), null, null, "Đã hủy thao tác theo yêu cầu của bạn.");
        }

        // Execute the confirmed tool directly on backend
        int stepIndex = 1;
        AiAssistantToolRegistry.ToolResult toolResult = toolRegistry.execute(
                pending.proposedListName(),
                Map.of());

        List<AssistantStep> steps = new ArrayList<>();
        Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.put("status", toolResult.status());
        resultMap.put("message", toolResult.summary());
        if (toolResult.result() != null) {
            resultMap.put("data", toolResult.result());
        }
        steps.add(new AssistantStep(stepIndex, pending.proposedListName(), toolResult.status(), toolResult.summary(), resultMap));
        List<Source> sources = toolResult.sources() != null ? new ArrayList<>(toolResult.sources()) : new ArrayList<>();

        // Deliver tool result to ADK runner so Gemini can formulate grounded natural language answer
        Content toolResultContent = Content.fromParts(Part.builder()
                .functionResponse(FunctionResponse.builder()
                        .name(pending.proposedListName())
                        .response(resultMap)
                        .build())
                .build());

        return runAdkTurn(request, userId, owner, auth, sessionHandle, state, ExecutionMode.AUTO,
                sessionHandle.consentScopes(), toolResultContent, true, pending.proposedListName(), steps, sources);
    }

    private AiChatResponse classify(AiChatRequest request, UUID sessionId, ExecutionMode mode, Long userId) {
        aiQuota.requireAvailable(userId, userId == null ? request.anonymousViewerId() : null);
        String model = chatSettings.resolve(userId, userId == null ? request.anonymousViewerId() : null);
        try {
            String latest = mapper.writeValueAsString(Map.of("contextType", request.contextType(), "question", request.question()));
            String classifierPrompt = promptLoader.loadClassifierPrompt();
            AiClient.ChatResult result = model == null
                    ? aiClient.classifyChatScope(classifierPrompt, latest)
                    : aiClient.classifyChatScope(model, classifierPrompt, latest);

            if (result == null || result.structuredJson() == null || result.structuredJson().isBlank()) {
                return buildResponse(request, sessionId, mode, "FAILED", List.of(), List.of(), null, null, CLASSIFICATION_UNAVAILABLE_ANSWER);
            }

            var usage = new AiUsageEvent(userId, userId == null ? request.anonymousViewerId() : null,
                    "FR-51", clock.instant(), result.usage());
            aiQuota.record(usage);
            events.publishEvent(usage);

            @SuppressWarnings("unchecked")
            Map<String, Object> decision = mapper.readValue(result.structuredJson(), Map.class);
            if (decision == null) {
                return buildResponse(request, sessionId, mode, "FAILED", List.of(), List.of(), null, null, CLASSIFICATION_UNAVAILABLE_ANSWER);
            }

            String decisionId = decision.get("decisionId") instanceof String v ? v : null;
            String reasonId = decision.get("reasonId") instanceof String v ? v : null;
            if ("MX_CHAT_OK".equals(decisionId) && "NONE".equals(reasonId)) {
                return null; // OK to proceed
            }

            String answer;
            if ("MX_CHAT_OFF_TOPIC".equals(decisionId) && "NONE".equals(reasonId)) {
                answer = "Mình chỉ hỗ trợ ẩm thực chay, công thức, nấu ăn và các tính năng Mâm Xanh.";
            } else if ("MX_CHAT_SENSITIVE".equals(decisionId) && "MEDICAL_RISK".equals(reasonId)) {
                answer = "Mình không thể chẩn đoán, kê điều trị hoặc lập chế độ ăn điều trị bệnh. Bạn hãy trao đổi với bác sĩ hoặc chuyên gia dinh dưỡng y tế.";
            } else if ("MX_CHAT_SENSITIVE".equals(decisionId) && "OTHER_HIGH_RISK".equals(reasonId)) {
                answer = "Mình không thể hỗ trợ yêu cầu có nội dung nhạy cảm hoặc gây hại. Mình có thể hỗ trợ ẩm thực chay và các tính năng Mâm Xanh.";
            } else {
                answer = CLASSIFICATION_UNAVAILABLE_ANSWER;
            }
            return buildResponse(request, sessionId, mode, "COMPLETED", List.of(), List.of(), null, null, answer);
        } catch (AiProviderException exception) {
            LOG.warn("AI request classification failed category={} status={}", exception.kind(), exception.httpStatus());
            return buildResponse(request, sessionId, mode, "FAILED", List.of(), List.of(), null, null, CLASSIFICATION_UNAVAILABLE_ANSWER);
        } catch (JacksonException exception) {
            return buildResponse(request, sessionId, mode, "FAILED", List.of(), List.of(), null, null, CLASSIFICATION_UNAVAILABLE_ANSWER);
        }
    }

    public AiChatResponse cancel(AiChatRequest request) {
        if (request == null || request.sessionId() == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Cần sessionId để dừng lượt chạy.");
        }
        Long userId = users.findAuthenticatedUserId().isPresent() ? users.requireActiveMember().id() : null;
        String owner = userId == null ? "guest:" + request.anonymousViewerId() : "user:" + userId;
        sessionStore.discardPending(request.sessionId(), owner);
        EngineSessionState state = engineSessions.get(request.sessionId());
        if (state != null) {
            state.pendingConfirmation = null;
        }
        return buildResponse(request, request.sessionId(), request.executionMode(), "STOPPED",
                List.of(), List.of(), null, null, "Đã dừng lượt chạy theo yêu cầu của bạn.");
    }

    public boolean ownsSession(UUID sessionId, String owner) {
        if (sessionId == null) return false;
        return sessionStore.peekEngine(sessionId, owner)
                .map(AiAssistantSessionStore.ENGINE_ADK::equals)
                .orElse(false);
    }

    public void deleteSession(UUID sessionId, String anonymousViewerId) {
        if (sessionId == null) return;
        Long userId = users.findAuthenticatedUserId().isPresent() ? users.requireActiveMember().id() : null;
        String owner = userId == null ? "guest:" + anonymousViewerId : "user:" + userId;
        sessionStore.delete(sessionId, owner);
        EngineSessionState state = engineSessions.remove(sessionId);
        if (state != null) {
            state.close();
        }
    }

    @Override
    public void close() {
        engineSessions.values().forEach(EngineSessionState::close);
        engineSessions.clear();
    }

    private void evictExpiredSessions() {
        Instant now = clock.instant();
        engineSessions.entrySet().removeIf(entry -> {
            if (!entry.getValue().expiresAt.isAfter(now)) {
                entry.getValue().close();
                return true;
            }
            return false;
        });
    }

    private String resolveModel(Long userId) {
        String resolved = chatSettings.resolve(userId, null);
        return resolved == null || resolved.isBlank() ? AiTokenUsage.MODEL : resolved;
    }

    private static Authentication createAuthentication(Long userId, String role) {
        String name = userId == null ? "guest" : String.valueOf(userId);
        return UsernamePasswordAuthenticationToken.authenticated(
                name,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    public static ConsentCategory personalCategory(String text) {
        if (text == null || text.isBlank()) return null;
        var matcher = PERSONAL_DATA.matcher(text);
        if (!matcher.find()) return null;
        String matched = matcher.group().toLowerCase(Locale.ROOT);
        if (matched.contains("sức khỏe") || matched.contains("bmi") || matched.contains("calo")
                || matched.contains("dị ứng") || matched.contains("bệnh nền")
                || matched.contains("cân nặng") || matched.contains("chiều cao")) {
            return ConsentCategory.HEALTH_PROFILE;
        }
        if (matched.contains("chế độ ăn") || matched.contains("tùy chọn ăn uống")
                || matched.contains("ăn chay")) {
            return ConsentCategory.DIETARY_PREFERENCES;
        }
        return ConsentCategory.BASIC_PROFILE;
    }

    private static String extractFinalAnswer(List<Event> events) {
        if (events == null || events.isEmpty()) return "";
        for (int i = events.size() - 1; i >= 0; i--) {
            Event event = events.get(i);
            if (event.content().isPresent()) {
                Content content = event.content().get();
                if (content.parts().isPresent()) {
                    for (Part part : content.parts().get()) {
                        if (part.text().isPresent() && !part.text().get().isBlank()) {
                            return part.text().get().strip();
                        }
                    }
                }
            }
        }
        return "";
    }

    private AiChatResponse buildResponse(
            AiChatRequest request,
            UUID sessionId,
            ExecutionMode mode,
            String status,
            List<AssistantStep> steps,
            List<Source> sources,
            ConsentRequest consent,
            PendingConfirmation pending,
            String answer) {
        Instant expires = clock.instant().plus(Duration.ofMinutes(30));
        return new AiChatResponse(
                request.contextType(),
                request.recipeId(),
                answer,
                List.of(),
                null,
                sessionId,
                mode != null ? mode : ExecutionMode.CONFIRM,
                UUID.randomUUID(),
                steps != null ? steps : List.of(),
                consent,
                pending,
                sources != null ? sources : List.of(),
                ENGINE,
                status,
                expires,
                "kb-v1.0.0-20261101");
    }

    private AiChatResponse handleRunFailure(
            AiChatRequest request,
            UUID sessionId,
            ExecutionMode mode,
            List<AssistantStep> steps,
            List<Source> sources,
            Throwable failure) {
        if (failure instanceof java.util.concurrent.TimeoutException) {
            return buildResponse(request, sessionId, mode, "TIMEOUT", steps, sources, null, null,
                    "Yêu cầu xử lý quá thời gian quy định (30 giây). Vui lòng thử lại với câu hỏi ngắn gọn hơn.");
        }
        return buildResponse(request, sessionId, mode, "FAILED", steps, sources, null, null,
                "Dịch vụ AI đang gặp sự cố khi xử lý lượt chạy. Vui lòng thử lại sau.");
    }

    private static LlmFactory createDefaultLlmFactory(String configuredApiKey) {
        return modelId -> {
            String apiKey = (configuredApiKey != null && !configuredApiKey.isBlank())
                    ? configuredApiKey
                    : System.getenv("GEMINI_API_KEY");
            if (apiKey == null || apiKey.isBlank()) {
                throw new AppException(ErrorCode.AI_PROVIDER_UNAVAILABLE, "Chưa cấu hình GEMINI_API_KEY.");
            }
            Client client = Client.builder()
                    .apiKey(apiKey.strip())
                    .httpOptions(HttpOptions.builder()
                            .timeout(10_000)
                            .retryOptions(HttpRetryOptions.builder().attempts(1).build())
                            .build())
                    .build();
            return new Gemini(modelId, client);
        };
    }

    private final class EngineSessionState implements AutoCloseable {
        private final UUID sessionId;
        private final Long userId;
        private final String owner;
        private final Authentication auth;
        private final AtomicBoolean busy = new AtomicBoolean(false);
        private volatile Instant expiresAt;
        private volatile boolean consentRevoked;
        private volatile PendingConfirmation pendingConfirmation;
        private InMemoryRunner runner;
        private String adkSessionId;
        private String boundModelId;

        private EngineSessionState(UUID sessionId, Long userId, String owner, Authentication auth, Instant expiresAt) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.owner = owner;
            this.auth = auth;
            this.expiresAt = expiresAt;
        }

        private synchronized void ensureRunner(
                String modelId,
                Authentication callerAuth,
                ExecutionMode mode,
                Set<ConsentScope> activeConsents,
                List<AssistantStep> steps,
                List<Source> sources,
                AtomicReference<PendingConfirmation> stagedConfirmation) {

            if (runner != null && Objects.equals(boundModelId, modelId)) {
                return;
            }

            closeRunnerOnly();

            String role = userId == null ? "GUEST" : callerAuth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
            List<AiAssistantToolRegistry.ToolDefinition> definitions = toolRegistry.definitionsFor(role);

            List<BaseTool> adkTools = definitions.stream().map(def -> (BaseTool) new ScopedAssistantTool(
                    def, callerAuth, mode, activeConsents, steps, sources, stagedConfirmation)).toList();

            MemberAccountingPolicy policy = new MemberAccountingPolicy(users, aiQuota, events);
            AdkModelGateway gateway = new AdkModelGateway(llmFactory.create(modelId), policy, clock);

            String instructions = promptLoader.loadSystemPrompt() + "\n\n" + promptLoader.loadAgentInstructions();

            LlmAgent agent = LlmAgent.builder()
                    .name("mamxanh_assistant")
                    .description("Mâm Xanh Unified AI Assistant")
                    .instruction(instructions)
                    .model(gateway)
                    .generateContentConfig(GenerateContentConfig.builder().maxOutputTokens(2048).candidateCount(1).build())
                    .beforeModelCallback((context, req) -> {
                        if (req.build().contents().toString().getBytes(StandardCharsets.UTF_8).length > MAX_CONTEXT_BYTES) {
                            return Maybe.error(new IllegalStateException("Model context exceeds byte budget (64 KB)"));
                        }
                        return Maybe.empty();
                    })
                    .tools(adkTools)
                    .build();

            this.runner = new InMemoryRunner(agent, "mamxanh_assistant_runner");
            this.adkSessionId = runner.sessionService().createSession(runner.appName(), owner).blockingGet().id();
            this.boundModelId = modelId;
        }

        private synchronized void closeRunnerOnly() {
            if (runner != null) {
                try {
                    runner.close();
                } catch (Exception ignored) {
                }
                runner = null;
            }
        }

        @Override
        public synchronized void close() {
            closeRunnerOnly();
            pendingConfirmation = null;
        }
    }

    private final class ScopedAssistantTool extends BaseTool {
        private final AiAssistantToolRegistry.ToolDefinition definition;
        private final Authentication auth;
        private final ExecutionMode mode;
        private final Set<ConsentScope> activeConsents;
        private final List<AssistantStep> steps;
        private final List<Source> sources;
        private final AtomicReference<PendingConfirmation> stagedConfirmation;

        private ScopedAssistantTool(
                AiAssistantToolRegistry.ToolDefinition definition,
                Authentication auth,
                ExecutionMode mode,
                Set<ConsentScope> activeConsents,
                List<AssistantStep> steps,
                List<Source> sources,
                AtomicReference<PendingConfirmation> stagedConfirmation) {
            super(definition.name(), definition.description());
            this.definition = definition;
            this.auth = auth;
            this.mode = mode;
            this.activeConsents = activeConsents;
            this.steps = steps;
            this.sources = sources;
            this.stagedConfirmation = stagedConfirmation;
        }

        @Override
        public Optional<FunctionDeclaration> declaration() {
            if (domainTools != null) {
                var decl = domainTools.getDeclaration(definition.name());
                if (decl != null) {
                    return Optional.of(decl);
                }
            }
            return Optional.of(FunctionDeclaration.builder()
                    .name(definition.name())
                    .description(definition.description())
                    .parameters(Schema.fromJson("{\"type\":\"OBJECT\"}"))
                    .build());
        }

        @Override
        public Single<Map<String, Object>> runAsync(Map<String, Object> args, ToolContext context) {
            return Single.fromCallable(() -> asCaller(() -> {
                // Check Consent
                if (!definition.consentCategories().isEmpty()) {
                    for (ConsentCategory cat : definition.consentCategories()) {
                        ConsentScope reqScope = new ConsentScope(cat, definition.write() ? ConsentPurpose.SHOPPING_LIST_WRITE : ConsentPurpose.AI_CHAT);
                        if (!activeConsents.contains(reqScope)) {
                            return Map.of("status", "CONSENT_REQUIRED", "message", "Thao tác cần có sự đồng ý cho phạm vi: " + cat.name());
                        }
                    }
                }

                // Check CONFIRM mode staging for write actions
                if (mode == ExecutionMode.CONFIRM && definition.write()) {
                    UUID planId = UUID.randomUUID();
                    PendingConfirmation pending = new PendingConfirmation(
                            planId,
                            definition.description(),
                            clock.instant().plus(Duration.ofMinutes(5)),
                            true,
                            definition.name());
                    stagedConfirmation.set(pending);
                    return Map.of(
                            "status", "AWAITING_CONFIRMATION",
                            "confirmationId", planId.toString(),
                            "message", "Thao tác cần người dùng xác nhận trước khi thực hiện.");
                }

                // Execute tool via registry
                int stepIndex = steps.size() + 1;
                AiAssistantToolRegistry.ToolResult result = toolRegistry.execute(definition.name(), args);

                if (result.sources() != null) {
                    sources.addAll(result.sources());
                }

                Map<String, Object> resultMap = new LinkedHashMap<>();
                resultMap.put("status", result.status());
                resultMap.put("message", result.summary());
                if (result.result() != null) {
                    resultMap.put("data", result.result());
                }

                steps.add(new AssistantStep(stepIndex, definition.name(), result.status(), result.summary(), resultMap));
                return resultMap;
            }));
        }

        private <T> T asCaller(Callable<T> action) throws Exception {
            var prev = SecurityContextHolder.getContext();
            var ctx = SecurityContextHolder.createEmptyContext();
            ctx.setAuthentication(auth);
            SecurityContextHolder.setContext(ctx);
            try {
                return action.call();
            } finally {
                SecurityContextHolder.setContext(prev);
            }
        }
    }
}

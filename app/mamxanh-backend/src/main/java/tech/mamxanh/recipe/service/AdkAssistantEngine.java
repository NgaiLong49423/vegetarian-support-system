package tech.mamxanh.recipe.service;

import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import tech.mamxanh.recipe.dto.response.AiChatStreamEvent;
import io.reactivex.rxjava3.disposables.Disposable;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
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
import com.google.genai.types.ThinkingConfig;
import com.google.genai.types.ThinkingLevel;
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
import tech.mamxanh.integration.ai.AiTurnDeadline;
import tech.mamxanh.integration.ai.AiPromptLoader;
import tech.mamxanh.integration.ai.AiTokenUsage;
import tech.mamxanh.integration.ai.AiUsageEvent;
import tech.mamxanh.integration.ai.AiProviderException;
import tech.mamxanh.integration.ai.adk.AdkModelGateway;
import tech.mamxanh.integration.ai.adk.MemberAccountingPolicy;
import tech.mamxanh.integration.ai.adk.ModelThinkingPolicy;
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
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
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
    public static final Duration RUN_TIMEOUT = AiTurnDeadline.OPERATIONAL_TURN;
    public static final Duration STEP_TIMEOUT = Duration.ofSeconds(10);
    public static final int MAX_LLM_CALLS = 6;
    public static final int MAX_CONTEXT_BYTES = 64_000;

    private static final String CLASSIFICATION_UNAVAILABLE_ANSWER =
            "Hiện mình chưa thể kiểm tra phạm vi câu hỏi. Câu hỏi chưa được chuyển sang bước trả lời; bạn vui lòng thử lại sau ít phút.";
    private static final Pattern PERSONAL_DATA = Pattern.compile(
            "(?iu)([\\w.+-]+@[\\w.-]+\\.[a-z]{2,}|(?:\\+?84|0)(?:[ .-]?\\d){8,10}|(?:hồ sơ|thông tin) (?:cá nhân|sức khỏe|suc khoe)|hồ sơ.{0,20}(?:tôi|mình)|(?:tôi|mình).{0,20}(?:hồ sơ|sức khỏe|BMI|calo)|sức khỏe (?:của )?(?:tôi|mình)|BMI (?:của )?(?:tôi|mình)|tên (?:hiển thị )?(?:của )?(?:tôi|mình)|bio (?:của )?(?:tôi|mình)|dị ứng|bệnh nền|cân nặng|chiều cao|(?:chế độ ăn|tùy chọn ăn uống) của (?:tôi|mình)|(?:tôi|mình).{0,20}(?:ăn chay|dị ứng|chế độ ăn|tùy chọn ăn uống))");

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
    private final ExecutionTruthfulnessVerifier verifier;
    private final AiAssistantExecutionJournal journal;
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
            @Autowired(required = false) ExecutionTruthfulnessVerifier verifier,
            @Autowired(required = false) AiAssistantExecutionJournal journal,
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
        this.verifier = verifier != null ? verifier : new ExecutionTruthfulnessVerifier();
        this.journal = journal != null ? journal : new AiAssistantExecutionJournal(null, null);
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
        this(users, aiQuota, chatSettings, sessionStore, toolRegistry, null, knowledge, promptLoader, aiClient, mapper, events, clock, llmFactory, null, null, configuredApiKey);
    }

    public AdkAssistantEngine(
            CurrentUserService users,
            AiTokenQuotaService aiQuota,
            AiChatSettingsService chatSettings,
            AiAssistantSessionStore sessionStore,
            AiAssistantToolRegistry toolRegistry,
            AdkDomainTools domainTools,
            ApprovedAiKnowledgeRetriever knowledge,
            AiPromptLoader promptLoader,
            AiClient aiClient,
            ObjectMapper mapper,
            ApplicationEventPublisher events,
            Clock clock,
            LlmFactory llmFactory,
            String configuredApiKey) {
        this(users, aiQuota, chatSettings, sessionStore, toolRegistry, domainTools, knowledge, promptLoader, aiClient, mapper, events, clock, llmFactory, null, null, configuredApiKey);
    }

    public AiChatResponse execute(AiChatRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Nội dung câu hỏi không được để trống.");
        }
        if (request.controlAction() != null) {
            return cancel(request);
        }
        AiTurnDeadline turnDeadline = AiTurnDeadline.after(RUN_TIMEOUT);

        Long userId = users.findAuthenticatedUserId().isPresent() ? users.requireActiveMember().id() : null;
        String owner = userId == null ? "guest:" + request.anonymousViewerId() : "user:" + userId;
        String role = userId == null ? "GUEST" : users.requireActiveMember().role().name();
        validateFormRole(request, role);
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
            state.serverPendingAction = null;
                return buildResponse(request, sessionId, request.executionMode(), "CONSENT_REVOKED",
                        List.of(), List.of(), null, null,
                        "Đã thu hồi sự đồng ý dữ liệu; những lượt tiếp theo sẽ không gửi dữ liệu này tới Google Gemini.");
            }
            state.consentRevoked = false;
        }

        ExecutionMode mode = request.executionMode() == null ? ExecutionMode.CHATBOT : request.executionMode();

        // 2. Handle Confirmation Decision (Human-in-the-loop resume/reject)
        if (request.confirmationDecision() != null || request.confirmationId() != null) {
            return handleConfirmationDecision(request, userId, owner, auth, sessionHandle, state, mode, turnDeadline);
        }

        // Keep a staged write until the user explicitly decides or switches to read-only CHATBOT mode.
        if (state.pendingConfirmation != null) {
            if (request.executionMode() == ExecutionMode.CHATBOT || request.executionMode() == ExecutionMode.QA) {
                sessionStore.discardPending(sessionId, owner);
                state.pendingConfirmation = null;
                state.serverPendingAction = null;
            } else {
                throw new AppException(ErrorCode.VALIDATION_FAILED,
                        "Hãy xác nhận, từ chối hoặc chuyển sang CHATBOT trước khi gửi yêu cầu mới.");
            }
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
        long classificationStarted = System.nanoTime();
        AiChatResponse refusal = classify(request, sessionId, mode, userId, turnDeadline);
        LOG.info("ADK chat classification sessionId={} elapsedMs={} outcome={}", sessionId,
                elapsedMillis(classificationStarted), refusal == null ? "IN_SCOPE" : refusal.runStatus());
        if (turnDeadline.remaining().isZero()) {
            return handleRunFailure(request, sessionId, mode, List.of(), List.of(),
                    new java.util.concurrent.TimeoutException("Turn deadline exceeded during classification"));
        }
        if (refusal != null) {
            return refusal;
        }

        // 5. Execute Turn via ADK Two-Way Agentic Loop
        List<Source> sources = new ArrayList<>();
        String effectivePromptText = consentContinuationPrompt(request, recipeContextPrompt(request));
        if (mode.isChatbot()) {
            long retrievalStarted = System.nanoTime();
            List<Source> retrieved = knowledge != null ? knowledge.retrieve(request.question()) : List.of();
            LOG.info("AI chat retrieval sessionId={} elapsedMs={} resultCount={}", sessionId,
                    elapsedMillis(retrievalStarted), retrieved.size());
            if (!retrieved.isEmpty()) {
                sources.addAll(retrieved);
                StringBuilder kbContext = new StringBuilder();
                kbContext.append(effectivePromptText).append("\n\n[TÀI LIỆU NỘI BỘ THAM KHẢO]:\n");
                for (Source s : retrieved) {
                    kbContext.append("- ").append(s.title()).append(": ").append(s.excerpt()).append("\n");
                }
                effectivePromptText = kbContext.toString();
            }
        }
        if (turnDeadline.remaining().isZero()) {
            return handleRunFailure(request, sessionId, mode, List.of(), sources,
                    new java.util.concurrent.TimeoutException("Turn deadline exceeded before generation"));
        }
        return runAdkTurn(request, userId, owner, auth, sessionHandle, state, mode, activeConsents,
                Content.fromParts(Part.fromText(effectivePromptText)), false, null, new ArrayList<>(), sources, turnDeadline);
    }

    private static void validateFormRole(AiChatRequest request, String role) {
        if (request.formContext() != null && !"EXPERT".equals(role)) throw new AppException(ErrorCode.ACCESS_DENIED);
    }

    private AiAssistantToolRegistry.ToolResult proposeRecipeFormPatch(AiChatRequest.FormContext form, Map<String, Object> args) {
        if (form == null || !(args.getOrDefault("fields", Map.of()) instanceof Map<?, ?> fields)
                || !(args.getOrDefault("appendText", Map.of()) instanceof Map<?, ?> appendText)
                || fields.size() + appendText.size() > 10)
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Cần biểu mẫu công thức đang mở và các trường hợp lệ.");
        Set<String> allowed = Set.of("title", "description", "instructions", "dishCategory", "vegetarianType",
                "difficulty", "servings", "prepTimeMinutes", "cookTimeMinutes", "youtubeUrl");
        Map<String, String> changes = new LinkedHashMap<>();
        for (var entry : fields.entrySet()) {
            if (!(entry.getKey() instanceof String key) || !allowed.contains(key)
                    || !(entry.getValue() instanceof String value) || value.length() > 5000)
                throw new AppException(ErrorCode.VALIDATION_FAILED, "Patch chứa trường hoặc giá trị không hợp lệ.");
            changes.put(key, value);
        }
        for (var entry : appendText.entrySet()) {
            if (!(entry.getKey() instanceof String key) || !Set.of("description", "instructions").contains(key)
                    || changes.containsKey(key) || !(entry.getValue() instanceof String suffix)
                    || suffix.isBlank() || suffix.length() > 5000)
                throw new AppException(ErrorCode.VALIDATION_FAILED, "Nội dung nối thêm chỉ hỗ trợ mô tả hoặc hướng dẫn hợp lệ.");
            String current = form.fields().getOrDefault(key, "");
            String addition = suffix.strip();
            String updated = current.contains(addition) ? current
                    : current.isBlank() ? addition : current.stripTrailing() + "\n\n" + addition;
            int maxLength = "description".equals(key) ? 2000 : 5000;
            if (updated.length() > maxLength)
                throw new AppException(ErrorCode.VALIDATION_FAILED, "Nội dung sau khi nối vượt giới hạn của biểu mẫu.");
            changes.put(key, updated);
        }
        Object rawIngredients = args.getOrDefault("addIngredients", List.of());
        if (!(rawIngredients instanceof List<?> additions) || additions.size() > 50 || (changes.isEmpty() && additions.isEmpty()))
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Danh sách nguyên liệu đề xuất không hợp lệ.");
        List<Map<String, String>> ingredients = new ArrayList<>();
        for (Object item : additions) {
            if (!(item instanceof Map<?, ?> row) || !(row.get("name") instanceof String name)
                    || !(row.get("quantity") instanceof String quantity) || !(row.get("unitCode") instanceof String unit)
                    || name.isBlank() || name.length() > 200 || quantity.length() > 40 || unit.isBlank() || unit.length() > 40)
                throw new AppException(ErrorCode.VALIDATION_FAILED, "Nguyên liệu đề xuất thiếu tên, số lượng hoặc đơn vị.");
            try {
                if (new java.math.BigDecimal(quantity).signum() <= 0) throw new NumberFormatException();
            } catch (NumberFormatException invalid) {
                throw new AppException(ErrorCode.VALIDATION_FAILED, "Số lượng nguyên liệu phải lớn hơn 0.");
            }
            ingredients.add(Map.of("name", name.strip(), "quantity", quantity.strip(), "unitCode", unit.strip()));
        }
        return new AiAssistantToolRegistry.ToolResult("UI_ACTION_REQUIRED", "Đang chờ giao diện xác nhận thay đổi biểu mẫu.",
                Map.of("actionId", UUID.randomUUID().toString(), "formId", form.formId().toString(),
                        "revision", form.revision(), "fields", Map.copyOf(changes), "addIngredients", List.copyOf(ingredients)), List.of());
    }

    private String recipeContextPrompt(AiChatRequest request) {
        try {
            StringBuilder prompt = new StringBuilder(request.question());
            if (request.contextType() == AiChatRequest.ContextType.RECIPE && request.recipeId() != null) {
                AiAssistantToolRegistry.ToolResult recipe = toolRegistry.execute("get_recipe_detail", Map.of("recipeId", request.recipeId()));
                prompt.append("\n\n[NGỮ CẢNH CÔNG THỨC CÔNG KHAI HIỆN TẠI]:\n").append(mapper.writeValueAsString(recipe.result()));
            }
            if (request.formContext() != null) {
                prompt.append("\n\n[FORM CÔNG THỨC CHƯA LƯU, DỮ LIỆU NGƯỜI DÙNG CHƯA XÁC THỰC]:\n")
                        .append(mapper.writeValueAsString(Map.of("formId", request.formContext().formId(),
                                "revision", request.formContext().revision(), "fields", request.formContext().fields(),
                                "ingredients", request.formContext().ingredients(), "mediaCount", request.formContext().mediaCount(),
                                "mediaUploading", request.formContext().mediaUploading())));
            }
            return prompt.toString();
        } catch (JacksonException failure) {
            throw new IllegalStateException("Không thể tạo ngữ cảnh công thức công khai.", failure);
        }
    }

    private static String consentContinuationPrompt(AiChatRequest request, String prompt) {
        if (request.consentDecision() == null || request.consentDecision().action() != ConsentAction.GRANT) {
            return prompt;
        }
        // Consent is a structured user decision, not a natural-language instruction. Make the
        // continuation explicit to ADK while keeping all actual authorization in the tool guard.
        return prompt + "\n\n[TIẾP TỤC SAU KHI NGƯỜI DÙNG ĐỒNG Ý DỮ LIỆU]:\n"
                + "Người dùng vừa cấp consent theo các scope có cấu trúc trong request này. Hãy tiếp tục yêu cầu "
                + "đang chờ trong hội thoại và thử lại công cụ đọc cần thiết nếu phù hợp. Backend vẫn kiểm tra "
                + "từng scope; nếu công cụ trả CONSENT_REQUIRED thì dừng và yêu cầu đúng scope còn thiếu. "
                + "Consent không phải xác nhận thao tác ghi; chỉ thực hiện đúng hành động người dùng đã yêu cầu. "
                + "Nếu người dùng chỉ yêu cầu xem trước, không áp dụng hoặc lưu dữ liệu. "
                + "Không tuyên bố đã hoàn thành nếu chưa có kết quả thực tế từ công cụ/backend.";
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
                inputContent, approvalResume, approvedConfirmationCallId, new ArrayList<>(), new ArrayList<>(),
                AiTurnDeadline.after(RUN_TIMEOUT));
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
            List<Source> initialSources,
            AiTurnDeadline turnDeadline) {

        String resolvedModel = resolveModel(userId, request.anonymousViewerId());
        List<AssistantStep> steps = initialSteps != null ? initialSteps : new ArrayList<>();
        List<Source> sources = initialSources != null ? initialSources : new ArrayList<>();
        AtomicReference<PendingConfirmation> stagedConfirmation = new AtomicReference<>();
        AtomicReference<Set<ConsentScope>> requiredConsentScopes = new AtomicReference<>(Set.of());
        String role = userId == null ? "GUEST" : auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");

        try {
            EngineSessionState.TurnContext turnContext = new EngineSessionState.TurnContext(
                    auth, mode, activeConsents, steps, sources, stagedConfirmation, request.clientRequestId(),
                    approvalResume, approvedConfirmationCallId, null, new AiChatCancellationSignal(), requiredConsentScopes, request.formContext(),
                    turnDeadline);
            state.currentTurn.set(turnContext);
            state.ensureRunner(resolvedModel, role, mode, request);
        } catch (RuntimeException unavailable) {
            LOG.warn("Failed to initialize ADK runner: {}", unavailable.getMessage());
            return buildResponse(request, sessionHandle.id(), mode, "FAILED", List.of(), List.of(), null, null,
                    "Không thể khởi tạo AI với model hoặc cấu hình hiện tại. Vui lòng thử lại sau.");
        }

        if (!state.busy.compareAndSet(false, true)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Phiên trợ lý đang xử lý một lượt chạy khác. Vui lòng đợi lượt hiện tại hoàn tất.");
        }

        long runStarted = System.nanoTime();
        List<Event> emittedEvents;
        try {
            emittedEvents = turnDeadline.enforce(state.runner.runAsync(owner, state.adkSessionId, inputContent,
                    RunConfig.builder().maxLlmCalls(MAX_LLM_CALLS).toolExecutionMode(RunConfig.ToolExecutionMode.SEQUENTIAL).build()),
                    "Run deadline exceeded").toList().blockingGet();
        } catch (RuntimeException failure) {
            LOG.warn("ADK turn execution failed sessionId={} elapsedMs={} toolSteps={} cause={}",
                    sessionHandle.id(), elapsedMillis(runStarted), steps.size(),
                    isTimeout(failure) ? "TIMEOUT" : failure.getClass().getSimpleName());
            state.busy.set(false);
            return handleRunFailure(request, sessionHandle.id(), mode, steps, sources, failure);
        } finally {
            state.busy.set(false);
        }
        LOG.info("ADK turn completed sessionId={} elapsedMs={} toolSteps={}",
                sessionHandle.id(), elapsedMillis(runStarted), steps.size());

        Set<ConsentScope> missingScopes = requiredConsentScopes.get();
        if (!missingScopes.isEmpty()) {
            return buildResponse(request, sessionHandle.id(), mode, "AWAITING_CONSENT",
                    steps, sources, new ConsentRequest(List.copyOf(missingScopes), "Google Gemini"), null,
                    "Để tiếp tục thao tác, vui lòng đồng ý đúng phạm vi dữ liệu được yêu cầu.");
        }

        // Check if a tool staged a confirmation in CONFIRM mode
        PendingConfirmation pending = stagedConfirmation.get();
        if (pending != null) {
            state.pendingConfirmation = pending;
            return buildResponse(request, sessionHandle.id(), mode, "AWAITING_CONFIRMATION",
                    steps, sources, null, pending,
                    verifier != null ? verifier.verify("Mình đã chuẩn bị thao tác \"" + pending.proposedListName() + "\". Bạn vui lòng kiểm tra và bấm Xác nhận để hoàn tất.", mode, steps, pending) : "Mình đã chuẩn bị thao tác \"" + pending.proposedListName() + "\". Bạn vui lòng kiểm tra và bấm Xác nhận để hoàn tất.");
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

        if (steps.stream().anyMatch(step -> "UI_ACTION_REQUIRED".equals(step.status()))) answer = "Mình đã đề xuất thay đổi và đang chờ giao diện xác nhận.";
        String verifiedAnswer = verifier != null ? verifier.verify(answer, mode, steps, null) : answer;
        return buildResponse(request, sessionHandle.id(), mode, "COMPLETED", steps, sources, null, null, verifiedAnswer);
    }

    private AiChatResponse handleConfirmationDecision(
            AiChatRequest request,
            Long userId,
            String owner,
            Authentication auth,
            AiAssistantSessionStore.SessionHandle sessionHandle,
            EngineSessionState state,
            ExecutionMode mode,
            AiTurnDeadline turnDeadline) {

        String toolName;
        Map<String, Object> arguments;
        String actionId;
        long ownerId;
        UUID effectiveRequestId;
        UUID confirmationId = request.confirmationId();

        synchronized (state) {
            AiAssistantSessionStore.PendingPlan storePlan = sessionStore.takePending(sessionHandle.id(), owner, confirmationId);
            ServerPendingAction pendingAction = state.serverPendingAction;

            if (confirmationId == null || request.confirmationDecision() == null
                    || (pendingAction == null && storePlan == null)
                    || (pendingAction != null && !pendingAction.confirmationId().equals(confirmationId))
                    || (pendingAction != null && clock.instant().isAfter(pendingAction.expiresAt()))
                    || (storePlan != null && !storePlan.id().equals(confirmationId))) {
                sessionStore.discardPending(sessionHandle.id(), owner);
                state.pendingConfirmation = null;
                state.serverPendingAction = null;
                throw new AppException(ErrorCode.VALIDATION_FAILED, "Yêu cầu xác nhận không hợp lệ hoặc đã hết hạn.");
            }

            if (pendingAction != null) {
                toolName = pendingAction.toolName();
                arguments = pendingAction.immutableArguments();
                actionId = pendingAction.actionId();
                ownerId = pendingAction.userId() != null ? pendingAction.userId() : (userId != null ? userId : 0L);
                effectiveRequestId = pendingAction.clientRequestId() != null ? pendingAction.clientRequestId() : (request.clientRequestId() != null ? request.clientRequestId() : sessionHandle.id());
            } else {
                var act = storePlan.actions().get(0);
                toolName = act.tool();
                arguments = act.arguments();
                actionId = toolName + "_1";
                ownerId = userId != null ? userId : 0L;
                effectiveRequestId = request.clientRequestId() != null ? request.clientRequestId() : sessionHandle.id();
            }

            if (arguments == null) {
                sessionStore.discardPending(sessionHandle.id(), owner);
                state.pendingConfirmation = null;
                state.serverPendingAction = null;
                throw new AppException(ErrorCode.VALIDATION_FAILED, "Tham số của thao tác cần xác nhận không tồn tại.");
            }

            if (request.confirmationDecision() == ConfirmationDecision.REJECT) {
                sessionStore.markPendingRejected(sessionHandle.id(), owner, confirmationId);
                state.pendingConfirmation = null;
                state.serverPendingAction = null;
                return buildResponse(request, sessionHandle.id(), mode, "COMPLETED",
                        List.of(), List.of(), null, null, "Đã hủy thao tác theo yêu cầu của bạn.");
            }
            if ("apply_ai_menu_preview".equals(toolName)
                    && (state.menuPreview == null || !state.menuPreview.id().toString().equals(arguments.get("previewId")))) {
                sessionStore.discardPending(sessionHandle.id(), owner);
                state.serverPendingAction = null;
                state.pendingConfirmation = null;
                throw new AppException(ErrorCode.VALIDATION_FAILED, "Bản xem trước đã thay đổi hoặc hết hạn; hãy tạo lại thực đơn.");
            }
            if ("create_recipe_post".equals(toolName)) {
                var form = request.formContext();
                var staged = state.pendingPublishForm;
                if (form == null || staged == null || form.mediaUploading()
                        || !staged.formId().equals(form.formId()) || staged.revision() != form.revision()
                        || form.publishPayload() == null
                        || !mapper.valueToTree(arguments).equals(mapper.valueToTree(form.publishPayload()))) {
                    sessionStore.discardPending(sessionHandle.id(), owner);
                    state.serverPendingAction = null;
                    state.pendingConfirmation = null;
                    state.pendingPublishForm = null;
                    throw new AppException(ErrorCode.VALIDATION_FAILED, "Biểu mẫu đã thay đổi; hãy kiểm tra và xác nhận lại.");
                }
            }
        }

        // Calculate argument hash for journal idempotency
        String argsJson = "";
        try {
            argsJson = mapper.writeValueAsString(arguments);
        } catch (Exception ignored) {
        }
        String argumentsHash = hash(actionId + ":" + toolName + ":" + argsJson);

        // Execute the confirmed tool directly on backend
        int stepIndex = 1;
        AiAssistantToolRegistry.ToolResult toolResult;
        try {
            toolResult = journal != null
                    ? journal.executeWithJournal(ownerId, effectiveRequestId, actionId, toolName, argumentsHash, () -> toolRegistry.execute(toolName, arguments))
                    : toolRegistry.execute(toolName, arguments);
            if ("apply_ai_menu_preview".equals(toolName) && "SUCCEEDED".equals(toolResult.status())) state.menuPreview = null;
            sessionStore.markPendingExecuted(sessionHandle.id(), owner, confirmationId);
        } catch (Exception failure) {
            sessionStore.markPendingFailed(sessionHandle.id(), owner, confirmationId);
            LOG.warn("Confirmed tool execution failed for tool={}: {}", toolName, failure.getMessage());
            if (failure instanceof AppException appEx) {
                throw appEx;
            }
            throw new AppException(ErrorCode.AI_RESPONSE_INVALID, "Thao tác không thể hoàn tất: " + failure.getMessage());
        } finally {
            synchronized (state) {
                state.pendingConfirmation = null;
                state.serverPendingAction = null;
            }
        }

        List<AssistantStep> steps = new ArrayList<>();
        Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.put("status", toolResult.status());
        resultMap.put("message", toolResult.summary());
        if (toolResult.result() != null) {
            resultMap.put("data", toolResult.result());
        }
        steps.add(new AssistantStep(stepIndex, toolName, toolResult.status(), toolResult.summary(), resultMap));
        List<Source> sources = toolResult.sources() != null ? new ArrayList<>(toolResult.sources()) : new ArrayList<>();

        // Deliver tool result to ADK runner so Gemini can formulate grounded natural language answer
        Content toolResultContent = Content.fromParts(Part.builder()
                .functionResponse(FunctionResponse.builder()
                        .name(toolName)
                        .response(resultMap)
                        .build())
                .build());

        return runAdkTurn(request, userId, owner, auth, sessionHandle, state, mode,
                sessionHandle.consentScopes(), toolResultContent, true, toolName, steps, sources, turnDeadline);
    }

    private AiChatResponse classify(AiChatRequest request, UUID sessionId, ExecutionMode mode, Long userId,
            AiTurnDeadline turnDeadline) {
        aiQuota.requireAvailable(userId, userId == null ? request.anonymousViewerId() : null);
        String model = chatSettings.resolve(userId, userId == null ? request.anonymousViewerId() : null);
        try {
            Duration classifierBudget = turnDeadline.modelCallBudget(AiTurnDeadline.OPERATIONAL_CLASSIFIER_CALL);
            String latest = mapper.writeValueAsString(Map.of("contextType", request.contextType(), "question", request.question()));
            String classifierPrompt = promptLoader.loadClassifierPrompt();
            AiClient.ChatResult result = model == null
                    ? aiClient.classifyChatScope(classifierPrompt, latest, classifierBudget)
                    : aiClient.classifyChatScope(model, classifierPrompt, latest, classifierBudget);

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
            LOG.warn("AI request classification failed category={} status={} message={}: {}",
                    exception.kind(), exception.httpStatus(), exception.getMessage(),
                    exception.getCause() != null ? exception.getCause().getMessage() : "no cause");
            if (exception.kind() == AiProviderException.FailureKind.TIMEOUT || turnDeadline.remaining().isZero()) {
                return handleRunFailure(request, sessionId, mode, List.of(), List.of(), exception);
            }
            return buildResponse(request, sessionId, mode, "FAILED", List.of(), List.of(), null, null, CLASSIFICATION_UNAVAILABLE_ANSWER);
        } catch (JacksonException exception) {
            LOG.warn("AI classification JSON parsing failed: {}", exception.getMessage());
            return buildResponse(request, sessionId, mode, "FAILED", List.of(), List.of(), null, null, CLASSIFICATION_UNAVAILABLE_ANSWER);
        } catch (IllegalStateException deadlineFailure) {
            if (turnDeadline.remaining().isZero()) {
                return handleRunFailure(request, sessionId, mode, List.of(), List.of(), deadlineFailure);
            }
            throw deadlineFailure;
        }
    }

    public void stream(AiChatRequest request, Consumer<AiChatStreamEvent> consumer) {
        stream(request, consumer, new AiChatCancellationSignal());
    }

    public void stream(AiChatRequest request, Consumer<AiChatStreamEvent> consumer,
            AiChatCancellationSignal cancellationSignal) {
        if (request == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Yêu cầu không được để trống.");
        }
        if (consumer == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Bộ nhận sự kiện không được để trống.");
        }
        AiTurnDeadline turnDeadline = AiTurnDeadline.after(RUN_TIMEOUT);
        AiChatCancellationSignal effectiveCancellationSignal = cancellationSignal == null
                ? new AiChatCancellationSignal() : cancellationSignal;
        if (effectiveCancellationSignal.isCancelled()) {
            consumer.accept(AiChatStreamEvent.complete(buildResponse(request, request.sessionId(), request.executionMode(),
                    "STOPPED", List.of(), List.of(), null, null,
                    "Đã dừng lượt chạy theo yêu cầu của bạn.")));
            return;
        }

        Long userId = users.findAuthenticatedUserId().isPresent() ? users.requireActiveMember().id() : null;
        String owner = userId == null ? "guest:" + request.anonymousViewerId() : "user:" + userId;
        String role = userId == null ? "GUEST" : users.requireActiveMember().role().name();
        validateFormRole(request, role);
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
                state.serverPendingAction = null;
                AiChatResponse response = buildResponse(request, sessionId, request.executionMode(), "CONSENT_REVOKED",
                        List.of(), List.of(), null, null,
                        "Đã thu hồi sự đồng ý dữ liệu; những lượt tiếp theo sẽ không gửi dữ liệu này tới Google Gemini.");
                consumer.accept(AiChatStreamEvent.complete(response));
                return;
            }
            state.consentRevoked = false;
        }

        ExecutionMode mode = request.executionMode() == null ? ExecutionMode.CHATBOT : request.executionMode();

        // 2. Handle Confirmation Decision (Human-in-the-loop resume/reject)
        if (request.confirmationDecision() != null || request.confirmationId() != null) {
            AiChatResponse resp = handleConfirmationDecision(request, userId, owner, auth, sessionHandle, state, mode, turnDeadline);
            consumer.accept(AiChatStreamEvent.complete(resp));
            return;
        }

        // Keep a staged write until the user explicitly decides or switches to read-only CHATBOT mode.
        if (state.pendingConfirmation != null) {
            if (request.executionMode() == ExecutionMode.CHATBOT || request.executionMode() == ExecutionMode.QA) {
                sessionStore.discardPending(sessionId, owner);
                state.pendingConfirmation = null;
                state.serverPendingAction = null;
            } else {
                consumer.accept(AiChatStreamEvent.error(
                        "Hãy xác nhận, từ chối hoặc chuyển sang CHATBOT trước khi gửi yêu cầu mới.", "VALIDATION_FAILED"));
                return;
            }
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
                AiChatResponse resp = buildResponse(request, sessionId, mode, "AWAITING_CONSENT",
                        List.of(), List.of(), new ConsentRequest(List.of(requiredScope), "Google Gemini"), null,
                        "Để gửi nội dung có dữ liệu cá nhân tới Google Gemini, vui lòng đồng ý đúng phạm vi trước.");
                consumer.accept(AiChatStreamEvent.complete(resp));
                return;
            }
        }

        // 4. Preflight Scope Classification (FR-51 / BR-09 / BR-41)
        long classificationStarted = System.nanoTime();
        AiChatResponse refusal = classify(request, sessionId, mode, userId, turnDeadline);
        LOG.info("ADK chat classification sessionId={} elapsedMs={} outcome={}", sessionId,
                elapsedMillis(classificationStarted), refusal == null ? "IN_SCOPE" : refusal.runStatus());
        if (turnDeadline.remaining().isZero()) {
            consumer.accept(AiChatStreamEvent.complete(handleRunFailure(request, sessionId, mode, List.of(), List.of(),
                    new java.util.concurrent.TimeoutException("Turn deadline exceeded during classification"))));
            return;
        }
        if (refusal != null) {
            consumer.accept(AiChatStreamEvent.complete(refusal));
            return;
        }
        if (effectiveCancellationSignal.isCancelled()) {
            consumer.accept(AiChatStreamEvent.complete(buildResponse(request, sessionId, mode, "STOPPED",
                    List.of(), List.of(), null, null, "Đã dừng lượt chạy theo yêu cầu của bạn.")));
            return;
        }

        // 5. Execute Turn via ADK Two-Way Agentic Loop with Streaming
        List<Source> sources = new ArrayList<>();
        String effectivePromptText = consentContinuationPrompt(request, recipeContextPrompt(request));
        if (mode.isChatbot()) {
            long retrievalStarted = System.nanoTime();
            List<Source> retrieved = knowledge != null ? knowledge.retrieve(request.question()) : List.of();
            LOG.info("AI chat retrieval sessionId={} elapsedMs={} resultCount={}", sessionId,
                    elapsedMillis(retrievalStarted), retrieved.size());
            if (!retrieved.isEmpty()) {
                sources.addAll(retrieved);
                StringBuilder kbContext = new StringBuilder();
                kbContext.append(effectivePromptText).append("\n\n[TÀI LIỆU NỘI BỘ THAM KHẢO]:\n");
                for (Source s : retrieved) {
                    kbContext.append("- ").append(s.title()).append(": ").append(s.excerpt()).append("\n");
                }
                effectivePromptText = kbContext.toString();
            }
        }
        if (turnDeadline.remaining().isZero()) {
            consumer.accept(AiChatStreamEvent.complete(handleRunFailure(request, sessionId, mode, List.of(), sources,
                    new java.util.concurrent.TimeoutException("Turn deadline exceeded before generation"))));
            return;
        }

        runAdkTurnStream(request, userId, owner, auth, sessionHandle, state, mode, activeConsents,
                Content.fromParts(Part.fromText(effectivePromptText)), false, null, new ArrayList<>(), sources, consumer,
                effectiveCancellationSignal, turnDeadline);
    }

    private void runAdkTurnStream(
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
            List<Source> initialSources,
            Consumer<AiChatStreamEvent> consumer,
            AiChatCancellationSignal cancellationSignal,
            AiTurnDeadline turnDeadline) {

        String resolvedModel = resolveModel(userId, request.anonymousViewerId());
        List<AssistantStep> steps = initialSteps != null ? initialSteps : new ArrayList<>();
        List<Source> sources = initialSources != null ? initialSources : new ArrayList<>();
        AtomicReference<PendingConfirmation> stagedConfirmation = new AtomicReference<>();
        AtomicReference<Set<ConsentScope>> requiredConsentScopes = new AtomicReference<>(Set.of());

        try {
            String role = userId == null ? "GUEST" : auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
            state.currentTurn.set(new EngineSessionState.TurnContext(
                    auth, mode, activeConsents, steps, sources, stagedConfirmation, request.clientRequestId(),
                    approvalResume, approvedConfirmationCallId,
                    step -> consumer.accept(AiChatStreamEvent.step(step)), cancellationSignal, requiredConsentScopes, request.formContext(),
                    turnDeadline));
            state.ensureRunner(resolvedModel, role, mode, request);
        } catch (RuntimeException unavailable) {
            LOG.warn("Failed to initialize ADK runner: {}", unavailable.getMessage());
            AiChatResponse resp = buildResponse(request, sessionHandle.id(), mode, "FAILED", List.of(), List.of(), null, null,
                    "Không thể khởi tạo AI với model hoặc cấu hình hiện tại. Vui lòng thử lại sau.");
            consumer.accept(AiChatStreamEvent.complete(resp));
            return;
        }

        if (!state.busy.compareAndSet(false, true)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Phiên trợ lý đang xử lý một lượt chạy khác. Vui lòng đợi lượt hiện tại hoàn tất.");
        }

        long runStarted = System.nanoTime();
        List<Event> emittedEvents = new ArrayList<>();
        StringBuilder streamedText = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> runFailure = new AtomicReference<>();
        state.activeLatch = latch;
        state.turnCancelled.set(cancellationSignal.isCancelled());
        if (cancellationSignal.isCancelled()) {
            state.busy.set(false);
            consumer.accept(AiChatStreamEvent.complete(buildResponse(request, sessionHandle.id(), mode, "STOPPED",
                    List.of(), List.of(), null, null, "Đã dừng lượt chạy theo yêu cầu của bạn.")));
            return;
        }

        Flowable<Event> flow = turnDeadline.enforce(state.runner.runAsync(owner, state.adkSessionId, inputContent,
                RunConfig.builder().maxLlmCalls(MAX_LLM_CALLS)
                        .toolExecutionMode(RunConfig.ToolExecutionMode.SEQUENTIAL)
                        .streamingMode(RunConfig.StreamingMode.SSE).build()), "Run deadline exceeded");

        Disposable disposable = flow.subscribe(
                event -> {
                    emittedEvents.add(event);
                    if (event.content().isPresent()) {
                        Content content = event.content().get();
                        if (content.parts().isPresent()) {
                            for (Part part : content.parts().get()) {
                                if (part.text().isPresent() && !part.text().get().isBlank()) {
                                    String text = part.text().get();
                                    streamedText.append(text);
                                    consumer.accept(AiChatStreamEvent.delta(text));
                                }
                            }
                        }
                    }
                },
                error -> {
                    if (isTimeout(error)) cancellationSignal.cancel();
                    runFailure.set(error);
                    latch.countDown();
                },
                latch::countDown
        );

        state.activeDisposable = disposable;
        if (cancellationSignal.isCancelled()) {
            disposable.dispose();
            latch.countDown();
        }

        try {
            latch.await();
        } catch (InterruptedException ie) {
            disposable.dispose();
            Thread.currentThread().interrupt();
        } finally {
            state.busy.set(false);
            state.activeDisposable = null;
            state.activeLatch = null;
        }

        if (state.turnCancelled.get()) {
            AiChatResponse stopped = buildResponse(request, sessionHandle.id(), mode, "STOPPED",
                    steps, sources, null, null, "Đã dừng lượt chạy theo yêu cầu của bạn.");
            consumer.accept(AiChatStreamEvent.complete(stopped));
            return;
        }

        if (runFailure.get() != null) {
            Throwable failure = runFailure.get();
            LOG.warn("ADK turn streaming failed sessionId={} elapsedMs={} toolSteps={} cause={}",
                    sessionHandle.id(), elapsedMillis(runStarted), steps.size(),
                    isTimeout(failure) ? "TIMEOUT" : failure.getClass().getSimpleName());
            AiChatResponse failureResp = handleRunFailure(request, sessionHandle.id(), mode, steps, sources,
                    failure instanceof RuntimeException re ? re : new RuntimeException(failure));
            consumer.accept(AiChatStreamEvent.complete(failureResp));
            return;
        }

        LOG.info("ADK turn stream completed sessionId={} elapsedMs={} toolSteps={}",
                sessionHandle.id(), elapsedMillis(runStarted), steps.size());

        Set<ConsentScope> missingScopes = requiredConsentScopes.get();
        if (!missingScopes.isEmpty()) {
            AiChatResponse response = buildResponse(request, sessionHandle.id(), mode, "AWAITING_CONSENT",
                    steps, sources, new ConsentRequest(List.copyOf(missingScopes), "Google Gemini"), null,
                    "Để tiếp tục thao tác, vui lòng đồng ý đúng phạm vi dữ liệu được yêu cầu.");
            consumer.accept(AiChatStreamEvent.complete(response));
            return;
        }

        // Check if a tool staged a confirmation
        PendingConfirmation pending = stagedConfirmation.get();
        if (pending != null) {
            state.pendingConfirmation = pending;
            AiChatResponse resp = buildResponse(request, sessionHandle.id(), mode, "AWAITING_CONFIRMATION",
                    steps, sources, null, pending,
                    verifier != null ? verifier.verify("Mình đã chuẩn bị thao tác \"" + pending.proposedListName() + "\". Bạn vui lòng kiểm tra và bấm Xác nhận để hoàn tất.", mode, steps, pending) : "Mình đã chuẩn bị thao tác \"" + pending.proposedListName() + "\". Bạn vui lòng kiểm tra và bấm Xác nhận để hoàn tất.");
            consumer.accept(AiChatStreamEvent.confirmation(pending));
            consumer.accept(AiChatStreamEvent.complete(resp));
            return;
        }

        // Extract Gemini final natural language answer
        String answer = extractFinalAnswer(emittedEvents);
        if (answer.isBlank() && streamedText.length() > 0) {
            answer = streamedText.toString().strip();
        }
        if (answer.isBlank()) {
            if (!steps.isEmpty()) {
                answer = steps.getLast().summary() != null ? steps.getLast().summary() : "Thao tác đã được thực hiện thành công.";
            } else {
                answer = "Mình đã xử lý yêu cầu của bạn.";
            }
        }

        if (steps.stream().anyMatch(step -> "UI_ACTION_REQUIRED".equals(step.status()))) answer = "Mình đã đề xuất thay đổi và đang chờ giao diện xác nhận.";
        String verifiedAnswer = verifier != null ? verifier.verify(answer, mode, steps, null) : answer;
        AiChatResponse response = buildResponse(request, sessionHandle.id(), mode, "COMPLETED", steps, sources, null, null, verifiedAnswer);
        consumer.accept(AiChatStreamEvent.complete(response));
    }

    public AiChatResponse cancel(AiChatRequest request) {
        return cancel(request, null);
    }

    public AiChatResponse cancel(AiChatRequest request, AiChatCancellationSignal cancellationSignal) {
        if (request == null || (request.sessionId() == null && request.clientRequestId() == null)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Cần sessionId hoặc clientRequestId để dừng lượt chạy.");
        }
        if (cancellationSignal != null) cancellationSignal.cancel();
        Long userId = users.findAuthenticatedUserId().isPresent() ? users.requireActiveMember().id() : null;
        String owner = userId == null ? "guest:" + request.anonymousViewerId() : "user:" + userId;
        UUID activeSessionId = request.sessionId();
        EngineSessionState state = activeSessionId == null ? null : engineSessions.get(activeSessionId);
        if (state != null && state.busy.get() && request.clientRequestId() != null) {
            EngineSessionState.TurnContext turn = state.currentTurn.get();
            if (turn != null && !request.clientRequestId().equals(turn.clientRequestId())) {
                throw new AppException(ErrorCode.VALIDATION_FAILED, "Yêu cầu dừng không khớp với lượt đang chạy.");
            }
        }
        if (state == null && request.clientRequestId() != null) {
            state = engineSessions.values().stream()
                    .filter(candidate -> candidate.owner.equals(owner))
                    .filter(candidate -> {
                        EngineSessionState.TurnContext turn = candidate.currentTurn.get();
                        return turn != null && request.clientRequestId().equals(turn.clientRequestId());
                    })
                    .findFirst().orElse(null);
            if (state != null) activeSessionId = state.sessionId;
        }
        if (state != null && !state.owner.equals(owner)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Phiên trợ lý không hợp lệ hoặc đã hết hạn.");
        }
        if (activeSessionId != null) sessionStore.discardPending(activeSessionId, owner);
        if (state != null) {
            state.cancelActiveTurn();
            state.pendingConfirmation = null;
            state.serverPendingAction = null;
        }
        return buildResponse(request, activeSessionId, request.executionMode(), "STOPPED",
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

    private String resolveModel(Long userId, String anonymousViewerId) {
        try {
            String resolved = chatSettings.resolve(userId, userId == null ? anonymousViewerId : null);
            return resolved == null || resolved.isBlank() ? AiTokenUsage.MODEL : resolved;
        } catch (Exception e) {
            return AiTokenUsage.MODEL;
        }
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
        if (matched.contains("sức khỏe") || matched.contains("dinh dưỡng") || matched.contains("bmi") || matched.contains("calo")
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

    private static ConsentPurpose consentPurposeFor(ConsentCategory category, boolean write) {
        return switch (category) {
            case BASIC_PROFILE -> write ? ConsentPurpose.PROFILE_UPDATE : ConsentPurpose.PROFILE_READ;
            case DIETARY_PREFERENCES -> write ? ConsentPurpose.DIETARY_UPDATE : ConsentPurpose.DIETARY_READ;
            case HEALTH_PROFILE -> write ? ConsentPurpose.HEALTH_PROFILE_UPDATE : ConsentPurpose.HEALTH_PROFILE_READ;
            case SHOPPING_LIST -> write ? ConsentPurpose.SHOPPING_LIST_WRITE : ConsentPurpose.SHOPPING_LIST_READ;
        };
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
        String workflowUrl = navigationWorkflowUrl(steps);
        boolean navigationAttempted = steps != null && steps.stream()
                .anyMatch(step -> "navigate_to_page".equals(step.tool()));
        String safeAnswer = answer;
        if (navigationAttempted) {
            safeAnswer = workflowUrl == null
                    ? "Mình chưa thể mở trang bạn yêu cầu. Hãy xem trạng thái bước bên dưới."
                    : "Đang mở trang bạn yêu cầu.";
        } else if (claimsNavigationCompleted(answer)) {
            safeAnswer = "Mình chưa xác nhận được việc mở trang. Hãy yêu cầu mở một trang cụ thể để hệ thống kiểm tra và điều hướng.";
        }
        return new AiChatResponse(
                request.contextType(),
                request.recipeId(),
                safeAnswer,
                List.of(),
                workflowUrl,
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

    private static String navigationWorkflowUrl(List<AssistantStep> steps) {
        if (steps == null) return null;
        return steps.stream()
                .filter(step -> "navigate_to_page".equals(step.tool()) && "SUCCEEDED".equals(step.status()))
                .map(AssistantStep::result)
                .filter(java.util.Objects::nonNull)
                .map(result -> {
                    Object direct = result.get("workflowUrl");
                    if (direct instanceof String url) return url;
                    Object data = result.get("data");
                    if (data instanceof Map<?, ?> map && map.get("workflowUrl") instanceof String url) return url;
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .findFirst().orElse(null);
    }

    private static boolean claimsNavigationCompleted(String answer) {
        if (answer == null || answer.isBlank()) return false;
        String normalized = answer.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("đã mở trang") || normalized.contains("đã chuyển bạn")
                || normalized.contains("mình đã chuyển") || normalized.contains("đang mở trang")
                || normalized.contains("đã đưa bạn đến") || normalized.contains("đã điều hướng bạn")
                || normalized.contains("mở trang") && (normalized.contains("rồi nhé")
                        || normalized.contains("cho bạn") || normalized.contains("thành công"));
    }

    private AiChatResponse handleRunFailure(
            AiChatRequest request,
            UUID sessionId,
            ExecutionMode mode,
            List<AssistantStep> steps,
            List<Source> sources,
            Throwable failure) {
        if (isTimeout(failure)) {
            return buildResponse(request, sessionId, mode, "TIMEOUT", steps, sources, null, null,
                    "Lượt trả lời AI đã vượt thời gian chờ. Bạn có thể thử lại sau ít phút.");
        }
        return buildResponse(request, sessionId, mode, "FAILED", steps, sources, null, null,
                "Dịch vụ AI đang gặp sự cố khi xử lý lượt chạy. Vui lòng thử lại sau.");
    }

    private static boolean isTimeout(Throwable failure) {
        for (int depth = 0; failure != null && depth < 8; depth++, failure = failure.getCause()) {
            if (failure instanceof java.util.concurrent.TimeoutException) return true;
        }
        return false;
    }

    private static long elapsedMillis(long startedNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
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
                            .timeout(Math.toIntExact(AiTurnDeadline.HTTP_TRANSPORT_TIMEOUT.toMillis()))
                            .retryOptions(HttpRetryOptions.builder().attempts(1).build())
                            .build())
                    .build();
            var model = new Gemini(modelId, client);
            var budget = tech.mamxanh.integration.ai.adk.PilotLiveBudget.configured(
                    System.getenv("MAMXANH_AI_CHAT_G3_LIVE_BUDGET_FILE"));
            return budget == null ? model : budget.wrap(model);
        };
    }

    public record ServerPendingAction(
            UUID confirmationId,
            String toolName,
            String actionId,
            Map<String, Object> immutableArguments,
            String argumentsJson,
            Long userId,
            String owner,
            UUID sessionId,
            UUID clientRequestId,
            Instant expiresAt,
            ExecutionMode originalMode
    ) { }

    private record MenuPreviewSnapshot(UUID id, Map<String, Object> applyArguments) { }
    private record PendingFormAction(UUID id, UUID formId, long revision, AiChatRequest.FormActionAck acknowledgement) { }

    public AiChatRequest.FormActionAck acknowledgeFormAction(AiChatRequest.FormActionAck ack) {
        var user = users.requireActiveMember();
        if (!"EXPERT".equals(user.role().name())) throw new AppException(ErrorCode.ACCESS_DENIED);
        String owner = "user:" + user.id();
        if (!ownsSession(ack.sessionId(), owner)) throw new AppException(ErrorCode.VALIDATION_FAILED);
        EngineSessionState state = engineSessions.get(ack.sessionId());
        if (state == null || !state.owner.equals(owner)) throw new AppException(ErrorCode.VALIDATION_FAILED);
        synchronized (state) {
            PendingFormAction pending = state.pendingFormAction;
            if (pending == null || !pending.id().equals(ack.actionId()) || !pending.formId().equals(ack.formId())
                    || pending.revision() != ack.baseRevision()
                    || (ack.status() == AiChatRequest.FormActionStatus.APPLIED && (ack.revision() == null || ack.revision() < pending.revision())))
                throw new AppException(ErrorCode.VALIDATION_FAILED, "Xác nhận biểu mẫu không hợp lệ hoặc đã hết hạn.");
            if (pending.acknowledgement() != null && !pending.acknowledgement().equals(ack))
                throw new AppException(ErrorCode.VALIDATION_FAILED, "Thao tác đã được xác nhận với kết quả khác.");
            state.pendingFormAction = new PendingFormAction(pending.id(), pending.formId(), pending.revision(), ack);
            return ack;
        }
    }

    private MenuPreviewSnapshot menuPreviewSnapshot(Map<String, Object> args, Object result) {
        if (!(args.get("request") instanceof Map<?, ?> request) || !(result instanceof Map<?, ?> preview)
                || !(preview.get("meals") instanceof List<?> meals) || meals.isEmpty()) return null;
        List<Map<String, Object>> selected = new ArrayList<>();
        for (Object value : meals) {
            if (!(value instanceof Map<?, ?> meal) || meal.get("mealDate") == null
                    || meal.get("mealType") == null || meal.get("recipeId") == null) return null;
            selected.add(Map.of("mealDate", meal.get("mealDate"), "mealType", meal.get("mealType"), "recipeId", meal.get("recipeId")));
        }
        if (request.get("weekStartDate") == null || request.get("mode") == null) return null;
        Map<String, Object> apply = new LinkedHashMap<>();
        apply.put("weekStartDate", request.get("weekStartDate"));
        apply.put("mode", request.get("mode"));
        if (request.get("goal") != null) apply.put("goal", request.get("goal"));
        if (request.get("avoidedIngredientIds") instanceof List<?> avoided) apply.put("avoidedIngredientIds", List.copyOf(avoided));
        apply.put("meals", List.copyOf(selected));
        UUID id = UUID.randomUUID();
        return new MenuPreviewSnapshot(id, Map.of("previewId", id.toString(), "request", Map.copyOf(apply)));
    }

    private final class EngineSessionState implements AutoCloseable {
        private final UUID sessionId;
        private final Long userId;
        private final String owner;
        private final Authentication auth;
        private final AtomicBoolean busy = new AtomicBoolean(false);
        private final AtomicBoolean turnCancelled = new AtomicBoolean(false);
        private volatile Disposable activeDisposable;
        private volatile CountDownLatch activeLatch;
        private volatile Instant expiresAt;
        private volatile boolean consentRevoked;
        private volatile PendingConfirmation pendingConfirmation;
        private volatile ServerPendingAction serverPendingAction;
        private volatile MenuPreviewSnapshot menuPreview;
        private volatile PendingFormAction pendingFormAction;
        private volatile AiChatRequest.FormContext pendingPublishForm;
        private InMemoryRunner runner;
        private String adkSessionId;
        private String boundModelId;
        private ExecutionMode boundMode;
        private Set<String> boundToolNames = Set.of();
        private record TurnContext(
                Authentication callerAuth,
                ExecutionMode mode,
                Set<ConsentScope> activeConsents,
                List<AssistantStep> steps,
                List<Source> sources,
                AtomicReference<PendingConfirmation> stagedConfirmation,
                UUID clientRequestId,
                boolean approvalResume,
                String approvedConfirmationCallId,
                Consumer<AssistantStep> onStepConsumer,
                AiChatCancellationSignal cancellationSignal,
                AtomicReference<Set<ConsentScope>> requiredConsentScopes,
                AiChatRequest.FormContext formContext,
                AiTurnDeadline turnDeadline
        ) {
            void addStep(AssistantStep step) {
                steps.add(step);
                if (onStepConsumer != null) {
                    onStepConsumer.accept(step);
                }
            }
        }
        private final AtomicReference<TurnContext> currentTurn = new AtomicReference<>();

        public void cancelActiveTurn() {
            TurnContext turn = currentTurn.get();
            if (turn != null) turn.cancellationSignal().cancel();
            turnCancelled.set(true);
            Disposable disposable = activeDisposable;
            if (disposable != null && !disposable.isDisposed()) disposable.dispose();
            CountDownLatch latch = activeLatch;
            if (latch != null) latch.countDown();

        }

        private EngineSessionState(UUID sessionId, Long userId, String owner, Authentication auth, Instant expiresAt) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.owner = owner;
            this.auth = auth;
            this.expiresAt = expiresAt;
        }

        private synchronized void ensureRunner(String modelId, String role, ExecutionMode mode, AiChatRequest request) {
            ExecutionMode canonicalMode = mode == null ? ExecutionMode.CHATBOT : mode.canonical();
            List<AiAssistantToolRegistry.ToolDefinition> definitions = scopedDefinitions(role, mode, request);
            Set<String> toolNames = definitions.stream().map(AiAssistantToolRegistry.ToolDefinition::name)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
            if (runner != null && Objects.equals(boundModelId, modelId) && Objects.equals(boundMode, canonicalMode)
                    && Objects.equals(boundToolNames, toolNames)) {
                return;
            }

            closeRunnerOnly();

            boolean isChatbot = mode == ExecutionMode.CHATBOT;

            List<BaseTool> adkTools = definitions.stream()
                    .map(def -> (BaseTool) new ScopedAssistantTool(def, this))
                    .toList();

            MemberAccountingPolicy policy = new MemberAccountingPolicy(users, aiQuota, events);
            AdkModelGateway gateway = new AdkModelGateway(llmFactory.create(modelId), policy, clock)
                    .withRemainingTurnBudget(() -> {
                        TurnContext turn = currentTurn.get();
                        return turn == null ? RUN_TIMEOUT : turn.turnDeadline().remaining();
                    })
                    .withRunAllowed(() -> {
                        TurnContext turn = currentTurn.get();
                        return turn != null && !turn.cancellationSignal().isCancelled()
                                && !turn.turnDeadline().remaining().isZero();
                    });

            String instructions = isChatbot
                    ? promptLoader.loadSystemPrompt()
                    : promptLoader.loadSystemPrompt() + "\n\n" + promptLoader.loadAgentInstructions();

            String agentName = isChatbot ? "mamxanh_chatbot" : "mamxanh_agent";
            String agentDescription = isChatbot
                    ? "Mâm Xanh Vegetarian Cooking Chatbot"
                    : "Mâm Xanh Unified AI Agent";

            LlmAgent agent = LlmAgent.builder()
                    .name(agentName)
                    .description(agentDescription)
                    .instruction(instructions)
                    .model(gateway)
                    .generateContentConfig(GenerateContentConfig.builder()
                            .maxOutputTokens(2048)
                            .candidateCount(1)
                            .thinkingConfig(ModelThinkingPolicy.resolveConfig(modelId, canonicalMode).orElseGet(ModelThinkingPolicy::fallbackConfig))
                            .build())
                    .beforeModelCallback((context, req) -> {
                        if (req.build().contents().toString().getBytes(StandardCharsets.UTF_8).length > MAX_CONTEXT_BYTES) {
                            return Maybe.error(new IllegalStateException("Model context exceeds byte budget (64 KB)"));
                        }
                        return Maybe.empty();
                    })
                    .tools(adkTools)
                    .build();

            this.runner = new InMemoryRunner(agent, agentName + "_runner");
            this.adkSessionId = runner.sessionService().createSession(runner.appName(), owner).blockingGet().id();
            this.boundModelId = modelId;
            this.boundMode = canonicalMode;
            this.boundToolNames = toolNames;
        }

        private List<AiAssistantToolRegistry.ToolDefinition> scopedDefinitions(
                String role, ExecutionMode mode, AiChatRequest request) {
            if (mode == ExecutionMode.CHATBOT) return List.of();
            List<AiAssistantToolRegistry.ToolDefinition> definitions = toolRegistry.definitionsFor(role);
            if (request.formContext() != null) {
                String question = request.question() == null ? "" : request.question().toLowerCase(java.util.Locale.ROOT);
                boolean publishTurn = question.contains("đăng") || question.contains("xuat ban")
                        || question.contains("xuất bản") || question.contains("publish") || question.contains("post");
                Set<String> formTools = publishTurn ? Set.of("create_recipe_post") : Set.of("propose_recipe_form_patch");
                return definitions.stream().filter(definition -> formTools.contains(definition.name())).toList();
            }
            String question = request.question() == null ? "" : request.question().toLowerCase(java.util.Locale.ROOT);
            boolean menuTurn = menuPreview != null || question.contains("thực đơn") || question.contains("thuc don")
                    || question.contains("menu") || question.contains("lịch ăn") || question.contains("lich an")
                    || question.contains("bữa ăn") || question.contains("bua an") || question.contains("meal plan");
            if (!menuTurn) return definitions;
            Set<String> menuTools = Set.of("search_approved_knowledge", "get_health_profile", "get_dietary_preferences",
                    "get_meal_plan", "get_ai_menu_candidates", "create_ai_menu_preview",
                    "apply_ai_menu_preview", "apply_weekly_menu");
            return definitions.stream().filter(definition -> menuTools.contains(definition.name())).toList();
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
        private final EngineSessionState sessionState;

        private ScopedAssistantTool(
                AiAssistantToolRegistry.ToolDefinition definition,
                EngineSessionState sessionState) {
            super(definition.name(), definition.description());
            this.definition = definition;
            this.sessionState = sessionState;
        }

        @Override
        public Optional<FunctionDeclaration> declaration() {
            if ("get_ai_menu_candidates".equals(definition.name())
                    || "create_ai_menu_preview".equals(definition.name())) {
                String properties = "\"request\":{\"type\":\"OBJECT\",\"properties\":{"
                        + "\"weekStartDate\":{\"type\":\"STRING\",\"description\":\"Monday date in YYYY-MM-DD format from the user's request\"},"
                        + "\"mode\":{\"type\":\"STRING\",\"enum\":[\"PROFILE\",\"THIS_WEEK\"]},"
                        + "\"goal\":{\"type\":\"STRING\"},"
                        + "\"preferredIngredientIds\":{\"type\":\"ARRAY\",\"items\":{\"type\":\"INTEGER\"}},"
                        + "\"avoidedIngredientIds\":{\"type\":\"ARRAY\",\"items\":{\"type\":\"INTEGER\"}}},"
                        + "\"required\":[\"weekStartDate\",\"mode\"]}";
                String schema = "get_ai_menu_candidates".equals(definition.name())
                        ? "{\"type\":\"OBJECT\",\"properties\":{" + properties + "},\"required\":[\"request\"]}"
                        : "{\"type\":\"OBJECT\",\"properties\":{" + properties
                                + ",\"rankedRecipeIds\":{\"type\":\"ARRAY\",\"items\":{\"type\":\"INTEGER\"}}},"
                                + "\"required\":[\"request\",\"rankedRecipeIds\"]}";
                return Optional.of(FunctionDeclaration.builder().name(definition.name())
                        .description(definition.description()).parameters(Schema.fromJson(schema)).build());
            }
            if ("propose_recipe_form_patch".equals(definition.name())) {
                return Optional.of(FunctionDeclaration.builder().name(definition.name()).description(definition.description())
                        .parameters(Schema.fromJson("""
                                {"type":"OBJECT","properties":{
                                  "fields":{"type":"OBJECT","properties":{
                                    "title":{"type":"STRING"},"description":{"type":"STRING"},
                                    "instructions":{"type":"STRING"},"dishCategory":{"type":"STRING"},
                                    "vegetarianType":{"type":"STRING"},"difficulty":{"type":"STRING"},
                                    "servings":{"type":"STRING"},"prepTimeMinutes":{"type":"STRING"},
                                    "cookTimeMinutes":{"type":"STRING"},"youtubeUrl":{"type":"STRING"}}},
                                  "appendText":{"type":"OBJECT","properties":{
                                    "description":{"type":"STRING"},"instructions":{"type":"STRING"}}},
                                  "addIngredients":{"type":"ARRAY","items":{"type":"OBJECT","properties":{
                                    "name":{"type":"STRING"},"quantity":{"type":"STRING"},
                                    "unitCode":{"type":"STRING"}},"required":["name","quantity","unitCode"]}}}}
                                """)).build());
            }
            if ("create_recipe_post".equals(definition.name())) {
                return Optional.of(FunctionDeclaration.builder().name(definition.name()).description(definition.description())
                        .parameters(Schema.fromJson("{\"type\":\"OBJECT\"}")).build());
            }
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
            EngineSessionState.TurnContext turn = sessionState.currentTurn.get();
            if (turn == null) {
                return Single.just(Map.of("status", "ERROR", "message", "Mất ngữ cảnh phiên làm việc."));
            }

            long toolStarted = System.nanoTime();
            return Single.<Map<String, Object>>fromCallable(() -> asCaller(turn.callerAuth(), () -> {
                if (!turn.cancellationSignal().tryBeginToolStep()) {
                    return Map.of("status", "STOPPED", "message", "Lượt chạy đã bị dừng trước bước công cụ này.");
                }
                // Check Consent
                if (!definition.consentCategories().isEmpty()) {
                    for (ConsentCategory cat : definition.consentCategories()) {
                        ConsentScope reqScope = new ConsentScope(cat, consentPurposeFor(cat, definition.write()));
                        if (!turn.activeConsents().contains(reqScope)) {
                            turn.requiredConsentScopes().updateAndGet(existing -> {
                                Set<ConsentScope> merged = new LinkedHashSet<>(existing);
                                merged.add(reqScope);
                                return Set.copyOf(merged);
                            });
                            return Map.of("status", "CONSENT_REQUIRED", "message", "Thao tác cần có sự đồng ý cho phạm vi: " + cat.name());
                        }
                    }
                }

                // Check if turn is resuming after approval: block chained writes/confirmations to prevent infinite loops
                if (turn.approvalResume() && (definition.write() || definition.isHighRisk())) {
                    return Map.of("status", "SKIPPED", "message", "Không thể gọi thêm công cụ ghi trong lượt phản hồi xác nhận.");
                }

                MenuPreviewSnapshot currentPreview = sessionState.menuPreview;
                if ("apply_ai_menu_preview".equals(definition.name())
                        && (currentPreview == null || args == null || !currentPreview.id().toString().equals(args.get("previewId")))) {
                    return Map.of("status", "BLOCKED", "message", "Không tìm thấy bản xem trước hợp lệ trong phiên này.");
                }
                AiChatRequest.FormContext publishForm = turn.formContext();
                if ("create_recipe_post".equals(definition.name()) && (publishForm == null || publishForm.mediaUploading()
                        || publishForm.publishPayload() == null)) {
                    return Map.of("status", "BLOCKED", "message", "Cần biểu mẫu hiện tại và ảnh đã tải lên trước khi đăng.");
                }
                final Map<String, Object> effectiveArgs = "apply_ai_menu_preview".equals(definition.name())
                        ? currentPreview.applyArguments() : "create_recipe_post".equals(definition.name())
                        ? publishForm.publishPayload() : (args == null ? Map.of() : args);

                // Check QA mode for write actions
                if (turn.mode().isChatbot() && definition.write()) {
                    int stepIndex = turn.steps().size() + 1;
                    String msg = "Chế độ QA không ghi dữ liệu. Hãy chọn CONFIRM hoặc AUTO cho yêu cầu này.";
                    Map<String, Object> preview = Map.of("preview", effectiveArgs);
                    turn.addStep(new AssistantStep(stepIndex, definition.name(), "SKIPPED_MODE", msg, preview));
                    return Map.of(
                            "status", "SKIPPED_MODE",
                            "message", msg,
                            "preview", preview);
                }

                // Check confirmation requirement for high-risk write operations:
                // 1. All HIGH_RISK_WRITE tools require confirmation in ALL modes (including AUTO and CONFIRM).
                // 2. In CONFIRM mode, all write actions require confirmation.
                boolean requiresConfirmation = definition.isHighRisk()
                        || ((turn.mode() == ExecutionMode.CONFIRM || turn.mode() == ExecutionMode.AGENT)
                                && definition.write());

                if (requiresConfirmation) {
                    if ("create_recipe_post".equals(definition.name())) {
                        toolRegistry.validateRecipePublish(effectiveArgs);
                        sessionState.pendingPublishForm = publishForm;
                    }
                    int stepIndex = turn.steps().size() + 1;
                    String actionId = definition.name() + "_" + stepIndex;
                    String summary = definition.isHighRisk()
                            ? "Thao tác rủi ro cao cần xác nhận trước khi thực hiện."
                            : "Cần xác nhận trước khi thay đổi dữ liệu.";
                    Map<String, Object> immutableArgs = Collections.unmodifiableMap(new LinkedHashMap<>(effectiveArgs));
                    String argsJson = "";
                    try {
                        argsJson = mapper.writeValueAsString(immutableArgs);
                    } catch (Exception ignored) {
                    }
                    long ownerId = sessionState.userId != null ? sessionState.userId : 0L;
                    AiAssistantSessionStore.PendingPlan storePlan = sessionStore.createPending(
                            sessionState.sessionId,
                            sessionState.owner,
                            List.of(new AiAssistantSessionStore.PendingAction(definition.name(), immutableArgs, true)),
                            true);
                    UUID planId = storePlan.id();
                    // The confirmation ID is known to the client even if the confirmation response is lost.
                    UUID effectiveRequestId = planId;

                    ServerPendingAction serverAction = new ServerPendingAction(
                            planId,
                            definition.name(),
                            actionId,
                            immutableArgs,
                            argsJson,
                            ownerId,
                            sessionState.owner,
                            sessionState.sessionId,
                            effectiveRequestId,
                            storePlan.expiresAt(),
                            turn.mode());
                    sessionState.serverPendingAction = serverAction;
                    Object proposedNameValue = args == null ? null : args.get("name");
                    String proposedListName = switch (definition.name()) {
                        case "create_shopping_list", "rename_shopping_list" -> proposedNameValue instanceof String name ? name.strip() : "";
                        default -> null;
                    };
                    PendingConfirmation pending = new PendingConfirmation(
                            planId,
                            definition.description(),
                            storePlan.expiresAt(),
                            true,
                            proposedListName == null || proposedListName.isBlank() ? null : proposedListName);
                    turn.stagedConfirmation().set(pending);
                    Map<String, Object> preview = Map.of("preview", effectiveArgs);
                    turn.addStep(new AssistantStep(stepIndex, definition.name(), "AWAITING_CONFIRMATION", summary, preview));
                    return Map.of(
                            "status", "AWAITING_CONFIRMATION",
                            "confirmationId", planId.toString(),
                            "message", "Thao tác cần người dùng xác nhận trước khi thực hiện.");
                }

                // Execute tool via registry (wrapped in journal for write operations)
                int stepIndex = turn.steps().size() + 1;
                String actionId = definition.name() + "_" + stepIndex;
                long ownerId = sessionState.userId != null ? sessionState.userId : 0L;
                UUID effectiveRequestId = turn.clientRequestId() != null ? turn.clientRequestId() : sessionState.sessionId;

                AiAssistantToolRegistry.ToolResult result;
                if ("propose_recipe_form_patch".equals(definition.name())) {
                    if (turn.steps().stream().anyMatch(step -> "UI_ACTION_REQUIRED".equals(step.status())))
                        throw new AppException(ErrorCode.VALIDATION_FAILED, "Chỉ xử lý một patch mỗi lượt.");
                    result = proposeRecipeFormPatch(turn.formContext(), effectiveArgs);
                    @SuppressWarnings("unchecked") Map<String, Object> action = (Map<String, Object>) result.result();
                    synchronized (sessionState) {
                        if (sessionState.pendingFormAction != null && sessionState.pendingFormAction.acknowledgement() == null)
                            throw new AppException(ErrorCode.VALIDATION_FAILED, "Patch trước chưa được giao diện xác nhận.");
                        sessionState.pendingFormAction = new PendingFormAction(UUID.fromString((String) action.get("actionId")),
                                turn.formContext().formId(), turn.formContext().revision(), null);
                    }
                } else if (definition.write() && journal != null) {
                    String argsJson = "";
                    try {
                        argsJson = mapper.writeValueAsString(effectiveArgs);
                    } catch (Exception ignored) {
                    }
                    String argumentsHash = hash(actionId + ":" + definition.name() + ":" + argsJson);
                    result = journal.executeWithJournal(
                            ownerId,
                            effectiveRequestId,
                            actionId,
                            definition.name(),
                            argumentsHash,
                            () -> toolRegistry.execute(definition.name(), effectiveArgs));
                } else {
                    result = toolRegistry.execute(definition.name(), effectiveArgs);
                }

                if ("create_ai_menu_preview".equals(definition.name()) && "SUCCEEDED".equals(result.status())) {
                    MenuPreviewSnapshot snapshot = menuPreviewSnapshot(effectiveArgs, result.result());
                    sessionState.menuPreview = snapshot;
                    if (snapshot != null && result.result() instanceof Map<?, ?> data) {
                        Map<String, Object> withId = new LinkedHashMap<>();
                        data.forEach((key, value) -> { if (key instanceof String text) withId.put(text, value); });
                        withId.put("previewId", snapshot.id().toString());
                        result = new AiAssistantToolRegistry.ToolResult(result.status(), result.summary(), withId, result.sources());
                    }
                }

                if (result.sources() != null) {
                    turn.sources().addAll(result.sources());
                }

                Map<String, Object> resultMap = new LinkedHashMap<>();
                resultMap.put("status", result.status());
                resultMap.put("message", result.summary());
                if (result.result() != null) {
                    resultMap.put("data", result.result());
                }

                turn.addStep(new AssistantStep(stepIndex, definition.name(), result.status(), result.summary(), resultMap));
                return resultMap;
            })).doOnSuccess(result -> LOG.info("ADK tool completed sessionId={} tool={} elapsedMs={} status={}",
                    sessionState.sessionId, definition.name(), elapsedMillis(toolStarted), result.get("status")))
                    .doOnError(failure -> LOG.warn("ADK tool failed sessionId={} tool={} elapsedMs={} cause={}",
                            sessionState.sessionId, definition.name(), elapsedMillis(toolStarted),
                            failure.getClass().getSimpleName()));
        }

        private <T> T asCaller(Authentication callerAuth, Callable<T> action) throws Exception {
            var prev = SecurityContextHolder.getContext();
            var ctx = SecurityContextHolder.createEmptyContext();
            ctx.setAuthentication(callerAuth);
            SecurityContextHolder.setContext(ctx);
            try {
                return action.call();
            } finally {
                SecurityContextHolder.setContext(prev);
            }
        }
    }
    private static String hash(String input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}

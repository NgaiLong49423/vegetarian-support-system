package tech.mamxanh.recipe.service;

import com.google.adk.events.Event;
import com.google.adk.models.BaseLlm;
import com.google.adk.models.Gemini;
import com.google.adk.tools.FunctionTool;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.FunctionResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import com.google.genai.types.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.ai.AiTokenUsage;
import tech.mamxanh.integration.ai.adk.AdkModelGateway;
import tech.mamxanh.integration.ai.adk.AdkSpikeSession;
import tech.mamxanh.integration.ai.knowledge.GeminiEmbeddingClient;
import tech.mamxanh.integration.ai.knowledge.HybridKnowledgeRetriever;
import tech.mamxanh.integration.ai.knowledge.KnowledgeCatalog;
import tech.mamxanh.integration.ai.knowledge.KnowledgeIndex;
import tech.mamxanh.recipe.dto.request.AiChatRequest;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ConfirmationDecision;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ConsentAction;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ConsentCategory;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ConsentPurpose;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ConsentScope;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ExecutionMode;
import tech.mamxanh.recipe.dto.response.AiChatResponse;
import tech.mamxanh.recipe.dto.response.AiChatResponse.AssistantStep;
import tech.mamxanh.recipe.dto.response.AiChatResponse.ConsentRequest;
import tech.mamxanh.recipe.dto.response.AiChatResponse.PendingConfirmation;
import tech.mamxanh.recipe.dto.response.AiChatResponse.Source;
import tech.mamxanh.shopping.dto.response.ShoppingListResponse;
import tech.mamxanh.shopping.service.ShoppingListService;
import tech.mamxanh.subscription.service.AiChatSettingsService;
import tech.mamxanh.subscription.service.AiTokenQuotaService;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * G3 Shopping List vertical slice orchestrated through the native Google ADK tool loop.
 * Enforces session engine pinning, Shopping List consent, Execution Mode (QA/CONFIRM/AUTO),
 * single-transaction D3 accounting, clientRequestId idempotency, and partial-failure preservation.
 */
@Service
public class AdkShoppingPilotService implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AdkShoppingPilotService.class);
    public static final String ENGINE = AiAssistantSessionStore.ENGINE_G3_PILOT;
    public static final List<ConsentScope> SHOPPING_CONSENT_SCOPES = List.of(
            new ConsentScope(ConsentCategory.SHOPPING_LIST, ConsentPurpose.AI_CHAT),
            new ConsentScope(ConsentCategory.SHOPPING_LIST, ConsentPurpose.SHOPPING_LIST_READ),
            new ConsentScope(ConsentCategory.SHOPPING_LIST, ConsentPurpose.SHOPPING_LIST_WRITE));

    private static final Pattern SHOPPING_DATA_MENTION = Pattern.compile(
            "(?iu)(?:danh\\s*sách|mua\\s*sắm|đi\\s*chợ|shopping\\s*list|tạo\\s*danh\\s*sách|đọc\\s*danh\\s*sách|list\\s*#?\\d+|id\\s*#?\\d+)");

    @FunctionalInterface
    public interface LlmFactory {
        BaseLlm create(String modelId);
    }

    private final CurrentUserService users;
    private final ShoppingListService shoppingLists;
    private final AiTokenQuotaService aiQuota;
    private final AiChatSettingsService chatSettings;
    private final AiAssistantSessionStore sessionStore;
    private final ObjectMapper mapper;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final KnowledgeCatalog g3Catalog;
    private final KnowledgeIndex g3Index;
    private final LlmFactory llmFactory;
    private final HybridKnowledgeRetriever.Embedder embedder;
    private volatile boolean pilotEnabled;

    private final Map<UUID, PilotSessionState> pilotSessions = new ConcurrentHashMap<>();

    @Autowired
    public AdkShoppingPilotService(CurrentUserService users,
            ShoppingListService shoppingLists,
            AiTokenQuotaService aiQuota,
            AiChatSettingsService chatSettings,
            AiAssistantSessionStore sessionStore,
            ObjectMapper mapper,
            ApplicationEventPublisher events,
            Clock clock,
            @Value("${mamxanh.ai-chat.g3-pilot-enabled:false}") boolean pilotEnabled,
            @Value("${GEMINI_API_KEY:}") String configuredApiKey,
            @Value("${mamxanh.ai-chat.g3-live-budget-file:}") String budgetFile) {
        this(users,
                shoppingLists,
                aiQuota,
                chatSettings,
                sessionStore,
                mapper,
                events,
                clock,
                KnowledgeCatalog.packagedG3(),
                loadOptionalG3Index(mapper, KnowledgeCatalog.packagedG3()),
                defaultLlmFactory(configuredApiKey, tech.mamxanh.integration.ai.adk.PilotLiveBudget.configured(budgetFile)),
                defaultEmbedder(configuredApiKey, loadOptionalG3Index(mapper, KnowledgeCatalog.packagedG3()), tech.mamxanh.integration.ai.adk.PilotLiveBudget.configured(budgetFile)),
                pilotEnabled);
    }

    public AdkShoppingPilotService(CurrentUserService users,
            ShoppingListService shoppingLists,
            AiTokenQuotaService aiQuota,
            AiChatSettingsService chatSettings,
            AiAssistantSessionStore sessionStore,
            ObjectMapper mapper,
            ApplicationEventPublisher events,
            Clock clock,
            KnowledgeCatalog g3Catalog,
            KnowledgeIndex g3Index,
            LlmFactory llmFactory,
            HybridKnowledgeRetriever.Embedder embedder,
            boolean pilotEnabled) {
        this.users = Objects.requireNonNull(users, "users");
        this.shoppingLists = Objects.requireNonNull(shoppingLists, "shoppingLists");
        this.aiQuota = Objects.requireNonNull(aiQuota, "aiQuota");
        this.chatSettings = chatSettings;
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.events = Objects.requireNonNull(events, "events");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.g3Catalog = Objects.requireNonNull(g3Catalog, "g3Catalog");
        this.g3Index = g3Index;
        this.llmFactory = Objects.requireNonNull(llmFactory, "llmFactory");
        this.embedder = embedder;
        this.pilotEnabled = pilotEnabled;
    }

    public boolean isPilotEnabled() {
        return pilotEnabled;
    }

    /** Rollback hook: disables new pilot sessions, invalidates all pending G3 actions, and closes cached ADK contexts. */
    public synchronized void setPilotEnabled(boolean enabled) {
        this.pilotEnabled = enabled;
        if (!enabled) {
            for (var state : pilotSessions.values()) {
                state.invalidatePendingAndCloseRunner();
            }
            pilotSessions.clear();
        }
    }

    public boolean ownsSession(UUID sessionId, String owner) {
        if (sessionId == null) return false;
        return sessionStore.peekEngine(sessionId, owner)
                .map(ENGINE::equals)
                .orElse(false);
    }

    public AiChatResponse execute(AiChatRequest request) {
        if (!pilotEnabled) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Pilot G3 Danh sách mua sắm hiện đang tắt. Phiên pilot cũ không tự chuyển sang bộ điều phối khác; vui lòng bắt đầu phiên trò chuyện mới.");
        }
        if (request.contextType() != AiChatRequest.ContextType.GENERAL || request.recipeId() != null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Pilot G3 chỉ áp dụng cho ngữ cảnh GENERAL.");
        }
        if (request.clientRequestId() == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Yêu cầu pilot G3 bắt buộc có clientRequestId hợp lệ.");
        }

        var member = users.requireActiveMember();
        if (member.role() != Role.CUSTOMER && member.role() != Role.EXPERT) {
            throw new AppException(ErrorCode.RECIPE_EDIT_NOT_ALLOWED);
        }
        long userId = member.id();
        String owner = "user:" + userId;
        Authentication memberAuth = UsernamePasswordAuthenticationToken.authenticated(
                String.valueOf(userId),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + member.role().name())));

        evictExpiredPilotStates();
        var handle = sessionStore.open(request.sessionId(), owner, ENGINE);
        UUID sessionId = handle.id();
        PilotSessionState state = pilotSessions.computeIfAbsent(sessionId,
                id -> new PilotSessionState(id, userId, owner, memberAuth, handle.expiresAt()));
        state.expiresAt = handle.expiresAt();

        if (state.busy.get() && request.consentDecision() != null && request.consentDecision().action() == ConsentAction.REVOKE) {
            var scopes=request.consentDecision().scopes();
            sessionStore.applyConsent(sessionId, owner, false, scopes==null?Set.of():Set.copyOf(scopes));
            sessionStore.discardPending(sessionId,owner);
            state.consentRevoked=true;
            state.pendingConfirmation=null;
            return buildResponse(request,sessionId,request.executionMode(),"CONSENT_REVOKED",state.expiresAt,List.of(),List.of(),null,null,
                    "Đã thu hồi consent; những lần gửi tiếp theo và bước ghi chưa bắt đầu sẽ bị chặn.");
        }

        String payloadHash = payloadHash(request);
        RequestLedgerEntry existingRequest = state.requests.get(request.clientRequestId());
        if (existingRequest != null) {
            if (existingRequest.inFlight()) {
                throw new AppException(ErrorCode.VALIDATION_FAILED,
                        "Yêu cầu này đang được xử lý. Vui lòng không gửi trùng khi yêu cầu chưa hoàn tất.");
            }
            if (!existingRequest.payloadHash().equals(payloadHash)) {
                throw new AppException(ErrorCode.VALIDATION_FAILED,
                        "clientRequestId đã được gắn với một nội dung yêu cầu khác.");
            }
            return existingRequest.response();
        }

        if (!state.busy.compareAndSet(false, true)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Phiên trợ lý đang xử lý một lượt chạy khác. Vui lòng đợi lượt hiện tại hoàn tất.");
        }
        state.requests.put(request.clientRequestId(), new RequestLedgerEntry(payloadHash, true, null));

        try {
            boolean newQuestion=request.confirmationDecision()==null && (request.consentDecision()==null || request.consentDecision().action()!=ConsentAction.REVOKE)
                    && (state.trustedTurns.isEmpty() || !state.trustedTurns.getLast().content().equals(request.question()));
            if(state.requests.size()>100 || state.trustedTurns.size()>=8 && newQuestion)
                throw new AppException(ErrorCode.VALIDATION_FAILED,"Phiên pilot đã đạt giới hạn ngữ cảnh. Vui lòng bắt đầu cuộc trò chuyện mới.");
            // Client history is a display hint, never evidence for write authorization or consent.
            AiChatRequest trusted=new AiChatRequest(request.contextType(),request.recipeId(),request.question(),List.copyOf(state.trustedTurns),
                    request.anonymousViewerId(),request.sessionId(),request.executionMode(),request.consentDecision(),request.confirmationId(),request.confirmationDecision(),request.clientRequestId());
            AiChatResponse response = executeTurn(trusted, userId, owner, memberAuth, handle, state);
            if(!state.consentRevoked && newQuestion)
                state.trustedTurns.add(new AiChatRequest.Turn(AiChatRequest.Speaker.USER,request.question(),List.of()));
            state.requests.put(request.clientRequestId(), new RequestLedgerEntry(payloadHash, false, response));
            return response;
        } catch (RuntimeException failure) {
            state.requests.remove(request.clientRequestId());
            throw failure;
        } finally {
            state.busy.set(false);
            if(state.consentRevoked) {state.invalidatePendingAndCloseRunner(); state.trustedTurns.clear();}
        }
    }

    public void deleteSession(UUID sessionId, String anonymousViewerId) {
        Long userId = users.findAuthenticatedUserId().map(id -> users.requireActiveMember().id()).orElse(null);
        String owner = userId == null ? "guest:" + anonymousViewerId : "user:" + userId;
        sessionStore.peekEngine(sessionId, owner); // Authenticate ownership before touching native state.
        var active=pilotSessions.get(sessionId);
        if(active!=null && active.busy.get()) throw new AppException(ErrorCode.VALIDATION_FAILED,"Phiên đang chạy; hãy đợi kết quả trước khi xóa phiên.");
        PilotSessionState removed = pilotSessions.remove(sessionId);
        if (removed != null) {
            removed.invalidatePendingAndCloseRunner();
        }
        sessionStore.delete(sessionId, owner);
    }

    private AiChatResponse executeTurn(AiChatRequest request, long userId, String owner,
            Authentication memberAuth, AiAssistantSessionStore.SessionHandle handle, PilotSessionState state) {
        ExecutionMode mode = request.executionMode() == null ? ExecutionMode.CONFIRM : request.executionMode();
        Set<ConsentScope> activeConsents = new LinkedHashSet<>(handle.consentScopes());

        if (request.consentDecision() != null) {
            var decision = request.consentDecision();
            Set<ConsentScope> scopes = Set.copyOf(decision.scopes() == null ? List.of() : decision.scopes());
            handle = sessionStore.applyConsent(handle.id(), owner, decision.action() == ConsentAction.GRANT, scopes);
            activeConsents = new LinkedHashSet<>(handle.consentScopes());
            state.expiresAt = handle.expiresAt();
            if (decision.action() == ConsentAction.REVOKE) {
                state.consentRevoked = true;
                state.invalidatePendingAndCloseRunner();
                sessionStore.discardPending(handle.id(), owner);
                return buildResponse(
                        request,
                        handle.id(),
                        mode,
                        "CONSENT_REVOKED",
                        handle.expiresAt(),
                        List.of(),
                        List.of(),
                        new ConsentRequest(SHOPPING_CONSENT_SCOPES, "Google Gemini"),
                        null,
                        "Đã thu hồi sự đồng ý dữ liệu Danh sách mua sắm, hủy thao tác đang chờ và xóa ngữ cảnh nhạy cảm khỏi phiên. Để tiếp tục sử dụng, bạn cần cấp lại sự đồng ý.");
            }
            if (activeConsents.containsAll(SHOPPING_CONSENT_SCOPES)) {
                state.consentRevoked = false;
            }
        }

        if (state.pendingConfirmation != null && state.pendingConfirmation.mode() != mode) {
            state.invalidatePendingAndCloseRunner();
            sessionStore.discardPending(handle.id(), owner);
        }

        if (request.confirmationDecision() != null || request.confirmationId() != null) {
            return handleConfirmationDecision(request, userId, owner, memberAuth, handle, state, mode, activeConsents);
        }

        if (state.pendingConfirmation != null) {
            // Any new message (including changing the list name) invalidates the previous pending confirmation.
            state.invalidatePendingAndCloseRunner();
            sessionStore.discardPending(handle.id(), owner);
        }

        String combinedUserText = request.question() + " " + (request.history() == null ? "" : request.history().stream()
                .filter(turn -> turn.speaker() == AiChatRequest.Speaker.USER && turn.content() != null)
                .map(AiChatRequest.Turn::content)
                .reduce("", (a, b) -> a + " " + b));

        ConsentCategory otherPersonalCategory = AiAssistantExecutionService.personalCategory(combinedUserText);
        List<ConsentScope> missingScopes = new ArrayList<>();
        if (otherPersonalCategory != null) {
            ConsentScope personalScope = new ConsentScope(otherPersonalCategory, ConsentPurpose.AI_CHAT);
            if (!activeConsents.contains(personalScope)) {
                missingScopes.add(personalScope);
            }
        }
        { // Every pilot prompt can contain list data; obtain consent before the first provider call.
            for (ConsentScope scope : SHOPPING_CONSENT_SCOPES) {
                if (!activeConsents.contains(scope)) {
                    missingScopes.add(scope);
                }
            }
        }
        if (!missingScopes.isEmpty()) {
            return buildResponse(
                    request,
                    handle.id(),
                    mode,
                    "AWAITING_CONSENT",
                    handle.expiresAt(),
                    List.of(),
                    List.of(),
                    new ConsentRequest(List.copyOf(missingScopes), "Google Gemini"),
                    null,
                    "Để xử lý yêu cầu liên quan đến Danh sách mua sắm (bao gồm nội dung yêu cầu, tên, ID và kết quả danh sách để tra cứu hướng dẫn và điều phối qua Google Gemini), vui lòng xác nhận đồng ý phạm vi dữ liệu trước.");
        }

        return runAdkTurn(
                request,
                userId,
                owner,
                memberAuth,
                handle,
                state,
                mode,
                activeConsents,
                Content.fromParts(Part.fromText(request.question())),
                false,
                null);
    }

    private AiChatResponse handleConfirmationDecision(AiChatRequest request, long userId, String owner,
            Authentication memberAuth, AiAssistantSessionStore.SessionHandle handle, PilotSessionState state,
            ExecutionMode mode, Set<ConsentScope> activeConsents) {
        PilotPendingConfirmation pending = state.pendingConfirmation;
        Instant now = clock.instant();
        if (request.confirmationId() == null
                || request.confirmationDecision() == null
                || pending == null
                || !pending.confirmationId().equals(request.confirmationId())
                || !pending.owner().equals(owner)
                || !pending.sessionId().equals(handle.id())
                || !pending.expiresAt().isAfter(now)
                || pending.mode() != mode) {
            state.invalidatePendingAndCloseRunner();
            sessionStore.discardPending(handle.id(), owner);
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Yêu cầu xác nhận không hợp lệ, đã hết hạn hoặc đã thay đổi tên/chế độ.");
        }

        state.pendingConfirmation = null;
        sessionStore.takePending(handle.id(), owner, request.confirmationId());

        if (request.confirmationDecision() == ConfirmationDecision.REJECT) {
            List<Source> cancellationSources=state.tools==null?List.of():state.tools.sources();
            state.invalidatePendingAndCloseRunner();
            return buildResponse(
                    request,
                    handle.id(),
                    mode,
                    "CANCELLED",
                    handle.expiresAt(),
                    List.of(new AssistantStep(
                            1,
                            "create_shopping_list",
                            "CANCELLED",
                            "Đã hủy thao tác tạo danh sách mua sắm \"" + pending.proposedListName() + "\".",
                            Map.of("proposedListName", pending.proposedListName()))),
                    cancellationSources,
                    null,
                    null,
                    "Đã hủy thao tác tạo danh sách mua sắm \"" + pending.proposedListName() + "\" theo yêu cầu của bạn.");
        }

        if (!activeConsents.containsAll(SHOPPING_CONSENT_SCOPES)) {
            state.invalidatePendingAndCloseRunner();
            return buildResponse(
                    request,
                    handle.id(),
                    mode,
                    "AWAITING_CONSENT",
                    handle.expiresAt(),
                    List.of(),
                    List.of(),
                    new ConsentRequest(SHOPPING_CONSENT_SCOPES, "Google Gemini"),
                    null,
                    "Sự đồng ý dữ liệu Danh sách mua sắm không còn hiệu lực. Vui lòng đồng ý lại trước khi thực hiện thao tác.");
        }

        if (!aiQuota.hasAvailableBudget(userId, null)) {
            state.invalidatePendingAndCloseRunner();
            return buildResponse(
                    request,
                    handle.id(),
                    mode,
                    "BLOCKED_BUDGET",
                    handle.expiresAt(),
                    List.of(new AssistantStep(
                            1,
                            "create_shopping_list",
                            "BLOCKED_BUDGET",
                            "Đã hết hạn mức miễn phí và AI Credit trước bước ghi; yêu cầu xác nhận cũ đã hết hiệu lực.",
                            Map.of("proposedListName", pending.proposedListName()))),
                    List.of(),
                    null,
                    null,
                    "Bạn đã hết cả hạn mức miễn phí và AI Credit trước bước tạo danh sách nên hệ thống đã dừng trước khi ghi. Sau khi bổ sung ngân sách, vui lòng gửi và duyệt lại yêu cầu.");
        }

        Content approvalMessage = Content.fromParts(Part.builder()
                .functionResponse(FunctionResponse.builder()
                        .id(pending.adkConfirmationCallId())
                        .name("adk_request_confirmation")
                        .response(Map.of("confirmed", true))
                        .build())
                .build());

        return runAdkTurn(
                request,
                userId,
                owner,
                memberAuth,
                handle,
                state,
                mode,
                activeConsents,
                approvalMessage,
                true,
                pending.proposedListName());
    }

    private AiChatResponse runAdkTurn(AiChatRequest request, long userId, String owner,
            Authentication memberAuth, AiAssistantSessionStore.SessionHandle handle, PilotSessionState state,
            ExecutionMode mode, Set<ConsentScope> activeConsents, Content inputContent,
            boolean approvalResume, String approvedNormalizedName) {
        String resolvedModel = resolveModel(userId);
        state.ensureRunner(resolvedModel, activeConsents);
        AdkShoppingPilotTools tools = state.tools;
        tools.beginTurn(userId, mode, request.question(), request.history(), approvalResume, approvedNormalizedName);

        List<Event> emittedEvents;
        try {
            emittedEvents = state.adkSession.run(memberAuth, inputContent).toList().blockingGet();
        } catch (RuntimeException failure) {
            return handleRunFailure(request, handle, state, mode, tools, failure);
        }

        AdkShoppingPilotTools.StagedConfirmation staged = tools.stagedConfirmation();
        if (staged != null) {
            String adkConfCallId = emittedEvents.stream()
                    .flatMap(event -> event.functionCalls().stream())
                    .filter(call -> "adk_request_confirmation".equals(call.name().orElse("")))
                    .map(call -> call.id().orElse(null))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(staged.functionCallId());
            var plan = sessionStore.createPending(
                    handle.id(),
                    owner,
                    List.of(new AiAssistantSessionStore.PendingAction(
                            "create_shopping_list",
                            Map.of("name", staged.proposedListName()),
                            true)),
                    true);
            state.pendingConfirmation = new PilotPendingConfirmation(
                    plan.id(),
                    owner,
                    handle.id(),
                    adkConfCallId,
                    staged.proposedListName(),
                    mode,
                    plan.expiresAt());
            PendingConfirmation confirmationDto = new PendingConfirmation(
                    plan.id(),
                    "Xác nhận tạo danh sách mua sắm rỗng \"" + staged.proposedListName() + "\"",
                    plan.expiresAt(),
                    true,
                    staged.proposedListName());
            return buildResponse(
                    request,
                    handle.id(),
                    mode,
                    "AWAITING_CONFIRMATION",
                    handle.expiresAt(),
                    tools.steps(),
                    tools.sources(),
                    null,
                    confirmationDto,
                    "Mình đã chuẩn bị tạo danh sách mua sắm rỗng \"" + staged.proposedListName()
                            + "\". Vui lòng kiểm tra tên danh sách và nhấn Xác nhận thay đổi để hệ thống tạo và đọc lại danh sách.");
        }

        ShoppingListResponse created = tools.committedCreate();
        ShoppingListResponse readBack = tools.verifiedRead();
        if (created != null && readBack == null) {
            List<AssistantStep> steps = new ArrayList<>(tools.steps());
            if (steps.stream().noneMatch(step -> "get_shopping_list".equals(step.tool()) && "FAILED".equals(step.status()))) {
                steps.add(new AssistantStep(
                        steps.size() + 1,
                        "get_shopping_list",
                        "FAILED",
                        "Danh sách #" + created.id() + " đã được tạo nhưng bước đọc lại chưa hoàn tất.",
                        Map.of("id", created.id(), "name", created.name())));
            }
            state.invalidatePendingAndCloseRunner();
            return buildResponse(
                    request,
                    handle.id(),
                    mode,
                    "PARTIAL_FAILURE",
                    handle.expiresAt(),
                    steps,
                    tools.sources(),
                    null,
                    null,
                    partialFailureMessage(created));
        }

        String modelAnswer = emittedEvents.stream()
                .filter(Event::finalResponse)
                .map(Event::stringifyContent)
                .filter(Objects::nonNull)
                .reduce("", (a, b) -> a + b)
                .strip();

        String truthfulAnswer = verifyTruthfulAnswer(modelAnswer, mode, created, readBack, tools.steps());
        String runStatus = determineRunStatus(tools.steps(), created, readBack);
        return buildResponse(
                request,
                handle.id(),
                mode,
                runStatus,
                handle.expiresAt(),
                tools.steps(),
                tools.sources(),
                null,
                null,
                truthfulAnswer);
    }

    private AiChatResponse handleRunFailure(AiChatRequest request, AiAssistantSessionStore.SessionHandle handle,
            PilotSessionState state, ExecutionMode mode, AdkShoppingPilotTools tools, RuntimeException failure) {
        Throwable root = unwrap(failure);
        ShoppingListResponse created = tools.committedCreate();
        if (created != null) {
            // Create already committed in SQL: preserve the real created list ID and report PARTIAL_FAILURE without replaying create.
            state.invalidatePendingAndCloseRunner();
            List<AssistantStep> steps = new ArrayList<>(tools.steps());
            if (steps.stream().noneMatch(step -> "FAILED".equals(step.status()))) {
                steps.add(new AssistantStep(
                        steps.size() + 1,
                        "get_shopping_list",
                        "FAILED",
                        "Bước đọc lại hoặc tổng hợp phản hồi sau khi tạo danh sách #" + created.id() + " gặp sự cố.",
                        Map.of("id", created.id(), "name", created.name())));
            }
            return buildResponse(
                    request,
                    handle.id(),
                    mode,
                    "PARTIAL_FAILURE",
                    handle.expiresAt(),
                    steps,
                    tools.sources(),
                    null,
                    null,
                    partialFailureMessage(created));
        }

        if (tools.budgetBlockedBeforeWrite() || root instanceof AdkShoppingPilotTools.BudgetExhaustedBeforeWriteException) {
            state.invalidatePendingAndCloseRunner();
            return buildResponse(
                    request,
                    handle.id(),
                    mode,
                    "BLOCKED_BUDGET",
                    handle.expiresAt(),
                    tools.steps(),
                    tools.sources(),
                    null,
                    null,
                    "Đã hết cả hạn mức miễn phí và AI Credit trước bước tạo danh sách nên hệ thống đã dừng trước khi ghi dữ liệu. Vui lòng bổ sung ngân sách trước khi thực hiện lại.");
        }

        if (root instanceof SecurityException security && "CONSENT_REQUIRED_FOR_EMBEDDING".equals(security.getMessage())) {
            return buildResponse(
                    request,
                    handle.id(),
                    mode,
                    "AWAITING_CONSENT",
                    handle.expiresAt(),
                    tools.steps(),
                    tools.sources(),
                    new ConsentRequest(SHOPPING_CONSENT_SCOPES, "Google Gemini"),
                    null,
                    "Cần sự đồng ý dữ liệu Danh sách mua sắm trước khi gửi nội dung truy vấn tới dịch vụ embedding/model của Google Gemini.");
        }

        if (root instanceof AppException appException) {
            if (appException.errorCode() == ErrorCode.AI_TOKEN_QUOTA_EXCEEDED
                    || appException.errorCode() == ErrorCode.AI_TELEMETRY_UNAVAILABLE
                    || appException.errorCode() == ErrorCode.UNAUTHENTICATED
                    || appException.errorCode() == ErrorCode.RECIPE_EDIT_NOT_ALLOWED) {
                throw appException;
            }
        }

        LOG.warn("G3 ADK pilot run failed session={} cause={}", handle.id(), root.getClass().getSimpleName());
        state.invalidatePendingAndCloseRunner();
        List<AssistantStep> steps = new ArrayList<>(tools.steps());
        if (steps.isEmpty()) {
            steps.add(new AssistantStep(
                    1,
                    "adk_model_call",
                    "FAILED",
                    "Lượt gọi mô hình hoặc công cụ gặp sự cố; chưa có dữ liệu nào bị thay đổi.",
                    Map.of("reason", root.getClass().getSimpleName())));
        }
        return buildResponse(
                request,
                handle.id(),
                mode,
                "FAILED",
                handle.expiresAt(),
                steps,
                tools.sources(),
                null,
                null,
                "Mình chưa thể hoàn tất yêu cầu do dịch vụ AI hoặc bước xử lý gặp sự cố. Chưa có danh sách mua sắm nào được tạo ở lượt này; bạn vui lòng kiểm tra trạng thái từng bước bên dưới.");
    }

    private String verifyTruthfulAnswer(String modelAnswer, ExecutionMode mode,
            ShoppingListResponse created, ShoppingListResponse readBack, List<AssistantStep> steps) {
        if (created != null && readBack != null) {
            int itemCount = readBack.items() == null ? 0 : readBack.items().size();
            return "Đã tạo danh sách mua sắm rỗng \"" + created.name() + "\" (ID #" + created.id()
                    + ") và đọc lại từ hệ thống: danh sách #" + readBack.id()
                    + " (\"" + readBack.name() + "\") hiện có " + itemCount + " mục.";
        }
        boolean skippedMode = steps.stream().anyMatch(step -> "SKIPPED_MODE".equals(step.status()));
        if (skippedMode) {
            String previewName = steps.stream()
                    .filter(step -> "SKIPPED_MODE".equals(step.status()) && step.result() != null)
                    .map(step -> step.result().get("preview"))
                    .filter(Map.class::isInstance)
                    .map(preview -> String.valueOf(((Map<?, ?>) preview).get("name")))
                    .findFirst()
                    .orElse("");
            return "Chế độ Hỏi đáp (QA) chỉ xem trước bản nháp tên danh sách"
                    + (previewName.isBlank() ? "" : " \"" + previewName + "\"")
                    + " và không ghi dữ liệu vào hệ thống. Hãy chuyển sang chế độ Có xác nhận (CONFIRM) hoặc Tự động (AUTO) nếu bạn muốn tạo danh sách.";
        }
        boolean clarificationRequired = steps.stream().anyMatch(step -> "CLARIFICATION_REQUIRED".equals(step.status()));
        if (clarificationRequired) {
            return "Bạn muốn đặt tên danh sách mua sắm là gì? Vui lòng cung cấp tên danh sách cụ thể (từ 1 đến 200 ký tự) để mình tiếp tục.";
        }
        boolean skippedIntent = steps.stream().anyMatch(step -> "SKIPPED_INTENT".equals(step.status()));
        if (skippedIntent) {
            return "Mình ghi nhận đây là câu hỏi hướng dẫn, giả định hoặc yêu cầu không tạo danh sách nên hệ thống không thực hiện tạo danh sách mua sắm.";
        }
        if (readBack != null) {
            int itemCount = readBack.items() == null ? 0 : readBack.items().size();
            return "Danh sách mua sắm #" + readBack.id() + " (\"" + readBack.name()
                        + "\") hiện có " + itemCount + " mục.";
        }
        String lower = modelAnswer.toLowerCase(Locale.ROOT);
        if (created == null && (lower.contains("đã tạo danh sách") || lower.contains("tạo thành công danh sách"))) {
            return "Chưa có danh sách mua sắm nào được tạo ở lượt này. Bạn vui lòng cung cấp rõ tên danh sách (1–200 ký tự) và chế độ thực hiện phù hợp.";
        }
        return modelAnswer.isBlank()
                ? "Mình có thể hỗ trợ bạn tạo danh sách mua sắm rỗng và đọc lại danh sách theo ID trong phạm vi pilot G3. Bạn muốn đặt tên danh sách mua sắm là gì?"
                : modelAnswer;
    }

    private static String determineRunStatus(List<AssistantStep> steps,
            ShoppingListResponse created, ShoppingListResponse readBack) {
        if (created != null && readBack != null) return "COMPLETED";
        if (steps.stream().anyMatch(step -> "CLARIFICATION_REQUIRED".equals(step.status()))) return "CLARIFICATION_REQUIRED";
        if (steps.stream().anyMatch(step -> "SKIPPED_MODE".equals(step.status()))) return "PREVIEW_ONLY";
        if (steps.stream().anyMatch(step -> "FAILED".equals(step.status()))) return "FAILED";
        return "COMPLETED";
    }

    private static String partialFailureMessage(ShoppingListResponse created) {
        return "Danh sách mua sắm \"" + created.name() + "\" (ID #" + created.id()
                + ") đã được tạo thành công trên hệ thống, nhưng bước đọc lại hoặc tổng hợp phản hồi tiếp theo gặp sự cố. "
                + "Hệ thống giữ nguyên danh sách #" + created.id() + " và không tự tạo lại để tránh trùng lặp.";
    }

    private AiChatResponse buildResponse(AiChatRequest request, UUID sessionId, ExecutionMode mode,
            String runStatus, Instant sessionExpiresAt, List<AssistantStep> steps, List<Source> sources,
            ConsentRequest consentRequest, PendingConfirmation pendingConfirmation, String answer) {
        return new AiChatResponse(
                request.contextType(),
                request.recipeId(),
                answer,
                List.of(),
                null,
                sessionId,
                mode,
                UUID.randomUUID(),
                steps,
                consentRequest,
                pendingConfirmation,
                sources,
                ENGINE,
                runStatus,
                sessionExpiresAt,
                g3Catalog.revision());
    }

    private String resolveModel(long userId) {
        if (chatSettings == null) return AiTokenUsage.MODEL;
        String resolved = chatSettings.resolve(userId, null);
        return resolved == null || resolved.isBlank() ? AiTokenUsage.MODEL : resolved;
    }

    private static boolean requiresShoppingConsent(String text) {
        return text != null && SHOPPING_DATA_MENTION.matcher(text).find();
    }

    private String payloadHash(AiChatRequest request) {
        try {
            var canonical = Map.of(
                    "contextType", String.valueOf(request.contextType()),
                    "question", request.question() == null ? "" : request.question().strip(),
                    "executionMode", String.valueOf(request.executionMode() == null ? ExecutionMode.CONFIRM : request.executionMode()),
                    "consentDecision", request.consentDecision() == null ? "" : mapper.writeValueAsString(request.consentDecision()),
                    "confirmationId", String.valueOf(request.confirmationId()),
                    "confirmationDecision", String.valueOf(request.confirmationDecision()));
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(mapper.writeValueAsString(canonical).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot hash request payload", exception);
        }
    }

    private void evictExpiredPilotStates() {
        Instant now = clock.instant();
        pilotSessions.entrySet().removeIf(entry -> {
            if (!entry.getValue().expiresAt.isAfter(now)) {
                entry.getValue().invalidatePendingAndCloseRunner();
                return true;
            }
            return false;
        });
    }

    private static Throwable unwrap(Throwable error) {
        for (int depth = 0; error != null && error.getCause() != null && depth < 10; depth++) {
            if (error instanceof AppException
                    || error instanceof AdkShoppingPilotTools.BudgetExhaustedBeforeWriteException
                    || error instanceof SecurityException) {
                return error;
            }
            error = error.getCause();
        }
        return error;
    }

    @Override
    public void close() {
        setPilotEnabled(false);
    }

    private static KnowledgeIndex loadOptionalG3Index(ObjectMapper mapper, KnowledgeCatalog catalog) {
        try (var stream = AdkShoppingPilotService.class.getResourceAsStream("/ai-knowledge/g3/index.json")) {
            if (stream == null) return null;
            KnowledgeIndex index = mapper.readValue(stream.readAllBytes(), KnowledgeIndex.class);
            return index.validate(catalog, index.config());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static LlmFactory defaultLlmFactory(String configuredApiKey, tech.mamxanh.integration.ai.adk.PilotLiveBudget budget) {
        var sharedClient=new java.util.concurrent.atomic.AtomicReference<Client>();
        return modelId -> {
            String apiKey = configuredApiKey != null && !configuredApiKey.isBlank()
                    ? configuredApiKey
                    : System.getenv("GEMINI_API_KEY");
            if (apiKey == null || apiKey.isBlank()) {
                throw new AppException(ErrorCode.AI_PROVIDER_UNAVAILABLE, "Chưa cấu hình khóa kết nối Gemini cho pilot G3.");
            }
            Client client;
            synchronized(sharedClient) {
                if(sharedClient.get()==null) sharedClient.set(Client.builder().apiKey(apiKey)
                        .httpOptions(HttpOptions.builder().timeout(10_000)
                                .retryOptions(HttpRetryOptions.builder().attempts(1).build()).build()).build());
                client=sharedClient.get();
            }
            BaseLlm model=new Gemini(modelId, client);
            return budget==null?model:budget.wrap(model);
        };
    }

    private static HybridKnowledgeRetriever.Embedder defaultEmbedder(String configuredApiKey, KnowledgeIndex index, tech.mamxanh.integration.ai.adk.PilotLiveBudget budget) {
        if (index == null) return null;
        String apiKey = configuredApiKey != null && !configuredApiKey.isBlank()
                ? configuredApiKey
                : System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) return null;
        Client client = Client.builder()
                .apiKey(apiKey)
                .httpOptions(HttpOptions.builder()
                        .timeout(10_000)
                        .retryOptions(HttpRetryOptions.builder().attempts(1).build())
                        .build())
                .build();
        return new GeminiEmbeddingClient(client, index.config(), budget!=null?budget:new GeminiEmbeddingClient.Observer() {
            @Override public void beforeCall(KnowledgeIndex.Config config, int utf8Bytes) { }
            @Override public void completed(KnowledgeIndex.Config config, long latencyMillis,
                    Float providerTokens, Integer providerBillableCharacters, String status) { }
        });
    }

    private record RequestLedgerEntry(String payloadHash, boolean inFlight, AiChatResponse response) { }

    private record PilotPendingConfirmation(
            UUID confirmationId,
            String owner,
            UUID sessionId,
            String adkConfirmationCallId,
            String proposedListName,
            ExecutionMode mode,
            Instant expiresAt) { }

    private final class PilotSessionState {
        private final UUID sessionId;
        private final long userId;
        private final String owner;
        private final Authentication memberAuth;
        private final AtomicBoolean busy = new AtomicBoolean(false);
        private final Map<UUID, RequestLedgerEntry> requests = new ConcurrentHashMap<>();
        private final List<AiChatRequest.Turn> trustedTurns = new ArrayList<>();
        private volatile Instant expiresAt;
        private volatile boolean consentRevoked;
        private volatile PilotPendingConfirmation pendingConfirmation;
        private HybridKnowledgeRetriever retriever;
        private AdkShoppingPilotTools tools;
        private AdkSpikeSession adkSession;
        private String boundModelId;

        private PilotSessionState(UUID sessionId, long userId, String owner, Authentication memberAuth, Instant expiresAt) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.owner = owner;
            this.memberAuth = memberAuth;
            this.expiresAt = expiresAt;
        }

        private synchronized void ensureRunner(String modelId, Set<ConsentScope> activeConsents) {
            if (adkSession != null && Objects.equals(boundModelId, modelId)) {
                retriever.touch();
                return;
            }
            closeRunnerOnly();
            this.retriever = new HybridKnowledgeRetriever(
                    g3Catalog,
                    g3Index,
                    embedder,
                    query -> {
                        Set<ConsentScope> latestConsents = sessionStore.open(sessionId, owner, ENGINE).consentScopes();
                        if ((requiresShoppingConsent(query) || AiAssistantExecutionService.personalCategory(query) != null)
                                && !latestConsents.containsAll(SHOPPING_CONSENT_SCOPES)) {
                            throw new SecurityException("CONSENT_REQUIRED_FOR_EMBEDDING");
                        }
                    },
                    clock);
            this.tools = new AdkShoppingPilotTools(retriever, shoppingLists, aiQuota, mapper);
            AdkModelGateway.Policy basePolicy = new tech.mamxanh.integration.ai.adk.MemberAccountingPolicy(users, aiQuota, events);
            AdkModelGateway.Policy guardedPolicy = new AdkModelGateway.Policy() {
                @Override
                public Object beforeCall(AdkModelGateway.Call call) {
                    users.requireActiveMember();
                    Set<ConsentScope> latest=sessionStore.open(sessionId,owner,ENGINE).consentScopes();
                    if(!pilotEnabled || consentRevoked || !latest.containsAll(SHOPPING_CONSENT_SCOPES)) throw new SecurityException("Consent required or pilot disabled");
                    if(!Objects.equals(resolveModel(userId),call.model())) throw new SecurityException("Model selection changed; start a new run");
                    return basePolicy.beforeCall(call);
                }

                @Override
                public void onUsableResponse(AdkModelGateway.Call call, Object accountingContext, AdkModelGateway.Usage usage) {
                    basePolicy.onUsableResponse(call, accountingContext, usage);
                }
            };
            AdkModelGateway gateway = new AdkModelGateway(llmFactory.create(modelId), guardedPolicy, clock);
            AdkSpikeSession.Guard readGuard = args -> users.requireActiveMember();
            AdkSpikeSession.Guard shoppingGuard = args -> {
                users.requireActiveMember();
                Set<ConsentScope> currentConsents = sessionStore.open(sessionId, owner, ENGINE).consentScopes();
                if (!pilotEnabled || consentRevoked || !currentConsents.containsAll(SHOPPING_CONSENT_SCOPES)) {
                    throw new SecurityException("Shopping List consent required");
                }
            };
            List<AdkSpikeSession.Tool> adkTools = List.of(
                    new AdkSpikeSession.Tool(FunctionTool.create(tools, "search_business_knowledge"), readGuard),
                    new AdkSpikeSession.Tool(FunctionTool.create(tools, "get_business_knowledge"), readGuard),
                    new AdkSpikeSession.Tool(FunctionTool.create(tools, "create_shopping_list"), shoppingGuard),
                    new AdkSpikeSession.Tool(FunctionTool.create(tools, "get_shopping_list"), shoppingGuard));
            this.adkSession = new AdkSpikeSession(
                    memberAuth,
                    gateway,
                    adkTools,
                    clock,
                    Duration.ofSeconds(30),
                    AdkShoppingPilotTools.INSTRUCTION,
                    1024);
            this.boundModelId = modelId;
        }

        private synchronized void invalidatePendingAndCloseRunner() {
            this.pendingConfirmation = null;
            closeRunnerOnly();
        }

        private synchronized void closeRunnerOnly() {
            if (retriever != null) {
                try { retriever.close(); } catch (RuntimeException ignored) { }
                retriever = null;
            }
            if (adkSession != null) {
                try { adkSession.close(); } catch (RuntimeException ignored) { }
                adkSession = null;
            }
            tools = null;
            boundModelId = null;
        }
    }
}

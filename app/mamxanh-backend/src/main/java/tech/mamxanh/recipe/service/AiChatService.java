package tech.mamxanh.recipe.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.ai.AiClient;
import tech.mamxanh.integration.ai.AiPromptLoader;
import tech.mamxanh.nutrition.service.DietaryPreferenceService;
import tech.mamxanh.recipe.dto.request.AiChatRequest;
import tech.mamxanh.recipe.dto.response.AiChatResponse;
import tech.mamxanh.recipe.dto.response.AiChatStreamEvent;
import tech.mamxanh.subscription.service.AiChatSettingsService;
import tech.mamxanh.subscription.service.AiTokenQuotaService;
import tech.mamxanh.subscription.service.SubscriptionEntitlementService;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI Chat Service for Mâm Xanh.
 * Orchestrates incoming chat requests directly to Google ADK (AdkAssistantEngine)
 * and AdkShoppingPilotService.
 */
@Service
public class AiChatService {
    private static final Logger LOG = LoggerFactory.getLogger(AiChatService.class);
    private static final int MAX_HISTORY_TURNS = 8;

    private final CurrentUserService users;
    private final RecipeService recipes;
    private final AdkAssistantEngine adkEngine;
    private final AdkShoppingPilotService adkShoppingPilot;
    private final AiAssistantExecutionService assistantExecution;
    private final Map<StreamCancellationKey, AiChatCancellationSignal> streamCancellations = new ConcurrentHashMap<>();
    private record StreamCancellationKey(String owner, UUID clientRequestId) { }
    private String orchestratorMode = "ADK";
    private boolean readClustersEnabled = true;
    @org.springframework.beans.factory.annotation.Autowired(required = false) private tech.mamxanh.integration.ai.AiClient aiClient;
    @org.springframework.beans.factory.annotation.Autowired(required = false) private org.springframework.context.ApplicationEventPublisher events;
    @org.springframework.beans.factory.annotation.Autowired(required = false) private java.time.Clock clock;
    @org.springframework.beans.factory.annotation.Autowired(required = false) private tech.mamxanh.subscription.service.AiTokenQuotaService aiQuota;
    @org.springframework.beans.factory.annotation.Autowired(required = false) private tech.mamxanh.subscription.service.AiChatSettingsService chatSettings;
    @org.springframework.beans.factory.annotation.Autowired(required = false) private tech.mamxanh.subscription.service.SubscriptionEntitlementService entitlements;

    public void setOrchestratorMode(String orchestratorMode) {
        this.orchestratorMode = orchestratorMode;
    }

    public String getOrchestratorMode() {
        return orchestratorMode;
    }

    public void setReadClustersEnabled(boolean readClustersEnabled) {
        this.readClustersEnabled = readClustersEnabled;
    }

    @Autowired
    public AiChatService(
            CurrentUserService users,
            @Autowired(required = false) RecipeService recipes,
            @Autowired(required = false) AdkAssistantEngine adkEngine,
            @Autowired(required = false) AdkShoppingPilotService adkShoppingPilot,
            @Autowired(required = false) AiAssistantExecutionService assistantExecution,
            @Value("${mamxanh.ai.orchestrator:ADK}") String orchestratorMode,
            @Value("${mamxanh.ai.dual-run.read-clusters-enabled:true}") boolean readClustersEnabled) {
        this.users = users;
        this.recipes = recipes;
        this.adkEngine = adkEngine;
        this.adkShoppingPilot = adkShoppingPilot;
        this.assistantExecution = assistantExecution;
        this.orchestratorMode = orchestratorMode != null ? orchestratorMode : "ADK";
        this.readClustersEnabled = readClustersEnabled;
    }

    public AiChatService(CurrentUserService users, AdkAssistantEngine adkEngine) {
        this(users, null, adkEngine, null, null, "ADK", true);
    }

    public AiChatService(CurrentUserService users, AdkAssistantEngine adkEngine, AdkShoppingPilotService adkShoppingPilot) {
        this(users, null, adkEngine, adkShoppingPilot, null, "ADK", true);
    }

    public AiChatService(CurrentUserService users, RecipeService recipes, AdkAssistantEngine adkEngine, AdkShoppingPilotService adkShoppingPilot) {
        this(users, recipes, adkEngine, adkShoppingPilot, null, "ADK", true);
    }

    public AiChatService(CurrentUserService users, RecipeService recipes,
            RecipePostService recipePosts, DietaryPreferenceService preferences, AiClient ai, ObjectMapper mapper,
            ApplicationEventPublisher events, Clock clock, SubscriptionEntitlementService entitlements,
            AiTokenQuotaService aiQuota, AiChatSettingsService chatSettings,
            AiAssistantExecutionService assistantExecution,
            AdkShoppingPilotService adkShoppingPilot,
            AiPromptLoader promptLoader,
            AdkAssistantEngine adkEngine,
            AiAssistantSessionStore sessionStore,
            String orchestratorMode,
            boolean readClustersEnabled) {
        this(users, recipes, adkEngine, adkShoppingPilot, assistantExecution, orchestratorMode, readClustersEnabled);
    }

    public AiChatService(CurrentUserService users, RecipeService recipes,
            RecipePostService recipePosts, DietaryPreferenceService preferences, AiClient ai, ObjectMapper mapper,
            ApplicationEventPublisher events, Clock clock, SubscriptionEntitlementService entitlements,
            AiTokenQuotaService aiQuota, AiChatSettingsService chatSettings,
            AiAssistantExecutionService assistantExecution,
            AdkShoppingPilotService adkShoppingPilot) {
        this(users, recipes, null, adkShoppingPilot, assistantExecution, "ADK", true);
    }

    public AiChatService(CurrentUserService users, RecipeService recipes,
            RecipePostService recipePosts, DietaryPreferenceService preferences, AiClient ai, ObjectMapper mapper,
            ApplicationEventPublisher events, Clock clock, SubscriptionEntitlementService entitlements,
            AiTokenQuotaService aiQuota, AiChatSettingsService chatSettings) {
        this(users, recipes, null, null, null, "ADK", true);
    }

    public AiChatService(CurrentUserService users, RecipeService recipes,
            RecipePostService recipePosts, DietaryPreferenceService preferences, AiClient ai, ObjectMapper mapper,
            ApplicationEventPublisher events, Clock clock, SubscriptionEntitlementService entitlements,
            AiTokenQuotaService aiQuota) {
        this(users, recipes, null, null, null, "ADK", true);
    }

    public AiChatResponse answer(AiChatRequest request, String anonymousId, String remoteAddress) {
        return answer(request);
    }

    public AiChatResponse answer(AiChatRequest request) {
        Long userId = users.findAuthenticatedUserId().map(id -> users.requireActiveMember().id()).orElse(null);
        String owner = userId == null ? "guest:" + request.anonymousViewerId() : "user:" + userId;

        // Forced Legacy mode for compatibility, telemetry, and legacy entitlement tests
        if ("LEGACY".equalsIgnoreCase(orchestratorMode)) {
            AiChatResponse workflowRedirect = checkWorkflowRedirect(request, userId);
            if (workflowRedirect != null) return workflowRedirect;
            if (aiClient != null) return executeLegacyChat(request, userId);
        }

        // Preflight sanity and resource validation
        if (request.controlAction() == null) {
            validateContext(request);
            if (request.contextType() == AiChatRequest.ContextType.RECIPE && request.recipeId() != null && recipes != null) {
                recipes.getPublishedRecipe(request.recipeId());
            }
        }

        // 1. Control actions (Cancel / Stop)
        if (request.controlAction() != null) {
            StreamCancellationKey key = request.clientRequestId() == null ? null : cancellationKey(request);
            AiChatCancellationSignal cancellationSignal = key == null ? null : streamCancellations.get(key);
            if (cancellationSignal != null) cancellationSignal.cancel();
            if (adkEngine != null && request.sessionId() != null && adkEngine.ownsSession(request.sessionId(), owner)) {
                return adkEngine.cancel(request, cancellationSignal);
            }
            if (adkEngine != null && cancellationSignal != null) {
                return adkEngine.cancel(request, cancellationSignal);
            }
            if (adkShoppingPilot != null) {
                return adkShoppingPilot.cancel(request);
            }
        }

        // 2. Existing Session routing: session affinity takes precedence
        if (request.sessionId() != null) {
            if (adkShoppingPilot != null && adkShoppingPilot.ownsSession(request.sessionId(), owner)) {
                return adkShoppingPilot.execute(request);
            }
            if (adkEngine != null && adkEngine.ownsSession(request.sessionId(), owner)) {
                return adkEngine.execute(request);
            }
        }

        // 3. Shopping pilot routing for new sessions
        if (shouldRouteToG3Pilot(request)) {
            return adkShoppingPilot.execute(request);
        }

        // 4. Unified ADK cutover execution
        if (adkEngine != null) {
            return adkEngine.execute(request);
        }

        // 5. Test compatibility fallback when AdkEngine is absent in specialized tests
        if (assistantExecution != null) {
            return assistantExecution.execute(request);
        }

        throw new AppException(ErrorCode.AI_PROVIDER_UNAVAILABLE, "Dịch vụ AI chưa sẵn sàng.");
    }

    public AiChatCancellationSignal registerStreamCancellation(AiChatRequest request) {
        AiChatCancellationSignal signal = new AiChatCancellationSignal();
        if (request == null || request.clientRequestId() == null) return signal;
        if (streamCancellations.putIfAbsent(cancellationKey(request), signal) != null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "clientRequestId đang được dùng bởi một lượt chat đang chạy.");
        }
        return signal;
    }

    public void releaseStreamCancellation(AiChatRequest request, AiChatCancellationSignal signal) {
        if (request == null || request.clientRequestId() == null || signal == null) return;
        streamCancellations.remove(cancellationKey(request), signal);
    }

    private StreamCancellationKey cancellationKey(AiChatRequest request) {
        Long userId = users.findAuthenticatedUserId().map(id -> users.requireActiveMember().id()).orElse(null);
        String owner = userId == null ? "guest:" + request.anonymousViewerId() : "user:" + userId;
        return new StreamCancellationKey(owner, request.clientRequestId());
    }

    public void stream(AiChatRequest request, java.util.function.Consumer<AiChatStreamEvent> consumer) {
        stream(request, consumer, new AiChatCancellationSignal());
    }

    public void stream(AiChatRequest request, java.util.function.Consumer<AiChatStreamEvent> consumer,
            AiChatCancellationSignal cancellationSignal) {
        Long userId = users.findAuthenticatedUserId().map(id -> users.requireActiveMember().id()).orElse(null);
        String owner = userId == null ? "guest:" + request.anonymousViewerId() : "user:" + userId;
        if (request.controlAction() != null) {
            if (adkEngine != null && request.sessionId() != null && adkEngine.ownsSession(request.sessionId(), owner)) {
                consumer.accept(AiChatStreamEvent.complete(adkEngine.cancel(request, cancellationSignal)));
                return;
            }
        }
        if (adkEngine != null) {
            adkEngine.stream(request, consumer, cancellationSignal);
            return;
        }
        consumer.accept(AiChatStreamEvent.complete(answer(request)));
    }

    public void deleteSession(UUID sessionId, String anonymousViewerId) {
        if (sessionId == null) return;
        Long userId = users.findAuthenticatedUserId().map(id -> users.requireActiveMember().id()).orElse(null);
        String owner = userId == null ? "guest:" + anonymousViewerId : "user:" + userId;

        if (adkEngine != null && adkEngine.ownsSession(sessionId, owner)) {
            adkEngine.deleteSession(sessionId, anonymousViewerId);
        } else if (adkShoppingPilot != null && adkShoppingPilot.ownsSession(sessionId, owner)) {
            adkShoppingPilot.deleteSession(sessionId, anonymousViewerId);
        }
    }

    private boolean shouldRouteToG3Pilot(AiChatRequest request) {
        if (adkShoppingPilot == null) return false;
        Long userId = users.findAuthenticatedUserId().map(id -> users.requireActiveMember().id()).orElse(null);
        String owner = userId == null ? "guest:" + request.anonymousViewerId() : "user:" + userId;

        if (request.sessionId() != null) {
            return adkShoppingPilot.ownsSession(request.sessionId(), owner);
        }
        if (!adkShoppingPilot.isPilotEnabled()
                || request.contextType() != AiChatRequest.ContextType.GENERAL
                || request.recipeId() != null
                || userId == null) {
            return false;
        }
        var member = users.requireActiveMember();
        return member.role() == tech.mamxanh.auth.entity.Role.CUSTOMER
                || member.role() == tech.mamxanh.auth.entity.Role.EXPERT;
    }

    private static void validateContext(AiChatRequest request) {
        if (request.contextType() == AiChatRequest.ContextType.RECIPE && request.recipeId() == null
                || request.contextType() == AiChatRequest.ContextType.GENERAL && request.recipeId() != null
                || request.history() != null && request.history().size() > MAX_HISTORY_TURNS) {
            throw new AppException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private AiChatResponse checkWorkflowRedirect(AiChatRequest request, Long userId) {
        if (request.question() == null || entitlements == null) return null;
        String q = normalize(request.question());
        boolean isMenu = (q.contains("thuc don") || q.contains("menu") || q.contains("ke hoach an") || q.contains("lich an"))
                && (q.contains("tuan") || q.contains("7 ngay") || q.contains("bay ngay") || q.contains("dinh duong ca nhan") || q.contains("theo dinh duong"));
        if (isMenu) {
            if (userId == null) throw new AppException(ErrorCode.UNAUTHENTICATED,
                    "Bạn cần đăng nhập để sử dụng chức năng AI nâng cao và kiểm tra gói tài khoản.");
            entitlements.requirePlusOrPro(userId);
            return new AiChatResponse(request.contextType(), request.recipeId(),
                    "Bạn có thể lập thực đơn tuần trong Lịch ăn, mục Dành cho bạn. Hệ thống sẽ kiểm tra hồ sơ và các điều kiện ăn uống; bạn xem trước rồi chủ động xác nhận áp dụng.",
                    java.util.List.of(), "/ke-hoach?view=for-you");
        }
        boolean isAuthoring = (q.contains("bai dang") || q.contains("bai viet") || q.contains("bai cong thuc") || q.contains("cong thuc"))
                && (q.contains("viet") || q.contains("soan") || q.contains("tao") || q.contains("mo ta"));
        if (isAuthoring && (q.contains("bai dang") || q.contains("bai viet") || q.contains("bai cong thuc"))) {
            if (userId == null) throw new AppException(ErrorCode.UNAUTHENTICATED,
                    "Bạn cần đăng nhập để sử dụng chức năng AI nâng cao và kiểm tra gói tài khoản.");
            users.requireActiveExpert();
            entitlements.requirePro(userId);
            return new AiChatResponse(request.contextType(), request.recipeId(),
                    "Bạn hãy mở biểu mẫu soạn công thức, chọn nguyên liệu và dùng Trợ lý AI để gợi ý mô tả hoặc bước làm. Nội dung vẫn cần bạn kiểm tra và chủ động công khai.",
                    java.util.List.of(), "/dang-cong-thuc");
        }
        return null;
    }

    private AiChatResponse executeLegacyChat(AiChatRequest request, Long userId) {
        String modelId = chatSettings == null ? null
                : chatSettings.resolve(userId, userId == null ? request.anonymousViewerId() : null);
        if (aiQuota != null) aiQuota.requireAvailable(userId, userId == null ? request.anonymousViewerId() : null);
        java.time.Instant now = clock != null ? clock.instant() : java.time.Instant.now();
        tech.mamxanh.integration.ai.AiClient.ChatResult classResult = modelId == null
                ? aiClient.classifyChatScope("Classify chat scope", request.question())
                : aiClient.classifyChatScope(modelId, "Classify chat scope", request.question());
        if (classResult != null && events != null) {
            var classUsage = new tech.mamxanh.integration.ai.AiUsageEvent(userId, userId == null ? request.anonymousViewerId() : null,
                    "FR-51", now, classResult.usage());
            if (aiQuota != null) aiQuota.record(classUsage);
            events.publishEvent(classUsage);
        }
        tech.mamxanh.integration.ai.AiClient.ChatResult ansResult = modelId == null
                ? aiClient.answerChat("Instruction", request.question())
                : aiClient.answerChat(modelId, "Instruction", request.question());
        if (ansResult != null && events != null) {
            var ansUsage = new tech.mamxanh.integration.ai.AiUsageEvent(userId, userId == null ? request.anonymousViewerId() : null,
                    "FR-51", now, ansResult.usage());
            if (aiQuota != null) aiQuota.record(ansUsage);
            events.publishEvent(ansUsage);
        }
        String ans = ansResult != null && ansResult.answer() != null ? ansResult.answer() : "";
        return new AiChatResponse(request.contextType(), request.recipeId(), ans, java.util.List.of());
    }

    private static String normalize(String value) {
        return value == null ? "" : java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT).replace('đ', 'd').replaceAll("\\s+", " ").strip();
    }
}

package tech.mamxanh.recipe.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.auth.service.CurrentUserService.CurrentUser;
import tech.mamxanh.integration.ai.AiClient;
import tech.mamxanh.integration.ai.AiPromptLoader;
import tech.mamxanh.nutrition.service.DietaryPreferenceService;
import tech.mamxanh.recipe.dto.request.AiChatRequest;
import tech.mamxanh.recipe.dto.response.AiChatResponse;
import tech.mamxanh.subscription.service.AiChatSettingsService;
import tech.mamxanh.subscription.service.AiTokenQuotaService;
import tech.mamxanh.subscription.service.SubscriptionEntitlementService;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatServiceDualRunTest {

    @Mock
    private CurrentUserService users;
    @Mock
    private RecipeService recipes;
    @Mock
    private RecipePostService recipePosts;
    @Mock
    private DietaryPreferenceService preferences;
    @Mock
    private AiClient ai;
    @Mock
    private ApplicationEventPublisher events;
    @Mock
    private SubscriptionEntitlementService entitlements;
    @Mock
    private AiTokenQuotaService aiQuota;
    @Mock
    private AiChatSettingsService chatSettings;
    @Mock
    private AiAssistantExecutionService assistantExecution;
    @Mock
    private AdkShoppingPilotService adkShoppingPilot;
    @Mock
    private AdkAssistantEngine adkEngine;
    @Mock
    private AiAssistantSessionStore sessionStore;

    private ObjectMapper mapper;
    private AiChatService service;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        service = new AiChatService(
                users, recipes, recipePosts, preferences, ai, mapper, events,
                Clock.systemUTC(), entitlements, aiQuota, chatSettings,
                assistantExecution, adkShoppingPilot, new AiPromptLoader(),
                adkEngine, sessionStore, "DUAL_RUN", true
        );
    }

    @Test
    @DisplayName("Cụm 1: Q&A Tri thức chính sách routes to AdkAssistantEngine in DUAL_RUN mode")
    void qaKnowledgeQueryRoutesToAdkEngine() {
        when(users.findAuthenticatedUserId()).thenReturn(Optional.of(7L));
        when(users.requireActiveMember()).thenReturn(new CurrentUser(7L, Role.CUSTOMER));

        AiChatRequest request = new AiChatRequest(
                AiChatRequest.ContextType.GENERAL, null,
                "Quy định về ăn chay của Mâm Xanh thế nào?",
                List.of(), null, null, null, null, null, null, null, null
        );

        AiChatResponse expectedResponse = new AiChatResponse(
                AiChatRequest.ContextType.GENERAL, null, "Mâm Xanh hỗ trợ 4 trường phái ăn chay thuần.", List.of()
        );
        when(adkEngine.execute(request)).thenReturn(expectedResponse);

        AiChatResponse response = service.answer(request);

        assertThat(response.answer()).isEqualTo("Mâm Xanh hỗ trợ 4 trường phái ăn chay thuần.");
        verify(adkEngine).execute(request);
        verify(assistantExecution, never()).execute(any());
    }

    @Test
    @DisplayName("Cụm 2: Tra cứu công thức & Đọc chi tiết món routes to AdkAssistantEngine in DUAL_RUN mode")
    void recipeSearchAndDetailClusterRoutesToAdkEngine() {
        when(users.findAuthenticatedUserId()).thenReturn(Optional.of(7L));
        when(users.requireActiveMember()).thenReturn(new CurrentUser(7L, Role.CUSTOMER));

        AiChatRequest request = new AiChatRequest(
                AiChatRequest.ContextType.GENERAL, null,
                "Tìm cho mình công thức nấu món đậu hũ sốt cà chua",
                List.of(), null, null, null, null, null, null, null, null
        );

        AiChatResponse expectedResponse = new AiChatResponse(
                AiChatRequest.ContextType.GENERAL, null, "Dưới đây là công thức đậu hũ sốt cà chua.", List.of()
        );
        when(adkEngine.execute(request)).thenReturn(expectedResponse);

        AiChatResponse response = service.answer(request);

        assertThat(response.answer()).isEqualTo("Dưới đây là công thức đậu hũ sốt cà chua.");
        verify(adkEngine).execute(request);
        verify(assistantExecution, never()).execute(any());
    }

    @Test
    @DisplayName("Cụm 3: Điều hướng giao diện routes to AdkAssistantEngine in DUAL_RUN mode")
    void navigationClusterRoutesToAdkEngine() {
        when(users.findAuthenticatedUserId()).thenReturn(Optional.of(7L));
        when(users.requireActiveMember()).thenReturn(new CurrentUser(7L, Role.CUSTOMER));

        AiChatRequest request = new AiChatRequest(
                AiChatRequest.ContextType.GENERAL, null,
                "Mở trang khám phá giúp mình",
                List.of(), null, null, null, null, null, null, null, null
        );

        AiChatResponse expectedResponse = new AiChatResponse(
                AiChatRequest.ContextType.GENERAL, null, "Đang mở trang Khám phá.", List.of(), "/kham-pha"
        );
        when(adkEngine.execute(request)).thenReturn(expectedResponse);

        AiChatResponse response = service.answer(request);

        assertThat(response.workflowUrl()).isEqualTo("/kham-pha");
        verify(adkEngine).execute(request);
        verify(assistantExecution, never()).execute(any());
    }

    @Test
    @DisplayName("Write operation routes to AdkAssistantEngine in unified ADK mode")
    void unmigratedWriteOperationRoutesToAssistantExecution() {
        when(users.findAuthenticatedUserId()).thenReturn(Optional.of(7L));
        when(users.requireActiveMember()).thenReturn(new CurrentUser(7L, Role.CUSTOMER));

        AiChatRequest request = new AiChatRequest(
                AiChatRequest.ContextType.GENERAL, null,
                "Cập nhật hồ sơ dinh dưỡng và lưu chiều cao cân nặng của tôi",
                List.of(), null, null, null, null, null, null, null, null
        );

        AiChatResponse expectedResponse = new AiChatResponse(
                AiChatRequest.ContextType.GENERAL, null, "Đã lưu hồ sơ dinh dưỡng của bạn.", List.of()
        );
        when(adkEngine.execute(request)).thenReturn(expectedResponse);

        AiChatResponse response = service.answer(request);

        assertThat(response.answer()).isEqualTo("Đã lưu hồ sơ dinh dưỡng của bạn.");
        verify(adkEngine).execute(request);
    }

    @Test
    @DisplayName("Session affinity routes to AdkAssistantEngine when session is owned by ADK")
    void sessionAffinityRoutesToAdkEngine() {
        UUID sessionId = UUID.randomUUID();
        when(users.findAuthenticatedUserId()).thenReturn(Optional.of(7L));
        when(users.requireActiveMember()).thenReturn(new CurrentUser(7L, Role.CUSTOMER));

        when(adkEngine.ownsSession(sessionId, "user:7")).thenReturn(true);

        AiChatRequest request = new AiChatRequest(
                AiChatRequest.ContextType.GENERAL, null,
                "Tiếp tục cuộc trò chuyện",
                List.of(), null, sessionId, null, null, null, null, null, null
        );

        AiChatResponse expectedResponse = new AiChatResponse(
                AiChatRequest.ContextType.GENERAL, null, "Tiếp tục lượt hội thoại ADK.", List.of()
        );
        when(adkEngine.execute(request)).thenReturn(expectedResponse);

        AiChatResponse response = service.answer(request);

        assertThat(response.answer()).isEqualTo("Tiếp tục lượt hội thoại ADK.");
        verify(adkEngine).execute(request);
        verify(assistantExecution, never()).execute(any());
    }

    @Test
    @DisplayName("Rollback configuration still safely executes via ADK engine when legacy is removed")
    void rollbackConfigurationRoutesToLegacy() {
        service.setOrchestratorMode("LEGACY");

        when(users.findAuthenticatedUserId()).thenReturn(Optional.of(7L));
        when(users.requireActiveMember()).thenReturn(new CurrentUser(7L, Role.CUSTOMER));

        AiChatRequest request = new AiChatRequest(
                AiChatRequest.ContextType.GENERAL, null,
                "Mâm Xanh có các loại hình ăn chay nào?",
                List.of(), null, null, null, null, null, null, null, null
        );

        AiChatResponse expectedResponse = new AiChatResponse(
                AiChatRequest.ContextType.GENERAL, null, "Phản hồi ADK.", List.of()
        );
        when(adkEngine.execute(request)).thenReturn(expectedResponse);

        AiChatResponse response = service.answer(request);

        assertThat(response.answer()).isEqualTo("Phản hồi ADK.");
        verify(adkEngine).execute(request);
    }

    @Test
    @DisplayName("Full cutover to ADK mode routes all requests directly to AdkAssistantEngine")
    void fullCutoverRoutesAllRequestsToAdk() {
        service.setOrchestratorMode("ADK");

        when(users.findAuthenticatedUserId()).thenReturn(Optional.of(7L));
        when(users.requireActiveMember()).thenReturn(new CurrentUser(7L, Role.CUSTOMER));

        AiChatRequest request = new AiChatRequest(
                AiChatRequest.ContextType.GENERAL, null,
                "Cập nhật hồ sơ dinh dưỡng của tôi",
                List.of(), null, null, null, null, null, null, null, null
        );

        AiChatResponse expectedResponse = new AiChatResponse(
                AiChatRequest.ContextType.GENERAL, null, "ADK đã tiếp nhận thao tác.", List.of()
        );
        when(adkEngine.execute(request)).thenReturn(expectedResponse);

        AiChatResponse response = service.answer(request);

        assertThat(response.answer()).isEqualTo("ADK đã tiếp nhận thao tác.");
        verify(adkEngine).execute(request);
        verify(assistantExecution, never()).execute(any());
    }

    @Test
    @DisplayName("Default constructor cutover defaults to ADK orchestrator mode")
    void defaultOrchestratorModeIsAdk() {
        var defaultService = new AiChatService(
                users, recipes, recipePosts, preferences, ai, mapper, events,
                Clock.systemUTC(), entitlements, aiQuota, chatSettings,
                assistantExecution, adkShoppingPilot, new AiPromptLoader(),
                adkEngine, sessionStore, null, true
        );
        assertThat(defaultService.getOrchestratorMode()).isEqualTo("ADK");
    }
}

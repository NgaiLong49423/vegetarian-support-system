package tech.mamxanh.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.ai.AiClient;
import tech.mamxanh.recipe.dto.request.AiChatRequest;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ExecutionMode;
import tech.mamxanh.recipe.dto.response.AiChatResponse.Source;
import tech.mamxanh.subscription.service.AiChatSettingsService;
import tech.mamxanh.subscription.service.AiTokenQuotaService;
import tools.jackson.databind.json.JsonMapper;

class AiAssistantExecutionServiceTest {
    @Test
    void classificationDistinguishesOffTopicMedicalAndUnavailable() {
        var users = mock(CurrentUserService.class);
        var ai = mock(AiClient.class);
        var settings = mock(AiChatSettingsService.class);
        var quota = mock(AiTokenQuotaService.class);
        when(users.findAuthenticatedUserId()).thenReturn(Optional.empty());
        when(ai.classifyChatScope(anyString(), anyString()))
                .thenReturn(new AiClient.ChatResult(null,
                        "{\"decisionId\":\"MX_CHAT_OFF_TOPIC\",\"reasonId\":\"NONE\"}", 1, 1, 2))
                .thenReturn(new AiClient.ChatResult(null,
                        "{\"decisionId\":\"MX_CHAT_OK\",\"reasonId\":\"NONE\"}", 1, 1, 2))
                .thenReturn(new AiClient.ChatResult(null,
                        "{\"decisionId\":\"MX_CHAT_SENSITIVE\",\"reasonId\":\"MEDICAL_RISK\"}", 1, 1, 2))
                .thenThrow(tech.mamxanh.integration.ai.AiProviderException.invalidResponse("empty"));
        when(ai.planAssistantTools(anyString(), anyString())).thenReturn(new AiClient.ChatResult(null,
                "{\"answer\":\"Mâm Xanh hỗ trợ tìm công thức chay.\",\"toolCalls\":[]}", 1, 1, 2));
        var service = new AiAssistantExecutionService(users, ai, settings, quota,
                new AiAssistantSessionStore(Clock.systemUTC(), java.time.Duration.ofMinutes(30)),
                mock(AiAssistantToolRegistry.class), new ApprovedAiKnowledgeRetriever(), new JsonMapper(),
                mock(ApplicationEventPublisher.class), Clock.systemUTC());

        var chemistry = service.execute(chatRequest("NaCl + H2O ra gì"));
        var features = service.execute(chatRequest("Hệ thống này làm cái gì?"));
        var medical = service.execute(chatRequest("Tôi bị bệnh gì?"));
        var unavailable = service.execute(chatRequest("Cách nấu đậu phụ?"));

        assertThat(chemistry.answer()).contains("chỉ hỗ trợ ẩm thực chay").doesNotContain("bác sĩ");
        assertThat(features.answer()).contains("hỗ trợ tìm công thức chay");
        assertThat(medical.answer()).contains("bác sĩ");
        assertThat(unavailable.answer()).contains("chưa thể kiểm tra phạm vi").doesNotContain("bác sĩ");
        verify(ai, times(1)).planAssistantTools(anyString(), anyString());
    }

    private static AiChatRequest chatRequest(String question) {
        return new AiChatRequest(AiChatRequest.ContextType.GENERAL, null, question, List.of(),
                "a01f3d0d-ec2e-40da-8517-1b18479c7ef7", null, ExecutionMode.CONFIRM, null, null, null);
    }

    @Test
    void failedNavigationDoesNotReturnTheModelsSuccessClaim() {
        var users = mock(CurrentUserService.class);
        var ai = mock(AiClient.class);
        var settings = mock(AiChatSettingsService.class);
        var quota = mock(AiTokenQuotaService.class);
        var registry = mock(AiAssistantToolRegistry.class);
        when(users.findAuthenticatedUserId()).thenReturn(Optional.of(7L));
        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(7, Role.CUSTOMER));
        when(settings.resolve(7L, null)).thenReturn(null);
        when(ai.classifyChatScope(anyString(), anyString())).thenReturn(new AiClient.ChatResult(null,
                "{\"decisionId\":\"MX_CHAT_OK\",\"reasonId\":\"NONE\"}", 1, 1, 2));
        when(ai.planAssistantTools(anyString(), anyString())).thenReturn(new AiClient.ChatResult(null,
                "{\"answer\":\"Đã mở trang chủ.\",\"toolCalls\":[{\"name\":\"navigate_to_page\",\"arguments\":{\"page\":\"UNKNOWN\"}}]}", 1, 1, 2));
        when(registry.definitionsFor("CUSTOMER")).thenReturn(List.of(new AiAssistantToolRegistry.ToolDefinition(
                "navigate_to_page", "Navigate", "{page:string}", false, false, java.util.Set.of())));
        when(registry.execute("navigate_to_page", Map.of("page", "UNKNOWN")))
                .thenThrow(new AppException(ErrorCode.AI_RESPONSE_INVALID));
        var service = new AiAssistantExecutionService(users, ai, settings, quota,
                new AiAssistantSessionStore(Clock.systemUTC(), java.time.Duration.ofMinutes(30)), registry,
                new ApprovedAiKnowledgeRetriever(), new JsonMapper(), mock(ApplicationEventPublisher.class), Clock.systemUTC());

        var response = service.execute(new AiChatRequest(AiChatRequest.ContextType.GENERAL, null,
                "Mở trang chủ", List.of(), null, null, ExecutionMode.CONFIRM, null, null, null));

        assertThat(response.steps()).singleElement().extracting("status").isEqualTo("FAILED");
        assertThat(response.answer()).contains("chưa hoàn tất").doesNotContain("Đã mở");
        assertThat(response.workflowUrl()).isNull();
    }

    @Test
    void personalQuestionRequiresConsentBeforeAnyProviderCall() {
        var users = mock(CurrentUserService.class);
        var ai = mock(AiClient.class);
        var settings = mock(AiChatSettingsService.class);
        var quota = mock(AiTokenQuotaService.class);
        when(users.findAuthenticatedUserId()).thenReturn(Optional.of(7L));
        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(7, Role.CUSTOMER));
        var service = new AiAssistantExecutionService(users, ai, settings, quota,
                new AiAssistantSessionStore(Clock.systemUTC(), java.time.Duration.ofMinutes(30)),
                mock(AiAssistantToolRegistry.class), new ApprovedAiKnowledgeRetriever(), new JsonMapper(),
                mock(ApplicationEventPublisher.class), Clock.systemUTC());

        var response = service.execute(new AiChatRequest(AiChatRequest.ContextType.GENERAL, null,
                "Thông tin sức khỏe của tôi?", List.of(), null, null, ExecutionMode.CONFIRM, null, null, null));

        assertThat(response.consentRequest().scopes()).hasSize(1);
        assertThat(response.consentRequest().scopes().getFirst().category()).isEqualTo(AiChatRequest.ConsentCategory.HEALTH_PROFILE);
        verifyNoInteractions(ai, settings, quota);
    }

    @Test
    void confirmModeStagesWritesThenExecutesOnlyAfterApproval() {
        var users = mock(CurrentUserService.class);
        var ai = mock(AiClient.class);
        var settings = mock(AiChatSettingsService.class);
        var quota = mock(AiTokenQuotaService.class);
        var registry = mock(AiAssistantToolRegistry.class);
        var events = mock(ApplicationEventPublisher.class);
        when(users.findAuthenticatedUserId()).thenReturn(Optional.of(7L));
        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(7, Role.CUSTOMER));
        when(settings.resolve(7L, null)).thenReturn(null);
        when(ai.classifyChatScope(anyString(), anyString())).thenReturn(new AiClient.ChatResult(null,
                "{\"decisionId\":\"MX_CHAT_OK\",\"reasonId\":\"NONE\"}", 1, 1, 2));
        when(ai.planAssistantTools(anyString(), anyString())).thenReturn(new AiClient.ChatResult(null,
                "{\"answer\":\"Tôi sẽ tạo danh sách.\",\"toolCalls\":[{\"name\":\"create_shopping_list\",\"arguments\":{\"name\":\"Rau tuần này\"}}]}", 1, 1, 2));
        when(registry.definitionsFor("CUSTOMER")).thenReturn(List.of(new AiAssistantToolRegistry.ToolDefinition(
                "create_shopping_list", "Create a list", "{name:string}", true, false, java.util.Set.of())));
        when(registry.execute(eq("create_shopping_list"), anyMap())).thenReturn(new AiAssistantToolRegistry.ToolResult(
                "SUCCEEDED", "Đã tạo danh sách.", Map.of("id", 2), List.<Source>of()));
        var service = new AiAssistantExecutionService(users, ai, settings, quota,
                new AiAssistantSessionStore(Clock.systemUTC(), java.time.Duration.ofMinutes(30)), registry,
                new ApprovedAiKnowledgeRetriever(), new JsonMapper(), events, Clock.systemUTC());

        var staged = service.execute(new AiChatRequest(AiChatRequest.ContextType.GENERAL, null,
                "Tạo danh sách mua sắm", List.of(), null, null, ExecutionMode.CONFIRM, null, null, null));

        assertThat(staged.pendingConfirmation()).isNotNull();
        assertThat(staged.steps()).singleElement().extracting("status").isEqualTo("AWAITING_CONFIRMATION");
        verify(registry, never()).execute(anyString(), anyMap());
        var approved = service.execute(new AiChatRequest(AiChatRequest.ContextType.GENERAL, null,
                "Xác nhận", List.of(), null, staged.sessionId(), ExecutionMode.CONFIRM, null,
                staged.pendingConfirmation().confirmationId(), AiChatRequest.ConfirmationDecision.APPROVE));
        assertThat(approved.steps()).singleElement().extracting("status").isEqualTo("SUCCEEDED");
        verify(registry, times(1)).execute(eq("create_shopping_list"), anyMap());
    }
}

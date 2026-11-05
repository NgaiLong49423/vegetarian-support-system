package tech.mamxanh.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.concurrent.atomic.AtomicReference;

import com.google.adk.models.BaseLlm;
import com.google.adk.models.BaseLlmConnection;
import com.google.adk.models.LlmRequest;
import com.google.adk.models.LlmResponse;
import com.google.genai.types.Content;
import com.google.genai.types.FunctionCall;
import com.google.genai.types.GenerateContentResponseUsageMetadata;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.ai.AiClient;
import tech.mamxanh.integration.ai.AiPromptLoader;
import tech.mamxanh.recipe.dto.request.AiChatRequest;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ConfirmationDecision;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ConsentCategory;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ConsentPurpose;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ConsentScope;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ContextType;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ExecutionMode;
import tech.mamxanh.recipe.dto.response.AiChatResponse;
import tech.mamxanh.subscription.service.AiChatSettingsService;
import tech.mamxanh.subscription.service.AiTokenQuotaService;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

class AdkAssistantEngineTest {
    private CurrentUserService users;
    private AiTokenQuotaService aiQuota;
    private AiChatSettingsService chatSettings;
    private AiAssistantSessionStore sessionStore;
    private AiAssistantToolRegistry toolRegistry;
    private ApprovedAiKnowledgeRetriever knowledge;
    private AiPromptLoader promptLoader;
    private AiClient aiClient;
    private ObjectMapper mapper;
    private ApplicationEventPublisher events;
    private Clock clock;
    private AtomicBoolean quotaAvailable;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-11-01T02:00:00Z"), ZoneOffset.UTC);
        users = mock(CurrentUserService.class);
        aiQuota = mock(AiTokenQuotaService.class);
        chatSettings = mock(AiChatSettingsService.class);
        sessionStore = new AiAssistantSessionStore(clock, Duration.ofMinutes(30));
        toolRegistry = mock(AiAssistantToolRegistry.class);
        knowledge = mock(ApprovedAiKnowledgeRetriever.class);
        promptLoader = mock(AiPromptLoader.class);
        aiClient = mock(AiClient.class);
        mapper = new ObjectMapper();
        events = mock(ApplicationEventPublisher.class);
        quotaAvailable = new AtomicBoolean(true);

        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(10L, Role.CUSTOMER));
        when(users.findAuthenticatedUserId()).thenReturn(Optional.of(10L));
        when(chatSettings.resolve(anyLong(), any())).thenReturn("gemini-3.8-flash");
        when(promptLoader.loadSystemPrompt()).thenReturn("System instruction for Mâm Xanh.");
        when(promptLoader.loadAgentInstructions()).thenReturn("Agent behavioral instruction.");
        when(promptLoader.loadClassifierPrompt()).thenReturn("Classifier prompt.");

        // Default classifier: OK
        when(aiClient.classifyChatScope(anyString(), anyString(), anyString(), any())).thenReturn(
                new AiClient.ChatResult("{}", "{\"decisionId\":\"MX_CHAT_OK\",\"reasonId\":\"NONE\"}", 10, 5, 15));
        when(aiClient.classifyChatScope(anyString(), anyString(), any(Duration.class))).thenReturn(
                new AiClient.ChatResult("{}", "{\"decisionId\":\"MX_CHAT_OK\",\"reasonId\":\"NONE\"}", 10, 5, 15));

        when(aiQuota.beginCall(anyLong(), any(), anyString(), anyString(), any())).thenAnswer(inv -> {
            if (!quotaAvailable.get()) throw new AppException(ErrorCode.AI_TOKEN_QUOTA_EXCEEDED);
            return new AiTokenQuotaService.CallStartSnapshot(10L, null, "FR-51", "FREE", 50_000,
                    LocalDateTime.of(2026, 11, 1, 0, 0), LocalDateTime.of(2026, 12, 1, 0, 0), "gemini-3.8-flash", 100, Instant.now());
        });
    }

    private AdkAssistantEngine createEngine(BaseLlm llm) {
        return new AdkAssistantEngine(
                users, aiQuota, chatSettings, sessionStore, toolRegistry,
                knowledge, promptLoader, aiClient, mapper, events, clock,
                modelId -> llm, null);
    }

    @Test
    void pureConversationalQueryReturnsGroundedAnswerWithoutToolCalls() {
        when(knowledge.retrieve(anyString())).thenReturn(List.of(
                new AiChatResponse.Source("DOMAIN_KNOWLEDGE", "unrelated", "Hướng dẫn khác", "", null)));
        ScriptedLlm llm = new ScriptedLlm(request -> textResponse("Chào bạn! Hôm nay bạn muốn nấu món chay gì?"));
        var engine = createEngine(llm);

        AiChatRequest request = new AiChatRequest(ContextType.GENERAL, null, "Chào bạn, hôm nay có món gì ngon?",
                List.of(), null, null, ExecutionMode.CONFIRM, null, null, null, UUID.randomUUID());

        AiChatResponse response = engine.execute(request);

        assertThat(response).isNotNull();
        assertThat(response.answer()).isEqualTo("Chào bạn! Hôm nay bạn muốn nấu món chay gì?");
        assertThat(response.runStatus()).isEqualTo("COMPLETED");
        assertThat(response.steps()).isEmpty();
        assertThat(response.sources()).isEmpty();
        assertThat(llm.requests).hasSize(1);
        var classifierBudget = ArgumentCaptor.forClass(Duration.class);
        verify(aiClient).classifyChatScope(anyString(), anyString(), anyString(), classifierBudget.capture());
        assertThat(classifierBudget.getValue().toNanos()).isBetween(1L, Duration.ofSeconds(10).toNanos());
    }

    @Test
    void wrappedModelTimeoutIsReportedAsTimeoutInsteadOfGenericFailure() {
        ScriptedLlm llm = new ScriptedLlm(request -> {
            throw new RuntimeException(new java.util.concurrent.TimeoutException("The source did not signal an event for 10000 milliseconds"));
        });
        var engine = createEngine(llm);
        AiChatRequest request = new AiChatRequest(ContextType.GENERAL, null,
                "Giải thích cách thay trứng trong bánh thuần chay", List.of(), null, null,
                ExecutionMode.CONFIRM, null, null, null, UUID.randomUUID());

        AiChatResponse response = engine.execute(request);

        assertThat(response.runStatus()).isEqualTo("TIMEOUT");
        assertThat(response.answer()).contains("vượt thời gian chờ");
        assertThat(response.answer()).doesNotContain("30 giây");
        assertThat(response.steps()).isEmpty();
    }

    @Test
    void twoWayAgenticLoopInvokesToolAndGroundsResponseOnRealBackendResult() {
        when(toolRegistry.definitionsFor("CUSTOMER")).thenReturn(List.of(
                new AiAssistantToolRegistry.ToolDefinition("search_public_recipes", "Tìm công thức công khai", "{}", false, false, Set.of())));

        when(toolRegistry.execute(anyString(), any())).thenReturn(
                new AiAssistantToolRegistry.ToolResult("SUCCEEDED", "Tìm thấy 2 công thức",
                        Map.of("recipes", List.of(Map.of("id", 101, "title", "Đậu hũ sốt cà"))),
                        List.of(new AiChatResponse.Source("MAMXANH_RECIPE", "101", "Đậu hũ sốt cà", "Món ngon thanh đạm", null))));

        ScriptedLlm llm = new ScriptedLlm(request -> {
            List<String> fnResponses = functionResponseNames(request);
            if (fnResponses.isEmpty()) {
                return functionCall("search_public_recipes", "call-search-1", Map.of("query", "đậu hũ"));
            }
            return textResponse("Mâm Xanh có món Đậu hũ sốt cà (#101) rất ngon và dễ làm!");
        });

        var engine = createEngine(llm);

        AiChatRequest request = new AiChatRequest(ContextType.GENERAL, null, "Tìm giúp mình món đậu hũ",
                List.of(), null, null, ExecutionMode.AUTO, null, null, null, UUID.randomUUID());

        AiChatResponse response = engine.execute(request);

        assertThat(response).isNotNull();
        assertThat(response.runStatus()).isEqualTo("COMPLETED");
        assertThat(response.answer()).contains("Đậu hũ sốt cà (#101)");
        assertThat(response.steps()).hasSize(1);
        assertThat(response.steps().getFirst().tool()).isEqualTo("search_public_recipes");
        assertThat(response.steps().getFirst().status()).isEqualTo("SUCCEEDED");
        assertThat(response.sources()).hasSize(1);
        assertThat(response.sources().getFirst().title()).isEqualTo("Đậu hũ sốt cà");

        assertThat(llm.requests).hasSize(2);
    }

    @Test
    void adkNavigationResponseCarriesTheValidatedRouteAndDoesNotRepeatGeminisPrematureSuccessClaim() {
        when(toolRegistry.definitionsFor("CUSTOMER")).thenReturn(List.of(
                new AiAssistantToolRegistry.ToolDefinition("navigate_to_page", "Điều hướng", "{}", false, false, Set.of())));
        when(toolRegistry.execute(eq("navigate_to_page"), any())).thenReturn(
                new AiAssistantToolRegistry.ToolResult("SUCCEEDED", "Đang mở trang EXPLORE.",
                        Map.of("workflowUrl", "/kham-pha"), List.of()));
        ScriptedLlm llm = new ScriptedLlm(request -> functionResponseNames(request).isEmpty()
                ? functionCall("navigate_to_page", "call-navigation-1", Map.of("page", "EXPLORE"))
                : textResponse("Mình đã mở trang Khám phá cho bạn rồi nhé!"));
        var engine = createEngine(llm);

        AiChatResponse response = engine.execute(new AiChatRequest(ContextType.GENERAL, null,
                "Mở trang Khám phá", List.of(), null, null, ExecutionMode.AGENT, null, null, null, UUID.randomUUID()));

        assertThat(response.workflowUrl()).isEqualTo("/kham-pha");
        assertThat(response.answer()).isEqualTo("Đang mở trang bạn yêu cầu.");
        assertThat(response.steps()).singleElement().satisfies(step -> {
            assertThat(step.tool()).isEqualTo("navigate_to_page");
            assertThat(step.status()).isEqualTo("SUCCEEDED");
        });
    }

    @Test
    void adkCannotClaimItNavigatedWhenNoNavigationToolRan() {
        ScriptedLlm llm = new ScriptedLlm(request -> textResponse("Mình đã mở trang Khám phá cho bạn rồi nhé!"));
        var engine = createEngine(llm);

        AiChatResponse response = engine.execute(new AiChatRequest(ContextType.GENERAL, null,
                "Trang Khám phá có gì?", List.of(), null, null, ExecutionMode.AGENT, null, null, null, UUID.randomUUID()));

        assertThat(response.workflowUrl()).isNull();
        assertThat(response.answer()).contains("chưa xác nhận được việc mở trang");
    }

    @Test
    void scopeClassificationRejectionReturnsRefusalWithoutCallingADK() {
        when(aiClient.classifyChatScope(anyString(), anyString(), anyString(), any())).thenReturn(
                new AiClient.ChatResult("{}", "{\"decisionId\":\"MX_CHAT_OFF_TOPIC\",\"reasonId\":\"NONE\"}", 10, 5, 15));

        ScriptedLlm llm = new ScriptedLlm(request -> textResponse("Should not be called"));
        var engine = createEngine(llm);

        AiChatRequest request = new AiChatRequest(ContextType.GENERAL, null, "Giá cổ phiếu hôm nay thế nào?",
                List.of(), null, null, ExecutionMode.CONFIRM, null, null, null, UUID.randomUUID());

        AiChatResponse response = engine.execute(request);

        assertThat(response.answer()).isEqualTo("Mình chỉ hỗ trợ ẩm thực chay, công thức, nấu ăn và các tính năng Mâm Xanh.");
        assertThat(response.runStatus()).isEqualTo("COMPLETED");
        assertThat(llm.requests).isEmpty();
    }

    @Test
    void personalDataDetectionWithoutConsentYieldsAwaitingConsent() {
        ScriptedLlm llm = new ScriptedLlm(request -> textResponse("Should not be called"));
        var engine = createEngine(llm);

        AiChatRequest request = new AiChatRequest(ContextType.GENERAL, null, "Xem chỉ số BMI và hồ sơ sức khỏe của tôi",
                List.of(), null, null, ExecutionMode.CONFIRM, null, null, null, UUID.randomUUID());

        AiChatResponse response = engine.execute(request);

        assertThat(response.runStatus()).isEqualTo("AWAITING_CONSENT");
        assertThat(response.consentRequest()).isNotNull();
        assertThat(response.consentRequest().scopes()).contains(new ConsentScope(ConsentCategory.HEALTH_PROFILE, ConsentPurpose.AI_CHAT));
        assertThat(llm.requests).isEmpty();
    }

    @Test
    void toolConsentRequirementIsReturnedAsAUserConsentRequest() {
        when(toolRegistry.definitionsFor("CUSTOMER")).thenReturn(List.of(
                new AiAssistantToolRegistry.ToolDefinition("create_shopping_list", "Tạo danh sách mua sắm", "{}",
                        true, false, Set.of(ConsentCategory.SHOPPING_LIST), AiAssistantToolRegistry.RiskTier.LOW_RISK_WRITE)));
        ScriptedLlm llm = new ScriptedLlm(request -> functionResponseNames(request).isEmpty()
                ? functionCall("create_shopping_list", "call-create-consent", Map.of("name", "Đi chợ cuối tuần"))
                : textResponse("Tôi cần sự đồng ý để tiếp tục."));

        AiChatRequest initialRequest = new AiChatRequest(ContextType.GENERAL, null,
                "Tạo danh sách mua sắm tên Đi chợ cuối tuần", List.of(), null, null,
                ExecutionMode.AGENT, null, null, null, UUID.randomUUID());
        AdkAssistantEngine engine = createEngine(llm);
        AiChatResponse response = engine.execute(initialRequest);

        assertThat(response.runStatus()).isEqualTo("AWAITING_CONSENT");
        assertThat(response.consentRequest()).isNotNull();
        assertThat(response.consentRequest().scopes()).containsExactly(
                new ConsentScope(ConsentCategory.SHOPPING_LIST, ConsentPurpose.SHOPPING_LIST_WRITE));
        assertThat(response.pendingConfirmation()).isNull();
        verify(toolRegistry, never()).execute(eq("create_shopping_list"), any());

        ScriptedLlm consentedLlm = new ScriptedLlm(request -> functionResponseNames(request).isEmpty()
                ? functionCall("create_shopping_list", "call-create-after-consent", Map.of("name", "Đi chợ cuối tuần"))
                : textResponse("Danh sách đã sẵn sàng để xác nhận."));
        AiChatRequest consentedRequest = new AiChatRequest(ContextType.GENERAL, null,
                initialRequest.question(), List.of(), null, response.sessionId(), ExecutionMode.AGENT,
                new AiChatRequest.ConsentDecision(AiChatRequest.ConsentAction.GRANT,
                        List.of(new ConsentScope(ConsentCategory.SHOPPING_LIST, ConsentPurpose.SHOPPING_LIST_WRITE))),
                null, null, UUID.randomUUID());
        AiChatResponse awaitingConfirmation = createEngine(consentedLlm).execute(consentedRequest);

        assertThat(awaitingConfirmation.runStatus()).isEqualTo("AWAITING_CONFIRMATION");
        assertThat(awaitingConfirmation.pendingConfirmation()).isNotNull();
        assertThat(awaitingConfirmation.pendingConfirmation().proposedListName()).isEqualTo("Đi chợ cuối tuần");
        verify(toolRegistry, never()).execute(eq("create_shopping_list"), any());
    }

    @Test
    void confirmModeStagesWriteToolAndResumesCleanlyUponApproval() {
        when(toolRegistry.definitionsFor("CUSTOMER")).thenReturn(List.of(
                new AiAssistantToolRegistry.ToolDefinition("create_shopping_list", "Tạo danh sách mua sắm", "{}", true, false, Set.of())));

        when(toolRegistry.execute(anyString(), any())).thenReturn(
                new AiAssistantToolRegistry.ToolResult("SUCCEEDED", "Đã tạo danh sách mua sắm", Map.of("id", 201), List.of()));

        ScriptedLlm llm = new ScriptedLlm(request -> {
            List<String> fnResponses = functionResponseNames(request);
            if (fnResponses.isEmpty()) {
                return functionCall("create_shopping_list", "call-create-1", Map.of("name", "Đi chợ nấu tiệc"));
            }
            if (isFunctionResponseStatus(request, "SUCCEEDED")) {
                return textResponse("Đã tạo thành công danh sách mua sắm Đi chợ nấu tiệc (#201) cho bạn!");
            }
            return textResponse("Vui lòng xác nhận tạo danh sách Đi chợ nấu tiệc.");
        });

        var engine = createEngine(llm);

        AiChatRequest req1 = new AiChatRequest(ContextType.GENERAL, null, "Tạo danh sách mua sắm Đi chợ nấu tiệc",
                List.of(), null, null, ExecutionMode.CONFIRM, null, null, null, UUID.randomUUID());

        AiChatResponse staged = engine.execute(req1);

        assertThat(staged.runStatus()).isEqualTo("AWAITING_CONFIRMATION");
        assertThat(staged.pendingConfirmation()).isNotNull();
        UUID confId = staged.pendingConfirmation().confirmationId();

        // Round 2 on the SAME engine: User APPROVES confirmation -> resumes, executes tool, grounds response
        AiChatRequest req2 = new AiChatRequest(ContextType.GENERAL, null, "Xác nhận tạo",
                List.of(), null, staged.sessionId(), ExecutionMode.CONFIRM,
                null, confId, ConfirmationDecision.APPROVE, UUID.randomUUID());

        AiChatResponse completed = engine.execute(req2);

        assertThat(completed.runStatus()).isEqualTo("COMPLETED");
        assertThat(completed.steps()).hasSize(1);
        assertThat(completed.steps().getFirst().tool()).isEqualTo("create_shopping_list");
        assertThat(completed.steps().getFirst().status()).isEqualTo("SUCCEEDED");
    }

    @Test
    void confirmModeRejectionCancelsWithoutToolExecution() {
        when(toolRegistry.definitionsFor("CUSTOMER")).thenReturn(List.of(
                new AiAssistantToolRegistry.ToolDefinition("create_shopping_list", "Tạo danh sách mua sắm", "{}", true, false, Set.of())));

        ScriptedLlm llm = new ScriptedLlm(request -> {
            List<String> fnResponses = functionResponseNames(request);
            if (fnResponses.isEmpty()) {
                return functionCall("create_shopping_list", "call-1", Map.of("name", "Test"));
            }
            return textResponse("Vui lòng xác nhận thao tác.");
        });

        var engine = createEngine(llm);

        AiChatRequest req1 = new AiChatRequest(ContextType.GENERAL, null, "Tạo danh sách Test",
                List.of(), null, null, ExecutionMode.CONFIRM, null, null, null, UUID.randomUUID());

        AiChatResponse staged = engine.execute(req1);
        assertThat(staged.pendingConfirmation()).isNotNull();

        AiChatRequest req2 = new AiChatRequest(ContextType.GENERAL, null, "Hủy thao tác",
                List.of(), null, staged.sessionId(), ExecutionMode.CONFIRM,
                null, staged.pendingConfirmation().confirmationId(), ConfirmationDecision.REJECT, UUID.randomUUID());

        AiChatResponse cancelled = engine.execute(req2);

        assertThat(cancelled.runStatus()).isEqualTo("COMPLETED");
        assertThat(cancelled.answer()).isEqualTo("Đã hủy thao tác theo yêu cầu của bạn.");
        assertThat(cancelled.steps()).isEmpty();
    }

    @Test
    void autoModeWithHighRiskWriteToolStagesConfirmationAndBlocksDirectExecution() {
        when(toolRegistry.definitionsFor("CUSTOMER")).thenReturn(List.of(
                new AiAssistantToolRegistry.ToolDefinition("delete_shopping_list", "Xóa danh sách mua sắm", "{}", true, true, Set.of(ConsentCategory.SHOPPING_LIST), AiAssistantToolRegistry.RiskTier.HIGH_RISK_WRITE)));

        ScriptedLlm llm = new ScriptedLlm(request -> {
            List<String> fnResponses = functionResponseNames(request);
            if (fnResponses.isEmpty()) {
                return functionCall("delete_shopping_list", "call-del-1", Map.of("listId", 999));
            }
            return textResponse("Vui lòng xác nhận xóa danh sách mua sắm.");
        });

        var engine = createEngine(llm);

        var consentDecision = new AiChatRequest.ConsentDecision(AiChatRequest.ConsentAction.GRANT,
                List.of(new ConsentScope(ConsentCategory.SHOPPING_LIST, ConsentPurpose.SHOPPING_LIST_WRITE),
                        new ConsentScope(ConsentCategory.SHOPPING_LIST, ConsentPurpose.AI_CHAT)));

        AiChatRequest request = new AiChatRequest(ContextType.GENERAL, null, "Xóa danh sách mua sắm #999 giúp mình",
                List.of(), null, null, ExecutionMode.AUTO, consentDecision, null, null, UUID.randomUUID());

        AiChatResponse response = engine.execute(request);

        assertThat(response.runStatus()).isEqualTo("AWAITING_CONFIRMATION");
        assertThat(response.pendingConfirmation()).isNotNull();
        assertThat(response.pendingConfirmation().proposedListName()).isNull();
        assertThat(response.steps()).hasSize(1);
        assertThat(response.steps().getFirst().status()).isEqualTo("AWAITING_CONFIRMATION");

        verify(toolRegistry, never()).execute(eq("delete_shopping_list"), any());
    }

    @Test
    void allEightHighRiskWriteToolsRequireConfirmationInAutoMode() {
        List<String> highRiskTools = List.of(
                "update_dietary_preferences",
                "update_health_profile",
                "apply_weekly_menu",
                "delete_shopping_item",
                "delete_shopping_list",
                "create_recipe_post",
                "update_own_recipe_post",
                "delete_own_recipe_post"
        );

        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(10L, Role.EXPERT));

        for (String toolName : highRiskTools) {
            when(toolRegistry.definitionsFor(anyString())).thenReturn(List.of(
                    new AiAssistantToolRegistry.ToolDefinition(toolName, "Thao tác " + toolName, "{}", true, true, Set.of(), AiAssistantToolRegistry.RiskTier.HIGH_RISK_WRITE)));

            ScriptedLlm llm = new ScriptedLlm(request -> {
                List<String> fnResponses = functionResponseNames(request);
                if (fnResponses.isEmpty()) {
                    return functionCall(toolName, "call-1", Map.of("id", 1));
                }
                return textResponse("Vui lòng xác nhận.");
            });

            var engine = createEngine(llm);

            AiChatRequest req = new AiChatRequest(ContextType.GENERAL, null, "Thực hiện " + toolName,
                    List.of(), null, null, ExecutionMode.AUTO, null, null, null, UUID.randomUUID());

            AiChatResponse resp = engine.execute(req);

            assertThat(resp.runStatus()).as("Tool %s must require confirmation in AUTO mode", toolName).isEqualTo("AWAITING_CONFIRMATION");
            assertThat(resp.pendingConfirmation()).as("Tool %s must have pendingConfirmation in AUTO mode", toolName).isNotNull();
            verify(toolRegistry, never()).execute(eq(toolName), any());
        }
    }

    @Test
    void autoModeWithLowRiskWriteToolExecutesDirectlyWithoutConfirmation() {
        when(toolRegistry.definitionsFor("CUSTOMER")).thenReturn(List.of(
                new AiAssistantToolRegistry.ToolDefinition("add_meal_plan_slot", "Thêm món vào kế hoạch", "{}", true, false, Set.of(), AiAssistantToolRegistry.RiskTier.LOW_RISK_WRITE)));

        when(toolRegistry.execute(eq("add_meal_plan_slot"), any())).thenReturn(
                new AiAssistantToolRegistry.ToolResult("SUCCEEDED", "Đã thêm món vào bữa sáng", Map.of("slotId", 501), List.of()));

        ScriptedLlm llm = new ScriptedLlm(request -> {
            List<String> fnResponses = functionResponseNames(request);
            if (fnResponses.isEmpty()) {
                return functionCall("add_meal_plan_slot", "call-slot-1", Map.of("recipeId", 101, "mealType", "BREAKFAST"));
            }
            return textResponse("Đã thêm món vào bữa sáng thành công!");
        });

        var engine = createEngine(llm);

        AiChatRequest request = new AiChatRequest(ContextType.GENERAL, null, "Thêm món 101 vào bữa sáng giúp mình",
                List.of(), null, null, ExecutionMode.AUTO, null, null, null, UUID.randomUUID());

        AiChatResponse response = engine.execute(request);

        assertThat(response.runStatus()).isEqualTo("COMPLETED");
        assertThat(response.pendingConfirmation()).isNull();
        assertThat(response.steps()).hasSize(1);
        assertThat(response.steps().getFirst().tool()).isEqualTo("add_meal_plan_slot");
        assertThat(response.steps().getFirst().status()).isEqualTo("SUCCEEDED");
        verify(toolRegistry, times(1)).execute(eq("add_meal_plan_slot"), any());
    }

    @Test
    void qaModeSkipsWriteTools() {
        when(toolRegistry.definitionsFor("CUSTOMER")).thenReturn(List.of(
                new AiAssistantToolRegistry.ToolDefinition("add_meal_plan_slot", "Thêm món vào kế hoạch", "{}", true, false, Set.of(), AiAssistantToolRegistry.RiskTier.LOW_RISK_WRITE)));

        ScriptedLlm llm = new ScriptedLlm(request -> {
            List<String> fnResponses = functionResponseNames(request);
            if (fnResponses.isEmpty()) {
                return functionCall("add_meal_plan_slot", "call-slot-1", Map.of("recipeId", 101));
            }
            return textResponse("Chế độ QA không ghi dữ liệu.");
        });

        var engine = createEngine(llm);

        AiChatRequest request = new AiChatRequest(ContextType.GENERAL, null, "Thêm món 101 vào kế hoạch",
                List.of(), null, null, ExecutionMode.QA, null, null, null, UUID.randomUUID());

        AiChatResponse response = engine.execute(request);

        assertThat(response.runStatus()).isEqualTo("COMPLETED");
        assertThat(response.steps()).hasSize(1);
        assertThat(response.steps().getFirst().status()).isEqualTo("SKIPPED_MODE");
        verify(toolRegistry, never()).execute(eq("add_meal_plan_slot"), any());
    }

    @Test
    void concurrentApprovalRequestsOnlyExecuteOnce() {
        when(toolRegistry.definitionsFor("CUSTOMER")).thenReturn(List.of(
                new AiAssistantToolRegistry.ToolDefinition("delete_shopping_list", "Xóa danh sách mua sắm", "{}", true, true, Set.of(), AiAssistantToolRegistry.RiskTier.HIGH_RISK_WRITE)));

        when(toolRegistry.execute(eq("delete_shopping_list"), any())).thenReturn(
                new AiAssistantToolRegistry.ToolResult("SUCCEEDED", "Đã xóa danh sách mua sắm", Map.of("deleted", true), List.of()));

        ScriptedLlm llm = new ScriptedLlm(request -> {
            List<String> fnResponses = functionResponseNames(request);
            if (fnResponses.isEmpty()) {
                return functionCall("delete_shopping_list", "call-del-1", Map.of("listId", 10));
            }
            return textResponse("Đã xóa xong!");
        });

        var engine = createEngine(llm);

        AiChatRequest req1 = new AiChatRequest(ContextType.GENERAL, null, "Xóa danh sách 10",
                List.of(), null, null, ExecutionMode.CONFIRM, null, null, null, UUID.randomUUID());
        AiChatResponse staged = engine.execute(req1);
        UUID confId = staged.pendingConfirmation().confirmationId();
        UUID sessId = staged.sessionId();

        AiChatRequest req2 = new AiChatRequest(ContextType.GENERAL, null, "Xác nhận",
                List.of(), null, sessId, ExecutionMode.CONFIRM, null, confId, ConfirmationDecision.APPROVE, UUID.randomUUID());
        AiChatResponse resp2 = engine.execute(req2);
        assertThat(resp2.runStatus()).isEqualTo("COMPLETED");

        AiChatRequest req3 = new AiChatRequest(ContextType.GENERAL, null, "Xác nhận lại",
                List.of(), null, sessId, ExecutionMode.CONFIRM, null, confId, ConfirmationDecision.APPROVE, UUID.randomUUID());
        assertThatThrownBy(() -> engine.execute(req3))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Yêu cầu xác nhận không hợp lệ hoặc đã hết hạn");

        verify(toolRegistry, times(1)).execute(eq("delete_shopping_list"), any());
    }

    @Test
    void expiredConfirmationThrowsValidationFailed() {
        when(toolRegistry.definitionsFor("CUSTOMER")).thenReturn(List.of(
                new AiAssistantToolRegistry.ToolDefinition("delete_shopping_list", "Xóa danh sách", "{}", true, true, Set.of(), AiAssistantToolRegistry.RiskTier.HIGH_RISK_WRITE)));

        ScriptedLlm llm = new ScriptedLlm(request -> {
            List<String> fnResponses = functionResponseNames(request);
            if (fnResponses.isEmpty()) {
                return functionCall("delete_shopping_list", "call-del-1", Map.of("listId", 10));
            }
            return textResponse("Vui lòng xác nhận.");
        });

        Instant t0 = Instant.parse("2026-11-01T02:00:00Z");
        AtomicReference<Instant> currentInstant = new AtomicReference<>(t0);
        Clock mutableClock = new Clock() {
            @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return currentInstant.get(); }
        };

        var customSessionStore = new AiAssistantSessionStore(mutableClock, Duration.ofMinutes(30));
        var engine = new AdkAssistantEngine(
                users, aiQuota, chatSettings, customSessionStore, toolRegistry,
                knowledge, promptLoader, aiClient, mapper, events, mutableClock,
                modelId -> llm, null);

        AiChatRequest req1 = new AiChatRequest(ContextType.GENERAL, null, "Xóa danh sách 10",
                List.of(), null, null, ExecutionMode.CONFIRM, null, null, null, UUID.randomUUID());
        AiChatResponse staged = engine.execute(req1);
        UUID confId = staged.pendingConfirmation().confirmationId();

        currentInstant.set(t0.plus(Duration.ofMinutes(6)));

        AiChatRequest req2 = new AiChatRequest(ContextType.GENERAL, null, "Xác nhận sau 6 phút",
                List.of(), null, staged.sessionId(), ExecutionMode.CONFIRM, null, confId, ConfirmationDecision.APPROVE, UUID.randomUUID());

        assertThatThrownBy(() -> engine.execute(req2))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Yêu cầu xác nhận không hợp lệ hoặc đã hết hạn");

        verify(toolRegistry, never()).execute(eq("delete_shopping_list"), any());
    }

    @Test
    void confirmedToolFailureMarksPendingFailedAndThrowsAppException() {
        when(toolRegistry.definitionsFor("CUSTOMER")).thenReturn(List.of(
                new AiAssistantToolRegistry.ToolDefinition("delete_shopping_list", "Xóa danh sách", "{}", true, true, Set.of(), AiAssistantToolRegistry.RiskTier.HIGH_RISK_WRITE)));

        when(toolRegistry.execute(eq("delete_shopping_list"), any())).thenThrow(
                new AppException(ErrorCode.NOT_FOUND, "Danh sách mua sắm không tồn tại"));

        ScriptedLlm llm = new ScriptedLlm(request -> {
            List<String> fnResponses = functionResponseNames(request);
            if (fnResponses.isEmpty()) {
                return functionCall("delete_shopping_list", "call-del-1", Map.of("listId", 999));
            }
            return textResponse("Vui lòng xác nhận.");
        });

        var engine = createEngine(llm);

        AiChatRequest req1 = new AiChatRequest(ContextType.GENERAL, null, "Xóa danh sách #999",
                List.of(), null, null, ExecutionMode.CONFIRM, null, null, null, UUID.randomUUID());
        AiChatResponse staged = engine.execute(req1);
        UUID confId = staged.pendingConfirmation().confirmationId();

        AiChatRequest req2 = new AiChatRequest(ContextType.GENERAL, null, "Xác nhận xóa",
                List.of(), null, staged.sessionId(), ExecutionMode.CONFIRM, null, confId, ConfirmationDecision.APPROVE, UUID.randomUUID());

        assertThatThrownBy(() -> engine.execute(req2))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Danh sách mua sắm không tồn tại");
    }

    @Test
    void approvalResumeBlocksChainedWritesToPreventInfiniteLoop() {
        when(toolRegistry.definitionsFor("CUSTOMER")).thenReturn(List.of(
                new AiAssistantToolRegistry.ToolDefinition("delete_shopping_list", "Xóa danh sách", "{}", true, true, Set.of(), AiAssistantToolRegistry.RiskTier.HIGH_RISK_WRITE),
                new AiAssistantToolRegistry.ToolDefinition("add_meal_plan_slot", "Thêm món", "{}", true, false, Set.of(), AiAssistantToolRegistry.RiskTier.LOW_RISK_WRITE)));

        when(toolRegistry.execute(eq("delete_shopping_list"), any())).thenReturn(
                new AiAssistantToolRegistry.ToolResult("SUCCEEDED", "Đã xóa danh sách", Map.of("deleted", true), List.of()));

        ScriptedLlm llm = new ScriptedLlm(request -> {
            List<String> fnResponses = functionResponseNames(request);
            if (fnResponses.isEmpty()) {
                return functionCall("delete_shopping_list", "call-del-1", Map.of("listId", 10));
            }
            if (isFunctionResponseStatus(request, "SUCCEEDED")) {
                return functionCall("add_meal_plan_slot", "call-chained-1", Map.of("recipeId", 101));
            }
            return textResponse("Vui lòng xác nhận xóa danh sách.");
        });

        var engine = createEngine(llm);

        AiChatRequest req1 = new AiChatRequest(ContextType.GENERAL, null, "Xóa danh sách 10",
                List.of(), null, null, ExecutionMode.CONFIRM, null, null, null, UUID.randomUUID());
        AiChatResponse staged = engine.execute(req1);
        UUID confId = staged.pendingConfirmation().confirmationId();

        AiChatRequest req2 = new AiChatRequest(ContextType.GENERAL, null, "Xác nhận",
                List.of(), null, staged.sessionId(), ExecutionMode.CONFIRM, null, confId, ConfirmationDecision.APPROVE, UUID.randomUUID());

        AiChatResponse resumed = engine.execute(req2);

        assertThat(resumed.runStatus()).isEqualTo("COMPLETED");
        verify(toolRegistry, never()).execute(eq("add_meal_plan_slot"), any());
        verify(toolRegistry, times(1)).execute(eq("delete_shopping_list"), any());
    }

    @Test
    void chatbotModeZeroToolsAnswersDirectlyAndInjectsKnowledgeSources() {
        ScriptedLlm llm = new ScriptedLlm(request -> {
            assertThat(request.tools()).isEmpty();
            return textResponse("Món đậu hũ sốt cà chua rất giàu đạm thực vật.");
        });

        var engine = createEngine(llm);

        AiChatRequest req = new AiChatRequest(ContextType.GENERAL, null, "Món chay nào giàu đạm?",
                List.of(), null, null, ExecutionMode.CHATBOT, null, null, null, UUID.randomUUID());
        AiChatResponse resp = engine.execute(req);

        assertThat(resp.runStatus()).isEqualTo("COMPLETED");
        assertThat(resp.answer()).contains("đậu hũ");
        verify(toolRegistry, never()).execute(any(), any());
    }

    @Test
    void modeSwitchingFromAgentToChatbotDiscardsPendingConfirmation() {
        when(toolRegistry.definitionsFor("CUSTOMER")).thenReturn(List.of(
                new AiAssistantToolRegistry.ToolDefinition("delete_shopping_list", "Xóa danh sách", "{}", true, true, Set.of(), AiAssistantToolRegistry.RiskTier.HIGH_RISK_WRITE)));

        ScriptedLlm llm = new ScriptedLlm(request -> {
            List<String> fnResponses = functionResponseNames(request);
            if (fnResponses.isEmpty() && !request.tools().isEmpty()) {
                return functionCall("delete_shopping_list", "call-del-1", Map.of("listId", 5));
            }
            return textResponse("Món chay giàu đạm gồm đậu nành, nấm và các loại hạt.");
        });

        var engine = createEngine(llm);

        // Turn 1 in AGENT mode stages confirmation
        AiChatRequest req1 = new AiChatRequest(ContextType.GENERAL, null, "Xóa danh sách số 5",
                List.of(), null, null, ExecutionMode.AGENT, null, null, null, UUID.randomUUID());
        AiChatResponse resp1 = engine.execute(req1);

        assertThat(resp1.runStatus()).isEqualTo("AWAITING_CONFIRMATION");
        assertThat(resp1.pendingConfirmation()).isNotNull();

        // Turn 2: User switches mode to CHATBOT and asks a general question
        AiChatRequest req2 = new AiChatRequest(ContextType.GENERAL, null, "Món chay nào giàu đạm?",
                List.of(), null, resp1.sessionId(), ExecutionMode.CHATBOT, null, null, null, UUID.randomUUID());
        AiChatResponse resp2 = engine.execute(req2);

        assertThat(resp2.runStatus()).isEqualTo("COMPLETED");
        assertThat(resp2.pendingConfirmation()).isNull();
        assertThat(sessionStore.inspect(resp1.sessionId(), "user:10").pending().status())
                .isEqualTo(AiAssistantSessionStore.ConfirmationStatus.CANCELLED);
    }

    @Test
    void newAgentQuestionDoesNotDiscardPendingConfirmation() {
        when(toolRegistry.definitionsFor("CUSTOMER")).thenReturn(List.of(
                new AiAssistantToolRegistry.ToolDefinition("delete_shopping_list", "Xóa danh sách", "{}", true, true, Set.of(), AiAssistantToolRegistry.RiskTier.HIGH_RISK_WRITE)));
        ScriptedLlm llm = new ScriptedLlm(request -> {
            if (functionResponseNames(request).isEmpty() && !request.tools().isEmpty()) {
                return functionCall("delete_shopping_list", "call-del-1", Map.of("listId", 5));
            }
            return textResponse("Đang chờ xác nhận.");
        });
        var engine = createEngine(llm);

        AiChatResponse staged = engine.execute(new AiChatRequest(ContextType.GENERAL, null, "Xóa danh sách số 5",
                List.of(), null, null, ExecutionMode.AGENT, null, null, null, UUID.randomUUID()));
        assertThat(staged.runStatus()).isEqualTo("AWAITING_CONFIRMATION");

        AiChatRequest nextQuestion = new AiChatRequest(ContextType.GENERAL, null, "Gợi ý món khác",
                List.of(), null, staged.sessionId(), ExecutionMode.AGENT, null, null, null, UUID.randomUUID());
        assertThatThrownBy(() -> engine.execute(nextQuestion))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Hãy xác nhận, từ chối hoặc chuyển sang CHATBOT");
        assertThat(sessionStore.inspect(staged.sessionId(), "user:10").pending().status())
                .isEqualTo(AiAssistantSessionStore.ConfirmationStatus.PENDING);
    }

    @Test
    void cancellationBeforeStreamStartsSkipsClassifierAndAgentRun() {
        AtomicBoolean providerCalled = new AtomicBoolean();
        ScriptedLlm llm = new ScriptedLlm(request -> {
            providerCalled.set(true);
            return textResponse("Không được gọi sau cancellation.");
        });
        var engine = createEngine(llm);
        AiChatCancellationSignal cancellation = new AiChatCancellationSignal();
        cancellation.cancel();
        List<tech.mamxanh.recipe.dto.response.AiChatStreamEvent> events = new ArrayList<>();
        AiChatRequest request = new AiChatRequest(ContextType.GENERAL, null, "Món chay giàu đạm?",
                List.of(), null, null, ExecutionMode.AGENT, null, null, null, UUID.randomUUID());

        engine.stream(request, events::add, cancellation);

        assertThat(providerCalled).isFalse();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).type()).isEqualTo("complete");
        assertThat(((AiChatResponse) events.get(0).data()).runStatus()).isEqualTo("STOPPED");
    }

    @Test
    void cancellationByClientRequestIdAfterFirstToolStepPreventsTheNextToolStep() {
        when(toolRegistry.definitionsFor("CUSTOMER")).thenReturn(List.of(
                new AiAssistantToolRegistry.ToolDefinition("get_shopping_list", "Đọc danh sách", "{}", false, false, Set.of(), AiAssistantToolRegistry.RiskTier.READ_ONLY)));
        when(toolRegistry.execute(eq("get_shopping_list"), any()))
                .thenReturn(new AiAssistantToolRegistry.ToolResult("SUCCEEDED", "Đã đọc danh sách", Map.of(), List.of()));
        ScriptedLlm llm = new ScriptedLlm(request -> {
            int functionResponses = functionResponseNames(request).size();
            return functionCall("get_shopping_list", "call-read-" + functionResponses, Map.of());
        });
        var engine = createEngine(llm);
        UUID clientRequestId = UUID.randomUUID();
        AiChatRequest request = new AiChatRequest(ContextType.GENERAL, null, "Đọc danh sách của tôi",
                List.of(), null, null, ExecutionMode.AGENT, null, null, null, clientRequestId);
        AtomicBoolean cancelSent = new AtomicBoolean();
        List<tech.mamxanh.recipe.dto.response.AiChatStreamEvent> events = new ArrayList<>();

        engine.stream(request, event -> {
            events.add(event);
            if ("step".equals(event.type()) && cancelSent.compareAndSet(false, true)) {
                AiChatRequest cancel = new AiChatRequest(ContextType.GENERAL, null, "Dừng",
                        List.of(), null, null, ExecutionMode.AGENT, null, null, null, clientRequestId,
                        AiChatRequest.ControlAction.CANCEL);
                engine.cancel(cancel);
            }
        });

        verify(toolRegistry, times(1)).execute(eq("get_shopping_list"), any());
        var complete = events.stream().filter(event -> "complete".equals(event.type())).findFirst().orElseThrow();
        assertThat(((AiChatResponse) complete.data()).runStatus()).isEqualTo("STOPPED");
    }


    @Test
    void streamingEmitsDeltaAndCompleteEvents() {
        ScriptedLlm llm = new ScriptedLlm(request -> textResponse("Đậu hũ và nấm là nguồn đạm thực vật phổ biến."));
        var engine = createEngine(llm);

        List<tech.mamxanh.recipe.dto.response.AiChatStreamEvent> collected = new ArrayList<>();
        AiChatRequest req = new AiChatRequest(ContextType.GENERAL, null, "Món chay nào giàu đạm?",
                List.of(), null, null, ExecutionMode.CHATBOT, null, null, null, UUID.randomUUID());

        engine.stream(req, collected::add);

        assertThat(collected).isNotEmpty();
        boolean hasDelta = collected.stream().anyMatch(e -> "delta".equals(e.type()));
        boolean hasComplete = collected.stream().anyMatch(e -> "complete".equals(e.type()));
        assertThat(hasDelta).isTrue();
        assertThat(hasComplete).isTrue();

        var completeEvent = collected.stream()
                .filter(e -> "complete".equals(e.type()))
                .findFirst()
                .orElseThrow();
        AiChatResponse response = (AiChatResponse) completeEvent.data();
        assertThat(response.runStatus()).isEqualTo("COMPLETED");
        assertThat(response.answer()).contains("Đậu hũ");
    }

    @Test
    void cancellationReturnsStoppedAndDiscardsPending() {
        when(toolRegistry.definitionsFor("CUSTOMER")).thenReturn(List.of(
                new AiAssistantToolRegistry.ToolDefinition("delete_shopping_list", "Xóa danh sách", "{}", true, true, Set.of(), AiAssistantToolRegistry.RiskTier.HIGH_RISK_WRITE)));

        ScriptedLlm llm = new ScriptedLlm(request -> {
            List<String> fnResponses = functionResponseNames(request);
            if (fnResponses.isEmpty() && !request.tools().isEmpty()) {
                return functionCall("delete_shopping_list", "call-del-1", Map.of("listId", 7));
            }
            return textResponse("Vui lòng xác nhận xóa danh sách.");
        });
        var engine = createEngine(llm);

        AiChatRequest req1 = new AiChatRequest(ContextType.GENERAL, null, "Xóa danh sách 7",
                List.of(), null, null, ExecutionMode.AGENT, null, null, null, UUID.randomUUID());
        AiChatResponse staged = engine.execute(req1);

        assertThat(staged.runStatus()).isEqualTo("AWAITING_CONFIRMATION");

        AiChatRequest cancelReq = new AiChatRequest(ContextType.GENERAL, null, "Dừng",
                List.of(), null, staged.sessionId(), ExecutionMode.AGENT, null, null, null, UUID.randomUUID(), AiChatRequest.ControlAction.CANCEL);
        AiChatResponse stopped = engine.cancel(cancelReq);

        assertThat(stopped.runStatus()).isEqualTo("STOPPED");
        assertThat(stopped.answer()).contains("Đã dừng lượt chạy");
        assertThat(sessionStore.inspect(staged.sessionId(), "user:10").pending().status())
                .isEqualTo(AiAssistantSessionStore.ConfirmationStatus.CANCELLED);
    }

    // Helper classes for test scripting
    static final class ScriptedLlm extends BaseLlm {
        private final Function<LlmRequest, LlmResponse> handler;
        final List<LlmRequest> requests = new ArrayList<>();

        ScriptedLlm(Function<LlmRequest, LlmResponse> handler) {
            super("gemini-3.8-flash");
            this.handler = handler;
        }

        @Override
        public Flowable<LlmResponse> generateContent(LlmRequest request, boolean stream) {
            requests.add(request);
            return Flowable.just(handler.apply(request));
        }

        @Override
        public BaseLlmConnection connect(LlmRequest request) {
            throw new UnsupportedOperationException();
        }
    }

    static LlmResponse functionCall(String name, String callId, Map<String, Object> args) {
        return LlmResponse.builder()
                .content(Content.fromParts(Part.builder()
                        .functionCall(FunctionCall.builder()
                                .name(name)
                                .id(callId)
                                .args(args)
                                .build())
                        .build()))
                .usageMetadata(GenerateContentResponseUsageMetadata.builder()
                        .promptTokenCount(100)
                        .candidatesTokenCount(50)
                        .totalTokenCount(150)
                        .build())
                .build();
    }

    static LlmResponse textResponse(String text) {
        return LlmResponse.builder()
                .content(Content.fromParts(Part.fromText(text)))
                .usageMetadata(GenerateContentResponseUsageMetadata.builder()
                        .promptTokenCount(100)
                        .candidatesTokenCount(50)
                        .totalTokenCount(150)
                        .build())
                .build();
    }

    static boolean isFunctionResponseStatus(LlmRequest request, String expectedStatus) {
        for (Content c : request.contents()) {
            if (c.parts().isPresent()) {
                for (Part p : c.parts().get()) {
                    if (p.functionResponse().isPresent()) {
                        var responseMap = p.functionResponse().get().response();
                        if (responseMap.isPresent() && responseMap.get() instanceof Map<?, ?> m) {
                            if (expectedStatus.equals(m.get("status"))) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    static List<String> functionResponseNames(LlmRequest request) {
        List<String> names = new ArrayList<>();
        request.contents().forEach(c -> c.parts().ifPresent(parts -> parts.forEach(p -> {
            p.functionResponse().ifPresent(fr -> names.add(fr.name().orElse("")));
        })));
        return names;
    }
}

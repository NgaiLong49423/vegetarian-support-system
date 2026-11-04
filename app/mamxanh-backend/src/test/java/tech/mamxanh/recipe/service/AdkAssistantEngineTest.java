package tech.mamxanh.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        when(aiClient.classifyChatScope(anyString(), anyString(), anyString())).thenReturn(
                new AiClient.ChatResult("{}", "{\"decisionId\":\"MX_CHAT_OK\",\"reasonId\":\"NONE\"}", 10, 5, 15));
        when(aiClient.classifyChatScope(anyString(), anyString())).thenReturn(
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
    void scopeClassificationRejectionReturnsRefusalWithoutCallingADK() {
        when(aiClient.classifyChatScope(anyString(), anyString(), anyString())).thenReturn(
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

    @Test
    void deterministicRagRetrievesExpertRegistrationDocumentAndGroundsResponse() {
        when(toolRegistry.definitionsFor("CUSTOMER")).thenReturn(List.of(
                new AiAssistantToolRegistry.ToolDefinition("get_business_knowledge", "Lấy tài liệu tri thức nghiệp vụ", "{}", false, false, Set.of())));

        when(toolRegistry.execute(eq("get_business_knowledge"), any())).thenReturn(
                new AiAssistantToolRegistry.ToolResult("SUCCEEDED", "Đã đọc tài liệu đăng ký chuyên gia dinh dưỡng",
                        Map.of("id", "expert-registration", "title", "Hướng dẫn đăng ký Chuyên gia dinh dưỡng"),
                        List.of(new AiChatResponse.Source("DOMAIN_KNOWLEDGE", "expert-registration",
                                "Hướng dẫn đăng ký Chuyên gia dinh dưỡng",
                                "Để đăng ký làm chuyên gia dinh dưỡng trên Mâm Xanh, bạn cần nộp hồ sơ bằng cấp và chứng chỉ chuyên môn.", "1.0.0"))));

        ScriptedLlm llm = new ScriptedLlm(request -> {
            List<String> fnResponses = functionResponseNames(request);
            if (fnResponses.isEmpty()) {
                return functionCall("get_business_knowledge", "call-rag-expert-1", Map.of("knowledgeId", "expert-registration"));
            }
            return textResponse("Để đăng ký làm chuyên gia dinh dưỡng trên Mâm Xanh, bạn cần nộp hồ sơ chuyên môn và chứng chỉ hành nghề để ban quản trị phê duyệt.");
        });

        var engine = createEngine(llm);

        AiChatRequest request = new AiChatRequest(ContextType.GENERAL, null,
                "Làm thế nào để đăng ký làm chuyên gia dinh dưỡng trên Mâm Xanh?",
                List.of(), null, null, ExecutionMode.AUTO, null, null, null, UUID.randomUUID());

        AiChatResponse response = engine.execute(request);

        assertThat(response).isNotNull();
        assertThat(response.runStatus()).isEqualTo("COMPLETED");
        assertThat(response.answer()).contains("chuyên gia dinh dưỡng");
        assertThat(response.steps()).hasSize(1);
        assertThat(response.steps().getFirst().tool()).isEqualTo("get_business_knowledge");
        assertThat(response.steps().getFirst().status()).isEqualTo("SUCCEEDED");
        assertThat(response.sources()).hasSize(1);
        assertThat(response.sources().getFirst().id()).isEqualTo("expert-registration");
        assertThat(response.sources().getFirst().title()).isEqualTo("Hướng dẫn đăng ký Chuyên gia dinh dưỡng");
        assertThat(llm.requests).hasSize(2);
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

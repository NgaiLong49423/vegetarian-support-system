package tech.mamxanh.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.ai.AiClient;
import tech.mamxanh.integration.ai.AiPromptLoader;
import tech.mamxanh.recipe.dto.request.AiChatRequest;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ConfirmationDecision;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ContextType;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ExecutionMode;
import tech.mamxanh.recipe.dto.response.AiChatResponse;
import tech.mamxanh.subscription.service.AiChatSettingsService;
import tech.mamxanh.subscription.service.AiTokenQuotaService;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * AI Assistant Security Guard Integration Test on Microsoft SQL Server:
 * Verifies high-risk action safety, human-in-the-loop confirmation lifecycle,
 * concurrent execution protection, idempotency, and non-regression of read/low-risk tools
 * against real database tables, constraints, transactions, and multi-threading.
 */
class AiAssistantSecurityGuardIntegrationTest extends AbstractIntegrationTest {

    @Autowired private JdbcTemplate sql;
    @Autowired private AiAssistantExecutionJournal journal;
    @Autowired private AiAssistantToolRegistry toolRegistry;
    @Autowired private AiAssistantSessionStore sessionStore;
    @Autowired private AdkDomainTools domainTools;
    @Autowired private ApprovedAiKnowledgeRetriever knowledge;
    @Autowired private AiPromptLoader promptLoader;
    @Autowired private CurrentUserService users;
    @Autowired private AiTokenQuotaService aiQuota;
    @Autowired private AiChatSettingsService chatSettings;
    @Autowired private ApplicationEventPublisher events;
    @Autowired private ObjectMapper mapper;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private AiClient aiClient;

    private long testUser;
    private long testListId;
    private int activeUnitId;
    private AdkAssistantEngine engine;
    private ScriptedLlm scriptedLlm;

    @BeforeEach
    void setupFixtures() {
        cleanupFixtures();
        testUser = sql.queryForObject(
                "INSERT INTO [USER] (email, display_name, role, account_status, email_verified) " +
                        "OUTPUT INSERTED.user_id VALUES (?, N'Security Guard Test User', 'CUSTOMER', 'ACTIVE', 1)",
                Long.class, "security-guard-" + UUID.randomUUID() + "@test.local");

        testListId = sql.queryForObject(
                "INSERT INTO [SHOPPING_LIST] (user_id, name) OUTPUT INSERTED.shopping_list_id VALUES (?, N'Danh sách đi chợ kiểm thử')",
                Long.class, testUser);

        activeUnitId = sql.queryForObject(
                "SELECT TOP 1 unit_id FROM [UNIT] WHERE is_active = 1", Integer.class);

        when(aiClient.classifyChatScope(anyString(), anyString())).thenReturn(
                new AiClient.ChatResult(null, "{\"decisionId\":\"MX_CHAT_OK\",\"reasonId\":\"NONE\"}", 100, 10, 110));
        when(aiClient.classifyChatScope(anyString(), anyString(), anyString())).thenReturn(
                new AiClient.ChatResult(null, "{\"decisionId\":\"MX_CHAT_OK\",\"reasonId\":\"NONE\"}", 100, 10, 110));
        when(aiClient.classifyChatScope(any(), any(), any(Duration.class))).thenReturn(
                new AiClient.ChatResult(null, "{\"decisionId\":\"MX_CHAT_OK\",\"reasonId\":\"NONE\"}", 100, 10, 110));
        when(aiClient.classifyChatScope(any(), any(), any(), any(Duration.class))).thenReturn(
                new AiClient.ChatResult(null, "{\"decisionId\":\"MX_CHAT_OK\",\"reasonId\":\"NONE\"}", 100, 10, 110));

        engine = new AdkAssistantEngine(
                users, aiQuota, chatSettings, sessionStore, toolRegistry, domainTools, knowledge,
                promptLoader, aiClient, mapper, events, clock,
                modelId -> scriptedLlm, new ExecutionTruthfulnessVerifier(), journal, "test-api-key"
        );
    }

    @AfterEach
    void cleanupFixtures() {
        try {
            sql.update("DELETE FROM AI_ASSISTANT_ACTION_JOURNAL WHERE owner_id IN (SELECT user_id FROM [USER] WHERE email LIKE 'security-guard-%@test.local')");
            sql.update("DELETE FROM AI_TOKEN_USAGE_LEDGER WHERE user_id IN (SELECT user_id FROM [USER] WHERE email LIKE 'security-guard-%@test.local')");
            sql.update("DELETE FROM AI_USAGE_TELEMETRY WHERE user_id IN (SELECT user_id FROM [USER] WHERE email LIKE 'security-guard-%@test.local')");
            sql.update("DELETE FROM AI_CREDIT_LEDGER WHERE user_id IN (SELECT user_id FROM [USER] WHERE email LIKE 'security-guard-%@test.local')");
            sql.update("DELETE FROM AI_CREDIT_TOP_UP_ORDER WHERE user_id IN (SELECT user_id FROM [USER] WHERE email LIKE 'security-guard-%@test.local')");
            sql.update("DELETE FROM AI_CREDIT_WALLET WHERE user_id IN (SELECT user_id FROM [USER] WHERE email LIKE 'security-guard-%@test.local')");
            sql.update("DELETE FROM USER_PREFERRED_AI_MODEL WHERE user_id IN (SELECT user_id FROM [USER] WHERE email LIKE 'security-guard-%@test.local')");
            sql.update("DELETE FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id IN (SELECT shopping_list_id FROM [SHOPPING_LIST] WHERE user_id IN (SELECT user_id FROM [USER] WHERE email LIKE 'security-guard-%@test.local'))");
            sql.update("DELETE FROM [SHOPPING_LIST] WHERE user_id IN (SELECT user_id FROM [USER] WHERE email LIKE 'security-guard-%@test.local')");
            sql.update("DELETE FROM [USER] WHERE email LIKE 'security-guard-%@test.local'");
        } catch (Exception ignored) {
        }
    }

    private AiChatRequest.ConsentDecision consent() {
        return new AiChatRequest.ConsentDecision(
                AiChatRequest.ConsentAction.GRANT,
                List.of(
                        new AiChatRequest.ConsentScope(AiChatRequest.ConsentCategory.SHOPPING_LIST, AiChatRequest.ConsentPurpose.SHOPPING_LIST_WRITE),
                        new AiChatRequest.ConsentScope(AiChatRequest.ConsentCategory.SHOPPING_LIST, AiChatRequest.ConsentPurpose.AI_CHAT)
                )
        );
    }

    private void authenticate() {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(
                        String.valueOf(testUser), null,
                        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_CUSTOMER"))
                )
        );
    }

    @Test
    @DisplayName("Thao tác rủi ro cao trong chế độ AUTO phải tạm dừng tạo xác nhận và bảo toàn dữ liệu đích")
    void autoModeWithHighRiskWriteToolStagesConfirmationAndPreservesTargetData() {
        authenticate();
        scriptedLlm = new ScriptedLlm(req -> {
            if (isFunctionResponseStatus(req, "AWAITING_CONFIRMATION")) {
                return textResponse("Đang đợi bạn xác nhận thao tác xóa danh sách.");
            }
            return functionCall("delete_shopping_list", "call-del-1", Map.of("listId", testListId));
        });

        AiChatRequest request = new AiChatRequest(
                ContextType.GENERAL, null, "Xóa danh sách đi chợ giúp mình",
                List.of(), null, null, ExecutionMode.AUTO, consent(), null, null, UUID.randomUUID()
        );

        AiChatResponse response = engine.execute(request);

        assertThat(response.runStatus()).isEqualTo("AWAITING_CONFIRMATION");
        assertThat(response.pendingConfirmation()).isNotNull();
        assertThat(response.pendingConfirmation().confirmationId()).isNotNull();
        assertThat(response.steps()).isNotEmpty();
        assertThat(response.steps().get(0).tool()).isEqualTo("delete_shopping_list");
        assertThat(response.steps().get(0).status()).isEqualTo("AWAITING_CONFIRMATION");

        int countBeforeApproval = sql.queryForObject(
                "SELECT COUNT(*) FROM [SHOPPING_LIST] WHERE shopping_list_id = ?",
                Integer.class, testListId);
        assertThat(countBeforeApproval).isEqualTo(1);
    }

    @Test
    @DisplayName("Xác nhận hợp lệ chỉ thực thi một lần duy nhất và từ chối xác nhận lặp lại")
    void validApprovalExecutesActionOnceAndRejectsRepeatedApproval() {
        authenticate();
        scriptedLlm = new ScriptedLlm(req -> {
            if (isFunctionResponseStatus(req, "AWAITING_CONFIRMATION")) {
                return textResponse("Cần xác nhận.");
            }
            if (isFunctionResponseStatus(req, "SUCCEEDED")) {
                return textResponse("Đã xóa danh sách thành công.");
            }
            return functionCall("delete_shopping_list", "call-del-2", Map.of("listId", testListId));
        });

        AiChatRequest stageReq = new AiChatRequest(
                ContextType.GENERAL, null, "Xóa danh sách",
                List.of(), null, null, ExecutionMode.AUTO, consent(), null, null, UUID.randomUUID()
        );
        AiChatResponse staged = engine.execute(stageReq);
        UUID confId = staged.pendingConfirmation().confirmationId();

        AiChatRequest approveReq1 = new AiChatRequest(
                ContextType.GENERAL, null, "Đồng ý",
                List.of(), null, staged.sessionId(), ExecutionMode.AUTO, null, confId, ConfirmationDecision.APPROVE, UUID.randomUUID()
        );
        AiChatResponse approvedResponse = engine.execute(approveReq1);

        assertThat(approvedResponse.runStatus()).isEqualTo("COMPLETED");

        int countAfterApprove = sql.queryForObject(
                "SELECT COUNT(*) FROM [SHOPPING_LIST] WHERE shopping_list_id = ?",
                Integer.class, testListId);
        assertThat(countAfterApprove).isZero();

        AiChatRequest approveReq2 = new AiChatRequest(
                ContextType.GENERAL, null, "Đồng ý lại",
                List.of(), null, staged.sessionId(), ExecutionMode.AUTO, null, confId, ConfirmationDecision.APPROVE, UUID.randomUUID()
        );
        assertThatThrownBy(() -> engine.execute(approveReq2))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("Từ chối thao tác hủy bỏ kế hoạch và bảo toàn dữ liệu trên cơ sở dữ liệu")
    void rejectionCancelsPlanAndPreservesTargetData() {
        authenticate();
        scriptedLlm = new ScriptedLlm(req -> {
            if (isFunctionResponseStatus(req, "AWAITING_CONFIRMATION")) return textResponse("Cần xác nhận.");
            return functionCall("delete_shopping_list", "call-del-3", Map.of("listId", testListId));
        });

        AiChatRequest stageReq = new AiChatRequest(
                ContextType.GENERAL, null, "Xóa danh sách",
                List.of(), null, null, ExecutionMode.CONFIRM, consent(), null, null, UUID.randomUUID()
        );
        AiChatResponse staged = engine.execute(stageReq);
        UUID confId = staged.pendingConfirmation().confirmationId();

        AiChatRequest rejectReq = new AiChatRequest(
                ContextType.GENERAL, null, "Hủy",
                List.of(), null, staged.sessionId(), ExecutionMode.CONFIRM, null, confId, ConfirmationDecision.REJECT, UUID.randomUUID()
        );
        AiChatResponse rejected = engine.execute(rejectReq);

        assertThat(rejected.runStatus()).isEqualTo("COMPLETED");
        assertThat(rejected.answer()).contains("Đã hủy");

        int count = sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_LIST] WHERE shopping_list_id = ?", Integer.class, testListId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Yêu cầu xác nhận đã quá hạn thời gian sống sẽ bị từ chối")
    void expiredConfirmationRejectsApproval() {
        authenticate();
        scriptedLlm = new ScriptedLlm(req -> {
            if (isFunctionResponseStatus(req, "AWAITING_CONFIRMATION")) return textResponse("Cần xác nhận.");
            return functionCall("delete_shopping_list", "call-del-4", Map.of("listId", testListId));
        });

        AiChatRequest stageReq = new AiChatRequest(
                ContextType.GENERAL, null, "Xóa danh sách",
                List.of(), null, null, ExecutionMode.CONFIRM, consent(), null, null, UUID.randomUUID()
        );
        AiChatResponse staged = engine.execute(stageReq);
        UUID confId = staged.pendingConfirmation().confirmationId();

        clock.advance(Duration.ofMinutes(6));

        AiChatRequest expiredReq = new AiChatRequest(
                ContextType.GENERAL, null, "Đồng ý",
                List.of(), null, staged.sessionId(), ExecutionMode.CONFIRM, null, confId, ConfirmationDecision.APPROVE, UUID.randomUUID()
        );

        assertThatThrownBy(() -> engine.execute(expiredReq))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);

        int count = sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_LIST] WHERE shopping_list_id = ?", Integer.class, testListId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Nhiều yêu cầu xác nhận đồng thời trên cơ sở dữ liệu chỉ cho phép đúng một luồng thực thi")
    void concurrentApprovalsExecuteActionOnlyOnce() throws Exception {
        authenticate();
        scriptedLlm = new ScriptedLlm(req -> {
            if (isFunctionResponseStatus(req, "AWAITING_CONFIRMATION")) return textResponse("Chờ");
            if (isFunctionResponseStatus(req, "SUCCEEDED")) return textResponse("Xong");
            return functionCall("delete_shopping_list", "call-del-5", Map.of("listId", testListId));
        });

        AiChatRequest stageReq = new AiChatRequest(
                ContextType.GENERAL, null, "Xóa danh sách",
                List.of(), null, null, ExecutionMode.AUTO, consent(), null, null, UUID.randomUUID()
        );
        AiChatResponse staged = engine.execute(stageReq);
        UUID confId = staged.pendingConfirmation().confirmationId();

        int threads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    authenticate();
                    AiChatRequest approveReq = new AiChatRequest(
                            ContextType.GENERAL, null, "Xác nhận",
                            List.of(), null, staged.sessionId(), ExecutionMode.AUTO, null, confId, ConfirmationDecision.APPROVE, UUID.randomUUID()
                    );
                    AiChatResponse res = engine.execute(approveReq);
                    if ("COMPLETED".equals(res.runStatus())) {
                        successCount.incrementAndGet();
                    }
                } catch (AppException appEx) {
                    if (appEx.errorCode() == ErrorCode.VALIDATION_FAILED) {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        endGate.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(threads - 1);

        int count = sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_LIST] WHERE shopping_list_id = ?", Integer.class, testListId);
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("Thực thi lại với cùng mã yêu cầu trả về kết quả đã lưu mà không lặp lại thao tác nghiệp vụ")
    void idempotentReplayReturnsCachedResultWithoutReInvokingAction() {
        UUID clientReqId = UUID.randomUUID();
        String actionId = "action-create-list";
        String toolName = "create_shopping_list";
        String argsHash = "hash-12345";
        AtomicInteger invocationCount = new AtomicInteger(0);

        Object result1 = journal.executeWithJournal(testUser, clientReqId, actionId, toolName, argsHash, () -> {
            invocationCount.incrementAndGet();
            return Map.of("createdListId", 9999L);
        });

        assertThat(invocationCount.get()).isEqualTo(1);
        assertThat(result1).isEqualTo(Map.of("createdListId", 9999L));

        String stateInSql = sql.queryForObject(
                "SELECT state FROM AI_ASSISTANT_ACTION_JOURNAL WHERE owner_id = ? AND client_request_id = ? AND action_id = ?",
                String.class, testUser, clientReqId, actionId);
        assertThat(stateInSql).isEqualTo("SUCCEEDED");

        Object result2 = journal.executeWithJournal(testUser, clientReqId, actionId, toolName, argsHash, () -> {
            invocationCount.incrementAndGet();
            return Map.of("createdListId", 9999L);
        });

        assertThat(invocationCount.get()).isEqualTo(1);
        assertThat(result2).isEqualTo(Map.of("createdListId", 9999L));
    }

    @Test
    @DisplayName("Cùng mã yêu cầu nhưng sai lệch băm tham số bị từ chối với lỗi hợp lệ hóa")
    void mismatchedArgumentsHashRejectsExecution() {
        UUID clientReqId = UUID.randomUUID();
        String actionId = "action-del";
        String toolName = "delete_shopping_list";

        journal.recordReservation(testUser, clientReqId, actionId, toolName, "original-hash-aaa");

        assertThatThrownBy(() -> journal.recordReservation(testUser, clientReqId, actionId, toolName, "mismatched-hash-bbb"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("Khi dịch vụ nghiệp vụ ném ngoại lệ, nhật ký lưu trạng thái thất bại và không bị treo")
    void domainActionFailureMarksFailedInJournalAndStore() {
        UUID clientReqId = UUID.randomUUID();
        String actionId = "action-fail";
        String toolName = "delete_shopping_list";

        assertThatThrownBy(() -> journal.executeWithJournal(testUser, clientReqId, actionId, toolName, "hash-fail", () -> {
            throw new RuntimeException("Simulated domain DB failure");
        })).isInstanceOf(RuntimeException.class).hasMessageContaining("Simulated domain DB failure");

        String stateInSql = sql.queryForObject(
                "SELECT state FROM AI_ASSISTANT_ACTION_JOURNAL WHERE owner_id = ? AND client_request_id = ? AND action_id = ?",
                String.class, testUser, clientReqId, actionId);
        assertThat(stateInSql).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("Công cụ đọc và ghi ít rủi ro thực thi trực tiếp mà không bị chặn xác nhận")
    void readOnlyAndLowRiskWritesExecuteWithoutBlockingConfirmation() {
        authenticate();

        scriptedLlm = new ScriptedLlm(req -> {
            if (isFunctionResponseStatus(req, "SUCCEEDED")) return textResponse("Danh sách có 0 món.");
            return functionCall("get_shopping_list", "call-read-1", Map.of("listId", testListId));
        });
        AiChatRequest readReq = new AiChatRequest(
                ContextType.GENERAL, null, "Xem danh sách của mình",
                List.of(), null, null, ExecutionMode.AUTO, consent(), null, null, UUID.randomUUID()
        );
        AiChatResponse readRes = engine.execute(readReq);
        assertThat(readRes.runStatus()).isEqualTo("COMPLETED");
        assertThat(readRes.pendingConfirmation()).isNull();

        scriptedLlm = new ScriptedLlm(req -> {
            if (isFunctionResponseStatus(req, "SUCCEEDED")) return textResponse("Đã thêm món vào danh sách.");
            return functionCall("add_shopping_item", "call-add-1", Map.of(
                    "listId", testListId,
                    "name", "Nấm đùi gà",
                    "quantity", 2,
                    "unitId", activeUnitId
            ));
        });
        AiChatRequest writeReq = new AiChatRequest(
                ContextType.GENERAL, null, "Thêm nấm vào danh sách",
                List.of(), null, null, ExecutionMode.AUTO, consent(), null, null, UUID.randomUUID()
        );
        AiChatResponse writeRes = engine.execute(writeReq);
        assertThat(writeRes.runStatus()).isEqualTo("COMPLETED");
        assertThat(writeRes.pendingConfirmation()).isNull();

        int itemCount = sql.queryForObject(
                "SELECT COUNT(*) FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id = ?",
                Integer.class, testListId);
        assertThat(itemCount).isEqualTo(1);
    }

    static final class ScriptedLlm extends BaseLlm {
        private final Function<LlmRequest, LlmResponse> handler;
        ScriptedLlm(Function<LlmRequest, LlmResponse> handler) {
            super("gemini-3.8-flash");
            this.handler = handler;
        }
        @Override
        public Flowable<LlmResponse> generateContent(LlmRequest request, boolean stream) {
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
                        .functionCall(FunctionCall.builder().name(name).id(callId).args(args).build())
                        .build()))
                .usageMetadata(GenerateContentResponseUsageMetadata.builder()
                        .promptTokenCount(100).candidatesTokenCount(50).totalTokenCount(150).build())
                .build();
    }

    static LlmResponse textResponse(String text) {
        return LlmResponse.builder()
                .content(Content.fromParts(Part.fromText(text)))
                .usageMetadata(GenerateContentResponseUsageMetadata.builder()
                        .promptTokenCount(100).candidatesTokenCount(50).totalTokenCount(150).build())
                .build();
    }

    static boolean isFunctionResponseStatus(LlmRequest request, String expectedStatus) {
        for (Content c : request.contents()) {
            if (c.parts().isPresent()) {
                for (Part p : c.parts().get()) {
                    if (p.functionResponse().isPresent()) {
                        var responseMap = p.functionResponse().get().response();
                        if (responseMap.isPresent() && responseMap.get() instanceof Map<?, ?> m) {
                            if (expectedStatus.equals(m.get("status"))) return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}

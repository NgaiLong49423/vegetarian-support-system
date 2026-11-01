package tech.mamxanh.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ApplicationEventPublisher;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.ai.AiClient;
import tech.mamxanh.integration.ai.knowledge.KnowledgeCatalog;
import tech.mamxanh.nutrition.service.DietaryPreferenceService;
import tech.mamxanh.recipe.dto.request.AiChatRequest;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ConfirmationDecision;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ConsentAction;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ConsentDecision;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ContextType;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ExecutionMode;
import tech.mamxanh.shopping.dto.request.CreateEmptyShoppingListRequest;
import tech.mamxanh.shopping.dto.response.ShoppingListResponse;
import tech.mamxanh.shopping.service.ShoppingListService;
import tech.mamxanh.subscription.service.AiChatSettingsService;
import tech.mamxanh.subscription.service.AiTokenQuotaService;
import tech.mamxanh.subscription.service.SubscriptionEntitlementService;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class AdkShoppingPilotServiceTest {
    private MutableClock clock;
    private CurrentUserService users;
    private ShoppingListService shoppingLists;
    private AiTokenQuotaService aiQuota;
    private AiChatSettingsService chatSettings;
    private AiAssistantSessionStore sessionStore;
    private ObjectMapper mapper;
    private ApplicationEventPublisher events;
    private KnowledgeCatalog g3Catalog;
    private AtomicBoolean budgetAvailable;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-11-01T02:00:00Z"));
        users = mock(CurrentUserService.class);
        shoppingLists = mock(ShoppingListService.class);
        aiQuota = mock(AiTokenQuotaService.class);
        chatSettings = mock(AiChatSettingsService.class);
        sessionStore = new AiAssistantSessionStore(clock, Duration.ofMinutes(30));
        mapper = new ObjectMapper();
        events = mock(ApplicationEventPublisher.class);
        g3Catalog = KnowledgeCatalog.packagedG3();
        budgetAvailable = new AtomicBoolean(true);

        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(7L, Role.CUSTOMER));
        when(users.findAuthenticatedUserId()).thenReturn(Optional.of(7L));
        when(chatSettings.resolve(anyLong(), any())).thenReturn("gemini-3.8-flash");
        when(aiQuota.hasAvailableBudget(anyLong(), any())).thenAnswer(inv -> budgetAvailable.get());
        when(aiQuota.beginCall(anyLong(), any(), anyString(), anyString(), any())).thenAnswer(inv -> {
            if (!budgetAvailable.get()) throw new AppException(ErrorCode.AI_TOKEN_QUOTA_EXCEEDED);
            return new AiTokenQuotaService.CallStartSnapshot(users.requireActiveMember().id(), null, "FR-51", "FREE", 50_000,
                    LocalDateTime.of(2026,11,1,0,0),LocalDateTime.of(2026,12,1,0,0),inv.getArgument(3),400,inv.getArgument(4));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"CUSTOMER", "EXPERT"})
    void confirmModeRequestsConsentStagesNativeConfirmationCreatesEmptyListAndReadsBackWithRealBackendId(String roleName) {
        Role role = Role.valueOf(roleName);
        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(7L, role));
        when(shoppingLists.createEmpty(new CreateEmptyShoppingListRequest("Đi chợ cuối tuần")))
                .thenReturn(emptyList(901L, "Đi chợ cuối tuần"));
        when(shoppingLists.get(901L))
                .thenReturn(emptyList(901L, "Đi chợ cuối tuần"));

        ScriptedLlm llm = new ScriptedLlm(request -> {
            List<String> fnResponses = functionResponseNames(request);
            if (fnResponses.isEmpty()) {
                return functionCall("search_business_knowledge", "call-search-1", Map.of("query", "tạo danh sách mua sắm rỗng"));
            }
            if (fnResponses.size() == 1 && fnResponses.contains("search_business_knowledge")) {
                return functionCall("get_business_knowledge", "call-read-doc-1", Map.of("knowledgeId", "shopping-create"));
            }
            if (!fnResponses.contains("create_shopping_list")) {
                return functionCall("create_shopping_list", "call-create-1", Map.of("name", "Đi chợ cuối tuần"));
            }
            long returnedId = extractCreatedId(request);
            if (returnedId <= 0) {
                return textResponse("Vui lòng xác nhận tạo danh sách Đi chợ cuối tuần.");
            }
            if (!fnResponses.contains("get_shopping_list")) {
                return functionCall("get_shopping_list", "call-get-1", Map.of("listId", returnedId));
            }
            return textResponse("Đã tạo và đọc lại danh sách Đi chợ cuối tuần (#901).");
        });

        var service = createPilotService(llm);

        // 1. First request without Shopping List consent -> AWAITING_CONSENT, no model call
        var askConsent = service.execute(pilotRequest(
                null,
                UUID.randomUUID(),
                "Hãy tạo danh sách mua sắm tên là Đi chợ cuối tuần",
                ExecutionMode.CONFIRM,
                null,
                null,
                null));
        assertThat(askConsent.engine()).isEqualTo(AiAssistantSessionStore.ENGINE_G3_PILOT);
        assertThat(askConsent.runStatus()).isEqualTo("AWAITING_CONSENT");
        assertThat(askConsent.consentRequest()).isNotNull();
        assertThat(askConsent.consentRequest().scopes()).containsExactlyElementsOf(AdkShoppingPilotService.SHOPPING_CONSENT_SCOPES);
        assertThat(llm.requests).isEmpty();
        verify(shoppingLists, never()).createEmpty(any());

        // 2. Grant consent -> ADK searches/reads knowledge, calls create_shopping_list, pauses via native adk_request_confirmation
        var staged = service.execute(pilotRequest(
                askConsent.sessionId(),
                UUID.randomUUID(),
                "Hãy tạo danh sách mua sắm tên là Đi chợ cuối tuần",
                ExecutionMode.CONFIRM,
                grantShoppingConsent(),
                null,
                null));
        assertThat(staged.runStatus()).isEqualTo("AWAITING_CONFIRMATION");
        assertThat(staged.pendingConfirmation()).isNotNull();
        assertThat(staged.pendingConfirmation().proposedListName()).isEqualTo("Đi chợ cuối tuần");
        assertThat(staged.sources()).extracting(s -> s.id()).contains("shopping-create");
        assertThat(staged.knowledgeRevision()).isEqualTo("g3-pilot-2");
        verify(shoppingLists, never()).createEmpty(any());

        // 3. Approve confirmation -> ADK resumes create_shopping_list -> receives id=901 -> calls get_shopping_list(901) -> COMPLETED
        UUID approveReqId = UUID.randomUUID();
        var completed = service.execute(pilotRequest(
                askConsent.sessionId(),
                approveReqId,
                "Tiếp tục với thay đổi đã xem trước",
                ExecutionMode.CONFIRM,
                null,
                staged.pendingConfirmation().confirmationId(),
                ConfirmationDecision.APPROVE));
        assertThat(completed.runStatus()).isEqualTo("COMPLETED");
        assertThat(completed.answer()).contains("Đi chợ cuối tuần", "#901", "0 mục");
        assertThat(completed.steps()).extracting(s -> s.tool() + ":" + s.status())
                .containsExactly("create_shopping_list:SUCCEEDED", "get_shopping_list:SUCCEEDED");
        verify(shoppingLists, times(1)).createEmpty(new CreateEmptyShoppingListRequest("Đi chợ cuối tuần"));
        verify(shoppingLists, times(1)).get(901L);

        // 4. Duplicate clientRequestId returns cached response without re-running createEmpty
        var duplicate = service.execute(pilotRequest(
                askConsent.sessionId(),
                approveReqId,
                "Tiếp tục với thay đổi đã xem trước",
                ExecutionMode.CONFIRM,
                null,
                staged.pendingConfirmation().confirmationId(),
                ConfirmationDecision.APPROVE));
        assertThat(duplicate.runId()).isEqualTo(completed.runId());
        verify(shoppingLists, times(1)).createEmpty(any());

        // 5. Replaying the consumed confirmationId with a new clientRequestId is rejected
        assertThatThrownBy(() -> service.execute(pilotRequest(
                askConsent.sessionId(),
                UUID.randomUUID(),
                "Tiếp tục với thay đổi đã xem trước",
                ExecutionMode.CONFIRM,
                null,
                staged.pendingConfirmation().confirmationId(),
                ConfirmationDecision.APPROVE)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);
        verify(shoppingLists, times(1)).createEmpty(any());
    }

    @Test
    void qaModeOnlyPreviewsAndAutoModeExecutesCreateThenReadWhenNameAndGuardsAreValid() {
        AtomicLong seq = new AtomicLong(500L);
        when(shoppingLists.createEmpty(any())).thenAnswer(inv -> {
            CreateEmptyShoppingListRequest req = inv.getArgument(0);
            return emptyList(seq.incrementAndGet(), req.name());
        });
        when(shoppingLists.get(anyLong())).thenAnswer(inv ->
                emptyList(inv.getArgument(0), "Bữa tối thứ Sáu"));

        AtomicInteger callSeq = new AtomicInteger();
        ScriptedLlm llm = new ScriptedLlm(request -> {
            List<String> fnResponses = functionResponseNames(request);
            if (!fnResponses.contains("create_shopping_list")) {
                return functionCall("create_shopping_list", "c-" + callSeq.incrementAndGet(), Map.of("name", "Bữa tối thứ Sáu"));
            }
            long id = extractCreatedId(request);
            if (id > 0 && !fnResponses.contains("get_shopping_list")) {
                return functionCall("get_shopping_list", "g-" + callSeq.incrementAndGet(), Map.of("listId", id));
            }
            return textResponse("Đã xử lý xong.");
        });

        var service = createPilotService(llm);

        // QA mode: preview only, no write
        var qaResponse = service.execute(pilotRequest(
                null,
                UUID.randomUUID(),
                "Tạo danh sách mua sắm Bữa tối thứ Sáu",
                ExecutionMode.QA,
                grantShoppingConsent(),
                null,
                null));
        assertThat(qaResponse.runStatus()).isEqualTo("PREVIEW_ONLY");
        assertThat(qaResponse.steps()).extracting(s -> s.status()).containsExactly("SKIPPED_MODE");
        assertThat(qaResponse.answer()).contains("Chế độ Hỏi đáp (QA) chỉ xem trước", "Bữa tối thứ Sáu");
        verify(shoppingLists, never()).createEmpty(any());

        // AUTO mode: executes create -> read immediately without confirmation
        var autoResponse = service.execute(pilotRequest(
                qaResponse.sessionId(),
                UUID.randomUUID(),
                "Tạo danh sách mua sắm Bữa tối thứ Sáu",
                ExecutionMode.AUTO,
                null,
                null,
                null));
        assertThat(autoResponse.runStatus()).isEqualTo("COMPLETED");
        assertThat(autoResponse.answer()).contains("#501", "Bữa tối thứ Sáu");
        verify(shoppingLists, times(1)).createEmpty(new CreateEmptyShoppingListRequest("Bữa tối thứ Sáu"));
        verify(shoppingLists, times(1)).get(501L);

        // Second distinct AUTO request with same list name is a separate business operation -> #502
        var secondAuto = service.execute(pilotRequest(
                qaResponse.sessionId(),
                UUID.randomUUID(),
                "Tạo danh sách mua sắm Bữa tối thứ Sáu",
                ExecutionMode.AUTO,
                null,
                null,
                null));
        assertThat(secondAuto.runStatus()).isEqualTo("COMPLETED");
        assertThat(secondAuto.answer()).contains("#502");
        verify(shoppingLists, times(2)).createEmpty(new CreateEmptyShoppingListRequest("Bữa tối thứ Sáu"));
    }

    @Test
    void missingNameAndNegativeHypotheticalOrHowToRequestsNeverExecuteWriteEvenInAutoMode() {
        AtomicInteger callSeq = new AtomicInteger();
        ScriptedLlm llm = new ScriptedLlm(request -> {
            List<String> fnResponses = functionResponseNames(request);
            if (!fnResponses.contains("create_shopping_list")) {
                return functionCall("create_shopping_list", "c-attempt-" + callSeq.incrementAndGet(), Map.of("name", "Tiệc chay"));
            }
            return textResponse("Đã tạo danh sách thành công!"); // Dishonest model claim must be overridden by Backend
        });
        var service = createPilotService(llm);

        // 1. Missing name (user did not mention "Tiệc chay")
        var missingName = service.execute(pilotRequest(
                null,
                UUID.randomUUID(),
                "Hãy tạo giúp mình một danh sách mua sắm mới nhé",
                ExecutionMode.AUTO,
                grantShoppingConsent(),
                null,
                null));
        assertThat(missingName.runStatus()).isEqualTo("CLARIFICATION_REQUIRED");
        assertThat(missingName.answer()).contains("Bạn muốn đặt tên danh sách mua sắm là gì?");
        verify(shoppingLists, never()).createEmpty(any());

        // 2. Negative intent ("Đừng tạo danh sách Tiệc chay")
        var negative = service.execute(pilotRequest(
                missingName.sessionId(),
                UUID.randomUUID(),
                "Đừng tạo danh sách mua sắm Tiệc chay lúc này",
                ExecutionMode.AUTO,
                null,
                null,
                null));
        assertThat(negative.steps()).extracting(s -> s.status()).containsExactly("SKIPPED_INTENT");
        assertThat(negative.answer()).contains("không thực hiện tạo danh sách");
        verify(shoppingLists, never()).createEmpty(any());

        // 3. Hypothetical / How-to intent ("Làm sao để tạo danh sách Tiệc chay?")
        var howTo = service.execute(pilotRequest(
                missingName.sessionId(),
                UUID.randomUUID(),
                "Làm sao để tạo danh sách mua sắm Tiệc chay?",
                ExecutionMode.AUTO,
                null,
                null,
                null));
        assertThat(howTo.steps()).extracting(s -> s.status()).containsExactly("SKIPPED_INTENT");
        verify(shoppingLists, never()).createEmpty(any());
    }

    @Test
    void confirmationInvalidatesOnNameChangeModeChangeExpiryOrConsentRevokeAndBlocksWhenBudgetExhaustedBeforeWrite() {
        AtomicInteger callSeq = new AtomicInteger();
        ScriptedLlm llm = new ScriptedLlm(request -> {
            List<String> fnResponses = functionResponseNames(request);
            if (!fnResponses.contains("create_shopping_list")) {
                return functionCall("create_shopping_list", "c-" + callSeq.incrementAndGet(), Map.of("name", "Đồ khô"));
            }
            return textResponse("Xong");
        });
        var service = createPilotService(llm);

        // Stage confirmation #1
        var staged1 = service.execute(pilotRequest(
                null,
                UUID.randomUUID(),
                "Tạo danh sách mua sắm Đồ khô",
                ExecutionMode.CONFIRM,
                grantShoppingConsent(),
                null,
                null));
        UUID conf1 = staged1.pendingConfirmation().confirmationId();

        // Changing mode on approval invalidates confirmation #1
        assertThatThrownBy(() -> service.execute(pilotRequest(
                staged1.sessionId(),
                UUID.randomUUID(),
                "Duyệt",
                ExecutionMode.AUTO,
                null,
                conf1,
                ConfirmationDecision.APPROVE)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);

        // Stage confirmation #2 and let 5 minutes expire
        var staged2 = service.execute(pilotRequest(
                staged1.sessionId(),
                UUID.randomUUID(),
                "Tạo danh sách mua sắm Đồ khô",
                ExecutionMode.CONFIRM,
                null,
                null,
                null));
        clock.advance(Duration.ofMinutes(5).plusSeconds(1));
        assertThatThrownBy(() -> service.execute(pilotRequest(
                staged1.sessionId(),
                UUID.randomUUID(),
                "Duyệt",
                ExecutionMode.CONFIRM,
                null,
                staged2.pendingConfirmation().confirmationId(),
                ConfirmationDecision.APPROVE)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);

        // Stage confirmation #3, then exhaust budget before APPROVE -> stops before write with BLOCKED_BUDGET
        var staged3 = service.execute(pilotRequest(
                staged1.sessionId(),
                UUID.randomUUID(),
                "Tạo danh sách mua sắm Đồ khô",
                ExecutionMode.CONFIRM,
                null,
                null,
                null));
        budgetAvailable.set(false);
        var blockedOnApprove = service.execute(pilotRequest(
                staged1.sessionId(),
                UUID.randomUUID(),
                "Duyệt",
                ExecutionMode.CONFIRM,
                null,
                staged3.pendingConfirmation().confirmationId(),
                ConfirmationDecision.APPROVE));
        assertThat(blockedOnApprove.runStatus()).isEqualTo("BLOCKED_BUDGET");
        verify(shoppingLists, never()).createEmpty(any());

        // Restore budget, stage confirmation #4, then REVOKE consent -> cancels pending & blocks next turn
        budgetAvailable.set(true);
        var staged4 = service.execute(pilotRequest(
                staged1.sessionId(),
                UUID.randomUUID(),
                "Tạo danh sách mua sắm Đồ khô",
                ExecutionMode.CONFIRM,
                null,
                null,
                null));
        var revoked = service.execute(pilotRequest(
                staged1.sessionId(),
                UUID.randomUUID(),
                "Thu hồi đồng ý",
                ExecutionMode.CONFIRM,
                new ConsentDecision(ConsentAction.REVOKE, AdkShoppingPilotService.SHOPPING_CONSENT_SCOPES),
                null,
                null));
        assertThat(revoked.runStatus()).isEqualTo("CONSENT_REVOKED");
        assertThatThrownBy(() -> service.execute(pilotRequest(
                staged1.sessionId(),
                UUID.randomUUID(),
                "Duyệt",
                ExecutionMode.CONFIRM,
                null,
                staged4.pendingConfirmation().confirmationId(),
                ConfirmationDecision.APPROVE)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);
        verify(shoppingLists, never()).createEmpty(any());
    }

    @Test
    void budgetExhaustedDuringModelCallStopsBeforeCreateShoppingListExecutes() {
        ScriptedLlm llm = new ScriptedLlm(request -> {
            // Simulate the model call consuming the last remaining quota/Credit
            budgetAvailable.set(false);
            return functionCall("create_shopping_list", "c-budget", Map.of("name", "Rau củ"));
        });
        var service = createPilotService(llm);

        var response = service.execute(pilotRequest(
                null,
                UUID.randomUUID(),
                "Tạo danh sách mua sắm Rau củ",
                ExecutionMode.AUTO,
                grantShoppingConsent(),
                null,
                null));
        assertThat(response.runStatus()).isEqualTo("BLOCKED_BUDGET");
        assertThat(response.steps()).extracting(s -> s.status()).containsExactly("BLOCKED_BUDGET");
        verify(shoppingLists, never()).createEmpty(any());
    }

    @Test
    void partialFailureAfterCreateCommitsPreservesCreatedListIdAndNeverReplaysCreate() {
        when(shoppingLists.createEmpty(new CreateEmptyShoppingListRequest("Đồ chay tuần mới")))
                .thenReturn(emptyList(777L, "Đồ chay tuần mới"));
        when(shoppingLists.get(777L))
                .thenThrow(new AppException(ErrorCode.INTERNAL_ERROR, "Lỗi đọc lại giả lập"));

        ScriptedLlm llm = new ScriptedLlm(request -> {
            List<String> fnResponses = functionResponseNames(request);
            if (!fnResponses.contains("create_shopping_list")) {
                return functionCall("create_shopping_list", "c-777", Map.of("name", "Đồ chay tuần mới"));
            }
            if (!fnResponses.contains("get_shopping_list")) {
                return functionCall("get_shopping_list", "g-777", Map.of("listId", extractCreatedId(request)));
            }
            return textResponse("Không thể đọc lại");
        });
        var service = createPilotService(llm);

        var partial = service.execute(pilotRequest(
                null,
                UUID.randomUUID(),
                "Tạo danh sách mua sắm Đồ chay tuần mới",
                ExecutionMode.AUTO,
                grantShoppingConsent(),
                null,
                null));
        assertThat(partial.runStatus()).isEqualTo("PARTIAL_FAILURE");
        assertThat(partial.answer()).contains("#777", "Đồ chay tuần mới", "không tự tạo lại");
        assertThat(partial.steps()).extracting(s -> s.tool() + ":" + s.status())
                .containsExactly("create_shopping_list:SUCCEEDED", "get_shopping_list:FAILED");
        verify(shoppingLists, times(1)).createEmpty(any());
    }

    @Test
    void routingPinsEnginePerSessionRejectsUnknownSessionAfterRestartAndSupportsRollback() {
        RecipeService recipes = mock(RecipeService.class);
        RecipePostService recipePosts = mock(RecipePostService.class);
        DietaryPreferenceService preferences = mock(DietaryPreferenceService.class);
        AiClient ai = mock(AiClient.class);
        SubscriptionEntitlementService entitlements = mock(SubscriptionEntitlementService.class);
        when(ai.classifyChatScope(anyString(), anyString())).thenReturn(
                new AiClient.ChatResult(null, "{\"decisionId\":\"MX_CHAT_OK\",\"reasonId\":\"NONE\"}", 1, 1, 2));
        when(ai.answerChat(anyString(), anyString())).thenReturn(new AiClient.ChatResult("Trả lời legacy.", 1, 2, 3));

        ScriptedLlm llm = new ScriptedLlm(request -> textResponse("Xin chào từ pilot G3."));
        var pilotService = createPilotService(llm);
        var legacyExecution = mock(AiAssistantExecutionService.class);
        when(legacyExecution.execute(any())).thenAnswer(inv -> {
            AiChatRequest req = inv.getArgument(0);
            var h = sessionStore.open(req.sessionId(), "user:7", AiAssistantSessionStore.ENGINE_LEGACY);
            return new tech.mamxanh.recipe.dto.response.AiChatResponse(
                    req.contextType(), req.recipeId(), "Trả lời legacy.", List.of(), null,
                    h.id(), ExecutionMode.CONFIRM, UUID.randomUUID(), List.of(), null, null,
                    List.of(), AiAssistantSessionStore.ENGINE_LEGACY, "COMPLETED", h.expiresAt(), null);
        });
        var chatService = new AiChatService(
                users, recipes, recipePosts, preferences, ai, new JsonMapper(), events, clock, entitlements, aiQuota,
                chatSettings, legacyExecution, pilotService);

        // 1. New GENERAL session for CUSTOMER when pilotEnabled=true routes to ADK_SHOPPING_PILOT
        var pilotTurn = chatService.answer(pilotRequest(
                null,
                UUID.randomUUID(),
                "Xin chào trợ lý",
                ExecutionMode.CONFIRM,
                null,
                null,
                null));
        assertThat(pilotTurn.engine()).isEqualTo(AiAssistantSessionStore.ENGINE_G3_PILOT);

        // 2. Unknown sessionId (simulating server restart or expired session) is rejected fail-closed
        assertThatThrownBy(() -> chatService.answer(pilotRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Tiếp tục phiên cũ sau restart",
                ExecutionMode.CONFIRM,
                null,
                null,
                null)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);

        // 3. Rollback: disabling pilot blocks existing pilot session from silently switching to legacy, while new sessions use legacy
        pilotService.setPilotEnabled(false);
        assertThatThrownBy(() -> chatService.answer(pilotRequest(
                pilotTurn.sessionId(),
                UUID.randomUUID(),
                "Thử gửi tiếp trên phiên pilot cũ",
                ExecutionMode.CONFIRM,
                null,
                null,
                null)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);

        var newLegacyTurn = chatService.answer(new AiChatRequest(
                ContextType.GENERAL, null, "Cách luộc rau ngon?", List.of()));
        assertThat(newLegacyTurn.engine()).isEqualTo(AiAssistantSessionStore.ENGINE_LEGACY);
    }

    @Test void clientHistoryCannotSupplyAnInventedListName() {
        var llm=new ScriptedLlm(r->functionResponseNames(r).isEmpty()
                ?functionCall("create_shopping_list","forged",Map.of("name","Bí mật")):textResponse("Bạn muốn đặt tên gì?"));
        var service=createPilotService(llm);
        var request=new AiChatRequest(ContextType.GENERAL,null,"Tạo danh sách mua sắm",List.of(
                new AiChatRequest.Turn(AiChatRequest.Speaker.USER,"Tên là Bí mật",List.of())),null,null,ExecutionMode.AUTO,
                grantShoppingConsent(),null,null,UUID.randomUUID());
        var result=service.execute(request);
        assertThat(result.runStatus()).isEqualTo("CLARIFICATION_REQUIRED");
        verify(shoppingLists,never()).createEmpty(any());
        assertThat(AdkShoppingPilotTools.isUserSuppliedListName("Tuấn","Tạo danh sách Tuần",List.of())).isFalse();
    }

    @Test void modelCannotReadAnotherIdAfterCreateOrCreateTwiceInTheSameTurn() {
        when(shoppingLists.createEmpty(any())).thenReturn(emptyList(901,"Rau củ"));
        for(boolean wrongRead:List.of(true,false)) {
            var llm=new ScriptedLlm(r->functionResponseNames(r).isEmpty()
                    ?functionCall("create_shopping_list","first",Map.of("name","Rau củ"))
                    :wrongRead?functionCall("get_shopping_list","wrong",Map.of("listId",902))
                    :functionCall("create_shopping_list","second",Map.of("name","Rau củ")));
            var service=createPilotService(llm);
            var result=service.execute(pilotRequest(null,UUID.randomUUID(),"Tạo danh sách mua sắm Rau củ",ExecutionMode.AUTO,grantShoppingConsent(),null,null));
            assertThat(result.runStatus()).isEqualTo("PARTIAL_FAILURE");
        }
        verify(shoppingLists,org.mockito.Mockito.times(2)).createEmpty(any());
        verify(shoppingLists,never()).get(anyLong());
    }

    @Test void revokeDuringInFlightModelStopsTheFollowingWrite() throws Exception {
        var started=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
        var llm=new ScriptedLlm(r->{started.countDown();try {release.await(5,java.util.concurrent.TimeUnit.SECONDS);} catch(InterruptedException e){Thread.currentThread().interrupt();}
            return functionCall("create_shopping_list","after-revoke",Map.of("name","Rau củ"));});
        var service=createPilotService(llm);
        var consent=service.execute(pilotRequest(null,UUID.randomUUID(),"Tạo danh sách Rau củ",ExecutionMode.AUTO,null,null,null));
        var run=java.util.concurrent.CompletableFuture.supplyAsync(()->service.execute(pilotRequest(consent.sessionId(),UUID.randomUUID(),"Tạo danh sách Rau củ",ExecutionMode.AUTO,grantShoppingConsent(),null,null)));
        assertThat(started.await(5,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        var revoke=service.execute(pilotRequest(consent.sessionId(),UUID.randomUUID(),"Thu hồi",ExecutionMode.AUTO,
                new ConsentDecision(ConsentAction.REVOKE,AdkShoppingPilotService.SHOPPING_CONSENT_SCOPES),null,null));
        assertThat(revoke.runStatus()).isEqualTo("CONSENT_REVOKED");release.countDown();run.get(8,java.util.concurrent.TimeUnit.SECONDS);
        verify(shoppingLists,never()).createEmpty(any());
    }

    @Test void anotherMemberCannotDeleteTheNativeSession() {
        var service=createPilotService(new ScriptedLlm(r->textResponse("Hướng dẫn")));
        var first=service.execute(pilotRequest(null,UUID.randomUUID(),"Tạo danh sách Rau củ",ExecutionMode.CONFIRM,null,null,null));
        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(8L,Role.EXPERT));when(users.findAuthenticatedUserId()).thenReturn(Optional.of(8L));
        assertThatThrownBy(()->service.deleteSession(first.sessionId(),null)).isInstanceOf(AppException.class);
        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(7L,Role.CUSTOMER));when(users.findAuthenticatedUserId()).thenReturn(Optional.of(7L));
        assertThat(service.ownsSession(first.sessionId(),"user:7")).isTrue();
    }

    @Test void sourcesReadBeforeClarificationRemainAttachedToConfirmationAndTheCommittedResult() {
        when(shoppingLists.createEmpty(any())).thenReturn(emptyList(901,"Rau củ"));when(shoppingLists.get(901)).thenReturn(emptyList(901,"Rau củ"));
        var calls=new java.util.concurrent.atomic.AtomicInteger();
        var service=createPilotService(new ScriptedLlm(r->switch(calls.getAndIncrement()) {
            case 0 -> functionCall("search_business_knowledge","search",Map.of("query","Tạo danh sách rỗng"));
            case 1 -> functionCall("get_business_knowledge","guide",Map.of("knowledgeId","shopping-create"));
            case 2 -> textResponse("Bạn muốn đặt tên gì?");
            case 3 -> functionCall("create_shopping_list","create",Map.of("name","Rau củ"));
            case 4 -> textResponse("Đang chờ bạn xác nhận");
            case 5 -> functionCall("get_shopping_list","read",Map.of("listId",901));
            default -> textResponse("Đã đọc lại");
        }));
        var ask=service.execute(pilotRequest(null,UUID.randomUUID(),"Hãy tạo danh sách mua sắm",ExecutionMode.CONFIRM,grantShoppingConsent(),null,null));
        assertThat(ask.sources()).extracting(s->s.id()).contains("shopping-create");
        var staged=service.execute(pilotRequest(ask.sessionId(),UUID.randomUUID(),"Tên là Rau củ",ExecutionMode.CONFIRM,null,null,null));
        assertThat(staged.sources()).extracting(s->s.id()).contains("shopping-create");
        var completed=service.execute(pilotRequest(ask.sessionId(),UUID.randomUUID(),"Duyệt",ExecutionMode.CONFIRM,null,staged.pendingConfirmation().confirmationId(),ConfirmationDecision.APPROVE));
        assertThat(completed.runStatus()).isEqualTo("COMPLETED");
        assertThat(completed.sources()).extracting(s->s.id()).contains("shopping-create");
        assertThat(completed.sources()).filteredOn(s -> s.id().equals("shopping-create"))
                .allSatisfy(source -> assertThat(source.version()).isEqualTo("2.0.0"));
    }

    @Test void cancellationKeepsTheSourcesActuallyReadForThePendingPreview() {
        var calls=new java.util.concurrent.atomic.AtomicInteger();
        var service=createPilotService(new ScriptedLlm(r->switch(calls.getAndIncrement()) {
            case 0 -> functionCall("get_business_knowledge","guide",Map.of("knowledgeId","shopping-create"));
            case 1 -> functionCall("create_shopping_list","create",Map.of("name","Rau củ"));
            default -> textResponse("Đang chờ xác nhận");
        }));
        var staged=service.execute(pilotRequest(null,UUID.randomUUID(),"Tạo danh sách Rau củ",ExecutionMode.CONFIRM,grantShoppingConsent(),null,null));
        var cancelled=service.execute(pilotRequest(staged.sessionId(),UUID.randomUUID(),"Hủy",ExecutionMode.CONFIRM,null,staged.pendingConfirmation().confirmationId(),ConfirmationDecision.REJECT));
        assertThat(cancelled.runStatus()).isEqualTo("CANCELLED");
        assertThat(cancelled.sources()).extracting(s->s.id()).contains("shopping-create");
        verify(shoppingLists,never()).createEmpty(any());
    }

    @Test void readOnlyReplyCannotClaimAWriteEvenWhenTheModelMentionsTheCorrectId() {
        when(shoppingLists.get(901)).thenReturn(emptyList(901,"Rau củ"));
        var service=createPilotService(new ScriptedLlm(r->functionResponseNames(r).isEmpty()
                ?functionCall("get_shopping_list","read",Map.of("listId",901)):textResponse("Đã xóa danh sách 901")));
        var response=service.execute(pilotRequest(null,UUID.randomUUID(),"Đọc danh sách ID #901",ExecutionMode.AUTO,grantShoppingConsent(),null,null));
        assertThat(response.answer()).contains("901","Rau củ").doesNotContain("Đã xóa");
        verify(shoppingLists,never()).createEmpty(any());
    }

    private static ShoppingListResponse emptyList(long id, String name) {
        LocalDateTime now = LocalDateTime.of(2026, 11, 1, 9, 0);
        return new ShoppingListResponse(id, name, now, now, List.of());
    }

    private AdkShoppingPilotService createPilotService(BaseLlm llm) {
        return new AdkShoppingPilotService(
                users,
                shoppingLists,
                aiQuota,
                chatSettings,
                sessionStore,
                mapper,
                events,
                clock,
                g3Catalog,
                null,
                modelId -> llm,
                null,
                true);
    }

    private static ConsentDecision grantShoppingConsent() {
        return new ConsentDecision(ConsentAction.GRANT, AdkShoppingPilotService.SHOPPING_CONSENT_SCOPES);
    }

    private static AiChatRequest pilotRequest(UUID sessionId, UUID clientRequestId, String question,
            ExecutionMode mode, ConsentDecision consent, UUID confirmationId, ConfirmationDecision decision) {
        return new AiChatRequest(
                ContextType.GENERAL,
                null,
                question,
                List.of(),
                null,
                sessionId,
                mode,
                consent,
                confirmationId,
                decision,
                clientRequestId);
    }

    private static List<Content> currentTurnContents(LlmRequest request) {
        List<Content> all = request.contents();
        int start = 0;
        for (int i = 0; i < all.size(); i++) {
            Content content = all.get(i);
            boolean isUserTurnStart = content.parts().orElse(List.of()).stream().anyMatch(part ->
                    part.text().isPresent() && "user".equalsIgnoreCase(content.role().orElse("user"))
                            || part.functionResponse().map(fr ->
                                    "adk_request_confirmation".equals(fr.name().orElse(""))).orElse(false));
            if (isUserTurnStart) {
                start = i;
            }
        }
        return all.subList(start, all.size());
    }

    private static List<String> functionResponseNames(LlmRequest request) {
        return currentTurnContents(request).stream()
                .flatMap(c -> c.parts().orElse(List.of()).stream())
                .filter(p -> p.functionResponse().isPresent())
                .map(p -> p.functionResponse().orElseThrow().name().orElse(""))
                .toList();
    }

    private static long extractCreatedId(LlmRequest request) {
        List<Long> ids = currentTurnContents(request).stream()
                .flatMap(c -> c.parts().orElse(List.of()).stream())
                .filter(p -> p.functionResponse().isPresent()
                        && "create_shopping_list".equals(p.functionResponse().orElseThrow().name().orElse("")))
                .map(p -> p.functionResponse().orElseThrow().response().orElse(Map.of()).get("id"))
                .filter(Number.class::isInstance)
                .map(n -> ((Number) n).longValue())
                .toList();
        return ids.isEmpty() ? -1L : ids.getLast();
    }

    private static LlmResponse functionCall(String name, String id, Map<String, Object> args) {
        return LlmResponse.builder()
                .content(Content.builder()
                        .role("model")
                        .parts(List.of(Part.builder()
                                .functionCall(FunctionCall.builder().id(id).name(name).args(args).build())
                                .build()))
                        .build())
                .usageMetadata(GenerateContentResponseUsageMetadata.builder()
                        .promptTokenCount(20)
                        .candidatesTokenCount(10)
                        .totalTokenCount(30)
                        .build())
                .build();
    }

    private static LlmResponse textResponse(String text) {
        return LlmResponse.builder()
                .content(Content.builder()
                        .role("model")
                        .parts(List.of(Part.fromText(text)))
                        .build())
                .usageMetadata(GenerateContentResponseUsageMetadata.builder()
                        .promptTokenCount(15)
                        .candidatesTokenCount(10)
                        .totalTokenCount(25)
                        .build())
                .build();
    }

    private static final class ScriptedLlm extends BaseLlm {
        private final Function<LlmRequest, LlmResponse> responder;
        private final List<LlmRequest> requests = new ArrayList<>();
        private final AtomicInteger calls = new AtomicInteger();

        private ScriptedLlm(Function<LlmRequest, LlmResponse> responder) {
            super("gemini-3.8-flash");
            this.responder = responder;
        }

        @Override
        public Flowable<LlmResponse> generateContent(LlmRequest request, boolean stream) {
            return Flowable.fromCallable(() -> {
                requests.add(request);
                calls.incrementAndGet();
                return responder.apply(request);
            });
        }

        @Override
        public BaseLlmConnection connect(LlmRequest request) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class MutableClock extends Clock {
        private Instant current;

        private MutableClock(Instant initial) {
            this.current = initial;
        }

        void advance(Duration delta) {
            this.current = this.current.plus(delta);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}

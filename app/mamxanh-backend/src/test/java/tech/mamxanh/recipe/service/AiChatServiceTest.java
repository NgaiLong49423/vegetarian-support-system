package tech.mamxanh.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.ai.AiClient;
import tech.mamxanh.integration.ai.AiUsageEvent;
import tech.mamxanh.nutrition.service.DietaryPreferenceService;
import tech.mamxanh.recipe.dto.request.AiChatRequest;
import tech.mamxanh.recipe.dto.response.RecipePostResponse;
import tools.jackson.databind.json.JsonMapper;

class AiChatServiceTest {
    private CurrentUserService users;
    private RecipeService recipes;
    private RecipePostService recipePosts;
    private DietaryPreferenceService preferences;
    private AiClient ai;
    private ApplicationEventPublisher events;
    private AiChatService service;
    private tech.mamxanh.subscription.service.SubscriptionEntitlementService entitlements;

    @BeforeEach
    void setup() {
        users = mock(CurrentUserService.class);
        recipes = mock(RecipeService.class);
        recipePosts = mock(RecipePostService.class);
        preferences = mock(DietaryPreferenceService.class);
        ai = mock(AiClient.class);
        when(ai.classifyChatScope(anyString(), anyString())).thenReturn(
                new AiClient.ChatResult(null, "{\"decisionId\":\"MX_CHAT_OK\",\"reasonId\":\"NONE\"}", 1, 1, 2));
        events = mock(ApplicationEventPublisher.class);
        entitlements = mock(tech.mamxanh.subscription.service.SubscriptionEntitlementService.class);
        service = new AiChatService(users, recipes, recipePosts, preferences, ai, new JsonMapper(), events, Clock.systemUTC(), entitlements);
        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(7, Role.CUSTOMER));
        when(users.findAuthenticatedUserId()).thenReturn(java.util.Optional.of(7L));
        when(preferences.menuConstraints(7)).thenReturn(new DietaryPreferenceService.MenuDietaryConstraints(
                "VEGAN", List.of(8L), List.of("Sữa")));
        when(recipePosts.searchPublished("", 0, 50)).thenReturn(new RecipePostService.RecipePageResponse(
                List.of(), 0, 50, 0, 0));
    }

    @Test
    void guestUsesTheSameChatWithoutLoadingAPersonalDietaryProfile() {
        when(users.findAuthenticatedUserId()).thenReturn(java.util.Optional.empty());
        when(ai.answerChat(anyString(), anyString())).thenReturn(new AiClient.ChatResult("Nấu nhỏ lửa.", 1, 2, 3));
        assertThat(service.answer(new AiChatRequest(AiChatRequest.ContextType.GENERAL, null,
                "Cách nấu canh?", List.of())).answer()).isEqualTo("Nấu nhỏ lửa.");
        verifyNoInteractions(preferences);
        verify(users, never()).requireActiveMember();
        ArgumentCaptor<AiUsageEvent> usage = ArgumentCaptor.forClass(AiUsageEvent.class);
        verify(events, times(2)).publishEvent(usage.capture());
        assertThat(usage.getValue().userId()).isNull();
    }

    @Test
    void generalChatUsesMemberDietaryConstraintsAndPublishesTokenCountsOnly() {
        when(ai.answerChat(anyString(), anyString())).thenReturn(new AiClient.ChatResult("Dùng sữa đậu nành.", 12, 8, 20));

        var response = service.answer(new AiChatRequest(AiChatRequest.ContextType.GENERAL, null,
                "Thay sữa bò bằng gì?", List.of(new AiChatRequest.Turn(AiChatRequest.Speaker.USER, "Món cháo"))));

        assertThat(response.answer()).isEqualTo("Dùng sữa đậu nành.");
        ArgumentCaptor<String> instruction = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(ai).answerChat(instruction.capture(), prompt.capture());
        assertThat(instruction.getValue()).contains("Never diagnose", "untrusted data", "Do not invent recipe facts");
        assertThat(prompt.getValue()).contains("VEGAN", "Sữa", "Thay sữa bò bằng gì?");
        ArgumentCaptor<AiUsageEvent> usage = ArgumentCaptor.forClass(AiUsageEvent.class);
        verify(events, times(2)).publishEvent(usage.capture());
        assertThat(usage.getAllValues()).allSatisfy(event -> assertThat(event.feature()).isEqualTo("FR-51"));
        assertThat(usage.getAllValues()).anySatisfy(event -> assertThat(event.totalTokens()).isEqualTo(20));
    }

    @Test
    void completeLongAnswersRemainUsableInTheNextConversationTurn() {
        String answer = "a".repeat(2500);
        when(ai.answerChat(anyString(), anyString())).thenReturn(new AiClient.ChatResult(answer, 1, 2, 3));
        var first = service.answer(new AiChatRequest(AiChatRequest.ContextType.GENERAL, null, "Hello", List.of()));
        assertThat(first.answer()).isEqualTo(answer);
        when(ai.answerChat(anyString(), anyString())).thenReturn(new AiClient.ChatResult("Next answer", 1, 2, 3));
        assertThat(service.answer(new AiChatRequest(AiChatRequest.ContextType.GENERAL, null, "Next",
                List.of(new AiChatRequest.Turn(AiChatRequest.Speaker.ASSISTANT, first.answer())))).answer())
                .isEqualTo("Next answer");
    }

    @Test
    void oversizedProviderAnswersAreRejectedWithoutPublishingSuccessfulUsage() {
        when(ai.answerChat(anyString(), anyString()))
                .thenReturn(new AiClient.ChatResult("a".repeat(AiClient.MAX_CHAT_ANSWER_LENGTH + 1), 1, 2, 3));
        assertThatThrownBy(() -> service.answer(new AiChatRequest(AiChatRequest.ContextType.GENERAL,
                null, "Hello", List.of()))).isInstanceOfSatisfying(AppException.class,
                failure -> assertThat(failure.errorCode()).isEqualTo(ErrorCode.AI_RESPONSE_INVALID));
        verify(events).publishEvent(any(AiUsageEvent.class));
    }

    @Test
    void recipeContextIsLoadedFromCurrentPublicRecipeService() {
        when(ai.answerChat(anyString(), anyString())).thenReturn(new AiClient.ChatResult("Nấu nhỏ lửa.", 1, 2, 3));
        when(recipes.getPublishedRecipe(19L)).thenReturn(new tech.mamxanh.recipe.dto.response.RecipeDetailResponse(
                19L, "Canh rau", "Mô tả", "Nấu trong 10 phút", "SOUP", "Món canh", "VEGAN", "Thuần chay",
                "EASY", "Dễ", 2, 5, 10, null, null, null, List.of(), true, List.of(), null, null, List.of()));

        var response = service.answer(new AiChatRequest(AiChatRequest.ContextType.RECIPE, 19L,
                "Nấu bao lâu?", List.of()));

        assertThat(response.recipeId()).isEqualTo(19L);
        verify(recipes).getPublishedRecipe(19L);
        verify(ai).answerChat(anyString(), contains("Canh rau"));
    }

    @Test
    void questionAboutCurrentRecipeDoesNotBecomeRecipeDiscovery() {
        when(recipes.getPublishedRecipe(19L)).thenReturn(new tech.mamxanh.recipe.dto.response.RecipeDetailResponse(
                19L, "Canh rau", "Mô tả", "Nấu trong 10 phút", "SOUP", "Món canh", "VEGAN", "Thuần chay",
                "EASY", "Dễ", 2, 5, 10, null, null, null, List.of(), true, List.of(), null, null, List.of()));
        when(ai.answerChat(anyString(), anyString())).thenReturn(structured(
                "{\"action\":\"ANSWER\",\"answer\":\"Bài viết chưa ghi danh sách nguyên liệu.\",\"recommendations\":[]}"));

        var response = service.answer(new AiChatRequest(AiChatRequest.ContextType.RECIPE, 19L,
                "Công thức này gồm những nguyên liệu nào?", List.of()));

        assertThat(response.answer()).isEqualTo("Bài viết chưa ghi danh sách nguyên liệu.");
        verify(recipePosts, never()).searchPublished(anyString(), anyInt(), anyInt());
        verify(ai).answerChat(anyString(), contains("Canh rau"));

        when(ai.answerChat(anyString(), anyString())).thenReturn(structured(
                "{\"action\":\"FIND_RECIPES\",\"answer\":\"\",\"recommendations\":[]}"));
        var discovery = service.answer(new AiChatRequest(AiChatRequest.ContextType.RECIPE, 19L,
                "Gợi ý món khác giống công thức này", List.of()));

        assertThat(discovery.answer()).contains("chưa tìm thấy");
        verify(recipePosts).searchPublished("", 0, 50);
    }

    @Test
    void invalidContextNeverCallsGemini() {
        assertThatThrownBy(() -> service.answer(new AiChatRequest(AiChatRequest.ContextType.RECIPE, null,
                "Câu hỏi", List.of())))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_FAILED);
        verifyNoInteractions(ai, events, recipes);
    }

    @Test
    void historyLongerThanEightTurnsIsRejectedBeforeGemini() {
        List<AiChatRequest.Turn> history = java.util.stream.IntStream.range(0, 9)
                .mapToObj(index -> new AiChatRequest.Turn(AiChatRequest.Speaker.USER, "Question " + index)).toList();

        assertThatThrownBy(() -> service.answer(new AiChatRequest(AiChatRequest.ContextType.GENERAL, null,
                "Câu hỏi hiện tại", history)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_FAILED);
        verifyNoInteractions(ai, events, recipes);
    }

    @Test
    void providerFailureBecomesFriendlyUnavailableError() {
        when(ai.answerChat(anyString(), anyString())).thenThrow(new tech.mamxanh.integration.ai.GeminiAiClient.AiProviderException("timeout"));

        assertThatThrownBy(() -> service.answer(new AiChatRequest(AiChatRequest.ContextType.GENERAL, null,
                "Gợi ý món chay", List.of())))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AI_PROVIDER_UNAVAILABLE);
        verify(events).publishEvent(any(AiUsageEvent.class));
    }

    @Test
    void recipeDiscoveryReturnsOnlyPublicVegetarianPostsWithoutAvoidedIngredients() {
        var acceptedWithCover = post(1, "VEGAN", "PUBLISHED",
                List.of(ingredient(9L, "Đậu phụ", null)), List.of(new RecipePostResponse.Media("/cover.jpg", "image/jpeg", 0, true)),
                "Món " + "ngon ".repeat(50));
        var acceptedWithoutCover = post(2, "VEGAN", "PUBLISHED", null,
                List.of(new RecipePostResponse.Media("/detail.jpg", "image/jpeg", 1, false)), null);
        var candidates = List.of(acceptedWithCover,
                post(3, "VEGAN", "DRAFT", List.of(), List.of(), "Draft"),
                post(4, "LACTO", "PUBLISHED", List.of(), List.of(), "Has dairy type"),
                post(5, "VEGAN", "PUBLISHED", List.of(ingredient(8L, "Sữa", null)), List.of(), "Avoided by ID"),
                post(6, "VEGAN", "PUBLISHED", List.of(ingredient(10L, "Sữa", null)), List.of(), "Avoided by name"),
                post(7, "VEGAN", "PUBLISHED", List.of(ingredient(10L, "Yến mạch", "Sữa")), List.of(), "Avoided by custom name"),
                acceptedWithoutCover);
        when(recipePosts.searchPublished("", 0, 50)).thenReturn(new RecipePostService.RecipePageResponse(candidates, 0, 50, 7, 1));
        when(recipePosts.getPublishedCards(List.of(1L, 2L))).thenReturn(List.of(acceptedWithCover, acceptedWithoutCover));
        when(ai.answerChat(anyString(), anyString())).thenReturn(structured(
                "{\"action\":\"FIND_RECIPES\",\"answer\":\"\",\"recommendations\":["
                        + "{\"recipeId\":1,\"reason\":\"Món thuần chay dễ nấu\"},"
                        + "{\"recipeId\":2,\"reason\":\"Có nguyên liệu đơn giản\"}]}"));

        var response = service.answer(chat("Gợi ý món chay dễ nấu"));

        assertThat(response.recommendations()).hasSize(2);
        assertThat(response.recommendations()).extracting("id").containsExactly(1L, 2L);
        assertThat(response.recommendations().get(0).coverUrl()).isEqualTo("/cover.jpg");
        assertThat(response.recommendations().get(1).coverUrl()).isNull();
        assertThat(response.answer()).isEqualTo("Mình tìm được một số bài công thức phù hợp:");
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(ai).answerChat(anyString(), prompt.capture());
        assertThat(prompt.getValue()).contains("Allowed public RecipePost IDs: [1, 2]")
                .doesNotContain("Avoided by ID", "Avoided by name", "Avoided by custom name", "Has dairy type", "Draft");
    }

    @Test
    void recipeDiscoveryRevalidatesRecommendationsBeforeReturningCards() {
        var candidate = post(12, "VEGAN", "PUBLISHED", List.of(), List.of(), "An toàn lúc tìm");
        when(recipePosts.searchPublished("", 0, 50)).thenReturn(new RecipePostService.RecipePageResponse(List.of(candidate), 0, 50, 1, 1));
        when(ai.answerChat(anyString(), anyString())).thenReturn(structured(
                "{\"action\":\"FIND_RECIPES\",\"answer\":\"Có món phù hợp\",\"recommendations\":["
                        + "{\"recipeId\":12,\"reason\":\"Thuần chay\"}]}"));
        when(recipePosts.getPublishedCards(List.of(12L))).thenReturn(List.of(
                post(12, "LACTO", "PUBLISHED", List.of(), List.of(), "Đổi loại ăn chay sau khi tìm")));

        var response = service.answer(chat("Tìm công thức chay"));

        assertThat(response.recommendations()).isEmpty();
        assertThat(response.answer()).contains("chưa tìm thấy");
    }

    @Test
    void recommendationIdsAndReasonsAreValidatedAgainstTheCandidateSet() {
        var candidate = post(20, "VEGAN", "PUBLISHED", List.of(), List.of(), "Món hợp lệ");
        when(recipePosts.searchPublished("", 0, 50)).thenReturn(new RecipePostService.RecipePageResponse(List.of(candidate), 0, 50, 1, 1));
        List<String> invalidSelections = List.of(
                "{\"recipeId\":0,\"reason\":\"Không hợp lệ\"}",
                "{\"recipeId\":21,\"reason\":\"Không thuộc danh mục\"}",
                "{\"recipeId\":20,\"reason\":\"  \"}",
                "{\"recipeId\":20,\"reason\":\"" + "x".repeat(301) + "\"}",
                "null");

        for (String selection : invalidSelections) {
            when(ai.answerChat(anyString(), anyString())).thenReturn(structured(
                    "{\"action\":\"FIND_RECIPES\",\"answer\":\"\",\"recommendations\":[" + selection + "]}"));
            assertThatThrownBy(() -> service.answer(chat("Gợi ý công thức")))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.AI_RESPONSE_INVALID);
        }

        when(ai.answerChat(anyString(), anyString())).thenReturn(structured(
                "{\"action\":\"FIND_RECIPES\",\"answer\":\"\",\"recommendations\":["
                        + "{\"recipeId\":20,\"reason\":\"Một\"},{\"recipeId\":20,\"reason\":\"Hai\"}]}"));
        assertThatThrownBy(() -> service.answer(chat("Gợi ý công thức")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AI_RESPONSE_INVALID);
    }

    @Test
    void malformedOrUnsupportedStructuredRepliesAreRejected() {
        List<String> invalidJson = List.of(
                "{not-json}",
                "{\"action\":null,\"answer\":\"x\",\"recommendations\":[]}",
                "{\"action\":\"ANSWER\",\"answer\":null,\"recommendations\":[]}",
                "{\"action\":\"ANSWER\",\"answer\":\"x\",\"recommendations\":null}",
                "{\"action\":\"UNKNOWN\",\"answer\":\"x\",\"recommendations\":[]}",
                "{\"action\":\"ANSWER\",\"answer\":\"x\",\"recommendations\":[{},{},{},{},{}]}");

        for (String json : invalidJson) {
            when(ai.answerChat(anyString(), anyString())).thenReturn(structured(json));
            assertThatThrownBy(() -> service.answer(chat("Cách nấu món chay?")))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.AI_RESPONSE_INVALID);
        }

        when(ai.answerChat(anyString(), anyString())).thenReturn(new AiClient.ChatResult("  ", 1, 1, 2));
        assertThatThrownBy(() -> service.answer(chat("Cách nấu món chay?")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AI_RESPONSE_INVALID);
        when(ai.answerChat(anyString(), anyString())).thenReturn(null);
        assertThatThrownBy(() -> service.answer(chat("Cách nấu món chay?")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AI_RESPONSE_INVALID);
    }

    @Test
    void recipeDiscoveryDoesNotInventResultsWhenCatalogOrCurrentPostIsEmpty() {
        when(ai.answerChat(anyString(), anyString())).thenReturn(structured(
                "{\"action\":\"FIND_RECIPES\",\"answer\":\"\",\"recommendations\":[]}"));
        var noCandidates = service.answer(chat("Gợi ý món chay"));
        assertThat(noCandidates.recommendations()).isEmpty();
        assertThat(noCandidates.answer()).contains("chưa tìm thấy");

        var candidate = post(25, "VEGAN", "PUBLISHED", List.of(), List.of(), "Món hợp lệ khi tìm");
        when(recipePosts.searchPublished("", 0, 50)).thenReturn(new RecipePostService.RecipePageResponse(List.of(candidate), 0, 50, 1, 1));
        when(ai.answerChat(anyString(), anyString())).thenReturn(structured(
                "{\"action\":\"FIND_RECIPES\",\"answer\":\"\",\"recommendations\":["
                        + "{\"recipeId\":25,\"reason\":\"Món công khai\"}]}"));
        when(recipePosts.getPublishedCards(List.of(25L))).thenReturn(List.of());

        var removedAfterSelection = service.answer(chat("Gợi ý món chay"));
        assertThat(removedAfterSelection.recommendations()).isEmpty();
        assertThat(removedAfterSelection.answer()).contains("chưa tìm thấy");
    }

    @Test
    void vegetarianTypeRulesAndMissingPreferenceListsAreApplied() {
        var catalog = List.of(
                post(31, "VEGAN", "PUBLISHED", null, List.of(), null),
                post(32, "LACTO", "PUBLISHED", List.of(), List.of(), "Lacto"),
                post(33, "OVO", "PUBLISHED", List.of(), List.of(), "Ovo"),
                post(34, "LACTO_OVO", "PUBLISHED", List.of(), List.of(), "Lacto-ovo"));
        when(recipePosts.searchPublished("", 0, 50)).thenReturn(new RecipePostService.RecipePageResponse(catalog, 0, 50, 4, 1));
        when(ai.answerChat(anyString(), anyString())).thenReturn(structured(
                "{\"action\":\"FIND_RECIPES\",\"answer\":\"\",\"recommendations\":[]}"));

        for (var preference : List.of(
                new DietaryPreferenceService.MenuDietaryConstraints("VEGAN", null, null),
                new DietaryPreferenceService.MenuDietaryConstraints("LACTO", List.of(), List.of()),
                new DietaryPreferenceService.MenuDietaryConstraints("OVO", null, java.util.Collections.singletonList(null)),
                new DietaryPreferenceService.MenuDietaryConstraints("LACTO_OVO", List.of(), null),
                new DietaryPreferenceService.MenuDietaryConstraints("UNRECOGNIZED", null, null),
                new DietaryPreferenceService.MenuDietaryConstraints(null, null, null),
                new DietaryPreferenceService.MenuDietaryConstraints(" ", List.of(), List.of()))) {
            when(preferences.menuConstraints(7)).thenReturn(preference);
            service.answer(chat("Gợi ý món chay"));
        }
        verify(ai, times(7)).answerChat(anyString(), anyString());
    }

    @Test
    void explicitCreationAndAdaptationAreOnlyHandledWhenRequested() {
        when(ai.answerChat(anyString(), anyString())).thenReturn(structured(
                "{\"action\":\"CREATE_RECIPE\",\"answer\":\"Bản nháp công thức\",\"recommendations\":[]}"));
        var unsolicitedDraft = service.answer(chat("Gợi ý món chay dễ nấu"));
        assertThat(unsolicitedDraft.answer()).contains("ưu tiên tìm công thức");
        var requestedDraft = service.answer(chat("Hãy viết công thức mới cho tôi"));
        assertThat(requestedDraft.answer()).isEqualTo("Bản nháp công thức");

        when(ai.answerChat(anyString(), anyString())).thenReturn(structured(
                "{\"action\":\"ADAPT_RECIPE\",\"answer\":\"Bản biến tấu\",\"recommendations\":[]}"));
        var unsolicitedAdaptation = service.answer(chat("Nấu món gì?"));
        assertThat(unsolicitedAdaptation.answer()).contains("khi bạn yêu cầu rõ");
        var adaptationWithoutRecipeContext = service.answer(chat("Hãy biến tấu công thức này"));
        assertThat(adaptationWithoutRecipeContext.answer()).contains("mở bài công thức");

        when(recipes.getPublishedRecipe(40L)).thenReturn(new tech.mamxanh.recipe.dto.response.RecipeDetailResponse(
                40L, "Canh bí", null, "Nấu canh", "SOUP", "Món canh", "VEGAN", "Thuần chay", "EASY", "Dễ",
                2, 5, 10, null, null, null, List.of(), true, List.of(), null, null, List.of()));
        var adaptationWithRecipeContext = service.answer(new AiChatRequest(AiChatRequest.ContextType.RECIPE, 40L,
                "Hãy biến tấu công thức này", List.of()));
        assertThat(adaptationWithRecipeContext.answer()).isEqualTo("Bản biến tấu");
    }

    @Test
    void unexpectedActionsAndContradictoryRecommendationsAreRejected() {
        when(ai.answerChat(anyString(), anyString())).thenReturn(structured(
                "{\"action\":\"ANSWER\",\"answer\":\"Trả lời\",\"recommendations\":["
                        + "{\"recipeId\":1,\"reason\":\"Lý do\"}]}"));
        assertThatThrownBy(() -> service.answer(chat("Hỏi về cách nấu")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AI_RESPONSE_INVALID);

        when(ai.answerChat(anyString(), anyString())).thenReturn(structured(
                "{\"action\":\"CREATE_RECIPE\",\"answer\":\"Bản nháp\",\"recommendations\":["
                        + "{\"recipeId\":1,\"reason\":\"Lý do\"}]}"));
        assertThatThrownBy(() -> service.answer(chat("Hãy viết công thức mới")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AI_RESPONSE_INVALID);

        when(ai.answerChat(anyString(), anyString())).thenReturn(structured(
                "{\"action\":\"ANSWER\",\"answer\":\"Trả lời\",\"recommendations\":[]}"));
        assertThatThrownBy(() -> service.answer(chat("Tìm công thức món chay")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AI_RESPONSE_INVALID);
    }

    private static AiChatRequest chat(String question) {
        return new AiChatRequest(AiChatRequest.ContextType.GENERAL, null, question, List.of());
    }

    @Test
    void weeklyMenuIsDeniedBeforePreferencesCatalogProviderOrUsageAndAllowedPaidMemberIsRoutedWithoutGeneration() {
        doThrow(new AppException(ErrorCode.AI_SUBSCRIPTION_REQUIRED).withProperty("requiredTier", "PLUS"))
                .when(entitlements).requirePlusOrPro(7L);
        assertThatThrownBy(() -> service.answer(chat("Lập thực đơn tuần 7 ngày theo dinh dưỡng cá nhân cho tôi.")))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.AI_SUBSCRIPTION_REQUIRED);
        verifyNoInteractions(preferences, recipes, recipePosts, ai, events);
        reset(entitlements);
        var routed = service.answer(chat("Lập thực đơn tuần"));
        assertThat(routed.workflowUrl()).isEqualTo("/ke-hoach?view=for-you");
        assertThat(routed.recommendations()).isEmpty();
        verify(entitlements).requirePlusOrPro(7L);
        verifyNoInteractions(preferences, recipes, recipePosts, ai, events);
    }

    @Test
    void guestCannotAskChatToRunAMemberWorkflow() {
        when(users.findAuthenticatedUserId()).thenReturn(java.util.Optional.empty());
        assertThatThrownBy(() -> service.answer(chat("Lên menu cho tuần này")))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.UNAUTHENTICATED);
        verifyNoInteractions(entitlements, preferences, recipes, recipePosts, ai, events);
    }

    @Test
    void recipePostAuthoringRechecksRoleAndProBeforeReturningTheDedicatedForm() {
        var routed = service.answer(chat("Viết mô tả cho bài đăng công thức"));
        verify(users).requireActiveExpert();
        verify(entitlements).requirePro(7L);
        assertThat(routed.workflowUrl()).isEqualTo("/dang-cong-thuc");
        verifyNoInteractions(ai, events);
    }

    @Test
    void followUpLoadsRealCatalogAndExcludesOnlyEarlierAssistantCardIds() {
        var old = post(41, "VEGAN", "PUBLISHED", List.of(), List.of(), "Cũ");
        var next = post(42, "VEGAN", "PUBLISHED", List.of(), List.of(), "Mới");
        when(recipePosts.searchPublished("", 0, 50)).thenReturn(new RecipePostService.RecipePageResponse(List.of(old, next), 0, 50, 2, 1));
        when(ai.answerChat(anyString(), anyString())).thenReturn(structured(
                "{\"action\":\"FIND_RECIPES\",\"answer\":\"Database không còn món\",\"recommendations\":[{\"recipeId\":42,\"reason\":\"Món công khai\"}]}"));
        when(recipePosts.getPublishedCards(List.of(42L))).thenReturn(List.of(next));
        var history = List.of(new AiChatRequest.Turn(AiChatRequest.Speaker.USER, "Gợi ý món chay"),
                new AiChatRequest.Turn(AiChatRequest.Speaker.ASSISTANT, "Món trước", List.of(41L, 999L)));
        var response = service.answer(new AiChatRequest(AiChatRequest.ContextType.GENERAL, null, "còn món khác không", history));
        assertThat(response.recommendations()).extracting(item -> item.id()).containsExactly(42L);
        assertThat(response.answer()).doesNotContain("không còn");
        var prompt = ArgumentCaptor.forClass(String.class);
        verify(ai).answerChat(anyString(), prompt.capture());
        assertThat(prompt.getValue()).contains("Allowed public RecipePost IDs: [42]", "bounded search");
    }

    @Test
    void categoryQueryRunsBeforePagingAndReadsAnotherPageWhenDietaryFiltersRemoveTheFirst() {
        var restricted = post(51, "LACTO", "PUBLISHED", List.of(), List.of(), "Không hợp Vegan");
        var original = post(52, "VEGAN", "PUBLISHED", List.of(), List.of(), "Món kho thật");
        var braised = new RecipePostResponse(original.id(), original.authorId(), original.authorName(), original.authorAvatarUrl(),
                original.title(), original.description(), original.instructions(), "BRAISED", original.vegetarianType(), original.difficulty(),
                original.servings(), original.prepTimeMin(), original.cookTimeMin(), original.youtubeUrl(), original.status(), original.media(),
                original.ingredients(), original.likes(), original.dislikes(), original.likePercentage(), original.viewCount());
        when(recipePosts.searchPublished("", 0, 50, RecipeSortMode.NEWEST, RecipeViewPeriod.ALL_TIME, null, "BRAISED", List.of(), null))
                .thenReturn(new RecipePostService.RecipePageResponse(List.of(restricted), 0, 50, 51, 2));
        when(recipePosts.searchPublished("", 1, 50, RecipeSortMode.NEWEST, RecipeViewPeriod.ALL_TIME, null, "BRAISED", List.of(), null))
                .thenReturn(new RecipePostService.RecipePageResponse(List.of(braised), 1, 50, 51, 2));
        when(ai.answerChat(anyString(), anyString())).thenReturn(structured(
                "{\"action\":\"FIND_RECIPES\",\"answer\":\"Món kho\",\"recommendations\":[{\"recipeId\":52,\"reason\":\"Món kho công khai\"}]}"));
        when(recipePosts.getPublishedCards(List.of(52L))).thenReturn(List.of(braised));
        assertThat(service.answer(chat("các món kho thử xem")).recommendations()).extracting(item -> item.id()).containsExactly(52L);
        verify(recipePosts, never()).searchPublished("", 0, 50);
    }

    @Test
    void boundedCatalogNeverBecomesAGlobalAbsenceClaim() {
        when(ai.answerChat(anyString(), anyString())).thenReturn(structured(
                "{\"action\":\"FIND_RECIPES\",\"answer\":\"Toàn hệ thống không có món\",\"recommendations\":[]}"));
        var response = service.answer(chat("còn món khác không"));
        assertThat(response.answer()).contains("phần danh mục đã kiểm tra").doesNotContain("Toàn hệ thống");
    }

    private static AiClient.ChatResult structured(String json) {
        return new AiClient.ChatResult(null, json, 1, 1, 2);
    }

    private static RecipePostResponse post(long id, String type, String status,
            List<RecipePostResponse.Ingredient> ingredients, List<RecipePostResponse.Media> media, String description) {
        return new RecipePostResponse(id, 1, "Tác giả", null, "Món " + id, description, "Hướng dẫn",
                "SOUP", type, "EASY", 2, 5, 10, null, status, media, ingredients,
                0, 0, BigDecimal.ZERO, 0);
    }

    private static RecipePostResponse.Ingredient ingredient(Long id, String name, String customName) {
        return new RecipePostResponse.Ingredient(id, name, customName, 1, "g", "gram", BigDecimal.ONE);
    }
}

package tech.mamxanh.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.Clock;
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
import tools.jackson.databind.json.JsonMapper;

class AiChatServiceTest {
    private CurrentUserService users;
    private RecipeService recipes;
    private DietaryPreferenceService preferences;
    private AiClient ai;
    private ApplicationEventPublisher events;
    private AiChatService service;

    @BeforeEach
    void setup() {
        users = mock(CurrentUserService.class);
        recipes = mock(RecipeService.class);
        preferences = mock(DietaryPreferenceService.class);
        ai = mock(AiClient.class);
        events = mock(ApplicationEventPublisher.class);
        service = new AiChatService(users, recipes, preferences, ai, new JsonMapper(), events, Clock.systemUTC());
        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(7, Role.CUSTOMER));
        when(preferences.menuConstraints(7)).thenReturn(new DietaryPreferenceService.MenuDietaryConstraints(
                "VEGAN", List.of(8L), List.of("Sữa")));
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
        verify(events).publishEvent(usage.capture());
        assertThat(usage.getValue().feature()).isEqualTo("FR-51");
        assertThat(usage.getValue().totalTokens()).isEqualTo(20);
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
        verify(events, never()).publishEvent(any(AiUsageEvent.class));
    }
}

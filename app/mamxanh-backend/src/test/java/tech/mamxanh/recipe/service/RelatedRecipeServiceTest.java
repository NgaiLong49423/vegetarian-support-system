package tech.mamxanh.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.ai.AiClient;
import tech.mamxanh.integration.ai.AiUsageEvent;
import tech.mamxanh.integration.ai.GeminiAiClient.AiProviderException;
import tech.mamxanh.recipe.dto.response.RecipePostResponse;
import tech.mamxanh.recipe.repository.RelatedRecipeRepository;
import tech.mamxanh.subscription.service.SubscriptionEntitlementService;
import tools.jackson.databind.json.JsonMapper;

class RelatedRecipeServiceTest {
    private RelatedRecipeRepository repository;
    private RecipePostService recipes;
    private CurrentUserService users;
    private SubscriptionEntitlementService entitlement;
    private AiClient ai;
    private ApplicationEventPublisher events;
    private RelatedRecipeService service;

    @BeforeEach
    void setup() {
        repository = mock(RelatedRecipeRepository.class);
        recipes = mock(RecipePostService.class);
        users = mock(CurrentUserService.class);
        entitlement = mock(SubscriptionEntitlementService.class);
        ai = mock(AiClient.class);
        events = mock(ApplicationEventPublisher.class);
        service = new RelatedRecipeService(repository, recipes, users, entitlement, ai,
                new JsonMapper(), events, Clock.systemUTC());
    }

    private void candidates() {
        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(7, Role.CUSTOMER));
        when(recipes.getPublishedCards(List.of(1L))).thenReturn(List.of(recipe(1, "VEGAN")));
        when(repository.findCandidates(1, 50, true)).thenReturn(List.of(2L, 3L));
        when(recipes.getPublishedCards(List.of(2L, 3L))).thenReturn(List.of(recipe(2, "VEGAN"), recipe(3, "VEGAN")));
    }

    @Test
    void ordinaryRelatedNeverAuthenticatesOrCallsGemini() {
        when(recipes.getPublishedCards(List.of(1L))).thenReturn(List.of(recipe(1, "VEGAN")));
        when(repository.findCandidates(1, 6, false)).thenReturn(List.of(2L));
        when(recipes.getPublishedCards(List.of(2L))).thenReturn(List.of(recipe(2, "VEGAN")));
        assertThat(service.related(1, 6).items()).extracting(RecipePostResponse::id).containsExactly(2L);
        verifyNoInteractions(users, entitlement, ai, events);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 13, -1})
    void rejectsInvalidLimits(int limit) {
        assertThatThrownBy(() -> service.related(1, limit)).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.recommend(1, limit)).isInstanceOf(AppException.class);
        verifyNoInteractions(repository, ai, users);
    }

    @Test
    void rejectsInvalidSourceAndUnavailableSource() {
        assertThatThrownBy(() -> service.related(0, 6)).isInstanceOf(AppException.class);
        when(recipes.getPublishedCards(List.of(1L))).thenReturn(List.of());
        assertThatThrownBy(() -> service.related(1, 6)).isInstanceOf(AppException.class);
        verifyNoInteractions(repository, ai);
    }

    @Test
    void entitlementIsCheckedBeforeReadingCatalogOrCallingProvider() {
        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(7, Role.CUSTOMER));
        doThrow(new AppException(ErrorCode.AI_SUBSCRIPTION_REQUIRED)).when(entitlement).requirePlusOrPro(7);
        assertThatThrownBy(() -> service.recommend(1, 6)).isInstanceOf(AppException.class);
        verifyNoInteractions(repository, recipes, ai);
    }

    @Test
    void emptyCandidateSetAvoidsGeminiCost() {
        candidates();
        when(repository.findCandidates(1, 50, true)).thenReturn(List.of());
        when(recipes.getPublishedCards(List.of())).thenReturn(List.of());
        assertThat(service.recommend(1, 6).items()).isEmpty();
        verifyNoInteractions(ai, events);
    }

    @Test
    void deduplicatesIdsAndHandsOffOnlyActualProviderUsage() {
        candidates();
        when(ai.recommendRecipes(anyString(), anyList(), anyInt()))
                .thenReturn(new AiClient.RecommendationResult("{\"recipeIds\":[2,2]}", 100, 10, 110));
        when(recipes.getPublishedCards(List.of(2L))).thenReturn(List.of(recipe(2, "VEGAN")));
        assertThat(service.recommend(1, 6).items()).hasSize(1);
        var usage = ArgumentCaptor.forClass(AiUsageEvent.class);
        verify(events).publishEvent(usage.capture());
        assertThat(usage.getValue().feature()).isEqualTo("FR-47");
        assertThat(usage.getValue().totalTokens()).isEqualTo(110);
        verify(ai).recommendRecipes(contains("untrusted data"), eq(List.of(2L, 3L)), eq(6));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "null", "not json", "{\"recipeIds\":1}", "{\"recipeIds\":[99]}",
            "{\"recipeIds\":[\"2\"]}", "{\"recipeIds\":[2.1]}", "{\"recipeIds\":[null]}",
            "{\"recipeIds\":[9223372036854775808]}", "{\"recipeIds\":[2,3,2,3,2,3,2]}"})
    void rejectsInvalidProviderSelectionWithoutSuccessfulTelemetry(String json) {
        candidates();
        when(ai.recommendRecipes(anyString(), anyList(), anyInt()))
                .thenReturn(new AiClient.RecommendationResult(json, null, null, null));
        assertThatThrownBy(() -> service.recommend(1, 6)).isInstanceOf(AppException.class);
        verifyNoInteractions(events);
    }

    @Test
    void rejectsNullAndOversizedResponses() {
        candidates();
        assertThatThrownBy(() -> service.recommend(1, 6)).isInstanceOf(AppException.class);
        for (String raw : new String[]{null, " ".repeat(8193)}) {
            when(ai.recommendRecipes(anyString(), anyList(), anyInt()))
                    .thenReturn(new AiClient.RecommendationResult(raw, null, null, null));
            assertThatThrownBy(() -> service.recommend(1, 6)).isInstanceOf(AppException.class);
        }
    }

    @Test
    void emptySelectionIsAValidNoMatchResponse() {
        candidates();
        when(ai.recommendRecipes(anyString(), anyList(), anyInt()))
                .thenReturn(new AiClient.RecommendationResult("{\"recipeIds\":[]}", null, null, null));
        when(recipes.getPublishedCards(List.of())).thenReturn(List.of());
        assertThat(service.recommend(1, 6).items()).isEmpty();
    }

    @Test
    void providerFailureDoesNotEmitSuccessfulUsage() {
        candidates();
        when(ai.recommendRecipes(anyString(), anyList(), anyInt())).thenThrow(new AiProviderException("timeout"));
        assertThatThrownBy(() -> service.recommend(1, 6)).isInstanceOf(AppException.class);
        verifyNoInteractions(events);
    }

    @Test
    void rechecksVisibilityAndDietaryTypeAfterProviderReturns() {
        candidates();
        when(ai.recommendRecipes(anyString(), anyList(), anyInt()))
                .thenReturn(new AiClient.RecommendationResult("{\"recipeIds\":[2,3]}", null, null, null));
        when(recipes.getPublishedCards(List.of(2L, 3L)))
                .thenReturn(List.of(recipe(2, "VEGAN"), recipe(3, "VEGAN")), List.of(recipe(3, "LACTO")));
        assertThat(service.recommend(1, 6).items()).isEmpty();
    }

    private static RecipePostResponse recipe(long id, String diet) {
        return new RecipePostResponse(id, 7, "Author", null, "Recipe " + id, null,
                "Instructions", "BRAISED", diet, "EASY", 2, 5, 10, null, "PUBLISHED",
                List.of(), List.of(), 0, 0, null, 0);
    }
}

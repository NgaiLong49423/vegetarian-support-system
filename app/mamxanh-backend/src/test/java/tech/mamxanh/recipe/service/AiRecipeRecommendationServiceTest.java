package tech.mamxanh.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.ai.AiClient;
import tech.mamxanh.integration.ai.AiUsageEvent;
import tech.mamxanh.nutrition.dto.response.DietaryPreferencesResponse;
import tech.mamxanh.nutrition.entity.OnboardingStatus;
import tech.mamxanh.nutrition.entity.VegetarianType;
import tech.mamxanh.nutrition.service.DietaryPreferenceService;
import tech.mamxanh.recipe.dto.response.RecipePostResponse;
import tech.mamxanh.subscription.service.SubscriptionEntitlementService;
import tools.jackson.databind.json.JsonMapper;

class AiRecipeRecommendationServiceTest {
    private RecipePostService recipes;
    private CurrentUserService users;
    private DietaryPreferenceService preferences;
    private SubscriptionEntitlementService entitlements;
    private AiClient ai;
    private ApplicationEventPublisher events;
    private AiRecipeRecommendationService service;

    @BeforeEach
    void setup() {
        recipes = mock(RecipePostService.class);
        users = mock(CurrentUserService.class);
        preferences = mock(DietaryPreferenceService.class);
        entitlements = mock(SubscriptionEntitlementService.class);
        ai = mock(AiClient.class);
        events = mock(ApplicationEventPublisher.class);
        service = new AiRecipeRecommendationService(recipes, users, preferences, entitlements, ai,
                new JsonMapper(), events, Clock.systemUTC());
        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(7, Role.CUSTOMER));
        when(preferences.getOwnPreferences()).thenReturn(profile());
    }

    @Test
    void incompleteProfileStopsBeforeCatalogOrGemini() {
        doThrow(new AppException(ErrorCode.DIETARY_PROFILE_INCOMPLETE)).when(preferences).requirePersonalizedAiEligible(7);
        assertThatThrownBy(() -> service.recommend(List.of(1L))).isInstanceOf(AppException.class);
        verifyNoInteractions(recipes, ai, events);
    }

    @Test
    void recommendationsUseSafeCandidatesAndRecordOnlyProviderUsage() {
        when(recipes.searchPublished("", 0, 50)).thenReturn(new RecipePostService.RecipePageResponse(
                List.of(recipe(1, "VEGAN", 1L, 2L), recipe(2, "VEGAN", 2L), recipe(3, "LACTO", 1L),
                        recipe(4, "VEGAN", 1L, 99L), recipe(5, "VEGAN", 1L)), 0, 50, 5, 1));
        when(ai.recommendRecipes(anyString(), anyList(), eq(5)))
                .thenReturn(new AiClient.RecommendationResult("{\"recipeIds\":[2,1,5]}", 10, 4, 14));
        var result = service.recommend(List.of(1L, 2L));
        assertThat(result.items()).extracting(item -> item.recipe().id()).containsExactly(1L, 2L, 5L);
        assertThat(result.items().getFirst().reason()).contains("2 nguyên liệu");
        verify(ai).recommendRecipes(contains("selectedIngredientIds"), eq(List.of(1L, 2L, 5L)), eq(5));
        var event = org.mockito.ArgumentCaptor.forClass(AiUsageEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().feature()).isEqualTo("FR-34");
        assertThat(event.getValue().totalTokens()).isEqualTo(14);
    }

    @Test
    void freeMemberCannotRequestWeeklyMenuBeforeProfileOrGeminiWork() {
        doThrow(new AppException(ErrorCode.AI_SUBSCRIPTION_REQUIRED)).when(entitlements).requirePro(7);
        assertThatThrownBy(() -> service.weeklyMenu(List.of(1L))).isInstanceOf(AppException.class);
        verifyNoInteractions(preferences, recipes, ai, events);
    }

    @Test
    void weeklyPreviewReturnsTwentyOnePublicSlotsWithoutApplyingMealPlanChanges() {
        when(recipes.searchPublished("", 0, 50)).thenReturn(new RecipePostService.RecipePageResponse(
                List.of(recipe(1, "VEGAN", 1L), recipe(2, "VEGAN", 1L)), 0, 50, 2, 1));
        String ids = java.util.stream.IntStream.range(0, 21)
                .mapToObj(index -> index % 2 == 0 ? "1" : "2").collect(java.util.stream.Collectors.joining(","));
        when(ai.recommendRecipeSequence(anyString(), anyList(), eq(21)))
                .thenReturn(new AiClient.RecommendationResult("{\"recipeIds\":[" + ids + "]}", 60, 15, 75));

        var preview = service.weeklyMenu(List.of(1L));

        assertThat(preview.slots()).hasSize(21);
        assertThat(preview.slots().getFirst().mealType()).isEqualTo("BREAKFAST");
        assertThat(preview.slots().getLast().dayOffset()).isEqualTo(6);
        assertThat(preview.slots().getLast().mealType()).isEqualTo("DINNER");
        verify(ai).recommendRecipeSequence(contains("selectedIngredientIds"), eq(List.of(1L, 2L)), eq(21));
        var event = org.mockito.ArgumentCaptor.forClass(AiUsageEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().feature()).isEqualTo("FR-34");
    }

    @Test
    void weeklyPreviewRejectsConsecutiveRepeatsAndDoesNotEmitUsage() {
        when(recipes.searchPublished("", 0, 50)).thenReturn(new RecipePostService.RecipePageResponse(
                List.of(recipe(1, "VEGAN", 1L), recipe(2, "VEGAN", 1L)), 0, 50, 2, 1));
        when(ai.recommendRecipeSequence(anyString(), anyList(), eq(21)))
                .thenReturn(new AiClient.RecommendationResult("{\"recipeIds\":[" + "1,".repeat(20) + "1]}", null, null, null));
        assertThatThrownBy(() -> service.weeklyMenu(List.of(1L)))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_RESPONSE_INVALID);
        verifyNoInteractions(events);
    }

    @Test
    void rejectsEmptyIngredientSelectionAndSkipsGeminiWhenCatalogHasNoMatches() {
        assertThatThrownBy(() -> service.recommend(List.of()))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);
        verifyNoInteractions(recipes, ai, events);

        when(recipes.searchPublished("", 0, 50)).thenReturn(
                new RecipePostService.RecipePageResponse(List.of(), 0, 50, 0, 0));
        var result = service.recommend(List.of(1L));
        assertThat(result.items()).isEmpty();
        verifyNoInteractions(ai, events);
    }

    @Test
    void mapsProviderAndMalformedResponseFailuresWithoutRecordingUsage() {
        when(recipes.searchPublished("", 0, 50)).thenReturn(new RecipePostService.RecipePageResponse(
                List.of(recipe(1, "VEGAN", 1L), recipe(2, "VEGAN", 1L), recipe(3, "VEGAN", 1L)), 0, 50, 3, 1));
        when(ai.recommendRecipes(anyString(), anyList(), eq(5)))
                .thenThrow(new tech.mamxanh.integration.ai.GeminiAiClient.AiProviderException("unavailable"));
        assertThatThrownBy(() -> service.recommend(List.of(1L)))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_PROVIDER_UNAVAILABLE);
        verifyNoInteractions(events);

        reset(ai);
        when(ai.recommendRecipes(anyString(), anyList(), eq(5)))
                .thenReturn(new AiClient.RecommendationResult("{\"recipeIds\":[1,1,2]}", 1, 1, 2));
        assertThatThrownBy(() -> service.recommend(List.of(1L)))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_RESPONSE_INVALID);
        verifyNoInteractions(events);
    }

    @Test
    void rechecksCurrentPublicationAndDietarySafetyBeforeApplyingWeeklyPreview() {
        var forbidden = recipe(9, "VEGAN", 99L);
        when(recipes.getPublishedCards(List.of(1L, 9L))).thenReturn(List.of(recipe(1, "VEGAN", 1L), forbidden));
        assertThatThrownBy(() -> service.requireWeeklyApplyEligible(7L, List.of(1L, 9L)))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECIPE_NOT_FOUND);

        when(recipes.getPublishedCards(List.of(1L))).thenReturn(List.of(recipe(1, "VEGAN", 1L)));
        service.requireWeeklyApplyEligible(7L, List.of(1L, 1L));
        verify(recipes).getPublishedCards(List.of(1L));
    }

    private static DietaryPreferencesResponse profile() {
        return new DietaryPreferencesResponse(VegetarianType.VEGAN,
                new DietaryPreferencesResponse.IngredientPreferences(false,
                        List.of(new DietaryPreferencesResponse.PreferenceItem(99L, "Ingredient 99"))),
                new DietaryPreferencesResponse.IngredientPreferences(true, List.of()), null, null, null,
                OnboardingStatus.COMPLETED, new DietaryPreferencesResponse.AiPersonalization(true, List.of()));
    }

    private static RecipePostResponse recipe(long id, String diet, Long... ingredients) {
        List<RecipePostResponse.Ingredient> lines = java.util.Arrays.stream(ingredients)
                .map(ingredient -> new RecipePostResponse.Ingredient(ingredient, "Ingredient " + ingredient,
                        null, 1, "G", "g", BigDecimal.ONE)).toList();
        return new RecipePostResponse(id, 10, "Author", null, "Recipe " + id, null, "Instructions",
                "SOUP", diet, "EASY", 2, 5, 10, null, "PUBLISHED", List.of(), lines, 0, 0, null, 0);
    }
}

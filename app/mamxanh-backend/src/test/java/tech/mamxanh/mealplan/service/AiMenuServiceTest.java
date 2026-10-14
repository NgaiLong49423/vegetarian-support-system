package tech.mamxanh.mealplan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.ai.AiClient;
import tech.mamxanh.mealplan.dto.request.ApplyAiMenuRequest;
import tech.mamxanh.mealplan.dto.request.CreateAiMenuPreviewRequest;
import tech.mamxanh.mealplan.entity.MealPlanEntity;
import tech.mamxanh.mealplan.repository.MealPlanEntryRepository;
import tech.mamxanh.mealplan.repository.MealPlanRepository;
import tech.mamxanh.nutrition.dto.response.UnitResponse;
import tech.mamxanh.nutrition.entity.MeasurementDimension;
import tech.mamxanh.nutrition.service.DietaryPreferenceService;
import tech.mamxanh.nutrition.service.IngredientCatalogLookupService;
import tech.mamxanh.nutrition.service.IngredientConversionLookupService;
import tech.mamxanh.nutrition.service.NutritionProfileService;
import tech.mamxanh.nutrition.service.RecipeNutritionService;
import tech.mamxanh.recipe.dto.response.RecipePostResponse;
import tech.mamxanh.recipe.service.RecipePostService;
import tech.mamxanh.subscription.service.SubscriptionEntitlementService;

class AiMenuServiceTest {
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);
    private final CurrentUserService users = mock(CurrentUserService.class);
    private final SubscriptionEntitlementService entitlements = mock(SubscriptionEntitlementService.class);
    private final NutritionProfileService profiles = mock(NutritionProfileService.class);
    private final DietaryPreferenceService dietary = mock(DietaryPreferenceService.class);
    private final RecipePostService recipes = mock(RecipePostService.class);
    private final RecipeNutritionService nutrition = mock(RecipeNutritionService.class);
    private final IngredientCatalogLookupService ingredients = mock(IngredientCatalogLookupService.class);
    private final IngredientConversionLookupService conversions = mock(IngredientConversionLookupService.class);
    private final AiClient ai = mock(AiClient.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final MealPlanRepository plans = mock(MealPlanRepository.class);
    private final MealPlanEntryRepository entries = mock(MealPlanEntryRepository.class);
    private final MealPlanReadService reads = mock(MealPlanReadService.class);
    private final AiMenuService service = new AiMenuService(users, entitlements, profiles, dietary, recipes,
            nutrition, ingredients, conversions, ai, events, Clock.systemUTC(), new JsonMapper(), plans, entries, reads);

    @BeforeEach
    void member() {
        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(17L, Role.CUSTOMER));
    }

    @Test
    void deniesNonProBeforeReadingHealthProfileOrCallingAi() {
        doThrow(new AppException(ErrorCode.AI_SUBSCRIPTION_REQUIRED)).when(entitlements).requirePro(17L);
        assertThatThrownBy(() -> service.createPreview(new CreateAiMenuPreviewRequest(MONDAY, "PROFILE", null, null, null)))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_SUBSCRIPTION_REQUIRED);
        verifyNoInteractions(profiles, dietary, recipes, ai);
    }

    @Test
    void rejectsMissingNutritionProfileBeforeCallingAi() {
        when(profiles.requireMenuNutritionContext(17L)).thenThrow(new tech.mamxanh.nutrition.service.NutritionProfileException(
                HttpStatus.CONFLICT, "NUTRITION_PROFILE_REQUIRED", "profile required"));
        assertThatThrownBy(() -> service.createPreview(new CreateAiMenuPreviewRequest(MONDAY, "PROFILE", null, null, null)))
                .isInstanceOf(tech.mamxanh.nutrition.service.NutritionProfileException.class);
        verifyNoInteractions(dietary, recipes, ai);
    }

    @Test
    void profileGoalMayBeNullAndPreviewRequiresTwentyOneTrustedNonAdjacentSlots() {
        prepareCandidates();
        when(ai.recommendRecipes(any(), anyList(), eq(21))).thenReturn(
                new AiClient.RecommendationResult("{\"recipeIds\":" + alternatingIds() + "}", 1, 1, 2));
        var result = service.createPreview(new CreateAiMenuPreviewRequest(MONDAY, "PROFILE", null, null, null));
        assertThat(result.goal()).isNull();
        assertThat(result.meals()).hasSize(21);
        for (int index = 1; index < result.meals().size(); index++) {
            assertThat(result.meals().get(index).recipeId()).isNotEqualTo(result.meals().get(index - 1).recipeId());
        }
        verify(ai).recommendRecipes(any(), anyList(), eq(21));
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue()).isInstanceOfSatisfying(tech.mamxanh.integration.ai.AiUsageEvent.class,
                usage -> assertThat(usage).extracting("feature", "promptTokens").containsExactly("FR-36", 1));
    }

    @Test
    void thisWeekPreferencesArePassedOnlyAsRequestContext() {
        prepareCandidates();
        when(ai.recommendRecipes(any(), anyList(), eq(21))).thenReturn(
                new AiClient.RecommendationResult("{\"recipeIds\":" + alternatingIds() + "}", 1, 1, 2));
        var request = new CreateAiMenuPreviewRequest(MONDAY, "THIS_WEEK", "LOSE_WEIGHT", List.of(2L), List.of(3L));

        var result = service.createPreview(request);

        assertThat(result.mode()).isEqualTo("THIS_WEEK");
        assertThat(result.goal()).isEqualTo("LOSE_WEIGHT");
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(ai).recommendRecipes(prompt.capture(), anyList(), eq(21));
        assertThat(prompt.getValue()).contains("Goal preference=LOSE_WEIGHT")
                .contains("preferred ingredient IDs=[2]")
                .contains("forbidden ingredient IDs=[3]");
        verify(events).publishEvent(any(tech.mamxanh.integration.ai.AiUsageEvent.class));
    }

    @Test
    void adjacentRepeatedSelectionIsRejectedBeforeWeekMutation() {
        prepareCandidates();
        var duplicate = List.of(new ApplyAiMenuRequest.SelectedMeal(MONDAY, "BREAKFAST", 11L),
                new ApplyAiMenuRequest.SelectedMeal(MONDAY, "LUNCH", 11L));
        var request = new ApplyAiMenuRequest(MONDAY, "PROFILE", null, null, duplicate);
        assertThatThrownBy(() -> service.apply(request)).isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);
        verifyNoInteractions(plans, entries);
        verify(recipes, never()).requirePublishedMealPlanRecipe(anyLong());
    }

    @Test
    void appliesSelectedPreviewByReplacingTheWeekOnlyAfterRevalidatingRecipes() {
        prepareCandidates();
        MealPlanEntity week = new MealPlanEntity();
        week.setId(31L);
        when(plans.lockWeek(17L, MONDAY)).thenReturn(java.util.Optional.of(week));
        var request = new ApplyAiMenuRequest(MONDAY, "THIS_WEEK", null, null, List.of(
                new ApplyAiMenuRequest.SelectedMeal(MONDAY, "BREAKFAST", 11L),
                new ApplyAiMenuRequest.SelectedMeal(MONDAY, "LUNCH", 12L)));

        service.apply(request);

        var inOrder = inOrder(recipes, plans, entries, reads);
        inOrder.verify(recipes).requirePublishedMealPlanRecipe(11L);
        inOrder.verify(recipes).requirePublishedMealPlanRecipe(12L);
        inOrder.verify(plans).createWeekIfAbsent(17L, MONDAY);
        inOrder.verify(plans).lockWeek(17L, MONDAY);
        inOrder.verify(entries).deleteAllByMealPlanId(31L);
        inOrder.verify(entries).flush();
        inOrder.verify(entries, times(2)).save(any());
        inOrder.verify(plans).touchWeek(31L);
        inOrder.verify(reads).getWeek(MONDAY);
    }

    @Test
    void missingLockedWeekDoesNotDeleteAnyExistingEntries() {
        prepareCandidates();
        when(plans.lockWeek(17L, MONDAY)).thenReturn(java.util.Optional.empty());
        var request = new ApplyAiMenuRequest(MONDAY, "PROFILE", null, null,
                List.of(new ApplyAiMenuRequest.SelectedMeal(MONDAY, "BREAKFAST", 11L)));

        assertThatThrownBy(() -> service.apply(request)).isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);

        verify(recipes).requirePublishedMealPlanRecipe(11L);
        verify(entries, never()).deleteAllByMealPlanId(anyLong());
        verify(entries, never()).save(any());
        verifyNoInteractions(reads);
    }

    @Test
    void rejectsProviderFailureAndMalformedAiOutputWithoutPublishingUsage() {
        prepareCandidates();
        when(ai.recommendRecipes(any(), anyList(), eq(21)))
                .thenThrow(new tech.mamxanh.integration.ai.GeminiAiClient.AiProviderException("unavailable"));
        assertThatThrownBy(() -> service.createPreview(new CreateAiMenuPreviewRequest(MONDAY, "PROFILE", null, null, null)))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_PROVIDER_UNAVAILABLE);
        verifyNoInteractions(events);

        reset(ai);
        when(ai.recommendRecipes(any(), anyList(), eq(21)))
                .thenReturn(new AiClient.RecommendationResult("not-json", 1, 1, 2));
        assertThatThrownBy(() -> service.createPreview(new CreateAiMenuPreviewRequest(MONDAY, "PROFILE", null, null, null)))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_RESPONSE_INVALID);
        verifyNoInteractions(events);
    }

    @Test
    void rejectsEmptySafeCandidatePoolBeforeCallingAi() {
        prepareCandidates();
        when(recipes.searchPublished("", 0, 50)).thenReturn(
                new RecipePostService.RecipePageResponse(List.of(), 0, 50, 0, 0));

        assertThatThrownBy(() -> service.createPreview(new CreateAiMenuPreviewRequest(MONDAY, "PROFILE", null, null, null)))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_RESPONSE_INVALID);
        verifyNoInteractions(ai, events);
    }

    private void prepareCandidates() {
        when(profiles.requireMenuNutritionContext(17L)).thenReturn(
                new NutritionProfileService.MenuNutritionContext(null, "FEMALE", BigDecimal.valueOf(160), BigDecimal.valueOf(55), "LIGHTLY_ACTIVE", 25));
        when(dietary.menuConstraints(17L)).thenReturn(new DietaryPreferenceService.MenuDietaryConstraints(null, List.of(), List.of()));
        when(ingredients.findActiveUnits()).thenReturn(List.of(new UnitResponse(1, "g", "gram", MeasurementDimension.MASS, BigDecimal.ONE, true)));
        when(recipes.searchPublished("", 0, 50)).thenReturn(new RecipePostService.RecipePageResponse(
                List.of(recipe(11, "Canh nấm", 1), recipe(12, "Đậu phụ kho", 2)), 0, 50, 2, 1));
        when(nutrition.calculate(anyList(), eq(2))).thenReturn(new RecipeNutritionService.NutritionSummary(true, List.of(),
                Map.of(), Map.of("ENERGY_KCAL", BigDecimal.valueOf(100), "PROTEIN_G", BigDecimal.valueOf(5))));
    }

    private static RecipePostResponse recipe(long id, String title, long ingredientId) {
        return new RecipePostResponse(id, 3, "Author", null, title, null, "Instructions", "SOUP", "VEGAN", "EASY", 2,
                10, 10, null, "PUBLISHED", List.of(), List.of(new RecipePostResponse.Ingredient(ingredientId,
                        "ingredient", null, 1, "g", "gram", BigDecimal.valueOf(100))), 0, 0, null, 0);
    }

    private static String alternatingIds() {
        return java.util.stream.IntStream.range(0, 21).mapToObj(index -> index % 2 == 0 ? "11" : "12")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }
}

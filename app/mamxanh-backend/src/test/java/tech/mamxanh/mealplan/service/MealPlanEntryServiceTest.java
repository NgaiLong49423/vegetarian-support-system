package tech.mamxanh.mealplan.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.mealplan.dto.request.AddMealPlanEntryRequest;
import tech.mamxanh.mealplan.dto.request.MoveMealPlanEntryRequest;
import tech.mamxanh.mealplan.dto.request.ApplyAiWeeklyMenuRequest;
import tech.mamxanh.mealplan.entity.MealPlanEntity;
import tech.mamxanh.mealplan.entity.MealPlanEntryEntity;
import tech.mamxanh.mealplan.repository.MealPlanRepository;
import tech.mamxanh.mealplan.repository.MealPlanEntryRepository;
import tech.mamxanh.recipe.service.RecipePostService;

class MealPlanEntryServiceTest {
    private static final LocalDate WEEK = LocalDate.of(2026, 10, 12);
    private final MealPlanRepository plans = mock(MealPlanRepository.class);
    private final MealPlanEntryRepository entries = mock(MealPlanEntryRepository.class);
    private final CurrentUserService users = mock(CurrentUserService.class);
    private final RecipePostService recipes = mock(RecipePostService.class);
    private final MealPlanReadService reads = mock(MealPlanReadService.class);
    private final tech.mamxanh.recipe.service.AiRecipeRecommendationService aiRecommendations =
            mock(tech.mamxanh.recipe.service.AiRecipeRecommendationService.class);
    private final MealPlanService service = new MealPlanService(plans, users, reads, entries, recipes, aiRecommendations);

    @BeforeEach
    void setup() {
        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(17L, Role.CUSTOMER));
        var plan = new MealPlanEntity();
        plan.setId(12L);
        when(plans.lockWeek(17L, WEEK)).thenReturn(Optional.of(plan));
        when(entries.saveAndFlush(any())).thenAnswer(call -> {
            MealPlanEntryEntity item = call.getArgument(0);
            if (item.getId() == null) item.setId(31L);
            return item;
        });
        when(recipes.findMealPlanReferences(anyList())).thenAnswer(call -> {
            List<Long> ids = call.getArgument(0);
            long id = ids.getFirst();
            return Map.of(id, new RecipePostService.MealPlanRecipeReference(id, "Món công khai", "PUBLISHED", null, "SOUP", 25));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"BREAKFAST", "LUNCH", "DINNER"})
    void addsInEachMealWithOnePlannedServing(String meal) {
        var result = service.addEntry(WEEK, new AddMealPlanEntryRequest(WEEK.plusDays(6), meal, 1L));
        assertThat(result.mealType()).isEqualTo(meal);
        assertThat(result.mealDate()).isEqualTo(WEEK.plusDays(6));
        assertThat(result.plannedServings()).isEqualByComparingTo("1");
        verify(plans).createWeekIfAbsent(17L, WEEK);
        verify(plans).touchWeek(12L);
        verify(recipes).requirePublishedMealPlanRecipe(1L);
    }

    @Test
    void aiMenuCollisionRejectsBeforeAnyEntryIsWritten() {
        var request = aiMenu();
        when(entries.existsByMealPlanIdAndMealDateAndMealTypeAndRecipeIdAndIdNot(
                anyLong(), any(), anyString(), anyLong(), anyLong())).thenReturn(false, true);

        assertThatThrownBy(() -> service.applyAiMenu(WEEK, request))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEAL_PLAN_RECIPE_DUPLICATE);
        verify(entries, never()).saveAllAndFlush(anyList());
        verify(plans, never()).touchWeek(anyLong());
        verify(aiRecommendations).requireWeeklyApplyEligible(17L, request.slots().stream()
                .map(ApplyAiWeeklyMenuRequest.Slot::recipeId).toList());
    }

    @Test
    void aiMenuAppliesAllTwentyOneSlotsTogether() {
        var request = aiMenu();
        var response = new tech.mamxanh.mealplan.dto.response.MealPlanWeekResponse(WEEK, List.of());
        when(reads.getWeek(WEEK)).thenReturn(response);
        when(entries.saveAllAndFlush(anyList())).thenAnswer(call -> call.getArgument(0));

        assertThat(service.applyAiMenu(WEEK, request)).isSameAs(response);
        var inserted = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(entries).saveAllAndFlush(inserted.capture());
        assertThat(inserted.getValue()).hasSize(21);
        verify(plans).touchWeek(12L);
    }

    private static ApplyAiWeeklyMenuRequest aiMenu() {
        List<ApplyAiWeeklyMenuRequest.Slot> slots = java.util.stream.IntStream.range(0, 21)
                .mapToObj(index -> new ApplyAiWeeklyMenuRequest.Slot(index / 3,
                        List.of("BREAKFAST", "LUNCH", "DINNER").get(index % 3), index % 2 == 0 ? 1L : 2L))
                .toList();
        return new ApplyAiWeeklyMenuRequest(slots);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 7})
    void rejectsDatesOutsideTheWeekBeforeWriting(int offset) {
        assertThatThrownBy(() -> service.addEntry(WEEK, new AddMealPlanEntryRequest(WEEK.plusDays(offset), "LUNCH", 1L)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);
        verify(plans, never()).createWeekIfAbsent(anyLong(), any());
        verifyNoInteractions(entries, recipes);
    }

    @Test
    void duplicateAddIsRejectedWithoutSaving() {
        when(entries.existsByMealPlanIdAndMealDateAndMealTypeAndRecipeIdAndIdNot(12, WEEK, "LUNCH", 1, 0)).thenReturn(true);
        assertThatThrownBy(() -> service.addEntry(WEEK, new AddMealPlanEntryRequest(WEEK, "LUNCH", 1L)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEAL_PLAN_RECIPE_DUPLICATE);
        verify(entries, never()).saveAndFlush(any());
        verifyNoInteractions(recipes);
    }

    @Test
    void unavailableSourceIsRejectedWithoutSavingAnEntry() {
        doThrow(new AppException(ErrorCode.RECIPE_NOT_FOUND)).when(recipes).requirePublishedMealPlanRecipe(1);
        assertThatThrownBy(() -> service.addEntry(WEEK, new AddMealPlanEntryRequest(WEEK, "LUNCH", 1L)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECIPE_NOT_FOUND);
        verify(entries, never()).saveAndFlush(any());
    }

    @Test
    void replacementPreservesSlotIdentityAndServings() {
        var entry = entry();
        when(entries.findByIdAndMealPlanId(31, 12)).thenReturn(Optional.of(entry));
        var result = service.replaceEntry(WEEK, 31, 2);
        assertThat(result.entryId()).isEqualTo(31);
        assertThat(result.recipeId()).isEqualTo(2);
        assertThat(result.mealType()).isEqualTo("LUNCH");
        assertThat(result.mealDate()).isEqualTo(WEEK);
        assertThat(result.plannedServings()).isEqualByComparingTo("2.5");
        verify(entries).existsByMealPlanIdAndMealDateAndMealTypeAndRecipeIdAndIdNot(12, WEEK, "LUNCH", 2, 31);
    }

    @Test
    void duplicateReplacementDoesNotChangeTheOriginalRecipe() {
        var entry = entry();
        when(entries.findByIdAndMealPlanId(31, 12)).thenReturn(Optional.of(entry));
        when(entries.existsByMealPlanIdAndMealDateAndMealTypeAndRecipeIdAndIdNot(12, WEEK, "LUNCH", 2, 31)).thenReturn(true);
        assertThatThrownBy(() -> service.replaceEntry(WEEK, 31, 2))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEAL_PLAN_RECIPE_DUPLICATE);
        assertThat(entry.getRecipeId()).isEqualTo(1);
        verify(entries, never()).saveAndFlush(any());
    }

    @Test
    void cannotReplaceOrRemoveAForeignOrMissingEntry() {
        assertThatThrownBy(() -> service.replaceEntry(WEEK, 900, 2))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
        assertThatThrownBy(() -> service.removeEntry(WEEK, 900))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
        verifyNoInteractions(recipes);
        verify(entries, never()).delete(any());
    }

    @Test
    void cannotAccessAnEntryWhenTheCallerHasNoWeek() {
        when(plans.lockWeek(17L, WEEK)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.removeEntry(WEEK, 31))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
        verifyNoInteractions(entries, recipes);
    }

    @Test
    void removalNeedsNoAvailableSourceRecipe() {
        var entry = entry();
        when(entries.findByIdAndMealPlanId(31, 12)).thenReturn(Optional.of(entry));
        service.removeEntry(WEEK, 31);
        verify(entries).delete(entry);
        verify(entries).flush();
        verify(plans).touchWeek(12L);
        verifyNoInteractions(recipes);
    }

    private MealPlanEntryEntity entry() {
        var entry = new MealPlanEntryEntity();
        entry.setId(31L);
        entry.setMealPlanId(12L);
        entry.setMealDate(WEEK);
        entry.setMealType("LUNCH");
        entry.setRecipeId(1L);
        entry.setPlannedServings(new BigDecimal("2.5"));
        return entry;
    }

    @Test
    void ac334MovesTheSameEntryAndPreservesFractionalServings() {
        var source = entry();
        when(entries.findByIdAndMealPlanId(31, 12)).thenReturn(Optional.of(source));
        var result = service.moveEntry(WEEK, 31, new MoveMealPlanEntryRequest(WEEK.plusDays(6), "DINNER"));
        assertThat(result.entryId()).isEqualTo(31);
        assertThat(result.recipeId()).isEqualTo(1);
        assertThat(result.mealDate()).isEqualTo(WEEK.plusDays(6));
        assertThat(result.mealType()).isEqualTo("DINNER");
        assertThat(result.plannedServings()).isEqualByComparingTo("2.5");
        verify(plans).lockWeek(17, WEEK);
        verify(plans).touchWeek(12);
        verify(recipes, never()).requirePublishedMealPlanRecipe(anyLong());
    }

    @Test
    void ac334DuplicateDestinationLeavesTheSourceUntouched() {
        var source = entry();
        when(entries.findByIdAndMealPlanId(31, 12)).thenReturn(Optional.of(source));
        when(entries.existsByMealPlanIdAndMealDateAndMealTypeAndRecipeIdAndIdNot(12, WEEK, "DINNER", 1, 31)).thenReturn(true);
        assertThatThrownBy(() -> service.moveEntry(WEEK, 31, new MoveMealPlanEntryRequest(WEEK, "DINNER")))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEAL_PLAN_RECIPE_DUPLICATE);
        assertThat(source.getMealType()).isEqualTo("LUNCH");
        assertThat(source.getMealDate()).isEqualTo(WEEK);
        assertThat(source.getPlannedServings()).isEqualByComparingTo("2.5");
        verify(entries, never()).saveAndFlush(any());
        verify(plans, never()).touchWeek(anyLong());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 7})
    void ac334CannotMoveOutsideTheWeek(int offset) {
        var source = entry();
        when(entries.findByIdAndMealPlanId(31, 12)).thenReturn(Optional.of(source));
        assertThatThrownBy(() -> service.moveEntry(WEEK, 31, new MoveMealPlanEntryRequest(WEEK.plusDays(offset), "DINNER")))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);
        assertThat(source.getMealDate()).isEqualTo(WEEK);
        verify(entries, never()).saveAndFlush(any());
    }

    @Test
    void cannotMoveAForeignEntry() {
        assertThatThrownBy(() -> service.moveEntry(WEEK, 999, new MoveMealPlanEntryRequest(WEEK, "DINNER")))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
        verify(entries, never()).saveAndFlush(any());
    }

    @Test
    void movingATombstoneKeepsItsContentPrivate() {
        when(entries.findByIdAndMealPlanId(31, 12)).thenReturn(Optional.of(entry()));
        doReturn(Map.of(1L,
                new RecipePostService.MealPlanRecipeReference(1, "Hidden title", "HIDDEN", "private-image", "SOUP", 25)))
                .when(recipes).findMealPlanReferences(anyList());
        var result = service.moveEntry(WEEK, 31, new MoveMealPlanEntryRequest(WEEK, "DINNER"));
        assertThat(result.recipeTitle()).isNull();
        assertThat(result.recipeCoverUrl()).isNull();
        assertThat(result.unavailableMessage()).isEqualTo("Công thức không còn khả dụng");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "4", "10"})
    void savesServingsWithoutChangingTheRecipeOrMeal(String quantity) {
        var entry = entry();
        when(entries.findByIdAndMealPlanId(31, 12)).thenReturn(Optional.of(entry));
        var result = service.updateServings(WEEK, 31, new BigDecimal(quantity));
        assertThat(result.plannedServings()).isEqualByComparingTo(quantity);
        assertThat(result.entryId()).isEqualTo(31);
        assertThat(result.recipeId()).isEqualTo(1);
        assertThat(result.mealDate()).isEqualTo(WEEK);
        assertThat(result.mealType()).isEqualTo("LUNCH");
        verify(entries).saveAndFlush(entry);
        verify(plans).touchWeek(12);
        verify(recipes, never()).requirePublishedMealPlanRecipe(anyLong());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "0.01", "10000", "1.1", "1.25", "9999.9"})
    void rejectsInvalidServingsBeforeChangingTheEntry(String quantity) {
        var entry = entry();
        when(entries.findByIdAndMealPlanId(31, 12)).thenReturn(Optional.of(entry));
        assertThatThrownBy(() -> service.updateServings(WEEK, 31, new BigDecimal(quantity)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);
        assertThat(entry.getPlannedServings()).isEqualByComparingTo("2.5");
        verify(entries, never()).saveAndFlush(any());
        verify(plans, never()).touchWeek(anyLong());
        verifyNoInteractions(recipes);
    }

    @Test
    void cannotChangeServingsOfAnotherMembersEntry() {
        assertThatThrownBy(() -> service.updateServings(WEEK, 900, BigDecimal.ONE))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
        verify(entries, never()).saveAndFlush(any());
        verifyNoInteractions(recipes);
    }

    @Test
    void updatingServingsDoesNotExposeHiddenRecipeContent() {
        when(entries.findByIdAndMealPlanId(31, 12)).thenReturn(Optional.of(entry()));
        doReturn(Map.of(1L,
                new RecipePostService.MealPlanRecipeReference(1, "Hidden title", "HIDDEN", "private-image", "SOUP", 25)))
                .when(recipes).findMealPlanReferences(anyList());
        var result = service.updateServings(WEEK, 31, new BigDecimal("4"));
        assertThat(result.plannedServings()).isEqualByComparingTo("4");
        assertThat(result.recipeTitle()).isNull();
        assertThat(result.recipeCoverUrl()).isNull();
        assertThat(result.unavailableMessage()).isEqualTo("Công thức không còn khả dụng");
    }
}

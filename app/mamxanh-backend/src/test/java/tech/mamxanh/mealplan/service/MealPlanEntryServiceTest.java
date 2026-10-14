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
    private final MealPlanService service = new MealPlanService(plans, users, mock(MealPlanReadService.class), entries, recipes);

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

    @ParameterizedTest
    @ValueSource(strings = {"1", "4", "9999"})
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

package tech.mamxanh.mealplan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.mealplan.entity.MealPlanEntity;
import tech.mamxanh.mealplan.entity.MealPlanEntryEntity;
import tech.mamxanh.mealplan.repository.MealPlanEntryRepository;
import tech.mamxanh.mealplan.repository.MealPlanRepository;
import tech.mamxanh.recipe.service.RecipePostService;
import tech.mamxanh.recipe.service.RecipePostService.MealPlanRecipeReference;

class MealPlanReadServiceTest {
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);
    private final MealPlanRepository plans = mock(MealPlanRepository.class);
    private final MealPlanEntryRepository entries = mock(MealPlanEntryRepository.class);
    private final CurrentUserService users = mock(CurrentUserService.class);
    private final RecipePostService recipes = mock(RecipePostService.class);
    private final MealPlanReadService service = new MealPlanReadService(plans, entries, users, recipes);

    @BeforeEach
    void member() {
        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(17L, Role.CUSTOMER));
    }

    @Test
    void emptyWeekNeverWritesOrFetchesRecipes() {
        when(plans.findByUserIdAndWeekStartDate(17L, MONDAY)).thenReturn(Optional.empty());
        var week = service.getWeek(MONDAY);
        assertThat(week.weekStartDate()).isEqualTo(MONDAY);
        assertThat(week.entries()).isEmpty();
        verifyNoInteractions(entries, recipes);
        verify(plans, never()).createWeekIfAbsent(anyLong(), any());
    }

    @Test
    void onlyPublishedRecipeContentIsExposedAndEveryReferenceRetainsItsSlot() {
        var plan = new MealPlanEntity();
        plan.setId(12L);
        when(plans.findByUserIdAndWeekStartDate(17L, MONDAY)).thenReturn(Optional.of(plan));
        when(entries.findAllByMealPlanIdOrderByMealDateAscMealTypeAscIdAsc(12L))
                .thenReturn(List.of(entry(1), entry(2), entry(3), entry(4)));
        when(recipes.findMealPlanReferences(List.of(1L, 2L, 3L, 4L))).thenReturn(Map.of(
                1L, reference(1, "PUBLISHED"), 2L, reference(2, "DELETED"), 3L, reference(3, "HIDDEN")));
        var result = service.getWeek(MONDAY).entries();
        assertThat(result).hasSize(4);
        assertThat(result.getFirst().recipeTitle()).isEqualTo("Món 1");
        assertThat(result.getFirst().unavailableMessage()).isNull();
        assertThat(result.get(1).recipeDeleted()).isTrue();
        for (var item : result.subList(1, 4)) {
            assertThat(item.recipeTitle()).isNull();
            assertThat(item.recipeCoverUrl()).isNull();
            assertThat(item.dishCategory()).isNull();
            assertThat(item.totalTimeMinutes()).isNull();
            assertThat(item.unavailableMessage()).isEqualTo("Công thức không còn khả dụng");
            assertThat(item.mealDate()).isEqualTo(MONDAY);
            assertThat(item.mealType()).isEqualTo("LUNCH");
            assertThat(item.plannedServings()).isEqualByComparingTo(BigDecimal.ONE);
        }
        assertThat(result.get(2).recipeDeleted()).isFalse();
        assertThat(result.get(3).recipeDeleted()).isFalse();
    }

    private MealPlanEntryEntity entry(long id) {
        var item = new MealPlanEntryEntity();
        item.setId(id);
        item.setRecipeId(id);
        item.setMealDate(MONDAY);
        item.setMealType("LUNCH");
        item.setPlannedServings(BigDecimal.ONE);
        return item;
    }

    private MealPlanRecipeReference reference(long id, String status) {
        return new MealPlanRecipeReference(id, "Món " + id, status, "https://example.test/image", "SOUP", 25);
    }
}

package tech.mamxanh.mealplan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.mealplan.dto.response.MealPlanWeekResponse;
import tech.mamxanh.mealplan.repository.MealPlanRepository;

class MealPlanServiceTest {
    private final MealPlanRepository repository = mock(MealPlanRepository.class);
    private final CurrentUserService users = mock(CurrentUserService.class);
    private final MealPlanReadService reads = mock(MealPlanReadService.class);
    private final tech.mamxanh.recipe.service.RecipePostService recipes = mock(tech.mamxanh.recipe.service.RecipePostService.class);
    private final MealPlanService service = new MealPlanService(repository, users, reads,
            mock(tech.mamxanh.mealplan.repository.MealPlanEntryRepository.class),
            recipes);

    @BeforeEach
    void member() {
        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(17L, Role.CUSTOMER));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-10-12", "2026-10-19", "2027-01-04"})
    void opensTheAuthenticatedMembersWeek(String date) {
        LocalDate monday = LocalDate.parse(date);
        var response = new MealPlanWeekResponse(monday, List.of());
        when(reads.getWeek(monday)).thenReturn(response);
        assertThat(service.openWeek(monday)).isSameAs(response);
        verify(repository).createWeekIfAbsent(17L, monday);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"2026-10-13", "2026-10-14", "2026-10-15", "2026-10-16", "2026-10-17", "2026-10-18", "0000-01-03", "9999-12-27"})
    void rejectsInvalidWeekBeforeWriting(String date) {
        assertThatThrownBy(() -> service.openWeek(date == null ? null : LocalDate.parse(date)))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);
        verifyNoInteractions(repository, reads);
    }

    @Test
    void failedAuthorizationNeverCreatesAPlan() {
        when(users.requireActiveMember()).thenThrow(new AppException(ErrorCode.ACCESS_DENIED));
        assertThatThrownBy(() -> service.openWeek(LocalDate.of(2026, 10, 12))).isInstanceOf(AppException.class);
        verifyNoInteractions(repository, reads);
    }

    @Test
    void acceptsHalfServingInTheApprovedRange() {
        var week = LocalDate.of(2026, 10, 12);
        var plan = new tech.mamxanh.mealplan.entity.MealPlanEntity();
        plan.setId(3L);
        var entry = new tech.mamxanh.mealplan.entity.MealPlanEntryEntity();
        entry.setId(5L); entry.setMealPlanId(3L); entry.setRecipeId(9L); entry.setMealDate(week);
        entry.setMealType("LUNCH"); entry.setPlannedServings(new java.math.BigDecimal("1"));
        when(repository.lockWeek(17L, week)).thenReturn(java.util.Optional.of(plan));
        var entryRepository = org.mockito.Mockito.mock(tech.mamxanh.mealplan.repository.MealPlanEntryRepository.class);
        when(entryRepository.findByIdAndMealPlanId(5L, 3L)).thenReturn(java.util.Optional.of(entry));
        when(recipes.findMealPlanReferences(List.of(9L))).thenReturn(java.util.Map.of(9L,
                new tech.mamxanh.recipe.service.RecipePostService.MealPlanRecipeReference(9L, "Soup", "PUBLISHED", null, "SOUP", 20)));
        var service = new MealPlanService(repository, users, reads, entryRepository, recipes);
        var result = service.updateServings(week, 5L, new java.math.BigDecimal("1.5"));
        assertThat(result.plannedServings()).isEqualByComparingTo("1.5");
        verify(repository).touchWeek(3L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.4", "0.6", "10.5", "11"})
    void rejectsOutOfRangeOrInvalidStepServing(String value) {
        var week = LocalDate.of(2026, 10, 12);
        var plan = new tech.mamxanh.mealplan.entity.MealPlanEntity();
        plan.setId(3L);
        var entry = new tech.mamxanh.mealplan.entity.MealPlanEntryEntity();
        entry.setId(5L); entry.setMealPlanId(3L); entry.setRecipeId(9L); entry.setMealDate(week);
        entry.setMealType("LUNCH"); entry.setPlannedServings(java.math.BigDecimal.ONE);
        when(repository.lockWeek(17L, week)).thenReturn(java.util.Optional.of(plan));
        var entryRepository = org.mockito.Mockito.mock(tech.mamxanh.mealplan.repository.MealPlanEntryRepository.class);
        when(entryRepository.findByIdAndMealPlanId(5L, 3L)).thenReturn(java.util.Optional.of(entry));
        var subject = new MealPlanService(repository, users, reads, entryRepository, recipes);
        assertThatThrownBy(() -> subject.updateServings(week, 5L, new java.math.BigDecimal(value)))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);
    }
}

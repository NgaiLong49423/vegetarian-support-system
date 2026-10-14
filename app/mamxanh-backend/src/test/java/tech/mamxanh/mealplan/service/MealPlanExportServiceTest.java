package tech.mamxanh.mealplan.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.mealplan.dto.response.MealPlanWeekResponse;
import tech.mamxanh.recipe.dto.response.RecipeDetailResponse;
import tech.mamxanh.recipe.service.RecipeService;

class MealPlanExportServiceTest {
    private final LocalDate week = LocalDate.of(2026, 10, 12);
    private final MealPlanReadService reads = mock(MealPlanReadService.class);
    private final RecipeService recipes = mock(RecipeService.class);
    private final MealPlanExportService service = new MealPlanExportService(reads, recipes);

    private MealPlanWeekResponse.Entry entry(long id, boolean available) {
        return new MealPlanWeekResponse.Entry(id, week, "LUNCH", new BigDecimal("2.5"), id,
                available ? "Old title" : null, null, null, null, false, available ? null : "Unavailable");
    }

    private void recipe(long id, boolean complete, Map<String, BigDecimal> nutrients) {
        var detail = mock(RecipeDetailResponse.class);
        when(detail.title()).thenReturn("Canh nấm mới cập nhật");
        when(detail.nutrition()).thenReturn(new RecipeDetailResponse.Nutrition(complete, List.of(), Map.of(), nutrients));
        when(recipes.getPublishedRecipe(id)).thenReturn(detail);
    }

    @Test
    void ac337UsesCurrentTitleAndScalesRealEnergyBySavedServings() {
        when(reads.getWeek(week)).thenReturn(new MealPlanWeekResponse(week, List.of(entry(1, true), entry(1, true))));
        recipe(1, true, Map.of("ENERGY_KCAL", new BigDecimal("123.45")));
        var result = service.getExport(week);
        assertThat(result.entries()).hasSize(2);
        assertThat(result.entries().getFirst().title()).isEqualTo("Canh nấm mới cập nhật");
        assertThat(result.entries().getFirst().energyKcal()).isEqualByComparingTo("308.625");
        verify(recipes, times(1)).getPublishedRecipe(1L);
    }

    @Test
    void ac337MissingNutritionIsNotZeroAndTombstonesDiscloseNoSource() {
        when(reads.getWeek(week)).thenReturn(new MealPlanWeekResponse(week, List.of(entry(1, true), entry(2, false))));
        recipe(1, false, Map.of("ENERGY_KCAL", BigDecimal.ZERO));
        var result = service.getExport(week).entries();
        assertThat(result.getFirst().energyKcal()).isNull();
        assertThat(result.get(1).title()).isEqualTo("Công thức không còn khả dụng");
        assertThat(result.get(1).energyKcal()).isNull();
        assertThat(result.get(1).plannedServings()).isNull();
        assertThat(result.get(1).available()).isFalse();
        verify(recipes, never()).getPublishedRecipe(2L);
    }

    @Test
    void ac337ZeroIsValidWhenNutritionIsCompleteButMissingEnergyIsNull() {
        when(reads.getWeek(week)).thenReturn(new MealPlanWeekResponse(week, List.of(entry(1, true), entry(2, true))));
        recipe(1, true, Map.of("ENERGY_KCAL", BigDecimal.ZERO));
        recipe(2, true, Map.of());
        var result = service.getExport(week).entries();
        assertThat(result.getFirst().energyKcal()).isEqualByComparingTo("0");
        assertThat(result.get(1).energyKcal()).isNull();
    }

    @Test
    void sourceHiddenDuringExportBecomesATombstone() {
        when(reads.getWeek(week)).thenReturn(new MealPlanWeekResponse(week, List.of(entry(1, true), entry(1, true))));
        when(recipes.getPublishedRecipe(1L)).thenThrow(new AppException(ErrorCode.NOT_FOUND));
        assertThat(service.getExport(week).entries()).allSatisfy(item -> assertThat(item.available()).isFalse());
        verify(recipes, times(1)).getPublishedRecipe(1L);
    }

    @Test
    void unexpectedFailureAbortsExportInsteadOfCreatingMisleadingFile() {
        when(reads.getWeek(week)).thenReturn(new MealPlanWeekResponse(week, List.of(entry(1, true))));
        when(recipes.getPublishedRecipe(1L)).thenThrow(new AppException(ErrorCode.ACCESS_DENIED));
        assertThatThrownBy(() -> service.getExport(week)).hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    void ownershipIsCheckedBeforeRecipeLookup() {
        when(reads.getWeek(week)).thenThrow(new AppException(ErrorCode.ACCESS_DENIED));
        assertThatThrownBy(() -> service.getExport(week)).hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
        verifyNoInteractions(recipes);
    }

    @Test
    void emptyWeekExportsWithoutCreatingData() {
        when(reads.getWeek(week)).thenReturn(new MealPlanWeekResponse(week, List.of()));
        assertThat(service.getExport(week).entries()).isEmpty();
        verifyNoInteractions(recipes);
    }
}

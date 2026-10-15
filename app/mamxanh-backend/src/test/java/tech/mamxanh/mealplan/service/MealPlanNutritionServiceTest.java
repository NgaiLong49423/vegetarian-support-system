package tech.mamxanh.mealplan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.mealplan.dto.response.MealPlanNutritionResponse;
import tech.mamxanh.mealplan.entity.MealPlanEntity;
import tech.mamxanh.mealplan.entity.MealPlanEntryEntity;
import tech.mamxanh.mealplan.repository.MealPlanEntryRepository;
import tech.mamxanh.mealplan.repository.MealPlanRepository;
import tech.mamxanh.nutrition.dto.response.NutritionProfileResponse;
import tech.mamxanh.nutrition.entity.NutritionDailyValueEntity;
import tech.mamxanh.nutrition.repository.NutritionDailyValueRepository;
import tech.mamxanh.nutrition.service.NutritionProfileService;
import tech.mamxanh.recipe.service.RecipeService;

class MealPlanNutritionServiceTest {
    private static final long OWNER = 17L;
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);
    private final CurrentUserService users = mock(CurrentUserService.class);
    private final NutritionProfileService profiles = mock(NutritionProfileService.class);
    private final MealPlanRepository plans = mock(MealPlanRepository.class);
    private final MealPlanEntryRepository entries = mock(MealPlanEntryRepository.class);
    private final NutritionDailyValueRepository dailyValues = mock(NutritionDailyValueRepository.class);
    private final RecipeService recipes = mock(RecipeService.class);
    private final MealPlanNutritionService service = new MealPlanNutritionService(
            users, profiles, plans, entries, dailyValues, recipes);

    @BeforeEach
    void setUp() {
        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(OWNER, Role.CUSTOMER));
        when(profiles.targetsIfProfileExists(OWNER)).thenReturn(Optional.empty());
    }

    @Test
    void rejectsInvalidWeekAndDayBeforeReadingMemberData() {
        assertThatThrownBy(() -> service.analyze(MONDAY.plusDays(1), null))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);
        assertThatThrownBy(() -> service.analyze(MONDAY, MONDAY.plusDays(7)))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);
        verifyNoInteractions(users, profiles, plans, entries, recipes, dailyValues);
    }

    @Test
    void emptyWeekUsesCatalogTargetsAndReturnsZeroTotalsAndAttainment() {
        when(plans.findByUserIdAndWeekStartDate(OWNER, MONDAY)).thenReturn(Optional.empty());
        when(recipes.calculatePlannedNutritionByEntry(Map.of(), Map.of())).thenReturn(Map.of());
        when(dailyValues.findAllById(anyIterable())).thenReturn(List.of(dailyValue("ENERGY_KCAL", "AMDR", "10")));

        var result = service.analyze(MONDAY, null);

        assertThat(result.scope()).isEqualTo("WEEK");
        assertThat(result.days()).hasSize(7);
        assertThat(result.days()).allSatisfy(day -> assertThat(day.nutrients().get("ENERGY_KCAL")).isEqualByComparingTo("0.00"));
        assertThat(result.weeklyAverage().get("ENERGY_KCAL")).isEqualByComparingTo("0.00");
        assertThat(result.attainmentDays().get("ENERGY_KCAL")).isZero();
        assertThat(result.targets()).hasSize(1);
        assertThat(result.incompleteEntries()).isEmpty();
    }

    @Test
    void dayAnalysisMapsPersonalTargetsAndReportsCompleteMissingAndUnavailableRecipes() {
        var plan = new MealPlanEntity(); plan.setId(31L);
        when(plans.findByUserIdAndWeekStartDate(OWNER, MONDAY)).thenReturn(Optional.of(plan));
        var complete = entry(101L, 501L, MONDAY, "BREAKFAST");
        var incomplete = entry(102L, 502L, MONDAY, "LUNCH");
        var unavailable = entry(103L, 503L, MONDAY, "DINNER");
        when(entries.findAllByMealPlanIdOrderByMealDateAscMealTypeAscIdAsc(31L))
                .thenReturn(List.of(complete, incomplete, unavailable));
        var allNutrients = Map.of("ENERGY_KCAL", new BigDecimal("210"), "PROTEIN_G", new BigDecimal("12"));
        when(recipes.calculatePlannedNutritionByEntry(anyMap(), anyMap())).thenReturn(Map.of(
                101L, new RecipeService.PlannedNutrition(501L, "Món đủ", true, List.of(), allNutrients),
                102L, new RecipeService.PlannedNutrition(502L, "Món thiếu", false, List.of("Sắt"), Map.of())));
        when(profiles.targetsIfProfileExists(OWNER)).thenReturn(Optional.of(personalTargets()));

        var result = service.analyze(MONDAY, MONDAY);

        assertThat(result.scope()).isEqualTo("DAY");
        assertThat(result.days()).hasSize(1);
        assertThat(result.days().getFirst().entries()).extracting(MealPlanNutritionResponse.Entry::recipeTitle)
                .containsExactly("Món đủ", "Món thiếu", "Công thức không còn khả dụng");
        assertThat(result.days().getFirst().nutrients().get("ENERGY_KCAL")).isEqualByComparingTo("210.00");
        assertThat(result.days().getFirst().nutrients().get("CALCIUM_MG")).isNull();
        assertThat(result.incompleteEntries()).hasSize(2);
        assertThat(result.targets()).extracting(MealPlanNutritionResponse.Target::key)
                .contains("ENERGY_KCAL", "PROTEIN_G", "CARBOHYDRATE_G", "TOTAL_FAT_G", "FIBER_G",
                        "CALCIUM_MG", "IRON_MG", "VITAMIN_B12_MCG", "ZINC_MG");
        assertThat(result.weeklyAverage()).isEmpty();
        assertThat(result.attainmentDays()).isEmpty();
    }

    @Test
    void eligibleProfileWithMissingTargetsAndAFullWeekProducesNullAveragesAndZeroAttainment() {
        when(plans.findByUserIdAndWeekStartDate(OWNER, MONDAY)).thenReturn(Optional.empty());
        when(recipes.calculatePlannedNutritionByEntry(Map.of(), Map.of())).thenReturn(Map.of());
        when(profiles.targetsIfProfileExists(OWNER)).thenReturn(Optional.of(new NutritionProfileResponse.Results(
                null, null, new BigDecimal("2000"), List.of(new NutritionProfileResponse.Target(
                        "protein", "Protein", BigDecimal.ONE, BigDecimal.TEN, "g", "AMDR", "reference")),
                "estimated", "reference")));

        var result = service.analyze(MONDAY, null);

        assertThat(result.targets()).hasSize(9);
        assertThat(result.targets()).filteredOn(target -> !target.key().equals("ENERGY_KCAL"))
                .filteredOn(target -> !target.key().equals("PROTEIN_G"))
                .allSatisfy(target -> assertThat(target.referenceType()).isEqualTo("NOT_AVAILABLE"));
        assertThat(result.weeklyAverage().get("ENERGY_KCAL")).isEqualByComparingTo("0.00");
        assertThat(result.attainmentDays().get("ENERGY_KCAL")).isZero();
        assertThat(result.referenceBasis()).contains("cá nhân");
    }

    @Test
    void weeklyAverageStaysUnknownWhenAPlannedRecipeHasNoComputableNutrients() {
        var plan = new MealPlanEntity(); plan.setId(32L);
        when(plans.findByUserIdAndWeekStartDate(OWNER, MONDAY)).thenReturn(Optional.of(plan));
        when(entries.findAllByMealPlanIdOrderByMealDateAscMealTypeAscIdAsc(32L))
                .thenReturn(List.of(entry(201L, 701L, MONDAY, "LUNCH")));
        when(recipes.calculatePlannedNutritionByEntry(anyMap(), anyMap())).thenReturn(Map.of(
                201L, new RecipeService.PlannedNutrition(701L, "Chưa đủ dữ liệu", false, List.of("Đậu"), Map.of())));
        when(dailyValues.findAllById(anyIterable())).thenReturn(List.of());

        var result = service.analyze(MONDAY, null);

        assertThat(result.days().getFirst().nutrients().get("ENERGY_KCAL")).isNull();
        assertThat(result.weeklyAverage().get("ENERGY_KCAL")).isNull();
        assertThat(result.incompleteEntries()).hasSize(1);
    }

    private static MealPlanEntryEntity entry(long id, long recipeId, LocalDate date, String mealType) {
        var entry = new MealPlanEntryEntity();
        entry.setId(id); entry.setRecipeId(recipeId); entry.setMealDate(date); entry.setMealType(mealType);
        entry.setPlannedServings(BigDecimal.ONE);
        return entry;
    }

    private static NutritionProfileResponse.Results personalTargets() {
        var targets = List.of("protein", "carbohydrate", "fat", "fiber", "calcium", "iron", "vitaminB12", "zinc", "FOLATE")
                .stream().map(key -> new NutritionProfileResponse.Target(key, key, BigDecimal.ONE, BigDecimal.TEN,
                        "g", "AMDR", "reference")).toList();
        return new NutritionProfileResponse.Results(null, null, new BigDecimal("1800"), targets, "estimated", "reference");
    }

    private static NutritionDailyValueEntity dailyValue(String key, String referenceType, String value) {
        var entity = mock(NutritionDailyValueEntity.class);
        when(entity.getNutrientKey()).thenReturn(key); when(entity.getLabel()).thenReturn(key);
        when(entity.getReferenceValue()).thenReturn(new BigDecimal(value)); when(entity.getUnit()).thenReturn("kcal");
        when(entity.getReferenceType()).thenReturn(referenceType); when(entity.getSourceName()).thenReturn("source");
        when(entity.getSourceUrl()).thenReturn("https://example.test"); when(entity.getVersionLabel()).thenReturn("v1");
        return entity;
    }
}

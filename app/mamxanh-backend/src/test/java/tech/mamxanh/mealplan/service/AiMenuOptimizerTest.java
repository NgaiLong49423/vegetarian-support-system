package tech.mamxanh.mealplan.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tech.mamxanh.nutrition.dto.response.NutritionProfileResponse.Target;

class AiMenuOptimizerTest {
    private final AiMenuOptimizer optimizer = new AiMenuOptimizer();
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 26);

    @Test
    void combinesOneServingRecipesToReachMealEnergyShares() {
        var ranked = List.of(candidate(1, 250), candidate(2, 250), candidate(3, 750), candidate(4, 750));

        var result = optimizer.optimize(MONDAY, MONDAY, MONDAY.plusDays(6), 2000, ranked, List.of());
        var firstDay = result.stream().filter(item -> item.date().equals(MONDAY)).toList();

        assertThat(firstDay).hasSize(4);
        assertThat(firstDay.stream().filter(item -> item.mealType().equals("BREAKFAST"))).hasSize(2);
        assertThat(kcal(firstDay, "BREAKFAST", ranked)).isEqualTo(500);
        assertThat(kcal(firstDay, "LUNCH", ranked)).isEqualTo(750);
        assertThat(kcal(firstDay, "DINNER", ranked)).isEqualTo(750);
        assertThat(result).hasSize(28);
    }

    @Test
    void prefersMealsInsideTheFivePercentagePointTolerance() {
        var ranked = List.of(candidate(1, 500), candidate(2, 750), candidate(3, 750), candidate(4, 100));

        var result = optimizer.optimize(MONDAY, MONDAY, MONDAY.plusDays(6), 2000, ranked, List.of());
        var firstDay = result.stream().filter(item -> item.date().equals(MONDAY)).toList();

        assertThat(kcal(firstDay, "BREAKFAST", ranked) / 2000.0).isBetween(0.20, 0.30);
        assertThat(kcal(firstDay, "LUNCH", ranked) / 2000.0).isBetween(0.325, 0.425);
        assertThat(kcal(firstDay, "DINNER", ranked) / 2000.0).isBetween(0.325, 0.425);
    }

    @Test
    void improvesOtherDailyNutritionCriteriaWhenEnergyDistributionIsEquivalent() {
        var ranked = List.of(candidate(1, 25), candidate(2, 38), candidate(3, 37),
                new AiMenuOptimizer.Candidate(4, Map.of("ENERGY_KCAL", BigDecimal.ZERO,
                        "PROTEIN_G", BigDecimal.TEN)));
        var protein = new Target("protein", "Chất đạm", BigDecimal.TEN, BigDecimal.valueOf(20), "g", "AMDR", "test");

        var result = optimizer.optimize(MONDAY, MONDAY, MONDAY.plusDays(6), 100, ranked, List.of(protein));
        var firstDay = result.stream().filter(item -> item.date().equals(MONDAY)).toList();

        assertThat(firstDay).anySatisfy(item -> {
            assertThat(item.recipeId()).isEqualTo(4);
            assertThat(item.mealType()).isEqualTo("BREAKFAST");
        });
        assertThat(firstDay.stream().filter(item -> item.mealType().equals("BREAKFAST")))
                .extracting(AiMenuOptimizer.Selection::recipeId).containsExactly(1L, 4L);
    }

    @Test
    void skipsDaysBeforeFirstSuggestionDate() {
        var ranked = List.of(candidate(1, 250), candidate(2, 750));

        var result = optimizer.optimize(MONDAY, MONDAY.plusDays(2), MONDAY.plusDays(6), 2000, ranked, List.of());

        assertThat(result).hasSize(15)
                .allSatisfy(selection -> assertThat(selection.date()).isAfterOrEqualTo(MONDAY.plusDays(2)));
    }

    private static int kcal(List<AiMenuOptimizer.Selection> meals, String type,
            List<AiMenuOptimizer.Candidate> candidates) {
        return meals.stream().filter(item -> item.mealType().equals(type)).mapToInt(item -> candidates.stream()
                .filter(candidate -> candidate.id() == item.recipeId()).findFirst().orElseThrow()
                .nutrition().get("ENERGY_KCAL").intValue()).sum();
    }

    private static AiMenuOptimizer.Candidate candidate(long id, int kcal) {
        return new AiMenuOptimizer.Candidate(id, Map.of("ENERGY_KCAL", BigDecimal.valueOf(kcal)));
    }
}

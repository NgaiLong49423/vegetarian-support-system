package tech.mamxanh.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class NutritionCalculationServiceTest {
    private final NutritionCalculationService service = new NutritionCalculationService();

    @Test
    void calculatesRawBmiAndClassifiesBeforeDisplayRounding() {
        BigDecimal bmi = service.bmi(new BigDecimal("65"), new BigDecimal("170"));
        assertEquals(0, new BigDecimal("22.4913494810").compareTo(bmi));
        assertEquals("Thiếu cân", service.bmiCategory(new BigDecimal("18.49")));
        assertEquals("Bình thường", service.bmiCategory(bmi));
        assertEquals("Bình thường", service.bmiCategory(new BigDecimal("24.96")));
        assertEquals("Thừa cân", service.bmiCategory(new BigDecimal("25")));
        assertEquals("Béo phì", service.bmiCategory(new BigDecimal("30")));
    }

    @Test
    void usesNaseMAdolescentEquationForAgeEighteenAndAdultEquationFromNineteen() {
        var height = new BigDecimal("170");
        var weight = new BigDecimal("65");
        BigDecimal eighteen = service.energyKcal(18, "FEMALE", height, weight, "SEDENTARY");
        BigDecimal nineteen = service.energyKcal(19, "FEMALE", height, weight, "SEDENTARY");
        assertEquals(0, new BigDecimal("55.59").subtract(new BigDecimal("22.25").multiply(BigDecimal.valueOf(18)))
                .add(new BigDecimal("8.43").multiply(height)).add(new BigDecimal("17.07").multiply(weight)).add(BigDecimal.valueOf(20)).compareTo(eighteen));
        assertTrue(eighteen.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(nineteen.compareTo(BigDecimal.ZERO) > 0);
        assertEquals(8, service.dailyTargets(19, "FEMALE", nineteen).size());
    }

    @Test
    void calculatesEnergyForBothAgeEquationsSexesAndEveryActivityLevel() {
        var height = new BigDecimal("170");
        var weight = new BigDecimal("65");
        List<String> sexes = List.of("FEMALE", "MALE");
        List<String> activities = List.of("SEDENTARY", "LIGHTLY_ACTIVE", "MODERATELY_ACTIVE", "VERY_ACTIVE");

        for (int age : List.of(18, 35)) {
            for (String sex : sexes) {
                for (String activity : activities) {
                    assertTrue(service.energyKcal(age, sex, height, weight, activity).compareTo(BigDecimal.ZERO) > 0,
                            () -> "Expected positive EER for age=" + age + ", sex=" + sex + ", activity=" + activity);
                }
            }
        }
    }

    @Test
    void selectsDailyTargetReferenceValuesAcrossAgeAndSexGroups() {
        assertTarget(18, "MALE", "fiber", "38");
        assertTarget(18, "FEMALE", "fiber", "26");
        assertTarget(35, "MALE", "fiber", "38");
        assertTarget(35, "FEMALE", "fiber", "25");
        assertTarget(51, "MALE", "fiber", "30");
        assertTarget(51, "FEMALE", "fiber", "21");
        assertTarget(71, "MALE", "calcium", "1200");
        assertTarget(35, "FEMALE", "calcium", "1000");
        assertTarget(51, "FEMALE", "calcium", "1200");
        assertTarget(18, "MALE", "calcium", "1300");
        assertTarget(18, "MALE", "iron", "11");
        assertTarget(18, "FEMALE", "iron", "15");
        assertTarget(35, "FEMALE", "iron", "18");
        assertTarget(51, "MALE", "iron", "8");
        assertTarget(18, "MALE", "zinc", "11");
        assertTarget(18, "FEMALE", "zinc", "9");
        assertTarget(35, "FEMALE", "zinc", "8");
        assertTarget(35, "FEMALE", "protein", "50", "175");
        assertTarget(18, "MALE", "protein", "50", "150");
        assertTarget(18, "FEMALE", "fat", "55.5555555556", "77.7777777778");
        assertEquals(8, service.dailyTargets(35, "MALE", new BigDecimal("2000")).size());
    }

    private void assertTarget(int age, String sex, String key, String expectedMinimum) {
        assertEquals(0, new BigDecimal(expectedMinimum).compareTo(target(age, sex, key).minimum()));
    }

    private void assertTarget(int age, String sex, String key, String expectedMinimum, String expectedMaximum) {
        var target = target(age, sex, key);
        assertEquals(0, new BigDecimal(expectedMinimum).compareTo(target.minimum()));
        assertEquals(0, new BigDecimal(expectedMaximum).compareTo(target.maximum()));
    }

    private tech.mamxanh.nutrition.dto.response.NutritionProfileResponse.Target target(int age, String sex, String key) {
        return service.dailyTargets(age, sex, new BigDecimal("2000")).stream()
                .filter(item -> item.key().equals(key))
                .findFirst()
                .orElseThrow();
    }
}

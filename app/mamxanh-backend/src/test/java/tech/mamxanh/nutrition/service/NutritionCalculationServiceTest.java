package tech.mamxanh.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class NutritionCalculationServiceTest {
    private final NutritionCalculationService service = new NutritionCalculationService();

    @Test
    void calculatesRawBmiAndClassifiesBeforeDisplayRounding() {
        BigDecimal bmi = service.bmi(new BigDecimal("65"), new BigDecimal("170"));
        assertEquals(0, new BigDecimal("22.4913494810").compareTo(bmi));
        assertEquals("Bình thường", service.bmiCategory(bmi));
        assertEquals("Bình thường", service.bmiCategory(new BigDecimal("24.96")));
        assertEquals("Thừa cân", service.bmiCategory(new BigDecimal("25")));
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
}

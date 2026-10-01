package tech.mamxanh.nutrition.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record NutritionProfileResponse(
        boolean hasProfile,
        boolean eligible,
        List<String> outOfScopeReasons,
        Profile profile,
        Results results) {

    public record Profile(
            LocalDate dateOfBirth,
            String biologicalSex,
            BigDecimal heightCm,
            BigDecimal weightKg,
            String activityLevel,
            String nutritionGoal) {
    }

    public record Results(
            BigDecimal bmi,
            String bmiCategory,
            BigDecimal energyKcal,
            List<Target> dailyTargets,
            String energySource,
            String nutrientSource) {
    }

    public record Target(
            String key,
            String label,
            BigDecimal minimum,
            BigDecimal maximum,
            String unit,
            String referenceType,
            String source) {
    }
}

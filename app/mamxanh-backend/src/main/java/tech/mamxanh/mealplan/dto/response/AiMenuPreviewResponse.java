package tech.mamxanh.mealplan.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

public record AiMenuPreviewResponse(LocalDate weekStartDate, String mode, String goal,
        BigDecimal energyTargetKcal, BigDecimal bmi, String bmiCategory,
        List<NutritionTarget> dailyTargets, List<Meal> meals, String nutritionDisclaimer) {
    public record NutritionTarget(String key, String label, BigDecimal minimum, BigDecimal maximum,
            String unit, String referenceType) { }
    public record Meal(LocalDate mealDate, String mealType, long recipeId, String recipeTitle,
            String coverUrl, BigDecimal plannedServings, Map<String, BigDecimal> nutritionPerServing,
            String reason) { }
}

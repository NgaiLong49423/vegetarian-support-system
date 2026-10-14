package tech.mamxanh.mealplan.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

public record AiMenuPreviewResponse(LocalDate weekStartDate, String mode, String goal,
        List<Meal> meals, String nutritionDisclaimer) {
    public record Meal(LocalDate mealDate, String mealType, long recipeId, String recipeTitle,
            String coverUrl, Map<String, BigDecimal> nutritionPerServing, String reason) { }
}

package tech.mamxanh.mealplan.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReplaceMealPlanEntryRequest(@NotNull @Min(1) Long recipeId,
        Boolean dietaryRiskAcknowledged, String dietaryRiskAssessmentToken) {
    public ReplaceMealPlanEntryRequest(Long recipeId) {
        this(recipeId, false, null);
    }
}

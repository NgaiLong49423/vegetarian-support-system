package tech.mamxanh.mealplan.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReplaceMealPlanEntryRequest(@NotNull @Min(1) Long recipeId) { }

package tech.mamxanh.mealplan.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

public record ReplaceMealPlanEntryRequest(@NotNull @Min(1) Long recipeId) { }

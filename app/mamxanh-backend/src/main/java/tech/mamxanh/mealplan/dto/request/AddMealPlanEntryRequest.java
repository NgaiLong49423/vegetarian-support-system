package tech.mamxanh.mealplan.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record AddMealPlanEntryRequest(
        @NotNull LocalDate mealDate,
        @NotNull @Pattern(regexp = "BREAKFAST|LUNCH|DINNER") String mealType,
        @NotNull @Min(1) Long recipeId) { }

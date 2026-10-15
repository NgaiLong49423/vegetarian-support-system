package tech.mamxanh.mealplan.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;

public record AddMealPlanEntryRequest(
        @NotNull LocalDate mealDate,
        @NotNull @Pattern(regexp = "BREAKFAST|LUNCH|DINNER") String mealType,
        @NotNull @Min(1) Long recipeId) { }

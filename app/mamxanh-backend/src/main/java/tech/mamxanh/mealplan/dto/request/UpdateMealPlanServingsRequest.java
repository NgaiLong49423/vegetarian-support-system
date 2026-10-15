package tech.mamxanh.mealplan.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateMealPlanServingsRequest(
        @NotNull @DecimalMin("0.5") @DecimalMax("10") @Digits(integer = 2, fraction = 1)
        @Schema(description = "Planned servings to eat; from 0.5 to 10 in increments of 0.5.",
                type = "number", example = "1.5", minimum = "0.5", maximum = "10", multipleOf = 0.5)
        BigDecimal plannedServings) { }

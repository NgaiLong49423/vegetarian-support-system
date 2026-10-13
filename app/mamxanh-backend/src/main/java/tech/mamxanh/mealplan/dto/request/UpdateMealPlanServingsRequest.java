package tech.mamxanh.mealplan.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateMealPlanServingsRequest(
        @NotNull @DecimalMin("1") @DecimalMax("9999") @Digits(integer = 4, fraction = 0)
        @Schema(description = "Number of servings to prepare; independent of the one-person nutrition summary.",
                type = "integer", example = "4", minimum = "1", maximum = "9999", multipleOf = 1)
        BigDecimal plannedServings) { }

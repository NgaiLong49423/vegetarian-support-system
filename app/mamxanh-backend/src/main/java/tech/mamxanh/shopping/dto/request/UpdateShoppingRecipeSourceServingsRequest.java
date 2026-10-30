package tech.mamxanh.shopping.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record UpdateShoppingRecipeSourceServingsRequest(
        @NotNull @DecimalMin("0.5") @DecimalMax("500.0") BigDecimal servings) { }

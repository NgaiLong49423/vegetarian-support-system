package tech.mamxanh.shopping.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateShoppingRecipeSourceServingsRequest(
        @NotNull @DecimalMin("0.5") @DecimalMax("500.0") BigDecimal servings) { }

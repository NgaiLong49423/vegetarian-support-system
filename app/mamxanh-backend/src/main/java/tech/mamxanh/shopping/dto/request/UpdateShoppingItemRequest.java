package tech.mamxanh.shopping.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

/** Quantity/unit edits never replace the ingredient identity or bought state. */
public record UpdateShoppingItemRequest(
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 8, fraction = 2) BigDecimal quantity,
        @NotNull @Positive Integer unitId) { }

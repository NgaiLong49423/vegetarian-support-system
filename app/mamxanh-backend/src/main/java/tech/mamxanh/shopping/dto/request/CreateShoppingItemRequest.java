package tech.mamxanh.shopping.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateShoppingItemRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 12, fraction = 6) BigDecimal quantity,
        @NotNull @Positive Integer unitId,
        @Positive Long ingredientId) {
    public CreateShoppingItemRequest(String name, BigDecimal quantity, Integer unitId) {
        this(name, quantity, unitId, null);
    }
}

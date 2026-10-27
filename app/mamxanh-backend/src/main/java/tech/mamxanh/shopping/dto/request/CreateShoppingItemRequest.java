package tech.mamxanh.shopping.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record CreateShoppingItemRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 12, fraction = 6) BigDecimal quantity,
        @NotNull @Positive Integer unitId) { }

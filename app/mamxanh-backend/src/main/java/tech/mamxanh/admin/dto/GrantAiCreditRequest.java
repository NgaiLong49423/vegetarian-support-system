package tech.mamxanh.admin.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record GrantAiCreditRequest(
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 29, fraction = 9) BigDecimal credits,
        @NotBlank @Size(min = 10, max = 500) String reason) { }

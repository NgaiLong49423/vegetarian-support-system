package tech.mamxanh.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;

public record GrantAiCreditRequest(
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 29, fraction = 9) BigDecimal credits,
        @NotBlank @Size(min = 10, max = 500) String reason) { }

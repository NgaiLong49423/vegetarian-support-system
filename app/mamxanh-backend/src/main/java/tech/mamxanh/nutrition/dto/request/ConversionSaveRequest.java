package tech.mamxanh.nutrition.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ConversionSaveRequest(
        @NotNull @DecimalMin(value = "0.000000001") @Digits(integer = 9, fraction = 9) BigDecimal gramsPerUnit,
        boolean approximate) { }

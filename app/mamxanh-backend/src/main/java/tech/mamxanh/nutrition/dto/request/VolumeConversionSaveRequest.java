package tech.mamxanh.nutrition.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record VolumeConversionSaveRequest(@NotNull @DecimalMin("0.000000001") @Digits(integer = 9, fraction = 9) BigDecimal gramsPerMl,
        boolean approximate) { }

package tech.mamxanh.nutrition.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import tech.mamxanh.nutrition.entity.MeasurementDimension;

public record UnitSaveRequest(
        @NotBlank @Size(max = 20) String code,
        @NotBlank @Size(max = 50) String name,
        @NotNull MeasurementDimension dimension,
        @NotNull @DecimalMin(value = "0.000001") @Digits(integer = 12, fraction = 6) BigDecimal baseFactor) { }
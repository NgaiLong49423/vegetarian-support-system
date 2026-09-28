package tech.mamxanh.nutrition.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ConversionSaveRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal gramsPerUnit,
        boolean approximate) { }

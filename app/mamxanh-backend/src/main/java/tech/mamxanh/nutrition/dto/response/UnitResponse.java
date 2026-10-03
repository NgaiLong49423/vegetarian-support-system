package tech.mamxanh.nutrition.dto.response;

import java.math.BigDecimal;
import tech.mamxanh.nutrition.entity.MeasurementDimension;

public record UnitResponse(Integer id, String code, String name, MeasurementDimension dimension,
                           BigDecimal baseFactor, boolean active) { }

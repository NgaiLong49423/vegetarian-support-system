package tech.mamxanh.nutrition.dto.response;

import tech.mamxanh.nutrition.entity.MeasurementDimension;

import java.math.BigDecimal;

public record IngredientUnitOptionResponse(Integer id, String code, String name, MeasurementDimension dimension,
        BigDecimal gramsPerUnit, boolean approximate) { }

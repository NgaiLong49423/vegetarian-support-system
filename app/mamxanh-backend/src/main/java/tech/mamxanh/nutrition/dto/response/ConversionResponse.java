package tech.mamxanh.nutrition.dto.response;

import java.math.BigDecimal;

public record ConversionResponse(Long ingredientId, Integer unitId, BigDecimal gramsPerUnit,
                                 boolean approximate, boolean active) { }

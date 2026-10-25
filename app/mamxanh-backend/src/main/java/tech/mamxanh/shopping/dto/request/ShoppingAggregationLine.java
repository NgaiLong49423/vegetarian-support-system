package tech.mamxanh.shopping.dto.request;

import java.math.BigDecimal;

/** Internal source line. Recipe callers must supply ingredientId; null denotes a manual item. */
public record ShoppingAggregationLine(Long ingredientId, String customName, BigDecimal quantity, Integer unitId) { }

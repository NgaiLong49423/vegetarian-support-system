package tech.mamxanh.nutrition.dto.response;

import java.math.BigDecimal;

public record IngredientNutritionResponse(
        BigDecimal energyKcalPer100g,
        BigDecimal proteinGramsPer100g,
        BigDecimal carbohydrateGramsPer100g,
        BigDecimal totalFatGramsPer100g,
        BigDecimal fiberGramsPer100g,
        BigDecimal calciumMgPer100g,
        BigDecimal ironMgPer100g,
        BigDecimal vitaminB12MicrogramsPer100g,
        BigDecimal zincMgPer100g) {
}

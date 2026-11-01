package tech.mamxanh.nutrition.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record IngredientNutritionResponse(
        @Schema(nullable = true, description = "Energy in kcal per 100 g; null means unknown.") BigDecimal energyKcalPer100g,
        @Schema(nullable = true, description = "Protein in g per 100 g; null means unknown.") BigDecimal proteinGramsPer100g,
        @Schema(nullable = true, description = "Carbohydrate in g per 100 g; null means unknown.") BigDecimal carbohydrateGramsPer100g,
        @Schema(nullable = true, description = "Total fat in g per 100 g; null means unknown.") BigDecimal totalFatGramsPer100g,
        @Schema(nullable = true, description = "Fiber in g per 100 g; null means unknown.") BigDecimal fiberGramsPer100g,
        @Schema(nullable = true, description = "Calcium in mg per 100 g; null means unknown.") BigDecimal calciumMgPer100g,
        @Schema(nullable = true, description = "Iron in mg per 100 g; null means unknown.") BigDecimal ironMgPer100g,
        @Schema(nullable = true, description = "Vitamin B12 in micrograms per 100 g, up to four decimal places; null means unknown.") BigDecimal vitaminB12MicrogramsPer100g,
        @Schema(nullable = true, description = "Zinc in mg per 100 g; null means unknown.") BigDecimal zincMgPer100g) {
}

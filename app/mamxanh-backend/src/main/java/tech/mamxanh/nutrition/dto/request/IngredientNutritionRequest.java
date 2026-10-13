package tech.mamxanh.nutrition.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

/** Nutrition values per 100 g. A null value represents data that is not known. */
public record IngredientNutritionRequest(
        @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal energyKcalPer100g,
        @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal proteinGramsPer100g,
        @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal carbohydrateGramsPer100g,
        @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal totalFatGramsPer100g,
        @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal fiberGramsPer100g,
        @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal calciumMgPer100g,
        @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal ironMgPer100g,
        @DecimalMin("0") @Digits(integer = 6, fraction = 4) BigDecimal vitaminB12MicrogramsPer100g,
        @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal zincMgPer100g) {
}

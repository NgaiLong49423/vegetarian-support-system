package tech.mamxanh.nutrition.dto.response;

import java.time.LocalDate;

public record IngredientResponse(Long id, String name, String ingredientGroup, String sourceName,
                                 String imageUrl, String sourceUrl, LocalDate referenceDate, boolean nutritionSupported,
                                 boolean active, IngredientNutritionResponse nutrition) {
    public IngredientResponse(Long id, String name, String ingredientGroup, String sourceName,
            String sourceUrl, LocalDate referenceDate, boolean nutritionSupported, boolean active) {
        this(id, name, ingredientGroup, sourceName, null, sourceUrl, referenceDate, nutritionSupported, active, null);
    }

    public IngredientResponse(Long id, String name, String ingredientGroup, String sourceName,
            String sourceUrl, LocalDate referenceDate, boolean nutritionSupported, boolean active,
            IngredientNutritionResponse nutrition) {
        this(id, name, ingredientGroup, sourceName, null, sourceUrl, referenceDate,
                nutritionSupported, active, nutrition);
    }
}

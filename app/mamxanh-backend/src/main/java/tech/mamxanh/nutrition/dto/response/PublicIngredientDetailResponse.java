package tech.mamxanh.nutrition.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** Public ingredient detail; internal catalog and nutrition provenance fields are intentionally omitted. */
public record PublicIngredientDetailResponse(long ingredientId, String name, String ingredientGroup,
        @Schema(nullable = true, description = "Optional ingredient illustration URL.") String imageUrl,
        @Schema(description = "Nutrition per 100 g. Null means unknown; zero is a known zero value.")
        IngredientNutritionResponse nutrition) {
}

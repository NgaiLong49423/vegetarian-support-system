package tech.mamxanh.nutrition.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** Public catalog identity and optional illustration; administrative source and nutrition fields are omitted. */
public record PublicIngredientResponse(long ingredientId, String name, String ingredientGroup,
        @Schema(nullable = true, description = "Optional ingredient illustration URL.") String imageUrl) {
}

package tech.mamxanh.nutrition.dto.response;

/** An active standard ingredient offered while the Member types an ingredient name. */
public record IngredientSuggestionResponse(Long id, String name, String ingredientGroup) {
}

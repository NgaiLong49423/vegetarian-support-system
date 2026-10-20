package tech.mamxanh.nutrition.dto.response;

/** Minimal public catalog item; administrative source and nutrition fields are intentionally omitted. */
public record PublicIngredientResponse(long ingredientId, String name, String ingredientGroup) {
}

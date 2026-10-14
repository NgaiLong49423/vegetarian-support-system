package tech.mamxanh.recipe.dto.response;

import java.util.List;

public record AiRecipeRecommendationResponse(List<Item> items) {
    public record Item(RecipePostResponse recipe, String reason) { }
}

package tech.mamxanh.recipe.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AiRecipeRecommendationRequest(
        @NotEmpty @Size(max = 20) List<Long> ingredientIds) { }

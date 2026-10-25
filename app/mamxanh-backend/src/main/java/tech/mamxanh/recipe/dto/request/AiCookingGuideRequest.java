package tech.mamxanh.recipe.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AiCookingGuideRequest(@NotNull @Min(1) Long recipeId) { }
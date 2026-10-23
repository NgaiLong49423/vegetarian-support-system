package tech.mamxanh.recipe.dto.response;

import java.util.List;

public record AiCookingGuideResponse(long recipeId, String recipeTitle, List<String> steps, String aiNotice) { }
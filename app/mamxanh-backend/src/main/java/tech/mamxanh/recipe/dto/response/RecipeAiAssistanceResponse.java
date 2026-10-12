package tech.mamxanh.recipe.dto.response;

import tech.mamxanh.recipe.dto.request.RecipeAiAssistanceRequest.ContentType;

public record RecipeAiAssistanceResponse(ContentType contentType, String content) { }
